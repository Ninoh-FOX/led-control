#!/system/bin/sh
# control_led.sh - GBA-style battery LED.
# Supports KONKR Pocket Advance (AW2033) and Pocket Block (AW2023).
#   100-21 %  -> solid green
#    20-11 %  -> solid red
#    10-0  %  -> blinking red
# KONKR needs root (SELinux denies the LED nodes to shell and apps).
# Pocket Block works from adb shell (SELinux is permissive there).
# Usage: sh control_led.sh        (detaches itself to the background)
#        sh control_led.sh stop   (stops the running instance)

AW2033=/sys/bus/i2c/drivers/aw2033_led/6-0045/leds
AW2023=/sys/bus/i2c/drivers/aw2023/5-0045
BATTERY=/sys/class/power_supply/battery/capacity
PIDFILE=/data/local/tmp/control_led.pid

if [ -d "$AW2033" ]; then
    LEDS=$AW2033
    RED=$LEDS/red/brightness GREEN=$LEDS/green/brightness BLUE=$LEDS/blue/brightness
    ON=$(cat "$LEDS/red/max_brightness" 2>/dev/null || echo 255)
elif [ -d "$AW2023" ]; then
    LEDS=$AW2023
    RED=$LEDS/red GREEN=$LEDS/green BLUE=$LEDS/blue
    ON=10 # same level the system uses for its charging light
else
    echo "control_led: no supported LED chip found" >&2
    exit 1
fi

if [ "$1" = "stop" ]; then
    [ -f "$PIDFILE" ] && kill "$(cat "$PIDFILE")" 2>/dev/null
    rm -f "$PIDFILE"
    exit 0
fi

if ! { echo 0 > "$BLUE"; } 2>/dev/null; then
    echo "control_led: cannot write to $LEDS (root required)" >&2
    exit 1
fi

# Relaunch detached so it survives the terminal / adb session closing.
if [ "$1" != "daemon" ]; then
    [ -f "$PIDFILE" ] && kill "$(cat "$PIDFILE")" 2>/dev/null
    nohup sh "$0" daemon >/dev/null 2>&1 &
    echo "control_led: running in background (pid $!)"
    exit 0
fi

echo $$ > "$PIDFILE"

set_led() { # set_led <red> <green>
    echo "$1" > "$RED"
    echo "$2" > "$GREEN"
    echo 0 > "$BLUE"
}

trap 'rm -f "$PIDFILE"; exit 0' TERM INT

# The color is rewritten on every pass because the system BatteryService
# also drives this LED and would otherwise overwrite it.
while true; do
    level=$(cat "$BATTERY" 2>/dev/null)
    if [ "${level:-100}" -gt 20 ]; then
        set_led 0 "$ON"
        sleep 2
    elif [ "$level" -gt 10 ]; then
        set_led "$ON" 0
        sleep 2
    else
        set_led "$ON" 0
        sleep 0.5
        set_led 0 0
        sleep 0.5
    fi
done
