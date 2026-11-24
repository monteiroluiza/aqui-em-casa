package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

public record ConfirmarSugestoesRequest(
        @PositiveOrZero long versao,
        @NotEmpty List<@Valid SugestaoSelecionadaRequest> itens) {}
