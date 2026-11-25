package io.github.monteiroluiza.aquiemcasa.b_application.categoria;

import java.util.UUID;
import io.github.monteiroluiza.aquiemcasa.a_domain.Categoria;

public record CategoriaResumoResponse(UUID id, String nome) {
    public static CategoriaResumoResponse de(Categoria categoria) {
        return new CategoriaResumoResponse(categoria.getId(), categoria.getNome());
    }
}
