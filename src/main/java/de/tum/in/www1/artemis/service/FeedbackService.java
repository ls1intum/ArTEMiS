package de.tum.in.www1.artemis.service;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.domain.Feedback;
import de.tum.in.www1.artemis.domain.TextBlock;
import de.tum.in.www1.artemis.domain.TextCluster;
import de.tum.in.www1.artemis.domain.TextExercise;
import de.tum.in.www1.artemis.domain.enumeration.FeedbackType;
import de.tum.in.www1.artemis.domain.enumeration.StaticCodeAnalysisTool;
import de.tum.in.www1.artemis.repository.FeedbackRepository;
import de.tum.in.www1.artemis.service.dto.StaticCodeAnalysisReportDTO;

@Service
public class FeedbackService {

    public static final String DEFAULT_FILEPATH = "notAvailable";

    private final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    private final FeedbackRepository feedbackRepository;

    // need bamboo service and result repository to create and store from old feedbacks
    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    /**
     * Find all existing Feedback Elements referencing a text block part of a TextCluster.
     *
     * @param cluster TextCluster requesting existing Feedbacks for.
     * @return Map<TextBlockId, Feedback>
     */
    public Map<String, Feedback> getFeedbackForTextExerciseInCluster(TextCluster cluster) {
        final List<String> references = cluster.getBlocks().stream().map(TextBlock::getId).collect(toList());
        final TextExercise exercise = cluster.getExercise();
        return feedbackRepository.findByReferenceInAndResult_Submission_Participation_Exercise(references, exercise).parallelStream()
                .collect(toMap(Feedback::getReference, feedback -> feedback));
    }

    /**
     * Transforms static code analysis reports to feedback objects.
     * As we reuse the Feedback entity to store static code analysis findings, a mapping to those attributes
     * has to be defined, violating the first normal form.
     *
     * Mapping:
     * - text: STATIC_CODE_ANALYSIS_FEEDBACK_IDENTIFIER
     * - reference: Tool
     * - detailText: Issue object as JSON
     *
     * @param reports Static code analysis reports to be transformed
     * @return Feedback objects representing the static code analysis findings
     */
    public List<Feedback> createFeedbackFromStaticCodeAnalysisReports(List<StaticCodeAnalysisReportDTO> reports) {
        ObjectMapper mapper = new ObjectMapper();
        List<Feedback> feedbackList = new ArrayList<>();
        for (final var report : reports) {
            StaticCodeAnalysisTool tool = report.getTool();

            for (final var issue : report.getIssues()) {
                // Remove CI specific path segments
                issue.setFilePath(removeCIDirectoriesFromPath(issue.getFilePath()));

                Feedback feedback = new Feedback();
                feedback.setText(Feedback.STATIC_CODE_ANALYSIS_FEEDBACK_IDENTIFIER);
                feedback.setReference(tool.name());
                feedback.setType(FeedbackType.AUTOMATIC);
                feedback.setPositive(false);

                // Store static code analysis in JSON format
                try {
                    feedback.setDetailText(mapper.writeValueAsString(issue));
                }
                catch (JsonProcessingException e) {
                    log.error("Failed to serialize static code analysis issue: " + issue);
                    continue;
                }
                feedbackList.add(feedback);
            }
        }
        return feedbackList;
    }

    /**
     * Removes CI specific path segments. Uses the assignment directory to decide where to cut the path.
     *
     * @param sourcePath Path to be shortened
     * @return Shortened path if it contains an assignment directory, otherwise the full path
     */
    private String removeCIDirectoriesFromPath(String sourcePath) {
        if (sourcePath == null || sourcePath.isEmpty()) {
            return DEFAULT_FILEPATH;
        }
        int workingDirectoryStart = sourcePath.indexOf(Constants.ASSIGNMENT_DIRECTORY);
        if (workingDirectoryStart == -1) {
            return sourcePath;
        }
        return sourcePath.substring(workingDirectoryStart + Constants.ASSIGNMENT_DIRECTORY.length());
    }
}
