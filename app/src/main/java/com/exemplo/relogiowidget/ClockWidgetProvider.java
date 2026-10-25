package com.exemplo.relogiowidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.RemoteViews;

/**
 * Relógio digital com rotação, fonte, cor e opacidade configuráveis.
 *
 * A hora é um TextClock (o sistema atualiza sozinho, sem alarme). A fonte e a rotação
 * vêm do layout escolhido (widget_clock_<fonte>_<rotação>); este código ajusta o tamanho
 * da letra para caber no widget, a cor com opacidade, a posição (offset em pixels)
 * e o que acontece ao tocar. Só roda quando o widget é criado, redimensionado
 * ou quando você salva os ajustes.
 */
public class ClockWidgetProvider extends AppWidgetProvider {

    // Configurações (valem para todos os widgets)
    static final String PREFS = "clock_prefs";
    static final String KEY_APP = "app_pkg";
    static final String KEY_OFFSET_X = "offset_x";
    static final String KEY_OFFSET_Y = "offset_y";
    static final String KEY_ACTION = "tap_action";   // o que o toque no relógio faz
    static final String KEY_FONT = "font";
    static final String KEY_ROTATION = "rotation";
    static final String KEY_COLOR = "color";
    static final String KEY_OPACITY = "opacity";
    static final String TAP_APP = "app";             // abre o app escolhido
    static final String TAP_NONE = "none";           // não faz nada
    static final String TAP_AWAKE = "awake";         // liga/desliga "tela sempre ligada"

    // ---- Ajustes visuais (mude aqui) ----
    private static final float MARGIN_DP = 4f;       // folga entre o relógio e a borda do widget
    private static final int DEFAULT_W_DP = 70;      // usados se o launcher não informar o tamanho
    private static final int DEFAULT_H_DP = 250;

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            update(context, manager, id);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                          int appWidgetId, Bundle newOptions) {
        // O widget foi redimensionado: recalcula o tamanho da letra
        update(context, manager, appWidgetId);
    }

    /** Atualiza todos os widgets na tela. Devolve quantos são. */
    static int updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, ClockWidgetProvider.class));
        for (int id : ids) {
            update(context, manager, id);
        }
        return ids.length;
    }

    /** Aplica fonte, rotação, tamanho, cor, posição e ação do toque em um widget. */
    static void update(Context context, AppWidgetManager manager, int id) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int dx = prefs.getInt(KEY_OFFSET_X, 0);
        int dy = prefs.getInt(KEY_OFFSET_Y, 0);
        int fontIndex = ClockOptions.fontIndex(prefs.getString(KEY_FONT, ClockOptions.FONT_KEYS[0]));
        int rotation = ClockOptions.rotationOrDefault(
                prefs.getInt(KEY_ROTATION, ClockOptions.DEFAULT_ROTATION));
        int color = ClockOptions.argb(
                prefs.getInt(KEY_COLOR, ClockOptions.DEFAULT_COLOR),
                prefs.getInt(KEY_OPACITY, ClockOptions.DEFAULT_OPACITY));

        // O layout carrega a fonte e a rotação (o widget não aceita trocar isso por código)
        int layoutId = context.getResources().getIdentifier(
                "widget_clock_" + ClockOptions.FONT_KEYS[fontIndex] + "_" + rotation,
                "layout", context.getPackageName());
        if (layoutId == 0) {
            layoutId = R.layout.widget_clock_normal_90;
        }
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

        // Tamanho da letra: o maior que cabe, já descontando o espaço do deslocamento
        views.setTextViewTextSize(R.id.clock, TypedValue.COMPLEX_UNIT_PX,
                fitTextSize(context, manager, id, dx, dy, ClockOptions.typeface(fontIndex), rotation));

        views.setTextColor(R.id.clock, color);

        // Posição: o relógio fica centralizado na área sem padding. Padding de 2*d de um lado
        // desloca o centro em d pixels (positivo = direita / baixo).
        views.setViewPadding(R.id.root,
                Math.max(0, 2 * dx), Math.max(0, 2 * dy),
                Math.max(0, -2 * dx), Math.max(0, -2 * dy));

        // Ação do toque
        String action = prefs.getString(KEY_ACTION, TAP_APP);
        PendingIntent pending = null;
        if (TAP_AWAKE.equals(action)) {
            pending = KeepAwakeService.toggleIntent(context);
        } else if (!TAP_NONE.equals(action)) {
            Intent launch = null;
            String pkg = prefs.getString(KEY_APP, null);
            if (pkg != null) {
                launch = context.getPackageManager().getLaunchIntentForPackage(pkg);
            }
            if (launch == null) {
                // Nenhum app escolhido (ou o app foi desinstalado): o toque abre a lista para escolher
                launch = new Intent(context, ConfigActivity.class);
                launch.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            // requestCode = id mantém um PendingIntent separado para cada widget
            pending = PendingIntent.getActivity(context, id, launch, flags);
        }
        if (pending != null) {
            views.setOnClickPendingIntent(R.id.root, pending);
        }

        manager.updateAppWidget(id, views);
    }

    /**
     * Maior tamanho de letra (em pixels) em que "00:00" cabe no widget, na fonte e rotação
     * escolhidas. Em 90 e 270 graus o comprimento do texto usa a altura do widget e a altura
     * dos dígitos usa a largura; em 0 e 180 é o contrário.
     */
    private static float fitTextSize(Context context, AppWidgetManager manager, int id,
                                     int dx, int dy, Typeface typeface, int rotation) {
        float density = context.getResources().getDisplayMetrics().density;
        boolean landscape = context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        // Tamanho atual do widget em dp, informado pelo launcher
        Bundle options = manager.getAppWidgetOptions(id);
        int wDp = 0;
        int hDp = 0;
        if (options != null) {
            wDp = options.getInt(landscape
                    ? AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
                    : AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
            hDp = options.getInt(landscape
                    ? AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
                    : AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
        }
        if (wDp <= 0 || hDp <= 0) {
            wDp = DEFAULT_W_DP;
            hDp = DEFAULT_H_DP;
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(typeface);
        final float base = 100f;
        paint.setTextSize(base);
        Rect digit = new Rect();
        paint.getTextBounds("0", 0, 1, digit);
        float length = paint.measureText("00:00");     // comprimento do texto
        float thickness = Math.max(1, digit.height()); // altura dos dígitos

        // Reserva espaço para o deslocamento (nos dois lados, pois o relógio é centralizado),
        // assim a hora nunca passa da borda do widget
        float margin = MARGIN_DP * density;
        float availWidth = Math.max(1f, wDp * density - 2 * margin - 2 * Math.abs(dx));
        float availHeight = Math.max(1f, hDp * density - 2 * margin - 2 * Math.abs(dy));
        boolean vertical = rotation == 90 || rotation == 270;
        float lengthSpace = vertical ? availHeight : availWidth;
        float thicknessSpace = vertical ? availWidth : availHeight;

        float scale = Math.min(thicknessSpace / thickness, lengthSpace / Math.max(1f, length));
        if (scale <= 0f) {
            scale = 1f;
        }
        return base * scale;
    }
}
