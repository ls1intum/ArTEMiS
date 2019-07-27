package de.tum.in.www1.artemis.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.quiz.QuizExercise;
import de.tum.in.www1.artemis.exception.GitException;
import de.tum.in.www1.artemis.repository.ExerciseRepository;
import de.tum.in.www1.artemis.service.connectors.ContinuousIntegrationService;
import de.tum.in.www1.artemis.service.connectors.GitService;
import de.tum.in.www1.artemis.service.connectors.VersionControlService;
import de.tum.in.www1.artemis.service.scheduled.QuizScheduleService;
import de.tum.in.www1.artemis.web.rest.errors.EntityNotFoundException;

/**
 * Service Implementation for managing Exercise.
 */
@Service
@Transactional
public class ExerciseService {

    private final Logger log = LoggerFactory.getLogger(ExerciseService.class);

    private final ExerciseRepository exerciseRepository;

    private final UserService userService;

    private final ParticipationService participationService;

    private final AuthorizationCheckService authCheckService;

    private final Optional<ContinuousIntegrationService> continuousIntegrationService;

    private final Optional<VersionControlService> versionControlService;

    private final Optional<GitService> gitService;

    private final Optional<ProgrammingExerciseService> programmingExerciseService;

    private final QuizStatisticService quizStatisticService;

    private final QuizScheduleService quizScheduleService;

    public ExerciseService(ExerciseRepository exerciseRepository, UserService userService, ParticipationService participationService, AuthorizationCheckService authCheckService,
            Optional<ContinuousIntegrationService> continuousIntegrationService, Optional<VersionControlService> versionControlService, Optional<GitService> gitService,
            Optional<ProgrammingExerciseService> programmingExerciseService, QuizStatisticService quizStatisticService, QuizScheduleService quizScheduleService) {
        this.exerciseRepository = exerciseRepository;
        this.userService = userService;
        this.participationService = participationService;
        this.authCheckService = authCheckService;
        this.continuousIntegrationService = continuousIntegrationService;
        this.versionControlService = versionControlService;
        this.gitService = gitService;
        this.programmingExerciseService = programmingExerciseService;
        this.quizStatisticService = quizStatisticService;
        this.quizScheduleService = quizScheduleService;
    }

    /**
     * Save a exercise.
     *
     * @param exercise the entity to save
     * @return the persisted entity
     */
    public Exercise save(Exercise exercise) {
        log.debug("Request to save Exercise : {}", exercise);
        return exerciseRepository.save(exercise);
    }

    /**
     * Get all the exercises.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Exercise> findAll(Pageable pageable) {
        log.debug("Request to get all Exercises");
        List<Exercise> result = exerciseRepository.findAll();
        User user = userService.getUserWithGroupsAndAuthorities();
        Stream<Exercise> userExercises = result.stream().filter(exercise -> authCheckService.isAllowedToSeeExercise(exercise, user));
        List<Exercise> filteredExercises = userExercises.collect(Collectors.toList());
        return new PageImpl<>(userExercises.collect(Collectors.toList()), pageable, filteredExercises.size());
    }

    /**
     * Get all exercises for a given course and a given user. This method is used to retrieve this
     *
     * @param course for return of exercises in course
     * @param user the user who requests the exercises in the client. Is used to determine, if the user is allowed to see the exercise
     * @return the list of exercises for the given course and user. This list can be empty, but should not be null
     */
    @Transactional(readOnly = true)
    public List<Exercise> findAllExercisesForCourseAdministration(Course course, User user) {
        List<Exercise> exercises = new ArrayList<>();
        if (authCheckService.isAdmin() || authCheckService.isInstructorInCourse(course, user) || authCheckService.isTeachingAssistantInCourse(course, user)) {
            // user can see this exercise
            exercises = exerciseRepository.findAllByCourseId(course.getId());
        }
        return exercises;
    }

    @Transactional(readOnly = true)
    public List<Exercise> findAllForCourse(Course course, boolean withLtiOutcomeUrlExisting, Principal principal, User user) {
        List<Exercise> exercises = null;
        if (authCheckService.isAdmin() || authCheckService.isInstructorInCourse(course, user) || authCheckService.isTeachingAssistantInCourse(course, user)) {
            // user can see this exercise
            exercises = exerciseRepository.findByCourseId(course.getId());
        }
        else if (authCheckService.isStudentInCourse(course, user)) {

            if (course.isOnlineCourse() && withLtiOutcomeUrlExisting) {
                // students in only courses can only see exercises where the lti outcome url exists, otherwise the result cannot be reported later on
                exercises = exerciseRepository.findByCourseIdWhereLtiOutcomeUrlExists(course.getId(), principal);
            }
            else {
                exercises = exerciseRepository.findByCourseId(course.getId());
            }

            // user is student for this course and might not have the right to see it so we have to filter

            // filter out exercises that are not released (or explicitly made visible to students) yet
            exercises = exercises.stream().filter(Exercise::isVisibleToStudents).collect(Collectors.toList());
        }

        // filter out questions and all statistical information about the quizPointStatistic from quizExercises (so users can't see which answer options are correct)
        for (Exercise exercise : exercises) {
            if (exercise instanceof QuizExercise) {
                QuizExercise quizExercise = (QuizExercise) exercise;
                quizExercise.filterSensitiveInformation();
            }
        }

        return exercises;
    }

    /**
     * Get one exercise by exerciseId.
     *
     * @param exerciseId the exerciseId of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Exercise findOne(Long exerciseId) {
        Optional<Exercise> exercise = exerciseRepository.findById(exerciseId);
        if (!exercise.isPresent()) {
            throw new EntityNotFoundException("Exercise with exerciseId " + exerciseId + " does not exist!");
        }
        updateExerciseElementsAfterDatabaseFetch(exercise.get());
        return exercise.get();
    }

    /**
     * Find exercise by exerciseId and load participations in this exercise.
     *
     * @param exerciseId the exerciseId of the exercise entity
     * @return the exercise entity
     */
    @Transactional(readOnly = true)
    public Exercise findOneLoadParticipations(Long exerciseId) {
        log.debug("Request to find Exercise with participations loaded: {}", exerciseId);
        Optional<Exercise> exercise = exerciseRepository.findByIdWithEagerParticipations(exerciseId);

        if (!exercise.isPresent()) {
            throw new EntityNotFoundException("Exercise with exerciseId " + exerciseId + " does not exist!");
        }
        updateExerciseElementsAfterDatabaseFetch(exercise.get());
        return exercise.get();
    }

    // TODO: we could move this to Exercise.java and override it in the subclasses to avoid the if-else statements
    private void updateExerciseElementsAfterDatabaseFetch(Exercise exercise) {
        if (exercise instanceof QuizExercise) {
            QuizExercise quizExercise = (QuizExercise) exercise;
            // eagerly load questions and statistic
            quizExercise.getQuizQuestions().size();
            quizExercise.getQuizPointStatistic().getId();
        }
        else if (exercise instanceof ProgrammingExercise) {
            ProgrammingExercise programmingExercise = (ProgrammingExercise) exercise;
            // eagerly load templateParticipation and solutionParticipation
            programmingExercise.setTemplateParticipation((TemplateProgrammingExerciseParticipation) Hibernate.unproxy(programmingExercise.getTemplateParticipation()));
            programmingExercise.setSolutionParticipation((SolutionProgrammingExerciseParticipation) Hibernate.unproxy(programmingExercise.getSolutionParticipation()));
        }
    }

    /**
     * Resets an Exercise by deleting all its Participations
     *
     * @param exercise
     */
    @Transactional(noRollbackFor = { Throwable.class })
    public void reset(Exercise exercise) {
        log.debug("Request reset Exercise : {}", exercise.getId());

        // delete all participations for this exercise
        participationService.deleteAllByExerciseId(exercise.getId(), true, true);

        if (exercise instanceof QuizExercise) {

            // refetch exercise to make sure we have an updated version
            exercise = findOneLoadParticipations(exercise.getId());

            // for quizzes we need to delete the statistics and we need to reset the quiz to its original state
            QuizExercise quizExercise = (QuizExercise) exercise;
            quizExercise.setIsVisibleBeforeStart(Boolean.FALSE);
            quizExercise.setIsPlannedToStart(Boolean.FALSE);
            quizExercise.setAllowedNumberOfAttempts(null);
            quizExercise.setIsOpenForPractice(Boolean.FALSE);
            quizExercise.setReleaseDate(null);

            // TODO: the dependencies to concrete exercise types here are not really nice. We should find a better way to structure this, e.g. having this call managed in the quiz
            // exercise resource
            // which delegates some functionality to the exercise service

            // in case the quiz has not yet started or the quiz is currently running, we have to cleanup
            quizScheduleService.cancelScheduledQuizStart(quizExercise.getId());
            quizScheduleService.clearQuizData(quizExercise.getId());

            // clean up the statistics
            quizStatisticService.recalculateStatistics(quizExercise);
        }
    }

    /**
     * Delete the exercise by id and all its participations.
     *
     * @param exercise                     the exercise to be deleted
     * @param deleteStudentReposBuildPlans whether the student repos and build plans should be deleted
     * @param deleteBaseReposBuildPlans    whether the template and solution repos and build plans should be deleted
     */
    @Transactional
    public void delete(Exercise exercise, boolean deleteStudentReposBuildPlans, boolean deleteBaseReposBuildPlans) {
        log.debug("Request to delete Exercise : {}", exercise.getTitle());
        // delete all participations belonging to this quiz
        participationService.deleteAllByExerciseId(exercise.getId(), deleteStudentReposBuildPlans, deleteStudentReposBuildPlans);
        // Programming exercises have some special stuff that needs to be cleaned up (solution/template participation, build plans, etc.).
        if (exercise instanceof ProgrammingExercise) {
            ProgrammingExercise programmingExercise = (ProgrammingExercise) exercise;
            programmingExerciseService.get().delete(programmingExercise, deleteBaseReposBuildPlans);
        }
        else {
            exerciseRepository.delete(exercise);
        }
    }

    /**
     * Delete build plans (except BASE) and optionally git repositories of all exercise participations.
     *
     * @param id id of the exercise for which build plans in respective participations are deleted
     */
    @Transactional(noRollbackFor = { Throwable.class })
    public void cleanup(Long id, boolean deleteRepositories) {
        Exercise exercise = findOneLoadParticipations(id);
        log.info("Request to cleanup all participations for Exercise : {}", exercise.getTitle());

        if (exercise instanceof ProgrammingExercise) {
            for (StudentParticipation participation : exercise.getParticipations()) {
                participationService.cleanupBuildPlan((ProgrammingExerciseStudentParticipation) participation);
            }

            if (!deleteRepositories) {
                return;    // in this case, we are done
            }

            for (StudentParticipation participation : exercise.getParticipations()) {
                participationService.cleanupRepository((ProgrammingExerciseStudentParticipation) participation);
            }

        }
        else {
            log.warn("Exercise with id {} is not an instance of ProgrammingExercise. Ignoring the request to cleanup repositories and build plan", id);
        }
    }

    /**
     * Get participations of coding exercises of a requested list of students packed together in one zip file.
     *
     * @param exerciseId the id of the exercise entity
     * @param studentIds TUM Student-Login ID of requested students
     * @return a zip file containing all requested participations
     */
    @Transactional(readOnly = true)
    public java.io.File exportParticipations(Long exerciseId, List<String> studentIds) {
        Exercise exercise = findOneLoadParticipations(exerciseId);
        List<Path> zippedRepoFiles = new ArrayList<>();
        Path zipFilePath = null;
        if (!Optional.ofNullable(exercise).isPresent() || !(exercise instanceof ProgrammingExercise)) {
            log.debug("Exercise with id {} is not an instance of ProgrammingExercise. Ignoring the request to export repositories", exerciseId);
            return null;
        }
        for (StudentParticipation participation : exercise.getParticipations()) {
            ProgrammingExerciseStudentParticipation studentParticipation = (ProgrammingExerciseStudentParticipation) participation;
            try {
                if (studentParticipation.getRepositoryUrl() == null || studentParticipation.getStudent() == null
                        || !studentIds.contains(studentParticipation.getStudent().getLogin())) {
                    // participation is not relevant for zip archive.
                    continue;
                }

                boolean repoAlreadyExists = gitService.get().repositoryAlreadyExists(studentParticipation.getRepositoryUrlAsUrl());

                Repository repo = gitService.get().getOrCheckoutRepository(studentParticipation);
                gitService.get().resetToOriginMaster(repo); // start with clean state
                gitService.get().filterLateSubmissions(repo, (ProgrammingExercise) exercise);
                programmingExerciseService.get().addStudentIdToProjectName(repo, (ProgrammingExercise) exercise, participation);
                gitService.get().squashAfterInstructor(repo, (ProgrammingExercise) exercise);
                log.debug("Create temporary zip file for repository " + repo.getLocalPath().toString());
                Path zippedRepoFile = gitService.get().zipRepository(repo);
                zippedRepoFiles.add(zippedRepoFile);
                boolean allowInlineEditor = ((ProgrammingExercise) exercise).isAllowOnlineEditor() != null && ((ProgrammingExercise) exercise).isAllowOnlineEditor();
                if (!allowInlineEditor || !repoAlreadyExists) {
                    // if onlineeditor is *not* allowed OR onlineEditor *is* allowed and repo didn't exist beforehand
                    // --> we are free to delete
                    log.debug("Delete temporary repoistory " + repo.getLocalPath().toString());
                    gitService.get().deleteLocalRepository(studentParticipation);
                }
                else {
                    // finish with clean state
                    gitService.get().checkoutBranch(repo, "master");
                    gitService.get().deleteLocalBranch(repo, "stager");
                    gitService.get().resetToOriginMaster(repo);
                }
            }
            catch (IOException | GitException | GitAPIException | InterruptedException ex) {
                log.error("export repository Participation for " + studentParticipation.getRepositoryUrlAsUrl() + "and Students" + studentIds + " did not work as expected: " + ex);
            }
        }
        if (exercise.getParticipations().isEmpty() || zippedRepoFiles.isEmpty()) {
            log.debug("The zip file could not be created. Ignoring the request to export repositories", exerciseId);
            return null;
        }
        try {
            // create a large zip file with all zipped repos and provide it for download
            log.debug("Create zip file for all repositories");
            zipFilePath = Paths.get(zippedRepoFiles.get(0).getParent().toString(), exercise.getCourse().getTitle() + " " + exercise.getTitle() + studentIds.hashCode() + ".zip");
            createZipFile(zipFilePath, zippedRepoFiles);
            scheduleForDeletion(zipFilePath, 10);

            log.debug("Delete all temporary zip repo files");
            // delete the temporary zipped repo files
            for (Path zippedRepoFile : zippedRepoFiles) {
                Files.delete(zippedRepoFile);
            }
        }
        catch (IOException ex) {
            log.error("Archiving and deleting the local repositories did not work as expected");
        }
        return new java.io.File(zipFilePath.toString());
    }

    // does not delete anything
    @Transactional(readOnly = true)
    public java.io.File archive(Long id) {
        Exercise exercise = findOneLoadParticipations(id);
        log.info("Request to archive all participations repositories for Exercise : {}", exercise.getTitle());
        List<Path> zippedRepoFiles = new ArrayList<>();
        Path finalZipFilePath = null;
        if (exercise instanceof ProgrammingExercise) {
            exercise.getParticipations().forEach(participation -> {
                ProgrammingExerciseStudentParticipation studentParticipation = (ProgrammingExerciseStudentParticipation) participation;
                try {
                    if (studentParticipation.getRepositoryUrl() != null) {     // ignore participations without repository URL and without student
                        // 1. clone the repository
                        Repository repo = gitService.get().getOrCheckoutRepository(studentParticipation);
                        // 2. zip repository and collect the zip file
                        log.debug("Create temporary zip file for repository " + repo.getLocalPath().toString());
                        Path zippedRepoFile = gitService.get().zipRepository(repo);
                        zippedRepoFiles.add(zippedRepoFile);
                        // 3. delete the locally cloned repo again
                        gitService.get().deleteLocalRepository(studentParticipation);
                    }
                }
                catch (IOException | GitException | GitAPIException | InterruptedException ex) {
                    log.error("Archiving and deleting the repository " + studentParticipation.getRepositoryUrlAsUrl() + " did not work as expected: " + ex);
                }
            });

            if (!exercise.getParticipations().isEmpty() && !zippedRepoFiles.isEmpty()) {
                try {
                    // create a large zip file with all zipped repos and provide it for download
                    log.info("Create zip file for all repositories");
                    String exerciseName = exercise.getShortName() != null ? exercise.getShortName() : exercise.getTitle().replaceAll("\\s", "");
                    finalZipFilePath = Paths.get(zippedRepoFiles.get(0).getParent().toString(), exercise.getCourse().getShortName() + "-" + exerciseName + ".zip");
                    createZipFile(finalZipFilePath, zippedRepoFiles);
                    scheduleForDeletion(finalZipFilePath, 300);

                    log.info("Delete all temporary zip repo files");
                    // delete the temporary zipped repo files
                    for (Path zippedRepoFile : zippedRepoFiles) {
                        Files.delete(zippedRepoFile);
                    }
                }
                catch (IOException ex) {
                    log.error("Archiving and deleting the local repositories did not work as expected");
                }
            }
            else {
                log.info("The zip file could not be created. Ignoring the request to archive repositories", id);
                return null;
            }
        }
        else {
            log.info("Exercise with id {} is not an instance of ProgrammingExercise. Ignoring the request to archive repositories", id);
            return null;
        }
        return new java.io.File(finalZipFilePath.toString());
    }

    private void createZipFile(Path zipFilePath, List<Path> paths) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            paths.stream().filter(path -> !Files.isDirectory(path)).forEach(path -> {
                ZipEntry zipEntry = new ZipEntry(path.toString());
                try {
                    zipOutputStream.putNextEntry(zipEntry);
                    Files.copy(path, zipOutputStream);
                    zipOutputStream.closeEntry();
                }
                catch (Exception e) {
                    log.error("Create zip file error", e);
                }
            });
        }
    }

    private Map<Path, ScheduledFuture> futures = new HashMap<>();

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    private static final TimeUnit UNITS = TimeUnit.SECONDS; // your time unit

    private void scheduleForDeletion(Path path, long delay) {
        ScheduledFuture future = executor.schedule(() -> {
            try {
                log.info("Delete file " + path);
                Files.delete(path);
                futures.remove(path);
            }
            catch (IOException e) {
                log.error("Deleting the file " + path + " did not work", e);
            }
        }, delay, UNITS);

        futures.put(path, future);
    }
}
