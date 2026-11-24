package io.github.monteiroluiza.aquiemcasa.b_application.estoque;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import io.github.monteiroluiza.aquiemcasa.a_domain.ProdutoEstoque;
import io.github.monteiroluiza.aquiemcasa.a_domain.CategoriaProduto;
import io.github.monteiroluiza.aquiemcasa.a_domain.SituacaoEstoque;
import io.github.monteiroluiza.aquiemcasa.b_application.casa.AcessoCasaService;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.ProdutoEstoqueRepository;

@Service
public class EstoqueService {
    private final ProdutoEstoqueRepository produtos;
    private final AcessoCasaService acesso;

    public EstoqueService(ProdutoEstoqueRepository produtos, AcessoCasaService acesso) {
        this.produtos = produtos;
        this.acesso = acesso;
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> consultar(UUID casaId, String email, String busca,
                                           CategoriaProduto categoria, SituacaoEstoque situacao) {
        acesso.exigirMembroAtivo(casaId, email);
        String termo = busca == null ? "" : busca.strip().toLowerCase(Locale.ROOT);
        return produtos.findByCasaIdOrderByCategoriaAscNomeAsc(casaId).stream()
                .filter(produto -> termo.isEmpty()
                        || produto.getNome().toLowerCase(Locale.ROOT).contains(termo)
                        || produto.getCategoria().name().toLowerCase(Locale.ROOT).contains(termo))
                .filter(produto -> categoria == null || produto.getCategoria() == categoria)
                .filter(produto -> situacao == null || produto.getSituacao() == situacao)
                .map(ProdutoResponse::de)
                .toList();
    }

    @Transactional
    public ProdutoResponse cadastrar(UUID casaId, String email, CadastrarProdutoRequest dados) {
        var membro = acesso.exigirMembroAtivo(casaId, email);
        var produto = new ProdutoEstoque(membro.getCasa(), dados.nome().strip(), dados.categoria(),
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
        produto.atualizar(dados.nome().strip(), dados.categoria(), dados.unidade().strip(),
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
        produtos.delete(produto);
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
