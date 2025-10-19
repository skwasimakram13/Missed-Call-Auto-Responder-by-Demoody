<?php

namespace MissedCall\Controllers;

use MissedCall\Models\MissedCall;
use MissedCall\Models\Device;
use MissedCall\Models\BlockedNumber;
use MissedCall\Services\SMSService;
use MissedCall\Services\RateLimitService;

class MissedCallController
{
    private $missedCallModel;
    private $deviceModel;
    private $blockedNumberModel;
    private $smsService;
    private $rateLimitService;
    private $config;

    public function __construct()
    {
        $this->missedCallModel = new MissedCall();
        $this->deviceModel = new Device();
        $this->blockedNumberModel = new BlockedNumber();
        $this->smsService = new SMSService();
        $this->rateLimitService = new RateLimitService();
        $this->config = require __DIR__ . '/../../config/config.php';
    }

    public function logMissedCall(): array
    {
        try {
            $input = $this->getJsonInput();
            
            // Validate required fields
            $required = ['device_id', 'phone_number', 'call_time'];
            foreach ($required as $field) {
                if (empty($input[$field])) {
                    return $this->errorResponse("Missing required field: $field", 400);
                }
            }

            // Authenticate device
            $device = $this->deviceModel->findByDeviceId($input['device_id']);
            if (!$device) {
                return $this->errorResponse('Device not registered', 401);
            }

            // Check rate limits
            if (!$this->rateLimitService->checkDeviceLimit($input['device_id'])) {
                return $this->errorResponse('Device rate limit exceeded', 429);
            }

            if (!$this->rateLimitService->checkPhoneLimit($input['phone_number'])) {
                return $this->errorResponse('Phone number rate limit exceeded', 429);
            }

            // Check if number is blocked
            if ($this->blockedNumberModel->isBlocked($input['phone_number'])) {
                return $this->successResponse([
                    'message' => 'Number is blocked, message not scheduled',
                    'status' => 'BLOCKED'
                ]);
            }

            // Validate phone number
            if (!$this->smsService->validatePhoneNumber($input['phone_number'])) {
                return $this->errorResponse('Invalid phone number format', 400);
            }

            // Check for duplicate within time window
            $existing = $this->missedCallModel->findByDeviceAndPhone(
                $input['device_id'], 
                $input['phone_number'], 
                3600 // 1 hour window
            );

            if ($existing && $existing['status'] !== 'FAILED') {
                return $this->successResponse([
                    'message' => 'Duplicate call detected, message not scheduled',
                    'existing_id' => $existing['id'],
                    'status' => $existing['status']
                ]);
            }

            // Calculate scheduled time
            $delayMinutes = $input['delay_minutes'] ?? $this->config['business']['default_delay_minutes'];
            $callTimestamp = is_numeric($input['call_time']) ? $input['call_time'] / 1000 : strtotime($input['call_time']);
            $scheduledTime = date('Y-m-d H:i:s', $callTimestamp + ($delayMinutes * 60));

            // Get message template
            $messageText = $input['message_text'] ?? $this->getDefaultMessage();

            // Create missed call record
            $callData = [
                'device_id' => $input['device_id'],
                'phone_number' => $input['phone_number'],
                'call_time' => date('Y-m-d H:i:s', is_numeric($input['call_time']) ? $input['call_time'] / 1000 : strtotime($input['call_time'])),
                'scheduled_time' => $scheduledTime,
                'message_text' => $messageText
            ];

            $callId = $this->missedCallModel->create($callData);
            
            if (!$callId) {
                return $this->errorResponse('Failed to create missed call record', 500);
            }

            // Update device activity
            $this->deviceModel->updateLastActivity($input['device_id']);

            return $this->successResponse([
                'message' => 'Missed call logged successfully',
                'call_id' => $callId,
                'scheduled_time' => $scheduledTime,
                'delay_minutes' => $delayMinutes
            ]);

        } catch (\Exception $e) {
            error_log('Error in logMissedCall: ' . $e->getMessage());
            return $this->errorResponse('Internal server error', 500);
        }
    }

    public function sendScheduledMessages(): array
    {
        try {
            $pendingCalls = $this->missedCallModel->getPendingCalls(50);
            $results = [];

            foreach ($pendingCalls as $call) {
                $result = $this->processSingleCall($call);
                $results[] = [
                    'call_id' => $call['id'],
                    'phone_number' => $call['phone_number'],
                    'result' => $result
                ];
            }

            return $this->successResponse([
                'message' => 'Processed scheduled messages',
                'processed_count' => count($results),
                'results' => $results
            ]);

        } catch (\Exception $e) {
            error_log('Error in sendScheduledMessages: ' . $e->getMessage());
            return $this->errorResponse('Internal server error', 500);
        }
    }

    private function processSingleCall(array $call): array
    {
        // Check if number is blocked
        if ($this->blockedNumberModel->isBlocked($call['phone_number'])) {
            $this->missedCallModel->updateStatus($call['id'], 'BLOCKED');
            return ['status' => 'BLOCKED', 'message' => 'Number is blocked'];
        }

        // Check business hours if configured
        if (!$this->isWithinBusinessHours()) {
            // Reschedule for next business hour
            $nextBusinessTime = $this->getNextBusinessTime();
            $this->missedCallModel->updateStatus($call['id'], 'PENDING', [
                'scheduled_time' => $nextBusinessTime
            ]);
            return ['status' => 'RESCHEDULED', 'message' => 'Outside business hours'];
        }

        // Send SMS
        $smsResult = $this->smsService->sendSMS($call['phone_number'], $call['message_text']);

        if ($smsResult['success']) {
            $this->missedCallModel->updateStatus($call['id'], 'SENT', [
                'provider_msg_id' => $smsResult['message_id'] ?? null
            ]);
            return ['status' => 'SENT', 'message_id' => $smsResult['message_id'] ?? null];
        } else {
            $attemptCount = ($call['attempt_count'] ?? 0) + 1;
            $maxAttempts = $this->config['business']['max_retry_attempts'];

            if ($attemptCount >= $maxAttempts) {
                $this->missedCallModel->updateStatus($call['id'], 'FAILED', [
                    'error_message' => $smsResult['error'],
                    'attempt_count' => $attemptCount
                ]);
                return ['status' => 'FAILED', 'error' => $smsResult['error']];
            } else {
                // Reschedule with exponential backoff
                $backoffMinutes = pow(2, $attemptCount) * 5; // 10, 20, 40 minutes
                $nextAttempt = date('Y-m-d H:i:s', time() + ($backoffMinutes * 60));
                
                $this->missedCallModel->updateStatus($call['id'], 'PENDING', [
                    'scheduled_time' => $nextAttempt,
                    'error_message' => $smsResult['error'],
                    'attempt_count' => $attemptCount
                ]);
                return ['status' => 'RETRY_SCHEDULED', 'next_attempt' => $nextAttempt];
            }
        }
    }

    public function registerDevice(): array
    {
        try {
            $input = $this->getJsonInput();
            
            if (empty($input['device_id'])) {
                return $this->errorResponse('Missing device_id', 400);
            }

            $apiToken = $this->deviceModel->generateApiToken();
            
            $deviceData = [
                'device_id' => $input['device_id'],
                'device_name' => $input['device_name'] ?? null,
                'api_token' => $apiToken
            ];

            $this->deviceModel->create($deviceData);

            return $this->successResponse([
                'message' => 'Device registered successfully',
                'api_token' => $apiToken
            ]);

        } catch (\Exception $e) {
            error_log('Error in registerDevice: ' . $e->getMessage());
            return $this->errorResponse('Internal server error', 500);
        }
    }

    public function getLogs(): array
    {
        try {
            $deviceId = $_GET['device_id'] ?? null;
            $page = (int)($_GET['page'] ?? 1);
            $limit = min((int)($_GET['limit'] ?? 20), 100);

            if (!$deviceId) {
                return $this->errorResponse('Missing device_id parameter', 400);
            }

            $device = $this->deviceModel->findByDeviceId($deviceId);
            if (!$device) {
                return $this->errorResponse('Device not found', 404);
            }

            $logs = $this->missedCallModel->getCallsByDevice($deviceId, $page, $limit);
            $stats = $this->missedCallModel->getStats($deviceId);

            return $this->successResponse([
                'logs' => $logs,
                'stats' => $stats,
                'pagination' => [
                    'page' => $page,
                    'limit' => $limit
                ]
            ]);

        } catch (\Exception $e) {
            error_log('Error in getLogs: ' . $e->getMessage());
            return $this->errorResponse('Internal server error', 500);
        }
    }

    public function handleOptOut(): array
    {
        try {
            $input = $this->getJsonInput();
            
            if (empty($input['phone_number'])) {
                return $this->errorResponse('Missing phone_number', 400);
            }

            $this->blockedNumberModel->block($input['phone_number'], 'USER_OPTOUT');

            return $this->successResponse([
                'message' => 'Number added to opt-out list'
            ]);

        } catch (\Exception $e) {
            error_log('Error in handleOptOut: ' . $e->getMessage());
            return $this->errorResponse('Internal server error', 500);
        }
    }

    private function getJsonInput(): array
    {
        $input = json_decode(file_get_contents('php://input'), true);
        return $input ?: [];
    }

    private function getDefaultMessage(): string
    {
        return "Hello! We missed your call. We're sorry we couldn't pick up. Reply CALLBACK or visit our website and we'll get back to you shortly. Reply STOP to opt out.";
    }

    private function isWithinBusinessHours(): bool
    {
        $config = $this->config['business'];
        $currentTime = date('H:i');
        
        return $currentTime >= $config['hours_start'] && $currentTime <= $config['hours_end'];
    }

    private function getNextBusinessTime(): string
    {
        $config = $this->config['business'];
        $tomorrow = date('Y-m-d', strtotime('+1 day'));
        
        return $tomorrow . ' ' . $config['hours_start'] . ':00';
    }

    private function successResponse(array $data, int $code = 200): array
    {
        http_response_code($code);
        return [
            'success' => true,
            'data' => $data,
            'timestamp' => date('c')
        ];
    }

    private function errorResponse(string $message, int $code = 400): array
    {
        http_response_code($code);
        return [
            'success' => false,
            'error' => $message,
            'timestamp' => date('c')
        ];
    }
}