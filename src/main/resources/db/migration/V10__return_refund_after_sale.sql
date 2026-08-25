ALTER TABLE refund
    ADD COLUMN after_sale_type VARCHAR(32) NOT NULL DEFAULT 'REFUND_ONLY' AFTER status,
    ADD COLUMN return_tracking_no VARCHAR(64) NULL AFTER after_sale_type,
    ADD COLUMN return_approved_at DATETIME NULL AFTER return_tracking_no,
    ADD COLUMN returned_at DATETIME NULL AFTER return_approved_at,
    ADD COLUMN received_at DATETIME NULL AFTER returned_at;
