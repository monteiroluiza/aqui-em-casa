package io.github.monteiroluiza.aquiemcasa.b_application.casa;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.MembroCasaRepository;

@Service
public class ConsultarCasasService {
    private final MembroCasaRepository membros;

    public ConsultarCasasService(MembroCasaRepository membros) {
        this.membros = membros;
    }

    @Transactional(readOnly = true)
    public List<CasaResponse> consultar(String email) {
        return membros.consultarCasasAtivas(email);
    }
}
