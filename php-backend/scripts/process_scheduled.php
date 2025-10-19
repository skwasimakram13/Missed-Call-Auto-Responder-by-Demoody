#!/usr/bin/env php
<?php

/**
 * Cron script to process scheduled messages
 * Add to crontab: * * * * * /path/to/php /path/to/process_scheduled.php
 */

// Change to script directory
chdir(__DIR__);

require_once __DIR__ . '/../vendor/autoload.php';

// Check if vendor directory exists
if (!file_exists(__DIR__ . '/../vendor/autoload.php')) {
    echo "[" . date('Y-m-d H:i:s') . "] Error: Composer dependencies not installed\n";
    exit(1);
}

use MissedCall\Controllers\MissedCallController;

echo "[" . date('Y-m-d H:i:s') . "] Starting scheduled message processing...\n";

try {
    $controller = new MissedCallController();
    $result = $controller->sendScheduledMessages();
    
    if ($result['success']) {
        $count = $result['data']['processed_count'];
        echo "[" . date('Y-m-d H:i:s') . "] Processed $count scheduled messages\n";
        
        // Log results for debugging
        foreach ($result['data']['results'] as $messageResult) {
            $status = $messageResult['result']['status'];
            $phone = $messageResult['phone_number'];
            echo "  - $phone: $status\n";
        }
    } else {
        echo "[" . date('Y-m-d H:i:s') . "] Error: " . $result['error'] . "\n";
        exit(1);
    }
    
} catch (Exception $e) {
    echo "[" . date('Y-m-d H:i:s') . "] Exception: " . $e->getMessage() . "\n";
    exit(1);
}

echo "[" . date('Y-m-d H:i:s') . "] Completed scheduled message processing\n";
exit(0);