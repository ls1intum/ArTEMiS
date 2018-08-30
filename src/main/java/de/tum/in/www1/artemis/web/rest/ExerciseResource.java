package de.tum.in.www1.artemis.web.rest;

import com.codahale.metrics.annotation.Timed;
import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.repository.ExerciseRepository;
import de.tum.in.www1.artemis.service.*;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;
import de.tum.in.www1.artemis.web.rest.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * REST controller for managing Exercise.
 */
@RestController
@RequestMapping({"/api", "/api_basic"})
@PreAuthorize("hasRole('ADMIN')")
public class ExerciseResource {

    private final Logger log = LoggerFactory.getLogger(ExerciseResource.class);

    private static final String ENTITY_NAME = "exercise";

    private final ExerciseRepository exerciseRepository;
    private final ExerciseService exerciseService;
    private final UserService userService;
    private final CourseService courseService;
    private final AuthorizationCheckService authCheckService;
    private final Optional<ContinuousIntegrationService> continuousIntegrationService;
    private final Optional<VersionControlService> versionControlService;

    public ExerciseResource(ExerciseRepository exerciseRepository, ExerciseService exerciseService,
                            UserService userService, CourseService courseService, AuthorizationCheckService authCheckService,
                            Optional<ContinuousIntegrationService> continuousIntegrationService, Optional<VersionControlService> versionControlService) {
        this.exerciseRepository = exerciseRepository;
        this.exerciseService = exerciseService;
        this.userService = userService;
        this.courseService = courseService;
        this.authCheckService = authCheckService;
        this.continuousIntegrationService = continuousIntegrationService;
        this.versionControlService = versionControlService;
    }

    /**
     * GET  /exercises : get all the exercises.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of exercises in body
     */
    @GetMapping("/exercises")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    @Timed
    public ResponseEntity<List<Exercise>> getAllExercises(Pageable pageable) {
        log.debug("REST request to get a page of Exercises");
        Page<Exercise> page = exerciseRepository.findAll(pageable);
        User user = userService.getUserWithGroupsAndAuthorities();
        Stream<Exercise> authorizedExercises = page.getContent().stream().filter(
            exercise -> {
                Course course = exercise.getCourse();
                return authCheckService.isTeachingAssistantInCourse(course, user) ||
                    authCheckService.isInstructorInCourse(course, user) ||
                    authCheckService.isAdmin();
            }
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/exercises");
        return new ResponseEntity<>(authorizedExercises.collect(Collectors.toList()), headers, HttpStatus.OK);
    }

    /**
     * GET /courses/:courseId/exercises : get all exercises for the given course
     *
     * @param courseId the course for which to retrieve all exercises
     * @return the ResponseEntity with status 200 (OK) and the list of exercises in body
     */
    @GetMapping(value = "/courses/{courseId}/exercises")
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    @Timed
    public ResponseEntity<Collection<Exercise>> getExercisesForCourse(@PathVariable Long courseId, @RequestParam(defaultValue = "false") boolean withLtiOutcomeUrlExisting, Principal principal) {
        log.debug("REST request to get Exercises for Course : {}", courseId);

        Course course = courseService.findOne(courseId);
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authCheckService.isStudentInCourse(course, user) &&
            !authCheckService.isTeachingAssistantInCourse(course, user) &&
            !authCheckService.isInstructorInCourse(course, user) &&
            !authCheckService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Exercise> result = exerciseService.findAllForCourse(course, withLtiOutcomeUrlExisting, principal, user);

        return ResponseEntity.ok(result);
    }

    /**
     * GET  /exercises/:id : get the "id" exercise.
     *
     * @param id the id of the exercise to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the exercise, or with status 404 (Not Found)
     */
    @GetMapping("/exercises/{id}")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    @Timed
    public ResponseEntity<Exercise> getExercise(@PathVariable Long id) {
        log.debug("REST request to get Exercise : {}", id);
        Exercise exercise = exerciseService.findOne(id);
        Course course = exercise.getCourse();
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authCheckService.isTeachingAssistantInCourse(course, user) &&
             !authCheckService.isInstructorInCourse(course, user) &&
             !authCheckService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(exercise));
    }

    /**
     * DELETE  /exercises/:id : delete the "id" exercise.
     *
     * @param id the id of the exercise to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/exercises/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Timed
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("REST request to delete Exercise : {}", id);
        Exercise exercise = exerciseService.findOneLoadParticipations(id);
        if (Optional.ofNullable(exercise).isPresent()) {
            Course course = exercise.getCourse();
            User user = userService.getUserWithGroupsAndAuthorities();
            if (!authCheckService.isInstructorInCourse(course, user) && !authCheckService.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            exerciseService.reset(exercise);
        }
        exerciseService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * DELETE  /exercises/:id/participations : delete all participations of "id" exercise (reset).
     *
     * @param id the id of the exercise to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping(value = "/exercises/{id}/participations")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Timed
    public ResponseEntity<Void> reset(@PathVariable Long id) {
        log.debug("REST request to reset Exercise : {}", id);
        Exercise exercise = exerciseService.findOneLoadParticipations(id);
        Course course = exercise.getCourse();
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authCheckService.isInstructorInCourse(course, user) && !authCheckService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        exerciseService.reset(exercise);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert("exercise", id.toString())).build();
    }

    /**
     * GET  /exercises/:id/cleanup : delete all build plans (except BASE) of all participations belonging to this exercise. Optionally delete and archive all repositories
     *
     * @param id                 the id of the exercise to delete build plans for
     * @param deleteRepositories whether repositories should be deleted or not
     * @return ResponseEntity with status
     */
    @DeleteMapping(value = "/exercises/{id}/cleanup")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Timed
    public ResponseEntity<Resource> cleanup(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean deleteRepositories) throws IOException {
        log.info("Start to cleanup build plans for Exercise: {}, delete repositories: {}", id, deleteRepositories);
        Exercise exercise = exerciseService.findOne(id);
        Course course = exercise.getCourse();
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authCheckService.isInstructorInCourse(course, user) && !authCheckService.isAdmin()) {
             return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        exerciseService.cleanup(id, deleteRepositories);
        log.info("Cleanup build plans was successful for Exercise : {}", id);
        return ResponseEntity.ok().headers(HeaderUtil.createAlert("Cleanup was successful. Repositories have been deleted: " + deleteRepositories, "")).build();
    }


    /**
     * GET  /exercises/:id/archive : archive all repositories (except BASE) of all participations belonging to this exercise into a zip file and provide a downloadable link.
     *
     * @param id the id of the exercise to delete and archive the repositories
     * @return ResponseEntity with status
     */
    @GetMapping(value = "/exercises/{id}/archive")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Timed
    public ResponseEntity<Resource> archiveRepositories(@PathVariable Long id) throws IOException {
        log.info("Start to archive repositories for Exercise : {}", id);
        Exercise exercise = exerciseService.findOne(id);
        Course course = exercise.getCourse();
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authCheckService.isInstructorInCourse(course, user) && !authCheckService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        File zipFile = exerciseService.archive(id);
        if (zipFile == null) {
            return ResponseEntity.noContent()
                .headers(HeaderUtil.createAlert("There was an error on the server and the zip file could not be created, possibly because all repositories have already been deleted or this is not a programming exercise.", ""))
                .build();
        }
        InputStreamResource resource = new InputStreamResource(new FileInputStream(zipFile));
        log.info("Archive repositories was successful for Exercise : {}", id);
        return ResponseEntity.ok()
            .contentLength(zipFile.length())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header("filename", zipFile.getName())
            .body(resource);
    }
    /**
    * GET  /exercises/:exerciseId/participations/:studentIds : sends all submissions from studentlist as zip
        *
        * @param exerciseId the id of the exercise to get the repos from
        * @param studentIds the studentIds seperated via semicolon to get their submissions
     * @return ResponseEntity with status
     */
    @GetMapping(value = "/exercises/{exerciseId}/participations/{studentIds}")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    @Timed
    public ResponseEntity<Resource> exportSubmissions(@PathVariable Long exerciseId, @PathVariable String studentIds) throws IOException {
        studentIds = studentIds.replaceAll(" ","");
        Exercise exercise = exerciseService.findOne(exerciseId);
        Course course = exercise.getCourse();
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authCheckService.isTeachingAssistantInCourse(course, user) &&
            !authCheckService.isInstructorInCourse(course, user) &&
            !authCheckService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<String> studentList = Arrays.asList(studentIds.split("\\s*,\\s*"));
        if(studentList.isEmpty() || studentList == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(HeaderUtil.createAlert("Given studentlist for export was empty or malformed","")).build();
        }

        File zipFile = exerciseService.exportParticipations(exerciseId,studentList);
        if (zipFile == null) {
            return ResponseEntity.noContent()
                .headers(HeaderUtil.createAlert("There was an error on the server and the zip file could not be created", ""))
                .build();
        }
        InputStreamResource resource = new InputStreamResource(new FileInputStream(zipFile));

        return ResponseEntity.ok()
            .contentLength(zipFile.length())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header("filename", zipFile.getName())
            .body(resource);
    }

}
