CREATE SEQUENCE IF NOT EXISTS IntoleranceSelection_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS camp_stats_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS global_definitions_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS health_stats_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS household_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS participants_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS room_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS signup_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS camp_participant_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS camp_participant_medication_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IntoleranceSelection
(
    id             BIGINT NOT NULL,
    participant_id BIGINT,
    intolerance_id BIGINT,
    custom_text    TEXT,
    severity       SMALLINT,
    CONSTRAINT pk_intoleranceselection PRIMARY KEY (id)
);

CREATE TABLE camp_stats
(
    id               BIGINT NOT NULL,
    participant_id   BIGINT,
    isTickVaccinated BOOLEAN,
    drugConsent      BOOLEAN,
    ahv              VARCHAR(16),
    krankenkasse     VARCHAR(50),
    krankenkassenNr  VARCHAR(50),
    medication       VARCHAR(255),
    familyDoctor     VARCHAR(255),
    nationality      VARCHAR(100),
    native_language  VARCHAR(100),
    food_preferences TEXT,
    notes            TEXT,
    CONSTRAINT pk_camp_stats PRIMARY KEY (id)
);

CREATE TABLE camp
(
    id                   VARCHAR(100) NOT NULL,
    title                VARCHAR(255),
    description          TEXT,
    start_date           DATE,
    end_date             DATE,
    signup_enddate       DATE,
    is_jugend_und_sport  BOOLEAN,
    price_first          DECIMAL(10, 2),
    price_second         DECIMAL(10, 2),
    price_third          DECIMAL(10, 2),
    CONSTRAINT pk_camp PRIMARY KEY (id)
);

CREATE TABLE signup
(
    id                                    BIGINT NOT NULL,
    household_id                          BIGINT,
    camp_id                               VARCHAR(100),
    state                                 VARCHAR(32),
    feedback                              TEXT,
    photo_consent                         BOOLEAN,
    info_email                            BOOLEAN,
    additional_contact_options_during_camp TEXT,
    CONSTRAINT pk_signup PRIMARY KEY (id)
);

CREATE TABLE room
(
    id           BIGINT NOT NULL,
    camp_id      VARCHAR(100),
    name         VARCHAR(255),
    max_capacity INTEGER,
    gender       VARCHAR(16),
    CONSTRAINT pk_room PRIMARY KEY (id)
);

CREATE TABLE camp_participant
(
    id                  BIGINT NOT NULL,
    participant_id      BIGINT,
    signup_id           BIGINT,
    camp_id             VARCHAR(100),
    room_id             BIGINT,
    school_class        VARCHAR(100),
    infos_zimmerleitung TEXT,
    bemerkungen         TEXT,
    CONSTRAINT pk_camp_participant PRIMARY KEY (id)
);

CREATE TABLE camp_participant_medication
(
    id                  BIGINT NOT NULL,
    camp_participant_id BIGINT,
    medication_name     VARCHAR(255),
    dose                VARCHAR(255),
    frequency           VARCHAR(255),
    purpose             VARCHAR(255),
    needs_help          BOOLEAN,
    confidential        BOOLEAN,
    CONSTRAINT pk_camp_participant_medication PRIMARY KEY (id)
);

CREATE TABLE room_leader_assignment
(
    room_id    BIGINT       NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    CONSTRAINT pk_room_leader_assignment PRIMARY KEY (room_id, user_email)
);

CREATE TABLE global_definitions
(
    id               BIGINT NOT NULL,
    label            VARCHAR(30),
    definition_value VARCHAR(200),
    category         SMALLINT,
    CONSTRAINT pk_global_definitions PRIMARY KEY (id)
);

CREATE TABLE health_stats
(
    id                  BIGINT NOT NULL,
    participant_id      BIGINT,
    isHealthy           BOOLEAN,
    healthy_reason      TEXT,
    excluded_activities VARCHAR(255),
    CONSTRAINT pk_health_stats PRIMARY KEY (id)
);

CREATE TABLE users
(
    email        VARCHAR(255)                NOT NULL,
    oidc_subject VARCHAR(100),
    username     VARCHAR(100),
    first_name   VARCHAR(100),
    phonenumber  VARCHAR(100),
    last_name    VARCHAR(100),
    address      TEXT,
    picture_url  TEXT,
    roles        TEXT,
    openid_connect_data TEXT,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_seen_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (email)
);

CREATE TABLE household
(
    id                      BIGINT NOT NULL,
    primary_contact_id      VARCHAR(255) NOT NULL,
    secondary_contact_id    VARCHAR(255),
    secondary_contact_email VARCHAR(255),
    street_and_number       VARCHAR(255),
    plz                     VARCHAR(20),
    place                   VARCHAR(100),
    CONSTRAINT pk_household PRIMARY KEY (id)
);

CREATE TABLE participants
(
    id              BIGINT NOT NULL,
    firstname       VARCHAR(100),
    lastname        VARCHAR(100),
    date_of_birth   DATE,
    gender          VARCHAR(16),
    last_updated_at TIMESTAMP WITHOUT TIME ZONE,
    household_id    BIGINT,
    CONSTRAINT pk_participants PRIMARY KEY (id)
);

ALTER TABLE camp_stats
    ADD CONSTRAINT uc_camp_stats_participant UNIQUE (participant_id);

ALTER TABLE health_stats
    ADD CONSTRAINT uc_health_stats_participant UNIQUE (participant_id);

ALTER TABLE household
    ADD CONSTRAINT uc_household_primary_contact UNIQUE (primary_contact_id);

ALTER TABLE users
    ADD CONSTRAINT uc_users_oidc_subject UNIQUE (oidc_subject);

ALTER TABLE camp_stats
    ADD CONSTRAINT FK_CAMP_STATS_ON_PARTICIPANT FOREIGN KEY (participant_id) REFERENCES participants (id);

ALTER TABLE room
    ADD CONSTRAINT FK_ROOM_ON_CAMP FOREIGN KEY (camp_id) REFERENCES camp (id);

ALTER TABLE signup
    ADD CONSTRAINT FK_SIGNUP_ON_HOUSEHOLD FOREIGN KEY (household_id) REFERENCES household (id);

ALTER TABLE signup
    ADD CONSTRAINT FK_SIGNUP_ON_CAMP FOREIGN KEY (camp_id) REFERENCES camp (id);

ALTER TABLE health_stats
    ADD CONSTRAINT FK_HEALTH_STATS_ON_PARTICIPANT FOREIGN KEY (participant_id) REFERENCES participants (id);

ALTER TABLE household
    ADD CONSTRAINT FK_HOUSEHOLD_ON_PRIMARY_CONTACT FOREIGN KEY (primary_contact_id) REFERENCES users (email);

ALTER TABLE household
    ADD CONSTRAINT FK_HOUSEHOLD_ON_SECONDARY_CONTACT FOREIGN KEY (secondary_contact_id) REFERENCES users (email);

ALTER TABLE IntoleranceSelection
    ADD CONSTRAINT FK_INTOLERANCESELECTION_ON_INTOLERANCE FOREIGN KEY (intolerance_id) REFERENCES global_definitions (id);

ALTER TABLE IntoleranceSelection
    ADD CONSTRAINT FK_INTOLERANCESELECTION_ON_PARTICIPANT FOREIGN KEY (participant_id) REFERENCES participants (id);

ALTER TABLE room_leader_assignment
    ADD CONSTRAINT FK_ROOM_LEADER_ASSIGNMENT_ON_ROOM FOREIGN KEY (room_id) REFERENCES room (id);

ALTER TABLE room_leader_assignment
    ADD CONSTRAINT FK_ROOM_LEADER_ASSIGNMENT_ON_USER FOREIGN KEY (user_email) REFERENCES users (email);

ALTER TABLE camp_participant
    ADD CONSTRAINT FK_CAMP_PARTICIPANT_ON_PARTICIPANT FOREIGN KEY (participant_id) REFERENCES participants (id);

ALTER TABLE camp_participant
    ADD CONSTRAINT FK_CAMP_PARTICIPANT_ON_SIGNUP FOREIGN KEY (signup_id) REFERENCES signup (id);

ALTER TABLE camp_participant
    ADD CONSTRAINT FK_CAMP_PARTICIPANT_ON_CAMP FOREIGN KEY (camp_id) REFERENCES camp (id);

ALTER TABLE camp_participant
    ADD CONSTRAINT FK_CAMP_PARTICIPANT_ON_ROOM FOREIGN KEY (room_id) REFERENCES room (id);

ALTER TABLE camp_participant_medication
    ADD CONSTRAINT FK_CAMP_PARTICIPANT_MEDICATION_ON_CAMP_PARTICIPANT FOREIGN KEY (camp_participant_id) REFERENCES camp_participant (id);

ALTER TABLE participants
    ADD CONSTRAINT FK_PARTICIPANTS_ON_HOUSEHOLD FOREIGN KEY (household_id) REFERENCES household (id);

INSERT INTO global_definitions (id, label, definition_value, category)
VALUES (nextval('global_definitions_seq'), 'gluten', 'Glutenunverträglichkeit / Zöliakie', 0),
       (nextval('global_definitions_seq'), 'laktose', 'Laktoseintoleranz', 0),
       (nextval('global_definitions_seq'), 'fruktose', 'Fruktoseintoleranz', 0),
       (nextval('global_definitions_seq'), 'histamin', 'Histaminintoleranz', 0),
       (nextval('global_definitions_seq'), 'milch', 'Milchallergie', 0),
       (nextval('global_definitions_seq'), 'ei', 'Hühnerei-Allergie', 0),
       (nextval('global_definitions_seq'), 'erdnuss', 'Erdnussallergie', 0),
       (nextval('global_definitions_seq'), 'nuesse', 'Schalenfruchtallergie / Nussallergie', 0),
       (nextval('global_definitions_seq'), 'mandel', 'Mandelallergie', 0),
       (nextval('global_definitions_seq'), 'haselnuss', 'Haselnussallergie', 0),
       (nextval('global_definitions_seq'), 'walnuss', 'Walnussallergie', 0),
       (nextval('global_definitions_seq'), 'cashew', 'Cashewallergie', 0),
       (nextval('global_definitions_seq'), 'pistazie', 'Pistazienallergie', 0),
       (nextval('global_definitions_seq'), 'sesam', 'Sesamallergie', 0),
       (nextval('global_definitions_seq'), 'soja', 'Sojaallergie', 0),
       (nextval('global_definitions_seq'), 'weizen', 'Weizenallergie', 0),
       (nextval('global_definitions_seq'), 'fisch', 'Fischallergie', 0),
       (nextval('global_definitions_seq'), 'krebstiere', 'Krebstierallergie', 0),
       (nextval('global_definitions_seq'), 'weichtiere', 'Weichtierallergie', 0),
       (nextval('global_definitions_seq'), 'sellerie', 'Sellerieallergie', 0),
       (nextval('global_definitions_seq'), 'senf', 'Senfallergie', 0),
       (nextval('global_definitions_seq'), 'lupine', 'Lupinenallergie', 0),
       (nextval('global_definitions_seq'), 'schwefeldioxid', 'Schwefeldioxid-/Sulfitempfindlichkeit', 0),
       (nextval('global_definitions_seq'), 'steinobst', 'Allergie auf Steinobst', 0),
       (nextval('global_definitions_seq'), 'zitrus', 'Zitrusfruchtallergie', 0),
       (nextval('global_definitions_seq'), 'honig', 'Honigallergie / Unverträglicheit', 0),
       (nextval('global_definitions_seq'), 'pollen', 'Pollenallergie', 1),
       (nextval('global_definitions_seq'), 'hausstaub', 'Hausstaubmilbenallergie', 1),
       (nextval('global_definitions_seq'), 'tierhaare', 'Tierhaarallergie', 1),
       (nextval('global_definitions_seq'), 'insektengift', 'Insektengiftallergie', 1),
       (nextval('global_definitions_seq'), 'latex', 'Latexallergie', 1),
       (nextval('global_definitions_seq'), 'schimmel', 'Schimmelpilzallergie', 1),
       (nextval('global_definitions_seq'), 'medikamente', 'Medikamentenallergie', 1),
       (nextval('global_definitions_seq'), 'penicillin', 'Penicillinallergie', 1),
       (nextval('global_definitions_seq'), 'sonne', 'Sonnenallergie', 1),
       (nextval('global_definitions_seq'), 'parfum', 'Duftstoffallergie', 1),
       (nextval('global_definitions_seq'), 'nickel', 'Nickelallergie', 1);

CREATE OR REPLACE VIEW participant_full_data AS
SELECT p.id                            AS participant_id,
       p.firstname                     AS participant_firstname,
       p.lastname                      AS participant_lastname,
       p.date_of_birth                 AS participant_date_of_birth,
       p.gender                        AS participant_gender,
       p.last_updated_at               AS participant_last_updated_at,
       p.household_id                  AS participant_household_id,
       h.secondary_contact_id          AS household_secondary_contact_id,
       h.secondary_contact_email       AS household_secondary_contact_email_raw,
       h.street_and_number             AS household_street_and_number,
       h.plz                           AS household_plz,
       h.place                         AS household_place,
       u.username                      AS household_primary_contact_username,
       u.email                         AS household_primary_contact_email,
       u.first_name                    AS household_primary_contact_first_name,
       u.phonenumber                   AS household_primary_contact_phone_number,
       u.last_name                     AS household_primary_contact_last_name,
       u.picture_url                   AS household_primary_contact_picture_url,
       secondary.first_name            AS household_secondary_contact_first_name,
       secondary.last_name             AS household_secondary_contact_last_name,
       secondary.email                 AS household_secondary_contact_email,
       secondary.phonenumber           AS household_secondary_contact_phone_number,
       secondary.picture_url           AS household_secondary_contact_picture_url,
       cmp.isTickVaccinated            AS camp_stats_is_tick_vaccinated,
       cmp.drugConsent                 AS camp_stats_drug_consent,
       cmp.ahv                         AS camp_stats_ahv,
       cmp.krankenkasse                AS camp_stats_krankenkasse,
       cmp.krankenkassenNr             AS camp_stats_krankenkassen_nr,
       cmp.medication                  AS camp_stats_medication,
       cmp.familyDoctor                AS camp_stats_family_doctor,
       cmp.nationality                 AS camp_stats_nationality,
       cmp.native_language             AS camp_stats_native_language,
       cmp.food_preferences            AS camp_stats_food_preferences,
       cmp.notes                       AS camp_stats_notes,
       hs.isHealthy                    AS health_stats_is_healthy,
       hs.healthy_reason               AS health_stats_healthy_reason,
       hs.excluded_activities          AS health_stats_excluded_activities
FROM participants p
         LEFT JOIN household h ON h.id = p.household_id
         LEFT JOIN users u ON u.email = h.primary_contact_id
         LEFT JOIN users secondary ON secondary.email = h.secondary_contact_id
         LEFT JOIN camp_stats cmp ON cmp.participant_id = p.id
         LEFT JOIN health_stats hs ON hs.participant_id = p.id;

CREATE OR REPLACE VIEW intolerances_full_data AS
SELECT i.custom_text      AS intolerance_selection_custom_text,
       i.severity         AS intolerance_selection_severity,
       p.id               AS participant_id,
       p.firstname        AS participant_firstname,
       p.lastname         AS participant_lastname,
       glo.id             AS global_definition_id,
       glo.label          AS global_definition_label,
       glo.definition_value AS global_definition_value,
       glo.category       AS global_definition_category
FROM IntoleranceSelection i
         JOIN participants p ON p.id = i.participant_id
         Left JOIN global_definitions glo ON glo.id = i.intolerance_id;

CREATE OR REPLACE VIEW EMERGENCY_DATA AS
SELECT cp.id                                               AS camp_participant_id,
       c.id                                                AS camp_id,
       s.id                                                AS signup_id,
       p.id                                                AS participant_id,
       p.firstname || ' ' || p.lastname                    AS participant_full_name,
       p.date_of_birth                                     AS date_of_birth,
       p.gender                                            AS gender,
       cmp.ahv                                             AS ahv,
       cmp.krankenkasse                                    AS krankenkasse,
       cmp.krankenkassenNr                                 AS krankenkassen_nr,
       cmp.medication                                      AS medication,
       cmp.familyDoctor                                    AS family_doctor,
       cmp.notes                                           AS notes,
       h.street_and_number                                 AS household_street_and_number,
       h.plz                                               AS household_plz,
       h.place                                             AS household_place,
       primary_contact.username                            AS primary_contact_username,
       primary_contact.email                               AS primary_contact_email,
       primary_contact.first_name                          AS primary_contact_first_name,
       primary_contact.last_name                           AS primary_contact_last_name,
       primary_contact.phonenumber                         AS primary_contact_phone_number,
       primary_contact.address                             AS primary_contact_address,
       primary_contact.picture_url                         AS primary_contact_picture_url,
       h.secondary_contact_id                              AS secondary_contact_id,
       h.secondary_contact_email                           AS secondary_contact_email_raw,
       secondary_contact.username                          AS secondary_contact_username,
       secondary_contact.email                             AS secondary_contact_email,
       secondary_contact.first_name                        AS secondary_contact_first_name,
       secondary_contact.last_name                         AS secondary_contact_last_name,
       secondary_contact.phonenumber                       AS secondary_contact_phone_number,
       secondary_contact.address                           AS secondary_contact_address,
       secondary_contact.picture_url                       AS secondary_contact_picture_url,
       COALESCE(
               (
                   SELECT STRING_AGG(
                                  COALESCE(cpm.medication_name, '') || ' / ' || COALESCE(cpm.dose, ''),
                                  ', ' ORDER BY cpm.id)
                   FROM camp_participant_medication cpm
                   WHERE cpm.camp_participant_id = cp.id
               ),
               'none'
       )                                                   AS camp_medication,
       COALESCE(
               (
                   SELECT STRING_AGG(
                                  TRIM(
                                          COALESCE(gd.definition_value, '') || ' ' ||
                                          COALESCE(i.custom_text, '') || ' ' ||
                                          CASE i.severity
                                              WHEN 0 THEN 'AFFECTED'
                                              WHEN 1 THEN 'STRONG'
                                              WHEN 2 THEN 'LIFE_THREATENING'
                                              ELSE ''
                                              END
                                  ),
                                  ', ' ORDER BY i.id)
                   FROM IntoleranceSelection i
                            LEFT JOIN global_definitions gd ON gd.id = i.intolerance_id
                   WHERE i.participant_id = p.id
               ),
               'none'
       )                                                   AS intolerances,
       CASE
           WHEN hs.id IS NULL THEN NULL
           WHEN hs.isHealthy THEN 'healthy'
           ELSE hs.healthy_reason
           END                                             AS health
FROM camp_participant cp
         JOIN participants p ON p.id = cp.participant_id
         JOIN signup s ON s.id = cp.signup_id
         JOIN camp c ON c.id = cp.camp_id
         LEFT JOIN camp_stats cmp ON cmp.participant_id = p.id
         LEFT JOIN household h ON h.id = s.household_id
         LEFT JOIN users primary_contact ON primary_contact.email = h.primary_contact_id
         LEFT JOIN users secondary_contact ON secondary_contact.email = h.secondary_contact_id
         LEFT JOIN health_stats hs ON hs.participant_id = p.id;
