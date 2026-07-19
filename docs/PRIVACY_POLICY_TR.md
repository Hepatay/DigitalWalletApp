# Dijital Cüzdan Gizlilik Politikası

Son güncelleme: 19 Temmuz 2026

Dijital Cüzdan, kişisel bütçe ve portföy takibi amacıyla geliştirilmiştir. Uygulama hesap oluşturmaz, reklam göstermez ve analiz/izleme SDK'sı kullanmaz.

## İşlenen veriler

- Gelir, gider, kategori, tarih, yatırım miktarı ve alış fiyatı gibi kullanıcı kayıtları yalnızca cihazdaki yerel veritabanında tutulur.
- Aylık limit ve son alınan piyasa verileri cihazdaki yerel tercihlerde saklanır.
- Bu kişisel finans kayıtları geliştiriciye veya piyasa verisi sağlayıcılarına gönderilmez.
- Bulut ve cihazlar arası otomatik yedekleme kapalıdır.

## Üçüncü taraf piyasa verileri

Uygulama piyasa verilerini göstermek için [Exchange Rate API](https://www.exchangerate-api.com/) ve [Gold API](https://gold-api.com/) servislerine HTTPS üzerinden istek gönderir. Bu sağlayıcılar IP adresi, istek zamanı ve IP adresinden türetilebilen yaklaşık konum gibi teknik bağlantı verilerini kendi gizlilik politikalarına göre işleyebilir. Uygulama bu isteklere kullanıcının finans kayıtlarını eklemez.

## Saklama ve silme

Yerel kayıtlar kullanıcı silene, uygulama verileri temizlenene veya uygulama kaldırılana kadar cihazda kalır. Uygulamanın kaldırılması ya da verilerin temizlenmesi sonrasında geliştiricinin bu kayıtları geri getirmesi mümkün değildir.

## Çocukların gizliliği

Uygulama özellikle çocuklara yönelik değildir ve bilerek çocuklardan kişisel veri toplamaz.

## Değişiklikler ve iletişim

Bu politika uygulamanın işlevleri veya yasal gereksinimler değiştiğinde güncellenebilir. Sorular ve talepler için geliştiriciye [GitHub profili](https://github.com/Hepatay) üzerinden ulaşılabilir.

Bu metin Play Console'a girilecek herkese açık gizlilik politikası sayfasının kaynağıdır. Yayından önce bu dosya herkese açık bir HTTPS adresinde yayımlanmalı ve mağaza kaydına eklenmelidir.
