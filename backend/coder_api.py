#!/usr/bin/env python3
"""
Локальный API сервер для Qwen2.5-Coder-1.5B-Instruct GGUF.
Использует llama-cpp-python для запуска модели.

Запуск:
    python backend/coder_api.py

Установка зависимостей:
    pip install llama-cpp-python flask flask-cors
"""

import sys
import os
from pathlib import Path

try:
    from llama_cpp import Llama
except ImportError:
    print("❌ llama-cpp-python не установлена!")
    print("Установите командой:")
    print("  pip install llama-cpp-python")
    sys.exit(1)

try:
    from flask import Flask, request, jsonify
    from flask_cors import CORS
except ImportError:
    print("❌ Flask не установлен!")
    print("Установите командой:")
    print("  pip install flask flask-cors")
    sys.exit(1)


# Конфигурация
MODEL_PATH = "models/qwen2.5-coder/qwen2.5-coder-1.5b-instruct-q8_0.gguf"
HOST = "127.0.0.1"
PORT = 5001
MAX_TOKENS = 512
TEMPERATURE = 0.7

app = Flask(__name__)
CORS(app)  # Разрешить CORS запросы

# Глобальная переменная для модели
llm = None


def load_model():
    """Загружает модель Qwen Coder."""
    global llm
    
    model_file = Path(MODEL_PATH)
    
    if not model_file.exists():
        print(f"❌ Модель не найдена: {MODEL_PATH}")
        print("Сначала запустите: python backend/download_coder.py")
        return False
    
    print(f"🤖 Загрузка модели: {MODEL_PATH}")
    print("Это может занять несколько минут...")
    
    try:
        llm = Llama(
            model_path=str(model_file),
            n_ctx=2048,  # Контекст 2048 токенов
            n_threads=4,  # Количество потоков CPU
            n_gpu_layers=0,  # 0 = только CPU (для ноутбуков без GPU)
            verbose=False,
        )
        print(f"✅ Модель загружена успешно!")
        return True
    except Exception as e:
        print(f"❌ Ошибка загрузки модели: {e}")
        return False


@app.route('/health', methods=['GET'])
def health_check():
    """Проверка доступности API."""
    return jsonify({
        'status': 'ok',
        'model': 'Qwen2.5-Coder-1.5B-Instruct',
        'loaded': llm is not None
    })


@app.route('/chat', methods=['POST'])
def chat():
    """Обработка запроса к чат-боту."""
    if llm is None:
        return jsonify({'error': 'Model not loaded'}), 503
    
    data = request.get_json()
    
    if not data or 'message' not in data:
        return jsonify({'error': 'Message is required'}), 400
    
    message = data['message']
    max_tokens = data.get('max_tokens', MAX_TOKENS)
    temperature = data.get('temperature', TEMPERATURE)
    
    print(f"📥 Запрос: {message}")
    
    try:
        # Формируем промпт для Qwen Coder Instruct
        prompt = f"<|im_start|>system\nТы полезный ассистент CosmoCoder AI, специализирующийся на генерации кода, отладке и рефакторинге.<|im_end|>\n<|im_start|>user\n{message}<|im_end|>\n<|im_start|>assistant\n"
        
        # Генерируем ответ
        output = llm(
            prompt,
            max_tokens=max_tokens,
            temperature=temperature,
            stop=['<|im_end|>', '<|im_start|>'],
            echo=False,
        )
        
        response_text = output['choices'][0]['text'].strip()
        
        print(f"📤 Ответ: {response_text}")
        
        return jsonify({
            'response': response_text,
            'model': 'Qwen2.5-Coder-1.5B-Instruct',
            'tokens_used': output['usage']['total_tokens']
        })
        
    except Exception as e:
        print(f"❌ Ошибка генерации: {e}")
        return jsonify({'error': str(e)}), 500


@app.route('/generate', methods=['POST'])
def generate():
    """Генерация текста (без системного промпта)."""
    if llm is None:
        return jsonify({'error': 'Model not loaded'}), 503
    
    data = request.get_json()
    
    if not data or 'prompt' not in data:
        return jsonify({'error': 'Prompt is required'}), 400
    
    prompt = data['prompt']
    max_tokens = data.get('max_tokens', MAX_TOKENS)
    
    try:
        output = llm(
            prompt,
            max_tokens=max_tokens,
            echo=False,
        )
        
        return jsonify({
            'text': output['choices'][0]['text'],
            'tokens': output['usage']['total_tokens']
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500


if __name__ == '__main__':
    print("=" * 60)
    print("🏆 CosmoCoder AI - Qwen2.5-Coder-1.5B Local API Server")
    print("=" * 60)
    
    # Загрузка модели
    if not load_model():
        sys.exit(1)
    
    # Запуск сервера
    print(f"\n🌐 Запуск сервера на http://{HOST}:{PORT}")
    print("Endpoints:")
    print("  GET  /health  - проверка доступности")
    print("  POST /chat    - запрос к чат-боту")
    print("  POST /generate - генерация текста")
    print("\nНажмите Ctrl+C для остановки")
    print("=" * 60)
    
    # Отключаем dotenv для избежания ошибок на Windows
    os.environ['FLASK_ENV'] = 'production'
    
    app.run(host=HOST, port=PORT, debug=False)