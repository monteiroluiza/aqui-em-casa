package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ConcluirCompraRequest(
        @NotNull UUID confirmacaoId,
        @PositiveOrZero long versao,
        @NotEmpty List<@Valid CompraConferidaRequest> compras) {}
