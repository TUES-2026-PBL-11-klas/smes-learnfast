CREATE TABLE IF NOT EXISTS call_records (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    caller_id        BIGINT NOT NULL,
    callee_id        BIGINT NOT NULL,
    room_id          VARCHAR(100) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'RINGING',
    started_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    answered_at      TIMESTAMP NULL,
    ended_at         TIMESTAMP NULL,
    duration_seconds INT NULL,
    CONSTRAINT fk_call_caller FOREIGN KEY (caller_id) REFERENCES users(id),
    CONSTRAINT fk_call_callee FOREIGN KEY (callee_id) REFERENCES users(id)
);
