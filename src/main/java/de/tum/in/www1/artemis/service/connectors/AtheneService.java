package de.tum.in.www1.artemis.service.connectors;

import static de.tum.in.www1.artemis.config.Constants.ATHENE_RESULT_API_PATH;
import static de.tum.in.www1.artemis.service.connectors.RemoteArtemisServiceConnector.authorizationHeaderForSymmetricSecret;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.*;

import javax.validation.constraints.NotNull;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.Language;
import de.tum.in.www1.artemis.exception.NetworkingError;
import de.tum.in.www1.artemis.repository.TextBlockRepository;
import de.tum.in.www1.artemis.repository.TextClusterRepository;
import de.tum.in.www1.artemis.repository.TextExerciseRepository;
import de.tum.in.www1.artemis.service.TextAssessmentQueueService;
import de.tum.in.www1.artemis.service.TextSubmissionService;
import de.tum.in.www1.artemis.web.rest.dto.AtheneDTO;

@Service
@Profile("athene")
public class AtheneService {

    private final Logger log = LoggerFactory.getLogger(AtheneService.class);

    @Value("${server.url}")
    private String artemisServerUrl;

    @Value("${artemis.athene.submit-url}")
    private String submitApiEndpoint;

    @Value("${artemis.athene.base64-secret}")
    private String apiSecret;

    private final TextAssessmentQueueService textAssessmentQueueService;

    private final TextBlockRepository textBlockRepository;

    private final TextClusterRepository textClusterRepository;

    private final TextExerciseRepository textExerciseRepository;

    private final TextSubmissionService textSubmissionService;

    private final RemoteArtemisServiceConnector<RequestDTO, ResponseDTO> connector = new RemoteArtemisServiceConnector<>(log, ResponseDTO.class);

    // Contains tasks submitted to Athene and currently processing
    private final List<Long> runningAtheneTasks = new ArrayList<>();

    public AtheneService(TextSubmissionService textSubmissionService, TextBlockRepository textBlockRepository, TextClusterRepository textClusterRepository,
            TextExerciseRepository textExerciseRepository, TextAssessmentQueueService textAssessmentQueueService) {
        this.textSubmissionService = textSubmissionService;
        this.textBlockRepository = textBlockRepository;
        this.textClusterRepository = textClusterRepository;
        this.textExerciseRepository = textExerciseRepository;
        this.textAssessmentQueueService = textAssessmentQueueService;
    }

    // region Request/Response DTOs
    private static class RequestDTO {

        public long courseId;

        public String callbackUrl;

        public List<TextSubmission> submissions;

        RequestDTO(@NotNull long courseId, @NotNull List<TextSubmission> submissions, @NotNull String callbackUrl) {
            this.courseId = courseId;
            this.callbackUrl = callbackUrl;
            this.submissions = createSubmissionDTOs(submissions);
        }

        /**
         * Converts TextSubmissions to DTO objects to prepare for sending them to Athene in a REST call.
         */
        @NotNull
        private static List<TextSubmission> createSubmissionDTOs(@NotNull List<TextSubmission> submissions) {
            return submissions.stream().map(textSubmission -> {
                final TextSubmission submission = new TextSubmission();
                submission.setText(textSubmission.getText());
                submission.setId(textSubmission.getId());
                return submission;
            }).collect(toList());
        }
    }

    private static class ResponseDTO {

        public String detail;

    }
    // endregion

    /**
     * Register an Athene task for an exercise as running
     * @param exerciseId the exerciseId which the Athene task is running for
     */
    public void startTask(Long exerciseId) {
        runningAtheneTasks.add(exerciseId);
    }

    /**
     * Delete an Athene task for an exercise from the running tasks
     * @param exerciseId the exerciseId which the Athene task finished for
     */
    public void finishTask(Long exerciseId) {
        runningAtheneTasks.remove(exerciseId);
    }

    /**
     * Check whether an Athene task is running for the given exerciseId
     * @param exerciseId the exerciseId to check for a running Athene task
     * @return true, if a task for the given exerciseId is running
     */
    public boolean isTaskRunning(Long exerciseId) {
        return runningAtheneTasks.contains(exerciseId);
    }

    /**
     * Calls the remote Athene service to submit a Job for calculating automatic feedback
     * @param exercise the exercise the automatic assessments should be calculated for
     */
    public void submitJob(TextExercise exercise) {
        submitJob(exercise, 1);
    }

    /**
     * Calls the remote Athene service to submit a Job for calculating automatic feedback
     * Falls back to naive splitting for less than 10 submissions
     * Note: See `TextSubmissionService:getTextSubmissionsByExerciseId` for selection of Submissions.
     * @param exercise the exercise the automatic assessments should be calculated for
     * @param maxRetries number of retries before the request will be canceled
     */
    public void submitJob(TextExercise exercise, int maxRetries) {
        log.debug("Start Athene Service for Text Exercise \"" + exercise.getTitle() + "\" (#" + exercise.getId() + ").");

        // Find all submissions for Exercise
        // We only support english languages so far, to prevent corruption of the clustering
        List<TextSubmission> textSubmissions = textSubmissionService.getTextSubmissionsWithTextBlocksByExerciseIdAndLanguage(exercise.getId(), Language.ENGLISH);

        // Athene only works with 10 or more submissions
        if (textSubmissions.size() < 10) {
            return;
        }

        log.info("Calling Remote Service to calculate automatic feedback for " + textSubmissions.size() + " submissions.");

        try {
            final RequestDTO request = new RequestDTO(exercise.getId(), textSubmissions, artemisServerUrl + ATHENE_RESULT_API_PATH + exercise.getId());
            ResponseDTO response = connector.invokeWithRetry(submitApiEndpoint, request, authorizationHeaderForSymmetricSecret(apiSecret), maxRetries);
            log.info("Remote Service to calculate automatic feedback responded: " + response.detail);

            // Register task for exercise as running, AtheneResource calls finishTask on result receive
            startTask(exercise.getId());
        }
        catch (NetworkingError networkingError) {
            log.error("Error while calling Remote Service: {}", networkingError.getMessage());
        }
    }

    /**
     * Processes results coming back from the Athene system via callbackUrl (see AtheneResource)
     * @param clusters the Map of calculated clusters to save to the database
     * @param blocks the list of calculated textBlocks to save to the database
     * @param exerciseId the exercise the automatic feedback suggestions were calculated for
     */
    public void processResult(Map<Integer, TextCluster> clusters, List<AtheneDTO.TextBlockDTO> blocks, Long exerciseId) {
        log.debug("Start processing incoming Athene results for exercise with id {}", exerciseId);

        // Parse textBlocks (blocks will come as AtheneDTO.TextBlock with their submissionId and need to be parsed)
        List<TextBlock> textBlocks = parseTextBlocks(blocks, exerciseId);

        // Save textBlocks in Database
        final Map<String, TextBlock> textBlockMap = textBlockRepository.saveAll(textBlocks).stream().collect(toMap(TextBlock::getId, block -> block));

        // Save clusters in Database
        processClusters(clusters, textBlockMap, exerciseId);

        // Notify atheneService of finished task
        finishTask(exerciseId);

        log.debug("Finished processing incoming Athene results for exercise with id {}", exerciseId);
    }

    /**
     * Parse text blocks of type AtheneDTO.TextBlock to TextBlocks linked to their submission
     *
     * @param blocks The list of AtheneDTO-blocks to parse
     * @param exerciseId The exerciseId of the exercise the blocks belong to
     * @return list of TextBlocks
     */
    public List<TextBlock> parseTextBlocks(List<AtheneDTO.TextBlockDTO> blocks, Long exerciseId) {
        // Create submissionsMap for lookup
        List<TextSubmission> submissions = textSubmissionService.getTextSubmissionsWithTextBlocksByExerciseId(exerciseId);
        Map<Long, TextSubmission> submissionsMap = submissions.stream().collect(toMap(/* Key: */ Submission::getId, /* Value: */ submission -> submission));

        // Map textBlocks to submissions
        List<TextBlock> textBlocks = new LinkedList();
        for (AtheneDTO.TextBlockDTO textBlockDTO : blocks) {
            // Convert DTO-TextBlock (including the submissionId) to TextBlock Entity
            TextBlock newBlock = new TextBlock();
            newBlock.setId(textBlockDTO.getId());
            newBlock.setText(textBlockDTO.getText());
            newBlock.setStartIndex(textBlockDTO.getStartIndex());
            newBlock.setEndIndex(textBlockDTO.getEndIndex());
            newBlock.automatic();

            // take the corresponding TextSubmission and add the text blocks.
            // The addBlocks method also sets the submission in the textBlock
            Hibernate.initialize(submissionsMap.get(textBlockDTO.getSubmissionId()).addBlock(newBlock));
            submissionsMap.get(textBlockDTO.getSubmissionId()).addBlock(newBlock);
            textBlocks.add(newBlock);
        }

        return textBlocks;
    }

    /**
     * Process clusters, link them with text blocks and vice versa, and save all in the database
     *
     * @param clusterMap The map of clusters to process
     * @param textBlockMap The map of textBlocks belonging to the clusters
     * @param exerciseId The exerciseId of the exercise the blocks belong to
     */
    public void processClusters(Map<Integer, TextCluster> clusterMap, Map<String, TextBlock> textBlockMap, Long exerciseId) {
        // Remove Cluster with Key "-1" as it is only contains the blocks belonging to no cluster.
        clusterMap.remove(-1);
        final List<TextCluster> savedClusters = textClusterRepository.saveAll(clusterMap.values());

        // Find exercise, which the clusters belong to
        Optional<TextExercise> optionalTextExercise = textExerciseRepository.findById(exerciseId);
        if (optionalTextExercise.isEmpty()) {
            log.error("Error while processing Athene clusters. Exercise with id " + exerciseId + " not found", new Error());
            return;
        }
        TextExercise textExercise = optionalTextExercise.get();

        // Link clusters with blocks
        for (TextCluster cluster : savedClusters) {
            cluster.setExercise(textExercise);
            List<TextBlock> updatedBlockReferences = cluster.getBlocks().parallelStream().map(block -> textBlockMap.get(block.getId())).peek(block -> block.setCluster(cluster))
                    .collect(toList());
            textAssessmentQueueService.setAddedDistances(updatedBlockReferences, cluster);
            cluster.setBlocks(updatedBlockReferences);
            textBlockRepository.saveAll(updatedBlockReferences);
        }

        // Save clusters in Database
        textClusterRepository.saveAll(savedClusters);
    }

}
