package de.tum.in.www1.artemis.service;

import static de.tum.in.www1.artemis.config.Constants.MAX_NUMBER_OF_LOCKED_SUBMISSIONS_PER_TUTOR;
import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.forbidden;
import static java.util.stream.Collectors.toList;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.AssessmentType;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.repository.StudentParticipationRepository;
import de.tum.in.www1.artemis.repository.SubmissionRepository;
import de.tum.in.www1.artemis.web.rest.dto.DueDateStat;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;
import de.tum.in.www1.artemis.web.rest.errors.EntityNotFoundException;

@Service
public class SubmissionService {

    private final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    protected SubmissionRepository submissionRepository;

    protected ResultRepository resultRepository;

    protected AuthorizationCheckService authCheckService;

    protected StudentParticipationRepository studentParticipationRepository;

    protected ParticipationService participationService;

    private final ExamService examService;

    private final UserService userService;

    private final CourseService courseService;

    public SubmissionService(SubmissionRepository submissionRepository, UserService userService, AuthorizationCheckService authCheckService, CourseService courseService,
            ResultRepository resultRepository, ExamService examService, StudentParticipationRepository studentParticipationRepository, ParticipationService participationService) {
        this.submissionRepository = submissionRepository;
        this.userService = userService;
        this.courseService = courseService;
        this.authCheckService = authCheckService;
        this.resultRepository = resultRepository;
        this.examService = examService;
        this.studentParticipationRepository = studentParticipationRepository;
        this.participationService = participationService;
    }

    /**
     * Check that the user is allowed to make the submission
     *
     * @param exercise      the exercise for which a submission should be saved
     * @param submission    the submission that should be saved
     * @param currentUser   the current user with groups and authorities
     * @param <T>           The type of the return type of the requesting route so that the
     *                      response can be returned there
     * @return an Optional with a typed ResponseEntity. If it is empty all checks passed
     */
    public <T> Optional<ResponseEntity<T>> checkSubmissionAllowance(Exercise exercise, Submission submission, User currentUser) {
        // Fetch course from database to make sure client didn't change groups
        final Course course = courseService.findOne(exercise.getCourseViaExerciseGroupOrCourseMember().getId());
        if (!authCheckService.isAtLeastStudentInCourse(course, currentUser)) {
            return Optional.of(forbidden());
        }

        // Fetch the submission with the corresponding participation if the id is set (on update) and check that the
        // user of the participation is the same as the user who executes this call (or student in the team).
        // This prevents injecting submissions to other users.
        if (submission.getId() != null) {
            Optional<Submission> existingSubmission = submissionRepository.findById(submission.getId());
            if (existingSubmission.isEmpty()) {
                return Optional.of(forbidden());
            }

            StudentParticipation participation = (StudentParticipation) existingSubmission.get().getParticipation();
            if (participation != null) {
                Optional<User> user = participation.getStudent();
                if (user.isPresent() && !user.get().equals(currentUser)) {
                    return Optional.of(forbidden());
                }

                Optional<Team> team = participation.getTeam();
                if (team.isPresent() && !authCheckService.isStudentInTeam(course, team.get().getShortName(), currentUser)) {
                    return Optional.of(forbidden());
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Check if the limit of simultaneously locked submissions (i.e. unfinished assessments) has been reached for the current user in the given course. Throws a
     * BadRequestAlertException if the limit has been reached.
     *
     * @param courseId the id of the course
     */
    public void checkSubmissionLockLimit(long courseId) {
        long numberOfLockedSubmissions = submissionRepository.countLockedSubmissionsByUserIdAndCourseId(userService.getUserWithGroupsAndAuthorities().getId(), courseId);
        if (numberOfLockedSubmissions >= MAX_NUMBER_OF_LOCKED_SUBMISSIONS_PER_TUTOR) {
            throw new BadRequestAlertException("The limit of locked submissions has been reached", "submission", "lockedSubmissionsLimitReached");
        }
    }

    /**
     * Get the number of simultaneously locked submissions (i.e. unfinished assessments) for the current user in the given course.
     *
     * @param courseId the id of the course
     * @return number of locked submissions for the current user in the given course
     */
    public long countSubmissionLocks(long courseId) {
        return submissionRepository.countLockedSubmissionsByUserIdAndCourseId(userService.getUserWithGroupsAndAuthorities().getId(), courseId);
    }

    /**
     * Get simultaneously locked submissions (i.e. unfinished assessments) for the current user in the given course.
     *
     * @param courseId the id of the course
     * @return number of locked submissions for the current user in the given course
     */
    public List<Submission> getLockedSubmissions(long courseId) {
        return submissionRepository.getLockedSubmissionsByUserIdAndCourseId(userService.getUserWithGroupsAndAuthorities().getId(), courseId);
    }

    /**
     * Given an exercise id and a tutor id, it returns all the submissions where the tutor has a result associated.
     *
     * @param exerciseId - the id of the exercise we are looking for
     * @param tutor - the tutor we are interested in
     * @param examMode - flag should be set to ignore the test run submissions
     * @return a list of Submissions
     */
    protected List<Submission> getAllSubmissionsAssessedByTutorForExercise(Long exerciseId, User tutor, boolean examMode) {
        List<Submission> submissions;
        if (examMode) {
            var participations = this.studentParticipationRepository.findAllByParticipationExerciseIdAndResultAssessorIgnoreTestRuns(exerciseId, tutor);
            submissions = participations.stream().map(StudentParticipation::findLatestSubmission).filter(Optional::isPresent).map(Optional::get).collect(toList());
        }
        else {
            submissions = this.submissionRepository.findAllByParticipationExerciseIdAndResultAssessor(exerciseId, tutor);
        }

        submissions.forEach(submission -> submission.getResult().setSubmission(null));
        return submissions;
    }

    /**
     * Given an exercise id, find a random submission for that exercise which still doesn't have any manual result.
     * No manual result means that no user has started an assessment for the corresponding submission yet.
     * For exam exercises we should also remove the test run participations as these should not be graded by the tutors.
     *
     * @param fileUploadExercise the exercise for which we want to retrieve a submission without manual result
     * @param examMode flag to determine if test runs should be removed. This should be set to true for exam exercises
     * @return a submission without any manual result or an empty Optional if no submission without manual result could be found
     */
    @Transactional(readOnly = true)
    public Optional<Submission> getRandomSubmissionEligibleForNewAssessment(Exercise fileUploadExercise, boolean examMode) {
        Random random = new Random();
        List<StudentParticipation> participations;
        if (examMode) {
            participations = participationService.findByExerciseIdWithLatestSubmissionWithoutManualResultsAndNoTestRun(fileUploadExercise.getId());
        }
        else {
            participations = participationService.findByExerciseIdWithLatestSubmissionWithoutManualResults(fileUploadExercise.getId());
        }

        List<Submission> submissionsWithoutResult = participations.stream().map(StudentParticipation::findLatestSubmission).filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toList());

        if (submissionsWithoutResult.isEmpty()) {
            return Optional.empty();
        }

        submissionsWithoutResult = selectOnlySubmissionsBeforeDueDateOrAll(submissionsWithoutResult, fileUploadExercise.getDueDate());

        var submissionWithoutResult = submissionsWithoutResult.get(random.nextInt(submissionsWithoutResult.size()));
        return Optional.of(submissionWithoutResult);
    }

    /**
     * Get the submission with the given id from the database. The submission is loaded together with its result and the assessor. Throws an EntityNotFoundException if no
     * submission could be found for the given id.
     *
     * @param submissionId the id of the submission that should be loaded from the database
     * @return the submission with the given id
     */
    public Submission findOneWithEagerResult(long submissionId) {
        return submissionRepository.findWithEagerResultById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission with id \"" + submissionId + "\" does not exist"));
    }

    /**
     * Count number of in-time submissions for course. Only submissions for Text, Modeling and File Upload exercises are included.
     * @param courseId the course id we are interested in
     * @return the number of submissions belonging to the course id, which have the submitted flag set to true and the submission date before the exercise due date, or no exercise
     *         due date at all
     */
    public long countInTimeSubmissionsForCourse(long courseId) {
        return submissionRepository.countByCourseIdSubmittedBeforeDueDate(courseId);
    }

    /**
     * Count number of late submissions for course. Only submissions for Text, Modeling and File Upload exercises are included.
     * @param courseId the course id we are interested in
     * @return the number of submissions belonging to the course id, which have the submitted flag set to true and the submission date after the exercise due date
     */
    public long countLateSubmissionsForCourse(long courseId) {
        return submissionRepository.countByCourseIdSubmittedAfterDueDate(courseId);
    }

    /**
     * Count number of submissions for exercise.
     * @param exerciseId the exercise id we are interested in
     * @param examMode should be set to ignore the test run submissions
     * @return the number of submissions belonging to the exercise id, which have the submitted flag set to true, separated into before and after the due date
     */
    public DueDateStat countSubmissionsForExercise(long exerciseId, boolean examMode) {
        if (examMode) {
            return new DueDateStat(submissionRepository.countByExerciseIdSubmittedBeforeDueDateIgnoreTestRuns(exerciseId), 0L);
        }
        return new DueDateStat(submissionRepository.countByExerciseIdSubmittedBeforeDueDate(exerciseId), submissionRepository.countByExerciseIdSubmittedAfterDueDate(exerciseId));
    }

    /**
     * Removes sensitive information (e.g. example solution of the exercise) from the submission based on the role of the current user. This should be called before sending a
     * submission to the client.
     * ***IMPORTANT***: Do not call this method from a transactional context as this would remove the sensitive information also from the entities in the
     * database without explicitly saving them.
     *
     * @param submission Submission to be modified.
     * @param user the currently logged in user which is used for hiding specific submission details based on instructor and teaching assistant rights
     */
    public void hideDetails(Submission submission, User user) {
        // do not send old submissions or old results to the client
        if (submission.getParticipation() != null) {
            submission.getParticipation().setSubmissions(null);
            submission.getParticipation().setResults(null);

            Exercise exercise = submission.getParticipation().getExercise();
            if (exercise != null) {
                // make sure that sensitive information is not sent to the client for students
                if (!authCheckService.isAtLeastTeachingAssistantForExercise(exercise, user)) {
                    exercise.filterSensitiveInformation();
                    submission.setResult(null);
                }
                // remove information about the student or team from the submission for tutors to ensure a double-blind assessment
                if (!authCheckService.isAtLeastInstructorForExercise(exercise, user)) {
                    StudentParticipation studentParticipation = (StudentParticipation) submission.getParticipation();

                    // the student himself is allowed to see the participant (i.e. himself or his team) of his participation
                    if (!authCheckService.isOwnerOfParticipation(studentParticipation, user)) {
                        studentParticipation.filterSensitiveInformation();
                    }
                }
            }
        }
    }

    /**
     * Creates a new Result object, assigns it to the given submission and stores the changes to the database.
     * @param submission the submission for which a new result should be created
     * @return the newly created result
     */
    public Result setNewResult(Submission submission) {
        Result result = new Result();
        result.setSubmission(submission);
        submission.setResult(result);
        result.setParticipation(submission.getParticipation());
        result = resultRepository.save(result);
        submissionRepository.save(submission);
        return result;
    }

    /**
     * Soft lock the submission to prevent other tutors from receiving and assessing it. We set the assessor and save the result to soft lock the assessment in the client, i.e. the client will not allow
     * tutors to assess a submission when an assessor is already assigned. If no result exists for this submission we create one first.
     *
     * @param submission the submission to lock
     */
    protected Result lockSubmission(Submission submission) {
        Result result = submission.getResult();
        if (result == null) {
            result = setNewResult(submission);
        }

        if (result.getAssessor() == null) {
            result.setAssessor(userService.getUser());
        }

        result.setAssessmentType(AssessmentType.MANUAL);
        result = resultRepository.save(result);
        log.debug("Assessment locked with result id: " + result.getId() + " for assessor: " + result.getAssessor().getName());
        return result;
    }

    /**
     * Filters the submissions on each participation so that only the latest submission for each participation remains
     * @param participations Participations for which to reduce the submissions
     * @param submittedOnly Flag whether to only consider submitted submissions when finding the latest one
     */
    public void reduceParticipationSubmissionsToLatest(List<StudentParticipation> participations, boolean submittedOnly) {
        participations.forEach(participation -> {
            participation.getExercise().setStudentParticipations(null);
            Optional<Submission> optionalSubmission = participation.findLatestSubmission();
            if (optionalSubmission.isPresent() && (!submittedOnly || optionalSubmission.get().isSubmitted())) {
                participation.setSubmissions(Set.of(optionalSubmission.get()));
                Optional.ofNullable(optionalSubmission.get().getResult()).ifPresent(result -> participation.setResults(Set.of(result)));
            }
            else {
                participation.setSubmissions(Set.of());
            }
        });
    }

    /**
     * Filters the submissions to contain only in-time submissions if there are any.
     * If not, the original list is returned.
     * @param submissions The submissions to filter
     * @param dueDate The due-date to filter by
     * @param <T> Placeholder for subclass of {@link Submission} e.g. {@link TextSubmission}
     * @return The filtered list of submissions
     */
    protected <T extends Submission> List<T> selectOnlySubmissionsBeforeDueDateOrAll(List<T> submissions, ZonedDateTime dueDate) {
        if (dueDate == null) {
            // this is an edge case, then basically all submissions are before due date
            return submissions;
        }

        boolean hasInTimeSubmissions = submissions.stream().anyMatch(submission -> submission.getSubmissionDate() != null && submission.getSubmissionDate().isBefore(dueDate));
        if (hasInTimeSubmissions) {
            return submissions.stream().filter(submission -> submission.getSubmissionDate() != null && submission.getSubmissionDate().isBefore(dueDate))
                    .collect(Collectors.toList());
        }
        else {
            return submissions;
        }
    }

    /**
     * Checks if the exercise due date has passed. For exam exercises it checks if the latest possible exam end date has passed.
     * @param exercise course exercise or exam exercise that is checked
     * @return boolean
     */
    public boolean checkIfExerciseDueDateIsReached(Exercise exercise) {
        final boolean isExamMode = exercise.hasExerciseGroup();
        // Tutors cannot start assessing submissions if the exercise due date hasn't been reached yet
        if (isExamMode) {
            ZonedDateTime latestIndividualExamEndDate = this.examService.getLatestIndiviudalExamEndDate(exercise.getExerciseGroup().getExam());
            if (latestIndividualExamEndDate != null && latestIndividualExamEndDate.isAfter(ZonedDateTime.now())) {
                return false;
            }
        }
        else {
            // special check for programming exercises as they use buildAndTestStudentSubmissionAfterDueDate instead of dueDate
            if (exercise instanceof ProgrammingExercise) {
                ProgrammingExercise programmingExercise = (ProgrammingExercise) exercise;
                if (programmingExercise.getBuildAndTestStudentSubmissionsAfterDueDate() != null
                        && programmingExercise.getBuildAndTestStudentSubmissionsAfterDueDate().isAfter(ZonedDateTime.now())) {
                    return false;
                }
            }

            if (exercise.getDueDate() != null && exercise.getDueDate().isAfter(ZonedDateTime.now())) {
                return false;
            }
        }
        return true;
    }
}
