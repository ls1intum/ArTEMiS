package de.tum.in.www1.artemis.service.compass.umlmodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.assertj.core.data.Offset;

import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.modeling.ModelingSubmission;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.service.compass.umlmodel.component.UMLComponent;
import de.tum.in.www1.artemis.service.compass.umlmodel.component.UMLComponentDiagram;
import de.tum.in.www1.artemis.service.compass.umlmodel.component.UMLComponentInterface;
import de.tum.in.www1.artemis.service.compass.umlmodel.component.UMLComponentRelationship;
import de.tum.in.www1.artemis.service.plagiarism.ModelingPlagiarismDetectionService;

public abstract class AbstractUMLDiagramTest {

    protected ModelingPlagiarismDetectionService modelingPlagiarismDetectionService = new ModelingPlagiarismDetectionService();

    protected void compareSubmissions(ModelingSubmission modelingSubmission1, ModelingSubmission modelingSubmission2, double minimumSimilarity, double expectedSimilarity) {
        // not really necessary, but avoids issues.
        modelingSubmission1.setId(1L);
        modelingSubmission2.setId(2L);

        var comparisonResult = modelingPlagiarismDetectionService.compareSubmissions(List.of(modelingSubmission1, modelingSubmission2), minimumSimilarity, 1, 0);
        assertThat(comparisonResult).isNotNull();
        assertThat(comparisonResult).hasSize(1);
        assertThat(comparisonResult.get(0).getSimilarity()).isEqualTo(expectedSimilarity, Offset.offset(0.01));
    }

    protected UMLComponent getComponent(UMLComponentDiagram componentDiagram, String name) {
        return componentDiagram.getComponentList().stream().filter(component -> component.getName().equals(name)).findFirst().get();
    }

    protected UMLComponentInterface getInterface(UMLComponentDiagram componentDiagram, String name) {
        return componentDiagram.getComponentInterfaceList().stream().filter(componentInterface -> componentInterface.getName().equals(name)).findFirst().get();
    }

    protected UMLComponentRelationship getRelationship(UMLComponentDiagram componentDiagram, UMLElement source, UMLElement target) {
        // Source and target do not really matter in this test so we can also check the other way round
        return componentDiagram.getComponentRelationshipList().stream().filter(relationship -> (relationship.getSource().equals(source) && relationship.getTarget().equals(target))
                || (relationship.getSource().equals(target) && relationship.getTarget().equals(source))).findFirst().get();
    }

    protected ModelingSubmission modelingSubmission(String model) {
        var submission = new ModelingSubmission();
        submission.setModel(model);
        var participation = new StudentParticipation();
        var user = new User();
        user.setLogin("student");
        participation.setParticipant(user);
        submission.setParticipation(participation);
        return submission;
    }
}
