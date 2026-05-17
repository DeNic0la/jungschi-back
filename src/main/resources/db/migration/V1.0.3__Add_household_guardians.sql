CREATE SEQUENCE IF NOT EXISTS household_guardian_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS household_guardian
(
    id           BIGINT       NOT NULL,
    household_id BIGINT       NOT NULL,
    user_email   VARCHAR(255),
    email        VARCHAR(255) NOT NULL,
    contact_type VARCHAR(32)  NOT NULL,
    CONSTRAINT pk_household_guardian PRIMARY KEY (id),
    CONSTRAINT uc_household_guardian_email UNIQUE (email),
    CONSTRAINT FK_HOUSEHOLD_GUARDIAN_ON_HOUSEHOLD FOREIGN KEY (household_id) REFERENCES household (id),
    CONSTRAINT FK_HOUSEHOLD_GUARDIAN_ON_USER FOREIGN KEY (user_email) REFERENCES users (email)
);

INSERT INTO household_guardian (id, household_id, user_email, email, contact_type)
SELECT nextval('household_guardian_seq'), id, primary_contact_id, primary_contact_id, 'PRIMARY'
FROM household
WHERE primary_contact_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM household_guardian hg WHERE hg.email = household.primary_contact_id
  );

INSERT INTO household_guardian (id, household_id, user_email, email, contact_type)
SELECT nextval('household_guardian_seq'), id, secondary_contact_id, secondary_contact_id, 'SECONDARY'
FROM household
WHERE secondary_contact_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM household_guardian hg WHERE hg.email = household.secondary_contact_id
  );

INSERT INTO household_guardian (id, household_id, user_email, email, contact_type)
SELECT nextval('household_guardian_seq'), id, NULL, LOWER(secondary_contact_email), 'PENDING'
FROM household
WHERE secondary_contact_email IS NOT NULL
  AND secondary_contact_email <> ''
  AND NOT EXISTS (
      SELECT 1 FROM household_guardian hg WHERE hg.email = LOWER(household.secondary_contact_email)
  );
