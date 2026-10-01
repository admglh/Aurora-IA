# Motor local da Aurora 5.1

A Aurora usa `dev.ffmpegkit-maintained:llama-android:0.1.1`, um AAR pré-compilado de llama.cpp para Android.

- GGUF local
- arm64-v8a
- CPU/NEON
- sem servidor e sem chave de API
- sem CMake/NDK no projeto da Aurora
- o modelo não é embutido no APK por padrão

O usuário importa o GGUF pela interface e o aplicativo copia o arquivo para o armazenamento privado.

O AAR não fornece streaming de tokens na edição gratuita, então a Aurora recebe a resposta completa e a apresenta ao usuário.
