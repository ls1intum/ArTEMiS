package de.tum.in.www1.artemis.domain.participation;

import java.util.Optional;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.view.QuizView;

@Entity
@DiscriminatorValue(value = "SP")
public class StudentParticipation extends Participation {

    private static final long serialVersionUID = 1L;

    @Column(name = "presentation_score")
    private Integer presentationScore;

    @ManyToOne
    @JsonView(QuizView.Before.class)
    private User student;

    @ManyToOne
    @JsonView(QuizView.Before.class)
    private Team team;

    public Integer getPresentationScore() {
        return presentationScore;
    }

    public StudentParticipation presentationScore(Integer presentationScore) {
        this.presentationScore = presentationScore;
        return this;
    }

    public void setPresentationScore(Integer presentationScore) {
        this.presentationScore = presentationScore;
    }

    // public User getStudent() {
    // return student;
    // }
    //
    // public Participation student(User user) {
    // this.student = user;
    // return this;
    // }
    //
    // public void setStudent(User user) {
    // this.student = user;
    // }

    public Team getTeam() {
        return team;
    }

    public Participation team(Team team) {
        this.team = team;
        return this;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public boolean isTeamParticipation() {
        return Optional.ofNullable(team).isPresent();
    }

    @JsonIgnore
    public Object getParticipant() {
        return Optional.ofNullable((Object) student).orElse(team);
    }

    public void setParticipant(Object participant) {
        if (participant instanceof User) {
            this.student = (User) participant;
        }
        else if (participant instanceof Team) {
            this.team = (Team) participant;
        }
        else if (participant == null) {
            this.student = null;
            this.team = null;
        }
        else {
            throw new Error("Unknown participant type");
        }
    }

    @JsonIgnore
    public String getParticipantIdentifier() {
        return Optional.ofNullable(student).map(User::getLogin).orElse(team.getShortName());
    }

    public Exercise getExercise() {
        return exercise;
    }

    public StudentParticipation exercise(Exercise exercise) {
        this.exercise = exercise;
        return this;
    }

    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
    }

    /**
     * Removes the student or team from the participation, can be invoked to make sure that sensitive information is not sent to the client.
     * E.g. tutors should not see information about the student.
     */
    public void filterSensitiveInformation() {
        // setStudent(null);
        setTeam(null);
    }

    public boolean isOwner(String userLogin) {
        return Optional.ofNullable(student).map(student -> student.getLogin().equals(userLogin)).orElse(team.hasStudentWithLogin(userLogin));
    }

    public boolean isOwner(User user) {
        return Optional.ofNullable(student).map(student -> student.equals(user)).orElse(team.hasStudent(user));
    }

    @Override
    public String toString() {
        String participantString = Optional.ofNullable(student).map(student -> "student=" + student).orElse("team=" + team);
        return "StudentParticipation{" + "id=" + getId() + ", presentationScore=" + presentationScore + ", " + participantString + "}";
    }

    @Override
    public Participation copyParticipationId() {
        var participation = new StudentParticipation();
        participation.setId(getId());
        return participation;
    }
}
