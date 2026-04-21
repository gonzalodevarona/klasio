ALTER TABLE professors ADD COLUMN user_id UUID REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX ON professors(user_id);
