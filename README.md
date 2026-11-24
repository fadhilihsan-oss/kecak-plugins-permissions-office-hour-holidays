# Panduan Konfigurasi: Holiday & Work Hour Permission

Panduan ini berisi langkah-langkah untuk mengonfigurasi plugin kontrol akses pada Kecak Workflow. Terdapat dua jenis pembatasan akses yang dibahas dalam dokumen ini:
1. **Holiday Permission**: Membatasi akses berdasarkan tanggal/hari libur.
2. **Work Hour Permission**: Membatasi akses berdasarkan rentang jam operasional (jam kerja).

---

## Bagian 1: Holiday Permission

**Holiday Permission** (*Permission by Date*) adalah plugin kontrol akses pada Kecak Workflow yang berfungsi untuk membatasi akses pengguna ke **Form** maupun **Menu Userview** pada tanggal-tanggal libur yang telah ditentukan.

### 1.1 Prasyarat Data
Sebelum memasang plugin ini pada Form atau Menu, pastikan Anda sudah memiliki:
* Form dan tabel khusus untuk **Master Data Hari Libur** (misalnya `master_holiday`).
* Terdapat setidaknya satu data hari libur yang telah diinput dengan status **`AKTIF`**.

### 1.2 Langkah Konfigurasi & Pemasangan

#### A. Mengelola Data Hari Libur
1. Buka menu **Master Data Hari Libur** pada portal.
2. Klik tombol **Tambah/New**, lalu lengkapi informasi berikut:
    * **Tanggal Libur:** Pilih tanggal yang akan diberlakukan sebagai hari libur.
    * **Keterangan:** Isi nama atau alasan libur (contoh: *Cuti Bersama* atau *Libur Nasional*).
    * **Status:** Pastikan Anda memilih/mengisi dengan nilai **`AKTIF`**.
3. Klik **Save** untuk menyimpan data libur tersebut.

#### B. Menerapkan Plugin ke Form atau Menu
1. Masuk ke ruang desain aplikasi Anda (*App Design*).
2. Tentukan di mana *permission* ini akan dipasang:
    * **Di Form:** Buka **Form Builder**, masuk ke tab **Properties** -> **Advanced** -> **Permission**.
    * **Di Userview (Menu):** Buka **Userview Builder**, lalu klik **Configure Permission** pada menu yang ingin dibatasi.
3. Pada pilihan *Permission Type*, pilih **Permission by Date**.
4. Arahkan sumber referensi *permission* ini ke tabel `master_holiday` yang sudah disiapkan sebelumnya.
5. Klik **OK** pada jendela konfigurasi, lalu klik tombol **SAVE** di bagian atas *Builder*.

#### C. Meninjau Hasil
* Saat pengguna biasa (non-admin) mencoba membuka Form/Menu tersebut pada tanggal yang berstatus `AKTIF` di data libur, akses akan langsung ditolak (*Access Denied*).
* Untuk membuka kembali aksesnya, cukup ubah status tanggal tersebut menjadi `TIDAK AKTIF` melalui menu Master Data Hari Libur.

---

## Bagian 2: Work Hour Permission

**Work Hour Permission** adalah plugin kontrol akses pada Kecak Workflow yang berfungsi untuk membatasi hak akses pengguna ke **Form** maupun **Menu Userview** berdasarkan rentang jam operasional atau jam kerja yang diizinkan.

### 2.1 Prasyarat
Pastikan Form atau Menu Userview yang ingin dibatasi hak aksesnya sudah selesai dibuat dan siap dikonfigurasi.

### 2.2 Langkah Konfigurasi & Pemasangan

#### A. Membuka Pengaturan Permission
1. Tentukan di mana *permission* ini akan dipasang:
    * **Di Form:** Buka **Form Builder**, klik tab **Properties** -> **Advanced** -> **Permission**.
    * **Di Userview (Menu):** Buka **Userview Builder**, lalu klik menu yang dituju dan pilih **Configure Permission**.
2. Pada *dropdown* pilihan tipe *permission*, pilih **Work Hour Permission**.

#### B. Mengatur Interval Jam Kerja (Update Terbaru)
Berdasarkan pembaruan format terbaru, jeda waktu istirahat dikonfigurasi dengan cara memecah atau membuat baris interval waktu baru.

1. Pada bagian **Allowed Work Intervals**, klik ikon **`+`** (plus) untuk menambahkan baris jam operasional.
2. Atur jam mulai dan jam selesai menggunakan pilihan *dropdown* atau format 24 jam (**HH:MM**).
    * **START TIME:** Jam mulai akses diizinkan.
    * **FINISH TIME:** Jam batas akhir akses diizinkan.
3. **Mengatur Waktu Istirahat:** Tambahkan baris interval baru untuk memisahkan jam kerja.
    * *Contoh Pengaturan (Istirahat jam 12:00 - 13:00):*
        * **Baris 1:** `08:00` - `12:00`
        * **Baris 2:** `13:00` - `17:00`
    * Dengan format di atas, akses form/menu akan otomatis ditolak pada pukul 12:01 hingga 12:59.
4. Anda bisa mengatur urutan baris menggunakan ikon panah atas/bawah, atau menghapus baris dengan ikon **`x`** silang merah.

#### C. Menyimpan Konfigurasi
1. Klik **OK** pada jendela konfigurasi *Work Hour Permission*.
2. Klik tombol **SAVE** di bagian atas *Builder* untuk menyimpan seluruh pengaturan ke dalam aplikasi.

---

## Bagian 3: Menggabungkan Kedua Permission (Opsional)
Jika Anda ingin menerapkan **kedua aturan secara bersamaan** (Form/Menu hanya bisa diakses pada jam kerja DAN bukan pada hari libur), ikuti langkah berikut:
1. Pada pilihan *Permission Type*, pilih **Composite Permission**.
2. Tambahkan **Permission by Date** (Holiday) dan **Work Hour Permission** ke dalam konfigurasi Composite tersebut.
3. Atur logika penggabungan menjadi **`AND`**.
4. Lakukan konfigurasi interval jam dan referensi tabel libur sesuai panduan di atas.