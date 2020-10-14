package de.tum.in.www1.artemis.service.compass.umlmodel.syntaxtree;

import de.tum.in.www1.artemis.service.compass.strategy.NameSimilarity;
import de.tum.in.www1.artemis.service.compass.umlmodel.Similarity;
import de.tum.in.www1.artemis.service.compass.umlmodel.UMLElement;

public class SyntaxTreeNonterminal extends UMLElement {

    public final static String SYNTAX_TREE_NONTERMINAL_TYPE = "SyntaxTreeNonterminal";

    private final String name;

    public SyntaxTreeNonterminal(String name, String jsonElementID) {
        super(jsonElementID);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return SYNTAX_TREE_NONTERMINAL_TYPE;
    }

    @Override
    public String toString() {
        return "SyntaxTreeNonterminal " + name;
    }

    @Override
    public double similarity(Similarity<UMLElement> reference) {
        double similarity = 0;

        if (!(reference instanceof SyntaxTreeNonterminal)) {
            return similarity;
        }
        SyntaxTreeNonterminal referenceNonterminal = (SyntaxTreeNonterminal) reference;

        similarity += NameSimilarity.levenshteinSimilarity(getName(), referenceNonterminal.getName());

        return ensureSimilarityRange(similarity);
    }

    /**
     * Checks for overall similarity including attributes and methods.
     *
     * @param reference the reference element to compare this object with
     * @return the similarity as number [0-1]
     */
    @Override
    public double overallSimilarity(Similarity<UMLElement> reference) {
        if (!(reference instanceof SyntaxTreeNonterminal)) {
            return 0;
        }

        SyntaxTreeNonterminal referenceNonterminal = (SyntaxTreeNonterminal) reference;

        double similarity = similarity(referenceNonterminal);

        return ensureSimilarityRange(similarity);
    }
}
