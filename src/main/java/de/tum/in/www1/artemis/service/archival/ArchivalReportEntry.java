package de.tum.in.www1.artemis.service.archival;

import java.util.OptionalLong;

public class ArchivalReportEntry {

    private final OptionalLong exerciseId;

    private final String exerciseName;

    private final int totalEntries;

    private final int successfulEntries;

    private final int skippedEntries;

    private final int failedEntries;

    /**
     * Generates a new entry with the given data
     *
     * @param exerciseId the id or a negative number if no id is applicable
     * @param skippedEntries Entries that did not get exported but did not fail either, e.g. because a participation did not have any submission
     */
    public ArchivalReportEntry(long exerciseId, String exerciseName, int totalEntries, int successfulEntries, int skippedEntries, int failedEntries) {
        this.exerciseId = exerciseId >= 0 ? OptionalLong.of(exerciseId) : OptionalLong.empty();
        this.exerciseName = exerciseName;
        this.totalEntries = totalEntries;
        this.successfulEntries = successfulEntries;
        this.skippedEntries = skippedEntries;
        this.failedEntries = failedEntries;
    }

    /**
     * Shortcut for {@link ArchivalReportEntry} but calculates the missing value
     */
    public ArchivalReportEntry(long exerciseId, String exerciseName, int totalEntries, int successfulEntries, int skippedEntries) {
        this(exerciseId, exerciseName, totalEntries, successfulEntries, skippedEntries, totalEntries - successfulEntries - skippedEntries);
    }

    /**
     * @return a line of a csv file for this entry
     */
    @Override
    public String toString() {
        return (exerciseId.isPresent() ? exerciseId.getAsLong() : "") + "," + exerciseName + "," + totalEntries + "," + successfulEntries + "," + skippedEntries + ","
                + failedEntries;
    }

    /**
     * @return the headline of a csv file containing entries of this class
     */
    public static String getHeadline() {
        return "exerciseId,exerciseName,totalEntries,successfulEntries,skippedEntries,failedEntries";
    }
}
