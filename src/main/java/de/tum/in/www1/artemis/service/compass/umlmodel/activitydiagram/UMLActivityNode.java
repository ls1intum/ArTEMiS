package de.tum.in.www1.artemis.service.compass.umlmodel.activitydiagram;

import com.google.common.base.CaseFormat;

public class UMLActivityNode extends UMLActivityElement {

    public enum UMLActivityNodeType {
        ACTIVITY_INITIAL_NODE, ACTIVITY_FINAL_NODE, ACTIVITY_ACTION_NODE, ACTIVITY_OBJECT_NODE, ACTIVITY_FORK_NODE, ACTIVITY_JOIN_NODE, ACTIVITY_DECISION_NODE, ACTIVITY_MERGE_NODE
    }

    private UMLActivityNodeType type;

    public UMLActivityNode(String name, String jsonElementID, UMLActivityNodeType type) {
        super(name, jsonElementID);

        this.type = type;
    }

    @Override
    public String getType() {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, type.name());
    }
}
