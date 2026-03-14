CREATE TABLE IF NOT EXISTS price_alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coin VARCHAR(255) NOT NULL,
    target_price DECIMAL(30, 8) NOT NULL,
    alert_condition VARCHAR(50) NOT NULL,
    triggered BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    triggered_at DATETIME,
    triggered_price DECIMAL(30, 8),
    CONSTRAINT fk_price_alert_user FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE IF NOT EXISTS payment_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    amount BIGINT,
    payment_order_status VARCHAR(50),
    payment_method VARCHAR(50),
    user_id BIGINT,
    CONSTRAINT fk_payment_order_user FOREIGN KEY (user_id) REFERENCES user(id)
);

-- Forced schema repair for IDENTITY strategy
-- These ensure that existing tables created with AUTO are upgraded to AUTO_INCREMENT
ALTER TABLE wallet MODIFY id BIGINT AUTO_INCREMENT;
ALTER TABLE withdrawal MODIFY id BIGINT AUTO_INCREMENT;
ALTER TABLE wallet_transaction MODIFY id BIGINT AUTO_INCREMENT;
ALTER TABLE orders MODIFY id BIGINT AUTO_INCREMENT;
ALTER TABLE order_item MODIFY id BIGINT AUTO_INCREMENT;
ALTER TABLE asset MODIFY id BIGINT AUTO_INCREMENT;
ALTER TABLE watchlist MODIFY id BIGINT AUTO_INCREMENT;
ALTER TABLE verification_code MODIFY id BIGINT AUTO_INCREMENT;
ALTER TABLE payment_order MODIFY id BIGINT AUTO_INCREMENT;
ALTER TABLE system_revenue MODIFY id BIGINT AUTO_INCREMENT;
