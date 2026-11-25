package io.github.monteiroluiza.aquiemcasa.d_adapter.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.github.monteiroluiza.aquiemcasa.b_application.categoria.CategoriaRequest;
import io.github.monteiroluiza.aquiemcasa.b_application.categoria.CategoriaResponse;
import io.github.monteiroluiza.aquiemcasa.b_application.categoria.CategoriaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/casas/{casaId}/categorias")
@SecurityRequirement(name = "bearer")
public class CategoriaController {
    private final CategoriaService categorias;

    public CategoriaController(CategoriaService categorias) { this.categorias = categorias; }

    @GetMapping
    public List<CategoriaResponse> listar(@PathVariable UUID casaId, Principal morador) {
        return categorias.listar(casaId, morador.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse criar(@PathVariable UUID casaId, Principal morador,
                                    @Valid @RequestBody CategoriaRequest dados) {
        return categorias.criar(casaId, morador.getName(), dados);
    }

    @PutMapping("/{categoriaId}")
    public CategoriaResponse renomear(@PathVariable UUID casaId, @PathVariable UUID categoriaId,
                                      Principal morador, @Valid @RequestBody CategoriaRequest dados) {
        return categorias.renomear(casaId, categoriaId, morador.getName(), dados);
    }

    @DeleteMapping("/{categoriaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID casaId, @PathVariable UUID categoriaId, Principal morador) {
        categorias.excluir(casaId, categoriaId, morador.getName());
    }
}
