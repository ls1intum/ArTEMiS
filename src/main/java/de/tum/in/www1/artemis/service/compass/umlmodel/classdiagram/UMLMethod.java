package de.tum.in.www1.artemis.service.compass.umlmodel.classdiagram;

import java.util.Collections;
import java.util.List;

import de.tum.in.www1.artemis.service.compass.strategy.NameSimilarity;
import de.tum.in.www1.artemis.service.compass.umlmodel.Similarity;
import de.tum.in.www1.artemis.service.compass.umlmodel.UMLElement;

public class UMLMethod extends UMLElement {

    public final static String UML_METHOD_TYPE = "ClassMethod";

    private UMLClass parentClass;

    private String completeName;

    private String name;

    private String returnType;

    private List<String> parameters;

    public UMLMethod(String completeName, String name, String returnType, List<String> parameter, String jsonElementID) {
        super(jsonElementID);

        this.completeName = completeName;
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameter;
    }

    /**
     * Set the parent class of this method, i.e. the UML class that contains it.
     *
     * @param parentClass the UML class that contains this method
     */
    public void setParentClass(UMLClass parentClass) {
        this.parentClass = parentClass;
    }

    /**
     * Get the return type of this method.
     *
     * @return the return type of this method as String
     */
    String getReturnType() {
        return returnType;
    }

    /**
     * Get the parameter list of this method.
     *
     * @return the list of parameters (as Strings) of this method
     */
    List<String> getParameters() {
        return parameters;
    }

    @Override
    public double similarity(Similarity<UMLElement> reference) {
        double similarity = 0;

        if (!(reference instanceof UMLMethod)) {
            return similarity;
        }

        UMLMethod referenceMethod = (UMLMethod) reference;

        int elementCount = parameters.size() + 2;
        double weight = 1.0 / elementCount;

        similarity += NameSimilarity.levenshteinSimilarity(name, referenceMethod.getName()) * weight;
        similarity += NameSimilarity.nameEqualsSimilarity(returnType, referenceMethod.getReturnType()) * weight;

        List<String> referenceParameters = referenceMethod.getParameters() != null ? referenceMethod.getParameters() : Collections.emptyList();
        for (String referenceParameter : referenceParameters) {
            if (parameters.contains(referenceParameter)) {
                similarity += weight;
            }
        }

        return ensureSimilarityRange(similarity);
    }

    @Override
    public String toString() {
        return "Method " + completeName + " in class " + parentClass.getName();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return UML_METHOD_TYPE;
    }
}
