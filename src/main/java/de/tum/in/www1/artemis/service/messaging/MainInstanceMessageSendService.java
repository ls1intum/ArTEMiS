package de.tum.in.www1.artemis.service.messaging;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * This service is only present on a node that runs the 'scheduling' profile.
 * As this node can handle all the processing without interaction with another node, everything is handled locally (without Hazelcast).
 */
@Service
@Profile("scheduling")
public class MainInstanceMessageSendService implements InstanceMessageSendService {

    public InstanceMessageReceiveService instanceMessageReceiveService;

    public MainInstanceMessageSendService(@Lazy InstanceMessageReceiveService instanceMessageReceiveService) {
        this.instanceMessageReceiveService = instanceMessageReceiveService;
    }

    @Override
    public void sendProgrammingExerciseSchedule(Long exerciseId) {
        // No need to go through the broker, pass it directly
        instanceMessageReceiveService.processScheduleProgrammingExercise(exerciseId);
    }

    @Override
    public void sendTextExerciseSchedule(Long exerciseId) {
        // No need to go through the broker, pass it directly
        instanceMessageReceiveService.processScheduleTextExercise(exerciseId);
    }

    @Override
    public void sendTextExerciseScheduleCancel(Long exerciseId) {
        // No need to go through the broker, pass it directly
        instanceMessageReceiveService.processTextExerciseScheduleCancel(exerciseId);
    }

    @Override
    public void sendTextExerciseInstantClustering(Long exerciseId) {
        // No need to go through the broker, pass it directly
        instanceMessageReceiveService.processTextExerciseInstantClustering(exerciseId);
    }

    @Override
    public void sendUnlockAllRepositories(Long exerciseId) {
        // No need to go through the broker, pass it directly
        instanceMessageReceiveService.processUnlockAllRepositories(exerciseId);
    }

    @Override
    public void sendLockAllRepositories(Long exerciseId) {
        // No need to go through the broker, pass it directly
        instanceMessageReceiveService.processLockAllRepositories(exerciseId);
    }
}
