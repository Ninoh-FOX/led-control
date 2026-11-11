# led-control

Control del LED de batería en consolas Android de AYANEO/KONKR.

## Pocket LED (`pocketled/`)

App para la **Pocket Block** (LED AW2023). No necesita root: en esa consola
SELinux está en modo permisivo y las apps pueden escribir en los nodos del LED.

- Tres niveles de batería (normal, baja, crítica) con umbrales configurables.
- Color y parpadeo por nivel, y brillo global.
- Mientras la consola carga, el LED se deja al sistema (luz de carga).
- Arranca sola al encender la consola.

## `control_led.sh`

Versión mínima en shell (verde > 20 %, rojo > 10 %, rojo parpadeando hasta 0).
Funciona por `adb shell` en la Pocket Block; en la KONKR Pocket Advance
(LED AW2033) necesita root.

## `app/`

App de pruebas para la KONKR Pocket Advance. Sin root no puede controlar el LED.

## Compilar

Sin Gradle; requiere el SDK de Android (build-tools 36.0.0, plataforma android-36) y un JDK.

```sh
./build.sh pocketled   # build-pocketled/pocketled.apk
./build.sh             # build/led-test.apk
```
