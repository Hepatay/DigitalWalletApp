# Google Play Yayın Kontrol Listesi

## Projede tamamlananlar

- Android 16 / API 36 `compileSdk` ve `targetSdk`
- AGP 8.13.2, Gradle 8.13, Kotlin 2.1.20 ve JDK 17
- R8 küçültme ve kaynak daraltma
- Room şema dışa aktarma; veri koruyan 2→4, 3→4, 4→5, 5→6, 6→7 ve 7→8 migration
- Düzenli ödeme/gelir kayıtları, günlük arka plan kontrolü ve yaklaşan tarih bildirimleri
- Aynı düzenli kaydın aynı ayda ikinci kez oluşmasını engelleyen dönem defteri
- Kategori bütçeleri, aylık rapor, işlem arama/filtreleme ve birikim hedefleri
- Excel uyumlu CSV ve çok sayfalı PDF dışa aktarma
- Finans kayıtları için bulut/cihaz aktarımı yedeklerinin kapatılması
- HTTPS dışındaki trafiğin kapatılması
- Exchange Rate API atfı ve günlük referans kur açıklaması
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
- Uygulamayı kapalı test kanalında gerçek cihazlarda test edin.
- Her yeni yüklemede `versionCode` değerini artırın.

## Son doğrulama

- Birim test: tarih, bütçe, birikim, dışa aktarma ve düzenli kayıt sınır durumları başarılı
- Emülatör testi (API 35): veri koruyan migration, otomatik aylık kayıt, çift kayıt engelleme, bildirim, yeni bütçe/rapor/hedef ekranları ve açık/koyu tema başarılı
- Release lint: 0 hata; yayın engellemeyen modernizasyon/yerelleştirme uyarıları mevcut
- R8’li release AAB: başarıyla üretildi; upload key henüz tanımlanmadığı için bu doğrulama paketi imzasızdır

## Önerilen mağaza sınıflandırması

Uygulama ödeme veya para transferi yapan bir mobil cüzdan değildir. Kişisel bütçe/portföy takip aracı olarak tanımlanmalı; “portfolio management” veya uygun “other financial feature” seçeneği değerlendirilmelidir.
