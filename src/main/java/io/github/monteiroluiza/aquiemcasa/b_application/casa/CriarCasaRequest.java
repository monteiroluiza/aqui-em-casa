package io.github.monteiroluiza.aquiemcasa.b_application.casa;

import jakarta.validation.constraints.NotBlank;

public record CriarCasaRequest(
        @NotBlank(message = "Informe o nome da casa.") String nomeCasa) {}
