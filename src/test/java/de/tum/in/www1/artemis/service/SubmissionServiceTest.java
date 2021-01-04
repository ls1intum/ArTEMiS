package de.tum.in.www1.artemis.service;

import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.forbidden;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;

import de.tum.in.www1.artemis.AbstractSpringIntegrationBambooBitbucketJiraTest;
import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.AssessmentType;
import de.tum.in.www1.artemis.domain.exam.Exam;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.repository.StudentParticipationRepository;
import de.tum.in.www1.artemis.repository.UserRepository;

public class SubmissionServiceTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    @Autowired
    CourseService courseService;

    @Autowired
    ExamService examService;

    @Autowired
    ResultRepository resultRepository;

    @Autowired
    StudentParticipationRepository studentParticipationRepository;

    @Autowired
    SubmissionService submissionService;

    @Autowired
    UserRepository userRepository;

    private User student1;

    private User student2;

    private User tutor1;

    private User tutor2;

    private User instructor;

    private Course course;

    private TextExercise examTextExercise;

    private ModelingExercise examModelingExercise;

    private FileUploadExercise examFileUploadExercise;

    private Exam exam;

    private Submission submission1 = new TextSubmission();

    private Submission submission2 = new TextSubmission();

    private Optional<Submission> unassessedSubmissionCorrectionRound0;

    private Optional<Submission> unassessedSubmissionCorrectionRound1;

    private List<Submission> submissionListTutor1CorrectionRound0;

    private List<Submission> submissionListTutor2CorrectionRound0;

    private List<Submission> submissionListTutor1CorrectionRound1;

    private List<Submission> submissionListTutor2CorrectionRound1;

    @BeforeEach
    void init() {
        List<User> users = database.addUsers(2, 2, 1);
        student1 = users.get(0);
        student2 = users.get(1);
        tutor1 = users.get(2);
        tutor2 = users.get(3);
        instructor = users.get(4);

        course = database.createCourse();
        exam = database.addExam(course);

        exam.setNumberOfCorrectionRoundsInExam(2);
        exam = examService.save(exam);

        exam = database.addExerciseGroupsAndExercisesToExam(exam, false);

        examTextExercise = (TextExercise) exam.getExerciseGroups().get(0).getExercises().stream().filter(exercise -> exercise.getClass().equals(TextExercise.class)).findAny()
                .orElse(null);
        examFileUploadExercise = (FileUploadExercise) exam.getExerciseGroups().get(2).getExercises().stream()
                .filter(exercise -> exercise.getClass().equals(FileUploadExercise.class)).findAny().orElse(null);
        examModelingExercise = (ModelingExercise) exam.getExerciseGroups().get(3).getExercises().stream().filter(exercise -> exercise.getClass().equals(ModelingExercise.class))
                .findAny().orElse(null);

    }

    @AfterEach
    public void tearDown() {
        database.resetDatabase();
        unassessedSubmissionCorrectionRound0 = null;
        unassessedSubmissionCorrectionRound1 = null;
        submissionListTutor1CorrectionRound0 = null;
        submissionListTutor2CorrectionRound0 = null;
        submissionListTutor1CorrectionRound1 = null;
        submissionListTutor2CorrectionRound1 = null;
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void testCheckSubmissionAllowance_groupCheck() {
        student1.setGroups(Collections.singleton("another-group"));
        userRepository.save(student1);
        Optional<ResponseEntity<Submission>> result = submissionService.checkSubmissionAllowance(examTextExercise, null, student1);
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo(forbidden());
    }

    private void queryTestingBasics(Exercise exercise) {
        Exam exam = exercise.getExerciseGroup().getExam();

        exam.setNumberOfCorrectionRoundsInExam(2);

        examService.save(exam);

        submission1.submitted(true);
        submission2.submitted(true);
        database.addSubmission(exercise, submission1, "student1");
        database.addSubmission(exercise, submission2, "student2");
    }

    private void getQueryResults(Exercise exercise) {
        unassessedSubmissionCorrectionRound0 = submissionService.getRandomSubmissionEligibleForNewAssessment(exercise, true, 0L);
        unassessedSubmissionCorrectionRound1 = submissionService.getRandomSubmissionEligibleForNewAssessment(exercise, true, 1L);

        submissionListTutor1CorrectionRound0 = submissionService.getAllSubmissionsAssessedByTutorForCorrectionRoundAndExercise(exercise.getId(), tutor1, true, 0L);
        submissionListTutor2CorrectionRound0 = submissionService.getAllSubmissionsAssessedByTutorForCorrectionRoundAndExercise(exercise.getId(), tutor2, true, 0L);

        submissionListTutor1CorrectionRound1 = submissionService.getAllSubmissionsAssessedByTutorForCorrectionRoundAndExercise(exercise.getId(), tutor1, true, 1L);
        submissionListTutor2CorrectionRound1 = submissionService.getAllSubmissionsAssessedByTutorForCorrectionRoundAndExercise(exercise.getId(), tutor2, true, 1L);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TUTOR")
    public void testTextExerciseGetRandomSubmissionEligibleForNewAssessment_NoAssessments() {

        // setup
        queryTestingBasics(this.examTextExercise);

        getQueryResults(this.examTextExercise);

        assertThat(examTextExercise.getExerciseGroup().getExam().getNumberOfCorrectionRoundsInExam()).isEqualTo(2L);

        assertThat(unassessedSubmissionCorrectionRound0.isPresent()).isTrue();
        assertThat(unassessedSubmissionCorrectionRound0.get()).isIn(submission1, submission2);
        assertThat(unassessedSubmissionCorrectionRound1.isPresent()).isFalse();

        assertThat(submissionListTutor1CorrectionRound0.size()).isEqualTo(0);
        assertThat(submissionListTutor2CorrectionRound0.size()).isEqualTo(0);
        assertThat(submissionListTutor1CorrectionRound1.size()).isEqualTo(0);
        assertThat(submissionListTutor2CorrectionRound1.size()).isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TUTOR")
    public void testTextExerciseGetRandomSubmissionEligibleForNewAssessment_OneAssessmentsWithoutLock() {

        // setup
        queryTestingBasics(examTextExercise);

        database.addResultToSubmission(submission1, AssessmentType.MANUAL, tutor1, 10L, true);

        // checks
        getQueryResults(examTextExercise);

        assertThat(examTextExercise.getExerciseGroup().getExam().getNumberOfCorrectionRoundsInExam()).isEqualTo(2L);

        assertThat(unassessedSubmissionCorrectionRound0.isPresent()).isTrue();
        assertThat(unassessedSubmissionCorrectionRound0.get()).isEqualTo(submission2);
        assertThat(unassessedSubmissionCorrectionRound1.isPresent()).isTrue();
        assertThat(unassessedSubmissionCorrectionRound1.get()).isEqualTo(submission1);

        assertThat(submissionListTutor1CorrectionRound0.size()).isEqualTo(1);
        assertThat(submissionListTutor1CorrectionRound0.get(0)).isEqualTo(submission1);
        assertThat(submissionListTutor2CorrectionRound0.size()).isEqualTo(0);
        assertThat(submissionListTutor1CorrectionRound1.size()).isEqualTo(0);
        assertThat(submissionListTutor2CorrectionRound1.size()).isEqualTo(0);

    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TUTOR")
    public void testTextExerciseGetRandomSubmissionEligibleForNewAssessment_OneAssessmentsWithLock() {

        // setup
        queryTestingBasics(this.examTextExercise);

        Result resultWithLock;

        resultWithLock = submissionService.saveNewEmptyResult(submission1);
        resultWithLock.setAssessor(tutor1);
        resultWithLock.setAssessmentType(AssessmentType.MANUAL);
        resultRepository.save(resultWithLock);

        // checks
        getQueryResults(examTextExercise);

        assertThat(examTextExercise.getExerciseGroup().getExam().getNumberOfCorrectionRoundsInExam()).isEqualTo(2L);

        assertThat(unassessedSubmissionCorrectionRound0.isPresent()).isTrue();
        assertThat(unassessedSubmissionCorrectionRound0.get()).isEqualTo(submission2);
        assertThat(unassessedSubmissionCorrectionRound1.isPresent()).isFalse();

        assertThat(submissionListTutor1CorrectionRound0.size()).isEqualTo(1);
        assertThat(submissionListTutor1CorrectionRound0.get(0)).isEqualTo(submission1);
        assertThat(submissionListTutor2CorrectionRound0.size()).isEqualTo(0);
        assertThat(submissionListTutor1CorrectionRound1.size()).isEqualTo(0);
        assertThat(submissionListTutor2CorrectionRound1.size()).isEqualTo(0);

    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TUTOR")
    public void testTextExerciseGetRandomSubmissionEligibleForNewAssessment_OneAssessmentsInSecondCorrectionRoundWithoutLock() {

        // setup
        queryTestingBasics(this.examTextExercise);

        database.addResultToSubmission(submission1, AssessmentType.MANUAL, tutor1, 10L, true);
        database.addResultToSubmission(submission1, AssessmentType.MANUAL, tutor2, 20L, true);

        // checks
        getQueryResults(examTextExercise);

        assertThat(submission1.getResults().size()).isEqualTo(2L);

        assertThat(examTextExercise.getExerciseGroup().getExam().getNumberOfCorrectionRoundsInExam()).isEqualTo(2L);

        assertThat(unassessedSubmissionCorrectionRound0.isPresent()).isTrue();
        assertThat(unassessedSubmissionCorrectionRound0.get()).isEqualTo(submission2);
        assertThat(unassessedSubmissionCorrectionRound1.isPresent()).isFalse();

        assertThat(submissionListTutor1CorrectionRound0.size()).isEqualTo(1);
        assertThat(submissionListTutor1CorrectionRound0.get(0)).isEqualTo(submission1);
        assertThat(submissionListTutor2CorrectionRound0.size()).isEqualTo(0);
        assertThat(submissionListTutor1CorrectionRound1.size()).isEqualTo(0);
        assertThat(submissionListTutor2CorrectionRound1.size()).isEqualTo(1);
        assertThat(submissionListTutor2CorrectionRound1.get(0)).isEqualTo(submission1);

    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TUTOR")
    public void testTextExerciseGetRandomSubmissionEligibleForNewAssessment_OneAssessmentsInSecondCorrectionRoundWithLock() {

        // setup
        queryTestingBasics(this.examTextExercise);

        database.addResultToSubmission(submission1, AssessmentType.MANUAL, tutor1, 10L, true);

        Result resultForSecondCorrectionWithLock;

        resultForSecondCorrectionWithLock = submissionService.saveNewEmptyResult(submission1);
        resultForSecondCorrectionWithLock.setAssessor(tutor2);
        resultForSecondCorrectionWithLock.setAssessmentType(AssessmentType.MANUAL);
        resultRepository.save(resultForSecondCorrectionWithLock);

        // checks
        getQueryResults(examTextExercise);

        assertThat(submission1.getResults().size()).isEqualTo(2L);

        assertThat(examTextExercise.getExerciseGroup().getExam().getNumberOfCorrectionRoundsInExam()).isEqualTo(2L);

        assertThat(unassessedSubmissionCorrectionRound0.isPresent()).isTrue();
        assertThat(unassessedSubmissionCorrectionRound0.get()).isEqualTo(submission2);
        assertThat(unassessedSubmissionCorrectionRound1.isPresent()).isFalse();

        assertThat(submissionListTutor1CorrectionRound0.size()).isEqualTo(1);
        assertThat(submissionListTutor1CorrectionRound0.get(0)).isEqualTo(submission1);
        assertThat(submissionListTutor2CorrectionRound0.size()).isEqualTo(0);
        assertThat(submissionListTutor1CorrectionRound1.size()).isEqualTo(0);
        assertThat(submissionListTutor2CorrectionRound1.size()).isEqualTo(1);
        assertThat(submissionListTutor2CorrectionRound1.get(0)).isEqualTo(submission1);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TUTOR")
    public void testModelingExerciseGetRandomSubmissionEligibleForNewAssessment_NoAssessments() {

        // setup
        queryTestingBasics(this.examModelingExercise);

        // checks
        getQueryResults(examModelingExercise);

        assertThat(examModelingExercise.getExerciseGroup().getExam().getNumberOfCorrectionRoundsInExam()).isEqualTo(2L);

        assertThat(unassessedSubmissionCorrectionRound0.isPresent()).isTrue();
        assertThat(unassessedSubmissionCorrectionRound0.get()).isIn(submission1, submission2);
        assertThat(unassessedSubmissionCorrectionRound1.isPresent()).isFalse();

        assertThat(submissionListTutor1CorrectionRound0.size()).isEqualTo(0);
        assertThat(submissionListTutor2CorrectionRound0.size()).isEqualTo(0);
        assertThat(submissionListTutor1CorrectionRound1.size()).isEqualTo(0);
        assertThat(submissionListTutor2CorrectionRound1.size()).isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TUTOR")
    public void testModelingExerciseGetRandomSubmissionEligibleForNewAssessment_OneAssessmentsWithoutLock() {

        // setup
        queryTestingBasics(this.examModelingExercise);

        database.addResultToSubmission(submission1, AssessmentType.MANUAL, tutor1, 10L, true);

        // checks
        getQueryResults(examModelingExercise);

        assertThat(examModelingExercise.getExerciseGroup().getExam().getNumberOfCorrectionRoundsInExam()).isEqualTo(2L);

        assertThat(unassessedSubmissionCorrectionRound0.isPresent()).isTrue();
        assertThat(unassessedSubmissionCorrectionRound0.get()).isEqualTo(submission2);
        assertThat(unassessedSubmissionCorrectionRound1.isPresent()).isTrue();
        assertThat(unassessedSubmissionCorrectionRound1.get()).isEqualTo(submission1);

        assertThat(submissionListTutor1CorrectionRound0.size()).isEqualTo(1);
        assertThat(submissionListTutor1CorrectionRound0.get(0)).isEqualTo(submission1);
        assertThat(submissionListTutor2CorrectionRound0.size()).isEqualTo(0);
        assertThat(submissionListTutor1CorrectionRound1.size()).isEqualTo(0);
        assertThat(submissionListTutor2CorrectionRound1.size()).isEqualTo(0);

    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TUTOR")
    public void testModelingExerciseGetRandomSubmissionEligibleForNewAssessment_OneAssessmentsWithLock() {

        // setup
        queryTestingBasics(this.examModelingExercise);

        Result resultWithLock;

        resultWithLock = submissionService.saveNewEmptyResult(submission1);
        resultWithLock.setAssessor(tutor1);
        resultWithLock.setAssessmentType(AssessmentType.MANUAL);
        resultRepository.save(resultWithLock);

        // checks
        getQueryResults(examModelingExercise);

        assertThat(examModelingExercise.getExerciseGroup().getExam().getNumberOfCorrectionRoundsInExam()).isEqualTo(2L);

        assertThat(unassessedSubmissionCorrectionRound0.isPresent()).isTrue();
        assertThat(unassessedSubmissionCorrectionRound0.get()).isEqualTo(submission2);
        assertThat(unassessedSubmissionCorrectionRound1.isPresent()).isFalse();

        assertThat(submissionListTutor1CorrectionRound0.size()).isEqualTo(1);
        assertThat(submissionListTutor1CorrectionRound0.get(0)).isEqualTo(submission1);
        assertThat(submissionListTutor2CorrectionRound0.size()).isEqualTo(0);
        assertThat(submissionListTutor1CorrectionRound1.size()).isEqualTo(0);
        assertThat(submissionListTutor2CorrectionRound1.size()).isEqualTo(0);

    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TUTOR")
    public void testModelingExerciseGetRandomSubmissionEligibleForNewAssessment_OneAssessmentsInSecondCorrectionRoundWithoutLock() {
        // setup
        queryTestingBasics(this.examModelingExercise);

        database.addResultToSubmission(submission1, AssessmentType.MANUAL, tutor1, 10L, true);
        database.addResultToSubmission(submission1, AssessmentType.MANUAL, tutor2, 20L, true);

        // checks
        getQueryResults(examModelingExercise);

        assertThat(submission1.getResults().size()).isEqualTo(2L);

        assertThat(examModelingExercise.getExerciseGroup().getExam().getNumberOfCorrectionRoundsInExam()).isEqualTo(2L);

        assertThat(unassessedSubmissionCorrectionRound0.isPresent()).isTrue();
        assertThat(unassessedSubmissionCorrectionRound0.get()).isEqualTo(submission2);
        assertThat(unassessedSubmissionCorrectionRound1.isPresent()).isFalse();

        assertThat(submissionListTutor1CorrectionRound0.size()).isEqualTo(1);
        assertThat(submissionListTutor1CorrectionRound0.get(0)).isEqualTo(submission1);
        assertThat(submissionListTutor2CorrectionRound0.size()).isEqualTo(0);
        assertThat(submissionListTutor1CorrectionRound1.size()).isEqualTo(0);
        assertThat(submissionListTutor2CorrectionRound1.size()).isEqualTo(1);
        assertThat(submissionListTutor2CorrectionRound1.get(0)).isEqualTo(submission1);

    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TUTOR")
    public void testModelingExerciseGetRandomSubmissionEligibleForNewAssessment_OneAssessmentsInSecondCorrectionRoundWithLock() {

        // setup
        queryTestingBasics(this.examModelingExercise);

        database.addResultToSubmission(submission1, AssessmentType.MANUAL, tutor1, 10L, true);

        Result resultForSecondCorrectionWithLock = submissionService.saveNewEmptyResult(submission1);
        resultForSecondCorrectionWithLock.setAssessor(tutor2);
        resultForSecondCorrectionWithLock.setAssessmentType(AssessmentType.MANUAL);
        resultRepository.save(resultForSecondCorrectionWithLock);

        // checks
        getQueryResults(examModelingExercise);

        assertThat(submission1.getResults().size()).isEqualTo(2L);

        assertThat(examModelingExercise.getExerciseGroup().getExam().getNumberOfCorrectionRoundsInExam()).isEqualTo(2L);

        assertThat(unassessedSubmissionCorrectionRound0.isPresent()).isTrue();
        assertThat(unassessedSubmissionCorrectionRound0.get()).isEqualTo(submission2);
        assertThat(unassessedSubmissionCorrectionRound1.isPresent()).isFalse();

        assertThat(submissionListTutor1CorrectionRound0.size()).isEqualTo(1);
        assertThat(submissionListTutor1CorrectionRound0.get(0)).isEqualTo(submission1);
        assertThat(submissionListTutor2CorrectionRound0.size()).isEqualTo(0);
        assertThat(submissionListTutor1CorrectionRound1.size()).isEqualTo(0);
        assertThat(submissionListTutor2CorrectionRound1.size()).isEqualTo(1);
        assertThat(submissionListTutor2CorrectionRound1.get(0)).isEqualTo(submission1);
    }

}
