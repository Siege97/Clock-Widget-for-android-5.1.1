package com.exemplo.relogiowidget;

import android.graphics.Typeface;

/** Opções de aparência do relógio: fontes, rotações, cores e opacidade. */
final class ClockOptions {

    private ClockOptions() {
    }

    // Fontes do sistema. A chave entra no nome do layout (widget_clock_<chave>_<rotação>)
    static final String[] FONT_KEYS = {
            "normal", "light", "thin", "medium", "black",
            "condensed", "serif", "mono", "casual", "cursive"
    };
    static final String[] FONT_FAMILIES = {
            "sans-serif", "sans-serif-light", "sans-serif-thin", "sans-serif-medium", "sans-serif-black",
            "sans-serif-condensed", "serif", "monospace", "casual", "cursive"
    };
    static final int[] FONT_LABELS = {
            R.string.fonte_normal, R.string.fonte_leve, R.string.fonte_fina, R.string.fonte_media,
            R.string.fonte_preta, R.string.fonte_condensada, R.string.fonte_serifada,
            R.string.fonte_mono, R.string.fonte_casual, R.string.fonte_cursiva
    };

    static final int[] ROTATIONS = {0, 90, 180, 270};
    static final int DEFAULT_ROTATION = 90;

    // 10 cores prontas (RGB, sem transparência)
    static final int[] COLORS = {
            0xFFFFFF, 0xB0BEC5, 0xFFEB3B, 0xFF9800, 0xF44336,
            0xF06292, 0xBA68C8, 0x42A5F5, 0x26C6DA, 0x66BB6A
    };
    static final int[] COLOR_NAMES = {
            R.string.cor_branco, R.string.cor_cinza, R.string.cor_amarelo, R.string.cor_laranja,
            R.string.cor_vermelho, R.string.cor_rosa, R.string.cor_roxo, R.string.cor_azul,
            R.string.cor_ciano, R.string.cor_verde
    };
    static final int DEFAULT_COLOR = 0xFFFFFF;

    // Opacidade em porcentagem (100 = totalmente visível)
    static final int MIN_OPACITY = 10;
    static final int DEFAULT_OPACITY = 100;

    /** Posição da fonte na lista; 0 (normal) se a chave for desconhecida. */
    static int fontIndex(String key) {
        for (int i = 0; i < FONT_KEYS.length; i++) {
            if (FONT_KEYS[i].equals(key)) return i;
        }
        return 0;
    }

    static Typeface typeface(int fontIndex) {
        return Typeface.create(FONT_FAMILIES[fontIndex], Typeface.NORMAL);
    }

    static int rotationOrDefault(int rotation) {
        for (int r : ROTATIONS) {
            if (r == rotation) return r;
        }
        return DEFAULT_ROTATION;
    }

    static int clampOpacity(int percent) {
        return Math.max(MIN_OPACITY, Math.min(100, percent));
    }

    /** Junta cor RGB e opacidade (%) em um único valor ARGB. */
    static int argb(int rgb, int opacityPercent) {
        int alpha = Math.round(clampOpacity(opacityPercent) * 255f / 100f);
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }
}
