# Aurora privada

Objetivo arquitetural: uma assistente local, com memória e dados privados no aparelho, sem depender de assinatura para o modo local.

## Componentes
1. UI Compose
2. AuroraAgent: persona, contexto, memória e histórico
3. Room: conversas e memórias
4. ModelManager: seleção entre local GGUF e endpoint opcional
5. NativeBridge: ponto de integração do runtime llama.cpp
6. AuroraVoice: TTS do Android

## Imagens
A geração de imagens deve ser tratada como um segundo runtime local, separado do LLM. O projeto atual não contém um modelo de difusão nem seus pesos. Não é seguro ou correto simular essa função.

## Privacidade
`allowBackup=false`, segredos em `SecretStore` e modelos dentro de `filesDir/models`.
