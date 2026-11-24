package io.github.monteiroluiza.aquiemcasa.b_application.estoque;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.monteiroluiza.aquiemcasa.a_domain.ProdutoEstoque;
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
    public List<ProdutoResponse> consultar(UUID casaId, String email) {
        acesso.exigirMembroAtivo(casaId, email);
        return produtos.findByCasaIdOrderByCategoriaAscNomeAsc(casaId).stream().map(ProdutoResponse::de).toList();
    }

    @Transactional
    public ProdutoResponse cadastrar(UUID casaId, String email, CadastrarProdutoRequest dados) {
        var membro = acesso.exigirMembroAtivo(casaId, email);
        var produto = new ProdutoEstoque(membro.getCasa(), dados.nome().strip(), dados.categoria(),
                dados.unidade().strip(), dados.quantidadeAtual(), dados.quantidadeDesejada(), membro);
        return ProdutoResponse.de(produtos.save(produto));
    }
}
