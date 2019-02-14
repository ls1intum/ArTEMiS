package de.tum.in.www1.artemis.domain;

import com.fasterxml.jackson.annotation.*;
import de.tum.in.www1.artemis.domain.view.QuizView;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A SubmittedAnswer.
 */
@Entity
@Table(name = "submitted_answer")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
    name="discriminator",
    discriminatorType=DiscriminatorType.STRING
)
@DiscriminatorValue(value="S")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)

// add JsonTypeInfo and JsonSubTypes annotation to help Jackson decide which class the JSON should be deserialized to
// depending on the value of the "type" property.
// Note: The "type" property has to be added on the front-end when making a request that includes a SubmittedAnswer Object
// However, the "type" property will be automatically added by Jackson when an object is serialized
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="type")
@JsonSubTypes({
    @JsonSubTypes.Type(value=MultipleChoiceSubmittedAnswer.class, name="multiple-choice"),
    @JsonSubTypes.Type(value=DragAndDropSubmittedAnswer.class, name="drag-and-drop"),
    @JsonSubTypes.Type(value = ShortAnswerSubmittedAnswer.class, name = "short-answer")
})
public abstract class SubmittedAnswer implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(QuizView.Before.class)
    private Long id;

    @Column(name = "score_in_points")
    @JsonView(QuizView.After.class)
    private Double scoreInPoints;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JsonIgnoreProperties({"questionStatistic", "exercise"})
    @JsonView(QuizView.Before.class)
    private Question question;

    @ManyToOne
    @JsonIgnore
    private QuizSubmission submission;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getScoreInPoints() {
        return scoreInPoints;
    }

    public SubmittedAnswer scoreInPoints(Double scoreInPoints) {
        this.scoreInPoints = scoreInPoints;
        return this;
    }

    public void setScoreInPoints(Double scoreInPoints) {
        this.scoreInPoints = scoreInPoints;
    }

    public Question getQuestion() {
        return question;
    }

    public SubmittedAnswer question(Question question) {
        this.question = question;
        return this;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public QuizSubmission getSubmission() {
        return submission;
    }

    public SubmittedAnswer submission(QuizSubmission quizSubmission) {
        this.submission = quizSubmission;
        return this;
    }

    public void setSubmission(QuizSubmission quizSubmission) {
        this.submission = quizSubmission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubmittedAnswer submittedAnswer = (SubmittedAnswer) o;
        if (submittedAnswer.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), submittedAnswer.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "SubmittedAnswer{" +
            "id=" + getId() +
            ", scoreInPoints='" + getScoreInPoints() + "'" +
            "}";
    }

    /**
     * Delete all references to question and question-elements if the quiz was changed
     *
     * @param quizExercise the changed quizExercise-object
     */
    public abstract void checkAndDeleteReferences (QuizExercise quizExercise);
}
