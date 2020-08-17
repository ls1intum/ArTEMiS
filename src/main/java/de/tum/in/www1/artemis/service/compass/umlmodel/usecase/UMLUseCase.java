package de.tum.in.www1.artemis.service.compass.umlmodel.usecase;

import java.util.Objects;

import de.tum.in.www1.artemis.service.compass.strategy.NameSimilarity;
import de.tum.in.www1.artemis.service.compass.umlmodel.Similarity;
import de.tum.in.www1.artemis.service.compass.umlmodel.UMLElement;
import de.tum.in.www1.artemis.service.compass.utils.CompassConfiguration;

public class UMLUseCase extends UMLElement {

    public final static String UML_USE_CASE_TYPE = "UseCase";

    private String name;

    public UMLUseCase(String name, String jsonElementID) {
        super(jsonElementID);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return UML_USE_CASE_TYPE;
    }

    @Override
    public String toString() {
        return "Use Case " + name;
    }

    @Override
    public double similarity(Similarity<UMLElement> reference) {
        if (!(reference instanceof UMLUseCase)) {
            return 0;
        }

        double similarity = 0;

        UMLUseCase referenceUseCase = (UMLUseCase) reference;

        similarity += NameSimilarity.levenshteinSimilarity(getName(), referenceUseCase.getName()) * CompassConfiguration.COMPONENT_NAME_WEIGHT;

        if (Objects.equals(getParentElement(), referenceUseCase.getParentElement())) {
            similarity += CompassConfiguration.COMPONENT_PARENT_WEIGHT;
        }

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
        if (!(reference instanceof UMLUseCase)) {
            return 0;
        }

        UMLUseCase referenceObject = (UMLUseCase) reference;

        double similarity = similarity(referenceObject);

        return ensureSimilarityRange(similarity);
    }
}
