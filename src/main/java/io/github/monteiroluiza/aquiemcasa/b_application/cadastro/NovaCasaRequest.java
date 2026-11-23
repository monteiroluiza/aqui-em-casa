package io.github.monteiroluiza.aquiemcasa.b_application.cadastro;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NovaCasaRequest(
        @NotBlank(message = "Informe o nome.") String nome,
        @NotBlank(message = "Informe o e-mail.") @Email(message = "Informe um e-mail válido.") String email,
        @NotBlank(message = "Informe a senha.") String senha,
        @NotBlank(message = "Informe o nome da casa.") String nomeCasa) {

    public NovaCasaRequest {
        if (email != null) {
            email = email.strip().toLowerCase(Locale.ROOT);
        }
    }
}
