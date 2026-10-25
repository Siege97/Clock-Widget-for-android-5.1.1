# Relógio Widget (Android 5.1.1+)

Widget de relógio digital simples. **Toque no relógio para alternar** entre:

- **Horizontal:** `14:35`
- **Vertical:** horas em cima, minutos embaixo

A escolha é salva separadamente para cada widget na tela.

## Como compilar

1. Abra a pasta `RelogioWidget` no Android Studio (Hedgehog ou mais novo, JDK 17).
2. Aguarde o Gradle sincronizar (o Android Studio baixa o Gradle 8.2 sozinho).
3. Conecte o celular com depuração USB ativada e clique em **Run**,
   ou use **Build > Build APK(s)** e instale o APK manualmente.

## Como usar

1. Na tela inicial, segure em um espaço vazio e abra **Widgets**.
2. Arraste **Relógio digital** para a tela.
3. Toque no relógio para virar na vertical (ou voltar para a horizontal).
4. Ao virar para a vertical, segure o widget e arraste as alças para deixá-lo
   mais alto e mais estreito. Em alguns launchers do 5.1 esse ajuste é manual.

## Personalização rápida

- **Tamanho do texto:** `android:textSize` em `res/layout/widget_horizontal.xml`
  e `res/layout/widget_vertical.xml`.
- **Cor do texto:** `android:textColor` nos mesmos arquivos.
- **Fundo:** `res/drawable/widget_bg.xml` (`#66000000` é preto semitransparente;
  use `#00000000` para fundo totalmente transparente).
- **12h/24h:** segue a configuração do sistema. Para forçar 24h, troque
  `format12Hour` por `HH:mm` (e `hh` por `HH` na versão vertical).
