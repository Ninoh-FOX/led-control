package com.pocketblock.led;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

/**
 * Keeps the LED showing the color configured for the current battery level.
 * While the charger is plugged in the LED is left to the system's charging light.
 */
public class LedService extends Service {
    /** Optional extra: show this level for a few seconds regardless of the battery. */
    static final String EXTRA_PREVIEW = "preview";

    private static final String CHANNEL = "led";
    // The color is rewritten periodically in case something else changes the LED.
    private static final long REFRESH_MS = 2000;
    private static final long BLINK_MS = 500;
    private static final long PREVIEW_MS = 5000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::tick;
    private Config config;
    private boolean blinkOn;
    private int previewLevel = -1;
    private long previewUntil;
    private int batteryPercent = 100;
    private boolean plugged;
    /** True while the LED shows one of our colors rather than the system's. */
    private boolean controlling;
    /** What the system had on the LED while charging, to put back after a preview. */
    private int[] systemColor;

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean wasPlugged = plugged;
            readBattery(intent);
            if (plugged != wasPlugged) restart();
        }
    };

    static void start(Context context, int previewLevel) {
        Intent intent = new Intent(context, LedService.class);
        if (previewLevel >= 0) intent.putExtra(EXTRA_PREVIEW, previewLevel);
        context.startForegroundService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        config = new Config(this);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(
            CHANNEL, "Control del LED", NotificationManager.IMPORTANCE_MIN));
        PendingIntent open = PendingIntent.getActivity(this, 0,
            new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.presence_online)
            .setContentTitle("Pocket LED activo")
            .setContentText("Controlando el LED de batería")
            .setContentIntent(open)
            .setOngoing(true)
            .build();
        startForeground(1, notification);
        readBattery(registerReceiver(batteryReceiver,
            new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(EXTRA_PREVIEW)) {
            previewLevel = intent.getIntExtra(EXTRA_PREVIEW, -1);
            previewUntil = SystemClock.elapsedRealtime() + PREVIEW_MS;
        }
        restart(); // apply any settings change right away
        return START_STICKY;
    }

    private void restart() {
        handler.removeCallbacks(tick);
        blinkOn = false;
        handler.post(tick);
    }

    private void tick() {
        if (!config.enabled()) {
            stopSelf();
            return;
        }
        int level;
        if (previewLevel >= 0 && SystemClock.elapsedRealtime() < previewUntil) {
            level = previewLevel;
        } else if (plugged) {
            previewLevel = -1;
            leaveToSystem();
            handler.postDelayed(tick, REFRESH_MS);
            return;
        } else {
            previewLevel = -1;
            level = config.levelFor(batteryPercent);
        }
        controlling = true;
        if (config.blink(level)) {
            blinkOn = !blinkOn;
            if (blinkOn) Led.set(config.channels(level)); else Led.off();
            handler.postDelayed(tick, BLINK_MS);
        } else {
            Led.set(config.channels(level));
            handler.postDelayed(tick, previewLevel >= 0 ? BLINK_MS : REFRESH_MS);
        }
    }

    /**
     * Charger plugged in: the system owns the LED. We only remember what it shows,
     * and put that back if a preview painted over it.
     */
    private void leaveToSystem() {
        if (controlling && systemColor != null) {
            Led.set(systemColor);
        } else {
            int[] current = Led.get();
            if (current != null) systemColor = current;
        }
        controlling = false;
    }

    private void readBattery(Intent battery) {
        if (battery == null) return;
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        if (level >= 0 && scale > 0) batteryPercent = level * 100 / scale;
        plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        // A new charging session starts from whatever the system paints.
        if (!plugged) systemColor = null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        unregisterReceiver(batteryReceiver);
        if (plugged) leaveToSystem(); else Led.off();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
