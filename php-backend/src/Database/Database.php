<?php

namespace MissedCall\Database;

use PDO;
use PDOException;

class Database
{
    private static $instance = null;
    private $connection;
    private $config;

    private function __construct()
    {
        $this->config = require __DIR__ . '/../../config/config.php';
        $this->connect();
    }

    public static function getInstance(): Database
    {
        if (self::$instance === null) {
            self::$instance = new self();
        }
        return self::$instance;
    }

    private function connect(): void
    {
        $dbConfig = $this->config['database'];
        
        $dsn = sprintf(
            'mysql:host=%s;port=%d;dbname=%s;charset=%s',
            $dbConfig['host'],
            $dbConfig['port'],
            $dbConfig['name'],
            $dbConfig['charset']
        );

        try {
            $this->connection = new PDO($dsn, $dbConfig['user'], $dbConfig['pass'], $dbConfig['options']);
            
            // Test the connection
            $this->connection->query('SELECT 1');
        } catch (PDOException $e) {
            error_log('Database connection failed: ' . $e->getMessage());
            
            // More specific error messages
            if (strpos($e->getMessage(), 'Access denied') !== false) {
                throw new \Exception('Database authentication failed. Check credentials.');
            } elseif (strpos($e->getMessage(), 'Unknown database') !== false) {
                throw new \Exception('Database does not exist. Run the setup script.');
            } else {
                throw new \Exception('Database connection failed: ' . $e->getMessage());
            }
        }
    }

    public function getConnection(): PDO
    {
        return $this->connection;
    }

    public function beginTransaction(): bool
    {
        return $this->connection->beginTransaction();
    }

    public function commit(): bool
    {
        return $this->connection->commit();
    }

    public function rollback(): bool
    {
        return $this->connection->rollback();
    }

    public function lastInsertId(): string
    {
        return $this->connection->lastInsertId();
    }
}