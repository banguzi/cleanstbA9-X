# CCTV Live di Dashboard TV

Add-on `com.banguzi.tv.cctv` (`customizations/cctv-live/`) menampilkan aliran RTSP kamera lokal secara *realtime* pada halaman *dashboard* TV dan layar penuh (*fullscreen*) tanpa menambahkan izin jaringan atau dependensi pemutar pada *launcher* `com.gathin.tvhome`.

## Cara kerja

1. **Preview langsung di halaman Home**: ketika *launcher* `com.gathin.tvhome` (`MainActivity`) berada di depan layar, jendela *overlay* 16:9 tanpa fokus (`FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE`) tampil di sisi kiri di bawah jam sehingga area kubah masjid pada tema *Islamic Night* dan baris *dock* tetap terbuka. Jalur audio dimatikan sepenuhnya saat *preview* agar tidak meminta fokus audio atau membebani dekoder suara.
2. **Berhenti otomatis di luar Home**: `HomeWatchService` memantau perpindahan jendela (`TYPE_WINDOW_STATE_CHANGED`, tanpa *polling* dan tanpa menyaring tombol remote) serta status layar/screensaver. Saat pengguna membuka aplikasi lain (misalnya YouTube, Netflix, Prayer Times, atau Setelan), saat jendela pengingat azan tampil, atau saat *screensaver* berjalan, jendela *preview* ditutup dan dekoder H.264 perangkat keras segera dilepas.
3. **Kartu modul & layar penuh**: kartu modul **CCTV Live** berada tepat setelah baris *dock* untuk membuka tampilan 1080p layar penuh dengan jam WIT dan bilah kontrol yang otomatis bersembunyi setelah beberapa detik:
   - **Suara: Mati / Aktif** — menyalakan atau membisukan suara kamera (`G.711 A-law`) saat layar penuh.
   - **Preview Home: Aktif / Nonaktif** — mengaktifkan atau mematikan jendela *preview* halaman utama.
   - **Posisi: Kiri / Kanan** — memindahkan *preview* antara kiri-bawah jam atau kanan-bawah tombol Setelan.
   - **Ukuran: Kecil / Sedang / Besar** — mengubah ukuran bingkai 16:9 (`320×180`, `384×216`, `432×243` dp).
   - **Muat Ulang** dan **Atur Stream** — menyambung ulang aliran atau memperbarui nama dan alamat RTSP secara lokal.
4. **Kompatibilitas SDP kamera (`RtspSdpProxy`)**: sebagian kamera RTSP lokal (seperti server `gvminirtsp`) mengirim baris SDP dengan pemisah `\n` tanpa `\r` dan tanpa header `Content-Base`, yang ditolak oleh parser RTSP ketat Media3. Relay lokal pada antarmuka `127.0.0.1` menormalisasi teks SDP ke CRLF standar tanpa mengubah paket RTP biner.

## Konfigurasi privat

Alamat kamera dan *token* aliran disimpan di luar Git (misalnya `backup/cctv-config.json` pada mesin kerja lokal) dan dikirim ke penyimpanan internal aplikasi melalui `ConfigProvider` yang dilindungi izin `WRITE_SECURE_SETTINGS`:

```json
{
  "rtspUrl": "rtsp://<CCTV_IP>:5543/<STREAM_TOKEN>/live/channel0",
  "cameraName": "CCTV",
  "homePreview": true,
  "previewPosition": "left",
  "previewSize": "medium"
}
```

## Izin add-on

| Izin / Layanan | Tujuan |
|---|---|
| `INTERNET` | Membuka aliran RTSP kamera pada jaringan lokal (tanpa telemetri/analitik luar) |
| `SYSTEM_ALERT_WINDOW` | Menampilkan jendela *preview* tanpa fokus di atas halaman utama *dashboard* |
| `HomeWatchService` (*Accessibility*) | Mendeteksi perpindahan halaman utama vs aplikasi lain tanpa menyaring tombol remote |
| `ConfigProvider` (`WRITE_SECURE_SETTINGS`) | Menerima konfigurasi RTSP dari shell ADB ke penyimpanan privat aplikasi |
