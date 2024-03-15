ALTER TABLE events
    ADD COLUMN user_id BIGINT;
ALTER TABLE events
    ADD CONSTRAINT fk_events_users FOREIGN KEY (user_id) REFERENCES users (id);