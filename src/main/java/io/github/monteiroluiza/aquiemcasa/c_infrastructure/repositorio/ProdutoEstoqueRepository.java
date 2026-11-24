package io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import io.github.monteiroluiza.aquiemcasa.a_domain.ProdutoEstoque;

public interface ProdutoEstoqueRepository extends JpaRepository<ProdutoEstoque, UUID> {
    @EntityGraph(attributePaths = {"atualizadoPor", "atualizadoPor.usuario"})
    List<ProdutoEstoque> findByCasaIdOrderByCategoriaAscNomeAsc(UUID casaId);
}
