package com.banguzi.tv.cctv;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Observes foreground window transitions (zero polling, no remote key filtering) so the live
 * CCTV preview is shown exclusively on the Clean Your TV Home dashboard and immediately stops
 * whenever another app, settings screen, Adhan display, or screensaver takes the screen.
 */
public class HomeWatchService extends AccessibilityService {
    private static final String HOME_PKG = "com.gathin.tvhome";
    private static final String HOME_ACTIVITY = "com.gathin.tvhome.MainActivity";

    private static volatile HomeWatchService instance;
    private static volatile boolean fullscreenOpen;

    private final Handler main = new Handler(Looper.getMainLooper());
    private DashboardPreview preview;
    private boolean onHome;
    private boolean screenOn = true;
    private boolean dreaming;
    private boolean receiverRegistered;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent == null ? null : intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                screenOn = false;
                syncPreview();
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                screenOn = true;
                syncPreview();
            } else if (Intent.ACTION_DREAMING_STARTED.equals(action)) {
                dreaming = true;
                syncPreview();
            } else if (Intent.ACTION_DREAMING_STOPPED.equals(action)) {
                dreaming = false;
                syncPreview();
            }
        }
    };

    static boolean isBound() {
        return instance != null;
    }

    static void setFullscreenOpen(boolean open) {
        fullscreenOpen = open;
        HomeWatchService svc = instance;
        if (svc == null) return;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (!open) {
                svc.recheckActiveWindowDelayed();
            }
            svc.syncPreview();
        } else {
            svc.main.post(() -> {
                if (!open) {
                    svc.recheckActiveWindowDelayed();
                }
                svc.syncPreview();
            });
        }
    }

    static void reloadConfig(Context context) {
        HomeWatchService svc = instance;
        if (svc == null) return;
        svc.main.post(svc::forceRefresh);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        preview = new DashboardPreview(this);

        PowerManager pm = getSystemService(PowerManager.class);
        screenOn = pm == null || pm.isInteractive();
        dreaming = false;

        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_DREAMING_STARTED);
            filter.addAction(Intent.ACTION_DREAMING_STOPPED);
            registerReceiver(stateReceiver, filter);
            receiverRegistered = true;
        }

        detectActiveWindowOnce();
        syncPreview();
        Log.i(CctvConfig.TAG, "home watch connected (onHome=" + onHome + ")");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) return;
        String pkg = pkgCs.toString();
        String cls = event.getClassName() == null ? "" : event.getClassName().toString();

        // Ignore our own non-Activity overlay window so adding DashboardPreview does not hide itself.
        if (getPackageName().equals(pkg)) {
            if (cls.contains("MainActivity") || cls.contains("Dialog")) {
                onHome = false;
                syncPreview();
            }
            return;
        }

        // Ignore transient volume HUD / toasts while on Home.
        if ("com.android.systemui".equals(pkg) && (cls.contains("Volume") || cls.contains("Toast"))) {
            return;
        }

        if (HOME_PKG.equals(pkg)) {
            if (HOME_ACTIVITY.equals(cls)) {
                onHome = true;
            } else if (cls.endsWith("Activity")) {
                onHome = false;
            } else if (cls.contains("Dialog")) {
                // Hide while a launcher dialog (e.g. App library or module card prompt) is up,
                // then re-check once it closes.
                onHome = false;
            } else {
                // Popup dismissal or root frame restoration inside tvhome: verify active window.
                recheckActiveWindowDelayed();
                return;
            }
        } else {
            onHome = false;
        }
        syncPreview();
    }

    @Override
    public void onInterrupt() {
        if (preview != null) {
            preview.hide();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        cleanup();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        cleanup();
        super.onDestroy();
    }

    private void cleanup() {
        instance = null;
        main.removeCallbacksAndMessages(null);
        if (receiverRegistered) {
            try {
                unregisterReceiver(stateReceiver);
            } catch (RuntimeException ignored) {
            }
            receiverRegistered = false;
        }
        if (preview != null) {
            preview.hide();
            preview = null;
        }
    }

    private void forceRefresh() {
        if (preview != null) {
            preview.hide();
        }
        detectActiveWindowOnce();
        syncPreview();
    }

    private void recheckActiveWindowDelayed() {
        main.postDelayed(() -> {
            detectActiveWindowOnce();
            syncPreview();
        }, 250);
    }

    private void detectActiveWindowOnce() {
        AccessibilityNodeInfo root = null;
        try {
            root = getRootInActiveWindow();
            if (root != null && root.getPackageName() != null) {
                String activePkg = root.getPackageName().toString();
                onHome = HOME_PKG.equals(activePkg);
            }
        } catch (RuntimeException ignored) {
        } finally {
            if (root != null) {
                //noinspection deprecation
                root.recycle();
            }
        }
    }

    private void syncPreview() {
        if (preview == null) return;
        if (onHome && screenOn && !dreaming && !fullscreenOpen) {
            preview.show(CctvConfig.load(this));
        } else {
            preview.hide();
        }
    }
}
