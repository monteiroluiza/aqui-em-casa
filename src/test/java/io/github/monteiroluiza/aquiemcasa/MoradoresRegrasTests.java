package io.github.monteiroluiza.aquiemcasa;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import io.github.monteiroluiza.aquiemcasa.a_domain.*;
import io.github.monteiroluiza.aquiemcasa.b_application.casa.AcessoCasaService;
import io.github.monteiroluiza.aquiemcasa.b_application.moradores.MoradoresService;
class MoradoresRegrasTests {
    private final JdbcClient jdbc=mock(JdbcClient.class,RETURNS_DEEP_STUBS);
    private final AcessoCasaService acesso=mock(AcessoCasaService.class);
    private final UUID casa=UUID.randomUUID(), membroId=UUID.randomUUID();
    private MoradoresService preparar(String papel) {
        var membro=mock(MembroCasa.class);
        when(membro.getId()).thenReturn(membroId);
        when(acesso.exigirMembroAtivo(casa,"autor")).thenReturn(membro);
        when(jdbc.sql("select id from casa where id=:id for update").param("id",casa).query(UUID.class).optional()).thenReturn(Optional.of(casa));
        when(jdbc.sql("select papel from membro_casa where id=:id").param("id",membroId).query(String.class).single()).thenReturn(papel);
        return new MoradoresService(jdbc,acesso,mock(PasswordEncoder.class));
    }
    @Test void moradorNaoPodeConvidar() {
        var service=preparar("MORADOR");
        var e=assertThrows(ResponseStatusException.class,() -> service.convidar(casa,"autor","novo@example.com"));
        assertEquals(403,e.getStatusCode().value());
    }
    @Test void ultimoAdministradorNaoPodeSerRemovidoOuRebaixado() {
        var service=preparar("ADMINISTRADOR");
        when(jdbc.sql("select papel from membro_casa where id=:id and casa_id=:casa and removido_em is null")
            .param("id",membroId).param("casa",casa).query(String.class).optional()).thenReturn(Optional.of("ADMINISTRADOR"));
        when(jdbc.sql("select count(*) from membro_casa where casa_id=:casa and papel='ADMINISTRADOR' and removido_em is null")
            .param("casa",casa).query(Long.class).single()).thenReturn(1L);
        assertEquals(409,assertThrows(ResponseStatusException.class,() -> service.alterar(casa,membroId,"autor",null,true)).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,() -> service.alterar(casa,membroId,"autor",Papel.MORADOR,false)).getStatusCode().value());
    }
}
