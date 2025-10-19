<?php

namespace MissedCall\Models;

use MissedCall\Database\Database;

class BlockedNumber
{
    private $db;

    public function __construct()
    {
        $this->db = Database::getInstance()->getConnection();
    }

    public function isBlocked(string $phoneNumber): bool
    {
        $sql = "SELECT COUNT(*) FROM blocked_numbers WHERE phone_number = :phone_number";
        $stmt = $this->db->prepare($sql);
        $stmt->execute([':phone_number' => $phoneNumber]);
        
        return $stmt->fetchColumn() > 0;
    }

    public function block(string $phoneNumber, string $reason = 'USER_OPTOUT'): bool
    {
        $sql = "INSERT INTO blocked_numbers (phone_number, reason) 
                VALUES (:phone_number, :reason)
                ON DUPLICATE KEY UPDATE 
                reason = VALUES(reason),
                blocked_at = CURRENT_TIMESTAMP";
        
        $stmt = $this->db->prepare($sql);
        return $stmt->execute([
            ':phone_number' => $phoneNumber,
            ':reason' => $reason
        ]);
    }

    public function unblock(string $phoneNumber): bool
    {
        $sql = "DELETE FROM blocked_numbers WHERE phone_number = :phone_number";
        $stmt = $this->db->prepare($sql);
        
        return $stmt->execute([':phone_number' => $phoneNumber]);
    }

    public function getBlockedNumbers(int $page = 1, int $limit = 50): array
    {
        $offset = ($page - 1) * $limit;
        
        $sql = "SELECT * FROM blocked_numbers 
                ORDER BY blocked_at DESC 
                LIMIT :limit OFFSET :offset";
        
        $stmt = $this->db->prepare($sql);
        $stmt->bindValue(':limit', $limit, \PDO::PARAM_INT);
        $stmt->bindValue(':offset', $offset, \PDO::PARAM_INT);
        $stmt->execute();
        
        return $stmt->fetchAll();
    }
}