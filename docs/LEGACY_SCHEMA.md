# Schema 2.0 observado
memoria.json: objeto {versao,itens[]} com id,texto,tipo,criado_em,atualizado_em.
conversas.json: objeto {versao,conversas{...}} — schema interno NÃO verificado (sem amostra); a importação o ignora e avisa.
conversas.jsonl: linhas com t,user,aurora.
accounts.json: credenciais locais; NÃO migrar automaticamente.
sessions.json: tokens de sessão; NÃO migrar automaticamente.
config/persona.md e config/regras.md: regras de comportamento; podem ser importados como recursos da aplicação.

Tempos da 2.0 estão em SEGUNDOS (float); a 5.0 converte para milissegundos.
Importado pela 5.0: memoria.json (itens[].texto/tipo/criado_em/atualizado_em) e conversas.jsonl (t/user/aurora).
