#!/usr/bin/env bash
# Compila una APK sin Gradle, con build-tools del SDK local.
# Uso: ./build.sh            -> app de prueba KONKR: build/led-test.apk
#      ./build.sh pocketled  -> app Pocket Block:    build-pocketled/pocketled.apk
# Firmadas con la clave de depuración debug.keystore (fuera de build/ para que
# la firma no cambie entre compilaciones y se pueda actualizar con adb install -r).
set -euo pipefail

SDK="${ANDROID_SDK:-$HOME/Android/Sdk}"
BT="$SDK/build-tools/36.0.0"
PLATFORM="$SDK/platforms/android-36/android.jar"
ROOT="$(cd "$(dirname "$0")" && pwd)"
MODULE="${1:-app}"
APP="$ROOT/$MODULE/src/main"
if [ "$MODULE" = app ]; then
    BUILD="$ROOT/build"; APK="$BUILD/led-test.apk"
else
    BUILD="$ROOT/build-$MODULE"; APK="$BUILD/$MODULE.apk"
fi
KEYSTORE="$ROOT/debug.keystore"

rm -rf "$BUILD"
mkdir -p "$BUILD/classes" "$BUILD/dex"

# 1. Manifiesto y recursos empaquetados (res/ es opcional)
RES_ARGS=()
if [ -d "$APP/res" ]; then
    "$BT/aapt2" compile --dir "$APP/res" -o "$BUILD/res.zip"
    RES_ARGS=("$BUILD/res.zip")
fi
"$BT/aapt2" link -I "$PLATFORM" \
    --manifest "$APP/AndroidManifest.xml" \
    --min-sdk-version 26 --target-sdk-version 31 \
    -o "$BUILD/base.apk" "${RES_ARGS[@]}"

# 2. Compilar Java
javac --release 11 -classpath "$PLATFORM" -d "$BUILD/classes" \
    $(find "$APP/java" -name '*.java')

# 3. Convertir a DEX
"$BT/d8" --release --lib "$PLATFORM" --min-api 26 --output "$BUILD/dex" \
    $(find "$BUILD/classes" -name '*.class')

# 4. Añadir classes.dex al APK
python3 - "$BUILD" <<'PY'
import sys, zipfile
b = sys.argv[1]
with zipfile.ZipFile(f"{b}/base.apk") as src, \
     zipfile.ZipFile(f"{b}/merged.apk", "w", zipfile.ZIP_DEFLATED) as out:
    for item in src.infolist():
        out.writestr(item, src.read(item.filename))
    out.write(f"{b}/dex/classes.dex", "classes.dex")
PY

# 5. Alinear y firmar con clave de depuración
"$BT/zipalign" -f 4 "$BUILD/merged.apk" "$BUILD/aligned.apk"
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -keystore "$KEYSTORE" -storepass android -keypass android \
        -alias androiddebugkey -dname "CN=Debug" -keyalg RSA -keysize 2048 \
        -validity 10000 >/dev/null
fi
"$BT/apksigner" sign --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android \
    --ks-key-alias androiddebugkey --out "$APK" "$BUILD/aligned.apk"

echo "APK generada: $APK"
