package de.tum.in.www1.artemis.service;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.FeedbackType;

@Service
@Profile("automaticText")
public class AutomaticTextFeedbackService {

    private final FeedbackService feedbackService;

    public AutomaticTextFeedbackService(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * @param result
     */
    @Transactional(readOnly = true)
    public void suggestFeedback(@NotNull Result result) {
        final TextSubmission textSubmission = (TextSubmission) result.getSubmission();
        final TextExercise exercise = (TextExercise) textSubmission.getParticipation().getExercise();
        final List<TextBlock> blocks = textSubmission.getBlocks();

        final List<Feedback> suggestedFeedback = blocks.parallelStream().map(block -> {
            final TextCluster cluster = block.getCluster();
            Feedback newFeedback = new Feedback().reference(block.getId());

            if (cluster != null) {
                final List<TextBlock> allBlocksInCluster = cluster.getBlocks();
                final Map<String, Feedback> feedbackForTextExerciseInCluster = feedbackService.getFeedbackForTextExerciseInCluster(exercise, cluster);

                final Optional<TextBlock> mostSimilarBlockInClusterWithFeedback = allBlocksInCluster.parallelStream()
                        .filter(element -> feedbackForTextExerciseInCluster.keySet().contains(element.getId()))
                        .min(comparing(element -> cluster.distanceBetweenBlocks(block, element)));

                if (mostSimilarBlockInClusterWithFeedback.isPresent()) {
                    final Feedback similarFeedback = feedbackForTextExerciseInCluster.get(mostSimilarBlockInClusterWithFeedback.get().getId());
                    return newFeedback.reference(block.getId()).credits(similarFeedback.getCredits()).detailText(similarFeedback.getDetailText()).type(FeedbackType.AUTOMATIC);

                }
            }

            return newFeedback.credits(0d).type(FeedbackType.MANUAL);
        }).collect(toList());

        result.getFeedbacks().addAll(suggestedFeedback);
    }

}
