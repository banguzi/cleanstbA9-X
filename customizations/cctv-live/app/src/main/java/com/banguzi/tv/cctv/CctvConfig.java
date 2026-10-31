package com.banguzi.tv.cctv;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Stores RTSP stream and dashboard preview preferences in app-private storage.
 * Raw URLs are never written to logcat so camera IPs and tokens remain private.
 */
final class CctvConfig {
    static final String TAG = "CctvLive";
    private static final String PREFS = "cctv_prefs";
    static final String PUSHED_FILE = "pushed-config.json";

    static final String POS_LEFT = "left";
    static final String POS_RIGHT = "right";

    static final String SIZE_SMALL = "small";
    static final String SIZE_MEDIUM = "medium";
    static final String SIZE_LARGE = "large";

    final String rtspUrl;
    final String cameraName;
    final boolean homePreview;
    final String previewPosition;
    final String previewSize;

    CctvConfig(String rtspUrl, String cameraName, boolean homePreview,
               String previewPosition, String previewSize) {
        this.rtspUrl = rtspUrl == null ? "" : rtspUrl.trim();
        this.cameraName = (cameraName == null || cameraName.trim().isEmpty()) ? "CCTV" : cameraName.trim();
        this.homePreview = homePreview;
        this.previewPosition = POS_RIGHT.equals(previewPosition) ? POS_RIGHT : POS_LEFT;
        if (SIZE_SMALL.equals(previewSize) || SIZE_LARGE.equals(previewSize)) {
            this.previewSize = previewSize;
        } else {
            this.previewSize = SIZE_MEDIUM;
        }
    }

    boolean isConfigured() {
        if (rtspUrl.isEmpty() || "RTSP_URL".equals(rtspUrl) || rtspUrl.contains("<")) {
            return false;
        }
        return rtspUrl.startsWith("rtsp://") || rtspUrl.startsWith("rtsps://");
    }

    int widthDp() {
        switch (previewSize) {
            case SIZE_SMALL:
                return 320;
            case SIZE_LARGE:
                return 432;
            default:
                return 384;
        }
    }

    int heightDp() {
        return widthDp() * 9 / 16;
    }

    String sizeLabel() {
        switch (previewSize) {
            case SIZE_SMALL:
                return "Kecil";
            case SIZE_LARGE:
                return "Besar";
            default:
                return "Sedang";
        }
    }

    String nextSize() {
        switch (previewSize) {
            case SIZE_SMALL:
                return SIZE_MEDIUM;
            case SIZE_MEDIUM:
                return SIZE_LARGE;
            default:
                return SIZE_SMALL;
        }
    }

    String positionLabel() {
        return POS_RIGHT.equals(previewPosition) ? "Kanan" : "Kiri";
    }

    String nextPosition() {
        return POS_RIGHT.equals(previewPosition) ? POS_LEFT : POS_RIGHT;
    }

    static CctvConfig load(Context context) {
        importIfPresent(context);
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String defaultUrl = context.getString(R.string.rtsp_url).trim();
        if ("RTSP_URL".equals(defaultUrl)) {
            defaultUrl = "";
        }
        String defaultName = context.getString(R.string.camera_name).trim();
        return new CctvConfig(
                sp.getString("rtspUrl", defaultUrl),
                sp.getString("cameraName", defaultName),
                sp.getBoolean("homePreview", true),
                sp.getString("previewPosition", POS_LEFT),
                sp.getString("previewSize", SIZE_MEDIUM)
        );
    }

    static void save(Context context, CctvConfig c) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString("rtspUrl", c.rtspUrl)
                .putString("cameraName", c.cameraName)
                .putBoolean("homePreview", c.homePreview)
                .putString("previewPosition", c.previewPosition)
                .putString("previewSize", c.previewSize)
                .apply();
    }

    static boolean importIfPresent(Context context) {
        File file = new File(context.getFilesDir(), PUSHED_FILE);
        if (!file.isFile()) {
            return false;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (FileInputStream in = new FileInputStream(file)) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            }
            // Delete staged file immediately after reading into preferences.
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            JSONObject json = new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
            SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor ed = sp.edit();
            if (json.has("rtspUrl")) {
                ed.putString("rtspUrl", json.optString("rtspUrl", "").trim());
            }
            if (json.has("cameraName")) {
                ed.putString("cameraName", json.optString("cameraName", "CCTV").trim());
            }
            if (json.has("homePreview")) {
                ed.putBoolean("homePreview", json.optBoolean("homePreview", true));
            }
            if (json.has("previewPosition")) {
                ed.putString("previewPosition", json.optString("previewPosition", POS_LEFT));
            }
            if (json.has("previewSize")) {
                ed.putString("previewSize", json.optString("previewSize", SIZE_MEDIUM));
            }
            ed.commit();
            Log.i(TAG, "config imported");
            return true;
        } catch (Exception e) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            Log.w(TAG, "config import failed: " + e.getClass().getSimpleName());
            return false;
        }
    }
}
