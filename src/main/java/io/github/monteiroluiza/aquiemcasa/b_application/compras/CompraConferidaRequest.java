package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CompraConferidaRequest(
        @NotEmpty List<@NotNull UUID> itemIds,
        UUID produtoId,
        @NotBlank String unidade,
        @NotNull @DecimalMin("0.000") BigDecimal saldoAtual) {}
