package io.github.monteiroluiza.aquiemcasa.d_adapter.web;

import java.security.Principal;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.config.TokensAcesso;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {
    private final AuthenticationManager manager;
    private final TokensAcesso tokens;

    public AutenticacaoController(AuthenticationManager manager, TokensAcesso tokens) {
        this.manager = manager;
        this.tokens = tokens;
    }

    public record Credenciais(@NotBlank String email, @NotBlank String senha) {}
    public record Sessao(String email) {}

    @PostMapping("/login")
    public TokensAcesso.TokenEmitido login(@Valid @RequestBody Credenciais dados) {
        Authentication morador;
        try {
            morador = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(dados.email(), dados.senha()));
        } catch (BadCredentialsException erro) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        }
        return tokens.emitir(morador.getName());
    }

    @SecurityRequirement(name = "bearer")
    @GetMapping("/sessao")
    public Sessao sessao(Principal morador) {
        return new Sessao(morador.getName());
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(BearerTokenAuthentication morador) {
        tokens.revogar(morador.getToken().getTokenValue());
    }
}
