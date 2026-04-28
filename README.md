# Security Token Service

Бэкенд-сервис для защиты операций с помощью одноразовых кодов подтверждения (OTP). 
Сервис генерирует временные коды, доставляет их через различные каналы и проверяет их корректность

## Stack

- **Java 17**
- **PostgreSQL 17** - хранение пользователей, конфигурации и OTP-кодов
- **JDBC** - взаимодействие с базой данных
- **com.sun.net.httpserver** - HTTP-сервер
- **JWT (HMAC-SHA256)** - токенная аутентификация
- **BCrypt** - хеширование паролей и OTP-кодов
- **Flyway** - миграции базы данных
- **Logback** - логирование
- **Maven** - система сборки
- **Docker / Docker Compose** - контейнеризация

## Структура проекта

```text
src/main/java/org/example/securitytokenservice/
├── api/          - HTTP-обработчики (AuthHandler, AdminHandler, OtpHandler, BaseHandler)
├── config/       - конфигурация приложения (AppConfig)
├── dao/          - слой доступа к данным (UserDao, OtpCodeDao, OtpConfigDao)
├── db/           - подключение к БД и миграции (Database, SchemaMigrator)
├── delivery/     - каналы доставки OTP (Email, SMS, Telegram, File)
├── dto/          - объекты запросов и ответов
├── error/        - обработка ошибок (ApiException)
├── model/        - модели данных (User, OtpCode, OtpConfig, Role, OtpStatus)
├── scheduler/    - планировщик истечения OTP-кодов
├── security/     - JWT-токены и хеширование паролей
└── service/      - бизнес-логика (AuthService, AdminService, OtpService)
```

## Возможности

- Регистрация и аутентификация пользователей с JWT-токенами
- Два типа ролей: **ADMIN** и **USER**
- Только один администратор в системе
- Генерация OTP-кодов привязанных к операции
- Доставка кодов через **Email**, **SMS (SMPP)**, **Telegram**, **файл**
- Валидация OTP с проверкой срока действия
- Автоматическое истечение просроченных кодов
- Настройка длины кода и времени жизни администратором
- OTP-коды хранятся в БД в хешированном виде

## Быстрый старт

### Требования

- Docker и Docker Compose

### Запуск

```bash
# 1. Клонировать репозиторий
git clone https://github.com/yumenfff/SecurityTokenService.git
cd SecurityTokenService

# 2. Настроить конфигурационные файлы (скопировать из примеров)
cp src/main/resources/email.properties.example src/main/resources/email.properties
cp src/main/resources/sms.properties.example src/main/resources/sms.properties
cp src/main/resources/telegram.properties.example src/main/resources/telegram.properties

# 3. Заполнить конфиги своими данными

# 4. Запустить
docker compose up --build
```

Сервис будет доступен на `http://localhost:8080`

База данных будет доступна на `localhost:5433` с именем `security_token_db`, пользователем `postgres` и паролем `postgres`

При старте автоматически применяются миграции **Flyway** из `src/main/resources/db/migration`

### Настройка каналов доставки

#### Email

- Заполните `src/main/resources/email.properties`

#### SMS (SMPP)

- Поднимите SMPP-эмулятор
- Заполните `src/main/resources/sms.properties` значениями эмулятора

#### Telegram

1. Создайте бота через `@BotFather` и получите токен
2. Напишите любое сообщение вашему боту в Telegram
3. Получите `chat_id` запросом:

```bash
curl "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates"
```

4. Заполните `src/main/resources/telegram.properties`

#### File

 - Для канала `FILE` код сохраняется в файл `otp-codes/otp-codes.txt` в корне проекта

### Остановка

```bash
docker compose down

# Полная очистка с удалением данных БД
docker compose down -v
```

## API

### Аутентификация

| Метод | Эндпоинт | Описание |
|-------|----------|---------|
| POST | `/api/auth/register` | Регистрация пользователя |
| POST | `/api/auth/login` | Вход, получение JWT-токена |

### OTP (требует токен)

| Метод | Эндпоинт | Описание |
|-------|----------|---------|
| POST | `/api/otp/generate` | Генерация и отправка OTP |
| POST | `/api/otp/validate` | Валидация OTP |

### Администратор (требует токен с ролью ADMIN)

| Метод | Эндпоинт | Описание |
|-------|----------|---------|
| GET | `/api/admin/users` | Список пользователей |
| DELETE | `/api/admin/users/{id}` | Удаление пользователя |
| PUT | `/api/admin/otp-config` | Изменение конфигурации OTP |

### Разграничение доступа

- Эндпоинты `/api/admin/*` доступны только роли `ADMIN`
- Для роли `USER` при обращении к `/api/admin/*` возвращается `403 Access denied`
- Эндпоинты `/api/otp/*` требуют валидного Bearer JWT-токена

## Примеры запросов

### Регистрация и вход

```bash
# Регистрация администратора
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"admin123","role":"ADMIN"}'

# Вход (возвращает JWT-токен)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"admin123"}'
```

### Генерация и валидация OTP

```bash
# Генерация OTP в файл
curl -X POST http://localhost:8080/api/otp/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"operationId":"op-1","destination":"user1","channel":"FILE"}'

# Генерация OTP на Email
curl -X POST http://localhost:8080/api/otp/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"operationId":"op-email","destination":"YOUR_EMAIL","channel":"EMAIL"}'

# Генерация OTP через Telegram
curl -X POST http://localhost:8080/api/otp/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"operationId":"op-telegram","destination":"telegram-user","channel":"TELEGRAM"}'

# Генерация OTP через SMS
curl -X POST http://localhost:8080/api/otp/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"operationId":"op-sms","destination":"YOUR_NUMBER","channel":"SMS"}'
  
# Валидация OTP
curl -X POST http://localhost:8080/api/otp/validate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"operationId":"op-1","code":"123456"}'
```

### Администрирование

```bash
# Список пользователей
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer ADMIN_TOKEN"

# Изменение конфигурации OTP
curl -X PUT http://localhost:8080/api/admin/otp-config \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -d '{"ttlSeconds":600,"codeLength":8}'

# Удаление пользователя
curl -X DELETE http://localhost:8080/api/admin/users/2 \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

## Тестирование

В корне проекта находится файл `api-scenarios.http` для JetBrains HTTP Client. 
Открой его в IntelliJ IDEA и запускай запросы по порядку сверху вниз

После запросов логина токены сохраняются в global-переменные HTTP Client и используются в следующих запросах

Для чистого прогона:

```bash
docker compose down -v && docker compose up --build
```

## Статусы OTP-кодов

| Статус | Описание |
|--------|---------|
| `ACTIVE` | Код активен и ожидает валидации |
| `EXPIRED` | Срок действия кода истёк |
| `USED` | Код успешно использован |

## Конфигурация

Настройки приложения через переменные окружения в `application.properties`:

| Переменная | Описание | По умолчанию                                         |
|-----------|---------|------------------------------------------------------|
| `APP_PORT` | Порт сервера | `8080`                                               |
| `APP_DB_URL` | JDBC URL PostgreSQL | `jdbc:postgresql://localhost:5433/security_token_db` |
| `APP_DB_USER` | Пользователь БД | `postgres`                                           |
| `APP_DB_PASSWORD` | Пароль БД | `postgres`                                           |
| `APP_TOKEN_SECRET` | Секрет для JWT | —                                                    |
| `APP_TOKEN_TTL_MINUTES` | Время жизни токена (мин) | `60`                                                 |
| `APP_OTP_DEFAULT_LENGTH` | Длина OTP-кода | `6`                                                  |
| `APP_OTP_DEFAULT_TTL_SECONDS` | Время жизни OTP (сек) | `300`                                                |
| `APP_OTP_SCHEDULER_FIXED_DELAY_SECONDS` | Интервал планировщика (сек) | `30`                                                 |
| `APP_FILE_OTP_PATH` | Путь к файлу/папке для OTP | `otp-codes`                                      |
| `APP_EMAIL_RESOURCE` | Путь к `email.properties` | `email.properties`                                   |
| `APP_SMS_RESOURCE` | Путь к `sms.properties` | `sms.properties`                                     |
| `APP_TELEGRAM_RESOURCE` | Путь к `telegram.properties` | `telegram.properties`                                |
