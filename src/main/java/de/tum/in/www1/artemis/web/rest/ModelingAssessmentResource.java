package de.tum.in.www1.artemis.web.rest;

import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.forbidden;

import java.util.List;
import java.util.Objects;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.domain.modeling.ModelingSubmission;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.service.*;
import de.tum.in.www1.artemis.service.compass.CompassService;
import de.tum.in.www1.artemis.web.rest.errors.ErrorConstants;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/** REST controller for managing ModelingAssessment. */
@RestController
@RequestMapping("/api")
public class ModelingAssessmentResource extends AssessmentResource {

    private final Logger log = LoggerFactory.getLogger(ModelingAssessmentResource.class);

    private static final String ENTITY_NAME = "modelingAssessment";

    private static final String PUT_SUBMIT_ASSESSMENT_200_REASON = "Given assessment has been saved and used for automatic assessment by Compass";

    private static final String POST_ASSESSMENT_AFTER_COMPLAINT_200_REASON = "Assessment has been updated after complaint";

    private final CompassService compassService;

    private final ModelingExerciseService modelingExerciseService;

    private final AuthorizationCheckService authCheckService;

    private final ModelingSubmissionService modelingSubmissionService;

    private final ExampleSubmissionService exampleSubmissionService;

    public ModelingAssessmentResource(AuthorizationCheckService authCheckService, UserService userService, CompassService compassService,
            ModelingExerciseService modelingExerciseService, AssessmentService assessmentService, ModelingSubmissionService modelingSubmissionService,
            ExampleSubmissionService exampleSubmissionService, WebsocketMessagingService messagingService, ExerciseService exerciseService, ResultRepository resultRepository,
            ExamService examService) {
        super(authCheckService, userService, exerciseService, modelingSubmissionService, assessmentService, resultRepository, examService, messagingService);
        this.compassService = compassService;
        this.modelingExerciseService = modelingExerciseService;
        this.authCheckService = authCheckService;
        this.modelingSubmissionService = modelingSubmissionService;
        this.exampleSubmissionService = exampleSubmissionService;
    }

    /**
     * Get the result of the modeling submission with the given id. See {@link AssessmentResource#getAssessmentBySubmissionId}.
     *
     * @param submissionId the id of the submission that should be sent to the client
     * @return the assessment of the given submission
     */
    @GetMapping("/modeling-submissions/{submissionId}/result")
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Result> getAssessmentBySubmissionId(@PathVariable Long submissionId) {
        return super.getAssessmentBySubmissionId(submissionId);
    }

    /**
     * Retrieve the result for an example submission, only if the user is an instructor or if the example submission is not used for tutorial purposes.
     *
     * @param exerciseId   the id of the exercise
     * @param submissionId the id of the example submission
     * @return the result linked to the example submission
     */
    @GetMapping("/exercise/{exerciseId}/modeling-submissions/{submissionId}/example-assessment")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Result> getExampleAssessment(@PathVariable long exerciseId, @PathVariable long submissionId) {
        log.debug("REST request to get example assessment for tutors text assessment: {}", submissionId);
        ModelingExercise modelingExercise = modelingExerciseService.findOne(exerciseId);
        ExampleSubmission exampleSubmission = exampleSubmissionService.findOneBySubmissionId(submissionId);

        // It is allowed to get the example assessment, if the user is an instructor or
        // if the user is a tutor and the submission is not used for tutorial in the assessment dashboard
        boolean isAllowed = authCheckService.isAtLeastInstructorForExercise(modelingExercise)
                || authCheckService.isAtLeastTeachingAssistantForExercise(modelingExercise) && !exampleSubmission.isUsedForTutorial();
        if (!isAllowed) {
            forbidden();
        }

        return ResponseEntity.ok(assessmentService.getExampleAssessment(submissionId));
    }

    /**
     * PUT modeling-submissions/:submissionId/assessment : save manual modeling assessment. See {@link AssessmentResource#saveAssessment}.
     *
     * @param submissionId id of the submission
     * @param feedbacks list of feedbacks
     * @param submit if true the assessment is submitted, else only saved
     * @return result after saving/submitting modeling assessment
     */
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses({ @ApiResponse(code = 200, message = PUT_SUBMIT_ASSESSMENT_200_REASON, response = Result.class),
            @ApiResponse(code = 403, message = ErrorConstants.REQ_403_REASON), @ApiResponse(code = 404, message = ErrorConstants.REQ_404_REASON) })
    @PutMapping("/modeling-submissions/{submissionId}/assessment")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Result> saveModelingAssessment(@PathVariable long submissionId, @RequestParam(value = "submit", defaultValue = "false") boolean submit,
            @RequestBody List<Feedback> feedbacks) {
        Submission submission = submissionService.findOneWithEagerResultAndFeedback(submissionId);
        ModelingExercise exercise = (ModelingExercise) submission.getParticipation().getExercise();
        ResponseEntity<Result> response = super.saveAssessment(submission, submit, feedbacks);

        if (response.getStatusCode().is2xxSuccessful() && submit && compassService.isSupported(exercise)) {
            compassService.addAssessment(exercise.getId(), submissionId, Objects.requireNonNull(response.getBody()).getFeedbacks());
        }

        return response;
    }

    /**
     * PUT modeling-submissions/:submissionId/example-assessment : save manual example modeling assessment
     *
     * @param submissionId id of the submission
     * @param feedbacks list of feedbacks
     * @return result after saving example modeling assessment
     */
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses({ @ApiResponse(code = 200, message = PUT_SUBMIT_ASSESSMENT_200_REASON, response = Result.class),
            @ApiResponse(code = 403, message = ErrorConstants.REQ_403_REASON), @ApiResponse(code = 404, message = ErrorConstants.REQ_404_REASON) })
    @PutMapping("/modeling-submissions/{submissionId}/example-assessment")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Object> saveModelingExampleAssessment(@PathVariable long submissionId, @RequestBody List<Feedback> feedbacks) {
        User user = userService.getUserWithGroupsAndAuthorities();
        ExampleSubmission exampleSubmission = exampleSubmissionService.findOneWithEagerResult(submissionId);
        ModelingSubmission modelingSubmission = (ModelingSubmission) exampleSubmission.getSubmission();
        ModelingExercise modelingExercise = (ModelingExercise) exampleSubmission.getExercise();
        checkAuthorization(modelingExercise, user);
        Result result = assessmentService.saveManualAssessment(modelingSubmission, feedbacks);
        return ResponseEntity.ok(result);
    }

    /**
     * Update an assessment after a complaint was accepted. After the result is updated accordingly, Compass is notified about the changed assessment in order to adapt all
     * automatic assessments based on this result, as well.
     *
     * @param submissionId     the id of the submission for which the assessment should be updated
     * @param assessmentUpdate the assessment update containing the new feedback items and the response to the complaint
     * @return the updated result
     */
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses({ @ApiResponse(code = 200, message = POST_ASSESSMENT_AFTER_COMPLAINT_200_REASON, response = Result.class),
            @ApiResponse(code = 403, message = ErrorConstants.REQ_403_REASON), @ApiResponse(code = 404, message = ErrorConstants.REQ_404_REASON) })
    @PutMapping("/modeling-submissions/{submissionId}/assessment-after-complaint")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Result> updateModelingAssessmentAfterComplaint(@PathVariable Long submissionId, @RequestBody AssessmentUpdate assessmentUpdate) {
        log.debug("REST request to update the assessment of submission {} after complaint.", submissionId);
        User user = userService.getUserWithGroupsAndAuthorities();
        ModelingSubmission modelingSubmission = modelingSubmissionService.findOneWithEagerResultAndFeedback(submissionId);
        StudentParticipation studentParticipation = (StudentParticipation) modelingSubmission.getParticipation();
        long exerciseId = studentParticipation.getExercise().getId();
        ModelingExercise modelingExercise = modelingExerciseService.findOne(exerciseId);
        checkAuthorization(modelingExercise, user);

        Result result = assessmentService.updateAssessmentAfterComplaint(modelingSubmission.getResult(), modelingExercise, assessmentUpdate);

        if (compassService.isSupported(modelingExercise)) {
            compassService.addAssessment(exerciseId, submissionId, result.getFeedbacks());
        }

        // remove circular dependencies if the results of the participation are there
        if (result.getParticipation() != null && Hibernate.isInitialized(result.getParticipation().getResults()) && result.getParticipation().getResults() != null) {
            result.getParticipation().setResults(null);
        }

        if (result.getParticipation() != null && result.getParticipation() instanceof StudentParticipation
                && !authCheckService.isAtLeastInstructorForExercise(modelingExercise, user)) {
            ((StudentParticipation) result.getParticipation()).setParticipant(null);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Cancel an assessment of a given submission for the current user, i.e. delete the corresponding result / release the lock. Then the submission is available for assessment
     * again.
     *
     * @param submissionId the id of the submission for which the current assessment should be canceled
     * @return 200 Ok response if canceling was successful, 403 Forbidden if current user is not the assessor of the submission
     */
    @PutMapping("/modeling-submissions/{submissionId}/cancel-assessment")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Void> cancelAssessment(@PathVariable Long submissionId) {
        return super.cancelAssessment(submissionId);
    }

    @Override
    String getEntityName() {
        return ENTITY_NAME;
    }
}
