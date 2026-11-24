package io.github.monteiroluiza.aquiemcasa.d_adapter.web;

import java.security.Principal;
import java.util.List;
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

    public CasaController(ConsultarCasasService casas) {
        this.casas = casas;
    }

    @GetMapping
    @Operation(summary = "Consultar minhas casas com vínculo ativo")
    public List<CasaResponse> consultar(Principal morador) {
        return casas.consultar(morador.getName());
    }
}
