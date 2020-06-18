package de.tum.in.www1.artemis.service;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.exam.Exam;
import de.tum.in.www1.artemis.domain.exam.ExerciseGroup;
import de.tum.in.www1.artemis.domain.exam.StudentExam;
import de.tum.in.www1.artemis.repository.ExamRepository;
import de.tum.in.www1.artemis.repository.StudentExamRepository;
import de.tum.in.www1.artemis.service.dto.StudentDTO;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;
import de.tum.in.www1.artemis.web.rest.errors.EntityNotFoundException;

/**
 * Service Implementation for managing Course.
 */
@Service
public class ExamService {

    private final Logger log = LoggerFactory.getLogger(ExamService.class);

    private final CourseService courseService;

    private final UserService userService;

    private final ExamRepository examRepository;

    private final StudentExamRepository studentExamRepository;

    public ExamService(ExamRepository examRepository, StudentExamRepository studentExamRepository, CourseService courseService, UserService userService) {
        this.examRepository = examRepository;
        this.studentExamRepository = studentExamRepository;
        this.courseService = courseService;
        this.userService = userService;
    }

    /**
     * Save an exam.
     *
     * @param exam the entity to save
     * @return the persisted entity
     */
    public Exam save(Exam exam) {
        log.debug("Request to save exam : {}", exam);
        return examRepository.save(exam);
    }

    /**
     * Get one exam by id.
     *
     * @param examId the id of the entity
     * @return the entity
     */
    @NotNull
    public Exam findOne(Long examId) {
        log.debug("Request to get exam : {}", examId);
        return examRepository.findById(examId).orElseThrow(() -> new EntityNotFoundException("Exam with id: \"" + examId + "\" does not exist"));
    }

    /**
     * Get one exam by id with exercise groups.
     *
     * @param examId the id of the entity
     * @return the exam with exercise groups
     */
    @NotNull
    public Exam findOneWithExerciseGroups(Long examId) {
        log.debug("Request to get exam with exercise groups : {}", examId);
        return examRepository.findWithExerciseGroupsById(examId).orElseThrow(() -> new EntityNotFoundException("Exam with id: \"" + examId + "\" does not exist"));
    }

    /**
     * Get one exam by id with registered users.
     *
     * @param examId the id of the entity
     * @return the exam with registered user
     */
    @NotNull
    public Exam findOneWithRegisteredUsers(Long examId) {
        log.debug("Request to get exam with registered users : {}", examId);
        return examRepository.findWithRegisteredUsersById(examId).orElseThrow(() -> new EntityNotFoundException("Exam with id: \"" + examId + "\" does not exist"));
    }

    /**
     * Get all exams for the given course.
     *
     * @param courseId the id of the course
     * @return the list of all exams
     */
    public List<Exam> findAllByCourseId(Long courseId) {
        log.debug("REST request to get all exams for Course : {}", courseId);
        return examRepository.findByCourseId(courseId);
    }

    /**
     * Get the exam of a course with exercise groups and student exams
     * @param examId {Long} The courseId of the course which contains the exam
     * @return The exam
     */
    public Exam findOneWithExercisesGroupsAndStudentExamsByExamId(Long examId) {
        log.debug("REST request to get the exam with student exams and exercise groups for Id : {}", examId);
        return examRepository.findOneWithEagerExercisesGroupsAndStudentExams(examId);
    }

    /**
     * Delete the exam by id.
     *
     * @param examId the id of the entity
     */
    public void delete(Long examId) {
        log.debug("Request to delete exam : {}", examId);
        examRepository.deleteById(examId);
    }

    /**
     * Filters the visible exams (excluding the ones that are not visible yet)
     *
     * @param exams a set of exams (e.g. the ones of a course)
     * @return only the visible exams
     */
    public Set<Exam> filterVisibleExams(Set<Exam> exams) {
        return exams.stream().filter(Exam::isVisibleToStudents).collect(Collectors.toSet());
    }

    /**
     * Generates the student exams randomly based on the exam configuration and the exercise groups
     *
     * @param examId        the id of the exam
     * @return the list of student exams with their corresponding users
     */
    public List<StudentExam> generateStudentExams(Long examId) {
        List<StudentExam> studentExams = new ArrayList<>();
        SecureRandom random = new SecureRandom();

        Exam exam = examRepository.findWithExercisesRegisteredUsersStudentExamsById(examId).get();
        // TODO: double check that all previously generated student exams are automatically deleted (based on orphan removal)

        for (User registeredUser : exam.getRegisteredUsers()) {
            // create one student exam per user
            StudentExam studentExam = new StudentExam();
            studentExam.setExam(exam);
            studentExam.setUser(registeredUser);

            List<ExerciseGroup> exerciseGroups = exam.getExerciseGroups();

            if (exerciseGroups.size() < exam.getNumberOfExercisesInExam()) {
                throw new BadRequestAlertException("The number of exercise groups is too small", "Exam", "exam.validation.tooFewExerciseGroups");
            }

            long numberOfOptionalExercises = exam.getNumberOfExercisesInExam() - exerciseGroups.stream().filter(ExerciseGroup::getIsMandatory).count();

            if (numberOfOptionalExercises < 0) {
                throw new BadRequestAlertException("The number of mandatory exercise groups is too large", "Exam", "exam.validation.tooManyMandatoryExerciseGroups");
            }

            for (ExerciseGroup exerciseGroup : exam.getExerciseGroups()) {
                if (Boolean.TRUE.equals(exerciseGroup.getIsMandatory())) {
                    // we get one random exercise from all mandatory exercise groups
                    studentExam.addExercise(selectRandomExercise(random, exerciseGroup));
                }
                else {
                    // this is an optional exercise group, we randomly take those if we still have spots left
                    boolean shouldSelectOptionalExercise = random.nextBoolean();
                    if (numberOfOptionalExercises > 0 && shouldSelectOptionalExercise) {
                        studentExam.addExercise(selectRandomExercise(random, exerciseGroup));
                        numberOfOptionalExercises--;
                    }
                }
            }

            // TODO: it can happen that all optional exercises are randomly NOT chosen. We have to make sure that exactly numberOfOptionalExercises is chosen

            if (Boolean.TRUE.equals(exam.getRandomizeExerciseOrder())) {
                Collections.shuffle(studentExam.getExercises());
            }

            studentExams.add(studentExam);
        }

        studentExams = studentExamRepository.saveAll(studentExams);

        // TODO: make sure the student exams still contain non proxy users

        return studentExams;
    }

    public Exercise selectRandomExercise(SecureRandom random, ExerciseGroup exerciseGroup) {
        List<Exercise> exercises = new ArrayList<>(exerciseGroup.getExercises());
        int randomIndex = random.nextInt(exercises.size());
        Exercise randomExercise = exercises.get(randomIndex);
        return randomExercise;
    }

    /**
     * Add multiple users to the students of the exam so that they can access the exam
     * The passed list of UserDTOs must include the registration number (the other entries are currently ignored and can be left out)
     * Note: registration based on other user attributes (e.g. email, name, login) is currently NOT supported
     *
     * This method first tries to find the student in the internal Artemis user database (because the user is most probably already using Artemis).
     * In case the user cannot be found, we additionally search the (TUM) LDAP in case it is configured properly.
     *
     * @param courseId      the id of the course
     * @param examId        the id of the exam
     * @param studentDtos   the list of students (with at least registration number) who should get access to the exam
     * @return the list of students who could not be registered for the exam, because they could NOT be found in the Artemis database and could NOT be found in the TUM LDAP
     */
    public List<StudentDTO> registerStudentsForExam(@PathVariable Long courseId, @PathVariable Long examId, @RequestBody List<StudentDTO> studentDtos) {
        var course = courseService.findOne(courseId);
        var exam = findOneWithRegisteredUsers(examId);
        List<StudentDTO> notFoundStudentsDtos = new ArrayList<>();
        for (var studentDto : studentDtos) {
            var registrationNumber = studentDto.getRegistrationNumber();
            try {
                // 1) we use the registration number and try to find the student in the Artemis user database
                Optional<User> optionalStudent = userService.findUserWithGroupsAndAuthoritiesByRegistrationNumber(registrationNumber);
                if (optionalStudent.isPresent()) {
                    var student = optionalStudent.get();
                    // we only need to add the student to the course group, if the student is not yet part of it, otherwise the student cannot access the exam (within the course)
                    if (!student.getGroups().contains(course.getStudentGroupName())) {
                        userService.addUserToGroup(student, course.getStudentGroupName());
                    }
                    exam.addUser(student);
                    continue;
                }
                // 2) if we cannot find the student, we use the registration number and try to find the student in the (TUM) LDAP, create it in the Artemis DB and in a potential
                // external user management system
                optionalStudent = userService.createUserFromLdap(registrationNumber);
                if (optionalStudent.isPresent()) {
                    var student = optionalStudent.get();
                    // the newly created student needs to get the rights to access the course, otherwise the student cannot access the exam (within the course)
                    userService.addUserToGroup(student, course.getStudentGroupName());
                    exam.addUser(student);
                    continue;
                }
                // 3) if we cannot find the user in the (TUM) LDAP, we report this to the client
                log.warn("User with registration number " + registrationNumber + " not found in Artemis user database and not found in (TUM) LDAP");
            }
            catch (Exception ex) {
                log.warn("Error while processing user with registration number " + registrationNumber + ": " + ex.getMessage(), ex);
            }

            notFoundStudentsDtos.add(studentDto);
        }
        examRepository.save(exam);
        return notFoundStudentsDtos;
    }
}
