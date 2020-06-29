package de.tum.in.www1.artemis.service.scheduled;

import static de.tum.in.www1.artemis.config.Constants.*;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.enumeration.ExerciseLifecycle;
import de.tum.in.www1.artemis.domain.participation.ProgrammingExerciseStudentParticipation;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.repository.ProgrammingExerciseRepository;
import de.tum.in.www1.artemis.security.SecurityUtils;
import de.tum.in.www1.artemis.service.GroupNotificationService;
import de.tum.in.www1.artemis.service.ParticipationService;
import de.tum.in.www1.artemis.service.ProgrammingSubmissionService;
import de.tum.in.www1.artemis.service.connectors.VersionControlService;
import de.tum.in.www1.artemis.service.util.Tuple;
import de.tum.in.www1.artemis.web.rest.errors.EntityNotFoundException;
import io.github.jhipster.config.JHipsterConstants;

@Service
@Profile("scheduling")
public class ProgrammingExerciseScheduleService implements IExerciseScheduleService<ProgrammingExercise> {

    private final Logger log = LoggerFactory.getLogger(ProgrammingExerciseScheduleService.class);

    private final ScheduleService scheduleService;

    private final Environment env;

    private final ProgrammingExerciseRepository programmingExerciseRepository;

    private final ProgrammingSubmissionService programmingSubmissionService;

    private final GroupNotificationService groupNotificationService;

    private final Optional<VersionControlService> versionControlService;

    private final ParticipationService participationService;

    public ProgrammingExerciseScheduleService(ScheduleService scheduleService, ProgrammingExerciseRepository programmingExerciseRepository, Environment env,
            ProgrammingSubmissionService programmingSubmissionService, GroupNotificationService groupNotificationService, Optional<VersionControlService> versionControlService,
            ParticipationService participationService) {
        this.scheduleService = scheduleService;
        this.programmingExerciseRepository = programmingExerciseRepository;
        this.programmingSubmissionService = programmingSubmissionService;
        this.groupNotificationService = groupNotificationService;
        this.versionControlService = versionControlService;
        this.participationService = participationService;
        this.env = env;
    }

    @PostConstruct
    @Override
    public void scheduleRunningExercisesOnStartup() {
        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        if (activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)) {
            // only execute this on production server, i.e. when the prod profile is active
            // NOTE: if you want to test this locally, please comment it out, but do not commit the changes
            return;
        }
        SecurityUtils.setAuthorizationObject();
        // TODO: also take exercises with manual assessments into account here
        List<ProgrammingExercise> programmingExercisesWithBuildAfterDueDate = programmingExerciseRepository
                .findAllByBuildAndTestStudentSubmissionsAfterDueDateAfterDate(ZonedDateTime.now());
        programmingExercisesWithBuildAfterDueDate.forEach(this::scheduleExercise);
        // for exams (TODO take info account that the individual due dates can be after the exam end date)
        List<ProgrammingExercise> programmingExercisesWithExam = programmingExerciseRepository.findAllWithEagerExamAllByExamEndDateAfterDate(ZonedDateTime.now());
        programmingExercisesWithExam.forEach(this::scheduleExamExercise);
        log.info("Scheduled building the student submissions for " + programmingExercisesWithBuildAfterDueDate.size() + " programming exercises with a buildAndTestAfterDueDate.");
    }

    /**
     * Will cancel a scheduled task if the buildAndTestAfterDueDate is null or has passed already.
     *
     * @param exercise ProgrammingExercise
     */
    @Override
    public void scheduleExerciseIfRequired(ProgrammingExercise exercise) {
        // TODO: also take exercises with manual assessments into account here and deal better with exams
        if (!isExamExercise(exercise)
                && (exercise.getBuildAndTestStudentSubmissionsAfterDueDate() == null || exercise.getBuildAndTestStudentSubmissionsAfterDueDate().isBefore(ZonedDateTime.now()))) {
            scheduleService.cancelScheduledTaskForLifecycle(exercise, ExerciseLifecycle.DUE);
            scheduleService.cancelScheduledTaskForLifecycle(exercise, ExerciseLifecycle.BUILD_AND_TEST_AFTER_DUE_DATE);
            return;
        }
        scheduleExercise(exercise);
    }

    private void scheduleExercise(ProgrammingExercise exercise) {
        if (isExamExercise(exercise)) {
            scheduleExamExercise(exercise);
        }
        else {
            scheduleRegularExercise(exercise);
        }
    }

    private void scheduleRegularExercise(ProgrammingExercise exercise) {
        // TODO: there is small logic error here. When build and run test date is after the due date, the lock operation might be executed even if it is not necessary.
        scheduleService.scheduleTask(exercise, ExerciseLifecycle.DUE, lockAllStudentRepositories(exercise));
        scheduleService.scheduleTask(exercise, ExerciseLifecycle.BUILD_AND_TEST_AFTER_DUE_DATE, buildAndTestRunnableForExercise(exercise));
        log.debug("Scheduled build and test for student submissions after due date for Programming Exercise \"" + exercise.getTitle() + "\" (#" + exercise.getId() + ") for "
                + exercise.getBuildAndTestStudentSubmissionsAfterDueDate() + ".");
    }

    private void scheduleExamExercise(ProgrammingExercise exercise) {
        var releaseDate = getExamProgrammingExerciseReleaseDate(exercise);
        if (releaseDate.isAfter(ZonedDateTime.now())) {
            // Use the custom date from the exam rather than the of the exercise's lifecycle
            scheduleService.scheduleTask(exercise, ExerciseLifecycle.RELEASE, Set.of(new Tuple<>(releaseDate, unlockAllStudentRepositoriesForExam(exercise))));
        }
        else {
            // This is only a backup (e.g. a crash of this node and restart during the exam)
            scheduleService.scheduleTask(exercise, ExerciseLifecycle.RELEASE,
                    Set.of(new Tuple<>(ZonedDateTime.now().plusSeconds(5), unlockAllStudentRepositoriesForExam(exercise))));
        }

        if (exercise.getBuildAndTestStudentSubmissionsAfterDueDate() != null) {
            scheduleService.scheduleTask(exercise, ExerciseLifecycle.BUILD_AND_TEST_AFTER_DUE_DATE, buildAndTestRunnableForExercise(exercise));
        }
        else {
            scheduleService.cancelScheduledTaskForLifecycle(exercise, ExerciseLifecycle.BUILD_AND_TEST_AFTER_DUE_DATE);
        }
        log.debug("Scheduled Exam Programming Exercise \"" + exercise.getTitle() + "\" (#" + exercise.getId() + ").");
    }

    @NotNull
    private Runnable buildAndTestRunnableForExercise(ProgrammingExercise exercise) {
        return () -> {
            SecurityUtils.setAuthorizationObject();
            try {
                log.info("Invoking scheduled task programming exercise with id " + exercise.getId() + ".");
                programmingSubmissionService.triggerInstructorBuildForExercise(exercise.getId());
            }
            catch (EntityNotFoundException ex) {
                log.error("Programming exercise with id " + exercise.getId() + " is no longer available in database for use in scheduled task.");
            }
        };
    }

    @NotNull
    private Runnable lockAllStudentRepositories(ProgrammingExercise exercise) {
        return lockStudentRepositories(exercise, participation -> true);
    }

    @NotNull
    private Runnable lockStudentRepositories(ProgrammingExercise exercise, Predicate<ProgrammingExerciseStudentParticipation> condition) {
        Long programmingExerciseId = exercise.getId();
        return () -> {
            SecurityUtils.setAuthorizationObject();
            try {
                List<ProgrammingExerciseStudentParticipation> failedLockOperations = removeWritePermissionsFromAllStudentRepositories(programmingExerciseId, condition);

                // We sent a notification to the instructor about the success of the repository locking operation.
                long numberOfFailedLockOperations = failedLockOperations.size();
                Optional<ProgrammingExercise> programmingExercise = programmingExerciseRepository.findWithTemplateParticipationAndSolutionParticipationById(programmingExerciseId);
                if (programmingExercise.isEmpty()) {
                    throw new EntityNotFoundException("programming exercise not found with id " + programmingExerciseId);
                }
                if (numberOfFailedLockOperations > 0) {
                    groupNotificationService.notifyInstructorGroupAboutExerciseUpdate(programmingExercise.get(),
                            Constants.PROGRAMMING_EXERCISE_FAILED_LOCK_OPERATIONS_NOTIFICATION + failedLockOperations.size());
                }
                else {
                    groupNotificationService.notifyInstructorGroupAboutExerciseUpdate(programmingExercise.get(),
                            Constants.PROGRAMMING_EXERCISE_SUCCESSFUL_LOCK_OPERATION_NOTIFICATION);
                }
            }
            catch (EntityNotFoundException ex) {
                log.error("Programming exercise with id " + programmingExerciseId + " is no longer available in database for use in scheduled task.");
            }
        };
    }

    @NotNull
    private Runnable unlockAllStudentRepositoriesForExam(ProgrammingExercise exercise) {
        Long programmingExerciseId = exercise.getId();
        return () -> {
            SecurityUtils.setAuthorizationObject();
            try {
                Set<Tuple<ZonedDateTime, ProgrammingExerciseStudentParticipation>> individualDueDates;
                BiConsumer<ProgrammingExercise, ProgrammingExerciseStudentParticipation> unlockAndCollectOperation;

                individualDueDates = new HashSet<>();
                // This operation unlocks the repositories and collects all individual due dates
                unlockAndCollectOperation = (programmingExercise, participation) -> {
                    var dueDate = participationService.getIndividualDueDate(programmingExercise, participation);
                    individualDueDates.add(new Tuple<>(dueDate, participation));
                    unlockStudentRepository(programmingExercise, participation);
                };
                List<ProgrammingExerciseStudentParticipation> failedUnlockOperations = invokeOperationOnAllParticipationsThatSatisfy(programmingExerciseId,
                        unlockAndCollectOperation, participation -> true, "add write permissions to all student repositories");

                // We sent a notification to the instructor about the success of the repository unlocking operation.
                long numberOfFailedUnlockOperations = failedUnlockOperations.size();
                Optional<ProgrammingExercise> programmingExercise = programmingExerciseRepository.findWithTemplateParticipationAndSolutionParticipationById(programmingExerciseId);
                if (programmingExercise.isEmpty()) {
                    throw new EntityNotFoundException("programming exercise not found with id " + programmingExerciseId);
                }
                if (numberOfFailedUnlockOperations > 0) {
                    groupNotificationService.notifyInstructorGroupAboutExerciseUpdate(programmingExercise.get(),
                            Constants.PROGRAMMING_EXERCISE_FAILED_LOCK_OPERATIONS_NOTIFICATION + failedUnlockOperations.size());
                }
                else {
                    groupNotificationService.notifyInstructorGroupAboutExerciseUpdate(programmingExercise.get(),
                            Constants.PROGRAMMING_EXERCISE_SUCCESSFUL_LOCK_OPERATION_NOTIFICATION);
                }

                // Schedule the lock operations here, this is also done here because the working times might change often before the exam start
                scheduleIndividualRepositoryLockTasks(exercise, individualDueDates);
            }
            catch (EntityNotFoundException ex) {
                log.error("Programming exercise with id " + programmingExerciseId + " is no longer available in database for use in scheduled task.");
            }
        };
    }

    private void scheduleIndividualRepositoryLockTasks(ProgrammingExercise exercise) {
        Optional<ProgrammingExercise> programmingExercise = programmingExerciseRepository.findWithEagerStudentParticipationsById(exercise.getId());
        if (programmingExercise.isEmpty()) {
            throw new EntityNotFoundException("programming exercise not found with id " + exercise.getId());
        }
        // Collect all individual due dates
        Set<Tuple<ZonedDateTime, ProgrammingExerciseStudentParticipation>> individualDueDates = new HashSet<>();
        for (StudentParticipation studentParticipation : programmingExercise.get().getStudentParticipations()) {
            var programmingExerciseStudentParticipation = (ProgrammingExerciseStudentParticipation) studentParticipation;
            var dueDate = participationService.getIndividualDueDate(programmingExercise.get(), programmingExerciseStudentParticipation);
            individualDueDates.add(new Tuple<>(dueDate, programmingExerciseStudentParticipation));
        }
        scheduleIndividualRepositoryLockTasks(exercise, individualDueDates);
    }

    private void scheduleIndividualRepositoryLockTasks(ProgrammingExercise exercise, Set<Tuple<ZonedDateTime, ProgrammingExerciseStudentParticipation>> individualDueDates) {
        // 1. Group all participations by due date (TODO use student exams for safety if some participations are not pre-generated)
        var participationsGroupedByDueDate = individualDueDates.stream().collect(Collectors.groupingBy(Tuple::getX, Collectors.mapping(Tuple::getY, Collectors.toSet())));
        // 2. Transform those groups into lock-repository tasks with times
        var tasks = participationsGroupedByDueDate.entrySet().stream().map(entry -> {
            Predicate<ProgrammingExerciseStudentParticipation> lockingCondition = participation -> entry.getValue().contains(participation);
            var groupDueDate = entry.getKey();
            var task = lockStudentRepositories(exercise, lockingCondition);
            return new Tuple<>(groupDueDate, task);
        }).collect(Collectors.toSet());
        // 3. Schedule all tasks
        scheduleService.scheduleTask(exercise, ExerciseLifecycle.DUE, tasks);
    }

    private static boolean isExamExercise(ProgrammingExercise exercise) {
        return exercise.hasExerciseGroup();
    }

    private static ZonedDateTime getExamProgrammingExerciseReleaseDate(ProgrammingExercise exercise) {
        // Should we take the exercise's own release date more into account?
        // using visible date here because unlocking will take some time, see delay below.
        var releaseDate = exercise.getExerciseGroup().getExam().getVisibleDate();
        if (releaseDate == null) {
            releaseDate = exercise.getReleaseDate();
        }
        return releaseDate;
    }

    /**
     * Remove the write permissions for all students for their programming exercise repository.
     * They will still be able to read the code, but won't be able to change it.
     *
     * Requests are executed in batches so that the VCS is not overloaded with requests.
     *
     * @param programmingExerciseId     ProgrammingExercise id.
     * @return a list of participations for which the locking operation has failed. If everything went as expected, this should be an empty list.
     * @throws EntityNotFoundException  if the programming exercise can't be found.
     */
    public List<ProgrammingExerciseStudentParticipation> removeWritePermissionsFromAllStudentRepositories(Long programmingExerciseId) throws EntityNotFoundException {
        return removeWritePermissionsFromAllStudentRepositories(programmingExerciseId, participation -> true);
    }

    private List<ProgrammingExerciseStudentParticipation> removeWritePermissionsFromAllStudentRepositories(Long programmingExerciseId,
            Predicate<ProgrammingExerciseStudentParticipation> condition) throws EntityNotFoundException {
        return invokeOperationOnAllParticipationsThatSatisfy(programmingExerciseId, this::lockStudentRepository, condition,
                "remove write permissions from all student repositories");
    }

    /**
     * Add the write permission for all students for their programming exercise repository.
     * This allows them to work on the programming exercise if the repositories were locked before.
     *
     * Requests are executed in batches so that the VCS is not overloaded with requests.
     *
     * @param programmingExerciseId     ProgrammingExercise id.
     * @return a list of participations for which the unlocking operation has failed. If everything went as expected, this should be an empty list.
     * @throws EntityNotFoundException  if the programming exercise can't be found.
     */
    public List<ProgrammingExerciseStudentParticipation> addWritePermissionsToAllStudentRepositories(Long programmingExerciseId) throws EntityNotFoundException {
        return invokeOperationOnAllParticipationsThatSatisfy(programmingExerciseId, this::unlockStudentRepository, participation -> true,
                "add write permissions to all student repositories");
    }

    /**
     * Invokes the given <code>operation</code> on all student participations that satisfy the <code>condition</code>-{@link Predicate}.
     * <p>
     * Requests are executed in batches so that the VCS is not overloaded with requests.
     * 
     * @param programmingExerciseId the programming exercise whose participations should be processed
     * @param operation the operation to perform
     * @param condition the condition that tests whether to invoke the operation on a participation
     * @param operationName the name of the operation, this is only used for logging
     * @return a list containing all participations for which the operation has failed with an exception
     * @throws EntityNotFoundException if the programming exercise can't be found.
     */
    private List<ProgrammingExerciseStudentParticipation> invokeOperationOnAllParticipationsThatSatisfy(Long programmingExerciseId,
            BiConsumer<ProgrammingExercise, ProgrammingExerciseStudentParticipation> operation, Predicate<ProgrammingExerciseStudentParticipation> condition,
            String operationName) {
        log.info("Invoking scheduled task '" + operationName + "' for programming exercise with id " + programmingExerciseId + ".");

        Optional<ProgrammingExercise> programmingExercise = programmingExerciseRepository.findWithEagerStudentParticipationsById(programmingExerciseId);
        if (programmingExercise.isEmpty()) {
            throw new EntityNotFoundException("programming exercise not found with id " + programmingExerciseId);
        }
        List<ProgrammingExerciseStudentParticipation> failedOperations = new LinkedList<>();

        int index = 0;
        for (StudentParticipation studentParticipation : programmingExercise.get().getStudentParticipations()) {
            ProgrammingExerciseStudentParticipation programmingExerciseStudentParticipation = (ProgrammingExerciseStudentParticipation) studentParticipation;

            // ignore all participations that don't fulfill the condition
            if (!condition.test(programmingExerciseStudentParticipation)) {
                continue;
            }

            // Execute requests in batches instead all at once.
            if (index > 0 && index % EXTERNAL_SYSTEM_REQUEST_BATCH_SIZE == 0) {
                try {
                    log.info("Sleep for {}s during invokeOperationOnAllParticipationsThatSatisfy", EXTERNAL_SYSTEM_REQUEST_BATCH_WAIT_TIME_MS / 1000);
                    Thread.sleep(EXTERNAL_SYSTEM_REQUEST_BATCH_WAIT_TIME_MS);
                }
                catch (InterruptedException ex) {
                    log.error("Exception encountered when pausing during '" + operationName + "' for exercise " + programmingExerciseId, ex);
                }
            }

            try {
                operation.accept(programmingExercise.get(), programmingExerciseStudentParticipation);
            }
            catch (Exception e) {
                log.error("'" + operationName + "' failed for programming exercise with id " + programmingExerciseId + " for student repository with participation id "
                        + studentParticipation.getId() + ": " + e);
                failedOperations.add(programmingExerciseStudentParticipation);
            }
            index++;
        }
        return failedOperations;
    }

    private void lockStudentRepository(ProgrammingExercise programmingExercise, ProgrammingExerciseStudentParticipation participation) {
        versionControlService.get().setRepositoryPermissionsToReadOnly(participation.getRepositoryUrlAsUrl(), programmingExercise.getProjectKey(), participation.getStudents());
    }

    private void unlockStudentRepository(ProgrammingExercise programmingExercise, ProgrammingExerciseStudentParticipation participation) {
        versionControlService.get().configureRepository(programmingExercise, participation.getRepositoryUrlAsUrl(), participation.getStudents(), true);
    }
}
