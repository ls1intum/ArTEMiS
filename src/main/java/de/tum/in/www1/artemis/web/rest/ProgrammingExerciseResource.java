package de.tum.in.www1.artemis.web.rest;

import static de.tum.in.www1.artemis.config.Constants.shortNamePattern;
import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.forbidden;
import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.notFound;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.ProgrammingLanguage;
import de.tum.in.www1.artemis.repository.ParticipationRepository;
import de.tum.in.www1.artemis.repository.ProgrammingExerciseRepository;
import de.tum.in.www1.artemis.service.*;
import de.tum.in.www1.artemis.service.connectors.ContinuousIntegrationService;
import de.tum.in.www1.artemis.service.connectors.VersionControlService;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;
import io.github.jhipster.web.util.ResponseUtil;

/** REST controller for managing ProgrammingExercise. */
@RestController
@RequestMapping("/api")
public class ProgrammingExerciseResource {

    private final Logger log = LoggerFactory.getLogger(ProgrammingExerciseResource.class);

    private static final String ENTITY_NAME = "programmingExercise";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ProgrammingExerciseRepository programmingExerciseRepository;

    private final UserService userService;

    private final CourseService courseService;

    private final AuthorizationCheckService authCheckService;

    private final Optional<ContinuousIntegrationService> continuousIntegrationService;

    private final Optional<VersionControlService> versionControlService;

    private final ExerciseService exerciseService;

    private final ProgrammingExerciseService programmingExerciseService;

    private final ParticipationRepository participationRepository;

    private final GroupNotificationService groupNotificationService;

    private final String packageNameRegex = "^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+[0-9a-z_]$";

    private final Pattern packageNamePattern = Pattern.compile(packageNameRegex);

    public ProgrammingExerciseResource(ProgrammingExerciseRepository programmingExerciseRepository, UserService userService, AuthorizationCheckService authCheckService,
            CourseService courseService, Optional<ContinuousIntegrationService> continuousIntegrationService, Optional<VersionControlService> versionControlService,
            ExerciseService exerciseService, ProgrammingExerciseService programmingExerciseService, ParticipationRepository participationRepository,
            GroupNotificationService groupNotificationService) {
        this.programmingExerciseRepository = programmingExerciseRepository;
        this.userService = userService;
        this.courseService = courseService;
        this.authCheckService = authCheckService;
        this.continuousIntegrationService = continuousIntegrationService;
        this.versionControlService = versionControlService;
        this.exerciseService = exerciseService;
        this.programmingExerciseService = programmingExerciseService;
        this.participationRepository = participationRepository;
        this.groupNotificationService = groupNotificationService;
    }

    /**
     * @param exercise the exercise object we want to check for errors
     * @return the error message as response or null if everything is fine
     */
    private ResponseEntity<ProgrammingExercise> checkProgrammingExerciseForError(ProgrammingExercise exercise) {
        if (!continuousIntegrationService.get().buildPlanIdIsValid(exercise.getTemplateBuildPlanId())) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert(applicationName, true, "exercise", "invalid.template.build.plan.id", "The Template Build Plan ID seems to be invalid."))
                    .body(null);
        }
        if (exercise.getTemplateRepositoryUrlAsUrl() == null || !versionControlService.get().repositoryUrlIsValid(exercise.getTemplateRepositoryUrlAsUrl())) {
            return ResponseEntity.badRequest()
                    .headers(
                            HeaderUtil.createFailureAlert(applicationName, true, "exercise", "invalid.template.repository.url", "The Template Repository URL seems to be invalid."))
                    .body(null);
        }
        if (exercise.getSolutionBuildPlanId() != null && !continuousIntegrationService.get().buildPlanIdIsValid(exercise.getSolutionBuildPlanId())) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert(applicationName, true, "exercise", "invalid.solution.build.plan.id", "The Solution Build Plan ID seems to be invalid."))
                    .body(null);
        }
        if (exercise.getSolutionRepositoryUrl() != null && !versionControlService.get().repositoryUrlIsValid(exercise.getSolutionRepositoryUrlAsUrl())) {
            return ResponseEntity.badRequest()
                    .headers(
                            HeaderUtil.createFailureAlert(applicationName, true, "exercise", "invalid.solution.repository.url", "The Solution Repository URL seems to be invalid."))
                    .body(null);
        }
        return null;
    }

    /**
     * POST /programming-exercises : Create a new programmingExercise.
     *
     * @param programmingExercise the programmingExercise to create
     * @return the ResponseEntity with status 201 (Created) and with body the new programmingExercise, or with status 400 (Bad Request) if the programmingExercise has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/programming-exercises")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ProgrammingExercise> createProgrammingExercise(@RequestBody ProgrammingExercise programmingExercise) throws URISyntaxException {
        log.debug("REST request to save ProgrammingExercise : {}", programmingExercise);
        if (programmingExercise.getId() != null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert(applicationName, true, ENTITY_NAME, "idexists", "A new programmingExercise cannot already have an ID")).body(null);
        }
        // fetch course from database to make sure client didn't change groups
        Course course = courseService.findOne(programmingExercise.getCourse().getId());
        if (course == null) {
            return ResponseEntity.badRequest()
                    .headers(
                            HeaderUtil.createFailureAlert(applicationName, true, ENTITY_NAME, "courseNotFound", "The course belonging to this programming exercise does not exist"))
                    .body(null);
        }
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authCheckService.isInstructorInCourse(course, user) && !authCheckService.isAdmin()) {
            return forbidden();
        }

        ResponseEntity<ProgrammingExercise> errorResponse = checkProgrammingExerciseForError(programmingExercise);
        if (errorResponse != null) {
            return errorResponse;
        }

        // we only initiate the programming exercises when creating the links
        programmingExerciseService.initParticipations(programmingExercise);

        // Only save after checking for errors
        programmingExerciseService.saveParticipations(programmingExercise);

        ProgrammingExercise result = programmingExerciseRepository.save(programmingExercise);

        groupNotificationService.notifyTutorGroupAboutExerciseCreated(programmingExercise);
        return ResponseEntity.created(new URI("/api/programming-exercises/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getTitle())).body(result);
    }

    /**
     * POST /programming-exercises/setup : Setup a new programmingExercise (with all needed repositories etc.)
     *
     * @param programmingExercise the programmingExercise to setup
     * @return the ResponseEntity with status 201 (Created) and with body the new programmingExercise, or with status 400 (Bad Request) if the parameters are invalid
     */
    @PostMapping("/programming-exercises/setup")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ProgrammingExercise> setupProgrammingExercise(@RequestBody ProgrammingExercise programmingExercise) {
        log.debug("REST request to setup ProgrammingExercise : {}", programmingExercise);
        if (programmingExercise.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createAlert(applicationName, "A new programmingExercise cannot already have an ID", "idexists")).body(null);
        }

        if (programmingExercise == null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createAlert(applicationName, "The programming exercise is not set", "programmingExerciseNotSet")).body(null);
        }

        if (programmingExercise.getCourse() == null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createAlert(applicationName, "The course is not set", "courseNotSet")).body(null);
        }

        // fetch course from database to make sure client didn't change groups
        Course course = courseService.findOne(programmingExercise.getCourse().getId());
        if (course == null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createAlert(applicationName, "The course belonging to this programming exercise does not exist", "courseNotFound")).body(null);
        }
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authCheckService.isInstructorInCourse(course, user) && !authCheckService.isAdmin()) {
            return forbidden();
        }
        // make sure that we use the values from the database and not the once which might have been
        // altered in the client
        programmingExercise.setCourse(course);

        // Check if exercise title is set
        if (programmingExercise.getTitle() == null || programmingExercise.getTitle().length() < 3) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createAlert(applicationName, "The title of the programming exercise is too short", "programmingExerciseTitleInvalid")).body(null);
        }

        // Check if exercise shortname is set
        if (programmingExercise.getShortName() == null || programmingExercise.getShortName().length() < 3) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createAlert(applicationName, "The shortname of the programming exercise is not set or too short", "programmingExerciseShortnameInvalid"))
                    .body(null);
        }

        // Check if exercise shortname matches regex
        Matcher shortNameMatcher = shortNamePattern.matcher(programmingExercise.getShortName());
        if (!shortNameMatcher.matches()) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createAlert(applicationName, "The shortname is invalid", "shortnameInvalid")).body(null);
        }

        // Check if course shortname is set
        if (course.getShortName() == null || course.getShortName().length() < 3) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createAlert(applicationName, "The shortname of the course is not set or too short", "courseShortnameInvalid"))
                    .body(null);
        }

        // Check if programming language is set
        if (programmingExercise.getProgrammingLanguage() == null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createAlert(applicationName, "No programming language was specified", "programmingLanguageNotSet")).body(null);
        }

        // Check if package name is set
        if (programmingExercise.getProgrammingLanguage() == ProgrammingLanguage.JAVA) {
            // only Java needs a valid package name at the moment
            if (programmingExercise.getPackageName() == null || programmingExercise.getPackageName().length() < 3) {
                return ResponseEntity.badRequest().headers(HeaderUtil.createAlert(applicationName, "The packagename is invalid", "packagenameInvalid")).body(null);
            }

            // Check if package name matches regex
            Matcher packageNameMatcher = packageNamePattern.matcher(programmingExercise.getPackageName());
            if (!packageNameMatcher.matches()) {
                return ResponseEntity.badRequest().headers(HeaderUtil.createAlert(applicationName, "The packagename is invalid", "packagenameInvalid")).body(null);
            }
        }

        // Check if max score is set
        if (programmingExercise.getMaxScore() == null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createAlert(applicationName, "The max score is invalid", "maxscoreInvalid")).body(null);
        }

        String projectKey = programmingExercise.getProjectKey();
        String projectName = programmingExercise.getProjectName();
        String errorMessageVCS = versionControlService.get().checkIfProjectExists(projectKey, projectName);
        if (errorMessageVCS != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createAlert(applicationName, errorMessageVCS, "vcsProjectExists")).body(null);
        }

        String errorMessageCI = continuousIntegrationService.get().checkIfProjectExists(projectKey, projectName);
        if (errorMessageCI != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createAlert(applicationName, errorMessageCI, "ciProjectExists")).body(null);
        }

        try {
            ProgrammingExercise result = programmingExerciseService.setupProgrammingExercise(programmingExercise); // Setup all repositories etc

            groupNotificationService.notifyTutorGroupAboutExerciseCreated(result);
            return ResponseEntity.created(new URI("/api/programming-exercises" + result.getId()))
                    .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getTitle())).body(result);
        }
        catch (Exception e) {
            log.error("Error while setting up programming exercise", e);
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createAlert(applicationName, "An error occurred while setting up the exercise: " + e.getMessage(), "errorProgrammingExercise")).body(null);
        }
    }

    /**
     * PUT /programming-exercises : Updates an existing programmingExercise.
     *
     * @param programmingExercise the programmingExercise to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated programmingExercise, or with status 400 (Bad Request) if the programmingExercise is not valid, or
     *         with status 500 (Internal Server Error) if the programmingExercise couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/programming-exercises")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ProgrammingExercise> updateProgrammingExercise(@RequestBody ProgrammingExercise programmingExercise,
            @RequestParam(value = "notificationText", required = false) String notificationText) throws URISyntaxException {
        log.debug("REST request to update ProgrammingExercise : {}", programmingExercise);
        if (programmingExercise.getId() == null) {
            return createProgrammingExercise(programmingExercise);
        }
        // fetch course from database to make sure client didn't change groups
        Course course = courseService.findOne(programmingExercise.getCourse().getId());
        if (course == null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createAlert(applicationName, "courseNotFound", "The course belonging to this programming exercise does not exist")).body(null);
        }
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authCheckService.isInstructorInCourse(course, user) && !authCheckService.isAdmin()) {
            return forbidden();
        }

        ResponseEntity<ProgrammingExercise> errorResponse = checkProgrammingExerciseForError(programmingExercise);
        if (errorResponse != null) {
            return errorResponse;
        }

        // When updating the participations, we need to make sure that the exercise is attached to each of them.
        // Otherwise we would remove the link between participation and exercise.
        if (programmingExercise.getTemplateParticipation() != null) {
            programmingExercise.getTemplateParticipation().setExercise(programmingExercise);
        }
        if (programmingExercise.getSolutionParticipation() != null) {
            programmingExercise.getSolutionParticipation().setExercise(programmingExercise);
        }
        programmingExercise.getParticipations().forEach(p -> p.setExercise(programmingExercise));
        // Only save after checking for errors
        programmingExerciseService.saveParticipations(programmingExercise);

        ProgrammingExercise result = programmingExerciseRepository.save(programmingExercise);

        if (notificationText != null) {
            groupNotificationService.notifyStudentGroupAboutExerciseUpdate(result, notificationText);
        }
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, programmingExercise.getTitle())).body(result);
    }

    /**
     * PATCH /programming-exercises-problem: Updates the problem statement of the exercise.
     *
     * @param problemStatementUpdate the programmingExercise to update with the new problemStatement
     * @return the ResponseEntity with status 200 (OK) and with body the updated problemStatement, or with status 400 (Bad Request) if the programmingExercise is not valid, or with
     *         status 500 (Internal Server Error) if the programmingExercise couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PatchMapping("/programming-exercises-problem")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ProgrammingExercise> updateProblemStatement(@RequestBody ProblemStatementUpdate problemStatementUpdate,
            @RequestParam(value = "notificationText", required = false) String notificationText) throws URISyntaxException {
        log.debug("REST request to update ProgrammingExercise with new problem statement: {}", problemStatementUpdate);
        // fetch course from database to make sure client didn't change groups
        ProgrammingExercise programmingExercise = (ProgrammingExercise) exerciseService.findOne(problemStatementUpdate.getExerciseId());
        Course course = courseService.findOne(programmingExercise.getCourse().getId());
        if (course == null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createAlert(applicationName, "courseNotFound", "The course belonging to this programming exercise does not exist")).body(null);
        }
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authCheckService.isAtLeastTeachingAssistantInCourse(course, user)) {
            return forbidden();
        }

        ResponseEntity<ProgrammingExercise> errorResponse = checkProgrammingExerciseForError(programmingExercise);
        if (errorResponse != null) {
            return errorResponse;
        }

        programmingExercise.setProblemStatement(problemStatementUpdate.getProblemStatement());

        ProgrammingExercise result = programmingExerciseRepository.save(programmingExercise);
        if (notificationText != null) {
            groupNotificationService.notifyStudentGroupAboutExerciseUpdate(result, notificationText);
        }
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, programmingExercise.getTitle())).body(result);
    }

    /**
     * GET /courses/:courseId/exercises : get all the exercises.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of programmingExercises in body
     */
    @GetMapping(value = "/courses/{courseId}/programming-exercises")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ProgrammingExercise>> getProgrammingExercisesForCourse(@PathVariable Long courseId) {
        log.debug("REST request to get all ProgrammingExercises for the course with id : {}", courseId);
        Course course = courseService.findOne(courseId);
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authCheckService.isTeachingAssistantInCourse(course, user) && !authCheckService.isInstructorInCourse(course, user) && !authCheckService.isAdmin()) {
            return forbidden();
        }
        List<ProgrammingExercise> exercises = programmingExerciseRepository.findByCourseIdWithLatestResultForParticipations(courseId);
        for (Exercise exercise : exercises) {
            // not required in the returned json body
            exercise.setParticipations(null);
            exercise.setCourse(null);
        }
        return ResponseEntity.ok().body(exercises);
    }

    /**
     * GET /programming-exercises/:id : get the "id" programmingExercise.
     *
     * @param id the id of the programmingExercise to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the programmingExercise, or with status 404 (Not Found)
     */
    @GetMapping("/programming-exercises/{id}")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ProgrammingExercise> getProgrammingExercise(@PathVariable Long id) {
        log.debug("REST request to get ProgrammingExercise : {}", id);
        Optional<ProgrammingExercise> programmingExercise = programmingExerciseRepository.findById(id);
        if (programmingExercise.isPresent()) {
            Course course = programmingExercise.get().getCourse();
            User user = userService.getUserWithGroupsAndAuthorities();
            if (!authCheckService.isTeachingAssistantInCourse(course, user) && !authCheckService.isInstructorInCourse(course, user) && !authCheckService.isAdmin()) {
                return forbidden();
            }
        }
        return ResponseUtil.wrapOrNotFound(programmingExercise);
    }

    /**
     * GET /programming-exercises-with-participations/:id : get the "id" programmingExercise.
     *
     * @param id the id of the programmingExercise to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the programmingExercise, or with status 404 (Not Found)
     */
    @GetMapping("/programming-exercises-with-participations/{id}")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ProgrammingExercise> getProgrammingExerciseWithAllParticipations(@PathVariable Long id) {
        log.debug("REST request to get ProgrammingExercise : {}", id);

        User user = userService.getUserWithGroupsAndAuthorities();
        Optional<ProgrammingExercise> programmingExerciseOpt = programmingExerciseRepository.findWithTemplateAndSolutionParticipationById(id);
        if (programmingExerciseOpt.isPresent()) {
            ProgrammingExercise programmingExercise = programmingExerciseOpt.get();
            Course course = programmingExercise.getCourse();

            Optional<Participation> assignmentParticipation = participationRepository.findByExerciseIdAndStudentIdWithLatestResult(programmingExercise.getId(), user.getId());
            Set<Participation> participations = new HashSet<>();
            assignmentParticipation.ifPresent(participations::add);
            programmingExercise.setParticipations(participations);

            if (!authCheckService.isAtLeastInstructorForCourse(course, user)) {
                return forbidden();
            }

            return ResponseEntity.ok(programmingExercise);
        }
        else {
            return notFound();
        }
    }

    /**
     * DELETE /programming-exercises/:id : delete the "id" programmingExercise.
     *
     * @param id the id of the programmingExercise to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/programming-exercises/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Void> deleteProgrammingExercise(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean deleteStudentReposBuildPlans,
            @RequestParam(defaultValue = "false") boolean deleteBaseReposBuildPlans) {
        log.debug("REST request to delete ProgrammingExercise : {}", id);
        Optional<ProgrammingExercise> programmingExercise = programmingExerciseRepository.findById(id);
        if (programmingExercise.isPresent()) {
            Course course = programmingExercise.get().getCourse();
            User user = userService.getUserWithGroupsAndAuthorities();
            if (!authCheckService.isInstructorInCourse(course, user) && !authCheckService.isAdmin()) {
                return forbidden();
            }
            String title = programmingExercise.get().getTitle();
            exerciseService.delete(programmingExercise.get(), deleteStudentReposBuildPlans, deleteBaseReposBuildPlans);
            return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, title)).build();
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Squash all commits into one in the template repository of a given exercise.
     * 
     * @param id of the exercise
     * @return
     */
    @PutMapping(value = "/programming-exercises/{id}/squash-template-commits", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Void> squashTemplateRepositoryCommits(@PathVariable Long id) {
        log.debug("REST request to generate the structure oracle for ProgrammingExercise with id: {}", id);

        Optional<ProgrammingExercise> programmingExerciseOptional = programmingExerciseRepository.findById(id);
        if (!programmingExerciseOptional.isPresent()) {
            return notFound();
        }
        ProgrammingExercise programmingExercise = programmingExerciseOptional.get();

        Course course = courseService.findOne(programmingExercise.getCourse().getId());
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authCheckService.isInstructorInCourse(course, user) && !authCheckService.isAdmin()) {
            return forbidden();
        }

        try {
            URL exerciseRepoURL = programmingExercise.getTemplateRepositoryUrlAsUrl();
            programmingExerciseService.squashAllCommitsOfRepositoryIntoOne(exerciseRepoURL);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        catch (IOException | IllegalStateException | InterruptedException | GitAPIException ex) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /programming-exercises/{id}/generate-tests : Makes a call to StructureOracleGenerator to generate the structure oracle aka the test.json file
     *
     * @param id The ID of the programming exercise for which the structure oracle should get generated
     * @return The ResponseEntity with status 201 (Created) or with status 400 (Bad Request) if the parameters are invalid
     */
    @GetMapping(value = "/programming-exercises/{id}/generate-tests", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<String> generateStructureOracleForExercise(@PathVariable Long id) {
        log.debug("REST request to generate the structure oracle for ProgrammingExercise with id: {}", id);

        if (id == null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createAlert(applicationName, "programmingExerciseNotFound", "The programming exercise does not exist"))
                    .body(null);
        }
        Optional<ProgrammingExercise> programmingExerciseOptional = programmingExerciseRepository.findById(id);
        if (!programmingExerciseOptional.isPresent()) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createAlert(applicationName, "programmingExerciseNotFound", "The programming exercise does not exist"))
                    .body(null);
        }

        ProgrammingExercise programmingExercise = programmingExerciseOptional.get();
        Course course = courseService.findOne(programmingExercise.getCourse().getId());
        if (course == null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createAlert(applicationName, "courseNotFound", "The course belonging to this programming exercise does not exist")).body(null);
        }
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authCheckService.isInstructorInCourse(course, user) && !authCheckService.isAdmin()) {
            return forbidden();
        }
        if (programmingExercise.getPackageName() == null || programmingExercise.getPackageName().length() < 3) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createAlert(applicationName,
                    "This is a linked exercise and generating the structure oracle for this exercise is not possible.", "couldNotGenerateStructureOracle")).body(null);
        }

        URL solutionRepoURL = programmingExercise.getSolutionRepositoryUrlAsUrl();
        URL exerciseRepoURL = programmingExercise.getTemplateRepositoryUrlAsUrl();
        URL testRepoURL = programmingExercise.getTestRepositoryUrlAsUrl();

        try {
            String testsPath = "test" + File.separator + programmingExercise.getPackageFolderName();
            // Atm we only have one folder that can have structural tests, but this could change.
            testsPath = programmingExercise.hasSequentialTestRuns() ? "structural" + File.separator + testsPath : testsPath;
            boolean didGenerateOracle = programmingExerciseService.generateStructureOracleFile(solutionRepoURL, exerciseRepoURL, testRepoURL, testsPath);

            if (didGenerateOracle) {
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(MediaType.TEXT_PLAIN);
                return new ResponseEntity<>("Successfully generated the structure oracle for the exercise " + programmingExercise.getProjectName(), responseHeaders, HttpStatus.OK);
            }
            else {
                return ResponseEntity.badRequest().headers(
                        HeaderUtil.createAlert(applicationName, "Did not update the oracle because there have not been any changes to it.", "didNotGenerateStructureOracle"))
                        .body(null);
            }
        }
        catch (Exception e) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createAlert(applicationName,
                            "An error occurred while generating the structure oracle for the exercise " + programmingExercise.getProjectName() + ": \n" + e.getMessage(),
                            "errorStructureOracleGeneration"))
                    .body(null);
        }
    }
}
