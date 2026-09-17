-- PostgreSQL relational schema, restored from the historical MySQL DDL in `DB.txt` (repo root).
-- `DB.txt` is the original reference record and is never modified; this file is its PostgreSQL
-- translation. Table/column shapes are kept as close to the original as possible on purpose --
-- this Phase restores the existing schema, it does not redesign it. See
-- docs/02-migration-and-data-quality.md for the full list of conversions and known issues.
--
-- Re-run policy (dev-environment reproduction, not a production migration tool):
--   This script is destructive by design: it drops every table/function it owns (CASCADE) and
--   recreates them. Re-running it on an existing database is always safe and always yields an
--   empty schema ready for seed.sql. This is intentionally simpler than writing idempotent
--   CREATE TABLE IF NOT EXISTS + ALTER migrations for a project at this stage.
--
-- Known Limitation: `applymentee` and `mentorcontent` are referenced by
-- backend/src/main/resources/mapper/mapper-mentordetail.xml but have no CREATE TABLE in DB.txt.
-- No schema is guessed for them here; see docs/02-migration-and-data-quality.md.

DROP TRIGGER IF EXISTS trg_update_path_info_stats ON tracking_path;
DROP TRIGGER IF EXISTS trg_update_course_level_after_insert ON course_reviews;
DROP TRIGGER IF EXISTS trg_update_course_rate_after_insert ON course_reviews;
DROP TRIGGER IF EXISTS trg_update_complaint_state_after_processing_insert ON processing;
DROP TRIGGER IF EXISTS trg_complaint_set_updated_at ON complaint;

DROP FUNCTION IF EXISTS update_path_info_stats();
DROP FUNCTION IF EXISTS update_course_level_after_insert();
DROP FUNCTION IF EXISTS update_course_rate_after_insert();
DROP FUNCTION IF EXISTS update_complaint_state_after_processing_insert();
DROP FUNCTION IF EXISTS set_updated_at();

DROP TABLE IF EXISTS tracking_path CASCADE;
DROP TABLE IF EXISTS path_info CASCADE;
DROP TABLE IF EXISTS community_likes CASCADE;
DROP TABLE IF EXISTS community_comment CASCADE;
DROP TABLE IF EXISTS community CASCADE;
DROP TABLE IF EXISTS course_reviews CASCADE;
DROP TABLE IF EXISTS course CASCADE;
DROP TABLE IF EXISTS processing CASCADE;
DROP TABLE IF EXISTS complaint CASCADE;
DROP TABLE IF EXISTS userhelpcall CASCADE;
DROP TABLE IF EXISTS usernotification CASCADE;
DROP TABLE IF EXISTS userprofile CASCADE;
DROP TABLE IF EXISTS mountain CASCADE;
DROP TABLE IF EXISTS app_user CASCADE;

-- ============================================================================
-- Tables
-- ============================================================================

-- DB.txt: `create table user (...)`.
-- Renamed user -> app_user because USER is a reserved word in PostgreSQL
-- (ambiguous with the SQL-standard CURRENT_USER pseudo-value). All FKs below
-- reference app_user(user_id) instead of user(user_id).
CREATE TABLE app_user (
    user_id          VARCHAR(50) PRIMARY KEY,
    user_pw          VARCHAR(50) NOT NULL,
    user_name        VARCHAR(50) NOT NULL,
    user_nickname    VARCHAR(50),
    user_email       VARCHAR(50),
    user_age         VARCHAR(30),
    user_address     VARCHAR(30),
    user_gender      VARCHAR(30),
    user_institution VARCHAR(50)
);

-- DB.txt: `create table mountain (...)`. DOUBLE -> DOUBLE PRECISION.
CREATE TABLE mountain (
    mountain_id       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    mountain_name     VARCHAR(50),
    mountain_image    VARCHAR(50),
    mountain_location VARCHAR(100),
    mountain_content  VARCHAR(300),
    mountain_lat      DOUBLE PRECISION,
    mountain_lon      DOUBLE PRECISION
);

-- DB.txt: `create table userProfile (...)`. AUTO_INCREMENT -> IDENTITY.
CREATE TABLE userprofile (
    profile_id    INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       VARCHAR(50),
    profile_image VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES app_user (user_id) ON DELETE CASCADE
);

-- DB.txt: `create table usernotification (...)`.
CREATE TABLE usernotification (
    id        INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id   VARCHAR(30),
    fcm_token VARCHAR(255),
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES app_user (user_id) ON DELETE CASCADE
);

-- DB.txt: `create table userHelpCall (...)`.
-- Known Limitation: no MyBatis mapper reads/writes this table today
-- (HelpCall_LocationController keeps live location state in memory, not in the DB).
-- Restored anyway because it exists in the original DDL and the goal of this phase
-- is to reproduce DB.txt, not to prune it.
CREATE TABLE userhelpcall (
    id        INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id   VARCHAR(30),
    latitude  VARCHAR(50),
    longitude VARCHAR(50),
    CONSTRAINT fk_userhelpcall_user_id FOREIGN KEY (user_id) REFERENCES app_user (user_id) ON DELETE CASCADE
);

-- DB.txt: `CREATE TABLE complaint (...)`. DOUBLE -> DOUBLE PRECISION.
-- `updated_at ... ON UPDATE CURRENT_TIMESTAMP` has no PostgreSQL DDL equivalent;
-- reproduced below with the set_updated_at() trigger.
-- No FK on user_id here -- DB.txt itself never defined one for this table; preserved as-is.
CREATE TABLE complaint (
    complaint_no      INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    complaint_title   VARCHAR(50),
    complaint_content VARCHAR(700),
    complaint_type    VARCHAR(10),
    complaint_state   VARCHAR(4) DEFAULT '미처리',
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    complaint_img     VARCHAR(300),
    user_id           VARCHAR(50),
    mountain_name     VARCHAR(50),
    latitude          DOUBLE PRECISION,
    longitude         DOUBLE PRECISION,
    institution       VARCHAR(100)
);

-- DB.txt: `CREATE TABLE processing (...)`.
CREATE TABLE processing (
    processing_no           INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    processing_content      VARCHAR(200),
    processor                VARCHAR(50),
    processing_complaint_no INTEGER,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processing_img          VARCHAR(300),
    CONSTRAINT fk_complaint_no FOREIGN KEY (processing_complaint_no)
        REFERENCES complaint (complaint_no) ON DELETE CASCADE ON UPDATE CASCADE
);

-- DB.txt: `create table course (...)`.
-- Known Issue (kept as-is on purpose, not "fixed"): `mountain_id` is a free-text VARCHAR label
-- (e.g. '북한산_백운대', '북한산_둘레길11'), NOT a foreign key to mountain.mountain_id (INTEGER).
-- DB.txt itself never declared this FK. This phase restores the original structure verbatim;
-- see docs/02-migration-and-data-quality.md Known Issue list. Any redesign is out of scope
-- for Phase 2 and belongs to the Phase 4 spatial modeling work.
CREATE TABLE course (
    course_id       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_name     VARCHAR(50) NOT NULL,
    mountain_id     VARCHAR(30) NOT NULL,
    course_rate     NUMERIC(2, 1),
    course_location VARCHAR(100) NOT NULL,
    course_content  VARCHAR(100) NOT NULL,
    distance        VARCHAR(50) NOT NULL,
    duration        VARCHAR(50) NOT NULL,
    course_level    VARCHAR(50) NOT NULL,
    image           VARCHAR(255) NOT NULL,
    video           VARCHAR(255) NOT NULL,
    course_lat      NUMERIC(9, 6) NOT NULL,
    course_lon      NUMERIC(9, 6) NOT NULL
);

-- DB.txt: `CREATE TABLE course_reviews (...)`.
CREATE TABLE course_reviews (
    review_id      INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_id      INTEGER NOT NULL,
    user_id        VARCHAR(50) NOT NULL,
    review_content TEXT,
    rating         INTEGER,
    image_path     VARCHAR(255),
    difficulty     VARCHAR(50),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES course (course_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES app_user (user_id) ON DELETE CASCADE
);

-- DB.txt: `CREATE TABLE community (...)`. DATETIME -> TIMESTAMP.
CREATE TABLE community (
    community_pk       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id            VARCHAR(50) NOT NULL,
    user_nickname      VARCHAR(50),
    course_name        VARCHAR(50),
    community_url      VARCHAR(255),
    community_title    VARCHAR(100),
    community_body     TEXT,
    community_reg_date TIMESTAMP,
    latitude           NUMERIC(9, 6) NOT NULL,
    longitude          NUMERIC(9, 6) NOT NULL,
    likes              INTEGER DEFAULT 0,
    average_rating     NUMERIC(3, 2) DEFAULT 0.0,
    public_state       INTEGER,
    safe_state         INTEGER,
    FOREIGN KEY (user_id) REFERENCES app_user (user_id) ON DELETE CASCADE
);

-- DB.txt: `create table community_comment (...)`. DATETIME -> TIMESTAMP.
CREATE TABLE community_comment (
    comment_pk       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    community_pk     INTEGER,
    user_nickname    VARCHAR(50),
    user_id          VARCHAR(50),
    comment_body     VARCHAR(300),
    comment_reg_date TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES app_user (user_id) ON DELETE CASCADE,
    FOREIGN KEY (community_pk) REFERENCES community (community_pk) ON DELETE CASCADE
);

-- DB.txt: `create table community_likes (...)`.
CREATE TABLE community_likes (
    likes_id     INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      VARCHAR(50),
    community_pk INTEGER,
    FOREIGN KEY (user_id) REFERENCES app_user (user_id) ON DELETE CASCADE,
    FOREIGN KEY (community_pk) REFERENCES community (community_pk) ON DELETE CASCADE
);

-- DB.txt: `CREATE TABLE path_info (...)`. DATETIME -> TIMESTAMP.
CREATE TABLE path_info (
    id             INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        VARCHAR(50),
    start_time     TIMESTAMP,
    end_time       TIMESTAMP,
    total_distance NUMERIC(10, 2),
    path_img       VARCHAR(300),
    total_time     VARCHAR(8),
    max_altitude   NUMERIC(10, 7),
    avg_speed      REAL,
    FOREIGN KEY (user_id) REFERENCES app_user (user_id)
);

-- DB.txt: `CREATE TABLE tracking_path (...)`.
CREATE TABLE tracking_path (
    id        INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    path_id   INTEGER NOT NULL,
    latitude  NUMERIC(10, 7) NOT NULL,
    longitude NUMERIC(10, 7) NOT NULL,
    altitude  NUMERIC(10, 7) NOT NULL,
    speed     REAL NOT NULL,
    bearing   REAL NOT NULL,
    timelog   VARCHAR(20),
    FOREIGN KEY (path_id) REFERENCES path_info (id) ON DELETE CASCADE
);

-- ============================================================================
-- Triggers
--
-- MySQL's `DELIMITER // ... END // DELIMITER ;` wrapper does not exist in PostgreSQL.
-- Each trigger is split into a PL/pgSQL function (CREATE FUNCTION ... $$ ... $$) plus a
-- separate CREATE TRIGGER, using CREATE OR REPLACE TRIGGER (PostgreSQL 14+; this project
-- runs PostgreSQL 16, see docs/01-architecture-and-modeling.md). The business logic inside
-- each function is kept identical to DB.txt -- only the MySQL procedural wrapper changed.
-- ============================================================================

-- DB.txt: `updated_at ... ON UPDATE CURRENT_TIMESTAMP` on complaint (column-level MySQL clause,
-- no PostgreSQL DDL equivalent -> BEFORE UPDATE trigger).
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_complaint_set_updated_at
BEFORE UPDATE ON complaint
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- DB.txt trigger 1/4: update_complaint_state_after_processing_insert
CREATE OR REPLACE FUNCTION update_complaint_state_after_processing_insert()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE complaint
    SET complaint_state = '처리완료'
    WHERE complaint_no = NEW.processing_complaint_no;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_update_complaint_state_after_processing_insert
AFTER INSERT ON processing
FOR EACH ROW
EXECUTE FUNCTION update_complaint_state_after_processing_insert();

-- DB.txt trigger 2/4: update_course_rate_after_insert
CREATE OR REPLACE FUNCTION update_course_rate_after_insert()
RETURNS TRIGGER AS $$
DECLARE
    avg_rating NUMERIC(2, 1);
BEGIN
    SELECT AVG(rating)
    INTO avg_rating
    FROM course_reviews
    WHERE course_id = NEW.course_id;

    UPDATE course
    SET course_rate = avg_rating
    WHERE course_id = NEW.course_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_update_course_rate_after_insert
AFTER INSERT ON course_reviews
FOR EACH ROW
EXECUTE FUNCTION update_course_rate_after_insert();

-- DB.txt trigger 3/4: update_course_level_after_insert
CREATE OR REPLACE FUNCTION update_course_level_after_insert()
RETURNS TRIGGER AS $$
DECLARE
    avg_difficulty NUMERIC(2, 1);
BEGIN
    SELECT AVG(
        CASE
            WHEN difficulty = '쉬움' THEN 1
            WHEN difficulty = '보통' THEN 2
            WHEN difficulty = '어려움' THEN 3
            ELSE 0
        END
    )
    INTO avg_difficulty
    FROM course_reviews
    WHERE course_id = NEW.course_id;

    UPDATE course
    SET course_level =
        CASE
            WHEN avg_difficulty <= 1.3 THEN '쉬움'
            WHEN avg_difficulty <= 2.3 THEN '보통'
            ELSE '어려움'
        END
    WHERE course_id = NEW.course_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_update_course_level_after_insert
AFTER INSERT ON course_reviews
FOR EACH ROW
EXECUTE FUNCTION update_course_level_after_insert();

-- DB.txt trigger 4/4: update_path_info_stats
-- Window function (LEAD() OVER (PARTITION BY ... ORDER BY ...)) and the CTE structure are
-- standard SQL and needed no logic changes -- only the DELIMITER/trigger wrapper did.
CREATE OR REPLACE FUNCTION update_path_info_stats()
RETURNS TRIGGER AS $$
DECLARE
    new_total_distance DOUBLE PRECISION;
BEGIN
    WITH distance_calculations AS (
        SELECT
            path_id,
            latitude,
            longitude,
            LEAD(latitude) OVER (PARTITION BY path_id ORDER BY id) AS next_latitude,
            LEAD(longitude) OVER (PARTITION BY path_id ORDER BY id) AS next_longitude
        FROM tracking_path
    ),
    distances AS (
        SELECT
            path_id,
            6371000 * 2 * ASIN(SQRT(
                POWER(SIN(RADIANS(next_latitude - latitude) / 2), 2) +
                COS(RADIANS(latitude)) * COS(RADIANS(next_latitude)) *
                POWER(SIN(RADIANS(next_longitude - longitude) / 2), 2)
            )) / 1000 AS segment_distance
        FROM distance_calculations
        WHERE next_latitude IS NOT NULL AND next_longitude IS NOT NULL
    )
    SELECT SUM(segment_distance)
    INTO new_total_distance
    FROM distances
    WHERE path_id = NEW.path_id;

    UPDATE path_info
    SET
        avg_speed = (SELECT AVG(speed) FROM tracking_path WHERE path_id = NEW.path_id),
        max_altitude = (SELECT MAX(altitude) FROM tracking_path WHERE path_id = NEW.path_id),
        total_distance = new_total_distance
    WHERE id = NEW.path_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_update_path_info_stats
AFTER INSERT ON tracking_path
FOR EACH ROW
EXECUTE FUNCTION update_path_info_stats();
