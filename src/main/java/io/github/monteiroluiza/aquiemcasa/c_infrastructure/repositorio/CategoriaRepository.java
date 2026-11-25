package io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import io.github.monteiroluiza.aquiemcasa.a_domain.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {
    List<Categoria> findByCasaIdOrderByNomeAsc(UUID casaId);
    Optional<Categoria> findByIdAndCasaId(UUID id, UUID casaId);

    @Query("""
            select count(c) > 0 from Categoria c
            where c.casa.id = :casaId and c.id <> :categoriaId
              and lower(trim(c.nome)) = lower(trim(:nome))
            """)
    boolean existeOutroNome(@Param("casaId") UUID casaId,
                            @Param("categoriaId") UUID categoriaId,
                            @Param("nome") String nome);
}
