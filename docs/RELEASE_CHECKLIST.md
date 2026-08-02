# Google Play Yayın Kontrol Listesi

## Projede tamamlananlar

- Android 16 / API 36 `compileSdk` ve `targetSdk`
- AGP 8.13.2, Gradle 8.13, Kotlin 2.1.20 ve JDK 17
- R8 küçültme ve kaynak daraltma
- Room şema dışa aktarma; veri koruyan migration zinciri ve 9→10 döviz/altın migration'ı
- Room 10→11 kaynak-zaman migration'ı; destructive migration kullanılmıyor
- Düzenli ödeme/gelir kayıtları, günlük arka plan kontrolü ve yaklaşan tarih bildirimleri
- Aynı düzenli kaydın aynı ayda ikinci kez oluşmasını engelleyen dönem defteri
- Kategori bütçeleri, aylık rapor, işlem arama/filtreleme ve birikim hedefleri
- Excel uyumlu CSV ve çok sayfalı PDF dışa aktarma
- Finans kayıtları için bulut/cihaz aktarımı yedeklerinin kapatılması
- HTTPS dışındaki trafiğin kapatılması
- TCMB döviz, apinoktam altın ve yerel flag-icons kaynak-lisans açıklamaları
- API Noktam anahtarının `local.properties`/`BuildConfig` üzerinden alınması
- Uygulama içi gizlilik özeti
- Keystore dosyalarının Git dışında tutulması
- Özgün launcher simgesi ve 512×512 Play Store simgesi
- 1024×500 Play Store feature graphic
- Üç adet 1080×1920 telefon ekran görüntüsü
- Türkçe mağaza başlığı, kısa açıklama ve uzun açıklama taslağı
- Birim test, emülatör testi, release lint ve R8’li AAB derleme doğrulaması

## Geliştiricinin tamamlaması gerekenler

- Play App Signing'i etkinleştirip repo dışında bir upload key oluşturun.
- `keystore.properties.example` dosyasını `keystore.properties` olarak kopyalayıp gerçek yerel değerleri girin.
- `docs/PRIVACY_POLICY_TR.md` metnini herkese açık HTTPS sayfasında yayımlayın ve URL'yi Play Console'a girin.
- Nihai imzalı AAB’den önce en az bir fiziksel telefonda kapalı test yapın.
- Play Console'da Data Safety, Financial Features, içerik derecelendirmesi, hedef kitle, reklam ve uygulama erişimi formlarını doldurun.
- Data Safety beyanında Google Mobile Ads/UMP SDK'larının cihaz veya reklam tanımlayıcıları, yaklaşık konum, reklam etkileşimleri ve tanılama verilerini işleyebileceğini güncel Google SDK beyanına göre belirtin.
- Play Console'da **Reklam içerir** seçeneğini işaretleyin.
- Financial Features formunda uygulamayı ödeme/cüzdan hizmeti olarak değil, kişisel bütçe ve referans portföy takip aracı olarak doğru biçimde beyan edin.
- Altın API sağlayıcısının kullanım koşullarını yayın öncesinde seçilen kullanım planı bakımından yeniden kontrol edin.
- Uygulamayı kapalı test kanalında gerçek cihazlarda test edin.
- Her yeni yüklemede `versionCode` değerini artırın.
- Play Console'daki son kodun `2` değerinden küçük olduğunu doğrulayın; değilse
  AAB üretmeden önce `versionCode` değerini tekrar artırın.
- `local.properties` içine `APINOKTAM_API_KEY` tanımlayın ve API Noktam kota/
  anahtar kısıtlarını doğrulayın.

## 1.1.0 yerel doğrulaması — 02.08.2026

- Birim test: 51/51 başarılı; fiyat eşleştirme, makas, portföy değeri,
  Türkçe biçim, geçersiz veri ve offline fallback kapsamda.
- Emülatör instrumented test: 3/3 başarılı; 9→10 ve 10→11 veri koruma
  migration'ları kapsamda.
- Debug APK, R8’li release APK ve release AAB derlemesi başarılı.
- Debug/release lint: 0 hata, 366 yayın engellemeyen uyarı.
- Emülatörde 1.0.0 (`versionCode 1`) → 1.1.0 (`versionCode 2`) üzerine
  kurulum ve soğuk açılış başarılı; crash buffer boş.
- Upload key tanımlı olmadığı için üretilen release APK/AAB imzasızdır ve Play'e
  yüklenmemelidir.

## Önerilen mağaza sınıflandırması

Uygulama ödeme veya para transferi yapan bir mobil cüzdan değildir. Kişisel bütçe/portföy takip aracı olarak tanımlanmalı; “portfolio management” veya uygun “other financial feature” seçeneği değerlendirilmelidir.
