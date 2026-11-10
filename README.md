# Maxiel Live Wallpaper

Aplikasi Android Live Wallpaper berbasis `WallpaperService` untuk Android 6.0+.

## Fitur
- Mengunduh video dari URL HTTP/HTTPS langsung di aplikasi.
- Memilih video dari penyimpanan Android.
- Pratinjau video.
- Memasang video melalui pemilih Live Wallpaper Android.
- Loop tanpa suara; pemutaran berhenti saat wallpaper tidak terlihat.

## Build APK lewat GitHub Actions

1. Upload **seluruh isi project** ini ke repository GitHub.
2. Buka **Actions**.
3. Pilih **Build Android APK**.
4. Jalankan workflow atau lakukan push ke branch `main`/`master`.
5. Jika selesai dengan tanda hijau, buka hasil workflow.
6. Pada bagian **Artifacts**, unduh `Maxiel-Live-Wallpaper-debug-apk`.
7. Ekstrak artifact lalu instal `app-debug.apk` di HP Android.

### Catatan GitHub Pages
Project web boleh tetap menggunakan:

- Source: **Deploy from a branch**
- Branch: **main**
- Folder: **/(root)**

Pengaturan Pages tersebut tidak berhubungan dengan proses build APK. Workflow APK berada di `.github/workflows/android.yml`.

## Build lokal

Gunakan JDK 17, Android SDK Platform 35, Build Tools 35.0.0, dan Gradle 8.7:

```bash
gradle assembleDebug
```

## Cara memakai

1. Instal APK.
2. Buka Maxiel Live Wallpaper.
3. Tempel URL MP4 langsung dan tekan **UNDUH DARI URL**, atau pilih video dari HP.
4. Tunggu pratinjau tampil.
5. Tekan **SET AS WALLPAPER**.
6. Di preview sistem Android, tekan **Set wallpaper**.

## Format video yang disarankan

Gunakan MP4 H.264 (AVC), resolusi 720p atau lebih rendah untuk perangkat dengan RAM terbatas. URL harus mengarah langsung ke file MP4 publik, bukan halaman pemutar atau tautan yang membutuhkan login.
