package io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import io.github.monteiroluiza.aquiemcasa.b_application.casa.CasaResponse;

import org.springframework.data.jpa.repository.JpaRepository;
import io.github.monteiroluiza.aquiemcasa.a_domain.MembroCasa;

public interface MembroCasaRepository extends JpaRepository<MembroCasa, UUID> {
    java.util.Optional<MembroCasa> findByCasaIdAndUsuarioEmailAndRemovidoEmIsNull(UUID casaId, String email);

    @Query("""
            select new io.github.monteiroluiza.aquiemcasa.b_application.casa.CasaResponse(
                m.casa.id, m.casa.nome, m.id, m.papel)
            from MembroCasa m
            where m.usuario.email = :email and m.removidoEm is null
            order by m.casa.nome, m.casa.id
            """)
    List<CasaResponse> consultarCasasAtivas(
            @Param("email") String email);
}
