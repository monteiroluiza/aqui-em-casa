package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import io.github.monteiroluiza.aquiemcasa.a_domain.CategoriaProduto;

public record CompraPrevistaResponse(
        List<UUID> itemIds,
        UUID produtoId,
        String nome,
        CategoriaProduto categoria,
        String unidade,
        BigDecimal quantidadeComprada,
        BigDecimal saldoAtual,
        BigDecimal saldoPrevisto,
        boolean novoProduto) {}
