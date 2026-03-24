-- Iyzico'nun numeric paymentId'sini saklamak için
-- payment_transaction_id = checkout form token (init sırasında)
-- provider_payment_id     = Iyzico'nun numeric paymentId'si (callback sırasında alınır)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS provider_payment_id VARCHAR(64);
