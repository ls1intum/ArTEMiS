package de.tum.in.www1.artemis.service.compass.umlmodel.activitydiagram;

import static de.tum.in.www1.artemis.service.compass.umlmodel.activitydiagram.UMLActivityNode.UMLActivityNodeType.ACTIVITY_ACTION_NODE;
import static de.tum.in.www1.artemis.service.compass.umlmodel.activitydiagram.UMLActivityNode.UMLActivityNodeType.ACTIVITY_FINAL_NODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import me.xdrop.fuzzywuzzy.FuzzySearch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class UMLActivityNodeTest {

    private UMLActivityNode activityNode;

    @Mock
    UMLActivityNode referenceNode;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        activityNode = new UMLActivityNode("myActivityNode", "activityNodeId", ACTIVITY_ACTION_NODE);

        when(referenceNode.getType()).thenReturn("ActivityActionNode");
    }

    @Test
    void similarity_null() {
        double similarity = activityNode.similarity(null);

        assertThat(similarity).isEqualTo(0);
    }

    @Test
    void similarity_differentElementType() {
        double similarity = activityNode.similarity(mock(UMLControlFlow.class));

        assertThat(similarity).isEqualTo(0);
    }

    @Test
    void similarity_sameActivityNode() {
        when(referenceNode.getName()).thenReturn("myActivityNode");

        double similarity = activityNode.similarity(referenceNode);

        assertThat(similarity).isEqualTo(1);
    }

    @Test
    void similarity_differentName() {
        when(referenceNode.getName()).thenReturn("otherElementName");
        double expectedSimilarity = FuzzySearch.ratio("myActivityNode", "otherElementName") / 100.0;

        double similarity = activityNode.similarity(referenceNode);

        assertThat(similarity).isEqualTo(expectedSimilarity);
    }

    @Test
    void similarity_nullReferenceName() {
        when(referenceNode.getName()).thenReturn(null);

        double similarity = activityNode.similarity(referenceNode);

        assertThat(similarity).isEqualTo(0);
    }

    @Test
    void similarity_bothNamesNull() {
        activityNode = new UMLActivityNode(null, "activityNodeId", ACTIVITY_ACTION_NODE);
        when(referenceNode.getName()).thenReturn(null);

        double similarity = activityNode.similarity(referenceNode);

        assertThat(similarity).isEqualTo(1);
    }

    @Test
    void similarity_bothNamesEmpty() {
        activityNode = new UMLActivityNode("", "activityNodeId", ACTIVITY_ACTION_NODE);
        when(referenceNode.getName()).thenReturn("");

        double similarity = activityNode.similarity(referenceNode);

        assertThat(similarity).isEqualTo(1);
    }

    @Test
    void similarity_differentNodeTypesWithoutName() {
        activityNode = new UMLActivityNode("", "finalNodeId", ACTIVITY_FINAL_NODE);
        when(referenceNode.getType()).thenReturn("ActivityInitialNode");
        when(referenceNode.getName()).thenReturn("");

        double similarity = activityNode.similarity(referenceNode);

        assertThat(similarity).isEqualTo(0);
    }

    @Test
    void getType() {
        String nodeType = activityNode.getType();

        assertThat(nodeType).isEqualTo("ActivityActionNode");
    }
}
