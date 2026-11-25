CREATE TABLE usuario (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nome text NOT NULL CHECK (btrim(nome) <> ''),
    email text NOT NULL UNIQUE CHECK (email <> '' AND email = lower(btrim(email))),
    senha_hash text NOT NULL CHECK (btrim(senha_hash) <> '')
);

CREATE TABLE casa (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nome text NOT NULL CHECK (btrim(nome) <> '')
);

CREATE TABLE membro_casa (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    casa_id uuid NOT NULL REFERENCES casa(id),
    usuario_id uuid NOT NULL REFERENCES usuario(id),
    papel text NOT NULL CHECK (papel IN ('ADMINISTRADOR', 'MORADOR')),
    entrou_em timestamptz NOT NULL DEFAULT now(),
    removido_em timestamptz,
    UNIQUE (casa_id, usuario_id),
    UNIQUE (casa_id, id)
);

CREATE TABLE convite (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    casa_id uuid NOT NULL REFERENCES casa(id),
    emitido_por_id uuid NOT NULL,
    email_destino text NOT NULL CHECK (email_destino <> '' AND email_destino = lower(btrim(email_destino))),
    codigo_hash text NOT NULL UNIQUE CHECK (btrim(codigo_hash) <> ''),
    estado text NOT NULL DEFAULT 'PENDENTE' CHECK (estado IN ('PENDENTE', 'ACEITO', 'CANCELADO')),
    FOREIGN KEY (casa_id, emitido_por_id) REFERENCES membro_casa(casa_id, id)
);

CREATE TABLE recuperacao_senha (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id uuid NOT NULL REFERENCES usuario(id),
    token_hash text NOT NULL UNIQUE CHECK (btrim(token_hash) <> ''),
    expira_em timestamptz NOT NULL,
    usado_em timestamptz
);

CREATE TABLE categoria (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    casa_id uuid NOT NULL REFERENCES casa(id),
    nome text NOT NULL CHECK (btrim(nome) <> ''),
    criada_em timestamptz NOT NULL DEFAULT now(),
    criada_por_id uuid NOT NULL,
    UNIQUE (casa_id, id),
    FOREIGN KEY (casa_id, criada_por_id) REFERENCES membro_casa(casa_id, id)
);

CREATE UNIQUE INDEX categoria_nome_unico
    ON categoria (casa_id, lower(btrim(nome)));

CREATE TABLE produto_estoque (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    casa_id uuid NOT NULL REFERENCES casa(id),
    nome text NOT NULL CHECK (btrim(nome) <> ''),
    categoria_id uuid NOT NULL,
    unidade text NOT NULL CHECK (btrim(unidade) <> ''),
    quantidade_atual numeric(12,3) NOT NULL DEFAULT 0 CHECK (quantidade_atual >= 0 AND quantidade_atual <> 'NaN'),
    quantidade_desejada numeric(12,3) NOT NULL DEFAULT 0 CHECK (quantidade_desejada >= 0 AND quantidade_desejada <> 'NaN'),
    atualizado_em timestamptz NOT NULL DEFAULT now(),
    atualizado_por_id uuid NOT NULL,
    UNIQUE (casa_id, id),
    FOREIGN KEY (casa_id, atualizado_por_id) REFERENCES membro_casa(casa_id, id),
    FOREIGN KEY (casa_id, categoria_id) REFERENCES categoria(casa_id, id)
);

CREATE TABLE lista_compras (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    casa_id uuid NOT NULL UNIQUE REFERENCES casa(id),
    versao bigint NOT NULL DEFAULT 0 CHECK (versao >= 0),
    UNIQUE (casa_id, id)
);

CREATE TABLE item_lista (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    casa_id uuid NOT NULL REFERENCES casa(id),
    lista_id uuid NOT NULL,
    produto_id uuid,
    nome text CHECK (btrim(nome) <> ''),
    unidade text CHECK (btrim(unidade) <> ''),
    categoria_id uuid,
    quantidade numeric(12,3) NOT NULL CHECK (quantidade > 0 AND quantidade <> 'NaN'),
    situacao text NOT NULL DEFAULT 'PENDENTE' CHECK (situacao IN ('PENDENTE', 'NO_CARRINHO')),
    FOREIGN KEY (casa_id, lista_id) REFERENCES lista_compras(casa_id, id),
    FOREIGN KEY (casa_id, produto_id) REFERENCES produto_estoque(casa_id, id),
    FOREIGN KEY (casa_id, categoria_id) REFERENCES categoria(casa_id, id),
    CONSTRAINT item_lista_origem_valida CHECK (
        (produto_id IS NULL AND nome IS NOT NULL AND unidade IS NOT NULL AND categoria_id IS NOT NULL)
        OR (produto_id IS NOT NULL AND nome IS NULL AND unidade IS NULL AND categoria_id IS NULL)
    )
);

CREATE UNIQUE INDEX item_lista_produto_unico ON item_lista (lista_id, produto_id)
    WHERE produto_id IS NOT NULL;

CREATE UNIQUE INDEX item_lista_avulso_unico ON item_lista (lista_id, lower(btrim(nome)), lower(btrim(unidade)))
    WHERE produto_id IS NULL;
