-- ============================================
-- Audit Log Table (Immutable Transaction Logs)
-- ============================================
-- This table is populated by the TransactionConsumer
-- which listens to the Kafka "transaction-events" topic.
-- It stores an immutable log of all wallet operations.

CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id        VARCHAR(64) NOT NULL UNIQUE,
    user_id         BIGINT NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    amount          DECIMAL(19,4) NOT NULL,
    reference_id    VARCHAR(64),
    description     VARCHAR(255),
    event_timestamp DATETIME NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_audit_user (user_id),
    INDEX idx_audit_event (event_id),
    INDEX idx_audit_type (transaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
