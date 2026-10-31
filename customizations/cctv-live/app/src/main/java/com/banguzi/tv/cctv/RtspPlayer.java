package com.banguzi.tv.cctv;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.TextureView;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;

/**
 * Low-latency RTSP live stream wrapper over Media3 ExoPlayer.
 * Automatically reconnects on network stalls or stream interruptions while active,
 * and releases the hardware video decoder immediately when stopped.
 */
@OptIn(markerClass = UnstableApi.class)
final class RtspPlayer {
    interface Listener {
        void onConnecting(boolean retrying);

        void onFirstFrame();

        void onVideoAspectChanged(float widthOverHeight);

        void onDisconnected(String reason);
    }

    private static final long CONNECT_TIMEOUT_MS = 15_000L;
    private static final long STALL_TIMEOUT_MS = 12_000L;

    private final Context context;
    private final boolean includeAudioTrack;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());

    private ExoPlayer player;
    private RtspSdpProxy proxy;
    private TextureView surface;
    private String currentUrl;
    private boolean active;
    private boolean muted = true;
    private boolean renderedFrame;
    private int retryCount;
    private long startedAt;

    private final Runnable connectWatchdog = () -> {
        if (!active || renderedFrame) return;
        Log.w(CctvConfig.TAG, "rtsp first-frame timeout; scheduling reconnect");
        scheduleReconnect("Waktu tunggu habis");
    };

    private final Runnable stallWatchdog = () -> {
        if (!active || player == null) return;
        if (player.getPlaybackState() == Player.STATE_BUFFERING) {
            Log.w(CctvConfig.TAG, "rtsp buffering stall; scheduling reconnect");
            scheduleReconnect("Stream terputus");
        }
    };

    private final Runnable reconnectTask = this::openPlayer;

    RtspPlayer(Context context, boolean includeAudioTrack, Listener listener) {
        this.context = context.getApplicationContext();
        this.includeAudioTrack = includeAudioTrack;
        this.listener = listener;
    }

    void start(String rtspUrl, TextureView targetSurface, boolean startMuted) {
        stop();
        if (rtspUrl == null || rtspUrl.isEmpty() || targetSurface == null) {
            return;
        }
        this.currentUrl = rtspUrl;
        this.surface = targetSurface;
        this.muted = startMuted;
        this.active = true;
        this.retryCount = 0;
        openPlayer();
    }

    void setMuted(boolean mute) {
        this.muted = mute;
        if (player != null && includeAudioTrack) {
            player.setVolume(mute ? 0f : 1f);
        }
    }

    boolean isMuted() {
        return muted;
    }

    void restart() {
        if (!active || currentUrl == null || surface == null) return;
        retryCount = 0;
        openPlayer();
    }

    void stop() {
        active = false;
        currentUrl = null;
        surface = null;
        main.removeCallbacks(connectWatchdog);
        main.removeCallbacks(stallWatchdog);
        main.removeCallbacks(reconnectTask);
        releasePlayerOnly();
    }

    private void openPlayer() {
        main.removeCallbacks(connectWatchdog);
        main.removeCallbacks(stallWatchdog);
        main.removeCallbacks(reconnectTask);
        releasePlayerOnly();

        if (!active || currentUrl == null || surface == null) {
            return;
        }

        renderedFrame = false;
        startedAt = SystemClock.elapsedRealtime();
        listener.onConnecting(retryCount > 0);

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(500, 2000, 250, 500)
                .build();

        player = new ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build();

        if (!includeAudioTrack) {
            player.setTrackSelectionParameters(
                    player.getTrackSelectionParameters()
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                            .build());
            player.setVolume(0f);
        } else {
            player.setVolume(muted ? 0f : 1f);
        }

        player.setVideoTextureView(surface);
        player.addListener(new Player.Listener() {
            @Override
            public void onRenderedFirstFrame() {
                if (!active || renderedFrame) return;
                renderedFrame = true;
                retryCount = 0;
                main.removeCallbacks(connectWatchdog);
                main.removeCallbacks(stallWatchdog);
                long elapsed = SystemClock.elapsedRealtime() - startedAt;
                Log.i(CctvConfig.TAG, "rtsp live playing after " + elapsed + " ms");
                listener.onFirstFrame();
            }

            @Override
            public void onVideoSizeChanged(VideoSize videoSize) {
                if (!active || videoSize.height <= 0 || videoSize.width <= 0) return;
                float aspect = (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height;
                if (aspect > 0.2f && aspect < 5f) {
                    listener.onVideoAspectChanged(aspect);
                }
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                if (!active) return;
                if (state == Player.STATE_BUFFERING && renderedFrame) {
                    main.removeCallbacks(stallWatchdog);
                    main.postDelayed(stallWatchdog, STALL_TIMEOUT_MS);
                } else if (state == Player.STATE_READY) {
                    main.removeCallbacks(stallWatchdog);
                } else if (state == Player.STATE_ENDED) {
                    Log.i(CctvConfig.TAG, "rtsp stream ended; reconnecting");
                    scheduleReconnect("Menyambung ulang");
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                if (!active) return;
                Log.w(CctvConfig.TAG, "rtsp error: " + error.getErrorCodeName());
                scheduleReconnect("Gangguan jaringan");
            }
        });

        String playbackUrl = currentUrl;
        try {
            proxy = RtspSdpProxy.start(currentUrl);
            playbackUrl = proxy.localUrl();
        } catch (java.io.IOException e) {
            Log.w(CctvConfig.TAG, "local sdp proxy fallback: " + e.getClass().getSimpleName());
        }

        MediaItem item = new MediaItem.Builder()
                .setUri(playbackUrl)
                .setLiveConfiguration(new MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(600)
                        .setMaxOffsetMs(2500)
                        .setMaxPlaybackSpeed(1.04f)
                        .build())
                .build();

        RtspMediaSource source = new RtspMediaSource.Factory()
                .setForceUseRtpTcp(true)
                .setTimeoutMs(8000)
                .createMediaSource(item);

        player.setMediaSource(source);
        player.setPlayWhenReady(true);
        player.prepare();
        main.postDelayed(connectWatchdog, CONNECT_TIMEOUT_MS);
    }

    private void scheduleReconnect(String reason) {
        main.removeCallbacks(connectWatchdog);
        main.removeCallbacks(stallWatchdog);
        main.removeCallbacks(reconnectTask);
        releasePlayerOnly();
        if (!active) return;

        listener.onDisconnected(reason);
        long delayMs;
        if (retryCount == 0) {
            delayMs = 1500L;
        } else if (retryCount == 1) {
            delayMs = 3000L;
        } else if (retryCount == 2) {
            delayMs = 6000L;
        } else {
            delayMs = 12_000L;
        }
        retryCount++;
        main.postDelayed(reconnectTask, delayMs);
    }

    private void releasePlayerOnly() {
        if (player != null) {
            try {
                player.release();
            } catch (RuntimeException e) {
                Log.w(CctvConfig.TAG, "player release warning: " + e.getClass().getSimpleName());
            }
            player = null;
        }
        if (proxy != null) {
            proxy.close();
            proxy = null;
        }
    }
}
