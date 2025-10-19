#!/bin/bash

# Missed Call Auto-Responder Deployment Script
# Usage: ./deploy.sh [environment]

set -e

ENVIRONMENT=${1:-production}
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "🚀 Deploying Missed Call Auto-Responder - Environment: $ENVIRONMENT"

# Check if required files exist
if [ ! -f "$PROJECT_ROOT/deployment/.env.$ENVIRONMENT" ]; then
    echo "❌ Environment file not found: deployment/.env.$ENVIRONMENT"
    echo "Please create the environment file with your configuration."
    exit 1
fi

# Copy environment file
cp "$PROJECT_ROOT/deployment/.env.$ENVIRONMENT" "$PROJECT_ROOT/deployment/.env"

# Navigate to deployment directory
cd "$PROJECT_ROOT/deployment"

echo "📦 Building and starting containers..."

# Stop existing containers
docker-compose down

# Build and start containers
docker-compose up -d --build

echo "⏳ Waiting for services to start..."
sleep 30

# Check if services are running
if docker-compose ps | grep -q "Up"; then
    echo "✅ Services are running!"
    
    # Show service status
    docker-compose ps
    
    echo ""
    echo "🌐 API Health Check:"
    curl -f http://localhost/api/v1/health || echo "❌ Health check failed"
    
    echo ""
    echo "📋 Deployment Summary:"
    echo "- Backend API: http://localhost"
    echo "- Database: localhost:3306"
    echo "- Logs: docker-compose logs -f"
    
    echo ""
    echo "🔧 Next Steps:"
    echo "1. Configure your Fast2SMS API key in the environment file"
    echo "2. Test the API endpoints"
    echo "3. Deploy the Android APK to your device"
    echo "4. Configure SSL certificate for production"
    
else
    echo "❌ Some services failed to start. Check logs:"
    docker-compose logs
    exit 1
fi

echo "🎉 Deployment completed successfully!"