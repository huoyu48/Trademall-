ALTER TABLE payment_transaction
    CHANGE COLUMN alipay_trade_no provider_trade_no VARCHAR(64) NULL,
    ADD COLUMN payment_token VARCHAR(64) NULL AFTER qr_code,
    ADD COLUMN expires_at DATETIME NULL AFTER payment_token,
    ADD UNIQUE KEY uk_payment_token (payment_token);
