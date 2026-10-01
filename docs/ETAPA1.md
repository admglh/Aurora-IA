# Etapa 1 — o que mudou e como verificar

## Mudanças
1. Removida a resposta fixa. `ViewModel → AuroraAgent → ModelManager → Provider` está ligado.
2. Estados reais (`NotConfigured / Loading / Ready / Generating / Error`) na UI: chip do cabeçalho, faixa de aviso, composer (envio bloqueado sem modelo; botão ■ para parar).
3. Room v2: `conversations` (threads) + `messages` + `memories` novas; **sem** `fallbackToDestructiveMigration`; `exportSchema=true` (schemas em `app/schemas/`).
4. Migration 1→2 preserva mensagens reais da v1 (numa conversa "Conversa anterior (v1)"), remove só a resposta fake, descarta a tabela `accounts` (chave em texto puro).
5. Importação da 2.0 reescrita para o schema real: `itens/texto/tipo`, cada linha `{t,user,aurora}` vira **duas** mensagens, segundos→ms, chaves únicas (idempotente), transação única, linhas ruins são puladas e relatadas.
6. Memória: `MemoryRetriever` injeta memórias de perfil + as que casam com a mensagem no system prompt.
7. Segurança: `allowBackup=false`; chave de API no Android Keystore (AES-GCM); HTTP em claro só para localhost/127.0.0.1; `POST_NOTIFICATIONS` removida; dados pessoais da 2.0 movidos para `src/debug`.
8. Limpeza: removidos `app/_` (nome `\x01` que quebrava `unzip`), `.cxx/`, backups `.antes-*`, logs de builds antigos.
9. `aurora_native.cpp`: `nativeCreate` devolve 0 (nunca "pronto" falso).

## NÃO verificado (sem Gradle/Android neste ambiente)
- Compilação. Se falhar, envie o erro exato.
- Validação de schema do Room na abertura do banco (o DDL da migração foi conferido contra as entidades via SQLite, mas só o aparelho confirma).
- Streaming SSE contra servidor real; SecretStore; toda a UI em aparelho.

## Checklist no aparelho
1. `./gradlew assembleDebug` compila.
2. Instalar **por cima** da versão anterior (mesmo `applicationId`) para exercitar a migration. Abrir o app: deve abrir sem crash.
3. Sem configurar nada: chip "SEM MODELO", faixa explicando o motivo, envio desabilitado.
4. "Conversas": deve existir "Aurora 2.0 — histórico importado" (14 mensagens) e, se havia testes antigos, "Conversa anterior (v1)" sem respostas fake.
5. "Modelo": configurar endpoint (ex.: `http://127.0.0.1:8080/v1` com um llama-server) → chip "PRONTA"; perguntar "O que você sabe sobre mim?" deve usar as 8 memórias.
6. Botão ■ durante a geração mantém o texto parcial.
7. Logcat tag `AppContainer`: relatório da importação (`memórias: +8 ... mensagens: +14`). Reabrir o app não deve duplicar.

## Pendências conhecidas
- Persona é rascunho; envie `persona.md`/`regras.md` da 2.0 para incorporar.
- Reinstalar do zero (sem dados) só importa a 2.0 em build **debug**.
- `ModelState.Ready` do endpoint remoto significa "configurado", não "conectado".
- Modelos "thinking" do Qwen3 podem emitir `<think>`: tratamento fica para a etapa do modelo local.
