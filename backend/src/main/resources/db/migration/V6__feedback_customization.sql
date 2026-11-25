ALTER TABLE restaurants ADD COLUMN welcome_message text NOT NULL DEFAULT '';
ALTER TABLE restaurants ADD COLUMN thank_you_message text NOT NULL DEFAULT '';
ALTER TABLE restaurants ADD COLUMN collect_contact boolean NOT NULL DEFAULT true;
