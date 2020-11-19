package de.tum.in.www1.artemis.domain.plagiarism;

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
