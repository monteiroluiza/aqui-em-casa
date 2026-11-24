package io.github.monteiroluiza.aquiemcasa.b_application.estoque;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record AjustarQuantidadeRequest(
        @NotNull @Digits(integer = 9, fraction = 3) BigDecimal ajuste) {}
