UPDATE signup
SET state = 'COMPLETED'
WHERE state = 'REVIEWED';

UPDATE signup
SET state = 'APPROVED'
WHERE state = 'DONE';
