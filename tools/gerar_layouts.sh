#!/usr/bin/env bash
# Gera os 40 layouts do relógio (10 fontes x 4 rotações).
#
# O widget do Android só aceita fonte e rotação definidas no layout, por isso existe
# um arquivo para cada combinação: app/src/main/res/layout/widget_clock_<fonte>_<rotação>.xml
#
# Para adicionar uma fonte:
#   1. inclua "chave:família" na lista abaixo;
#   2. acrescente a chave, a família e o nome em ClockOptions.java;
#   3. acrescente o nome da fonte em strings.xml;
#   4. rode: bash tools/gerar_layouts.sh
set -euo pipefail
cd "$(dirname "$0")/.."

destino="app/src/main/res/layout"
mkdir -p "$destino"

for par in "normal:sans-serif" "light:sans-serif-light" "thin:sans-serif-thin" \
           "medium:sans-serif-medium" "black:sans-serif-black" \
           "condensed:sans-serif-condensed" "serif:serif" "mono:monospace" \
           "casual:casual" "cursive:cursive"; do
  chave="${par%%:*}"
  familia="${par#*:}"
  for rot in 0 90 180 270; do
    cat > "$destino/widget_clock_${chave}_${rot}.xml" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<!-- Gerado por tools/gerar_layouts.sh (fonte ${familia}, rotação ${rot} graus). Não edite à mão. -->
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextClock
        android:id="@+id/clock"
        android:layout_width="1000dp"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:fontFamily="${familia}"
        android:format12Hour="hh:mm"
        android:format24Hour="HH:mm"
        android:gravity="center"
        android:includeFontPadding="false"
        android:rotation="${rot}"
        android:singleLine="true"
        android:textColor="#FFFFFF"
        android:textSize="48sp" />

</FrameLayout>
EOF
  done
done

echo "Layouts gerados em $destino"
