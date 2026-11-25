CREATE TABLE item_compra_historico (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    compra_id uuid NOT NULL REFERENCES requisicao_conclusao_compra(id),
    nome text NOT NULL,
    categoria text NOT NULL,
    unidade text NOT NULL,
    quantidade numeric(12,3) NOT NULL CHECK (quantidade > 0)
);
CREATE INDEX historico_item_compra ON item_compra_historico(compra_id);
CREATE INDEX historico_compra_casa ON requisicao_conclusao_compra(casa_id, concluida_em DESC);
