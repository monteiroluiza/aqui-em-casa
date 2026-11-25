package io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import io.github.monteiroluiza.aquiemcasa.a_domain.ItemLista;

public interface ItemListaRepository extends JpaRepository<ItemLista, UUID> {
    boolean existsByCategoriaId(UUID categoriaId);
    @EntityGraph(attributePaths = {"produto", "produto.categoria", "categoria"})
    List<ItemLista> findByListaIdAndProdutoIsNotNull(UUID listaId);

    @EntityGraph(attributePaths = {"produto", "produto.categoria", "categoria"})
    List<ItemLista> findByListaId(UUID listaId);

    @EntityGraph(attributePaths = {"produto", "produto.categoria", "categoria"})
    Optional<ItemLista> findByIdAndListaId(UUID id, UUID listaId);
}
