package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.time.Instant;
import java.util.UUID;

public record ConclusaoCompraResponse(
        UUID confirmacaoId,
        Instant concluidaEm,
        long versaoLista,
        int itensProcessados,
        boolean reprocessada) {}
