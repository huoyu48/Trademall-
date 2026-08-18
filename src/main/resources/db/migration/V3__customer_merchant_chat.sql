-- 顾客与商家会话：一位顾客在一个商家租户下维护一条长期咨询会话。
CREATE TABLE IF NOT EXISTS chat_conversation (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id               BIGINT       NOT NULL COMMENT '商家租户',
    customer_id             BIGINT       NOT NULL,
    last_message_content    VARCHAR(200) NULL,
    last_message_at         DATETIME     NULL,
    customer_unread_count   INT          NOT NULL DEFAULT 0,
    merchant_unread_count   INT          NOT NULL DEFAULT 0,
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_chat_customer_tenant (customer_id, tenant_id),
    KEY idx_chat_customer_last (customer_id, last_message_at),
    KEY idx_chat_merchant_last (tenant_id, last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='顾客商家咨询会话';

-- 消息必须先持久化，WebSocket 仅用于实时推送；离线时可按会话补拉。
CREATE TABLE IF NOT EXISTS chat_message (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    conversation_id   BIGINT        NOT NULL,
    tenant_id         BIGINT        NOT NULL,
    sender_type       VARCHAR(16)   NOT NULL COMMENT 'CUSTOMER/MERCHANT',
    sender_id         BIGINT        NOT NULL,
    content           VARCHAR(1000) NOT NULL,
    read_at           DATETIME      NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_chat_message_history (conversation_id, id),
    KEY idx_chat_message_tenant (tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='顾客商家聊天消息';
