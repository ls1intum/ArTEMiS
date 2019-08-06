package de.tum.in.www1.artemis.web.rest;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.ProgrammingSubmission;
import de.tum.in.www1.artemis.security.SecurityUtils;
import de.tum.in.www1.artemis.service.ExerciseService;
import de.tum.in.www1.artemis.service.ProgrammingExerciseService;
import de.tum.in.www1.artemis.service.ProgrammingSubmissionService;

/**
 * REST controller for managing ProgrammingSubmission.
 */
@RestController
@RequestMapping("/api")
public class ProgrammingSubmissionResource {

    private final Logger log = LoggerFactory.getLogger(ProgrammingSubmissionResource.class);

    private static final String ENTITY_NAME = "programmingSubmission";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ProgrammingSubmissionService programmingSubmissionService;

    private final ExerciseService exerciseService;

    private final ProgrammingExerciseService programmingExerciseService;

    private final SimpMessageSendingOperations messagingTemplate;

    public ProgrammingSubmissionResource(ProgrammingSubmissionService programmingSubmissionService, ExerciseService exerciseService,
            ProgrammingExerciseService programmingExerciseService, SimpMessageSendingOperations messagingTemplate) {
        this.programmingSubmissionService = programmingSubmissionService;
        this.exerciseService = exerciseService;
        this.programmingExerciseService = programmingExerciseService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * POST /programming-submissions/:participationId : Notify the application about a new push to the VCS for the participation with Id participationId This API is invoked by the
     * VCS Server at the push of a new commit
     *
     * @param participationId the participationId of the participation the repository is linked to
     * @return the ResponseEntity with status 200 (OK), or with status 400 (Bad Request) if the latest commit was already notified about
     */
    @PostMapping(value = Constants.PROGRAMMING_SUBMISSION_RESOURCE_PATH + "{participationId}")
    public ResponseEntity<?> notifyPush(@PathVariable("participationId") Long participationId, @RequestBody Object requestBody) {

        log.info("REST request to inform about new commit+push for participation: {}", participationId);
        ProgrammingSubmission submission = programmingSubmissionService.notifyPush(participationId, requestBody);

        // Remove unnecessary information from the new submission.
        submission.getParticipation().setExercise(null);
        submission.getParticipation().setSubmissions(null);
        // notify user via websocket.
        messagingTemplate.convertAndSend("/topic/participation/" + submission.getParticipation().getId() + "/newSubmission", submission);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * POST /programming-exercises/test-cases-changed/:exerciseId : informs Artemis about changed test cases for the "id" programmingExercise. e
     * 
     * @param exerciseId the id of the programmingExercise where the test cases got changed
     * @return the ResponseEntity with status 200 (OK)
     */
    @PostMapping(Constants.TEST_CASE_CHANGED_PATH + "{exerciseId}")
    public ResponseEntity<Void> testCaseChanged(@PathVariable Long exerciseId, @RequestBody Object requestBody) {
        log.info("REST request to inform about changed test cases of ProgrammingExercise : {}", exerciseId);
        // This is needed as a request using a custom query is made using the ExerciseRepository, but the user is not authenticated
        // as the VCS-server performs the request
        SecurityUtils.setAuthorizationObject();

        Exercise exercise = exerciseService.findOneLoadParticipations(exerciseId);

        if (!(exercise instanceof ProgrammingExercise)) {
            log.warn("REST request to inform about changed test cases of non existing ProgrammingExercise : {}", exerciseId);
            return ResponseEntity.notFound().build();
        }

        ProgrammingExercise programmingExercise = (ProgrammingExercise) exercise;
        List<ProgrammingSubmission> submissions = programmingExerciseService.notifyChangedTestCases(programmingExercise, requestBody);

        // notify users via websocket.
        for (ProgrammingSubmission submission : submissions) {
            messagingTemplate.convertAndSend("/topic/participation/" + submission.getParticipation().getId() + "/newSubmission", submission);
        }

        return ResponseEntity.ok().build();
    }
}
