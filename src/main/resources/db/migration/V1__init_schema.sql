-- ──────────────────────────────────────────────────────────────────────────────
-- V1__init_schema.sql
-- Initial schema for the Matchmaking application
-- ──────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS companies (
    id          CHAR(36)     NOT NULL,
    name        VARCHAR(150) NOT NULL,
    domain      VARCHAR(100) NOT NULL UNIQUE,
    industry    VARCHAR(100),
    logo_url    VARCHAR(500),
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME,
    PRIMARY KEY (id),
    INDEX idx_company_domain (domain),
    INDEX idx_company_name   (name)
);

CREATE TABLE IF NOT EXISTS users (
    id                  CHAR(36)     NOT NULL,
    cognito_sub         VARCHAR(100) NOT NULL UNIQUE,
    email               VARCHAR(150) NOT NULL UNIQUE,
    first_name          VARCHAR(80)  NOT NULL,
    last_name           VARCHAR(80),
    company_id          CHAR(36)     NOT NULL,
    gender              VARCHAR(20),
    date_of_birth       DATE,
    timezone            VARCHAR(50),
    industry            VARCHAR(100),
    bio                 VARCHAR(500),
    profile_picture_url VARCHAR(500),
    status              VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    is_active           TINYINT(1)   NOT NULL DEFAULT 1,
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME,
    PRIMARY KEY (id),
    INDEX idx_user_cognito_sub (cognito_sub),
    INDEX idx_user_email       (email),
    INDEX idx_user_status      (status),
    INDEX idx_user_company     (company_id),
    CONSTRAINT fk_user_company FOREIGN KEY (company_id) REFERENCES companies (id)
);

CREATE TABLE IF NOT EXISTS user_interests (
    user_id  CHAR(36)    NOT NULL,
    interest VARCHAR(80) NOT NULL,
    CONSTRAINT fk_interest_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS user_preferences (
    id                        CHAR(36)  NOT NULL,
    user_id                   CHAR(36)  NOT NULL UNIQUE,
    min_age                   INT       DEFAULT 18,
    max_age                   INT       DEFAULT 60,
    preferred_gender          VARCHAR(20),
    max_timezone_offset_hours INT       DEFAULT 5,
    same_company_allowed      TINYINT(1) NOT NULL DEFAULT 0,
    created_at                DATETIME  NOT NULL,
    updated_at                DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_pref_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS preference_industries (
    preference_id CHAR(36)    NOT NULL,
    industry      VARCHAR(100) NOT NULL,
    CONSTRAINT fk_pref_industry FOREIGN KEY (preference_id) REFERENCES user_preferences (id)
);

CREATE TABLE IF NOT EXISTS matches (
    id                  CHAR(36)     NOT NULL,
    user_a_id           CHAR(36)     NOT NULL,
    user_b_id           CHAR(36)     NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    compatibility_score DOUBLE,
    round_count         INT          NOT NULL DEFAULT 0,
    max_rounds          INT          NOT NULL DEFAULT 3,
    meeting_type        VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME,
    PRIMARY KEY (id),
    INDEX idx_match_user_a (user_a_id),
    INDEX idx_match_user_b (user_b_id),
    INDEX idx_match_status (status),
    CONSTRAINT fk_match_user_a FOREIGN KEY (user_a_id) REFERENCES users (id),
    CONSTRAINT fk_match_user_b FOREIGN KEY (user_b_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS meetings (
    id              CHAR(36)     NOT NULL,
    match_id        CHAR(36)     NOT NULL,
    round_number    INT          NOT NULL,
    zoom_meeting_id VARCHAR(50),
    zoom_join_url   VARCHAR(500),
    zoom_start_url  VARCHAR(500),
    zoom_password   VARCHAR(50),
    scheduled_at    DATETIME,
    duration_minutes INT         NOT NULL DEFAULT 30,
    meeting_type    VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    status          VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME,
    PRIMARY KEY (id),
    INDEX idx_meeting_match  (match_id),
    INDEX idx_meeting_status (status),
    INDEX idx_meeting_time   (scheduled_at),
    CONSTRAINT fk_meeting_match FOREIGN KEY (match_id) REFERENCES matches (id)
);

CREATE TABLE IF NOT EXISTS meeting_feedback (
    id          CHAR(36)    NOT NULL,
    meeting_id  CHAR(36)    NOT NULL,
    user_id     CHAR(36)    NOT NULL,
    response    VARCHAR(20) NOT NULL,
    notes       VARCHAR(300),
    created_at  DATETIME    NOT NULL,
    updated_at  DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_feedback_meeting_user (meeting_id, user_id),
    INDEX idx_feedback_meeting (meeting_id),
    INDEX idx_feedback_user    (user_id),
    CONSTRAINT fk_feedback_meeting FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    CONSTRAINT fk_feedback_user    FOREIGN KEY (user_id)    REFERENCES users (id)
);

INSERT INTO users (
    id,
    cognito_sub,
    email,
    first_name,
    last_name,
    is_active,
    status,
    created_at,
    updated_at
) VALUES (
             gen_random_uuid(),
             '51638dda-a0b1-70e4-2b7c-9d30746999f7',
             'shiviishiv7@gmail.com',
             'Shiv',
             'User',
             true,
             'ACTIVE',
             NOW(),
             NOW()
         );