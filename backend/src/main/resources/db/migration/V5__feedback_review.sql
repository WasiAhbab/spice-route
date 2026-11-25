ALTER TABLE feedback ADD COLUMN review_status text NOT NULL DEFAULT 'NEW' CHECK (review_status IN ('NEW','IN_REVIEW','IN_PROGRESS','RESOLVED'));
ALTER TABLE feedback ADD COLUMN internal_note text NOT NULL DEFAULT '';
ALTER TABLE feedback ADD COLUMN version bigint NOT NULL DEFAULT 0;
