package de.tum.in.www1.artemis.service;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.InitializationState;
import de.tum.in.www1.artemis.domain.enumeration.SubmissionType;
import de.tum.in.www1.artemis.repository.ParticipationRepository;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.repository.TextSubmissionRepository;
import de.tum.in.www1.artemis.service.scheduled.AutomaticSubmissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class TextSubmissionService {

    private final TextSubmissionRepository textSubmissionRepository;
    private final ParticipationRepository participationRepository;
    private final ParticipationService participationService;
    private final ResultRepository resultRepository;

    public TextSubmissionService(TextSubmissionRepository textSubmissionRepository,
                                 ParticipationRepository participationRepository,
                                 ParticipationService participationService,
                                 ResultRepository resultRepository) {
        this.textSubmissionRepository = textSubmissionRepository;
        this.participationRepository = participationRepository;
        this.participationService = participationService;
        this.resultRepository = resultRepository;
    }

    /**
     * Saves the given submission and the corresponding model and creates the result if necessary.
     * Furthermore, the submission is added to the AutomaticSubmissionService if not submitted yet.
     * Is used for creating and updating text submissions.
     * If it is used for a submit action, Compass is notified about the new model.
     * Rolls back if inserting fails - occurs for concurrent createTextSubmission() calls.
     *
     * @param textSubmission the submission to notifyCompass
     * @param textExercise the exercise to notifyCompass in
     * @param participation the participation where the result should be saved
     * @return the textSubmission entity
     */
    @Transactional(rollbackFor = Exception.class)
    public TextSubmission save(TextSubmission textSubmission, TextExercise textExercise, Participation participation) {
        // update submission properties
        textSubmission.setSubmissionDate(ZonedDateTime.now());
        textSubmission.setType(SubmissionType.MANUAL);

        boolean isExampleSubmission = textSubmission.isExampleSubmission() == Boolean.TRUE;

        // Example submissions do not have a participation
        if (!isExampleSubmission) {
            textSubmission.setParticipation(participation);
        }
        textSubmission = textSubmissionRepository.save(textSubmission);

        // All the following are useful only for real submissions
        if (!isExampleSubmission) {
            participation.addSubmissions(textSubmission);

            User user = participation.getStudent();

            if (textSubmission.isSubmitted()) {
                participation.setInitializationState(InitializationState.FINISHED);
            } else if (textExercise.getDueDate() != null && !textExercise.isEnded()) {
                // save submission to HashMap if exercise not ended yet
                AutomaticSubmissionService.updateSubmission(textExercise.getId(), user.getLogin(), textSubmission);
            }
            Participation savedParticipation = participationRepository.save(participation);
            if (textSubmission.getId() == null) {
                textSubmission = savedParticipation.findLatestTextSubmission();
            }
        }

        return textSubmission;
    }

    /**
     * Given an exercise id, find a text submission for that exercise which still doesn't have any result
     *
     * @param exerciseId the exercise we want to retrieve
     * @return a textSubmission without any result, if any
     */
    @Transactional(readOnly = true)
    public Optional<TextSubmission> textSubmissionWithoutResult(long exerciseId) {
        return this.participationService.findByExerciseIdWithEagerSubmissions(exerciseId)
            .stream()
            .peek(participation -> {
                participation.getExercise().setParticipations(null);
            })

            // Map to Latest Submission
            .map(Participation::findLatestTextSubmission)
            .filter(Objects::nonNull)

            // It needs to be submitted to be ready for assessment
            .filter(Submission::isSubmitted)

            .filter(textSubmission -> {
                Result result = resultRepository.findDistinctBySubmissionId(textSubmission.getId()).orElse(null);
                return result == null;

            })

            .findAny();
    }
}
