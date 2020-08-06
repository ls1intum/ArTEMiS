package de.tum.in.www1.artemis.service.plagiarism.text;

import org.jetbrains.annotations.NotNull;

import de.tum.in.www1.artemis.domain.TextSubmission;
import info.debatty.java.stringsimilarity.Cosine;
import info.debatty.java.stringsimilarity.MetricLCS;
import info.debatty.java.stringsimilarity.NGram;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;

public interface TextComparisionStrategy {

    /**
     * Compare two Text Submissions and compute the distance [0.0, 1.1]
     *
     * @param a First text submission
     * @param b Second text submission
     * @return Distance Score [0.0, 1.1]
     */
    double compare(@NotNull TextSubmission a, @NotNull TextSubmission b);

    /**
     * See https://github.com/tdebatty/java-string-similarity#normalized-levenshtein
     * @return Normalized Levenshtein Strategy
     */
    static TextComparisionStrategy normalizedLevenshtein() {
        return (a, b) -> new NormalizedLevenshtein().distance(a.getText(), b.getText());
    }

    /**
     * See https://github.com/tdebatty/java-string-similarity#metric-longest-common-subsequence
     * @return Metric Longest Common Subsequence Strategy
     */
    static TextComparisionStrategy metricLongestCommonSubsequence() {
        return (a, b) -> new MetricLCS().distance(a.getText(), b.getText());
    }

    /**
     * See https://github.com/tdebatty/java-string-similarity#n-gram
     * @return N-Gram Strategy
     */
    static TextComparisionStrategy nGram() {
        return (a, b) -> new NGram().distance(a.getText(), b.getText());
    }

    /**
     * See https://github.com/tdebatty/java-string-similarity#shingle-n-gram-based-algorithms
     * @return Cosine similarity  Strategy
     */
    static TextComparisionStrategy cosine() {
        return (a, b) -> new Cosine().distance(a.getText(), b.getText());
    }

}
