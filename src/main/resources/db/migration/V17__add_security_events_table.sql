-- V17: security_events tablosu
-- Güvenlik olaylarını saklar: giriş başarı/başarısız, kayıt, çıkış.
-- user_id ON DELETE SET NULL: kullanıcı silinse bile event geçmişi korunur.

CREATE TABLE security_events (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type  VARCHAR(50) NOT NULL,
    user_id     UUID        REFERENCES users(id) ON DELETE SET NULL,
    email       VARCHAR(255),
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    metadata    JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_security_events_user_id    ON security_events(user_id);
CREATE INDEX idx_security_events_event_type ON security_events(event_type);
CREATE INDEX idx_security_events_email      ON security_events(email);
CREATE INDEX idx_security_events_created_at ON security_events(created_at DESC);
