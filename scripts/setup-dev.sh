#!/bin/bash

# Development Environment Setup Script
# Usage: ./setup-dev.sh

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "🛠️  Setting up Missed Call Auto-Responder Development Environment"

# Check if required tools are installed
echo "🔍 Checking required tools..."

# Check PHP
if ! command -v php &> /dev/null; then
    echo "❌ PHP is not installed. Please install PHP 7.4 or higher."
    exit 1
fi

# Check Composer
if ! command -v composer &> /dev/null; then
    echo "❌ Composer is not installed. Please install Composer."
    exit 1
fi

# Check Docker (optional)
if command -v docker &> /dev/null; then
    echo "✅ Docker found"
    DOCKER_AVAILABLE=true
else
    echo "⚠️  Docker not found. You can still run the PHP backend manually."
    DOCKER_AVAILABLE=false
fi

# Setup PHP Backend
echo "📦 Setting up PHP Backend..."
cd "$PROJECT_ROOT/php-backend"

# Install PHP dependencies
if [ -f "composer.json" ]; then
    composer install
    if [ $? -ne 0 ]; then
        echo "❌ Composer install failed. Please check your PHP and Composer installation."
        exit 1
    fi
else
    echo "❌ composer.json not found in php-backend directory"
    exit 1
fi

# Create environment file
if [ ! -f ".env" ]; then
    if [ -f ".env.example" ]; then
        cp ".env.example" ".env"
        echo "📝 Created .env file. Please update it with your configuration."
    else
        echo "❌ .env.example not found. Creating basic .env file..."
        cat > .env << EOF
APP_ENV=development
APP_DEBUG=true
DB_HOST=127.0.0.1
DB_NAME=missed_call
DB_USER=root
DB_PASS=
FAST2SMS_API_KEY=your_api_key_here
JWT_SECRET=your_jwt_secret_here
EOF
    fi
fi

# Create logs directory
mkdir -p logs
chmod 777 logs

# Setup database (if MySQL is available)
if command -v mysql &> /dev/null; then
    echo "🗄️  MySQL found. You can run the database setup:"
    echo "   mysql -u root -p < config/database.sql"
else
    echo "⚠️  MySQL not found. Please install MySQL and run:"
    echo "   mysql -u root -p < config/database.sql"
fi

# Android setup check
echo "🤖 Checking Android development setup..."
if [ -d "$PROJECT_ROOT/android-app" ]; then
    cd "$PROJECT_ROOT/android-app"
    
    # Check if Android SDK is available
    if [ -n "$ANDROID_HOME" ]; then
        echo "✅ Android SDK found at: $ANDROID_HOME"
    else
        echo "⚠️  ANDROID_HOME not set. Please install Android Studio and set ANDROID_HOME."
    fi
    
    # Create gradle wrapper if it doesn't exist
    if [ ! -f "gradlew" ]; then
        echo "📦 Creating Gradle wrapper..."
        gradle wrapper
    fi
    
    chmod +x gradlew
    
else
    echo "❌ Android project directory not found"
fi

# Make scripts executable
echo "🔧 Making scripts executable..."
chmod +x "$PROJECT_ROOT/scripts/"*.sh
chmod +x "$PROJECT_ROOT/php-backend/scripts/"*.php

echo ""
echo "🎉 Development environment setup completed!"
echo ""
echo "📋 Next Steps:"
echo ""
echo "1. Backend Setup:"
echo "   - Update php-backend/.env with your database and API credentials"
echo "   - Run: mysql -u root -p < php-backend/config/database.sql"
echo "   - Start PHP server: cd php-backend && php -S localhost:8000 -t public"
echo ""
echo "2. Android Setup:"
echo "   - Open android-app in Android Studio"
echo "   - Update API_BASE_URL in build.gradle"
echo "   - Build and install APK on device"
echo ""
echo "3. Testing:"
echo "   - Test API: curl http://localhost:8000/api/v1/health"
echo "   - Test Android app with simulated missed calls"
echo ""

if [ "$DOCKER_AVAILABLE" = true ]; then
    echo "4. Docker Alternative:"
    echo "   - Run: cd deployment && docker-compose up -d"
    echo "   - This will start all services automatically"
    echo ""
fi

echo "📚 Documentation: See README.md for detailed instructions"