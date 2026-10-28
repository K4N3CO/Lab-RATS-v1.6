package com.labs.labrats;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class OemStabilityHelper {
    private static final String TAG = "OemStabilityHelper";

    public static void requestBatteryOptimizationBypass(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.os.PowerManager pm = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (pm != null && !pm.isIgnoringBatteryOptimizations(context.getPackageName())) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
                    context.startActivity(intent);
                    FirebaseConfig.logActivity("STABILITY_FIX: Requested Battery Optimization Bypass for " + Build.MANUFACTURER);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Battery Bypass Error: " + e.getMessage());
        }
    }

    public static void requestAutoStart(Context context) {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        List<Intent> intents = new ArrayList<>();

        if (manufacturer.contains("xiaomi")) {
            intents.add(new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")));
        } else if (manufacturer.contains("oppo") || manufacturer.contains("realme")) {
            intents.add(new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")));
            intents.add(new Intent().setComponent(new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")));
            intents.add(new Intent().setComponent(new ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaurd.PowerUsageModelActivity")));
        } else if (manufacturer.contains("vivo")) {
            intents.add(new Intent().setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")));
            intents.add(new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")));
        } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            intents.add(new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")));
            intents.add(new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")));
        } else if (manufacturer.contains("samsung")) {
            intents.add(new Intent().setComponent(new ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")));
            intents.add(new Intent().setComponent(new ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.ram.AutoRunActivity")));
            intents.add(new Intent().setComponent(new ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")));
        }

        boolean success = false;
        for (Intent intent : intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (isIntentCallable(context, intent)) {
                    context.startActivity(intent);
                    FirebaseConfig.logActivity("STABILITY_FIX: Launched OEM Auto-start menu for " + Build.MANUFACTURER);
                    success = true;
                    break;
                }
            } catch (Exception ignored) {}
        }

        if (!success) {
            success = discoverHeuristicAutoStart(context);
        }

        if (!success) {
            // Fallback to App Details if no OEM specific menu found
            try {
                Intent fallback = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                fallback.setData(android.net.Uri.parse("package:" + context.getPackageName()));
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallback);
                FirebaseConfig.logActivity("STABILITY_FIX: Falling back to App Details for " + Build.MANUFACTURER);
            } catch (Exception ignored) {}
        }
    }

    private static boolean discoverHeuristicAutoStart(Context context) {
        PackageManager pm = context.getPackageManager();
        List<android.content.pm.PackageInfo> installedPackages = pm.getInstalledPackages(0);
        
        for (android.content.pm.PackageInfo pkg : installedPackages) {
            String pName = pkg.packageName.toLowerCase();
            // Skip stock android/google packages
            if (pName.contains("android") || pName.contains("google")) continue;
            
            // Look for security/manager packages
            if (pName.contains("security") || pName.contains("manager") || pName.contains("powersave") || pName.contains("cleaner")) {
                try {
                    android.content.pm.ActivityInfo[] activities = pm.getPackageInfo(pkg.packageName, PackageManager.GET_ACTIVITIES).activities;
                    if (activities != null) {
                        for (android.content.pm.ActivityInfo act : activities) {
                            String aName = act.name.toLowerCase();
                            // Score based on keywords
                            if (aName.contains("autostart") || aName.contains("startup") || aName.contains("bgstartup") || aName.contains("autorun")) {
                                Intent intent = new Intent();
                                intent.setComponent(new ComponentName(pkg.packageName, act.name));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                                FirebaseConfig.logActivity("STABILITY_HEURISTIC: Found hidden OEM menu in " + pkg.packageName);
                                return true;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    private static boolean isIntentCallable(Context context, Intent intent) {
        return context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }
}
