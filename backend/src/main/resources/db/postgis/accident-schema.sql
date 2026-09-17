-- Accident Raw Spatial Layer (Phase 8). See docs/01-architecture-and-modeling.md and
-- docs/02-migration-and-data-quality.md ("Accident Raw Data Import" section) for the full
-- design rationale, and docs/05-accident-spatial-query.md for the spatial query design built
-- on top of this table.
--
-- Layering recap:
--   Original Accident Source = frontend/public/data/2023산악사고_인왕산.geojson (42 Features)
--   Accident Raw Layer       = accident_point (this file)
--   Derived Spatial Relation = ST_DWithin/ST_Distance between accident_point and trail_segment,
--                              computed at query time only (com.season.semiproject.spatial.query)
--                              -- never stored as a column here or on trail_segment.
--
-- IMPORTANT: frontend/public/data/2023산악사고_인왕산2.geojson (15 Features) is a SEPARATE
-- "Legacy UI Display Dataset". Verified during Phase 8 analysis: only 5 of its 15 Features carry
-- a real msfrtn_resc_reprt_no, one of which is reused 11 times with fabricated coordinates/type
-- values to cover all 4 trail courses for the existing marker UI (several of those fabricated
-- points sit at exactly 0.00m from a TrailSegment, which real GPS data would not do). It is
-- NEVER imported into this table and must not be joined against accident_point.
--
-- This file's DROP statement touches ONLY accident_point. It never touches trail/trail_feature/
-- trail_node/trail_segment (Raw/Network Trail layers), and is not touched by spatial-schema.sql
-- or network-schema.sql's DROP statements either -- each layer's reset is independent.
--
-- No FK to trail_segment (no trail_segment_id/nearest_segment_id column): the nearest-Segment
-- relationship was found to be highly ambiguous in this dataset (42/42 points have a
-- second-nearest Segment within 5m of the nearest one, because TrailSegment is finely
-- decomposed -- median length ~3m) and TrailSegment itself is a rebuildable Derived Network
-- Layer whose ids are not permanent identifiers. See docs/05-accident-spatial-query.md.

DROP TABLE IF EXISTS accident_point CASCADE;

-- ============================================================================
-- AccidentPoint
--
-- One row per original GeoJSON Feature (1:1, no filtering/grouping/exclusion -- all 42 Features
-- in the source file passed structural validation). Only 4 columns are projected out of the
-- original ~65 properties (report_no, dispatch_date, accident_type, location_name) because they
-- are the only ones with 0 NULLs across all 42 Features and actual use in Phase 8 spatial
-- queries; everything else (weather, fire-station timeline, etc.) is kept only in
-- `raw_properties`, not normalized into columns, to avoid over-modeling a 42-row dataset.
--
-- dispatch_date is DATE, converted from the original `dsp_ymd` integer (e.g. 20230115 ->
-- 2023-01-15) after confirming all 42 values parse as valid calendar dates. The original integer
-- value is still preserved unchanged inside raw_properties, so no information is lost.
--
-- No UNIQUE(geom): 10 groups of Features (21 of 42) legitimately share exact coordinates with a
-- DIFFERENT report_no/properties -- these are real distinct accidents, not duplicates, most
-- likely because the source system logs a nearby fixed rescue-point coordinate rather than a
-- precise GPS fix (see docs/02, "Accident Raw Data Import" section).
-- No UNIQUE(report_no): 42/42 unique within this one file, but global uniqueness across future
-- source files (other years, nationwide data) is unverified, so it is not enforced as a DB
-- constraint (and no plain index either -- unjustified at 42 rows).
-- No trail_segment_id / nearest_segment_id / distance_m / within_30m / nearby_segment_count /
-- risk_score column: all of these are Derived Spatial Relations, computed at query time (see
-- mapper-spatial-query.xml), never persisted here.
-- ============================================================================
CREATE TABLE accident_point (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_file           VARCHAR(255) NOT NULL,
    source_feature_index  INTEGER NOT NULL,
    report_no             VARCHAR(50) NOT NULL,
    dispatch_date         DATE NOT NULL,
    accident_type         VARCHAR(50) NOT NULL,
    location_name         VARCHAR(50) NOT NULL,
    geom                  geometry(Point, 4326) NOT NULL,
    raw_properties        JSONB NOT NULL,
    CONSTRAINT uq_accident_point_source UNIQUE (source_file, source_feature_index)
);

-- No GiST index on accident_point.geom -- see docs/05-accident-spatial-query.md for the
-- EXPLAIN ANALYZE results this decision is based on (42 rows; Seq Scan cost is negligible and no
-- query plan showed it would be used). Add only if a future EXPLAIN shows it would actually be
-- used by the planner at a larger data scale.
