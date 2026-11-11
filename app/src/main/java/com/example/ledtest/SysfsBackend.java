package com.example.ledtest;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Escribe la intensidad de cada canal del chip AW2033 en sysfs.
 *
 * Según las notas del proyecto, SELinux deniega esta escritura a apps y a
 * la cuenta shell. Este backend existe para comprobarlo en el dispositivo:
 * si falla, la UI muestra el error tal cual.
 */
public class SysfsBackend implements LedBackend {

    private static final String BASE = "/sys/bus/i2c/drivers/aw2033_led/6-0045/leds/";

    @Override
    public String name() {
        return "Sysfs AW2033";
    }

    @Override
    public String apply(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        try {
            write("red", r);
            write("green", g);
            write("blue", b);
            return null;
        } catch (IOException e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static void write(String channel, int value) throws IOException {
        try (FileOutputStream out = new FileOutputStream(BASE + channel + "/brightness")) {
            out.write(String.valueOf(value).getBytes("US-ASCII"));
        }
    }
}
