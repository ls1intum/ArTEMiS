package de.tum.in.www1.artemis.service;

import static de.tum.in.www1.artemis.domain.Authority.ADMIN_AUTHORITY;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.exam.Exam;
import de.tum.in.www1.artemis.domain.exam.StudentExam;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.repository.CourseRepository;
import de.tum.in.www1.artemis.repository.ExamRepository;
import de.tum.in.www1.artemis.service.dto.StudentDTO;
import de.tum.in.www1.artemis.web.rest.errors.EntityNotFoundException;

/**
 * Service Implementation for registering students in the exam.
 */
@Service
public class ExamRegistrationService {

    private final Logger log = LoggerFactory.getLogger(ExamRegistrationService.class);

    private final UserRetrievalService userRetrievalService;

    private final UserService userService;

    private final ParticipationService participationService;

    private final StudentExamService studentExamService;

    private final AuditEventRepository auditEventRepository;

    private final ExamRepository examRepository;

    private final CourseRepository courseRepository;

    public ExamRegistrationService(ExamRepository examRepository, UserService userService, ParticipationService participationService, UserRetrievalService userRetrievalService,
            AuditEventRepository auditEventRepository, CourseRepository courseRepository, StudentExamService studentExamService) {
        this.examRepository = examRepository;
        this.userService = userService;
        this.userRetrievalService = userRetrievalService;
        this.participationService = participationService;
        this.auditEventRepository = auditEventRepository;
        this.courseRepository = courseRepository;
        this.studentExamService = studentExamService;
    }

    /**
     * Add multiple users to the students of the exam so that they can access the exam
     * The passed list of UserDTOs must include the registration number (the other entries are currently ignored and can be left out)
     * Note: registration based on other user attributes (e.g. email, name, login) is currently NOT supported
     * <p>
     * This method first tries to find the student in the internal Artemis user database (because the user is most probably already using Artemis).
     * In case the user cannot be found, we additionally search the (TUM) LDAP in case it is configured properly.
     *
     * @param courseId      the id of the course
     * @param examId        the id of the exam
     * @param studentDTOs   the list of students (with at least registration number) who should get access to the exam
     * @return the list of students who could not be registered for the exam, because they could NOT be found in the Artemis database and could NOT be found in the TUM LDAP
     */
    public List<StudentDTO> registerStudentsForExam(Long courseId, Long examId, List<StudentDTO> studentDTOs) {
        var course = courseRepository.findById(courseId).orElseThrow(() -> new EntityNotFoundException("Course with id: \"" + courseId + "\" does not exist"));
        var exam = examRepository.findWithRegisteredUsersById(examId).orElseThrow(() -> new EntityNotFoundException("Exam with id: \"" + examId + "\" does not exist"));
        List<StudentDTO> notFoundStudentsDTOs = new ArrayList<>();
        for (var studentDto : studentDTOs) {
            var registrationNumber = studentDto.getRegistrationNumber();
            var login = studentDto.getLogin();
            try {
                // 1) we use the registration number and try to find the student in the Artemis user database
                var optionalStudent = userRetrievalService.findUserWithGroupsAndAuthoritiesByRegistrationNumber(registrationNumber);
                if (optionalStudent.isPresent()) {
                    var student = optionalStudent.get();
                    // we only need to add the student to the course group, if the student is not yet part of it, otherwise the student cannot access the exam (within the
                    // course)
                    if (!student.getGroups().contains(course.getStudentGroupName())) {
                        userService.addUserToGroup(student, course.getStudentGroupName());
                    }
                    exam.addRegisteredUser(student);
                    continue;
                }

                // 2) if we cannot find the student, we use the registration number and try to find the student in the (TUM) LDAP, create it in the Artemis DB and in a
                // potential
                // external user management system
                optionalStudent = userService.createUserFromLdap(registrationNumber);
                if (optionalStudent.isPresent()) {
                    var student = optionalStudent.get();
                    // the newly created student needs to get the rights to access the course, otherwise the student cannot access the exam (within the course)
                    userService.addUserToGroup(student, course.getStudentGroupName());
                    exam.addRegisteredUser(student);
                    continue;
                }

                // 3) if we cannot find the user in the (TUM) LDAP or the registration number was not set properly, try again using the login
                optionalStudent = userRetrievalService.findUserWithGroupsAndAuthoritiesByLogin(login);
                if (optionalStudent.isPresent()) {
                    var student = optionalStudent.get();
                    // the newly created student needs to get the rights to access the course, otherwise the student cannot access the exam (within the course)
                    userService.addUserToGroup(student, course.getStudentGroupName());
                    exam.addRegisteredUser(student);
                    continue;
                }

                log.warn("User with registration number '" + registrationNumber + "' and login '" + login + "' not found in Artemis user database nor found in (TUM) LDAP");
            }
            catch (Exception ex) {
                log.warn("Error while processing user with registration number " + registrationNumber + ": " + ex.getMessage(), ex);
            }

            notFoundStudentsDTOs.add(studentDto);
        }
        examRepository.save(exam);

        try {
            User currentUser = userRetrievalService.getUserWithGroupsAndAuthorities();
            Map<String, Object> userData = new HashMap<>();
            userData.put("exam", exam.getTitle());
            for (var i = 0; i < studentDTOs.size(); i++) {
                var studentDTO = studentDTOs.get(i);
                userData.put("student" + i, studentDTO.toDatabaseString());
            }
            AuditEvent auditEvent = new AuditEvent(currentUser.getLogin(), Constants.ADD_USER_TO_EXAM, userData);
            auditEventRepository.add(auditEvent);
            log.info("User " + currentUser.getLogin() + " has added multiple users " + studentDTOs + " to the exam " + exam.getTitle() + " with id " + exam.getId());
        }
        catch (Exception ex) {
            log.warn("Could not add audit event to audit log", ex);
        }

        return notFoundStudentsDTOs;
    }

    /**
     * Returns <code>true</code> if the current user is registered for the exam
     *
     * @param examId the id of the exam
     * @return <code>true</code> if the user if registered for the exam, false if this is not the case or the exam does not exist
     */
    public boolean isCurrentUserRegisteredForExam(Long examId) {
        return isUserRegisteredForExam(examId, userRetrievalService.getUser().getId());
    }

    /**
     * Returns <code>true</code> if the user with the given id is registered for the exam
     *
     * @param examId the id of the exam
     * @param userId the id of the user to check
     * @return <code>true</code> if the user if registered for the exam, false if this is not the case or the exam does not exist
     */
    public boolean isUserRegisteredForExam(Long examId, Long userId) {
        return examRepository.isUserRegisteredForExam(examId, userId);
    }

    /**
     * Registers student to the exam. In order to do this,  we add the user the the course group, because the user only has access to the exam of a course if the student also has access to the course of the exam.
     * We only need to add the user to the course group, if the student is not yet part of it, otherwise the student cannot access the exam (within the course).
     *
     * @param course  the course containing the exam
     * @param exam    the exam for which we want to register a student
     * @param student the student to be registered to the exam
     */
    public void registerStudentToExam(Course course, Exam exam, User student) {
        exam.addRegisteredUser(student);

        if (!student.getGroups().contains(course.getStudentGroupName())) {
            userService.addUserToGroup(student, course.getStudentGroupName());
        }
        examRepository.save(exam);

        User currentUser = userRetrievalService.getUserWithGroupsAndAuthorities();
        AuditEvent auditEvent = new AuditEvent(currentUser.getLogin(), Constants.ADD_USER_TO_EXAM, "exam=" + exam.getTitle(), "student=" + student.getLogin());
        auditEventRepository.add(auditEvent);
        log.info("User " + currentUser.getLogin() + " has added user " + student.getLogin() + " to the exam " + exam.getTitle() + " with id " + exam.getId());
    }

    /**
     *
     * @param examId the exam for which a student should be unregistered
     * @param deleteParticipationsAndSubmission whether the participations and submissions of the student should be deleted
     * @param student the user object that should be unregistered
     */
    public void unregisterStudentFromExam(Long examId, boolean deleteParticipationsAndSubmission, User student) {
        var exam = examRepository.findWithRegisteredUsersById(examId).orElseThrow(() -> new EntityNotFoundException("Exam with id: \"" + examId + "\" does not exist"));
        exam.removeRegisteredUser(student);

        // Note: we intentionally do not remove the user from the course, because the student might just have "unregistered" from the exam, but should
        // still have access to the course.
        examRepository.save(exam);

        // The student exam might already be generated, then we need to delete it
        Optional<StudentExam> optionalStudentExam = studentExamService.findOneWithExercisesByUserIdAndExamIdOptional(student.getId(), exam.getId());
        if (optionalStudentExam.isPresent()) {
            StudentExam studentExam = optionalStudentExam.get();

            // Optionally delete participations and submissions
            if (deleteParticipationsAndSubmission) {
                List<StudentParticipation> participations = participationService.findByStudentExamWithEagerSubmissionsResult(studentExam);
                for (var participation : participations) {
                    participationService.delete(participation.getId(), true, true);
                }
            }

            // Delete the student exam
            studentExamService.deleteStudentExam(studentExam.getId());
        }

        User currentUser = userRetrievalService.getUserWithGroupsAndAuthorities();
        AuditEvent auditEvent = new AuditEvent(currentUser.getLogin(), Constants.REMOVE_USER_FROM_EXAM, "exam=" + exam.getTitle(), "user=" + student.getLogin());
        auditEventRepository.add(auditEvent);
        log.info("User " + currentUser.getLogin() + " has removed user " + student.getLogin() + " from the exam " + exam.getTitle() + " with id " + exam.getId()
                + ". This also deleted a potentially existing student exam with all its participations and submissions.");
    }

    /**
     * Adds all students registered in the course to the given exam
     *
     * @param courseId Id of the course
     * @param examId Id of the exam
     */
    public void addAllStudentsOfCourseToExam(Long courseId, Long examId) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new EntityNotFoundException("Course with id: \"" + courseId + "\" does not exist"));
        var students = userRetrievalService.getStudents(course);
        var examOpt = examRepository.findWithRegisteredUsersById(examId);

        if (examOpt.isPresent()) {
            Exam exam = examOpt.get();
            students.forEach(student -> {
                if (!exam.getRegisteredUsers().contains(student) && !student.getAuthorities().contains(ADMIN_AUTHORITY)
                        && !student.getGroups().contains(course.getInstructorGroupName())) {
                    exam.addRegisteredUser(student);
                }
            });
            examRepository.save(exam);
        }

    }
}
