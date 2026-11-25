package io.github.monteiroluiza.aquiemcasa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import io.github.monteiroluiza.aquiemcasa.a_domain.*;
import io.github.monteiroluiza.aquiemcasa.b_application.cadastro.CadastroService;
import io.github.monteiroluiza.aquiemcasa.b_application.casa.CasaResponse;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.*;

class CriarCasaTests {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final CasaRepository casas = mock(CasaRepository.class);
    private final MembroCasaRepository membros = mock(MembroCasaRepository.class);
    private final ListaComprasRepository listas = mock(ListaComprasRepository.class);
    private final CategoriaRepository categorias = mock(CategoriaRepository.class);
    private final CadastroService service = new CadastroService(usuarios, casas, membros, listas,
            categorias, mock(PasswordEncoder.class), mock(JdbcClient.class, RETURNS_DEEP_STUBS));
    private final String email = "morador@example.com";

    @Test void criaCasaComoAdministradorSemRecriarUsuario() {
        var usuario = mock(Usuario.class);
        var usuarioId = UUID.randomUUID();
        when(usuario.getId()).thenReturn(usuarioId);
        when(usuarios.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(membros.consultarCasasAtivas(email)).thenReturn(List.of());
        when(casas.save(any(Casa.class))).thenAnswer(i -> i.getArgument(0));
        when(membros.save(any(MembroCasa.class))).thenAnswer(i -> i.getArgument(0));
        when(listas.save(any(ListaCompras.class))).thenAnswer(i -> i.getArgument(0));

        var resposta = service.criarParaUsuario(email, " Minha casa ");

        assertEquals(usuarioId, resposta.usuarioId());
        assertEquals(Papel.ADMINISTRADOR, resposta.papel());
        verify(usuarios, never()).saveAndFlush(any());
        verify(casas).save(argThat(c -> c.getNome().equals("Minha casa")));
        verify(listas).save(any(ListaCompras.class));
        verify(categorias).saveAll(argThat(c -> ((List<?>) c).size() == 4));
    }

    @Test void rejeitaUsuarioComCasaAtivaSemCriarRegistros() {
        when(usuarios.findByEmail(email)).thenReturn(Optional.of(mock(Usuario.class)));
        when(membros.consultarCasasAtivas(email)).thenReturn(List.of(
                new CasaResponse(UUID.randomUUID(), "Casa", UUID.randomUUID(), Papel.MORADOR)));
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.criarParaUsuario(email, "Outra casa"));
        assertEquals(409, erro.getStatusCode().value());
        verifyNoInteractions(casas, listas, categorias);
    }
}
