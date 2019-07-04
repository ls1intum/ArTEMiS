package de.tum.in.www1.artemis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import de.tum.in.www1.artemis.domain.Feedback;
import de.tum.in.www1.artemis.domain.Result;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.enumeration.AssessmentType;
import de.tum.in.www1.artemis.domain.enumeration.EscalationState;
import de.tum.in.www1.artemis.domain.enumeration.FeedbackType;
import de.tum.in.www1.artemis.domain.modeling.ModelAssessmentConflict;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.domain.modeling.ModelingSubmission;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.service.ModelingSubmissionService;
import de.tum.in.www1.artemis.service.ParticipationService;
import de.tum.in.www1.artemis.util.DatabaseUtilService;
import de.tum.in.www1.artemis.util.ModelFactory;
import de.tum.in.www1.artemis.util.RequestUtilService;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@ActiveProfiles("artemis")
public class ModelingAssessmentIntegrationTest {

    @Autowired
    CourseRepository courseRepo;

    @Autowired
    ExerciseRepository exerciseRepo;

    @Autowired
    ModelAssessmentConflictRepository conflictRepo;

    @Autowired
    UserRepository userRepo;

    @Autowired
    RequestUtilService request;

    @Autowired
    DatabaseUtilService database;

    @Autowired
    ModelingSubmissionService modelSubmissionService;

    @Autowired
    ModelingSubmissionRepository modelingSubmissionRepo;

    @Autowired
    ResultRepository resultRepo;

    @Autowired
    ParticipationService participationService;

    private ModelingExercise classExercise;

    private ModelingExercise activityExercise;

    private ModelingExercise objectExercise;

    private ModelingExercise useCaseExercise;

    private ModelingSubmission modelingSubmission;

    private Result modelingAssessment;

    @Before
    public void initTestCase() throws Exception {
        database.resetDatabase();
        database.addUsers(6, 1);
        database.addCourseWithDifferentModelingExercises();
        classExercise = (ModelingExercise) exerciseRepo.findAll().get(0);
        activityExercise = (ModelingExercise) exerciseRepo.findAll().get(1);
        objectExercise = (ModelingExercise) exerciseRepo.findAll().get(2);
        useCaseExercise = (ModelingExercise) exerciseRepo.findAll().get(3);
    }

    @Test
    @WithMockUser(username = "student1")
    public void manualAssessmentSubmitAsStudent() throws Exception {
        ModelingSubmission submission = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.54727.json", "student1");

        List<Feedback> feedbacks = database.loadAssessmentFomResources("test-data/model-assessment/assessment.54727.json");
        request.put("/api/modeling-submissions/" + submission.getId() + "/feedback?submit=true", feedbacks, HttpStatus.FORBIDDEN);

        Optional<Result> storedResult = resultRepo.findDistinctBySubmissionId(submission.getId());
        assertThat(storedResult).as("result is not saved").isNotPresent();
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void manualAssessmentSave() throws Exception {
        User assessor = database.getUserByLogin("tutor1");
        ModelingSubmission submission = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.54727.json", "student1");

        List<Feedback> feedbacks = database.loadAssessmentFomResources("test-data/model-assessment/assessment.54727.json");
        request.put("/api/modeling-submissions/" + submission.getId() + "/feedback", feedbacks, HttpStatus.OK);

        ModelingSubmission storedSubmission = modelingSubmissionRepo.findById(submission.getId()).get();
        Result storedResult = resultRepo.findByIdWithEagerFeedbacksAndAssessor(storedSubmission.getResult().getId()).get();
        checkFeedbackCorrectlyStored(feedbacks, storedResult.getFeedbacks(), FeedbackType.MANUAL);
        checkAssessmentNotFinished(storedResult, assessor);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void manualAssessmentSubmit_classDiagram() throws Exception {
        User assessor = database.getUserByLogin("tutor1");
        ModelingSubmission submission = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.54727.json", "student1");

        List<Feedback> feedbacks = database.loadAssessmentFomResources("test-data/model-assessment/assessment.54727.json");
        request.put("/api/modeling-submissions/" + submission.getId() + "/feedback?submit=true", feedbacks, HttpStatus.OK);

        ModelingSubmission storedSubmission = modelingSubmissionRepo.findById(submission.getId()).get();
        Result storedResult = resultRepo.findByIdWithEagerFeedbacksAndAssessor(storedSubmission.getResult().getId()).get();
        checkFeedbackCorrectlyStored(feedbacks, storedResult.getFeedbacks(), FeedbackType.MANUAL);
        checkAssessmentFinished(storedResult, assessor);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void manualAssessmentSubmit_activityDiagram() throws Exception {
        User assessor = database.getUserByLogin("tutor1");
        ModelingSubmission submission = database.addModelingSubmissionFromResources(activityExercise, "test-data/model-submission/activity-model.json", "student1");

        List<Feedback> feedbacks = database.loadAssessmentFomResources("test-data/model-assessment/activity-assessment.json");
        request.put("/api/modeling-submissions/" + submission.getId() + "/feedback?submit=true", feedbacks, HttpStatus.OK);

        ModelingSubmission storedSubmission = modelingSubmissionRepo.findById(submission.getId()).get();
        Result storedResult = resultRepo.findByIdWithEagerFeedbacksAndAssessor(storedSubmission.getResult().getId()).get();
        checkFeedbackCorrectlyStored(feedbacks, storedResult.getFeedbacks(), FeedbackType.MANUAL);
        checkAssessmentFinished(storedResult, assessor);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void manualAssessmentSubmit_objectDiagram() throws Exception {
        User assessor = database.getUserByLogin("tutor1");
        ModelingSubmission submission = database.addModelingSubmissionFromResources(objectExercise, "test-data/model-submission/object-model.json", "student1");

        List<Feedback> feedbacks = database.loadAssessmentFomResources("test-data/model-assessment/object-assessment.json");
        request.put("/api/modeling-submissions/" + submission.getId() + "/feedback?submit=true", feedbacks, HttpStatus.OK);

        ModelingSubmission storedSubmission = modelingSubmissionRepo.findById(submission.getId()).get();
        Result storedResult = resultRepo.findByIdWithEagerFeedbacksAndAssessor(storedSubmission.getResult().getId()).get();
        checkFeedbackCorrectlyStored(feedbacks, storedResult.getFeedbacks(), FeedbackType.MANUAL);
        checkAssessmentFinished(storedResult, assessor);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void manualAssessmentSubmit_useCaseDiagram() throws Exception {
        User assessor = database.getUserByLogin("tutor1");
        ModelingSubmission submission = database.addModelingSubmissionFromResources(useCaseExercise, "test-data/model-submission/use-case-model.json", "student1");

        List<Feedback> feedbacks = database.loadAssessmentFomResources("test-data/model-assessment/use-case-assessment.json");
        request.put("/api/modeling-submissions/" + submission.getId() + "/feedback?submit=true", feedbacks, HttpStatus.OK);

        ModelingSubmission storedSubmission = modelingSubmissionRepo.findById(submission.getId()).get();
        Result storedResult = resultRepo.findByIdWithEagerFeedbacksAndAssessor(storedSubmission.getResult().getId()).get();
        checkFeedbackCorrectlyStored(feedbacks, storedResult.getFeedbacks(), FeedbackType.MANUAL);
        checkAssessmentFinished(storedResult, assessor);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void manualAssessmentSaveAndSubmit() throws Exception {
        User assessor = database.getUserByLogin("tutor1");
        ModelingSubmission submission = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.54727.json", "student1");

        List<Feedback> feedbacks = database.loadAssessmentFomResources("test-data/model-assessment/assessment.54727.json");
        request.put("/api/modeling-submissions/" + submission.getId() + "/feedback", feedbacks, HttpStatus.OK);

        ModelingSubmission storedSubmission = modelingSubmissionRepo.findById(submission.getId()).get();
        Result storedResult = resultRepo.findByIdWithEagerFeedbacksAndAssessor(storedSubmission.getResult().getId()).get();
        checkFeedbackCorrectlyStored(feedbacks, storedResult.getFeedbacks(), FeedbackType.MANUAL);
        checkAssessmentNotFinished(storedResult, assessor);

        feedbacks = database.loadAssessmentFomResources("test-data/model-assessment/assessment.54727.v2.json");
        request.put("/api/modeling-submissions/" + submission.getId() + "/feedback?submit=true", feedbacks, HttpStatus.OK);

        storedSubmission = modelingSubmissionRepo.findById(submission.getId()).get();
        storedResult = resultRepo.findByIdWithEagerFeedbacksAndAssessor(storedSubmission.getResult().getId()).get();
        checkFeedbackCorrectlyStored(feedbacks, storedResult.getFeedbacks(), FeedbackType.MANUAL);
        checkAssessmentFinished(storedResult, assessor);
    }

    // region Automatic Assessment Tests
    @Test
    @WithMockUser(username = "student2")
    public void automaticAssessmentUponModelSubmission_identicalModel() throws Exception {
        saveModelingSubmissionAndAssessment();
        database.addParticipationForExercise(classExercise, "student2");

        ModelingSubmission submission = ModelFactory.generateModelingSubmission(database.loadFileFromResources("test-data/model-submission/model.54727.cpy.json"), true);
        ModelingSubmission storedSubmission = request.postWithResponseBody("/api/exercises/" + classExercise.getId() + "/modeling-submissions", submission,
                ModelingSubmission.class, HttpStatus.OK);

        Optional<Result> automaticResult = resultRepo.findDistinctWithFeedbackBySubmissionId(storedSubmission.getId());
        assertThat(automaticResult).as("automatic assessment is created").isPresent();
        checkAutomaticAssessment(automaticResult.get());
        checkFeedbackCorrectlyStored(modelingAssessment.getFeedbacks(), automaticResult.get().getFeedbacks(), FeedbackType.AUTOMATIC);
    }

    @Test
    @WithMockUser(username = "student2")
    public void automaticAssessmentUponModelSubmission_partialModel() throws Exception {
        saveModelingSubmissionAndAssessment();
        database.addParticipationForExercise(classExercise, "student2");

        ModelingSubmission submission = ModelFactory.generateModelingSubmission(database.loadFileFromResources("test-data/model-submission/model.54727.partial.json"), true);
        ModelingSubmission storedSubmission = request.postWithResponseBody("/api/exercises/" + classExercise.getId() + "/modeling-submissions", submission,
                ModelingSubmission.class, HttpStatus.OK);

        Optional<Result> automaticResult = resultRepo.findDistinctWithFeedbackBySubmissionId(storedSubmission.getId());
        assertThat(automaticResult).as("automatic assessment is created").isPresent();
        checkAutomaticAssessment(automaticResult.get());
        List<Feedback> feedbackUsedForAutomaticAssessment = modelingAssessment.getFeedbacks().stream()
                .filter(feedback -> automaticResult.get().getFeedbacks().stream().anyMatch(storedFeedback -> storedFeedback.getReference().equals(feedback.getReference())))
                .collect(Collectors.toList());
        checkFeedbackCorrectlyStored(feedbackUsedForAutomaticAssessment, automaticResult.get().getFeedbacks(), FeedbackType.AUTOMATIC);
    }

    @Test
    @WithMockUser(username = "student2")
    public void automaticAssessmentUponModelSubmission_partialModelExists() throws Exception {
        modelingSubmission = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.54727.partial.json", "student1");
        modelingAssessment = database.addModelingAssessmentForSubmission(classExercise, modelingSubmission, "test-data/model-assessment/assessment.54727.partial.json", "tutor1");
        database.addParticipationForExercise(classExercise, "student2");

        ModelingSubmission submission = ModelFactory.generateModelingSubmission(database.loadFileFromResources("test-data/model-submission/model.54727.json"), true);
        ModelingSubmission storedSubmission = request.postWithResponseBody("/api/exercises/" + classExercise.getId() + "/modeling-submissions", submission,
                ModelingSubmission.class, HttpStatus.OK);

        Optional<Result> automaticResult = resultRepo.findDistinctWithFeedbackBySubmissionId(storedSubmission.getId());
        assertThat(automaticResult).as("automatic assessment is created").isPresent();
        checkAutomaticAssessment(automaticResult.get());
        checkFeedbackCorrectlyStored(modelingAssessment.getFeedbacks(), automaticResult.get().getFeedbacks(), FeedbackType.AUTOMATIC);
    }

    @Test
    @WithMockUser(username = "student2")
    public void automaticAssessmentUponModelSubmission_noSimilarity() throws Exception {
        modelingSubmission = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.54745.json", "student1");
        database.addModelingAssessmentForSubmission(classExercise, modelingSubmission, "test-data/model-assessment/assessment.54745.json", "tutor1");
        database.addParticipationForExercise(classExercise, "student2");

        ModelingSubmission submission = ModelFactory.generateModelingSubmission(database.loadFileFromResources("test-data/model-submission/model.54727.json"), true);
        ModelingSubmission storedSubmission = request.postWithResponseBody("/api/exercises/" + classExercise.getId() + "/modeling-submissions", submission,
                ModelingSubmission.class, HttpStatus.OK);

        Optional<Result> automaticResult = resultRepo.findDistinctWithFeedbackBySubmissionId(storedSubmission.getId());
        assertThat(automaticResult).as("automatic assessment is created").isPresent();
        checkAutomaticAssessment(automaticResult.get());
        assertThat(automaticResult.get().getFeedbacks()).as("no feedback has been assigned").isEmpty();
    }

    @Test
    @WithMockUser(username = "student2")
    public void automaticAssessmentUponModelSubmission_similarElementsWithinModel() throws Exception {
        modelingSubmission = ModelFactory.generateModelingSubmission(database.loadFileFromResources("test-data/model-submission/model.inheritance.json"), true);
        modelingSubmission = database.addModelingSubmission(classExercise, modelingSubmission, "student1");
        modelingAssessment = database.addModelingAssessmentForSubmission(classExercise, modelingSubmission, "test-data/model-assessment/assessment.inheritance.json", "tutor1");
        database.addParticipationForExercise(classExercise, "student2");

        ModelingSubmission submission = ModelFactory.generateModelingSubmission(database.loadFileFromResources("test-data/model-submission/model.inheritance.cpy.json"), true);
        ModelingSubmission storedSubmission = request.postWithResponseBody("/api/exercises/" + classExercise.getId() + "/modeling-submissions", submission,
                ModelingSubmission.class, HttpStatus.OK);

        Optional<Result> automaticResult = resultRepo.findDistinctWithFeedbackBySubmissionId(storedSubmission.getId());
        assertThat(automaticResult).as("automatic assessment is created").isPresent();
        checkAutomaticAssessment(automaticResult.get());
        checkFeedbackCorrectlyStored(modelingAssessment.getFeedbacks(), automaticResult.get().getFeedbacks(), FeedbackType.AUTOMATIC);
    }

    @Test
    @WithMockUser(username = "student2")
    public void noAutomaticAssessmentUponModelSave() throws Exception {
        saveModelingSubmissionAndAssessment();
        database.addParticipationForExercise(classExercise, "student2");

        ModelingSubmission submission = ModelFactory.generateModelingSubmission(database.loadFileFromResources("test-data/model-submission/model.54727.cpy.json"), false);
        ModelingSubmission storedSubmission = request.postWithResponseBody("/api/exercises/" + classExercise.getId() + "/modeling-submissions", submission,
                ModelingSubmission.class, HttpStatus.OK);

        assertThat(storedSubmission.getResult()).as("no result has been created").isNull();
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testConfidenceThreshold() throws Exception {
        Feedback feedbackOnePoint = new Feedback().credits(1.0).reference("Class:6aba5764-d102-4740-9675-b2bd0a4f2123");
        Feedback feedbackTwentyPoints = new Feedback().credits(20.0).reference("Class:6aba5764-d102-4740-9675-b2bd0a4f2123");
        ModelingSubmission submission1 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.one-element.json", "student1");
        ModelingSubmission submission2 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.one-element.json", "student2");
        ModelingSubmission submission3 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.one-element.json", "student3");
        ModelingSubmission submission4 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.one-element.json", "student4");
        ModelingSubmission submission5 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.one-element.json", "student5");
        ModelingSubmission submissionToCheck = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.one-element.json", "student6");

        request.put("/api/modeling-submissions/" + submission1.getId() + "/feedback?submit=true&ignoreConflicts=true",
                Collections.singletonList(feedbackTwentyPoints.text("wrong text")), HttpStatus.OK);

        Optional<Result> automaticResult = resultRepo.findDistinctWithFeedbackBySubmissionId(submissionToCheck.getId());
        assertThat(automaticResult).as("automatic result was created").isPresent();
        assertThat(automaticResult.get().getFeedbacks().size()).as("element is assessed automatically").isEqualTo(1);
        assertThat(automaticResult.get().getFeedbacks().get(0).getCredits()).as("credits of element are correct").isEqualTo(20);
        assertThat(automaticResult.get().getFeedbacks().get(0).getText()).as("feedback text of element is correct").isEqualTo("wrong text");

        request.put("/api/modeling-submissions/" + submission2.getId() + "/feedback?submit=true&ignoreConflicts=true",
                Collections.singletonList(feedbackOnePoint.text("long feedback text")), HttpStatus.OK);

        automaticResult = resultRepo.findDistinctWithFeedbackBySubmissionId(submissionToCheck.getId());
        assertThat(automaticResult).as("automatic result was created").isPresent();
        assertThat(automaticResult.get().getFeedbacks().size()).as("element is not assessed automatically").isEqualTo(0);

        request.put("/api/modeling-submissions/" + submission3.getId() + "/feedback?submit=true&ignoreConflicts=true",
                Collections.singletonList(feedbackOnePoint.text("short text")), HttpStatus.OK);

        automaticResult = resultRepo.findDistinctWithFeedbackBySubmissionId(submissionToCheck.getId());
        assertThat(automaticResult).as("automatic result was created").isPresent();
        assertThat(automaticResult.get().getFeedbacks().size()).as("element is not assessed automatically").isEqualTo(0);

        request.put("/api/modeling-submissions/" + submission4.getId() + "/feedback?submit=true&ignoreConflicts=true",
                Collections.singletonList(feedbackOnePoint.text("very long feedback text")), HttpStatus.OK);

        automaticResult = resultRepo.findDistinctWithFeedbackBySubmissionId(submissionToCheck.getId());
        assertThat(automaticResult).as("automatic result was created").isPresent();
        assertThat(automaticResult.get().getFeedbacks().size()).as("element is not assessed automatically").isEqualTo(0);

        request.put("/api/modeling-submissions/" + submission5.getId() + "/feedback?submit=true&ignoreConflicts=true",
                Collections.singletonList(feedbackOnePoint.text("medium text")), HttpStatus.OK);

        automaticResult = resultRepo.findDistinctWithFeedbackBySubmissionId(submissionToCheck.getId());
        assertThat(automaticResult).as("automatic result was created").isPresent();
        assertThat(automaticResult.get().getFeedbacks().size()).as("element is assessed automatically").isEqualTo(1);
        assertThat(automaticResult.get().getFeedbacks().get(0).getCredits()).as("credits of element are correct").isEqualTo(1);
        assertThat(automaticResult.get().getFeedbacks().get(0).getText()).as("feedback text of element is correct").isEqualTo("very long feedback text");
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testLongestFeedbackTextSelection() throws Exception {
        Feedback feedbackOnePoint = new Feedback().credits(1.0).reference("Class:6aba5764-d102-4740-9675-b2bd0a4f2123");
        ModelingSubmission submission1 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.one-element.json", "student1");
        ModelingSubmission submission2 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.one-element.json", "student2");
        ModelingSubmission submission3 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.one-element.json", "student3");
        ModelingSubmission submissionToCheck = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.one-element.json", "student4");

        request.put("/api/modeling-submissions/" + submission1.getId() + "/feedback?submit=true&ignoreConflicts=true",
                Collections.singletonList(feedbackOnePoint.text("feedback text")), HttpStatus.OK);

        Optional<Result> automaticResult = resultRepo.findDistinctWithFeedbackBySubmissionId(submissionToCheck.getId());
        assertThat(automaticResult).as("automatic result was created").isPresent();
        assertThat(automaticResult.get().getFeedbacks().size()).as("element is assessed automatically").isEqualTo(1);
        assertThat(automaticResult.get().getFeedbacks().get(0).getText()).as("feedback text of element is correct").isEqualTo("feedback text");

        request.put("/api/modeling-submissions/" + submission2.getId() + "/feedback?submit=true&ignoreConflicts=true", Collections.singletonList(feedbackOnePoint.text("short")),
                HttpStatus.OK);

        automaticResult = resultRepo.findDistinctWithFeedbackBySubmissionId(submissionToCheck.getId());
        assertThat(automaticResult).as("automatic result was created").isPresent();
        assertThat(automaticResult.get().getFeedbacks().size()).as("element is assessed automatically").isEqualTo(1);
        assertThat(automaticResult.get().getFeedbacks().get(0).getText()).as("feedback text of element is correct").isEqualTo("feedback text");

        request.put("/api/modeling-submissions/" + submission3.getId() + "/feedback?submit=true&ignoreConflicts=true",
                Collections.singletonList(feedbackOnePoint.text("very long feedback text")), HttpStatus.OK);

        automaticResult = resultRepo.findDistinctWithFeedbackBySubmissionId(submissionToCheck.getId());
        assertThat(automaticResult).as("automatic result was created").isPresent();
        assertThat(automaticResult.get().getFeedbacks().size()).as("element is assessed automatically").isEqualTo(1);
        assertThat(automaticResult.get().getFeedbacks().get(0).getText()).as("feedback text of element is correct").isEqualTo("very long feedback text");
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void automaticAssessmentUponAssessmentSubmission() throws Exception {
        ModelingSubmission submission1 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.54727.json", "student1");
        ModelingSubmission submission2 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.54727.cpy.json", "student2");
        List<Feedback> feedbacks = database.loadAssessmentFomResources("test-data/model-assessment/assessment.54727.json");

        request.put("/api/modeling-submissions/" + submission1.getId() + "/feedback?submit=true", feedbacks, HttpStatus.OK);

        Optional<Result> storedResultOfSubmission2 = resultRepo.findDistinctWithFeedbackBySubmissionId(submission2.getId());
        assertThat(storedResultOfSubmission2).as("result is present").isPresent();
        checkAutomaticAssessment(storedResultOfSubmission2.get());
        checkFeedbackCorrectlyStored(feedbacks, storedResultOfSubmission2.get().getFeedbacks(), FeedbackType.AUTOMATIC);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void noAutomaticAssessmentUponAssessmentSave() throws Exception {
        ModelingSubmission submission1 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.54727.json", "student1");
        ModelingSubmission submission2 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.54727.cpy.json", "student2");
        List<Feedback> feedbacks = database.loadAssessmentFomResources("test-data/model-assessment/assessment.54727.json");

        request.put("/api/modeling-submissions/" + submission1.getId() + "/feedback", feedbacks, HttpStatus.OK);

        Optional<Result> storedResultOfSubmission2 = resultRepo.findDistinctWithFeedbackBySubmissionId(submission2.getId());
        assertThat(storedResultOfSubmission2).as("result is not present").isNotPresent();
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testAutomaticAssessment_elementsWithDifferentContextInSameSimilaritySet() throws Exception {
        List<Feedback> assessment1 = database.loadAssessmentFomResources("test-data/model-assessment/assessment.different-context.json");
        List<Feedback> assessment2 = database.loadAssessmentFomResources("test-data/model-assessment/assessment.different-context.automatic.json");
        ModelingSubmission submission1 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.different-context.json", "student1");
        ModelingSubmission submission2 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.different-context.json", "student2");
        ModelingSubmission submissionToCheck = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.different-context.json", "student3");

        request.put("/api/modeling-submissions/" + submission1.getId() + "/feedback?submit=true&ignoreConflicts=true", assessment1, HttpStatus.OK);

        Optional<Result> automaticResult = resultRepo.findDistinctWithFeedbackBySubmissionId(submissionToCheck.getId());
        assertThat(automaticResult).as("automatic result was created").isPresent();
        assertThat(automaticResult.get().getFeedbacks().size()).as("element is assessed automatically").isEqualTo(4);

        request.put("/api/modeling-submissions/" + submission2.getId() + "/feedback?submit=true&ignoreConflicts=true", assessment2, HttpStatus.OK);

        automaticResult = resultRepo.findDistinctWithFeedbackBySubmissionId(submissionToCheck.getId());
        assertThat(automaticResult).as("automatic result was created").isPresent();
        assertThat(automaticResult.get().getFeedbacks().size()).as("element is not assessed automatically").isEqualTo(2);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void overrideAutomaticAssessment() throws Exception {
        modelingSubmission = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.54727.partial.json", "student1");
        modelingAssessment = database.addModelingAssessmentForSubmission(classExercise, modelingSubmission, "test-data/model-assessment/assessment.54727.partial.json", "tutor1");
        database.addParticipationForExercise(classExercise, "tutor1");

        ModelingSubmission submission = ModelFactory.generateModelingSubmission(database.loadFileFromResources("test-data/model-submission/model.54727.json"), true);
        ModelingSubmission storedSubmission = request.postWithResponseBody("/api/exercises/" + classExercise.getId() + "/modeling-submissions", submission,
                ModelingSubmission.class, HttpStatus.OK);

        List<Feedback> existingFeedback = storedSubmission.getResult().getFeedbacks();
        Feedback feedback = existingFeedback.get(0);
        existingFeedback.set(0, feedback.credits(feedback.getCredits() + 0.5));
        feedback = existingFeedback.get(2);
        existingFeedback.set(2, feedback.text(feedback.getText() + " foo"));
        List<Feedback> newFeedback = database.loadAssessmentFomResources("test-data/model-assessment/assessment.54727.partial2.json");
        List<Feedback> overrideFeedback = new ArrayList<>(existingFeedback);
        overrideFeedback.addAll(newFeedback);

        Result storedResult = request.putWithResponseBody("/api/modeling-submissions/" + modelingSubmission.getId() + "/feedback", overrideFeedback, Result.class, HttpStatus.OK);

        List<Feedback> manualFeedback = new ArrayList<>();
        List<Feedback> automaticFeedback = new ArrayList<>();
        List<Feedback> adaptedFeedback = new ArrayList<>();
        storedResult.getFeedbacks().forEach(storedFeedback -> {
            if (storedFeedback.getType().equals(FeedbackType.MANUAL)) {
                manualFeedback.add(storedFeedback);
            }
            else if (storedFeedback.getType().equals(FeedbackType.AUTOMATIC)) {
                automaticFeedback.add(storedFeedback);
            }
            else {
                adaptedFeedback.add(storedFeedback);
            }
        });
        assertThat(storedResult.getAssessmentType()).as("type of result is MANUAL").isEqualTo(AssessmentType.MANUAL);
        assertThat(manualFeedback.size()).as("number of manual feedback elements is correct").isEqualTo(newFeedback.size());
        assertThat(automaticFeedback.size()).as("number of automatic feedback elements is correct").isEqualTo(existingFeedback.size() - 2);
        assertThat(adaptedFeedback.size()).as("number of adapted feedback elements is correct").isEqualTo(2);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void overrideAutomaticAssessment_existingManualAssessmentDoesNotChange() throws Exception {
        Feedback originalFeedback = new Feedback().credits(1.0).text("some feedback text").reference("Class:6aba5764-d102-4740-9675-b2bd0a4f2123");
        Feedback changedFeedback = new Feedback().credits(2.0).text("another text").reference("Class:6aba5764-d102-4740-9675-b2bd0a4f2123");
        modelingSubmission = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.one-element.json", "student1");
        ModelingSubmission modelingSubmission2 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.one-element.json", "student2");
        request.put("/api/modeling-submissions/" + modelingSubmission.getId() + "/feedback?submit=true&ignoreConflicts=true", Collections.singletonList(originalFeedback),
                HttpStatus.OK);

        request.put("/api/modeling-submissions/" + modelingSubmission2.getId() + "/feedback?submit=true&ignoreConflicts=true", Collections.singletonList(changedFeedback),
                HttpStatus.OK);

        modelingAssessment = resultRepo.findDistinctWithFeedbackBySubmissionId(modelingSubmission2.getId()).get();
        assertThat(modelingAssessment.getFeedbacks().size()).as("assessment is correctly stored").isEqualTo(1);
        assertThat(modelingAssessment.getFeedbacks().get(0)).as("feedback credits and text are correct").isEqualToComparingOnlyGivenFields(changedFeedback, "credits", "text");
        modelingAssessment = resultRepo.findDistinctWithFeedbackBySubmissionId(modelingSubmission.getId()).get();
        assertThat(modelingAssessment.getFeedbacks().size()).as("existing manual assessment has correct amount of feedback").isEqualTo(1);
        assertThat(modelingAssessment.getFeedbacks().get(0)).as("existing manual assessment did not change").isEqualToComparingOnlyGivenFields(originalFeedback, "credits", "text");
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void overrideSubmittedManualAssessment_noConflict() throws Exception {
        Feedback originalFeedback = new Feedback().credits(1.0).text("some feedback text").reference("Class:6aba5764-d102-4740-9675-b2bd0a4f2123");
        modelingSubmission = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.one-element.json", "student1");
        ModelingSubmission modelingSubmission2 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.one-element.json", "student2");
        request.put("/api/modeling-submissions/" + modelingSubmission.getId() + "/feedback?submit=true&ignoreConflicts=true", Collections.singletonList(originalFeedback),
                HttpStatus.OK);

        Result originalResult = resultRepo.findDistinctWithFeedbackBySubmissionId(modelingSubmission.getId()).get();
        Feedback changedFeedback = originalResult.getFeedbacks().get(0).credits(2.0).text("another text");
        request.put("/api/modeling-submissions/" + modelingSubmission.getId() + "/feedback?submit=true&ignoreConflicts=true", Collections.singletonList(changedFeedback),
                HttpStatus.OK);

        modelingAssessment = resultRepo.findDistinctWithFeedbackBySubmissionId(modelingSubmission.getId()).get();
        assertThat(modelingAssessment.getFeedbacks().size()).as("overridden assessment has correct amount of feedback").isEqualTo(1);
        assertThat(modelingAssessment.getFeedbacks().get(0)).as("feedback is properly overridden").isEqualToComparingOnlyGivenFields(changedFeedback, "credits", "text");
        modelingAssessment = resultRepo.findDistinctWithFeedbackBySubmissionId(modelingSubmission2.getId()).get();
        assertThat(modelingAssessment.getFeedbacks().size()).as("automatic assessment still exists").isEqualTo(1);
        assertThat(modelingAssessment.getFeedbacks().get(0)).as("automatic assessment is overridden properly").isEqualToComparingOnlyGivenFields(changedFeedback, "credits",
                "text");
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void overrideSubmittedManualAssessment_conflict() throws Exception {
        Feedback originalFeedback = new Feedback().credits(1.0).text("some feedback text").reference("Class:6aba5764-d102-4740-9675-b2bd0a4f2123");
        modelingSubmission = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.one-element.json", "student1");
        ModelingSubmission modelingSubmission2 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.one-element.json", "student2");
        ModelingSubmission modelingSubmission3 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.one-element.json", "student3");
        request.put("/api/modeling-submissions/" + modelingSubmission.getId() + "/feedback?submit=true&ignoreConflicts=true", Collections.singletonList(originalFeedback),
                HttpStatus.OK);
        request.put("/api/modeling-submissions/" + modelingSubmission2.getId() + "/feedback?submit=true&ignoreConflicts=true", Collections.singletonList(originalFeedback),
                HttpStatus.OK);

        Result originalResult = resultRepo.findDistinctWithFeedbackBySubmissionId(modelingSubmission.getId()).get();
        Feedback changedFeedback = originalResult.getFeedbacks().get(0).credits(2.0).text("another text");
        request.put("/api/modeling-submissions/" + modelingSubmission.getId() + "/feedback?submit=true&ignoreConflicts=true", Collections.singletonList(changedFeedback),
                HttpStatus.OK);

        modelingAssessment = resultRepo.findDistinctWithFeedbackBySubmissionId(modelingSubmission.getId()).get();
        assertThat(modelingAssessment.getFeedbacks().size()).as("overridden assessment has correct amount of feedback").isEqualTo(1);
        assertThat(modelingAssessment.getFeedbacks().get(0)).as("feedback is properly overridden").isEqualToComparingOnlyGivenFields(changedFeedback, "credits", "text");
        modelingAssessment = resultRepo.findDistinctWithFeedbackBySubmissionId(modelingSubmission2.getId()).get();
        assertThat(modelingAssessment.getFeedbacks().size()).as("existing submitted assessment still exists").isEqualTo(1);
        assertThat(modelingAssessment.getFeedbacks().get(0)).as("existing feedback is still the same").isEqualToComparingOnlyGivenFields(originalFeedback, "credits", "text");
        modelingAssessment = resultRepo.findDistinctWithFeedbackBySubmissionId(modelingSubmission3.getId()).get();
        assertThat(modelingAssessment.getFeedbacks().size()).as("automatic assessment is not possible").isEqualTo(0);
    }
    // endregion

    // TODO: Fix defective test
    @Ignore
    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testConflictDetection() throws Exception {
        ModelingSubmission submission1 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.conflict.1.json", "student1");
        ModelingSubmission submission2 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.conflict.2.json", "student2");
        causeConflict("tutor1", submission1, submission2);
    }

    // TODO: Fix defective test
    @Ignore
    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testResolvePartConflictByCausingTutorOnUpdate() throws Exception {
        ModelingSubmission submission1 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.conflict.1.json", "student1");
        ModelingSubmission submission2 = database.addModelingSubmissionFromResources(classExercise, "test-data/model-submission/model.conflict.2.json", "student2");
        causeConflict("tutor1", submission1, submission2);
        List<Feedback> feedbacks = database.loadAssessmentFomResources("test-data/model-assessment/assessment.conflict.2.update.json");
        List<ModelAssessmentConflict> conflicts = request.putWithResponseBodyList("/api/modeling-submissions/" + submission2.getId() + "/feedback?submit=true", feedbacks,
                ModelAssessmentConflict.class, HttpStatus.CONFLICT);
        assertThat(conflicts.size()).as("1 Conflict got resolved").isEqualTo(2);
        conflicts.forEach(conflict -> {
            assertThat(conflict.getCausingConflictingResult().getModelElementId()).as("correct conflict has been resolved").doesNotMatch("62db30c6-520e-4346-b841-4cf98857d784");
        });
    }

    private List<ModelAssessmentConflict> causeConflict(String assessorName, ModelingSubmission submission1, ModelingSubmission submission2) throws Exception {
        User assessor = database.getUserByLogin(assessorName);
        List<Feedback> feedbacks1 = database.loadAssessmentFomResources("test-data/model-assessment/assessment.conflict.1.json");
        List<Feedback> feedbacks2 = database.loadAssessmentFomResources("test-data/model-assessment/assessment.conflict.2.json");
        request.put("/api/modeling-submissions/" + submission1.getId() + "/feedback?submit=true", feedbacks1, HttpStatus.OK);
        List<ModelAssessmentConflict> conflicts = request.putWithResponseBodyList("/api/modeling-submissions/" + submission2.getId() + "/feedback?submit=true", feedbacks2,
                ModelAssessmentConflict.class, HttpStatus.CONFLICT);
        ModelingSubmission stored2ndSubmission = modelingSubmissionRepo.findById(submission2.getId()).get();
        Result stored2ndResult = resultRepo.findByIdWithEagerFeedbacks(stored2ndSubmission.getResult().getId()).get();
        checkFeedbackCorrectlyStored(feedbacks2, stored2ndResult.getFeedbacks(), FeedbackType.MANUAL);
        checkAssessmentNotFinished(stored2ndResult, assessor);
        conflicts.forEach(conflict -> {
            assertThat(conflict.getCausingConflictingResult().getResult().getId()).as("CausingResult correctly set").isEqualTo(stored2ndResult.getId());
            assertThat(conflict.getState()).as("conflicts have correct state").isEqualTo(EscalationState.UNHANDLED);
            assertThat(conflict.getCausingConflictingResult().getModelElementId()).as("correct model elements detected")
                    .matches("(62db30c6-520e-4346-b841-4cf98857d784|0749e2c9-1abc-4460-a28d-b6ffdd52b026|77f659ca-670f-4942-beb1-5b257971fc27)");
        });
        assertThat(conflictRepo.count()).as("all conflicts have been persisted").isEqualTo(conflicts.size());
        return conflicts;
    }

    private void checkAssessmentNotFinished(Result storedResult, User assessor) {
        assertThat(storedResult.isRated() == null || !storedResult.isRated()).as("rated has not been set").isTrue();
        assertThat(storedResult.getScore()).as("score has not been calculated").isNull();
        assertThat(storedResult.getAssessor()).as("Assessor has been set").isEqualTo(assessor);
        assertThat(storedResult.getResultString()).as("result string has not been set").isNull();
        assertThat(storedResult.getCompletionDate()).as("completion date has not been set").isNull();
    }

    private void checkAssessmentFinished(Result storedResult, User assessor) {
        assertThat(storedResult.isRated()).as("rated has been set").isTrue();
        assertThat(storedResult.getScore()).as("score has been calculated").isNotNull();
        assertThat(storedResult.getAssessor()).as("Assessor has been set").isEqualTo(assessor);
        assertThat(storedResult.getResultString()).as("result string has been set").isNotNull().isNotEqualTo("");
        assertThat(storedResult.getCompletionDate()).as("completion date has been set").isNotNull();
    }

    private void checkFeedbackCorrectlyStored(List<Feedback> sentFeedback, List<Feedback> storedFeedback, FeedbackType feedbackType) {
        assertThat(sentFeedback.size()).as("contains the same amount of feedback").isEqualTo(storedFeedback.size());
        Result storedFeedbackResult = new Result();
        Result sentFeedbackResult = new Result();
        storedFeedbackResult.setFeedbacks(storedFeedback);
        sentFeedbackResult.setFeedbacks(sentFeedback);
        storedFeedbackResult.evaluateFeedback(20);
        sentFeedbackResult.evaluateFeedback(20);
        assertThat(storedFeedbackResult.getScore()).as("stored feedback evaluates to the same score as sent feedback").isEqualTo(sentFeedbackResult.getScore());
        storedFeedback.forEach(feedback -> {
            assertThat(feedback.getType()).as("type has been set to MANUAL").isEqualTo(feedbackType);
        });
    }

    private void checkAutomaticAssessment(Result storedResult) {
        assertThat(storedResult.isRated() == null || !storedResult.isRated()).as("rated has not been set").isTrue();
        assertThat(storedResult.getScore()).as("score has not been calculated").isNull();
        assertThat(storedResult.getAssessor()).as("assessor has not been set").isNull();
        assertThat(storedResult.getResultString()).as("result string has not been set").isNull();
        assertThat(storedResult.getCompletionDate()).as("completion date has not been set").isNull();
        assertThat(storedResult.getAssessmentType()).as("result type is AUTOMATIC").isEqualTo(AssessmentType.AUTOMATIC);
    }

    private void saveModelingSubmissionAndAssessment() throws Exception {
        modelingSubmission = ModelFactory.generateModelingSubmission(database.loadFileFromResources("test-data/model-submission/model.54727.json"), true);
        modelingSubmission = database.addModelingSubmission(classExercise, modelingSubmission, "student1");
        modelingAssessment = database.addModelingAssessmentForSubmission(classExercise, modelingSubmission, "test-data/model-assessment/assessment.54727.v2.json", "tutor1");
    }
}
