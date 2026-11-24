package io.github.monteiroluiza.aquiemcasa.b_application.casa;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import io.github.monteiroluiza.aquiemcasa.a_domain.MembroCasa;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.MembroCasaRepository;

@Service
public class AcessoCasaService {
    private final MembroCasaRepository membros;

    public AcessoCasaService(MembroCasaRepository membros) {
        this.membros = membros;
    }

    @Transactional(readOnly = true)
    public MembroCasa exigirMembroAtivo(UUID casaId, String email) {
        return membros.findByCasaIdAndUsuarioEmailAndRemovidoEmIsNull(casaId, email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Casa não encontrada ou acesso removido."));
    }
}
