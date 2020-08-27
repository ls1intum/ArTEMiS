package de.tum.in.www1.artemis.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import de.tum.in.www1.artemis.AbstractSpringIntegrationBambooBitbucketJiraTest;
import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.DiagramType;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.domain.modeling.ModelingSubmission;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.repository.CourseRepository;
import de.tum.in.www1.artemis.repository.ExerciseRepository;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.util.DatabaseUtilService;
import de.tum.in.www1.artemis.util.ModelFactory;
import de.tum.in.www1.artemis.util.RequestUtilService;

public class AssessmentServiceTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    @Autowired
    RequestUtilService request;

    @Autowired
    DatabaseUtilService database;

    @Autowired
    ExerciseRepository exerciseRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    ResultRepository resultRepository;

    @Autowired
    AssessmentService assessmentService;

    ZonedDateTime pastTimestamp = ZonedDateTime.now().minusDays(5);

    ZonedDateTime futureTimestamp = ZonedDateTime.now().plusDays(5);

    ZonedDateTime futureFutureTimestamp = ZonedDateTime.now().plusDays(8);

    Course course1 = new Course();

    @AfterEach
    public void tearDown() {
        database.resetDatabase();
    }

    @BeforeEach
    public void init() {
        database.addUsers(2, 2, 1);
        course1 = ModelFactory.generateCourse(null, pastTimestamp, futureTimestamp, new HashSet<>(), "tumuser", "tutor", "instructor");
        course1.setRegistrationEnabled(true);
        courseRepository.save(course1);
    }

    public TextExercise createTextExerciseWithSGI(Course course) {
        TextExercise textExercise = ModelFactory.generateTextExercise(pastTimestamp, futureTimestamp, futureFutureTimestamp, course);
        textExercise.setMaxScore(7.0);
        database.addGradingInstructionsToExercise(textExercise);
        exerciseRepository.save(textExercise);
        textExercise.getCategories().add("Text");
        course.addExercises(textExercise);
        return textExercise;
    }

    public ModelingExercise createModelingExerciseWithSGI(Course course) {
        ModelingExercise modelingExercise = ModelFactory.generateModelingExercise(pastTimestamp, futureTimestamp, futureFutureTimestamp, DiagramType.ClassDiagram, course);
        modelingExercise.setMaxScore(7.0);
        database.addGradingInstructionsToExercise(modelingExercise);
        exerciseRepository.save(modelingExercise);
        modelingExercise.getCategories().add("Modeling");
        course.addExercises(modelingExercise);
        return modelingExercise;
    }

    public FileUploadExercise createFileuploadExerciseWithSGI(Course course) {
        FileUploadExercise fileUploadExercise = ModelFactory.generateFileUploadExercise(pastTimestamp, futureTimestamp, futureFutureTimestamp, "png", course);
        fileUploadExercise.setMaxScore(7.0);
        database.addGradingInstructionsToExercise(fileUploadExercise);
        fileUploadExercise.getCategories().add("File");
        exerciseRepository.save(fileUploadExercise);
        course.addExercises(fileUploadExercise);
        return fileUploadExercise;
    }

    public List<Feedback> createFeedback(Exercise exercise) {

        var gradingInstructionNoLimit = exercise.getGradingCriteria().get(0).getStructuredGradingInstructions().get(0);
        var gradingInstructionLimited = exercise.getGradingCriteria().get(1).getStructuredGradingInstructions().get(0);
        var gradingInstructionBigLimit = exercise.getGradingCriteria().get(2).getStructuredGradingInstructions().get(0);

        var feedbacks = new ArrayList<Feedback>();
        var feedbackAppliedSGINoLimit = new Feedback();
        var feedbackAppliedSGINoLimit2 = new Feedback();
        var feedbackAppliedSGILimited = new Feedback();
        var feedbackAppliedSGILimited2 = new Feedback();
        var feedbackAppliedSGIBigLimit = new Feedback();
        var feedbackAppliedSGIBigLimit2 = new Feedback();
        var feedbackNoSGI = new Feedback();

        feedbackAppliedSGIBigLimit.setGradingInstruction(gradingInstructionBigLimit);
        feedbackAppliedSGIBigLimit.setCredits(gradingInstructionBigLimit.getCredits());

        feedbackAppliedSGIBigLimit2.setGradingInstruction(gradingInstructionBigLimit);
        feedbackAppliedSGIBigLimit2.setCredits(gradingInstructionBigLimit.getCredits());

        feedbackAppliedSGINoLimit.setGradingInstruction(gradingInstructionNoLimit);
        feedbackAppliedSGINoLimit.setCredits(gradingInstructionNoLimit.getCredits());

        feedbackAppliedSGILimited.setGradingInstruction(gradingInstructionLimited);
        feedbackAppliedSGILimited.setCredits(gradingInstructionLimited.getCredits());

        feedbackAppliedSGINoLimit2.setGradingInstruction(gradingInstructionNoLimit);
        feedbackAppliedSGINoLimit2.setCredits(gradingInstructionNoLimit.getCredits());

        feedbackAppliedSGILimited2.setGradingInstruction(gradingInstructionLimited);
        feedbackAppliedSGILimited2.setCredits(gradingInstructionLimited.getCredits());

        feedbackNoSGI.setCredits(1.0);
        feedbackNoSGI.setDetailText("This is a free text feedback");

        feedbacks.add(feedbackAppliedSGIBigLimit); // +1P
        feedbacks.add(feedbackAppliedSGIBigLimit2); // +1P limited but we did not pass the limit yet so point will be counted
        feedbacks.add(feedbackAppliedSGINoLimit); // +1P
        feedbacks.add(feedbackAppliedSGILimited); // +1P
        feedbacks.add(feedbackAppliedSGINoLimit2); // +1P
        feedbacks.add(feedbackAppliedSGILimited2); // +1P will not be counted, we passed the limit!!!
        feedbacks.add(feedbackNoSGI); // +1P
        return feedbacks; // so total score is 6P
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void createFileUploadSubmissionAndCalculateScore() {
        FileUploadExercise exercise = createFileuploadExerciseWithSGI(course1);
        Submission submissionWithoutResult = new FileUploadSubmission();
        submissionWithoutResult.setSubmissionDate(pastTimestamp.plusMinutes(3L));
        submissionWithoutResult = database.addSubmission(exercise, submissionWithoutResult, "student1");
        database.addSubmission((StudentParticipation) submissionWithoutResult.getParticipation(), submissionWithoutResult);

        List<Feedback> feedbacks = createFeedback(exercise);
        var result = new Result();
        result.setSubmission(submissionWithoutResult);
        result.setFeedbacks(feedbacks);
        submissionWithoutResult.setResult(result);

        var calculatedScore = assessmentService.calculateTotalScore(feedbacks);
        assessmentService.submitResult(result, exercise, calculatedScore);
        resultRepository.save(result);

        assertThat(result.getResultString()).isEqualTo("6 of 7 points");
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void createTextExerciseSubmissionAndCalculateScore() {
        TextExercise exercise = createTextExerciseWithSGI(course1);
        Submission submissionWithoutResult = new TextSubmission();
        submissionWithoutResult.setSubmissionDate(pastTimestamp.plusMinutes(3L));
        submissionWithoutResult = database.addSubmission(exercise, submissionWithoutResult, "student1");
        database.addSubmission((StudentParticipation) submissionWithoutResult.getParticipation(), submissionWithoutResult);

        List<Feedback> feedbacks = createFeedback(exercise);
        var result = new Result();
        result.setSubmission(submissionWithoutResult);
        result.setFeedbacks(feedbacks);
        submissionWithoutResult.setResult(result);

        var calculatedScore = assessmentService.calculateTotalScore(feedbacks);
        assessmentService.submitResult(result, exercise, calculatedScore);
        resultRepository.save(result);

        assertThat(result.getResultString()).isEqualTo("6 of 7 points");
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void createModelingExerciseSubmissionAndCalculateScore() {
        ModelingExercise exercise = createModelingExerciseWithSGI(course1);
        Submission submissionWithoutResult = new ModelingSubmission();
        submissionWithoutResult.setSubmissionDate(pastTimestamp.plusMinutes(3L));
        submissionWithoutResult = database.addSubmission(exercise, submissionWithoutResult, "student1");
        database.addSubmission((StudentParticipation) submissionWithoutResult.getParticipation(), submissionWithoutResult);

        List<Feedback> feedbacks = createFeedback(exercise);
        var result = new Result();
        result.setSubmission(submissionWithoutResult);
        result.setFeedbacks(feedbacks);
        submissionWithoutResult.setResult(result);

        var calculatedScore = assessmentService.calculateTotalScore(feedbacks);
        assessmentService.submitResult(result, exercise, calculatedScore);
        resultRepository.save(result);

        assertThat(result.getResultString()).isEqualTo("6 of 7 points");
    }
}
