# VarlıkCep Gizlilik Politikası

**Son güncelleme:** 27 Ağustos 2026

VarlıkCep, kullanıcıların kişisel bütçe, gelir, gider ve yatırım kayıtlarını
takip edebilmesi amacıyla Hüseyin Epatay tarafından geliştirilmiştir.

Bu gizlilik politikası; VarlıkCep uygulamasının hangi verileri işlediğini,
verilerin nasıl kullanıldığını, saklandığını, bulut senkronizasyonunu ve
üçüncü taraf hizmetlerle hangi durumlarda paylaşıldığını açıklar.

## KVKK aydınlatma özeti

- **Veri sorumlusu:** Hüseyin Epatay
- **İşlenen veriler:** Kullanıcının oluşturduğu finans kayıtları (gelir, gider,
  bütçe, altın, döviz ve birikim hedefleri), Google ile giriş yapıldığında
  temel hesap bilgileri (Google UID, ad-soyad, e-posta adresi) ile reklam,
  analitik ve ağ sağlayıcılarının otomatik işleyebileceği teknik bağlantı,
  tanımlayıcı, reklam etkileşimi ve tanılama verileri
- **İşleme amaçları:** Bütçe ve portföy işlevlerini sunmak, verileri güvenli
  şekilde bulutta (Firebase) yedeklemek ve cihazlar arası senkronize etmek,
  piyasa verilerini göstermek, reklam sunmak ve ölçmek, izin tercihlerini
  yönetmek, güvenliği sağlamak ve teknik sorunları gidermek
- **Toplama yöntemi:** Kullanıcının uygulamaya girişi, cihazdaki yerel işlemler,
  isteğe bağlı Google ile oturum açma işlemi ve üçüncü taraf SDK/HTTPS
  bağlantıları üzerinden otomatik yollar
- **Hukuki sebep:** Uygulama işlevinin kullanıcının talebiyle sunulması, veri
  sorumlusunun meşru menfaatleri, hukuki yükümlülüklerin yerine getirilmesi ve
  açık rıza gereken işlemlerde kullanıcının izni
- **Aktarım:** Finansal kayıtlar kullanıcının kimliğine özel olarak izole edilmiş
  Google Firebase (Firestore) bulut altyapısında saklanır; reklam hizmetlerine,
  veri simsarlarına veya piyasa sağlayıcılarına aktarılmaz.
- **Başvuru ve haklar:** Kullanıcılar 6698 sayılı Kanun'un 11. maddesindeki
  hakları için bu politikanın sonundaki iletişim kanalını kullanabilir.

## 1. Kullanıcının girdiği finansal veriler

VarlıkCep içerisinde kullanıcı tarafından girilen aşağıdaki bilgiler
cihazdaki yerel veritabanında ve oturum açıldığında Google Firebase
(Firestore) bulut altyapısında saklanır:

- Gelir ve gider kayıtları
- İşlem kategorileri
- İşlem tarihleri
- Aylık bütçe ve harcama limitleri
- Yatırım türleri (Döviz ve Altın)
- Yatırım miktarları
- Alış fiyatları
- Portföy bilgileri
- Düzenli gelir ve gider tanımları
- Kategori bütçeleri
- Birikim hedefleri ve birikim hareketleri
- Kullanıcının eklediği açıklama ve notlar

Bu kişisel finans kayıtları kullanıcının kimliğine (`user_id`) özel olarak
şifreli ve izole şekilde işlenir; reklamverenlere, TCMB'ye veya altın veri
sağlayıcılarına gönderilmez.

## 2. Veri saklama, bulut senkronizasyonu ve hesap yönetimi

Kullanıcılar uygulamayı misafir olarak (yalnızca yerel cihazda)
kullanabileceği gibi, dilerlerse **Google ile Giriş Yap** özelliğini
kullanarak verilerini buluta yedekleyebilirler.

### Google ile oturum açma (İsteğe bağlı)
Kullanıcı Google ile oturum açtığında; kullanıcı kimliği (Google UID),
ad-soyad ve e-posta adresi kimlik doğrulama amacıyla Firebase Authentication
altyapısında işlenir. Kullanıcının Google şifresi veya hassas hesap parolası
kesinlikle VarlıkCep tarafından talep edilmez ve saklanmaz.

### Bulut senkronizasyonu (Firebase Firestore)
Oturum açıldığında finansal kayıtlar Google Firebase bulut sunucularına
HTTPS/TLS şifrelemesiyle aktarılır. Firestore güvenlik kuralları uyarınca
her kullanıcı yalnızca kendi hesabına (`user_id`) ait verileri okuyabilir
ve değiştirebilir; başka kullanıcıların bu verilere erişmesi imkansızdır.

### Dışa aktarma ve paylaşma

Kullanıcı; gelir ve gider kayıtlarını `.xlsx`, `.csv` veya `.pdf` dosyası
olarak, kategori bütçeleri ile yatırım kayıtlarını ise `.xlsx` veya `.csv`
dosyası olarak dışa aktarabilir. Bu dosyalar yalnızca kullanıcının Android
dosya seçicisi üzerinden belirlediği konuma yazılır. Uygulama dışa aktarılan
dosyaları sunuculara otomatik olarak göndermez.

Kullanıcı bir dosyayı başka bir uygulamayla paylaşmayı seçerse dosya, seçilen
uygulamanın ve hizmet sağlayıcının gizlilik koşullarına tabi olabilir. Dışa
aktarılan dosyaların saklanması, paylaşılması ve silinmesi kullanıcının
sorumluluğundadır.

## 3. Hesap ve verilerin kalıcı olarak silinmesi

Kullanıcılar verileri üzerindeki tam kontrol ve silme hakkına sahiptir:

- **Kayıt Silme:** Uygulama içinden silinen herhangi bir işlem veya yatırım
  kaydı, anında hem cihazdan hem de bulut veritabanından kalıcı olarak silinir.
- **Hesap ve Tüm Verileri Silme:** Kullanıcı, Profil ekranında yer alan
  **"Hesabımı ve Tüm Verilerimi Sil"** butonunu kullanarak dilediği zaman:
  1. Firestore bulut veritabanındaki tüm gelir, gider, bütçe ve portföy verilerini,
  2. Firebase Authentication üzerindeki kullanıcı hesabını ve kimlik kaydını,
  3. Cihazdaki yerel veritabanını ve önbelleği,
  geri döndürülemez şekilde tamamen ve kalıcı olarak silebilir.

## 4. Reklamlar ve Google AdMob

VarlıkCep, uygulama içerisinde banner reklam göstermek amacıyla Google
AdMob ve Google Mobile Ads SDK kullanır.

Google Mobile Ads SDK aşağıdaki teknik verileri otomatik olarak
işleyebilir veya Google ile paylaşabilir:

- IP adresi ve IP adresinden tahmin edilen genel konum
- Cihaz ve uygulama tanımlayıcıları
- Android reklam kimliği
- App Set ID ve benzeri tanımlayıcılar
- Uygulama açılışları ve reklam etkileşimleri
- Dokunma ve görüntüleme bilgileri
- Uygulama ve reklam SDK'sı performans bilgileri
- Çökme, donma ve diğer tanılama bilgileri

Bu bilgiler reklam sunma, reklam performansını ölçme, analiz yapma,
hataları giderme ve sahtekârlığı önleme amaçlarıyla kullanılabilir.

Google tarafından işlenen veriler, Google'ın gizlilik politikası ve reklam
teknolojileri politikalarına tabidir:

- Google Gizlilik Politikası:
  https://policies.google.com/privacy

- Google'ın reklam teknolojilerini kullanma biçimi:
  https://policies.google.com/technologies/ads

## 5. Kişiselleştirilmiş ve kişiselleştirilmemiş reklamlar

Kullanıcının bulunduğu ülkeye, yasal gerekliliklere ve verdiği izinlere
bağlı olarak kişiselleştirilmiş, kişiselleştirilmemiş veya sınırlı reklamlar
gösterilebilir.

Avrupa Ekonomik Alanı, Birleşik Krallık ve İsviçre gibi izin alınması
gereken bölgelerde VarlıkCep, Google User Messaging Platform üzerinden
kullanıcıya gizlilik ve reklam tercihleri formu gösterebilir.

Kullanıcının reklam kişiselleştirmesine izin vermemesi, uygulamanın temel
bütçe ve portföy özelliklerini kullanmasını engellemez.

## 6. Gizlilik tercihlerinin değiştirilmesi

Kullanıcılar daha önce verdikleri reklam ve veri işleme tercihlerini, ilgili
bölgelerde uygulamanın alt bölümündeki **Gizlilik** bağlantısı üzerinden açılan
Google User Messaging Platform formuyla yeniden inceleyebilir ve
değiştirebilir.

Android reklam kimliği ayrıca cihazın Android gizlilik veya reklam
ayarlarından sıfırlanabilir ya da silinebilir.

## 7. Üçüncü taraf piyasa verileri

VarlıkCep, piyasa bilgilerini göstermek için aşağıdaki servislere HTTPS
üzerinden istek gönderebilir:

- Türkiye Cumhuriyet Merkez Bankası günlük döviz kurları:
  https://www.tcmb.gov.tr/kurlar/today.xml

- apinoktam altın fiyatları (truncgil.com verisi):
  https://apinoktam.erenozdemir.com.tr/

Bu servisler IP adresi, istek zamanı, cihazın genel bağlantı bilgileri ve
IP adresinden çıkarılabilen yaklaşık konum gibi teknik bilgileri kendi
gizlilik politikalarına göre işleyebilir.

VarlıkCep, kullanıcının gelir, gider, bütçe veya yatırım kayıtlarını bu
servislere göndermez.

Bayrak görselleri uygulama paketinde yerel olarak bulunur; bayrakları
göstermek için herhangi bir bayrak servisine ağ isteği gönderilmez. Görseller
yalnızca görsel sunum içindir ve fiyat verisinin kaynağı değildir. Görsellerin
dayandığı `flag-icons` projesi MIT lisansıyla sunulur. Ayrıntılı üçüncü taraf
bildirimleri `docs/THIRD_PARTY_NOTICES.md` dosyasındadır.

## 8. Verilerin güvenliği

Uygulamanın bulut sunucuları (Firebase), piyasa verisi ve reklam hizmetleriyle
yaptığı tüm ağ bağlantıları HTTPS/TLS üzerinden şifreli olarak gerçekleştirilir.

Uygulama düzeyinde düz metin (cleartext/HTTP) trafiği işletim sistemi düzeyinde
engellenmiştir.

Kullanıcı, cihazının ekran kilidini ve Google hesap güvenliğini korumaktan
sorumludur.

## 9. Çocukların gizliliği

VarlıkCep özellikle çocuklara yönelik olarak tasarlanmamıştır.

Uygulama bilerek 13 yaş altındaki çocuklardan ad, iletişim bilgisi veya kişisel
finans bilgisi toplamaz. Uygulamanın çocuklar tarafından kullanılması durumunda
ebeveyn veya yasal vasinin gözetimi önerilir.

## 10. Kullanıcının hakları

Geçerli veri koruma mevzuatına (KVKK / GDPR) bağlı olarak kullanıcılar
aşağıdaki haklara sahiptir:

- Verilerinin nasıl işlendiği hakkında bilgi alma
- Verilerin düzeltilmesini veya silinmesini talep etme
- Hesabını ve tüm verilerini kalıcı olarak silme
- Belirli veri işleme faaliyetlerine itiraz etme
- Daha önce verilen izni geri çekme

Kullanıcılar verilerini uygulama içerisindeki "Hesabımı ve Tüm Verilerimi Sil"
seçeneği ile anında ve kalıcı olarak silebilir veya geliştiriciye başvurarak
destek alabilirler.

## 11. Finansal bilgi ve sorumluluk sınırı

VarlıkCep ödeme, para transferi, aracılık veya yatırım danışmanlığı hizmeti
sunmaz. Gösterilen döviz, altın, portföy değeri, kâr ve zarar hesapları genel
bilgilendirme ve referans amaçlıdır; yatırım tavsiyesi, kesin fiyat veya
alım-satım teklifi değildir. Veriler gecikebilir, eksik olabilir ve çevrimdışı
durumda son kaydedilen değerler gösterilebilir. Kuyumcu, banka, şehir, işçilik
ve piyasa koşullarına göre gerçekleşen fiyatlar farklı olabilir.

## 12. Gizlilik politikasındaki değişiklikler

Bu politika; uygulamanın özellikleri, kullanılan üçüncü taraf hizmetler
veya yasal gereklilikler değiştiğinde güncellenebilir.

Politikanın güncel sürümü bu sayfada yayımlanır ve son güncelleme tarihi
sayfanın üst kısmında belirtilir.

## 13. Geliştirici ve iletişim

**Uygulama:** VarlıkCep  
**Geliştirici:** Hüseyin Epatay

Gizlilik politikasıyla ilgili soru, talep veya bildirimler için:

https://github.com/Hepatay/DigitalWalletApp/issues
