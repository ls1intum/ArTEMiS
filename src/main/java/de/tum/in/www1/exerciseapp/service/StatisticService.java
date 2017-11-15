package de.tum.in.www1.exerciseapp.service;

import de.tum.in.www1.exerciseapp.domain.QuizExercise;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Created by Josias Montag on 06.10.16.
 */
@Service
public class StatisticService {

    private final Optional<ContinuousIntegrationService> continuousIntegrationService;
    private final LtiService ltiService;
    private final SimpMessageSendingOperations messagingTemplate;

    public StatisticService(Optional<ContinuousIntegrationService> continuousIntegrationService, LtiService ltiService, SimpMessageSendingOperations messagingTemplate) {
        this.continuousIntegrationService = continuousIntegrationService;
        this.ltiService = ltiService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Perform async operations after we were notified about new results.
     *
     */
    @Async

    public void updateStatistic(QuizExercise quizExercise) {
        // fetches the new build result
        // notify user via websocket
        messagingTemplate.convertAndSend("/topic/statistic/"+quizExercise.getId(), true);
        // handles new results and sends them to LTI consumers
        //ltiService.onNewBuildResult(participation);
    }

}
