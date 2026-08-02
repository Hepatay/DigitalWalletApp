# Değişiklik Günlüğü

Bu proje [Keep a Changelog](https://keepachangelog.com/tr/1.1.0/) yaklaşımını
izler. Sürümler `MAJOR.MINOR.PATCH` biçimindedir.

## [1.1.0] - 2026-08-02

### Eklendi

- Beş desteklenen altın ürünü için alış, satış, makas ve makas yüzdesi.
- Kaynak veri zamanı ile uygulamanın çekim zamanını ayıran alanlar.
- BigDecimal tabanlı ortak portföy hesaplama ve piyasa fiyatı doğrulama katmanı.
- Makas, portföy değeri, Türkçe sayı biçimi, ters alan, geçersiz veri ve offline
  fallback regresyon testleri.
- Room 10→11 veri koruyan migration ve migration instrumented testi.

### Değiştirildi

- Portföy değeri piyasa alış fiyatıyla “Tahmini satış değeri” olarak hesaplanır.
- Otomatik alış fiyatı önerisi piyasa satış fiyatı olarak açıkça etiketlendi;
  manuel gerçek alış fiyatı girişi korunur.
- Altın kaynağı “API Noktam / Trunçgil Finans” olarak açıkça gösterilir.
- `versionCode` 2 ve `versionName` 1.1.0 olarak güncellendi.

### Düzeltildi

- Gram Altın ile Has Altın ürünlerinin karışmasını engelleyen kesin tür eşleştirmesi.
- Eksik, yinelenmiş, eski, sıfır, negatif, aşırı büyük ve ters alış/satış
  verilerinin başarılı veri olarak Room'a yazılması.
- Başarısız yenilemenin son sağlam fiyat setini silme riski.

### Güvenlik

- API anahtarı `local.properties` üzerinden üretilen `BuildConfig` alanına taşındı;
  repoda sabit anahtar tutulmuyor.

## [1.0.0]

- İlk kapalı test sürümü.
