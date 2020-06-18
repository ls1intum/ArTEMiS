package de.tum.in.www1.artemis.web.rest;

import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.*;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.tum.in.www1.artemis.domain.exam.StudentExam;
import de.tum.in.www1.artemis.service.*;

/**
 * REST controller for managing ExerciseGroup.
 */
@RestController
@RequestMapping("/api")
public class StudentExamResource {

    private final Logger log = LoggerFactory.getLogger(StudentExamResource.class);

    private final ExamAccessService examAccessService;

    private final StudentExamService studentExamService;

    private final StudentExamAccessService studentExamAccessService;

    public StudentExamResource(ExamAccessService examAccessService, StudentExamService studentExamService, StudentExamAccessService studentExamAccessService) {
        this.examAccessService = examAccessService;
        this.studentExamService = studentExamService;
        this.studentExamAccessService = studentExamAccessService;
    }

    /**
     * GET /courses/{courseId}/exams/{examId}/studentExams/{studentExamId} : Find a student exam by id.
     *
     * @param courseId      the course to which the student exam belongs to
     * @param examId        the exam to which the student exam belongs to
     * @param studentExamId the id of the student exam to find
     * @return the ResponseEntity with status 200 (OK) and with the found student exam as body
     */
    @GetMapping("/courses/{courseId}/exams/{examId}/studentExams/{studentExamId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<StudentExam> getStudentExam(@PathVariable Long courseId, @PathVariable Long examId, @PathVariable Long studentExamId) {
        log.debug("REST request to get student exam : {}", studentExamId);
        Optional<ResponseEntity<StudentExam>> accessFailure = examAccessService.checkCourseAndExamAndStudentExamAccess(courseId, examId, studentExamId);
        return accessFailure.orElseGet(() -> ResponseEntity.ok(studentExamService.findOne(studentExamId)));
    }

    /**
     * GET /courses/{courseId}/exams/{examId}/studentExams : Get all student exams for the given exam
     *
     * @param courseId  the course to which the student exams belong to
     * @param examId    the exam to which the student exams belong to
     * @return the ResponseEntity with status 200 (OK) and a list of student exams. The list can be empty
     */
    @GetMapping("/courses/{courseId}/exams/{examId}/studentExams")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<List<StudentExam>> getStudentExamsForExam(@PathVariable Long courseId, @PathVariable Long examId) {
        log.debug("REST request to get all student exams for exam : {}", examId);
        Optional<ResponseEntity<List<StudentExam>>> courseAndExamAccessFailure = examAccessService.checkCourseAndExamAccess(courseId, examId);
        return courseAndExamAccessFailure.orElseGet(() -> ResponseEntity.ok(studentExamService.findAllByExamId(examId)));
    }

    /**
     * GET /courses/{courseId}/exams/{examId}/studentExams/{studentExamId}/conduction : Find a student exam by id.
     * This will be used for the actual conduction of the exam. The student exam will be returned with the exercises
     * and with the student participation and with the submissions.
     *
     * @param courseId      the course to which the student exam belongs to
     * @param examId        the exam to which the student exam belongs to
     * @param studentExamId the id of the student exam to find
     * @return the ResponseEntity with status 200 (OK) and with the found student exam as body
     */
    @GetMapping("/courses/{courseId}/exams/{examId}/studentExams/{studentExamId}/conduction")
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<StudentExam> getStudentExamForConduction(@PathVariable Long courseId, @PathVariable Long examId, @PathVariable Long studentExamId) {
        log.debug("REST request to get student exam : {}", studentExamId);
        return studentExamAccessService.checkAndGetStudentExamAccessWithExercises(courseId, examId, studentExamId);
    }
}
