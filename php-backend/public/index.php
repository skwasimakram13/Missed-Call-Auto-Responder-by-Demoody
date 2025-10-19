<?php

require_once __DIR__ . '/../vendor/autoload.php';

use MissedCall\Controllers\MissedCallController;

// Set headers
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Simple routing
$requestUri = $_SERVER['REQUEST_URI'];
$requestMethod = $_SERVER['REQUEST_METHOD'];

// Remove query string and leading slash
$path = strtok($requestUri, '?');
$path = trim($path, '/');

// Initialize controller
$controller = new MissedCallController();

try {
    // Route requests
    switch ($path) {
        case 'api/v1/missed_calls':
            if ($requestMethod === 'POST') {
                $response = $controller->logMissedCall();
            } else {
                $response = ['success' => false, 'error' => 'Method not allowed'];
                http_response_code(405);
            }
            break;

        case 'api/v1/send_scheduled':
            if ($requestMethod === 'POST') {
                $response = $controller->sendScheduledMessages();
            } else {
                $response = ['success' => false, 'error' => 'Method not allowed'];
                http_response_code(405);
            }
            break;

        case 'api/v1/register_device':
            if ($requestMethod === 'POST') {
                $response = $controller->registerDevice();
            } else {
                $response = ['success' => false, 'error' => 'Method not allowed'];
                http_response_code(405);
            }
            break;

        case 'api/v1/logs':
            if ($requestMethod === 'GET') {
                $response = $controller->getLogs();
            } else {
                $response = ['success' => false, 'error' => 'Method not allowed'];
                http_response_code(405);
            }
            break;

        case 'api/v1/opt_out':
            if ($requestMethod === 'POST') {
                $response = $controller->handleOptOut();
            } else {
                $response = ['success' => false, 'error' => 'Method not allowed'];
                http_response_code(405);
            }
            break;

        case 'api/v1/health':
            $response = [
                'success' => true,
                'data' => [
                    'status' => 'healthy',
                    'version' => '1.0.0',
                    'timestamp' => date('c')
                ]
            ];
            break;

        case '':
        case 'index.php':
            $response = [
                'success' => true,
                'data' => [
                    'message' => 'Missed Call Auto-Responder API',
                    'version' => '1.0.0',
                    'endpoints' => [
                        'POST /api/v1/missed_calls' => 'Log a missed call',
                        'POST /api/v1/send_scheduled' => 'Process scheduled messages',
                        'POST /api/v1/register_device' => 'Register a device',
                        'GET /api/v1/logs' => 'Get call logs',
                        'POST /api/v1/opt_out' => 'Opt out a phone number',
                        'GET /api/v1/health' => 'Health check'
                    ]
                ]
            ];
            break;

        default:
            $response = ['success' => false, 'error' => 'Endpoint not found'];
            http_response_code(404);
            break;
    }

} catch (Exception $e) {
    error_log('API Error: ' . $e->getMessage());
    $response = [
        'success' => false,
        'error' => 'Internal server error',
        'timestamp' => date('c')
    ];
    http_response_code(500);
}

// Output response
echo json_encode($response, JSON_PRETTY_PRINT);
?>