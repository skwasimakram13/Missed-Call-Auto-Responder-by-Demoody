<?php

namespace MissedCall\Services;

class SMSService
{
    private $config;
    private $apiKey;
    private $senderId;
    private $baseUrl;

    public function __construct()
    {
        $this->config = require __DIR__ . '/../../config/config.php';
        $this->apiKey = $this->config['fast2sms']['api_key'];
        $this->senderId = $this->config['fast2sms']['sender_id'];
        $this->baseUrl = $this->config['fast2sms']['base_url'];
    }

    public function sendSMS(string $phoneNumber, string $message): array
    {
        if (empty($this->apiKey)) {
            return [
                'success' => false,
                'error' => 'Fast2SMS API key not configured',
                'provider_response' => null
            ];
        }

        // Format phone number (ensure it starts with country code)
        $formattedNumber = $this->formatPhoneNumber($phoneNumber);
        
        // Prepare POST data
        $postData = http_build_query([
            'sender_id' => $this->senderId,
            'message' => $message,
            'route' => 'q', // Quality route
            'numbers' => $formattedNumber
        ]);

        // Initialize cURL
        $ch = curl_init($this->baseUrl);
        curl_setopt_array($ch, [
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => $postData,
            CURLOPT_HTTPHEADER => [
                'authorization: ' . $this->apiKey,
                'Content-Type: application/x-www-form-urlencoded'
            ],
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 30,
            CURLOPT_CONNECTTIMEOUT => 10,
            CURLOPT_SSL_VERIFYPEER => true,
            CURLOPT_USERAGENT => 'MissedCall-AutoResponder/1.0'
        ]);

        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $curlError = curl_error($ch);
        curl_close($ch);

        // Handle cURL errors
        if ($curlError) {
            error_log("SMS cURL Error: $curlError");
            return [
                'success' => false,
                'error' => 'Network error: ' . $curlError,
                'provider_response' => null
            ];
        }

        // Parse response
        $responseData = json_decode($response, true);
        
        if ($httpCode === 200 && $responseData) {
            return $this->parseProviderResponse($responseData);
        }

        return [
            'success' => false,
            'error' => "HTTP $httpCode: " . ($response ?: 'Unknown error'),
            'provider_response' => $responseData
        ];
    }

    private function formatPhoneNumber(string $phoneNumber): string
    {
        // Remove any non-digit characters
        $cleaned = preg_replace('/\D/', '', $phoneNumber);
        
        // If number doesn't start with country code, assume India (+91)
        if (strlen($cleaned) === 10) {
            $cleaned = '91' . $cleaned;
        }
        
        return $cleaned;
    }

    private function parseProviderResponse(array $response): array
    {
        // Fast2SMS response format varies, adapt as needed
        if (isset($response['return']) && $response['return'] === true) {
            return [
                'success' => true,
                'message_id' => $response['request_id'] ?? null,
                'provider_response' => $response
            ];
        }

        return [
            'success' => false,
            'error' => $response['message'] ?? 'Unknown provider error',
            'provider_response' => $response
        ];
    }

    public function validatePhoneNumber(string $phoneNumber): bool
    {
        $cleaned = preg_replace('/\D/', '', $phoneNumber);
        
        // Indian mobile numbers: 10 digits or 12 digits with country code
        return (strlen($cleaned) === 10 && preg_match('/^[6-9]/', $cleaned)) ||
               (strlen($cleaned) === 12 && substr($cleaned, 0, 2) === '91');
    }

    public function getProviderStatus(): array
    {
        // Simple health check - you might want to implement a dedicated endpoint
        return [
            'provider' => 'Fast2SMS',
            'configured' => !empty($this->apiKey),
            'base_url' => $this->baseUrl
        ];
    }
}