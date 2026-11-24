package io.github.monteiroluiza.aquiemcasa.b_application.estoque;

import java.math.BigDecimal;
import io.github.monteiroluiza.aquiemcasa.a_domain.CategoriaProduto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AtualizarProdutoRequest(
        @NotBlank String nome,
        @NotNull CategoriaProduto categoria,
        @NotBlank String unidade,
        @NotNull @DecimalMin("0.000") @Digits(integer = 9, fraction = 3) BigDecimal quantidadeAtual,
        @NotNull @DecimalMin("0.000") @Digits(integer = 9, fraction = 3) BigDecimal quantidadeDesejada) {}
