package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.util.List;
import java.util.UUID;

public record PreviaListaResponse(UUID listaId, long versao, List<SugestaoItemResponse> sugestoes) {}
