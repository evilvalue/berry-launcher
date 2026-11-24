# BerryUp

Лаунчер-надстройка над [Prism Launcher](https://prismlauncher.org/) (и форками вроде
[PineconeMC](https://pineconemc.com/)) для сборки **BerryTrip** — Minecraft 1.21.1 на NeoForge.

BerryUp не запускает игру сам и не заменяет Prism: он находит инстанс BerryTrip,
ставит и обновляет сборку, применяет настройки модов и запускает игру нужным
профилем. Поэтому Prism или PineconeMC нужно поставить один раз — об этом же
написано на [сайте сборки](https://evilvalue.github.io/berry/).

![Окно BerryUp](docs/screenshot.png)

## Скачать

- **BerryUp.zip** — jar + `BerryUp.cmd` + инструкция: <https://evilvalue.github.io/berry/launcher/BerryUp.zip>
- **BerryUp.jar** — только лаунчер: <https://evilvalue.github.io/berry/launcher/BerryUp.jar>
- Релизы: <https://github.com/evilvalue/berry-launcher/releases>
- Метаданные версии и хеши: <https://evilvalue.github.io/berry/launcher/latest-java.json>

Распакуй архив в обычную папку (например `C:\BerryUp`) и запусти `BerryUp.cmd`.
Скрипт сам находит Java: `BERRYUP_JAVA`, `JAVA_HOME`, рантаймы Prism/PineconeMC,
затем системную. Отдельно ставить Java не нужно.

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

Скрипт собирает fat jar, пакует `BerryUp.zip`, считает SHA-256 и пишет
`latest-java.json`, а затем копирует jar/zip/cmd в `packwiz/launcher` сайта
(флаг `-NoCopy` — только собрать).

## Тестовые режимы

```bash
java -jar BerryUp.jar --selftest                  # что найдено: JSON без окна
java -jar BerryUp.jar --ping [host] [port]        # пинг по протоколу Minecraft
java -jar BerryUp.jar --render build/render       # вкладки в PNG
BerryUp.cmd --java                                # какой Java нашёл скрипт
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
- **Обновление BerryUp** — новый jar скачивается в `%LOCALAPPDATA%\BerryUp\update`,
  сверяется по SHA-256, а `BerryUp.cmd` подменяет файл при следующем запуске
  (работающий jar на Windows заменить нельзя).

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
  Updater.java    скачивание и проверка нового jar
  ui/             Swing-интерфейс на FlatLaf
packaging/        BerryUp.cmd и README.txt для игроков
build/            publish-java.ps1
```

## Ограничения

Игра и живое окно в CI не проверяются: используются `--selftest`, `--ping`
и оффскрин-рендер вкладок. Сборка ведёт себя как обычный Prism-инстанс,
поэтому всё, что делает BerryUp, видно в `%TEMP%\BerryUp.log`.
