CREATE TABLE IF NOT EXISTS events (
                                      id BIGSERIAL PRIMARY KEY,
                                      title VARCHAR(255) NOT NULL,
    event_date DATE NOT NULL,
    max_seats INTEGER NOT NULL CHECK (max_seats > 0)
    );

CREATE TABLE IF NOT EXISTS participants (
                                            id BIGSERIAL PRIMARY KEY,
                                            event_id BIGINT NOT NULL,
                                            student_name VARCHAR(255) NOT NULL,
    student_email VARCHAR(255) NOT NULL,

    CONSTRAINT fk_participants_event
    FOREIGN KEY (event_id)
    REFERENCES events(id)
    ON DELETE CASCADE,

    CONSTRAINT uq_participant_event_email
    UNIQUE (event_id, student_email)
    );

CREATE INDEX IF NOT EXISTS idx_participants_event_id
    ON participants(event_id);

CREATE INDEX IF NOT EXISTS idx_events_event_date
    ON events(event_date);