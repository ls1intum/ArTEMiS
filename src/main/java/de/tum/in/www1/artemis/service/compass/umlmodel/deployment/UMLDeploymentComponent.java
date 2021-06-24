package de.tum.in.www1.artemis.service.compass.umlmodel.deployment;

import de.tum.in.www1.artemis.service.compass.umlmodel.component.UMLComponent;

public class UMLDeploymentComponent extends UMLComponent {

    public static final String UML_COMPONENT_TYPE = "DeploymentComponent";

    public UMLDeploymentComponent(String name, String jsonElementID) {
        super(name, jsonElementID);
    }

    @Override
    public String toString() {
        return "DeploymentComponent " + getName();
    }

    @Override
    public String getType() {
        return UML_COMPONENT_TYPE;
    }
}
