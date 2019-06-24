package de.tum.in.www1.artemis.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.*;
import java.time.ZonedDateTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.DiagramType;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.domain.modeling.ModelingSubmission;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.service.ModelingAssessmentService;
import de.tum.in.www1.artemis.service.ModelingSubmissionService;

/** Service responsible for initializing the database with specific testdata for a testscenario */
@Service
public class DatabaseUtilService {

    private static ZonedDateTime pastTimestamp = ZonedDateTime.now().minusDays(1);

    private static ZonedDateTime futureTimestamp = ZonedDateTime.now().plusDays(1);

    private static ZonedDateTime futureFutureTimestamp = ZonedDateTime.now().plusDays(2);

    @Autowired
    CourseRepository courseRepo;

    @Autowired
    ExerciseRepository exerciseRepo;

    @Autowired
    UserRepository userRepo;

    @Autowired
    ResultRepository resultRepo;

    @Autowired
    ParticipationRepository participationRepo;

    @Autowired
    ModelingSubmissionRepository modelingSubmissionRepo;

    @Autowired
    ModelAssessmentConflictRepository conflictRepo;

    @Autowired
    ConflictingResultRepository conflictingResultRepo;

    @Autowired
    FeedbackRepository feedbackRepo;

    @Autowired
    ComplaintRepository complaintRepo;

    @Autowired
    ComplaintResponseRepository complaintResponseRepo;

    @Autowired
    ModelingSubmissionService modelSubmissionService;

    @Autowired
    ModelingAssessmentService modelingAssessmentService;

    @Autowired
    ObjectMapper mapper;

    public void resetDatabase() {
        conflictRepo.deleteAll();
        conflictingResultRepo.deleteAll();
        complaintResponseRepo.deleteAll();
        complaintRepo.deleteAll();
        resultRepo.deleteAll();
        feedbackRepo.deleteAll();
        modelingSubmissionRepo.deleteAll();
        participationRepo.deleteAll();
        exerciseRepo.deleteAll();
        courseRepo.deleteAll();
        userRepo.deleteAll();
        assertThat(courseRepo.findAll()).as("course data has been cleared").isEmpty();
        assertThat(exerciseRepo.findAll()).as("exercise data has been cleared").isEmpty();
        assertThat(userRepo.findAll()).as("user data has been cleared").isEmpty();
    }

    /**
     * Adds the provided number of students and tutors into the user repository. Students login is a concatenation of the prefix "student" and a number counting from 1 to
     * numberOfStudents Tutors login is a concatenation of the prefix "tutor" and a number counting from 1 to numberOfStudents Tutors are all in the "tutor" group and students in
     * the "tumuser" group
     *
     * @param numberOfStudents
     * @param numberOfTutors
     */
    public void addUsers(int numberOfStudents, int numberOfTutors) {
        LinkedList<User> students = ModelFactory.generateActivatedUsers("student", new String[] { "tumuser" }, numberOfStudents);
        LinkedList<User> tutors = ModelFactory.generateActivatedUsers("tutor", new String[] { "tutor" }, numberOfTutors);
        LinkedList<User> usersToAdd = new LinkedList<>();
        usersToAdd.addAll(students);
        usersToAdd.addAll(tutors);
        userRepo.saveAll(usersToAdd);
        assertThat(userRepo.findAll().size()).as("all users are created").isEqualTo(numberOfStudents + numberOfTutors);
        assertThat(userRepo.findAll()).as("users are correctly stored").containsAnyOf(usersToAdd.toArray(new User[0]));
    }

    /**
     * Stores participation of the user with the given login for the given exercise
     *
     * @param exercise the exercise for which the participation will be created
     * @param login    login of the user
     * @return eagerly loaded representation of the participation object stored in the database
     */
    public Participation addParticipationForExercise(Exercise exercise, String login) {
        Optional<Participation> storedParticipation = participationRepo.findByExerciseIdAndStudentLogin(exercise.getId(), login);
        if (storedParticipation.isPresent()) {
            return storedParticipation.get();
        }
        User user = getUserByLogin(login);
        Participation participation = new Participation();
        participation.setStudent(user);
        participation.setExercise(exercise);
        participationRepo.save(participation);
        storedParticipation = participationRepo.findByExerciseIdAndStudentLogin(exercise.getId(), login);
        assertThat(storedParticipation).isPresent();
        return participationRepo.findByIdWithEagerSubmissionsAndEagerResultsAndEagerAssessors(storedParticipation.get().getId()).get();
    }

    public void addCourseWithOneModelingExercise() {
        Course course = ModelFactory.generateCourse(null, pastTimestamp, futureFutureTimestamp, new HashSet<>(), "tumuser", "tutor", "tutor");
        ModelingExercise modelingExercise = ModelFactory.generateModelingExercise(pastTimestamp, futureTimestamp, futureFutureTimestamp, DiagramType.ClassDiagram, course);
        course.addExercises(modelingExercise);
        courseRepo.save(course);
        exerciseRepo.save(modelingExercise);
        List<Course> courseRepoContent = courseRepo.findAllActiveWithEagerExercises();
        List<Exercise> exerciseRepoContent = exerciseRepo.findAll();
        assertThat(exerciseRepoContent.size()).as("one exercise got stored").isEqualTo(1);
        assertThat(courseRepoContent.size()).as("a course got stored").isEqualTo(1);
        assertThat(courseRepoContent.get(0).getExercises()).as("course contains the exercise").containsExactlyInAnyOrder(exerciseRepoContent.toArray(new Exercise[] {}));
    }

    public void addCourseWithDifferentModelingExercises() {
        Course course = ModelFactory.generateCourse(null, pastTimestamp, futureFutureTimestamp, new HashSet<>(), "tumuser", "tutor", "tutor");
        ModelingExercise classExercise = ModelFactory.generateModelingExercise(pastTimestamp, futureTimestamp, futureFutureTimestamp, DiagramType.ClassDiagram, course);
        course.addExercises(classExercise);
        ModelingExercise activityExercise = ModelFactory.generateModelingExercise(pastTimestamp, futureTimestamp, futureFutureTimestamp, DiagramType.ActivityDiagram, course);
        course.addExercises(activityExercise);
        ModelingExercise objectExercise = ModelFactory.generateModelingExercise(pastTimestamp, futureTimestamp, futureFutureTimestamp, DiagramType.ObjectDiagram, course);
        course.addExercises(objectExercise);
        ModelingExercise useCaseExercise = ModelFactory.generateModelingExercise(pastTimestamp, futureTimestamp, futureFutureTimestamp, DiagramType.UseCaseDiagram, course);
        course.addExercises(useCaseExercise);
        courseRepo.save(course);
        exerciseRepo.save(classExercise);
        exerciseRepo.save(activityExercise);
        exerciseRepo.save(objectExercise);
        exerciseRepo.save(useCaseExercise);
        List<Course> courseRepoContent = courseRepo.findAllActiveWithEagerExercises();
        List<Exercise> exerciseRepoContent = exerciseRepo.findAll();
        assertThat(exerciseRepoContent.size()).as("four exercises got stored").isEqualTo(4);
        assertThat(courseRepoContent.size()).as("a course got stored").isEqualTo(1);
        assertThat(courseRepoContent.get(0).getExercises().size()).as("Course contains exercise").isEqualTo(4);
        assertThat(courseRepoContent.get(0).getExercises()).as("Contains all exercises").containsExactlyInAnyOrder(exerciseRepoContent.toArray(new Exercise[] {}));
    }

    /**
     * Stores for the given model a submission of the user and initiates the corresponding Result
     *
     * @param exercise exercise the submission belongs to
     * @param model    ModelingSubmission json as string contained in the submission
     * @param login    of the user the submission belongs to
     * @return submission stored in the modelingSubmissionRepository
     */
    public ModelingSubmission addModelingSubmissionWithEmptyResult(ModelingExercise exercise, String model, String login) {
        Participation participation = addParticipationForExercise(exercise, login);
        ModelingSubmission submission = ModelFactory.generateModelingSubmission(model, true);
        submission = modelSubmissionService.save(submission, exercise, login);
        Result result = new Result();
        result.setSubmission(submission);
        submission.setResult(result);
        participation.addResult(result);
        resultRepo.save(result);
        participationRepo.save(participation);
        modelingSubmissionRepo.save(submission);
        return submission;
    }

    @Transactional
    public ModelingSubmission addModelingSubmission(ModelingExercise exercise, ModelingSubmission submission, String login) {
        Participation participation = addParticipationForExercise(exercise, login);
        participation.addSubmissions(submission);
        submission.setParticipation(participation);
        modelingSubmissionRepo.save(submission);
        participationRepo.save(participation);
        return submission;
    }

    @Transactional
    public ModelingSubmission addModelingSubmissionWithResultAndAssessor(ModelingExercise exercise, ModelingSubmission submission, String login, String assessorLogin) {
        Participation participation = addParticipationForExercise(exercise, login);
        participation.addSubmissions(submission);
        Result result = new Result();
        result.setSubmission(submission);
        result.setAssessor(getUserByLogin(assessorLogin));
        submission.setParticipation(participation);
        submission.setResult(result);
        submission.getParticipation().addResult(result);
        modelingSubmissionRepo.save(submission);
        resultRepo.save(result);
        participationRepo.save(participation);
        return submission;
    }

    public ModelingSubmission addModelingSubmissionFromResources(ModelingExercise exercise, String path, String login) throws Exception {
        String model = loadFileFromResources(path);
        ModelingSubmission submission = ModelFactory.generateModelingSubmission(model, true);
        submission = addModelingSubmission(exercise, submission, login);
        checkSubmissionCorrectlyStored(submission.getId(), model);
        return submission;
    }

    public void checkSubmissionCorrectlyStored(Long submissionId, String sentModel) throws Exception {
        String storedModel = modelingSubmissionRepo.findById(submissionId).get().getModel();
        JsonParser parser = new JsonParser();
        JsonObject sentModelObject = parser.parse(sentModel).getAsJsonObject();
        JsonObject storedModelObject = parser.parse(storedModel).getAsJsonObject();
        assertThat(storedModelObject).as("model correctly stored").isEqualTo(sentModelObject);
    }

    public Result addModelingAssessmentForSubmission(ModelingExercise exercise, ModelingSubmission submission, String path, String login) throws Exception {
        List<Feedback> assessment = loadAssessmentFomResources(path);
        Result result = modelingAssessmentService.saveManualAssessment(submission, assessment, exercise);
        result.setParticipation(submission.getParticipation().results(null));
        result.setAssessor(getUserByLogin(login));
        result = modelingAssessmentService.submitManualAssessment(result, exercise, submission.getSubmissionDate());
        return result;
    }

    /**
     * @param path path relative to the test resources folder
     * @return string representation of given file
     * @throws Exception
     */
    public String loadFileFromResources(String path) throws Exception {
        java.io.File file = ResourceUtils.getFile("classpath:" + path);
        StringBuilder builder = new StringBuilder();
        Files.lines(file.toPath()).forEach(builder::append);
        assertThat(builder.toString()).as("model has been correctly read from file").isNotEqualTo("");
        return builder.toString();
    }

    public List<Feedback> loadAssessmentFomResources(String path) throws Exception {
        String fileContent = loadFileFromResources(path);
        List<Feedback> modelingAssessment = mapper.readValue(fileContent, mapper.getTypeFactory().constructCollectionType(List.class, Feedback.class));
        return modelingAssessment;
    }

    public User getUserByLogin(String login) {
        return userRepo.findOneByLogin(login).orElseThrow(() -> new IllegalArgumentException("Provided login does not exist in database"));
    }

    public void updateExerciseDueDate(long exerciseId, ZonedDateTime newDueDate) {
        Exercise exercise = exerciseRepo.findById(exerciseId).orElseThrow(() -> new IllegalArgumentException("Exercise with given ID could not be found"));
        exercise.setDueDate(newDueDate);
        exerciseRepo.save(exercise);
    }

    public void updateAssessmentDueDate(long exerciseId, ZonedDateTime newDueDate) {
        Exercise exercise = exerciseRepo.findById(exerciseId).orElseThrow(() -> new IllegalArgumentException("Exercise with given ID could not be found"));
        exercise.setAssessmentDueDate(newDueDate);
        exerciseRepo.save(exercise);
    }

    public void updateResultCompletionDate(long resultId, ZonedDateTime newCompletionDate) {
        Result result = resultRepo.findById(resultId).orElseThrow(() -> new IllegalArgumentException("Result with given ID could not be found"));
        result.setCompletionDate(newCompletionDate);
        resultRepo.save(result);
    }

    public void addComplaints(String studentLogin, Participation participation, int numberOfComplaints) {
        for (int i = 0; i < numberOfComplaints; i++) {
            Result dummyResult = new Result().participation(participation);
            dummyResult = resultRepo.save(dummyResult);
            Complaint complaint = new Complaint().student(getUserByLogin(studentLogin)).result(dummyResult);
            complaintRepo.save(complaint);
        }
    }
}
