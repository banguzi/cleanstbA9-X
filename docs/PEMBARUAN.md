# Memperbarui dokumentasi dan push berikutnya

Dokumentasi tidak tersinkron otomatis dari STB. Setelah ada perubahan, pengguna dapat meminta: **“Perbarui dokumentasi dan push ke banguzi/cleanstbA9-X.”**

## Alur setiap pembaruan

1. Baca keadaan repository dan perubahan terbaru. Gunakan checkout terpisah bila folder kerja berisi modifikasi lain.
2. Catat apa yang berubah, alasan, dan keadaan akhirnya. Jangan sekadar menambahkan catatan kronologis yang bertentangan dengan deskripsi lama.
3. Perbarui README, panduan terkait, dan CHANGELOG. Jika kode add-on atau tema berubah, sinkronkan kustomisasi sumbernya.
4. Perbarui matriks pengujian hanya berdasarkan bukti. Pisahkan dibuat, dipasang, dan benar-benar diuji pada TV.
5. Periksa IP, SSID, email, serial, Android ID, MAC, nama pribadi, koordinat privat, kunci ADB, token, APK dan audio pribadi. Jangan memasukkannya ke commit.
6. Tinjau diff, jalankan pemeriksaan yang sesuai perubahan, lalu buat commit satu topik. Untuk perubahan dokumentasi saja, periksa tautan lokal dan konsistensi; tidak perlu membangun aplikasi ulang.
7. Push ke repository ini bila diminta pengguna. Jika ada perubahan jarak jauh, gabungkan dengan aman; jangan force push atau menimpa pekerjaan orang lain.
8. Laporkan tautan commit, perubahan utama, bukti pengujian, dan hal yang masih belum diverifikasi.

Contoh judul commit: `docs: catat pembaruan layar azan` atau `prayer-times: perbaiki penutupan layar`.

## Catatan perubahan singkat

```markdown
## YYYY-MM-DD
- Perubahan:
- Alasan:
- Keadaan akhir:
- Pengujian nyata:
- Belum diuji:
- Pemulihan:
```

Push berikutnya memerlukan akses GitHub yang masih berlaku. Permintaan ini tidak membuat jadwal otomatis, pemantauan terus-menerus, atau izin untuk menerbitkan setiap perubahan tanpa instruksi.
