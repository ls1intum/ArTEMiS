package de.tum.in.www1.artemis.service;

import java.security.Principal;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.security.AuthoritiesConstants;
import de.tum.in.www1.artemis.security.SecurityUtils;

/**
 * Service used to check whether user is authorized to perform actions on the entity.
 */
@Service
public class AuthorizationCheckService {

    private final Logger log = LoggerFactory.getLogger(AuthorizationCheckService.class);

    private final UserService userService;

    private Authority adminAuthority;

    public AuthorizationCheckService(UserService userService) {
        this.userService = userService;
        adminAuthority = new Authority();
        adminAuthority.setName(AuthoritiesConstants.ADMIN);
    }

    /**
     * Given any type of exercise, the method returns if the current user is at least TA for the course the exercise belongs to. If exercise is not present, it will return false,
     * because the optional will be empty, and therefore `isPresent()` will return false This is due how `filter` works: If a value is present, apply the provided mapping function
     * to it, and if the result is non-null, return an Optional describing the result. Otherwise return an empty Optional.
     * https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html#filter-java.util.function.Predicate-
     *
     * @param exercise the exercise that needed to be checked
     * @return true if the user is at least a teaching assistant
     */
    public <T extends Exercise> boolean isAtLeastTeachingAssistantForExercise(Optional<T> exercise) {
        return exercise.filter(this::isAtLeastTeachingAssistantForExercise).isPresent();
    }

    public boolean isAtLeastTeachingAssistantForExercise(Exercise exercise) {
        return isAtLeastTeachingAssistantInCourse(exercise.getCourse(), null);
    }

    public boolean isAtLeastTeachingAssistantForExercise(Optional<Exercise> exercise, User user) {
        return exercise.filter(observedExercise -> isAtLeastTeachingAssistantForExercise(observedExercise, user)).isPresent();
    }

    public boolean isAtLeastTeachingAssistantForExercise(Exercise exercise, User user) {
        return isAtLeastTeachingAssistantInCourse(exercise.getCourse(), user);
    }

    public boolean isAtLeastStudentForExercise(Exercise exercise) {
        return isStudentInCourse(exercise.getCourse(), null) || isAtLeastTeachingAssistantForExercise(exercise);
    }

    public boolean isAtLeastTeachingAssistantInCourse(Course course, User user) {
        if (user == null || user.getGroups() == null) {
            user = userService.getUserWithGroupsAndAuthorities();
        }
        return user.getGroups().contains(course.getInstructorGroupName()) || user.getGroups().contains(course.getTeachingAssistantGroupName()) || isAdmin();

    }

    public boolean isAtLeastInstructorForExercise(Exercise exercise) {
        return isAtLeastInstructorForCourse(exercise.getCourse(), null);
    }

    public boolean isAtLeastInstructorForCourse(Course course, User user) {
        if (user == null || user.getGroups() == null) {
            user = userService.getUserWithGroupsAndAuthorities();
        }
        return user.getGroups().contains(course.getInstructorGroupName()) || isAdmin();
    }

    /**
     * Method used to check whether the current logged in user is instructor of this course
     *
     * @param course course to check the rights for
     * @return true, if user is instructor of this course, otherwise false
     */
    public boolean isInstructorInCourse(Course course, User user) {
        if (user == null || user.getGroups() == null) {
            user = userService.getUserWithGroupsAndAuthorities();
        }
        return user.getGroups().contains(course.getInstructorGroupName());
    }

    /**
     * Method used to check whether the current logged in user is teaching assistant of this course
     *
     * @param course course to check the rights for
     * @return true, if user is teaching assistant of this course, otherwise false
     */
    public boolean isTeachingAssistantInCourse(Course course, User user) {
        if (user == null || user.getGroups() == null) {
            user = userService.getUserWithGroupsAndAuthorities();
        }
        return user.getGroups().contains(course.getTeachingAssistantGroupName());
    }

    /**
     * method used to check whether the current logged in user is only a student of this course. This means the user is NOT a tutor, NOT an instructor and NOT an ADMIN
     *
     * @param course course to check the rights for
     * @return true, if user is only student of this course, otherwise false
     */
    public boolean isOnlyStudentInCourse(Course course, User user) {
        if (user == null || user.getGroups() == null) {
            user = userService.getUserWithGroupsAndAuthorities();
        }
        return user.getGroups().contains(course.getStudentGroupName()) && !isAtLeastTeachingAssistantInCourse(course, user);
    }

    /**
     * method used to check whether the current logged in user is student of this course
     *
     * @param course course to check the rights for
     * @return true, if user is student of this course, otherwise false
     */
    public boolean isStudentInCourse(Course course, User user) {
        if (user == null || user.getGroups() == null) {
            user = userService.getUserWithGroupsAndAuthorities();
        }
        return user.getGroups().contains(course.getStudentGroupName());
    }

    /**
     * Method used to check whether the current logged in user is owner of this participation
     *
     * @param participation participation to check the rights for
     * @return true, if user is student is owner of this participation, otherwise false
     */
    public boolean isOwnerOfParticipation(Participation participation) {
        // A template/solution participation doesn't have a student, this is done to avoid null pointer exceptions
        if (participation.getStudent() == null) {
            return false;
        }
        else {
            return participation.getStudent().getLogin().equals(SecurityUtils.getCurrentUserLogin().get());
        }
    }

    /**
     * Method used to check whether the user of the websocket message is owner of this participation
     *
     * @param participation participation to check the rights for
     * @param principal     websocket user
     * @return true, if user is student is owner of this participation, otherwise false
     */
    public boolean isOwnerOfParticipation(Participation participation, Principal principal) {
        return participation.getStudent() != null && participation.getStudent().getLogin().equals(principal.getName());
    }

    /**
     * Method used to check whether the current logged in user is allowed to see this exercise
     *
     * @param exercise exercise to check the rights for
     * @return true, if user is allowed to see this exercise, otherwise false
     */
    public boolean isAllowedToSeeExercise(Exercise exercise, User user) {
        if (isAdmin()) {
            return true;
        }
        if (user == null || user.getGroups() == null) {
            user = userService.getUserWithGroupsAndAuthorities();
        }
        Course course = exercise.getCourse();
        return isInstructorInCourse(course, user) || isTeachingAssistantInCourse(course, user) || (isStudentInCourse(course, user) && exercise.isVisibleToStudents());
    }

    /**
     * Method used to check whether the current logged in user is application admin
     *
     * @return true, if user is admin, otherwise false
     */
    public boolean isAdmin() {
        return SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ADMIN);
    }

    /**
     * Checks if the current user is allowed to retrieve the given result. The user is allowed to retrieve the result if he is at least a student in the corresponding course, the
     * submission is his submission, the assessment due date of the corresponding exercise is in the past (or not set) and the result is finished.
     *
     * @param exercise      the corresponding exercise
     * @param participation the participation the result belongs to
     * @param result        the result that should be sent to the client
     * @return true if the user is allowed to retrieve the given result, false otherwise
     */
    public boolean isUserAllowedToGetResult(Exercise exercise, Participation participation, Result result) {
        return isAtLeastStudentForExercise(exercise) && isOwnerOfParticipation(participation)
                && (exercise.getAssessmentDueDate() == null || exercise.getAssessmentDueDate().isBefore(ZonedDateTime.now())) && result.getAssessor() != null
                && result.getCompletionDate() != null;
    }
}
