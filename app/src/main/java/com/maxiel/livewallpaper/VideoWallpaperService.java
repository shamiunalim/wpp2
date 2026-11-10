package com.maxiel.livewallpaper;

import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.media.MediaPlayer;
import android.net.Uri;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.io.File;

public class VideoWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new VideoEngine();
    }

    private class VideoEngine extends Engine implements SurfaceHolder.Callback {
        private MediaPlayer player;
        private boolean visible;
        private boolean surfaceReady;
        private final Handler handler = new Handler(Looper.getMainLooper());

        VideoEngine() {
            getSurfaceHolder().addCallback(this);
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            surfaceReady = true;
            startVideo();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (player != null && player.isPlaying()) {
                // Surface dimensions changed; keep current playback.
            } else if (visible) {
                startVideo();
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            surfaceReady = false;
            releasePlayer();
        }

        @Override
        public void onVisibilityChanged(boolean isVisible) {
            visible = isVisible;
            if (visible && surfaceReady) startVideo();
            else pauseVideo();
        }

        @Override
        public void onDestroy() {
            visible = false;
            releasePlayer();
            getSurfaceHolder().removeCallback(this);
            handler.removeCallbacksAndMessages(null);
            super.onDestroy();
        }

        private void startVideo() {
            if (!visible || !surfaceReady) return;
            releasePlayer();

            SharedPreferences prefs = getSharedPreferences("maxiel_wallpaper", MODE_PRIVATE);
            String saved = prefs.getString("video_uri", null);
            if (saved == null) return;

            try {
                Uri uri = Uri.parse(saved);
                player = new MediaPlayer();
                player.setLooping(true);
                player.setVolume(0f, 0f);
                player.setSurface(getSurfaceHolder().getSurface());
                player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                if ("file".equals(uri.getScheme())) {
                    player.setDataSource(uri.getPath());
                } else {
                    player.setDataSource(VideoWallpaperService.this, uri);
                }
                player.setOnPreparedListener(mp -> {
                    if (visible && surfaceReady) {
                        try { mp.start(); } catch (Exception ignored) {}
                    }
                });
                player.setOnErrorListener((mp, what, extra) -> {
                    releasePlayer();
                    return true;
                });
                player.prepareAsync();
            } catch (Exception e) {
                releasePlayer();
            }
        }

        private void pauseVideo() {
            if (player != null) {
                try {
                    if (player.isPlaying()) player.pause();
                } catch (Exception ignored) {}
            }
        }

        private void releasePlayer() {
            if (player != null) {
                try { player.setSurface(null); } catch (Exception ignored) {}
                try { player.reset(); } catch (Exception ignored) {}
                try { player.release(); } catch (Exception ignored) {}
                player = null;
            }
        }
    }
}
