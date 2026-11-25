# Estoque e moradores

As rotas da casa exigem Bearer Token e vínculo ativo. Convites e gestão de papéis exigem ADMINISTRADOR. O último administrador não pode ser removido ou rebaixado.

| Método | Rota | Corpo / parâmetros |
|---|---|---|
| POST | /api/casas/{casaId}/produtos/{produtoId}/consumos | {"quantidade": 1} |
| GET | /api/casas/{casaId}/movimentacoes | pagina=0&tamanho=20; produtoId opcional |
| GET | /api/casas/{casaId}/alertas | Produtos EM_FALTA e BAIXO |
| GET | /api/casas/{casaId}/moradores | Moradores ativos |
| POST | /api/casas/{casaId}/convites | {"email": "pessoa@example.com"} |
| GET | /api/casas/{casaId}/convites | Estado e validade dos convites |
| DELETE | /api/casas/{casaId}/convites/{conviteId} | Cancela convite pendente |
| POST | /api/convites/aceitar | {"codigo": "...", "email": "pessoa@example.com", "senha": "...", "nome": "Pessoa"} |
| PATCH | /api/casas/{casaId}/moradores/{membroId}/papel | {"papel": "ADMINISTRADOR"} ou MORADOR |
| DELETE | /api/casas/{casaId}/moradores/{membroId} | Remove vínculo, preservando histórico |

## Convites

A criação retorna o código apenas uma vez para compartilhamento manual. Não há envio automático de e-mail. O banco armazena somente o hash do código, com validade de sete dias. Um novo convite cancela os anteriores pendentes para o mesmo e-mail na casa.

O aceite é público, mas exige código e e-mail correspondentes. Se a conta existir, exige a senha atual; se não existir, cria a conta com nome e senha informados. Depois, o morador faz login normalmente. O novo vínculo recebe MORADOR. O código não pode ser reutilizado.

## Movimentações e alertas

A migration V5 registra CADASTRO, AJUSTE, CONSUMO, COMPRA e EXCLUSAO por trigger na mesma transação do estoque. Guarda identificação do produto, nome, unidade, saldos, diferença, data e responsável. Registros sobrevivem à exclusão do produto. Não há reconstrução de movimentações anteriores à V5. A repetição idempotente de uma conclusão não gera novas entradas.

Consumo recebe quantidade positiva, até três casas decimais, e bloqueia o produto durante a atualização. Não permite saldo negativo e não altera a meta mínima. Alertas são uma consulta atual do estoque, sem envio de notificações.

## Erros

Respostas usam application/problem+json, com status e detail. Erros de campos incluem erros (campo e mensagem). Resumo desatualizado mantém resumoAtualizado. Respostas inesperadas não expõem detalhes internos. Token ausente/inválido retorna 401; papel insuficiente, 403; casa inacessível, 404; conflitos, 409.

## Validação

Testes adicionados: meta preservada após compra, consumo sem saldo negativo, isolamento de casa, restrição de convites e proteção do último administrador. A execução de `mvn test` foi tentada após as implementações, mas a compilação foi bloqueada pela leitura do jackson-annotations-2.21.jar no cache Maven local. Compilação, migration V5 e integração HTTP ainda precisam ser validadas em ambiente com o cache acessível.
