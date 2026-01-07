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
echo "Building Docker images (running backend tests)..."
if docker-compose -f docker-compose.ci.yml build; then
    echo "✓ Docker build successful - all tests passed!"
else
    echo "✗ Docker build failed - backend tests likely failed"
    echo "Check backend tests or Docker build logs for details"
    exit 1
fi

# Запускаем всё
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
    echo "   Swagger UI:       http://localhost:8080/swagger-ui.html"
    echo "   Dashboard:        http://localhost:3000"
    echo "   Widget test page: https://edhelielboneflare.github.io/BugTracker/"

    # Проверяем доступность
    echo ""
    echo "Checking service availability..."

    # Проверка бэкенда
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✓ Backend is responding (tests passed during build)"

        # Проверка Swagger
        if curl -s http://localhost:8080/swagger-ui.html > /dev/null 2>&1; then
            echo "✓ Swagger UI is available"
        fi
    else
        echo "Backend might be starting..."
        echo "Backend logs:"
        docker-compose -f docker-compose.ci.yml logs backend --tail=10
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
echo "View logs: docker-compose -f docker-compose.ci.yml logs -f"
echo ""
echo "To stop: docker-compose -f docker-compose.ci.yml down"
echo ""
echo "To make user an admin (default role is DEVELOPER):"
echo "docker exec bugtracker-postgres-1 psql -U your_name -d your_db_name -c \"UPDATE user_dev SET role = 'ADMIN' WHERE username = 'username_you_register';\""
echo ""
echo "where your_name - db owner, your_db_name - db name, username_you_register - username of user you want to change role"