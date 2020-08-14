package de.tum.in.www1.artemis.service.compass.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.EnumUtils;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import de.tum.in.www1.artemis.domain.enumeration.DiagramType;
import de.tum.in.www1.artemis.service.compass.umlmodel.UMLDiagram;
import de.tum.in.www1.artemis.service.compass.umlmodel.UMLElement;
import de.tum.in.www1.artemis.service.compass.umlmodel.activity.UMLActivity;
import de.tum.in.www1.artemis.service.compass.umlmodel.activity.UMLActivityDiagram;
import de.tum.in.www1.artemis.service.compass.umlmodel.activity.UMLActivityElement;
import de.tum.in.www1.artemis.service.compass.umlmodel.activity.UMLActivityNode;
import de.tum.in.www1.artemis.service.compass.umlmodel.activity.UMLActivityNode.UMLActivityNodeType;
import de.tum.in.www1.artemis.service.compass.umlmodel.activity.UMLControlFlow;
import de.tum.in.www1.artemis.service.compass.umlmodel.classdiagram.UMLAttribute;
import de.tum.in.www1.artemis.service.compass.umlmodel.classdiagram.UMLClass;
import de.tum.in.www1.artemis.service.compass.umlmodel.classdiagram.UMLClass.UMLClassType;
import de.tum.in.www1.artemis.service.compass.umlmodel.classdiagram.UMLClassDiagram;
import de.tum.in.www1.artemis.service.compass.umlmodel.classdiagram.UMLMethod;
import de.tum.in.www1.artemis.service.compass.umlmodel.classdiagram.UMLPackage;
import de.tum.in.www1.artemis.service.compass.umlmodel.classdiagram.UMLRelationship;
import de.tum.in.www1.artemis.service.compass.umlmodel.classdiagram.UMLRelationship.UMLRelationshipType;
import de.tum.in.www1.artemis.service.compass.umlmodel.communication.*;
import de.tum.in.www1.artemis.service.compass.umlmodel.component.UMLComponent;
import de.tum.in.www1.artemis.service.compass.umlmodel.component.UMLComponentDiagram;
import de.tum.in.www1.artemis.service.compass.umlmodel.component.UMLComponentInterface;
import de.tum.in.www1.artemis.service.compass.umlmodel.component.UMLComponentRelationship;
import de.tum.in.www1.artemis.service.compass.umlmodel.deployment.UMLArtifact;
import de.tum.in.www1.artemis.service.compass.umlmodel.deployment.UMLDeploymentDiagram;
import de.tum.in.www1.artemis.service.compass.umlmodel.deployment.UMLNode;
import de.tum.in.www1.artemis.service.compass.umlmodel.object.UMLObject;
import de.tum.in.www1.artemis.service.compass.umlmodel.object.UMLObjectDiagram;
import de.tum.in.www1.artemis.service.compass.umlmodel.usecase.*;
import de.tum.in.www1.artemis.service.compass.utils.JSONMapping;

public class UMLModelParser {

    /**
     * Create a UML diagram from a given JSON object.
     *
     * @param root              the JSON object containing the JSON representation of a UML diagram
     * @param modelSubmissionId the ID of the modeling submission containing the given UML diagram
     * @return the UML diagram as Java object
     * @throws IOException on unexpected JSON formats
     */
    public static UMLDiagram buildModelFromJSON(JsonObject root, long modelSubmissionId) throws IOException {
        String diagramType = root.get(JSONMapping.DIAGRAM_TYPE).getAsString();
        JsonArray modelElements = root.getAsJsonArray(JSONMapping.ELEMENTS);
        JsonArray relationships = root.getAsJsonArray(JSONMapping.RELATIONSHIPS);

        if (DiagramType.ClassDiagram.name().equals(diagramType)) {
            return buildClassDiagramFromJSON(modelElements, relationships, modelSubmissionId);
        }
        else if (DiagramType.ActivityDiagram.name().equals(diagramType)) {
            return buildActivityDiagramFromJSON(modelElements, relationships, modelSubmissionId);
        }
        else if (DiagramType.UseCaseDiagram.name().equals(diagramType)) {
            return buildUseCaseDiagramFromJSON(modelElements, relationships, modelSubmissionId);
        }
        else if (DiagramType.CommunicationDiagram.name().equals(diagramType)) {
            return buildCommunicationDiagramFromJSON(modelElements, relationships, modelSubmissionId);
        }
        else if (DiagramType.ComponentDiagram.name().equals(diagramType)) {
            return buildComponentDiagramFromJSON(modelElements, relationships, modelSubmissionId);
        }
        else if (DiagramType.DeploymentDiagram.name().equals(diagramType)) {
            return buildDeploymentDiagramFromJSON(modelElements, relationships, modelSubmissionId);
        }
        else if (DiagramType.ObjectDiagram.name().equals(diagramType)) {
            return buildObjectDiagramFromJSON(modelElements, relationships, modelSubmissionId);
        }

        throw new IllegalArgumentException("Diagram type of passed JSON not supported or not recognized by JSON Parser.");
    }

    /**
     * Create a UML communication diagram from the model and relationship elements given as JSON arrays. It parses the JSON objects to corresponding Java objects and creates a
     * communication diagram containing these UML model elements.
     *
     * @param modelElements the model elements as JSON array
     * @param relationships the relationship elements as JSON array
     * @param modelSubmissionId the ID of the corresponding modeling submission
     * @return a UML communication diagram containing the parsed model elements and relationships
     * @throws IOException when no corresponding model elements could be found for the source and target IDs in the relationship JSON objects
     */
    private static UMLCommunicationDiagram buildCommunicationDiagramFromJSON(JsonArray modelElements, JsonArray relationships, long modelSubmissionId) throws IOException {
        Map<String, UMLObject> umlObjectMap = new HashMap<>();
        List<UMLCommunicationLink> umlCommunicationLinkList = new ArrayList<>();

        // loop over all JSON elements and create the UML objects
        for (JsonElement elem : modelElements) {
            JsonObject element = elem.getAsJsonObject();
            String elementType = element.get(JSONMapping.ELEMENT_TYPE).getAsString();
            if ("ObjectName".equals(elementType)) {
                UMLObject umlObject = parseObject(element, modelElements);
                umlObjectMap.put(umlObject.getJSONElementID(), umlObject);
            }
        }

        // loop over all JSON control flow elements and create UML communication links
        for (JsonElement rel : relationships) {
            Optional<UMLCommunicationLink> communicationLink = parseCommunicationLink(rel.getAsJsonObject(), umlObjectMap);
            communicationLink.ifPresent(umlCommunicationLinkList::add);
        }

        return new UMLCommunicationDiagram(modelSubmissionId, new ArrayList<>(umlObjectMap.values()), umlCommunicationLinkList);
    }

    /**
     * Create a UML use case diagram from the model and relationship elements given as JSON arrays. It parses the JSON objects to corresponding Java objects and creates a
     * use case diagram containing these UML model elements.
     *
     * @param modelElements the model elements as JSON array
     * @param relationships the relationship elements as JSON array
     * @param modelSubmissionId the ID of the corresponding modeling submission
     * @return a UML use case diagram containing the parsed model elements and relationships
     * @throws IOException when no corresponding model elements could be found for the source and target IDs in the relationship JSON objects
     */
    private static UMLUseCaseDiagram buildUseCaseDiagramFromJSON(JsonArray modelElements, JsonArray relationships, long modelSubmissionId) throws IOException {
        Map<String, UMLSystemBoundary> umlSystemBoundaryMap = new HashMap<>();
        Map<String, UMLActor> umlActorMap = new HashMap<>();
        Map<String, UMLUseCase> umlUseCaseMap = new HashMap<>();
        Map<String, UMLElement> allUmlElementsMap = new HashMap<>();
        List<UMLUseCaseAssociation> umlUseCaseAssocationList = new ArrayList<>();

        // owners might not yet be available, therefore we need to store them in a map first before we can resolve them
        Map<UMLElement, String> ownerRelationships = new HashMap<>();

        // loop over all JSON elements and create the UML objects
        for (JsonElement jsonElement : modelElements) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            UMLElement umlElement = null;
            String elementType = jsonObject.get(JSONMapping.ELEMENT_TYPE).getAsString();
            if (UMLSystemBoundary.UML_SYSTEM_BOUNDARY_TYPE.equals(elementType)) {
                UMLSystemBoundary umlSystemBoundary = parseSystemBoundary(jsonObject);
                umlSystemBoundaryMap.put(umlSystemBoundary.getJSONElementID(), umlSystemBoundary);
                allUmlElementsMap.put(umlSystemBoundary.getJSONElementID(), umlSystemBoundary);
                umlElement = umlSystemBoundary;
            }
            else if (UMLActor.UML_ACTOR_TYPE.equals(elementType)) {
                UMLActor umlActor = parseActor(jsonObject);
                umlActorMap.put(umlActor.getJSONElementID(), umlActor);
                allUmlElementsMap.put(umlActor.getJSONElementID(), umlActor);
                umlElement = umlActor;
            }
            else if (UMLUseCase.UML_USE_CASE_TYPE.equals(elementType)) {
                UMLUseCase umlUseCase = parseUseCase(jsonObject);
                umlUseCaseMap.put(umlUseCase.getJSONElementID(), umlUseCase);
                allUmlElementsMap.put(umlUseCase.getJSONElementID(), umlUseCase);
                umlElement = umlUseCase;
            }
            if (jsonObject.has(JSONMapping.ELEMENT_OWNER) && !jsonObject.get(JSONMapping.ELEMENT_OWNER).isJsonNull() && umlElement != null) {
                String ownerId = jsonObject.get(JSONMapping.ELEMENT_OWNER).getAsString();
                ownerRelationships.put(umlElement, ownerId);
            }
        }

        // now we can resolve the owners: for this diagram type, only uml system boundaries can be the actual owner
        for (var ownerEntry : ownerRelationships.entrySet()) {
            String ownerId = ownerEntry.getValue();
            UMLElement umlElement = ownerEntry.getKey();
            UMLSystemBoundary parentSystemBoundary = umlSystemBoundaryMap.get(ownerId);
            umlElement.setParentElement(parentSystemBoundary);
        }

        // loop over all JSON control flow elements and create UML communication links
        for (JsonElement rel : relationships) {
            Optional<UMLUseCaseAssociation> useCaseAssociation = parseUseCaseAssociation(rel.getAsJsonObject(), allUmlElementsMap);
            useCaseAssociation.ifPresent(umlUseCaseAssocationList::add);
        }

        return new UMLUseCaseDiagram(modelSubmissionId, new ArrayList<>(umlSystemBoundaryMap.values()), new ArrayList<>(umlActorMap.values()),
                new ArrayList<>(umlUseCaseMap.values()), umlUseCaseAssocationList);
    }

    /**
     * Create a UML object diagram from the model and relationship elements given as JSON arrays. It parses the JSON objects to corresponding Java objects and creates a
     * object diagram containing these UML model elements.
     *
     * @param modelElements the model elements as JSON array
     * @param relationships the relationship elements as JSON array
     * @param modelSubmissionId the ID of the corresponding modeling submission
     * @return a UML object diagram containing the parsed model elements and relationships
     * @throws IOException when no corresponding model elements could be found for the source and target IDs in the relationship JSON objects
     */
    private static UMLObjectDiagram buildObjectDiagramFromJSON(JsonArray modelElements, JsonArray relationships, long modelSubmissionId) throws IOException {
        throw new IllegalArgumentException("Diagram type of passed JSON not supported or not recognized by JSON Parser.");
    }

    /**
     * Create a UML component diagram from the model and relationship elements given as JSON arrays. It parses the JSON objects to corresponding Java objects and creates a
     * component diagram containing these UML model elements.
     *
     * @param modelElements the model elements as JSON array
     * @param relationships the relationship elements as JSON array
     * @param modelSubmissionId the ID of the corresponding modeling submission
     * @return a UML component diagram containing the parsed model elements and relationships
     * @throws IOException when no corresponding model elements could be found for the source and target IDs in the relationship JSON objects
     */
    private static UMLComponentDiagram buildComponentDiagramFromJSON(JsonArray modelElements, JsonArray relationships, long modelSubmissionId) throws IOException {
        Map<String, UMLComponent> umlComponentMap = new HashMap<>();
        Map<String, UMLComponentInterface> umlComponentInterfaceMap = new HashMap<>();
        Map<String, UMLElement> allUmlElementsMap = new HashMap<>();
        List<UMLComponentRelationship> umlComponentRelationshipList = new ArrayList<>();

        // owners might not yet be available, therefore we need to store them in a map first before we can resolve them
        Map<UMLElement, String> ownerRelationships = new HashMap<>();

        // loop over all JSON elements and create the UML objects
        for (JsonElement jsonElement : modelElements) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            UMLElement umlElement = null;
            String elementType = jsonObject.get(JSONMapping.ELEMENT_TYPE).getAsString();
            if (UMLComponent.UML_COMPONENT_TYPE.equals(elementType)) {
                UMLComponent umlComponent = parseComponent(jsonObject);
                umlComponentMap.put(umlComponent.getJSONElementID(), umlComponent);
                allUmlElementsMap.put(umlComponent.getJSONElementID(), umlComponent);
                umlElement = umlComponent;
            }
            else if (UMLComponentInterface.UML_COMPONENT_INTERFACE_TYPE.equals(elementType)) {
                UMLComponentInterface umlComponentInterface = parseComponentInterface(jsonObject);
                umlComponentInterfaceMap.put(umlComponentInterface.getJSONElementID(), umlComponentInterface);
                allUmlElementsMap.put(umlComponentInterface.getJSONElementID(), umlComponentInterface);
                umlElement = umlComponentInterface;
            }
            if (jsonObject.has(JSONMapping.ELEMENT_OWNER) && !jsonObject.get(JSONMapping.ELEMENT_OWNER).isJsonNull() && umlElement != null) {
                String ownerId = jsonObject.get(JSONMapping.ELEMENT_OWNER).getAsString();
                ownerRelationships.put(umlElement, ownerId);
            }
        }

        // now we can resolve the owners: for this diagram type, only uml components can be the actual owner
        for (var ownerEntry : ownerRelationships.entrySet()) {
            String ownerId = ownerEntry.getValue();
            UMLElement umlElement = ownerEntry.getKey();
            UMLComponent parentComopnent = umlComponentMap.get(ownerId);
            umlElement.setParentElement(parentComopnent);
        }

        // loop over all JSON control flow elements and create UML communication links
        for (JsonElement rel : relationships) {
            Optional<UMLComponentRelationship> componentRelationship = parseComponentRelationship(rel.getAsJsonObject(), allUmlElementsMap);
            componentRelationship.ifPresent(umlComponentRelationshipList::add);
        }

        return new UMLComponentDiagram(modelSubmissionId, new ArrayList<>(umlComponentMap.values()), new ArrayList<>(umlComponentInterfaceMap.values()),
                umlComponentRelationshipList);
    }

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
    private static UMLDeploymentDiagram buildDeploymentDiagramFromJSON(JsonArray modelElements, JsonArray relationships, long modelSubmissionId) throws IOException {
        // TODO: reduce code duplication with buildComponentDiatramFromJSON

        Map<String, UMLComponent> umlComponentMap = new HashMap<>();
        Map<String, UMLNode> umlNodeMap = new HashMap<>();
        Map<String, UMLArtifact> umlArtifactMap = new HashMap<>();
        Map<String, UMLComponentInterface> umlComponentInterfaceMap = new HashMap<>();
        Map<String, UMLElement> allUmlElementsMap = new HashMap<>();
        List<UMLComponentRelationship> umlComponentRelationshipList = new ArrayList<>();

        // owners might not yet be available, therefore we need to store them in a map first before we can resolve them
        Map<UMLElement, String> ownerRelationships = new HashMap<>();

        // loop over all JSON elements and create the UML objects
        for (JsonElement jsonElement : modelElements) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            UMLElement umlElement = null;
            String elementType = jsonObject.get(JSONMapping.ELEMENT_TYPE).getAsString();
            if (UMLComponent.UML_COMPONENT_TYPE.equals(elementType)) {
                UMLComponent umlComponent = parseComponent(jsonObject);
                umlComponentMap.put(umlComponent.getJSONElementID(), umlComponent);
                allUmlElementsMap.put(umlComponent.getJSONElementID(), umlComponent);
                umlElement = umlComponent;
            }
            // NOTE: there is a difference in the json between ComponentInterface and DeploymentInterface
            else if (UMLComponentInterface.UML_DEPLOYMENT_INTERFACE_TYPE.equals(elementType)) {
                UMLComponentInterface umlComponentInterface = parseComponentInterface(jsonObject);
                umlComponentInterfaceMap.put(umlComponentInterface.getJSONElementID(), umlComponentInterface);
                allUmlElementsMap.put(umlComponentInterface.getJSONElementID(), umlComponentInterface);
                umlElement = umlComponentInterface;
            }
            else if (UMLNode.UML_NODE_TYPE.equals(elementType)) {
                UMLNode umlNode = parseNode(jsonObject);
                umlNodeMap.put(umlNode.getJSONElementID(), umlNode);
                allUmlElementsMap.put(umlNode.getJSONElementID(), umlNode);
                umlElement = umlNode;
            }
            else if (UMLArtifact.UML_ARTIFACT_TYPE.equals(elementType)) {
                UMLArtifact umlArtifact = parseArtifact(jsonObject);
                umlArtifactMap.put(umlArtifact.getJSONElementID(), umlArtifact);
                allUmlElementsMap.put(umlArtifact.getJSONElementID(), umlArtifact);
                umlElement = umlArtifact;
            }
            if (jsonObject.has(JSONMapping.ELEMENT_OWNER) && !jsonObject.get(JSONMapping.ELEMENT_OWNER).isJsonNull() && umlElement != null) {
                String ownerId = jsonObject.get(JSONMapping.ELEMENT_OWNER).getAsString();
                ownerRelationships.put(umlElement, ownerId);
            }
        }

        // now we can resolve the owners: for this diagram type, uml components and uml nodes can be the actual owner
        for (var ownerEntry : ownerRelationships.entrySet()) {
            String ownerId = ownerEntry.getValue();
            UMLElement umlElement = ownerEntry.getKey();
            UMLElement parentComopnent = allUmlElementsMap.get(ownerId);
            umlElement.setParentElement(parentComopnent);
        }

        // loop over all JSON control flow elements and create UML communication links
        for (JsonElement rel : relationships) {
            Optional<UMLComponentRelationship> componentRelationship = parseComponentRelationship(rel.getAsJsonObject(), allUmlElementsMap);
            componentRelationship.ifPresent(umlComponentRelationshipList::add);
        }

        return new UMLDeploymentDiagram(modelSubmissionId, new ArrayList<>(umlComponentMap.values()), new ArrayList<>(umlComponentInterfaceMap.values()),
                umlComponentRelationshipList, new ArrayList<>(umlNodeMap.values()), new ArrayList<>(umlArtifactMap.values()));
    }

    /**
     * Create a UML class diagram from the model and relationship elements given as JSON arrays. It parses the JSON objects to corresponding Java objects and creates a class
     * diagram containing these UML model elements.
     *
     * @param modelElements the model elements as JSON array
     * @param relationships the relationship elements as JSON array
     * @param modelSubmissionId the ID of the corresponding modeling submission
     * @return a UML class diagram containing the parsed model elements and relationships
     * @throws IOException when no corresponding model elements could be found for the source and target IDs in the relationship JSON objects
     */
    private static UMLClassDiagram buildClassDiagramFromJSON(JsonArray modelElements, JsonArray relationships, long modelSubmissionId) throws IOException {
        Map<String, UMLClass> umlClassMap = new HashMap<>();
        List<UMLRelationship> umlRelationshipList = new ArrayList<>();
        Map<String, UMLPackage> umlPackageMap = new HashMap<>();

        // loop over all JSON elements and UML package objects
        for (JsonElement elem : modelElements) {
            JsonObject element = elem.getAsJsonObject();
            String elementType = element.get(JSONMapping.ELEMENT_TYPE).getAsString();

            if (elementType.equals(UMLPackage.UML_PACKAGE_TYPE)) {
                UMLPackage umlPackage = parsePackage(element);
                umlPackageMap.put(umlPackage.getJSONElementID(), umlPackage);
            }
        }

        // loop over all JSON elements and create the UML class objects
        for (JsonElement elem : modelElements) {
            JsonObject element = elem.getAsJsonObject();

            String elementType = element.get(JSONMapping.ELEMENT_TYPE).getAsString();
            elementType = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, elementType);

            if (EnumUtils.isValidEnum(UMLClassType.class, elementType)) {
                UMLClass umlClass = parseClass(elementType, element, modelElements, umlPackageMap);
                umlClassMap.put(umlClass.getJSONElementID(), umlClass);
            }
        }

        // loop over all JSON relationship elements and create UML relationships
        for (JsonElement rel : relationships) {
            Optional<UMLRelationship> relationship = parseRelationship(rel.getAsJsonObject(), umlClassMap, umlPackageMap);
            relationship.ifPresent(umlRelationshipList::add);
        }

        return new UMLClassDiagram(modelSubmissionId, new ArrayList<>(umlClassMap.values()), umlRelationshipList, new ArrayList<>(umlPackageMap.values()));
    }

    /**
     * Parses the given JSON representation of a UML package to a UMLPackage Java object.
     *
     * @param packageJson the JSON object containing the package
     * @return the UMLPackage object parsed from the JSON object
     */
    private static UMLPackage parsePackage(JsonObject packageJson) {
        String packageName = packageJson.get(JSONMapping.ELEMENT_NAME).getAsString();
        List<UMLElement> umlClassList = new ArrayList<>();
        String jsonElementId = packageJson.get(JSONMapping.ELEMENT_ID).getAsString();
        return new UMLPackage(packageName, umlClassList, jsonElementId);
    }

    /**
     * Parses the given JSON representation of a UML class to a UMLClass Java object.
     *
     * @param classType the type of the UML class
     * @param classJson the JSON object containing the UML class
     * @param modelElements a JSON array containing all the model elements of the corresponding UML class diagram as JSON objects
     * @param umlPackageMap a map containing all the packages of the corresponding UML class diagram
     * @return the UMLClass object parsed from the JSON object
     */
    private static UMLClass parseClass(String classType, JsonObject classJson, JsonArray modelElements, Map<String, UMLPackage> umlPackageMap) {
        Map<String, JsonObject> jsonElementMap = generateJsonElementMap(modelElements);

        UMLClassType umlClassType = UMLClassType.valueOf(classType);
        String className = classJson.get(JSONMapping.ELEMENT_NAME).getAsString();

        List<UMLAttribute> umlAttributesList = new ArrayList<>();
        for (JsonElement attributeId : classJson.getAsJsonArray(JSONMapping.ELEMENT_ATTRIBUTES)) {
            UMLAttribute newAttr = parseAttribute(jsonElementMap.get(attributeId.getAsString()));
            umlAttributesList.add(newAttr);
        }

        List<UMLMethod> umlMethodList = new ArrayList<>();
        for (JsonElement methodId : classJson.getAsJsonArray(JSONMapping.ELEMENT_METHODS)) {
            UMLMethod newMethod = parseMethod(jsonElementMap.get(methodId.getAsString()));
            umlMethodList.add(newMethod);
        }

        UMLClass newClass = new UMLClass(className, umlAttributesList, umlMethodList, classJson.get(JSONMapping.ELEMENT_ID).getAsString(), umlClassType);

        if (classJson.has(JSONMapping.ELEMENT_OWNER) && !classJson.get(JSONMapping.ELEMENT_OWNER).isJsonNull()) {
            String packageId = classJson.get(JSONMapping.ELEMENT_OWNER).getAsString();
            UMLPackage umlPackage = umlPackageMap.get(packageId);
            if (umlPackage != null) {
                umlPackage.addSubElement(newClass);
                newClass.setUmlPackage(umlPackage);
            }
        }

        return newClass;
    }

    /**
     * Parses the given JSON representation of a UML object to a UMLObject Java object.
     *
     * @param objectJson the JSON object containing the UML object
     * @param modelElements a JSON array containing all the model elements of the corresponding UML class diagram as JSON objects
     * @return the UMLObject object parsed from the JSON object
     */
    private static UMLObject parseObject(JsonObject objectJson, JsonArray modelElements) {
        Map<String, JsonObject> jsonElementMap = generateJsonElementMap(modelElements);
        String objectName = objectJson.get(JSONMapping.ELEMENT_NAME).getAsString();
        List<UMLAttribute> umlAttributesList = new ArrayList<>();
        for (JsonElement attributeId : objectJson.getAsJsonArray(JSONMapping.ELEMENT_ATTRIBUTES)) {
            UMLAttribute newAttr = parseAttribute(jsonElementMap.get(attributeId.getAsString()));
            umlAttributesList.add(newAttr);
        }

        List<UMLMethod> umlMethodList = new ArrayList<>();
        for (JsonElement methodId : objectJson.getAsJsonArray(JSONMapping.ELEMENT_METHODS)) {
            UMLMethod newMethod = parseMethod(jsonElementMap.get(methodId.getAsString()));
            umlMethodList.add(newMethod);
        }
        return new UMLObject(objectName, umlAttributesList, umlMethodList, objectJson.get(JSONMapping.ELEMENT_ID).getAsString());
    }

    /**
     * Parses the given JSON representation of a UML attribute to a UMLAttribute Java object.
     *
     * @param attributeJson the JSON object containing the attribute
     * @return the UMLAttribute object parsed from the JSON object
     */
    private static UMLAttribute parseAttribute(JsonObject attributeJson) {
        String[] attributeNameArray = attributeJson.get(JSONMapping.ELEMENT_NAME).getAsString().replaceAll("\\s+", "").split(":");
        String attributeName = attributeNameArray[0];
        String attributeType = "";

        if (attributeNameArray.length == 2) {
            attributeType = attributeNameArray[1];
        }

        return new UMLAttribute(attributeName, attributeType, attributeJson.get(JSONMapping.ELEMENT_ID).getAsString());
    }

    /**
     * Parses the given JSON representation of a UML method to a UMLMethod Java object.
     *
     * @param methodJson the JSON object containing the method
     * @return the UMLMethod object parsed from the JSON object
     */
    private static UMLMethod parseMethod(JsonObject methodJson) {
        String completeMethodName = methodJson.get(JSONMapping.ELEMENT_NAME).getAsString();
        String[] methodEntryArray = completeMethodName.replaceAll("\\s+", "").split(":");
        String[] methodParts = methodEntryArray[0].split("[()]");

        String methodName = "";
        if (methodParts.length > 0) {
            methodName = methodParts[0];
        }

        String[] methodParams = {};
        if (methodParts.length == 2) {
            methodParams = methodParts[1].split(",");
        }

        String methodReturnType = "";
        if (methodEntryArray.length == 2) {
            methodReturnType = methodEntryArray[1];
        }

        return new UMLMethod(completeMethodName, methodName, methodReturnType, Arrays.asList(methodParams), methodJson.get(JSONMapping.ELEMENT_ID).getAsString());
    }

    /**
     * Parses the given JSON representation of a UML relationship to a UMLRelationship Java object.
     *
     * @param relationshipJson the JSON object containing the relationship
     * @param classMap a map containing all classes of the corresponding class diagram, necessary for assigning source and target element of the relationships
     * @param packageMap the package map contains all packages of the diagram
     * @return the UMLRelationship object parsed from the JSON object
     * @throws IOException when no class could be found in the classMap for the source and target ID in the JSON object
     */
    private static Optional<UMLRelationship> parseRelationship(JsonObject relationshipJson, Map<String, UMLClass> classMap, Map<String, UMLPackage> packageMap) throws IOException {
        String relationshipType = relationshipJson.get(JSONMapping.RELATIONSHIP_TYPE).getAsString();
        relationshipType = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, relationshipType);

        if (!EnumUtils.isValidEnum(UMLRelationshipType.class, relationshipType)) {
            return Optional.empty();
        }

        JsonObject relationshipSource = relationshipJson.getAsJsonObject(JSONMapping.RELATIONSHIP_SOURCE);
        JsonObject relationshipTarget = relationshipJson.getAsJsonObject(JSONMapping.RELATIONSHIP_TARGET);

        String sourceJSONID = relationshipSource.get(JSONMapping.RELATIONSHIP_ENDPOINT_ID).getAsString();
        String targetJSONID = relationshipTarget.get(JSONMapping.RELATIONSHIP_ENDPOINT_ID).getAsString();

        UMLClass source = classMap.get(sourceJSONID);
        UMLClass target = classMap.get(targetJSONID);

        JsonElement relationshipSourceRole = relationshipSource.has(JSONMapping.RELATIONSHIP_ROLE) ? relationshipSource.get(JSONMapping.RELATIONSHIP_ROLE) : JsonNull.INSTANCE;
        JsonElement relationshipTargetRole = relationshipTarget.has(JSONMapping.RELATIONSHIP_ROLE) ? relationshipTarget.get(JSONMapping.RELATIONSHIP_ROLE) : JsonNull.INSTANCE;
        JsonElement relationshipSourceMultiplicity = relationshipSource.has(JSONMapping.RELATIONSHIP_MULTIPLICITY) ? relationshipSource.get(JSONMapping.RELATIONSHIP_MULTIPLICITY)
                : JsonNull.INSTANCE;
        JsonElement relationshipTargetMultiplicity = relationshipTarget.has(JSONMapping.RELATIONSHIP_MULTIPLICITY) ? relationshipTarget.get(JSONMapping.RELATIONSHIP_MULTIPLICITY)
                : JsonNull.INSTANCE;

        if (source != null && target != null) {
            UMLRelationship newRelationship = new UMLRelationship(source, target, UMLRelationshipType.valueOf(relationshipType),
                    relationshipJson.get(JSONMapping.ELEMENT_ID).getAsString(), relationshipSourceRole.isJsonNull() ? "" : relationshipSourceRole.getAsString(),
                    relationshipTargetRole.isJsonNull() ? "" : relationshipTargetRole.getAsString(),
                    relationshipSourceMultiplicity.isJsonNull() ? "" : relationshipSourceMultiplicity.getAsString(),
                    relationshipTargetMultiplicity.isJsonNull() ? "" : relationshipTargetMultiplicity.getAsString());

            return Optional.of(newRelationship);
        }
        else {
            if (source == null && packageMap.containsKey(sourceJSONID) || target == null && packageMap.containsKey(targetJSONID)) {
                // workaround: prevent exception when a package is source or target of a relationship
                return Optional.empty();
            }

            throw new IOException("Relationship source or target not part of model!");
        }
    }

    private static Optional<UMLComponentRelationship> parseComponentRelationship(JsonObject relationshipJson, Map<String, UMLElement> allUmlElementsMap) throws IOException {

        String relationshipType = relationshipJson.get(JSONMapping.RELATIONSHIP_TYPE).getAsString();
        relationshipType = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, relationshipType);

        if (!EnumUtils.isValidEnum(UMLComponentRelationship.UMLComponentRelationshipType.class, relationshipType)) {
            return Optional.empty();
        }

        JsonObject relationshipSource = relationshipJson.getAsJsonObject(JSONMapping.RELATIONSHIP_SOURCE);
        JsonObject relationshipTarget = relationshipJson.getAsJsonObject(JSONMapping.RELATIONSHIP_TARGET);

        String sourceJSONID = relationshipSource.get(JSONMapping.RELATIONSHIP_ENDPOINT_ID).getAsString();
        String targetJSONID = relationshipTarget.get(JSONMapping.RELATIONSHIP_ENDPOINT_ID).getAsString();

        UMLElement source = allUmlElementsMap.get(sourceJSONID);
        UMLElement target = allUmlElementsMap.get(targetJSONID);

        if (source != null && target != null) {
            UMLComponentRelationship newComponentRelationship = new UMLComponentRelationship(source, target,
                    UMLComponentRelationship.UMLComponentRelationshipType.valueOf(relationshipType), relationshipJson.get(JSONMapping.ELEMENT_ID).getAsString());
            return Optional.of(newComponentRelationship);
        }
        else {
            throw new IOException("Relationship source or target not part of model!");
        }
    }

    private static Optional<UMLUseCaseAssociation> parseUseCaseAssociation(JsonObject relationshipJson, Map<String, UMLElement> allUmlElementsMap) throws IOException {

        String relationshipType = relationshipJson.get(JSONMapping.RELATIONSHIP_TYPE).getAsString();
        relationshipType = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, relationshipType);

        if (!EnumUtils.isValidEnum(UMLUseCaseAssociation.UMLUseCaseAssociationType.class, relationshipType)) {
            return Optional.empty();
        }

        var associationType = UMLUseCaseAssociation.UMLUseCaseAssociationType.valueOf(relationshipType);
        String name = null;
        if (associationType == UMLUseCaseAssociation.UMLUseCaseAssociationType.USE_CASE_ASSOCIATION) {
            name = relationshipJson.get(JSONMapping.ELEMENT_NAME).getAsString();
        }

        JsonObject relationshipSource = relationshipJson.getAsJsonObject(JSONMapping.RELATIONSHIP_SOURCE);
        JsonObject relationshipTarget = relationshipJson.getAsJsonObject(JSONMapping.RELATIONSHIP_TARGET);

        String sourceJSONID = relationshipSource.get(JSONMapping.RELATIONSHIP_ENDPOINT_ID).getAsString();
        String targetJSONID = relationshipTarget.get(JSONMapping.RELATIONSHIP_ENDPOINT_ID).getAsString();

        UMLElement source = allUmlElementsMap.get(sourceJSONID);
        UMLElement target = allUmlElementsMap.get(targetJSONID);

        if (source != null && target != null) {
            UMLUseCaseAssociation newComponentRelationship = new UMLUseCaseAssociation(name, source, target, associationType,
                    relationshipJson.get(JSONMapping.ELEMENT_ID).getAsString());
            return Optional.of(newComponentRelationship);
        }
        else {
            throw new IOException("Relationship source or target not part of model!");
        }
    }

    private static UMLComponentInterface parseComponentInterface(JsonObject componentInterfaceJson) {
        String componentInterfaceName = componentInterfaceJson.get(JSONMapping.ELEMENT_NAME).getAsString();
        return new UMLComponentInterface(componentInterfaceName, componentInterfaceJson.get(JSONMapping.ELEMENT_ID).getAsString());
    }

    private static UMLComponent parseComponent(JsonObject componentJson) {
        String componentName = componentJson.get(JSONMapping.ELEMENT_NAME).getAsString();
        return new UMLComponent(componentName, componentJson.get(JSONMapping.ELEMENT_ID).getAsString());
    }

    private static UMLNode parseNode(JsonObject nodeJson) {
        String nodeName = nodeJson.get(JSONMapping.ELEMENT_NAME).getAsString();
        String stereotypeName = nodeJson.get(JSONMapping.STEREOTYPE_NAME).getAsString();
        return new UMLNode(nodeName, stereotypeName, nodeJson.get(JSONMapping.ELEMENT_ID).getAsString());
    }

    private static UMLArtifact parseArtifact(JsonObject artifactJson) {
        String artifactName = artifactJson.get(JSONMapping.ELEMENT_NAME).getAsString();
        return new UMLArtifact(artifactName, artifactJson.get(JSONMapping.ELEMENT_ID).getAsString());
    }

    private static UMLSystemBoundary parseSystemBoundary(JsonObject componentJson) {
        String systemBoundaryName = componentJson.get(JSONMapping.ELEMENT_NAME).getAsString();
        return new UMLSystemBoundary(systemBoundaryName, componentJson.get(JSONMapping.ELEMENT_ID).getAsString());
    }

    private static UMLActor parseActor(JsonObject componentJson) {
        String actorName = componentJson.get(JSONMapping.ELEMENT_NAME).getAsString();
        return new UMLActor(actorName, componentJson.get(JSONMapping.ELEMENT_ID).getAsString());
    }

    private static UMLUseCase parseUseCase(JsonObject componentJson) {
        String useCaseName = componentJson.get(JSONMapping.ELEMENT_NAME).getAsString();
        return new UMLUseCase(useCaseName, componentJson.get(JSONMapping.ELEMENT_ID).getAsString());
    }

    /**
     * Parses the given JSON representation of a UML relationship to a UMLRelationship Java object.
     *
     * @param relationshipJson the JSON object containing the relationship
     * @param objectMap a map containing all objects of the corresponding communication diagram, necessary for assigning source and target element of the relationships
     * @return the UMLRelationship object parsed from the JSON object
     * @throws IOException when no class could be found in the classMap for the source and target ID in the JSON object
     */
    private static Optional<UMLCommunicationLink> parseCommunicationLink(JsonObject relationshipJson, Map<String, UMLObject> objectMap) throws IOException {

        JsonObject relationshipSource = relationshipJson.getAsJsonObject(JSONMapping.RELATIONSHIP_SOURCE);
        JsonObject relationshipTarget = relationshipJson.getAsJsonObject(JSONMapping.RELATIONSHIP_TARGET);

        String sourceJSONID = relationshipSource.get(JSONMapping.RELATIONSHIP_ENDPOINT_ID).getAsString();
        String targetJSONID = relationshipTarget.get(JSONMapping.RELATIONSHIP_ENDPOINT_ID).getAsString();

        UMLObject source = objectMap.get(sourceJSONID);
        UMLObject target = objectMap.get(targetJSONID);

        List<UMLMessage> messages = new ArrayList<>();

        for (JsonElement messageJson : relationshipJson.getAsJsonArray(JSONMapping.RELATIONSHIP_MESSAGES)) {
            JsonObject messageJsonObject = (JsonObject) messageJson;
            Direction direction = messageJsonObject.get("direction").getAsString().equals("target") ? Direction.TARGET : Direction.SOURCE;
            UMLMessage message = new UMLMessage(messageJsonObject.get("name").getAsString(), direction);
            messages.add(message);
        }

        if (source != null && target != null) {
            UMLCommunicationLink newCommunicationLink = new UMLCommunicationLink(source, target, messages, relationshipJson.get(JSONMapping.ELEMENT_ID).getAsString());
            return Optional.of(newCommunicationLink);
        }
        else {
            throw new IOException("Relationship source or target not part of model!");
        }
    }

    /**
     * Create an activity diagram from the model and control flow elements given as JSON arrays. It parses the JSON objects to corresponding Java objects and creates an activity
     * diagram containing these UML model elements.
     *
     * @param modelElements the model elements (UML activities and activity nodes) as JSON array
     * @param controlFlows the control flow elements as JSON array
     * @param modelSubmissionId the ID of the corresponding modeling submission
     * @return a UML activity diagram containing the parsed model elements and control flows
     * @throws IOException when no corresponding model elements could be found for the source and target IDs in the control flow JSON objects
     */
    private static UMLActivityDiagram buildActivityDiagramFromJSON(JsonArray modelElements, JsonArray controlFlows, long modelSubmissionId) throws IOException {
        Map<String, UMLActivityElement> umlActivityElementMap = new HashMap<>();
        Map<String, UMLActivity> umlActivityMap = new HashMap<>();
        List<UMLActivityNode> umlActivityNodeList = new ArrayList<>();
        List<UMLControlFlow> umlControlFlowList = new ArrayList<>();

        // loop over all JSON elements and create activity and activity node objects
        for (JsonElement elem : modelElements) {
            JsonObject element = elem.getAsJsonObject();

            String elementType = element.get(JSONMapping.ELEMENT_TYPE).getAsString();
            String elementTypeUpperUnderscore = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, elementType);

            if (EnumUtils.isValidEnum(UMLActivityNodeType.class, elementTypeUpperUnderscore)) {
                UMLActivityNode activityNode = parseActivityNode(elementTypeUpperUnderscore, element);
                umlActivityNodeList.add(activityNode);
                umlActivityElementMap.put(activityNode.getJSONElementID(), activityNode);
            }
            else if (UMLActivity.UML_ACTIVITY_TYPE.equals(elementType)) {
                UMLActivity activity = parseActivity(element);
                umlActivityMap.put(activity.getJSONElementID(), activity);
                umlActivityElementMap.put(activity.getJSONElementID(), activity);
            }
        }

        // loop over all JSON elements again to connect parent activity elements with their child elements
        for (JsonElement elem : modelElements) {
            JsonObject element = elem.getAsJsonObject();

            if (element.has(JSONMapping.ELEMENT_OWNER) && !element.get(JSONMapping.ELEMENT_OWNER).isJsonNull()) {
                String parentActivityId = element.get(JSONMapping.ELEMENT_OWNER).getAsString();
                UMLActivity parentActivity = umlActivityMap.get(parentActivityId);
                UMLActivityElement childElement = umlActivityElementMap.get(element.get(JSONMapping.ELEMENT_ID).getAsString());

                if (parentActivity != null && childElement != null) {
                    parentActivity.addChildElement(childElement);
                    childElement.setParentActivity(parentActivity);
                }
            }
        }

        // loop over all JSON control flow elements and create control flow objects
        for (JsonElement controlFlow : controlFlows) {
            UMLControlFlow newControlFlow = parseControlFlow(controlFlow.getAsJsonObject(), umlActivityElementMap);
            umlControlFlowList.add(newControlFlow);
        }

        return new UMLActivityDiagram(modelSubmissionId, umlActivityNodeList, new ArrayList<>(umlActivityMap.values()), umlControlFlowList);
    }

    /**
     * Parses the given JSON representation of a UML activity node to a UMLActivityNode Java object.
     *
     * @param activityNodeType the type of the activity node
     * @param activityNodeJson the JSON object containing the activity
     * @return the UMLActivityNode object parsed from the JSON object
     */
    private static UMLActivityNode parseActivityNode(String activityNodeType, JsonObject activityNodeJson) {
        UMLActivityNodeType nodeType = UMLActivityNodeType.valueOf(activityNodeType);
        String jsonElementId = activityNodeJson.get(JSONMapping.ELEMENT_ID).getAsString();
        String nodeName = activityNodeJson.get(JSONMapping.ELEMENT_NAME).getAsString();
        return new UMLActivityNode(nodeName, jsonElementId, nodeType);
    }

    /**
     * Parses the given JSON representation of a UML activity to a UMLActivity Java object.
     *
     * @param activityJson the JSON object containing the activity
     * @return the UMLActivity object parsed from the JSON object
     */
    private static UMLActivity parseActivity(JsonObject activityJson) {
        String jsonElementId = activityJson.get(JSONMapping.ELEMENT_ID).getAsString();
        String activityName = activityJson.get(JSONMapping.ELEMENT_NAME).getAsString();
        return new UMLActivity(activityName, new ArrayList<>(), jsonElementId);
    }

    /**
     * Parses the given JSON representation of a UML control flow to a UMLControlFlow Java object.
     *
     * @param controlFlowJson the JSON object containing the control flow
     * @param activityElementMap a map containing all activity elements of the corresponding activity diagram, necessary for assigning source and target element of the control flow
     * @return the UMLControlFlow object parsed from the JSON object
     * @throws IOException when no activity elements could be found in the activityElementMap for the source and target ID in the JSON object
     */
    private static UMLControlFlow parseControlFlow(JsonObject controlFlowJson, Map<String, UMLActivityElement> activityElementMap) throws IOException {
        JsonObject source = controlFlowJson.getAsJsonObject(JSONMapping.RELATIONSHIP_SOURCE);
        JsonObject target = controlFlowJson.getAsJsonObject(JSONMapping.RELATIONSHIP_TARGET);

        String sourceJSONID = source.get(JSONMapping.RELATIONSHIP_ENDPOINT_ID).getAsString();
        String targetJSONID = target.get(JSONMapping.RELATIONSHIP_ENDPOINT_ID).getAsString();

        UMLActivityElement sourceElement = activityElementMap.get(sourceJSONID);
        UMLActivityElement targetElement = activityElementMap.get(targetJSONID);

        if (sourceElement != null && targetElement != null) {
            return new UMLControlFlow(sourceElement, targetElement, controlFlowJson.get(JSONMapping.ELEMENT_ID).getAsString());
        }
        else {
            throw new IOException("Control flow source or target not part of model!");
        }
    }

    /**
     * Create a map from the elements of the given JSON array. Every entry contains the ID of the element as key and the corresponding JSON element as value.
     *
     * @param elements a JSON array of elements from which the map should be created
     * @return a map that maps elementId -> element
     */
    // TODO: why is this done more than once?
    private static Map<String, JsonObject> generateJsonElementMap(JsonArray elements) {
        Map<String, JsonObject> jsonElementMap = new HashMap<>();
        elements.forEach(element -> jsonElementMap.put(element.getAsJsonObject().get("id").getAsString(), element.getAsJsonObject()));
        return jsonElementMap;
    }
}
