package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import io.github.monteiroluiza.aquiemcasa.b_application.casa.AcessoCasaService;

@Service
@Transactional(readOnly = true)
public class HistoricoComprasService {
    private final JdbcClient jdbc;
    private final AcessoCasaService acesso;
    public HistoricoComprasService(JdbcClient jdbc, AcessoCasaService acesso) {
        this.jdbc = jdbc;
        this.acesso = acesso;
    }

    public List<Map<String, Object>> listar(UUID casaId, String email, int pagina, int tamanho) {
        acesso.exigirMembroAtivo(casaId, email);
        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pagina deve ser positiva ou zero e tamanho entre 1 e 100.");
        }
        return jdbc.sql("""
                select c.id, c.concluida_em as "concluidaEm", u.nome as "responsavel",
                       c.itens_processados as "itensProcessados"
                from requisicao_conclusao_compra c
                join membro_casa m on m.id = c.concluido_por_id
                join usuario u on u.id = m.usuario_id
                where c.casa_id = :casa
                order by c.concluida_em desc, c.id
                limit :limite offset :inicio
                """).param("casa", casaId).param("limite", tamanho)
                .param("inicio", (long) pagina * tamanho).query().listOfRows();
    }

    public List<Map<String, Object>> itens(UUID casaId, UUID compraId, String email) {
        acesso.exigirMembroAtivo(casaId, email);
        boolean existe = jdbc.sql("select exists(select 1 from requisicao_conclusao_compra where id = :id and casa_id = :casa)")
                .param("id", compraId).param("casa", casaId).query(Boolean.class).single();
        if (!existe) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra nao encontrada nesta casa.");
        return jdbc.sql("select id, nome, categoria, unidade, quantidade from item_compra_historico where compra_id = :id order by nome, id")
                .param("id", compraId).query().listOfRows();
    }
}
