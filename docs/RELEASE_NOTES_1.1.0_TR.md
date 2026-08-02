# VarlıkCep 1.1.0 — Kapalı Test Notları

- Altın kartlarında alış, satış, makas ve makas yüzdesi artık ayrı gösteriliyor.
- Portföy kartlarında piyasa alış/satış fiyatı ile kullanıcı alış fiyatı açıkça
  ayrılıyor; güncel tutar “Tahmini satış değeri” adıyla piyasa alış fiyatından
  hesaplanıyor.
- Yeni yatırımın ilk farkının alış-satış makasından kaynaklanabileceğini açıklayan
  bilgi eklendi. Gerçek kâr/zarar gizlenmiyor.
- Gram Altın ve Has Altın eşleştirmesi ayrıştırıldı; eski, eksik veya mantıksız
  altın fiyatları reddediliyor.
- İnternet veya servis hatasında son başarılı Room fiyatları uyarıyla gösteriliyor.
- Para hesaplamaları BigDecimal tabanlı ortak katmanda toplandı; Türkçe para biçimi
  ve kritik hesap senaryoları için yeni testler eklendi.
- Room 10→11 geçişi kullanıcı verilerini silmeden uygulanıyor.
- Portföyüm, Bütçem ve Piyasalar ekranları arasında sağa-sola kaydırma eklendi;
  alt menü seçimi kaydırılan ekranla birlikte güncelleniyor.
- TCMB'nin kaynak veri tarihi ile uygulamanın veriyi cihaza çektiği zaman artık
  portföy kartında ayrı “Veri” ve “Çekildi” etiketleriyle gösteriliyor.
- Bütçem ekranı sadeleştirildi: yaklaşan ödemeler, bütçe araçları ve işlemler
  açılıp kapatılabiliyor; kategori bütçeleri doğrudan erişilebilir durumda.
- Altın kartlarının görseli yenilendi.

Finansal veriler yalnızca referans ve bilgilendirme amaçlıdır; yatırım tavsiyesi
değildir.
