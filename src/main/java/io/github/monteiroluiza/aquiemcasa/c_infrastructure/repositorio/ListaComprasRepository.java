package io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import io.github.monteiroluiza.aquiemcasa.a_domain.ListaCompras;

public interface ListaComprasRepository extends JpaRepository<ListaCompras, UUID> {
}
