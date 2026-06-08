# Open Banking Payment Initiation Demo (Spring Boot, Kafka, Redis & MySQL)

Bu proje, bir bankanın Açık Bankacılık (Open Banking) "Ödeme Başlatma" (Payment Initiation) akışını simüle eden, asenkron ve event-driven mimarili bir Spring Boot uygulamasıdır.

---

## 🚀 İş Akışı Mimarisi (Açık Bankacılık Ödeme Başlatma)

1. **REST API**: İstemci `POST /api/v1/payments` ucuna ödeme başlatma isteği (Gönderen IBAN, Alıcı IBAN, Tutar vb.) gönderir.
2. **UUID Üretimi**: Servis katmanında işlem için benzersiz bir `transactionUuid` (UUID) oluşturulur.
3. **MySQL DB**: İşlem, Spring `JdbcTemplate` kullanılarak MySQL veritabanındaki `open_banking_transactions` tablosuna **PENDING** statüsünde kaydedilir.
4. **Redis Cache**: İşlemin güncel durumu UUID anahtarı ile Redis'e yazılır (TTL: 10 dakika).
5. **Kafka Event**: İşlem detayı `payment-events` Kafka konusuna asenkron ve non-blocking olarak fırlatılır.
6. **HTTP 202 Accepted**: İstemciye hemen yanıt dönülerek UUID ve PENDING statüsü teslim edilir.
7. **Kafka Tüketici**: `PaymentConsumer` mesajı yakalar:
   - **BkmMockService**'i çağırır. Bu servis BKM entegrasyonunu simüle ederek 1.5 saniye ağ gecikmesi yaşatır ve rastgele %80 başarı (APPROVED) ya da %20 ret (REJECTED) üretir.
   - BKM'den dönen sonuca göre veritabanındaki ilgili kaydın statüsünü günceller.
   - Redis'teki durumu güncelleyerek TTL'i yeniler.

---

## 🛠️ Teknolojiler ve Bağımlılıklar

* **Java 17**
* **Spring Boot 3.5.x**
* **Spring Kafka**
* **Spring Data Redis (Lettuce Client)**
* **MySQL Connector / Spring JDBC (JdbcTemplate)**
* **Lombok**
* **Jakarta Validation API**

---

## 📁 Proje Yapısı

```text
src/main/java/com/example/demo/
├── DemoApplication.java          # Spring Boot Başlangıç Sınıfı
├── config/
│   ├── KafkaConfig.java          # Kafka Topic (payment-events) otomatik oluşturma
│   └── RedisConfig.java          # RedisTemplate Yapılandırması
├── consumer/
│   └── PaymentConsumer.java      # Kafka Event Consumer (BKM çağrısı & durum güncelleme)
├── controller/
│   └── PaymentController.java    # REST API (/api/v1/payments)
├── dto/
│   ├── PaymentEvent.java         # Kafka Event Payload Modeli
│   ├── PaymentRequest.java       # Ödeme Başlatma Request Modeli ve IBAN validasyonu
│   └── PaymentResponse.java      # Ödeme Başlatma Response Modeli (UUID & Status)
├── model/
│   └── Transaction.java          # Veritabanı Varlık Modeli
├── repository/
│   └── TransactionRepository.java # JdbcTemplate ile MySQL veri erişim katmanı
└── service/
    ├── BkmMockService.java       # BKM API Mock Servisi (%80 success / 1.5s delay)
    └── PaymentService.java       # Ödeme Başlatma İş Mantığı
```

---

## 🗄️ Veritabanı Şeması (schema.sql)

Uygulama `src/main/resources/schema.sql` dosyasındaki DDL script'ini kullanarak `open_banking_transactions` tablosunu otomatik olarak oluşturur:

```sql
CREATE TABLE IF NOT EXISTS open_banking_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_uuid VARCHAR(36) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

---

## 💻 Kurulum ve Çalıştırma

### 1. MySQL, Redis ve Kafka'yı Başlatma (Docker)

Yerel ortamınızda altyapıyı hızlıca başlatmak için aşağıdaki Docker komutlarını kullanabilirsiniz:

```bash
# MySQL'i Başlat
docker run -d --name local-mysql -e MYSQL_DATABASE=open_banking_db -e MYSQL_ROOT_PASSWORD=password -p 3306:3306 mysql:latest

# Redis'i Başlat
docker run -d --name local-redis -p 6379:6379 redis:alpine

# Kafka'yı Başlat (KRaft Modunda)
docker run -d --name local-kafka -p 9092:9092 \
  -e KAFKA_ENABLE_KRAFT=yes \
  -e KAFKA_CFG_PROCESS_ROLES=broker,controller \
  -e KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
  -e KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CFG_BROKER_ID=1 \
  -e KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=1@127.0.0.1:9093 \
  -e ALLOW_PLAINTEXT_LISTENER=yes \
  bitnami/kafka:latest
```

### 2. Uygulamayı Derleme ve Çalıştırma

```bash
# Sınıfları Derle
./gradlew compileJava

# Uygulamayı Başlat
./gradlew bootRun
```

---

## 🧪 Akışın Test Edilmesi

### 1. Ödeme Başlatma İsteği Gönder (cURL)

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "senderIban": "TR123456789012345678901234",
    "receiverIban": "TR987654321098765432109876",
    "amount": 2500.00,
    "currency": "TRY",
    "description": "Kira Ödemesi"
  }'
```

**Alınacak HTTP Yanıtı (Status: 202 Accepted):**
```json
{
  "transactionUuid": "9c148e24-ff9a-4c28-98e3-51887e382d5a",
  "status": "PENDING",
  "message": "Payment request has been accepted and is being processed.",
  "timestamp": "2026-06-05T15:15:30.123"
}
```

### 2. Konsol Loglarını Gözlemleme

Uygulamanın çalıştığı konsolda BKM çağrısı, gecikme simülasyonu ve veritabanı statü güncellemesinin loglandığını görebilirsiniz:

```text
INFO  c.e.d.c.PaymentController : Received Payment Initiation request from sender: TR123456789012345678901234
INFO  c.e.d.s.PaymentService    : Initiating Payment. Generated Transaction UUID: 9c148e24-ff9a-4c28-98e3-51887e382d5a
INFO  c.e.d.r.TransactionRepository : Successfully saved transaction to DB. UUID: 9c148e24-ff9a-4c28-98e3-51887e382d5a, Status: PENDING
INFO  c.e.d.s.PaymentService    : Payment status 'PENDING' cached in Redis. Key: 9c148e24-ff9a-4c28-98e3-51887e382d5a
INFO  c.e.d.s.PaymentService    : Payment Event published successfully to Kafka topic: payment-events for UUID: 9c148e24-ff9a-4c28-98e3-51887e382d5a. Offset: 0

INFO  c.e.d.c.PaymentConsumer   : =========================================================
INFO  c.e.d.c.PaymentConsumer   : KAFKA CONSUMER: Received payment event from topic: payment-events
INFO  c.e.d.c.PaymentConsumer   : Transaction UUID: 9c148e24-ff9a-4c28-98e3-51887e382d5a
INFO  c.e.d.c.PaymentConsumer   : Sender IBAN     : TR123456789012345678901234
INFO  c.e.d.c.PaymentConsumer   : Receiver IBAN   : TR987654321098765432109876
INFO  c.e.d.c.PaymentConsumer   : Amount          : 2500.0 TRY
INFO  c.e.d.c.PaymentConsumer   : Current Status  : PENDING
INFO  c.e.d.c.PaymentConsumer   : =========================================================

INFO  c.e.d.c.PaymentConsumer   : Initiating verification with BKM for Transaction: 9c148e24-ff9a-4c28-98e3-51887e382d5a
INFO  c.e.d.s.BkmMockService    : BKM MOCK: Received Payment Request for transaction UUID: 9c148e24-ff9a-4c28-98e3-51887e382d5a, Amount: 2500.0
# -- 1.5 saniye bekleme süresi --
INFO  c.e.d.s.BkmMockService    : BKM MOCK: APPROVED for transaction 9c148e24-ff9a-4c28-98e3-51887e382d5a. Approval Code: BKM-APP-E6A1B2C3
INFO  c.e.d.c.PaymentConsumer   : BKM response received. Updating status to APPROVED for Transaction: 9c148e24-ff9a-4c28-98e3-51887e382d5a
INFO  c.e.d.r.TransactionRepository : Successfully updated transaction in DB. UUID: 9c148e24-ff9a-4c28-98e3-51887e382d5a, New Status: APPROVED
INFO  c.e.d.c.PaymentConsumer   : Redis cache updated successfully. Key: 9c148e24-ff9a-4c28-98e3-51887e382d5a, New Status: APPROVED
```

### 3. Veritabanı ve Redis Sorgulama

* **MySQL Veritabanı**:
  ```sql
  mysql -h localhost -P 3306 -u root -ppassword
  USE open_banking_db;
  SELECT * FROM open_banking_transactions;
  ```

* **Redis**:
  ```bash
  redis-cli
  GET 9c148e24-ff9a-4c28-98e3-51887e382d5a
  # "APPROVED" veya "REJECTED" değerini döndürecektir.
  ```
