package de.tum.in.www1.artemis.programmingexercise;

import static de.tum.in.www1.artemis.config.Constants.NEW_SUBMISSION_TOPIC;
import static de.tum.in.www1.artemis.util.TestConstants.COMMIT_HASH_OBJECT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.util.LinkedMultiValueMap;

import de.tum.in.www1.artemis.AbstractSpringIntegrationBambooBitbucketJiraTest;
import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.AssessmentType;
import de.tum.in.www1.artemis.domain.enumeration.FeedbackType;
import de.tum.in.www1.artemis.domain.enumeration.SubmissionType;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.domain.modeling.ModelingSubmission;
import de.tum.in.www1.artemis.domain.participation.*;
import de.tum.in.www1.artemis.repository.ProgrammingExerciseRepository;
import de.tum.in.www1.artemis.repository.ProgrammingExerciseStudentParticipationRepository;
import de.tum.in.www1.artemis.repository.ProgrammingSubmissionRepository;
import de.tum.in.www1.artemis.service.connectors.bamboo.dto.BambooBuildPlanDTO;
import de.tum.in.www1.artemis.util.FileUtils;
import de.tum.in.www1.artemis.util.ModelFactory;
import de.tum.in.www1.artemis.util.TestConstants;

public class ProgrammingSubmissionIntegrationTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    @Autowired
    private ProgrammingExerciseRepository programmingExerciseRepository;

    @Autowired
    private ProgrammingSubmissionRepository submissionRepository;

    @Autowired
    private ProgrammingExerciseStudentParticipationRepository programmingExerciseStudentParticipationRepository;

    private ProgrammingExercise exercise;

    private ProgrammingExerciseStudentParticipation programmingExerciseStudentParticipation;

    @BeforeEach
    public void init() {
        database.addUsers(10, 2, 1, 2);
        database.addCourseWithOneProgrammingExerciseAndTestCases();

        exercise = programmingExerciseRepository.findAllWithEagerParticipationsAndLegalSubmissions().get(0);
        database.addSolutionParticipationForProgrammingExercise(exercise);
        database.addTemplateParticipationForProgrammingExercise(exercise);
        database.addProgrammingParticipationWithResultForExercise(exercise, "student1");
        exercise.setTestCasesChanged(true);
        programmingExerciseRepository.save(exercise);

        programmingExerciseStudentParticipation = database.addStudentParticipationForProgrammingExercise(exercise, "student2");

        var newObjectId = new ObjectId(4, 5, 2, 5, 3);
        doReturn(newObjectId).when(gitService).getLastCommitHash(null);
        doReturn(newObjectId).when(gitService).getLastCommitHash(exercise.getTemplateParticipation().getVcsRepositoryUrl());

        var dummyHash = "9b3a9bd71a0d80e5bbc42204c319ed3d1d4f0d6d";
        doReturn(ObjectId.fromString(dummyHash)).when(gitService).getLastCommitHash(programmingExerciseStudentParticipation.getVcsRepositoryUrl());
    }

    @AfterEach
    public void tearDown() {
        database.resetDatabase();
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    void triggerBuildStudent() throws Exception {
        bambooRequestMockProvider.enableMockingOfRequests();
        doReturn(COMMIT_HASH_OBJECT_ID).when(gitService).getLastCommitHash(any());

        String login = "student1";
        StudentParticipation participation = database.addStudentParticipationForProgrammingExercise(exercise, login);
        bambooRequestMockProvider.mockTriggerBuild((ProgrammingExerciseParticipation) participation);

        String url = "/api/programming-submissions/" + participation.getId() + "/trigger-build";
        request.postWithoutLocation(url, null, HttpStatus.OK, new HttpHeaders());

        List<ProgrammingSubmission> submissions = submissionRepository.findAll();
        assertThat(submissions).hasSize(1);

        ProgrammingSubmission submission = submissions.get(0);
        var optionalSubmission = submissionRepository.findWithEagerResultsById(submission.getId());
        assertThat(optionalSubmission).isPresent();
        assertThat(optionalSubmission.get().getLatestResult()).isNull();
        assertThat(submission.isSubmitted()).isTrue();
        assertThat(submission.getType()).isEqualTo(SubmissionType.MANUAL);
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    void triggerBuildStudentSubmissionNotFound() throws Exception {
        String login = "student1";
        Course course = database.addCourseWithDifferentModelingExercises();
        ModelingExercise classExercise = database.findModelingExerciseWithTitle(course.getExercises(), "ClassDiagram");
        ModelingSubmission modelingSubmission = ModelFactory.generateModelingSubmission(FileUtils.loadFileFromResources("test-data/model-submission/empty-class-diagram.json"),
                true);
        modelingSubmission = database.addModelingSubmission(classExercise, modelingSubmission, login);

        String url = "/api/programming-submissions/" + modelingSubmission.getParticipation().getId() + "/trigger-build";
        request.postWithoutLocation(url, null, HttpStatus.NOT_FOUND, new HttpHeaders());
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    void triggerBuildInstructor() throws Exception {
        bambooRequestMockProvider.enableMockingOfRequests();
        doReturn(COMMIT_HASH_OBJECT_ID).when(gitService).getLastCommitHash(any());
        String login = "student1";
        StudentParticipation participation = database.addStudentParticipationForProgrammingExercise(exercise, login);
        bambooRequestMockProvider.mockTriggerBuild((ProgrammingExerciseParticipation) participation);
        bambooRequestMockProvider.mockTriggerBuild((ProgrammingExerciseParticipation) participation);
        request.postWithoutLocation("/api/programming-submissions/" + participation.getId() + "/trigger-build?submissionType=INSTRUCTOR", null, HttpStatus.OK, new HttpHeaders());

        List<ProgrammingSubmission> submissions = submissionRepository.findAll();
        assertThat(submissions).hasSize(1);

        ProgrammingSubmission submission = submissions.get(0);
        var optionalSubmission = submissionRepository.findWithEagerResultsById(submission.getId());
        assertThat(optionalSubmission).isPresent();
        assertThat(optionalSubmission.get().getLatestResult()).isNull();
        assertThat(submission.isSubmitted()).isTrue();
        assertThat(submission.getType()).isEqualTo(SubmissionType.INSTRUCTOR);

        // Trigger the call again and make sure that the submission shouldn't be recreated
        request.postWithoutLocation("/api/programming-submissions/" + participation.getId() + "/trigger-build?submissionType=INSTRUCTOR", null, HttpStatus.OK, new HttpHeaders());
        var updatedSubmissions = submissionRepository.findAll();
        assertThat(updatedSubmissions).hasSize(1);
        assertThat(updatedSubmissions.get(0).getId()).isEqualTo(submission.getId());
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    void triggerBuildInstructorTutorForbidden() throws Exception {
        String login = "student1";
        StudentParticipation participation = database.addStudentParticipationForProgrammingExercise(exercise, login);

        String url = "/api/programming-submissions/" + participation.getId() + "/trigger-build?submissionType=INSTRUCTOR";
        request.postWithoutLocation(url, null, HttpStatus.FORBIDDEN, new HttpHeaders());
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    void triggerBuildInstructorStudentForbidden() throws Exception {
        String login = "student1";
        StudentParticipation participation = database.addStudentParticipationForProgrammingExercise(exercise, login);

        String url = "/api/programming-submissions/" + participation.getId() + "/trigger-build?submissionType=INSTRUCTOR";
        request.postWithoutLocation(url, null, HttpStatus.FORBIDDEN, new HttpHeaders());
    }

    @Test
    @Timeout(5)
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    void triggerBuildForExerciseAsInstructor() throws Exception {
        bambooRequestMockProvider.enableMockingOfRequests();
        doReturn(COMMIT_HASH_OBJECT_ID).when(gitService).getLastCommitHash(any());

        String login1 = "student1";
        String login2 = "student2";
        String login3 = "student3";
        final var firstParticipation = database.addStudentParticipationForProgrammingExercise(exercise, login1);
        final var secondParticipation = database.addStudentParticipationForProgrammingExercise(exercise, login2);
        final var thirdParticipation = database.addStudentParticipationForProgrammingExercise(exercise, login3);

        // Set test cases changed to true; after the build run it should be false;
        exercise.setTestCasesChanged(true);
        programmingExerciseRepository.save(exercise);
        bambooRequestMockProvider.mockTriggerBuild(firstParticipation);
        bambooRequestMockProvider.mockTriggerBuild(secondParticipation);
        bambooRequestMockProvider.mockTriggerBuild(thirdParticipation);

        // Each trigger build is mocked twice per participation so that we test
        // that no new submission is created on re-trigger
        bambooRequestMockProvider.mockTriggerBuild(firstParticipation);
        bambooRequestMockProvider.mockTriggerBuild(secondParticipation);
        bambooRequestMockProvider.mockTriggerBuild(thirdParticipation);

        // Perform a call to trigger-instructor-build-all twice. We want to check that the submissions
        // aren't being re-created.
        String url = "/api/programming-exercises/" + exercise.getId() + "/trigger-instructor-build-all";
        request.postWithoutLocation(url, null, HttpStatus.OK, new HttpHeaders());
        request.postWithoutLocation(url, null, HttpStatus.OK, new HttpHeaders());

        await().until(() -> submissionRepository.count() >= 3);

        List<ProgrammingSubmission> submissions = submissionRepository.findAll();

        // Make sure submissions aren't re-created.
        assertThat(submissions.size()).isEqualTo(3);

        List<ProgrammingExerciseParticipation> participations = new ArrayList<>();
        for (ProgrammingSubmission submission : submissions) {
            var optionalSubmission = submissionRepository.findWithEagerResultsById(submission.getId());
            assertThat(optionalSubmission).isPresent();
            assertThat(optionalSubmission.get().getLatestResult()).isNull();
            assertThat(submission.isSubmitted()).isTrue();
            assertThat(submission.getType()).isEqualTo(SubmissionType.INSTRUCTOR);
            assertThat(submission.getParticipation()).isNotNull();

            // There should be no participation assigned to two submissions.
            assertThat(participations.stream().noneMatch(p -> p.equals(submission.getParticipation()))).isTrue();
            participations.add((ProgrammingExerciseParticipation) submission.getParticipation());
        }

        var optionalUpdatedProgrammingExercise = programmingExerciseRepository.findWithTemplateAndSolutionParticipationTeamAssignmentConfigCategoriesById(exercise.getId());
        assertThat(optionalUpdatedProgrammingExercise).isPresent();
        ProgrammingExercise updatedProgrammingExercise = optionalUpdatedProgrammingExercise.get();
        assertThat(updatedProgrammingExercise.getTestCasesChanged()).isFalse();
        verify(groupNotificationService, times(1)).notifyEditorAndInstructorGroupAboutExerciseUpdate(updatedProgrammingExercise,
                Constants.TEST_CASES_CHANGED_RUN_COMPLETED_NOTIFICATION);
        verify(websocketMessagingService, times(1)).sendMessage("/topic/programming-exercises/" + exercise.getId() + "/test-cases-changed", false);
    }

    @Test
    @WithMockUser(username = "editor1", roles = "EDITOR")
    void triggerBuildForExerciseEditorForbidden() throws Exception {
        String url = "/api/programming-exercises/" + 1L + "/trigger-instructor-build-all";
        request.postWithoutLocation(url, null, HttpStatus.FORBIDDEN, new HttpHeaders());
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    void triggerBuildForExerciseTutorForbidden() throws Exception {
        String url = "/api/programming-exercises/" + 1L + "/trigger-instructor-build-all";
        request.postWithoutLocation(url, null, HttpStatus.FORBIDDEN, new HttpHeaders());
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    void triggerBuildForExerciseStudentForbidden() throws Exception {
        String url = "/api/programming-exercises/" + 1L + "/trigger-instructor-build-all";
        request.postWithoutLocation(url, null, HttpStatus.FORBIDDEN, new HttpHeaders());
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    void triggerBuildForParticipationsInstructor() throws Exception {
        bambooRequestMockProvider.enableMockingOfRequests();
        String login1 = "student1";
        String login2 = "student2";
        String login3 = "student3";
        ProgrammingExerciseStudentParticipation participation1 = database.addStudentParticipationForProgrammingExercise(exercise, login1);
        ProgrammingExerciseStudentParticipation participation2 = database.addStudentParticipationForProgrammingExercise(exercise, login2);
        ProgrammingExerciseStudentParticipation participation3 = database.addStudentParticipationForProgrammingExercise(exercise, login3);

        // We only trigger two participations here: 1 and 3.
        bambooRequestMockProvider.mockTriggerBuild(participation1);
        bambooRequestMockProvider.mockTriggerBuild(participation3);

        // Mock again because we call the trigger request two times
        bambooRequestMockProvider.mockTriggerBuild(participation1);
        bambooRequestMockProvider.mockTriggerBuild(participation3);

        List<Long> participationsToTrigger = new ArrayList<>(Arrays.asList(participation1.getId(), participation3.getId()));

        doReturn(COMMIT_HASH_OBJECT_ID).when(gitService).getLastCommitHash(any());

        // Perform a call to trigger-instructor-build-all twice. We want to check that the submissions
        // aren't being re-created.
        String url = "/api/programming-exercises/" + exercise.getId() + "/trigger-instructor-build";
        request.postWithoutLocation(url, participationsToTrigger, HttpStatus.OK, new HttpHeaders());
        request.postWithoutLocation(url, participationsToTrigger, HttpStatus.OK, new HttpHeaders());

        List<ProgrammingSubmission> submissions = submissionRepository.findAll();
        assertThat(submissions).hasSize(2);

        List<ProgrammingExerciseStudentParticipation> participations = new ArrayList<>();
        for (ProgrammingSubmission submission : submissions) {
            var optionalSubmission = submissionRepository.findWithEagerResultsById(submission.getId());
            assertThat(optionalSubmission).isPresent();
            assertThat(optionalSubmission.get().getLatestResult()).isNull();
            assertThat(submission.isSubmitted()).isTrue();
            assertThat(submission.getType()).isEqualTo(SubmissionType.INSTRUCTOR);
            assertThat(submission.getParticipation()).isNotNull();
            // There should be no submission for the participation that was not sent to the endpoint.
            assertThat(submission.getParticipation().getId()).isNotEqualTo(participation2.getId());
            // There should be no participation assigned to two submissions.
            assertThat(participations.stream().noneMatch(p -> p.equals(submission.getParticipation()))).isTrue();
            participations.add((ProgrammingExerciseStudentParticipation) submission.getParticipation());
        }
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    void triggerBuildForParticipationsInstructorParticipationsEmpty() throws Exception {
        String url = "/api/programming-exercises/" + exercise.getId() + "/trigger-instructor-build";
        request.postWithoutLocation(url, List.of(), HttpStatus.BAD_REQUEST, new HttpHeaders());
    }

    @Test
    @WithMockUser(username = "editor1", roles = "EDITOR")
    void triggerBuildForParticipationsEditorForbidden() throws Exception {
        String url = "/api/programming-exercises/" + 1L + "/trigger-instructor-build";
        request.postWithoutLocation(url, new ArrayList<>(), HttpStatus.FORBIDDEN, new HttpHeaders());
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    void triggerBuildForParticipationsTutorForbidden() throws Exception {
        String url = "/api/programming-exercises/" + 1L + "/trigger-instructor-build";
        request.postWithoutLocation(url, new ArrayList<>(), HttpStatus.FORBIDDEN, new HttpHeaders());
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    void triggerBuildForParticipationsStudentForbidden() throws Exception {
        String url = "/api/programming-exercises/" + 1L + "/trigger-instructor-build";
        request.postWithoutLocation(url, new ArrayList<>(), HttpStatus.FORBIDDEN, new HttpHeaders());
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void triggerFailedBuildResultPresentInCIOk() throws Exception {
        var user = database.getUserByLogin("student1");
        var submission = new ProgrammingSubmission();
        submission.setSubmissionDate(ZonedDateTime.now().minusMinutes(4));
        submission.setSubmitted(true);
        submission.setCommitHash(TestConstants.COMMIT_HASH_STRING);
        submission.setType(SubmissionType.MANUAL);
        submission = database.addProgrammingSubmission(exercise, submission, "student1");
        var optionalParticipation = programmingExerciseStudentParticipationRepository.findById(submission.getParticipation().getId());
        assertThat(optionalParticipation).isPresent();
        final var participation = optionalParticipation.get();
        bambooRequestMockProvider.enableMockingOfRequests();
        var buildPlan = new BambooBuildPlanDTO(true, false);
        bambooRequestMockProvider.mockGetBuildPlan(participation.getBuildPlanId(), buildPlan, false);
        // Mock again because we call the trigger request two times
        bambooRequestMockProvider.mockGetBuildPlan(participation.getBuildPlanId(), buildPlan, false);

        String url = "/api" + Constants.PROGRAMMING_SUBMISSION_RESOURCE_PATH + participation.getId() + "/trigger-failed-build";
        request.postWithoutLocation(url, null, HttpStatus.OK, null);

        verify(messagingTemplate).convertAndSendToUser(user.getLogin(), NEW_SUBMISSION_TOPIC, submission);

        // Perform the request again and make sure no new submission was created
        request.postWithoutLocation(url, null, HttpStatus.OK, null);
        var updatedSubmissions = submissionRepository.findAll();
        assertThat(updatedSubmissions.size()).isEqualTo(1);
        assertThat(updatedSubmissions.get(0).getId()).isEqualTo(submission.getId());
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void triggerFailedBuildSubmissionNotLatestButLastGradedNotFound() throws Exception {
        var user = database.getUserByLogin("student1");
        exercise.setDueDate(ZonedDateTime.now().minusDays(1));
        programmingExerciseRepository.save(exercise);
        var submission = new ProgrammingSubmission();
        submission.setType(SubmissionType.MANUAL);
        submission = database.addProgrammingSubmission(exercise, submission, user.getLogin());
        var optionalParticipation = programmingExerciseStudentParticipationRepository.findById(submission.getParticipation().getId());
        assertThat(optionalParticipation).isPresent();
        final var participation = optionalParticipation.get();
        participation.setBuildPlanId(null);
        programmingExerciseStudentParticipationRepository.save(participation);
        doReturn(Optional.of(submission)).when(programmingSubmissionService).getLatestPendingSubmission(anyLong(), anyBoolean());

        String url = "/api" + Constants.PROGRAMMING_SUBMISSION_RESOURCE_PATH + participation.getId() + "/trigger-failed-build?lastGraded=true";
        request.postWithoutLocation(url, null, HttpStatus.NOT_FOUND, null);
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    void triggerFailedBuildForbiddenParticipationAccess() throws Exception {
        String login = "student2";
        StudentParticipation participation = database.addStudentParticipationForProgrammingExercise(exercise, login);

        String url = "/api/programming-submissions/" + participation.getId() + "/trigger-failed-build";
        request.postWithoutLocation(url, null, HttpStatus.FORBIDDEN, new HttpHeaders());
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    void triggerFailedBuildEmptyLatestPendingSubmission() throws Exception {
        bambooRequestMockProvider.enableMockingOfRequests();
        doReturn(COMMIT_HASH_OBJECT_ID).when(gitService).getLastCommitHash(any());

        String login = "student1";
        StudentParticipation participation = database.addStudentParticipationForProgrammingExercise(exercise, login);
        bambooRequestMockProvider.mockTriggerBuild((ProgrammingExerciseParticipation) participation);
        doReturn(Optional.empty()).when(programmingSubmissionService).getLatestPendingSubmission(anyLong(), anyBoolean());

        String url = "/api/programming-submissions/" + participation.getId() + "/trigger-failed-build";
        request.postWithoutLocation(url, null, HttpStatus.BAD_REQUEST, new HttpHeaders());
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    void triggerFailedBuildSubmissionNotFound() throws Exception {
        String login = "student1";
        Course course = database.addCourseWithDifferentModelingExercises();
        ModelingExercise classExercise = database.findModelingExerciseWithTitle(course.getExercises(), "ClassDiagram");
        ModelingSubmission modelingSubmission = ModelFactory.generateModelingSubmission(FileUtils.loadFileFromResources("test-data/model-submission/empty-class-diagram.json"),
                true);
        modelingSubmission = database.addModelingSubmission(classExercise, modelingSubmission, login);

        String url = "/api/programming-submissions/" + modelingSubmission.getParticipation().getId() + "/trigger-failed-build";
        request.postWithoutLocation(url, null, HttpStatus.NOT_FOUND, new HttpHeaders());
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void getAllProgrammingSubmissionsAsUserForbidden() throws Exception {
        request.get("/api/exercises/" + exercise.getId() + "/programming-submissions", HttpStatus.FORBIDDEN, String.class);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void getAllProgrammingSubmissionsAsInstructorAllSubmissionsReturned() throws Exception {
        final var submissions = new LinkedList<ProgrammingSubmission>();
        for (int i = 1; i < 4; i++) {
            final var submission = ModelFactory.generateProgrammingSubmission(true);
            submissions.add(submission);
            database.addProgrammingSubmission(exercise, submission, "student" + i);
        }

        String url = "/api/exercises/" + exercise.getId() + "/programming-submissions";
        final var responseSubmissions = request.getList(url, HttpStatus.OK, ProgrammingSubmission.class);

        assertThat(responseSubmissions).containsExactly(submissions.toArray(new ProgrammingSubmission[0]));
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void getAllProgrammingSubmissionsAssessedByTutorAllSubmissionsReturned() throws Exception {
        database.addProgrammingSubmission(exercise, ModelFactory.generateProgrammingSubmission(true), "student1");
        var assessedSubmission = ModelFactory.generateProgrammingSubmission(true);
        assessedSubmission = database.addProgrammingSubmission(exercise, assessedSubmission, "student2");
        final var tutor = database.getUserByLogin("tutor1");
        database.addResultToSubmission(assessedSubmission, AssessmentType.SEMI_AUTOMATIC, null);
        database.addResultToSubmission(assessedSubmission, AssessmentType.AUTOMATIC, null);
        database.addResultToSubmission(assessedSubmission, AssessmentType.SEMI_AUTOMATIC, tutor);

        final var paramMap = new LinkedMultiValueMap<String, String>();
        paramMap.add("assessedByTutor", "true");

        String url = "/api/exercises/" + exercise.getId() + "/programming-submissions";
        final var responseSubmissions = request.getList(url, HttpStatus.OK, ProgrammingSubmission.class, paramMap);

        assertThat(responseSubmissions).containsExactly(assessedSubmission);
        assertThat(responseSubmissions.get(0).getResults().size()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void getProgrammingSubmissionWithoutAssessmentAsTutorWithOneAvailableReturnsSubmission() throws Exception {
        String login = "student1";
        exercise.setBuildAndTestStudentSubmissionsAfterDueDate(ZonedDateTime.now().minusDays(1));
        exercise.setDueDate(ZonedDateTime.now().minusDays(1));
        programmingExerciseRepository.saveAndFlush(exercise);
        final var submission = database.addProgrammingSubmission(exercise, ModelFactory.generateProgrammingSubmission(true), login);
        database.addResultToSubmission(submission, AssessmentType.AUTOMATIC, null);

        String url = "/api/exercises/" + exercise.getId() + "/programming-submission-without-assessment";
        final var responseSubmission = request.get(url, HttpStatus.OK, ProgrammingSubmission.class);

        assertThat(responseSubmission).isEqualTo(submission);
    }

    @Test
    @WithMockUser(value = "tutor1", roles = "TA")
    public void testLockAndGetProgrammingSubmissionWithManualResult() throws Exception {
        String login = "student1";
        database.addGradingInstructionsToExercise(exercise);
        programmingExerciseRepository.save(exercise);

        ProgrammingSubmission submission = ModelFactory.generateProgrammingSubmission(true);
        submission = database.addProgrammingSubmission(exercise, submission, login);
        exercise.setAssessmentType(AssessmentType.SEMI_AUTOMATIC);
        exercise = programmingExerciseRepository.save(exercise);
        database.updateExerciseDueDate(exercise.getId(), ZonedDateTime.now().minusHours(1));
        Result result = database.addResultToParticipation(AssessmentType.SEMI_AUTOMATIC, ZonedDateTime.now().minusHours(1).minusMinutes(30),
                programmingExerciseStudentParticipation);

        result.setSubmission(submission);
        submission.addResult(result);
        submission.setParticipation(programmingExerciseStudentParticipation);
        submission = submissionRepository.save(submission);
        var submissions = submissionRepository.findAll();

        String url = "/api/programming-submissions/" + submission.getId() + "/lock";
        var storedSubmission = request.get(url, HttpStatus.OK, ProgrammingSubmission.class);

        // Make sure no new submissions are created
        var latestSubmissions = submissionRepository.findAll();
        assertThat(submissions.size()).isEqualTo(latestSubmissions.size());

        // Check that grading instructions are loaded
        ProgrammingExercise exercise = (ProgrammingExercise) storedSubmission.getParticipation().getExercise();
        assertThat(exercise.getGradingCriteria().get(0).getStructuredGradingInstructions().size()).isEqualTo(1);
        assertThat(exercise.getGradingCriteria().get(1).getStructuredGradingInstructions().size()).isEqualTo(1);
    }

    @Test
    @WithMockUser(value = "tutor1", roles = "TA")
    public void testLockAndGetProgrammingSubmissionLessManualResultsThanCorrectionRoundWithoutAutomaticResult() throws Exception {

        ProgrammingSubmission submission = ModelFactory.generateProgrammingSubmission(true);
        submission = database.addProgrammingSubmission(exercise, submission, "student1");
        exercise.setAssessmentType(AssessmentType.SEMI_AUTOMATIC);
        exercise = programmingExerciseRepository.save(exercise);
        database.updateExerciseDueDate(exercise.getId(), ZonedDateTime.now().minusHours(1));

        submission.setParticipation(programmingExerciseStudentParticipation);
        submission = submissionRepository.save(submission);

        // Make sure that there are no results on the current submission
        assertThat(submission.getLatestResult()).isNull();

        String url = "/api/programming-submissions/" + submission.getId() + "/lock";
        var storedSubmission = request.get(url, HttpStatus.OK, ProgrammingSubmission.class);

        // Make sure that the stored submission has a semi automatic assessment by tutor 1
        assertThat(storedSubmission.getLatestResult()).isNotNull();
        assertThat(storedSubmission.getLatestResult().getAssessmentType()).isEqualTo(AssessmentType.SEMI_AUTOMATIC);
        assertThat(storedSubmission.getLatestResult().getAssessor().getLogin()).isEqualTo("tutor1");
    }

    @Test
    @WithMockUser(value = "tutor1", roles = "TA")
    public void testLockAndGetProgrammingSubmissionLessManualResultsThanCorrectionRoundWithAutomaticResult() throws Exception {

        ProgrammingSubmission submission = ModelFactory.generateProgrammingSubmission(true);
        submission = database.addProgrammingSubmission(exercise, submission, "student1");
        exercise.setAssessmentType(AssessmentType.SEMI_AUTOMATIC);
        exercise = programmingExerciseRepository.save(exercise);
        database.updateExerciseDueDate(exercise.getId(), ZonedDateTime.now().minusHours(1));

        submission.setParticipation(programmingExerciseStudentParticipation);
        submission = submissionRepository.save(submission);

        Result result = database.addResultToParticipation(AssessmentType.AUTOMATIC, ZonedDateTime.now().minusHours(1).minusMinutes(30), programmingExerciseStudentParticipation);

        result.setSubmission(submission);
        submission.addResult(result);
        submission.setParticipation(programmingExerciseStudentParticipation);
        submission = submissionRepository.save(submission);

        // Make sure that there is one automatic result on the current submission
        assertThat(submission.getLatestResult()).isNotNull();
        assertThat(submission.getLatestResult().getAssessmentType()).isEqualTo(AssessmentType.AUTOMATIC);

        String url = "/api/programming-submissions/" + submission.getId() + "/lock?correction-round=1";
        var storedSubmission = request.get(url, HttpStatus.OK, ProgrammingSubmission.class);

        // Make sure that the stored submission has a latest manual assessment by tutor 1
        assertThat(storedSubmission.getLatestResult()).isNotNull();
        assertThat(storedSubmission.getLatestResult().getAssessmentType()).isEqualTo(AssessmentType.SEMI_AUTOMATIC);
        assertThat(storedSubmission.getLatestResult().getAssessor().getLogin()).isEqualTo("tutor1");
    }

    @Test
    @WithMockUser(value = "tutor1", roles = "TA")
    public void testLockAndGetProgrammingSubmissionWithoutManualResult() throws Exception {
        var result = database.addResultToParticipation(AssessmentType.AUTOMATIC, ZonedDateTime.now().minusHours(1).minusMinutes(30), programmingExerciseStudentParticipation);
        var submission = database.addProgrammingSubmissionToResultAndParticipation(result, programmingExerciseStudentParticipation, "9b3a9bd71a0d80e5bbc42204c319ed3d1d4f0d6d");
        exercise.setAssessmentType(AssessmentType.AUTOMATIC);
        exercise = programmingExerciseRepository.save(exercise);
        database.updateExerciseDueDate(exercise.getId(), ZonedDateTime.now().minusHours(1));
        var submissions = submissionRepository.findAll();

        String url = "/api/programming-submissions/" + submission.getId() + "/lock";
        request.get(url, HttpStatus.FORBIDDEN, Participation.class);

        // Make sure no new submissions are created
        var latestSubmissions = submissionRepository.findAll();
        assertThat(submissions.size()).isEqualTo(latestSubmissions.size());
    }

    @Test
    @WithMockUser(value = "tutor1", roles = "TA")
    public void testGetProgrammingSubmissionWithoutAssessment() throws Exception {
        String login = "student1";
        ProgrammingSubmission submission = ModelFactory.generateProgrammingSubmission(true);
        submission = database.addProgrammingSubmission(exercise, submission, login);
        database.updateExerciseDueDate(exercise.getId(), ZonedDateTime.now().minusHours(1));

        String url = "/api/exercises/" + exercise.getId() + "/programming-submission-without-assessment";
        ProgrammingSubmission storedSubmission = request.get(url, HttpStatus.OK, ProgrammingSubmission.class);

        // set dates to UTC and round to milliseconds for comparison
        submission.setSubmissionDate(ZonedDateTime.ofInstant(submission.getSubmissionDate().truncatedTo(ChronoUnit.MILLIS).toInstant(), ZoneId.of("UTC")));
        storedSubmission.setSubmissionDate(ZonedDateTime.ofInstant(storedSubmission.getSubmissionDate().truncatedTo(ChronoUnit.MILLIS).toInstant(), ZoneId.of("UTC")));
        assertThat(storedSubmission).as("submission was found").isEqualToIgnoringGivenFields(submission, "result");
        assertThat(storedSubmission.getLatestResult()).as("result is not set").isNull();
    }

    @Test
    @WithMockUser(value = "tutor1", roles = "TA")
    public void testGetProgrammingSubmissionWithoutAssessmentLockSubmission() throws Exception {
        database.addGradingInstructionsToExercise(exercise);
        programmingExerciseRepository.save(exercise);
        User user = database.getUserByLogin("tutor1");
        var newResult = database.addResultToParticipation(AssessmentType.AUTOMATIC, ZonedDateTime.now().minusHours(2), programmingExerciseStudentParticipation);
        programmingExerciseStudentParticipation.addResult(newResult);
        var submission = database.addProgrammingSubmissionToResultAndParticipation(newResult, programmingExerciseStudentParticipation, "9b3a9bd71a0d80e5bbc42204c319ed3d1d4f0d6d");

        database.updateExerciseDueDate(exercise.getId(), ZonedDateTime.now().minusHours(1));

        String url = "/api/exercises/" + exercise.getId() + "/programming-submission-without-assessment?lock=true";
        ProgrammingSubmission storedSubmission = request.get(url, HttpStatus.OK, ProgrammingSubmission.class);

        assertThat(storedSubmission.getLatestResult()).as("result is set").isNotNull();
        assertThat(storedSubmission.getLatestResult().getAssessmentType()).isEqualTo(AssessmentType.SEMI_AUTOMATIC);
        var automaticResults = storedSubmission.getLatestResult().getFeedbacks().stream().filter(feedback -> feedback.getType() == FeedbackType.AUTOMATIC)
                .collect(Collectors.toList());
        assertThat(storedSubmission.getLatestResult().getFeedbacks().size()).isEqualTo(automaticResults.size());
        assertThat(storedSubmission.getLatestResult().getAssessor()).as("assessor is tutor1").isEqualTo(user);
        assertThat(submission.getLatestResult()).isNotNull();
        assertThat(storedSubmission.getLatestResult().getResultString()).isEqualTo(submission.getLatestResult().getResultString());

        // Make sure no new submissions are created
        var latestSubmissions = submissionRepository.findAll();
        assertThat(latestSubmissions.size()).isEqualTo(1);
        assertThat(latestSubmissions.get(0).getId()).isEqualTo(submission.getId());

        // Check that grading instructions are loaded
        ProgrammingExercise exercise = (ProgrammingExercise) storedSubmission.getParticipation().getExercise();
        assertThat(exercise.getGradingCriteria().get(0).getStructuredGradingInstructions().size()).isEqualTo(1);
        assertThat(exercise.getGradingCriteria().get(1).getStructuredGradingInstructions().size()).isEqualTo(1);
    }

    @Test
    @WithMockUser(value = "tutor1", roles = "TA")
    public void testGetModelSubmissionWithoutAssessmentTestLockLimit() throws Exception {
        createTenLockedSubmissionsForExercise("tutor1");
        database.updateExerciseDueDate(exercise.getId(), ZonedDateTime.now().minusHours(1));

        String url = "/api/exercises/" + exercise.getId() + "/programming-submission-without-assessment";
        request.get(url, HttpStatus.BAD_REQUEST, ProgrammingSubmission.class);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void getProgrammingSubmissionWithoutAssessmentDueDateNotPassedYet() throws Exception {
        exercise.setBuildAndTestStudentSubmissionsAfterDueDate(ZonedDateTime.now().plusDays(1));
        programmingExerciseRepository.saveAndFlush(exercise);
        final var submission = database.addProgrammingSubmission(exercise, ModelFactory.generateProgrammingSubmission(true), "student1");
        database.addResultToSubmission(submission, AssessmentType.AUTOMATIC, null);

        String url = "/api/exercises/" + exercise.getId() + "/programming-submission-without-assessment";
        request.get(url, HttpStatus.FORBIDDEN, String.class);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void getProgrammingSubmissionWithoutAssessmentAlreadyAssessedNoFound() throws Exception {
        exercise.setDueDate(ZonedDateTime.now().minusDays(2));
        exercise.setBuildAndTestStudentSubmissionsAfterDueDate(ZonedDateTime.now().minusDays(1));
        programmingExerciseRepository.saveAndFlush(exercise);
        var submission = ModelFactory.generateProgrammingSubmission(true);
        submission = database.addProgrammingSubmission(exercise, submission, "student1");
        final var tutor = database.getUserByLogin("tutor1");
        database.addResultToSubmission(submission, AssessmentType.SEMI_AUTOMATIC, tutor);

        String url = "/api/exercises/" + exercise.getId() + "/programming-submission-without-assessment";
        request.get(url, HttpStatus.NOT_FOUND, String.class);
    }

    private void createTenLockedSubmissionsForExercise(String assessor) {
        ProgrammingSubmission submission;
        for (int i = 1; i < 11; i++) {
            submission = ModelFactory.generateProgrammingSubmission(true);
            database.addProgrammingSubmissionWithResultAndAssessor(exercise, submission, "student" + i, assessor, AssessmentType.SEMI_AUTOMATIC, false);
        }
    }
}
