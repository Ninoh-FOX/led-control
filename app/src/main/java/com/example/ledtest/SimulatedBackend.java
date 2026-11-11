package com.example.ledtest;

/** Backend de simulación: no toca hardware, solo recuerda el último color. */
public class SimulatedBackend implements LedBackend {

    private int last = 0;

    @Override
    public String name() {
        return "Simulado";
    }

    @Override
    public String apply(int argb) {
        last = argb;
        return null;
    }

    public int lastColor() {
        return last;
    }
}
