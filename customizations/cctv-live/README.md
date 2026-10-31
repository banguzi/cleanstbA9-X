# Add-on CCTV Live

Add-on Android TV ringan (`com.banguzi.tv.cctv`) untuk menampilkan *stream* RTSP kamera secara *realtime* di STB A9-X tanpa menambahkan izin jaringan atau pemutar video ke *launcher* utama (`com.gathin.tvhome`).

## Fitur

- **Preview langsung di halaman Dashboard (Home)**: jendela *live preview* 16:9 tanpa suara (*mute*) di halaman utama (default di sisi kiri di bawah jam agar kubah masjid pada tema *Islamic Night* tetap terlihat). Jendela tidak mengambil fokus remote dan otomatis berhenti serta melepas dekoder video perangkat keras saat membuka YouTube, Netflix, pengaturan, *screensaver*, atau saat layar azan tampil.
- **Kartu modul `CCTV Live`**: tampil pada *dashboard* Clean Your TV untuk membuka tayangan layar penuh (*fullscreen*) dengan jam WIT, opsi menyalakan/mematikan suara kamera, pengatur posisi/ukuran *preview*, muat ulang *stream*, serta pengubah alamat RTSP.

## Konfigurasi dan Build

1. Salin folder `customizations/cctv-live` ke `addons/cctv-live` pada *checkout* sumber [Clean Your TV](https://github.com/Gathin23/cleanyourtv), lalu salin *wrapper* Gradle dari add-on upstream (`cp -R addons/frigate-glance/gradle addons/frigate-glance/gradlew addons/cctv-live/`).
2. Buat berkas konfigurasi lokal di luar Git (misalnya `backup/cctv-config.json`) mengacu pada `config.example.json`. Jangan pernah melakukan *commit* IP kamera atau *token* RTSP pribadi.
3. Jalankan skrip pemasangan dari *root* *checkout* upstream:

```bash
TV_HOST=<TV_IP> bash addons/cctv-live/install.sh backup/cctv-config.json
```

## Izin yang Digunakan

| Izin / Komponen | Tujuan |
|---|---|
| `INTERNET` | Membuka aliran RTSP kamera pada jaringan lokal (tanpa telemetri/analitik) |
| `SYSTEM_ALERT_WINDOW` | Menampilkan jendela *preview* tanpa fokus di atas halaman utama *dashboard* |
| `HomeWatchService` (*Accessibility*) | Mendeteksi pergantian jendela halaman utama vs aplikasi lain tanpa *polling* dan tanpa menyaring tombol remote |
| `ConfigProvider` (`WRITE_SECURE_SETTINGS`) | Menerima berkas konfigurasi RTSP dari shell ADB ke penyimpanan privat aplikasi |
