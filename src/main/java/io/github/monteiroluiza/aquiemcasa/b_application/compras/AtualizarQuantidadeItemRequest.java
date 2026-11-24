package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;

public record AtualizarQuantidadeItemRequest(
        @PositiveOrZero long versao,
        @NotNull @DecimalMin(value = "0.000", inclusive = false)
        @Digits(integer = 9, fraction = 3) BigDecimal quantidade) {}
