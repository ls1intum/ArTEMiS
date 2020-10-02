package de.tum.in.www1.artemis.service;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.eclipse.jgit.lib.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.FeedbackType;
import de.tum.in.www1.artemis.domain.enumeration.SubmissionType;
import de.tum.in.www1.artemis.domain.participation.*;
import de.tum.in.www1.artemis.exception.ContinousIntegrationException;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.service.connectors.ContinuousIntegrationService;
import de.tum.in.www1.artemis.web.rest.errors.EntityNotFoundException;

@Service
public class ProgrammingExerciseGradingService {

    private final Logger log = LoggerFactory.getLogger(ProgrammingExerciseGradingService.class);

    private final Optional<ContinuousIntegrationService> continuousIntegrationService;

    private final ProgrammingExerciseTestCaseService testCaseService;

    private final ProgrammingExerciseService programmingExerciseService;

    private final ProgrammingSubmissionService programmingSubmissionService;

    private final SimpMessageSendingOperations messagingTemplate;

    private final ResultRepository resultRepository;

    private final ParticipationService participationService;

    public ProgrammingExerciseGradingService(ProgrammingExerciseTestCaseService testCaseService, ProgrammingExerciseService programmingExerciseService,
            ProgrammingSubmissionService programmingSubmissionService, ParticipationService participationService, ResultRepository resultRepository,
            Optional<ContinuousIntegrationService> continuousIntegrationService, SimpMessageSendingOperations messagingTemplate) {
        this.testCaseService = testCaseService;
        this.programmingExerciseService = programmingExerciseService;
        this.programmingSubmissionService = programmingSubmissionService;
        this.participationService = participationService;
        this.continuousIntegrationService = continuousIntegrationService;
        this.resultRepository = resultRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Use the given requestBody to extract the relevant information from it. Fetch and attach the result's feedback items to it. For programming exercises the test cases are
     * extracted from the feedbacks & the result is updated with the information from the test cases.
     *
     * @param participation the participation for which the build was finished
     * @param requestBody   RequestBody containing the build result and its feedback items
     * @return result after compilation
     */
    public Optional<Result> processNewProgrammingExerciseResult(@NotNull Participation participation, @NotNull Object requestBody) {
        log.debug("Received new build result (NEW) for participation " + participation.getId());

        if (!(participation instanceof ProgrammingExerciseParticipation)) {
            throw new EntityNotFoundException("Participation with id " + participation.getId() + " is not a programming exercise participation!");
        }

        Result result;
        try {
            result = continuousIntegrationService.get().onBuildCompleted((ProgrammingExerciseParticipation) participation, requestBody);
        }
        catch (ContinousIntegrationException ex) {
            log.error("Result for participation " + participation.getId() + " could not be created due to the following exception: " + ex);
            return Optional.empty();
        }

        if (result != null) {
            ProgrammingExercise programmingExercise = (ProgrammingExercise) participation.getExercise();
            boolean isSolutionParticipation = participation instanceof SolutionProgrammingExerciseParticipation;
            boolean isTemplateParticipation = participation instanceof TemplateProgrammingExerciseParticipation;
            // Find out which test cases were executed and calculate the score according to their status and weight.
            // This needs to be done as some test cases might not have been executed.
            // When the result is from a solution participation , extract the feedback items (= test cases) and store them in our database.
            if (isSolutionParticipation) {
                extractTestCasesFromResult(programmingExercise, result);
            }
            result = updateResult(result, programmingExercise, !isSolutionParticipation && !isTemplateParticipation);
            result = resultRepository.save(result);
            // workaround to prevent that result.submission suddenly turns into a proxy and cannot be used any more later after returning this method

            // If the solution participation was updated, also trigger the template participation build.
            if (isSolutionParticipation) {
                // This method will return without triggering the build if the submission is not of type TEST.
                triggerTemplateBuildIfTestCasesChanged(programmingExercise.getId(), result.getId());
            }
        }
        return Optional.ofNullable(result);
    }

    /**
     * Trigger the build of the template repository, if the submission of the provided result is of type TEST.
     * Will use the commitHash of the submission for triggering the template build.
     *
     * If the submission of the provided result is not of type TEST, the method will return without triggering the build.
     *
     * @param programmingExerciseId ProgrammingExercise id that belongs to the result.
     * @param resultId              Result id.
     */
    private void triggerTemplateBuildIfTestCasesChanged(long programmingExerciseId, long resultId) {
        ProgrammingSubmission submission;
        try {
            submission = programmingSubmissionService.findByResultId(resultId);
        }
        catch (EntityNotFoundException ex) {
            // This is an unlikely error that would mean that no submission could be created for the result. In this case we can only log and abort.
            log.error("Could not trigger the build of the template repository for the programming exercise id " + programmingExerciseId
                    + " because no submission could be found for the provided result id " + resultId);
            return;
        }
        // We only trigger the template build when the test repository was changed.
        if (!submission.getType().equals(SubmissionType.TEST)) {
            return;
        }
        // We use the last commitHash of the test repository.
        ObjectId testCommitHash = ObjectId.fromString(submission.getCommitHash());
        try {
            programmingSubmissionService.triggerTemplateBuildAndNotifyUser(programmingExerciseId, testCommitHash, SubmissionType.TEST);
        }
        catch (EntityNotFoundException ex) {
            // If for some reason the programming exercise does not have a template participation, we can only log and abort.
            log.error("Could not trigger the build of the template repository for the programming exercise id " + programmingExerciseId
                    + " because no template participation could be found for the given exercise");
        }
    }

    /**
     * Generates test cases from the given result's feedbacks & notifies the subscribing users about the test cases if they have changed. Has the side effect of sending a message
     * through the websocket!
     *
     * @param exercise the programming exercise for which the test cases should be extracted from the new result
     * @param result   from which to extract the test cases.
     */
    private void extractTestCasesFromResult(ProgrammingExercise exercise, Result result) {
        boolean haveTestCasesChanged = testCaseService.generateTestCasesFromFeedbacks(result.getFeedbacks(), exercise);
        if (haveTestCasesChanged) {
            // Notify the client about the updated testCases
            Set<ProgrammingExerciseTestCase> testCases = testCaseService.findByExerciseId(exercise.getId());
            messagingTemplate.convertAndSend("/topic/programming-exercise/" + exercise.getId() + "/test-cases", testCases);
        }
    }

    /**
     * Updates an incoming result with the information of the exercises test cases. This update includes:
     * - Checking which test cases were not executed as this is not part of the bamboo build (not all test cases are executed in an exercise with sequential test runs)
     * - Checking the due date and the afterDueDate flag
     * - Recalculating the score based based on the successful test cases weight vs the total weight of all test cases.
     *
     * If there are no test cases stored in the database for the given exercise (i.e. we have a legacy exercise) or the weight has not been changed, then the result will not change
     *
     * @param result   to modify with new score, result string & added feedbacks (not executed tests)
     * @param exercise the result belongs to.
     * @param isStudentParticipation boolean flag indicating weather the participation of the result is not a solution/template participation.
     * @return Result with updated feedbacks, score and result string.
     */
    public Result updateResult(Result result, ProgrammingExercise exercise, boolean isStudentParticipation) {
        Set<ProgrammingExerciseTestCase> testCases = testCaseService.findActiveByExerciseId(exercise.getId());
        Set<ProgrammingExerciseTestCase> testCasesForCurrentDate = testCases;
        // We don't filter the test cases for the solution/template participation's results as they are used as indicators for the instructor!
        if (isStudentParticipation) {
            testCasesForCurrentDate = filterTestCasesForCurrentDate(exercise, testCases);
        }
        return updateResult(testCases, testCasesForCurrentDate, result, exercise);
    }

    /**
     * Updates <b>all</b> latest automatic results of the given exercise with the information of the exercises test cases. This update includes:
     * - Checking which test cases were not executed as this is not part of the bamboo build (not all test cases are executed in an exercise with sequential test runs)
     * - Checking the due date and the afterDueDate flag
     * - Recalculating the score based based on the successful test cases weight vs the total weight of all test cases.
     *
     * If there are no test cases stored in the database for the given exercise (i.e. we have a legacy exercise) or the weight has not been changed, then the result will not change
     *
     * @param exercise the exercise whose results should be updated
     * @return the results of the exercise that have been updated
     */
    public List<Result> updateAllResults(ProgrammingExercise exercise) {
        Set<ProgrammingExerciseTestCase> testCases = testCaseService.findActiveByExerciseId(exercise.getId());

        ArrayList<Result> updatedResults = new ArrayList<>();

        Result templateResult = exercise.getTemplateParticipation().findLatestResult();
        Result solutionResult = exercise.getSolutionParticipation().findLatestResult();
        // template and solution are always updated using ALL test cases
        if (templateResult != null) {
            updateResult(testCases, testCases, templateResult, exercise);
            updatedResults.add(templateResult);
        }
        if (solutionResult != null) {
            updateResult(testCases, testCases, solutionResult, exercise);
            updatedResults.add(solutionResult);
        }
        // filter the test cases for the student results if necessary
        Set<ProgrammingExerciseTestCase> testCasesForCurrentDate = filterTestCasesForCurrentDate(exercise, testCases);
        // We only update the latest automatic results here, later manual assessments are not affected
        List<StudentParticipation> participations = participationService.findByExerciseIdWithLatestAutomaticResultAndFeedbacks(exercise.getId());

        for (StudentParticipation studentParticipation : participations) {
            Result result = studentParticipation.findLatestResult();
            if (result != null) {
                updateResult(testCases, testCasesForCurrentDate, result, exercise);
                updatedResults.add(result);
            }
        }
        return updatedResults;
    }

    private Set<ProgrammingExerciseTestCase> filterTestCasesForCurrentDate(ProgrammingExercise exercise, Set<ProgrammingExerciseTestCase> testCases) {
        boolean shouldTestsWithAfterDueDateFlagBeRemoved = exercise.getBuildAndTestStudentSubmissionsAfterDueDate() != null
                && ZonedDateTime.now().isBefore(exercise.getBuildAndTestStudentSubmissionsAfterDueDate());
        // Filter all test cases from the score calculation that are only executed after due date if the due date has not yet passed.
        return testCases.stream().filter(testCase -> !shouldTestsWithAfterDueDateFlagBeRemoved || !testCase.isAfterDueDate()).collect(Collectors.toSet());
    }

    /**
     * TODO: For now we are only concerned with not breaking existing functionality and not losing static code analysis feedback.
     * This method has to be extended/refactored when a grading concept for static code analysis has been created
     */
    private Result updateResult(Set<ProgrammingExerciseTestCase> testCases, Set<ProgrammingExerciseTestCase> testCasesForCurrentDate, @NotNull Result result,
            ProgrammingExercise exercise) {

        // Distinguish between static code analysis feedback and test case feedback
        List<Feedback> testCaseFeedback = new ArrayList<>();
        List<Feedback> staticCodeAnalysisFeedback = new ArrayList<>();
        for (var feedback : result.getFeedbacks()) {
            if (feedback.isStaticCodeAnalysisFeedback()) {
                staticCodeAnalysisFeedback.add(feedback);
            }
            else {
                testCaseFeedback.add(feedback);
            }
        }

        // Case 1: There are tests and test case feedback, find out which tests were not executed or should only count to the score after the due date.
        if (testCasesForCurrentDate.size() > 0 && testCaseFeedback.size() > 0 && result.getFeedbacks().size() > 0) {
            // Remove feedbacks that the student should not see yet because of the due date.
            removeFeedbacksForAfterDueDateTests(result, testCasesForCurrentDate);

            Set<ProgrammingExerciseTestCase> successfulTestCases = testCasesForCurrentDate.stream().filter(isSuccessful(result)).collect(Collectors.toSet());

            // Add feedbacks for tests that were not executed ("test was not executed").
            createFeedbackForNotExecutedTests(result, testCasesForCurrentDate);

            // Recalculate the achieved score by including the test cases individual weight.
            // The score is always calculated from ALL test cases, regardless of the current date!
            updateScore(result, successfulTestCases, testCases, exercise);

            // Create a new result string that reflects passed, failed & not executed test cases.
            updateResultString(result, successfulTestCases, testCasesForCurrentDate);
        }
        // Case 2: There are no test cases that are executed before the due date has passed. We need to do this to differentiate this case from a build error.
        else if (testCases.size() > 0 && result.getFeedbacks().size() > 0 && testCaseFeedback.size() > 0) {
            removeAllTestCaseFeedbackAndSetScoreToZero(result, staticCodeAnalysisFeedback);
        }
        // Case 3: If there is no test case feedback, the build has failed or it has previously fallen under case 2. In this case we just return the original result without
        // changing it.
        return result;
    }

    /**
     * Check which tests were not executed and add a new Feedback for them to the exercise.
     * @param result of the build run.
     * @param allTests of the given programming exercise.
     */
    private void createFeedbackForNotExecutedTests(Result result, Set<ProgrammingExerciseTestCase> allTests) {
        List<Feedback> feedbacksForNotExecutedTestCases = allTests.stream().filter(wasNotExecuted(result))
                .map(testCase -> new Feedback().type(FeedbackType.AUTOMATIC).text(testCase.getTestName()).detailText("Test was not executed.")).collect(Collectors.toList());
        result.addFeedbacks(feedbacksForNotExecutedTestCases);
    }

    /**
     * Check which tests were executed but which result should not be made public to the student yet.
     * @param result of the build run.
     * @param testCasesForCurrentDate of the given programming exercise.
     */
    private void removeFeedbacksForAfterDueDateTests(Result result, Set<ProgrammingExerciseTestCase> testCasesForCurrentDate) {
        // Find feedback which is not associated with test cases for the current date. Does not remove static code analysis feedback
        List<Feedback> feedbacksToFilterForCurrentDate = result.getFeedbacks().stream().filter(
                feedback -> !feedback.isStaticCodeAnalysisFeedback() && testCasesForCurrentDate.stream().noneMatch(testCase -> testCase.getTestName().equals(feedback.getText())))
                .collect(Collectors.toList());
        feedbacksToFilterForCurrentDate.forEach(result::removeFeedback);
        // If there are no feedbacks left after filtering those not valid for the current date, also setHasFeedback to false.
        if (result.getFeedbacks().stream().noneMatch(feedback -> Boolean.FALSE.equals(feedback.isPositive())
                || feedback.getType() != null && (feedback.getType().equals(FeedbackType.MANUAL) || feedback.getType().equals(FeedbackType.MANUAL_UNREFERENCED))))
            result.setHasFeedback(false);
    }

    /**
     * Update the score given the positive tests score divided by all tests's score.
     * Takes weight, bonus multiplier and absolute bonus points into account
     *
     * @param result of the build run.
     * @param successfulTestCases test cases with positive feedback.
     * @param allTests of a given programming exercise.
     */
    private void updateScore(Result result, Set<ProgrammingExerciseTestCase> successfulTestCases, Set<ProgrammingExerciseTestCase> allTests,
            ProgrammingExercise programmingExercise) {
        if (successfulTestCases.size() > 0) {

            double weightSum = allTests.stream().mapToDouble(ProgrammingExerciseTestCase::getWeight).sum();
            double successfulTestPoints = successfulTestCases.stream().mapToDouble(test -> {
                double testWeight = test.getWeight() * test.getBonusMultiplier();
                double testPoints = testWeight / weightSum * programmingExercise.getMaxScore();
                double testPointsWithBonus = testPoints + test.getBonusPoints();
                // update credits of related feedback
                result.getFeedbacks().stream().filter(fb -> fb.getText().equals(test.getTestName())).findFirst().ifPresent(feedback -> feedback.setCredits(testPointsWithBonus));
                return testPointsWithBonus;
            }).sum();
            double maxPoints = programmingExercise.getMaxScore() + Optional.ofNullable(programmingExercise.getBonusPoints()).orElse(0.0);
            // The points are capped by the maximum achievable points
            if (successfulTestPoints > maxPoints) {
                successfulTestPoints = maxPoints;
            }
            // The score is calculated as a percentage of the maximum points
            long score = (long) (successfulTestPoints / programmingExercise.getMaxScore() * 100.0);

            result.setScore(score);
        }
        else {
            result.setScore(0L);
        }
    }

    /**
     * Update the result's result string given the successful tests vs. all tests (x of y passed).
     * @param result of the build run.
     * @param successfulTestCases test cases with positive feedback.
     * @param allTests of the given programming exercise.
     */
    private void updateResultString(Result result, Set<ProgrammingExerciseTestCase> successfulTestCases, Set<ProgrammingExerciseTestCase> allTests) {
        // Create a new result string that reflects passed, failed & not executed test cases.
        String newResultString = successfulTestCases.size() + " of " + allTests.size() + " passed";
        result.setResultString(newResultString);
    }

    /**
     * Remove all test case feedback information from a result and treat it as if it has a score of 0.
     * @param result Result containing all feedback
     * @param staticCodeAnalysisFeedback Static code analysis feedback to keep
     */
    private void removeAllTestCaseFeedbackAndSetScoreToZero(Result result, List<Feedback> staticCodeAnalysisFeedback) {
        result.setFeedbacks(staticCodeAnalysisFeedback);
        result.hasFeedback(staticCodeAnalysisFeedback.size() > 0);
        result.setScore(0L);
        result.setResultString("0 of 0 passed");
    }

    /**
     * Check if the provided test was found in the result's feedbacks with positive = true.
     * @param result of the build run.
     * @return true if there is a positive feedback for a given test.
     */
    private Predicate<ProgrammingExerciseTestCase> isSuccessful(Result result) {
        return testCase -> result.getFeedbacks().stream()
                .anyMatch(feedback -> feedback.getText() != null && feedback.getText().equals(testCase.getTestName()) && Boolean.TRUE.equals(feedback.isPositive()));
    }

    /**
     * Check if the provided test was not found in the result's feedbacks.
     * @param result of the build run.
     * @return true if there is no feedback for a given test.
     */
    private Predicate<ProgrammingExerciseTestCase> wasNotExecuted(Result result) {
        return testCase -> result.getFeedbacks().stream().noneMatch(feedback -> feedback.getText().equals(testCase.getTestName()));
    }

}
