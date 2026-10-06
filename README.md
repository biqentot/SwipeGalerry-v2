# Swipe Gallery

Android app untuk menyortir galeri foto dengan swipe cepat. Swipe kiri untuk buang, swipe kanan untuk simpan. Foto yang dibuang masuk Trash dan bisa dikembalikan.

## Fitur

- Pilih folder (Internal / SD Card / Semua) sebelum mulai
- Swipe kiri (buang) & swipe kanan (simpan)
- Trash dengan restore & hapus permanen
- Auto-hapus trash setelah 30 hari (maks 500 MB)
- Dua bahasa: Indonesia & English
- Dark mode

## Tech Stack

- Kotlin
- Android SDK 35, min SDK 30
- Room Database
- Glide
- Material Components
- GitHub Actions (build APK otomatis)

## Build

APK otomatis di-build lewat GitHub Actions setiap push ke `main`.
Download dari tab **Actions** → pilih run terbaru → **Artifacts** → `app-debug`.

## Struktur
