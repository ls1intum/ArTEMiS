package de.tum.in.www1.artemis.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.repository.JsonAssessmentRepository;
import de.tum.in.www1.artemis.repository.ParticipationRepository;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.service.*;
import de.tum.in.www1.artemis.service.compass.CompassService;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;
import de.tum.in.www1.artemis.web.rest.util.ResponseUtil;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Set;

import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.forbidden;

/**
 * REST controller for managing ModelingAssessment.
 */
@RestController
@RequestMapping("/api")
public class ModelingAssessmentResource {
    private final Logger log = LoggerFactory.getLogger(ModelingAssessmentResource.class);

    private static final String ENTITY_NAME = "modelingAssessment";

    private final JsonAssessmentRepository jsonAssessmentRepository;
    private final ResultRepository resultRepository;
    private final ParticipationRepository participationRepository;
    private final CompassService compassService;
    private final ModelingExerciseService modelingExerciseService;
    private final UserService userService;
    private final AuthorizationCheckService authCheckService;
    private final CourseService courseService;
    private final ModelingAssessmentService modelingAssessmentService;

    public ModelingAssessmentResource(JsonAssessmentRepository jsonAssessmentRepository, ResultRepository resultRepository, ParticipationRepository participationRepository, CompassService compassService, ModelingExerciseService modelingExerciseService, UserService userService, AuthorizationCheckService authCheckService, CourseService courseService, ModelingAssessmentService modelingAssessmentService) {
        this.jsonAssessmentRepository = jsonAssessmentRepository;
        this.resultRepository = resultRepository;
        this.participationRepository = participationRepository;
        this.compassService = compassService;
        this.modelingExerciseService = modelingExerciseService;
        this.userService = userService;
        this.authCheckService = authCheckService;
        this.courseService = courseService;
        this.modelingAssessmentService = modelingAssessmentService;
    }

    //TODO: all API path in this class do not really make sense, we should restructure them and potentially start with /exercise/

    @DeleteMapping("/modeling-assessments/exercise/{exerciseId}/optimal-model-submissions")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    @Timed
    public ResponseEntity<String> resetOptimalModels(@PathVariable Long exerciseId) {
        ModelingExercise modelingExercise = modelingExerciseService.findOne(exerciseId);
        if (modelingExercise == null) {
            return ResponseUtil.notFound();
        }

        ResponseEntity responseFailure = checkModelingExercise(modelingExercise);
        if (responseFailure != null) return responseFailure;

        compassService.resetModelsWaitingForAssessment(exerciseId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/modeling-assessments/exercise/{exerciseId}/optimal-model-submissions")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    @Timed
    public ResponseEntity<String> getNextOptimalModelSubmissions(@PathVariable Long exerciseId) {
        ModelingExercise modelingExercise = modelingExerciseService.findOne(exerciseId);

        ResponseEntity responseFailure = checkModelingExercise(modelingExercise);
        if (responseFailure != null) return responseFailure;

        //TODO: we need to make sure that per participation there is only one optimalModel
        Set<Long> optimalModelSubmissions = compassService.getModelsWaitingForAssessment(exerciseId);
        JsonArray response = new JsonArray();
        for (Long optimalModelSubmissionId : optimalModelSubmissions) {
            JsonObject entry = new JsonObject();
            response.add(entry);
            entry.addProperty("id", optimalModelSubmissionId);
        }
        return ResponseEntity.ok(response.toString());
    }

    @GetMapping("/modeling-assessments/exercise/{exerciseId}/submission/{submissionId}/partial-assessment")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    @Timed
    public ResponseEntity<String> getPartialAssessment(@PathVariable Long exerciseId, @PathVariable Long submissionId) {
        ModelingExercise modelingExercise = modelingExerciseService.findOne(exerciseId);

        ResponseEntity responseFailure = checkModelingExercise(modelingExercise);
        if (responseFailure != null) return responseFailure;

        JsonObject partialAssessment = compassService.getPartialAssessment(exerciseId, submissionId);
        return ResponseEntity.ok(partialAssessment.get("assessments").toString());
    }

    /**
     * Returns assessments (if found) for a given participationId and submissionId.
     *
     * @param participationId   the participationId for which to find assessments for
     * @param submissionId      the submissionId for which to find assessments for
     * @return the ResponseEntity with assessments string as body
     */
    @GetMapping("/modeling-assessments/participation/{participationId}/submission/{submissionId}")
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    @Timed
    public ResponseEntity<String> getAssessmentBySubmissionId(@PathVariable Long participationId, @PathVariable Long submissionId) {
        Optional<Participation> optionalParticipation = participationRepository.findById(participationId);
        if (!optionalParticipation.isPresent()) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(ENTITY_NAME, "participationNotFound", "No participation was found for the given ID.")).body(null);
        }

        Participation participation = optionalParticipation.get();

        if (!courseService.userHasAtLeastStudentPermissions(participation.getExercise().getCourse()) || !authCheckService.isOwnerOfParticipation(participation)) {
            return forbidden();
        }

        Long exerciseId = participation.getExercise().getId();
        Long studentId = participation.getStudent().getId();
        if (jsonAssessmentRepository.exists(exerciseId, studentId, submissionId, true)) {
            JsonObject assessmentJson = jsonAssessmentRepository.readAssessment(exerciseId, studentId, submissionId, true);
            return ResponseEntity.ok(assessmentJson.get("assessments").toString());
        }
        if (jsonAssessmentRepository.exists(exerciseId, studentId, submissionId, false)) {
            JsonObject assessmentJson = jsonAssessmentRepository.readAssessment(exerciseId, studentId, submissionId, false);
            return ResponseEntity.ok(assessmentJson.get("assessments").toString());
        }
        return ResponseEntity.ok("");
    }

    /**
     * Saves assessments and updates result.
     *
     * @param exerciseId            the exerciseId the assessment belongs to
     * @param resultId              the resultId the assessment belongs to
     * @param modelingAssessment    the assessments as string
     * @return the ResponseEntity with result as body
     */
    @PutMapping("/modeling-assessments/exercise/{exerciseId}/result/{resultId}")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    @Timed
    public ResponseEntity<Result> saveModelingAssessment(@PathVariable Long exerciseId, @PathVariable Long resultId, @RequestBody String modelingAssessment) {
        ModelingExercise modelingExercise = modelingExerciseService.findOne(exerciseId);
        if (modelingExercise == null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("modelingExercise", "exerciseNotFound", "No exercise was found for the given ID.")).body(null);
        }
        ResponseEntity responseFailure = checkModelingExercise(modelingExercise);
        if (responseFailure != null) return responseFailure;

        Result result = modelingAssessmentService.saveManualAssessment(resultId, exerciseId, modelingAssessment);

        return ResponseEntity.ok(result);
    }

    /**
     * Saves assessments and updates result. Sets result to rated so the student can see the assessments.
     *
     * @param exerciseId            the exerciseId the assessment belongs to
     * @param resultId              the resultId the assessment belongs to
     * @param modelingAssessment    the assessments as string
     * @return the ResponseEntity with result as body
     */
    @PutMapping("/modeling-assessments/exercise/{exerciseId}/result/{resultId}/submit")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    @Timed
    public ResponseEntity<Result> submitModelingAssessment(@PathVariable Long exerciseId, @PathVariable Long resultId, @RequestBody String modelingAssessment) {
        ModelingExercise modelingExercise = modelingExerciseService.findOne(exerciseId);

        ResponseEntity responseFailure = checkModelingExercise(modelingExercise);
        if (responseFailure != null) return responseFailure;

        Result result = modelingAssessmentService.submitManualAssessment(resultId, exerciseId, modelingAssessment);
        Long submissionId = result.getSubmission().getId();
        // add assessment to compass to include it in the automatic grading process
        compassService.addAssessment(exerciseId, submissionId, modelingAssessment);

        return ResponseEntity.ok(result);
    }

    @Nullable
    private <X> ResponseEntity<X> checkModelingExercise(ModelingExercise modelingExercise) {
        Course course = modelingExercise.getCourse();
        if (course == null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(ENTITY_NAME, "courseNotFound", "The course belonging to this modeling exercise does not exist")).body(null);
        }
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authCheckService.isTeachingAssistantInCourse(course, user) &&
            !authCheckService.isInstructorInCourse(course, user) &&
            !authCheckService.isAdmin()) {
            return forbidden();
        }
        return null;
    }
}
