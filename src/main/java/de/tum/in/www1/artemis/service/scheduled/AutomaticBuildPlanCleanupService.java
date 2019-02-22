package de.tum.in.www1.artemis.service.scheduled;

import de.tum.in.www1.artemis.domain.Participation;
import de.tum.in.www1.artemis.domain.Result;
import de.tum.in.www1.artemis.repository.ParticipationRepository;
import de.tum.in.www1.artemis.service.ParticipationService;
import io.github.jhipster.config.JHipsterConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import static java.time.ZonedDateTime.now;

@Service
public class AutomaticBuildPlanCleanupService {

    private static final Logger log = LoggerFactory.getLogger(AutomaticBuildPlanCleanupService.class);

    private final Environment env;
    private final ParticipationRepository participationRepository;
    private final ParticipationService participationService;

    public AutomaticBuildPlanCleanupService(Environment env,
                                            ParticipationRepository participationRepository,
                                            ParticipationService participationService) {
        this.env = env;
        this.participationRepository = participationRepository;
        this.participationService = participationService;
    }

    //TODO: before we deploy this to production, we need to make sure the scheduler is deactivated
    private static ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
    static {
        threadPoolTaskScheduler.setThreadNamePrefix("AutomaticBuildPlanCleanupService");
        threadPoolTaskScheduler.setPoolSize(1);
        threadPoolTaskScheduler.initialize();
    }

    private ScheduledFuture scheduledFuture;

    /**
     * start scheduler
     */
    public void startSchedule(long delayInMillis) {
        log.info("AutomaticBuildPlanCleanupService was started to run repeatedly with {} second gaps.", delayInMillis / 1000.0);
        scheduledFuture = threadPoolTaskScheduler.scheduleWithFixedDelay(this::cleanupBuildPlans, delayInMillis);
    }

    /**
     * stop scheduler (doesn't interrupt if running)
     */
    public void stopSchedule() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }

    @Scheduled(cron="0 0 3 * * *") //execute this every night at 3:00:00 am
    @Transactional
    public void cleanupBuildPlans() {
        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        if (!activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_PRODUCTION)) {
            // only execute this on production server, i.e. when the prod profile is active
            return;
        }

        long start = System.currentTimeMillis();
        log.info("Find build plans for potential cleanup");

        long countNoResultAfter14Days = 0;
        long countSuccessfulLatestResultAfter7Days = 0;
        long countUnsuccessfulLatestResultAfter14Days = 0;

        List<Participation> allParticipationsWithBuildPlanId = participationRepository.findAllWithBuildPlanId();
        Set<Participation> participationsWithBuildPlanToDelete = new HashSet<>();

        for (Participation participation : allParticipationsWithBuildPlanId) {

            if (participation.getBuildPlanId() == null) {
                continue;
            }
            Result result = participation.findLatestResult();
            // 1st case: delete the build plan 14 days after the participation was initialized in case there is no result
            if (result == null) {
                if (participation.getInitializationDate() != null && participation.getInitializationDate().plusDays(14).isBefore(now())) {
                    participationsWithBuildPlanToDelete.add(participation);
                    countNoResultAfter14Days++;
                }
            }
            else {
                // 2nd case: delete the build plan after 7 days in case the latest result is successful
                if (result.isSuccessful()) {
                    if (result.getCompletionDate() != null && result.getCompletionDate().plusDays(7).isBefore(now())) {
                        participationsWithBuildPlanToDelete.add(participation);
                        countSuccessfulLatestResultAfter7Days++;
                    }
                }
                // 3rd case: delete the build plan after 14 days in case the latest result is NOT successful
                else {
                    if (result.getCompletionDate() != null && result.getCompletionDate().plusDays(14).isBefore(now())) {
                        participationsWithBuildPlanToDelete.add(participation);
                        countUnsuccessfulLatestResultAfter14Days++;
                    }
                }
            }
        }
        log.info("Found " + allParticipationsWithBuildPlanId.size() + " participations with build plans in " + (System.currentTimeMillis() - start) + " ms execution time");
        log.info("Found " + participationsWithBuildPlanToDelete.size() + " old build plans to delete");
        log.info("Found " + countNoResultAfter14Days + " build plans without results 14 days after initialization");
        log.info("Found " + countSuccessfulLatestResultAfter7Days + " build plans with successful latest result is older than 7 days");
        log.info("Found " + countUnsuccessfulLatestResultAfter14Days + " build plans with unsuccessful latest result is older than 14 days");
        List<String> buildPlanIds = participationsWithBuildPlanToDelete.stream().map(Participation::getBuildPlanId).collect(Collectors.toList());
        log.info("Build plans: " + buildPlanIds);

        for (Participation participation : participationsWithBuildPlanToDelete) {
            //TODO: the actual cleanup is deactivated for now, we will activate it soon after some initial tests.
//            participationService.cleanupBuildPlan(participation);
        }
    }
}
