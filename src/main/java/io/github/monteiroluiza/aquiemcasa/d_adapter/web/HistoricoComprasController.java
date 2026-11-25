package io.github.monteiroluiza.aquiemcasa.d_adapter.web;

import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.github.monteiroluiza.aquiemcasa.b_application.compras.HistoricoComprasService;

@RestController
@RequestMapping("/api/casas/{casaId}/compras")
@SecurityRequirement(name = "bearer")
public class HistoricoComprasController {
    private final HistoricoComprasService historico;
    public HistoricoComprasController(HistoricoComprasService historico) { this.historico = historico; }

    @GetMapping
    public List<Map<String, Object>> listar(@PathVariable UUID casaId, Principal morador,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
        return historico.listar(casaId, morador.getName(), pagina, tamanho);
    }

    @GetMapping("/{compraId}/itens")
    public List<Map<String, Object>> itens(@PathVariable UUID casaId, @PathVariable UUID compraId, Principal morador) {
        return historico.itens(casaId, compraId, morador.getName());
    }
}
