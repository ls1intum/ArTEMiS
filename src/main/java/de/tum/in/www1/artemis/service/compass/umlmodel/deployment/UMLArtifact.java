package de.tum.in.www1.artemis.service.compass.umlmodel.deployment;

import java.util.Objects;

import de.tum.in.www1.artemis.service.compass.strategy.NameSimilarity;
import de.tum.in.www1.artemis.service.compass.umlmodel.Similarity;
import de.tum.in.www1.artemis.service.compass.umlmodel.UMLElement;
import de.tum.in.www1.artemis.service.compass.utils.CompassConfiguration;

public class UMLArtifact extends UMLElement {

    public final static String UML_ARTIFACT_TYPE = "DeploymentArtifact";

    private final String name;

    public UMLArtifact(String name, String jsonElementID) {
        super(jsonElementID);
        this.name = name;
    }

    @Override
    public double similarity(Similarity<UMLElement> reference) {
        if (!(reference instanceof UMLArtifact)) {
            return 0;
        }

        double similarity = 0;

        UMLArtifact referenceArtifact = (UMLArtifact) reference;
        similarity += NameSimilarity.levenshteinSimilarity(getName(), referenceArtifact.getName()) * CompassConfiguration.COMPONENT_NAME_WEIGHT;

        if (Objects.equals(getParentElement(), referenceArtifact.getParentElement())) {
            similarity += CompassConfiguration.COMPONENT_PARENT_WEIGHT;
        }

        return ensureSimilarityRange(similarity);
    }

    @Override
    public String toString() {
        return "Artifact " + name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return UML_ARTIFACT_TYPE;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        UMLArtifact otherArtifact = (UMLArtifact) obj;

        return otherArtifact.name.equals(this.name);
    }
}
