Aplikasi Catat Keuangan Pake AI
Ini bukan aplikasi pencatat keuangan biasa yang cuma bisa nambah pemasukan atau pengeluaran. Di project ini, gw ngembangin fitur AI dari OpenRouter buat jadi asisten keuangan pribadi dan negosiator budget. Jadi aplikasinya bisa nganalisis kebiasaan jajan kita berdasarkan catatan transaksinya.

Fitur Utama
- Asisten Keuangan AI: Bisa diajak ngobrol soal kondisi keuangan. AI-nya bisa bedain mana pengeluaran yang bagus kaya beli buku, dan mana yang jelek kaya foya-foya, trus ngasih saran atau bahkan ngeroasting pengeluaran kita.
- Negosiator Budget: Fitur buat deal-dealan jatah jajan harian. Kalau kita udah sepakat sama satu angka, AI bakal ngeluarin kode khusus yang dibaca sama sistem aplikasi buat ngunci budget tersebut. Nanti bakal ada peringatan kalau pengeluaran kita lewat dari batas deal-dealan itu.
- Database Offline: Aplikasi ini tetep nyimpen data dengan aman di hp pake Room Database buat riwayat transaksi dan DataStore buat nyimpen kontrak budget harian.
- UI dan UX: Pake Bottom Navigation biar gampang pindah menu, dan state viewmodel yang dijaga biar history chat ga ilang pas pindah layar.
- Bahasa yang Dipake: Kotlin

Cara Jalanin Project Ini
Kalau mau coba run project ini di laptop lu, ikutin langkah ini:
- Clone repo ini ke laptop lu pake perintah:
git clone https://github.com/DiyasGar/catat-keuangan-ai.git

- Buka projectnya pake Android Studio atau apapun itu.
- Lu butuh API Key dari OpenRouter buat nyalain fitur AI-nya. Bikin akun dan ambil API Key gratis di web OpenRouter.
- Buka file local.properties di root folder project lu, terus tambahin baris ini:
OPENROUTER_API_KEY=isi_api_key_lu_disini

- Run projectnya di emulator atau hp android asli.

Screenshot dan Demo
<img width="591" height="1280" alt="WhatsApp Image 2026-04-17 at 16 10 36 (1)" src="https://github.com/user-attachments/assets/5b2c878e-3b22-4242-988a-771c352355ca" />
<img width="591" height="1280" alt="WhatsApp Image 2026-04-17 at 16 10 35" src="https://github.com/user-attachments/assets/c65eef42-c2c1-4bf9-bb61-80fc9c2b16e5" />
<img width="591" height="1280" alt="WhatsApp Image 2026-04-17 at 16 10 37" src="https://github.com/user-attachments/assets/5bf38978-dafb-4ea6-ad71-b9ffe1924a3e" />
<img width="591" height="1280" alt="WhatsApp Image 2026-04-17 at 16 10 37 (1)" src="https://github.com/user-attachments/assets/f6346692-2653-4d6d-b5e2-9e1b38bf7340" />
<img width="591" height="1280" alt="WhatsApp Image 2026-04-17 at 16 10 36" src="https://github.com/user-attachments/assets/f985e1dd-0aaa-409e-962c-2161543804e3" />
<img width="591" height="1280" alt="WhatsApp Image 2026-04-17 at 16 10 36 (2)" src="https://github.com/user-attachments/assets/48bbcfe6-4d44-496c-86d4-4a2b2b6221a7" />


Dibuat oleh Diaz Garcia Pratama.
