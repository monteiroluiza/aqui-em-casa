package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record SugestaoSelecionadaRequest(
        @NotNull UUID produtoId,
        @NotNull @DecimalMin(value = "0.000", inclusive = false)
        @Digits(integer = 9, fraction = 3) BigDecimal quantidade) {}
