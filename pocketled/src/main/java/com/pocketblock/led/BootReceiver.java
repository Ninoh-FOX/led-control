package com.pocketblock.led;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restarts the LED service after the console boots or the app is updated. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Only boot and app-update broadcasts are declared in the manifest.
        if (new Config(context).enabled()) {
            LedService.start(context, -1);
        }
    }
}
