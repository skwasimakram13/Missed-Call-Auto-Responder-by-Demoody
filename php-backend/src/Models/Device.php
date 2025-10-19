<?php

namespace MissedCall\Models;

use MissedCall\Database\Database;

class Device
{
    private $db;

    public function __construct()
    {
        $this->db = Database::getInstance()->getConnection();
    }

    public function create(array $data): ?int
    {
        $sql = "INSERT INTO devices (device_id, device_name, api_token) 
                VALUES (:device_id, :device_name, :api_token)
                ON DUPLICATE KEY UPDATE 
                device_name = VALUES(device_name),
                updated_at = CURRENT_TIMESTAMP";
        
        try {
            $stmt = $this->db->prepare($sql);
            $result = $stmt->execute([
                ':device_id' => $data['device_id'],
                ':device_name' => $data['device_name'] ?? null,
                ':api_token' => $data['api_token']
            ]);
            
            return $result ? (int)$this->db->lastInsertId() : null;
        } catch (\PDOException $e) {
            error_log('Failed to create device: ' . $e->getMessage());
            throw $e;
        }
    }

    public function findByDeviceId(string $deviceId): ?array
    {
        $sql = "SELECT * FROM devices WHERE device_id = :device_id AND is_active = 1";
        $stmt = $this->db->prepare($sql);
        $stmt->execute([':device_id' => $deviceId]);
        
        $result = $stmt->fetch();
        return $result ?: null;
    }

    public function findByToken(string $token): ?array
    {
        $sql = "SELECT * FROM devices WHERE api_token = :token AND is_active = 1";
        $stmt = $this->db->prepare($sql);
        $stmt->execute([':token' => $token]);
        
        $result = $stmt->fetch();
        return $result ?: null;
    }

    public function updateLastActivity(string $deviceId): bool
    {
        $sql = "UPDATE devices SET updated_at = CURRENT_TIMESTAMP WHERE device_id = :device_id";
        $stmt = $this->db->prepare($sql);
        
        return $stmt->execute([':device_id' => $deviceId]);
    }

    public function deactivate(string $deviceId): bool
    {
        $sql = "UPDATE devices SET is_active = 0 WHERE device_id = :device_id";
        $stmt = $this->db->prepare($sql);
        
        return $stmt->execute([':device_id' => $deviceId]);
    }

    public function generateApiToken(): string
    {
        return bin2hex(random_bytes(32));
    }
}