package de.tum.in.www1.artemis.service.messaging;

/**
 * This interface offers a service that will send messages to the node that runs the scheduling.
 * This can either be the same node or a different node within the Hazelcast cluster.
 */
public interface InstanceMessageSendService {

    /**
     * Send a message to the main server that a programming exercise was created or updated and a (re-)scheduling has to be performed
     * @param exerciseId the id of the exercise that should be scheduled
     */
    void sendProgrammingExerciseSchedule(Long exerciseId);

    /**
     * Send a message to the main server that a text exercise was created or updated and a (re-)scheduling has to be performed
     * @param exerciseId the id of the exercise that should be scheduled
     */
    void sendTextExerciseSchedule(Long exerciseId);

    /**
     * Send a message to the main server that a text exercise was deleted and a scheduling should be stopped
     * @param exerciseId the id of the exercise that should no longer be scheduled
     */
    void sendTextExerciseScheduleCancel(Long exerciseId);

    /**
     * Send a message to the main server that a text exercise should be instantly get clustered
     * @param exerciseId the id of the exercise that should be clustered
     */
    void sendTextExerciseInstantClustering(Long exerciseId);

    /**
     * Send a message to the main server that all repositories of an exercise should be instantly unlocked
     * @param exerciseId the id of the exercise that should be unlocked
     */
    void sendUnlockAllRepositories(Long exerciseId);

    /**
     * Send a message to the main server that all repositories of an exercise should be instantly locked
     * @param exerciseId the id of the exercise that should be locked
     */
    void sendLockAllRepositories(Long exerciseId);
}
