# Aurora 5.0 — implementação única

Esta versão reorganiza a Aurora como um aplicativo de assistente pessoal para celular.

## Experiência

- A tela inicial apresenta a Aurora como presença visual central.
- A caneta azul é somente o ícone do aplicativo.
- A tela inicial oferece quatro áreas: Conversar, Ferramentas, Memória e Configurações.
- O Termux não faz parte da experiência do usuário.
- Conversas podem ser criadas, abertas e apagadas.
- Memórias importadas podem ser visualizadas e apagadas.
- Voz, modelo, endpoint e importação de GGUF ficam dentro de Configurações.

## Honestidade de estado

A interface não chama um modelo de pronto apenas por causa de texto decorativo. O runtime local continua explicitamente indisponível enquanto llama.cpp não estiver vendorado. O endpoint remoto permanece configurável e sua geração só acontece quando a conexão real for usada.

## Runtime local

O projeto agora liga o `externalNativeBuild` e compila a ponte JNI esqueleto. Isso não significa que llama.cpp foi incluído: o código nativo continua retornando indisponibilidade deliberadamente. O próximo passo técnico para inferência totalmente local é incorporar uma versão fixa do llama.cpp e implementar carregamento GGUF, tokenização, geração, streaming e cancelamento.

## Animação

A tela inicial possui uma presença animada desenhada por Canvas, com brilho, silhueta abstrata e órbitas suaves, sem depender de um asset quebrado. Ela é uma representação provisória: se a animação/personagem original do projeto estiver disponível, deve substituir esta presença sem alterar a navegação ou a arquitetura.

## Limitação de validação

O ambiente usado para preparar este ZIP não possui acesso de rede para baixar a distribuição Gradle, então não foi possível executar `assembleDebug` aqui. O projeto foi editado preservando a estrutura Gradle/Compose existente. A compilação final deve ser feita no ambiente Android/Termux que já possui o SDK/Gradle usados pelo projeto.
