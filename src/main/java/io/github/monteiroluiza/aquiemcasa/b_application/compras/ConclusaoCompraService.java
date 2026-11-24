package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import io.github.monteiroluiza.aquiemcasa.a_domain.CategoriaProduto;
import io.github.monteiroluiza.aquiemcasa.a_domain.ItemLista;
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

    public ConclusaoCompraService(AcessoCasaService acesso, ListaComprasRepository listas,
                                  ItemListaRepository itens, ProdutoEstoqueRepository produtos) {
        this.acesso = acesso;
        this.listas = listas;
        this.itens = itens;
        this.produtos = produtos;
    }

    @Transactional(readOnly = true)
    public PreviaConclusaoResponse gerarPrevia(UUID casaId, String email) {
        acesso.exigirMembroAtivo(casaId, email);
        var lista = listas.findByCasaId(casaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lista de compras não encontrada."));
        var itensAtuais = itens.findByListaId(lista.getId());
        var carrinho = itensAtuais.stream()
                .filter(item -> item.getSituacao() == SituacaoItemLista.NO_CARRINHO)
                .toList();
        if (carrinho.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Marque ao menos um item no carrinho antes de concluir a compra.");
        }

        var produtosDaCasa = produtos.findByCasaIdOrderByCategoriaAscNomeAsc(casaId);
        Map<String, CompraAcumulada> acumuladas = new LinkedHashMap<>();
        for (var item : carrinho) {
            ProdutoEstoque produto = resolverProduto(item, produtosDaCasa);
            String chave = produto == null
                    ? "avulso:" + normalizar(item.getNomeExibicao()) + "|" + normalizar(item.getUnidadeExibicao())
                    : "produto:" + produto.getId();
            acumuladas.computeIfAbsent(chave, ignorada -> new CompraAcumulada(item, produto))
                    .adicionar(item);
        }

        var compras = acumuladas.values().stream()
                .map(CompraAcumulada::resposta)
                .sorted(Comparator.comparing(CompraPrevistaResponse::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
        var pendentes = itensAtuais.stream()
                .filter(item -> item.getSituacao() == SituacaoItemLista.PENDENTE)
                .sorted(Comparator.comparing(ItemLista::getNomeExibicao, String.CASE_INSENSITIVE_ORDER))
                .map(ItemListaResponse::de)
                .toList();
        return new PreviaConclusaoResponse(lista.getId(), lista.getVersao(), compras, pendentes);
    }

    private ProdutoEstoque resolverProduto(ItemLista item, List<ProdutoEstoque> produtosDaCasa) {
        if (item.getProduto() != null) {
            return item.getProduto();
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

    private String normalizar(String valor) {
        return valor.strip().toLowerCase(Locale.ROOT);
    }

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

        void adicionar(ItemLista item) {
            itemIds.add(item.getId());
            quantidade = quantidade.add(item.getQuantidade());
        }

        CompraPrevistaResponse resposta() {
            BigDecimal saldoAtual = produto == null ? BigDecimal.ZERO : produto.getQuantidadeAtual();
            return new CompraPrevistaResponse(List.copyOf(itemIds), produto == null ? null : produto.getId(),
                    nome, categoria, unidade, quantidade, saldoAtual, saldoAtual.add(quantidade), produto == null);
        }
    }
}
