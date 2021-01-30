package de.tum.in.www1.artemis;

import de.tum.in.www1.artemis.connector.jira.JiraRequestMockProvider;
import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.DiagramType;
import de.tum.in.www1.artemis.domain.enumeration.IncludedInOverallScore;
import de.tum.in.www1.artemis.domain.exam.Exam;
import de.tum.in.www1.artemis.domain.exam.ExerciseGroup;
import de.tum.in.www1.artemis.domain.exam.StudentExam;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.domain.modeling.ModelingSubmission;
import de.tum.in.www1.artemis.domain.participation.Participation;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.security.SecurityUtils;
import de.tum.in.www1.artemis.service.ExamService;
import de.tum.in.www1.artemis.service.dto.StudentDTO;
import de.tum.in.www1.artemis.service.ldap.LdapUserDto;
import de.tum.in.www1.artemis.util.ModelFactory;
import de.tum.in.www1.artemis.web.rest.dto.ExamInformationDTO;
import de.tum.in.www1.artemis.web.rest.dto.ExamScoresDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.util.LinkedMultiValueMap;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.ZonedDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class ExamIntegrationTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    @Autowired
    JiraRequestMockProvider jiraRequestMockProvider;

    @Autowired
    CourseRepository courseRepo;

    @Autowired
    ExerciseRepository exerciseRepo;

    @Autowired
    UserRepository userRepo;

    @Autowired
    ExamRepository examRepository;

    @Autowired
    ExamService examService;

    @Autowired
    ExerciseGroupRepository exerciseGroupRepository;

    @Autowired
    StudentExamRepository studentExamRepository;

    @Autowired
    TextExerciseRepository textExerciseRepository;

    @Autowired
    ProgrammingExerciseRepository programmingExerciseRepository;

    @Autowired
    StudentParticipationRepository studentParticipationRepository;

    @Autowired
    SubmissionRepository submissionRepository;

    @Autowired
    ResultRepository resultRepository;

    @Autowired
    ParticipationTestRepository participationTestRepository;

    private List<User> users;

    private Course course1;

    private Course course2;

    private Exam exam1;

    private Exam exam2;

    @BeforeEach
    public void initTestCase() {
        users = database.addUsers(10, 5, 1);
        course1 = database.addEmptyCourse();
        course2 = database.addEmptyCourse();
        exam1 = database.addExam(course1);
        exam2 = database.addExamWithExerciseGroup(course1, true);

        // Add users that are not in the course
        userRepo.save(ModelFactory.generateActivatedUser("student42"));
        userRepo.save(ModelFactory.generateActivatedUser("tutor6"));
        userRepo.save(ModelFactory.generateActivatedUser("instructor6"));
    }

    @AfterEach
    public void resetDatabase() {
        database.resetDatabase();
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testRegisterUserInExam_addedToCourseStudentsGroup() throws Exception {
        jiraRequestMockProvider.enableMockingOfRequests();
        jiraRequestMockProvider.mockAddUserToGroup(course1.getStudentGroupName());

        List<User> studentsInCourseBefore = userRepo.findAllInGroup(course1.getStudentGroupName());
        request.postWithoutLocation("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/students/student42", null, HttpStatus.OK, null);
        SecurityUtils.setAuthorizationObject(); // TODO: Why do we need this
        List<User> studentsInCourseAfter = userRepo.findAllInGroup(course1.getStudentGroupName());
        User student42 = database.getUserByLogin("student42");
        studentsInCourseBefore.add(student42);
        assertThat(studentsInCourseBefore).containsExactlyInAnyOrderElementsOf(studentsInCourseAfter);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testRegisterUsersInExam() throws Exception {
        jiraRequestMockProvider.enableMockingOfRequests();

        var exam = ModelFactory.generateExam(course1);
        var savedExam = examRepository.save(exam);
        var student1 = database.getUserByLogin("student1");
        var student2 = database.getUserByLogin("student2");
        var student3 = database.getUserByLogin("student3");
        var student5 = database.getUserByLogin("student5");
        var student6 = database.getUserByLogin("student6");
        var student7 = database.getUserByLogin("student7");
        var student8 = database.getUserByLogin("student8");
        var student9 = database.getUserByLogin("student9");
        var student10 = database.getUserByLogin("student10");
        var registrationNumber1 = "1111111";
        var registrationNumber2 = "1111112";
        var registrationNumber3 = "1111113";
        var registrationNumber3WithTypo = registrationNumber3 + "0";
        var registrationNumber5 = "1111115";
        var registrationNumber5WithTypo = registrationNumber5 + "1";
        var registrationNumber6 = "1111116";
        var registrationNumber99 = "1111199";
        var registrationNumber100 = "1111100";
        var emptyRegistrationNumber = "";
        student1.setRegistrationNumber(registrationNumber1);
        student2.setRegistrationNumber(registrationNumber2);
        student3.setRegistrationNumber(registrationNumber3);
        student5.setRegistrationNumber(registrationNumber5);
        student6.setRegistrationNumber(registrationNumber6);
        student7.setRegistrationNumber(null);
        student8.setRegistrationNumber("");
        student9.setRegistrationNumber(" ");
        student10.setRegistrationNumber(null);
        student1 = userRepo.save(student1);
        student2 = userRepo.save(student2);
        userRepo.save(student3);
        userRepo.save(student5);
        userRepo.save(student6);
        userRepo.save(student7);
        userRepo.save(student8);
        userRepo.save(student9);
        userRepo.save(student10);

        // mock the ldap service
        doReturn(Optional.empty()).when(ldapUserService).findByRegistrationNumber(registrationNumber3WithTypo);
        doReturn(Optional.empty()).when(ldapUserService).findByRegistrationNumber(emptyRegistrationNumber);
        doReturn(Optional.empty()).when(ldapUserService).findByRegistrationNumber(registrationNumber5WithTypo);
        var ldapUser100Dto = new LdapUserDto().registrationNumber(registrationNumber100).firstName("Student100").lastName("Student100").username("student100")
                .email("student100@tum.de");
        doReturn(Optional.of(ldapUser100Dto)).when(ldapUserService).findByRegistrationNumber(registrationNumber100);

        // first and second mocked calls are expected to add student 5 and 99 to the course students
        jiraRequestMockProvider.mockAddUserToGroup(course1.getStudentGroupName());
        jiraRequestMockProvider.mockAddUserToGroup(course1.getStudentGroupName());
        // third mocked call expected to create student 100
        jiraRequestMockProvider.mockCreateUserInExternalUserManagement(ldapUser100Dto.getUsername(), ldapUser100Dto.getFirstName() + " " + ldapUser100Dto.getLastName(),
                ldapUser100Dto.getEmail());
        // the last two mocked calls are expected to add students 100, 6, 7, 8, and 9 to the course student group
        for (int i = 0; i < 5; i++) {
            jiraRequestMockProvider.mockAddUserToGroup(course1.getStudentGroupName());
        }

        var student99 = ModelFactory.generateActivatedUser("student99");     // not registered for the course
        student99.setRegistrationNumber(registrationNumber99);
        userRepo.save(student99);
        student99 = userRepo.findOneWithGroupsAndAuthoritiesByLogin("student99").get();
        assertThat(student99.getGroups()).doesNotContain(course1.getStudentGroupName());

        // Note: student100 is not yet a user of Artemis and should be retrieved from the LDAP

        request.postWithoutLocation("/api/courses/" + course1.getId() + "/exams/" + savedExam.getId() + "/students/student1", null, HttpStatus.OK, null);
        request.postWithoutLocation("/api/courses/" + course1.getId() + "/exams/" + savedExam.getId() + "/students/nonExistingStudent", null, HttpStatus.NOT_FOUND, null);

        Exam storedExam = examRepository.findWithRegisteredUsersById(savedExam.getId()).get();
        assertThat(storedExam.getRegisteredUsers()).containsExactly(student1);

        request.delete("/api/courses/" + course1.getId() + "/exams/" + savedExam.getId() + "/students/student1", HttpStatus.OK);
        request.delete("/api/courses/" + course1.getId() + "/exams/" + savedExam.getId() + "/students/nonExistingStudent", HttpStatus.NOT_FOUND);
        storedExam = examRepository.findWithRegisteredUsersById(savedExam.getId()).get();
        assertThat(storedExam.getRegisteredUsers()).isEmpty();

        var studentDto1 = new StudentDTO().registrationNumber(registrationNumber1);
        var studentDto2 = new StudentDTO().registrationNumber(registrationNumber2);
        var studentDto3 = new StudentDTO().registrationNumber(registrationNumber3WithTypo); // explicit typo, should be a registration failure later
        var studentDto5 = new StudentDTO().registrationNumber(registrationNumber5WithTypo); // explicit typo, should fall back to login name later
        studentDto5.setLogin(student5.getLogin());
        var studentDto7 = new StudentDTO();
        studentDto7.setLogin(student7.getLogin());
        var studentDto8 = new StudentDTO();
        studentDto8.setLogin(student8.getLogin());
        var studentDto9 = new StudentDTO();
        studentDto9.setLogin(student9.getLogin());
        var studentDto10 = new StudentDTO();    // completely empty

        var studentDto99 = new StudentDTO().registrationNumber(registrationNumber99);
        var studentDto100 = new StudentDTO().registrationNumber(registrationNumber100);

        // Add a student with login but empty registration number
        var studentDto6 = new StudentDTO().registrationNumber(emptyRegistrationNumber);
        studentDto6.setLogin(student6.getLogin());
        var studentsToRegister = List.of(studentDto1, studentDto2, studentDto3, studentDto5, studentDto99, studentDto100, studentDto6, studentDto7, studentDto8, studentDto9,
                studentDto10);

        // now we register all these students for the exam.
        List<StudentDTO> registrationFailures = request.postListWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + savedExam.getId() + "/students",
                studentsToRegister, StudentDTO.class, HttpStatus.OK);
        assertThat(registrationFailures).containsExactlyInAnyOrder(studentDto3, studentDto10);
        storedExam = examRepository.findWithRegisteredUsersById(savedExam.getId()).get();

        // now a new user student100 should exist
        var student100 = database.getUserByLogin("student100");

        assertThat(storedExam.getRegisteredUsers()).containsExactlyInAnyOrder(student1, student2, student5, student99, student100, student6, student7, student8, student9);

        for (var user : storedExam.getRegisteredUsers()) {
            // all registered users must have access to the course
            user = userRepo.findOneWithGroupsAndAuthoritiesByLogin(user.getLogin()).get();
            assertThat(user.getGroups()).contains(course1.getStudentGroupName());
        }

        // Make sure delete also works if so many objects have been created before
        request.delete("/api/courses/" + course1.getId() + "/exams/" + savedExam.getId(), HttpStatus.OK);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testStartExercisesWithTextExercise() throws Exception {

        // TODO IMPORTANT test more complex exam configurations (mixed exercise type, more variants and more registered students)

        // registering users
        var student1 = database.getUserByLogin("student1");
        var student2 = database.getUserByLogin("student2");
        var registeredUsers = Set.of(student1, student2);
        exam2.setRegisteredUsers(registeredUsers);
        // setting dates
        exam2.setStartDate(now().plusHours(2));
        exam2.setEndDate(now().plusHours(3));
        exam2.setVisibleDate(now().plusHours(1));

        // creating exercise
        ExerciseGroup exerciseGroup = exam2.getExerciseGroups().get(0);

        TextExercise textExercise = ModelFactory.generateTextExerciseForExam(exerciseGroup);
        exerciseGroup.addExercise(textExercise);
        exerciseGroupRepository.save(exerciseGroup);
        textExercise = exerciseRepo.save(textExercise);

        List<StudentExam> createdStudentExams = new ArrayList<>();

        // creating student exams
        for (User user : registeredUsers) {
            StudentExam studentExam = new StudentExam();
            studentExam.addExercise(textExercise);
            studentExam.setUser(user);
            exam2.addStudentExam(studentExam);
            createdStudentExams.add(studentExamRepository.save(studentExam));
        }

        exam2 = examRepository.save(exam2);

        // invoke start exercises
        Integer noGeneratedParticipations = request.postWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam2.getId() + "/student-exams/start-exercises",
                Optional.empty(), Integer.class, HttpStatus.OK);
        assertThat(noGeneratedParticipations).isEqualTo(exam2.getStudentExams().size());
        List<Participation> studentParticipations = participationTestRepository.findAllWithSubmissions();

        for (Participation participation : studentParticipations) {
            assertThat(participation.getExercise()).isEqualTo(textExercise);
            assertThat(participation.getExercise().getCourseViaExerciseGroupOrCourseMember()).isNotNull();
            assertThat(participation.getExercise().getExerciseGroup()).isEqualTo(exam2.getExerciseGroups().get(0));
            assertThat(participation.getSubmissions()).hasSize(1);
            var textSubmission = (TextSubmission) participation.getSubmissions().iterator().next();
            assertThat(textSubmission.getText()).isNull();
        }

        // Cleanup of Bidirectional Relationships
        for (StudentExam studentExam : createdStudentExams) {
            exam2.removeStudentExam(studentExam);
        }
        examRepository.save(exam2);

    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testStartExercisesWithModelingExercise() throws Exception {
        // TODO IMPORTANT test more complex exam configurations (mixed exercise type, more variants and more registered students)

        // registering users
        var student1 = database.getUserByLogin("student1");
        var student2 = database.getUserByLogin("student2");
        var registeredUsers = Set.of(student1, student2);
        exam2.setRegisteredUsers(registeredUsers);
        // setting dates
        exam2.setStartDate(now().plusHours(2));
        exam2.setEndDate(now().plusHours(3));
        exam2.setVisibleDate(now().plusHours(1));

        // creating exercise
        ModelingExercise modelingExercise = ModelFactory.generateModelingExerciseForExam(DiagramType.ClassDiagram, exam2.getExerciseGroups().get(0));
        exam2.getExerciseGroups().get(0).addExercise(modelingExercise);
        exerciseGroupRepository.save(exam2.getExerciseGroups().get(0));
        modelingExercise = exerciseRepo.save(modelingExercise);

        List<StudentExam> createdStudentExams = new ArrayList<>();

        // creating student exams
        for (User user : registeredUsers) {
            StudentExam studentExam = new StudentExam();
            studentExam.addExercise(modelingExercise);
            studentExam.setUser(user);
            exam2.addStudentExam(studentExam);
            createdStudentExams.add(studentExamRepository.save(studentExam));
        }

        exam2 = examRepository.save(exam2);

        // invoke start exercises
        Integer noGeneratedParticipations = request.postWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam2.getId() + "/student-exams/start-exercises",
                Optional.empty(), Integer.class, HttpStatus.OK);
        assertThat(noGeneratedParticipations).isEqualTo(exam2.getStudentExams().size());
        List<Participation> studentParticipations = participationTestRepository.findAllWithSubmissions();

        for (Participation participation : studentParticipations) {
            assertThat(participation.getExercise()).isEqualTo(modelingExercise);
            assertThat(participation.getExercise().getCourseViaExerciseGroupOrCourseMember()).isNotNull();
            assertThat(participation.getExercise().getExerciseGroup()).isEqualTo(exam2.getExerciseGroups().get(0));
            assertThat(participation.getSubmissions()).hasSize(1);
            var textSubmission = (ModelingSubmission) participation.getSubmissions().iterator().next();
            assertThat(textSubmission.getModel()).isNull();
            assertThat(textSubmission.getExplanationText()).isNull();
        }

        // Cleanup of Bidirectional Relationships
        for (StudentExam studentExam : createdStudentExams) {
            exam2.removeStudentExam(studentExam);
        }
        examRepository.save(exam2);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGenerateStudentExams() throws Exception {
        Exam exam = database.setupExamWithExerciseGroupsExercisesRegisteredStudents(course1);

        // invoke generate student exams
        List<StudentExam> studentExams = request.postListWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/generate-student-exams",
                Optional.empty(), StudentExam.class, HttpStatus.OK);
        assertThat(studentExams).hasSize(exam.getRegisteredUsers().size());
        for (StudentExam studentExam : studentExams) {
            assertThat(studentExam.getWorkingTime()).as("Working time is set correctly").isEqualTo(120 * 60);
        }

        for (var studentExam : studentExams) {
            assertThat(studentExam.getExercises()).hasSize(exam.getNumberOfExercisesInExam());
            assertThat(studentExam.getExam()).isEqualTo(exam);
            // TODO: check exercise configuration, each mandatory exercise group has to appear, one optional exercise should appear
        }

        // Make sure delete also works if so many objects have been created before
        request.delete("/api/courses/" + course1.getId() + "/exams/" + exam.getId(), HttpStatus.OK);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGenerateStudentExamsNoDates_badRequest() throws Exception {
        Exam exam = database.setupExamWithExerciseGroupsExercisesRegisteredStudents(course1);
        exam.setStartDate(null);
        examRepository.save(exam);

        // invoke generate student exams
        request.postListWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/generate-student-exams", Optional.empty(), StudentExam.class,
                HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGenerateStudentExamsNoExerciseGroups_badRequest() throws Exception {
        Exam exam = database.addExamWithExerciseGroup(course1, true);
        exam.setStartDate(now());
        exam.setEndDate(now().plusHours(2));
        exam = examRepository.save(exam);

        // invoke generate student exams
        request.postListWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/generate-student-exams", Optional.empty(), StudentExam.class,
                HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGenerateStudentExamsNoExerciseNumber_badRequest() throws Exception {
        Exam exam = database.setupExamWithExerciseGroupsExercisesRegisteredStudents(course1);
        exam.setNumberOfExercisesInExam(null);
        examRepository.save(exam);

        // invoke generate student exams
        request.postListWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/generate-student-exams", Optional.empty(), StudentExam.class,
                HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGenerateStudentExamsNotEnoughExerciseGroups_badRequest() throws Exception {
        Exam exam = database.setupExamWithExerciseGroupsExercisesRegisteredStudents(course1);
        exam.setNumberOfExercisesInExam(exam.getNumberOfExercisesInExam() + 2);
        examRepository.save(exam);

        // invoke generate student exams
        request.postListWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/generate-student-exams", Optional.empty(), StudentExam.class,
                HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGenerateStudentExamsTooManyMandatoryExerciseGroups_badRequest() throws Exception {
        Exam exam = database.setupExamWithExerciseGroupsExercisesRegisteredStudents(course1);
        exam.setNumberOfExercisesInExam(exam.getNumberOfExercisesInExam() - 2);
        examRepository.save(exam);

        // invoke generate student exams
        request.postListWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/generate-student-exams", Optional.empty(), StudentExam.class,
                HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGenerateMissingStudentExams() throws Exception {
        Exam exam = database.setupExamWithExerciseGroupsExercisesRegisteredStudents(course1);

        // Generate student exams
        List<StudentExam> studentExams = request.postListWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/generate-student-exams",
                Optional.empty(), StudentExam.class, HttpStatus.OK);
        assertThat(studentExams).hasSize(exam.getRegisteredUsers().size());

        // Register two new students
        var student5 = database.getUserByLogin("student5");
        var student6 = database.getUserByLogin("student6");
        exam.getRegisteredUsers().addAll(Set.of(student5, student6));
        examRepository.save(exam);

        // Generate individual exams for the two missing students
        List<StudentExam> missingStudentExams = request.postListWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/generate-missing-student-exams",
                Optional.empty(), StudentExam.class, HttpStatus.OK);
        assertThat(missingStudentExams).hasSize(2);

        // Fetch student exams
        List<StudentExam> studentExamsDB = request.getList("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/studentExams", HttpStatus.OK, StudentExam.class);
        assertThat(studentExamsDB).hasSize(exam.getRegisteredUsers().size());

        // Another request should not create any exams
        missingStudentExams = request.postListWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/generate-missing-student-exams", Optional.empty(),
                StudentExam.class, HttpStatus.OK);
        assertThat(missingStudentExams).hasSize(0);
        studentExamsDB = request.getList("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/studentExams", HttpStatus.OK, StudentExam.class);
        assertThat(studentExamsDB).hasSize(exam.getRegisteredUsers().size());

        // Make sure delete also works if so many objects have been created before
        request.delete("/api/courses/" + course1.getId() + "/exams/" + exam.getId(), HttpStatus.OK);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testSaveExamWithExerciseGroupWithExerciseToDatabase() {
        database.addCourseExamExerciseGroupWithOneTextExercise();
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void testAll_asStudent() throws Exception {
        this.testAllPreAuthorize();
        ModelFactory.generateExam(course1);
        request.getList("/api/courses/" + course1.getId() + "/exams", HttpStatus.FORBIDDEN, Exam.class);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testAll_asTutor() throws Exception {
        this.testAllPreAuthorize();
    }

    private void testAllPreAuthorize() throws Exception {
        Exam exam = ModelFactory.generateExam(course1);
        request.post("/api/courses/" + course1.getId() + "/exams", exam, HttpStatus.FORBIDDEN);
        request.put("/api/courses/" + course1.getId() + "/exams", exam, HttpStatus.FORBIDDEN);
        request.get("/api/courses/" + course1.getId() + "/exams/" + exam1.getId(), HttpStatus.FORBIDDEN, Exam.class);
        request.delete("/api/courses/" + course1.getId() + "/exams/" + exam1.getId(), HttpStatus.FORBIDDEN);
        request.post("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/students/student1", null, HttpStatus.FORBIDDEN);
        request.post("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/students", Collections.singletonList(new StudentDTO()), HttpStatus.FORBIDDEN);
        request.delete("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/students/student1", HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "instructor6", roles = "INSTRUCTOR")
    public void testCreateExam_checkCourseAccess_InstructorNotInCourse_forbidden() throws Exception {
        Exam exam = ModelFactory.generateExam(course1);
        request.post("/api/courses/" + course1.getId() + "/exams", exam, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testCreateExam_asInstructor() throws Exception {
        // Test for bad request when exam id is already set.
        Exam examA = ModelFactory.generateExam(course1);
        examA.setId(55L);
        request.post("/api/courses/" + course1.getId() + "/exams", examA, HttpStatus.BAD_REQUEST);
        // Test for conflict when course is null.
        Exam examB = ModelFactory.generateExam(course1);
        examB.setCourse(null);
        request.post("/api/courses/" + course1.getId() + "/exams", examB, HttpStatus.CONFLICT);
        // Test for conflict when course deviates from course specified in route.
        Exam examC = ModelFactory.generateExam(course1);
        request.post("/api/courses/" + course2.getId() + "/exams", examC, HttpStatus.CONFLICT);
        // Test invalid dates
        List<Exam> examsWithInvalidDate = createExamsWithInvalidDates(course1);
        for (var exam : examsWithInvalidDate) {
            request.post("/api/courses/" + course1.getId() + "/exams", exam, HttpStatus.CONFLICT);
        }
        // Test for forbidden when user tries to create an exam with exercise groups.
        Exam examD = ModelFactory.generateExam(course1);
        examD.addExerciseGroup(ModelFactory.generateExerciseGroup(true, exam1));
        request.post("/api/courses/" + course1.getId() + "/exams", examD, HttpStatus.FORBIDDEN);
        // Test examAccessService.
        Exam examE = ModelFactory.generateExam(course1);
        request.post("/api/courses/" + course1.getId() + "/exams", examE, HttpStatus.CREATED);
        verify(examAccessService, times(1)).checkCourseAccessForInstructor(course1.getId());
    }

    private List<Exam> createExamsWithInvalidDates(Course course) {
        // Test for conflict, visible date not set
        Exam examA = ModelFactory.generateExam(course);
        examA.setVisibleDate(null);
        // Test for conflict, start date not set
        Exam examB = ModelFactory.generateExam(course);
        examB.setStartDate(null);
        // Test for conflict, end date not set
        Exam examC = ModelFactory.generateExam(course);
        examC.setEndDate(null);
        // Test for conflict, start date not after visible date
        Exam examD = ModelFactory.generateExam(course);
        examD.setStartDate(examD.getVisibleDate());
        // Test for conflict, end date not after start date
        Exam examE = ModelFactory.generateExam(course);
        examE.setEndDate(examE.getStartDate());
        return List.of(examA, examB, examC, examD, examE);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testUpdateExam_asInstructor() throws Exception {
        // Create instead of update if no id was set
        Exam exam = ModelFactory.generateExam(course1);
        exam.setTitle("Over 9000!");
        long examCountBefore = examRepository.count();
        Exam createdExam = request.putWithResponseBody("/api/courses/" + course1.getId() + "/exams", exam, Exam.class, HttpStatus.CREATED);
        assertThat(exam.getEndDate()).isEqualTo(createdExam.getEndDate());
        assertThat(exam.getStartDate()).isEqualTo(createdExam.getStartDate());
        assertThat(exam.getVisibleDate()).isEqualTo(createdExam.getVisibleDate());
        // Note: ZonedDateTime has problems with comparison due to time zone differences for values saved in the database and values not saved in the database
        assertThat(exam).usingRecursiveComparison().ignoringFields("id", "course", "endDate", "startDate", "visibleDate").isEqualTo(createdExam);
        assertThat(examCountBefore + 1).isEqualTo(examRepository.count());
        // No course is set -> conflict
        exam = ModelFactory.generateExam(course1);
        exam.setId(1L);
        exam.setCourse(null);
        request.put("/api/courses/" + course1.getId() + "/exams", exam, HttpStatus.CONFLICT);
        // Course id in the updated exam and in the REST resource url do not match -> conflict
        exam = ModelFactory.generateExam(course1);
        exam.setId(1L);
        request.put("/api/courses/" + course2.getId() + "/exams", exam, HttpStatus.CONFLICT);
        // Dates in the updated exam are not valid -> conflict
        List<Exam> examsWithInvalidDate = createExamsWithInvalidDates(course1);
        for (var examWithInvDate : examsWithInvalidDate) {
            examWithInvDate.setId(1L);
            request.put("/api/courses/" + course1.getId() + "/exams", examWithInvDate, HttpStatus.CONFLICT);
        }
        // Update the exam -> ok
        exam1.setTitle("Best exam ever");
        var returnedExam = request.putWithResponseBody("/api/courses/" + course1.getId() + "/exams", exam1, Exam.class, HttpStatus.OK);
        assertEquals(exam1, returnedExam);
        verify(instanceMessageSendService, never()).sendProgrammingExerciseSchedule(any());
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testUpdateExam_reschedule_visibleAndStartDateChanged() throws Exception {
        // Add a programming exercise to the exam and change the dates in order to invoke a rescheduling
        var programmingEx = database.addCourseExamExerciseGroupWithOneProgrammingExerciseAndTestCases();
        var examWithProgrammingEx = programmingEx.getExerciseGroup().getExam();
        examWithProgrammingEx.setVisibleDate(examWithProgrammingEx.getVisibleDate().plusSeconds(1));
        examWithProgrammingEx.setStartDate(examWithProgrammingEx.getStartDate().plusSeconds(1));
        request.put("/api/courses/" + examWithProgrammingEx.getCourse().getId() + "/exams", examWithProgrammingEx, HttpStatus.OK);
        verify(instanceMessageSendService, times(1)).sendProgrammingExerciseSchedule(programmingEx.getId());
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testUpdateExam_reschedule_visibleDateChanged() throws Exception {
        var programmingEx = database.addCourseExamExerciseGroupWithOneProgrammingExerciseAndTestCases();
        var examWithProgrammingEx = programmingEx.getExerciseGroup().getExam();
        examWithProgrammingEx.setVisibleDate(examWithProgrammingEx.getVisibleDate().plusSeconds(1));
        request.put("/api/courses/" + examWithProgrammingEx.getCourse().getId() + "/exams", examWithProgrammingEx, HttpStatus.OK);
        verify(instanceMessageSendService, times(1)).sendProgrammingExerciseSchedule(programmingEx.getId());
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testUpdateExam_reschedule_startDateChanged() throws Exception {
        var programmingEx = database.addCourseExamExerciseGroupWithOneProgrammingExerciseAndTestCases();
        var examWithProgrammingEx = programmingEx.getExerciseGroup().getExam();
        examWithProgrammingEx.setStartDate(examWithProgrammingEx.getStartDate().plusSeconds(1));
        request.put("/api/courses/" + examWithProgrammingEx.getCourse().getId() + "/exams", examWithProgrammingEx, HttpStatus.OK);
        verify(instanceMessageSendService, times(1)).sendProgrammingExerciseSchedule(programmingEx.getId());
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetExam_asInstructor() throws Exception {
        request.get("/api/courses/" + course1.getId() + "/exams/" + exam1.getId(), HttpStatus.OK, Exam.class);
        verify(examAccessService, times(1)).checkCourseAndExamAccessForInstructor(course1.getId(), exam1.getId());
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetExamsForCourse_asInstructor() throws Exception {
        request.getList("/api/courses/" + course1.getId() + "/exams", HttpStatus.OK, Exam.class);
        verify(examAccessService, times(1)).checkCourseAccessForTeachingAssistant(course1.getId());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testGetCurrentAndUpcomingExams() throws Exception {
        request.getList("/api/courses/upcoming-exams", HttpStatus.OK, Exam.class);
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    public void testGetCurrentAndUpcomingExamsForbiddenForUser() throws Exception {
        request.getList("/api/courses/upcoming-exams", HttpStatus.FORBIDDEN, Exam.class);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetCurrentAndUpcomingExamsForbiddenForInstructor() throws Exception {
        request.getList("/api/courses/upcoming-exams", HttpStatus.FORBIDDEN, Exam.class);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testGetCurrentAndUpcomingExamsForbiddenForTutor() throws Exception {
        request.getList("/api/courses/upcoming-exams", HttpStatus.FORBIDDEN, Exam.class);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testDeleteEmptyExam_asInstructor() throws Exception {
        request.delete("/api/courses/" + course1.getId() + "/exams/" + exam1.getId(), HttpStatus.OK);
        verify(examAccessService, times(1)).checkCourseAndExamAccessForInstructor(course1.getId(), exam1.getId());
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testDeleteExamWithExerciseGroupAndTextExercise_asInstructor() throws Exception {
        TextExercise textExercise = ModelFactory.generateTextExerciseForExam(exam2.getExerciseGroups().get(0));
        exerciseRepo.save(textExercise);
        request.delete("/api/courses/" + course1.getId() + "/exams/" + exam2.getId(), HttpStatus.OK);
        verify(examAccessService, times(1)).checkCourseAndExamAccessForInstructor(course1.getId(), exam2.getId());
    }

    @Test
    @WithMockUser(value = "admin", roles = "ADMIN")
    public void testDeleteExamThatDoesNotExist() throws Exception {
        request.delete("/api/courses/" + course2.getId() + "/exams/654555", HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testDeleteStudent() throws Exception {
        // Create an exam with registered students
        Exam exam = database.setupExamWithExerciseGroupsExercisesRegisteredStudents(course1);
        var student1 = database.getUserByLogin("student1");
        var student2 = database.getUserByLogin("student2");

        // Remove student1 from the exam
        request.delete("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/students/student1", HttpStatus.OK);

        // Get the exam with all registered users
        var params = new LinkedMultiValueMap<String, String>();
        params.add("withStudents", "true");
        Exam storedExam = request.get("/api/courses/" + course1.getId() + "/exams/" + exam.getId(), HttpStatus.OK, Exam.class, params);

        // Ensure that student1 was removed from the exam
        assertThat(storedExam.getRegisteredUsers()).doesNotContain(student1);
        assertThat(storedExam.getRegisteredUsers()).hasSize(3);

        // Create individual student exams
        List<StudentExam> generatedStudentExams = request.postListWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/generate-student-exams",
                Optional.empty(), StudentExam.class, HttpStatus.OK);
        assertThat(generatedStudentExams).hasSize(storedExam.getRegisteredUsers().size());

        // Start the exam to create participations
        request.postWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/student-exams/start-exercises", Optional.empty(), Integer.class,
                HttpStatus.OK);

        // Get the student exam of student2
        Optional<StudentExam> optionalStudent1Exam = generatedStudentExams.stream().filter(studentExam -> studentExam.getUser().equals(student2)).findFirst();
        assertThat(optionalStudent1Exam.get()).isNotNull();
        var studentExam2 = optionalStudent1Exam.get();

        // explicitly set the user again to prevent issues in the following server call due to the use of SecurityUtils.setAuthorizationObject();
        database.changeUser("instructor1");
        // Remove student2 from the exam
        request.delete("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/students/student2", HttpStatus.OK);

        // Get the exam with all registered users
        params = new LinkedMultiValueMap<>();
        params.add("withStudents", "true");
        storedExam = request.get("/api/courses/" + course1.getId() + "/exams/" + exam.getId(), HttpStatus.OK, Exam.class, params);

        // Ensure that student2 was removed from the exam
        assertThat(storedExam.getRegisteredUsers()).doesNotContain(student2);
        assertThat(storedExam.getRegisteredUsers()).hasSize(2);

        // Ensure that the student exam of student2 was deleted
        List<StudentExam> studentExams = request.getList("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/studentExams", HttpStatus.OK, StudentExam.class);
        assertThat(studentExams).hasSize(storedExam.getRegisteredUsers().size());
        assertThat(studentExams).doesNotContain(studentExam2);

        // Ensure that the participations were not deleted
        SecurityUtils.setAuthorizationObject(); // TODO why do we get an exception here without that?
        List<StudentParticipation> participationsStudent2 = studentParticipationRepository.findByStudentIdAndIndividualExercisesWithEagerSubmissionsResult(student2.getId(),
                studentExam2.getExercises());
        assertThat(participationsStudent2).hasSize(studentExam2.getExercises().size());

        // Make sure delete also works if so many objects have been created before
        request.delete("/api/courses/" + course1.getId() + "/exams/" + exam.getId(), HttpStatus.OK);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetExamWithOptions() throws Exception {
        User user = userRepo.findOneByLogin("student1").get();
        Course course = database.createCourseWithExamAndExerciseGroupAndExercises(user, now().minusHours(3), now().minusHours(2), now().minusHours(1));
        var exam = examRepository.findWithExerciseGroupsAndExercisesById(course.getExams().iterator().next().getId()).get();
        // Get the exam with all registered users
        // 1. without options
        var exam1 = request.get("/api/courses/" + course.getId() + "/exams/" + exam.getId(), HttpStatus.OK, Exam.class);
        assertThat(exam1.getRegisteredUsers()).isEmpty();
        assertThat(exam1.getExerciseGroups()).isEmpty();

        // 2. with students, without exercise groups
        var params = new LinkedMultiValueMap<String, String>();
        params.add("withStudents", "true");
        var exam2 = request.get("/api/courses/" + course.getId() + "/exams/" + exam.getId(), HttpStatus.OK, Exam.class, params);
        assertThat(exam2.getRegisteredUsers()).hasSize(1);
        assertThat(exam2.getExerciseGroups()).isEmpty();

        // 3. with students, with exercise groups
        params.add("withExerciseGroups", "true");
        var exam3 = request.get("/api/courses/" + course.getId() + "/exams/" + exam.getId(), HttpStatus.OK, Exam.class, params);
        assertThat(exam3.getRegisteredUsers()).hasSize(1);
        assertThat(exam3.getExerciseGroups()).hasSize(exam.getExerciseGroups().size());
        assertThat(exam3.getExerciseGroups().get(0).getExercises()).hasSize(exam.getExerciseGroups().get(0).getExercises().size());
        assertThat(exam3.getExerciseGroups().get(1).getExercises()).hasSize(exam.getExerciseGroups().get(1).getExercises().size());

        // 4. without students, with exercise groups
        params = new LinkedMultiValueMap<>();
        params.add("withExerciseGroups", "true");
        var exam4 = request.get("/api/courses/" + course.getId() + "/exams/" + exam.getId(), HttpStatus.OK, Exam.class, params);
        assertThat(exam4.getRegisteredUsers()).isEmpty();
        assertThat(exam4.getExerciseGroups()).hasSize(exam.getExerciseGroups().size());
        assertThat(exam4.getExerciseGroups().get(0).getExercises()).hasSize(exam.getExerciseGroups().get(0).getExercises().size());
        assertThat(exam4.getExerciseGroups().get(1).getExercises()).hasSize(exam.getExerciseGroups().get(1).getExercises().size());
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testDeleteStudentWithParticipationsAndSubmissions() throws Exception {
        // Create an exam with registered students
        Exam exam = database.setupExamWithExerciseGroupsExercisesRegisteredStudents(course1);
        var student1 = database.getUserByLogin("student1");

        // Create individual student exams
        List<StudentExam> generatedStudentExams = request.postListWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/generate-student-exams",
                Optional.empty(), StudentExam.class, HttpStatus.OK);

        // Get the student exam of student1
        Optional<StudentExam> optionalStudent1Exam = generatedStudentExams.stream().filter(studentExam -> studentExam.getUser().equals(student1)).findFirst();
        assertThat(optionalStudent1Exam.get()).isNotNull();
        var studentExam1 = optionalStudent1Exam.get();

        // Start the exam to create participations
        request.postWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/student-exams/start-exercises", Optional.empty(), Integer.class,
                HttpStatus.OK);
        SecurityUtils.setAuthorizationObject(); // TODO why do we get an exception here without that?
        List<StudentParticipation> participationsStudent1 = studentParticipationRepository.findByStudentIdAndIndividualExercisesWithEagerSubmissionsResult(student1.getId(),
                studentExam1.getExercises());
        assertThat(participationsStudent1).hasSize(studentExam1.getExercises().size());

        // explicitly set the user again to prevent issues in the following server call due to the use of SecurityUtils.setAuthorizationObject();
        database.changeUser("instructor1");

        // Remove student1 from the exam and his participations
        var params = new LinkedMultiValueMap<String, String>();
        params.add("withParticipationsAndSubmission", "true");
        request.delete("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/students/student1", HttpStatus.OK, params);

        // Get the exam with all registered users
        params = new LinkedMultiValueMap<>();
        params.add("withStudents", "true");
        Exam storedExam = request.get("/api/courses/" + course1.getId() + "/exams/" + exam.getId(), HttpStatus.OK, Exam.class, params);

        // Ensure that student1 was removed from the exam
        assertThat(storedExam.getRegisteredUsers()).doesNotContain(student1);
        assertThat(storedExam.getRegisteredUsers()).hasSize(3);

        // Ensure that the student exam of student1 was deleted
        List<StudentExam> studentExams = request.getList("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/studentExams", HttpStatus.OK, StudentExam.class);
        assertThat(studentExams).hasSize(storedExam.getRegisteredUsers().size());
        assertThat(studentExams).doesNotContain(studentExam1);

        // Ensure that the participations of student1 were deleted
        SecurityUtils.setAuthorizationObject(); // TODO why do we get an exception here without that?
        participationsStudent1 = studentParticipationRepository.findByStudentIdAndIndividualExercisesWithEagerSubmissionsResult(student1.getId(), studentExam1.getExercises());
        assertThat(participationsStudent1).isEmpty();

        // Make sure delete also works if so many objects have been created before
        request.delete("/api/courses/" + course1.getId() + "/exams/" + exam.getId(), HttpStatus.OK);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testGetExamForTestRunDashboard_forbidden() throws Exception {
        request.get("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/for-exam-tutor-test-run-dashboard", HttpStatus.FORBIDDEN, Exam.class);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetExamForTestRunDashboard_conflict() throws Exception {
        request.get("/api/courses/" + course2.getId() + "/exams/" + exam1.getId() + "/for-exam-tutor-test-run-dashboard", HttpStatus.CONFLICT, Exam.class);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testDeleteExamWithOneTestRun() throws Exception {
        var instructor = database.getUserByLogin("instructor1");
        var exam = database.addExam(course1);
        exam = database.addTextModelingProgrammingExercisesToExam(exam, false);
        database.setupTestRunForExamWithExerciseGroupsForInstructor(exam, instructor, exam.getExerciseGroups());
        request.delete("/api/courses/" + exam.getCourse().getId() + "/exams/" + exam.getId(), HttpStatus.OK);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetExamForTestRunDashboard_ok() throws Exception {
        var instructor = database.getUserByLogin("instructor1");
        var exam = database.addExam(course1);
        exam = database.addTextModelingProgrammingExercisesToExam(exam, false);
        database.setupTestRunForExamWithExerciseGroupsForInstructor(exam, instructor, exam.getExerciseGroups());
        exam = request.get("/api/courses/" + exam.getCourse().getId() + "/exams/" + exam.getId() + "/for-exam-tutor-test-run-dashboard", HttpStatus.OK, Exam.class);
        assertThat(exam.getExerciseGroups().stream().flatMap(exerciseGroup -> exerciseGroup.getExercises().stream()).collect(Collectors.toList())).isNotEmpty();
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testDeleteStudentThatDoesNotExist() throws Exception {
        Exam exam = database.setupExamWithExerciseGroupsExercisesRegisteredStudents(course1);
        request.delete("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/students/nonExistingStudent", HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void testGetExamForConduction() throws Exception {
        Exam exam = database.addActiveExamWithRegisteredUser(course1, users.get(0));
        StudentExam response = request.get("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/conduction", HttpStatus.OK, StudentExam.class);
        assertThat(response.getExam()).isEqualTo(exam);
        verify(examAccessService, times(1)).checkAndGetCourseAndExamAccessForConduction(course1.getId(), exam.getId());
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testAddAllRegisteredUsersToExam() throws Exception {
        Course course = database.addEmptyCourse();
        Exam exam = database.addExam(course);
        exam = database.addExerciseGroupsAndExercisesToExam(exam, false);
        exam = examRepository.save(exam);
        course.addExam(exam);
        course = courseRepo.save(course);

        var instructor = database.getUserByLogin("instructor1");
        instructor.setGroups(Collections.singleton("instructor"));
        userRepo.save(instructor);

        var student99 = ModelFactory.generateActivatedUser("student99");     // not registered for the course
        student99.setRegistrationNumber("1234");
        userRepo.save(student99);
        student99 = userRepo.findOneWithGroupsAndAuthoritiesByLogin("student99").get();
        student99.setGroups(Collections.singleton("tumuser"));
        userRepo.save(student99);
        assertThat(student99.getGroups()).contains(course.getStudentGroupName());

        assertThat(exam.getRegisteredUsers()).doesNotContain(student99);
        var response = request.postWithResponseBody("/api/courses/" + course.getId() + "/exams/" + exam.getId() + "/addAllStudentsOfCourse", Optional.empty(), List.class,
                HttpStatus.OK);

        exam = examRepository.findWithRegisteredUsersById(exam.getId()).get();

        assertThat(exam.getRegisteredUsers().size()).isEqualTo(1);
        assertThat(exam.getRegisteredUsers()).contains(student99);
        assertThat(response.size()).isEqualTo(10);
        verify(examAccessService, times(1)).checkCourseAndExamAccessForInstructor(course.getId(), exam.getId());
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testUpdateOrderOfExerciseGroups() throws Exception {
        ExerciseGroup exerciseGroup1 = new ExerciseGroup();
        exerciseGroup1.setTitle("first");
        ExerciseGroup exerciseGroup2 = new ExerciseGroup();
        exerciseGroup2.setTitle("second");
        ExerciseGroup exerciseGroup3 = new ExerciseGroup();
        exerciseGroup3.setTitle("third");

        Exam exam = database.addExam(course1);
        exam.addExerciseGroup(exerciseGroup1);
        exam.addExerciseGroup(exerciseGroup2);
        exam.addExerciseGroup(exerciseGroup3);
        examRepository.save(exam);

        Exam examWithExerciseGroups = examRepository.findWithExerciseGroupsById(exam.getId()).get();
        exerciseGroup1 = examWithExerciseGroups.getExerciseGroups().get(0);
        exerciseGroup2 = examWithExerciseGroups.getExerciseGroups().get(1);
        exerciseGroup3 = examWithExerciseGroups.getExerciseGroups().get(2);

        TextExercise exercise1_1 = ModelFactory.generateTextExerciseForExam(exerciseGroup1);
        TextExercise exercise1_2 = ModelFactory.generateTextExerciseForExam(exerciseGroup1);
        TextExercise exercise2_1 = ModelFactory.generateTextExerciseForExam(exerciseGroup2);
        TextExercise exercise3_1 = ModelFactory.generateTextExerciseForExam(exerciseGroup3);
        TextExercise exercise3_2 = ModelFactory.generateTextExerciseForExam(exerciseGroup3);
        TextExercise exercise3_3 = ModelFactory.generateTextExerciseForExam(exerciseGroup3);
        exercise1_1 = textExerciseRepository.save(exercise1_1);
        exercise1_2 = textExerciseRepository.save(exercise1_2);
        exercise2_1 = textExerciseRepository.save(exercise2_1);
        exercise3_1 = textExerciseRepository.save(exercise3_1);
        exercise3_2 = textExerciseRepository.save(exercise3_2);
        exercise3_3 = textExerciseRepository.save(exercise3_3);

        examWithExerciseGroups = examRepository.findWithExerciseGroupsById(exam.getId()).get();
        exerciseGroup1 = examWithExerciseGroups.getExerciseGroups().get(0);
        exerciseGroup2 = examWithExerciseGroups.getExerciseGroups().get(1);
        exerciseGroup3 = examWithExerciseGroups.getExerciseGroups().get(2);
        List<ExerciseGroup> orderedExerciseGroups = new ArrayList<>();
        orderedExerciseGroups.add(exerciseGroup2);
        orderedExerciseGroups.add(exerciseGroup3);
        orderedExerciseGroups.add(exerciseGroup1);

        // Should save new order
        request.put("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/exerciseGroupsOrder", orderedExerciseGroups, HttpStatus.OK);
        verify(examAccessService, times(1)).checkCourseAndExamAccessForInstructor(course1.getId(), exam.getId());
        List<ExerciseGroup> savedExerciseGroups = examRepository.findWithExerciseGroupsById(exam.getId()).get().getExerciseGroups();
        assertThat(savedExerciseGroups.get(0).getTitle()).isEqualTo("second");
        assertThat(savedExerciseGroups.get(1).getTitle()).isEqualTo("third");
        assertThat(savedExerciseGroups.get(2).getTitle()).isEqualTo("first");

        // Exercises should be preserved
        Exam savedExam = examRepository.findWithExerciseGroupsAndExercisesById(exam.getId()).get();
        ExerciseGroup savedExerciseGroup1 = savedExam.getExerciseGroups().get(2);
        ExerciseGroup savedExerciseGroup2 = savedExam.getExerciseGroups().get(0);
        ExerciseGroup savedExerciseGroup3 = savedExam.getExerciseGroups().get(1);
        assertThat(savedExerciseGroup1.getExercises().size()).isEqualTo(2);
        assertThat(savedExerciseGroup2.getExercises().size()).isEqualTo(1);
        assertThat(savedExerciseGroup3.getExercises().size()).isEqualTo(3);
        assertThat(savedExerciseGroup1.getExercises().contains(exercise1_1)).isTrue();
        assertThat(savedExerciseGroup1.getExercises().contains(exercise1_2)).isTrue();
        assertThat(savedExerciseGroup2.getExercises().contains(exercise2_1)).isTrue();
        assertThat(savedExerciseGroup3.getExercises().contains(exercise3_1)).isTrue();
        assertThat(savedExerciseGroup3.getExercises().contains(exercise3_2)).isTrue();
        assertThat(savedExerciseGroup3.getExercises().contains(exercise3_3)).isTrue();

        // Should fail with too many exercise groups
        orderedExerciseGroups.add(exerciseGroup1);
        request.put("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/exerciseGroupsOrder", orderedExerciseGroups, HttpStatus.FORBIDDEN);

        // Should fail with too few exercise groups
        orderedExerciseGroups.remove(3);
        orderedExerciseGroups.remove(2);
        request.put("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/exerciseGroupsOrder", orderedExerciseGroups, HttpStatus.FORBIDDEN);

        // Should fail with different exercise group
        orderedExerciseGroups = new ArrayList<>();
        orderedExerciseGroups.add(exerciseGroup2);
        orderedExerciseGroups.add(exerciseGroup3);
        orderedExerciseGroups.add(ModelFactory.generateExerciseGroup(true, exam));
        request.put("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/exerciseGroupsOrder", orderedExerciseGroups, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void lockAllRepositories_noInstructor() throws Exception {
        ExerciseGroup exerciseGroup1 = new ExerciseGroup();

        Exam exam = database.addExam(course1);
        exam.addExerciseGroup(exerciseGroup1);
        exam = examRepository.save(exam);

        request.postWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/student-exams/lock-all-repositories", Optional.empty(), Integer.class,
                HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void lockAllRepositories() throws Exception {
        ExerciseGroup exerciseGroup1 = new ExerciseGroup();

        Exam exam = database.addExam(course1);
        exam.addExerciseGroup(exerciseGroup1);
        exam = examRepository.save(exam);

        Exam examWithExerciseGroups = examRepository.findWithExerciseGroupsAndExercisesById(exam.getId()).get();
        exerciseGroup1 = examWithExerciseGroups.getExerciseGroups().get(0);

        ProgrammingExercise programmingExercise = ModelFactory.generateProgrammingExerciseForExam(exerciseGroup1);
        programmingExercise = programmingExerciseRepository.save(programmingExercise);
        exerciseGroup1.addExercise(programmingExercise);

        ProgrammingExercise programmingExercise2 = ModelFactory.generateProgrammingExerciseForExam(exerciseGroup1);
        programmingExercise2 = programmingExerciseRepository.save(programmingExercise2);
        exerciseGroup1.addExercise(programmingExercise2);

        exerciseGroupRepository.save(exerciseGroup1);

        Integer numOfLockedExercises = request.postWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/student-exams/lock-all-repositories",
                Optional.empty(), Integer.class, HttpStatus.OK);

        assertThat(numOfLockedExercises).isEqualTo(2);

        verify(programmingExerciseScheduleService, times(1)).lockAllStudentRepositories(programmingExercise);
        verify(programmingExerciseScheduleService, times(1)).lockAllStudentRepositories(programmingExercise2);
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void unlockAllRepositories_preAuthNoInstructor() throws Exception {
        ExerciseGroup exerciseGroup1 = new ExerciseGroup();

        Exam exam = database.addExam(course1);
        exam.addExerciseGroup(exerciseGroup1);
        exam = examRepository.save(exam);

        request.postWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/student-exams/unlock-all-repositories", Optional.empty(), Integer.class,
                HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void unlockAllRepositories() throws Exception {
        ExerciseGroup exerciseGroup1 = new ExerciseGroup();

        Exam exam = database.addExam(course1);
        exam.addExerciseGroup(exerciseGroup1);
        exam = examRepository.save(exam);

        Exam examWithExerciseGroups = examRepository.findWithExerciseGroupsAndExercisesById(exam.getId()).get();
        exerciseGroup1 = examWithExerciseGroups.getExerciseGroups().get(0);

        ProgrammingExercise programmingExercise = ModelFactory.generateProgrammingExerciseForExam(exerciseGroup1);
        programmingExercise = programmingExerciseRepository.save(programmingExercise);
        exerciseGroup1.addExercise(programmingExercise);

        ProgrammingExercise programmingExercise2 = ModelFactory.generateProgrammingExerciseForExam(exerciseGroup1);
        programmingExercise2 = programmingExerciseRepository.save(programmingExercise2);
        exerciseGroup1.addExercise(programmingExercise2);

        exerciseGroupRepository.save(exerciseGroup1);

        Integer numOfUnlockedExercises = request.postWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam.getId() + "/student-exams/unlock-all-repositories",
                Optional.empty(), Integer.class, HttpStatus.OK);

        assertThat(numOfUnlockedExercises).isEqualTo(2);

        verify(programmingExerciseScheduleService, times(1)).unlockAllStudentRepositoriesForExam(programmingExercise);
        verify(programmingExerciseScheduleService, times(1)).unlockAllStudentRepositoriesForExam(programmingExercise2);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testGetExamForExamAssessmentDashboard() throws Exception {
        User user = userRepo.findOneByLogin("student1").get();
        // we need an exam from the past, otherwise the tutor won't have access
        Course course = database.createCourseWithExamAndExerciseGroupAndExercises(user, now().minusHours(3), now().minusHours(2), now().minusHours(1));
        Exam receivedExam = request.get("/api/courses/" + course.getId() + "/exams/" + course.getExams().iterator().next().getId() + "/for-exam-tutor-dashboard", HttpStatus.OK,
                Exam.class);

        // Test that the received exam has two text exercises
        assertThat(receivedExam.getExerciseGroups().get(0).getExercises().size()).as("Two exercises are returned").isEqualTo(2);
        // Test that the received exam has zero quiz exercises, because quiz exercises do not need to be corrected manually
        assertThat(receivedExam.getExerciseGroups().get(1).getExercises().size()).as("Zero exercises are returned").isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testGetExamForExamAssessmentDashboard_beforeDueDate() throws Exception {
        User user = userRepo.findOneByLogin("student1").get();
        Course course = database.createCourseWithExamAndExerciseGroupAndExercises(user);
        Exam exam = course.getExams().iterator().next();
        exam.setEndDate(now().plusWeeks(1));
        examRepository.save(exam);

        request.get("/api/courses/" + course.getId() + "/exams/" + course.getExams().iterator().next().getId() + "/for-exam-tutor-dashboard", HttpStatus.FORBIDDEN, Exam.class);
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    public void testGetExamForExamAssessmentDashboard_asStudent_forbidden() throws Exception {
        User user = userRepo.findOneByLogin("student1").get();
        Course course = database.createCourseWithExamAndExerciseGroupAndExercises(user);
        request.get("/api/courses/" + course.getId() + "/exams/" + course.getExams().iterator().next().getId() + "/for-exam-tutor-dashboard", HttpStatus.FORBIDDEN, Course.class);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetExamForExamAssessmentDashboard_courseIdDoesNotMatch_conflict() throws Exception {
        request.get("/api/courses/" + course2.getId() + "/exams/" + exam1.getId() + "/for-exam-tutor-dashboard", HttpStatus.CONFLICT, Course.class);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testGetExamForExamAssessmentDashboard_notFound() throws Exception {
        request.get("/api/courses/1/exams/1/for-exam-tutor-dashboard", HttpStatus.NOT_FOUND, Course.class);
    }

    @Test
    @WithMockUser(username = "tutor6", roles = "TA")
    public void testGetExamForExamDashboard_NotTAOfCourse_forbidden() throws Exception {
        User user = userRepo.findOneByLogin("student1").get();
        Course course = database.createCourseWithExamAndExerciseGroupAndExercises(user);
        Exam exam = course.getExams().iterator().next();
        exam.setEndDate(now().plusWeeks(1));
        examRepository.save(exam);

        request.get("/api/courses/" + course.getId() + "/exams/" + course.getExams().iterator().next().getId() + "/for-exam-tutor-dashboard", HttpStatus.FORBIDDEN, Course.class);
    }

    @Test
    @WithMockUser(username = "tutor6", roles = "TA")
    public void testGetExamScore_tutorNotInCourse_forbidden() throws Exception {
        request.get("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/scores", HttpStatus.FORBIDDEN, ExamScoresDTO.class);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetExamScore() throws Exception {

        // TODO avoid duplicated code with StudentExamIntegrationTest

        var examVisibleDate = ZonedDateTime.now().minusMinutes(5);
        var examStartDate = ZonedDateTime.now().plusMinutes(5);
        var examEndDate = ZonedDateTime.now().plusMinutes(20);

        Course course = database.addEmptyCourse();
        Exam exam = database.addExam(course, examVisibleDate, examStartDate, examEndDate);

        // TODO: it would be nice if we can support programming exercises here as well
        exam = database.addExerciseGroupsAndExercisesToExam(exam, false);

        // register users. Instructors are ignored from scores as they are exclusive for test run exercises
        Set<User> registeredStudents = users.stream().filter(user -> !user.getLogin().contains("instructor") && !user.getLogin().contains("admin")).collect(Collectors.toSet());
        exam.setRegisteredUsers(registeredStudents);
        exam.setNumberOfExercisesInExam(exam.getExerciseGroups().size());
        exam.setRandomizeExerciseOrder(false);
        exam = examRepository.save(exam);
        exam = examRepository.findWithRegisteredUsersAndExerciseGroupsAndExercisesById(exam.getId()).get();

        // generate individual student exams
        List<StudentExam> studentExams = request.postListWithResponseBody("/api/courses/" + course.getId() + "/exams/" + exam.getId() + "/generate-student-exams", Optional.empty(),
                StudentExam.class, HttpStatus.OK);
        assertThat(studentExams).hasSize(exam.getRegisteredUsers().size());

        assertThat(studentExamRepository.findAll()).hasSize(registeredStudents.size());

        // start exercises

        Integer noGeneratedParticipations = request.postWithResponseBody("/api/courses/" + course.getId() + "/exams/" + exam.getId() + "/student-exams/start-exercises",
                Optional.empty(), Integer.class, HttpStatus.OK);
        assertThat(noGeneratedParticipations).isEqualTo(registeredStudents.size() * exam.getExerciseGroups().size());

        // explicitly set the user again to prevent issues in the following server call due to the use of SecurityUtils.setAuthorizationObject();
        database.changeUser("instructor1");

        // Fetch the created participations and assign them to the exercises
        int participationCounter = 0;
        List<Exercise> exercisesInExam = exam.getExerciseGroups().stream().map(ExerciseGroup::getExercises).flatMap(Collection::stream).collect(Collectors.toList());
        SecurityUtils.setAuthorizationObject(); // TODO why do we get an exception here without that?
        for (var exercise : exercisesInExam) {
            List<StudentParticipation> participations = studentParticipationRepository.findByExerciseIdWithEagerSubmissionsResult(exercise.getId());
            exercise.setStudentParticipations(new HashSet<>(participations));
            participationCounter += exercise.getStudentParticipations().size();
        }
        assertEquals(participationCounter, noGeneratedParticipations);

        // Score used for all exercise results
        Long resultScore = 75L;

        // Assign results to participations and submissions
        for (var exercise : exercisesInExam) {
            for (var participation : exercise.getStudentParticipations()) {
                Submission submission;
                // Programming exercises don't have a submission yet
                if (exercise instanceof ProgrammingExercise) {
                    assertThat(participation.getSubmissions()).hasSize(0);
                    submission = new ProgrammingSubmission();
                    submission.setParticipation(participation);
                    submission = submissionRepository.save(submission);
                }
                else {
                    // There should only be one submission for text, quiz, modeling and file upload
                    assertThat(participation.getSubmissions()).hasSize(1);
                    submission = participation.getSubmissions().iterator().next();
                }
                // Create results
                var result = new Result().score(resultScore).rated(true).resultString("Good").completionDate(ZonedDateTime.now().minusMinutes(5));
                result.setParticipation(participation);
                result = resultRepository.save(result);
                result.setSubmission(submission);
                submission.addResult(result);
                submissionRepository.save(submission);
            }
        }
        // explicitly set the user again to prevent issues in the following server call due to the use of SecurityUtils.setAuthorizationObject();
        database.changeUser("instructor1");
        final var exerciseWithNoUsers = ModelFactory.generateTextExerciseForExam(exam.getExerciseGroups().get(0));
        exerciseRepo.save(exerciseWithNoUsers);
        var response = request.get("/api/courses/" + course.getId() + "/exams/" + exam.getId() + "/scores", HttpStatus.OK, ExamScoresDTO.class);

        // Compare generated results to data in ExamScoresDTO
        // Compare top-level DTO properties
        assertThat(response.maxPoints).isEqualTo(exam.getMaxPoints());

        // For calculation assume that all exercises within an exerciseGroups have the same max points
        double calculatedAverageScore = 0.0;
        for (var exerciseGroup : exam.getExerciseGroups()) {
            var exercise = exerciseGroup.getExercises().stream().findAny().get();
            if (exercise.getIncludedInOverallScore().equals(IncludedInOverallScore.NOT_INCLUDED)) {
                continue;
            }
            calculatedAverageScore += Math.round(exercise.getMaxScore() * resultScore / 100.00 * 10) / 10.0;
        }

        assertThat(response.averagePointsAchieved).isEqualTo(calculatedAverageScore);
        assertThat(response.title).isEqualTo(exam.getTitle());
        assertThat(response.examId).isEqualTo(exam.getId());

        // Ensure that all exerciseGroups of the exam are present in the DTO
        List<Long> exerciseGroupIdsInDTO = response.exerciseGroups.stream().map(exerciseGroup -> exerciseGroup.id).collect(Collectors.toList());
        List<Long> exerciseGroupIdsInExam = exam.getExerciseGroups().stream().map(ExerciseGroup::getId).collect(Collectors.toList());
        assertThat(exerciseGroupIdsInExam).containsExactlyInAnyOrderElementsOf(exerciseGroupIdsInDTO);

        // Compare exerciseGroups in DTO to exam exerciseGroups
        // Tolerated absolute difference for floating-point number comparisons
        double EPSILON = 0000.1;
        for (var exerciseGroupDTO : response.exerciseGroups) {
            // Find the original exerciseGroup of the exam using the id in ExerciseGroupId
            ExerciseGroup originalExerciseGroup = exam.getExerciseGroups().stream().filter(exerciseGroup -> exerciseGroup.getId().equals(exerciseGroupDTO.id)).findFirst().get();

            // Assume that all exercises in a group have the same max score
            Double groupMaxScoreFromExam = originalExerciseGroup.getExercises().stream().findAny().get().getMaxScore();
            assertThat(exerciseGroupDTO.maxPoints).isEqualTo(originalExerciseGroup.getExercises().stream().findAny().get().getMaxScore());
            assertEquals(exerciseGroupDTO.maxPoints, groupMaxScoreFromExam, EPSILON);

            // Compare exercise information
            long noOfExerciseGroupParticipations = 0;
            for (var originalExercise : originalExerciseGroup.getExercises()) {
                // Find the corresponding ExerciseInfo object
                var exerciseDTO = exerciseGroupDTO.containedExercises.stream().filter(exerciseInfo -> exerciseInfo.exerciseId.equals(originalExercise.getId())).findFirst().get();
                // Check the exercise title
                assertThat(originalExercise.getTitle()).isEqualTo(exerciseDTO.title);
                // Check the max points of the exercise
                assertThat(originalExercise.getMaxScore()).isEqualTo(exerciseDTO.maxPoints);
                // Check the number of exercise participants and update the group participant counter
                var noOfExerciseParticipations = originalExercise.getStudentParticipations().size();
                noOfExerciseGroupParticipations += noOfExerciseParticipations;
                assertThat(Long.valueOf(originalExercise.getStudentParticipations().size())).isEqualTo(exerciseDTO.numberOfParticipants);
            }
            assertThat(noOfExerciseGroupParticipations).isEqualTo(exerciseGroupDTO.numberOfParticipants);
        }

        // Ensure that all registered students have a StudentResult
        List<Long> studentIdsWithStudentResults = response.studentResults.stream().map(studentResult -> studentResult.userId).collect(Collectors.toList());
        List<Long> registeredUsersIds = exam.getRegisteredUsers().stream().map(DomainObject::getId).collect(Collectors.toList());
        assertThat(studentIdsWithStudentResults).containsExactlyInAnyOrderElementsOf(registeredUsersIds);

        // Compare StudentResult with the generated results
        for (var studentResult : response.studentResults) {
            // Find the original user using the id in StudentResult
            User originalUser = exam.getRegisteredUsers().stream().filter(users -> users.getId().equals(studentResult.userId)).findFirst().get();
            StudentExam studentExamOfUser = studentExams.stream().filter(studentExam -> studentExam.getUser().equals(originalUser)).findFirst().get();

            assertThat(studentResult.name).isEqualTo(originalUser.getName());
            assertThat(studentResult.eMail).isEqualTo(originalUser.getEmail());
            assertThat(studentResult.login).isEqualTo(originalUser.getLogin());
            assertThat(studentResult.registrationNumber).isEqualTo(originalUser.getRegistrationNumber());

            // Calculate overall points achieved

            var calculatedOverallPoints = studentExamOfUser.getExercises().stream()
                    .filter(exercise -> !exercise.getIncludedInOverallScore().equals(IncludedInOverallScore.NOT_INCLUDED)).map(exercise -> exercise.getMaxScore())
                    .reduce(0.0, (total, maxScore) -> (Math.round((total + maxScore * resultScore / 100) * 10) / 10.0));

            assertEquals(studentResult.overallPointsAchieved, calculatedOverallPoints, EPSILON);

            // Calculate overall score achieved
            var calculatedOverallScore = calculatedOverallPoints / response.maxPoints * 100;
            assertEquals(studentResult.overallScoreAchieved, calculatedOverallScore, EPSILON);

            // Ensure that the exercise ids of the student exam are the same as the exercise ids in the students exercise results
            List<Long> exerciseIdsOfStudentResult = studentResult.exerciseGroupIdToExerciseResult.values().stream().map(exerciseResult -> exerciseResult.exerciseId)
                    .collect(Collectors.toList());
            List<Long> exerciseIdsInStudentExam = studentExamOfUser.getExercises().stream().map(DomainObject::getId).collect(Collectors.toList());
            assertThat(exerciseIdsOfStudentResult).containsExactlyInAnyOrderElementsOf(exerciseIdsInStudentExam);
            for (Map.Entry<Long, ExamScoresDTO.ExerciseResult> entry : studentResult.exerciseGroupIdToExerciseResult.entrySet()) {
                var exerciseResult = entry.getValue();

                // Find the original exercise using the id in ExerciseResult
                Exercise originalExercise = studentExamOfUser.getExercises().stream().filter(exercise -> exercise.getId().equals(exerciseResult.exerciseId)).findFirst().get();

                // Check that the key is associated with the exerciseGroup which actually contains the exercise in the exerciseResult
                assertThat(originalExercise.getExerciseGroup().getId()).isEqualTo(entry.getKey());

                assertThat(exerciseResult.title).isEqualTo(originalExercise.getTitle());
                assertThat(exerciseResult.maxScore).isEqualTo(originalExercise.getMaxScore());
                assertThat(exerciseResult.achievedScore).isEqualTo(resultScore);
                assertEquals(exerciseResult.achievedPoints, originalExercise.getMaxScore() * resultScore / 100, EPSILON);
            }
        }

        // change back to instructor user
        database.changeUser("instructor1");
        // Make sure delete also works if so many objects have been created before
        request.delete("/api/courses/" + course.getId() + "/exams/" + exam.getId(), HttpStatus.OK);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testLatestExamEndDate_noStartDate_notFound() throws Exception {
        exam1.setStartDate(null);
        examRepository.save(exam1);
        request.get("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/latest-end-date", HttpStatus.NOT_FOUND, ExamInformationDTO.class);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testLatestExamEndDate() throws Exception {
        // Setup exam and user
        User user = userRepo.findOneByLogin("student1").get();

        // Set student exam without working time and save into database
        StudentExam studentExam = new StudentExam();
        studentExam.setUser(user);
        studentExam.setTestRun(false);
        studentExam = studentExamRepository.save(studentExam);

        // Add student exam to exam and save into database
        exam2.addStudentExam(studentExam);
        exam2 = examRepository.save(exam2);

        // Get latest exam end date DTO from server -> This returns the endDate as no specific student working time is set
        ExamInformationDTO examInfo = request.get("/api/courses/" + exam2.getCourse().getId() + "/exams/" + exam2.getId() + "/latest-end-date", HttpStatus.OK,
                ExamInformationDTO.class);
        // Check that latest end date is equal to endDate (no specific student working time). Do not check for equality as we lose precision when saving to the database
        assertThat(examInfo.latestIndividualEndDate).isEqualToIgnoringNanos(exam2.getEndDate());

        // Set student exam with working time and save
        studentExam.setWorkingTime(3600);
        studentExamRepository.save(studentExam);

        // Get latest exam end date DTO from server -> This returns the startDate + workingTime
        ExamInformationDTO examInfo2 = request.get("/api/courses/" + exam2.getCourse().getId() + "/exams/" + exam2.getId() + "/latest-end-date", HttpStatus.OK,
                ExamInformationDTO.class);
        // Check that latest end date is equal to startDate + workingTime
        assertThat(examInfo2.latestIndividualEndDate).isEqualToIgnoringNanos(exam2.getStartDate().plusHours(1));
    }

    @Test
    @WithMockUser(username = "instructor6", roles = "INSTRUCTOR")
    public void testCourseAndExamAccessForInstructors_notInstructorInCourse_forbidden() throws Exception {
        // Instructor6 is not instructor for the course
        // Update exam
        request.put("/api/courses/" + course1.getId() + "/exams", exam1, HttpStatus.FORBIDDEN);
        // Get exam
        request.get("/api/courses/" + course1.getId() + "/exams/" + exam1.getId(), HttpStatus.FORBIDDEN, Exam.class);
        // Add student to exam
        request.post("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/students/student1", null, HttpStatus.FORBIDDEN);
        // Generate student exams
        request.postListWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/generate-student-exams", Optional.empty(), StudentExam.class,
                HttpStatus.FORBIDDEN);
        // Generate missing exams
        request.postListWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/generate-missing-student-exams", Optional.empty(), StudentExam.class,
                HttpStatus.FORBIDDEN);
        // Start exercises
        request.postWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/student-exams/start-exercises", Optional.empty(), Integer.class,
                HttpStatus.FORBIDDEN);
        // Unlock all repositories
        request.postWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/student-exams/unlock-all-repositories", Optional.empty(), Integer.class,
                HttpStatus.FORBIDDEN);
        // Lock all repositories
        request.postWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/student-exams/lock-all-repositories", Optional.empty(), Integer.class,
                HttpStatus.FORBIDDEN);
        // Add students to exam
        request.post("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/students", Collections.singletonList(new StudentDTO()), HttpStatus.FORBIDDEN);
        // Delete student from exam
        request.delete("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/students/student1", HttpStatus.FORBIDDEN);
        // Update order of exerciseGroups
        request.put("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/exerciseGroupsOrder", new ArrayList<ExerciseGroup>(), HttpStatus.FORBIDDEN);
        // Get latest individual end date
        request.get("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/latest-end-date", HttpStatus.FORBIDDEN, ExamInformationDTO.class);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testLatestIndividualEndDate_noStudentExams() {
        final var now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        exam1.setStartDate(now.minusHours(2));
        exam1.setEndDate(now);
        final var exam = examRepository.save(exam1);
        final var latestIndividualExamEndDate = examService.getLatestIndividualExamEndDate(exam.getId());
        assertThat(latestIndividualExamEndDate.isEqual(exam.getEndDate())).isTrue();
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetAllIndividualExamEndDates_noStartDate() {
        final var now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        exam1.setStartDate(null);
        exam1.setEndDate(now);
        final var exam = examRepository.save(exam1);
        final var individualExamEndDates = examService.getAllIndividualExamEndDates(exam);
        assertThat(individualExamEndDates).isNull();
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetAllIndividualExamEndDates() {
        final var now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        exam1.setStartDate(now.minusHours(2));
        exam1.setEndDate(now);
        final var exam = examRepository.save(exam1);

        final var studentExam1 = new StudentExam();
        studentExam1.setExam(exam);
        studentExam1.setUser(users.get(0));
        studentExam1.setWorkingTime(120);
        studentExam1.setTestRun(false);
        studentExamRepository.save(studentExam1);

        final var studentExam2 = new StudentExam();
        studentExam2.setExam(exam);
        studentExam2.setUser(users.get(0));
        studentExam2.setWorkingTime(120);
        studentExam2.setTestRun(false);
        studentExamRepository.save(studentExam2);

        final var studentExam3 = new StudentExam();
        studentExam3.setExam(exam);
        studentExam3.setUser(users.get(0));
        studentExam3.setWorkingTime(60);
        studentExam3.setTestRun(false);
        studentExamRepository.save(studentExam3);

        final var individualWorkingTimes = examService.getAllIndividualExamEndDates(exam.getId());
        assertThat(individualWorkingTimes.size()).isEqualTo(2);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testIsExamOver_GracePeriod() {
        final var now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        exam1.setStartDate(now.minusHours(2));
        exam1.setEndDate(now);
        exam1.setGracePeriod(180);
        final var exam = examRepository.save(exam1);
        final var isOver = examService.isExamOver(exam.getId());
        assertThat(isOver).isFalse();
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testIsUserRegisteredForExam() {
        exam1.addRegisteredUser(users.get(0));
        final var exam = examRepository.save(exam1);
        final var isUserRegistered = examService.isUserRegisteredForExam(exam.getId(), users.get(0).getId());
        final var isCurrentUserRegistered = examService.isCurrentUserRegisteredForExam(exam.getId());
        assertThat(isUserRegistered).isTrue();
        assertThat(isCurrentUserRegistered).isFalse();
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testRegisterInstructorToExam() throws Exception {
        request.postWithoutLocation("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/students/instructor1", null, HttpStatus.FORBIDDEN, null);
    }
}
