💼 VarlıkCep — Kişisel Finans ve Portföy Takibi

VarlıkCep, gelirlerinizi, giderlerinizi, bütçelerinizi, birikim hedeflerinizi ve yatırım varlıklarınızı tek bir uygulama üzerinden takip etmenizi sağlayan modern bir Android uygulamasıdır.

Uygulama; kişisel finans yönetimini kolaylaştırmayı, günlük referans döviz ve gram altın verilerini anlaşılır bir arayüzle sunmayı ve kullanıcıların mali durumlarını daha düzenli takip etmelerine yardımcı olmayı amaçlar.

Sürüm: 1.0.0Platform: AndroidDil: KotlinPaket adı: com.epatay.digitalwallet

📌 İçindekiler

Özellikler

Kullanılan teknolojiler

Proje yapısı

Ekran görüntüleri

Kurulum

Gizlilik

Yol haritası

Yasal uyarı

Geliştirici

✨ Özellikler

💱 Döviz ve Gram Altın Takibi

USD, EUR, GBP, CHF, JPY, CAD, AUD, RUB ve CNY kurlarını görüntüleme

Gram altın için güncel referans fiyat gösterimi

Kur verilerini API üzerinden yenileme

Para birimlerini özel ikon ve bayraklarla listeleme

Kullanıcının girdiği miktarı tüm para birimlerine anında dönüştürme

Ek hesaplama butonu gerektirmeyen gerçek zamanlı dönüşüm

Son başarılı kur güncelleme zamanını görüntüleme

İnternet bağlantısı olmadığında son kaydedilen verileri gösterme

💰 Gelir ve Gider Yönetimi

Gelir ve gider kaydı oluşturma

Kayıtları düzenleme ve silme

Maaş, kira, fatura ve abonelik gibi düzenli işlemler tanımlama

İsteğe bağlı aylık otomatik işlem oluşturma

Yaklaşan ödeme ve gelirleri ana ekranda görüntüleme

Yaklaşan tarihler için bildirim alma

Metin, kategori, işlem türü ve tarih aralığına göre filtreleme

Toplam gelir, toplam gider ve mevcut bakiye hesaplama

Verileri Room Database ile cihazda kalıcı olarak saklama

📊 Bütçe ve Raporlama

Kategori bazlı aylık bütçe belirleme

Harcama limitinin kullanım oranını takip etme

Aylık gelir, gider ve bakiye özeti

Kategori bazlı gider dağılımı

Grafik destekli finansal raporlar

Filtrelenen işlemleri Excel uyumlu CSV formatında dışa aktarma

İşlem raporlarını PDF olarak dışa aktarma

🎯 Birikim Hedefleri

Birikim hedefi oluşturma

Hedef tutarı ve mevcut birikimi takip etme

Hedefe para ekleme

Hedeften para çekme

Birikim hareketlerini kayıt altında tutma

Hedef ilerlemesini görsel olarak izleme

📈 Yatırım ve Portföy Takibi

Döviz, altın ve diğer yatırım varlıklarını portföye ekleme

Yatırım miktarı ve alış fiyatı kaydetme

Toplam alış maliyetini hesaplama

Son alınan referans fiyatlarla güncel portföy değerini görüntüleme

Kâr ve zarar durumunu takip etme

Yatırımları tarih sırasına göre listeleme

🏠 Ana Panel

Toplam gelir ve gider özeti

Güncel bakiye bilgisi

Aylık limit ve kalan bütçe

Günlük harcanabilir tutar

Yaklaşan düzenli ödeme ve gelir önizlemesi

Aranabilir ve filtrelenebilir işlem listesi

Bütçe, rapor, birikim ve dışa aktarma ekranlarına hızlı erişim

Finans ve portföy bilgilerinin tek ekranda sunulması

📴 Çevrimdışı Kullanım

Son alınan kur verilerini SharedPreferences ile saklama

Finansal kayıtları Room Database ile cihazda tutma

İnternet olmadığında kayıtlı son kur verilerini gösterme

Bağlantı ve sunucu hatalarında kullanıcı dostu bilgilendirme mesajları

🎨 Kullanıcı Arayüzü

Material Design bileşenleri

XML tabanlı ekran tasarımları

ViewBinding kullanımı

ConstraintLayout ve LinearLayout

RecyclerView tabanlı dinamik listeler

Yükleme göstergeleri ve hata durumları

Giriş animasyonu

Farklı varlıklar için özel ikonlar

🛠 Kullanılan Teknolojiler

Alan

Teknoloji

Programlama dili

Kotlin

Platform

Android

Arayüz

XML, Material Components, ConstraintLayout

UI erişimi

ViewBinding

Listeleme

RecyclerView, Adapter Pattern

Mimari bileşenler

ViewModel, Repository

Yerel veritabanı

Room Database

Veri erişimi

DAO

Reaktif veri

Flow, StateFlow, LiveData

Ağ işlemleri

Retrofit

JSON dönüşümü

Gson

Yerel önbellek

SharedPreferences

Asenkron işlemler

Kotlin Coroutines

Arka plan görevleri

WorkManager

Grafik

MPAndroidChart

Reklam

Google Mobile Ads SDK

İzin yönetimi

Google User Messaging Platform

Sürüm kontrolü

Git ve GitHub

🏗 Proje Yapısı

Projede veri katmanı, kullanıcı arayüzü, ağ işlemleri ve arka plan görevleri birbirinden ayrılmıştır.

com.epatay.digitalwallet
│
├── data
│   ├── Transaction
│   ├── TransactionDao
│   ├── TransactionDatabase
│   ├── TransactionRepository
│   ├── InvestmentItem
│   ├── InvestmentDao
│   ├── CurrencyItem
│   ├── CurrencyManager
│   └── ExchangeRateResponse
│
├── recurring
│   ├── RecurringTransactionWorker
│   └── RecurringTransactionScheduler
│
└── ui
├── DashboardFragment
├── AnalysisFragment
├── CurrencyFragment
├── InvestmentFragment
├── TransactionAdapter
├── CurrencyAdapter
├── InvestmentAdapter
└── ViewModel sınıfları

Proje geliştikçe klasör ve sınıf yapısı değişebilir.

📱 Ekran Görüntüleri

Ana Sayfa



Portföy



Döviz ve Altın



▶️ Kurulum

Gereksinimler

Android Studio

JDK 17

Android SDK

İnternet bağlantısı

Android 7.0 veya daha yeni bir cihaz ya da emülatör

Projeyi çalıştırma

Depoyu bilgisayarınıza klonlayın:

git clone https://github.com/Hepatay/DigitalWalletApp.git

Proje klasörüne girin:

cd DigitalWalletApp

Android Studio'yu açın.

Open seçeneğiyle proje klasörünü seçin.

Gradle senkronizasyonunun tamamlanmasını bekleyin.

Bir Android emülatörü veya fiziksel cihaz seçin.

Uygulamayı çalıştırın.

🔐 Gizlilik

VarlıkCep; finansal kayıtları cihaz üzerindeki yerel veritabanında saklar. Uygulamada reklam gösterimi için Google Mobile Ads SDK ve gerekli bölgelerde kullanıcı izinlerini yönetmek için Google User Messaging Platform kullanılmaktadır.

Ayrıntılı bilgi:

VarlıkCep Gizlilik Politikası

İmzalama anahtarları, şifreler, local.properties, *.jks, *.keystore ve benzeri özel dosyalar GitHub deposuna yüklenmemelidir.

🗺️ Yol Haritası

Tamamlananlar

Temel kullanıcı arayüzü

RecyclerView ve Adapter entegrasyonu

Referans döviz kuru takibi

Gram altın referans fiyatı

Gerçek zamanlı kur hesaplama

Döviz bayrakları ve özel ikonlar

SharedPreferences ile çevrimdışı kur saklama

Room Database entegrasyonu

Gelir ve gider yönetimi

Yatırım ve portföy kayıt sistemi

Portföy değer hesaplamaları

Dashboard ve kategori grafikleri

Repository ve ViewModel kullanımı

Düzenli ödeme ve gelirler

Aylık otomatik işlem oluşturma

Yaklaşan tarih bildirimleri

Kategori bazlı bütçeler

Aylık finansal raporlar

Arama ve gelişmiş filtreleme

Birikim hedefleri ve hareketleri

CSV ve PDF dışa aktarma

Kullanıcı izin yönetimi

Banner reklam entegrasyonu

Açılış animasyonu ve başlangıç optimizasyonları

Planlananlar

Çeyrek, yarım ve tam altın takibi

Kripto para takibi

Döviz ve yatırım favorileri

Gelişmiş portföy dağılım grafiği

Fiyat değişim yüzdeleri

Dark Mode

PIN ve biyometrik giriş

Fiyat alarmı

Bulut yedekleme

Çoklu cihaz senkronizasyonu

⚠️ Yasal Uyarı

VarlıkCep yalnızca kişisel bütçe, gelir-gider, birikim ve portföy takibi amacıyla geliştirilmiştir.

Uygulamada gösterilen döviz, altın ve yatırım verileri bilgilendirme amaçlı referans değerlerdir. Veriler yatırım tavsiyesi değildir ve herhangi bir yatırım kararının tek dayanağı olarak kullanılmamalıdır.

Kur ve piyasa verilerinde kullanılan servise, internet bağlantısına ve son güncelleme zamanına bağlı gecikmeler oluşabilir.

VarlıkCep;

Bankacılık hizmeti sunmaz.

Para transferi gerçekleştirmez.

Ödeme veya kredi hizmeti sağlamaz.

Yatırım danışmanlığı yapmaz.

Kullanıcı adına yatırım işlemi gerçekleştirmez.

👨‍💻 Geliştirici

Hüseyin Epatay

GitHub: @Hepatay

Proje: DigitalWalletApp

⭐ Destek

Projeyi faydalı bulduysanız GitHub üzerinden yıldız vererek destek olabilirsiniz.

Hata bildirimi ve geliştirme önerileri için GitHub üzerindeki Issues bölümünü kullanabilirsiniz.