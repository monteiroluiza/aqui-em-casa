package io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import io.github.monteiroluiza.aquiemcasa.a_domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    java.util.Optional<Usuario> findByEmail(String email);
}
