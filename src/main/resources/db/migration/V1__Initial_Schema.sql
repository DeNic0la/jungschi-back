CREATE SEQUENCE IF NOT EXISTS IntoleranceSelection_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS camp_stats_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS global_definitions_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS health_stats_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS participants_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IntoleranceSelection
(
    id             BIGINT NOT NULL,
    participant_id BIGINT,
    intolerance_id BIGINT,
    custom_text    VARCHAR(255),
    severity       SMALLINT,
    CONSTRAINT pk_intoleranceselection PRIMARY KEY (id)
);

CREATE TABLE camp_stats
(
    id               BIGINT NOT NULL,
    participant_id   BIGINT,
    isTickVaccinated BOOLEAN,
    drugConsent      BOOLEAN,
    ahv              VARCHAR(255),
    krankenkasse     VARCHAR(255),
    notes            VARCHAR(255),
    CONSTRAINT pk_camp_stats PRIMARY KEY (id)
);

CREATE TABLE global_definitions
(
    id       BIGINT NOT NULL,
    label    VARCHAR(255),
    value    VARCHAR(255),
    category SMALLINT,
    CONSTRAINT pk_global_definitions PRIMARY KEY (id)
);

CREATE TABLE health_stats
(
    id                  BIGINT NOT NULL,
    participant_id      BIGINT,
    isHealthy           BOOLEAN,
    helthy_reason       VARCHAR(255),
    excluded_activities VARCHAR(255),
    CONSTRAINT pk_health_stats PRIMARY KEY (id)
);

CREATE TABLE participants
(
    id              BIGINT NOT NULL,
    firstname       VARCHAR(255),
    lastname        VARCHAR(255),
    date_of_birth   date,
    last_updated_at TIMESTAMP WITHOUT TIME ZONE,
    app_user_id     VARCHAR(255),
    CONSTRAINT pk_participants PRIMARY KEY (id)
);

CREATE TABLE users
(
    oidc_subject VARCHAR(255) NOT NULL,
    username     VARCHAR(255),
    email        VARCHAR(255),
    first_name   VARCHAR(255),
    phonenumber  VARCHAR(255),
    last_name    VARCHAR(255),
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_seen_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (oidc_subject)
);

ALTER TABLE camp_stats
    ADD CONSTRAINT FK_CAMP_STATS_ON_PARTICIPANT FOREIGN KEY (participant_id) REFERENCES participants (id);

ALTER TABLE health_stats
    ADD CONSTRAINT FK_HEALTH_STATS_ON_PARTICIPANT FOREIGN KEY (participant_id) REFERENCES participants (id);

ALTER TABLE IntoleranceSelection
    ADD CONSTRAINT FK_INTOLERANCESELECTION_ON_INTOLERANCE FOREIGN KEY (intolerance_id) REFERENCES global_definitions (id);

ALTER TABLE IntoleranceSelection
    ADD CONSTRAINT FK_INTOLERANCESELECTION_ON_PARTICIPANT FOREIGN KEY (participant_id) REFERENCES participants (id);

ALTER TABLE participants
    ADD CONSTRAINT FK_PARTICIPANTS_ON_APP_USER FOREIGN KEY (app_user_id) REFERENCES users (oidc_subject);