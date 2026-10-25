# Relógio Widget para Android 5.1.1

Widget de relógio digital para a tela inicial do Android 5.1.1 (API 22) ou mais novo.

## O que faz

- Relógio com **rotação** de 0°, 90° (padrão), 180° ou 270°.
- **Fonte** entre 10 famílias do sistema, **cor** (10 prontas ou código HEX) e **opacidade** (10% a 100%).
- **Posição** ajustável em pixels (horizontal e vertical).
- **Ao tocar**: abrir um app à escolha, não fazer nada, ou manter a tela ligada (liga e desliga a cada toque, com um cadeado na barra de status).
- Os ajustes ficam no ícone **Relógio Widget** da lista de apps.

## Como gerar o APK

O GitHub compila sozinho a cada commit:

1. Abra a aba **Actions** e entre na execução mais recente.
2. Baixe o arquivo em **Artifacts**. O nome segue o padrão `relógio_90_<versão>.apk`.
3. Instale no celular (permita "fontes desconhecidas" se o Android pedir).

O APK é assinado com a chave fixa `app/debug.keystore`, então cada versão nova instala por cima da anterior sem desinstalar. É uma chave de depuração pública (senha `android`), adequada só para uso pessoal.

## Estrutura

| Caminho | Conteúdo |
| --- | --- |
| `.github/workflows/build.yml` | Receita de compilação e nome do APK |
| `app/build.gradle` | Configuração do app e **número da versão** (`versionName`) |
| `app/src/main/java/.../ClockWidgetProvider.java` | Desenha o widget: tamanho, cor, posição e toque |
| `app/src/main/java/.../SettingsActivity.java` | Tela de ajustes |
| `app/src/main/java/.../ConfigActivity.java` | Lista de apps para o toque |
| `app/src/main/java/.../KeepAwakeService.java` | Mantém a tela ligada |
| `app/src/main/java/.../ClockOptions.java` | Lista de fontes, cores e limites |
| `app/src/main/res/layout/widget_clock_*.xml` | Um layout por fonte e rotação (gerados) |
| `tools/gerar_layouts.sh` | Gera os 40 layouts do relógio |

## Nova versão

1. Mude o `versionName` (e o `versionCode`) em `app/build.gradle`.
2. Faça o commit. O nome do APK acompanha o `versionName`.

## Como funciona

O texto do relógio é um `TextClock` comum, atualizado pelo próprio sistema (sem alarme nem serviço). O widget do Android 5.1 só aceita fonte e rotação definidas no layout, por isso há um arquivo para cada combinação. O código escolhe o layout, ajusta o tamanho da letra ao tamanho do widget, aplica a cor e a posição e define o que o toque faz. Isso só roda quando o widget é criado, redimensionado ou quando os ajustes são salvos.
