package io.github.monteiroluiza.aquiemcasa.b_application.estoque;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import io.github.monteiroluiza.aquiemcasa.a_domain.CategoriaProduto;
import io.github.monteiroluiza.aquiemcasa.a_domain.ProdutoEstoque;
import io.github.monteiroluiza.aquiemcasa.a_domain.SituacaoEstoque;

public record ProdutoResponse(
        UUID id,
        String nome,
        CategoriaProduto categoria,
        String unidade,
        BigDecimal quantidadeAtual,
        BigDecimal quantidadeDesejada,
        SituacaoEstoque situacao,
        BigDecimal quantidadeFaltante,
        Instant atualizadoEm,
        UUID atualizadoPorId,
        String atualizadoPorNome) {

    public static ProdutoResponse de(ProdutoEstoque produto) {
        return new ProdutoResponse(produto.getId(), produto.getNome(), produto.getCategoria(), produto.getUnidade(),
                produto.getQuantidadeAtual(), produto.getQuantidadeDesejada(), produto.getSituacao(),
                produto.getQuantidadeFaltante(), produto.getAtualizadoEm(), produto.getAtualizadoPor().getId(),
                produto.getAtualizadoPor().getUsuario().getNome());
    }
}
