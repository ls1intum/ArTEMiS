package de.tum.in.www1.artemis.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import de.tum.in.www1.artemis.domain.view.QuizView;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A ShortAnswerSubmittedText.
 */
@Entity
@Table(name = "short_answer_submitted_text")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ShortAnswerSubmittedText implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(QuizView.Before.class)
    private Long id;

    @Column(name = "text")
    @JsonView(QuizView.Before.class)
    private String text;

    @Column(name = "isCorrect")
    @JsonView(QuizView.Before.class)
    private Boolean isCorrect = false;

    @OneToOne
    @JoinColumn()
    @JsonView(QuizView.Before.class)
    private ShortAnswerSpot spot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private ShortAnswerSubmittedAnswer submittedAnswer;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public ShortAnswerSubmittedText text(String text) {
        this.text = text;
        return this;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean isIsCorrect() {
        return isCorrect;
    }

    public ShortAnswerSubmittedText isCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
        return this;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public ShortAnswerSpot getSpot() {
        return spot;
    }

    public ShortAnswerSubmittedText spot(ShortAnswerSpot shortAnswerSpot) {
        this.spot = shortAnswerSpot;
        return this;
    }

    public void setSpot(ShortAnswerSpot shortAnswerSpot) {
        this.spot = shortAnswerSpot;
    }

    public ShortAnswerSubmittedAnswer getSubmittedAnswer() {
        return submittedAnswer;
    }

    public ShortAnswerSubmittedText submittedAnswer(ShortAnswerSubmittedAnswer shortAnswerSubmittedAnswer) {
        this.submittedAnswer = shortAnswerSubmittedAnswer;
        return this;
    }

    public void setSubmittedAnswer(ShortAnswerSubmittedAnswer shortAnswerSubmittedAnswer) {
        this.submittedAnswer = shortAnswerSubmittedAnswer;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here, do not remove

    /**
     * This function checks if the submittedText (typos included) matches the solution.
     * There can be max 2 typos at all.
     * solutions with length 0-4 allow 0 typos
     * solutions with length 4-8 allow 1 typo
     * solutions with length 8< allow 2 typos
     *
     * @param submittedText
     * @param solution
     * @return boolean true if submittedText fits the restrictions above, false when not
     */
    public boolean isSubmittedTextCorrect(String submittedText, String solution) {
        LevenshteinDistance distance = new LevenshteinDistance();
        int numberOfTypos = distance.apply(submittedText.toLowerCase(), solution.toLowerCase());
        if ( (solution.length() < 4 && numberOfTypos > 0)
            || ((solution.length() >= 4 && solution.length() <= 8) && numberOfTypos > 2)
            || (solution.length() > 8 && numberOfTypos > 3) ) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ShortAnswerSubmittedText shortAnswerSubmittedText = (ShortAnswerSubmittedText) o;
        if (shortAnswerSubmittedText.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), shortAnswerSubmittedText.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "ShortAnswerSubmittedText{" +
            "id=" + getId() +
            ", text='" + getText() + "'" +
            ", isCorrect='" + isIsCorrect() + "'" +
            "}";
    }
}
