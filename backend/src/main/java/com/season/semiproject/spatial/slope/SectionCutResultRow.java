package com.season.semiproject.spatial.slope;

/** MyBatis result row for the batched {@code cutSections} query. */
public final class SectionCutResultRow {

    private int idx;
    private String geometryGeoJson;

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public String getGeometryGeoJson() {
        return geometryGeoJson;
    }

    public void setGeometryGeoJson(String geometryGeoJson) {
        this.geometryGeoJson = geometryGeoJson;
    }
}
