package com.banguzi.tv.cctv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

/**
 * Full-screen live CCTV viewer with auto-hiding D-pad controls for audio toggle
 * and Home dashboard preview configuration.
 */
public class MainActivity extends Activity {
    private static final long HUD_HIDE_DELAY_MS = 6000L;

    private final Handler main = new Handler(Looper.getMainLooper());
    private RtspPlayer player;
    private CctvConfig config;

    private FrameLayout root;
    private FrameLayout videoBox;
    private TextureView videoView;
    private TextView centerStatus;
    private FrameLayout hudContainer;
    private View liveDot;
    private TextView titleView;
    private TextView subStatusView;

    private TextView btnAudio;
    private TextView btnHomePreview;
    private TextView btnPosition;
    private TextView btnSize;
    private TextView btnReload;
    private TextView btnStream;
    private TextView btnPermBanner;

    private float videoAspect = 16f / 9f;
    private boolean isLive;
    private boolean hudVisible = true;
    private boolean swallowWakeUp;

    private final Runnable hideHudTask = () -> {
        if (isLive) {
            setHudVisible(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        config = CctvConfig.load(this);

        buildUi();
        setContentView(root);

        player = new RtspPlayer(this, true, new RtspPlayer.Listener() {
            @Override
            public void onConnecting(boolean retrying) {
                isLive = false;
                setLiveVisuals(false, retrying ? "Menyambung ulang ke kamera…" : "Menyambung ke kamera…");
                showHudTemporarily();
            }

            @Override
            public void onFirstFrame() {
                isLive = true;
                centerStatus.setVisibility(View.GONE);
                setLiveVisuals(true, "Langsung · RTSP LAN");
                videoView.animate().alpha(1f).setDuration(240).start();
                scheduleHudHide();
            }

            @Override
            public void onVideoAspectChanged(float widthOverHeight) {
                videoAspect = widthOverHeight;
                fitVideoBox();
            }

            @Override
            public void onDisconnected(String reason) {
                isLive = false;
                videoView.setAlpha(0f);
                setLiveVisuals(false, reason + " · mencoba ulang…");
                showHudTemporarily();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Release the Home preview's hardware decoder synchronously before opening full-screen video.
        HomeWatchService.setFullscreenOpen(true);
        config = CctvConfig.load(this);
        refreshLabels();
        showHudTemporarily();

        if (config.isConfigured()) {
            videoView.setAlpha(0f);
            player.start(config.rtspUrl, videoView, player.isMuted());
        } else {
            isLive = false;
            videoView.setAlpha(0f);
            setLiveVisuals(false, "URL RTSP belum diatur — pilih 'Atur Stream'");
        }
    }

    @Override
    protected void onPause() {
        main.removeCallbacks(hideHudTask);
        if (player != null) {
            player.stop();
        }
        HomeWatchService.setFullscreenOpen(false);
        super.onPause();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        if (code == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                finish();
            }
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (!hudVisible) {
                swallowWakeUp = true;
                showHudTemporarily();
                btnAudio.requestFocus();
                return true;
            }
            swallowWakeUp = false;
            scheduleHudHide();
        } else if (event.getAction() == KeyEvent.ACTION_UP && swallowWakeUp) {
            swallowWakeUp = false;
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @SuppressLint("RtlHardcoded")
    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        videoBox = new FrameLayout(this);
        videoView = new TextureView(this);
        videoView.setAlpha(0f);
        videoBox.addView(videoView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(videoBox, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER));

        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> fitVideoBox());

        centerStatus = new TextView(this);
        centerStatus.setText("Menyambung ke kamera…");
        centerStatus.setTextSize(18f);
        centerStatus.setTextColor(0xFFE4D497);
        centerStatus.setGravity(Gravity.CENTER);
        root.addView(centerStatus, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        hudContainer = new FrameLayout(this);

        // Unified bottom HUD bar (keeps top-left camera OSD timestamp unobstructed)
        LinearLayout bottomWrap = new LinearLayout(this);
        bottomWrap.setOrientation(LinearLayout.VERTICAL);
        bottomWrap.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xEC050E15, 0xB3050E15, 0x00050E15}));
        bottomWrap.setPadding(dp(40), dp(42), dp(40), dp(28));

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        headerLp.bottomMargin = dp(14);

        liveDot = new View(this);
        liveDot.setBackground(circle(0xFFE4D497));
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(9), dp(9));
        dotLp.rightMargin = dp(10);
        headerRow.addView(liveDot, dotLp);

        titleView = new TextView(this);
        titleView.setTextSize(18f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(0xFFFFFFFF);
        headerRow.addView(titleView);

        subStatusView = new TextView(this);
        subStatusView.setTextSize(13f);
        subStatusView.setTextColor(0xCCE4D497);
        subStatusView.setPadding(dp(12), 0, 0, 0);
        headerRow.addView(subStatusView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextClock clock = new TextClock(this);
        clock.setTimeZone("Asia/Jayapura");
        clock.setFormat12Hour("HH:mm 'WIT'");
        clock.setFormat24Hour("HH:mm 'WIT'");
        clock.setTextSize(18f);
        clock.setTypeface(Typeface.DEFAULT_BOLD);
        clock.setTextColor(0xFFE4D497);
        headerRow.addView(clock);

        bottomWrap.addView(headerRow, headerLp);

        btnPermBanner = pillButton("");
        btnPermBanner.setVisibility(View.GONE);
        LinearLayout.LayoutParams permLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        permLp.bottomMargin = dp(10);
        bottomWrap.addView(btnPermBanner, permLp);

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);

        btnAudio = pillButton("Suara: Mati");
        btnAudio.setOnClickListener(v -> {
            player.setMuted(!player.isMuted());
            refreshLabels();
            scheduleHudHide();
        });
        addControl(controls, btnAudio);

        btnHomePreview = pillButton("Preview Home: Aktif");
        btnHomePreview.setOnClickListener(v -> {
            config = new CctvConfig(config.rtspUrl, config.cameraName,
                    !config.homePreview, config.previewPosition, config.previewSize);
            CctvConfig.save(this, config);
            HomeWatchService.reloadConfig(this);
            refreshLabels();
            scheduleHudHide();
        });
        addControl(controls, btnHomePreview);

        btnPosition = pillButton("Posisi: Kiri");
        btnPosition.setOnClickListener(v -> {
            config = new CctvConfig(config.rtspUrl, config.cameraName,
                    config.homePreview, config.nextPosition(), config.previewSize);
            CctvConfig.save(this, config);
            HomeWatchService.reloadConfig(this);
            refreshLabels();
            scheduleHudHide();
        });
        addControl(controls, btnPosition);

        btnSize = pillButton("Ukuran: Sedang");
        btnSize.setOnClickListener(v -> {
            config = new CctvConfig(config.rtspUrl, config.cameraName,
                    config.homePreview, config.previewPosition, config.nextSize());
            CctvConfig.save(this, config);
            HomeWatchService.reloadConfig(this);
            refreshLabels();
            scheduleHudHide();
        });
        addControl(controls, btnSize);

        btnReload = pillButton("Muat Ulang");
        btnReload.setOnClickListener(v -> {
            if (config.isConfigured()) {
                videoView.setAlpha(0f);
                player.start(config.rtspUrl, videoView, player.isMuted());
            }
            scheduleHudHide();
        });
        addControl(controls, btnReload);

        btnStream = pillButton("Atur Stream");
        btnStream.setOnClickListener(v -> openStreamDialog());
        addControl(controls, btnStream);

        scroll.addView(controls);
        bottomWrap.addView(scroll);

        TextView hint = new TextView(this);
        hint.setText("Tekan Back untuk kembali ke Dashboard · OK untuk mengubah pengaturan");
        hint.setTextSize(12f);
        hint.setTextColor(0x99FFFFFF);
        hint.setPadding(dp(4), dp(10), 0, 0);
        bottomWrap.addView(hint);

        hudContainer.addView(bottomWrap, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM));

        root.addView(hudContainer, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        btnAudio.requestFocus();
    }

    private TextView pillButton(String label) {
        TextView b = new TextView(this);
        b.setText(label);
        b.setTextSize(13.5f);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setFocusable(true);
        b.setClickable(true);
        b.setBackgroundResource(R.drawable.pill_bg);
        b.setPadding(dp(16), dp(9), dp(16), dp(9));
        ColorStateList colors = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_focused}, new int[]{}},
                new int[]{0xFF07151E, 0xF2FFFFFF});
        b.setTextColor(colors);
        return b;
    }

    private void addControl(LinearLayout parent, View child) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(10);
        parent.addView(child, lp);
    }

    private void refreshLabels() {
        titleView.setText("LIVE · " + config.cameraName);
        boolean muted = player == null || player.isMuted();
        btnAudio.setText(muted ? "Suara: Mati" : "Suara: Aktif");
        btnHomePreview.setText("Preview Home: " + (config.homePreview ? "Aktif" : "Nonaktif"));
        btnPosition.setText("Posisi: " + config.positionLabel());
        btnSize.setText("Ukuran: " + config.sizeLabel());

        if (config.homePreview && !Settings.canDrawOverlays(this)) {
            btnPermBanner.setVisibility(View.VISIBLE);
            btnPermBanner.setText("Izin Tampil di Atas Aplikasi belum aktif — klik untuk mengaktifkan");
            btnPermBanner.setOnClickListener(v -> startActivity(new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()))));
        } else if (config.homePreview && !HomeWatchService.isBound()) {
            btnPermBanner.setVisibility(View.VISIBLE);
            btnPermBanner.setText("Layanan Deteksi Home belum aktif — klik untuk membuka Aksesibilitas");
            btnPermBanner.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        } else {
            btnPermBanner.setVisibility(View.GONE);
        }
    }

    private void setLiveVisuals(boolean live, String status) {
        liveDot.setBackground(circle(live ? 0xFF4ADE80 : 0xFFE4D497));
        subStatusView.setText(status);
        if (!live) {
            centerStatus.setText(status);
            centerStatus.setVisibility(View.VISIBLE);
        }
    }

    private void fitVideoBox() {
        int rw = root.getWidth();
        int rh = root.getHeight();
        if (rw <= 0 || rh <= 0 || videoAspect <= 0f) return;
        int w = rw;
        int h = Math.round(w / videoAspect);
        if (h > rh) {
            h = rh;
            w = Math.round(h * videoAspect);
        }
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) videoBox.getLayoutParams();
        if (lp.width != w || lp.height != h) {
            lp.width = w;
            lp.height = h;
            videoBox.setLayoutParams(lp);
        }
    }

    private void showHudTemporarily() {
        setHudVisible(true);
        scheduleHudHide();
    }

    private void scheduleHudHide() {
        main.removeCallbacks(hideHudTask);
        if (isLive) {
            main.postDelayed(hideHudTask, HUD_HIDE_DELAY_MS);
        }
    }

    private void setHudVisible(boolean visible) {
        if (hudVisible == visible) return;
        hudVisible = visible;
        hudContainer.animate().cancel();
        if (visible) {
            hudContainer.setVisibility(View.VISIBLE);
            hudContainer.animate().alpha(1f).setDuration(180).start();
        } else {
            hudContainer.animate().alpha(0f).setDuration(220)
                    .withEndAction(() -> {
                        if (!hudVisible) hudContainer.setVisibility(View.INVISIBLE);
                    }).start();
        }
    }

    private void openStreamDialog() {
        main.removeCallbacks(hideHudTask);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        form.setPadding(pad, dp(12), pad, dp(4));

        TextView nameLbl = new TextView(this);
        nameLbl.setText("Nama kamera");
        form.addView(nameLbl);

        EditText nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setText(config.cameraName);
        form.addView(nameInput);

        TextView urlLbl = new TextView(this);
        urlLbl.setText("Alamat RTSP (rtsp://…)");
        urlLbl.setPadding(0, dp(10), 0, 0);
        form.addView(urlLbl);

        EditText urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setText(config.rtspUrl);
        form.addView(urlInput);

        new AlertDialog.Builder(this)
                .setTitle("Pengaturan Stream CCTV")
                .setView(form)
                .setPositiveButton("Simpan", (d, w) -> {
                    config = new CctvConfig(
                            urlInput.getText().toString(),
                            nameInput.getText().toString(),
                            config.homePreview,
                            config.previewPosition,
                            config.previewSize);
                    CctvConfig.save(this, config);
                    HomeWatchService.reloadConfig(this);
                    refreshLabels();
                    if (config.isConfigured()) {
                        videoView.setAlpha(0f);
                        player.start(config.rtspUrl, videoView, player.isMuted());
                    } else {
                        player.stop();
                        setLiveVisuals(false, "URL RTSP belum valid");
                    }
                    scheduleHudHide();
                })
                .setNegativeButton("Batal", (d, w) -> scheduleHudHide())
                .setOnCancelListener(d -> scheduleHudHide())
                .show();
    }

    private GradientDrawable circle(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
