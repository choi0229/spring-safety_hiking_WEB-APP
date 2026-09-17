package com.season.semiproject.spatial.accident;

/** Summary of one import run, returned for logging by AccidentImportRunner. */
public class AccidentImportResult {

    public final String sourceFile;
    public final int totalFeaturesInFile;
    public final int importedCount;

    public AccidentImportResult(String sourceFile, int totalFeaturesInFile, int importedCount) {
        this.sourceFile = sourceFile;
        this.totalFeaturesInFile = totalFeaturesInFile;
        this.importedCount = importedCount;
    }

    @Override
    public String toString() {
        return "AccidentImportResult{sourceFile=" + sourceFile
                + ", totalFeaturesInFile=" + totalFeaturesInFile
                + ", importedCount=" + importedCount + "}";
    }
}
