# envanter-api

> Küçük ve orta ölçekli işletmeler için geliştirilmiş, ürün stoğu ve demirbaş yönetimini tek çatı altında toplayan REST API.

![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?style=flat-square&logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql)
![JWT](https://img.shields.io/badge/Auth-JWT-000000?style=flat-square&logo=jsonwebtokens)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)

---

## Özellikler

- **Kimlik Doğrulama** — JWT tabanlı, rol bazlı erişim kontrolü (ADMIN / MANAGER / STAFF)
- **Ürün & Kategori Yönetimi** — SKU takibi, stok miktarı, fiyatlandırma
- **Demirbaş Yönetimi** — Zimmetleme, depo transferi, bakım takibi ve tam geçmiş kaydı
- **Stok Hareketleri** — Giriş / Çıkış / Transfer işlemleri, işlem geçmişi
- **Depo & Tedarikçi Yönetimi** — Çoklu depo desteği, tedarikçi bilgi takibi
- **Kullanıcı Yönetimi** — Geçici şifre üretme, şifre değiştirme zorunluluğu, aktif/pasif durumu
- **Dashboard** — Anlık istatistik özeti
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
| Derleme | Maven |
| Dokümantasyon | springdoc-openapi 2.6.0 |

---

## Başlarken

### Gereksinimler

- Java 17+
- Maven 3.8+
- MySQL 8.0+

### Kurulum

**1. Repoyu klonlayın**
```bash
git clone https://github.com/kullanici-adi/envanter-api.git
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
```

> Üretim ortamında bu değerleri ortam değişkenleriyle (`${DB_PASSWORD}` gibi) yönetin.

**4. Uygulamayı başlatın**
```bash
mvn spring-boot:run
```

API `http://localhost:8080` adresinde çalışmaya başlar.

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
| Auth | `/api/auth` | Giriş, şifre değiştirme |
| Kullanıcılar | `/api/users` | CRUD, şifre sıfırlama (ADMIN) |
| Ürünler | `/api/products` | CRUD |
| Kategoriler | `/api/categories` | CRUD |
| Depolar | `/api/warehouses` | CRUD |
| Tedarikçiler | `/api/suppliers` | CRUD |
| Demirbaşlar | `/api/assets` | CRUD + zimmet / transfer / bakım |
| Stok Hareketleri | `/api/inventory` | Giriş / Çıkış / Transfer |
| Dashboard | `/api/dashboard` | İstatistik özeti |

---

## Rol Yetki Matrisi

| İşlem | ADMIN | MANAGER | STAFF |
|-------|:-----:|:-------:|:-----:|
| Giriş yapma | ✓ | ✓ | ✓ |
| Listeleme / Görüntüleme | ✓ | ✓ | ✓ |
| Ekleme / Güncelleme | ✓ | ✓ | — |
| Silme | ✓ | ✓ | — |
| Kullanıcı yönetimi | ✓ | — | — |

---

## Proje Yapısı

```
src/main/java/org/example/inventoryapi/
├── config/          # OpenAPI yapılandırması
├── controller/      # REST endpoint'leri
├── dto/             # İstek / yanıt nesneleri
├── model/
│   ├── entity/      # JPA varlıkları
│   └── enums/       # Role, AssetStatus, TransactionType
├── repository/      # Spring Data JPA arayüzleri
├── security/        # JWT filtresi, SecurityConfig
└── service/         # İş mantığı katmanı
```

---

## İlgili Proje

Bu API, React + TypeScript ile geliştirilen **[envanter-ui](https://github.com/kullanici-adi/envanter-ui)** arayüzü ile birlikte çalışmaktadır.

---

## Lisans

MIT
