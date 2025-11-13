-- Chat Improvements Migration Script
-- EduConnect Application
-- This script adds new tables and columns for enhanced chat features

-- ============================================================================
-- MESSAGE REACTIONS
-- ============================================================================

-- Table: message_reactions
-- Stores emoji reactions for messages
CREATE TABLE IF NOT EXISTS message_reactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    message_id INT NOT NULL,
    user_id INT NOT NULL,
    reaction VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_reaction (message_id, user_id)
);

-- Index for fast reaction lookups
CREATE INDEX idx_message_reactions ON message_reactions(message_id);

-- ============================================================================
-- MESSAGES TABLE ENHANCEMENTS
-- ============================================================================

-- Add new columns to messages table for enhanced features
ALTER TABLE messages 
    ADD COLUMN IF NOT EXISTS status ENUM('sent', 'delivered', 'read') DEFAULT 'sent',
    ADD COLUMN IF NOT EXISTS message_type ENUM('text', 'image', 'file') DEFAULT 'text',
    ADD COLUMN IF NOT EXISTS file_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS edited BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS edited_at TIMESTAMP NULL;

-- Index for message status queries
CREATE INDEX IF NOT EXISTS idx_messages_status ON messages(status);

-- ============================================================================
-- TYPING STATUS
-- ============================================================================

-- Table: typing_status
-- Tracks which users are currently typing in conversations
CREATE TABLE IF NOT EXISTS typing_status (
    user_id INT NOT NULL,
    conversation_id INT NOT NULL,
    is_typing BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, conversation_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

-- Index for fast typing status lookups
CREATE INDEX IF NOT EXISTS idx_typing_status ON typing_status(conversation_id, is_typing);

-- ============================================================================
-- PERFORMANCE NOTES
-- ============================================================================

-- All indexes are created to optimize query performance:
-- 1. idx_message_reactions: Fast lookup of reactions for a message
-- 2. idx_messages_status: Efficient filtering by message status
-- 3. idx_typing_status: Quick retrieval of typing users in a conversation

-- ============================================================================
-- CLEANUP NOTES
-- ============================================================================

-- Typing status should be cleaned up periodically (older than 10 seconds)
-- This can be done via a scheduled job or application logic

-- Example cleanup query (not executed automatically):
-- DELETE FROM typing_status WHERE updated_at < DATE_SUB(NOW(), INTERVAL 10 SECOND);
