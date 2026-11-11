package com.pocketblock.led;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Pocket Block power LED (AW2023), driven through its sysfs nodes. */
final class Led {
    private static final String DIR = "/sys/bus/i2c/drivers/aw2023/5-0045/";
    private static final String[] NODES = {"red", "green", "blue"};

    private Led() {}

    static boolean available() {
        for (String node : NODES) {
            if (!new File(DIR + node).canWrite()) return false;
        }
        return true;
    }

    /** Shows a steady {red, green, blue}; returns false if any node could not be written. */
    static boolean set(int[] rgb) {
        boolean ok = true;
        for (int i = 0; i < 3; i++) ok &= write(NODES[i], rgb[i]);
        // The system clears led_switch when the charger is unplugged and when the
        // screen turns on, which keeps the LED dark whatever color is written.
        boolean lit = rgb[0] != 0 || rgb[1] != 0 || rgb[2] != 0;
        if (lit && read("led_switch") != 1) write("led_switch", 1);
        return ok;
    }

    private static boolean write(String node, int value) {
        try (FileOutputStream out = new FileOutputStream(DIR + node)) {
            out.write(Integer.toString(value).getBytes());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Returns the node's value, or -1 if it cannot be read. */
    private static int read(String node) {
        try {
            byte[] raw = Files.readAllBytes(Paths.get(DIR + node));
            return Integer.parseInt(new String(raw).trim());
        } catch (IOException | NumberFormatException e) {
            return -1;
        }
    }

    /** Reads the current {red, green, blue}, or null if the nodes cannot be read. */
    static int[] get() {
        int[] rgb = new int[3];
        for (int i = 0; i < 3; i++) {
            rgb[i] = read(NODES[i]);
            if (rgb[i] < 0) return null;
        }
        return rgb;
    }

    static boolean off() {
        return set(new int[] {0, 0, 0});
    }
}
