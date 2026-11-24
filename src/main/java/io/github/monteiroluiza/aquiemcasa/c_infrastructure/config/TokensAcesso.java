package io.github.monteiroluiza.aquiemcasa.c_infrastructure.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TokensAcesso implements OpaqueTokenIntrospector {
    private static final long VALIDADE_SEGUNDOS = 3600;
    private final SecureRandom random = new SecureRandom();
    private final JdbcClient jdbc;

    public TokensAcesso(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record TokenEmitido(String email, String accessToken, String tokenType, long expiresIn) {}

    @Transactional
    public TokenEmitido emitir(String email) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.sql("DELETE FROM token_acesso WHERE expira_em <= now()").update();
        int inseridos = jdbc.sql("""
                INSERT INTO token_acesso (token_hash, usuario_id, expira_em)
                SELECT ?, id, now() + (? * interval '1 second') FROM usuario WHERE email = ?
                """).params(hash(token), VALIDADE_SEGUNDOS, email).update();
        if (inseridos != 1) {
            throw new IllegalStateException("Conta indisponível para emissão de token.");
        }
        return new TokenEmitido(email, token, "Bearer", VALIDADE_SEGUNDOS);
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        String email = jdbc.sql("""
                SELECT u.email FROM token_acesso t JOIN usuario u ON u.id = t.usuario_id
                WHERE t.token_hash = ? AND t.expira_em > now()
                """).param(hash(token)).query(String.class).optional()
                .orElseThrow(() -> new BadOpaqueTokenException("Token inválido ou expirado."));
        return new DefaultOAuth2AuthenticatedPrincipal(email, Map.of("sub", email),
                List.of(new SimpleGrantedAuthority("USUARIO")));
    }

    public void revogar(String token) {
        jdbc.sql("DELETE FROM token_acesso WHERE token_hash = ?").param(hash(token)).update();
    }

    private static byte[] hash(String token) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException(erro);
        }
    }
}
