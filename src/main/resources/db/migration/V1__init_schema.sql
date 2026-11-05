-- Eventify Database Migration: V1 Initial Schema Definition
-- Compatible with Flyway, Liquibase, and native Spring SQL initialization

CREATE TABLE IF NOT EXISTS clubs (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_CLUB',
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_token VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    registration_deadline TIMESTAMP WITH TIME ZONE NOT NULL,
    details TEXT,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    venue VARCHAR(300) NOT NULL,
    guests TEXT,
    image_link VARCHAR(255),
    allowed_departments VARCHAR(255),
    allowed_study_years VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_events_club FOREIGN KEY (club_id) REFERENCES clubs (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS registrations (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    attendee_name VARCHAR(100) NOT NULL,
    attendee_email VARCHAR(150) NOT NULL,
    roll_number VARCHAR(50),
    department VARCHAR(100),
    study_year INTEGER CHECK (study_year >= 1 AND study_year <= 5),
    is_checked_in BOOLEAN NOT NULL DEFAULT FALSE,
    registered_at TIMESTAMP WITH TIME ZONE,
    allowed_departments VARCHAR(255),
    allowed_study_years VARCHAR(255),
    CONSTRAINT fk_registrations_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT uk_registrations_event_attendee_email UNIQUE (event_id, attendee_email)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(120) NOT NULL UNIQUE,
    club_id UUID NOT NULL,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_refresh_tokens_club FOREIGN KEY (club_id) REFERENCES clubs (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_events_club_id ON events (club_id);
CREATE INDEX IF NOT EXISTS idx_registrations_event_id ON registrations (event_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens (token);
