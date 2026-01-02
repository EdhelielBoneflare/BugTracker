# BugTracker

## Участники
Груздева Анна 5130904/30105 \
Ананьева Лариса 5130904/30105 \
Березнева Екатерина 5130904/30105

## Этапы
### Определение проблемы
### Выработка требований
### Разработка архитектуры и детальное проектирование
### Кодирование и отладка
### Unit тестирование
### Интеграционное тестирование
### Сборка
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