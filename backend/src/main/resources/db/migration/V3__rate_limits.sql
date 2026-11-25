CREATE TABLE rate_limit_buckets (
    bucket_key VARCHAR(64) PRIMARY KEY,
    expires_at TIMESTAMPTZ NOT NULL,
    hits INTEGER NOT NULL CHECK (hits > 0)
);

CREATE INDEX idx_rate_limit_expiry ON rate_limit_buckets (expires_at);
