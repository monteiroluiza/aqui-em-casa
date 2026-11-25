package io.github.monteiroluiza.aquiemcasa.b_application.categoria;

import java.time.Instant;
import java.util.UUID;
import io.github.monteiroluiza.aquiemcasa.a_domain.Categoria;

public record CategoriaResponse(UUID id, String nome, Instant criadaEm,
                                UUID criadaPorId, String criadaPorNome) {
    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getCriadaEm(),
                categoria.getCriadaPor().getId(), categoria.getCriadaPor().getUsuario().getNome());
    }
}
