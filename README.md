# CleanSTB A9-X

Dokumentasi dan kustomisasi STB **A9-X (titan-p1), Android 10 / SDK 29** berbasis [Clean Your TV](https://github.com/Gathin23/cleanyourtv).

Setup ini memakai launcher ringan, wallpaper Islami, kartu Prayer Times, screensaver jadwal salat, serta azan otomatis dengan layar pengingat. Pembaruan terakhir: **24 Oktober 2026, WIT**.

## Fitur saat ini

| Fitur | Keadaan terakhir |
|---|---|
| Launcher | `com.gathin.tvhome`, Home default; launcher bawaan tetap aktif untuk pemulihan |
| Dock | YouTube, Netflix, Play Store, Prayer Times |
| Wallpaper | Islamic Night, gambar statis agar tetap ringan; tema neobrutalism tersedia |
| Prayer Times | Jadwal lima waktu, hitung mundur, jam WIT, ayat dan terjemahan |
| Screensaver | Jadwal salat; idle disetel lima menit |
| Azan otomatis | Alarm Android dan kalender lokal, tanpa proses pemutar permanen |
| Audio | `adzan-subuh.mp3` untuk Subuh; `adzan.mp3` untuk Zuhur, Asar, Magrib, Isya |
| Layar azan | Wallpaper, pesan **WAKTU SOLAT TELAH TIBA**, nama salat, hadis, dan indikator audio |
| Penutupan layar | Otomatis ketika audio selesai; tersedia Hentikan dan tombol Back |
| Quran for Android | Sudah terpasang; kelengkapan 114 surah Al-Husary dan uji offline masih perlu dikonfirmasi |

## Panduan

- [Setup dan konfigurasi](docs/SETUP.md)
- [Aplikasi yang dipertahankan/dinonaktifkan](docs/APLIKASI.md)
- [Azan otomatis dan screensaver](docs/AZAN.md)
- [Pemulihan dan masalah umum](docs/PEMULIHAN.md)
- [Hasil pengujian dan batasnya](docs/PENGUJIAN.md)
- [Cara memperbarui dokumentasi](docs/PEMBARUAN.md)
- [Riwayat perubahan](CHANGELOG.md)
- [Sumber, kredit, dan lisensi](NOTICE.md)

## Isi repository

- `customizations/prayer-times/`: sumber perubahan add-on untuk diterapkan di atas sumber upstream, bukan APK siap pasang.
- `themes/`: tema pribadi yang sudah dipakai pada STB.
- `docs/`: panduan dan bukti pengujian dalam bentuk ringkasan.

Audio pribadi tidak disertakan. Letakkan berkas sendiri secara lokal sesuai [panduan setup](docs/SETUP.md). IP, akun, serial, kunci ADB, dan dump perangkat tidak dipublikasikan.

Repository ini mendokumentasikan satu STB; bukan jaminan kompatibilitas pada perangkat lain. Tidak ada aplikasi sistem yang dihapus: perubahan debloat menggunakan penonaktifan yang dapat dipulihkan.
