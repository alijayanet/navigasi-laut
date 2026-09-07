# 🚢 Nusantara Bahari — Navigasi Laut & Radar Maritim Pintar 🌊

[![Android](https://img.shields.io/badge/Platform-Android%206.0%2B%20(API%2023%2B)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2F%20MVVM-FF6F00?style=flat-square)](https://developer.android.com/topic/architecture)
[![License](https://img.shields.io/badge/License-MIT-green.svg?style=flat-square)](LICENSE)

> **Nusantara Bahari** adalah aplikasi sistem navigasi kapal laut, radar AIS maritim, peta batimetri satelit HD, dan manajemen keselamatan terpadu yang dirancang khusus untuk Nelayan, Pelaut, Nahkoda, serta Operator Kapal di seluruh wilayah perairan Indonesia.

---

## 🌟 Fitur Utama Navigasi & Keselamatan

### 🛰️ 1. Peta Satelit Laut HD & Lapisan Kartografi Navionics
* **Slippy Map Tile Engine Multi-Layer**: Mendukung *Citra Satelit Maritim HD Resolusi Tinggi*, *Peta Laut Vektor ENC (IHO)*, dan *SonarChart™ HD Batimetri Kontur Kerapatan 1 Meter*.
* **Graticule Koordinat & Grid Maritim**: Garis lintang/bujur presisi tinggi, garis pemisah jalur alur laut (*TSS / Traffic Separation Scheme*), kabel bawah laut, dan titik bahaya bangkai kapal karam.
* **Spot Soundings Kedalaman**: Angka kedalaman air laut (*bathymetry soundings*) resmi hidrografi.

### 🚢 2. Radar AIS Kapal & Pelampung Suar (SBNP IALA) Real-Time
* **Pelacakan Posisi Kapal**: Membaca data target AIS kapal sekitar (Nama Kapal, Call Sign, MMSI, Tipe Kapal, Kecepatan, dan Haluan).
* **Rambu Suar SBNP IALA Region A**: Identifikasi rambu suar lateral (*Port Hand* merah silinder, *Starboard Hand* hijau kerucut), tanda kardinal (Utara, Selatan, Timur, Barat), bahaya terisolasi, dan alur aman (*Fairway Buoy*).
* **Data Lintas Wilayah Nusantara**: Terkoneksi cerdas ke area pelabuhan dan perairan strategis (Pelabuhan Patimban Subang, PLTU & Kilang Balongan Indramayu, Teluk Jakarta & Kepulauan Seribu, Selat Sunda, Laut Jawa, Selat Bali, Selat Makassar, Natuna Utara, dll).

### 🚨 3. Alarm Bahaya Tabrakan Kapal (CPA & TCPA Collision Alarm)
* **Perhitungan Vektor Relatif Maritim Real-Time**:
  * **CPA (*Closest Point of Approach*)**: Menghitung jarak terdekat pertemuan kapal dalam Mil Laut (NM).
  * **TCPA (*Time to CPA*)**: Estimasi sisa waktu sebelum titik temu terdekat dalam menit.
* **Sistem Peringatan Dini**: Memunculkan banner bahaya merah berkedip, sirine darurat, dan *halo ring* merah berdenyut saat terdeteksi risiko benturan ($CPA < 0.6\text{ NM}, TCPA \le 15\text{ menit}$).

### ⚓ 4. Pengawasan & Alarm Lego Jangkar (*Anchor Drag Alarm*)
* **Lingkaran Batas Aman (*Geofence Radius*)**: Pilihan radius pengawasan 30m, 50m, 100m, hingga 150m.
* **Deteksi Hanyut Otomatis**: Memantau pergeseran kapal dari titik lego jangkar secara *live*. Memicu alarm sirine nyaring jika kapal hanyut terbawa arus atau angin kencang melebihi batas toleransi.

### 🌙 5. Mode Navigasi Malam Anjungan (*Night Vision Crimson Mode*)
* **Filter Merah Monokrom**: Mengubah antarmuka layar menjadi palet warna merah anjungan (*bridge night mode*) untuk menjaga adaptasi penglihatan malam nahkoda di kegelapan laut tanpa silau layar (*anti-glare*).

### 📍 6. Perekam Jejak Rute & Navigasi Putar Balik (*Breadcrumb & Backtrack*)
* **Perekaman Jejak Kapal**: Menyimpan titik-titik lintasan pelayaran dengan alur garis berpendar (*breadcrumb trail*).
* **Fitur Backtrack**: Memandu kapal berputar balik secara aman mengikuti alur keberangkatan yang telah terbukti bebas rintangan karang/dangkal saat kabut tebal atau badai.

### 🌊 7. Prediksi Pasang Surut & Vektor Arus Laut (*Tides & Currents*)
* **Perhitungan Harmonik Pasang Surut**: Menghitung elevasi air pasang secara live ($+1.4\text{ m}$), status fase (*Pasang Naik / HW / Surut / LW*), serta jadwal puncak pasang tinggi (*High Water*) dan surut terendah (*Low Water*).
* **Vektor Aliran Arus**: Estimasi kecepatan arus laut (*Knot*) beserta sudut derajat arah aliran kompas.

### 🐟 8. Echosounder Sonar Ikan & Spot ZPPI Satelit
* **Visual Akustik Sonar CHIRP**: Simulasi tampilan kedalaman 200 kHz CHIRP dengan deteksi lapisan termoklin, struktur dasar laut, dan target ikan (*fish arches*).
* **Zona Potensi Penangkapan Ikan (ZPPI)**: Peta sebaran *Suhu Permukaan Laut (SST)* dan konsentrasi *Klorofil-a* satelit untuk efisiensi BBM dan peningkatan hasil tangkapan.

### 📟 9. NMEA 0183 & AIS Telemetry Stream
* **Simulator Kalimat NMEA Serial (38400 Baud)**: Menghasilkan pesan standar NMEA 0183 (`$GPGGA`, `$GPRMC`, `$GPVTG`, `$SDDPT`, `!AIVDM`) untuk integrasi ke instrumen eksternal atau autopilots.

### 📱 10. Desain Landscape Navigasi Layar Penuh & Anti-Sleep
* **Peta Navigasi 100% Bebas Halangan**: HUD telemetri navigasi dipadatkan ke dalam ribbon kompak atas, bilah alat mengambang vertikal di sisi kanan, serta panel samping kiri responsif saat marker kapal/suar diketuk.
* **Anti-Sleep (*Keep Screen On*)**: Layar smartphone tidak akan meredup atau terkunci otomatis saat diletakkan pada dashboard anjungan kapal.

---

## 🛠️ Arsitektur & Teknologi

* **Bahasa**: [Kotlin 2.0](https://kotlinlang.org/)
* **User Interface**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material Design 3
* **Arsitektur**: Clean Architecture + MVVM (*Model-View-ViewModel*) dengan Kotlin Coroutines & `StateFlow`
* **Penyimpanan Lokal**: [Room Database](https://developer.android.com/training/data-storage/room) (*Offline-First*)
* **Rendering Peta**: Custom Vector Canvas & Slippy Tile Caching Engine (Disk + Memory LRU)
* **Audio & Haptic**: `ToneGenerator`, `MediaPlayer`, and `Vibrator` untuk sistem alarm maritim darurat
* **Kompatibilitas**: Minimum SDK 23 (Android 6.0 Marshmallow) hingga SDK 36 (Android 16)

---

## 📂 Struktur Direktori Proyek

```
Nusantara-Bahari/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/          # Room Entities & DAOs (Waypoint, CatchLog, Maintenance, SOS)
│   │   │   │   │   ├── model/          # Data Models (Vessel, AtoN, Lighthouse, CPA, Anchor, Tide)
│   │   │   │   │   ├── repository/     # Maritime Repository & Offline Seed Data
│   │   │   │   │   └── util/           # Navigational Math, Tile Manager, NMEA & Audio Helpers
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/     # Reusable UI Maritime Components
│   │   │   │   │   ├── screens/        # MapVesselScreen, WeatherScreen, CatchLogScreen, dll.
│   │   │   │   │   ├── theme/          # Marine Color Palette & Typography
│   │   │   │   │   └── viewmodel/      # MaritimeViewModel & State Management
│   │   │   │   └── MainActivity.kt     # Entry Point & Navigation Host
│   │   │   └── AndroidManifest.xml
│   │   └── test/                       # Unit Tests (Robolectric & Local Tests)
│   └── build.gradle.kts
├── nusantara-bahari.apk                # Pre-built Ready-to-Install APK
├── build.gradle.kts
└── README.md
```

---

## 🚀 Cara Menjalankan & Membangun Proyek

### Prasyarat
1. **Android Studio** (Koala / Ladybug / Meerkat atau yang lebih baru)
2. **JDK 17** atau **JDK 21**
3. **Android SDK** (API Level 23 - 36)

### Langkah-langkah:
1. **Clone Repositori**:
   ```bash
   git clone https://github.com/alijayanet/navigasi-laut.git
   cd navigasi-laut
   ```
2. **Buka di Android Studio**:
   Pilih `File > Open`, arahkan ke folder proyek.
3. **Build APK Debug**:
   ```bash
   ./gradlew assembleDebug
   ```
   *File APK siap pakai akan dibuat di `app/build/outputs/apk/debug/app-debug.apk` atau dapat langsung diunduh berkas `nusantara-bahari.apk` di root direktori.*

---

## 📞 Pengembang & Kontak Resmi

Aplikasi ini dikembangkan dan dipelihara oleh:

* **Organisasi / Pengembang**: **ALIJAYA-NET**
* **WhatsApp / Telepon**: [**+62 819-4721-5703**](https://wa.me/6281947215703) (`081947215703`)
* **Repositori GitHub**: [https://github.com/alijayanet/navigasi-laut](https://github.com/alijayanet/navigasi-laut)
* **Wilayah**: Indonesia 🇮🇩

---

## 📄 Lisensi

Proyek ini dilisensikan di bawah lisensi MIT — silakan merujuk ke berkas [LICENSE](LICENSE) untuk ketentuan lebih lanjut.

*Hak Cipta © 2026 ALIJAYA-NET. Seluruh hak cipta dilindungi.*
