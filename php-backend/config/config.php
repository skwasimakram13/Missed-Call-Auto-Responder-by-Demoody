<?php

// Check if vendor directory exists and load autoloader
if (file_exists(__DIR__ . '/../vendor/autoload.php')) {
    require_once __DIR__ . '/../vendor/autoload.php';
}

// Function to load environment variables manually
function loadEnvFile($filePath)
{
    if (!file_exists($filePath)) {
        return;
    }

    $lines = file($filePath, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
    foreach ($lines as $line) {
        $line = trim($line);
        if (empty($line) || strpos($line, '#') === 0) {
            continue;
        }

        if (strpos($line, '=') === false) {
            continue;
        }

        list($name, $value) = explode('=', $line, 2);
        $name = trim($name);
        $value = trim($value, '"\'');

        if (!array_key_exists($name, $_ENV)) {
            $_ENV[$name] = $value;
        }
    }
}

// Load environment variables
$envPath = __DIR__ . '/../.env';
if (class_exists('Dotenv\\Dotenv') && file_exists($envPath)) {
    try {
        $dotenvClass = 'Dotenv\\Dotenv';
        $dotenv = $dotenvClass::createImmutable(__DIR__ . '/../');
        $dotenv->load();
    } catch (Exception $e) {
        error_log('Dotenv error: ' . $e->getMessage());
        loadEnvFile($envPath);
    }
} else {
    loadEnvFile($envPath);
}

return [
    'app' => [
        'env' => $_ENV['APP_ENV'] ?? 'development',
        'debug' => filter_var($_ENV['APP_DEBUG'] ?? false, FILTER_VALIDATE_BOOLEAN),
        'url' => $_ENV['APP_URL'] ?? 'http://localhost',
    ],

    'database' => [
        'host' => $_ENV['DB_HOST'] ?? '127.0.0.1',
        'port' => (int)($_ENV['DB_PORT'] ?? 3306),
        'name' => $_ENV['DB_NAME'] ?? 'missed_call',
        'user' => $_ENV['DB_USER'] ?? 'root',
        'pass' => $_ENV['DB_PASS'] ?? '',
        'charset' => 'utf8mb4',
        'options' => [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false,
        ]
    ],

    'fast2sms' => [
        'api_key' => $_ENV['FAST2SMS_API_KEY'] ?? '',
        'sender_id' => $_ENV['FAST2SMS_SENDER_ID'] ?? 'TXTIND',
        'base_url' => 'https://www.fast2sms.com/dev/bulkV2'
    ],

    'jwt' => [
        'secret' => $_ENV['JWT_SECRET'] ?? 'default_secret_change_in_production',
        'expiry' => (int)($_ENV['JWT_EXPIRY'] ?? 86400)
    ],

    'rate_limit' => [
        'per_device' => (int)($_ENV['RATE_LIMIT_PER_DEVICE'] ?? 100),
        'per_phone' => (int)($_ENV['RATE_LIMIT_PER_PHONE'] ?? 5)
    ],

    'business' => [
        'default_delay_minutes' => (int)($_ENV['DEFAULT_DELAY_MINUTES'] ?? 5),
        'max_retry_attempts' => (int)($_ENV['MAX_RETRY_ATTEMPTS'] ?? 3),
        'hours_start' => $_ENV['BUSINESS_HOURS_START'] ?? '09:00',
        'hours_end' => $_ENV['BUSINESS_HOURS_END'] ?? '18:00'
    ]
];
