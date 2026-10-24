# Pengujian dan batas bukti

Pengujian dilakukan pada STB A9-X Android 10, 24 Oktober 2026. Waktu di bawah menggunakan WIT. Tidak semua hal telah diuji; status dibedakan agar dokumentasi tidak memberi jaminan yang belum dibuktikan.

| Pemeriksaan | Hasil |
|---|---|
| Pencadangan awal | Daftar paket, pengaturan dan properti tersimpan lokal |
| Home setelah reboot | Launcher baru tetap Home; ADB kembali tersambung |
| Status paket nonaktif | Dibaca ulang setelah penonaktifan dan reboot awal |
| Vidio aktif kembali | Terverifikasi melalui daftar paket aktif |
| RBTV nonaktif | Terverifikasi melalui daftar paket nonaktif |
| Wallpaper dan dock | Diperiksa melalui screenshot STB |
| Prayer Times dan terjemahan ayat | Tampilan jadwal, jam dan kutipan diperiksa |
| Screensaver manual | DreamService terikat dan menampilkan jadwal |
| Screensaver idle lima menit | Sudah dikonfigurasi; menunggu idle penuh belum diamati |
| Azan terjadwal versi awal | Magrib mulai otomatis pukul 18:26:00.128; Isya berikutnya terjadwal 19:36 |
| Suara uji azan awal | Pengguna mengonfirmasi terdengar |
| Build add-on akhir | `lintRelease` dan `assembleRelease` lulus |
| Logika alarm | Uji waktu awal/terlambat, duplikat, nomor salat tidak valid dan waktu kosong lulus |
| Integritas dua MP3 pengguna | Hash sumber dan berkas dalam APK sama; decoder membaca sampai akhir tanpa kesalahan |
| Pemilihan audio biasa | Log memilih `adzan.mp3`, durasi 219436 ms; uji dihentikan manual |
| Pemilihan audio Subuh | Log memilih `adzan-subuh.mp3`, durasi 187279 ms |
| Layar azan di atas dashboard | Tetap tampil setelah Home ditekan |
| Penutupan otomatis versi akhir | Audio Subuh selesai 18:53:49.534, layar ditutup 18:53:49.577; dashboard kembali |
| Pembersihan setelah selesai | Jendela azan dan layanan pemutar tidak ditemukan setelah pemutaran selesai |
| Alarm setelah uji akhir | Isya 19:36 tetap tercatat |
| Quran offline lengkap | Aplikasi, mushaf dan terjemahan tersedia; unduhan 114 surah serta uji tanpa internet belum dikonfirmasi |
| Penutupan ADB akhir | Belum diverifikasi |

Screenshot mentah dan log asli disimpan lokal karena dapat mengandung informasi perangkat. Hanya screenshot tema yang telah ditinjau tanpa identitas pribadi disertakan; pengujian lain dirangkum dengan data yang disamarkan. Build/lint tidak menggantikan uji suara, tombol remote, reboot, atau kompatibilitas perangkat lain.

Versi dengan dua MP3 dan layar pengingat telah diuji lewat tombol uji serta penutupan otomatis. Pengamatan alarm Magrib nyata dilakukan pada versi awal sebelum dua MP3 pengguna ditambahkan; pengamatan itu tidak dipresentasikan sebagai uji alarm nyata pada seluruh lima waktu versi akhir.
