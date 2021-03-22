package de.tum.in.www1.artemis.web.rest.dto;

import java.util.List;

import de.tum.in.www1.artemis.domain.Exercise;

public class CourseManagementOverviewDTO {

    private Long courseId;

    private List<Exercise> exerciseDetails;

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public List<Exercise> getExerciseDetails() {
        return exerciseDetails;
    }

    public void setExerciseDetails(List<Exercise> exerciseDetails) {
        this.exerciseDetails = exerciseDetails;
    }
}
