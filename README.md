# BerryUp

Лаунчер-надстройка над [Prism Launcher](https://prismlauncher.org/) (и форками вроде
[PineconeMC](https://pineconemc.com/)) для сборки **BerryTrip** — Minecraft 1.21.1 на NeoForge.

BerryUp не запускает игру сам и не заменяет Prism: он находит инстанс BerryTrip,
ставит и обновляет сборку, применяет настройки модов и запускает игру нужным
профилем. Поэтому Prism или PineconeMC нужно поставить один раз — об этом же
написано на [сайте сборки](https://evilvalue.github.io/berry/).

Интерфейс — в духе Minecraft: пиксельный шрифт Monocraft, логотип-малина и
переключатель светлой/тёмной темы в «Настройках».

![Окно BerryUp](docs/screenshot.png)

## Скачать

- **BerryUp.zip** — лаунчер одним архивом (`BerryUp.jar` + `START-BerryUp.bat`): <https://github.com/evilvalue/berry-launcher/releases/latest/download/BerryUp.zip>
- **BerryUp.jar** — только jar, если `.jar` уже открывается Java: <https://evilvalue.github.io/berry/launcher/BerryUp.jar>
- Релизы: <https://github.com/evilvalue/berry-launcher/releases>
- Метаданные версии и хеши: <https://evilvalue.github.io/berry/launcher/latest-java.json>

Запусти jar из любой папки — при первом запуске BerryUp сам поставится в
`%LOCALAPPDATA%\BerryUp`, сделает иконку и ярлыки на рабочем столе и в меню «Пуск»,
а скачанный файл уберёт. Дальше запускай с ярлыка: ярлык берёт Java из Prism
(или текущую Java 9+), поэтому на масштабе Windows 125–150% шрифт остаётся резким.
Отдельно ставить Java не нужно — BerryUp рассчитан на Java 8+ и запускается на
Java из Prism Launcher. В архиве рядом с jar лежит `START-BerryUp.bat` — двойной щелчок по нему и запускает лаунчер.

На чистой Windows `.jar` может быть ничем не связан, поэтому в архиве рядом с jar
лежит `START-BerryUp.bat`: он находит Java из Prism, `JAVA_HOME`, `PATH`, официального
лаунчера Minecraft или установленных JDK и запускает лаунчер без настройки ассоциаций.

## Сборка

Нужен JDK 8+ (проект целится в Java 8, поэтому jar идёт и на Java 21 из Prism).

```bash
./gradlew fatJar        # build/libs/BerryUp.jar со всеми зависимостями
./gradlew run           # запуск из исходников
```

Собрать дистрибутив и обновить файлы сайта:

```powershell
powershell -ExecutionPolicy Bypass -File build\publish-java.ps1 -Notes "что нового"
```

Скрипт собирает fat jar, считает SHA-256 и пишет `latest-java.json`, а затем копирует
`BerryUp.jar` и метаданные в `packwiz/launcher` сайта (флаг `-NoCopy` — только собрать).

## Тестовые режимы

```bash
java -jar BerryUp.jar --selftest                  # что найдено: JSON без окна
java -jar BerryUp.jar --ping [host] [port]        # пинг по протоколу Minecraft
java -jar BerryUp.jar --render build/render       # вкладки в PNG
java -jar BerryUp.jar --install                   # поставить в %LOCALAPPDATA% и сделать ярлыки
java -jar BerryUp.jar --shortcut                  # пересоздать ярлыки
```

## Как это работает

- **Поиск лаунчера** — `%LOCALAPPDATA%\Programs`, `Program Files`, `Program Files (x86)`,
  имена `prismlauncher.exe`, `pineconemc.exe`, `elyprismlauncher.exe`; путь можно
  указать вручную в «Настройках» (включая папку данных).
- **Инстанс** — по `name=BerryTrip` в `instance.cfg`, а не по имени папки.
- **Запуск** — `prism --launch <инстанс> [--server <адрес>]`.
- **Обновление сборки** — `java -jar packwiz-installer-bootstrap.jar -g <pack.toml>`
  Java-рантаймом самого Prism.
- **Моды** — выключенные уносятся в `minecraft/berry-off`, чтобы NeoForge их не грузил;
  перед синхронизацией возвращаются, после — снова убираются.
- **Память, ресурспаки, шейдеры** — правятся в `instance.cfg`, `options.txt`,
  `config/iris.properties` инстанса.
- **Установка** — при первом запуске jar переезжает в `%LOCALAPPDATA%\BerryUp`,
  рядом появляются `berry.ico` и ярлыки на рабочем столе и в «Пуске».
- **Обновление BerryUp** — новый jar скачивается в `%LOCALAPPDATA%\BerryUp\update`,
  сверяется по SHA-256, а затем BerryUp запускает себя вторым процессом
  (`--apply-update`): тот ждёт выхода старого окна, подменяет файл на месте и
  открывает новую версию (работающий jar на Windows заменить нельзя).
- **Вход** — «Аккаунт» → ник → «Войти». Сервер выдаёт одноразовую ссылку на
  Telegram; после Start ник сразу привязывается и попадает в вайтлист, а лаунчер
  сам сохраняет токен. Адрес API — `auth.api` в `%APPDATA%\BerryUp\config.ini`
  (по умолчанию `http://135.181.126.167:25712`).
- **Скин и плащ** — PNG или GIF показываются объёмной моделью прямо в лаунчере:
  крутятся мышью, файл можно перетащить в карточку. Поддержаны только форматы
  QuickSkin: скин 64×32 или 64×64…2048×2048, плащ 64×32…2048×1024. Файл кладётся
  в пул мода (`quickskin/uploads/skins` или `capes`) и прописывается в
  `quickskin-client.json`.
- **Интерфейс** — светлая и тёмная темы, нейтральный фон, 3D-модель без сцены;
  окно держит 3:2 и растёт за угол без прокрутки страниц. Масштаб 100–200%
  задаётся в настройках, текст рисуется с LCD-сглаживанием.

Настройки лежат в `%APPDATA%\BerryUp` и совместимы с прежней exe-версией;
`BERRYUP_CONFIG_DIR` подменяет папку настроек для тестов.

## Структура

```
src/main/java/ru/berrytrip/berryup/
  Cfg.java        адреса, версия, три входа на сервер
  Prism.java      поиск лаунчера, инстанса и Java
  Sync.java       установка сборки, packwiz, запуск игры
  Tweaks.java     моды, ресурспаки, шейдеры, память
  Ping.java       пинг сервера (status)
  Updater.java    скачивание, проверка и самоподмена jar
  ui/             Swing-интерфейс на FlatLaf
build/            publish-java.ps1
```

## Ограничения

Игра и живое окно в CI не проверяются: используются `--selftest`, `--ping`
и оффскрин-рендер вкладок. Сборка ведёт себя как обычный Prism-инстанс,
поэтому всё, что делает BerryUp, видно в `%TEMP%\BerryUp.log`.
