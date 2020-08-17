package de.tum.in.www1.artemis.service.compass.umlmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import de.tum.in.www1.artemis.service.compass.assessment.CompassResult;
import de.tum.in.www1.artemis.service.compass.umlmodel.activity.UMLActivityDiagram;

class UMLDiagramTest {

    @Spy
    UMLDiagram umlDiagram;

    @Spy
    UMLDiagram referenceDiagram;

    @Spy
    UMLElement umlElement1;

    @Spy
    UMLElement umlElement2;

    @Spy
    UMLElement umlElement3;

    @Spy
    UMLElement referenceElement1;

    @Spy
    UMLElement referenceElement2;

    @Spy
    CompassResult compassResult;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        doReturn(List.of(umlElement1, umlElement2)).when(umlDiagram).getModelElements();
        doReturn(List.of(referenceElement1, referenceElement2)).when(referenceDiagram).getModelElements();

        mockOverallSimilarity(umlElement1, referenceElement1, 1.0);
        mockOverallSimilarity(umlElement1, referenceElement2, 1.0);
        mockOverallSimilarity(umlElement2, referenceElement1, 1.0);
        mockOverallSimilarity(umlElement2, referenceElement2, 1.0);
        mockOverallSimilarity(umlElement3, referenceElement1, 0.123);
        mockOverallSimilarity(umlElement3, referenceElement2, 0.456);
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(umlDiagram, umlElement1, umlElement2, umlElement3, referenceElement1, referenceElement2, referenceDiagram, compassResult);
    }

    @Test
    void similarity_null() {
        double similarity = umlDiagram.similarity(null);
        assertThat(similarity).isEqualTo(0);
    }

    @Test
    void similarity_differentDiagramType() {
        double similarity = umlDiagram.similarity(spy(UMLActivityDiagram.class));
        assertThat(similarity).isEqualTo(0);
    }

    @Test
    void similarity_sameDiagram() {
        double similarity = umlDiagram.similarity(referenceDiagram);
        assertThat(similarity).isEqualTo(1);
    }

    @Test
    void similarity_differentDiagrams() {
        when(umlDiagram.getModelElements()).thenReturn(List.of(umlElement1, umlElement2, umlElement3));
        mockOverallSimilarity(referenceElement1, umlElement1, 0.1);
        mockOverallSimilarity(referenceElement1, umlElement2, 0.2);
        mockOverallSimilarity(referenceElement1, umlElement3, 0.3);
        mockOverallSimilarity(referenceElement2, umlElement1, 0.4);
        mockOverallSimilarity(referenceElement2, umlElement2, 0.6);
        mockOverallSimilarity(referenceElement2, umlElement3, 0.5);
        double weight = 1.0 / 3;
        double expectedSimilarity = 0.3 * weight + 0.6 * weight;

        double similarity = umlDiagram.similarity(referenceDiagram);
        double symmetricSimilarity = referenceDiagram.similarity(umlDiagram);

        assertThat(similarity).isEqualTo(expectedSimilarity);
        assertThat(symmetricSimilarity).isEqualTo(similarity);
    }

    @Test
    void isUnassessed_true() {
        boolean isUnassessed = umlDiagram.isUnassessed();
        assertThat(isUnassessed).isTrue();
    }

    @Test
    void isUnassessed_false() {
        doReturn(spy(CompassResult.class)).when(umlDiagram).getLastAssessmentCompassResult();
        boolean isUnassessed = umlDiagram.isUnassessed();
        assertThat(isUnassessed).isFalse();
    }

    @Test
    void getLastAssessmentConfidence() {
        doReturn(compassResult).when(umlDiagram).getLastAssessmentCompassResult();
        doReturn(0.456).when(compassResult).getConfidence();
        double confidence = umlDiagram.getLastAssessmentConfidence();
        assertThat(confidence).isEqualTo(0.456);
    }

    @Test
    void getLastAssessmentConfidence_noCompassResult() {
        double confidence = umlDiagram.getLastAssessmentConfidence();
        assertThat(confidence).isEqualTo(-1);
    }

    @Test
    void getLastAssessmentCoverage() {
        doReturn(compassResult).when(umlDiagram).getLastAssessmentCompassResult();
        doReturn(0.789).when(compassResult).getCoverage();
        double confidence = umlDiagram.getLastAssessmentCoverage();
        assertThat(confidence).isEqualTo(0.789);
    }

    @Test
    void getLastAssessmentCoverage_noCompassResult() {
        double confidence = umlDiagram.getLastAssessmentCoverage();
        assertThat(confidence).isEqualTo(-1);
    }

    private void verifyNoElementInteraction() {
        verify(umlElement1, Mockito.never()).similarity(any());
        verify(umlElement1, Mockito.never()).overallSimilarity(any());
        verify(umlElement2, Mockito.never()).similarity(any());
        verify(umlElement2, Mockito.never()).overallSimilarity(any());
        verify(umlElement3, Mockito.never()).similarity(any());
        verify(umlElement3, Mockito.never()).overallSimilarity(any());
        verify(referenceElement1, Mockito.never()).similarity(any());
        verify(referenceElement1, Mockito.never()).overallSimilarity(any());
        verify(referenceElement2, Mockito.never()).similarity(any());
        verify(referenceElement2, Mockito.never()).overallSimilarity(any());
    }

    private void mockOverallSimilarity(Similarity<UMLElement> element1, Similarity<UMLElement> element2, double similarityValue) {
        when(element1.overallSimilarity(element2)).thenReturn(similarityValue);
        when(element2.overallSimilarity(element1)).thenReturn(similarityValue);
    }
}
