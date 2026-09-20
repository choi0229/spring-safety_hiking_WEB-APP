-- Derived Analysis Layer (Phase after 12D). See docs/09-slope-section-analysis.md for the full
-- design rationale, including why this layer moved from compute-on-request to precompute +
-- persistence: SlopeSection is a deterministic function of (Network state, TrailFeature.dn_value,
-- windowMeters, the calculation rules in NetworkChainBuilder/ElevationProfile/
-- SlopeSectionCalculator) -- it never varies per request, so there is no reason to recompute it
-- on every HTTP call once the Network it derives from is stable.
--
-- Layering recap:
--   Raw Spatial Layer     = trail / trail_feature       (db/postgis/spatial-schema.sql)
--   Derived Network Layer = trail_node / trail_segment   (db/postgis/network-schema.sql)
--   Derived Analysis Layer = slope_section (this file) -- derived from the Network layer by
--     com.season.semiproject.spatial.slope.SlopeSectionBuildService, run only under the
--     `spatial-slope-build` Spring profile (SlopeSectionBuildRunner). It is NOT Raw data and NOT
--     a Network Edge -- it is a service-facing analysis unit that can be dropped and fully
--     rebuilt from trail_segment/trail_feature at any time without any loss, exactly like
--     trail_node/trail_segment can be rebuilt from trail_feature.
--
-- THIS FILE IS A DESTRUCTIVE DEV-ENVIRONMENT RESET SCRIPT, NOT A NON-DESTRUCTIVE MIGRATION --
-- same category as spatial-schema.sql/network-schema.sql. Every run unconditionally drops and
-- recreates `slope_section` empty; re-populating it requires re-running the Slope Build
-- (`spatial-slope-build` profile) afterward. This file's DROP touches ONLY slope_section -- it
-- never touches trail/trail_feature/trail_node/trail_segment/accident_point.
--
-- Required order for a full from-scratch reproduction (extends the order documented in
-- network-schema.sql):
--   1) db/postgresql/schema.sql, 2) db/postgresql/seed.sql, 3) db/postgis/spatial-schema.sql,
--   4) Phase 5 import (spatial-import profile), 5) db/postgis/network-schema.sql,
--   6) Phase 6 network build (spatial-network-build profile),
--   7) db/postgis/slope-section-schema.sql (this file),
--   8) Slope build (spatial-slope-build profile) -- populates slope_section.
-- Slope Build MUST run after Network Build -- it reads trail_segment/trail_node, so a stale or
-- missing Network produces a stale or missing Analysis Layer. There is no automatic dependency
-- tracking between these steps; the order above must be followed manually (see docs/09).

DROP TABLE IF EXISTS slope_section CASCADE;

-- ============================================================================
-- SlopeSection (Derived Analysis Layer)
--
-- One row = one 20m fixed-distance analysis unit along a branch-free NetworkChain (see
-- com.season.semiproject.spatial.slope.NetworkChainBuilder/ElevationProfile/
-- SlopeSectionCalculator for the exact construction rules -- unchanged by this persistence
-- work). A SlopeSection is NOT a TrailSegment: it commonly spans several TrailSegments (median
-- TrailSegment length is ~3m, far shorter than 20m), so there is deliberately no
-- trail_segment_id column here -- a single-Segment FK could never represent a SlopeSection's
-- real provenance across multiple Segments, and a full N:M provenance table
-- (slope_section_segment) was considered and rejected as unneeded complexity for a Derived
-- Layer that is always fully rebuilt from trail_segment/trail_feature together, never edited
-- in place (see docs/09).
--
-- chain_sequence/section_sequence are NOT database identity values -- they are the deterministic
-- ordering NetworkChainBuilder/SlopeSectionCalculator already assign on every rebuild (chain
-- traversal order is fixed by TrailNode/TrailSegment id comparisons, section order is fixed by
-- position along the chain), copied here as plain columns so API responses can be ordered
-- without depending on insertion order or the `id` identity column.
--
-- window_m is CHECKed to always be 20: Production only ever consumes the 20m analysis unit (see
-- docs/09, "why 20m"); the 10m/30m candidates compared during Phase 12A-12C remain available
-- only as pure in-memory calculations (SlopeSectionCalculator, exercised directly by its unit
-- tests) and are deliberately never persisted here, to avoid a table that silently mixes several
-- different analysis granularities under one schema.
--
-- No `estimated_elevation_source` column: that string is a fixed, unconditional literal
-- (see SlopeSectionFeatureCollection) describing the whole model's provenance, not a per-row
-- fact -- storing it per row would only duplicate a constant.
-- No `analysis_version`/`model_version` column: exactly one slope model exists today and this
-- table is always fully dropped and rebuilt as a whole, so there is nothing yet for a version
-- column to distinguish.
-- ============================================================================
CREATE TABLE slope_section (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trail_id                    BIGINT NOT NULL,
    chain_sequence              INTEGER NOT NULL,
    section_sequence            INTEGER NOT NULL,
    window_m                    INTEGER NOT NULL,
    distance_m                  DOUBLE PRECISION NOT NULL,
    estimated_elevation_start   DOUBLE PRECISION NOT NULL,
    estimated_elevation_end     DOUBLE PRECISION NOT NULL,
    estimated_elevation_delta   DOUBLE PRECISION NOT NULL,
    estimated_slope_percent     DOUBLE PRECISION NOT NULL,
    data_quality_flag           VARCHAR(20),
    geom                        geometry(LineString, 4326) NOT NULL,
    generated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_slope_section_trail
        FOREIGN KEY (trail_id) REFERENCES trail (id) ON DELETE CASCADE,
    CONSTRAINT chk_slope_section_window_m
        CHECK (window_m = 20),
    CONSTRAINT chk_slope_section_distance_positive
        CHECK (distance_m > 0),
    CONSTRAINT uq_slope_section_sequence
        UNIQUE (trail_id, window_m, chain_sequence, section_sequence)
);

-- Covers the only query pattern the Production API actually uses today (WHERE trail_id = ? AND
-- window_m = ? ORDER BY chain_sequence, section_sequence) -- see mapper-slope-section.xml,
-- findPersistedSections. No GiST index on `geom`: the Production API never filters/sorts by
-- geometry, only serializes it (ST_AsGeoJSON) for rows already selected by trail_id -- adding a
-- spatial index with no query that uses it would be exactly the kind of speculative indexing
-- this project has avoided elsewhere (see docs/05-accident-spatial-query.md, Q4).
CREATE INDEX idx_slope_section_trail_window_order
    ON slope_section (trail_id, window_m, chain_sequence, section_sequence);
