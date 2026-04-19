-- V1: Initial schema - roles, users, subjects, sessions, reviews, chat_messages

CREATE TABLE IF NOT EXISTS roles (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role_id    BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    age        INTEGER      NOT NULL,
    bio        VARCHAR(1000),
    avatar_url VARCHAR(255),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE IF NOT EXISTS subjects (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS mentor_subjects (
    mentor_id  BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    PRIMARY KEY (mentor_id, subject_id),
    CONSTRAINT fk_ms_mentor  FOREIGN KEY (mentor_id)  REFERENCES users    (id),
    CONSTRAINT fk_ms_subject FOREIGN KEY (subject_id) REFERENCES subjects (id)
);

CREATE TABLE IF NOT EXISTS sessions (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT      NOT NULL,
    mentor_id  BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP   NOT NULL,
    room_id    VARCHAR(100),
    CONSTRAINT fk_sessions_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_sessions_mentor  FOREIGN KEY (mentor_id)  REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS reviews (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT        NOT NULL,
    mentor_id  BIGINT        NOT NULL,
    rating     INTEGER       NOT NULL,
    comment    VARCHAR(1000),
    created_at TIMESTAMP     NOT NULL,
    CONSTRAINT fk_reviews_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_mentor  FOREIGN KEY (mentor_id)  REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id          BIGINT    AUTO_INCREMENT PRIMARY KEY,
    sender_id   BIGINT    NOT NULL,
    receiver_id BIGINT    NOT NULL,
    message     TEXT      NOT NULL,
    sent_at     TIMESTAMP NOT NULL,
    CONSTRAINT fk_chat_sender   FOREIGN KEY (sender_id)   REFERENCES users (id),
    CONSTRAINT fk_chat_receiver FOREIGN KEY (receiver_id) REFERENCES users (id)
);
