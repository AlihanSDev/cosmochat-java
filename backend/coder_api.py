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
import io
import logging
from pathlib import Path

if os.name == 'nt':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

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


# Настройка логирования
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger("coder_api")

# Конфигурация
ROOT_DIR = Path(__file__).resolve().parent
MODEL_PATH = ROOT_DIR / "models" / "qwen2.5-coder" / "qwen2.5-coder-1.5b-instruct-q2_k.gguf"
HOST = "127.0.0.1"
PORT = 5001
MAX_TOKENS = 512
TEMPERATURE = 0.7

app = Flask(__name__)
CORS(app)  # Разрешить CORS запросы

# Логирование всех входящих запросов
@app.before_request
def log_request():
    logger.info("→ %s %s", request.method, request.path)

# Логирование всех ответов
@app.after_request
def log_response(response):
    logger.info("← %s %s - %d", request.method, request.path, response.status_code)
    return response

# Глобальная переменная для модели
llm = None


def load_model():
    """Загружает модель Qwen Coder."""
    global llm
    
    model_file = Path(MODEL_PATH)
    
    if not model_file.exists():
        logger.error("Модель не найдена: %s", model_file)
        logger.error("Сначала запустите: python backend/download_coder.py")
        return False
    
    logger.info("Загрузка модели: %s", model_file)
    logger.info("Это может занять несколько минут...")
    
    try:
        llm = Llama(
            model_path=str(model_file),
            n_ctx=2048,  # Контекст 2048 токенов
            n_threads=4,  # Количество потоков CPU
            n_gpu_layers=0,  # 0 = только CPU (для ноутбуков без GPU)
            verbose=False,
        )
        logger.info("Модель загружена успешно!")
        return True
    except Exception as e:
        logger.exception("Ошибка загрузки модели")
        return False


@app.route('/health', methods=['GET'])
def health_check():
    """Проверка доступности API."""
    loaded = llm is not None
    logger.info("Health check requested. model_loaded=%s", loaded)
    return jsonify({
        'status': 'ok',
        'model': 'Qwen2.5-Coder-1.5B-Instruct',
        'loaded': loaded
    })


@app.route('/chat', methods=['POST'])
def chat():
    """Обработка запроса к чат-боту."""
    logger.info("📥 Получен запрос на /chat")
    
    if llm is None:
        logger.warning("Модель не загружена, отклоняем запрос")
        return jsonify({'error': 'Model not loaded'}), 503
    
    try:
        data = request.get_json(silent=True)
        if not data or 'message' not in data:
            payload = request.get_data(as_text=True)
            logger.warning("Invalid chat request: missing JSON or message field. Headers=%s Body=%s", dict(request.headers), payload)
            return jsonify({'error': 'Message is required'}), 400

        message = data['message']
        max_tokens = data.get('max_tokens', MAX_TOKENS)
        temperature = data.get('temperature', TEMPERATURE)

        logger.info(" Запрос: %s", message[:100] + "..." if len(message) > 100 else message)
        logger.debug("Parameters: max_tokens=%s, temperature=%s", max_tokens, temperature)

        # Формируем промпт для Qwen Coder Instruct
        prompt = (
            "<|im_start|>system\n"
            "Ты полезный ассистент CosmoCoder AI, специализирующийся на генерации кода, отладке и рефакторинге.<|im_end|>\n"
            "<|im_start|>user\n"
            f"{message}<|im_end|>\n"
            "<|im_start|>assistant\n"
        )
        
        # Генерируем ответ
        output = llm(
            prompt,
            max_tokens=max_tokens,
            temperature=temperature,
            stop=['<|im_end|>', '<|im_start|>'],
            echo=False,
        )
        
        if not output or 'choices' not in output or len(output['choices']) == 0:
            raise ValueError('AI model returned invalid response format')

        response_text = output['choices'][0].get('text', '').strip()
        token_count = output.get('usage', {}).get('total_tokens', 0)
        
        if not response_text:
            raise ValueError('AI model returned empty text')
        
        logger.info("📤 Ответ: %s", response_text[:200] + "..." if len(response_text) > 200 else response_text)
        logger.info("Tokens used: %d", token_count)
        
        return jsonify({
            'response': response_text,
            'model': 'Qwen2.5-Coder-1.5B-Instruct',
            'tokens_used': token_count
        })
        
    except ValueError as ve:
        logger.error("Ошибка валидации ответа модели: %s", ve)
        return jsonify({'error': 'Model returned invalid response: ' + str(ve)}), 500
    except Exception as e:
        logger.exception("Ошибка генерации ответа")
        return jsonify({'error': str(e)}), 500


@app.route('/generate', methods=['POST'])
def generate():
    """Генерация текста (без системного промпта)."""
    logger.info("Получен запрос на /generate")
    
    if llm is None:
        logger.warning("Модель не загружена для /generate")
        return jsonify({'error': 'Model not loaded'}), 503
    
    try:
        data = request.get_json()
        if not data or 'prompt' not in data:
            return jsonify({'error': 'Prompt is required'}), 400
        
        prompt = data['prompt']
        max_tokens = data.get('max_tokens', MAX_TOKENS)
        
        logger.info("Генерация текста: %s", prompt[:100] + "..." if len(prompt) > 100 else prompt)
        
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
        logger.exception("Ошибка в /generate")
        return jsonify({'error': str(e)}), 500


# Глобальный обработчик ошибок - гарантирует, что ВСЕ исключения возвращаются как JSON
@app.errorhandler(500)
@app.errorhandler(Exception)
def handle_exception(e):
    """Обработчик всех необработанных исключений."""
    logger.exception("Необработанное исключение в Flask")
    return jsonify({
        'error': 'Internal server error',
        'message': str(e) if app.debug else 'An unexpected error occurred'
    }), 500


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