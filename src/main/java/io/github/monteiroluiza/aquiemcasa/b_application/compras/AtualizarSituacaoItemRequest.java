package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import io.github.monteiroluiza.aquiemcasa.a_domain.SituacaoItemLista;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AtualizarSituacaoItemRequest(
        @PositiveOrZero long versao,
        @NotNull SituacaoItemLista situacao) {}
