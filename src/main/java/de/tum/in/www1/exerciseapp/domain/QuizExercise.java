package de.tum.in.www1.exerciseapp.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

/**
 * A QuizExercise.
 */
@Entity
@DiscriminatorValue(value="Q")
//@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class QuizExercise extends Exercise implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "description")
    private String description;

    @Column(name = "explanation")
    private String explanation;

    @Column(name = "randomize_question_order")
    private Boolean randomizeQuestionOrder;

    @Column(name = "allowed_number_of_attempts")
    private Integer allowedNumberOfAttempts;

    @Column(name = "is_visible_before_start")
    private Boolean isVisibleBeforeStart;

    @Column(name = "is_open_for_practice")
    private Boolean isOpenForPractice;

    @Column(name = "is_planned_to_start")
    private Boolean isPlannedToStart;

    /**
     * The duration of the quiz exercise in seconds
     */
    @Column(name = "duration")
    private Integer duration;

    @OneToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER, orphanRemoval=true)
    @JoinColumn(unique = true)
    private QuizPointStatistic quizPointStatistic;

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, orphanRemoval=true)
    @OrderColumn
    @JoinColumn(name="exercise_id")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<Question> questions = new ArrayList<>();

    public String getDescription() {
        return description;
    }

    public QuizExercise description(String description) {
        this.description = description;
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExplanation() {
        return explanation;
    }

    public QuizExercise explanation(String explanation) {
        this.explanation = explanation;
        return this;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Boolean isRandomizeQuestionOrder() {
        return randomizeQuestionOrder;
    }

    public QuizExercise randomizeQuestionOrder(Boolean randomizeQuestionOrder) {
        this.randomizeQuestionOrder = randomizeQuestionOrder;
        return this;
    }

    public void setRandomizeQuestionOrder(Boolean randomizeQuestionOrder) {
        this.randomizeQuestionOrder = randomizeQuestionOrder;
    }

    public Integer getAllowedNumberOfAttempts() {
        return allowedNumberOfAttempts;
    }

    public QuizExercise allowedNumberOfAttempts(Integer allowedNumberOfAttempts) {
        this.allowedNumberOfAttempts = allowedNumberOfAttempts;
        return this;
    }

    public void setAllowedNumberOfAttempts(Integer allowedNumberOfAttempts) {
        this.allowedNumberOfAttempts = allowedNumberOfAttempts;
    }

    public Boolean isIsVisibleBeforeStart() {
        return isVisibleBeforeStart;
    }

    public QuizExercise isVisibleBeforeStart(Boolean isVisibleBeforeStart) {
        this.isVisibleBeforeStart = isVisibleBeforeStart;
        return this;
    }

    public void setIsVisibleBeforeStart(Boolean isVisibleBeforeStart) {
        this.isVisibleBeforeStart = isVisibleBeforeStart;
    }

    public Boolean isIsOpenForPractice() {
        return isOpenForPractice;
    }

    public QuizExercise isOpenForPractice(Boolean isOpenForPractice) {
        this.isOpenForPractice = isOpenForPractice;
        return this;
    }

    public void setIsOpenForPractice(Boolean isOpenForPractice) {
        this.isOpenForPractice = isOpenForPractice;
    }

    public Boolean isIsPlannedToStart() {
        return isPlannedToStart;
    }

    public QuizExercise isPlannedToStart(Boolean isPlannedToStart) {
        this.isPlannedToStart = isPlannedToStart;
        return this;
    }

    public void setIsPlannedToStart(Boolean isPlannedToStart) {
        this.isPlannedToStart = isPlannedToStart;
    }

    public Integer getDuration() {
        return duration;
    }

    public QuizExercise duration(Integer duration) {
        this.duration = duration;
        return this;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public QuizPointStatistic getQuizPointStatistic() {
        return quizPointStatistic;
    }

    public QuizExercise quizPointStatistic(QuizPointStatistic quizPointStatistic) {
        this.quizPointStatistic = quizPointStatistic;
        return this;
    }

    public void setQuizPointStatistic(QuizPointStatistic quizPointStatistic) {
        this.quizPointStatistic = quizPointStatistic;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public QuizExercise questions(List<Question> questions) {
        this.questions = questions;
        recalculatePointCounters();
        return this;
    }

    public QuizExercise addQuestions(Question question) {
        this.questions.add(question);
        question.setExercise(this);
        recalculatePointCounters();
        return this;
    }

    public QuizExercise removeQuestions(Question question) {
        this.questions.remove(question);
        question.setExercise(null);
        recalculatePointCounters();
        return this;
    }

    public void setQuestions(List<Question> questions) {

        this.questions = questions;
        recalculatePointCounters();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QuizExercise quizExercise = (QuizExercise) o;
        if (quizExercise.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), quizExercise.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "QuizExercise{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", description='" + getDescription() + "'" +
            ", explanation='" + getExplanation() + "'" +
            ", randomizeQuestionOrder='" + isRandomizeQuestionOrder() + "'" +
            ", allowedNumberOfAttempts='" + getAllowedNumberOfAttempts() + "'" +
            ", isVisibleBeforeStart='" + isIsVisibleBeforeStart() + "'" +
            ", isOpenForPractice='" + isIsOpenForPractice() + "'" +
            ", isPlannedToStart='" + isIsPlannedToStart() + "'" +
            ", duration='" + getDuration() + "'" +
            ", questions='" + getQuestions() + "'" +
            "}";
    }

    public QuizExercise(){
        quizPointStatistic = new QuizPointStatistic();
        quizPointStatistic.setQuiz(this);
    }

    private void recalculatePointCounters(){

        double quizScore = 0.0;

        //calculate Score of the Quiz
        for(Question question: questions){
            quizScore = quizScore + question.getScore();
        }
        //add new Scores
        for(double i = 0.0 ; i <= quizScore; i++){  // for variable ScoreSteps change: i++ into: i= i + scoreStep
            quizPointStatistic.addScore(new Double(i));
        }
        //delete old PointCounter
        Set<PointCounter> delete = new HashSet<>();
        for (PointCounter pointCounter : quizPointStatistic.getPointCounters()) {
            if (pointCounter.getId() != null) {                                               // for variable ScoreSteps
                if(pointCounter.getPoints() > quizScore || pointCounter.getPoints() < 0 /*|| (pointCounter.getPoints()% pointStep) != 0*/){ ;
                    delete.add(pointCounter);
                    pointCounter.setQuizPointStatistic(null);
                }
            }
        }
        quizPointStatistic.getPointCounters().removeAll(delete);
    }
}
