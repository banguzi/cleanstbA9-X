# Setup dan konfigurasi

## Acuan sumber

Sumber upstream: https://github.com/Gathin23/cleanyourtv

Commit acuan: `30626a8e4714f86bf837384b109b5c73ee736917`.

Perangkat yang diuji: A9-X, titan-p1, Android 10 / SDK 29. Koneksi melalui LAN bawaan. Receiver mouse memakai USB. Tanggal dan waktu tampilan menggunakan WIT.

## Sebelum mengubah perangkat

Ikuti `PROMPT.md` upstream: cadangkan daftar paket lengkap/nonaktif, properti dan pengaturan. Simpan semuanya secara lokal. Periksa peran paket yang tidak jelas. Jangan membuka menu factory, melakukan root, flashing, atau menghapus paket sistem.

Aktifkan debugging hanya selama pengerjaan. Gunakan alamat perangkat sendiri:

```bash
export TV_HOST="<TV_IP>"
```

Komputer dan STB harus berada pada jaringan yang dapat saling terhubung. Izinkan permintaan debugging pada TV.

## Launcher

Gunakan sumber launcher upstream, minSdk 26, JDK 17 dan Android SDK 34/build-tools 34.0.0. Jangan memakai APK dari sumber yang tidak diketahui atau modifikasi launcher lama untuk perangkat lain.

Konfigurasi dock pada `launcher/app/src/main/res/values/config.xml`:

```xml
<string-array name="dock_apps">
    <item>com.google.android.youtube.tv</item>
    <item>com.netflix.mediaclient</item>
    <item>com.android.vending</item>
    <item>com.majnioui.tv.prayer</item>
</string-array>
<string-array name="library_apps" translatable="false">
    <item>com.netflix.mediaclient</item>
    <item>com.softwinner.TvdFileManager</item>
    <item>com.quran.labs.androidquran</item>
</string-array>
```

Netflix pada perangkat ini adalah versi ponsel; `library_apps` diperlukan agar tampil. Buat launcher baru menjadi Home, konfirmasi aplikasi yang benar-benar tampil, lalu reboot dan periksa kembali. Launcher bawaan `com.gecen.tvlauncher` tetap aktif sebagai jalur pemulihan.

## Add-on Prayer Times

1. Gunakan checkout upstream pada commit acuan.
2. Salin isi `customizations/prayer-times/app/src/main/` dari repository ini ke `addons/prayer-times/app/src/main/` dalam checkout tersebut. Berkas lain upstream tetap diperlukan.
3. Isi `latitude`, `longitude`, dan `place` pada `res/values/config.xml`. Nilai penanda LATITUDE/LONGITUDE sengaja harus diganti sebelum build. Metode Aladhan adalah `20` (Kemenag).
4. Letakkan audio pribadi berikut pada `addons/prayer-times/app/src/main/res/raw/`:
   - `adhan_subuh.mp3` — berkas asli `adzan-subuh.mp3`.
   - `adhan_standard.mp3` — berkas asli `adzan.mp3`.
5. Dari folder add-on, jalankan `./gradlew --no-daemon lintRelease assembleRelease` dengan JDK 17 dan Android SDK 34.
6. Kirim APK hasil build memakai `tools/b64push.py`, periksa hash transfer, lalu pasang dengan `pm install -r`. APK dan kunci penandatanganan tidak masuk GitHub.
7. Untuk memperbarui aplikasi yang sudah terpasang, gunakan kunci penandatanganan yang sama. Jangan menghapus aplikasi atau datanya untuk mengatasi ketidakcocokan tanda tangan.
8. Di pengaturan Android, izinkan Prayer Times tampil di atas aplikasi lain. Aktifkan sakelar **Azan otomatis**. Uji audio biasa dan Subuh.

```bash
# Dari root checkout upstream; jalur APK disesuaikan.
tools/venv/bin/python tools/b64push.py addons/prayer-times/app/build/outputs/apk/release/app-release.apk /data/local/tmp/prayer-times.apk
tools/venv/bin/python tools/tv.py 'pm install -r /data/local/tmp/prayer-times.apk'
```

## Tema

Tema `themes/islamic-night/` dan `themes/neobrutalism-light/` memakai kunci konfigurasi upstream. Jalankan dari checkout upstream dengan jalur absolut folder tema repository ini:

```bash
bash tools/theme.sh /jalur/ke/cleanstbA9-X/themes/islamic-night
```

Tema tidak mengubah kode atau izin launcher. Wallpaper berupa gambar statis, bukan video atau live wallpaper.

## Selesai

Matikan USB debugging dan Network/ADB debugging, cabut otorisasi, reboot, lalu gunakan `tools/adb-closed.py <TV_IP>` untuk memeriksa port. Penutupan ADB pada akhir sesi terakhir belum diverifikasi.
