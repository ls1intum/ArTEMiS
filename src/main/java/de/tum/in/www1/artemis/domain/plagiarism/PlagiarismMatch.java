package de.tum.in.www1.artemis.domain.plagiarism;

import jplag.Match;

/**
 * A `PlagiarismMatch` is a sequence of identical elements of both submissions.
 */
public class PlagiarismMatch {

    /**
     * Index of the first element of submission A that is part of this match.
     */
    private int startA;

    /**
     * Index of the first element of submission B that is part of this match.
     */
    private int startB;

    /**
     * Length of the sequence of identical elements, beginning at startA and startB, respectively.
     */
    private int length;

    /**
     * Create a new PlagiarismMatch instance from an existing JPlag Match
     *
     * @param jplagMatch the JPlag Match to create the PlagiarismMatch from
     * @return a new PlagiarismMatch instance
     */
    public static PlagiarismMatch fromJPlagMatch(Match jplagMatch) {
        PlagiarismMatch match = new PlagiarismMatch();

        match.setStartA(jplagMatch.startA);
        match.setStartB(jplagMatch.startB);
        match.setLength(jplagMatch.length);

        return match;
    }

    public int getStartA() {
        return startA;
    }

    public void setStartA(int startA) {
        this.startA = startA;
    }

    public int getStartB() {
        return startB;
    }

    public void setStartB(int startB) {
        this.startB = startB;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
