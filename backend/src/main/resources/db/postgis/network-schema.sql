-- Derived Network Layer (Phase 6). See docs/01-architecture-and-modeling.md, Phase 6 section,
-- for the full design rationale (why Feature != Segment, why exact-match only in this phase,
-- why trail_id was NOT duplicated onto these tables).
--
-- Layering recap:
--   Original Source     = GeoJSON + Import Manifest (backend/src/main/resources/spatial-import/)
--   Validated Raw Layer = trail / trail_feature       (db/postgis/spatial-schema.sql)
--   Derived Network     = trail_node / trail_segment  (this file)
-- trail_node/trail_segment are entirely DERIVED from trail_feature.geom by
-- com.season.semiproject.spatial.network.NetworkBuildService (`spatial-network-build` profile).
-- They can be dropped and rebuilt at any time from the Raw layer without any loss, because they
-- never carry information that isn't already in trail_feature.
--
-- IMPORTANT: this file's DROP statements touch ONLY trail_segment/trail_node. They must NEVER
-- drop `trail_feature` or `trail` (the Raw Spatial Layer, populated by the Phase 5 importer) --
-- that is exactly why this network layer got its own file instead of being appended to
-- db/postgis/spatial-schema.sql, whose DROP section intentionally still only touches
-- trail/trail_feature. Re-running this file is a destructive reset for the Network layer only:
-- every run drops and recreates trail_segment/trail_node empty. It is NOT a non-destructive
-- migration, and it never touches Raw data.
--
-- Required order for a full from-scratch reproduction:
--   1) db/postgresql/schema.sql, 2) db/postgresql/seed.sql, 3) db/postgis/spatial-schema.sql,
--   4) Phase 5 import (spatial-import profile), 5) db/postgis/network-schema.sql,
--   6) Phase 6 network build (spatial-network-build profile).

DROP TABLE IF EXISTS trail_segment CASCADE;
DROP TABLE IF EXISTS trail_node CASCADE;

-- ============================================================================
-- TrailNode
--
-- A point where LineString Parts (decomposed from TrailFeature.geom via ST_Dump) meet, under
-- EXACT coordinate equality only (see docs/01-architecture-and-modeling.md, Phase 6, for the
-- near-endpoint tolerance experiment that was run but NOT applied in this phase -- no snapping,
-- no geometry modification). A degree-1 node is a loose end (trail terminus or a gap boundary);
-- degree-2 is a simple pass-through; this dataset has zero degree>=3 nodes under exact matching
-- (see docs for the "false branch" finding that this exact-match analysis corrected).
--
-- No `trail_id` column: a node's association with a Trail is derived transitively through the
-- segments that meet there (trail_segment -> source_trail_feature_id -> trail_feature.trail_id).
-- No cross-trail shared coordinate was found in this dataset (nearest cross-trail endpoints are
-- >270m apart), but hard-coding a single-trail-per-node assumption into the schema would be the
-- wrong model if that ever changes -- a node is purely a spatial fact, not a Trail-scoped one.
-- No `node_type` (ENDPOINT/JUNCTION/...) column either: degree is always derivable from
-- trail_segment (COUNT of segments referencing this node) and storing it risks going stale
-- across rebuilds without any current consumer that needs it precomputed.
-- ============================================================================
CREATE TABLE trail_node (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    geom geometry(Point, 4326) NOT NULL
);

CREATE INDEX idx_trail_node_geom ON trail_node USING GIST (geom);

-- ============================================================================
-- TrailSegment
--
-- From this Phase onward, TrailSegment means EXACTLY ONE THING: a single LineString Part
-- (one ST_Dump() result of a TrailFeature.geom) with its two exact endpoints resolved to
-- TrailNode rows. It is the Derived Network Edge -- it is NOT a GeoJSON Feature (see Phase 4/5
-- "Feature != Segment" finding) and its geometry is never split further in this phase, because
-- no genuine mid-line (non-endpoint) intersection was found in the actual data (see docs).
--
-- No `sequence` column: TrailFeature.sequence is a source-order artifact of the original file,
-- not a network/traversal property, and was deliberately NOT copied here (network order is a
-- graph-structural property of which segments share a node, not a linear attribute of an edge).
-- No `trail_id`: derivable via source_trail_feature_id -> trail_feature.trail_id; storing it here
-- too would duplicate data that can never disagree with its source without a bug.
-- No `direction`/`bidirectional` column: nothing in the source GeoJSON properties indicates
-- one-way restriction, so every edge is treated as bidirectionally traversable by default;
-- from_node/to_node exist only to preserve the LineString's own start/end orientation, not to
-- assert "only from_node -> to_node is walkable".
-- No `distance_m`: derivable via ST_Length(geom::geography) on demand; no current consumer
-- justifies caching it, and geometry here never changes after creation (no snapping applied),
-- so caching would only add a value that can drift from what ST_Length would recompute if
-- someone forgets to update it after a future rebuild policy change.
-- ============================================================================
CREATE TABLE trail_segment (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    from_node_id            BIGINT NOT NULL,
    to_node_id              BIGINT NOT NULL,
    geom                    geometry(LineString, 4326) NOT NULL,
    source_trail_feature_id BIGINT NOT NULL,
    source_part_index       INTEGER NOT NULL,
    CONSTRAINT fk_trail_segment_from_node
        FOREIGN KEY (from_node_id) REFERENCES trail_node (id),
    CONSTRAINT fk_trail_segment_to_node
        FOREIGN KEY (to_node_id) REFERENCES trail_node (id),
    CONSTRAINT fk_trail_segment_trail_feature
        FOREIGN KEY (source_trail_feature_id) REFERENCES trail_feature (id) ON DELETE CASCADE,
    CONSTRAINT chk_trail_segment_distinct_nodes
        CHECK (from_node_id <> to_node_id),
    CONSTRAINT uq_trail_segment_source_part
        UNIQUE (source_trail_feature_id, source_part_index)
);

CREATE INDEX idx_trail_segment_geom ON trail_segment USING GIST (geom);
CREATE INDEX idx_trail_segment_from_node ON trail_segment (from_node_id);
CREATE INDEX idx_trail_segment_to_node ON trail_segment (to_node_id);
