.PHONY: help build up down logs clean shell qwen-shell coder-shell test-qwen test-coder

help:
	@echo "CosmoChat Python Backend - Docker Commands"
	@echo ""
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@echo "  build            Build Docker images"
	@echo "  up               Start all services"
	@echo "  down             Stop all services"
	@echo "  restart          Restart all services"
	@echo "  logs             Show logs from all services"
	@echo "  logs-qwen        Show logs from Qwen API"
	@echo "  logs-coder       Show logs from Coder API"
	@echo "  shell-qwen       Open shell in Qwen API container"
	@echo "  shell-coder      Open shell in Coder API container"
	@echo "  test-qwen        Test Qwen API health endpoint"
	@echo "  test-coder       Test Coder API health endpoint"
	@echo "  clean            Remove containers, networks, and volumes"
	@echo "  clean-models     Remove downloaded models (WARNING: deletes all models)"
	@echo "  download-models  Download all models (requires running container)"
	@echo "  prune            System cleanup (dangling images, stopped containers)"

build:
	@echo "Building Docker images..."
	docker-compose build

up:
	@echo "Starting services..."
	docker-compose up -d
	@echo ""
	@echo "Services starting..."
	@echo "  Qwen API:    http://localhost:5000"
	@echo "  Coder API:   http://localhost:5001"
	@echo ""
	@echo "Check logs: make logs"

down:
	@echo "Stopping services..."
	docker-compose down

restart:
	@echo "Restarting services..."
	docker-compose restart

logs:
	docker-compose logs -f

logs-qwen:
	docker-compose logs -f qwen-api

logs-coder:
	docker-compose logs -f coder-api

shell-qwen:
	docker-compose exec qwen-api bash

shell-coder:
	docker-compose exec coder-api bash

test-qwen:
	@echo "Testing Qwen API..."
	curl -s http://localhost:5000/health | python -m json.tool

test-coder:
	@echo "Testing Coder API..."
	curl -s http://localhost:5001/health | python -m json.tool

clean:
	@echo "Removing containers, networks, and volumes..."
	docker-compose down -v
	@echo "Done."

clean-models:
	@echo "WARNING: This will delete all downloaded models!"
	@read -p "Are you sure? (y/N): " confirm; \
	if [ "$$confirm" = "y" ] || [ "$$confirm" = "Y" ]; then \
		docker volume rm cosmochat_models; \
		echo "Models volume removed."; \
	else \
		echo "Aborted."; \
	fi

download-models:
	@echo "Starting temporary container to download models..."
	docker-compose run --rm qwen-api sh -c "python download_instruct.py && python download_coder.py"
	@echo "Models downloaded to ./backend/models/"

prune:
	@echo "Cleaning up Docker system..."
	docker system prune -a -f
