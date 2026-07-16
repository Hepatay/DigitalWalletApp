# 🚀 Dijital Cüzdan (Digital Wallet)

Güncel döviz kurlarını takip etmenizi ve anlık hesaplamalar yapmanızı sağlayan modern, hızlı ve kullanıcı dostu bir Android uygulaması.

## ✨ Özellikler

*   **Anlık Döviz Çevirici (Live Converter):** Kullanıcı tarafından girilen miktarı anlık olarak tüm para birimlerine (USD, EUR, GBP, JPY, vb.) dönüştürür. Ekstra bir hesaplama butonuna ihtiyaç duymaz.
*   **Dinamik Liste Yapısı:** Veriler, performanslı ve esnek bir yapı olan `RecyclerView` kullanılarak listelenir.
*   **Çevrimdışı Çalışma (Offline Caching):** En son çekilen kur verileri `SharedPreferences` ile cihaz hafızasına kaydedilir. Uygulama internet olmadan açıldığında bile son verileri gösterir.
*   **Hata Yönetimi ve UI/UX:** İnternet bağlantısı kesintilerinde kullanıcıya nazik uyarılar verir ve veri çekilirken yükleme durumu (ProgressBar) sunar.

## 🛠 Kullanılan Teknolojiler (Tech Stack)

*   **Dil:** Kotlin
*   **Mimari ve Arayüz:** ViewBinding, RecyclerView, LinearLayout/ConstraintLayout, Adapter Pattern
*   **Ağ (Network):** API Entegrasyonu (Retrofit), Ağ Durumu Dinleme
*   **Veri Saklama:** SharedPreferences (Caching)

## 📸 Ekran Görüntüleri
*(Buraya uygulamanın ekran görüntüsünü ekleyebilirsiniz. Örn: `![App Screenshot](link-to-image)`)*

## 🗺️ Yol Haritası (Roadmap)
- [x] Temel UI ve kısıt (constraint) düzeltmeleri
- [x] RecyclerView ve Adapter entegrasyonu
- [x] Anlık TextWatcher tabanlı döviz hesaplayıcı
- [ ] **Room Database** entegrasyonu ile kişisel bakiye ve cüzdan yönetimi
- [ ] Detaylı portföy (toplam varlık) hesaplama ekranı

---

**Geliştirici:** Hüseyin Epatay  
**GitHub:** [@Hepatay](https://github.com/Hepatay)
