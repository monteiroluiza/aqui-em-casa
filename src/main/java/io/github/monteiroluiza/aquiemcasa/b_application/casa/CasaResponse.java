package io.github.monteiroluiza.aquiemcasa.b_application.casa;

import java.util.UUID;
import io.github.monteiroluiza.aquiemcasa.a_domain.Papel;

public record CasaResponse(UUID casaId, String nome, UUID membroId, Papel papel) {}
