package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import io.github.monteiroluiza.aquiemcasa.b_application.categoria.CategoriaResumoResponse;

public record CompraPrevistaResponse(
        List<UUID> itemIds,
        UUID produtoId,
        String nome,
        CategoriaResumoResponse categoria,
        String unidade,
        BigDecimal quantidadeComprada,
        BigDecimal saldoAtual,
        BigDecimal saldoPrevisto,
        boolean novoProduto) {}
