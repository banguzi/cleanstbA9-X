# Pemulihan dan masalah umum

## Aplikasi yang dinonaktifkan

Gunakan `tools/restore.sh` upstream dengan daftar sesi asli yang disimpan lokal. Simpan catatan pemulihan `~/.cleanyourtv/`; daftar dan catatan ini tidak dipublikasikan. NFC yang sudah nonaktif sebelumnya harus tetap dibiarkan sebagaimana kondisi awal.

Untuk satu paket, gunakan `pm enable --user 0 <paket>` dan verifikasi daftar aplikasi aktif.

## Mengembalikan Home

Launcher bawaan tetap aktif. Pada perangkat A9-X yang diuji, komponen Home awal adalah `com.gecen.tvlauncher/.LoadingActivity`:

```bash
export TV_HOST="<TV_IP>"
tools/venv/bin/python tools/tv.py 'cmd package set-home-activity --user 0 com.gecen.tvlauncher/.LoadingActivity'
tools/venv/bin/python tools/tv.py 'input keyevent HOME'
```

Komponen tersebut khusus catatan perangkat ini; periksa ulang pada firmware lain. Jangan menonaktifkan launcher yang sedang menjadi satu-satunya Home.

## Tema

```bash
bash tools/theme.sh --reset
```

Ini mengembalikan konfigurasi bawaan launcher hasil build. Untuk kembali ke tema lain, terapkan folder tema tersebut melalui `tools/theme.sh`.

## CCTV Live dan jendela preview

- Buka kartu **CCTV Live**, lalu ubah **Preview Home: Aktif** menjadi **Nonaktif** bila ingin menyembunyikan jendela *preview* di halaman utama tanpa menghapus aplikasi.
- Pilih **Atur Stream** untuk mengganti alamat RTSP apabila IP kamera berubah di jaringan lokal.
- Untuk menonaktifkan layanan deteksi halaman utama lewat ADB tanpa menghapus aplikasi:
  ```bash
  export TV_HOST="<TV_IP>"
  tools/venv/bin/python tools/tv.py 'appops set com.banguzi.tv.cctv SYSTEM_ALERT_WINDOW ignore; settings put secure enabled_accessibility_services ""'
  ```
- Untuk mencopot add-on sepenuhnya: `pm uninstall com.banguzi.tv.cctv` (hanya untuk add-on buatan sendiri, bukan paket sistem).

## Azan dan layar pengingat

- Matikan sakelar **Azan otomatis** untuk menghentikan penjadwalan.
- Tekan **Hentikan azan** atau Back untuk menutup pemutaran berjalan.
- Izin tampil di atas aplikasi lain dapat dicabut melalui pengaturan Android untuk Prayer Times.
- Jangan menghapus data aplikasi untuk sekadar menghentikan suara: data jadwal dan pilihan akan ikut hilang.
- Kembalikan APK lokal sebelumnya hanya bila menggunakan tanda tangan yang sama. Repository tidak menyimpan APK maupun kunci penandatanganan.

## Screensaver

Kembalikan pilihan screensaver dan waktu idle dari cadangan pengaturan awal perangkat sendiri. Nilai timeout awal perangkat ini tidak boleh dianggap sebagai default semua STB.

## Mouse USB tidak terdeteksi

Pada sesi ini mouse kembali berfungsi setelah reboot. USB debugging dapat mengganggu daya/host USB pada sebagian STB; itu kemungkinan penyebab, belum dibuktikan sebagai satu-satunya penyebab. Coba cabut-pasang receiver, reboot, dan matikan debugging setelah sesi selesai. Jangan menonaktifkan layanan input untuk memperbaikinya.

## ADB terputus

Pastikan STB menyala, IP belum berubah, debugging aktif dan komputer diizinkan. Pada sesi ini koneksi pernah kembali setelah STB dinyalakan. Port yang sesaat terbuka bukan bukti shell ADB stabil. Jangan melakukan perubahan paket tanpa koneksi yang dapat diverifikasi.

## Tidak terdengar azan

Periksa sakelar otomatis, jam perangkat, jadwal berikutnya, volume media, speaker, izin dan apakah aplikasi pernah dipaksa berhenti. Tombol Coba azan tersedia untuk membedakan masalah audio dari masalah penjadwalan. Jangan mengubah jam sistem hanya untuk menguji alarm.
