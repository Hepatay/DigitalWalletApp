# VarlıkCep Gizlilik ve Güvenlik Politikası

**Son güncelleme:** 31 Ağustos 2026

VarlıkCep, kullanıcıların kişisel bütçe, gelir, gider ve yatırım kayıtlarını güvenle takip edebilmesi amacıyla Hüseyin Epatay tarafından geliştirilmiştir.

VarlıkCep olarak gizliliğinize en üst düzeyde önem veriyoruz. Bu politika; verilerinizin nasıl korunduğunu, şifrelendiğini ve yönetildiğini açıklar.

---

## 🔒 1. Sıfır Bilgi (Zero-Knowledge) & Uçtan Uca Şifreleme Garantisi

VarlıkCep, **Sıfır Bilgi (Zero-Knowledge)** güvenlik mimarisiyle tasarlanmıştır:

- **Cihaz Düzeyinde Şifreleme:** Bütçe, gelir, gider, birikim hedefleri ve portföy kayıtlarınız (tutar, miktar, birim fiyat, notlar vb.) buluta (Google Firebase) gönderilmeden önce **kullanıcının kendi cihazında AES-256-GCM ile şifrelenir.**
- **Geliştirici veya Sunucu Verilerinizi Göremez:** Şifre çözme anahtarı yalnızca kullanıcının kendi cihazında üretilir. Firebase veritabanında kayıtlarınız yalnızca anlamsız şifreli metin (ciphertext) olarak tutulur. **Geliştirici dahil hiç kimse harcamalarınızı, bakiyelerinizi veya varlıklarınızı göremez.**
- **İzole Veri:** Hiçbir kullanıcı bir başka kullanıcının verisine erişemez veya değiştiremez.

---

## 📱 2. Misafir Modu & Yerel Saklama

- Uygulama, Google hesabı olmadan **tamamen çevrimdışı (Misafir Modu)** olarak kullanılabilir.
- Misafir modunda hiçbir veriniz internete çıkmaz, yalnızca telefonunuzun dahili hafızasındaki yerel veritabanında (Room DB) saklanır.
- Google ile oturum açtığınızda ise verileriniz uçtan uca şifreli olarak yedeklenir ve farklı cihazlarınız arasında senkronize edilir.

---

## 🗑️ 3. Hesap ve Tüm Verilerin Kalıcı Olarak Silinmesi

Kullanıcılar verileri üzerinde %100 silme ve kontrol hakkına sahiptir:

- **Tek Tıkla Kalıcı Silme:** Profil ekranında bulunan **"Hesabımı ve Tüm Verilerimi Sil"** butonuna basıldığında;
  1. Bulut veritabanındaki tüm şifreli bütçe ve portföy kayıtları,
  2. Firebase üzerindeki kullanıcı hesabı ve kimlik bilgileri,
  3. Cihazdaki yerel veritabanı ve önbellek,
  **anında ve kalıcı olarak yok edilir.**

---

## 📤 4. Dışa Aktarma (Excel / PDF / CSV)

- Kullanıcı dilediği zaman bütçe ve portföy raporlarını `.xlsx`, `.csv` veya `.pdf` formatında dışa aktarabilir.
- Dışa aktarılan dosyalar yalnızca kullanıcının seçtiği yerel cihaz dizinine yazılır; sunuculara otomatik olarak gönderilmez.

---

## 📊 5. Piyasa Verileri ve Reklamlar

- **Piyasa Kurları:** Gösterge döviz kurları TCMB'den, altın fiyatları ise referans servislerden HTTPS ile alınır. Finansal verileriniz bu servislere gönderilmez.
- **Reklamlar:** Uygulamada banner reklam gösterimi için Google AdMob kullanılır. Google AdMob standart teknik verileri (IP, yaklaşık konum, cihaz tanımlayıcıları) kendi politikalarına göre işleyebilir. Finansal verileriniz asla reklam sağlayıcılarıyla paylaşılmaz.
- **Gizlilik Tercihleri:** Kullanıcılar alt menüdeki **Tercihler / Gizlilik** alanından reklam izinlerini istedikleri zaman değiştirebilirler.

---

## ⚠️ 6. Yasal ve Finansal Sorumluluk Sınırı

VarlıkCep bir ödeme, para transferi, aracılık veya yatırım danışmanlığı uygulaması değildir. Gösterilen piyasa ve portföy verileri genel bilgilendirme ve kişisel takip amaçlıdır; yatırım tavsiyesi niteliği taşımaz.

---

## 📬 7. İletişim

Gizlilik politikası veya veri güvenliği ile ilgili her türlü soru ve talebiniz için:

- **Geliştirici:** Hüseyin Epatay  
- **İletişim & Destek:** https://github.com/Hepatay/DigitalWalletApp/issues

