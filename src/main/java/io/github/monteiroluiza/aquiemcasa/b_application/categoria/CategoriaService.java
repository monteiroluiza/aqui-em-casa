package io.github.monteiroluiza.aquiemcasa.b_application.categoria;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import io.github.monteiroluiza.aquiemcasa.a_domain.Categoria;
import io.github.monteiroluiza.aquiemcasa.b_application.casa.AcessoCasaService;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.CategoriaRepository;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.ItemListaRepository;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.ProdutoEstoqueRepository;

@Service
public class CategoriaService {
    private final CategoriaRepository categorias;
    private final ProdutoEstoqueRepository produtos;
    private final ItemListaRepository itens;
    private final AcessoCasaService acesso;

    public CategoriaService(CategoriaRepository categorias, ProdutoEstoqueRepository produtos,
                            ItemListaRepository itens, AcessoCasaService acesso) {
        this.categorias = categorias;
        this.produtos = produtos;
        this.itens = itens;
        this.acesso = acesso;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(UUID casaId, String email) {
        acesso.exigirMembroAtivo(casaId, email);
        return categorias.findByCasaIdOrderByNomeAsc(casaId).stream().map(CategoriaResponse::de).toList();
    }

    @Transactional
    public CategoriaResponse criar(UUID casaId, String email, CategoriaRequest dados) {
        var membro = acesso.exigirMembroAtivo(casaId, email);
        String nome = dados.nome().strip();
        validarNomeUnico(casaId, null, nome);
        return CategoriaResponse.de(categorias.save(new Categoria(membro.getCasa(), nome, membro)));
    }

    @Transactional
    public CategoriaResponse renomear(UUID casaId, UUID categoriaId, String email, CategoriaRequest dados) {
        acesso.exigirMembroAtivo(casaId, email);
        var categoria = buscar(casaId, categoriaId);
        String nome = dados.nome().strip();
        validarNomeUnico(casaId, categoriaId, nome);
        categoria.renomear(nome);
        return CategoriaResponse.de(categoria);
    }

    @Transactional
    public void excluir(UUID casaId, UUID categoriaId, String email) {
        acesso.exigirMembroAtivo(casaId, email);
        var categoria = buscar(casaId, categoriaId);
        if (produtos.existsByCategoriaId(categoriaId) || itens.existsByCategoriaId(categoriaId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A categoria possui produtos ou itens vinculados e nao pode ser excluida.");
        }
        categorias.delete(categoria);
    }

    public Categoria buscarDaCasa(UUID casaId, UUID categoriaId) {
        return buscar(casaId, categoriaId);
    }

    private Categoria buscar(UUID casaId, UUID categoriaId) {
        return categorias.findByIdAndCasaId(categoriaId, casaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Categoria nao encontrada nesta casa."));
    }

    private void validarNomeUnico(UUID casaId, UUID categoriaId, String nome) {
        UUID idIgnorado = categoriaId == null ? new UUID(0, 0) : categoriaId;
        if (categorias.existeOutroNome(casaId, idIgnorado, nome)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ja existe uma categoria com este nome na casa.");
        }
    }
}
