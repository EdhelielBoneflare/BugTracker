# BugTracker
## Участники
- Груздева Анна `5130904/30105`
- Ананьева Лариса `5130904/30105`
- Березнева Екатерина `5130904/30105`
## Этапы
### Определение проблемы
Команде разработчиков сложно отловить баги, вызывающие не явные ошибки, а непредвиденное поведение системы. В ручных отчетах контекст зачастую недостаточен, из-за чего много времени уходит на уточнение деталей и поиск причины/механизма сбоя. В результате увеличивается количество времени необходимое для устранения багов, что замедляет процесс разработки.
### Выработка требований
Целевая нагрузка: ориентироваться на ~10 000 пользователей в сутки. Период хранения данных: 5 лет.

Пользовательские истории:
- Как менеджер продукта, я хочу просматривать список задач и фильтры по проекту, чтобы приоритизировать работу.
- Как разработчик, я хочу получать полный отчёт с информацией о сессии и вложениями, чтобы воспроизводить и исправлять баги.
- Как пользователь, я хочу отправить баг с вложением и получить подтверждение получения.


### Разработка архитектуры и детальное проектирование
Стек: Java 21, Spring Boot, Spring Data JPA, PostgreSQL, JWT (Spring Security), frontend на TypeScript/JS. Сервис использует `Gradle` и запускается в Docker.\
Ключевые нефункциональные требования:
- P95 latency: чтения ≤ 200 ms, записи ≤ 500 ms.
Объемы трафика и хранения:
- Оценки R/W: чтения доминируют (примерно 70% чтений / 30% записей)
- Ориентировочные пики: ~100 RPS чтений, ~43 RPS записей.
- Средний размер HTTP-пакета: чтение 0.5–2 KB, запись 1–10 KB
Как сервис выдержит нефункциональные требования:
- Использование индексов в базе данных для ускорения операций чтения.
- Вложения вынесены в отдельное хранилище для снижения нагрузки на БД.
- Допускается возможность шардирования и партиционирования таблиц для масштабирования.
#### Контракты API
Полную документацию API можно найти по ссылке: [API Documentation](http://localhost:8080/swagger-ui/)
Основные эндпоинты:
Аутентификация и регистрация:
- POST `/api/auth/login` - body { username, password } → 200 { token } 
- POST `/api/auth/register` — body { username, password } → new token.
Проекты:
- GET `/api/projects` — получить список проектов. → 200 [{id, name}]
- POST `/api/projects` — создать проект. — body { name } → 201 {id, name}
- PATCH `/api/users/{userId}/projects/assign/{projectId}` → 200 {id, username, role, projectIds: []}
Отчеты о багах:
- POST `/api/reports/widget` — создать отчет о баге. — body { projectId, sessionId, title, tags: [], reportedAt, comments, userEmail, screen, currentUrl, userProvided} → 200
- GET `/api/reports/{reportId}` — получить отчет о баге. → 200 { id, projectId, sessionId, title, tags: [], reportedAt, comments, userEmail, screen, currentUrl, userProvided, eventIds: [], level, status, developerName}
Сессии:
- POST `/api/sessions` — создать сессию. — body { projectId, startTime, browser, os, deviceType, screenResolution, viewportSize, language, userAgent, ipAddress, cookiesHash, plugins: [] } → 201 { message, sessionId }
- GET `/api/sessions/{sessionId}` — получить сессию. → 200 { sessionId, projectId, isActive, startTime, endTime, browser, browserVersion, os, deviceType, screenResolution, viewportSize, language, userAgent, ipAddress, plugins: []}
Действия пользователя:
- POST `/api/events` — записать действие пользователя. — body { sessionId, type, name, log, stackTrace, url, element, timestamp, metadata: {filename, lineNumber, statusCode} } → 201
- GET `/api/events/{id}` — получить действие пользователя. → 200 { id, sessionId, type, name, log, stackTrace, url, element, timestamp, metadata: {filename, lineNumber, statusCode} }
### Кодирование и отладка
### Unit тестирование
Unit тесты в src/test/java (JUnit 5, Mockito).
### Интеграционное тестирование
Реализован сценарий end-to-end: создание проекта → создание задачи → получение списка → получение деталей (покрывает одну пользовательскую историю).
### Сборка
Ожидаемые файлы:
- docker-compose.ci.yml — сервисы: db (postgres), minio, ci-runner, app.
- scripts/ci.sh — единый скрипт для сборки, запуска unit и integration тестов и старта приложения.
Единая команда для сборки и тестирования:
```bash ./scripts/ci.sh```

Чтобы встроить систему в свой сайт на все страницы лобавьте следующий код в раздел <head> вашего HTML документа:
```html
<script src="https://example.com/bugtracker.js"></script>
<script>
        (async function() {
            // Server base URL (where /api/* endpoints live)
            const apiUrl = 'http://localhost:8080';
            // projectId received from service on project registration
            const projectId = 1;
            
            const bt = new window.BugTracker.BugTracker({
                apiUrl: apiUrl,
                projectId: projectId,
                // flushInterval: 0   // default is 0 (disabled); only ACTION/ERROR trigger immediate flush
                // otherwise sec*1000 flush interval
            });

            try {
                await bt.initialize();            // initialize() is async and will:
                                                  // - create a local session and attempt server session creation in background,
                                                  // - start trackers (errors, network, user actions),
                                                  // - create the Report button UI.
            } catch (err) {
                console.error('BugTracker failed to initialize:', err);
            }
        })();
    </script>
```