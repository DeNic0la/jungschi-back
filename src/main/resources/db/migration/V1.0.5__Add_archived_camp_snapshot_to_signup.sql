ALTER TABLE signup
    ADD COLUMN archived_camp_id VARCHAR(100);

ALTER TABLE signup
    ADD COLUMN archived_camp_title VARCHAR(255);

ALTER TABLE signup
    ADD COLUMN archived_camp_start_date DATE;

ALTER TABLE signup
    ADD COLUMN archived_camp_end_date DATE;
