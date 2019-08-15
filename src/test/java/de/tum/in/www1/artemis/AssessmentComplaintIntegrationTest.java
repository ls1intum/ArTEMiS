package de.tum.in.www1.artemis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.ComplaintType;
import de.tum.in.www1.artemis.domain.enumeration.FeedbackType;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.domain.modeling.ModelingSubmission;
import de.tum.in.www1.artemis.repository.ComplaintRepository;
import de.tum.in.www1.artemis.repository.ComplaintResponseRepository;
import de.tum.in.www1.artemis.repository.ExerciseRepository;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.util.DatabaseUtilService;
import de.tum.in.www1.artemis.util.ModelFactory;
import de.tum.in.www1.artemis.util.RequestUtilService;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@ActiveProfiles("artemis")
public class AssessmentComplaintIntegrationTest {

    @Autowired
    ExerciseRepository exerciseRepo;

    @Autowired
    RequestUtilService request;

    @Autowired
    DatabaseUtilService database;

    @Autowired
    ResultRepository resultRepo;

    @Autowired
    ComplaintRepository complaintRepo;

    @Autowired
    ComplaintResponseRepository complaintResponseRepo;

    @Autowired
    ObjectMapper mapper;

    private ModelingExercise modelingExercise;

    private ModelingSubmission modelingSubmission;

    private Result modelingAssessment;

    private Complaint complaint;

    private Complaint moreFeedbackRequest;

    @BeforeEach
    public void initTestCase() throws Exception {
        database.resetDatabase();
        database.addUsers(1, 2, 1);
        database.addCourseWithOneModelingExercise();
        modelingExercise = (ModelingExercise) exerciseRepo.findAll().get(0);
        saveModelingSubmissionAndAssessment();
        complaint = new Complaint().result(modelingAssessment).complaintText("This is not fair").complaintType(ComplaintType.COMPLAINT);
        moreFeedbackRequest = new Complaint().result(modelingAssessment).complaintText("Please explain").complaintType(ComplaintType.MORE_FEEDBACK);
    }

    @Test
    @WithMockUser(username = "student1")
    public void submitComplaintAboutModelingAssessment() throws Exception {
        request.post("/api/complaints", complaint, HttpStatus.CREATED);

        Optional<Complaint> storedComplaint = complaintRepo.findByResult_Id(modelingAssessment.getId());
        assertThat(storedComplaint).as("complaint is saved").isPresent();
        assertThat(storedComplaint.get().getComplaintText()).as("complaint text got correctly saved").isEqualTo(complaint.getComplaintText());
        assertThat(storedComplaint.get().isAccepted()).as("accepted flag of complaint is not set").isNull();
        Result storedResult = resultRepo.findByIdWithEagerFeedbacksAndAssessor(modelingAssessment.getId()).get();
        assertThat(storedResult.hasComplaint()).as("hasComplaint flag of result is true").isTrue();
        Result resultBeforeComplaint = mapper.readValue(storedComplaint.get().getResultBeforeComplaint(), Result.class);
        // set date to UTC for comparison as the date saved in resultBeforeComplaint string is in UTC
        storedResult.setCompletionDate(ZonedDateTime.ofInstant(storedResult.getCompletionDate().toInstant(), ZoneId.of("UTC")));
        assertThat(resultBeforeComplaint).as("result before complaint is correctly stored").isEqualToIgnoringGivenFields(storedResult, "participation", "submission");
    }

    @Test
    @WithMockUser(username = "student1")
    public void submitComplaintAboutModelingAssessment_complaintLimitReached() throws Exception {
        database.addComplaints("student1", modelingAssessment.getParticipation(), 3, ComplaintType.COMPLAINT);

        request.post("/api/complaints", complaint, HttpStatus.BAD_REQUEST);

        assertThat(complaintRepo.findByResult_Id(modelingAssessment.getId())).as("complaint is not saved").isNotPresent();
        Result storedResult = resultRepo.findByIdWithEagerFeedbacksAndAssessor(modelingAssessment.getId()).get();
        assertThat(storedResult.hasComplaint()).as("hasComplaint flag of result is false").isFalse();
    }

    @Test
    @WithMockUser(username = "student1")
    public void requestMoreFeedbackAboutModelingAssessment_noLimit() throws Exception {
        database.addComplaints("student1", modelingAssessment.getParticipation(), 3, ComplaintType.MORE_FEEDBACK);

        request.post("/api/complaints", complaint, HttpStatus.CREATED);

        assertThat(complaintRepo.findByResult_Id(modelingAssessment.getId())).as("complaint is saved").isPresent();
        Result storedResult = resultRepo.findByIdWithEagerFeedbacksAndAssessor(modelingAssessment.getId()).get();
        assertThat(storedResult.hasComplaint()).as("hasComplaint flag of result is true").isTrue();

        // Only one complaint is possible for exercise regardless of its type
        request.post("/api/complaints", moreFeedbackRequest, HttpStatus.BAD_REQUEST);
        assertThat(complaintRepo.findByResult_Id(modelingAssessment.getId()).get().getComplaintType() == ComplaintType.MORE_FEEDBACK).as("more feedback request is not saved")
                .isFalse();
    }

    @Test
    @WithMockUser(username = "student1")
    public void submitComplaintAboutModelingAssessment_assessmentTooOld() throws Exception {
        database.updateAssessmentDueDate(modelingExercise.getId(), ZonedDateTime.now().minusDays(8));
        database.updateResultCompletionDate(modelingAssessment.getId(), ZonedDateTime.now().minusDays(8));

        request.post("/api/complaints", complaint, HttpStatus.BAD_REQUEST);

        assertThat(complaintRepo.findByResult_Id(modelingAssessment.getId())).as("complaint is not saved").isNotPresent();
        Result storedResult = resultRepo.findByIdWithEagerFeedbacksAndAssessor(modelingAssessment.getId()).get();
        assertThat(storedResult.hasComplaint()).as("hasComplaint flag of result is false").isFalse();
    }

    @Test
    @WithMockUser(username = "tutor2", roles = "TA")
    public void submitComplaintResponse_rejectComplaint() throws Exception {
        complaintRepo.save(complaint);

        ComplaintResponse complaintResponse = new ComplaintResponse().complaint(complaint.accepted(false)).responseText("rejected");
        request.post("/api/complaint-responses", complaintResponse, HttpStatus.CREATED);

        Complaint storedComplaint = complaintRepo.findByResult_Id(modelingAssessment.getId()).get();
        assertThat(storedComplaint.isAccepted()).as("complaint is not accepted").isFalse();
        Result storedResult = resultRepo.findByIdWithEagerFeedbacks(modelingAssessment.getId()).get();
        checkFeedbackCorrectlyStored(modelingAssessment.getFeedbacks(), storedResult.getFeedbacks());
    }

    @Test
    @WithMockUser(username = "tutor2", roles = "TA")
    public void submitComplaintResponse_updateAssessment() throws Exception {
        complaint = complaintRepo.save(complaint);

        List<Feedback> feedback = database.loadAssessmentFomResources("test-data/model-assessment/assessment.54727.json");
        ComplaintResponse complaintResponse = new ComplaintResponse().complaint(complaint.accepted(true)).responseText("accepted");
        AssessmentUpdate assessmentUpdate = new AssessmentUpdate().feedbacks(feedback).complaintResponse(complaintResponse);
        Result receivedResult = request.postWithResponseBody("/api/modeling-submissions/" + modelingSubmission.getId() + "/assessment-after-complaint", assessmentUpdate,
                Result.class, HttpStatus.OK);

        assertThat(((StudentParticipation) receivedResult.getParticipation()).getStudent()).as("student is hidden in response").isNull();
        Complaint storedComplaint = complaintRepo.findByResult_Id(modelingAssessment.getId()).get();
        assertThat(storedComplaint.isAccepted()).as("complaint is accepted").isTrue();
        Result resultBeforeComplaint = mapper.readValue(storedComplaint.getResultBeforeComplaint(), Result.class);
        // set completion date to UTC for comparison as the date saved in resultBeforeComplaint string is in UTC
        modelingAssessment.setCompletionDate(ZonedDateTime.ofInstant(modelingAssessment.getCompletionDate().toInstant(), ZoneId.of("UTC")));
        assertThat(resultBeforeComplaint).as("result before complaint is correctly stored").isEqualToIgnoringGivenFields(modelingAssessment, "participation", "submission");
        Result storedResult = resultRepo.findByIdWithEagerFeedbacksAndAssessor(modelingAssessment.getId()).get();
        checkFeedbackCorrectlyStored(feedback, storedResult.getFeedbacks());
        assertThat(storedResult.getAssessor()).as("assessor is still the original one").isEqualTo(modelingAssessment.getAssessor());
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void submitComplaintResponse_asAssessor_forbidden() throws Exception {
        complaintRepo.save(complaint);

        ComplaintResponse complaintResponse = new ComplaintResponse().complaint(complaint.accepted(false)).responseText("rejected");
        request.post("/api/complaint-responses", complaintResponse, HttpStatus.FORBIDDEN);

        Complaint storedComplaint = complaintRepo.findByResult_Id(modelingAssessment.getId()).get();
        assertThat(storedComplaint.isAccepted()).as("accepted flag of complaint is not set").isNull();
    }

    @Test
    @WithMockUser(username = "student1")
    public void getComplaintByResultId_assessorHiddenForStudent() throws Exception {
        complaintRepo.save(complaint);

        Complaint receivedComplaint = request.get("/api/complaints/result/" + complaint.getResult().getId(), HttpStatus.OK, Complaint.class);

        assertThat(receivedComplaint.getResult().getAssessor()).as("assessor is not set").isNull();
    }

    @Test
    @WithMockUser(username = "student1")
    public void getComplaintResponse_reviewerHiddenForStudent() throws Exception {
        complaint.setStudent(database.getUserByLogin("student1"));
        complaintRepo.save(complaint);

        ComplaintResponse complaintResponse = new ComplaintResponse().complaint(complaint.accepted(false)).responseText("rejected").reviewer(database.getUserByLogin("tutor1"));
        complaintResponse = complaintResponseRepo.save(complaintResponse);

        ComplaintResponse receivedComplaintResponse = request.get("/api/complaint-responses/" + complaintResponse.getId(), HttpStatus.OK, ComplaintResponse.class);

        assertThat(receivedComplaintResponse.getReviewer()).as("reviewer is not set").isNull();
    }

    @Test
    @WithMockUser(username = "tutor1")
    public void getComplaintResponse_studentHiddenForTutor() throws Exception {
        complaint.setStudent(database.getUserByLogin("student1"));
        complaintRepo.save(complaint);

        ComplaintResponse complaintResponse = new ComplaintResponse().complaint(complaint.accepted(false)).responseText("rejected").reviewer(database.getUserByLogin("tutor1"));
        complaintResponse = complaintResponseRepo.save(complaintResponse);

        ComplaintResponse receivedComplaintResponse = request.get("/api/complaint-responses/" + complaintResponse.getId(), HttpStatus.OK, ComplaintResponse.class);

        assertThat(receivedComplaintResponse.getComplaint().getStudent()).as("student is not set").isNull();
    }

    @Test
    @WithMockUser(username = "student1")
    public void getComplaintResponseByComplaintId_reviewerHiddenForStudent() throws Exception {
        complaint.setStudent(database.getUserByLogin("student1"));
        complaintRepo.save(complaint);

        ComplaintResponse complaintResponse = new ComplaintResponse().complaint(complaint.accepted(false)).responseText("rejected").reviewer(database.getUserByLogin("tutor1"));
        complaintResponseRepo.save(complaintResponse);

        ComplaintResponse receivedComplaintResponse = request.get("/api/complaint-responses/complaint/" + complaint.getId(), HttpStatus.OK, ComplaintResponse.class);

        assertThat(receivedComplaintResponse.getReviewer()).as("reviewer is not set").isNull();
    }

    @Test
    @WithMockUser(username = "tutor1")
    public void getComplaintResponseByComplaintId_studentHiddenForTutor() throws Exception {
        complaint.setStudent(database.getUserByLogin("student1"));
        complaintRepo.save(complaint);

        ComplaintResponse complaintResponse = new ComplaintResponse().complaint(complaint.accepted(false)).responseText("rejected").reviewer(database.getUserByLogin("tutor1"));
        complaintResponseRepo.save(complaintResponse);

        ComplaintResponse receivedComplaintResponse = request.get("/api/complaint-responses/complaint/" + complaint.getId(), HttpStatus.OK, ComplaintResponse.class);

        assertThat(receivedComplaintResponse.getComplaint().getStudent()).as("student is not set").isNull();
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void getSubmittedComplaints_byComplaintType() throws Exception {
        database.addComplaints("student1", modelingAssessment.getParticipation(), 1, ComplaintType.COMPLAINT);
        database.addComplaints("student1", modelingAssessment.getParticipation(), 2, ComplaintType.MORE_FEEDBACK);

        String exercisesUrl = "/api/exercises/" + modelingExercise.getId() + "/complaints";
        String coursesUrl = "/api/courses/" + modelingExercise.getCourse().getId() + "/complaints";
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("complaintType", ComplaintType.COMPLAINT.toString());
        List<ComplaintResponse> complaintResponsesByCourse = request.getList(coursesUrl, HttpStatus.OK, ComplaintResponse.class, params);
        List<ComplaintResponse> complaintResponsesByExercise = request.getList(exercisesUrl, HttpStatus.OK, ComplaintResponse.class, params);
        assertThat(complaintResponsesByExercise.size()).isEqualTo(complaintResponsesByCourse.size()).isEqualTo(1);

        params.set("complaintType", ComplaintType.MORE_FEEDBACK.toString());
        complaintResponsesByCourse = request.getList(exercisesUrl, HttpStatus.OK, ComplaintResponse.class, params);
        complaintResponsesByExercise = request.getList(coursesUrl, HttpStatus.OK, ComplaintResponse.class, params);
        assertThat(complaintResponsesByCourse.size()).isEqualTo(complaintResponsesByExercise.size()).isEqualTo(2);
    }

    @Test
    @WithMockUser(username = "student1")
    public void getSubmittedComplaints_asStudent_forbidden() throws Exception {
        complaintRepo.save(complaint);

        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("complaintType", ComplaintType.COMPLAINT.toString());
        params.add("exerciseId", modelingExercise.getId().toString());

        request.getList("/api/complaints", HttpStatus.FORBIDDEN, ComplaintResponse.class, params);
    }

    private void checkFeedbackCorrectlyStored(List<Feedback> sentFeedback, List<Feedback> storedFeedback) {
        assertThat(sentFeedback.size()).as("contains the same amount of feedback").isEqualTo(storedFeedback.size());
        Result storedFeedbackResult = new Result();
        Result sentFeedbackResult = new Result();
        storedFeedbackResult.setFeedbacks(storedFeedback);
        sentFeedbackResult.setFeedbacks(sentFeedback);
        storedFeedbackResult.evaluateFeedback(20);
        sentFeedbackResult.evaluateFeedback(20);
        assertThat(storedFeedbackResult.getScore()).as("stored feedback evaluates to the same score as sent feedback").isEqualTo(sentFeedbackResult.getScore());
        storedFeedback.forEach(feedback -> {
            assertThat(feedback.getType()).as("type has been set to MANUAL").isEqualTo(FeedbackType.MANUAL);
        });
    }

    private void saveModelingSubmissionAndAssessment() throws Exception {
        modelingSubmission = ModelFactory.generateModelingSubmission(database.loadFileFromResources("test-data/model-submission/model.54727.json"), true);
        modelingSubmission = database.addModelingSubmission(modelingExercise, modelingSubmission, "student1");
        modelingAssessment = database.addModelingAssessmentForSubmission(modelingExercise, modelingSubmission, "test-data/model-assessment/assessment.54727.v2.json", "tutor1",
                true);
    }
}
