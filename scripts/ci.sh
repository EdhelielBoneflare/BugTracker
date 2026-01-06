#!/bin/bash
set -e

echo "Starting BugTracker CI Pipeline..."
echo "Project root: $(pwd)"

# Проверяем package-lock.json для widget
if [ ! -f "frontend/widget/package-lock.json" ]; then
    echo "No package-lock.json in widget folder"
    echo "Generating package-lock.json..."
    cd frontend/widget
    npm install --package-lock-only
    cd ../..
fi

# Проверяем package-lock.json для dashboard
if [ ! -f "frontend/dashboard/package-lock.json" ]; then
    echo "No package-lock.json in dashboard folder"
    echo "Generating package-lock.json..."
    cd frontend/dashboard
    npm install --package-lock-only
    cd ../..
fi

# Проверяем .env файл
if [ -f ".env" ]; then
    echo "Found .env file"
else
    echo ".env file not found in project root!"
    echo "Creating .env file from .env.example..."
    if [ -f ".env.example" ]; then
        cp .env.example .env
        echo "Please update the .env file with your configuration"
    fi
fi

# Проверяем Docker
if ! command -v docker &> /dev/null; then
    echo "Docker is not installed"
    exit 1
fi

echo "Docker: $(docker --version)"

# Собираем виджет как статический файл
echo ""
echo "Building widget as static JS file..."
if [ -d "frontend/widget" ]; then
    cd frontend/widget

    # Устанавливаем зависимости если нет node_modules
    if [ ! -d "node_modules" ]; then
        echo "Installing widget dependencies..."
        npm ci --silent || npm install --silent
    fi

    # Собираем виджет
    echo "Building widget..."
    if npm run build 2>/dev/null || npm run build:prod 2>/dev/null; then
        echo "Widget built successfully!"

        # Проверяем собранный файл
        if [ -f "dist/bugtracker.js" ] || [ -f "dist/main.js" ] || [ -f "dist/widget.js" ]; then
            echo "Widget JS file created"
            ls -la dist/

            # Копируем в docs для тестовой страницы
            if [ -d "../../docs" ]; then
                echo "Copying widget to docs folder for testing..."
                cp dist/*.js ../../docs/ 2>/dev/null || true
            fi
        fi
    else
        echo "Warning: Widget build might have failed or no build script found"
        echo "Continuing anyway..."
    fi

    cd ../..
else
    echo "Warning: Widget folder not found"
fi

# Очищаем предыдущие контейнеры
echo ""
echo "Cleaning up previous containers..."
docker-compose -f docker-compose.ci.yml down --remove-orphans 2>/dev/null || true

# Собираем образы
echo "Building Docker images..."
docker-compose -f docker-compose.ci.yml build

# Запускаем всё (без виджета как сервиса)
echo "Starting services..."
docker-compose -f docker-compose.ci.yml up -d

echo ""
echo "Waiting for services to start (20 seconds)..."
sleep 20

# Проверяем статус
echo ""
echo "Services status:"
echo "==================="

if docker-compose -f docker-compose.ci.yml ps | grep -q "Up"; then
    echo "All services are running!"

    # Показываем детальный статус
    echo ""
    echo "Detailed status:"
    docker-compose -f docker-compose.ci.yml ps

    echo ""
    echo "Access URLs:"
    echo "   Backend API:     http://localhost:8080"
    echo "   Swagger UI:      http://localhost:8080/swagger-ui.html"
    echo "   Dashboard:       http://localhost:3000"
    echo "   Widget JS (local): file://$(pwd)/frontend/widget/dist/bugtracker.js"

    # Проверяем доступность
    echo ""
    echo "Checking service availability..."

    # Проверка бэкенда
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✓ Backend is responding"

        # Проверка Swagger
        if curl -s http://localhost:8080/swagger-ui.html > /dev/null 2>&1; then
            echo "✓ Swagger UI is available"
        fi
    else
        echo "Backend might be starting..."
    fi

    # Проверка dashboard
    if curl -s http://localhost:3000 > /dev/null 2>&1; then
        echo "✓ Dashboard is responding"
    fi

else
    echo "Some services failed to start"
    docker-compose -f docker-compose.ci.yml ps
    echo ""
    echo "Check logs:"
    docker-compose -f docker-compose.ci.yml logs --tail=20
fi

echo ""
echo "To use the widget:"
echo "1. Include this in your HTML:"
echo "   <script src=\"http://localhost:8080/static/bugtracker.js\"></script>"
echo "   (You'll need to serve the widget file through backend static resources)"
echo ""
echo "2. Or use the local file:"
echo "   <script src=\"file://$(pwd)/frontend/widget/dist/bugtracker.js\"></script>"
echo ""
echo "View logs:"
echo "   docker-compose -f docker-compose.ci.yml logs -f"
echo ""
echo "To stop: docker-compose -f docker-compose.ci.yml down"
echo ""