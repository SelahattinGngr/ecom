CREATE TABLE user_activity_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID,
    device_id   VARCHAR(255),
    ip_address  VARCHAR(45),
    method      VARCHAR(10)  NOT NULL,
    endpoint    VARCHAR(500) NOT NULL,
    status_code INTEGER,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_uae_user_id    ON user_activity_events(user_id);
CREATE INDEX idx_uae_device_id  ON user_activity_events(device_id);
CREATE INDEX idx_uae_ip_address ON user_activity_events(ip_address);
CREATE INDEX idx_uae_created_at ON user_activity_events(created_at DESC);
