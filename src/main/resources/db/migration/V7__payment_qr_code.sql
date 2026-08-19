ALTER TABLE payment_transaction
    ADD COLUMN qr_code VARCHAR(1024) NULL AFTER status;
