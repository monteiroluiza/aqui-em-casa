package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.math.BigDecimal;
import java.util.UUID;
import io.github.monteiroluiza.aquiemcasa.a_domain.CategoriaProduto;
import io.github.monteiroluiza.aquiemcasa.a_domain.ItemLista;
import io.github.monteiroluiza.aquiemcasa.a_domain.SituacaoItemLista;

public record ItemListaResponse(
        UUID id,
        UUID produtoId,
        String nome,
        CategoriaProduto categoria,
        String unidade,
        BigDecimal quantidade,
        SituacaoItemLista situacao) {

    public static ItemListaResponse de(ItemLista item) {
        var produto = item.getProduto();
        return new ItemListaResponse(item.getId(), produto == null ? null : produto.getId(),
                item.getNomeExibicao(), item.getCategoriaExibicao(), item.getUnidadeExibicao(),
                item.getQuantidade(), item.getSituacao());
    }
}
