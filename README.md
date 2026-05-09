# CosmoChat

[![Java CI](https://github.com/AlihanSDev/cosmochat-java/actions/workflows/java-ci.yml/badge.svg)](https://github.com/AlihanSDev/cosmochat-java/actions/workflows/java-ci.yml)
![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21-2C2255?logo=java&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.11%2B-3776AB?logo=python&logoColor=white)
![HuggingFace](https://img.shields.io/badge/HuggingFace-API-FFD21E?logo=huggingface&logoColor=000)

CosmoChat — desktop JavaFX‑клиент чата с подключаемыми AI‑бэкендами:

- **Python backend** (локальные модели / высокая нагрузка) — запускается вручную, **не** гоняется в GitHub Actions.
- **Spring Boot HuggingFace backend** — лёгкий HTTP‑сервис для проксирования запросов в HuggingFace Inference API; **проверяется в CI** и поднимается через Docker.

## Архитектура

- `src/main/java` — JavaFX приложение (UI, SQLite, сетевой клиент).
- `backend/` — Python API (локальные модели) и Java Spring‑сервис:
  - `backend/spring-huggingface` — Spring Boot сервис `:8080`
  - `backend/qwen_api.py` — Python API `:5000`
  - `backend/coder_api.py` — Python API `:5001`
- `docker-compose.yml` — поднимает бэкенды (Spring + Python) в Docker.

## Требования

Для **JavaFX клиента**:
- JDK **17+** (проверено на 17)
- Windows PowerShell (скрипты `.ps1`)
- JavaFX SDK уже лежит в репозитории: `javafx/javafx-sdk-21.0.11`

Для **Spring HuggingFace сервиса**:
- Docker (для сборки/запуска контейнера), либо Maven + JDK 17+

Для **Python backend** (опционально):
- Python 3.11+
- Модель(и) в `backend/models/` (тяжёлые файлы не входят в репо)

## Быстрый старт (Windows)

1) Собрать JavaFX клиент (без Maven):
```powershell
.\build.ps1
```

2) Запустить JavaFX клиент:
```powershell
.\run-manual.ps1
```

Важно: UI откроется только на машине с графическим окружением.

## Запуск Spring HuggingFace backend (Docker)

Spring‑сервис слушает `http://localhost:8080` и имеет health endpoint:
- `GET /api/v1/health`

### Через docker-compose (рекомендуется)

Создайте переменные окружения (минимум нужен `HF_TOKEN`):
```powershell
$env:HF_TOKEN="hf_your_token"
```

Запуск:
```powershell
docker compose up --build
```

Проверка:
```powershell
curl http://localhost:8080/api/v1/health
```

### Отдельно (без compose)

Сборка:
```powershell
docker build -f backend/spring-huggingface/Dockerfile -t cosmochat-huggingface .
```

Запуск:
```powershell
docker run --rm -p 8080:8080 -e HF_TOKEN="hf_your_token" cosmochat-huggingface
```

## Запуск Python backend (вручную, не в CI)

Python часть специально **не** запускается в GitHub Actions из‑за моделей и нагрузки.

Пример (локально):
```powershell
python backend\coder_api.py
```

Health:
```powershell
curl http://127.0.0.1:5001/health
```

## CI (GitHub Actions)

Workflow: `.github/workflows/java-ci.yml`

Что проверяется:
- **JavaFX desktop**: компиляция через `build.ps1` (без Maven) на `windows-latest`
- **Spring HuggingFace**: сборка Docker‑образа и проверка `GET /api/v1/health` на `ubuntu-latest`

Что намеренно НЕ проверяется:
- Docker/запуск **Python backend** (тяжёлые модели, высокая нагрузка)

## Полезные переменные окружения

Для Spring HuggingFace сервиса (см. `docker-compose.yml`):
- `HF_TOKEN` (обязательно)
- `HF_API_BASE_URL` (по умолчанию `https://router.huggingface.co/v1`)
- `HF_MODEL_ID` (по умолчанию `Qwen/Qwen2.5-Coder-7B-Instruct`)
- `HF_NSCALE_SUFFIX` (по умолчанию `:featherless-ai`)
- `SERVER_PORT` (по умолчанию `8080`)

## Troubleshooting

- Если JavaFX клиент не стартует: проверьте, что `.\build.ps1` завершился успешно и JDK 17+ доступен в PATH.
- Если Spring контейнер падает сразу: чаще всего не задан `HF_TOKEN` (сервис валидирует токен на старте).
- Если `docker compose up` ругается на healthcheck: убедитесь, что порт `8080` свободен.
