package io.github.monteiroluiza.aquiemcasa.c_infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.http.HttpMethod;

@Configuration
public class SegurancaConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var cadastro = PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/cadastros/nova-casa");
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers(cadastro))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(cadastro).permitAll()
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults())
                .build();
    }
}
