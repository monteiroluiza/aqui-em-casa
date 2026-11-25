package io.github.monteiroluiza.aquiemcasa.d_adapter.web;

import java.security.Principal;
import java.util.UUID;
import io.github.monteiroluiza.aquiemcasa.b_application.compras.GerarListaService;
import io.github.monteiroluiza.aquiemcasa.b_application.compras.PreviaListaResponse;
import io.github.monteiroluiza.aquiemcasa.b_application.compras.ConfirmarSugestoesRequest;
import io.github.monteiroluiza.aquiemcasa.b_application.compras.ListaComprasResponse;
import io.github.monteiroluiza.aquiemcasa.b_application.compras.ListaComprasService;
import io.github.monteiroluiza.aquiemcasa.b_application.compras.AtualizarQuantidadeItemRequest;
import io.github.monteiroluiza.aquiemcasa.b_application.compras.AtualizarSituacaoItemRequest;
import io.github.monteiroluiza.aquiemcasa.b_application.compras.IncluirItemManualRequest;
import io.github.monteiroluiza.aquiemcasa.b_application.compras.ConclusaoCompraService;
import io.github.monteiroluiza.aquiemcasa.b_application.compras.PreviaConclusaoResponse;
import io.github.monteiroluiza.aquiemcasa.b_application.compras.ConcluirCompraRequest;
import io.github.monteiroluiza.aquiemcasa.b_application.compras.ConclusaoCompraResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/casas/{casaId}/lista-compras")
@SecurityRequirement(name = "bearer")
public class ListaComprasController {
    private final GerarListaService gerarLista;
    private final ListaComprasService listaCompras;
    private final ConclusaoCompraService conclusaoCompra;

    public ListaComprasController(GerarListaService gerarLista, ListaComprasService listaCompras,
                                  ConclusaoCompraService conclusaoCompra) {
        this.gerarLista = gerarLista;
        this.listaCompras = listaCompras;
        this.conclusaoCompra = conclusaoCompra;
    }

    @GetMapping
    @Operation(summary = "Consultar a lista de compras compartilhada")
    public ListaComprasResponse consultar(@PathVariable UUID casaId, Principal morador) {
        return listaCompras.consultar(casaId, morador.getName());
    }

    @GetMapping("/previa")
    @Operation(summary = "Gerar a prévia de sugestões sem alterar a lista")
    public PreviaListaResponse gerarPrevia(@PathVariable UUID casaId, Principal morador) {
        return gerarLista.gerarPrevia(casaId, morador.getName());
    }

    @GetMapping("/conclusao/previa")
    @Operation(summary = "Conferir saldos previstos antes de concluir a compra")
    public PreviaConclusaoResponse gerarPreviaConclusao(@PathVariable UUID casaId, Principal morador) {
        return conclusaoCompra.gerarPrevia(casaId, morador.getName());
    }

    @PostMapping("/conclusao")
    @Operation(summary = "Concluir os itens que estão no carrinho")
    public ConclusaoCompraResponse concluir(@PathVariable UUID casaId, Principal morador,
                                            @Valid @RequestBody ConcluirCompraRequest dados) {
        return conclusaoCompra.concluir(casaId, morador.getName(), dados);
    }

    @PostMapping("/sugestoes")
    @Operation(summary = "Confirmar as sugestões selecionadas na prévia")
    public ListaComprasResponse confirmar(@PathVariable UUID casaId, Principal morador,
                                          @Valid @RequestBody ConfirmarSugestoesRequest dados) {
        return gerarLista.confirmar(casaId, morador.getName(), dados);
    }

    @PostMapping("/itens")
    @Operation(summary = "Incluir manualmente um produto existente ou um item avulso")
    public ListaComprasResponse incluirManualmente(@PathVariable UUID casaId, Principal morador,
                                                   @Valid @RequestBody IncluirItemManualRequest dados) {
        return listaCompras.incluirManualmente(casaId, morador.getName(), dados);
    }

    @PutMapping("/itens/{itemId}/quantidade")
    @Operation(summary = "Alterar a quantidade total de uma linha")
    public ListaComprasResponse atualizarQuantidade(@PathVariable UUID casaId, @PathVariable UUID itemId,
                                                    Principal morador,
                                                    @Valid @RequestBody AtualizarQuantidadeItemRequest dados) {
        return listaCompras.atualizarQuantidade(casaId, itemId, morador.getName(), dados);
    }

    @PatchMapping("/itens/{itemId}/situacao")
    @Operation(summary = "Marcar ou retirar uma linha do carrinho")
    public ListaComprasResponse atualizarSituacao(@PathVariable UUID casaId, @PathVariable UUID itemId,
                                                  Principal morador,
                                                  @Valid @RequestBody AtualizarSituacaoItemRequest dados) {
        return listaCompras.atualizarSituacao(casaId, itemId, morador.getName(), dados);
    }

    @DeleteMapping("/itens/{itemId}")
    @Operation(summary = "Remover uma linha da lista")
    public ListaComprasResponse remover(@PathVariable UUID casaId, @PathVariable UUID itemId,
                                        @RequestParam long versao, Principal morador) {
        return listaCompras.remover(casaId, itemId, morador.getName(), versao);
    }
}
