ALTER TABLE restaurants ADD COLUMN archived boolean NOT NULL DEFAULT false;
ALTER TABLE branches ADD COLUMN archived boolean NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN owner_suspended boolean NOT NULL DEFAULT false;
