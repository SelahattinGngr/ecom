-- Iyzico iade için gereken per-item paymentTransactionId'leri JSON array olarak saklar
-- Ödeme callback'inde doldurulur, iade sırasında kullanılır
ALTER TABLE payments ADD COLUMN IF NOT EXISTS provider_item_transaction_ids jsonb;
