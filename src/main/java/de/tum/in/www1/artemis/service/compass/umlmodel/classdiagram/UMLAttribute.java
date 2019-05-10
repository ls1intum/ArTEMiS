package de.tum.in.www1.artemis.service.compass.umlmodel.classdiagram;

import de.tum.in.www1.artemis.service.compass.strategy.NameSimilarity;
import de.tum.in.www1.artemis.service.compass.umlmodel.UMLElement;
import de.tum.in.www1.artemis.service.compass.utils.CompassConfiguration;

public class UMLAttribute extends UMLElement {

    public final static String UML_ATTRIBUTE_TYPE = "ClassAttribute";

    private UMLClass parentClass;

    private String name;

    private String type;

    public UMLAttribute(String name, String type, String jsonElementID) {
        this.name = name;
        this.type = type;
        this.setJsonElementID(jsonElementID);
    }

    public void setParentClass(UMLClass parentClass) {
        this.parentClass = parentClass;
    }

    /**
     * Compare this with another element to calculate the similarity
     *
     * @param element the element to compare with
     * @return the similarity as number [0-1]
     */
    @Override
    public double similarity(UMLElement element) {
        double similarity = 0;

        if (element.getClass() != UMLAttribute.class) {
            return similarity;
        }

        UMLAttribute other = (UMLAttribute) element;

        similarity += NameSimilarity.namePartiallyEqualsSimilarity(name, other.name) * CompassConfiguration.ATTRIBUTE_NAME_WEIGHT;

        similarity += NameSimilarity.nameEqualsSimilarity(type, other.type) * CompassConfiguration.ATTRIBUTE_TYPE_WEIGHT;

        return similarity;
    }

    @Override
    public String getName() {
        return "Attribute " + name + (type != null && !type.equals("") ? ": " + type : "") + " in class " + parentClass.getValue();
    }

    @Override
    public String getValue() {
        return name;
    }

    @Override
    public String getType() {
        return UML_ATTRIBUTE_TYPE;
    }
}
