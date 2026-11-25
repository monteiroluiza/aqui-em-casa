package io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import io.github.monteiroluiza.aquiemcasa.a_domain.ProdutoEstoque;

public interface ProdutoEstoqueRepository extends JpaRepository<ProdutoEstoque, UUID> {
    @EntityGraph(attributePaths = {"atualizadoPor", "atualizadoPor.usuario"})
    List<ProdutoEstoque> findByCasaIdOrderByCategoriaAscNomeAsc(UUID casaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"atualizadoPor", "atualizadoPor.usuario"})
    @Query("select p from ProdutoEstoque p where p.id = :produtoId and p.casa.id = :casaId")
    Optional<ProdutoEstoque> buscarParaAlteracao(@Param("casaId") UUID casaId,
                                                @Param("produtoId") UUID produtoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProdutoEstoque p where p.casa.id = :casaId and p.id in :produtoIds")
    List<ProdutoEstoque> buscarTodosParaLista(@Param("casaId") UUID casaId,
                                             @Param("produtoIds") java.util.Collection<UUID> produtoIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from ProdutoEstoque p
            where p.casa.id = :casaId
              and lower(trim(p.nome)) = lower(trim(:nome))
              and lower(trim(p.unidade)) = lower(trim(:unidade))
            """)
    List<ProdutoEstoque> buscarCompativeis(@Param("casaId") UUID casaId,
                                          @Param("nome") String nome,
                                          @Param("unidade") String unidade);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProdutoEstoque p where p.casa.id = :casaId order by p.id")
    List<ProdutoEstoque> buscarTodosDaCasaParaAlteracao(@Param("casaId") UUID casaId);

    @Query(value = "select id from item_lista where produto_id = :produtoId limit 1", nativeQuery = true)
    Optional<UUID> buscarItemVinculado(@Param("produtoId") UUID produtoId);
}
