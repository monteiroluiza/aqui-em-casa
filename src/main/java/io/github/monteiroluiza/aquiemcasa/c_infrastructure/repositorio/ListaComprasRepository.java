package io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio;

import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import io.github.monteiroluiza.aquiemcasa.a_domain.ListaCompras;

public interface ListaComprasRepository extends JpaRepository<ListaCompras, UUID> {
    Optional<ListaCompras> findByCasaId(UUID casaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from ListaCompras l where l.casa.id = :casaId")
    Optional<ListaCompras> buscarPorCasaParaAlteracao(@Param("casaId") UUID casaId);
}
