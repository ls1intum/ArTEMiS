package de.tum.in.www1.artemis.service.messaging;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.hazelcast.core.HazelcastInstance;

import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.TextExercise;
import de.tum.in.www1.artemis.service.ProgrammingExerciseService;
import de.tum.in.www1.artemis.service.TextExerciseService;
import de.tum.in.www1.artemis.service.scheduled.ProgrammingExerciseScheduleService;
import de.tum.in.www1.artemis.service.scheduled.TextClusteringScheduleService;

/**
 * This service is only available on a node with the 'scheduling' profile.
 * It receives messages from Hazelcast whenever another node sends a message to a specific topic and processes it on this node.
 */
@Service
@Profile("scheduling")
public class InstanceMessageReceiveService {

    private final Logger log = LoggerFactory.getLogger(InstanceMessageReceiveService.class);

    protected ProgrammingExerciseService programmingExerciseService;

    private ProgrammingExerciseScheduleService programmingExerciseScheduleService;

    protected TextExerciseService textExerciseService;

    private Optional<TextClusteringScheduleService> textClusteringScheduleService;

    public InstanceMessageReceiveService(ProgrammingExerciseService programmingExerciseService, ProgrammingExerciseScheduleService programmingExerciseScheduleService,
            TextExerciseService textExerciseService, Optional<TextClusteringScheduleService> textClusteringScheduleService, HazelcastInstance hazelcastInstance) {
        this.programmingExerciseService = programmingExerciseService;
        this.programmingExerciseScheduleService = programmingExerciseScheduleService;
        this.textExerciseService = textExerciseService;
        this.textClusteringScheduleService = textClusteringScheduleService;

        hazelcastInstance.<Long>getTopic("programming-exercise-schedule").addMessageListener(message -> processScheduleProgrammingExercise((message.getMessageObject())));
        hazelcastInstance.<Long>getTopic("text-exercise-schedule").addMessageListener(message -> processScheduleTextExercise((message.getMessageObject())));
        hazelcastInstance.<Long>getTopic("text-exercise-schedule-cancel").addMessageListener(message -> processTextExerciseScheduleCancel((message.getMessageObject())));
        hazelcastInstance.<Long>getTopic("text-exercise-schedule-instant-clustering")
                .addMessageListener(message -> processTextExerciseInstantClustering((message.getMessageObject())));
    }

    public void processScheduleProgrammingExercise(Long exerciseId) {
        log.info("Received schedule update for programming exercise " + exerciseId);
        ProgrammingExercise programmingExercise = programmingExerciseService.findWithTemplateParticipationAndSolutionParticipationById(exerciseId);
        programmingExerciseScheduleService.scheduleExerciseIfRequired(programmingExercise);
    }

    public void processScheduleTextExercise(Long exerciseId) {
        log.info("Received schedule update for text exercise " + exerciseId);
        TextExercise textExercise = textExerciseService.findOne(exerciseId);
        textClusteringScheduleService.ifPresent(service -> service.scheduleExerciseForClusteringIfRequired(textExercise));
    }

    public void processTextExerciseScheduleCancel(Long exerciseId) {
        log.info("Received schedule cancel for text exercise " + exerciseId);
        textClusteringScheduleService.ifPresent(service -> service.cancelScheduledClustering(exerciseId));
    }

    public void processTextExerciseInstantClustering(Long exerciseId) {
        log.info("Received schedule instant clustering for text exercise " + exerciseId);
        TextExercise textExercise = textExerciseService.findOne(exerciseId);
        textClusteringScheduleService.ifPresent(service -> service.scheduleExerciseForInstantClustering(textExercise));
    }
}
