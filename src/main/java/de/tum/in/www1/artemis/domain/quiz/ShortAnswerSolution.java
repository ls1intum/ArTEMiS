package de.tum.in.www1.artemis.domain.quiz;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.*;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;

import de.tum.in.www1.artemis.domain.DomainObject;
import de.tum.in.www1.artemis.domain.view.QuizView;

/**
 * A ShortAnswerSolution.
 */
@Entity
@Table(name = "short_answer_solution")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ShortAnswerSolution extends DomainObject {

    @Column(name = "text")
    @JsonView(QuizView.Before.class)
    private String text;

    @Column(name = "invalid")
    @JsonView(QuizView.Before.class)
    private Boolean invalid = false;

    @ManyToOne
    @JsonIgnore
    private ShortAnswerQuestion question;

    // added additionally, could possibly be created within artemis.jh?
    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "solution")
    @JsonIgnore
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<ShortAnswerMapping> mappings = new HashSet<>();

    /**
     * tempID is needed to refer to solutions that have not been persisted yet in the correctMappings of a question (so user can create mappings in the UI before saving new
     * solutions)
     */
    @Transient
    // variable name must be different from Getter name,
    // so that Jackson ignores the @Transient annotation,
    // but Hibernate still respects it
    private Long tempIDTransient;

    public Long getTempID() {
        return tempIDTransient;
    }

    public void setTempID(Long tempID) {
        this.tempIDTransient = tempID;
    }

    public String getText() {
        return text;
    }

    public ShortAnswerSolution text(String text) {
        this.text = text;
        return this;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean isInvalid() {
        return invalid != null && invalid;
    }

    public void setInvalid(Boolean invalid) {
        this.invalid = invalid;
    }

    public ShortAnswerQuestion getQuestion() {
        return question;
    }

    public void setQuestion(ShortAnswerQuestion shortAnswerQuestion) {
        this.question = shortAnswerQuestion;
    }

    public Set<ShortAnswerMapping> getMappings() {
        return mappings;
    }

    public ShortAnswerSolution addMappings(ShortAnswerMapping mapping) {
        this.mappings.add(mapping);
        mapping.setSolution(this);
        return this;
    }

    public ShortAnswerSolution removeMappings(ShortAnswerMapping mapping) {
        this.mappings.remove(mapping);
        mapping.setSolution(null);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ShortAnswerSolution shortAnswerSolution = (ShortAnswerSolution) o;
        if (shortAnswerSolution.getTempID() != null && getTempID() != null && Objects.equals(getTempID(), shortAnswerSolution.getTempID())) {
            return true;
        }
        return super.equals(o);
    }

    @Override
    public String toString() {
        return "ShortAnswerSolution{" + "id=" + getId() + ", text='" + getText() + "'" + ", invalid='" + isInvalid() + "'" + "}";
    }
}
