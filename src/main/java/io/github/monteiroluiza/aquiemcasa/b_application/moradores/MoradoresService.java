package io.github.monteiroluiza.aquiemcasa.b_application.moradores;

import java.util.*;
import java.time.Instant;
import java.security.*;
import java.nio.charset.StandardCharsets;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;
import io.github.monteiroluiza.aquiemcasa.b_application.casa.AcessoCasaService;
import io.github.monteiroluiza.aquiemcasa.a_domain.Papel;

@Service
@Transactional
public class MoradoresService {
    private final JdbcClient jdbc;
    private final AcessoCasaService acesso;
    private final PasswordEncoder senhas;
    public MoradoresService(JdbcClient jdbc, AcessoCasaService acesso, PasswordEncoder senhas) {
        this.jdbc=jdbc; this.acesso=acesso; this.senhas=senhas;
    }
    private void bloquearCasa(UUID casa) {
        jdbc.sql("select id from casa where id=:id for update").param("id", casa).query(UUID.class).optional()
            .orElseThrow(() -> erro(HttpStatus.NOT_FOUND,"Casa nao encontrada."));
    }
    private UUID administrador(UUID casa, String email) {
        bloquearCasa(casa);
        var membro=acesso.exigirMembroAtivo(casa,email);
        String papel=jdbc.sql("select papel from membro_casa where id=:id").param("id",membro.getId()).query(String.class).single();
        if (!papel.equals("ADMINISTRADOR")) throw erro(HttpStatus.FORBIDDEN,"Apenas administradores podem gerenciar moradores e convites.");
        return membro.getId();
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> listar(UUID casa,String email) {
        acesso.exigirMembroAtivo(casa,email);
        return jdbc.sql("select m.id, u.nome, u.email, m.papel, m.entrou_em as \"entrouEm\" from membro_casa m join usuario u on u.id=m.usuario_id where m.casa_id=:casa and m.removido_em is null order by u.nome,m.id")
            .param("casa",casa).query().listOfRows();
    }
    public Map<String,Object> convidar(UUID casa,String autor,String destino) {
        UUID membro=administrador(casa,autor);
        destino=destino.strip().toLowerCase(Locale.ROOT);
        if (jdbc.sql("select exists(select 1 from membro_casa m join usuario u on u.id=m.usuario_id where m.casa_id=:casa and u.email=:email and m.removido_em is null)")
            .param("casa",casa).param("email",destino).query(Boolean.class).single())
            throw erro(HttpStatus.CONFLICT,"Este usuario ja participa da casa.");
        jdbc.sql("update convite set estado='CANCELADO' where casa_id=:casa and email_destino=:email and estado='PENDENTE'")
            .param("casa",casa).param("email",destino).update();
        byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes);
        String codigo=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        UUID id=UUID.randomUUID(); Instant expira=Instant.now().plusSeconds(7*86400);
        jdbc.sql("insert into convite(id,casa_id,emitido_por_id,email_destino,codigo_hash,expira_em) values(:id,:casa,:autor,:email,:hash,:expira)")
            .param("id",id).param("casa",casa).param("autor",membro).param("email",destino).param("hash",hash(codigo)).param("expira", expira.atOffset(java.time.ZoneOffset.UTC)).update();
        return Map.of("id",id,"email",destino,"codigo",codigo,"expiraEm",expira);
    }
    public List<Map<String,Object>> convites(UUID casa,String email) {
        administrador(casa,email);
        return jdbc.sql("select id,email_destino as email,estado,expira_em as \"expiraEm\",(estado='PENDENTE' and expira_em<=now()) as expirado from convite where casa_id=:casa order by criado_em desc,id")
            .param("casa",casa).query().listOfRows();
    }
    public void cancelar(UUID casa,UUID convite,String email) {
        administrador(casa,email);
        int n=jdbc.sql("update convite set estado='CANCELADO' where id=:id and casa_id=:casa and estado='PENDENTE'")
            .param("id",convite).param("casa",casa).update();
        if(n==0) throw erro(HttpStatus.NOT_FOUND,"Convite pendente nao encontrado.");
    }
    public Map<String,Object> aceitar(String codigo,String email,String senha,String nome) {
        String destino=email.strip().toLowerCase(Locale.ROOT);
        String digest=hash(codigo);
        UUID casa=jdbc.sql("select casa_id from convite where codigo_hash=:hash and email_destino=:email")
            .param("hash",digest).param("email",destino).query(UUID.class).optional()
            .orElseThrow(() -> erro(HttpStatus.NOT_FOUND,"Convite nao encontrado."));
        bloquearCasa(casa);
        UUID convite=jdbc.sql("select id from convite where codigo_hash=:hash and estado='PENDENTE' and expira_em>now() for update")
            .param("hash",digest).query(UUID.class).optional()
            .orElseThrow(() -> erro(HttpStatus.CONFLICT,"Convite expirado ou ja utilizado."));
        // Serializa criacao da conta, inclusive convites de casas distintas.
        jdbc.sql("select pg_advisory_xact_lock(hashtextextended(:email, 0)) is null").param("email",destino).query(Boolean.class).single();
        var usuario=jdbc.sql("select id,senha_hash from usuario where email=:email").param("email",destino).query((rs,row) -> Map.<String,Object>of("id",rs.getObject("id",UUID.class),"senha_hash",rs.getString("senha_hash"))).optional();
        UUID usuarioId;
        if(usuario.isPresent()) {
            if(!senhas.matches(senha,(String)usuario.get().get("senha_hash"))) throw erro(HttpStatus.UNAUTHORIZED,"E-mail ou senha invalidos.");
            usuarioId=(UUID)usuario.get().get("id");
        } else {
            if(nome==null || nome.isBlank()) throw erro(HttpStatus.BAD_REQUEST,"Informe o nome para criar sua conta.");
            usuarioId=UUID.randomUUID();
            jdbc.sql("insert into usuario(id,nome,email,senha_hash) values(:id,:nome,:email,:senha)")
                .param("id",usuarioId).param("nome",nome.strip()).param("email",destino).param("senha",senhas.encode(senha)).update();
        }
        jdbc.sql("""
            insert into membro_casa(casa_id,usuario_id,papel) values(:casa,:usuario,'MORADOR')
            on conflict(casa_id,usuario_id) do update set removido_em=null, entrou_em=now(), papel='MORADOR'
            where membro_casa.removido_em is not null
            """).param("casa",casa).param("usuario",usuarioId).update();
        jdbc.sql("update convite set estado='ACEITO' where id=:id").param("id",convite).update();
        return Map.of("casaId",casa,"email",destino);
    }
    public void alterar(UUID casa,UUID membro,String email,Papel papel,boolean remover) {
        administrador(casa,email);
        String atual=jdbc.sql("select papel from membro_casa where id=:id and casa_id=:casa and removido_em is null")
            .param("id",membro).param("casa",casa).query(String.class).optional()
            .orElseThrow(() -> erro(HttpStatus.NOT_FOUND,"Morador nao encontrado."));
        if(atual.equals("ADMINISTRADOR") && (remover || papel!=Papel.ADMINISTRADOR)) {
            long total=jdbc.sql("select count(*) from membro_casa where casa_id=:casa and papel='ADMINISTRADOR' and removido_em is null")
                .param("casa",casa).query(Long.class).single();
            if(total<=1) throw erro(HttpStatus.CONFLICT,"A casa precisa manter ao menos um administrador.");
        }
        if(remover) jdbc.sql("update membro_casa set removido_em=now() where id=:id").param("id",membro).update();
        else jdbc.sql("update membro_casa set papel=:papel where id=:id").param("papel",papel.name()).param("id",membro).update();
    }
    private String hash(String codigo) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(codigo.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private ResponseStatusException erro(HttpStatus status,String mensagem) { return new ResponseStatusException(status,mensagem); }
}
