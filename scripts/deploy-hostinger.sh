#!/bin/bash

# Hostinger Deployment Script for Missed Call Auto-Responder
# Usage: ./deploy-hostinger.sh

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "🌐 Deploying to Hostinger Premium Hosting"
echo "=========================================="

# Check if required files exist
if [ ! -f "$PROJECT_ROOT/deployment/.env.hostinger" ]; then
    echo "❌ Hostinger environment file not found: deployment/.env.hostinger"
    echo "Please create the environment file with your Hostinger configuration."
    exit 1
fi

# Create deployment package
echo "📦 Creating deployment package..."
DEPLOY_DIR="$PROJECT_ROOT/hostinger-deploy"
mkdir -p "$DEPLOY_DIR"

# Copy PHP backend files
echo "📁 Copying PHP backend files..."
cp -r "$PROJECT_ROOT/php-backend/"* "$DEPLOY_DIR/"

# Copy environment file
cp "$PROJECT_ROOT/deployment/.env.hostinger" "$DEPLOY_DIR/.env"

# Create Hostinger-specific .htaccess
echo "🔧 Creating Hostinger-optimized .htaccess..."
cat > "$DEPLOY_DIR/public/.htaccess" << 'EOF'
RewriteEngine On

# Handle API routes
RewriteCond %{REQUEST_FILENAME} !-f
RewriteCond %{REQUEST_FILENAME} !-d
RewriteRule ^(.*)$ index.php [QSA,L]

# Security headers for Hostinger
<IfModule mod_headers.c>
    Header always set X-Content-Type-Options nosniff
    Header always set X-Frame-Options DENY
    Header always set X-XSS-Protection "1; mode=block"
    Header always set Referrer-Policy "strict-origin-when-cross-origin"
    Header always set Access-Control-Allow-Origin "*"
    Header always set Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS"
    Header always set Access-Control-Allow-Headers "Content-Type, Authorization, X-Requested-With"
</IfModule>

# Hostinger-specific optimizations
<IfModule mod_deflate.c>
    AddOutputFilterByType DEFLATE text/plain
    AddOutputFilterByType DEFLATE text/html
    AddOutputFilterByType DEFLATE text/xml
    AddOutputFilterByType DEFLATE text/css
    AddOutputFilterByType DEFLATE application/xml
    AddOutputFilterByType DEFLATE application/xhtml+xml
    AddOutputFilterByType DEFLATE application/rss+xml
    AddOutputFilterByType DEFLATE application/javascript
    AddOutputFilterByType DEFLATE application/x-javascript
</IfModule>

# Cache static files
<IfModule mod_expires.c>
    ExpiresActive on
    ExpiresByType text/css "access plus 1 year"
    ExpiresByType application/javascript "access plus 1 year"
    ExpiresByType image/png "access plus 1 year"
    ExpiresByType image/jpg "access plus 1 year"
    ExpiresByType image/jpeg "access plus 1 year"
</IfModule>

# Prevent access to sensitive files
<FilesMatch "\.(env|log|sql|md)$">
    Order allow,deny
    Deny from all
</FilesMatch>

# Hide server information
ServerSignature Off
EOF

# Create PHP configuration for Hostinger
echo "⚙️ Creating PHP configuration..."
cat > "$DEPLOY_DIR/php.ini" << 'EOF'
; Hostinger PHP Configuration for Missed Call Auto-Responder
max_execution_time = 300
memory_limit = 256M
upload_max_filesize = 50M
post_max_size = 50M
max_input_vars = 3000
date.timezone = "Asia/Kolkata"

; Error handling for production
display_errors = Off
log_errors = On
error_log = logs/php_errors.log

; Session configuration
session.cookie_httponly = On
session.cookie_secure = On
session.use_strict_mode = On
EOF

# Create deployment instructions
echo "📋 Creating deployment instructions..."
cat > "$DEPLOY_DIR/HOSTINGER_SETUP.txt" << 'EOF'
HOSTINGER DEPLOYMENT INSTRUCTIONS
=================================

1. UPLOAD FILES:
   - Upload all files from this folder to your Hostinger public_html/api/ directory
   - Use File Manager or FTP client

2. INSTALL COMPOSER DEPENDENCIES:
   Option A (SSH - if available):
   - ssh username@yourdomain.com
   - cd public_html/api
   - composer install --no-dev --optimize-autoloader
   
   Option B (Manual):
   - Run "composer install" locally
   - Upload the "vendor" folder via File Manager

3. DATABASE SETUP:
   - Create MySQL database in Hostinger hPanel
   - Import config/database.sql via phpMyAdmin
   - Update .env file with your database credentials

4. CONFIGURE ENVIRONMENT:
   - Edit .env file with your actual values:
     * Database credentials from Hostinger
     * Fast2SMS API key
     * Your domain URL
     * Generate secure JWT secret

5. SETUP CRON JOB:
   - Go to Advanced > Cron Jobs in hPanel
   - Add: * * * * * /usr/bin/php /home/username/public_html/api/scripts/process_scheduled.php
   - Replace "username" with your actual hosting username

6. ENABLE SSL:
   - Go to Security > SSL in hPanel
   - Enable Free SSL Certificate

7. TEST DEPLOYMENT:
   - Visit: https://yourdomain.com/api/api/v1/health
   - Should return JSON with "success": true

8. UPDATE ANDROID APP:
   - Change API_BASE_URL in build.gradle to: https://yourdomain.com/api/
   - Rebuild and install APK

For support: hello@skwasimakram.com
EOF

# Create logs directory
mkdir -p "$DEPLOY_DIR/logs"
chmod 777 "$DEPLOY_DIR/logs"

# Create ZIP package for easy upload
echo "📦 Creating ZIP package..."
cd "$PROJECT_ROOT"
zip -r "hostinger-missed-call-backend.zip" "hostinger-deploy/" -x "hostinger-deploy/vendor/*"

echo ""
echo "✅ Hostinger deployment package created successfully!"
echo ""
echo "📁 Package Location: $PROJECT_ROOT/hostinger-missed-call-backend.zip"
echo "📁 Files Directory: $PROJECT_ROOT/hostinger-deploy/"
echo ""
echo "📋 Next Steps:"
echo "1. Download the ZIP file: hostinger-missed-call-backend.zip"
echo "2. Extract and upload to your Hostinger public_html/api/ directory"
echo "3. Follow instructions in HOSTINGER_SETUP.txt"
echo "4. Install Composer dependencies"
echo "5. Configure database and environment variables"
echo "6. Setup cron job for message processing"
echo "7. Enable SSL certificate"
echo "8. Test the API endpoints"
echo ""
echo "📚 Detailed Guide: See HOSTINGER_DEPLOYMENT.md"
echo "🆘 Support: hello@skwasimakram.com"
echo ""
echo "🎉 Ready for Hostinger deployment!"