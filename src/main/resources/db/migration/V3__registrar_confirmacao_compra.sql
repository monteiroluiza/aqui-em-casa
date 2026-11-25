CREATE TABLE requisicao_conclusao_compra (
    id uuid PRIMARY KEY,
    casa_id uuid NOT NULL,
    lista_id uuid NOT NULL,
    concluido_por_id uuid NOT NULL,
    payload_hash bytea NOT NULL,
    versao_resultante bigint NOT NULL CHECK (versao_resultante >= 0),
    itens_processados integer NOT NULL CHECK (itens_processados > 0),
    concluida_em timestamptz NOT NULL,
    FOREIGN KEY (casa_id, lista_id) REFERENCES lista_compras(casa_id, id),
    FOREIGN KEY (casa_id, concluido_por_id) REFERENCES membro_casa(casa_id, id)
);
