# Aurora 5.1

Esta versão é uma reconstrução de manutenção sobre a base Aurora 5.0.

## O que está funcional
- Interface Compose e navegação.
- Banco local de conversas e memórias.
- Importação de GGUF para o armazenamento privado do app.
- Configuração de endpoint compatível com OpenAI.
- Voz via TTS do Android.
- Presença animada opcional, desligada por padrão.
- Backup Android desativado para dados privados.

## Limite técnico real
O projeto original não contém o runtime llama.cpp nem um motor de geração de imagens local. A ponte JNI original é deliberadamente um stub. Portanto esta versão não deve declarar que o GGUF local ou geração de imagens funcionam sem um runtime real.

## Próximo componente de infraestrutura
Para inferência local completa, o APK precisa incorporar um build ARM64 de llama.cpp/ggml e conectar a API JNI a `LocalGgufProvider`. Para geração de imagens local, é necessário um segundo runtime/modelo apropriado. Esses componentes não podem ser inventados a partir do ZIP original sem seus binários/fontes de runtime.
