# Petunjuk pemeliharaan repository ini

Repository ini menyimpan dokumentasi pribadi dan lapisan kustomisasi A9-X, bukan repository upstream Clean Your TV.

- Baca README, docs/PENGUJIAN.md dan docs/PEMBARUAN.md sebelum memperbarui.
- Perbarui deskripsi akhir dan CHANGELOG setiap ada perubahan yang diminta; hindari catatan lama yang bertentangan.
- Jangan menyatakan fitur terpasang atau diuji tanpa bukti pada perangkat. Pertahankan status belum terverifikasi, termasuk audio Quran offline dan penutupan ADB, sampai ada bukti baru.
- Jangan commit IP/SSID/email/serial/Android ID/MAC/koordinat privat, backup, screenshot mentah, log, kunci, token, APK, audio pribadi atau output build.
- Jangan menyalin semua isi work/ atau backup/ dari workspace setup. Ambil hanya sumber yang relevan, disamarkan dan ditinjau.
- Source customizations/prayer-times adalah overlay di atas commit upstream yang disebut README. Lokasi wajib dikonfigurasi dan dua MP3 wajib disediakan lokal sebelum build.
- Izin jaringan, alarm, pemutar dan overlay hanya pada add-on. Launcher tetap kecil dan tidak diberi izin internet.
- Perubahan perangkat harus mengikuti PROMPT.md upstream: cadangkan dahulu, disable bukan uninstall, jaga core/input/remote/Home, jangan membuka factory menu.
- Push hanya ke repository ini ketika pengguna meminta. Jangan force push, menimpa perubahan lain, atau membuka PR ke upstream tanpa permintaan.
