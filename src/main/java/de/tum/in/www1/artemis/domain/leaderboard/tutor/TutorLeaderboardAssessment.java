package de.tum.in.www1.artemis.domain.leaderboard.tutor;

import com.fasterxml.jackson.annotation.JsonInclude;

// Custom object for sql query
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TutorLeaderboardAssessment {

    private final long userId;

    private final long assessments;

    private final double points;

    public long getAssessments() {
        return assessments;
    }

    public double getPoints() {
        return points;
    }

    public long getUserId() {
        return userId;
    }

    public TutorLeaderboardAssessment(long userId, long assessments, double points) {
        this.userId = userId;
        this.assessments = assessments;
        this.points = points;
    }
}
