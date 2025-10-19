<?php

namespace MissedCall\Models;

use MissedCall\Database\Database;
use PDO;

class MissedCall
{
    private $db;

    public function __construct()
    {
        $this->db = Database::getInstance()->getConnection();
    }

    public function create(array $data): ?int
    {
        $sql = "INSERT INTO missed_calls (device_id, phone_number, call_time, scheduled_time, message_text) 
                VALUES (:device_id, :phone_number, :call_time, :scheduled_time, :message_text)";
        
        try {
            $stmt = $this->db->prepare($sql);
            $result = $stmt->execute([
                ':device_id' => $data['device_id'],
                ':phone_number' => $data['phone_number'],
                ':call_time' => $data['call_time'],
                ':scheduled_time' => $data['scheduled_time'] ?? null,
                ':message_text' => $data['message_text'] ?? null
            ]);
            
            return $result ? (int)$this->db->lastInsertId() : null;
        } catch (\PDOException $e) {
            if ($e->getCode() == 23000) { // Duplicate entry
                return null; // Already exists, skip
            }
            error_log('Failed to create missed call: ' . $e->getMessage());
            throw $e;
        }
    }

    public function findById(int $id): ?array
    {
        $sql = "SELECT * FROM missed_calls WHERE id = :id";
        $stmt = $this->db->prepare($sql);
        $stmt->execute([':id' => $id]);
        
        $result = $stmt->fetch();
        return $result ?: null;
    }

    public function findByDeviceAndPhone(string $deviceId, string $phoneNumber, int $timeWindow = 3600): ?array
    {
        $sql = "SELECT * FROM missed_calls 
                WHERE device_id = :device_id 
                AND phone_number = :phone_number 
                AND call_time >= DATE_SUB(NOW(), INTERVAL :time_window SECOND)
                ORDER BY call_time DESC 
                LIMIT 1";
        
        $stmt = $this->db->prepare($sql);
        $stmt->execute([
            ':device_id' => $deviceId,
            ':phone_number' => $phoneNumber,
            ':time_window' => $timeWindow
        ]);
        
        $result = $stmt->fetch();
        return $result ?: null;
    }

    public function updateStatus(int $id, string $status, array $additionalData = []): bool
    {
        $fields = ['status = :status'];
        $params = [':id' => $id, ':status' => $status];
        
        if (isset($additionalData['provider_msg_id'])) {
            $fields[] = 'provider_msg_id = :provider_msg_id';
            $params[':provider_msg_id'] = $additionalData['provider_msg_id'];
        }
        
        if (isset($additionalData['error_message'])) {
            $fields[] = 'error_message = :error_message';
            $params[':error_message'] = $additionalData['error_message'];
        }
        
        if (isset($additionalData['attempt_count'])) {
            $fields[] = 'attempt_count = :attempt_count';
            $params[':attempt_count'] = $additionalData['attempt_count'];
        }
        
        if ($status === 'SENT') {
            $fields[] = 'sent_at = NOW()';
        }
        
        $sql = "UPDATE missed_calls SET " . implode(', ', $fields) . " WHERE id = :id";
        $stmt = $this->db->prepare($sql);
        
        return $stmt->execute($params);
    }

    public function getPendingCalls(int $limit = 50): array
    {
        $sql = "SELECT * FROM missed_calls 
                WHERE status = 'PENDING' 
                AND scheduled_time <= NOW() 
                ORDER BY scheduled_time ASC 
                LIMIT :limit";
        
        $stmt = $this->db->prepare($sql);
        $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
        $stmt->execute();
        
        return $stmt->fetchAll();
    }

    public function getCallsByDevice(string $deviceId, int $page = 1, int $limit = 20): array
    {
        $offset = ($page - 1) * $limit;
        
        $sql = "SELECT * FROM missed_calls 
                WHERE device_id = :device_id 
                ORDER BY call_time DESC 
                LIMIT :limit OFFSET :offset";
        
        $stmt = $this->db->prepare($sql);
        $stmt->bindValue(':device_id', $deviceId);
        $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
        $stmt->bindValue(':offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        
        return $stmt->fetchAll();
    }

    public function getStats(string $deviceId = null): array
    {
        $whereClause = $deviceId ? "WHERE device_id = :device_id" : "";
        $params = $deviceId ? [':device_id' => $deviceId] : [];
        
        $sql = "SELECT 
                    COUNT(*) as total_calls,
                    SUM(CASE WHEN status = 'SENT' THEN 1 ELSE 0 END) as sent_count,
                    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed_count,
                    SUM(CASE WHEN status = 'SKIPPED' THEN 1 ELSE 0 END) as skipped_count,
                    SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending_count
                FROM missed_calls $whereClause";
        
        $stmt = $this->db->prepare($sql);
        $stmt->execute($params);
        
        return $stmt->fetch();
    }
}