# Aurora 9.0 — Local First / Resilient

## Objetivo
A Aurora 9.0 reorganiza o projeto para que o núcleo local seja a primeira opção e as integrações externas sejam opcionais.

## Runtime local
O módulo `dev.ffmpegkit-maintained:llama-android:0.1.1` fornece llama.cpp pré-compilado para Android arm64-v8a. Ele não exige CMake/NDK no projeto da Aurora e aceita modelos GGUF. A API é usada por `LocalGgufProvider`.

## Estratégia de recuperação
1. Modelo GGUF local.
2. Endpoint compatível remoto, somente se configurado.
3. A UI mostra estado real, sem fingir que há modelo quando não há.
4. O build não depende de CMake do SDK.
5. O script de build tenta Gradle offline antes de buscar dependências.
6. O modelo fica fora do APK e pode ser substituído pelo usuário.

## Memória e RAG
O Room existente continua local. O llama.cpp expõe embeddings no mesmo AAR, deixando espaço para busca semântica/RAG sem trocar a arquitetura de `ChatRepository`.

## Voz
TTS Android permanece local. A camada `AuroraVoice` está isolada para que reconhecimento de fala offline possa ser acoplado depois sem alterar o agente.

## Privacidade
`allowBackup=false` permanece ativo. O endpoint remoto é opcional e vazio por padrão. O modelo e o histórico ficam no armazenamento privado do aplicativo.

## Limites honestos
A Aurora não contém os modelos proprietários do ChatGPT nem pode reproduzir serviços pagos/proprietários. O projeto busca capacidades equivalentes usando componentes locais e abertos.
