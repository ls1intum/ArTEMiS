package de.tum.in.www1.artemis.service.scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.InitializationState;
import de.tum.in.www1.artemis.domain.enumeration.SubmissionType;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.domain.modeling.ModelingSubmission;
import de.tum.in.www1.artemis.repository.SubmissionRepository;
import de.tum.in.www1.artemis.service.ModelingSubmissionService;
import de.tum.in.www1.artemis.service.ParticipationService;

/*
 * This service handles automatic submission after exercises ended. Currently it is only used for modeling exercises. It manages a hash map with submissions, which stores not
 * submitted submissions. It checks periodically, whether the exercise (the submission belongs to) has ended yet and submits it automatically if that's the case.
 */
@Service
public class AutomaticSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(AutomaticSubmissionService.class);

    // TODO Rework this service to manipulate unsubmitted submission objects for text and modeling exercises in the database directly without the use of hash maps
    // 1) using a cron job, we get all text submissions and modeling submissions from the database with submitted = false
    // 2) we check for each submission whether the corresponding exercise has finished (i.e. due date < now)
    // 3a) if yes, we set the submission to submitted = true (without changing the submission date). We also set submissionType to TIMEOUT
    // 3b) if no, we ignore the submission
    // ==> This will prevent problems when the server is restarted during a modeling / text exercise

    private final ParticipationService participationService;

    private final ModelingSubmissionService modelingSubmissionService;

    private final SubmissionRepository submissionRepository;

    private final SimpMessageSendingOperations messagingTemplate;

    public AutomaticSubmissionService(ParticipationService participationService, ModelingSubmissionService modelingSubmissionService, SubmissionRepository submissionRepository,
            SimpMessageSendingOperations messagingTemplate) {
        this.participationService = participationService;
        this.modelingSubmissionService = modelingSubmissionService;
        this.submissionRepository = submissionRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(cron = "0 0 1 * * *") // execute this every night at 1:00:00 am in the future
    @Transactional
    public void run() {
        // global try-catch for error logging
        try {
            // TODO get unsubmitted text and modeling submissions from SubmissionRepository with submitted = false (probably left join participation left join exercise)
            List<Submission> unsubmittedSubmissions = new ArrayList<>();

            // update Participations if the submission was submitted or if the exercise has ended and save them to Database (DB Write)
            for (Submission unsubmittedSubmission : unsubmittedSubmissions) {

                Exercise exercise = unsubmittedSubmission.getParticipation().getExercise();

                // if exercise has ended, all submissions will be processed => we can remove the inner HashMap for this exercise
                // if exercise hasn't ended, some submissions (those that are not submitted) will stay in HashMap => keep inner HashMap
                Map<String, Submission> submissions;
                if (exercise.isEnded()) {
                    unsubmittedSubmission.setSubmitted(true);
                    unsubmittedSubmission.setType(SubmissionType.TIMEOUT);
                }

                updateParticipation(unsubmittedSubmission);

                submissionRepository.save(unsubmittedSubmission);
                if (unsubmittedSubmission != null) {
                    String username = unsubmittedSubmission.getParticipation().getStudent().getLogin();
                    if (unsubmittedSubmission instanceof ModelingSubmission) {
                        messagingTemplate.convertAndSendToUser(username, "/topic/modelingSubmission/" + unsubmittedSubmission.getId(), unsubmittedSubmission);
                    }
                    if (unsubmittedSubmission instanceof TextSubmission) {
                        messagingTemplate.convertAndSendToUser(username, "/topic/textSubmission/" + unsubmittedSubmission.getId(), unsubmittedSubmission);
                    }
                }
            }

            // TODO add some logs how long this took and how many submissions have been processed
        }
        catch (Exception e) {
            log.error("Exception in AutomaticSubmissionService:\n{}", e.getMessage());
        }
    }

    /**
     * Updates the participation for a given submission. The participation is set to FINISHED. Currently only handles modeling submissions.
     *
     * @param submission the submission for which the participation should be updated for
     * @return submission if updating participation successful, otherwise null
     */
    private Submission updateParticipation(Submission submission) {
        if (submission != null) {
            Participation participation = submission.getParticipation();
            if (participation == null) {
                log.error("The submission {} has no participation.", submission);
                return null;
            }
            Exercise exercise = participation.getExercise();
            if (submission instanceof ModelingSubmission) {
                ModelingExercise modelingExercise = (ModelingExercise) exercise;
                ModelingSubmission modelingSubmission = (ModelingSubmission) submission;
                // notify compass about new submission
                modelingSubmissionService.notifyCompass(modelingSubmission, modelingExercise);
                // check if compass could assess automatically
                modelingSubmissionService.checkAutomaticResult(modelingSubmission, modelingExercise);
                // set participation state to finished and persist it
            }
            participation.setInitializationState(InitializationState.FINISHED);
            participationService.save(participation);
            // return modeling submission with model and optional result
            return submission;
        }
        return null;
    }
}
