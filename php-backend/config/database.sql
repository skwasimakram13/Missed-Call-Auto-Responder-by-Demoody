-- Database schema for Missed Call Auto-Responder
-- Run this script to create the required tables

CREATE DATABASE IF NOT EXISTS missed_call CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE missed_call;

-- Devices table for authentication and tracking
CREATE TABLE devices (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(128) UNIQUE NOT NULL,
    device_name VARCHAR(255),
    api_token VARCHAR(255) UNIQUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_device_id (device_id),
    INDEX idx_api_token (api_token)
);

-- Missed calls table for logging and deduplication
CREATE TABLE missed_calls (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(128) NOT NULL,
    phone_number VARCHAR(32) NOT NULL,
    call_time DATETIME NOT NULL,
    scheduled_time DATETIME,
    status ENUM('PENDING','SENT','FAILED','SKIPPED','BLOCKED') DEFAULT 'PENDING',
    message_text TEXT,
    provider_msg_id VARCHAR(128),
    attempt_count INT DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL,
    UNIQUE KEY unique_call (device_id, phone_number, call_time),
    INDEX idx_device_phone (device_id, phone_number),
    INDEX idx_status (status),
    INDEX idx_call_time (call_time),
    FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE
);

-- Blocked numbers table for opt-out management
CREATE TABLE blocked_numbers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    phone_number VARCHAR(32) UNIQUE NOT NULL,
    reason ENUM('USER_OPTOUT','ADMIN_BLOCK','SPAM') DEFAULT 'USER_OPTOUT',
    blocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_phone_number (phone_number)
);

-- Message templates table
CREATE TABLE message_templates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    template_text TEXT NOT NULL,
    language VARCHAR(10) DEFAULT 'en',
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Rate limiting table
CREATE TABLE rate_limits (
    id INT AUTO_INCREMENT PRIMARY KEY,
    identifier VARCHAR(128) NOT NULL,
    type ENUM('DEVICE','PHONE') NOT NULL,
    request_count INT DEFAULT 1,
    window_start TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_limit (identifier, type),
    INDEX idx_window_start (window_start)
);

-- Insert default message template
INSERT INTO message_templates (name, template_text, language, is_default, is_active) VALUES
('default_english', 'Hello! We missed your call. We''re sorry we couldn''t pick up. Reply CALLBACK or visit our website and we''ll get back to you shortly. Reply STOP to opt out.', 'en', TRUE, TRUE),
('default_hindi', 'नमस्ते! हमसे आपका कॉल छूट गया। हमें खुशी होगी यदि आप दोबारा कॉल करें। STOP भेजकर इस सेवा से बाहर निकल सकते हैं।', 'hi', FALSE, TRUE);

-- Create user for the application (adjust credentials as needed)
-- CREATE USER 'missed_user'@'localhost' IDENTIFIED BY 'your_secure_password';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON missed_call.* TO 'missed_user'@'localhost';
-- FLUSH PRIVILEGES;