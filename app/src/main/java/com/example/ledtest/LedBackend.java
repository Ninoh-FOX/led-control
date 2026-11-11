package com.example.ledtest;

/**
 * Abstracción del LED. La UI solo habla con esta interfaz, así que se puede
 * cambiar el backend sin tocar la pantalla.
 */
public interface LedBackend {

    /** Nombre visible en la UI. */
    String name();

    /**
     * Aplica un color ARGB.
     *
     * @return null si funcionó, o un mensaje de error si no.
     */
    String apply(int argb);
}
