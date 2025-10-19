# Hostinger Premium Hosting Deployment Guide

This guide will help you deploy the Missed Call Auto-Responder PHP backend on **Hostinger Premium Hosting**.

## ✅ **Hostinger Compatibility**

Our PHP backend is **100% compatible** with Hostinger Premium Hosting:
- ✅ PHP 7.4+ (Hostinger supports PHP 8.1+)
- ✅ MySQL databases
- ✅ Composer support
- ✅ SSL certificates (Let's Encrypt)
- ✅ Cron jobs
- ✅ File manager access
- ✅ Custom domains

## 🚀 **Step-by-Step Deployment**

### **Step 1: Prepare Your Hostinger Account**

1. **Login to Hostinger hPanel**
   - Go to [hpanel.hostinger.com](https://hpanel.hostinger.com)
   - Login with your credentials

2. **Create MySQL Database**
   - Go to **Databases** → **MySQL Databases**
   - Click **Create Database**
   - Database name: `u123456789_missed_call` (Hostinger format)
   - Username: `u123456789_missed_user`
   - Password: Generate a strong password
   - **Save these credentials!**

### **Step 2: Upload Backend Files**

1. **Using File Manager (Recommended)**
   - Go to **Files** → **File Manager**
   - Navigate to `public_html` folder
   - Create folder: `api` (your API will be at yourdomain.com/api/)
   - Upload all files from `php-backend` folder to `public_html/api/`

2. **Using FTP (Alternative)**
   - Use FileZilla or similar FTP client
   - Host: your-domain.com
   - Username: Your hosting username
   - Password: Your hosting password
   - Upload to `/public_html/api/`

### **Step 3: Install Composer Dependencies**

1. **Enable SSH Access** (if available on your plan)
   - Go to **Advanced** → **SSH Access**
   - Enable SSH and note the connection details

2. **Install Dependencies via SSH**
   ```bash
   ssh username@your-domain.com
   cd public_html/api
   composer install --no-dev --optimize-autoloader
   ```

3. **Alternative: Manual Upload**
   - Run `composer install` locally
   - Upload the entire `vendor` folder via File Manager

### **Step 4: Configure Environment**

1. **Create .env file**
   - In File Manager, go to `public_html/api/`
   - Create new file: `.env`
   - Copy content from `.env.example` and update:

```env
# Hostinger Production Configuration
APP_ENV=production
APP_DEBUG=false
APP_URL=https://yourdomain.com

# Hostinger Database (update with your actual values)
DB_HOST=localhost
DB_PORT=3306
DB_NAME=u123456789_missed_call
DB_USER=u123456789_missed_user
DB_PASS=your_database_password

# Fast2SMS Configuration
FAST2SMS_API_KEY=your_fast2sms_api_key_here
FAST2SMS_SENDER_ID=TXTIND

# Security
JWT_SECRET=your_very_long_random_secret_key_here
JWT_EXPIRY=86400

# Rate Limiting
RATE_LIMIT_PER_DEVICE=100
RATE_LIMIT_PER_PHONE=5

# Business Configuration
DEFAULT_DELAY_MINUTES=5
MAX_RETRY_ATTEMPTS=3
BUSINESS_HOURS_START=09:00
BUSINESS_HOURS_END=18:00
```

### **Step 5: Setup Database**

1. **Access phpMyAdmin**
   - Go to **Databases** → **phpMyAdmin**
   - Select your database
   - Click **Import**
   - Upload `config/database.sql`
   - Click **Go**

2. **Verify Tables Created**
   - Check that these tables exist:
     - `devices`
     - `missed_calls`
     - `blocked_numbers`
     - `message_templates`
     - `rate_limits`

### **Step 6: Configure Web Server**

1. **Create .htaccess file**
   - In `public_html/api/public/` create `.htaccess`:

```apache
RewriteEngine On

# Handle Angular and other front-end framework routes
RewriteCond %{REQUEST_FILENAME} !-f
RewriteCond %{REQUEST_FILENAME} !-d

# Redirect everything to index.php
RewriteRule ^(.*)$ index.php [QSA,L]

# Security headers
<IfModule mod_headers.c>
    Header always set X-Content-Type-Options nosniff
    Header always set X-Frame-Options DENY
    Header always set X-XSS-Protection "1; mode=block"
    Header always set Strict-Transport-Security "max-age=63072000; includeSubDomains; preload"
    Header always set Referrer-Policy "strict-origin-when-cross-origin"
    Header always set Access-Control-Allow-Origin "*"
    Header always set Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS"
    Header always set Access-Control-Allow-Headers "Content-Type, Authorization, X-Requested-With"
</IfModule>

# Disable server signature
ServerSignature Off

# Hide PHP version
<IfModule mod_headers.c>
    Header unset X-Powered-By
</IfModule>

# Prevent access to sensitive files
<FilesMatch "\.(env|log|sql)$">
    Order allow,deny
    Deny from all
</FilesMatch>
```

### **Step 7: Setup Cron Jobs**

1. **Access Cron Jobs**
   - Go to **Advanced** → **Cron Jobs**
   - Click **Create Cron Job**

2. **Add Message Processing Cron**
   - **Frequency**: Every minute (`* * * * *`)
   - **Command**: 
   ```bash
   /usr/bin/php /home/username/public_html/api/scripts/process_scheduled.php
   ```
   - Replace `username` with your actual hosting username

### **Step 8: SSL Certificate**

1. **Enable SSL**
   - Go to **Security** → **SSL**
   - Enable **Free SSL Certificate**
   - Wait for activation (usually 5-15 minutes)

2. **Force HTTPS**
   - Add to your main `.htaccess` in `public_html`:
   ```apache
   RewriteEngine On
   RewriteCond %{HTTPS} off
   RewriteRule ^(.*)$ https://%{HTTP_HOST}%{REQUEST_URI} [L,R=301]
   ```

### **Step 9: Test Your Deployment**

1. **API Health Check**
   ```bash
   curl https://yourdomain.com/api/api/v1/health
   ```

2. **Expected Response**
   ```json
   {
     "success": true,
     "data": {
       "status": "healthy",
       "version": "1.0.0",
       "timestamp": "2025-01-19T12:00:00Z"
     }
   }
   ```

## 📱 **Android App Configuration**

Update your Android app to use the Hostinger backend:

1. **Update API URL in Android**
   - Edit `android-app/app/build.gradle`
   - Change `API_BASE_URL` to your domain:
   ```gradle
   buildConfigField "String", "API_BASE_URL", "\"https://yourdomain.com/api/\""
   ```

2. **Rebuild APK**
   ```bash
   cd android-app
   ./gradlew assembleRelease
   ```

## 🔧 **Hostinger-Specific Optimizations**

### **PHP Configuration**
Create `php.ini` in your root directory:
```ini
max_execution_time = 300
memory_limit = 256M
upload_max_filesize = 50M
post_max_size = 50M
max_input_vars = 3000
```

### **Database Optimization**
```sql
-- Add indexes for better performance
ALTER TABLE missed_calls ADD INDEX idx_device_status (device_id, status);
ALTER TABLE missed_calls ADD INDEX idx_scheduled_time (scheduled_time);
```

### **Caching (Optional)**
If your plan supports it, enable:
- **LiteSpeed Cache**
- **Cloudflare** (free tier available)

## 🛡️ **Security Best Practices**

1. **Protect Sensitive Files**
   ```apache
   # Add to .htaccess
   <Files ".env">
       Order allow,deny
       Deny from all
   </Files>
   ```

2. **Database Security**
   - Use strong passwords
   - Limit database user permissions
   - Regular backups

3. **API Security**
   - Rate limiting is built-in
   - JWT tokens for authentication
   - Input validation implemented

## 📊 **Monitoring & Maintenance**

### **Log Files**
- Check `logs/` directory for errors
- Monitor via Hostinger's error logs

### **Database Backups**
- Use Hostinger's automatic backups
- Or setup manual backups via cron:
```bash
mysqldump -u username -p database_name > backup_$(date +%Y%m%d).sql
```

### **Performance Monitoring**
- Monitor API response times
- Check database query performance
- Monitor disk space usage

## 💰 **Cost Estimation**

**Hostinger Premium Plan** (~$2.99/month):
- ✅ 100GB SSD Storage
- ✅ Unlimited Bandwidth  
- ✅ MySQL Databases
- ✅ SSL Certificate
- ✅ Cron Jobs
- ✅ PHP 8.1+

**Additional Costs**:
- Fast2SMS: ~₹0.08-0.50 per SMS
- Domain (if not included): ~$10/year

## 🆘 **Troubleshooting**

### **Common Issues**

1. **500 Internal Server Error**
   - Check file permissions (755 for folders, 644 for files)
   - Verify .htaccess syntax
   - Check error logs

2. **Database Connection Failed**
   - Verify database credentials in .env
   - Check if database exists
   - Ensure database user has proper permissions

3. **Composer Dependencies Missing**
   - Upload vendor folder manually
   - Or run composer install via SSH

4. **Cron Job Not Working**
   - Check cron job syntax
   - Verify PHP path: `/usr/bin/php`
   - Check script permissions

### **Support Resources**
- **Hostinger Support**: 24/7 live chat
- **Documentation**: [hostinger.com/tutorials](https://hostinger.com/tutorials)
- **Community**: Hostinger Community Forum

## ✅ **Deployment Checklist**

- [ ] MySQL database created
- [ ] Files uploaded to public_html/api/
- [ ] Composer dependencies installed
- [ ] .env file configured
- [ ] Database tables created
- [ ] .htaccess configured
- [ ] Cron job setup
- [ ] SSL certificate enabled
- [ ] API health check passes
- [ ] Android app updated with new URL
- [ ] Test missed call functionality

---

**🎉 Your Missed Call Auto-Responder is now live on Hostinger!**

**API Endpoint**: `https://yourdomain.com/api/api/v1/`

For support: hello@skwasimakram.com