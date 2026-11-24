package io.github.monteiroluiza.aquiemcasa.c_infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import java.util.Locale;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;



import org.springframework.security.config.http.SessionCreationPolicy;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;

@Configuration
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer")
public class SegurancaConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    UserDetailsService moradores(UsuarioRepository usuarios) {
        return email -> usuarios.findByEmail(email.strip().toLowerCase(Locale.ROOT))
                .map(usuario -> User.withUsername(usuario.getEmail()).password(usuario.getSenhaHash())
                        .authorities("USUARIO").build())
                .orElseThrow(() -> new UsernameNotFoundException("Conta desconhecida."));
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService moradores, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(moradores);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(config -> config.disable())
                .logout(config -> config.disable())
                .oauth2ResourceServer(config -> config.opaqueToken(Customizer.withDefaults()))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/cadastros/nova-casa").permitAll()
                        .anyRequest().authenticated())
                .build();
    }
}
