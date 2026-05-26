# envanter-api

> Küçük ve orta ölçekli işletmeler için geliştirilmiş, ürün stoğu ve demirbaş yönetimini tek çatı altında toplayan REST API.

![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?style=flat-square&logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql)
![JWT](https://img.shields.io/badge/Auth-JWT-000000?style=flat-square&logo=jsonwebtokens)
![Tests](https://img.shields.io/badge/tests-119%20passed-brightgreen?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)

---

## Özellikler

- **Kimlik Doğrulama** — JWT tabanlı, rol bazlı erişim kontrolü (ADMIN / MANAGER / STAFF / SUPPLIER)
- **Ürün & Kategori Yönetimi** — SKU takibi, çok depolu stok yönetimi, fiyatlandırma
- **Demirbaş Yönetimi** — Zimmetleme, depo transferi, bakım takibi ve tam geçmiş kaydı
- **Stok Hareketleri** — Giriş / Çıkış / Transfer işlemleri, işlem geçmişi
- **Tedarikçi Portalı** — Tedarikçi rolüne özel sipariş yönetimi ve fiyat güncelleme
- **Sipariş Yönetimi** — BEKLIYOR → ONAYLANDI → YOLDA → TESLİM ALINDI durum makinesi
- **Stok Talep Sistemi** — STAFF'ın stok talebi oluşturması, MANAGER/ADMIN onay akışı
- **Bildirim Sistemi** — Sipariş, stok talebi ve kritik stok seviyesi bildirimleri
- **Depo & Tedarikçi Yönetimi** — Çoklu depo desteği, tedarikçi-ürün ilişkilendirmesi
- **Kullanıcı Yönetimi** — Geçici şifre üretme, şifre sıfırlama maili, aktif/pasif durumu
- **Dashboard** — Role göre filtrelenmiş anlık istatistik özeti
- **API Dokümantasyonu** — Swagger UI entegrasyonu

---

## Teknoloji Yığını

| Katman | Teknoloji |
|--------|-----------|
| Framework | Spring Boot 3.3.5 |
| Dil | Java 17 |
| Veritabanı | MySQL 8 |
| ORM | Spring Data JPA / Hibernate |
| Güvenlik | Spring Security + JWT (jjwt 0.12.6) |
| Şifreleme | BCrypt |
| Mail | Spring Boot Mail |
| Derleme | Maven |
| Dokümantasyon | springdoc-openapi 2.6.0 |
| Test | JUnit 5 + Mockito |

---

## Başlarken

### Gereksinimler

- Java 17+
- Maven 3.8+
- MySQL 8.0+

### Kurulum

**1. Repoyu klonlayın**
```bash
git clone https://github.com/FatihErdogan1/envanter-api.git
cd envanter-api
```

**2. Veritabanını oluşturun**
```sql
CREATE DATABASE envanter CHARACTER SET utf8mb4 COLLATE utf8mb4_turkish_ci;
```

**3. `application.properties` dosyasını düzenleyin**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/envanter?useSSL=false&serverTimezone=UTC
spring.datasource.username=DB_KULLANICI
spring.datasource.password=DB_SIFRE

jwt.secret=GUCLU_BIR_SECRET_KEY_BURAYA
jwt.expiration=86400000

spring.mail.host=smtp.gmail.com
spring.mail.username=MAIL_ADRESINIZ
spring.mail.password=UYGULAMA_SIFRENIZ
```

> Üretim ortamında bu değerleri ortam değişkenleriyle (`${DB_PASSWORD}` gibi) yönetin.

**4. Uygulamayı başlatın**
```bash
mvn spring-boot:run
```

API `http://localhost:8080` adresinde çalışmaya başlar.

---

## Testler

```bash
mvn test
```

8 servis için 119 unit test bulunmaktadır. Tüm testler Spring context'i ayağa kaldırmadan Mockito ile izole biçimde çalışır.

| Servis | Test Sayısı |
|--------|:-----------:|
| AssetService | 22 |
| UserService | 18 |
| SupplierOrderService | 18 |
| SupplierService | 13 |
| InventoryService | 11 |
| StockRequestService | 10 |
| WarehouseService | 9 |
| CategoryService | 8 |
| **Toplam** | **119** |

---

## API Dokümantasyonu

Uygulama ayaktayken Swagger UI'a erişin:

```
http://localhost:8080/swagger-ui/index.html
```

Token ile test etmek için:
1. `POST /api/auth/login` ile token alın
2. Sayfanın sağ üstündeki **Authorize** butonuna token'ı girin
3. Tüm korumalı endpoint'leri doğrudan arayüzden test edin

---

## Endpoint'lere Genel Bakış

| Grup | Prefix | Açıklama |
|------|--------|----------|
| Auth | `/api/auth` | Giriş, kayıt, şifre sıfırlama |
| Kullanıcılar | `/api/users` | CRUD, şifre sıfırlama (ADMIN) |
| Ürünler | `/api/products` | CRUD, depo bazlı stok özeti |
| Kategoriler | `/api/categories` | CRUD |
| Depolar | `/api/warehouses` | CRUD, depo stok listesi |
| Tedarikçiler | `/api/suppliers` | CRUD |
| Tedarikçi Siparişleri | `/api/supplier-orders` | Sipariş yönetimi, durum makinesi |
| Demirbaşlar | `/api/assets` | CRUD + zimmet / transfer / bakım |
| Stok Hareketleri | `/api/inventory` | Giriş / Çıkış / Transfer |
| Stok Talepleri | `/api/stock-requests` | Talep oluşturma ve onay akışı |
| Bildirimler | `/api/notifications` | Kullanıcıya özel bildirimler |
| Dashboard | `/api/dashboard` | İstatistik özeti |

---

## Rol Yetki Matrisi

| İşlem | ADMIN | MANAGER | STAFF | SUPPLIER |
|-------|:-----:|:-------:|:-----:|:--------:|
| Giriş yapma | ✓ | ✓ | ✓ | ✓ |
| Listeleme / Görüntüleme | ✓ | ✓ | ✓ | — |
| Ekleme / Güncelleme | ✓ | ✓ | — | — |
| Silme | ✓ | ✓ | — | — |
| Kullanıcı yönetimi | ✓ | — | — | — |
| Stok talebi oluşturma | ✓ | ✓ | ✓ | — |
| Stok talebi onaylama | ✓ | ✓ | — | — |
| Sipariş onaylama / reddetme | ✓ | — | — | ✓ |
| Sipariş teslim alma | ✓ | ✓ | — | — |
| Kendi ürün fiyatını güncelleme | — | — | — | ✓ |

---

## Proje Yapısı

```
src/
├── main/java/org/example/inventoryapi/
│   ├── config/          # OpenAPI yapılandırması
│   ├── constants/       # Uygulama sabitleri (LOW_STOCK_THRESHOLD vb.)
│   ├── controller/      # REST endpoint'leri
│   ├── dto/             # İstek / yanıt nesneleri
│   ├── exception/       # Özel exception sınıfları
│   ├── model/
│   │   ├── entity/      # JPA varlıkları
│   │   └── enums/       # Role, AssetStatus, SupplierOrderStatus vb.
│   ├── repository/      # Spring Data JPA arayüzleri
│   ├── security/        # JWT filtresi, SecurityConfig
│   └── service/         # İş mantığı katmanı
└── test/java/org/example/inventoryapi/
    └── service/         # 119 unit test (JUnit 5 + Mockito)
```

---

## İlgili Proje

Bu API, React + TypeScript ile geliştirilen **[envanter-ui](https://github.com/FatihErdogan1/envanter-ui)** arayüzü ile birlikte çalışmaktadır.

---

## Lisans

MIT
