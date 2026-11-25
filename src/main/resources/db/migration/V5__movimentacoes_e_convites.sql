CREATE TABLE movimentacao_estoque (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    casa_id uuid NOT NULL REFERENCES casa(id),
    produto_id uuid NOT NULL,
    nome text NOT NULL,
    unidade text NOT NULL,
    tipo text NOT NULL,
    saldo_anterior numeric(12,3) NOT NULL,
    saldo_atual numeric(12,3) NOT NULL,
    quantidade numeric(12,3) NOT NULL,
    responsavel_id uuid NOT NULL REFERENCES membro_casa(id),
    criada_em timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX movimentacao_casa_data ON movimentacao_estoque(casa_id, criada_em DESC, id);
CREATE FUNCTION registrar_movimentacao_estoque() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE anterior numeric(12,3); atual numeric(12,3); tipo_mov text; registro produto_estoque;
BEGIN
    IF TG_OP = 'UPDATE' AND NEW.quantidade_atual = OLD.quantidade_atual AND NEW.unidade = OLD.unidade THEN RETURN NEW; END IF;
    IF TG_OP = 'DELETE' THEN registro := OLD; atual := 0; ELSE registro := NEW; atual := NEW.quantidade_atual; END IF;
    IF TG_OP = 'INSERT' THEN anterior := 0; ELSE anterior := OLD.quantidade_atual; END IF;
    tipo_mov := COALESCE(NULLIF(current_setting('app.tipo_movimentacao', true), ''),
        CASE TG_OP WHEN 'INSERT' THEN 'CADASTRO' WHEN 'DELETE' THEN 'EXCLUSAO' ELSE 'AJUSTE' END);
    INSERT INTO movimentacao_estoque(casa_id, produto_id, nome, unidade, tipo, saldo_anterior, saldo_atual, quantidade, responsavel_id)
    VALUES(registro.casa_id, registro.id, registro.nome, registro.unidade, tipo_mov, anterior, atual, atual-anterior, registro.atualizado_por_id);
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER produto_movimentacao AFTER INSERT OR UPDATE OR DELETE ON produto_estoque
    FOR EACH ROW EXECUTE FUNCTION registrar_movimentacao_estoque();
ALTER TABLE convite ADD COLUMN criado_em timestamptz NOT NULL DEFAULT now();
ALTER TABLE convite ADD COLUMN expira_em timestamptz NOT NULL DEFAULT now() + interval '7 days';
CREATE INDEX convite_destinatario ON convite(email_destino, estado);
