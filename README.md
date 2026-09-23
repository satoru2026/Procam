# ProCam — Aplikasi Kamera Android Lengkap

Proyek Android Studio (Kotlin + Jetpack Compose + CameraX) berisi aplikasi kamera
custom dengan fitur setara Google Camera. **Bukan clone/tiruan Google Camera** —
nama, ikon, dan algoritma proprietary Google (HDR+, Night Sight, Astrophotography)
tidak dapat direproduksi karena bukan open-source. Semua di sini dibuat dari nol
menggunakan API resmi Android (CameraX + Camera2 interop).

## Build APK online (tanpa Android Studio)
Proyek ini sudah menyertakan workflow GitHub Actions (`.github/workflows/build.yml`)
yang otomatis meng-compile APK di server GitHub setiap kali kode di-push:

1. Buat repository baru di GitHub, lalu upload/push seluruh isi folder `ProCam` ini.
2. Buka tab **Actions** di repo tersebut — build akan berjalan otomatis
   (atau klik **Run workflow** untuk memicu manual).
3. Setelah selesai (±3–5 menit), buka hasil run tersebut → bagian **Artifacts**
   → unduh `ProCam-debug-apk`, lalu ekstrak untuk mendapatkan `app-debug.apk`.
4. Salin APK ke HP Android dan install (aktifkan "Install dari sumber tidak
   dikenal" jika diminta).

Ini APK **debug** (belum ditandatangani untuk rilis Play Store) — cukup untuk
dipakai/dites sendiri. Kalau butuh APK release yang ditandatangani, beri tahu
saya, nanti workflow-nya saya sesuaikan.

## Cara membuka & menjalankan (di Android Studio)
1. Buka Android Studio (Giraffe/Koala/Ladybug atau lebih baru).
2. **File → Open** → pilih folder `ProCam` ini. Android Studio akan otomatis
   membuat Gradle Wrapper yang hilang (karena file wrapper biner tidak bisa
   disertakan di sini).
3. Tunggu proses Gradle sync selesai (butuh koneksi internet untuk mengunduh
   dependency pertama kali).
4. Jalankan di perangkat fisik (kamera tidak berjalan penuh di emulator) via
   tombol Run ▶, minimum Android 8.0 (API 26).

## Fitur yang sudah diimplementasikan
- **Foto & Video**: pratinjau langsung, ambil foto, rekam video (disimpan ke
  `Pictures/ProCam` dan `Movies/ProCam` via MediaStore).
- **Mode Manual/Pro**: kontrol ISO, kecepatan rana, jarak fokus, white balance,
  dan EV compensation langsung ke sensor lewat `Camera2Interop`.
- **Mode Malam**: memaksa eksposur lebih panjang (~1/4 detik) + ISO lebih
  tinggi secara otomatis saat memotret.
- **HDR**: `ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY`, bisa dimatikan.
- **Mode Potret**: blur latar belakang sederhana (elliptical mask + box blur)
  sebagai pendekatan tanpa sensor depth/ML segmentation.
- **Panorama & Slow-Motion**: mode UI dan pipeline video siap; panorama
  memberi panduan menggeser kamera (stitching penuh butuh library tambahan
  seperti OpenCV bila ingin hasil otomatis).
- **Filter warna**: Normal, Mono, Sepia, Vivid, Cool, Warm, Noir (ColorMatrix).
- **Watermark** tanggal & nama aplikasi (opsional, di Pengaturan).
- **Kontrol umum**: flash (off/on/auto/torch), grid 3x3, timer 3s/10s, ganti
  kamera depan/belakang, pinch-to-zoom, tap-to-focus.
- **Galeri bawaan**: menampilkan semua foto/video yang diambil ProCam.
- **Pengaturan**: simpan lokasi GPS, watermark, suara rana, HDR default.

## Batasan yang jujur perlu diketahui
- Night Sight/HDR+ asli Google memakai model ML terlatih (burst alignment,
  denoising neural) yang tidak publik — mode malam di sini adalah pendekatan
  eksposur panjang manual, bukan multi-frame stacking AI.
- Mode potret memakai masking elips + blur sederhana, bukan segmentasi
  berbasis machine learning (perlu ML Kit Selfie Segmentation atau model
  serupa untuk hasil akurat mengikuti tepi subjek — bisa ditambahkan).
- Panorama belum melakukan stitching otomatis (perlu OpenCV/library stitching).
- Belum ada RAW capture (bisa ditambahkan lewat `ImageCapture.OutputFileOptions`
  dengan `DngCreator` di device yang mendukung `CameraCharacteristics
  .REQUEST_AVAILABLE_CAPABILITIES_RAW`).

## Struktur proyek
```
app/src/main/java/com/procam/app/
  MainActivity.kt          - entry point, permission, navigasi
  MainViewModel.kt          - state & orkestrasi kamera
  camera/CameraController.kt- wrapper CameraX + Camera2Interop
  camera/ImageProcessor.kt  - filter warna, watermark, blur potret
  camera/CameraModels.kt    - model data & enum
  ui/CameraScreen.kt        - layar utama (viewfinder + kontrol)
  ui/GalleryScreen.kt       - galeri foto/video
  ui/SettingsScreen.kt      - pengaturan
  ui/components/            - TopBar, BottomBar, ManualPanel, GridOverlay
```
