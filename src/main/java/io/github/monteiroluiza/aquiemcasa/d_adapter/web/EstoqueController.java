package io.github.monteiroluiza.aquiemcasa.d_adapter.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import io.github.monteiroluiza.aquiemcasa.b_application.estoque.CadastrarProdutoRequest;
import io.github.monteiroluiza.aquiemcasa.b_application.estoque.EstoqueService;
import io.github.monteiroluiza.aquiemcasa.b_application.estoque.ProdutoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/casas/{casaId}/produtos")
@SecurityRequirement(name = "bearer")
public class EstoqueController {
    private final EstoqueService estoque;

    public EstoqueController(EstoqueService estoque) {
        this.estoque = estoque;
    }

    @GetMapping
    @Operation(summary = "Consultar o estoque da casa")
    public List<ProdutoResponse> consultar(@PathVariable UUID casaId, Principal morador) {
        return estoque.consultar(casaId, morador.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar um produto no estoque")
    public ProdutoResponse cadastrar(@PathVariable UUID casaId, Principal morador,
                                     @Valid @RequestBody CadastrarProdutoRequest dados) {
        return estoque.cadastrar(casaId, morador.getName(), dados);
    }
}
