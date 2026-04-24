# OTP Codes Service

Сервис для защиты операций через OTP. Поддерживает генерацию OTP и их рассылку по каналам **Email**, **SMS**, **Telegram
**, **File**.

## Стек

- Java 21, Spring Boot 4.0.5
- PostgreSQL 17, Liquibase, JDBC
- JWTß
- Angus Mail (SMTP), jsmpp (SMPP), HttpClient (Telegram)
- Maven

## Структура

```
src/main/java/ru/example/otpcodes
├── api/          - REST + Handler
├── service/      - бизнес логика
├── sender/       - NotificationSender + реализации + Router
├── dao/          - JDBC DAO
├── domain/       - модели
├── dto/          - DTO
├── security/     - JWT + SecurityConfig
├── scheduler/    - шедулер
└── exception/    - exceptions
```

## Запуск

```bash
docker compose up --build -d
```

- `postgres` - порт 5432
- `mailpit` - SMTP-эмулятор, UI http://localhost:8025
- `smppsim` - SMPP-эмулятор ([MavoCz/smscsim](https://github.com/MavoCz/smscsim)), порт 2775
- `app` - порт 8080

Файл с OTP - внутри контейнера по пути `/app/otp-codes.log` (volume `otp-codes-log`).

### Параметры

Для Telegram создать `.env` в корне:

```
TELEGRAM_BOT_TOKEN=123454321:токен
TELEGRAM_CHAT_ID=12345

JWT_SECRET=secret
```

SMPP эмулятор поднимется автоматически в докере. Если контейнер `smppsim` погашен, то вызов SMS канала вернет 502.

## API

Все, кроме `/auth/*`, требуют токен.

### Публичные

**Регистрация** - `POST /auth/register`

```json
{
  "login": "mr credo",
  "password": "secret123",
  "role": "USER"
}
```

Роль `ADMIN` доступна, пока в БД нет ни одного админа - второй крашнется с 409.

**Логин** - `POST /auth/login`

```json
{
  "login": "mr credo",
  "password": "secret123"
}
```

Ответ:

```json
{
  "token": "eyJ...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600
}
```

### Пользователь (`USER`)

**Генерация OTP** - `POST /otp/generate`

```json
{
  "operationId": "tx-42",
  "channel": "EMAIL",
  "destination": "mrcredo@example.com"
}
```

- `channel` - `EMAIL` | `SMS` | `TELEGRAM` | `FILE`
- `destination` - адрес получателя (для `FILE` может быть пустым)
- Ответ: `{otpId, operationId, expiresAt}`

**Проверка OTP** - `POST /otp/validate`

```json
{
  "code": "123456",
  "operationId": "tx-42"
}
```

Ответ: `{valid: true|false, status: "USED"|"EXPIRED"|"INVALID", message}`. Валидный переводится в `USED` и больше
никогда не примется.

### Администратор (роль `ADMIN`)

- `GET /admin/users` - список пользователей без админов
- `DELETE /admin/users/{id}` - удаление пользователя и его OTP
- `GET /admin/otp-config`
- `PUT /admin/otp-config` - `{"codeLength": 6, "ttlSeconds": 300}` (`codeLength` 4-10, `ttlSeconds` 30-86400)

## БД

- **users**
- **otp_config**
- **otp_codes**

## Шедулер

Раз в `otp.expiration.scan-interval-ms` (по дефолту 60000 мс):

```sql
UPDATE otp_codes SET status='EXPIRED' WHERE status = 'ACTIVE' AND expires_at < NOW()
```

## Env

| ENV                                                                                                            | application.yaml             | default                                |
|----------------------------------------------------------------------------------------------------------------|------------------------------|----------------------------------------|
| `DB_URL`                                                                                                       | `spring.datasource.url`      | `jdbc:postgresql://localhost:5432/otp` |
| `DB_USERNAME`                                                                                                  | `spring.datasource.username` | `otp`                                  |
| `DB_PASSWORD`                                                                                                  | `spring.datasource.password` | `otp`                                  |
| `JWT_SECRET`                                                                                                   | `security.jwt.secret`        | тестовый                               |
| `JWT_TTL_MINUTES`                                                                                              | `security.jwt.ttl-minutes`   | `60`                                   |
| `OTP_FILE_PATH`                                                                                                | `otp.file.path`              | `./otp-codes.log`                      |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_AUTH` / `SMTP_STARTTLS` / `EMAIL_USERNAME` / `EMAIL_PASSWORD` / `EMAIL_FROM` | `email.properties`           | MailHog-friendly                       |
| `SMPP_HOST` / `SMPP_PORT` / `SMPP_SYSTEM_ID` / `SMPP_PASSWORD` / `SMPP_SYSTEM_TYPE` / `SMPP_SOURCE_ADDR`       | `sms.properties`             | SMPPsim                                |
| `TELEGRAM_BOT_TOKEN` / `TELEGRAM_CHAT_ID` / `TELEGRAM_API_URL`                                                 | `telegram.properties`        | заглушки                               |

## Пример

```bash
# регистрация админов
curl -X POST localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"login":"admin","password":"admin123","role":"ADMIN"}'

# логин
TOKEN=$(curl -s -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"admin","password":"admin123"}' | jq -r .token)

# регистрация юзеров
curl -X POST localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"login":"mr credo","password":"secret123","role":"USER"}'

# логин
UTOKEN=$(curl -s -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"mr credo","password":"secret123"}' | jq -r .token)

# генерация OTP в файл
curl -X POST localhost:8080/otp/generate \
  -H "Authorization: Bearer $UTOKEN" -H 'Content-Type: application/json' \
  -d '{"operationId":"tx-1","channel":"FILE"}'

# проверка OTP (виден в ./otp-codes.log)
curl -X POST localhost:8080/otp/validate \
  -H "Authorization: Bearer $UTOKEN" -H 'Content-Type: application/json' \
  -d '{"code":"123456","operationId":"tx-1"}'

# список пользователей
curl -H "Authorization: Bearer $TOKEN" localhost:8080/admin/users

# изменить конфиг
curl -X PUT localhost:8080/admin/otp-config \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"codeLength":8,"ttlSeconds":666}'
```

## Тестирование

- **Email**: Mailpit UI http://localhost:8025
- **File**: `docker compose exec app cat /app/otp-codes.log`
- **SMS**:
    - `docker compose logs app | grep "SMS with OTP"` - подтверждение отправки
    - `docker compose logs smppsim`
- **Telegram**: с реальной отправкой. Создать бота, написать ему, получить токен и chat_id через `getUpdates`, положить
  в `.env`:
  ```
  TELEGRAM_BOT_TOKEN=123454321:токен
  TELEGRAM_CHAT_ID=12345
  ```
