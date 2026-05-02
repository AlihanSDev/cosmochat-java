#!/usr/bin/env python3
"""
Скрипт для загрузки квантованной модели Qwen2.5-1.5B-Coder с HuggingFace.
Модель специализирована на генерации кода и программировании.

Модель: bartowski/Qwen2.5-1.5B-Coder-GGUF (квантованная версия Q8_0)
Размер: ~1.7 GB
Требования к RAM: ~2-3 GB
"""

import os
import sys
from pathlib import Path

try:
    from huggingface_hub import hf_hub_download, list_repo_files
except ImportError:
    print("❌ Библиотека huggingface_hub не установлена.")
    print("Установите её командой:")
    print("  pip install huggingface_hub")
    sys.exit(1)


def download_model(
    repo_id: str = "bartowski/Qwen2.5-1.5B-Coder-GGUF",
    filename: str = "Qwen2.5-1.5B-Coder-Q8_0.gguf",
    output_dir: str = "models/qwen2.5-1.5b-coder-gguf"
) -> str:
    """
    Загружает квантованную модель Qwen Coder с HuggingFace.

    Args:
        repo_id: Идентификатор репозитория на HuggingFace.
        filename: Имя файла модели (квантование Q8_0 — баланс качества и размера).
        output_dir: Локальная директория для сохранения модели.

    Returns:
        Путь к загруженному файлу модели.
    """
    print(f"🚀 Начало загрузки модели {repo_id}...")
    print(f"📄 Файл: {filename}")
    print(f"📁 Директория сохранения: {output_dir}")

    Path(output_dir).mkdir(parents=True, exist_ok=True)

    try:
        print("🔍 Проверка доступных файлов в репозитории...")
        files = list_repo_files(repo_id=repo_id)
        gguf_files = [f for f in files if f.endswith('.gguf')]
        if gguf_files:
            print(f"   Доступные GGUF файлы ({len(gguf_files)}):")
            for f in gguf_files[:5]:
                print(f"      - {f}")
            print(f"      ... и ещё {len(gguf_files) - 5} файлов" if len(gguf_files) > 5 else "")

        print(f"⬇️  Загрузка файла {filename}...")
        model_path = hf_hub_download(
            repo_id=repo_id,
            filename=filename,
            local_dir=output_dir,
            resume_download=True,
        )
        print(f"✅ Модель успешно загружена: {model_path}")

        size = os.path.getsize(model_path)
        size_gb = size / (1024 ** 3)
        print(f"📦 Размер: {size_gb:.2f} GB")

        return model_path
    except Exception as e:
        print(f"❌ Ошибка при загрузке модели: {e}")
        sys.exit(1)


def main():
    """Точка входа скрипта."""
    print("\n" + "="*60)
    print("  Qwen2.5-1.5B-Coder — загрузка модели")
    print("="*60 + "\n")

    output_dir = "models/qwen2.5-1.5b-coder-gguf"
    filename = "Qwen2.5-1.5B-Coder-Q8_0.gguf"

    if len(sys.argv) > 1:
        output_dir = sys.argv[1]
    if len(sys.argv) > 2:
        filename = sys.argv[2]

    download_model(filename=filename, output_dir=output_dir)

    print("\n" + "="*60)
    print("  Информация о модели:")
    print("="*60)
    print(f"   • Модель: Qwen2.5-1.5B-Coder (GGUF)")
    print(f"   • Квантование: Q8_0 (8-битное, баланс качество/размер)")
    print(f"   • Требуемая RAM: ~2-3 GB")
    print(f"   • Специализация: генерация кода, отладка, рефакторинг")
    print(f"   • Для запуска: llama-cpp-python, llamafile или ollama")
    print("="*60 + "\n")


if __name__ == "__main__":
    main()
