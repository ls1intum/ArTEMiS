package de.tum.in.www1.artemis.service;

import static de.tum.in.www1.artemis.config.Constants.MAX_COMPLAINT_NUMBER_PER_STUDENT;

import java.security.Principal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.ComplaintType;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.domain.modeling.ModelingSubmission;
import de.tum.in.www1.artemis.repository.ComplaintRepository;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;
import de.tum.in.www1.artemis.web.rest.errors.InternalServerErrorException;

/**
 * Service for managing complaints.
 */
@Service
public class ComplaintService {

    private static final String ENTITY_NAME = "complaint";

    private ComplaintRepository complaintRepository;

    private ResultRepository resultRepository;

    private ResultService resultService;

    public ComplaintService(ComplaintRepository complaintRepository, ResultRepository resultRepository, ResultService resultService) {
        this.complaintRepository = complaintRepository;
        this.resultRepository = resultRepository;
        this.resultService = resultService;
    }

    /**
     * Create a new complaint checking the user has still enough complaint to create
     *
     * @param complaint the complaint to create
     * @param principal the current Principal
     * @return the saved complaint
     */
    @Transactional
    public Complaint createComplaint(Complaint complaint, Principal principal) {
        Result originalResult = resultRepository.findByIdWithEagerFeedbacksAndAssessor(complaint.getResult().getId())
                .orElseThrow(() -> new BadRequestAlertException("The result you are referring to does not exist", ENTITY_NAME, "resultnotfound"));
        StudentParticipation studentParticipation = (StudentParticipation) originalResult.getParticipation();
        User student = studentParticipation.getStudent();
        Long courseId = studentParticipation.getExercise().getCourse().getId();

        long numberOfUnacceptedComplaints = countUnacceptedComplaintsByStudentIdAndCourseId(student.getId(), courseId);
        if (numberOfUnacceptedComplaints >= MAX_COMPLAINT_NUMBER_PER_STUDENT && complaint.getComplaintType() == ComplaintType.COMPLAINT) {
            throw new BadRequestAlertException("You cannot have more than " + MAX_COMPLAINT_NUMBER_PER_STUDENT + " open or rejected complaints at the same time.", ENTITY_NAME,
                    "toomanycomplaints");
        }
        if (!isTimeOfComplaintValid(originalResult, studentParticipation.getExercise())) {
            throw new BadRequestAlertException("You cannot submit a complaint for a result that is older than one week.", ENTITY_NAME, "resultolderthanaweek");
        }
        if (!student.getLogin().equals(principal.getName())) {
            throw new BadRequestAlertException("You can create a complaint only for a result you submitted", ENTITY_NAME, "differentuser");
        }

        originalResult.setHasComplaint(true);

        complaint.setSubmittedTime(ZonedDateTime.now());
        complaint.setStudent(student);
        complaint.setResult(originalResult);
        try {
            // Store the original result with the complaint
            complaint.setResultBeforeComplaint(resultService.getOriginalResultAsString(originalResult));
        }
        catch (JsonProcessingException exception) {
            throw new InternalServerErrorException("Failed to store original result");
        }

        resultRepository.save(originalResult);

        return complaintRepository.save(complaint);
    }

    @Transactional(readOnly = true)
    public Optional<Complaint> getById(long complaintId) {
        return complaintRepository.findById(complaintId);
    }

    @Transactional(readOnly = true)
    public Optional<Complaint> getByResultId(long resultId) {
        return complaintRepository.findByResult_Id(resultId);
    }

    @Transactional(readOnly = true)
    public long countUnacceptedComplaintsByStudentIdAndCourseId(long studentId, long courseId) {
        return complaintRepository.countUnacceptedComplaintsByComplaintTypeStudentIdAndCourseId(studentId, courseId);
    }

    @Transactional(readOnly = true)
    public long countComplaintsByCourseId(long courseId) {
        return complaintRepository.countByResult_Participation_Exercise_Course_IdAndComplaintType(courseId, ComplaintType.COMPLAINT);
    }

    @Transactional(readOnly = true)
    public long countMoreFeedbackRequestsByCourseId(long courseId) {
        return complaintRepository.countByResult_Participation_Exercise_Course_IdAndComplaintType(courseId, ComplaintType.MORE_FEEDBACK);
    }

    @Transactional(readOnly = true)
    public long countComplaintsByExerciseId(long exerciseId) {
        return complaintRepository.countByResult_Participation_Exercise_IdAndComplaintType(exerciseId, ComplaintType.COMPLAINT);
    }

    @Transactional(readOnly = true)
    public long countMoreFeedbackRequestsByExerciseId(long exerciseId) {
        return complaintRepository.countByResult_Participation_Exercise_IdAndComplaintType(exerciseId, ComplaintType.MORE_FEEDBACK);
    }

    /**
     * Given an exercise id, retrieve all the complaints apart the ones related to whoever is calling the method. Useful for creating a list of complaints a tutor can review.
     *
     * @param exerciseId - the id of the exercise we are interested in
     * @param principal  - the callee
     * @return a list of complaints
     */
    @Transactional(readOnly = true)
    public List<Complaint> getAllComplaintsByExerciseIdButMine(long exerciseId, Principal principal) {
        Optional<List<Complaint>> databaseComplaints = complaintRepository.findByResult_Participation_Exercise_Id_ComplaintTypeWithEagerSubmissionAndEagerAssessor(exerciseId,
                ComplaintType.COMPLAINT);

        return buildComplaintsListForAssessor(databaseComplaints, principal, false);
    }

    /**
     * Given an exercise id, retrieve more feedback requests related to whoever is calling the method. Useful for creating a list of more feedback requests a tutor can review.
     *
     * @param exerciseId - the id of the exercise we are interested in
     * @param principal  - the callee
     * @return a list of complaints
     */
    @Transactional(readOnly = true)
    public List<Complaint> getMyMoreFeedbackRequests(long exerciseId, Principal principal) {
        Optional<List<Complaint>> databaseMoreFeedbackRequests = complaintRepository
                .findByResult_Participation_Exercise_Id_ComplaintTypeWithEagerSubmissionAndEagerAssessor(exerciseId, ComplaintType.MORE_FEEDBACK);

        return buildComplaintsListForAssessor(databaseMoreFeedbackRequests, principal, true);
    }

    @Transactional(readOnly = true)
    public List<Complaint> getAllComplaintsByTutorId(Long tutorId) {
        List<Complaint> complaints = complaintRepository.getAllByResult_Assessor_Id(tutorId);

        return filterOutUselessDataFromComplaints(complaints, true);
    }

    @Transactional(readOnly = true)
    public List<Complaint> getAllComplaintsByCourseId(Long courseId, boolean includeStudentsName) {
        List<Complaint> complaints = complaintRepository.getAllByResult_Participation_Exercise_Course_Id(courseId);

        return filterOutUselessDataFromComplaints(complaints, !includeStudentsName);
    }

    @Transactional(readOnly = true)
    public List<Complaint> getAllComplaintsByCourseIdAndTutorId(Long courseId, Long tutorId, boolean includeStudentsName) {
        List<Complaint> complaints = complaintRepository.getAllByResult_Assessor_IdAndResult_Participation_Exercise_Course_Id(tutorId, courseId);

        return filterOutUselessDataFromComplaints(complaints, !includeStudentsName);
    }

    @Transactional(readOnly = true)
    public List<Complaint> getAllComplaintsByExerciseId(Long exerciseId, boolean includeStudentsName) {
        List<Complaint> complaints = complaintRepository.getAllByResult_Participation_Exercise_Id(exerciseId);

        return filterOutUselessDataFromComplaints(complaints, !includeStudentsName);
    }

    @Transactional(readOnly = true)
    public List<Complaint> getAllComplaintsByExerciseIdAndTutorId(Long exerciseId, Long tutorId, boolean includeStudentsName) {
        List<Complaint> complaints = complaintRepository.getAllByResult_Assessor_IdAndResult_Participation_Exercise_Id(tutorId, exerciseId);

        return filterOutUselessDataFromComplaints(complaints, !includeStudentsName);
    }

    private void filterOutStudentFromComplaint(Complaint complaint) {
        complaint.setStudent(null);
        complaint.setResultBeforeComplaint(null);

        if (complaint.getResult() != null && complaint.getResult().getParticipation() != null) {
            StudentParticipation studentParticipation = (StudentParticipation) complaint.getResult().getParticipation();
            studentParticipation.setStudent(null);
        }
    }

    private void filterOutUselessDataFromComplaint(Complaint complaint) {
        if (complaint.getResult() == null) {
            return;
        }

        StudentParticipation originalParticipation = (StudentParticipation) complaint.getResult().getParticipation();
        if (originalParticipation != null && originalParticipation.getExercise() != null) {
            Exercise exerciseWithOnlyTitle = originalParticipation.getExercise();
            if (exerciseWithOnlyTitle instanceof TextExercise) {
                exerciseWithOnlyTitle = new TextExercise();
            }
            else if (exerciseWithOnlyTitle instanceof ModelingExercise) {
                exerciseWithOnlyTitle = new ModelingExercise();
            }
            else if (exerciseWithOnlyTitle instanceof FileUploadExercise) {
                exerciseWithOnlyTitle = new FileUploadExercise();
            }
            exerciseWithOnlyTitle.setTitle(originalParticipation.getExercise().getTitle());
            exerciseWithOnlyTitle.setId(originalParticipation.getExercise().getId());

            originalParticipation.setExercise(exerciseWithOnlyTitle);
        }

        Submission originalSubmission = complaint.getResult().getSubmission();
        if (originalSubmission != null) {
            Submission submissionWithOnlyId;
            if (originalSubmission instanceof TextSubmission) {
                submissionWithOnlyId = new TextSubmission();
            }
            else if (originalSubmission instanceof ModelingSubmission) {
                submissionWithOnlyId = new ModelingSubmission();
            }
            else if (originalSubmission instanceof FileUploadSubmission) {
                submissionWithOnlyId = new FileUploadSubmission();
            }
            else {
                return;
            }
            submissionWithOnlyId.setId(originalSubmission.getId());
            complaint.getResult().setSubmission(submissionWithOnlyId);
        }
    }

    private List<Complaint> filterOutUselessDataFromComplaints(List<Complaint> complaints, boolean filterOutStudentFromComplaints) {
        if (filterOutStudentFromComplaints) {
            complaints.forEach(this::filterOutStudentFromComplaint);
        }

        complaints.forEach(this::filterOutUselessDataFromComplaint);

        return complaints;
    }

    private List<Complaint> buildComplaintsListForAssessor(Optional<List<Complaint>> databaseComplaints, Principal principal, boolean assessorSameAsCaller) {
        List<Complaint> responseComplaints = new ArrayList<>();

        if (!databaseComplaints.isPresent()) {
            return responseComplaints;
        }

        databaseComplaints.get().forEach(complaint -> {
            String submissorName = principal.getName();
            User assessor = complaint.getResult().getAssessor();

            if (assessor.getLogin().equals(submissorName) == assessorSameAsCaller) {
                // Remove data about the student
                StudentParticipation studentParticipation = (StudentParticipation) complaint.getResult().getParticipation();
                studentParticipation.setStudent(null);
                complaint.setStudent(null);
                complaint.setResultBeforeComplaint(null);

                responseComplaints.add(complaint);
            }
        });

        return responseComplaints;
    }

    /**
     * This function checks whether the student is allowed to submit a complaint or not. Submitting a complaint is allowed within one week after the student received the result. If
     * the result was submitted after the assessment due date or the assessment due date is not set, the completion date of the result is checked. If the result was submitted
     * before the assessment due date, the assessment due date is checked, as the student can only see the result after the assessment due date.
     */
    private boolean isTimeOfComplaintValid(Result result, Exercise exercise) {
        if (exercise.getAssessmentDueDate() == null || result.getCompletionDate().isAfter(exercise.getAssessmentDueDate())) {
            return result.getCompletionDate().isAfter(ZonedDateTime.now().minusWeeks(1));
        }
        return exercise.getAssessmentDueDate().isAfter(ZonedDateTime.now().minusWeeks(1));
    }
}
