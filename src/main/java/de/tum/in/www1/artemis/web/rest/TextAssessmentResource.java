package de.tum.in.www1.artemis.web.rest;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.repository.TextSubmissionRepository;
import de.tum.in.www1.artemis.service.*;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing TextAssessment.
 */
@RestController
@RequestMapping("/api/text-assessments")
public class TextAssessmentResource extends AssessmentResource {

    private final Logger log = LoggerFactory.getLogger(TextAssessmentResource.class);

    private static final String ENTITY_NAME = "textAssessment";

    private final ParticipationService participationService;
    private final ResultService resultService;
    private final TextAssessmentService textAssessmentService;
    private final TextExerciseService textExerciseService;
    private final TextSubmissionRepository textSubmissionRepository;

    public TextAssessmentResource(AuthorizationCheckService authCheckService,
                                  ParticipationService participationService,
                                  ResultService resultService,
                                  TextAssessmentService textAssessmentService,
                                  TextExerciseService textExerciseService,
                                  TextSubmissionRepository textSubmissionRepository,
                                  UserService userService) {
        super(authCheckService, userService);

        this.participationService = participationService;
        this.resultService = resultService;
        this.textAssessmentService = textAssessmentService;
        this.textExerciseService = textExerciseService;
        this.textSubmissionRepository = textSubmissionRepository;
    }

    @PutMapping("/exercise/{exerciseId}/result/{resultId}")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Result> saveTextAssessment(@PathVariable Long exerciseId, @PathVariable Long resultId, @RequestBody List<Feedback> textAssessments) {
        TextExercise textExercise = textExerciseService.findOne(exerciseId);
        ResponseEntity<Result> responseFailure = checkTextExerciseForRequest(textExercise);
        if (responseFailure != null) return responseFailure;

        Result result = textAssessmentService.saveAssessment(resultId, textAssessments);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/exercise/{exerciseId}/result/{resultId}/submit")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Result> submitTextAssessment(@PathVariable Long exerciseId, @PathVariable Long resultId, @RequestBody List<Feedback> textAssessments) {
        TextExercise textExercise = textExerciseService.findOne(exerciseId);
        ResponseEntity<Result> responseFailure = checkTextExerciseForRequest(textExercise);
        if (responseFailure != null) return responseFailure;

        Result result = textAssessmentService.submitAssessment(resultId, textExercise, textAssessments);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/exercise/{exerciseId}/submission/{submissionId}")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Participation> getDataForTutor(@PathVariable Long exerciseId, @PathVariable Long submissionId) {
        log.debug("REST request to get data for tutors text assessment: {}", exerciseId, submissionId);
        TextExercise textExercise = textExerciseService.findOne(exerciseId);
        ResponseEntity<Participation> responseFailure = checkTextExerciseForRequest(textExercise);
        if (responseFailure != null) return responseFailure;

        Optional<TextSubmission> textSubmission = textSubmissionRepository.findById(submissionId);
        if (!textSubmission.isPresent()) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("textSubmission", "textSubmissionNotFound", "No Submission was found for the given ID.")).body(null);
        }

        Participation participation = textSubmission.get().getParticipation();
        participation  = participationService.findOneWithEagerResultsAndSubmissions(participation.getId());
        if (participation.getResults().isEmpty()) {
            Result result = new Result();
            result.setParticipation(participation);
            result.setSubmission(textSubmission.get());
            resultService.createNewResult(result);
            participation.addResult(result);
        }

        for (Result result : participation.getResults()) {
            List<Feedback> assessments = textAssessmentService.getAssessmentsForResult(result);
            result.setFeedbacks(assessments);
        }

        return ResponseEntity.ok(participation);
    }

    @Override
    String getEntityName() {
        return ENTITY_NAME;
    }

    @Nullable
    private <X> ResponseEntity<X> checkTextExerciseForRequest(TextExercise textExercise) {
        if (textExercise == null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("textExercise", "exerciseNotFound", "No exercise was found for the given ID.")).body(null);
        }
        return checkExercise(textExercise);
    }

}
