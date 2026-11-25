package io.github.monteiroluiza.aquiemcasa.d_adapter.web;

import java.security.Principal;
import java.util.List;
import jakarta.validation.Valid;
import io.github.monteiroluiza.aquiemcasa.b_application.cadastro.CadastroService;
import io.github.monteiroluiza.aquiemcasa.b_application.cadastro.NovaCasaResponse;
import io.github.monteiroluiza.aquiemcasa.b_application.casa.CriarCasaRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import io.github.monteiroluiza.aquiemcasa.b_application.casa.CasaResponse;
import io.github.monteiroluiza.aquiemcasa.b_application.casa.ConsultarCasasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/casas")
@SecurityRequirement(name = "bearer")
public class CasaController {
    private final ConsultarCasasService casas;
    private final CadastroService cadastro;

    public CasaController(ConsultarCasasService casas, CadastroService cadastro) {
        this.casas = casas;
        this.cadastro = cadastro;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar casa para usuario autenticado sem casa ativa")
    public NovaCasaResponse criar(Principal morador, @Valid @RequestBody CriarCasaRequest dados) {
        return cadastro.criarParaUsuario(morador.getName(), dados.nomeCasa());
    }

    @GetMapping
    @Operation(summary = "Consultar minhas casas com vínculo ativo")
    public List<CasaResponse> consultar(Principal morador) {
        return casas.consultar(morador.getName());
    }
}
