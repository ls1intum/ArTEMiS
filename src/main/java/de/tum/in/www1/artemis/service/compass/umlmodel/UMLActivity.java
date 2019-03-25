package de.tum.in.www1.artemis.service.compass.umlmodel;

import de.tum.in.www1.artemis.service.compass.strategy.NameSimilarity;

import java.util.List;

public class UMLActivity extends UMLElement {

    private String name;

    public enum UMLActivityType {
        ACTIVITY_CONTROL_INITIAL_NODE,
        ACTIVITY_CONTROL_FINAL_NODE,
        ACTIVITY_ACTION_NODE,
        ACTIVITY_OBJECT,
        ACTIVITY_MERGE_NODE,
        ACTIVITY_FORK_NODE,
        ACTIVITY_FORK_NODE_HORIZONTAL
    }

    public UMLActivity(String name, String jsonElementID) {
        this.name = name;
        this.setJsonElementID(jsonElementID);
    }

    @Override
    public double similarity(UMLElement element) {
        double similarity = 0;
        if (element.getClass() == UMLActivity.class) {
            similarity += NameSimilarity.nameContainsSimilarity(name, element.getName());
        }
        return similarity;
    }

    @Override
    public String getName() {
        return "Package " + name;
    }

    @Override
    public String getValue() {
        return name;
    }
}
