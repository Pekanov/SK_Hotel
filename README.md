# Hotel Booking System

Система бронирования отелей на базе Spring Boot с микросервисной архитектурой, реализующая паттерн Saga для распределённых транзакций.

## Архитектура системы
```
┌─────────────────┐
│   API Gateway   │ :8080
│  (Auth Filter)  │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
┌───▼────┐ ┌──▼─────────┐
│ Booking│ │   Hotel    │
│Service │ │  Service   │
│ :8081  │ │   :8082    │
└───┬────┘ └─────┬──────┘
    │            │
    └──┬─────────┘
       │
  ┌────▼──────┐
  │  Eureka   │
  │  Server   │
  │   :8761   │
  └───────────┘
```

## Компоненты системы

### 1. Eureka Server (порт 8761)
- Service Registry для динамического обнаружения сервисов

### 2. API Gateway (порт 8080)
- Единая точка входа для всех клиентских запросов
- JWT аутентификация и авторизация
- Трассировка запросов (X-Trace-Id)
- CORS конфигурация
- Логирование и мониторинг

### 3. Booking Service (порт 8081)
- Управление бронированиями пользователей
- Регистрация и авторизация пользователей
- Saga-координатор с механизмом компенсации
- Resilience4j (Circuit Breaker, Retry, Rate Limiter)
- Интеграция с Hotel Service через Feign Client

### 4. Hotel Service (порт 8082)
- Управление отелями и номерами
- Алгоритм равномерной загрузки номеров
- Pessimistic Locking для предотвращения двойного бронирования
- Рекомендательная система номеров

## Cтек

- **Java**: 17
- **Spring Boot**: 3.5.9
- **Spring Cloud**: 2025.0.1
- **База данных**: H2
- **Security**: Spring Security + JWT
- **Service Discovery**: Eureka
- **API Gateway**: Spring Cloud Gateway
- **HTTP Client**: OpenFeign
- **Resilience**: Resilience4j
- **Документация**: SpringDoc OpenAPI
- **Маппинг**: MapStruct
- **Утилиты**: Lombok

## Быстрый старт

### Предварительные требования
- Java 17 или выше
- Maven 3.6 или выше
- Порты 8080, 8081, 8082, 8761 должны быть свободны

```bash
# 1. Клонируйте репозиторий
git clone <repository-url>
cd hotel-booking-system

# 2. Соберите проект
mvn clean install

# 3. Запустите Eureka Server
cd eureka-server
mvn spring-boot:run

# 4. Запустите Hotel Service (в новом терминале)
cd hotel-service
mvn spring-boot:run

# 5. Запустите Booking Service (в новом терминале)
cd booking-service
mvn spring-boot:run

# 6. Запустите API Gateway (в новом терминале)
cd api-gateway
mvn spring-boot:run
```

### Проверка запуска

Подождите 30-60 секунд после запуска всех сервисов, затем проверьте:
- Eureka Dashboard: http://localhost:8761
- API Gateway Health: http://localhost:8080
- Booking Service Swagger: http://localhost:8081/swagger-ui.html
- Hotel Service Swagger: http://localhost:8082/swagger-ui.html

## Предзаполненные данные

### Пользователи (Booking Service)

| Username | Password    | Role       |
|----------|-------------|------------|
| admin    | admin123    | ROLE_ADMIN |
| user1    | password123 | ROLE_USER  |
| user2    | password123 | ROLE_USER  |

### Отели и номера (Hotel Service)

- **Grand Plaza Hotel** - 5 номеров (101-105)
- **Seaside Resort** - 5 номеров (201-205)
- **Mountain View Lodge** - 5 номеров (301-305)

## API Endpoints (через Gateway)

### Аутентификация (публичные)
```bash
# Регистрация нового пользователя
POST http://localhost:8080/user/register
Content-Type: application/json
{
  "username": "newuser",
  "password": "password123"
}

# Вход (получение JWT токена)
POST http://localhost:8080/user/auth
Content-Type: application/json
{
  "username": "user1",
  "password": "password123"
}
```

### Отели (требуется аутентификация)
```bash
# Получить все отели
GET http://localhost:8080/api/hotels
Authorization: Bearer <token>

# Создать отель (только ADMIN)
POST http://localhost:8080/api/hotels
Authorization: Bearer <token>
Content-Type: application/json
{
  "name": "New Hotel",
  "address": "123 Street, City"
}
```

### Номера (требуется аутентификация)
```bash
# Получить все доступные номера
GET http://localhost:8080/api/rooms
Authorization: Bearer <token>

# Получить рекомендованные номера (по загруженности)
GET http://localhost:8080/api/rooms/recommend
Authorization: Bearer <token>

# Создать номер (только ADMIN)
POST http://localhost:8080/api/rooms
Authorization: Bearer <token>
Content-Type: application/json
{
  "hotelId": 1,
  "number": "106",
  "available": true
}
```

### Бронирования (требуется аутентификация)
```bash
# Создать бронирование с выбором номера
POST http://localhost:8080/booking
Authorization: Bearer <token>
Content-Type: application/json
{
  "roomId": 1,
  "startDate": "2026-02-01",
  "endDate": "2026-02-05",
  "autoSelect": false
}

# Создать бронирование с автоподбором номера
POST http://localhost:8080/booking
Authorization: Bearer <token>
Content-Type: application/json
{
  "startDate": "2026-02-10",
  "endDate": "2026-02-15",
  "autoSelect": true
}

# Получить свои бронирования
GET http://localhost:8080/bookings
Authorization: Bearer <token>

# Получить бронирование по ID
GET http://localhost:8080/booking/{id}
Authorization: Bearer <token>

# Отменить бронирование
DELETE http://localhost:8080/booking/{id}
Authorization: Bearer <token>
```

## Примеры использования

### 1. Полный цикл бронирования
```bash
# Шаг 1: Вход в систему
curl -X POST http://localhost:8080/user/auth \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"password123"}'

# Ответ:
# {"token":"eyJhbGc...","username":"user1","role":"ROLE_USER"}

# Шаг 2: Получить рекомендованные номера
TOKEN="<token_from_step_1>"
curl -X GET http://localhost:8080/api/rooms/recommend \
  -H "Authorization: Bearer $TOKEN"

# Шаг 3: Создать бронирование
curl -X POST http://localhost:8080/booking \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "startDate": "2026-02-01",
    "endDate": "2026-02-05",
    "autoSelect": true
  }'

# Шаг 4: Проверить статус бронирования
curl -X GET http://localhost:8080/bookings \
  -H "Authorization: Bearer $TOKEN"
```

### 2. Администрирование (ADMIN)
```bash
# Вход как admin
curl -X POST http://localhost:8080/user/auth \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

ADMIN_TOKEN="<admin_token>"

# Создать новый отель
curl -X POST http://localhost:8080/api/hotels \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Luxury Resort",
    "address": "456 Beach Road"
  }'

# Создать номер
curl -X POST http://localhost:8080/api/rooms \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "hotelId": 1,
    "number": "501",
    "available": true
  }'

# Создать пользователя
curl -X POST http://localhost:8080/user \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "pass123",
    "role": "ROLE_USER"
  }'
```

## Ключевые функции

### 1. Паттерн Saga с компенсацией

Booking Service реализует двухшаговую распределённую транзакцию:

```
Успешный сценарий:
1. PENDING → Создание бронирования в Booking Service
2. Hotel Service → Подтверждение доступности номера
3. CONFIRMED → Бронирование подтверждено

Сценарий с компенсацией:
1. PENDING → Создание бронирования
2. Hotel Service → Ошибка подтверждения доступности
3. CANCELLED → Отмена бронирования + освобождение номера
```

### 2. Идемпотентность
- Каждый запрос помечается уникальным `requestId`
- Повторные запросы возвращают кешированный результат
- Предотвращение дублирования операций при сбоях сети

### 3. Resilience4j для отказоустойчивости

**Circuit Breaker**
```java
@CircuitBreaker(name = "hotelService", fallbackMethod = "fallbackMethod")
public RoomResponse getRoomDetails(Long roomId) {
    // Вызов внешнего сервиса
}
```

**Конфигурация**:
- `slidingWindowSize`: 10
- `failureRateThreshold`: 50%
- `waitDurationInOpenState`: 10s

**Retry Pattern**
```java
@Retry(name = "hotelService", fallbackMethod = "retryFallback")
public boolean confirmBooking(BookingRequest request) {
    // Повторные попытки при временных сбоях
}
```
**Конфигурация**:
- `maxAttempts`: 3
- `waitDuration`: 1s
- exponentialBackoffMultiplier: 2

### 4. Алгоритм равномерной загрузки

Номера сортируются по количеству предыдущих бронирований:
```sql
SELECT * FROM rooms 
WHERE available = true 
ORDER BY times_booked ASC, id ASC
```

### 5. Pessimistic Locking для предотвращения двойного бронирования

Предотвращение race conditions:
```java
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdWithLock(@Param("id") Long id);
}
```

### 6. Трассировка запросов

Каждый запрос через систему получает уникальный `X-Trace-Id`:
- Генерация в API Gateway
- Передача через все сервисы
- Логирование с привязкой к traceId для отладки


## Архитектурные решения

### 1) Saga Pattern для распределённых транзакций

**Контекст:** Необходимость координации транзакций между Booking и Hotel Service без единой БД

**Решение:** Choreography-based Saga с компенсационными транзакциями

**Обоснование:**
- Отсутствие единой точки отказа
- Высокая доступность (нет центрального координатора)
- Простота реализации и понимания

**Компромиссы:**
- Eventual consistency (временная несогласованность)
- Необходимость идемпотентных операций
- Усложнение отладки распределённых транзакций

### 2) Pessimistic Locking для номеров

**Контекст:** Конкурентный доступ к одному номеру.

**Решение:** `PESSIMISTIC_WRITE` lock в репозитории.

**Обоснование:**
- Гарантия отсутствия двойного бронирования
- Простота реализации
- Короткие транзакции

**Компромиссы:**
- Потенциальное снижение throughput
- Deadlock риски

### 3) Multi-layer Security

**Контекст:** Защита микросервисной архитектуры с несколькими точками входа

**Решение:** Защита на уровне Gateway и каждом сервисе

**Слои безопасности:**
- Gateway Layer: Первичная проверка JWT, CORS, Rate Limiting
- Service Layer: Детальная проверка прав доступа, ролевая модель
- Database Layer: Валидация на уровне БД, constraints

### 4) Service-to-Service Authentication

**Контекст:**  Booking Service должен вызывать Hotel Service безопасно

**Решение:** Двухступенчатая аутентификация:

- JWT Forwarding: Передача пользовательского токена
- API Key: Секретный ключ для межсервисного общения
```java
// Feign конфигурация
public RequestInterceptor requestInterceptor() {
    return template -> {
        // JWT пользователя
        template.header("Authorization", "Bearer " + userToken);
        // API ключ сервиса
        template.header("X-Service-API-Key", serviceApiKey);
    };
}
```
## Troubleshooting

### Сервисы не регистрируются в Eureka
```bash
# Проверьте, что Eureka запущен
curl http://localhost:8761

# Проверьте логи сервиса
tail -f booking-service/logs/application.log
```

### JWT токен невалиден
```bash
# Убедитесь, что секрет одинаковый во всех сервисах
grep jwt.secret */src/main/resources/application.yml
```

### Бронирование не создаётся
```bash
# Проверьте статус Circuit Breaker
curl http://localhost:8081/actuator/health

# Проверьте доступность Hotel Service
curl http://localhost:8082/actuator/health
```

### Конфликт портов
```bash
# Проверьте занятые порты
lsof -i :8080
lsof -i :8081
lsof -i :8082
lsof -i :8761

# Убейте процесс при необходимости
kill -9 <PID>
```