CREATE TABLE IF NOT EXISTS payment_transaction (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id             BIGINT       NOT NULL,
    order_id              BIGINT       NOT NULL,
    out_trade_no          VARCHAR(64)  NOT NULL,
    provider              VARCHAR(32)  NOT NULL,
    amount_cent           BIGINT       NOT NULL,
    status                VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    alipay_trade_no       VARCHAR(64)  NULL,
    callback_payload      TEXT         NULL,
    paid_at               DATETIME     NULL,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_out_trade_no (out_trade_no),
    UNIQUE KEY uk_payment_order_provider (order_id, provider),
    KEY idx_payment_status_created (status, created_at),
    KEY idx_payment_tenant_order (tenant_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方支付流水；订单支付状态必须以验签后的异步通知为准';
