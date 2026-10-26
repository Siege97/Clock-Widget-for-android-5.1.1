package com.exemplo.relogiowidget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.widget.Toast;

/**
 * Mantém a tela ligada enquanto estiver ativo. Cada toque no relógio (ou na notificação)
 * liga ou desliga. A notificação fixa mostra o ícone na barra de status.
 */
public class KeepAwakeService extends Service {

    private static final String ACTION_TOGGLE = "com.exemplo.relogiowidget.TOGGLE_AWAKE";
    private static final String CHANNEL_ID = "tela_ligada";
    private static final int NOTIFICATION_ID = 1;

    private static volatile boolean running = false;

    private PowerManager.WakeLock wakeLock;

    static boolean isRunning() {
        return running;
    }

    /** PendingIntent que liga/desliga a tela sempre ligada. */
    static PendingIntent toggleIntent(Context context) {
        Intent intent = new Intent(context, KeepAwakeService.class);
        intent.setAction(ACTION_TOGGLE);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        if (Build.VERSION.SDK_INT >= 26) {
            return PendingIntent.getForegroundService(context, 0, intent, flags);
        }
        return PendingIntent.getService(context, 0, intent, flags);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Exigido pelo Android 8+ quando o serviço é iniciado em primeiro plano
        startForeground(NOTIFICATION_ID, buildNotification());

        if (running) {
            turnOff();
        } else {
            turnOn();
        }
        return START_NOT_STICKY;
    }

    private void turnOn() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "relogiowidget:tela");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire();
        running = true;
        feedback(true);
    }

    private void turnOff() {
        releaseLock();
        running = false;
        feedback(false);
        stopForeground(true);
        stopSelf();
    }

    private void releaseLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock = null;
    }

    @Override
    public void onDestroy() {
        releaseLock();
        if (running) {
            // Desligado de fora (por exemplo, ao salvar os ajustes com outro tipo de toque)
            running = false;
            feedback(false);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** Avisa a mudança: mensagem na tela e vibração (1 curta ao ativar, 2 curtas ao voltar ao normal). */
    private void feedback(boolean active) {
        Toast.makeText(getApplicationContext(),
                active ? R.string.aviso_tela_ativada : R.string.aviso_modo_normal,
                Toast.LENGTH_SHORT).show();
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(active ? new long[]{0, 100} : new long[]{0, 100, 100, 100}, -1);
            }
        } catch (SecurityException e) {
            // sem permissão de vibração: fica só a mensagem
        }
    }

    private Notification buildNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, getString(R.string.canal_tela), NotificationManager.IMPORTANCE_LOW));
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
            builder.setPriority(Notification.PRIORITY_LOW);
        }
        builder.setSmallIcon(R.drawable.ic_stat_awake)
                .setContentTitle(getString(R.string.tela_ligada))
                .setContentText(getString(R.string.toque_para_desligar))
                .setOngoing(true)
                .setContentIntent(toggleIntent(this));   // tocar na notificação desliga
        return builder.build();
    }
}
