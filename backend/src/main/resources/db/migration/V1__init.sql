CREATE TABLE app_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL UNIQUE,
    password_hash   VARCHAR(128) NOT NULL,
    display_name    VARCHAR(128) NOT NULL,
    role            VARCHAR(16)  NOT NULL CHECK (role IN ('ADMIN','KID')),
    avatar_color    VARCHAR(16)  NOT NULL DEFAULT '#4263eb',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE task (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    points          INT          NOT NULL DEFAULT 1,
    icon            VARCHAR(8),
    recurrence      VARCHAR(16)  NOT NULL DEFAULT 'DAILY' CHECK (recurrence IN ('DAILY','WEEKLY','ONCE')),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE task_assignment (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (task_id, user_id)
);
CREATE INDEX ix_assignment_user ON task_assignment(user_id);

CREATE TABLE task_completion (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    completion_date DATE   NOT NULL,
    done            BOOLEAN NOT NULL DEFAULT TRUE,
    completed_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (task_id, user_id, completion_date)
);
CREATE INDEX ix_completion_user_date ON task_completion(user_id, completion_date);
