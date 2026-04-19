-- V6: Group channels (named chat + call rooms owned by a mentor)

CREATE TABLE IF NOT EXISTS channels (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    creator_id BIGINT       NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_channels_creator FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS channel_members (
    channel_id BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    PRIMARY KEY (channel_id, user_id),
    CONSTRAINT fk_cm_channel FOREIGN KEY (channel_id) REFERENCES channels (id) ON DELETE CASCADE,
    CONSTRAINT fk_cm_user    FOREIGN KEY (user_id)    REFERENCES users    (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS channel_messages (
    id           BIGINT    AUTO_INCREMENT PRIMARY KEY,
    channel_id   BIGINT    NOT NULL,
    sender_id    BIGINT    NOT NULL,
    message      TEXT      NOT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    sent_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chmsg_channel FOREIGN KEY (channel_id) REFERENCES channels (id) ON DELETE CASCADE,
    CONSTRAINT fk_chmsg_sender  FOREIGN KEY (sender_id)  REFERENCES users    (id)
);
