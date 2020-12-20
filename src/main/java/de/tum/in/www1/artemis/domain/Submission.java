package de.tum.in.www1.artemis.domain;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.*;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DiscriminatorOptions;

import com.fasterxml.jackson.annotation.*;

import de.tum.in.www1.artemis.domain.enumeration.SubmissionType;
import de.tum.in.www1.artemis.domain.modeling.ModelingSubmission;
import de.tum.in.www1.artemis.domain.participation.Participation;
import de.tum.in.www1.artemis.domain.quiz.QuizSubmission;
import de.tum.in.www1.artemis.domain.view.QuizView;

/**
 * A Submission.
 */
@Entity
@Table(name = "submission")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "discriminator", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue(value = "S")
@DiscriminatorOptions(force = true)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "submissionExerciseType")
// Annotation necessary to distinguish between concrete implementations of Submission when deserializing from JSON
@JsonSubTypes({ @JsonSubTypes.Type(value = ProgrammingSubmission.class, name = "programming"), @JsonSubTypes.Type(value = ModelingSubmission.class, name = "modeling"),
        @JsonSubTypes.Type(value = QuizSubmission.class, name = "quiz"), @JsonSubTypes.Type(value = TextSubmission.class, name = "text"),
        @JsonSubTypes.Type(value = FileUploadSubmission.class, name = "file-upload"), })
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class Submission extends DomainObject {

    @Column(name = "submitted")
    @JsonView(QuizView.Before.class)
    private Boolean submitted;

    @Enumerated(EnumType.STRING)
    @Column(name = "jhi_type")
    @JsonView(QuizView.Before.class)
    private SubmissionType type;

    @Column(name = "example_submission")
    private Boolean exampleSubmission;

    @ManyToOne
    private Participation participation;

    @JsonIgnore
    @OneToMany(mappedBy = "submission", cascade = CascadeType.REMOVE)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<SubmissionVersion> versions = new HashSet<>();

    /**
     * A submission can have multiple results, therefore, results are persisted and removed with a submission.
     */
    @OneToMany(mappedBy = "submission", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn
    @JsonIgnoreProperties({ "submission", "participation" })
    private List<Result> results = new ArrayList<>();

    @Column(name = "submission_date")
    private ZonedDateTime submissionDate;

    @JsonView(QuizView.Before.class)
    public ZonedDateTime getSubmissionDate() {
        return submissionDate;
    }

    /**
     * Calculates the duration of a submission in minutes and adds it into the json response
     *
     * @return duration in minutes or null if it can not be determined
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Long getDurationInMinutes() {
        if (this.participation == null || this.participation.getInitializationDate() == null || this.submissionDate == null) {
            return null;
        }

        ZonedDateTime initilizationDate = this.participation.getInitializationDate();
        ZonedDateTime submissionDate = this.getSubmissionDate();

        return Duration.between(initilizationDate, submissionDate).toMinutes();
    }

    /**
     * Get the latest result of the submission
     *
     * @return a {@link Result} or null
     */
    @Nullable
    @JsonIgnore
    public Result getLatestResult() {
        if (results != null && !results.isEmpty()) {
            return results.get(results.size() - 1);
        }
        return null;
    }

    @Nullable
    @JsonProperty(value = "results", access = JsonProperty.Access.READ_ONLY)
    public List<Result> getResults() {
        return results;
    }

    /**
     * Get the first result of the submission
     *
     * @return a {@link Result} or null if no result is present
     */
    @Nullable
    @JsonIgnore
    public Result getFirstResult() {
        if (results != null && !results.isEmpty()) {
            return results.get(0);
        }
        return null;
    }

    /**
     * Add a result to the list.
     * NOTE: You must make sure to correctly persist the result in the database!
     *
     * @param result the {@link Result} which should be added
     */
    public void addResult(Result result) {
        this.results.add(result);
    }

    /**
     * Set the results list to the specified list.
     * NOTE: You must correctly persist this change in the database manually!
     *
     * @param results The list of {@link Result} which should replace the existing results of the submission
     */
    @JsonProperty(value = "results", access = JsonProperty.Access.WRITE_ONLY)
    public void setResults(List<Result> results) {
        this.results = results;
    }

    public Participation getParticipation() {
        return participation;
    }

    public void setParticipation(Participation participation) {
        this.participation = participation;
    }

    public Submission submissionDate(ZonedDateTime submissionDate) {
        this.submissionDate = submissionDate;
        return this;
    }

    public void setSubmissionDate(ZonedDateTime submissionDate) {
        this.submissionDate = submissionDate;
    }

    public Boolean isSubmitted() {
        return submitted != null ? submitted : false;
    }

    public Submission submitted(Boolean submitted) {
        this.submitted = submitted;
        return this;
    }

    public void setSubmitted(Boolean submitted) {
        this.submitted = submitted;
    }

    public SubmissionType getType() {
        return type;
    }

    public Submission type(SubmissionType type) {
        this.type = type;
        return this;
    }

    public void setType(SubmissionType type) {
        this.type = type;
    }

    public Boolean isExampleSubmission() {
        return exampleSubmission;
    }

    public void setExampleSubmission(Boolean exampleSubmission) {
        this.exampleSubmission = exampleSubmission;
    }

    /**
     * determine whether a submission is empty, i.e. the student did not work properly on the corresponding exercise
     * @return whether the submission is empty (true) or not (false)
     */
    public abstract boolean isEmpty();
}
