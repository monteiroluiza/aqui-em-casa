package io.github.monteiroluiza.aquiemcasa.b_application.estoque;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import io.github.monteiroluiza.aquiemcasa.a_domain.ProdutoEstoque;
import io.github.monteiroluiza.aquiemcasa.a_domain.SituacaoEstoque;
import io.github.monteiroluiza.aquiemcasa.b_application.categoria.CategoriaService;
import io.github.monteiroluiza.aquiemcasa.b_application.casa.AcessoCasaService;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.ProdutoEstoqueRepository;

@Service
public class EstoqueService {
    private final ProdutoEstoqueRepository produtos;
    private final AcessoCasaService acesso;
    private final CategoriaService categorias;
    private final org.springframework.jdbc.core.simple.JdbcClient jdbc;

    public EstoqueService(ProdutoEstoqueRepository produtos, AcessoCasaService acesso,
                          CategoriaService categorias, org.springframework.jdbc.core.simple.JdbcClient jdbc) {
        this.produtos = produtos;
        this.acesso = acesso;
        this.categorias = categorias;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> consultar(UUID casaId, String email, String busca,
                                           UUID categoriaId, SituacaoEstoque situacao) {
        acesso.exigirMembroAtivo(casaId, email);
        String termo = busca == null ? "" : busca.strip().toLowerCase(Locale.ROOT);
        return produtos.findByCasaIdOrderByCategoriaNomeAscNomeAsc(casaId).stream()
                .filter(produto -> termo.isEmpty()
                        || produto.getNome().toLowerCase(Locale.ROOT).contains(termo)
                        || produto.getCategoria().getNome().toLowerCase(Locale.ROOT).contains(termo))
                .filter(produto -> categoriaId == null || produto.getCategoria().getId().equals(categoriaId))
                .filter(produto -> situacao == null || produto.getSituacao() == situacao)
                .map(ProdutoResponse::de)
                .toList();
    }

    @Transactional
    public ProdutoResponse cadastrar(UUID casaId, String email, CadastrarProdutoRequest dados) {
        var membro = acesso.exigirMembroAtivo(casaId, email);
        var categoria = categorias.buscarDaCasa(casaId, dados.categoriaId());
        var produto = new ProdutoEstoque(membro.getCasa(), dados.nome().strip(), categoria,
                dados.unidade().strip(), dados.quantidadeAtual(), dados.quantidadeDesejada(), membro);
        return ProdutoResponse.de(produtos.save(produto));
    }

    @Transactional
    public ProdutoResponse atualizar(UUID casaId, UUID produtoId, String email, AtualizarProdutoRequest dados) {
        var membro = acesso.exigirMembroAtivo(casaId, email);
        var produto = buscarParaAlteracao(casaId, produtoId);
        var itemId = produtos.buscarItemVinculado(produtoId);
        if (!produto.getUnidade().equals(dados.unidade().strip()) && itemId.isPresent()) {
            throw conflitoComLista(itemId.get(), "Não é possível trocar a unidade deste produto.");
        }
        var categoria = categorias.buscarDaCasa(casaId, dados.categoriaId());
        produto.atualizar(dados.nome().strip(), categoria, dados.unidade().strip(),
                dados.quantidadeAtual(), dados.quantidadeDesejada(), membro);
        return ProdutoResponse.de(produto);
    }

    @Transactional
    public ProdutoResponse ajustarQuantidade(UUID casaId, UUID produtoId, String email,
                                              AjustarQuantidadeRequest dados) {
        var membro = acesso.exigirMembroAtivo(casaId, email);
        var produto = buscarParaAlteracao(casaId, produtoId);
        if (dados.ajuste().signum() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ajuste deve ser diferente de zero.");
        }
        var novaQuantidade = produto.getQuantidadeAtual().add(dados.ajuste());
        if (novaQuantidade.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O estoque não pode ficar negativo.");
        }
        if (novaQuantidade.scale() > 3 || novaQuantidade.precision() - novaQuantidade.scale() > 9) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A quantidade excede o limite permitido.");
        }
        produto.ajustarQuantidade(novaQuantidade, membro);
        return ProdutoResponse.de(produto);
    }

    @Transactional
    public void excluir(UUID casaId, UUID produtoId, String email) {
        acesso.exigirMembroAtivo(casaId, email);
        var produto = buscarParaAlteracao(casaId, produtoId);
        produtos.buscarItemVinculado(produtoId).ifPresent(itemId -> {
            throw conflitoComLista(itemId, "Não é possível excluir este produto.");
        });
        produto.ajustarQuantidade(produto.getQuantidadeAtual(), acesso.exigirMembroAtivo(casaId, email));
        produtos.flush();
        produtos.delete(produto);
    }

    @Transactional
    public ProdutoResponse consumir(UUID casaId, UUID produtoId, String email, ConsumirProdutoRequest dados) {
        jdbc.sql("select set_config('app.tipo_movimentacao', 'CONSUMO', true)").query(String.class).single();
        return ajustarQuantidade(casaId, produtoId, email, new AjustarQuantidadeRequest(dados.quantidade().negate()));
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> alertas(UUID casaId, String email) {
        return consultar(casaId, email, null, null, null).stream()
                .filter(p -> p.situacao() != SituacaoEstoque.EM_DIA).toList();
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> movimentacoes(UUID casaId, String email, UUID produtoId, int pagina, int tamanho) {
        acesso.exigirMembroAtivo(casaId, email);
        if (pagina < 0 || tamanho < 1 || tamanho > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pagina deve ser >= 0 e tamanho entre 1 e 100.");
        return jdbc.sql("""
            select m.id, m.produto_id as "produtoId", m.nome, m.unidade, m.tipo,
                   m.saldo_anterior as "saldoAnterior", m.saldo_atual as "saldoAtual", m.quantidade,
                   m.responsavel_id as "responsavelId", u.nome as responsavel, m.criada_em as "criadaEm"
            from movimentacao_estoque m join membro_casa mc on mc.id=m.responsavel_id
            join usuario u on u.id=mc.usuario_id
            where m.casa_id=:casa
            """ + (produtoId == null ? "" : " and m.produto_id=:produto") + " order by m.criada_em desc, m.id limit :limite offset :inicio")
            .params(produtoId == null ? java.util.Map.of("casa", casaId, "limite", tamanho, "inicio", (long) pagina*tamanho)
                : java.util.Map.of("casa", casaId, "produto", produtoId, "limite", tamanho, "inicio", (long) pagina*tamanho))
            .query().listOfRows();
    }
    private ProdutoEstoque buscarParaAlteracao(UUID casaId, UUID produtoId) {
        return produtos.buscarParaAlteracao(casaId, produtoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
    }

    private ResponseStatusException conflitoComLista(UUID itemId, String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                mensagem + " Remova primeiro o item da lista de compras: " + itemId + ".");
    }
}
