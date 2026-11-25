package io.github.monteiroluiza.aquiemcasa.b_application.cadastro;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import io.github.monteiroluiza.aquiemcasa.a_domain.Casa;
import io.github.monteiroluiza.aquiemcasa.a_domain.Categoria;
import io.github.monteiroluiza.aquiemcasa.a_domain.ListaCompras;
import io.github.monteiroluiza.aquiemcasa.a_domain.MembroCasa;
import io.github.monteiroluiza.aquiemcasa.a_domain.Papel;
import io.github.monteiroluiza.aquiemcasa.a_domain.Usuario;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.CasaRepository;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.CategoriaRepository;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.ListaComprasRepository;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.MembroCasaRepository;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.UsuarioRepository;

@Service
public class CadastroService {
    private final UsuarioRepository usuarios;
    private final CasaRepository casas;
    private final MembroCasaRepository membros;
    private final ListaComprasRepository listas;
    private final CategoriaRepository categorias;
    private final PasswordEncoder encoder;
    private final org.springframework.jdbc.core.simple.JdbcClient jdbc;

    public CadastroService(UsuarioRepository usuarios, CasaRepository casas, MembroCasaRepository membros,
                           ListaComprasRepository listas, CategoriaRepository categorias,
                           PasswordEncoder encoder, org.springframework.jdbc.core.simple.JdbcClient jdbc) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.usuarios = usuarios;
        this.casas = casas;
        this.membros = membros;
        this.listas = listas;
        this.categorias = categorias;
    }

    @Transactional
    public NovaCasaResponse cadastrar(NovaCasaRequest dados) {
        Usuario usuario = new Usuario(dados.nome().strip(), dados.email(), encoder.encode(dados.senha()));
        try {
            usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException erro) {
            if (erro.getCause() instanceof ConstraintViolationException restricao
                    && "usuario_email_key".equals(restricao.getConstraintName())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.", erro);
            }
            throw erro;
        }
        return criarCasa(usuario, dados.nomeCasa());
    }

    @Transactional
    public NovaCasaResponse criarParaUsuario(String email, String nomeCasa) {
        // Mesmo bloqueio do aceite de convites: serializa novos vinculos do usuario.
        jdbc.sql("select pg_advisory_xact_lock(hashtextextended(:email, 0)) is null")
                .param("email", email).query(Boolean.class).single();
        Usuario usuario = usuarios.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario nao encontrado."));
        if (!membros.consultarCasasAtivas(email).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voce ja possui uma casa ativa.");
        }
        return criarCasa(usuario, nomeCasa);
    }

    private NovaCasaResponse criarCasa(Usuario usuario, String nomeCasa) {
        Casa casa = casas.save(new Casa(nomeCasa.strip()));
        Papel papel = Papel.ADMINISTRADOR;
        MembroCasa membro = membros.save(new MembroCasa(casa, usuario, papel));
        ListaCompras lista = listas.save(new ListaCompras(casa));
        categorias.saveAll(java.util.List.of(
                new Categoria(casa, "Despensa", membro),
                new Categoria(casa, "Geladeira", membro),
                new Categoria(casa, "Limpeza", membro),
                new Categoria(casa, "Higiene", membro)));
        return new NovaCasaResponse(usuario.getId(), casa.getId(), membro.getId(), lista.getId(), papel);
    }
}
