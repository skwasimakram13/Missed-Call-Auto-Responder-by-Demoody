<?php

namespace MissedCall\Services;

use MissedCall\Database\Database;

class RateLimitService
{
    private $db;
    private $config;

    public function __construct()
    {
        $this->db = Database::getInstance()->getConnection();
        $this->config = require __DIR__ . '/../../config/config.php';
    }

    public function checkDeviceLimit(string $deviceId): bool
    {
        return $this->checkLimit($deviceId, 'DEVICE', $this->config['rate_limit']['per_device']);
    }

    public function checkPhoneLimit(string $phoneNumber): bool
    {
        return $this->checkLimit($phoneNumber, 'PHONE', $this->config['rate_limit']['per_phone']);
    }

    private function checkLimit(string $identifier, string $type, int $maxRequests): bool
    {
        $windowDuration = 3600; // 1 hour window
        
        // Clean old entries
        $this->cleanOldEntries($windowDuration);
        
        // Get current count
        $sql = "SELECT request_count FROM rate_limits 
                WHERE identifier = :identifier AND type = :type 
                AND window_start >= DATE_SUB(NOW(), INTERVAL :window SECOND)";
        
        $stmt = $this->db->prepare($sql);
        $stmt->execute([
            ':identifier' => $identifier,
            ':type' => $type,
            ':window' => $windowDuration
        ]);
        
        $currentCount = $stmt->fetchColumn() ?: 0;
        
        if ($currentCount >= $maxRequests) {
            return false; // Rate limit exceeded
        }
        
        // Increment counter
        $this->incrementCounter($identifier, $type);
        
        return true;
    }

    private function incrementCounter(string $identifier, string $type): void
    {
        $sql = "INSERT INTO rate_limits (identifier, type, request_count, window_start) 
                VALUES (:identifier, :type, 1, NOW())
                ON DUPLICATE KEY UPDATE 
                request_count = request_count + 1";
        
        $stmt = $this->db->prepare($sql);
        $stmt->execute([
            ':identifier' => $identifier,
            ':type' => $type
        ]);
    }

    private function cleanOldEntries(int $windowDuration): void
    {
        $sql = "DELETE FROM rate_limits 
                WHERE window_start < DATE_SUB(NOW(), INTERVAL :window SECOND)";
        
        $stmt = $this->db->prepare($sql);
        $stmt->execute([':window' => $windowDuration]);
    }

    public function getRemainingRequests(string $identifier, string $type): int
    {
        $maxRequests = $type === 'DEVICE' 
            ? $this->config['rate_limit']['per_device']
            : $this->config['rate_limit']['per_phone'];
        
        $sql = "SELECT request_count FROM rate_limits 
                WHERE identifier = :identifier AND type = :type 
                AND window_start >= DATE_SUB(NOW(), INTERVAL 3600 SECOND)";
        
        $stmt = $this->db->prepare($sql);
        $stmt->execute([
            ':identifier' => $identifier,
            ':type' => $type
        ]);
        
        $currentCount = $stmt->fetchColumn() ?: 0;
        
        return max(0, $maxRequests - $currentCount);
    }
}