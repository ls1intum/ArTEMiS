package de.tum.in.www1.artemis.service;

import static java.util.stream.Collectors.toList;
import static org.hibernate.Hibernate.isInitialized;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.participation.Participation;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.service.connectors.LtiService;

@Service
public class TextAssessmentService extends AssessmentService {

    private final TextBlockService textBlockService;

    private final Optional<AutomaticTextFeedbackService> automaticTextFeedbackService;

    private final FeedbackConflictRepository feedbackConflictRepository;

    public TextAssessmentService(UserService userService, ComplaintResponseService complaintResponseService, ComplaintRepository complaintRepository,
            FeedbackRepository feedbackRepository, ResultRepository resultRepository, StudentParticipationRepository studentParticipationRepository, ResultService resultService,
            SubmissionRepository submissionRepository, TextBlockService textBlockService, Optional<AutomaticTextFeedbackService> automaticTextFeedbackService,
            ExamService examService, FeedbackConflictRepository feedbackConflictRepository, GradingCriterionService gradingCriterionService, SubmissionService submissionService,
            LtiService ltiService) {
        super(complaintResponseService, complaintRepository, feedbackRepository, resultRepository, studentParticipationRepository, resultService, submissionService,
                submissionRepository, examService, gradingCriterionService, userService, ltiService);
        this.textBlockService = textBlockService;
        this.automaticTextFeedbackService = automaticTextFeedbackService;
        this.feedbackConflictRepository = feedbackConflictRepository;
    }

    public List<Feedback> getAssessmentsForResult(Result result) {
        return this.feedbackRepository.findByResult(result);
    }

    /**
     * Load entities from database needed for text assessment & compute Feedback suggestions (Athene):
     *   1. Create or load the result
     *   2. Compute Feedback Suggestions
     *   3. Load Text Blocks
     *   4. Compute Fallback Text Blocks if needed
     *
     * @param textSubmission Text Submission to be assessed
     */
    public void prepareSubmissionForAssessment(TextSubmission textSubmission) {
        final Participation participation = textSubmission.getParticipation();
        final TextExercise exercise = (TextExercise) participation.getExercise();
        Result result = textSubmission.getResult();

        final boolean computeFeedbackSuggestions = automaticTextFeedbackService.isPresent() && exercise.isAutomaticAssessmentEnabled();

        if (result != null) {
            // Load Feedback already created for this assessment
            final List<Feedback> assessments = exercise.isAutomaticAssessmentEnabled() ? getAssessmentsForResultWithConflicts(result) : getAssessmentsForResult(result);
            result.setFeedbacks(assessments);
            if (assessments.isEmpty() && computeFeedbackSuggestions) {
                automaticTextFeedbackService.get().suggestFeedback(result);
            }
        }
        else {
            // We are the first ones to open assess this submission, we want to lock it.
            result = new Result();
            result.setParticipation(participation);
            result.setSubmission(textSubmission);
            resultService.createNewRatedManualResult(result, false);
            result.setCompletionDate(null);
            result = resultRepository.save(result);
            textSubmission.setResult(result);

            // If enabled, we want to compute feedback suggestions using Athene.
            if (computeFeedbackSuggestions) {
                result.setSubmission(textSubmission); // make sure this is not a Hibernate Proxy
                automaticTextFeedbackService.get().suggestFeedback(result);
            }
        }

        // If we did not call AutomaticTextFeedbackService::suggestFeedback, we need to fetch them now.
        if (!result.getFeedbacks().isEmpty() || !computeFeedbackSuggestions) {
            final var textBlocks = textBlockService.findAllBySubmissionId(textSubmission.getId());
            textSubmission.setBlocks(textBlocks);
        }

        // If we did not fetch blocks from the database before, we fall back to computing them based on syntax.
        if (textSubmission.getBlocks() == null || !isInitialized(textSubmission.getBlocks()) || textSubmission.getBlocks().isEmpty()) {
            textBlockService.computeTextBlocksForSubmissionBasedOnSyntax(textSubmission);
        }
    }

    private List<Feedback> getAssessmentsForResultWithConflicts(Result result) {
        List<Feedback> feedbackList = this.feedbackRepository.findByResult(result);
        final List<FeedbackConflict> allConflictsByFeedbackList = this.feedbackConflictRepository
                .findAllConflictsByFeedbackList(feedbackList.stream().map(Feedback::getId).collect(toList()));
        feedbackList.forEach(feedback -> {
            feedback.setFirstConflicts(allConflictsByFeedbackList.stream().filter(c -> c.getFirstFeedback().getId().equals(feedback.getId())).collect(toList()));
            feedback.setSecondConflicts(allConflictsByFeedbackList.stream().filter(c -> c.getSecondFeedback().getId().equals(feedback.getId())).collect(toList()));
        });
        return feedbackList;
    }
}
