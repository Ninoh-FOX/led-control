package com.example.ledtest;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Pantalla de prueba: botones de color, selector de backend y estado. */
public class MainActivity extends Activity {

    private final SimulatedBackend simulated = new SimulatedBackend();
    private final SysfsBackend sysfs = new SysfsBackend();
    private LedBackend backend = simulated;

    private TextView status;
    private TextView backendLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        backendLabel = new TextView(this);
        backendLabel.setTextSize(18);
        root.addView(backendLabel);

        Button toggle = new Button(this);
        toggle.setText("Cambiar backend");
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backend = (backend == simulated) ? sysfs : simulated;
                refreshLabel();
                setStatus("Backend cambiado a " + backend.name());
            }
        });
        root.addView(toggle);

        addColorButton(root, "Verde", Color.rgb(0, 255, 0));
        addColorButton(root, "Rojo", Color.rgb(255, 0, 0));
        addColorButton(root, "Blanco", Color.rgb(255, 255, 255));
        addColorButton(root, "Apagar", Color.BLACK);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, dp(16), 0, 0);
        root.addView(status);

        refreshLabel();
        setStatus("Listo");
        setContentView(root);
    }

    private void addColorButton(LinearLayout root, String text, final int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String error = backend.apply(color);
                if (error == null) {
                    setStatus("OK: " + backend.name() + " -> #" + Integer.toHexString(color & 0xFFFFFF));
                } else {
                    setStatus("Error en " + backend.name() + ": " + error);
                }
            }
        });
        root.addView(b);
    }

    private void refreshLabel() {
        backendLabel.setText("Backend: " + backend.name());
    }

    private void setStatus(String text) {
        status.setText(text);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
