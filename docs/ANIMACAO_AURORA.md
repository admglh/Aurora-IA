# Animação da Aurora

A animação da tela inicial foi construída a partir do vídeo fornecido pelo usuário (`60310.mp4`).

- 61 quadros extraídos a aproximadamente 12 fps.
- Resolução interna: 360×640, adequada para a tela principal de celular.
- O fundo claro do vídeo foi removido por máscara baseada no fundo conectado às bordas.
- O personagem foi composto sobre um cenário noturno em preto/navy com halos azul/ciano/violeta.
- Os quadros foram empacotados em `app/src/main/res/drawable/aurora_animation.webp`.
- O WebP é animado e reproduz em loop através de `AnimatedImageDrawable` (Android 9/API 28+; o projeto exige API 29).

O desenho da caneta continua sendo o ícone do aplicativo. A personagem não é usada como ícone.
