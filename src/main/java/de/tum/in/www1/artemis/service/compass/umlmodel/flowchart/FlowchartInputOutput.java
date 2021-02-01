package de.tum.in.www1.artemis.service.compass.umlmodel.flowchart;

import de.tum.in.www1.artemis.service.compass.strategy.NameSimilarity;
import de.tum.in.www1.artemis.service.compass.umlmodel.Similarity;
import de.tum.in.www1.artemis.service.compass.umlmodel.UMLElement;

public class FlowchartInputOutput extends UMLElement {

    public static final String FLOWCHART_INPUT_OUTPUT_TYPE = "FlowchartInputOutput";

    private final String name;

    public FlowchartInputOutput(String name, String jsonElementID) {
        super(jsonElementID);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return FLOWCHART_INPUT_OUTPUT_TYPE;
    }

    @Override
    public String toString() {
        return "FlowchartDecision " + name;
    }

    @Override
    public double similarity(Similarity<UMLElement> reference) {
        double similarity = 0;

        if (!(reference instanceof FlowchartInputOutput)) {
            return similarity;
        }
        FlowchartInputOutput referenceDecision = (FlowchartInputOutput) reference;

        similarity += NameSimilarity.levenshteinSimilarity(getName(), referenceDecision.getName());

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
        if (!(reference instanceof FlowchartInputOutput)) {
            return 0;
        }

        FlowchartInputOutput referenceDecision = (FlowchartInputOutput) reference;

        double similarity = similarity(referenceDecision);

        return ensureSimilarityRange(similarity);
    }
}
