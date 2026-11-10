package com.maxiel.livewallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_VIDEO = 501;
    private static final String PREFS = "maxiel_wallpaper";
    private static final String KEY_VIDEO = "video_uri";

    private EditText urlInput;
    private TextView status;
    private VideoView preview;
    private Button setButton;
    private Uri currentVideo;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildInterface();
        loadSavedVideo();
    }

    private void buildInterface() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(26), dp(20), dp(20));
        page.setBackgroundColor(Color.rgb(3, 8, 20));

        TextView title = new TextView(this);
        title.setText("MAXIEL  /  LIVE WALLPAPER");
        title.setTextColor(Color.rgb(0, 200, 255));
        title.setTextSize(21);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(8), 0, dp(8));
        page.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Pilih video dari HP atau unduh melalui URL MP4.");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, dp(18));
        page.addView(subtitle, matchWrap());

        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setHint("https://contoh.com/video.mp4");
        urlInput.setHintTextColor(Color.GRAY);
        urlInput.setTextColor(Color.WHITE);
        urlInput.setTextSize(14);
        urlInput.setPadding(dp(12), 0, dp(12), 0);
        urlInput.setBackgroundColor(Color.rgb(20, 32, 52));
        page.addView(urlInput, new LinearLayout.LayoutParams(-1, dp(52)));

        Button download = makeButton("UNDUH DARI URL");
        page.addView(download, buttonParams());
        download.setOnClickListener(v -> downloadFromUrl());

        Button choose = makeButton("PILIH VIDEO DARI HP");
        page.addView(choose, buttonParams());
        choose.setOnClickListener(v -> chooseVideo());

        preview = new VideoView(this);
        preview.setBackgroundColor(Color.BLACK);
        LinearLayout.LayoutParams videoParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        videoParams.topMargin = dp(12);
        videoParams.bottomMargin = dp(12);
        page.addView(preview, videoParams);

        status = new TextView(this);
        status.setText("Belum ada video dipilih.");
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(13);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(4), 0, dp(8));
        page.addView(status, matchWrap());

        setButton = makeButton("SET AS WALLPAPER");
        page.addView(setButton, buttonParams());
        setButton.setEnabled(false);
        setButton.setOnClickListener(v -> openWallpaperPicker());

        TextView note = new TextView(this);
        note.setText("Video akan diputar berulang. Untuk hemat baterai, pemutaran berhenti saat wallpaper tidak terlihat.");
        note.setTextColor(Color.GRAY);
        note.setTextSize(11);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, dp(12), 0, 0);
        page.addView(note, matchWrap());

        setContentView(page);
    }

    private void loadSavedVideo() {
        String saved = getPreferencesStore().getString(KEY_VIDEO, null);
        if (saved != null) {
            try {
                Uri uri = Uri.parse(saved);
                if ("file".equals(uri.getScheme()) && uri.getPath() != null
                        && new File(uri.getPath()).exists()) {
                    useVideo(uri);
                } else if ("content".equals(uri.getScheme())) {
                    useVideo(uri);
                }
            } catch (Exception ignored) {}
        }
    }

    private void chooseVideo() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_VIDEO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            useVideo(uri);
        }
    }

    private void downloadFromUrl() {
        String address = urlInput.getText().toString().trim();
        if (!(address.startsWith("https://") || address.startsWith("http://"))) {
            toast("Masukkan URL http:// atau https://");
            return;
        }

        status.setText("Mengunduh video...");
        setButton.setEnabled(false);

        executor.execute(() -> {
            File temp = new File(getExternalFilesDir("videos"), "wallpaper.tmp");
            File target = new File(getExternalFilesDir("videos"), "wallpaper.mp4");
            HttpURLConnection connection = null;
            try {
                File dir = temp.getParentFile();
                if (dir != null && !dir.exists() && !dir.mkdirs()) {
                    throw new Exception("Tidak bisa membuat folder penyimpanan");
                }

                URL url = new URL(address);
                connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(true);
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("User-Agent", "MaxielLiveWallpaper/1.0");
                connection.connect();

                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) throw new Exception("Server merespons HTTP " + code);

                try (InputStream in = new BufferedInputStream(connection.getInputStream());
                     FileOutputStream out = new FileOutputStream(temp)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    long total = 0;
                    while ((count = in.read(buffer)) != -1) {
                        total += count;
                        if (total > 250L * 1024L * 1024L) throw new Exception("Video lebih besar dari batas 250 MB");
                        out.write(buffer, 0, count);
                    }
                }

                if (!temp.exists() || temp.length() < 16) throw new Exception("File unduhan kosong atau terlalu kecil");
                if (!hasMp4Signature(temp)) throw new Exception("URL tidak menghasilkan file MP4 yang valid");

                if (target.exists() && !target.delete()) throw new Exception("Tidak bisa mengganti video lama");
                if (!temp.renameTo(target)) throw new Exception("Gagal menyimpan file video");

                Uri uri = Uri.fromFile(target);
                runOnUiThread(() -> useVideo(uri));
            } catch (Exception e) {
                temp.delete();
                String message = e.getMessage() == null ? "Download gagal" : e.getMessage();
                runOnUiThread(() -> {
                    status.setText("Gagal: " + message);
                    setButton.setEnabled(currentVideo != null);
                    toast(message);
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private boolean hasMp4Signature(File file) {
        try (InputStream in = new java.io.FileInputStream(file)) {
            byte[] bytes = new byte[64];
            int n = in.read(bytes);
            if (n < 12) return false;
            for (int i = 4; i <= Math.min(n - 4, 32); i++) {
                if (bytes[i] == 'f' && bytes[i + 1] == 't'
                        && bytes[i + 2] == 'y' && bytes[i + 3] == 'p') return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void useVideo(Uri uri) {
        currentVideo = uri;
        getPreferencesStore().edit().putString(KEY_VIDEO, uri.toString()).apply();
        status.setText("Video dipilih: " + uri.getLastPathSegment());
        setButton.setEnabled(true);
        try {
            preview.setVideoURI(uri);
            preview.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                preview.start();
            });
            preview.setOnErrorListener((mp, what, extra) -> {
                status.setText("Pratinjau gagal. Coba video MP4 H.264 dengan audio AAC atau tanpa audio.");
                return true;
            });
        } catch (Exception e) {
            status.setText("Tidak bisa memutar pratinjau video ini.");
        }
    }

    private void openWallpaperPicker() {
        if (currentVideo == null) {
            toast("Pilih video terlebih dahulu");
            return;
        }
        try {
            Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(this, VideoWallpaperService.class));
            startActivity(intent);
        } catch (Exception e) {
            try {
                startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
            } catch (Exception ignored) {
                toast("Pengaturan Live Wallpaper tidak tersedia di perangkat ini.");
            }
        }
    }

    private SharedPreferences getPreferencesStore() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private Button makeButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(0, 115, 190)));
        return button;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(50));
        p.topMargin = dp(8);
        return p;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        if (preview != null) {
            preview.stopPlayback();
        }
    }
}
