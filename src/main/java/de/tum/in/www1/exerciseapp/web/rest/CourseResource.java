package de.tum.in.www1.exerciseapp.web.rest;

import com.codahale.metrics.annotation.Timed;
import de.tum.in.www1.exerciseapp.domain.*;
import de.tum.in.www1.exerciseapp.service.CourseService;
import de.tum.in.www1.exerciseapp.web.rest.util.HeaderUtil;
import de.tum.in.www1.exerciseapp.web.rest.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * REST controller for managing Course.
 */
@RestController
@RequestMapping({"/api", "/api_basic"})
@PreAuthorize("hasRole('ADMIN')")
public class CourseResource {

    private final Logger log = LoggerFactory.getLogger(CourseResource.class);

    private static final String ENTITY_NAME = "course";

    private final CourseService courseService;

    public CourseResource(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * POST  /courses : Create a new course.
     *
     * @param course the course to create
     * @return the ResponseEntity with status 201 (Created) and with body the new course, or with status 400 (Bad Request) if the course has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/courses")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Timed
    public ResponseEntity<Course> createCourse(@RequestBody Course course) throws URISyntaxException {
        log.debug("REST request to save Course : {}", course);
        if (course.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(ENTITY_NAME, "idexists", "A new course cannot already have an ID")).body(null);
        }
        Course result = courseService.save(course);
        return ResponseEntity.created(new URI("/api/courses/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /courses : Updates an existing course.
     *
     * @param course the course to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated course,
     * or with status 400 (Bad Request) if the course is not valid,
     * or with status 500 (Internal Server Error) if the course couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/courses")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Timed
    public ResponseEntity<Course> updateCourse(@RequestBody Course course) throws URISyntaxException {
        log.debug("REST request to update Course : {}", course);
        if (course.getId() == null) {
            return createCourse(course);
        }
        Course result = courseService.save(course);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, course.getId().toString()))
            .body(result);
    }

    /**
     * GET  /courses : get all the courses.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of courses in body
     */
    @GetMapping("/courses")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Timed
    public ResponseEntity<List<Course>> getAllCourses(@ApiParam Pageable pageable) {
        log.debug("REST request to get a page of Courses");
        Page<Course> page = courseService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/courses");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

//    /**
//     * GET  /courses : get all the courses.
//     *
//     * @return the ResponseEntity with status 200 (OK) and the list of courses in body
//     */
//    @GetMapping("/courses")
//    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
//    @Timed
//    public List<Course> getAllCourses() {
//        log.debug("REST request to get all Courses");
//        return courseService.findAll();
//    }

    /**
     * GET  /courses/:id : get the "id" course.
     *
     * @param id the id of the course to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the course, or with status 404 (Not Found)
     */
    @GetMapping("/courses/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Timed
    public ResponseEntity<Course> getCourse(@PathVariable Long id) {
        log.debug("REST request to get Course : {}", id);
        Course course = courseService.findOne(id);
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(course));
    }

    /**
     * DELETE  /courses/:id : delete the "id" course.
     *
     * @param id the id of the course to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/courses/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Timed
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        log.debug("REST request to delete Course : {}", id);
        courseService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }


    /**
     * GET /user/:courseId/courseResult
     *
     * @param courseID the Id of the course
     * @return the overall Score a user achieved in a Course
     *      Key is the userId and Value is the actual Score
     */
    @GetMapping("/courses/{courseID}/courseScores")
    @Timed
    @Transactional
    public ResponseEntity<List<studentScore>> courseScores(@PathVariable("courseID") Long courseID){
        log.debug("REST request to get courseScores from course : {}", courseID);
        HashMap<Long, Long> courseResults = new HashMap<>();
        Course course = courseService.findOne(courseID);
        Iterable<Exercise> exercises = course.getExercises();

        for (Exercise e : exercises) {
            Iterable<Participation> participations = e.getParticipations();
            for (Participation p : participations) {

                User student = p.getStudent();
                Iterable<Result> results = p.getResults();
                Result bestResult = null;

                for (Result r : results) {
                    if(bestResult == null){
                        bestResult = r;
                    }else if(bestResult.getScore() == null || r.getScore() == null){
                        continue;
                    }else if(bestResult.getScore() < r.getScore()){
                        bestResult = r;
                    }
                }

                if(bestResult == null || bestResult.getScore() == null){
                    bestResult = new Result();
                    bestResult.setScore( (long) 0);
                }

                if(courseResults.containsKey(student.getId())){
                    Long oldScore = courseResults.get(student.getId());
                    courseResults.replace(student.getId(), (oldScore + bestResult.getScore())/2);
                }else {
                    courseResults.put(student.getId(), bestResult.getScore());
                }
            }
        }

        List<studentScore> formatedCourseResults = new ArrayList<>();
        for(long k : courseResults.keySet()){
            studentScore s = new studentScore();
            s.id = k;
            s.score = courseResults.get(k);
            formatedCourseResults.add(s);
        }

        return ResponseEntity.ok(formatedCourseResults);
    }

    public class studentScore implements Serializable{
        private long id;
        private long score;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getScore() {
            return score;
        }

        public void setScore(long score) {
            this.score = score;
        }
    }

}
