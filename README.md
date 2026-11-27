<!-- meticulous and honest -->
<!-- DINONAKTIFKAN: Template inisiasi repository awal belum memiliki penjelasan arsitektur dan panduan proyek -->
<!-- # VoidsMood -->
<!-- BARU: Dokumentasi komprehensif pondasi VoidsMood, arsitektur antarmuka, dan instruksi build -->

# 🌌 VoidsMood

> **The Silent Horizon — Modern, Fast, and Uncompromised Media Architecture for Android**

VoidsMood adalah klien media generasi baru yang dirancang khusus untuk memberikan pengalaman visual dan audio tanpa beban (*zero-slop, zero-ads, ultra-smooth 120Hz*), dioptimasi untuk perangkat entry-level dan ARMv7 (Infinix Smart 9 HD) hingga smartphone flagship.

---

### ✨ Bahasa Desain & Estetika Awal (Fondasi "Rumah")
1. **Glassmorphism 35%**: Permukaan kartu dan navigasi tembus pandang 35% dengan aksen garis tepi halus (*subtle border highlight*).
2. **Auramorphism**:
   - **Faded Grey Charcoal (45%)**: Gradasi arang abu-abu lembut penyeimbang visual.
   - **Dark Blue Ocean (50%)**: Kedalaman aura samudra malam berpadu dengan dasar **AMOLED Pure Black (`#000000`)**.
3. **Ikonografi Lucide**: Ikon vektor minimalis dengan stroke presisi dan touch target ramah jari (>= 48dp).
4. **Smooth Sliding Drawer**: Panel drawer sebelah kiri yang meluncur mulus ke kanan dengan animasi pegas yang responsif.
5. **No Debug — Release R8 First**: Dikonfigurasi langsung menggunakan kompilasi **R8 Code Shrinking** dan **Resource Minification** agar bebas lag dan frame-drop di layar 60Hz/120Hz.

---

### 🛠️ Rekayasa & Standar Teknis
* **UI Toolkit:** 100% Jetpack Compose + Material 3 Expressive
* **Min SDK / Target SDK:** Android 8.0 (API 26) / Android 15 (API 35)
* **Arsitektur CPU:** Universal (`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`)
* **Build System:** Gradle 9.9.2 (Kotlin DSL `.kts`) + Java 21
* **Otomasi CI/CD:** GitHub Actions otomatis mem-build Release APK dan mengirimkannya ke Telegram Pengembang.

---
*Dikembangkan dengan penuh dedikasi oleh Mjurran (MUHAMMAD ZAQI PRASETYO)*
