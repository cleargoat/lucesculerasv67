# LightUtils Color v2 — Minecraft Forge 1.20.1

Versión **2.0.0** para Minecraft **1.20.1**, Forge **47.x** y Java **17**.
El archivo generado se llama `lightutils-color-v2-2.0.0.jar`.

## Compilar localmente

Con JDK 17 y Gradle 8.8:

```bash
gradle --no-daemon build
```

El JAR se genera en `build/libs/`.

## GitHub Actions

1. Sube el contenido de esta carpeta a la raíz de un repositorio.
2. En GitHub, abre **Actions**.
3. El workflow **Build LightUtils Color v2** compila en cada `push` o `pull_request`; también puedes iniciarlo manualmente con **Run workflow**.
4. Descarga el artefacto **lightutils-color-v2** al terminar.

## Comandos

Requieren permiso OP 2:

```text
/lightutils add <color> <intensidad 1-15> <radio 1-64> <tag>
/lightutils clear <tag>
```

Colores: rojo, verde, azul, amarillo, naranja, morado, rosa, cian, blanco o hexadecimal (ej. `ff8800`).
