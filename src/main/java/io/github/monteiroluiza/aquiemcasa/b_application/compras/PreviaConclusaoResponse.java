package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.util.List;
import java.util.UUID;

public record PreviaConclusaoResponse(
        UUID listaId,
        long versao,
        List<CompraPrevistaResponse> compras,
        List<ItemListaResponse> pendentes) {}
