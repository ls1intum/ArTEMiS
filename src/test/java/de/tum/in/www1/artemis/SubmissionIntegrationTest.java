package de.tum.in.www1.artemis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.AssessmentType;
import de.tum.in.www1.artemis.repository.*;

public class SubmissionIntegrationTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    @Autowired
    SubmissionRepository submissionRepository;

    @Autowired
    ResultRepository resultRepository;

    @Test
    public void addMultipleResultsToOneSubmission() {
        AssessmentType assessmentType = AssessmentType.MANUAL;

        Submission submission = new TextSubmission();
        submission = submissionRepository.save(submission);

        Result result1 = new Result().assessmentType(assessmentType).resultString("x points of y").score(100L).rated(true);
        result1 = resultRepository.save(result1);
        result1.setSubmission(submission);

        Result result2 = new Result().assessmentType(assessmentType).resultString("x points of y 2").score(200L).rated(true);
        result2 = resultRepository.save(result2);
        result2.setSubmission(submission);

        submission.addResult(result1);
        submission.addResult(result2);

        var savedSubmission = submissionRepository.save(submission);
        submission = submissionRepository.findWithEagerResultsAndAssessorById(savedSubmission.getId()).orElseThrow();

        assert submission.getResults() != null;
        assertThat(submission.getResults().size()).isEqualTo(2);
        assertThat(submission.getFirstResult()).isNotEqualTo(submission.getLatestResult());
        assertThat(submission.getFirstResult()).isEqualTo(result1);
        assertThat(submission.getLatestResult()).isEqualTo(result2);

    }

    @Test
    public void addMultipleResultsToOneSubmissionSavedSequentially() {
        AssessmentType assessmentType = AssessmentType.MANUAL;

        Submission submission = new TextSubmission();
        submission = submissionRepository.save(submission);

        Result result1 = new Result().assessmentType(assessmentType).resultString("x points of y").score(100L).rated(true);
        result1 = resultRepository.save(result1);
        result1.setSubmission(submission);

        submission.addResult(result1);
        submission = submissionRepository.save(submission);

        Result result2 = new Result().assessmentType(assessmentType).resultString("x points of y 2").score(200L).rated(true);
        result2 = resultRepository.save(result2);
        result2.setSubmission(submission);

        submission.addResult(result2);
        submission = submissionRepository.save(submission);

        submission = submissionRepository.findWithEagerResultsAndAssessorById(submission.getId()).orElseThrow();

        assert submission.getResults() != null;
        assertThat(submission.getResults().size()).isEqualTo(2);
        assertThat(submission.getFirstResult()).isNotEqualTo(submission.getLatestResult());
        assertThat(submission.getFirstResult()).isEqualTo(result1);
        assertThat(submission.getLatestResult()).isEqualTo(result2);

    }

    @Test
    public void updateMultipleResultsFromOneSubmission() {
        AssessmentType assessmentType = AssessmentType.MANUAL;

        Submission submission = new TextSubmission();
        submission = submissionRepository.save(submission);

        Result result1 = new Result().assessmentType(assessmentType).resultString("Points 1").score(100L).rated(true);
        result1 = resultRepository.save(result1);
        result1.setSubmission(submission);

        submission.addResult(result1);
        submission = submissionRepository.save(submission);

        Result result2 = new Result().assessmentType(assessmentType).resultString("Points 2").score(200L).rated(true);
        result2 = resultRepository.save(result2);
        result2.setSubmission(submission);

        submission.addResult(result2);
        submission = submissionRepository.save(submission);

        result1.setResultString("New Result #1");
        result1 = resultRepository.save(result1);

        result2.setResultString("New Result #2");
        result2 = resultRepository.save(result2);

        submission = submissionRepository.findWithEagerResultsAndAssessorById(submission.getId()).orElseThrow();

        assert submission.getResults() != null;
        assertThat(submission.getResults().size()).isEqualTo(2);
        assertThat(submission.getFirstResult()).isNotEqualTo(submission.getLatestResult());
        assertThat(submission.getFirstResult()).isEqualTo(result1);
        assertThat(submission.getLatestResult()).isEqualTo(result2);

    }

}
