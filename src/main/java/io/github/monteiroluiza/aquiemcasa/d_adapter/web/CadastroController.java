package io.github.monteiroluiza.aquiemcasa.d_adapter.web;

import io.github.monteiroluiza.aquiemcasa.b_application.cadastro.CadastroService;
import io.github.monteiroluiza.aquiemcasa.b_application.cadastro.NovaCasaRequest;
import io.github.monteiroluiza.aquiemcasa.b_application.cadastro.NovaCasaResponse;

import java.util.LinkedHashMap;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cadastros")
public class CadastroController {
    private final CadastroService cadastro;

    public CadastroController(CadastroService cadastro) {
        this.cadastro = cadastro;
    }

    @Operation(summary = "Cadastrar uma conta e criar sua casa", responses = {
            @ApiResponse(responseCode = "201", description = "Conta, casa, administrador e lista criados", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    })
    @PostMapping(path = "/nova-casa", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public NovaCasaResponse novaCasa(@Valid @RequestBody NovaCasaRequest dados) {
        return cadastro.cadastrar(dados);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail dadosInvalidos(MethodArgumentNotValidException erro) {
        var campos = new LinkedHashMap<String, String>();
        erro.getBindingResult().getFieldErrors().forEach(campo ->
                campos.putIfAbsent(campo.getField(), campo.getDefaultMessage()));
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Confira os campos do cadastro.");
        problema.setProperty("campos", campos);
        return problema;
    }
}
