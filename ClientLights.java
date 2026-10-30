package com.lightutils;

import java.util.List;

/** Copia de las luces que recibe el cliente (solo datos, sin clases de render). */
public class ClientLights {
    public static volatile List<LightData> lights = List.of();

    public static void set(List<LightData> l) {
        lights = List.copyOf(l);
    }
}
