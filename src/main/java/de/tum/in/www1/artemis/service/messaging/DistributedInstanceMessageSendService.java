package de.tum.in.www1.artemis.service.messaging;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.hazelcast.core.HazelcastInstance;

/**
 * This service is only active on a node that does not run with the 'scheduling' profile.
 * All requests are forwarded to a Hazelcast topic and a node with the 'scheduling' profile will then process it.
 */
@Service
@Profile("!scheduling")
public class DistributedInstanceMessageSendService implements InstanceMessageSendService {

    private final Logger log = LoggerFactory.getLogger(DistributedInstanceMessageSendService.class);

    private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);

    private HazelcastInstance hazelcastInstance;

    public DistributedInstanceMessageSendService(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public void sendProgrammingExerciseSchedule(Long exerciseId) {
        log.info("Sending schedule for programming exercise " + exerciseId + " to broker.");
        sendMessageDelayed("programming-exercise-schedule", exerciseId);
    }

    @Override
    public void sendTextExerciseSchedule(Long exerciseId) {
        log.info("Sending schedule for text exercise " + exerciseId + " to broker.");
        sendMessageDelayed("text-exercise-schedule", exerciseId);
    }

    @Override
    public void sendTextExerciseScheduleCancel(Long exerciseId) {
        log.info("Sending schedule cancel for text exercise " + exerciseId + " to broker.");
        sendMessageDelayed("text-exercise-schedule-cancel", exerciseId);
    }

    @Override
    public void sendTextExerciseInstantClustering(Long exerciseId) {
        log.info("Sending schedule instant clustering for text exercise " + exerciseId + " to broker.");
        sendMessageDelayed("text-exercise-schedule-instant-clustering", exerciseId);
    }

    private void sendMessageDelayed(String destination, Long exerciseId) {
        exec.schedule(() -> hazelcastInstance.getTopic(destination).publish(exerciseId), 1, TimeUnit.SECONDS);
    }
}
