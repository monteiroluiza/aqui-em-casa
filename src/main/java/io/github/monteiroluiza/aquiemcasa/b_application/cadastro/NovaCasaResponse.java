package io.github.monteiroluiza.aquiemcasa.b_application.cadastro;

import java.util.UUID;
import io.github.monteiroluiza.aquiemcasa.a_domain.Papel;

public record NovaCasaResponse(UUID usuarioId, UUID casaId, UUID membroId, UUID listaId, Papel papel) {}
