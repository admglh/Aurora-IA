# Arquitetura de resiliência

A regra de manutenção da Aurora é: **não acoplar a interface ao motor**.

- `ModelProvider`: contrato do motor.
- `LocalGgufProvider`: inferência GGUF no aparelho.
- `OpenAICompatibleProvider`: rota opcional para servidores compatíveis.
- `ModelManager`: seleção do primeiro motor realmente pronto.
- `AuroraAgent`: persona, memória e contexto acima do motor.
- Room: persistência local.

Se uma implementação de runtime for substituída, somente o adaptador deve mudar. A conversa, memória e interface não precisam ser reescritas.

Também evitamos depender de CMake próprio da aplicação. O runtime pré-compilado reduz uma classe inteira de falhas de toolchain Android.
