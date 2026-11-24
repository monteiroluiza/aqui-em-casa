package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.util.List;
import java.util.UUID;

public record ListaComprasResponse(UUID listaId, long versao, List<ItemListaResponse> itens) {}
