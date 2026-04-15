# Аукцион

Система аукциона на `Spring Boot + Thymeleaf + PostgreSQL`.

## Что реализовано

- регистрация и авторизация пользователей;
- роли: администратор, продавец, участник торгов;
- разные экраны и действия в зависимости от роли;
- публикация лотов продавцом;
- просмотр списка лотов и карточки лота;
- размещение ставок участником торгов;
- автоматическое закрытие просроченных лотов;
- определение текущего лидера и победителя.

## Правила

- продавец не может делать ставки;
- участник торгов не может создавать лоты;
- нельзя ставить на собственный лот;
- ставка должна быть не меньше текущей цены плюс шаг;
- после истечения времени лот закрывается.

## Запуск

1. Убедитесь, что установлен `Java 17`.
2. Задайте пароль БД через переменную окружения `SPRING_DATASOURCE_PASSWORD`.
3. При необходимости задайте `SPRING_DATASOURCE_URL` и `SPRING_DATASOURCE_USERNAME`.
4. Запустите проект из IDE как `focussashka.auction.AuctionApplication`.
5. Либо выполните `mvn spring-boot:run`, если Maven установлен в системе.
6. Откройте `http://localhost:8080`.

## Запуск в Docker

1. Задайте пароль БД через переменную окружения `DB_PASSWORD`.
2. При необходимости задайте `DB_NAME`, `DB_USERNAME` и `APP_PORT`.
3. Выполните `docker compose up --build -d`.
4. Откройте `http://localhost:8081`.
5. Для остановки выполните `docker compose down`.

Если нужно удалить и контейнеры, и данные БД, используйте `docker compose down -v`.

Порт приложения можно переопределить через переменную окружения `APP_PORT`, например:

```bash
DB_PASSWORD=strong-password APP_PORT=8080 docker compose up --build -d
```

## Демо-данные

- по умолчанию демо-пользователи и тестовый лот не создаются;
- для локальной демонстрации их можно включить вручную через `app.demo-data.enabled=true`;
- пароли демо-пользователей должны передаваться через переменные окружения и не хранятся в репозитории.

## База данных

- используется `PostgreSQL`;
- по умолчанию приложение ожидает БД на `jdbc:postgresql://localhost:5432/auction`;
- значения можно переопределить переменными окружения `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.

Пример создания БД и пользователя:

```sql
CREATE USER auction WITH PASSWORD 'auction';
CREATE DATABASE auction OWNER auction;
```

Пример:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/auction
export SPRING_DATASOURCE_USERNAME=auction
export SPRING_DATASOURCE_PASSWORD=strong-password
```

Пример запуска с демо-данными:

```bash
export APP_DEMO_DATA_ENABLED=true
export APP_DEMO_DATA_ADMIN_PASSWORD=admin-password
export APP_DEMO_DATA_SELLER_PASSWORD=seller-password
export APP_DEMO_DATA_BIDDER_PASSWORD=bidder-password
```
