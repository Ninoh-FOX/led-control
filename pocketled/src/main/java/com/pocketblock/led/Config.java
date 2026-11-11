package com.pocketblock.led;

import android.content.Context;
import android.content.SharedPreferences;

/** Persisted settings plus the LED color palette. */
final class Config {
    static final int NORMAL = 0, LOW = 1, CRITICAL = 2;
    static final String[] LEVEL_NAMES = {"Normal", "Baja", "Crítica"};

    /** Highest per-channel value sent to the AW2023 (the system itself uses 10). */
    static final int MAX_BRIGHTNESS = 15;

    static final String[] COLOR_NAMES = {
        "Verde", "Rojo", "Naranja", "Amarillo", "Cian", "Azul", "Violeta", "Blanco", "Apagado"};
    /** Channel weights {red, green, blue}, scaled by the brightness setting. */
    static final float[][] COLOR_MIX = {
        {0, 1, 0}, {1, 0, 0}, {1, 0.35f, 0}, {1, 1, 0}, {0, 1, 1},
        {0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 0, 0}};
    /** How each color is drawn on screen. */
    static final int[] COLOR_SCREEN = {
        0xFF2ECC40, 0xFFE53935, 0xFFFF8C00, 0xFFFFDC00, 0xFF00D8D8,
        0xFF2979FF, 0xFFC040FF, 0xFFFFFFFF, 0xFF303030};

    private static final int[] DEFAULT_COLOR = {0, 1, 1};
    private static final boolean[] DEFAULT_BLINK = {false, false, true};

    private final SharedPreferences prefs;

    Config(Context context) {
        prefs = context.getSharedPreferences("led", Context.MODE_PRIVATE);
    }

    boolean enabled() { return prefs.getBoolean("enabled", true); }
    void setEnabled(boolean v) { prefs.edit().putBoolean("enabled", v).apply(); }

    int brightness() { return prefs.getInt("brightness", 10); }
    void setBrightness(int v) { prefs.edit().putInt("brightness", v).apply(); }

    /** Whether the LED breathes while the screen is off. */
    boolean standbyBreath() { return prefs.getBoolean("standbyBreath", true); }
    void setStandbyBreath(boolean v) { prefs.edit().putBoolean("standbyBreath", v).apply(); }

    /** Battery percentage at or below which the level is "low". */
    int lowThreshold() { return prefs.getInt("low", 20); }
    void setLowThreshold(int v) { prefs.edit().putInt("low", v).apply(); }

    /** Battery percentage at or below which the level is "critical". */
    int criticalThreshold() { return prefs.getInt("critical", 10); }
    void setCriticalThreshold(int v) { prefs.edit().putInt("critical", v).apply(); }

    int color(int level) {
        int c = prefs.getInt("color" + level, DEFAULT_COLOR[level]);
        return c >= 0 && c < COLOR_NAMES.length ? c : DEFAULT_COLOR[level];
    }
    void setColor(int level, int color) { prefs.edit().putInt("color" + level, color).apply(); }

    boolean blink(int level) { return prefs.getBoolean("blink" + level, DEFAULT_BLINK[level]); }
    void setBlink(int level, boolean v) { prefs.edit().putBoolean("blink" + level, v).apply(); }

    int levelFor(int batteryPercent) {
        if (batteryPercent <= criticalThreshold()) return CRITICAL;
        if (batteryPercent <= lowThreshold()) return LOW;
        return NORMAL;
    }

    /** Channel values {red, green, blue} to write for the given level. */
    int[] channels(int level) {
        float[] mix = COLOR_MIX[color(level)];
        int brightness = brightness();
        int[] out = new int[3];
        for (int i = 0; i < 3; i++) {
            // A channel that is part of the color never rounds down to off.
            out[i] = mix[i] == 0 ? 0 : Math.max(1, Math.round(mix[i] * brightness));
        }
        return out;
    }
}
