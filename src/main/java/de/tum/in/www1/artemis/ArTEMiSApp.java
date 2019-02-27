package de.tum.in.www1.artemis;

import de.tum.in.www1.artemis.config.ApplicationProperties;
import de.tum.in.www1.artemis.config.DefaultProfileUtil;
import de.tum.in.www1.artemis.service.ProgrammingExerciseService;
import de.tum.in.www1.artemis.service.scheduled.AutomaticBuildPlanCleanupService;
import de.tum.in.www1.artemis.service.scheduled.AutomaticSubmissionService;
import de.tum.in.www1.artemis.service.scheduled.QuizScheduleService;
import io.github.jhipster.config.JHipsterConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;

@SpringBootApplication
@EnableConfigurationProperties({LiquibaseProperties.class, ApplicationProperties.class})
public class ArTEMiSApp {

    private static final Logger log = LoggerFactory.getLogger(ArTEMiSApp.class);

    private final Environment env;
    private final QuizScheduleService quizScheduleService;
    private final AutomaticSubmissionService automaticSubmissionService;
    private final AutomaticBuildPlanCleanupService automaticBuildPlanCleanupService;
    private final ProgrammingExerciseService programmingExerciseService;

    public ArTEMiSApp(Environment env, QuizScheduleService quizScheduleService, AutomaticSubmissionService automaticSubmissionService, AutomaticBuildPlanCleanupService automaticBuildPlanCleanupService, ProgrammingExerciseService programmingExerciseService) {
        this.env = env;
        this.quizScheduleService = quizScheduleService;
        this.automaticSubmissionService = automaticSubmissionService;
        this.automaticBuildPlanCleanupService = automaticBuildPlanCleanupService;
        this.programmingExerciseService = programmingExerciseService;
    }

    /**
     * Initializes ArTEMiS.
     * <p>
     * Spring profiles can be configured with a program arguments --spring.profiles.active=your-active-profile
     * <p>
     * You can find more information on how profiles work with JHipster on <a href="http://www.jhipster.tech/profiles/">http://www.jhipster.tech/profiles/</a>.
     */
    @PostConstruct
    public void initApplication() {
        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        if (activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT) && activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_PRODUCTION)) {
            log.error("You have misconfigured your application! It should not run " +
                "with both the 'dev' and 'prod' profiles at the same time.");
        }
        if (activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT) && activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_CLOUD)) {
            log.error("You have misconfigured your application! It should not " +
                "run with both the 'dev' and 'cloud' profiles at the same time.");
        }

        programmingExerciseService.migrateAllProgrammingExercises();

        // activate Quiz Schedule Service
        quizScheduleService.startSchedule(3 * 1000);                          //every 3 seconds

        // activate Automatic Submission Service
        automaticSubmissionService.startSchedule(10 * 1000);                  //every 10 seconds

        // activate Automatic Submission Service
        automaticBuildPlanCleanupService.startSchedule(12 * 60 * 60 * 1000);       //every 12 hours
    }

    /**
     * Main method, used to run the application.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ArTEMiSApp.class);
        DefaultProfileUtil.addDefaultProfile(app);
        Environment env = app.run(args).getEnvironment();
        logApplicationStartup(env);
    }

    private static void logApplicationStartup(Environment env) {
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        String serverPort = env.getProperty("server.port");
        String contextPath = env.getProperty("server.servlet.context-path");
        if (StringUtils.isBlank(contextPath)) {
            contextPath = "/";
        }
        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using `localhost` as fallback");
        }
        log.info("\n----------------------------------------------------------\n\t" +
                "Application '{}' is running! Access URLs:\n\t" +
                "Local: \t\t{}://localhost:{}{}\n\t" +
                "External: \t{}://{}:{}{}\n\t" +
                "Profile(s): \t{}\n----------------------------------------------------------",
            env.getProperty("spring.application.name"),
            protocol,
            serverPort,
            contextPath,
            protocol,
            hostAddress,
            serverPort,
            contextPath,
            env.getActiveProfiles());
    }
}
