CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL, -- SHA-256 hex = 64 chars
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    replaced_by UUID NULL,
    created_by_ip VARCHAR(64) NULL,
    user_agent VARCHAR(255) NULL,

    CONSTRAINT fk_refresh_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE UNIQUE INDEX uq_refresh_token_hash ON refresh_tokens(token_hash);
