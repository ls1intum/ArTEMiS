package de.tum.in.www1.exerciseapp.service;

import de.tum.in.www1.exerciseapp.domain.*;
import de.tum.in.www1.exerciseapp.domain.enumeration.ParticipationState;
import de.tum.in.www1.exerciseapp.repository.ParticipationRepository;
import de.tum.in.www1.exerciseapp.repository.ResultRepository;
import de.tum.in.www1.exerciseapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service Implementation for managing Participation.
 */
@Service
@Transactional
public class ParticipationService {

    private final Logger log = LoggerFactory.getLogger(ParticipationService.class);

    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;
    private final Optional<GitService> gitService;
    private final Optional<ContinuousIntegrationService> continuousIntegrationService;
    private final Optional<VersionControlService> versionControlService;

    private final ResultRepository resultRepository;

    public ParticipationService(ParticipationRepository participationRepository, UserRepository userRepository, Optional<GitService> gitService, Optional<ContinuousIntegrationService> continuousIntegrationService, Optional<VersionControlService> versionControlService, ResultRepository resultRepository) {
        this.participationRepository = participationRepository;
        this.userRepository = userRepository;
        this.gitService = gitService;
        this.continuousIntegrationService = continuousIntegrationService;
        this.versionControlService = versionControlService;
        this.resultRepository = resultRepository;
    }

    /**
     * Save a participation.
     *
     * @param participation the entity to save
     * @return the persisted entity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Participation save(Participation participation) {
        log.debug("Request to save Participation : {}", participation);
        return participationRepository.saveAndFlush(participation);
    }

    /**
     * This method should only be invoked for programming exercises, not for other exercises
     *
     * @param exercise
     * @param username
     * @return
     */
    @Transactional
    public Participation init(Exercise exercise, String username) {

        // common for all exercises
        // Check if participation already exists
        Participation participation = participationRepository.findOneByExerciseIdAndStudentLogin(exercise.getId(), username);
        if (!Optional.ofNullable(participation).isPresent() || (!(exercise instanceof QuizExercise) && participation.getInitializationState() == ParticipationState.FINISHED)) { //create a new participation only if it was finished before (not for quiz exercise)
            participation = new Participation();
            participation.setExercise(exercise);

            Optional<User> user = userRepository.findOneByLogin(username);
            if (user.isPresent()) {
                participation.setStudent(user.get());
            }
            participation = save(participation);
        }


        // specific to programming exercises
        if (exercise instanceof ProgrammingExercise) {
            ProgrammingExercise programmingExercise = (ProgrammingExercise) exercise;
            participation.setInitializationState(ParticipationState.UNINITIALIZED);
            participation = copyRepository(participation, programmingExercise);
            participation = configureRepository(participation, programmingExercise);
            participation = copyBuildPlan(participation, programmingExercise);
            participation = configureBuildPlan(participation, programmingExercise);
            participation.setInitializationState(ParticipationState.INITIALIZED);
            participation.setInitializationDate(ZonedDateTime.now());
        } else if (exercise instanceof QuizExercise) {
            if (participation.getInitializationState() == null) {
                participation.setInitializationState(ParticipationState.INITIALIZED);
            }
            if (!Optional.ofNullable(participation.getInitializationDate()).isPresent()) {
                participation.setInitializationDate(ZonedDateTime.now());
            }
        }

        save(participation);
        return participation;
    }

    /**
     * Service method to resume inactive participation (with previously deleted build plan)
     *
     * @param exercise exercise to which the inactive participation belongs
     * @return resumed participation
     */
    public Participation resume(Exercise exercise, Participation participation) {
        ProgrammingExercise programmingExercise = (ProgrammingExercise) exercise;
        participation = copyBuildPlan(participation, programmingExercise);
        participation = configureBuildPlan(participation, programmingExercise);
        participation.setInitializationState(ParticipationState.INITIALIZED);
        if (participation.getInitializationDate() == null) {
            //only set the date if it was not set before (which should NOT be the case)
            participation.setInitializationDate(ZonedDateTime.now());
        }
        save(participation);
        return participation;
    }

    private Participation copyRepository(Participation participation, ProgrammingExercise exercise) {
        if (!participation.getInitializationState().hasCompletedState(ParticipationState.REPO_COPIED)) {
            URL repositoryUrl = versionControlService.get().copyRepository(exercise.getBaseRepositoryUrlAsUrl(), participation.getStudent().getLogin());
            if (Optional.ofNullable(repositoryUrl).isPresent()) {
                participation.setRepositoryUrl(repositoryUrl.toString());
                participation.setInitializationState(ParticipationState.REPO_COPIED);
            }
            return save(participation);
        } else {
            return participation;
        }
    }

    private Participation configureRepository(Participation participation, ProgrammingExercise exercise) {
        if (!participation.getInitializationState().hasCompletedState(ParticipationState.REPO_CONFIGURED)) {
            versionControlService.get().configureRepository(participation.getRepositoryUrlAsUrl(), participation.getStudent().getLogin());
            participation.setInitializationState(ParticipationState.REPO_CONFIGURED);
            return save(participation);
        } else {
            return participation;
        }
    }

    private Participation copyBuildPlan(Participation participation, ProgrammingExercise exercise) {
        if (!participation.getInitializationState().hasCompletedState(ParticipationState.BUILD_PLAN_COPIED)) {
            String buildPlanId = continuousIntegrationService.get().copyBuildPlan(exercise.getBaseBuildPlanId(), participation.getStudent().getLogin());
            participation.setBuildPlanId(buildPlanId);
            participation.setInitializationState(ParticipationState.BUILD_PLAN_COPIED);
            return save(participation);
        } else {
            return participation;
        }
    }

    private Participation configureBuildPlan(Participation participation, ProgrammingExercise exercise) {
        if (!participation.getInitializationState().hasCompletedState(ParticipationState.BUILD_PLAN_CONFIGURED)) {
            continuousIntegrationService.get().configureBuildPlan(
                participation.getBuildPlanId(),
                participation.getRepositoryUrlAsUrl(),
                participation.getStudent().getLogin());
            participation.setInitializationState(ParticipationState.BUILD_PLAN_CONFIGURED);
            return save(participation);
        } else {
            return participation;
        }
    }

    /**
     * Get all the participations.
     *
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public List<Participation> findAll() {
        log.debug("Request to get all Participations");
        return participationRepository.findAll();
    }

    /**
     * Get all the participations.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Participation> findAll(Pageable pageable) {
        log.debug("Request to get all Participations");
        return participationRepository.findAll(pageable);
    }

    /**
     * Get one participation by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Participation findOne(Long id) {
        log.debug("Request to get Participation : {}", id);
        return participationRepository.findOne(id);
    }

    /**
     * Get one initialized/inactive participation by its student and exercise.
     *
     * @param exerciseId the project key of the exercise
     * @param username   the username of the student
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Participation findOneByExerciseIdAndStudentLogin(Long exerciseId, String username) {
        log.debug("Request to get initialized/inactive Participation for User {} for Exercise with id: {}", username, exerciseId);

        Participation participation = participationRepository.findOneByExerciseIdAndStudentLoginAndInitializationState(exerciseId, username, ParticipationState.INITIALIZED);
        if(!Optional.ofNullable(participation).isPresent()) {
            participation = participationRepository.findOneByExerciseIdAndStudentLoginAndInitializationState(exerciseId, username, ParticipationState.INACTIVE);
        }
        return participation;
    }

    /**
     * Get one participation (in any state) by its student and exercise.
     *
     * @param exerciseId the project key of the exercise
     * @param username   the username of the student
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Participation findOneByExerciseIdAndStudentLoginAnyState(Long exerciseId, String username) {
        log.debug("Request to get Participation for User {} for Exercise with id: {}", username, exerciseId);

        Participation participation = participationRepository.findOneByExerciseIdAndStudentLogin(exerciseId, username);
        return participation;
    }

    @Transactional(readOnly = true)
    public Participation findOneByBuildPlanId(String buildPlanId) {
        log.debug("Request to get Participation for build plan id: {}", buildPlanId);
        return participationRepository.findOneByBuildPlanId(buildPlanId);
    }

    /**
     * Delete the participation by id.
     *
     * @param id the id of the entity
     */
    @Transactional
    public void delete(Long id, boolean deleteBuildPlan, boolean deleteRepository) {
        log.debug("Request to delete Participation : {}", id);
        Participation participation = participationRepository.findOne(id);
        if (participation != null && participation.getExercise() instanceof ProgrammingExercise) {
            if (deleteBuildPlan && participation.getBuildPlanId() != null) {
                continuousIntegrationService.get().deleteBuildPlan(participation.getBuildPlanId());
            }
            if (deleteRepository && participation.getRepositoryUrl() != null) {
                versionControlService.get().deleteRepository(participation.getRepositoryUrlAsUrl());
            }

            // delete local repository cache
            try {
                gitService.get().deleteLocalRepository(participation);
            } catch (Exception e) {
                log.error("Error while deleting local repository", e);
            }

        }
        participationRepository.delete(id);
    }

    /**
     * Create the fake participation and Result for the first Exercise in the Tutorial
     *
     * @param user the user for whom the participation is created
     * @param exercise the exercise to which the participation belongs
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createTutorialParticipation(User user, Exercise exercise){
        //check if participation and result already exists
        Participation tutorialE1 = participationRepository.findOneByExerciseIdAndStudentLogin(exercise.getId(), user.getLogin());
        if(tutorialE1 != null){
            return;
        }
        //creating the fake participation
        tutorialE1 = new Participation();
        tutorialE1.setRepositoryUrl("tutorial");
        tutorialE1.setBuildPlanId("tutorial");
        tutorialE1.setStudent(user);
        tutorialE1.setInitializationDate(ZonedDateTime.now());
        tutorialE1.setExercise(exercise);
        tutorialE1.setInitializationState(ParticipationState.INITIALIZED);
        tutorialE1 = save(tutorialE1);

        //creating the fake result for the participation
        Result result = new Result();
        result.setParticipation(tutorialE1);
        result.setSuccessful(true);
        result.setCompletionDate(ZonedDateTime.now());
        result.setResultString("5 passed");
        resultRepository.saveAndFlush(result);
    }
}
