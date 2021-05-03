package de.tum.in.www1.artemis.web.rest;

import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.domain.plagiarism.modeling.ModelingPlagiarismResult;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.security.Role;
import de.tum.in.www1.artemis.service.*;
import de.tum.in.www1.artemis.service.compass.CompassService;
import de.tum.in.www1.artemis.service.plagiarism.ModelingPlagiarismDetectionService;
import de.tum.in.www1.artemis.service.util.TimeLogUtil;
import de.tum.in.www1.artemis.web.rest.dto.PageableSearchDTO;
import de.tum.in.www1.artemis.web.rest.dto.SearchResultPageDTO;
import de.tum.in.www1.artemis.web.rest.dto.SubmissionExportOptionsDTO;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;

/** REST controller for managing ModelingExercise. */
@RestController
@RequestMapping("/api")
public class ModelingExerciseResource {

    private final Logger log = LoggerFactory.getLogger(ModelingExerciseResource.class);

    private static final String ENTITY_NAME = "modelingExercise";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ModelingExerciseRepository modelingExerciseRepository;

    private final UserRepository userRepository;

    private final CourseRepository courseRepository;

    private final AuthorizationCheckService authCheckService;

    private final ModelingExerciseService modelingExerciseService;

    private final ExerciseService exerciseService;

    private final PlagiarismResultRepository plagiarismResultRepository;

    private final ModelingExerciseImportService modelingExerciseImportService;

    private final SubmissionExportService modelingSubmissionExportService;

    private final GroupNotificationService groupNotificationService;

    private final CompassService compassService;

    private final GradingCriterionRepository gradingCriterionRepository;

    private final ModelingPlagiarismDetectionService modelingPlagiarismDetectionService;

    private final ExampleSubmissionRepository exampleSubmissionRepository;

    public ModelingExerciseResource(ModelingExerciseRepository modelingExerciseRepository, UserRepository userRepository, AuthorizationCheckService authCheckService,
            CourseRepository courseRepository, ModelingExerciseService modelingExerciseService, PlagiarismResultRepository plagiarismResultRepository,
            ModelingExerciseImportService modelingExerciseImportService, SubmissionExportService modelingSubmissionExportService, GroupNotificationService groupNotificationService,
            CompassService compassService, ExerciseService exerciseService, GradingCriterionRepository gradingCriterionRepository,
            ModelingPlagiarismDetectionService modelingPlagiarismDetectionService, ExampleSubmissionRepository exampleSubmissionRepository) {
        this.modelingExerciseRepository = modelingExerciseRepository;
        this.modelingExerciseService = modelingExerciseService;
        this.plagiarismResultRepository = plagiarismResultRepository;
        this.modelingExerciseImportService = modelingExerciseImportService;
        this.modelingSubmissionExportService = modelingSubmissionExportService;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.authCheckService = authCheckService;
        this.compassService = compassService;
        this.groupNotificationService = groupNotificationService;
        this.exerciseService = exerciseService;
        this.gradingCriterionRepository = gradingCriterionRepository;
        this.modelingPlagiarismDetectionService = modelingPlagiarismDetectionService;
        this.exampleSubmissionRepository = exampleSubmissionRepository;
    }

    // TODO: most of these calls should be done in the context of a course

    /**
     * POST /modeling-exercises : Create a new modelingExercise.
     *
     * @param modelingExercise the modelingExercise to create
     * @return the ResponseEntity with status 201 (Created) and with body the new modelingExercise, or with status 400 (Bad Request) if the modelingExercise has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    // TODO: we should add courses/{courseId} here
    @PostMapping("/modeling-exercises")
    @PreAuthorize("hasRole('EDITOR')")
    public ResponseEntity<ModelingExercise> createModelingExercise(@RequestBody ModelingExercise modelingExercise) throws URISyntaxException {
        log.debug("REST request to save ModelingExercise : {}", modelingExercise);
        if (modelingExercise.getId() != null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert(applicationName, true, ENTITY_NAME, "idexists", "A new modeling exercise cannot already have an ID")).body(null);
        }
        modelingExercise.checkCourseAndExerciseGroupExclusivity("Modeling Exercise");
        // make sure the course actually exists
        var course = courseRepository.findByIdElseThrow(modelingExercise.getCourseViaExerciseGroupOrCourseMember().getId());
        authCheckService.checkHasAtLeastRoleInCourseElseThrow(Role.EDITOR, course, null);
        exerciseService.validateScoreSettings(modelingExercise);

        ModelingExercise result = modelingExerciseRepository.save(modelingExercise);
        groupNotificationService.notifyTutorGroupAboutExerciseCreated(modelingExercise);
        return ResponseEntity.created(new URI("/api/modeling-exercises/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString())).body(result);
    }

    /**
     * Search for all modeling exercises by title and course title. The result is pageable since there might be hundreds
     * of exercises in the DB.
     *
     * @param search The pageable search containing the page size, page number and query string
     * @return The desired page, sorted and matching the given query
     */
    @GetMapping("/modeling-exercises")
    @PreAuthorize("hasRole('EDITOR')")
    public ResponseEntity<SearchResultPageDTO<ModelingExercise>> getAllExercisesOnPage(PageableSearchDTO<String> search) {
        final var user = userRepository.getUserWithGroupsAndAuthorities();
        return ResponseEntity.ok(modelingExerciseService.getAllOnPageWithSize(search, user));
    }

    /**
     * PUT /modeling-exercises : Updates an existing modelingExercise.
     *
     * @param modelingExercise the modelingExercise to update
     * @param notificationText the text shown to students
     * @return the ResponseEntity with status 200 (OK) and with body the updated modelingExercise, or with status 400 (Bad Request) if the modelingExercise is not valid, or with
     *         status 500 (Internal Server Error) if the modelingExercise couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/modeling-exercises")
    @PreAuthorize("hasRole('EDITOR')")
    public ResponseEntity<ModelingExercise> updateModelingExercise(@RequestBody ModelingExercise modelingExercise,
            @RequestParam(value = "notificationText", required = false) String notificationText) throws URISyntaxException {
        log.debug("REST request to update ModelingExercise : {}", modelingExercise);
        if (modelingExercise.getId() == null) {
            return createModelingExercise(modelingExercise);
        }
        modelingExercise.checkCourseAndExerciseGroupExclusivity("Modeling Exercise");
        var user = userRepository.getUserWithGroupsAndAuthorities();
        // make sure the course actually exists
        var course = courseRepository.findByIdElseThrow(modelingExercise.getCourseViaExerciseGroupOrCourseMember().getId());
        authCheckService.checkHasAtLeastRoleInCourseElseThrow(Role.EDITOR, course, user);
        exerciseService.validateScoreSettings(modelingExercise);

        ModelingExercise modelingExerciseBeforeUpdate = modelingExerciseRepository.findByIdElseThrow(modelingExercise.getId());
        ModelingExercise updatedModelingExercise = modelingExerciseRepository.save(modelingExercise);
        exerciseService.logUpdate(modelingExercise, modelingExercise.getCourseViaExerciseGroupOrCourseMember(), user);

        exerciseService.updatePointsInRelatedParticipantScores(modelingExerciseBeforeUpdate, updatedModelingExercise);
        // Avoid recursions
        if (updatedModelingExercise.getExampleSubmissions().size() != 0) {
            Set<ExampleSubmission> exampleSubmissionsWithResults = exampleSubmissionRepository.findAllWithResultByExerciseId(updatedModelingExercise.getId());
            updatedModelingExercise.setExampleSubmissions(exampleSubmissionsWithResults);
            updatedModelingExercise.getExampleSubmissions().forEach(exampleSubmission -> exampleSubmission.setExercise(null));
            updatedModelingExercise.getExampleSubmissions().forEach(exampleSubmission -> exampleSubmission.setTutorParticipations(null));
        }

        if (notificationText != null) {
            groupNotificationService.notifyStudentGroupAboutExerciseUpdate(modelingExercise, notificationText);
        }
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, modelingExercise.getId().toString()))
                .body(updatedModelingExercise);
    }

    /**
     * GET /courses/:courseId/exercises : get all the exercises.
     *
     * @param courseId the id of the course
     * @return the ResponseEntity with status 200 (OK) and the list of modelingExercises in body
     */
    @GetMapping(value = "/courses/{courseId}/modeling-exercises")
    @PreAuthorize("hasRole('TA')")
    public ResponseEntity<List<ModelingExercise>> getModelingExercisesForCourse(@PathVariable Long courseId) {
        log.debug("REST request to get all ModelingExercises for the course with id : {}", courseId);
        Course course = courseRepository.findByIdElseThrow(courseId);
        User user = userRepository.getUserWithGroupsAndAuthorities();
        if (!authCheckService.isAtLeastTeachingAssistantInCourse(course, user)) {
            return forbidden();
        }
        List<ModelingExercise> exercises = modelingExerciseRepository.findByCourseId(courseId);
        for (Exercise exercise : exercises) {
            // not required in the returned json body
            exercise.setStudentParticipations(null);
            exercise.setCourse(null);
        }
        return ResponseEntity.ok().body(exercises);
    }

    /**
     * GET /modeling-exercises/:id/statistics : get the "id" modelingExercise statistics.
     *
     * @param exerciseId the id of the modelingExercise for which the statistics should be retrieved
     * @return the json encoded modelingExercise statistics
     */
    @GetMapping(value = "/modeling-exercises/{exerciseId}/statistics")
    @PreAuthorize("hasRole('TA')")
    public ResponseEntity<String> getModelingExerciseStatistics(@PathVariable Long exerciseId) {
        log.debug("REST request to get ModelingExercise Statistics for Exercise: {}", exerciseId);
        var modelingExercise = modelingExerciseRepository.findByIdElseThrow(exerciseId);
        authCheckService.checkHasAtLeastRoleForExerciseElseThrow(Role.TEACHING_ASSISTANT, modelingExercise, null);
        if (compassService.isSupported(modelingExercise)) {
            return ResponseEntity.ok(compassService.getStatistics(exerciseId).toString());
        }
        else {
            return notFound();
        }
    }

    /**
     * Prints the Compass statistic regarding the automatic assessment of the modeling exercise with the given id.
     *
     * @param exerciseId the id of the modeling exercise for which we want to get the Compass statistic
     * @return the statistic as key-value pairs in json
     */
    @GetMapping("/modeling-exercises/{exerciseId}/print-statistic")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> printCompassStatisticForExercise(@PathVariable Long exerciseId) {
        ModelingExercise modelingExercise = modelingExerciseRepository.findByIdElseThrow(exerciseId);
        compassService.printStatistic(modelingExercise.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * GET /modeling-exercises/:id : get the "id" modelingExercise.
     *
     * @param exerciseId the id of the modelingExercise to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the modelingExercise, or with status 404 (Not Found)
     */
    @GetMapping("/modeling-exercises/{exerciseId}")
    @PreAuthorize("hasRole('TA')")
    public ResponseEntity<ModelingExercise> getModelingExercise(@PathVariable Long exerciseId) {
        log.debug("REST request to get ModelingExercise : {}", exerciseId);
        var modelingExercise = modelingExerciseRepository.findWithEagerExampleSubmissionsByIdElseThrow(exerciseId);
        authCheckService.checkHasAtLeastRoleForExerciseElseThrow(Role.TEACHING_ASSISTANT, modelingExercise, null);
        List<GradingCriterion> gradingCriteria = gradingCriterionRepository.findByExerciseIdWithEagerGradingCriteria(exerciseId);
        modelingExercise.setGradingCriteria(gradingCriteria);
        return ResponseEntity.ok().body(modelingExercise);
    }

    /**
     * DELETE /modeling-exercises/:id : delete the "id" modelingExercise.
     *
     * @param exerciseId the id of the modelingExercise to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/modeling-exercises/{exerciseId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<Void> deleteModelingExercise(@PathVariable Long exerciseId) {
        log.info("REST request to delete ModelingExercise : {}", exerciseId);
        var modelingExercise = modelingExerciseRepository.findByIdElseThrow(exerciseId);
        User user = userRepository.getUserWithGroupsAndAuthorities();
        authCheckService.checkHasAtLeastRoleForExerciseElseThrow(Role.INSTRUCTOR, modelingExercise, user);
        // note: we use the exercise service here, because this one makes sure to clean up all lazy references correctly.
        exerciseService.logDeletion(modelingExercise, modelingExercise.getCourseViaExerciseGroupOrCourseMember(), user);
        exerciseService.delete(exerciseId, false, false);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, modelingExercise.getTitle())).build();
    }

    /**
     * POST /modeling-exercises/import: Imports an existing modeling exercise into an existing course
     *
     * This will import the whole exercise except for the participations and Dates.
     * Referenced entities will get cloned and assigned a new id.
     * Uses {@link ModelingExerciseImportService}.
     *
     * @param sourceExerciseId The ID of the original exercise which should get imported
     * @param importedExercise The new exercise containing values that should get overwritten in the imported exercise, s.a. the title or difficulty
     * @throws URISyntaxException When the URI of the response entity is invalid
     *
     * @return The imported exercise (200), a not found error (404) if the template does not exist, or a forbidden error
     *         (403) if the user is not at least an instructor in the target course.
     */
    @PostMapping("/modeling-exercises/import/{sourceExerciseId}")
    @PreAuthorize("hasRole('EDITOR')")
    public ResponseEntity<ModelingExercise> importExercise(@PathVariable long sourceExerciseId, @RequestBody ModelingExercise importedExercise) throws URISyntaxException {
        if (sourceExerciseId <= 0 || (importedExercise.getCourseViaExerciseGroupOrCourseMember() == null && importedExercise.getExerciseGroup() == null)) {
            log.debug("Either the courseId or exerciseGroupId must be set for an import");
            return badRequest();
        }
        final var originalModelingExercise = modelingExerciseRepository.findByIdWithExampleSubmissionsAndResultsElseThrow(sourceExerciseId);
        importedExercise.checkCourseAndExerciseGroupExclusivity("Modeling Exercise");
        var user = userRepository.getUserWithGroupsAndAuthorities();
        authCheckService.checkHasAtLeastRoleForExerciseElseThrow(Role.EDITOR, importedExercise, user);
        authCheckService.checkHasAtLeastRoleForExerciseElseThrow(Role.EDITOR, originalModelingExercise, user);
        exerciseService.validateScoreSettings(importedExercise);

        if (importedExercise.isExamExercise()) {
            log.debug("REST request to import text exercise {} into exercise group {}", sourceExerciseId, importedExercise.getExerciseGroup().getId());
        }
        else {
            log.debug("REST request to import text exercise with {} into course {}", sourceExerciseId, importedExercise.getCourseViaExerciseGroupOrCourseMember().getId());
        }

        final var newModelingExercise = modelingExerciseImportService.importModelingExercise(originalModelingExercise, importedExercise);
        modelingExerciseRepository.save(newModelingExercise);
        return ResponseEntity.created(new URI("/api/modeling-exercises/" + newModelingExercise.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, newModelingExercise.getId().toString())).body(newModelingExercise);
    }

    /**
     * POST /modeling-exercises/:exerciseId/export-submissions : sends exercise submissions as zip
     *
     * @param exerciseId the id of the exercise to get the repos from
     * @param submissionExportOptions the options that should be used for the export
     * @return ResponseEntity with status
     */
    @PostMapping("/modeling-exercises/{exerciseId}/export-submissions")
    @PreAuthorize("hasRole('TA')")
    public ResponseEntity<Resource> exportSubmissions(@PathVariable long exerciseId, @RequestBody SubmissionExportOptionsDTO submissionExportOptions) {
        var modelingExercise = modelingExerciseRepository.findByIdElseThrow(exerciseId);
        authCheckService.checkHasAtLeastRoleForExerciseElseThrow(Role.TEACHING_ASSISTANT, modelingExercise, null);

        // ta's are not allowed to download all participations
        if (submissionExportOptions.isExportAllParticipants() && !authCheckService.isAtLeastInstructorInCourse(modelingExercise.getCourseViaExerciseGroupOrCourseMember(), null)) {
            return forbidden();
        }

        try {
            Optional<File> zipFile = modelingSubmissionExportService.exportStudentSubmissions(exerciseId, submissionExportOptions);

            if (zipFile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .headers(HeaderUtil.createFailureAlert(applicationName, true, ENTITY_NAME, "nosubmissions", "No existing user was specified or no submission exists."))
                        .body(null);
            }

            InputStreamResource resource = new InputStreamResource(new FileInputStream(zipFile.get()));
            return ResponseEntity.ok().contentLength(zipFile.get().length()).contentType(MediaType.APPLICATION_OCTET_STREAM).header("filename", zipFile.get().getName())
                    .body(resource);

        }
        catch (IOException e) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(applicationName, true, ENTITY_NAME, "internalServerError",
                    "There was an error on the server and the zip file could not be created.")).body(null);
        }
    }

    /**
     * GET /modeling-exercises/{exerciseId}/plagiarism-result
     * <p>
     * Return the latest plagiarism result or null, if no plagiarism was detected for this exercise yet.
     *
     * @param exerciseId ID of the modeling exercise for which the plagiarism result should be
     *                   returned
     * @return The ResponseEntity with status 200 (Ok) or with status 400 (Bad Request) if the
     * parameters are invalid
     */
    @GetMapping("/modeling-exercises/{exerciseId}/plagiarism-result")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ModelingPlagiarismResult> getPlagiarismResult(@PathVariable long exerciseId) {
        log.debug("REST request to get the latest plagiarism result for the modeling exercise with id: {}", exerciseId);
        ModelingExercise modelingExercise = modelingExerciseRepository.findByIdWithStudentParticipationsSubmissionsResultsElseThrow(exerciseId);
        authCheckService.checkHasAtLeastRoleForExerciseElseThrow(Role.INSTRUCTOR, modelingExercise, null);
        var plagiarismResult = plagiarismResultRepository.findFirstByExerciseIdOrderByLastModifiedDateDescOrNull(modelingExercise.getId());
        return ResponseEntity.ok((ModelingPlagiarismResult) plagiarismResult);
    }

    /**
     * GET /modeling-exercises/{exerciseId}/check-plagiarism
     * <p>
     * Start the automated plagiarism detection for the given exercise and return its result.
     *
     * @param exerciseId          for which all submission should be checked
     * @param similarityThreshold ignore comparisons whose similarity is below this threshold (%)
     * @param minimumScore        consider only submissions whose score is greater or equal to this
     *                            value
     * @param minimumSize         consider only submissions whose size is greater or equal to this
     *                            value
     * @return the ResponseEntity with status 200 (OK) and the list of at most 500 pair-wise submissions with a similarity above the given threshold (e.g. 50%).
     */
    @GetMapping("/modeling-exercises/{exerciseId}/check-plagiarism")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ModelingPlagiarismResult> checkPlagiarism(@PathVariable long exerciseId, @RequestParam float similarityThreshold, @RequestParam int minimumScore,
            @RequestParam int minimumSize) {
        var modelingExercise = modelingExerciseRepository.findByIdWithStudentParticipationsSubmissionsResultsElseThrow(exerciseId);
        authCheckService.checkHasAtLeastRoleForExerciseElseThrow(Role.INSTRUCTOR, modelingExercise, null);
        long start = System.nanoTime();
        ModelingPlagiarismResult result = modelingPlagiarismDetectionService.compareSubmissions(modelingExercise, similarityThreshold / 100, minimumSize, minimumScore);
        log.info("Finished modelingPlagiarismDetectionService.compareSubmissions call for {} comparisons in {}", result.getComparisons().size(),
                TimeLogUtil.formatDurationFrom(start));
        result.sortAndLimit(500);
        log.info("Limited number of comparisons to {} to avoid performance issues when saving to database", result.getComparisons().size());
        start = System.nanoTime();
        plagiarismResultRepository.savePlagiarismResultAndRemovePrevious(result);
        log.info("Finished plagiarismResultRepository.savePlagiarismResultAndRemovePrevious call in {}", TimeLogUtil.formatDurationFrom(start));
        return ResponseEntity.ok(result);
    }
}
