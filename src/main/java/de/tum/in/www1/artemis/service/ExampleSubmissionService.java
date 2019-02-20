package de.tum.in.www1.artemis.service;

import de.tum.in.www1.artemis.domain.ExampleSubmission;
import de.tum.in.www1.artemis.domain.Feedback;
import de.tum.in.www1.artemis.domain.Result;
import de.tum.in.www1.artemis.domain.Submission;
import de.tum.in.www1.artemis.repository.ExampleSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ExampleSubmissionService {

    private final ExampleSubmissionRepository exampleSubmissionRepository;

    public ExampleSubmissionService(ExampleSubmissionRepository exampleSubmissionRepository) {
        this.exampleSubmissionRepository = exampleSubmissionRepository;
    }

    public Optional<ExampleSubmission> get(long id) {
        return exampleSubmissionRepository.findById(id);
    }

    /**
     * Saves the given example submission and the corresponding model and creates the result if necessary.
     * Rolls back if inserting fails - occurs for concurrent createExampleSubmission() calls.
     *
     * @param exampleSubmission the submission to save
     * @return the exampleSubmission entity
     */
    @Transactional(rollbackFor = Exception.class)
    public ExampleSubmission save(ExampleSubmission exampleSubmission) {
        return exampleSubmissionRepository.saveAndFlush(exampleSubmission);
    }

    /**
     * Given the id of an example submission, it returns the results of the linked submission, if any
     *
     * @param exampleSubmissionId the id of the example submission we want to retrieve
     * @return list of feedback for an example submission
     */
    @Transactional
    public List<Feedback> getFeedbackForExampleSubmission(Long exampleSubmissionId) {
        ExampleSubmission exampleSubmission = this.exampleSubmissionRepository.getOne(exampleSubmissionId);
        Submission submission = exampleSubmission.getSubmission();

        if (submission == null) {
            return null;
        }

        Result result = submission.getResult();

        // result.isExampleResult() can have 3 values: null, false, true. We return if it is not true
        if (result == null || result.isExampleResult() != Boolean.TRUE) {
            return null;
        }

        // TODO: create different return for different type of exercises, this is for text exercises
        return result.getFeedbacks();
    }
}
