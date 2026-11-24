package io.github.monteiroluiza.aquiemcasa.b_application.cadastro;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import io.github.monteiroluiza.aquiemcasa.a_domain.Casa;
import io.github.monteiroluiza.aquiemcasa.a_domain.ListaCompras;
import io.github.monteiroluiza.aquiemcasa.a_domain.MembroCasa;
import io.github.monteiroluiza.aquiemcasa.a_domain.Papel;
import io.github.monteiroluiza.aquiemcasa.a_domain.Usuario;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.CasaRepository;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.ListaComprasRepository;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.MembroCasaRepository;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.UsuarioRepository;

@Service
public class CadastroService {
    private final UsuarioRepository usuarios;
    private final CasaRepository casas;
    private final MembroCasaRepository membros;
    private final ListaComprasRepository listas;
    private final PasswordEncoder encoder;

    public CadastroService(UsuarioRepository usuarios, CasaRepository casas, MembroCasaRepository membros,
                           ListaComprasRepository listas, PasswordEncoder encoder) {
        this.encoder = encoder;
        this.usuarios = usuarios;
        this.casas = casas;
        this.membros = membros;
        this.listas = listas;
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
        Casa casa = casas.save(new Casa(dados.nomeCasa().strip()));
        Papel papel = Papel.ADMINISTRADOR;
        MembroCasa membro = membros.save(new MembroCasa(casa, usuario, papel));
        ListaCompras lista = listas.save(new ListaCompras(casa));
        return new NovaCasaResponse(usuario.getId(), casa.getId(), membro.getId(), lista.getId(), papel);
    }
}
