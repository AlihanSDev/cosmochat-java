#!/usr/bin/env python3
"""
Скрипт для загрузки эмбеддингов BAAI/bge-m3 с HuggingFace.
Модель для генерации высококачественных эмбеддингов текста.
"""

import os
import sys
from pathlib import Path

try:
    from huggingface_hub import snapshot_download
except ImportError:
    print("❌ Библиотека huggingface_hub не установлена.")
    print("Установите её командой:")
    print("  pip install huggingface_hub")
    sys.exit(1)


def download_embeddings(
    repo_id: str = "BAAI/bge-m3",
    output_dir: str = "models/bge-m3"
) -> str:
    """
    Загружает модель эмбеддингов с HuggingFace.

    Args:
        repo_id: Идентификатор репозитория на HuggingFace.
        output_dir: Локальная директория для сохранения модели.

    Returns:
        Путь к загруженной модели.
    """
    print(f"🚀 Начало загрузки модели {repo_id}...")
    print(f"📁 Директория сохранения: {output_dir}")

    # Создаём директорию, если она не существует
    Path(output_dir).mkdir(parents=True, exist_ok=True)

    try:
        # Загружаем модель
        model_path = snapshot_download(
            repo_id=repo_id,
            local_dir=output_dir,
            local_dir_use_symlinks=False,
            resume_download=True,
        )
        print(f"✅ Модель успешно загружена в: {model_path}")
        return model_path
    except Exception as e:
        print(f"❌ Ошибка при загрузке модели: {e}")
        sys.exit(1)


def main():
    """Точка входа скрипта."""
    # Можно переопределить через аргументы командной строки
    if len(sys.argv) > 1:
        output_dir = sys.argv[1]
    else:
        output_dir = "models/bge-m3"

    download_embeddings(output_dir=output_dir)

    print("\n📋 Содержимое загруженной модели:")
    for root, _, files in os.walk(output_dir):
        for file in files:
            filepath = os.path.join(root, file)
            size = os.path.getsize(filepath)
            print(f"   {filepath} ({size:,} байт)")


if __name__ == "__main__":
    main()
