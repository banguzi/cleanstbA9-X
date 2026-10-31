# Riwayat perubahan

## 2026-10-31

- Tambahkan add-on `customizations/cctv-live` (`com.banguzi.tv.cctv`) untuk menampilkan aliran CCTV RTSP lokal secara *realtime* tanpa menambah izin jaringan ke *launcher*.
- Tampilkan jendela *live preview* 16:9 tanpa suara di sisi kiri bawah jam pada halaman utama *dashboard* tema Islamic Night, yang otomatis berhenti dan melepas dekoder saat aplikasi lain dibuka.
- Tambahkan kartu modul **CCTV Live** dan layar penuh 1080p dengan jam WIT, tombol nyala/mati suara kamera, pengatur posisi dan ukuran *preview*, serta dialog pengaturan alamat RTSP.
- Tambahkan normalisasi SDP lokal (`RtspSdpProxy`) agar kompatibel dengan kamera RTSP (`gvminirtsp`) yang mengirim pemisah baris `\n` tanpa `\r`.
- Pasang dan verifikasi di STB A9-X: kartu modul tampil, *preview* langsung berjalan di *dashboard*, dekoder dilepas saat masuk layar penuh maupun aplikasi lain, dan kembali aktif saat kembali ke Home.
- Dokumentasikan setup, pemulihan, dan hasil pengujian CCTV dengan tetap menyamarkan alamat IP dan *token* kamera pribadi.

## 2026-10-24

- Dokumentasikan setup A9-X Android 10 berdasarkan sumber Clean Your TV pada commit acuan.
- Launcher menjadi Home dan mempertahankan launcher bawaan untuk pemulihan.
- Atur dock YouTube, Netflix, Play Store, dan Prayer Times; aplikasi ponsel tertentu ditambahkan lewat library_apps.
- Catat penonaktifan aplikasi yang disetujui, pengaktifan kembali Vidio, dan penonaktifan RBTV.
- Buat tema neobrutalism ringan, kemudian ganti tema aktif menjadi Islamic Night.
- Tambahkan jadwal salat, jam WIT, kutipan ayat, serta screensaver.
- Tambahkan alarm azan otomatis dan cache kalender lokal.
- Ganti rekaman awal dengan dua MP3 pengguna: khusus Subuh dan umum empat waktu.
- Tambahkan layar azan sementara, hadis lengkap, penghentian manual dan penutupan otomatis.
- Verifikasi layar hilang setelah audio Subuh selesai dan alarm berikutnya tetap terjadwal.
- Catat Quran for Android sudah terpasang; kelengkapan audio Al-Husary dan offline masih belum terverifikasi.
- Pisahkan data pribadi, cadangan, audio dan kunci dari repository.
