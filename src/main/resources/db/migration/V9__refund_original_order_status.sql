ALTER TABLE refund
    ADD COLUMN original_order_status VARCHAR(32) NULL AFTER status;
