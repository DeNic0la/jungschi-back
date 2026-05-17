-- Dev-only mock data for the local PostgreSQL database.
-- This is intentionally not a Flyway migration. Run after Flyway initialized the schema:
--   docker exec -i jungschi-postgres psql -U jungschi -d jungschi < backend/DEV_MOCK_DATA.sql

\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF to_regclass('public.users') IS NULL THEN
        RAISE EXCEPTION 'Application schema is missing. Start the backend once so Flyway can create it, then rerun this file.';
    END IF;
END $$;

-- Recreate only dev mock rows from the reserved 900000 id range and mock emails.
DELETE FROM camp_participant_medication
WHERE id BETWEEN 900000 AND 900999
   OR camp_participant_id BETWEEN 900000 AND 900999;

DELETE FROM room_leader_assignment
WHERE room_id BETWEEN 900000 AND 900999
   OR user_email IN (
        'guardian@example.com',
        'guardian.secondary@example.com',
        'parent.mueller@example.com',
        'parent.mueller.secondary@example.com',
        'parent.steiner@example.com',
        'parent.steiner.secondary@example.com',
        'parent.keller@example.com',
        'parent.keller.secondary@example.com',
        'parent.frei@example.com',
        'team@example.com',
        'team2@example.com',
        'sanitaet@example.com'
   );

DELETE FROM camp_participant
WHERE id BETWEEN 900000 AND 900999
   OR participant_id BETWEEN 900000 AND 900999
   OR signup_id BETWEEN 900000 AND 900999
   OR room_id BETWEEN 900000 AND 900999;

DELETE FROM signup
WHERE id BETWEEN 900000 AND 900999
   OR household_id BETWEEN 900000 AND 900999
   OR camp_id IN ('sela-2026', 'weekend-2026', 'pfila-2027');

DELETE FROM room
WHERE id BETWEEN 900000 AND 900999
   OR camp_id IN ('sela-2026', 'weekend-2026', 'pfila-2027');

DELETE FROM IntoleranceSelection
WHERE id BETWEEN 900000 AND 900999
   OR participant_id BETWEEN 900000 AND 900999;

DELETE FROM camp_stats
WHERE id BETWEEN 900000 AND 900999
   OR participant_id BETWEEN 900000 AND 900999;

DELETE FROM health_stats
WHERE id BETWEEN 900000 AND 900999
   OR participant_id BETWEEN 900000 AND 900999;

DELETE FROM participants
WHERE id BETWEEN 900000 AND 900999
   OR household_id BETWEEN 900000 AND 900999;

DELETE FROM household
WHERE id BETWEEN 900000 AND 900999
   OR primary_contact_id IN (
        'guardian@example.com',
        'parent.mueller@example.com',
        'parent.steiner@example.com',
        'parent.keller@example.com',
        'parent.frei@example.com'
   );

DELETE FROM camp
WHERE id IN ('sela-2026', 'weekend-2026', 'pfila-2027');

DELETE FROM users
WHERE email IN (
    'guardian@example.com',
    'guardian.secondary@example.com',
    'parent.mueller@example.com',
    'parent.mueller.secondary@example.com',
    'parent.steiner@example.com',
    'parent.steiner.secondary@example.com',
    'parent.keller@example.com',
    'parent.keller.secondary@example.com',
    'parent.frei@example.com',
    'team@example.com',
    'team2@example.com',
    'sanitaet@example.com'
);

INSERT INTO users (
    email, username, first_name, phonenumber, last_name,
    address, picture_url, roles, openid_connect_data, created_at, last_seen_at
)
VALUES
    ('admin@example.com', 'admin', 'Ada', '+41 76 000 00 01', 'Admin', 'Jungschihuette 1, 8000 Zuerich', NULL, 'ADMIN,Jungschiteam,Sanitaet,guardian', '{}', now(), now()),
    ('guardian@example.com', 'guardian', 'Gabi', '+41 79 111 22 33', 'Beispiel', 'Waldweg 12, 8000 Zuerich', NULL, 'guardian', '{}', now(), now()),
    ('guardian.secondary@example.com', 'guardian-secondary', 'Marco', '+41 79 222 33 44', 'Beispiel', 'Waldweg 12, 8000 Zuerich', NULL, 'guardian', '{}', now(), now()),
    ('parent.mueller@example.com', 'parent-mueller', 'Nora', '+41 78 333 44 55', 'Mueller', 'Bachstrasse 4, 8050 Zuerich', NULL, 'guardian', '{}', now(), now()),
    ('parent.mueller.secondary@example.com', 'parent-mueller-secondary', 'Felix', '+41 78 444 55 66', 'Mueller', 'Bachstrasse 4, 8050 Zuerich', NULL, 'guardian', '{}', now(), now()),
    ('parent.steiner@example.com', 'parent-steiner', 'Sarah', '+41 77 123 45 67', 'Steiner', 'Hofmatt 8, 8048 Zuerich', NULL, 'guardian', '{}', now(), now()),
    ('parent.steiner.secondary@example.com', 'parent-steiner-secondary', 'Jonas', '+41 77 234 56 78', 'Steiner', 'Hofmatt 8, 8048 Zuerich', NULL, 'guardian', '{}', now(), now()),
    ('parent.keller@example.com', 'parent-keller', 'Iris', '+41 76 345 67 89', 'Keller', 'Rebweg 17, 8038 Zuerich', NULL, 'guardian', '{}', now(), now()),
    ('parent.keller.secondary@example.com', 'parent-keller-secondary', 'David', '+41 76 456 78 90', 'Keller', 'Rebweg 17, 8038 Zuerich', NULL, 'guardian', '{}', now(), now()),
    ('parent.frei@example.com', 'parent-frei', 'Claudia', '+41 75 567 89 01', 'Frei', 'Seestrasse 21, 8802 Kilchberg', NULL, 'guardian', '{}', now(), now()),
    ('team@example.com', 'team', 'Tina', '+41 77 555 66 77', 'Team', 'Jungschihuette 1, 8000 Zuerich', NULL, 'guardian,Jungschiteam', '{}', now(), now()),
    ('team2@example.com', 'team2', 'Luca', '+41 77 888 99 00', 'Leitung', 'Jungschihuette 1, 8000 Zuerich', NULL, 'guardian,Jungschiteam', '{}', now(), now()),
    ('sanitaet@example.com', 'sanitaet', 'Sami', '+41 76 666 77 88', 'Sanitaet', 'Jungschihuette 1, 8000 Zuerich', NULL, 'guardian,Jungschiteam,Sanitaet', '{}', now(), now())
ON CONFLICT (email) DO UPDATE SET
    username = EXCLUDED.username,
    first_name = EXCLUDED.first_name,
    phonenumber = EXCLUDED.phonenumber,
    last_name = EXCLUDED.last_name,
    address = EXCLUDED.address,
    picture_url = EXCLUDED.picture_url,
    roles = EXCLUDED.roles,
    openid_connect_data = EXCLUDED.openid_connect_data,
    last_seen_at = now();

INSERT INTO household (
    id, primary_contact_id, secondary_contact_id, secondary_contact_email,
    street_and_number, plz, place
)
VALUES
    (900001, 'guardian@example.com', 'guardian.secondary@example.com', 'guardian.secondary@example.com', 'Waldweg 12', '8000', 'Zuerich'),
    (900002, 'parent.mueller@example.com', 'parent.mueller.secondary@example.com', 'parent.mueller.secondary@example.com', 'Bachstrasse 4', '8050', 'Zuerich'),
    (900003, 'parent.steiner@example.com', 'parent.steiner.secondary@example.com', 'parent.steiner.secondary@example.com', 'Hofmatt 8', '8048', 'Zuerich'),
    (900004, 'parent.keller@example.com', 'parent.keller.secondary@example.com', 'parent.keller.secondary@example.com', 'Rebweg 17', '8038', 'Zuerich'),
    (900005, 'parent.frei@example.com', NULL, 'grossmutter.frei@example.com', 'Seestrasse 21', '8802', 'Kilchberg');

INSERT INTO participants (
    id, firstname, lastname, date_of_birth, gender, last_updated_at, household_id
)
VALUES
    (900001, 'Tim', 'Beispiel', DATE '2014-03-18', 'MALE', now(), 900001),
    (900002, 'Lea', 'Beispiel', DATE '2016-09-02', 'FEMALE', now(), 900001),
    (900003, 'Noah', 'Mueller', DATE '2013-11-27', 'MALE', now(), 900002),
    (900004, 'Mia', 'Mueller', DATE '2017-05-09', 'FEMALE', now(), 900002),
    (900005, 'Elin', 'Steiner', DATE '2012-02-14', 'FEMALE', now(), 900003),
    (900006, 'Lio', 'Steiner', DATE '2015-08-23', 'MALE', now(), 900003),
    (900007, 'Anja', 'Keller', DATE '2014-12-01', 'FEMALE', now(), 900004),
    (900008, 'Ben', 'Keller', DATE '2018-01-30', 'MALE', now(), 900004),
    (900009, 'Sven', 'Frei', DATE '2011-06-11', 'MALE', now(), 900005),
    (900010, 'Nina', 'Frei', DATE '2016-04-19', 'FEMALE', now(), 900005),
    (900011, 'Kim', 'Beispiel', DATE '2018-10-05', 'ELSE', now(), 900001),
    (900012, 'Joel', 'Mueller', DATE '2019-07-22', 'MALE', now(), 900002);

INSERT INTO health_stats (
    id, participant_id, isHealthy, healthy_reason, excluded_activities
)
VALUES
    (900001, 900001, TRUE, NULL, NULL),
    (900002, 900002, FALSE, 'Asthma bei starker Belastung', 'lange Ausdauerlaeufe'),
    (900003, 900003, TRUE, NULL, NULL),
    (900004, 900004, FALSE, 'Knie nach Sportverletzung schonen', 'Kontaktsport'),
    (900005, 900005, TRUE, NULL, NULL),
    (900006, 900006, FALSE, 'Heuschnupfen im Fruehling', 'Wiesenposten im Mai'),
    (900007, 900007, TRUE, NULL, NULL),
    (900008, 900008, TRUE, NULL, NULL),
    (900009, 900009, FALSE, 'Migraene bei Schlafmangel', 'Nachtwachen'),
    (900010, 900010, TRUE, NULL, NULL),
    (900011, 900011, TRUE, NULL, NULL),
    (900012, 900012, FALSE, 'Braucht regelmaessige Pausen', 'lange Wanderungen');

INSERT INTO camp_stats (
    id, participant_id, isTickVaccinated, drugConsent, ahv, krankenkasse,
    krankenkassenNr, medication, familyDoctor, nationality, native_language,
    food_preferences, notes
)
VALUES
    (900001, 900001, TRUE, TRUE, '756.1234.5678.97', 'CSS', 'CSS-10001', 'keine', 'Dr. Meier, Zuerich', 'CH', 'Deutsch', 'isst vegetarisch mit', 'Schwimmt sicher.'),
    (900002, 900002, FALSE, FALSE, '756.2234.5678.12', 'Swica', 'SW-20002', 'Salbutamol Spray bei Bedarf', 'Dr. Keller, Zuerich', 'CH', 'Deutsch', 'kein Schweinefleisch', 'Inhalator im Tagesrucksack.'),
    (900003, 900003, TRUE, TRUE, '756.3234.5678.34', 'Helsana', 'HE-30003', 'keine', 'Dr. Baumann, Oerlikon', 'CH', 'Deutsch', 'alles ok', 'Braucht nachts manchmal Licht.'),
    (900004, 900004, TRUE, TRUE, '756.4234.5678.56', 'Concordia', 'CO-40004', 'Ibuprofen nur nach Ruecksprache', 'Dr. Frei, Zuerich', 'CH', 'Deutsch', 'mag keine Pilze', 'Kniebandage fuer Wanderung dabei.'),
    (900005, 900005, TRUE, TRUE, '756.5234.5678.78', 'Assura', 'AS-50005', 'keine', 'Dr. Ott, Zuerich', 'CH', 'Deutsch', 'vegan', 'Kann gut Karten lesen.'),
    (900006, 900006, FALSE, TRUE, '756.6234.5678.90', 'KPT', 'KPT-60006', 'Antihistaminikum bei Bedarf', 'Dr. Ott, Zuerich', 'CH', 'Deutsch', 'keine rohen Tomaten', 'Heuschnupfen beachten.'),
    (900007, 900007, TRUE, TRUE, '756.7234.5678.11', 'Visana', 'VI-70007', 'keine', 'Dr. Schmid, Wollishofen', 'CH', 'Deutsch', 'glutenfrei', 'Hilft gern in der Kueche.'),
    (900008, 900008, TRUE, FALSE, '756.8234.5678.22', 'Visana', 'VI-80008', 'keine', 'Dr. Schmid, Wollishofen', 'CH', 'Deutsch', 'isst wenig scharf', 'Noch etwas Heimweh.'),
    (900009, 900009, TRUE, TRUE, '756.9234.5678.33', 'Sanitas', 'SA-90009', 'Paracetamol nach Ruecksprache', 'Dr. Hartmann, Kilchberg', 'CH', 'Deutsch', 'normal', 'Schlafrhythmus beachten.'),
    (900010, 900010, FALSE, TRUE, '756.1034.5678.44', 'Sanitas', 'SA-10010', 'keine', 'Dr. Hartmann, Kilchberg', 'CH', 'Deutsch', 'laktosefrei', 'Braucht Brille beim Lesen.'),
    (900011, 900011, TRUE, TRUE, '756.1134.5678.55', 'CSS', 'CSS-11011', 'keine', 'Dr. Meier, Zuerich', 'CH', 'Deutsch', 'kleine Portionen', 'Neues Jungschi-Kind.'),
    (900012, 900012, FALSE, FALSE, '756.1234.5678.66', 'Helsana', 'HE-12012', 'keine', 'Dr. Baumann, Oerlikon', 'CH', 'Deutsch', 'mag Reisgerichte', 'Pausen auf Wanderungen einplanen.');

INSERT INTO IntoleranceSelection (
    id, participant_id, intolerance_id, custom_text, severity
)
VALUES
    (900001, 900002, (SELECT id FROM global_definitions WHERE label = 'pollen' LIMIT 1), NULL, 0),
    (900002, 900002, (SELECT id FROM global_definitions WHERE label = 'laktose' LIMIT 1), NULL, 1),
    (900003, 900003, (SELECT id FROM global_definitions WHERE label = 'erdnuss' LIMIT 1), 'Spuren vermeiden', 2),
    (900004, 900004, (SELECT id FROM global_definitions WHERE label = 'gluten' LIMIT 1), NULL, 1),
    (900005, 900006, (SELECT id FROM global_definitions WHERE label = 'pollen' LIMIT 1), 'Birke und Graeser', 1),
    (900006, 900007, (SELECT id FROM global_definitions WHERE label = 'gluten' LIMIT 1), NULL, 2),
    (900007, 900008, (SELECT id FROM global_definitions WHERE label = 'ei' LIMIT 1), NULL, 0),
    (900008, 900009, (SELECT id FROM global_definitions WHERE label = 'medikamente' LIMIT 1), 'Aspirin vermeiden', 1),
    (900009, 900010, (SELECT id FROM global_definitions WHERE label = 'laktose' LIMIT 1), NULL, 1),
    (900010, 900012, (SELECT id FROM global_definitions WHERE label = 'hausstaub' LIMIT 1), NULL, 0);

INSERT INTO camp (
    id, title, description, start_date, end_date, signup_enddate,
    is_jugend_und_sport, price_first, price_second, price_third
)
VALUES
    ('sela-2026', 'Sommerlager 2026', 'Mock Sommerlager fuer lokale Entwicklung und Tests.', DATE '2026-07-12', DATE '2026-07-19', DATE '2026-06-15', TRUE, 240.00, 210.00, 180.00),
    ('weekend-2026', 'Herbstweekend 2026', 'Kurzes Beispielweekend fuer Anmeldungen und Zimmerlisten.', DATE '2026-10-03', DATE '2026-10-04', DATE '2026-09-20', FALSE, 60.00, 50.00, 45.00),
    ('pfila-2027', 'Pfingstlager 2027', 'Zukuenftiges Mock-Lager fuer Planungsansichten.', DATE '2027-05-22', DATE '2027-05-24', DATE '2027-04-30', TRUE, 110.00, 95.00, 80.00);

INSERT INTO room (
    id, camp_id, name, max_capacity, gender
)
VALUES
    (900001, 'sela-2026', 'Fuchs', 8, 'MALE'),
    (900002, 'sela-2026', 'Adler', 8, 'FEMALE'),
    (900003, 'sela-2026', 'Dachs', 6, 'ELSE'),
    (900004, 'sela-2026', 'Leitung', 4, 'ELSE'),
    (900005, 'weekend-2026', 'Gruppenraum A', 18, 'ELSE'),
    (900006, 'weekend-2026', 'Gruppenraum B', 18, 'ELSE'),
    (900007, 'pfila-2027', 'Zelt Rot', 10, 'MALE'),
    (900008, 'pfila-2027', 'Zelt Blau', 10, 'FEMALE');

INSERT INTO room_leader_assignment (room_id, user_email)
VALUES
    (900001, 'team@example.com'),
    (900002, 'team2@example.com'),
    (900003, 'admin@example.com'),
    (900004, 'sanitaet@example.com'),
    (900005, 'team@example.com'),
    (900006, 'team2@example.com'),
    (900007, 'team@example.com'),
    (900008, 'sanitaet@example.com');

INSERT INTO signup (
    id, household_id, camp_id, state, feedback, photo_consent,
    info_email, additional_contact_options_during_camp
)
VALUES
    (900001, 900001, 'sela-2026', 'REVIEWED', 'Freuen uns auf das Lager.', TRUE, TRUE, 'Grosseltern: +41 79 777 88 99'),
    (900002, 900002, 'sela-2026', 'COMPLETED', 'Bitte Allergiehinweise beachten.', TRUE, TRUE, 'Nachbarin erreichbar am Abend.'),
    (900003, 900003, 'sela-2026', 'IN_PROGRESS', 'Noch offen wegen Schultermin.', FALSE, TRUE, NULL),
    (900004, 900004, 'sela-2026', 'REVIEWED', 'Anreise mit Velo moeglich.', TRUE, FALSE, 'Vater am Arbeitsplatz erreichbar.'),
    (900005, 900005, 'sela-2026', 'COMPLETED', 'Ein Kind kommt direkt ab Schule.', TRUE, TRUE, 'Grossmutter als Backupkontakt.'),
    (900006, 900001, 'weekend-2026', 'IN_PROGRESS', NULL, FALSE, TRUE, NULL),
    (900007, 900002, 'weekend-2026', 'REVIEWED', 'Noah kommt nur Samstag.', TRUE, TRUE, NULL),
    (900008, 900003, 'pfila-2027', 'IN_PROGRESS', 'Fruehe Planung testen.', TRUE, TRUE, NULL),
    (900009, 900004, 'pfila-2027', 'IN_PROGRESS', 'Noch nicht definitiv.', FALSE, FALSE, NULL);

INSERT INTO camp_participant (
    id, participant_id, signup_id, camp_id, room_id, school_class,
    infos_zimmerleitung, bemerkungen
)
VALUES
    (900001, 900001, 900001, 'sela-2026', 900001, '5. Klasse', 'Kennt viele Kinder, hilft gerne beim Aufbau.', 'Vegetarische Option beachten.'),
    (900002, 900002, 900001, 'sela-2026', 900002, '3. Klasse', 'Asthmahinweis sichtbar fuer Leitung.', 'Inhalator mitfuehren.'),
    (900003, 900011, 900001, 'sela-2026', 900003, '1. Klasse', 'Neues Jungschi-Kind, braucht Bezugsperson.', 'Kleine Portionen.'),
    (900004, 900003, 900002, 'sela-2026', 900001, '6. Klasse', 'Erdnussallergie klar markieren.', 'Notfallkontakt direkt informieren.'),
    (900005, 900004, 900002, 'sela-2026', 900002, '2. Klasse', 'Knie schonen bei Gelaendespielen.', 'Kniebandage vorhanden.'),
    (900006, 900012, 900002, 'sela-2026', 900003, '1. Kindergarten', 'Pausen einplanen.', 'Frueh ins Bett.'),
    (900007, 900005, 900003, 'sela-2026', 900002, '1. Sek', 'Kann Verantwortung fuer Material uebernehmen.', 'Vegan planen.'),
    (900008, 900006, 900003, 'sela-2026', 900001, '4. Klasse', 'Heuschnupfen beachten.', 'Antihistaminikum im Sanitaetsbeutel.'),
    (900009, 900007, 900004, 'sela-2026', 900002, '5. Klasse', 'Glutenfrei strikt beachten.', 'Eigene Snacks dabei.'),
    (900010, 900008, 900004, 'sela-2026', 900001, '2. Klasse', 'Braucht klare Tagesstruktur.', 'Kein Ei zum Fruehstueck.'),
    (900011, 900009, 900005, 'sela-2026', 900001, '2. Sek', 'Nachtwachen vermeiden.', 'Migraenehinweis.'),
    (900012, 900010, 900005, 'sela-2026', 900002, '3. Klasse', 'Brille nicht vergessen.', 'Laktosefrei.'),
    (900013, 900001, 900006, 'weekend-2026', 900005, '5. Klasse', 'Weekend-Testdatensatz.', NULL),
    (900014, 900002, 900006, 'weekend-2026', 900006, '3. Klasse', 'Weekend-Testdatensatz.', NULL),
    (900015, 900003, 900007, 'weekend-2026', 900005, '6. Klasse', 'Kommt nur Samstag.', 'Erdnussallergie weiterhin beachten.'),
    (900016, 900004, 900007, 'weekend-2026', 900006, '2. Klasse', 'Knie schonen.', NULL),
    (900017, 900005, 900008, 'pfila-2027', 900008, '1. Sek', 'Planungsansicht.', NULL),
    (900018, 900006, 900008, 'pfila-2027', 900007, '4. Klasse', 'Planungsansicht.', NULL),
    (900019, 900007, 900009, 'pfila-2027', 900008, '5. Klasse', 'Planungsansicht.', NULL),
    (900020, 900008, 900009, 'pfila-2027', 900007, '2. Klasse', 'Planungsansicht.', NULL);

INSERT INTO camp_participant_medication (
    id, camp_participant_id, medication_name, dose, frequency,
    purpose, needs_help, confidential
)
VALUES
    (900001, 900002, 'Salbutamol', '2 Huebe', 'bei Bedarf', 'Asthma', TRUE, FALSE),
    (900002, 900004, 'Notfallset Allergie', 'gemaess Packung', 'bei allergischer Reaktion', 'Erdnussallergie', TRUE, TRUE),
    (900003, 900005, 'Ibuprofen', '200 mg', 'nur nach Ruecksprache', 'Knieschmerzen', TRUE, TRUE),
    (900004, 900008, 'Cetirizin', '10 mg', 'abends bei Bedarf', 'Heuschnupfen', TRUE, FALSE),
    (900005, 900009, 'Glutenfreie Snacks', 'nach Bedarf', 'zu Mahlzeiten', 'Zoeliakie', FALSE, FALSE),
    (900006, 900011, 'Paracetamol', '500 mg', 'nur nach Ruecksprache', 'Migraene', TRUE, TRUE),
    (900007, 900014, 'Salbutamol', '2 Huebe', 'bei Bedarf', 'Asthma', TRUE, FALSE),
    (900008, 900018, 'Cetirizin', '10 mg', 'abends bei Bedarf', 'Heuschnupfen', TRUE, FALSE);

SELECT setval('household_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM household), (SELECT last_value FROM household_seq)), true);
SELECT setval('participants_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM participants), (SELECT last_value FROM participants_seq)), true);
SELECT setval('health_stats_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM health_stats), (SELECT last_value FROM health_stats_seq)), true);
SELECT setval('camp_stats_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM camp_stats), (SELECT last_value FROM camp_stats_seq)), true);
SELECT setval('IntoleranceSelection_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM IntoleranceSelection), (SELECT last_value FROM IntoleranceSelection_seq)), true);
SELECT setval('room_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM room), (SELECT last_value FROM room_seq)), true);
SELECT setval('signup_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM signup), (SELECT last_value FROM signup_seq)), true);
SELECT setval('camp_participant_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM camp_participant), (SELECT last_value FROM camp_participant_seq)), true);
SELECT setval('camp_participant_medication_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM camp_participant_medication), (SELECT last_value FROM camp_participant_medication_seq)), true);

COMMIT;
