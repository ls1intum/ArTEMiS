package de.tum.in.www1.artemis.service.compass.umlmodel.parsers;

import static de.tum.in.www1.artemis.service.compass.utils.JSONMapping.*;

import java.io.IOException;
import java.util.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.tum.in.www1.artemis.service.compass.umlmodel.UMLElement;
import de.tum.in.www1.artemis.service.compass.umlmodel.component.UMLComponent;
import de.tum.in.www1.artemis.service.compass.umlmodel.component.UMLComponentInterface;
import de.tum.in.www1.artemis.service.compass.umlmodel.component.UMLComponentRelationship;
import de.tum.in.www1.artemis.service.compass.umlmodel.deployment.UMLArtifact;
import de.tum.in.www1.artemis.service.compass.umlmodel.deployment.UMLDeploymentDiagram;
import de.tum.in.www1.artemis.service.compass.umlmodel.deployment.UMLNode;

public class DeploymentDiagramParser {

    /**
     * Create a UML deployment diagram from the model and relationship elements given as JSON arrays. It parses the JSON objects to corresponding Java objects and creates a
     * deployment diagram containing these UML model elements.
     *
     * @param modelElements the model elements as JSON array
     * @param relationships the relationship elements as JSON array
     * @param modelSubmissionId the ID of the corresponding modeling submission
     * @return a UML deployment diagram containing the parsed model elements and relationships
     * @throws IOException when no corresponding model elements could be found for the source and target IDs in the relationship JSON objects
     */
    protected static UMLDeploymentDiagram buildDeploymentDiagramFromJSON(JsonArray modelElements, JsonArray relationships, long modelSubmissionId) throws IOException {

        // TODO: try to further reduce code duplication from buildComponentDiagramFromJSON

        Map<String, UMLComponent> umlComponentMap = new HashMap<>();
        Map<String, UMLComponentInterface> umlComponentInterfaceMap = new HashMap<>();
        Map<String, UMLNode> umlNodeMap = new HashMap<>();
        Map<String, UMLArtifact> umlArtifactMap = new HashMap<>();
        Map<String, UMLElement> allUmlElementsMap = new HashMap<>();
        List<UMLComponentRelationship> umlComponentRelationshipList = new ArrayList<>();

        // owners might not yet be available, therefore we need to store them in a map first before we can resolve them
        Map<UMLElement, String> ownerRelationships = new HashMap<>();

        // loop over all JSON elements and create the UML objects
        for (JsonElement jsonElement : modelElements) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            UMLElement umlElement = null;
            String elementType = jsonObject.get(ELEMENT_TYPE).getAsString();
            if (UMLComponent.UML_COMPONENT_TYPE.equals(elementType)) {
                UMLComponent umlComponent = ComponentDiagramParser.parseComponent(jsonObject);
                umlComponentMap.put(umlComponent.getJSONElementID(), umlComponent);
                umlElement = umlComponent;
            }
            // NOTE: there is a difference in the json between ComponentInterface and DeploymentInterface
            else if (UMLComponentInterface.UML_DEPLOYMENT_INTERFACE_TYPE.equals(elementType)) {
                UMLComponentInterface umlComponentInterface = ComponentDiagramParser.parseComponentInterface(jsonObject);
                umlComponentInterfaceMap.put(umlComponentInterface.getJSONElementID(), umlComponentInterface);
                umlElement = umlComponentInterface;
            }
            else if (UMLNode.UML_NODE_TYPE.equals(elementType)) {
                UMLNode umlNode = parseNode(jsonObject);
                umlNodeMap.put(umlNode.getJSONElementID(), umlNode);
                umlElement = umlNode;
            }
            else if (UMLArtifact.UML_ARTIFACT_TYPE.equals(elementType)) {
                UMLArtifact umlArtifact = parseArtifact(jsonObject);
                umlArtifactMap.put(umlArtifact.getJSONElementID(), umlArtifact);
                umlElement = umlArtifact;
            }
            if (umlElement != null) {
                allUmlElementsMap.put(umlElement.getJSONElementID(), umlElement);
                ComponentDiagramParser.findOwner(ownerRelationships, jsonObject, umlElement);
            }
        }

        // now we can resolve the owners: for this diagram type, uml components and uml nodes can be the actual owner
        ComponentDiagramParser.resolveParentComponent(allUmlElementsMap, ownerRelationships);

        // loop over all JSON control flow elements and create UML communication links
        for (JsonElement rel : relationships) {
            Optional<UMLComponentRelationship> componentRelationship = ComponentDiagramParser.parseComponentRelationship(rel.getAsJsonObject(), allUmlElementsMap);
            componentRelationship.ifPresent(umlComponentRelationshipList::add);
        }

        return new UMLDeploymentDiagram(modelSubmissionId, new ArrayList<>(umlComponentMap.values()), new ArrayList<>(umlComponentInterfaceMap.values()),
                umlComponentRelationshipList, new ArrayList<>(umlNodeMap.values()), new ArrayList<>(umlArtifactMap.values()));
    }

    private static UMLNode parseNode(JsonObject nodeJson) {
        String nodeName = nodeJson.get(ELEMENT_NAME).getAsString();
        String stereotypeName = nodeJson.get(STEREOTYPE_NAME).getAsString();
        return new UMLNode(nodeName, stereotypeName, nodeJson.get(ELEMENT_ID).getAsString());
    }

    private static UMLArtifact parseArtifact(JsonObject artifactJson) {
        String artifactName = artifactJson.get(ELEMENT_NAME).getAsString();
        return new UMLArtifact(artifactName, artifactJson.get(ELEMENT_ID).getAsString());
    }

}
