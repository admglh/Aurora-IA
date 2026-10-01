# Arquitetura (Etapa 1)

```
MainActivity (Compose)
   └─ AuroraViewModel ── estado real: modelState, mensagens, geração, erro
        ├─ ChatRepository ── Room (conversations / messages)
        └─ AuroraAgent ── persona + data/hora + memórias relevantes + histórico
             ├─ PersonaProvider   (assets/persona/aurora_5_0.md)
             ├─ MemoryRetriever   (Room: memories)
             └─ ModelManager ── escolhe o provedor e expõe UM ModelState
                  ├─ LocalGgufProvider        (NÃO IMPLEMENTADO — só reporta o motivo)
                  └─ OpenAICompatibleProvider (implementado; sem teste em servidor real)
AppContainer (composição) · AuroraSettings · SecretStore (Keystore) · LegacyMigration
```

## Status
| Componente | Status |
|---|---|
| UI de chat, conversas (abrir/apagar), estado do modelo, parar geração | FUNCIONAL (não testado em aparelho) |
| Room v2 + Migration 1→2 (sem fallback destrutivo) | FUNCIONAL (SQL testado em SQLite; validação do Room só no build/aparelho) |
| Importação Aurora 2.0 (memória + conversas, idempotente) | FUNCIONAL (lógica testada com os dados reais fora do Android) |
| Recuperação de memória (lexical) → prompt | FUNCIONAL (simples; sem embeddings) |
| Persona / system prompt | RASCUNHO (não é o persona.md da 2.0, que não foi enviado) |
| OpenAICompatibleProvider (streaming SSE, cancelamento) | IMPLEMENTADO, NÃO TESTADO em servidor real |
| SecretStore (chave de API cifrada no Keystore) | IMPLEMENTADO, NÃO TESTADO em aparelho |
| LocalGgufProvider / NativeBridge / JNI | PLACEHOLDER HONESTO: nunca fica "pronto" |
| llama.cpp, gerenciador de GGUF, template Qwen3 | NÃO EXISTE |
| Voz (TTS), tarefas agendadas, ferramentas, permissões | NÃO EXISTE (`TaskEntity`/`Tool` são stubs sem uso) |
| Tela Aurora, Configurações definitivas, nova identidade visual | NÃO EXISTE (Etapa 2) |
