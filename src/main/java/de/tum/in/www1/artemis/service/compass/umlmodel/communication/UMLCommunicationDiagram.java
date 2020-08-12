package de.tum.in.www1.artemis.service.compass.umlmodel.communication;

import java.util.ArrayList;
import java.util.List;

import de.tum.in.www1.artemis.service.compass.umlmodel.UMLDiagram;
import de.tum.in.www1.artemis.service.compass.umlmodel.UMLElement;

public class UMLCommunicationDiagram extends UMLDiagram {

    private final List<UMLObject> objectList;

    private final List<UMLCommunicationLink> communicationLinkList;

    public UMLCommunicationDiagram(long modelSubmissionId, List<UMLObject> objectList, List<UMLCommunicationLink> communicationLinkList) {
        super(modelSubmissionId);
        this.objectList = objectList;
        this.communicationLinkList = communicationLinkList;
    }

    @Override
    public UMLElement getElementByJSONID(String jsonElementId) {

        for (UMLObject object : objectList) {
            if (object.getJSONElementID().equals(jsonElementId)) {
                return object;
            }
        }

        for (UMLCommunicationLink communicationLink : communicationLinkList) {
            if (communicationLink.getJSONElementID().equals(jsonElementId)) {
                return communicationLink;
            }
        }

        return null;
    }

    public List<UMLObject> getObjectList() {
        return objectList;
    }

    public List<UMLCommunicationLink> getCommunicationLinkList() {
        return communicationLinkList;
    }

    @Override
    protected List<UMLElement> getModelElements() {
        List<UMLElement> modelElements = new ArrayList<>();
        modelElements.addAll(objectList);
        modelElements.addAll(communicationLinkList);
        return modelElements;
    }
}
