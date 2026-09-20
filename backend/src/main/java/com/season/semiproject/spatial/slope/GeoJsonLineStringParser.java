package com.season.semiproject.spatial.slope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parses a PostGIS {@code ST_AsGeoJSON(geom)} LineString string into a plain coordinate list, and
 * builds WKT text back from a coordinate list for the {@code ST_LineSubstring} round-trip (see
 * SlopeSectionDAO). No JTS/geometry library is used anywhere in this project -- coordinates are
 * handled as plain {@code [lon, lat]} double pairs, consistent with how TrailGeoJsonDAO/mapper
 * already pass geometry through as GeoJSON text.
 */
public final class GeoJsonLineStringParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GeoJsonLineStringParser() {
    }

    /** Returns an unmodifiable list of {@code [lon, lat]} pairs, in the order stored in the JSON. */
    public static List<double[]> parseLineString(String geoJson) {
        try {
            JsonNode root = MAPPER.readTree(geoJson);
            JsonNode coords = root.get("coordinates");
            List<double[]> result = new ArrayList<>(coords.size());
            for (JsonNode pt : coords) {
                result.add(new double[] { pt.get(0).asDouble(), pt.get(1).asDouble() });
            }
            return Collections.unmodifiableList(result);
        } catch (Exception e) {
            throw new IllegalArgumentException("Not a valid GeoJSON LineString: " + geoJson, e);
        }
    }

    public static List<double[]> reversed(List<double[]> coordinates) {
        List<double[]> copy = new ArrayList<>(coordinates);
        Collections.reverse(copy);
        return copy;
    }

    /** Builds {@code LINESTRING(lon lat, lon lat, ...)} WKT text for {@code ST_GeomFromText}. */
    public static String toWkt(List<double[]> coordinates) {
        StringBuilder sb = new StringBuilder("LINESTRING(");
        for (int i = 0; i < coordinates.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            double[] pt = coordinates.get(i);
            sb.append(pt[0]).append(' ').append(pt[1]);
        }
        sb.append(')');
        return sb.toString();
    }
}
