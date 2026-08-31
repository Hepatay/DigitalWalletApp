# VarlıkCep

VarlıkCep; gelir, gider, bütçe, birikim hedefleri ve yatırım portföyünü (altın & döviz) tek bir yerde yönetmenizi sağlayan, modern mimariyle geliştirilmiş, yerel öncelikli (offline-first) ve bulut senkronizasyonlu bir Android finans asistanıdır.

Uygulama; bankacılık, para transferi veya yatırım danışmanlığı hizmeti sunmaz. Gösterilen döviz, altın ve portföy değerleri bilgilendirme amaçlı referans verilerdir.

---

## 📱 Proje Bilgileri

| Alan | Değer |
| :--- | :--- |
| **Platform** | Android 7.0 (API 24) ve üzeri |
| **Geliştirme Dili** | Kotlin |
| **Kullanıcı Arayüzü** | XML, Material 3 Design Components, ViewBinding |
| **Mimari** | MVVM (Model-View-ViewModel), Repository Pattern, Offline-First |
| **Veritabanı** | Room Database (v14) & Firebase Firestore |
| **Kimlik Doğrulama** | Firebase Authentication (Google ile Giriş Yap & Misafir Modu) |
| **Paket Adı** | `com.epatay.digitalwallet` |
| **Sürüm** | `1.2.0` (`versionCode 3`) |

---

## 🌟 Temel Özellikler

### 1. Bütçe ve İşlem Yönetimi
- **Gelir & Gider Takibi:** Hızlı işlem ekleme, düzenleme ve silme.
- **Girdi Doğrulaması & Limit Sınırları:** Tüm parasal ve metin alanlarında özel filtreleme (`MoneyInputFilter`, `SafeTextInputFilter`), 999.999.999,99 ₺ tavan sınırı ve anlık hata geri bildirimi.
- **Kategori Bütçeleri (Limitleri):** Popüler kategori sıralaması (`Market`, `Yiyecek ve İçecek`, `Fatura ve Abonelikler`, `Ulaşım`, `Alışveriş`, `Ev`, `Araç`, `Kişisel`, `Sağlık`, `Eğlence`, `Eğitim`, `Spor ve Hobi`, `Seyahat`, `İş`, `Diğer`), her kategoriye özel renkli ikonlar ve bütçe aşım takibi.
- **Gelişmiş Arama ve Filtreleme:** Metin araması, kategori seçimi, gelir/gider türü, tarih aralığı ve **Harcama Tutarı Filtresi** (hazır çipli aralıklar: *0-500 ₺*, *500-2.500 ₺*, *2.500-10.000 ₺*, *10.000 ₺+* veya özel min-max girişi).
- **Düzenli Kayıtlar:** Aylık otomatik tekrarlanan fatura/abonelik tanımlamaları ve hatırlatıcı bildirimler.
- **Birikim Hedefleri:** Hedef tutar ve tarih belirleme, birikim hareketleri kaydetme ve dinamik ilerleme çubuğu.

### 2. Portföy ve Varlık Yönetimi
- **Döviz & Altın Takibi:** USD, EUR, GBP, CHF, CAD, AUD, SAR, AED, JPY vb. dövizler ile Gram, Çeyrek, Yarım, Tam, Cumhuriyet, Ata, Bilezik gibi altın türlerini portföye ekleme.
- **Bayrak & İkon Desteği:** Döviz listesinde resmi ülke bayrakları ve Türkçe para birimi adları, altın listesinde altın ikonları.
- **Kâr/Zarar ve Değerleme Analizi:** Alış maliyeti, güncel piyasa değeri, anlık kâr/zarar tutarı ve yüzdesi hesaplaması.
- **Portföy Pasta Grafiği:** Varlık dağılımını gösteren interaktif MPAndroidChart pasta grafiği.

### 3. Canlı Piyasalar ve Döviz Çevirici
- **TCMB Döviz Kurları:** Günlük resmi döviz alış/satış kurları ve çapraz kur hesaplamaları.
- **Canlı Altın Kurları:** Güncel alış, satış ve makas oranları.
- **Çevrimdışı Dayanıklılık (Offline Fallback):** İnternet olmadığında son başarılı piyasa verileri Room önbelleğinden kesintisiz sunulur.

### 4. Bulut Senkronizasyonu & Sıfır Bilgi (Zero-Knowledge) Güvenlik Mimarisi
- **🔒 Sıfır Bilgi & Uçtan Uca İstemci Şifreleme (AES-256-GCM):** Tüm finansal kayıtlar (tutar, bakiye, bütçe, altın, döviz, notlar) buluta aktarılmadan önce cihazda AES-256 ile şifrelenir. Firebase sunucusunda veriler yalnızca şifreli metin (ciphertext) olarak tutulur; geliştirici dahil hiç kimse kullanıcının finansal verilerini göremez.
- **Offline-First & İki Yönlü Senkronizasyon:** Çevrimdışıyken yapılan tüm ekleme/güncelleme/silme işlemleri yerel Room veritabanında tutulur, internet bağlantısı sağlandığında `WorkManager` ve `FirebaseSyncManager` ile Firestore'a aktarılır.
- **Soft-Delete Mimarisi:** Silinen kayıtlar bulutta ve yerelde senkronize edilerek veri kaybı ve çakışmalar engellenir.
- **Misafir Modu & Hesap Aktarımı:** Misafir olarak kullanılan veriler, Google ile giriş yapıldığında tek tıkla kullanıcının bulut hesabına taşınır.
- **Hesap ve Tüm Verileri Kalıcı Silme:** Tek tuşla tüm bulut ve yerel verileri anında yok etme seçeneği.

### 5. Dışa Aktarma (Raporlama)
- **Excel (`.xlsx`):** FastExcel ile oluşturulmuş, tüm işlem ve portföy sekmelerini içeren formatlı tablo.
- **PDF Raporu:** VarlıkCep logolu, işlem listesi ve portföy varlıklarını içeren resmi işlem özeti.
- **CSV Formatı:** UTF-8 kodlamalı, virgül/noktalı virgül uyumlu dışa aktarım.
- Android Storage Access Framework ile güvenli dosya kaydı.

### 6. Kullanıcı Deneyimi & Arayüz
- **İnteraktif Tanıtım Turu (Tutorial v2):** Uygulama özelliklerini tanıtan, canlı kurlarla örnek veriler oluşturan ve tur sonunda verileri otomatik temizleyen rehber overlay.
- **Bildirim Hapı (In-App Notification):** Standart Snackbar'ların aksine alttaki butonları kaydırmayan, ekranın üstünde zarifçe açılan durum bildirimleri.
- **Karanlık / Aydınlık Tema:** Sistem temasıyla tam uyumlu Material 3 renk paleti.

---

## 🛠️ Teknik Mimari ve Kullanılan Teknolojiler

```text
app/src/main/java/com/epatay/digitalwallet/
├── data/          # Room DAO, Entities, Mappers, Remote Data Sources (TCMB & Altın)
├── export/        # Excel (.xlsx), CSV ve PDF dışa aktarma motorları
├── recurring/     # Düzenli işlemler ve zamanlanmış bildirim altyapısı
├── sync/          # Firebase Firestore senkronizasyonu ve WorkManager işçileri
├── ui/            # Fragment, ViewModel, Adapter ve BottomSheet bileşenleri
│   ├── login/     # Google Sign-In & LoginActivity
│   └── tutorial/  # TutorialManager ve dinamik Canvas overlay
└── util/          # Girdi doğrulama, filtreleme, bildirim ve UI yardımcıları
```

- **Kotlin Coroutines & Flow:** Reaktif veri akışları ve asenkron işlemler.
- **Android Architecture Components:** ViewModel, StateFlow, LiveData, ViewBinding, Navigation.
- **Firebase:** Authentication (Google Sign-In) & Cloud Firestore.
- **WorkManager:** Periyodik kâr/zarar hesaplamaları ve arka plan senkronizasyonu.
- **Room Persistence Library:** Tip güvenli yerel SQLite veritabanı.
- **MPAndroidChart:** Portföy ve harcama analitiği grafikleri.
- **FastExcel:** `.xlsx` üretim motoru.

---

## 🔒 Gizlilik ve Güvenlik

- Tüm ağ iletişimleri TLS/HTTPS ile şifrelenir; düz metin (cleartext) trafiği manifest düzeyinde engellenmiştir.
- Kullanıcı verileri yalnızca kullanıcının kendi `user_id` alanı ile izole edilir ve üçüncü taraflarla paylaşılmaz.
- [Gizlilik Politikası (Web)](https://hepatay.github.io/DigitalWalletApp/)
- [Depodaki Gizlilik Politikası](docs/PRIVACY_POLICY_TR.md)

---

## 👨‍💻 Geliştirici

**Hüseyin Epatay**  
GitHub: [@Hepatay](https://github.com/Hepatay)