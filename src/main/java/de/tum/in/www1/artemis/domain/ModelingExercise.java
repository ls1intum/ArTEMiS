package de.tum.in.www1.artemis.domain;

import de.tum.in.www1.artemis.domain.enumeration.DiagramType;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A ModelingExercise.
 */
@Entity
@DiscriminatorValue(value="M")
//@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ModelingExercise extends Exercise implements Serializable {

    private static final long serialVersionUID = 1L;

    @Enumerated(EnumType.STRING)
    @Column(name = "diagram_type")
    private DiagramType diagramType;

    @Column(name = "sample_solution_model")
    @Lob
    private String sampleSolutionModel;

    @Column(name = "sample_solution_explanation")
    @Lob
    private String sampleSolutionExplanation;

    // jhipster-needle-entity-add-field - Jhipster will add fields here, do not remove

    public DiagramType getDiagramType() {
        return diagramType;
    }

    public ModelingExercise diagramType(DiagramType diagramType) {
        this.diagramType = diagramType;
        return this;
    }

    public void setDiagramType(DiagramType diagramType) {
        this.diagramType = diagramType;
    }

    public String getSampleSolutionModel() {
        return sampleSolutionModel;
    }

    public ModelingExercise sampleSolutionModel(String sampleSolutionModel) {
        this.sampleSolutionModel = sampleSolutionModel;
        return this;
    }

    public void setSampleSolutionModel(String sampleSolutionModel) {
        this.sampleSolutionModel = sampleSolutionModel;
    }

    public String getSampleSolutionExplanation() {
        return sampleSolutionExplanation;
    }

    public ModelingExercise sampleSolutionExplanation(String sampleSolutionExplanation) {
        this.sampleSolutionExplanation = sampleSolutionExplanation;
        return this;
    }

    public void setSampleSolutionExplanation(String sampleSolutionExplanation) {
        this.sampleSolutionExplanation = sampleSolutionExplanation;
    }

    // jhipster-needle-entity-add-getters-setters - Jhipster will add getters and setters here, do not remove

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ModelingExercise modelingExercise = (ModelingExercise) o;
        if (modelingExercise.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), modelingExercise.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "ModelingExercise{" +
            "id=" + getId() +
            ", maxScore='" + getMaxScore() + "'" +
            ", diagramType='" + getDiagramType() + "'" +
            ", sampleSolutionModel='" + getSampleSolutionModel() + "'" +
            ", sampleSolutionExplanation='" + getSampleSolutionExplanation() + "'" +
            "}";
    }
}
