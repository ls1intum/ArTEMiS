package de.tum.in.www1.artemis.service;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;
import de.tum.in.www1.artemis.web.rest.errors.EntityNotFoundException;
import de.tum.in.www1.artemis.web.rest.errors.InternalServerErrorException;

@Service
public class AssessmentService {

    private final ComplaintResponseService complaintResponseService;

    private final ComplaintRepository complaintRepository;

    protected final FeedbackRepository feedbackRepository;

    protected final ResultRepository resultRepository;

    private final StudentParticipationRepository studentParticipationRepository;

    private final ResultService resultService;

    private final SubmissionRepository submissionRepository;

    public AssessmentService(ComplaintResponseService complaintResponseService, ComplaintRepository complaintRepository, FeedbackRepository feedbackRepository,
            ResultRepository resultRepository, StudentParticipationRepository studentParticipationRepository, ResultService resultService,
            SubmissionRepository submissionRepository) {
        this.complaintResponseService = complaintResponseService;
        this.complaintRepository = complaintRepository;
        this.feedbackRepository = feedbackRepository;
        this.resultRepository = resultRepository;
        this.studentParticipationRepository = studentParticipationRepository;
        this.resultService = resultService;
        this.submissionRepository = submissionRepository;
    }

    Result submitResult(Result result, Exercise exercise, Double calculatedScore) {
        Double maxScore = exercise.getMaxScore();
        result.setRatedIfNotExceeded(exercise.getDueDate(), result.getSubmission().getSubmissionDate());
        result.setCompletionDate(ZonedDateTime.now());
        double totalScore = calculateTotalScore(calculatedScore, maxScore);
        result.setScore(totalScore, maxScore);
        result.setResultString(totalScore, maxScore);
        return resultRepository.save(result);
    }

    /**
     * Handles an assessment update after a complaint. It first saves the corresponding complaint response and then updates the Result that was complaint about. Note, that it
     * updates the score and the feedback of the original Result, but NOT the assessor. The user that is responsible for the update can be found in the 'reviewer' field of the
     * complaint. The original Result gets stored in the 'resultBeforeComplaint' field of the ComplaintResponse for future lookup.
     *
     * @param originalResult   the original assessment that was complained about
     * @param exercise         the exercise to which the result belongs
     * @param assessmentUpdate the assessment update containing a ComplaintResponse and the updated Feedback list
     * @return the updated Result
     */
    // NOTE: transactional makes sense here because we change multiple objects in the database and the changes might be invalid in case, one save operation fails
    @Transactional
    public Result updateAssessmentAfterComplaint(Result originalResult, Exercise exercise, AssessmentUpdate assessmentUpdate) {
        if (assessmentUpdate.getFeedbacks() == null || assessmentUpdate.getComplaintResponse() == null) {
            throw new BadRequestAlertException("Feedbacks and complaint response must not be null.", "AssessmentUpdate", "notnull");
        }
        // Save the complaint response
        ComplaintResponse complaintResponse = complaintResponseService.createComplaintResponse(assessmentUpdate.getComplaintResponse());

        try {
            // Store the original result with the complaint
            Complaint complaint = complaintResponse.getComplaint();
            complaint.setResultBeforeComplaint(resultService.getOriginalResultAsString(originalResult));
            complaintRepository.save(complaint);
        }
        catch (JsonProcessingException exception) {
            throw new InternalServerErrorException("Failed to store original result");
        }

        // Update the result that was complained about with the new feedback
        originalResult.updateAllFeedbackItems(assessmentUpdate.getFeedbacks());
        if (!(exercise instanceof ProgrammingExercise)) {
            // tutors can define the manual result string and score in programming exercises, therefore we must not update these values here!
            originalResult.evaluateFeedback(exercise.getMaxScore());
        }
        // Note: This also saves the feedback objects in the database because of the 'cascade =
        // CascadeType.ALL' option.
        return resultRepository.save(originalResult);
    }

    /**
     * Cancel an assessment of a given submission for the current user, i.e. delete the corresponding result / release the lock. Then the submission is available for assessment
     * again.
     *
     * @param submission the submission for which the current assessment should be canceled
     */
    @Transactional // NOTE: As we use delete methods with underscores, we need a transactional context here!
    public void cancelAssessmentOfSubmission(Submission submission) {
        StudentParticipation participation = studentParticipationRepository.findByIdWithEagerResults(submission.getParticipation().getId())
                .orElseThrow(() -> new BadRequestAlertException("Participation could not be found", "participation", "notfound"));
        Result result = submission.getResult();
        participation.removeResult(result);
        feedbackRepository.deleteByResult_Id(result.getId());
        resultRepository.deleteById(result.getId());
    }

    /**
     * Finds the example result for the given submission ID. The submission has to be an example submission
     *
     * @param submissionId The ID of the submission for which the result should be fetched
     * @return The example result, which is linked to the submission
     */
    public Submission getSubmissionOfExampleSubmissionWithResult(long submissionId) {
        return submissionRepository.findSubmissionWithExampleSubmissionByIdWithEagerResult(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Example Submission with id \"" + submissionId + "\" does not exist"));
    }

    /**
     * Checks the assessment for general (without reference) feedback entries. Throws a BadRequestAlertException if there is more than one general feedback.
     *
     * @param assessment the assessment to check
     */
    void checkGeneralFeedback(List<Feedback> assessment) {
        final long generalFeedbackCount = assessment.stream().filter(feedback -> feedback.getReference() == null).count();
        if (generalFeedbackCount > 1) {
            throw new BadRequestAlertException("There cannot be more than one general Feedback per Assessment", "assessment", "moreThanOneGeneralFeedback");
        }
    }

    private double calculateTotalScore(Double calculatedScore, Double maxScore) {
        double totalScore = Math.max(0, calculatedScore);
        return (maxScore == null) ? totalScore : Math.min(totalScore, maxScore);
    }

    /**
     * Helper function to calculate the total score of a feedback list. It loops through all assessed model elements and sums the credits up.
     *
     * @param assessments the List of Feedback
     * @return the total score
     */
    protected Double calculateTotalScore(List<Feedback> assessments) {
        return assessments.stream().mapToDouble(Feedback::getCredits).sum();
    }
}
