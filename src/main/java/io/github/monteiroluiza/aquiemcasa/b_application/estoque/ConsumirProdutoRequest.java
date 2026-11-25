package io.github.monteiroluiza.aquiemcasa.b_application.estoque;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
public record ConsumirProdutoRequest(@NotNull @DecimalMin(value="0", inclusive=false) @Digits(integer=9, fraction=3) BigDecimal quantidade) {}
