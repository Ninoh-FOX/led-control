package com.pocketblock.led;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** Settings screen. The UI is built in code because the APK has no resources. */
public class MainActivity extends Activity {
    private Config config;
    private LinearLayout root;
    private SeekBar lowBar, criticalBar;
    private TextView lowLabel, criticalLabel;
    private final TextView[] levelTitles = new TextView[3];
    private final Button[][] swatches = new Button[3][Config.COLOR_NAMES.length];

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        config = new Config(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        text("Pocket LED", 24);
        TextView status = text(Led.available()
            ? "LED de batería detectado."
            : "No se puede escribir en el LED: esta consola no es compatible.", 14);
        if (!Led.available()) status.setTextColor(0xFFFF6E6E);

        text("Mientras la consola carga, el LED lo controla el sistema; "
            + "\"Probar\" sigue funcionando.", 14);

        Switch enabled = new Switch(this);
        enabled.setText("Controlar el LED de batería");
        enabled.setTextSize(18);
        enabled.setChecked(config.enabled());
        enabled.setOnCheckedChangeListener((v, on) -> {
            config.setEnabled(on);
            apply(-1);
        });
        root.addView(enabled, rowParams());

        Switch standby = new Switch(this);
        standby.setText("Respirar en reposo (pantalla apagada)");
        standby.setTextSize(18);
        standby.setChecked(config.standbyBreath());
        standby.setOnCheckedChangeListener((v, on) -> {
            config.setStandbyBreath(on);
            apply(-1);
        });
        root.addView(standby, rowParams());
        text("Para que el LED respire, la consola no entra en suspensión profunda: "
            + "la batería dura menos en reposo.", 14);

        TextView brightnessLabel = text(brightnessText(config.brightness()), 16);
        slider(1, Config.MAX_BRIGHTNESS, config.brightness(), v -> {
            config.setBrightness(v);
            brightnessLabel.setText(brightnessText(v));
        });

        lowLabel = text("", 16);
        lowBar = slider(2, 95, config.lowThreshold(), v -> {
            config.setLowThreshold(v);
            if (config.criticalThreshold() >= v) criticalBar.setProgress(v - 1);
            refreshRanges();
        });
        criticalLabel = text("", 16);
        criticalBar = slider(1, 94, config.criticalThreshold(), v -> {
            config.setCriticalThreshold(v);
            if (config.lowThreshold() <= v) lowBar.setProgress(v + 1);
            refreshRanges();
        });

        for (int level = 0; level < 3; level++) levelSection(level);
        refreshRanges();
        apply(-1);
    }

    private void levelSection(int level) {
        levelTitles[level] = text("", 20);
        levelTitles[level].setPadding(0, dp(18), 0, dp(4));

        LinearLayout row = new LinearLayout(this);
        for (int c = 0; c < Config.COLOR_NAMES.length; c++) {
            final int color = c;
            Button swatch = new Button(this);
            swatch.setContentDescription(Config.COLOR_NAMES[c]);
            swatch.setTextSize(20);
            swatch.setOnClickListener(v -> {
                config.setColor(level, color);
                refreshSwatches(level);
                apply(level);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(52), 1);
            lp.setMargins(dp(3), 0, dp(3), 0);
            row.addView(swatch, lp);
            swatches[level][c] = swatch;
        }
        root.addView(row, rowParams());
        refreshSwatches(level);

        LinearLayout options = new LinearLayout(this);
        options.setGravity(Gravity.CENTER_VERTICAL);
        CheckBox blink = new CheckBox(this);
        blink.setText("Parpadear");
        blink.setTextSize(16);
        blink.setChecked(config.blink(level));
        blink.setOnCheckedChangeListener((v, on) -> {
            config.setBlink(level, on);
            apply(level);
        });
        options.addView(blink, new LinearLayout.LayoutParams(0, -2, 1));
        Button preview = new Button(this);
        preview.setText("Probar");
        preview.setOnClickListener(v -> apply(level));
        options.addView(preview);
        root.addView(options, rowParams());
    }

    /** Pushes the settings to the service; previewLevel >= 0 shows that level briefly. */
    private void apply(int previewLevel) {
        if (config.enabled()) {
            LedService.start(this, previewLevel);
        } else {
            stopService(new Intent(this, LedService.class));
        }
    }

    private static String brightnessText(int value) {
        return "Brillo: " + value + " / " + Config.MAX_BRIGHTNESS;
    }

    private void refreshRanges() {
        int low = config.lowThreshold(), critical = config.criticalThreshold();
        lowLabel.setText("Batería baja: " + low + " % o menos");
        criticalLabel.setText("Batería crítica: " + critical + " % o menos");
        levelTitles[Config.NORMAL].setText("Normal (100 – " + (low + 1) + " %)");
        levelTitles[Config.LOW].setText("Baja (" + low + " – " + (critical + 1) + " %)");
        levelTitles[Config.CRITICAL].setText("Crítica (" + critical + " – 0 %)");
    }

    private void refreshSwatches(int level) {
        int selected = config.color(level);
        for (int c = 0; c < swatches[level].length; c++) {
            Button swatch = swatches[level][c];
            int fill = Config.COLOR_SCREEN[c];
            swatch.setBackground(swatchBackground(fill));
            swatch.setText(c == selected ? "✓" : "");
            swatch.setTextColor(Color.luminance(fill) > 0.4f ? Color.BLACK : Color.WHITE);
        }
    }

    /** Solid swatch with a white ring while focused, so it can be driven with the d-pad. */
    private Drawable swatchBackground(int fill) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, swatchShape(fill, dp(4), Color.WHITE));
        states.addState(new int[0], swatchShape(fill, dp(1), 0xFF808080));
        return states;
    }

    private Drawable swatchShape(int fill, int strokeWidth, int strokeColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(8));
        shape.setStroke(strokeWidth, strokeColor);
        return shape;
    }

    private interface OnValue {
        void set(int value);
    }

    private SeekBar slider(int min, int max, int value, OnValue onValue) {
        SeekBar bar = new SeekBar(this);
        bar.setMin(min);
        bar.setMax(max);
        bar.setProgress(value);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar b, int progress, boolean fromUser) {
                onValue.set(progress);
                apply(-1);
            }
            @Override public void onStartTrackingTouch(SeekBar b) {}
            @Override public void onStopTrackingTouch(SeekBar b) {}
        });
        root.addView(bar, rowParams());
        return bar;
    }

    private TextView text(String value, int sizeSp) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        root.addView(view, rowParams());
        return view;
    }

    private LinearLayout.LayoutParams rowParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(6), 0, dp(6));
        return lp;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
