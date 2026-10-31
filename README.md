# CleanSTB A9-X

Dokumentasi dan kustomisasi STB **A9-X (titan-p1), Android 10 / SDK 29** berbasis [Clean Your TV](https://github.com/Gathin23/cleanyourtv).

Setup ini memakai launcher ringan, wallpaper Islami, kartu dan *live preview* CCTV lokal di *dashboard*, kartu Prayer Times, screensaver jadwal salat, serta azan otomatis dengan layar pengingat. Pembaruan terakhir: **31 Oktober 2026, WIT**.

## Fitur saat ini

| Fitur | Keadaan terakhir |
|---|---|
| Launcher | `com.gathin.tvhome`, Home default; launcher bawaan tetap aktif untuk pemulihan |
| Dock & Kartu Modul | Dock: YouTube, Netflix, Play Store, Prayer Times; kartu modul **CCTV Live** tepat di bawah dock |
| Wallpaper | Islamic Night, gambar statis agar tetap ringan; tema neobrutalism tersedia |
| CCTV Live | *Preview* RTSP *realtime* tanpa suara di halaman utama (otomatis mati di luar Home) + tayangan layar penuh dengan opsi suara |
| Prayer Times | Jadwal lima waktu, hitung mundur, jam WIT, ayat dan terjemahan |
| Screensaver | Jadwal salat; idle disetel lima menit |
| Azan otomatis | Alarm Android dan kalender lokal, tanpa proses pemutar permanen |
| Audio | `adzan-subuh.mp3` untuk Subuh; `adzan.mp3` untuk Zuhur, Asar, Magrib, Isya |
| Layar azan | Wallpaper, pesan **WAKTU SOLAT TELAH TIBA**, nama salat, hadis, dan indikator audio |
| Penutupan layar | Otomatis ketika audio selesai; tersedia Hentikan dan tombol Back |
| Quran for Android | Sudah terpasang; kelengkapan 114 surah Al-Husary dan uji offline masih perlu dikonfirmasi |

## Panduan

- [Setup dan konfigurasi](docs/SETUP.md)
- [CCTV realtime di dashboard](docs/CCTV.md)
- [Aplikasi yang dipertahankan/dinonaktifkan](docs/APLIKASI.md)
- [Azan otomatis dan screensaver](docs/AZAN.md)
- [Pemulihan dan masalah umum](docs/PEMULIHAN.md)
- [Hasil pengujian dan batasnya](docs/PENGUJIAN.md)
- [Cara memperbarui dokumentasi](docs/PEMBARUAN.md)
- [Riwayat perubahan](CHANGELOG.md)
- [Sumber, kredit, dan lisensi](NOTICE.md)

## Isi repository

- `customizations/cctv-live/`: sumber add-on *streaming* CCTV RTSP lokal untuk *preview* *dashboard* dan tampilan layar penuh.
- `customizations/prayer-times/`: sumber perubahan add-on jadwal salat dan azan untuk diterapkan di atas sumber upstream, bukan APK siap pasang.
- `themes/`: tema pribadi yang sudah dipakai pada STB.
- `docs/`: panduan dan bukti pengujian dalam bentuk ringkasan.

Audio pribadi dan konfigurasi RTSP tidak disertakan. Letakkan berkas sendiri secara lokal sesuai [panduan setup](docs/SETUP.md). IP, *token* stream, akun, serial, kunci ADB, dan dump perangkat tidak dipublikasikan.

Repository ini mendokumentasikan satu STB; bukan jaminan kompatibilitas pada perangkat lain. Tidak ada aplikasi sistem yang dihapus: perubahan debloat menggunakan penonaktifan yang dapat dipulihkan.
