package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.math.BigDecimal;
import java.util.UUID;
import io.github.monteiroluiza.aquiemcasa.a_domain.CategoriaProduto;

public record SugestaoItemResponse(
        UUID produtoId,
        String nome,
        CategoriaProduto categoria,
        String unidade,
        BigDecimal quantidadeFaltante,
        BigDecimal quantidadeNaLista,
        BigDecimal quantidadeSugerida,
        AcaoSugestao acao) {}
