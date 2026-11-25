package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import io.github.monteiroluiza.aquiemcasa.a_domain.CategoriaProduto;
import io.github.monteiroluiza.aquiemcasa.a_domain.ItemLista;
import io.github.monteiroluiza.aquiemcasa.a_domain.ListaCompras;
import io.github.monteiroluiza.aquiemcasa.a_domain.ProdutoEstoque;
import io.github.monteiroluiza.aquiemcasa.a_domain.SituacaoItemLista;
import io.github.monteiroluiza.aquiemcasa.b_application.casa.AcessoCasaService;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.ItemListaRepository;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.ListaComprasRepository;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.ProdutoEstoqueRepository;

@Service
public class ConclusaoCompraService {
    private final AcessoCasaService acesso;
    private final ListaComprasRepository listas;
    private final ItemListaRepository itens;
    private final ProdutoEstoqueRepository produtos;
    private final JdbcClient jdbc;

    public ConclusaoCompraService(AcessoCasaService acesso, ListaComprasRepository listas,
                                  ItemListaRepository itens, ProdutoEstoqueRepository produtos,
                                  JdbcClient jdbc) {
        this.acesso = acesso;
        this.listas = listas;
        this.itens = itens;
        this.produtos = produtos;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public PreviaConclusaoResponse gerarPrevia(UUID casaId, String email) {
        acesso.exigirMembroAtivo(casaId, email);
        var lista = listas.findByCasaId(casaId).orElseThrow(this::listaNaoEncontrada);
        var resumo = calcularResumo(lista, itens.findByListaId(lista.getId()),
                produtos.findByCasaIdOrderByCategoriaAscNomeAsc(casaId));
        exigirItensNoCarrinho(resumo);
        return resumo.resposta();
    }

    @Transactional
    public ConclusaoCompraResponse concluir(UUID casaId, String email, ConcluirCompraRequest dados) {
        var membro = acesso.exigirMembroAtivo(casaId, email);
        byte[] payloadHash = calcularHash(dados);

        bloquearConfirmacao(dados.confirmacaoId());
        var anterior = buscarConfirmacao(dados.confirmacaoId());
        if (anterior != null) {
            if (!anterior.casaId().equals(casaId) || !MessageDigest.isEqual(anterior.payloadHash(), payloadHash)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O identificador de confirmacao ja foi utilizado em outra requisicao.");
            }
            return new ConclusaoCompraResponse(dados.confirmacaoId(), anterior.concluidaEm(),
                    anterior.versaoResultante(), anterior.itensProcessados(), true);
        }

        var lista = listas.buscarPorCasaParaAlteracao(casaId).orElseThrow(this::listaNaoEncontrada);
        var resumo = calcularResumo(lista, itens.findByListaId(lista.getId()),
                produtos.buscarTodosDaCasaParaAlteracao(casaId));
        exigirItensNoCarrinho(resumo);
        if (lista.getVersao() != dados.versao() || !snapshotAtual(resumo).equals(snapshotRecebido(dados))) {
            throw new ResumoDesatualizadoException(resumo.resposta());
        }

        for (var compra : resumo.compras()) {
            validarQuantidade(compra.quantidade());
            if (compra.produto() == null) {
                produtos.save(new ProdutoEstoque(lista.getCasa(), compra.nome(), compra.categoria(),
                        compra.unidade(), compra.quantidade(), BigDecimal.ZERO, membro));
            } else {
                validarQuantidade(compra.produto().getQuantidadeAtual().add(compra.quantidade()));
                compra.produto().receberCompra(compra.quantidade(), membro);
            }
        }

        int itensProcessados = resumo.carrinho().size();
        itens.deleteAll(resumo.carrinho());
        lista.avancarVersao();
        listas.saveAndFlush(lista);

        Instant concluidaEm = Instant.now();
        jdbc.sql("""
                insert into requisicao_conclusao_compra
                    (id, casa_id, lista_id, concluido_por_id, payload_hash,
                     versao_resultante, itens_processados, concluida_em)
                values (:id, :casaId, :listaId, :membroId, :payloadHash,
                        :versao, :itensProcessados, :concluidaEm)
                """)
                .param("id", dados.confirmacaoId()).param("casaId", casaId)
                .param("listaId", lista.getId()).param("membroId", membro.getId())
                .param("payloadHash", payloadHash).param("versao", lista.getVersao())
                .param("itensProcessados", itensProcessados).param("concluidaEm", concluidaEm)
                .update();
        return new ConclusaoCompraResponse(dados.confirmacaoId(), concluidaEm,
                lista.getVersao(), itensProcessados, false);
    }

    private ResumoCalculado calcularResumo(ListaCompras lista, List<ItemLista> itensAtuais,
                                           List<ProdutoEstoque> produtosDaCasa) {
        var carrinho = itensAtuais.stream()
                .filter(item -> item.getSituacao() == SituacaoItemLista.NO_CARRINHO).toList();
        Map<String, CompraAcumulada> acumuladas = new LinkedHashMap<>();
        for (var item : carrinho) {
            ProdutoEstoque produto = resolverProduto(item, produtosDaCasa);
            String chave = produto == null
                    ? "avulso:" + normalizar(item.getNomeExibicao()) + "|" + normalizar(item.getUnidadeExibicao())
                    : "produto:" + produto.getId();
            acumuladas.computeIfAbsent(chave, ignorada -> new CompraAcumulada(item, produto)).adicionar(item);
        }
        var comprasAcumuladas = acumuladas.values().stream()
                .sorted(Comparator.comparing(CompraAcumulada::nome, String.CASE_INSENSITIVE_ORDER)).toList();
        var compras = comprasAcumuladas.stream().map(CompraAcumulada::resposta).toList();
        var pendentes = itensAtuais.stream()
                .filter(item -> item.getSituacao() == SituacaoItemLista.PENDENTE)
                .sorted(Comparator.comparing(ItemLista::getNomeExibicao, String.CASE_INSENSITIVE_ORDER))
                .map(ItemListaResponse::de).toList();
        return new ResumoCalculado(new PreviaConclusaoResponse(lista.getId(), lista.getVersao(), compras, pendentes),
                comprasAcumuladas, carrinho);
    }

    private ProdutoEstoque resolverProduto(ItemLista item, List<ProdutoEstoque> produtosDaCasa) {
        if (item.getProduto() != null) {
            UUID produtoId = item.getProduto().getId();
            return produtosDaCasa.stream().filter(produto -> produto.getId().equals(produtoId)).findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "O produto associado ao item nao esta mais disponivel."));
        }
        var compativeis = produtosDaCasa.stream()
                .filter(produto -> normalizar(produto.getNome()).equals(normalizar(item.getNomeExibicao())))
                .filter(produto -> normalizar(produto.getUnidade()).equals(normalizar(item.getUnidadeExibicao())))
                .toList();
        if (compativeis.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O item avulso " + item.getNomeExibicao()
                            + " corresponde a mais de um produto. Remova-o e escolha o produto correto.");
        }
        return compativeis.isEmpty() ? null : compativeis.getFirst();
    }

    private List<String> snapshotAtual(ResumoCalculado resumo) {
        return resumo.compras().stream().map(compra -> snapshot(compra.itemIds(),
                        compra.produto() == null ? null : compra.produto().getId(), compra.unidade(),
                        compra.produto() == null ? BigDecimal.ZERO : compra.produto().getQuantidadeAtual()))
                .sorted().toList();
    }

    private List<String> snapshotRecebido(ConcluirCompraRequest dados) {
        return dados.compras().stream()
                .map(compra -> snapshot(compra.itemIds(), compra.produtoId(), compra.unidade(), compra.saldoAtual()))
                .sorted().toList();
    }

    private String snapshot(List<UUID> itemIds, UUID produtoId, String unidade, BigDecimal saldoAtual) {
        String ids = itemIds.stream().map(UUID::toString).sorted().reduce((a, b) -> a + "," + b).orElse("");
        return ids + "|" + (produtoId == null ? "novo" : produtoId) + "|"
                + normalizar(unidade) + "|" + numeroCanonico(saldoAtual);
    }

    private byte[] calcularHash(ConcluirCompraRequest dados) {
        String conteudo = dados.versao() + "\n" + String.join("\n", snapshotRecebido(dados));
        try {
            return MessageDigest.getInstance("SHA-256").digest(conteudo.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException excecao) {
            throw new IllegalStateException("SHA-256 indisponivel.", excecao);
        }
    }

    private void bloquearConfirmacao(UUID confirmacaoId) {
        long chave = confirmacaoId.getMostSignificantBits() ^ confirmacaoId.getLeastSignificantBits();
        jdbc.sql("select pg_advisory_xact_lock(:chave) is null")
                .param("chave", chave).query(Boolean.class).single();
    }

    private ConfirmacaoRegistrada buscarConfirmacao(UUID confirmacaoId) {
        return jdbc.sql("""
                select casa_id, payload_hash, versao_resultante, itens_processados, concluida_em
                from requisicao_conclusao_compra where id = :id
                """).param("id", confirmacaoId)
                .query((rs, linha) -> new ConfirmacaoRegistrada(rs.getObject("casa_id", UUID.class),
                        rs.getBytes("payload_hash"), rs.getLong("versao_resultante"),
                        rs.getInt("itens_processados"), rs.getTimestamp("concluida_em").toInstant()))
                .optional().orElse(null);
    }

    private void exigirItensNoCarrinho(ResumoCalculado resumo) {
        if (resumo.carrinho().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Marque ao menos um item no carrinho antes de concluir a compra.");
        }
    }

    private void validarQuantidade(BigDecimal quantidade) {
        BigDecimal normalizada = quantidade.stripTrailingZeros();
        int casasDecimais = Math.max(normalizada.scale(), 0);
        int casasInteiras = normalizada.precision() - normalizada.scale();
        if (casasDecimais > 3 || casasInteiras > 9) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A quantidade excede o limite permitido.");
        }
    }

    private String numeroCanonico(BigDecimal valor) { return valor.stripTrailingZeros().toPlainString(); }
    private String normalizar(String valor) { return valor.strip().toLowerCase(Locale.ROOT); }
    private ResponseStatusException listaNaoEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Lista de compras nao encontrada.");
    }

    private record ResumoCalculado(PreviaConclusaoResponse resposta, List<CompraAcumulada> compras,
                                   List<ItemLista> carrinho) {}
    private record ConfirmacaoRegistrada(UUID casaId, byte[] payloadHash, long versaoResultante,
                                         int itensProcessados, Instant concluidaEm) {}

    private static class CompraAcumulada {
        private final List<UUID> itemIds = new ArrayList<>();
        private final ProdutoEstoque produto;
        private final String nome;
        private final CategoriaProduto categoria;
        private final String unidade;
        private BigDecimal quantidade = BigDecimal.ZERO;

        CompraAcumulada(ItemLista item, ProdutoEstoque produto) {
            this.produto = produto;
            this.nome = produto == null ? item.getNomeExibicao() : produto.getNome();
            this.categoria = produto == null ? item.getCategoriaExibicao() : produto.getCategoria();
            this.unidade = produto == null ? item.getUnidadeExibicao() : produto.getUnidade();
        }
        void adicionar(ItemLista item) { itemIds.add(item.getId()); quantidade = quantidade.add(item.getQuantidade()); }
        String nome() { return nome; }
        CategoriaProduto categoria() { return categoria; }
        String unidade() { return unidade; }
        BigDecimal quantidade() { return quantidade; }
        ProdutoEstoque produto() { return produto; }
        List<UUID> itemIds() { return itemIds; }
        CompraPrevistaResponse resposta() {
            BigDecimal saldoAtual = produto == null ? BigDecimal.ZERO : produto.getQuantidadeAtual();
            return new CompraPrevistaResponse(List.copyOf(itemIds), produto == null ? null : produto.getId(),
                    nome, categoria, unidade, quantidade, saldoAtual, saldoAtual.add(quantidade), produto == null);
        }
    }
}
