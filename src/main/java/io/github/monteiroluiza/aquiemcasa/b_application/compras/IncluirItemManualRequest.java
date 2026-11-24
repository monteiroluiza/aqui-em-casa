package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.math.BigDecimal;
import java.util.UUID;
import io.github.monteiroluiza.aquiemcasa.a_domain.CategoriaProduto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record IncluirItemManualRequest(
        @PositiveOrZero long versao,
        UUID produtoId,
        String nome,
        String unidade,
        CategoriaProduto categoria,
        @NotNull @DecimalMin(value = "0.000", inclusive = false)
        @Digits(integer = 9, fraction = 3) BigDecimal quantidade) {}
