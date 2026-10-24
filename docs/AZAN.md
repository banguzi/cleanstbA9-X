# Jadwal salat, screensaver, dan azan otomatis

## Jadwal dan tampilan

Prayer Times mengambil jadwal Aladhan dengan metode Kemenag (20) untuk koordinat yang dikonfigurasi. Jadwal hasil perhitungan perlu dicocokkan dengan masjid setempat. Ini bukan klaim jadwal resmi masjid atau jadwal resmi yang diterbitkan Kemenag.

Halaman aplikasi dan screensaver menampilkan jam WIT serta petikan QS. Ar-Ra'd 13:28 beserta terjemahannya. Screensaver menggunakan DreamService Android; idle disetel lima menit. Aplikasi pemutar video yang menjaga layar tetap menyala dapat menunda screensaver.

## Audio lima waktu

| Waktu | Berkas sumber pengguna | Nama resource lokal | Durasi terukur |
|---|---|---|---|
| Subuh | `adzan-subuh.mp3` | `adhan_subuh.mp3` | sekitar 3:07 |
| Zuhur, Asar, Magrib, Isya | `adzan.mp3` | `adhan_standard.mp3` | sekitar 3:39 |

Audio disalin utuh tanpa pemotongan. Rekaman Mekkah yang dipakai pada tahap awal sudah digantikan oleh dua audio pengguna. Audio pribadi tidak dipublikasikan di repository.

## Alur saat waktu salat tiba

1. Alarm Android memicu layanan pemutar.
2. Pemutar meminta fokus audio sementara; aplikasi lain yang mendukungnya dapat menjeda audio.
3. Rekaman yang sesuai mulai diputar secara offline.
4. Layar sementara tampil di atas aplikasi lain: wallpaper Islami, **WAKTU SOLAT TELAH TIBA**, nama salat, jam, hadis, dan kemajuan pemutaran.
5. Setelah rekaman selesai, layar dan layanan pemutar ditutup. Tampilan sebelumnya kembali terlihat.

Pemutaran juga dapat dihentikan dengan **Hentikan azan** atau Back. Volume mengikuti volume media STB; tidak dinaikkan paksa.

Hadis yang ditampilkan adalah matan lengkap tentang salat berjamaah lebih utama 27 derajat daripada salat sendiri, dari Abdullah bin Umar, HR. al-Bukhari 645. Ada teks Arab, terjemahan makna Indonesia, dan sumber. Ini matan lengkap, bukan seluruh rangkaian sanad.

## Penjadwalan dan offline

Kalender bulan berjalan serta bulan berikutnya disimpan lokal. Penyegaran terjadwal membutuhkan jaringan. Saat audio diputar, internet tidak diperlukan. Cache yang sudah ada tetap dipakai bila pembaruan jaringan gagal; setelah rentang cache habis, jadwal baru memerlukan jaringan kembali.

Hanya satu alarm salat berikutnya yang aktif. Setelah dipicu, alarm selanjutnya dijadwalkan. Tidak ada alarm Terbit. Pemicu lebih awal, lebih dari 90 detik terlambat, waktu tidak valid, dan ID ganda diabaikan. Boot/perubahan waktu mencoba memulihkan alarm mendatang, bukan membunyikan ulang salat yang terlewat.

STB dan speaker harus menyala, tidak mute. Mematikan paksa aplikasi dapat menghentikan alarm sampai aplikasi dibuka lagi. Mematikan TV/speaker juga dapat membuat audio tidak terdengar meski STB aktif.

## Izin add-on

| Izin/layanan | Tujuan |
|---|---|
| `INTERNET` | Mengambil jadwal Aladhan |
| `RECEIVE_BOOT_COMPLETED` | Memulihkan jadwal setelah boot |
| `SCHEDULE_EXACT_ALARM` | Penjadwalan tepat pada Android yang memerlukannya |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Pemutar yang terlihat melalui notifikasi selama azan |
| `WAKE_LOCK` | Mendukung pemutaran audio sampai selesai |
| `SYSTEM_ALERT_WINDOW` | Layar sementara di atas aplikasi lain selama azan |
| DreamService / JobService | Screensaver dan pekerjaan pengambilan jadwal yang diikat Android |

Izin tersebut milik add-on, bukan launcher. Tidak ada izin internet baru pada launcher, telemetry, atau layanan pemutar yang berjalan permanen.
