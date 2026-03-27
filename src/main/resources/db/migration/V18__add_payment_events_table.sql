-- V18: payment_events tablosu
-- Ödeme akışındaki her kritik adımı kaydeder: webhook, durum değişimi, iade, capture, void.
-- payment_id ON DELETE SET NULL: ödeme silinse bile event geçmişi korunur.

CREATE TABLE payment_events (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id  UUID        REFERENCES payments(id) ON DELETE SET NULL,
    event_type  VARCHAR(50) NOT NULL,
    provider    VARCHAR(20),
    raw_payload JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_events_payment_id  ON payment_events(payment_id);
CREATE INDEX idx_payment_events_event_type  ON payment_events(event_type);
CREATE INDEX idx_payment_events_created_at  ON payment_events(created_at DESC);
