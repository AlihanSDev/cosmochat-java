#!/usr/bin/env python3
"""
Скрипт для загрузки квантованной модели Qwen2.5-Coder-1.5B с HuggingFace.
Модель специализирована на генерации кода и программировании.

Репозиторий: lmstudio-community/Qwen2.5-Coder-1.5B-GGUF
Формат: GGUF, квантование Q8_0 (~1.7 GB)
Требования к RAM: ~2-3 GB
"""

import os
import sys
from pathlib import Path

try:
    from huggingface_hub import hf_hub_download, list_repo_files
except ImportError:
    print("❌ huggingface_hub не установлен.")
    print("Установи: pip install huggingface_hub")
    sys.exit(1)


def find_best_gguf(repo_id: str, preferred: str = "Q8_0") -> str:
    """
    Автоматически выбирает GGUF файл (предпочитает Q8_0).
    """
    print("🔍 Получение списка файлов...")
    files = list_repo_files(repo_id)

    gguf_files = [f for f in files if f.endswith(".gguf")]

    if not gguf_files:
        raise RuntimeError("GGUF файлы не найдены!")

    print(f"📦 Найдено GGUF файлов: {len(gguf_files)}")

    # Пытаемся найти предпочтительное квантование
    for f in gguf_files:
        if preferred in f:
            print(f"✅ Выбран (предпочтительный): {f}")
            return f

    # fallback — первый файл
    print(f"⚠️ {preferred} не найден, используем: {gguf_files[0]}")
    return gguf_files[0]


def download_model(
    repo_id: str = "lmstudio-community/Qwen2.5-Coder-1.5B-GGUF",
    filename: str = "Qwen2.5-Coder-1.5B-Q8_0.gguf",
    output_dir: str = "models/qwen2.5-coder-1.5b-gguf"
) -> str:
    """
    Загружает модель с HuggingFace.
    """

    print("\n🚀 Старт загрузки")
    print(f"📚 Репозиторий: {repo_id}")

    Path(output_dir).mkdir(parents=True, exist_ok=True)

    try:
        # если файл не указан — выбираем автоматически
        if filename is None:
            filename = find_best_gguf(repo_id)

        print(f"📄 Файл: {filename}")
        print(f"📁 Папка: {output_dir}")

        model_path = hf_hub_download(
            repo_id=repo_id,
            filename=filename,
            local_dir=output_dir,
            local_dir_use_symlinks=False,  # важно для Windows
            resume_download=True,
        )

        size = os.path.getsize(model_path) / (1024 ** 3)

        print("\n✅ УСПЕХ")
        print(f"📍 Путь: {model_path}")
        print(f"📦 Размер: {size:.2f} GB")

        return model_path

    except Exception as e:
        print(f"\n❌ Ошибка: {e}")
        sys.exit(1)


def main():
    """Точка входа скрипта."""
    print("\n" + "="*60)
    print("  Qwen2.5-Coder-1.5B — загрузка модели")
    print("="*60 + "\n")

    output_dir = "models/qwen2.5-coder-1.5b-gguf"
    filename = None

    if len(sys.argv) > 1:
        output_dir = sys.argv[1]
    if len(sys.argv) > 2:
        filename = sys.argv[2]

    download_model(
        repo_id="lmstudio-community/Qwen2.5-Coder-1.5B-GGUF",
        filename=filename,
        output_dir=output_dir
    )

    print("\n" + "="*60)
    print("  Информация о модели:")
    print("="*60)
    print(f"   • Модель: Qwen2.5-Coder-1.5B (GGUF)")
    print(f"   • Квантование: Q8_0 (если доступно)")
    print(f"   • Требуемая RAM: ~2-3 GB")
    print(f"   • Специализация: генерация кода, отладка, рефакторинг")
    print(f"   • Для запуска: llama-cpp-python, llamafile или ollama")
    print("="*60 + "\n")


if __name__ == "__main__":
    main()