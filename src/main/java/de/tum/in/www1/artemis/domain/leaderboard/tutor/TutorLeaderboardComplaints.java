package de.tum.in.www1.artemis.domain.leaderboard.tutor;

import com.fasterxml.jackson.annotation.JsonInclude;

// Custom object for sql query
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TutorLeaderboardComplaints {

    private final long userId;

    private final long allComplaints;

    private final long acceptedComplaints;

    private final double points;

    public long getAllComplaints() {
        return allComplaints;
    }

    public long getAcceptedComplaints() {
        return acceptedComplaints;
    }

    public double getPoints() {
        return points;
    }

    public long getUserId() {
        return userId;
    }

    public TutorLeaderboardComplaints(long userId, long allComplaints, long acceptedComplaints, double points) {
        this.userId = userId;
        this.allComplaints = allComplaints;
        this.acceptedComplaints = acceptedComplaints;
        this.points = points;
    }
}
