CREATE TABLE token_acesso (
    token_hash bytea PRIMARY KEY,
    usuario_id uuid NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    expira_em timestamptz NOT NULL
);

CREATE INDEX token_acesso_expiracao ON token_acesso (expira_em);
