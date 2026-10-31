package com.banguzi.tv.cctv;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Non-focusable, hardware-accelerated live CCTV preview card rendered on the TV Home dashboard.
 * Never intercepts remote keys or focus, and releases its ExoPlayer decoder immediately when hidden.
 */
final class DashboardPreview {
    private static final int MARGIN_X_DP = 58;
    private static final int MARGIN_TOP_LEFT_DP = 122;
    private static final int MARGIN_TOP_RIGHT_DP = 104;

    private final Context context;
    private final WindowManager wm;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final RtspPlayer player;

    private FrameLayout root;
    private FrameLayout inner;
    private TextureView video;
    private TextView placeholder;
    private View liveDot;
    private TextView badgeText;
    private CctvConfig activeConfig;
    private boolean showing;

    DashboardPreview(Context context) {
        this.context = context;
        this.wm = context.getSystemService(WindowManager.class);
        this.player = new RtspPlayer(context, false, new RtspPlayer.Listener() {
            @Override
            public void onConnecting(boolean retrying) {
                if (!showing) return;
                setIndicator(false, retrying ? "Menyambung ulang…" : "Menyambung CCTV…");
            }

            @Override
            public void onFirstFrame() {
                if (!showing || video == null) return;
                if (placeholder != null) {
                    placeholder.setVisibility(View.GONE);
                }
                setIndicator(true, null);
                video.animate().alpha(1f).setDuration(220).start();
            }

            @Override
            public void onVideoAspectChanged(float widthOverHeight) {
                // Preview container stays 16:9 to match standard HD camera feeds cleanly.
            }

            @Override
            public void onDisconnected(String reason) {
                if (!showing) return;
                if (video != null) {
                    video.setAlpha(0f);
                }
                setIndicator(false, reason + "…");
            }
        });
    }

    void show(CctvConfig config) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post(() -> show(config));
            return;
        }
        if (config == null || !config.homePreview || !config.isConfigured()) {
            hide();
            return;
        }
        if (!Settings.canDrawOverlays(context)) {
            Log.w(CctvConfig.TAG, "overlay permission not granted yet");
            hide();
            return;
        }

        if (showing && activeConfig != null && sameLayoutAndStream(activeConfig, config)) {
            return;
        }

        hide();
        activeConfig = config;
        buildView(config);

        try {
            wm.addView(root, createLayoutParams(config));
            showing = true;
            player.start(config.rtspUrl, video, true);
        } catch (RuntimeException e) {
            Log.w(CctvConfig.TAG, "preview window add failed: " + e.getMessage());
            root = null;
            video = null;
            showing = false;
        }
    }

    void hide() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post(this::hide);
            return;
        }
        player.stop();
        showing = false;
        activeConfig = null;
        if (root != null) {
            View old = root;
            root = null;
            inner = null;
            video = null;
            placeholder = null;
            liveDot = null;
            badgeText = null;
            try {
                wm.removeViewImmediate(old);
            } catch (RuntimeException ignored) {
                // Already detached.
            }
        }
    }

    private boolean sameLayoutAndStream(CctvConfig a, CctvConfig b) {
        return a.rtspUrl.equals(b.rtspUrl)
                && a.cameraName.equals(b.cameraName)
                && a.previewPosition.equals(b.previewPosition)
                && a.previewSize.equals(b.previewSize);
    }

    private void buildView(CctvConfig config) {
        int borderPad = dp(2);
        float innerRadius = dp(10);

        root = new FrameLayout(context);
        root.setBackgroundResource(R.drawable.preview_frame);
        root.setPadding(borderPad, borderPad, borderPad, borderPad);

        inner = new FrameLayout(context);
        inner.setBackgroundColor(0xFF061119);
        inner.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), innerRadius);
            }
        });
        inner.setClipToOutline(true);

        video = new TextureView(context);
        video.setOpaque(false);
        video.setAlpha(0f);
        inner.addView(video, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        placeholder = new TextView(context);
        placeholder.setText("Menyambung CCTV…");
        placeholder.setTextSize(12f);
        placeholder.setTextColor(0xCCE4D497);
        placeholder.setGravity(Gravity.CENTER);
        inner.addView(placeholder, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        LinearLayout badge = new LinearLayout(context);
        badge.setOrientation(LinearLayout.HORIZONTAL);
        badge.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(0xB307151E);
        pillBg.setCornerRadius(dp(10));
        pillBg.setStroke(Math.max(1, dp(1)), 0x33E4D497);
        badge.setBackground(pillBg);
        badge.setPadding(dp(8), dp(3), dp(9), dp(3));

        liveDot = new View(context);
        liveDot.setBackground(circle(0xFFE4D497));
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(7), dp(7));
        dotLp.rightMargin = dp(6);
        badge.addView(liveDot, dotLp);

        badgeText = new TextView(context);
        badgeText.setText("LIVE · " + config.cameraName);
        badgeText.setTextSize(10.5f);
        badgeText.setTypeface(Typeface.DEFAULT_BOLD);
        badgeText.setTextColor(0xF2FFFFFF);
        badgeText.setLetterSpacing(0.06f);
        badge.addView(badgeText);

        @SuppressLint("RtlHardcoded")
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.LEFT);
        badgeLp.setMargins(dp(8), dp(8), dp(8), dp(8));
        inner.addView(badge, badgeLp);

        root.addView(inner, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void setIndicator(boolean isLive, String statusMessage) {
        if (liveDot != null) {
            liveDot.setBackground(circle(isLive ? 0xFF4ADE80 : 0xFFE4D497));
        }
        if (badgeText != null && activeConfig != null) {
            badgeText.setText((isLive ? "LIVE · " : "") + activeConfig.cameraName);
        }
        if (placeholder != null && statusMessage != null) {
            placeholder.setText(statusMessage);
            placeholder.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("RtlHardcoded")
    private WindowManager.LayoutParams createLayoutParams(CctvConfig config) {
        int width = dp(config.widthDp());
        int height = dp(config.heightDp());
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        boolean right = CctvConfig.POS_RIGHT.equals(config.previewPosition);
        p.gravity = Gravity.TOP | (right ? Gravity.RIGHT : Gravity.LEFT);
        p.x = dp(MARGIN_X_DP);
        p.y = dp(right ? MARGIN_TOP_RIGHT_DP : MARGIN_TOP_LEFT_DP);
        p.setTitle("CCTV Live — Dashboard Preview");
        return p;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
