package com.season.semiproject.spatial.slope;

/**
 * One row of the batched {@code cutSections} Mapper query (mapper-slope-section.xml): cut Chain
 * WKT {@code wkt} between {@code startFraction}/{@code endFraction} via {@code ST_LineSubstring}.
 * All SlopeSections of a whole Trail are cut in a single query (see SlopeSectionDAO#cutSections)
 * to avoid one query per Section/Chain.
 */
public final class SectionCutRequest {

    private final int idx;
    private final String wkt;
    private final double startFraction;
    private final double endFraction;

    public SectionCutRequest(int idx, String wkt, double startFraction, double endFraction) {
        this.idx = idx;
        this.wkt = wkt;
        this.startFraction = startFraction;
        this.endFraction = endFraction;
    }

    public int getIdx() {
        return idx;
    }

    public String getWkt() {
        return wkt;
    }

    public double getStartFraction() {
        return startFraction;
    }

    public double getEndFraction() {
        return endFraction;
    }
}
