# Asset Charts

Android-приложение на Kotlin для визуализации рыночных данных. Получает свечи из Tinkoff Invest API, строит интерактивный график и накладывает индикаторы Ишимоку и сигналы возможных точек входа.

## Возможности

- Аутентификация через Firebase (email/пароль).
- Загрузка свечей через Tinkoff Invest API.
- Интерактивный график свечей (масштабирование и перемещение).
- Индикаторы Ишимоку: Tenkan-sen, Kijun-sen, Senkou Span A/B, Chikou Span.
- Сигналы покупки/продажи при пересечении Tenkan/Kijun.
- Кэширование свечей и история запросов в Firebase Realtime Database.
- Восстановление последнего запроса при следующем входе.

## Поддерживаемые тикеры

| Тикер | Компания |
|------:|----------|
| sber  | Сбербанк |
| ibm   | IBM |
| appl  | Apple |
| gazp  | Газпром |
| sibn  | Газпром нефть |
| tatn  | Татнефть |
| rosn  | Роснефть |
| nvtk  | Новатэк |
| lkoh  | Лукойл |
| rnft  | Русснефть |
| plzl  | Полюс |
| alrs  | АЛРОСА |
| fesh  | ДВМП |

## Поддерживаемые интервалы

- `1h`
- `4h`
- `1d`
- `1w`
- `1M`

## Стек

- Kotlin, AndroidX, ViewBinding
- MPAndroidChart
- Tinkoff Invest API Java SDK + gRPC OkHttp
- Firebase Auth, Firebase Realtime Database, Firebase Analytics

## Требования

- Android Studio + JDK 17 (AGP 8.x)
- Android SDK (compileSdk 35)
- minSdk 26

## Установка и запуск

1. Клонируйте репозиторий и откройте проект в Android Studio.
2. Укажите токен Tinkoff Invest API в файле `keystore.properties` (в корне проекта):

   ```properties
   TINKOFF_API_TOKEN=ваш_токен
   ```

3. Настройте Firebase:
   - Включите Email/Password в Firebase Auth.
   - Подключите Realtime Database.
   - Создайте `app/google-services.json` для вашего проекта Firebase.
4. Запустите приложение на устройстве/эмуляторе.

## Как пользоваться

1. Зарегистрируйтесь или войдите по email/паролю.
2. Введите тикер (в нижнем регистре, например `sber`) и интервал (`1h`, `4h`, `1d`, `1w`, `1M`).
3. Нажмите **Get chart** — график построится автоматически.

## Хранение данных

- Свечи кэшируются в Firebase по пути `/Candles/{figi}/{interval}`.
- История запросов и последний запрос сохраняются в `/UsersRequests/{userId}`.
