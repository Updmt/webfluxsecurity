CREATE TABLE files
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255),
    created   TIMESTAMP,
    deleted   BOOLEAN
);

CREATE TABLE events
(
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    created TIMESTAMP,
    file_id BIGINT
);

ALTER TABLE events ADD CONSTRAINT fk_events_files FOREIGN KEY (file_id) REFERENCES files (id);

