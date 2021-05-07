package de.tum.in.www1.artemis.web.rest;

import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.security.Role;
import de.tum.in.www1.artemis.service.AuthorizationCheckService;
import de.tum.in.www1.artemis.service.GroupNotificationService;
import de.tum.in.www1.artemis.web.rest.errors.AccessForbiddenException;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;

/**
 * REST controller for managing StudentQuestion.
 */
@RestController
@RequestMapping("/api")
public class StudentQuestionResource {

    private final Logger log = LoggerFactory.getLogger(StudentQuestionResource.class);

    private static final String ENTITY_NAME = "studentQuestion";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final StudentQuestionRepository studentQuestionRepository;

    private final ExerciseRepository exerciseRepository;

    private final LectureRepository lectureRepository;

    private final CourseRepository courseRepository;

    private final AuthorizationCheckService authorizationCheckService;

    private final UserRepository userRepository;

    private final GroupNotificationService groupNotificationService;

    public StudentQuestionResource(StudentQuestionRepository studentQuestionRepository, GroupNotificationService groupNotificationService, LectureRepository lectureRepository,
            AuthorizationCheckService authorizationCheckService, UserRepository userRepository, ExerciseRepository exerciseRepository, CourseRepository courseRepository) {
        this.studentQuestionRepository = studentQuestionRepository;
        this.groupNotificationService = groupNotificationService;
        this.authorizationCheckService = authorizationCheckService;
        this.userRepository = userRepository;
        this.exerciseRepository = exerciseRepository;
        this.lectureRepository = lectureRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * POST /courses/{courseId}/student-questions : Create a new studentQuestion.
     *
     * @param courseId course the question belongs to
     * @param studentQuestion the studentQuestion to create
     * @return the ResponseEntity with status 201 (Created) and with body the new studentQuestion, or with status 400 (Bad Request) if the studentQuestion
     * already has an ID or the courseId in the body doesn't match the PathVariable
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("courses/{courseId}/student-questions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<StudentQuestion> createStudentQuestion(@PathVariable Long courseId, @RequestBody StudentQuestion studentQuestion) throws URISyntaxException {
        if (!studentQuestion.getCourse().getId().equals(courseId)) {
            return badRequest("courseId", "400", "PathVariable courseId doesn't match the courseId of the sent StudentQuestion in Body");
        }
        log.debug("REST request to save StudentQuestion : {}", studentQuestion);
        User user = this.userRepository.getUserWithGroupsAndAuthorities();
        if (studentQuestion.getId() != null) {
            throw new BadRequestAlertException("A new studentQuestion cannot already have an ID", ENTITY_NAME, "idexists");
        }
        final Course course = courseRepository.findByIdElseThrow(courseId);
        authorizationCheckService.checkHasAtLeastRoleInCourseElseThrow(Role.STUDENT, course, user);
        // set author to current user
        studentQuestion.setAuthor(user);
        StudentQuestion question = studentQuestionRepository.save(studentQuestion);
        if (question.getExercise() != null) {
            groupNotificationService.notifyTutorAndEditorAndInstructorGroupAboutNewQuestionForExercise(question);
        }
        if (question.getLecture() != null) {
            groupNotificationService.notifyTutorAndEditorAndInstructorGroupAboutNewQuestionForLecture(question);
        }
        return ResponseEntity.created(new URI("/api/courses/" + courseId + "/student-questions/" + question.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, question.getId().toString())).body(question);
    }

    /**
     * PUT /courses/{courseId}/student-questions : Updates an existing studentQuestion.
     *
     * @param courseId course the question belongs to
     * @param studentQuestion the studentQuestion to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated studentQuestion, or with status 400 (Bad Request) if the studentQuestion is not valid, or with
     *         status 500 (Internal Server Error) if the studentQuestion couldn't be updated
     */
    @PutMapping("courses/{courseId}/student-questions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<StudentQuestion> updateStudentQuestion(@PathVariable Long courseId, @RequestBody StudentQuestion studentQuestion) {
        User user = userRepository.getUserWithGroupsAndAuthorities();
        log.debug("REST request to update StudentQuestion : {}", studentQuestion);
        if (studentQuestion.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        courseRepository.findByIdElseThrow(courseId);
        StudentQuestion existingStudentQuestion = studentQuestionRepository.findByIdElseThrow(studentQuestion.getId());
        if (!existingStudentQuestion.getCourse().getId().equals(courseId)) {
            return badRequest("courseId", "400", "PathVariable courseId doesnt match courseId of the StudentQuestion that should be changed");
        }
        mayUpdateOrDeleteStudentQuestionElseThrow(existingStudentQuestion, user);
        existingStudentQuestion.setQuestionText(studentQuestion.getQuestionText());
        existingStudentQuestion.setVisibleForStudents(studentQuestion.isVisibleForStudents());
        StudentQuestion result = studentQuestionRepository.save(existingStudentQuestion);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, studentQuestion.getId().toString())).body(result);
    }

    /**
     * PUT /courses/{courseId}/student-questions/{questionId}/votes : Updates votes for a studentQuestion.
     *
     * @param courseId course the question belongs to
     * @param questionId the ID of the question to update
     * @param voteChange value by which votes are increased / decreased
     * @return the ResponseEntity with status 200 (OK) and with body the updated studentQuestion, or with status 400 (Bad Request) if the studentQuestion or the voteChanges are invalid, or with
     *         status 500 (Internal Server Error) if the studentQuestion couldn't be updated
     */
    @PutMapping("courses/{courseId}/student-questions/{questionId}/votes")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<StudentQuestion> updateStudentQuestionVotes(@PathVariable Long courseId, @PathVariable Long questionId, @RequestBody Integer voteChange) {
        if (voteChange < -2 || voteChange > 2) {
            return badRequest("voteChange", "400", "voteChange must be >= -2 and <= 2");
        }
        final User user = userRepository.getUserWithGroupsAndAuthorities();
        StudentQuestion studentQuestion = studentQuestionRepository.findByIdElseThrow(questionId);
        courseRepository.findByIdElseThrow(courseId);
        if (!studentQuestion.getCourse().getId().equals(courseId)) {
            return badRequest("courseId", "400", "PathVariable courseId doesnt match courseId of the StudentQuestion that should be changed");
        }
        mayUpdateStudentQuestionVotesElseThrow(studentQuestion, user);
        Integer newVotes = studentQuestion.getVotes() + voteChange;
        studentQuestion.setVotes(newVotes);
        StudentQuestion result = studentQuestionRepository.save(studentQuestion);
        return ResponseEntity.ok().body(result);
    }

    /**
     * GET /courses/{courseId}/exercises/{exerciseId}/student-questions : get all student questions for exercise.
     *
     * @param courseId course the question belongs to
     * @param exerciseId the exercise that the student questions belong to
     * @return the ResponseEntity with status 200 (OK) and with body all student questions for exercise or 400 (Bad Request) if exercises courseId doesnt match
     * the PathVariable courseId
     */
    @GetMapping("courses/{courseId}/exercises/{exerciseId}/student-questions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<StudentQuestion>> getAllQuestionsForExercise(@PathVariable Long courseId, @PathVariable Long exerciseId) {
        final User user = userRepository.getUserWithGroupsAndAuthorities();
        Exercise exercise = exerciseRepository.findByIdElseThrow(exerciseId);
        courseRepository.findByIdElseThrow(courseId);
        authorizationCheckService.checkHasAtLeastRoleForExerciseElseThrow(Role.STUDENT, exercise, user);
        if (!exercise.getCourseViaExerciseGroupOrCourseMember().getId().equals(courseId)) {
            return badRequest("courseId", "400", "PathVariable courseId doesnt match courseId of the exercise that should be returned");
        }
        List<StudentQuestion> studentQuestions = studentQuestionRepository.findStudentQuestionsForExercise(exerciseId);
        hideSensitiveInformation(studentQuestions);
        return new ResponseEntity<>(studentQuestions, null, HttpStatus.OK);
    }

    /**
     * GET /courses/{courseId}/lectures/{lectureId}/student-questions : get all student questions for lecture.
     *
     * @param courseId course the question belongs to
     * @param lectureId the lecture that the student questions belong to
     * @return the ResponseEntity with status 200 (OK) and with body all student questions for lecture or 400 (Bad Request) if the lectures courseId doesnt match
     * the PathVariable courseId
     */
    @GetMapping("courses/{courseId}/lectures/{lectureId}/student-questions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<StudentQuestion>> getAllQuestionsForLecture(@PathVariable Long courseId, @PathVariable Long lectureId) {
        Lecture lecture = lectureRepository.findByIdElseThrow(lectureId);
        courseRepository.findByIdElseThrow(courseId);
        final User user = userRepository.getUserWithGroupsAndAuthorities();
        if (lecture.getCourse().getId().equals(courseId)) {
            authorizationCheckService.checkHasAtLeastRoleInCourseElseThrow(Role.STUDENT, lecture.getCourse(), user);
            List<StudentQuestion> studentQuestions = studentQuestionRepository.findStudentQuestionsForLecture(lectureId);
            hideSensitiveInformation(studentQuestions);
            return new ResponseEntity<>(studentQuestions, null, HttpStatus.OK);
        }
        else {
            return badRequest("courseId", "400", "PathVariable courseId and the courseId of the Lecture dont match");
        }
    }

    /**
     *
     * GET /courses/{courseId}/student-questions : get all student questions for course
     * @param courseId the course that the student questions belong to
     * @return the ResponseEntity with status 200 (OK) and with body all student questions for course
     */
    @GetMapping("courses/{courseId}/student-questions")
    @PreAuthorize("hasRole('TA')")
    public ResponseEntity<List<StudentQuestion>> getAllQuestionsForCourse(@PathVariable Long courseId) {
        var course = courseRepository.findByIdElseThrow(courseId);
        authorizationCheckService.checkHasAtLeastRoleInCourseElseThrow(Role.TEACHING_ASSISTANT, course, null);
        List<StudentQuestion> studentQuestions = studentQuestionRepository.findStudentQuestionsForCourse(courseId);
        return new ResponseEntity<>(studentQuestions, null, HttpStatus.OK);
    }

    private void hideSensitiveInformation(List<StudentQuestion> studentQuestions) {
        for (StudentQuestion question : studentQuestions) {
            question.setExercise(null);
            question.setLecture(null);
            question.setAuthor(question.getAuthor().copyBasicUser());
            for (StudentQuestionAnswer answer : question.getAnswers()) {
                answer.setAuthor(answer.getAuthor().copyBasicUser());
            }
        }
    }

    /**
     * DELETE /courses/{courseId}/student-questions/:id : delete the "id" studentQuestion.
     *
     * @param courseId course the question belongs to
     * @param studentQuestionId the id of the studentQuestion to delete
     * @return the ResponseEntity with status 200 (OK) or 400 (Bad Request) if the data is inconsistent
     */
    @DeleteMapping("courses/{courseId}/student-questions/{studentQuestionId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteStudentQuestion(@PathVariable Long courseId, @PathVariable Long studentQuestionId) {
        User user = userRepository.getUserWithGroupsAndAuthorities();
        courseRepository.findByIdElseThrow(courseId);
        StudentQuestion studentQuestion = studentQuestionRepository.findByIdElseThrow(studentQuestionId);
        String entity = "";
        if (studentQuestion.getLecture() != null) {
            entity = "lecture with id: " + studentQuestion.getLecture().getId();
        }
        else if (studentQuestion.getExercise() != null) {
            entity = "exercise with id: " + studentQuestion.getExercise().getId();
        }
        if (studentQuestion.getCourse() == null) {
            return ResponseEntity.badRequest().build();
        }
        mayUpdateOrDeleteStudentQuestionElseThrow(studentQuestion, user);
        log.info("StudentQuestion deleted by " + user.getLogin() + ". Question: " + studentQuestion.getQuestionText() + " for " + entity);
        studentQuestionRepository.deleteById(studentQuestionId);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, studentQuestionId.toString())).build();

    }

    /**
     * Check if user can update or delete StudentQuestion, if not throws an AccessForbiddenException
     *
     * @param studentQuestion studentQuestion for which to check
     * @param user user for which to check
     */
    private void mayUpdateOrDeleteStudentQuestionElseThrow(StudentQuestion studentQuestion, User user) {
        if (!user.getId().equals(studentQuestion.getAuthor().getId())) {
            authorizationCheckService.checkHasAtLeastRoleInCourseElseThrow(Role.TEACHING_ASSISTANT, studentQuestion.getCourse(), user);
        }
    }

    /**
     * Check if user can update the StudentQuestions votes, if not throws an AccessForbiddenException
     *
     * @param studentQuestion studentQuestionAnswer for which to check
     * @param user user for which to check
     */
    private void mayUpdateStudentQuestionVotesElseThrow(StudentQuestion studentQuestion, User user) {
        Course course = studentQuestion.getCourse();
        Exercise exercise = studentQuestion.getExercise();
        if (course != null) {
            authorizationCheckService.checkHasAtLeastRoleInCourseElseThrow(Role.STUDENT, course, user);
        }
        else if (exercise != null) {
            authorizationCheckService.checkHasAtLeastRoleForExerciseElseThrow(Role.STUDENT, exercise, user);
        }
        else {
            throw new AccessForbiddenException("StudentQuestion", studentQuestion.getId());
        }
    }
}
