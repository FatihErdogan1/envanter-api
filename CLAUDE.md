Bu repoda ana branch "master"dır, "main" değil.
develop branch'i yoksa master'dan oluştur.
Tüm feature branch'leri develop'tan aç ve develop'a merge et.
Hiçbir şeyi direkt master'a commit etme veya merge etme.

Projedeki git değişikliklerini incele.

1. git status ve git diff ile tüm API değişikliklerini gör

2. develop branch'i yoksa master'dan oluştur

3. Her feature için develop'tan branch aç, sadece o feature'a
   ait API dosyalarını stage'e al (git add . kullanma, tek tek ekle):

   feature/api-silme-kontrolu
   → silme kontrolü, 409 endpoint değişiklikleri

   feature/api-urun-tedarikci
   → /urunler/:id/tedarikciler endpoint, many-to-many model,
     migration dosyaları

   feature/api-depo-stok
   → /urunler/:id/stok-ozeti endpoint

   feature/api-islem-gecmisi
   → /urunler/:id/islem-gecmisi endpoint

4. Her branch için:
   - Dosyaları tek tek stage'e al
   - Commit at: feat(api): açıklama
   - develop'a merge et
   - Branch'i sil

5. Sonunda git log --oneline develop göster

Aynı dosyada birden fazla feature değişikliği varsa
git add -p ile satır bazlı ayır.
