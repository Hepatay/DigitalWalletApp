# VarlıkCep

VarlıkCep; gelir, gider, bütçe, birikim hedefi ve yatırım varlıklarını tek
uygulamada takip etmeye yardımcı olan, Kotlin ile geliştirilmiş yerel öncelikli
bir Android uygulamasıdır.

Uygulama ödeme, para transferi veya yatırım danışmanlığı hizmeti sunmaz.
Gösterilen döviz, altın ve portföy değerleri bilgilendirme amaçlı referans
verilerdir ve yatırım tavsiyesi değildir.

## Proje bilgileri

| Alan | Değer |
| --- | --- |
| Platform | Android 7.0 (API 24) ve üzeri |
| Dil | Kotlin |
| Arayüz | XML, Material Components, ViewBinding |
| Yerel veri | Room Database |
| Paket adı | `com.epatay.digitalwallet` |
| Sürüm | `1.0.0` |

## Özellikler

### Bütçe ve işlemler

- Gelir ve gider ekleme, düzenleme ve silme
- Pozitif tutar, sayı biçimi ve zorunlu alan doğrulaması
- Yalnızca listeden seçilebilen gider kategorileri
- Aylık bütçe, kalan bütçe ve günlük harcanabilir tutar hesaplaması
- Kategori bazlı bütçeler ve aylık raporlar
- Metin, kategori, işlem türü ve tarih aralığına göre arama ve filtreleme
- Düzenli gelir/gider tanımlama ve yaklaşan kayıt bildirimleri
- Birikim hedefi, para ekleme/çekme ve ilerleme takibi

### Piyasalar ve döviz çevirici

- TCMB günlük döviz alış ve satış kurları
- TL dönüşümleri ve USD→EUR gibi çapraz döviz hesaplamaları
- Son başarılı kur verisini Room üzerinde çevrimdışı gösterme
- Desteklenen para birimleri için uygulama paketindeki yerel PNG bayraklar
- İnternetten bayrak görseli indirmeyen çevrimdışı görsel yapı
- apinoktam üzerinden altın alış ve satış referans fiyatları
- Döviz ve altın için ayrı Piyasalar sekmeleri

### Portföy

- Döviz ve altın varlığı ekleme, güncelleme ve silme
- Alış fiyatı, miktar, toplam maliyet ve referans güncel kur takibi
- Güncel tahmini değer ile kâr/zarar hesaplama
- Dövizler için bayraklarla uyumlu sabit grafik renkleri
- Altın varlıkları için sarı grafik gösterimi
- Kullanıcı verilerini koruyan Room migration zinciri

### Dışa aktarma

- Microsoft Excel, Google Sheets ve LibreOffice ile uyumlu gerçek `.xlsx`
  çalışma kitabı
- UTF-8 ve doğru CSV escape kurallarıyla `.csv` dışa aktarma
- Gelir ve gider işlemleri için `.pdf` rapor
- Gelir, gider, kategori bütçesi, döviz yatırımı ve altın yatırımı sütunları
- Android Storage Access Framework ile kullanıcı tarafından seçilen konuma
  kaydetme
- Uygun MIME türleri ve güvenli stream/workbook kapatma

### Kullanıcı arayüzü

- Portföyüm, Bütçem ve Piyasalar için sabit alt navigasyon
- Sistem çubukları, klavye ve reklam alanını dikkate alan WindowInsets yapısı
- İçeriği kapatmayan ortak banner reklam alanı
- Responsive kartlar ve yatay taşma oluşturmayan ekran düzeni
- Klavye açıldığında sabit kalan alt navigasyon
- Uygulama içinden erişilebilen Yasal, Gizlilik ve yatırım uyarısı bağlantıları
- Türkçe karakterleri koruyan UI, Room, arama ve dışa aktarma akışları

## Veri kaynakları

- Döviz kurları: [Türkiye Cumhuriyet Merkez Bankası](https://www.tcmb.gov.tr/kurlar/today.xml)
- Altın referans fiyatları: [apinoktam](https://apinoktam.erenozdemir.com.tr/)
- Reklam: Google Mobile Ads SDK
- İzin tercihleri: Google User Messaging Platform

Piyasa verileri gecikebilir veya geçici olarak erişilemez olabilir. Uygulama bu
durumda son başarılı yerel veriyi gösterebilir.

## Teknik yapı

- ViewModel ve Repository katmanları
- Room DAO, Flow ve StateFlow
- Kotlin Coroutines ve WorkManager
- Retrofit/OkHttp tabanlı HTTPS bağlantıları
- TCMB XML ayrıştırma katmanı
- MPAndroidChart grafik bileşenleri
- FastExcel ile OOXML `.xlsx` üretimi
- Android scoped storage ve Activity Result API

Ana kaynak dizinleri:

```text
app/src/main/java/com/epatay/digitalwallet/
├── data/       Room, ağ kaynakları, doğrulama ve hesaplama
├── export/     XLSX, CSV ve PDF dışa aktarma
├── recurring/  Düzenli kayıt ve bildirim işleri
└── ui/         Fragment, ViewModel ve adapter sınıfları
```

## Ekran görüntüleri

| Bütçem | Portföyüm | Piyasalar |
| --- | --- | --- |
| ![Bütçem](docs/play-store/screenshots/01-dashboard.png) | ![Portföyüm](docs/play-store/screenshots/02-portfolio.png) | ![Piyasalar](docs/play-store/screenshots/03-markets.png) |

## Kurulum

Gereksinimler:

- Güncel Android Studio
- JDK 17 veya üzeri
- Android SDK
- Android 7.0 veya üzeri cihaz/emülatör

Projeyi klonlayın:

```bash
git clone https://github.com/Hepatay/DigitalWalletApp.git
cd DigitalWalletApp
```

Projeyi Android Studio ile açın, Gradle senkronizasyonunu tamamlayın ve `app`
konfigürasyonunu çalıştırın. Canlı AdMob ve imzalama değerleri repoya eklenmez;
yerel yapılandırma dosyaları kullanılmalıdır.

Komut satırından doğrulama:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Windows:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

## Gizlilik

Finansal kayıtlar Room veritabanında kullanıcının cihazında saklanır. Hesap veya
bulut senkronizasyonu bulunmaz. Kullanıcı tarafından dışa aktarılan dosyalar,
yalnızca kullanıcının seçtiği konuma veya paylaşım hedefine yazılır.

- [Güncel gizlilik politikası](https://hepatay.github.io/DigitalWalletApp/)
- [Depodaki gizlilik politikası](docs/PRIVACY_POLICY_TR.md)
- [Üçüncü taraf bildirimleri](docs/THIRD_PARTY_NOTICES.md)

## Güvenlik

İmzalama anahtarları, parolalar, `local.properties`, `keystore.properties`,
`*.jks`, `*.keystore`, AAB ve APK dosyaları GitHub deposuna eklenmemelidir.

Güvenlik veya gizlilik bildirimleri için
[GitHub Issues](https://github.com/Hepatay/DigitalWalletApp/issues) kullanılabilir.

## Yasal uyarı

VarlıkCep bankacılık, ödeme, kredi, aracılık veya yatırım danışmanlığı hizmeti
sunmaz. Döviz ve altın fiyatları ile portföy, kâr ve zarar hesapları kesin fiyat
veya alım-satım teklifi değildir. Kullanıcılar finansal kararlarını kendi
değerlendirmeleri ve gerektiğinde yetkili uzman görüşüyle vermelidir.

## Geliştirici

Hüseyin Epatay

[GitHub: @Hepatay](https://github.com/Hepatay)
