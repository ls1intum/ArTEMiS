package de.tum.in.www1.artemis.service;

import com.google.gson.JsonObject;
import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.ParticipationState;
import de.tum.in.www1.artemis.domain.enumeration.SubmissionType;
import de.tum.in.www1.artemis.repository.JsonAssessmentRepository;
import de.tum.in.www1.artemis.repository.JsonModelRepository;
import de.tum.in.www1.artemis.repository.ModelingSubmissionRepository;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.service.compass.CompassService;
import de.tum.in.www1.artemis.web.rest.errors.ConflictException;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;

@Service
@Transactional
public class ModelingSubmissionService {
    private final Logger log = LoggerFactory.getLogger(ModelingSubmissionService.class);

    private final ModelingSubmissionRepository modelingSubmissionRepository;
    private final ResultRepository resultRepository;
    private final JsonModelRepository jsonModelRepository;
    private final JsonAssessmentRepository jsonAssessmentRepository;
    private final CompassService compassService;
    private final ParticipationService participationService;

    public ModelingSubmissionService(ModelingSubmissionRepository modelingSubmissionRepository,
                                     ResultRepository resultRepository,
                                     JsonModelRepository jsonModelRepository,
                                     JsonAssessmentRepository jsonAssessmentRepository,
                                     CompassService compassService,
                                     ParticipationService participationService) {
        this.modelingSubmissionRepository = modelingSubmissionRepository;
        this.resultRepository = resultRepository;
        this.jsonModelRepository = jsonModelRepository;
        this.jsonAssessmentRepository = jsonAssessmentRepository;
        this.compassService = compassService;
        this.participationService = participationService;
    }

    /**
     * Saves the given submission and the corresponding model and creates the result if necessary.
     * Furthermore, the submission is added to the AutomaticSubmissionService if not submitted yet.
     * Is used for creating and updating modeling submissions.
     * If it is used for a submit action, Compass is notified about the new model.
     * Rolls back if inserting fails - occurs for concurrent createModelingSubmission() calls.
     *
     * @param modelingSubmission the submission to notifyCompass
     * @param modelingExercise the exercise to notifyCompass in
     * @param participation the participation where the result should be saved
     * @return the modelingSubmission entity
     */
    @Transactional(rollbackFor = Exception.class)
    public ModelingSubmission save(ModelingSubmission modelingSubmission, ModelingExercise modelingExercise, Participation participation) {
        // update submission properties
        modelingSubmission.setSubmissionDate(ZonedDateTime.now());
        modelingSubmission.setType(SubmissionType.MANUAL);
        modelingSubmission.setParticipation(participation);
        modelingSubmission = modelingSubmissionRepository.save(modelingSubmission);

        User user = participation.getStudent();
        String model = modelingSubmission.getModel();
        if (model != null && !model.isEmpty()) {
            jsonModelRepository.writeModel(modelingExercise.getId(), user.getId(), modelingSubmission.getId(), model);
        }

        if (modelingSubmission.isSubmitted()) {
            notifyCompass(modelingSubmission, modelingExercise);
            handleSubmission(modelingSubmission);
        } else if (modelingExercise.getDueDate() != null && !modelingExercise.isEnded()) {
            // save submission to HashMap if exercise not ended yet
            AutomaticSubmissionService.updateSubmission(modelingExercise.getId(), user.getLogin(), modelingSubmission);
        }

        return modelingSubmission;
    }

    /**
     * Adds a model to compass service to include it in the automatic grading process.
     *
     * @param modelingSubmission    the submission which contains the model
     * @param modelingExercise      the exercise the submission belongs to
     */
    public void notifyCompass(ModelingSubmission modelingSubmission, ModelingExercise modelingExercise) {
        modelingSubmission = getAndSetModel(modelingSubmission);
        this.compassService.addModel(modelingExercise.getId(), modelingSubmission.getId(), modelingSubmission.getModel());
    }

    /**
     * Checks if zhe model for given exerciseId, studentId and model modelId exists and returns it if found.
     *
     * @param exerciseId    the exercise modelId for which to find the model
     * @param studentId     the student modelId for which to find the model
     * @param modelId       the model modelId which corresponds to the submission id
     * @return the model JsonObject if found otherwise null
     */
    public JsonObject getModel(long exerciseId, long studentId, long modelId) {
        if (jsonModelRepository.exists(exerciseId, studentId, modelId)) {
            return jsonModelRepository.readModel(exerciseId, studentId, modelId);
        }
        return null;
    }

    /**
     * Find the latest modeling submission by a given participation. First, it tries to retrieve the modeling submission
     * from the participation's submissions. Then it looks for the submission through the participation's results.
     *
     * @param participation    the participation for which to find the modelingSubmission
     * @return the modelingSubmission if found otherwise null
     */
    public ModelingSubmission findLatestModelingSubmissionByParticipation(Participation participation) {
        ModelingSubmission modelingSubmission = null;
        Submission submission = participationService.findLatestSubmission(participation);
        if (submission != null && submission instanceof ModelingSubmission) {
            modelingSubmission = (ModelingSubmission) submission;
        }

        /**
         * This is needed as a backup to find the modeling submission through the participation's results
         * because in the past submissions weren't saved to the participation. Therefore some submissions
         * do not have a reference to their participation and vice versa.
         */
        if (modelingSubmission == null) {
            Result result = participationService.findLatestResult(participation);
            if (result != null && result.getSubmission() != null) {
                if (result.getSubmission() instanceof HibernateProxy) {
                    modelingSubmission = (ModelingSubmission) Hibernate.unproxy(result.getSubmission());
                } else if (result.getSubmission() instanceof ModelingSubmission) {
                    modelingSubmission = (ModelingSubmission) result.getSubmission();
                }
            }
        }
        return modelingSubmission;
    }

    /**
     * Checks whether the given modelingSubmission has a model or not and tries to read and set it.
     *
     * @param modelingSubmission    the modeling submission for which to get and set the model
     * @return the modelingSubmission with the model or null if error occurred while getting model or participation is null
     */
    public ModelingSubmission getAndSetModel(ModelingSubmission modelingSubmission) {
        if (modelingSubmission.getModel() == null || modelingSubmission.getModel() == "") {
            Participation participation = modelingSubmission.getParticipation();
            if (participation == null) {
                log.error("The modeling submission {} does not have a participation.", modelingSubmission);
                return null;
            }
            Exercise exercise = participation.getExercise();
            try {
                JsonObject model = getModel(exercise.getId(), participation.getStudent().getId(), modelingSubmission.getId());
                modelingSubmission.setModel(model.toString());
            } catch (Exception e) {
                log.error("Exception while retrieving the model for modeling submission {}:\n{}", modelingSubmission.getId(), e.getMessage());
                return null;
            }
        }
        return modelingSubmission;
    }

    /**
     * If student submits his model, set participation to finished and check if automatic assessment is available.
     *
     * @param modelingSubmission    the modeling submission, which contains the model and the submission status
     * @return the modelingSubmission with the result if applicable
     */
    @Transactional
    public ModelingSubmission handleSubmission(ModelingSubmission modelingSubmission) {
        if (modelingSubmission.isSubmitted()) {
            Participation participation = participationService.findOne(modelingSubmission.getParticipation().getId());
            participation.setInitializationState(ParticipationState.FINISHED);
            participationService.save(participation);

            if (modelingSubmission.getResult() == null && jsonAssessmentRepository.exists(participation.getExercise().getId(), participation.getStudent().getId(), modelingSubmission.getId(), false)) {
                Result result = resultRepository.findDistinctBySubmissionId(modelingSubmission.getId()).orElse(null);
                modelingSubmission.setResult(result);
                modelingSubmissionRepository.save(modelingSubmission);
            }
        }
        return modelingSubmission;
    }
}
