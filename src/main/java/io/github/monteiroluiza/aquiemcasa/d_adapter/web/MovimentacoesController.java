package io.github.monteiroluiza.aquiemcasa.d_adapter.web;
import java.security.Principal;
import java.util.*;
import io.github.monteiroluiza.aquiemcasa.b_application.estoque.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/casas/{casaId}")
@SecurityRequirement(name="bearer")
public class MovimentacoesController {
    private final EstoqueService estoque;
    public MovimentacoesController(EstoqueService estoque) { this.estoque=estoque; }
    @PostMapping("/produtos/{produtoId}/consumos")
    public ProdutoResponse consumir(@PathVariable UUID casaId, @PathVariable UUID produtoId, Principal usuario,
            @Valid @RequestBody ConsumirProdutoRequest dados) {
        return estoque.consumir(casaId, produtoId, usuario.getName(), dados);
    }
    @GetMapping("/alertas")
    public List<ProdutoResponse> alertas(@PathVariable UUID casaId, Principal usuario) {
        return estoque.alertas(casaId, usuario.getName());
    }
    @GetMapping("/movimentacoes")
    public List<Map<String,Object>> movimentacoes(@PathVariable UUID casaId, Principal usuario,
            @RequestParam(required=false) UUID produtoId, @RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="20") int tamanho) {
        return estoque.movimentacoes(casaId, usuario.getName(), produtoId, pagina, tamanho);
    }
}
