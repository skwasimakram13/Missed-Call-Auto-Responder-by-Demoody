# Installation Guide

This guide will help you install and configure the Missed Call Auto-Responder system.

## Prerequisites

### Backend Requirements
- PHP 7.4 or higher
- MySQL 5.7 or higher
- Composer
- Apache/Nginx web server
- SSL certificate (for production)

### Android Requirements
- Android Studio
- Android SDK (API level 21+)
- Java 8 or higher

### Optional
- Docker & Docker Compose (for containerized deployment)

## Quick Start with Docker

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd missed-call-auto-responder
   ```

2. **Setup environment**
   ```bash
   cp deployment/.env.production deployment/.env
   # Edit deployment/.env with your configuration
   ```

3. **Deploy with Docker**
   ```bash
   chmod +x scripts/deploy.sh
   ./scripts/deploy.sh production
   ```

4. **Build Android APK**
   ```bash
   chmod +x scripts/build-android.sh
   ./scripts/build-android.sh release
   ```

## Manual Installation

### Backend Setup

1. **Install PHP dependencies**
   ```bash
   cd php-backend
   composer install
   ```

2. **Configure environment**
   ```bash
   cp .env.example .env
   # Edit .env with your settings
   ```

3. **Setup database**
   ```bash
   mysql -u root -p < config/database.sql
   ```

4. **Configure web server**
   - Point document root to `php-backend/public`
   - Enable mod_rewrite (Apache) or configure URL rewriting (Nginx)
   - Setup SSL certificate

5. **Setup cron job**
   ```bash
   # Add to crontab
   * * * * * cd /path/to/php-backend && php scripts/process_scheduled.php
   ```

### Android Setup

1. **Open in Android Studio**
   ```bash
   # Open android-app directory in Android Studio
   ```

2. **Configure API endpoint**
   - Update `API_BASE_URL` in `app/build.gradle`
   - Set your backend server URL

3. **Build APK**
   ```bash
   cd android-app
   ./gradlew assembleRelease
   ```

4. **Install on device**
   - Enable "Unknown sources" in Android settings
   - Install the APK file
   - Grant required permissions

## Configuration

### Fast2SMS Setup

1. **Get API Key**
   - Sign up at [Fast2SMS](https://www.fast2sms.com/)
   - Get your API key from dashboard

2. **Configure in backend**
   ```bash
   # In php-backend/.env
   FAST2SMS_API_KEY=your_api_key_here
   FAST2SMS_SENDER_ID=TXTIND
   ```

### Android App Configuration

1. **First Launch**
   - Grant phone and call log permissions
   - Configure message template
   - Set delay time
   - Enable auto-responder

2. **Backend Connection**
   - Enter your server URL in settings
   - App will register automatically

## Testing

### Backend API Test
```bash
# Health check
curl http://your-domain.com/api/v1/health

# Register device
curl -X POST http://your-domain.com/api/v1/register_device \
  -H "Content-Type: application/json" \
  -d '{"device_id":"test123","device_name":"Test Device"}'
```

### Android App Test
1. Enable auto-responder
2. Use "Test Message" button
3. Check logs in backend

## Troubleshooting

### Common Issues

1. **Permissions Denied**
   - Ensure all required permissions are granted
   - Check battery optimization settings

2. **Messages Not Sending**
   - Verify Fast2SMS API key
   - Check cron job is running
   - Review backend logs

3. **App Not Detecting Calls**
   - Verify phone state permission
   - Check if service is running
   - Test on different devices

### Logs

- **Backend logs**: `php-backend/logs/`
- **Android logs**: Use `adb logcat` or Android Studio
- **Docker logs**: `docker-compose logs -f`

## Production Deployment

### Security Checklist

- [ ] SSL certificate installed
- [ ] Database credentials secured
- [ ] API keys in environment variables
- [ ] Rate limiting configured
- [ ] Firewall rules applied
- [ ] Regular backups scheduled

### Monitoring

- Setup log monitoring (ELK stack, Papertrail)
- Configure alerts for failures
- Monitor API response times
- Track message delivery rates

### Scaling

- Use load balancer for multiple backend instances
- Consider Redis for session storage
- Implement database read replicas
- Use CDN for static assets

## Support

For issues and questions:
- Check the FAQ in README.md
- Review logs for error messages
- Create an issue in the repository
- Contact: hello@skwasimakram.com