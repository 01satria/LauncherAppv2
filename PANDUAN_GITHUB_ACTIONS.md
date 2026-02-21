# Panduan Setup GitHub Actions → Release APK

## Gambaran Alur

```
Push tag (v1.0) ──► GitHub Actions build ──► APK di-sign ──► GitHub Release
```

---

## Langkah 1 — Buat Keystore (sekali saja)

Keystore adalah "sertifikat digital" yang menandatangani APK kamu.
**Simpan file ini baik-baik — tidak bisa dibuat ulang jika hilang.**

Jalankan di terminal / Command Prompt:

```bash
keytool -genkey -v \
  -keystore satria-launcher.jks \
  -alias satria \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Kamu akan ditanya beberapa hal:
- **Enter keystore password**: buat password yang kuat (catat!)
- **Re-enter new password**: ulangi
- **What is your first and last name?**: nama kamu
- **What is the name of your organizational unit?**: bisa kosong
- **What is the name of your organization?**: bisa nama app
- **What is the name of your City or Locality?**: kota kamu
- **What is the name of your State or Province?**: provinsi
- **What is the two-letter country code?**: `ID`
- **Is CN=... correct?**: ketik `yes`
- **Enter key password** (bisa sama dengan keystore password): catat!

Hasilnya: file `satria-launcher.jks`

---

## Langkah 2 — Encode Keystore ke Base64

GitHub Secrets hanya bisa menyimpan teks, bukan file binary.
Kita encode dulu ke base64:

**macOS / Linux:**
```bash
base64 -i satria-launcher.jks | tr -d '\n'
```

**Windows PowerShell:**
```powershell
[Convert]::ToBase64String([System.IO.File]::ReadAllBytes("satria-launcher.jks"))
```

Salin output-nya (string panjang), akan dipakai di langkah berikutnya.

---

## Langkah 3 — Tambahkan Secrets ke GitHub

1. Buka repo kamu di GitHub
2. Klik **Settings** (tab paling kanan)
3. Klik **Secrets and variables** → **Actions**
4. Klik **New repository secret**
5. Tambahkan **4 secrets** berikut:

| Secret Name | Value | Keterangan |
|---|---|---|
| `KEYSTORE_BASE64` | output base64 dari langkah 2 | File keystore ter-encode |
| `KEYSTORE_PASSWORD` | password keystore kamu | Password saat `keytool -genkey` |
| `KEY_ALIAS` | `satria` | Alias yang kamu tulis di `-alias` |
| `KEY_PASSWORD` | password key kamu | Bisa sama dengan KEYSTORE_PASSWORD |

---

## Langkah 4 — Struktur File di Repo

Pastikan file-file ini ada di repo:

```
SatriaLauncher/
├── .github/
│   └── workflows/
│       └── release.yml        ← file workflow (sudah disertakan)
├── app/
│   └── build.gradle.kts       ← sudah ada ABI split & signing config
└── gradlew                    ← Gradle wrapper (wajib ada)
```

> ⚠️ **Penting:** File `gradlew` harus ada dan bisa dieksekusi.
> Jika belum ada, generate dulu di Android Studio:
> **View → Tool Windows → Terminal** lalu ketik:
> ```bash
> gradle wrapper --gradle-version 8.9
> ```

---

## Langkah 5 — Push Tag untuk Trigger Build

```bash
# Pastikan semua perubahan sudah di-commit
git add .
git commit -m "feat: add GitHub Actions release workflow"

# Buat tag versi (sesuaikan angkanya)
git tag v1.0

# Push tag ke GitHub → ini yang memicu build!
git push origin v1.0
```

---

## Langkah 6 — Pantau Build

1. Buka repo GitHub
2. Klik tab **Actions**
3. Kamu akan lihat workflow "Build & Release APK" sedang berjalan
4. Klik untuk melihat log real-time

Build biasanya selesai dalam **3–7 menit**.

---

## Langkah 7 — Download APK dari Releases

Setelah build sukses:

1. Klik tab **Releases** di repo GitHub
2. Akan ada release baru bernama "Satria Launcher v1.0"
3. Di bagian **Assets**, ada 3 APK:
   - `SatriaLauncher-1.0-arm64-v8a.apk` ← **untuk HP modern**
   - `SatriaLauncher-1.0-armeabi-v7a.apk` ← untuk HP lama
   - `SatriaLauncher-1.0-x86_64.apk` ← untuk emulator

---

## Update Versi Berikutnya

Untuk release versi baru:

```bash
# 1. Update versionCode dan versionName di app/build.gradle.kts
#    versionCode = 2
#    versionName = "1.1"

# 2. Commit
git add app/build.gradle.kts
git commit -m "bump version to 1.1"

# 3. Tag dan push
git tag v1.1
git push origin v1.1
```

---

## Troubleshooting

### ❌ "Keystore file not found"
Pastikan step "Decode keystore" berhasil dan path-nya benar:
`$GITHUB_WORKSPACE/app/keystore.jks`

### ❌ "Wrong password" / "Key not found"
Cek kembali nilai di GitHub Secrets — pastikan tidak ada spasi tersembunyi.
Coba hapus dan buat ulang secret-nya.

### ❌ Build gagal di Gradle sync
- Cek `gradlew` ada di root repo
- Cek `gradle/libs.versions.toml` ada
- Lihat log Actions untuk pesan error spesifik

### ❌ APK tidak ter-sign / "apk is not signed"
Pastikan 4 environment variable terbaca di step "Build Release APKs":
```yaml
env:
  KEYSTORE_PATH:     ${{ github.workspace }}/app/keystore.jks
  KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
  KEY_ALIAS:         ${{ secrets.KEY_ALIAS }}
  KEY_PASSWORD:      ${{ secrets.KEY_PASSWORD }}
```

---

## Tips

- **Jangan commit file `keystore.jks` ke git** — tambahkan ke `.gitignore`:
  ```
  *.jks
  *.keystore
  ```
- Simpan keystore + password di tempat aman (cloud storage pribadi)
- `versionCode` harus selalu naik setiap release (tidak boleh turun)
