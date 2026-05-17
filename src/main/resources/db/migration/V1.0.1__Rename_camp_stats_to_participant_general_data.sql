CREATE SCHEMA IF NOT EXISTS reporting;

DROP VIEW IF EXISTS reporting.EMERGENCY_DATA;
DROP VIEW IF EXISTS reporting.participant_full_data;
DROP VIEW IF EXISTS EMERGENCY_DATA;
DROP VIEW IF EXISTS participant_full_data;

ALTER TABLE camp_stats RENAME TO participant_general_data;

CREATE SEQUENCE IF NOT EXISTS participant_general_data_seq START WITH 100000 INCREMENT BY 50;

ALTER TABLE participant_general_data DROP COLUMN drugConsent;
ALTER TABLE participant_general_data DROP COLUMN medication;

CREATE OR REPLACE VIEW reporting.participant_full_data AS
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
       pgd.isTickVaccinated            AS participant_general_data_is_tick_vaccinated,
       pgd.ahv                         AS participant_general_data_ahv,
       pgd.krankenkasse                AS participant_general_data_krankenkasse,
       pgd.krankenkassenNr             AS participant_general_data_krankenkassen_nr,
       pgd.familyDoctor                AS participant_general_data_family_doctor,
       pgd.nationality                 AS participant_general_data_nationality,
       pgd.native_language             AS participant_general_data_native_language,
       pgd.food_preferences            AS participant_general_data_food_preferences,
       pgd.notes                       AS participant_general_data_notes,
       hs.isHealthy                    AS health_stats_is_healthy,
       hs.healthy_reason               AS health_stats_healthy_reason,
       hs.excluded_activities          AS health_stats_excluded_activities
FROM participants p
         LEFT JOIN household h ON h.id = p.household_id
         LEFT JOIN users u ON u.email = h.primary_contact_id
         LEFT JOIN users secondary ON secondary.email = h.secondary_contact_id
         LEFT JOIN participant_general_data pgd ON pgd.participant_id = p.id
         LEFT JOIN health_stats hs ON hs.participant_id = p.id;

CREATE OR REPLACE VIEW reporting.EMERGENCY_DATA AS
SELECT cp.id                                               AS camp_participant_id,
       c.id                                                AS camp_id,
       s.id                                                AS signup_id,
       p.id                                                AS participant_id,
       p.firstname || ' ' || p.lastname                    AS participant_full_name,
       p.date_of_birth                                     AS date_of_birth,
       p.gender                                            AS gender,
       pgd.ahv                                             AS ahv,
       pgd.krankenkasse                                    AS krankenkasse,
       pgd.krankenkassenNr                                 AS krankenkassen_nr,
       pgd.familyDoctor                                    AS family_doctor,
       pgd.notes                                           AS notes,
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
         LEFT JOIN participant_general_data pgd ON pgd.participant_id = p.id
         LEFT JOIN household h ON h.id = s.household_id
         LEFT JOIN users primary_contact ON primary_contact.email = h.primary_contact_id
         LEFT JOIN users secondary_contact ON secondary_contact.email = h.secondary_contact_id
         LEFT JOIN health_stats hs ON hs.participant_id = p.id;
