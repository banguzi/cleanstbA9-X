# Keadaan aplikasi terakhir

Ini catatan sesi pada satu perangkat, bukan daftar debloat yang boleh diterapkan tanpa pemeriksaan. Paket dinonaktifkan untuk pengguna 0; tidak dihapus. Belum ada bukti pemakaian beberapa hari untuk menyebutnya daftar TV teruji upstream.

| Paket | Keadaan akhir sesi | Alasan |
|---|---|---|
| `com.google.android.youtube.tv` | Dipertahankan | Permintaan pengguna |
| `com.netflix.mediaclient` | Dipertahankan | Permintaan pengguna |
| `com.vidio.android.tv` | Aktif kembali | Sebelumnya nonaktif, lalu diminta aktif kembali |
| `com.rctiplus.atv` | Dinonaktifkan | Tidak digunakan |
| `com.softwinner.miracastReceiver` | Dinonaktifkan | Tidak digunakan |
| `com.google.android.katniss` | Dinonaktifkan | Asisten suara tidak digunakan |
| `com.charon.rocketfly` | Dinonaktifkan | Pembersih bawaan |
| `com.rblive.app` | Dinonaktifkan | Permintaan pengguna |
| `com.android.nfc` | Tidak diubah | Sudah nonaktif sebelum sesi |
| `com.gecen.tvlauncher` | Tetap aktif | Pemulihan Home |
| `com.gecen.updatesystem` | Dipertahankan | Pembaruan firmware |

Play Store, Play Services, keyboard, remote/Bluetooth, input/source, dan komponen inti Android dipertahankan. Aplikasi lain yang belum disepakati tidak ikut dinonaktifkan.

## Mengaktifkan satu aplikasi

```bash
export TV_HOST="<TV_IP>"
tools/venv/bin/python tools/tv.py 'pm enable --user 0 com.rblive.app'
```

Periksa kembali `pm list packages -e` untuk memastikan statusnya. Jangan menerapkan ulang daftar sesi lama tanpa memperhitungkan bahwa Vidio sudah diaktifkan kembali.
