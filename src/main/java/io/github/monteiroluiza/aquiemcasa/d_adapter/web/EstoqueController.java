package io.github.monteiroluiza.aquiemcasa.d_adapter.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import io.github.monteiroluiza.aquiemcasa.b_application.estoque.CadastrarProdutoRequest;
import io.github.monteiroluiza.aquiemcasa.b_application.estoque.AtualizarProdutoRequest;
import io.github.monteiroluiza.aquiemcasa.b_application.estoque.AjustarQuantidadeRequest;
import io.github.monteiroluiza.aquiemcasa.b_application.estoque.EstoqueService;
import io.github.monteiroluiza.aquiemcasa.b_application.estoque.ProdutoResponse;
import io.github.monteiroluiza.aquiemcasa.a_domain.SituacaoEstoque;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public List<ProdutoResponse> consultar(
            @PathVariable UUID casaId,
            Principal morador,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) SituacaoEstoque situacao) {
        return estoque.consultar(casaId, morador.getName(), busca, categoriaId, situacao);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar um produto no estoque")
    public ProdutoResponse cadastrar(@PathVariable UUID casaId, Principal morador,
                                     @Valid @RequestBody CadastrarProdutoRequest dados) {
        return estoque.cadastrar(casaId, morador.getName(), dados);
    }

    @PutMapping("/{produtoId}")
    @Operation(summary = "Editar um produto do estoque")
    public ProdutoResponse atualizar(@PathVariable UUID casaId, @PathVariable UUID produtoId,
                                     Principal morador, @Valid @RequestBody AtualizarProdutoRequest dados) {
        return estoque.atualizar(casaId, produtoId, morador.getName(), dados);
    }

    @PatchMapping("/{produtoId}/quantidade")
    @Operation(summary = "Somar ou subtrair uma quantidade do saldo atual")
    public ProdutoResponse ajustarQuantidade(@PathVariable UUID casaId, @PathVariable UUID produtoId,
                                             Principal morador, @Valid @RequestBody AjustarQuantidadeRequest dados) {
        return estoque.ajustarQuantidade(casaId, produtoId, morador.getName(), dados);
    }

    @DeleteMapping("/{produtoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir um produto do estoque")
    public void excluir(@PathVariable UUID casaId, @PathVariable UUID produtoId, Principal morador) {
        estoque.excluir(casaId, produtoId, morador.getName());
    }
}
