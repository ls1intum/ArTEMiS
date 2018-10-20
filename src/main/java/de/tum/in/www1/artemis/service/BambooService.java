package de.tum.in.www1.artemis.service;

import com.atlassian.bamboo.specs.api.builders.BambooKey;
import com.atlassian.bamboo.specs.api.builders.applink.ApplicationLink;
import com.atlassian.bamboo.specs.api.builders.permission.PermissionType;
import com.atlassian.bamboo.specs.api.builders.permission.Permissions;
import com.atlassian.bamboo.specs.api.builders.permission.PlanPermissions;
import com.atlassian.bamboo.specs.api.builders.plan.Job;
import com.atlassian.bamboo.specs.api.builders.plan.Plan;
import com.atlassian.bamboo.specs.api.builders.plan.PlanIdentifier;
import com.atlassian.bamboo.specs.api.builders.plan.Stage;
import com.atlassian.bamboo.specs.api.builders.plan.configuration.ConcurrentBuilds;
import com.atlassian.bamboo.specs.api.builders.project.Project;
import com.atlassian.bamboo.specs.api.builders.repository.VcsChangeDetection;
import com.atlassian.bamboo.specs.api.builders.repository.VcsRepositoryIdentifier;
import com.atlassian.bamboo.specs.builders.repository.bitbucket.server.BitbucketServerRepository;
import com.atlassian.bamboo.specs.builders.repository.viewer.BitbucketServerRepositoryViewer;
import com.atlassian.bamboo.specs.builders.task.CheckoutItem;
import com.atlassian.bamboo.specs.builders.task.MavenTask;
import com.atlassian.bamboo.specs.builders.task.ScriptTask;
import com.atlassian.bamboo.specs.builders.task.VcsCheckoutTask;
import com.atlassian.bamboo.specs.builders.trigger.BitbucketServerTrigger;
import com.atlassian.bamboo.specs.model.task.ScriptTaskProperties;
import com.atlassian.bamboo.specs.util.BambooServer;
import com.atlassian.bamboo.specs.util.SimpleUserPasswordCredentials;
import com.atlassian.bamboo.specs.util.UserPasswordCredentials;
import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.FeedbackType;
import de.tum.in.www1.artemis.exception.BambooException;
import de.tum.in.www1.artemis.exception.GitException;
import de.tum.in.www1.artemis.repository.FeedbackRepository;
import de.tum.in.www1.artemis.repository.ParticipationRepository;
import de.tum.in.www1.artemis.repository.ProgrammingSubmissionRepository;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.swift.bamboo.cli.BambooClient;
import org.swift.bitbucket.cli.BitbucketClient;
import org.swift.bitbucket.cli.objects.RemoteRepository;
import org.swift.common.cli.CliClient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Profile("bamboo")
public class BambooService implements ContinuousIntegrationService {

    private final Logger log = LoggerFactory.getLogger(BambooService.class);

    @Value("${artemis.bamboo.url}")
    private URL BAMBOO_SERVER_URL;

    @Value("${artemis.bamboo.empty-commit-necessary}")
    private Boolean BAMBOO_EMPTY_COMMIT_WORKAROUND_NECESSARY;

    @Value("${artemis.bamboo.user}")
    private String BAMBOO_USER;

    @Value("${artemis.bamboo.password}")
    private String BAMBOO_PASSWORD;

    @Value("${artemis.bamboo.bitbucket-application-link-id}")
    private String BITBUCKET_APPLICATION_LINK_ID;

    @Value("${artemis.result-retrieval-delay}")
    private int RESULT_RETRIEVAL_DELAY = 10000;

    @Value("${server.url}")
    private URL SERVER_URL;

    //TODO: get these values from somewhere?!?
    private final String TEST_REPO_NAME = "Tests";

    private final String ASSIGNMENT_REPO_NAME = "Assignment";

    private final String ASSIGNMENT_REPO_PATH = "assignment";

    private final GitService gitService;
    private final ResultRepository resultRepository;
    private final FeedbackRepository feedbackRepository;
    private final ParticipationRepository participationRepository;
    private final ProgrammingSubmissionRepository programmingSubmissionRepository;
    private final VersionControlService versionControlService;
    private final ContinuousIntegrationUpdateService continuousIntegrationUpdateService;

    public BambooService(GitService gitService, ResultRepository resultRepository, FeedbackRepository feedbackRepository, ParticipationRepository participationRepository,
                         ProgrammingSubmissionRepository programmingSubmissionRepository, VersionControlService versionControlService,
                         ContinuousIntegrationUpdateService continuousIntegrationUpdateService) {
        this.gitService = gitService;
        this.resultRepository = resultRepository;
        this.feedbackRepository = feedbackRepository;
        this.participationRepository = participationRepository;
        this.programmingSubmissionRepository = programmingSubmissionRepository;
        this.versionControlService = versionControlService;
        this.continuousIntegrationUpdateService = continuousIntegrationUpdateService;
    }

    public void createBaseBuildPlanForExercise(ProgrammingExercise exercise, String planKey, String vcsRepositorySlug) {
        UserPasswordCredentials userPasswordCredentials = new SimpleUserPasswordCredentials(BAMBOO_USER, BAMBOO_PASSWORD);
        BambooServer bambooServer = new BambooServer(BAMBOO_SERVER_URL.toString(), userPasswordCredentials);

        //TODO: Get some of these values from the exercise object or a config object

        //Bamboo build plan
        final String planName = "Artemis Build Plan for Exercise " + exercise.getTitle() + " (" + planKey + ")"; // Must be unique
        final String planDescription = "Artemis BASE Build Plan for Exercise " + exercise.getTitle();

        //Bamboo build project
        final String projectKey = exercise.getCIProjectKey();
        final String projectName = "Artemis Project for Exercise " + exercise.getTitle();

        //Bitbucket project and repos
        final String vcsProjectKey = exercise.getVCSProjectKey();
        final String vcsAssignmentRepositorySlug = vcsRepositorySlug; // exercise.getShortName() + "-assignment"
        final String vcsTestRepositorySlug = "tests";

        //Permissions
        Course course = exercise.getCourse();
        final String adminGroupName = "ls1instructor";  //see admin-group-name // TODO: maybe get this from the JiraAuthenticationProvider
        final String teachingAssistantGroupName = course.getTeachingAssistantGroupName();
        final String instructorGroupName = course.getInstructorGroupName();

        final Plan plan = createPlan(planKey, planName, planDescription, projectKey, projectName, vcsProjectKey, vcsAssignmentRepositorySlug, vcsTestRepositorySlug);
        bambooServer.publish(plan);

        final PlanPermissions planPermission = setPlanPermission(projectKey, planKey, adminGroupName, instructorGroupName, teachingAssistantGroupName);
        bambooServer.publish(planPermission);
    }


    private Project createProject(String name, String key) {
        return new Project()
            .key(key)
            .name(name);
    }

    private Plan createPlan(String planKey, String planName, String planDescription, String projectKey, String projectName,
                     String vcsProjectKey, String vcsAssignmentRepositorySlug, String vcsTestRepositorySlug) {
        @SuppressWarnings("unchecked")
        final Plan plan = new Plan(createProject(projectName, projectKey), planName, planKey)
            .description(planDescription)
            .pluginConfigurations(new ConcurrentBuilds().useSystemWideDefault(true))
            .planRepositories(
                createBuildPlanRepository(ASSIGNMENT_REPO_NAME, vcsProjectKey, vcsAssignmentRepositorySlug),
                createBuildPlanRepository(TEST_REPO_NAME, vcsProjectKey, vcsTestRepositorySlug))
            .stages(new Stage("Default Stage")
                .jobs(new Job("Default Job",
                    new BambooKey("JOB1"))
                    .tasks(new VcsCheckoutTask()
                            .description("Checkout Default Repository")
                            .checkoutItems(new CheckoutItem()
                                    .repository(new VcsRepositoryIdentifier()
                                        .name(ASSIGNMENT_REPO_NAME))
                                    .path(ASSIGNMENT_REPO_PATH),	//TODO: this path needs to be specified in the Maven pom.xml in the Tests Repo
                                new CheckoutItem()
                                    .repository(new VcsRepositoryIdentifier()
                                        .name(TEST_REPO_NAME))),
                        new MavenTask()
                            .goal("clean test")
                            .jdk("JDK 1.8")
                            .executableLabel("Maven 3")
                            .hasTests(true))
                    .finalTasks(new ScriptTask()
                        .description("Notify ArTEMiS")
                        .interpreter(ScriptTaskProperties.Interpreter.BINSH_OR_CMDEXE)
                        .inlineBody("curl -k -X POST " + BAMBOO_SERVER_URL + "/api/results/${bamboo.planKey}"))))
            .triggers(new BitbucketServerTrigger());
        return plan;
    }

    private BitbucketServerRepository createBuildPlanRepository(String name, String vcsProjectKey, String repositorySlug) {
        return new BitbucketServerRepository()
            .name(name)
            .server(new ApplicationLink()
                .id(BITBUCKET_APPLICATION_LINK_ID))
            .projectKey(vcsProjectKey)
            .repositorySlug(repositorySlug)
            .shallowClonesEnabled(true)
            .remoteAgentCacheEnabled(false)
            .changeDetection(new VcsChangeDetection());
    }

    private PlanPermissions setPlanPermission(String bambooProjectKey, String bambooPlanKey, String teachingAssistantGroupName, String instructorGroupName, String adminGroupName) {
        final PlanPermissions planPermission = new PlanPermissions(new PlanIdentifier(bambooProjectKey, bambooPlanKey))
            .permissions(new Permissions()
                .userPermissions(BAMBOO_USER, PermissionType.EDIT, PermissionType.BUILD, PermissionType.CLONE, PermissionType.VIEW, PermissionType.ADMIN)
                .groupPermissions(adminGroupName, PermissionType.CLONE, PermissionType.BUILD, PermissionType.EDIT, PermissionType.VIEW, PermissionType.ADMIN)
                .groupPermissions(instructorGroupName, PermissionType.CLONE, PermissionType.BUILD, PermissionType.EDIT, PermissionType.VIEW, PermissionType.ADMIN)
                .groupPermissions(teachingAssistantGroupName, PermissionType.BUILD, PermissionType.VIEW));
        return planPermission;
    }

    private BambooClient getBambooClient() {
        final BambooClient bambooClient = new BambooClient();
        //setup the Bamboo Client to use the correct username and password

        String[] args = new String[]{
            "-s", BAMBOO_SERVER_URL.toString(),
            "--user", BAMBOO_USER,
            "--password", BAMBOO_PASSWORD,
        };

        bambooClient.doWork(args); //only invoke this to set server address, username and password so that the following action will work
        return bambooClient;
    }


    @Override
    public void createProject(String projectName) {
        try {
            createProjectImpl(projectName);
        }
        catch(BambooException bambooException) {
            if (bambooException.getMessage().contains("already exists")) {
                log.info("Project already exists. Reusing it...");
            }
            else throw bambooException;
        }

    }

    private void createProjectImpl(String projectName) throws BambooException {
        try {
            log.info("Creating project " + projectName);
            String message = getBambooClient().getProjectHelper().createProject(projectName, projectName, "Project created by ArTEMiS");
            log.info("Project was successfully created. " + message);

        } catch (CliClient.ClientException clientException) {
            log.error(clientException.getMessage(), clientException);
            if (clientException.getMessage().contains("already exists")) {
                throw new BambooException(clientException.getMessage());
            }
        } catch (CliClient.RemoteRestException e) {
            log.error(e.getMessage(), e);
            throw new BambooException("Something went wrong while creating the project", e);
        }
    }

    @Override
    public void copyBuildPlanFromTemplate(String buildplanName, String projectName, String templateBuildPlanName, String templateProjectName) {
        try {
            clonePlan(templateProjectName, templateBuildPlanName, projectName, buildplanName);
        }
        catch(BambooException bambooException) {
            if (bambooException.getMessage().contains("already exists")) {
                log.info("Build Plan already exists. Going to reuse it...");
                return;
            }
            else throw bambooException;
        }
    }

    @Override
    public void grantProjectPermissions(String projectKey, String instructorGroupName, String teachingAssistantGroupName) {
        grantGroupPermissionToProject(projectKey, instructorGroupName, new String[]{"READ", "WRITE", "BUILD", "CLONE", "ADMINISTRATION"});
        grantGroupPermissionToProject(projectKey, teachingAssistantGroupName, new String[]{"READ", "BUILD"});

    }

    private void grantGroupPermissionToProject(String projectKey, String groupName, String[] permissions) {
        HttpHeaders headers = HeaderUtil.createAuthorization(BAMBOO_USER, BAMBOO_PASSWORD);
        HttpEntity<?> entity = new HttpEntity<>(permissions, headers);
        RestTemplate restTemplate = new RestTemplate();
        try {
            restTemplate.exchange(
                BAMBOO_SERVER_URL + "/rest/api/latest/permissions/projectplan/" + projectKey.toUpperCase() + "/groups/" + groupName,
                HttpMethod.PUT,
                entity,
                Object.class);
        } catch (Exception e) {
            log.error("HttpError while setting permissions for build plan", e);
        }
    }

    @Override
    public String copyBuildPlan(String baseBuildPlanId, String wantedPlanKey) {
        wantedPlanKey = cleanPlanKey(wantedPlanKey);
        String projectKey = getProjectKeyFromBuildPlanId(baseBuildPlanId);
        try {
            return clonePlan(projectKey, getPlanKeyFromBuildPlanId(baseBuildPlanId), projectKey, wantedPlanKey); // Save the new plan in the same project
        }
        catch(BambooException bambooException) {
            if (bambooException.getMessage().contains("already exists")) {
                log.info("Build Plan already exists. Going to recover build plan information...");
                return getProjectKeyFromBuildPlanId(baseBuildPlanId) + "-" + wantedPlanKey;
            }
            else throw bambooException;
        }
    }

    @Override
    public void configureBuildPlan(Participation participation) {
        String buildPlanId = participation.getBuildPlanId();
        URL repositoryUrl = participation.getRepositoryUrlAsUrl();
        updatePlanRepository(
            getProjectKeyFromBuildPlanId(buildPlanId),
            getPlanKeyFromBuildPlanId(buildPlanId),
            ASSIGNMENT_REPO_NAME,
            versionControlService.getProjectName(repositoryUrl),
            versionControlService.getRepositoryName(repositoryUrl)
        );
        enablePlan(getProjectKeyFromBuildPlanId(buildPlanId), getPlanKeyFromBuildPlanId(buildPlanId));
        // We need to trigger an initial update in order for Gitlab to work correctly
        continuousIntegrationUpdateService.triggerUpdate(buildPlanId, true);

        // Empty commit - Bamboo bug workaround

        if(BAMBOO_EMPTY_COMMIT_WORKAROUND_NECESSARY) {
            try {
                Repository repo = gitService.getOrCheckoutRepository(repositoryUrl);
                gitService.commitAndPush(repo, "Setup");
                ProgrammingExercise exercise = (ProgrammingExercise) participation.getExercise();
                //only delete the git repository, if the online editor is NOT allowed
                //this saves some performance, when the student opens the online editor
                if (!exercise.isAllowOnlineEditor()) {
                    gitService.deleteLocalRepository(repo);
                }
            } catch (GitAPIException ex) {
                log.error("Git error while doing empty commit", ex);
                throw new GitException("Git error while doing empty commit");
            } catch (IOException ex) {
                log.error("IOError while doing empty commit", ex);
                throw new GitException("IOError while doing empty commit");
            } catch (InterruptedException ex) {
                log.error("InterruptedException while doing empty commit", ex);
                throw new GitException("IOError while doing empty commit");
            }
        }
    }

    @Override
    public void deleteBuildPlan(String buildPlanId) {
        deletePlan(getProjectKeyFromBuildPlanId(buildPlanId), getPlanKeyFromBuildPlanId(buildPlanId));
    }

    @Override
    public BuildStatus getBuildStatus(Participation participation) {
        Map<String, Boolean> status = retrieveBuildStatus(participation.getBuildPlanId());
        if (status == null) {
            return BuildStatus.INACTIVE;
        }
        if (status.get("isActive") && !status.get("isBuilding")) {
            return BuildStatus.QUEUED;
        }
        else if (status.get("isActive") && status.get("isBuilding")) {
            return BuildStatus.BUILDING;
        }
        else {
            return BuildStatus.INACTIVE;
        }
    }

    @Override
    public List<Feedback> getLatestBuildResultDetails(Result result) {
        Map<String, Object> buildResultDetails = retrieveLatestBuildResultDetails(result.getParticipation().getBuildPlanId());
        List<Feedback> feedbackItems = addFeedbackToResult(result, buildResultDetails);
        return feedbackItems;
    }

    @Override
    public List<BuildLogEntry> getLatestBuildLogs(Participation participation) {
        return retrieveLatestBuildLogs(participation.getBuildPlanId());
    }


    @Override
    public URL getBuildPlanWebUrl(Participation participation) {
        try {
            return new URL(BAMBOO_SERVER_URL + "/browse/" + participation.getBuildPlanId().toUpperCase());
        } catch (MalformedURLException e) {
            log.error("Couldn't construct build plan web URL");
        }
        return BAMBOO_SERVER_URL;
    }

    /**
     * Clones an existing Bamboo plan.
     *
     * @param baseProject The Bamboo project in which the plan is contained.
     * @param basePlan    The plan's name.
     * @param toProject   The Bamboo project in which the new plan should be contained.
     * @param name        The name to give the cloned plan.
     * @return            The name of the new build plan
     */
    public String clonePlan(String baseProject, String basePlan, String toProject, String name) throws BambooException {

        String toPlan = toProject + "-" + name;
        try {
            log.info("Clone build plan " + baseProject + "-" + basePlan + " to " + toPlan);
            String message = getBambooClient().getPlanHelper().clonePlan(baseProject + "-" + basePlan, toPlan, toPlan, "", "", true);
            log.info("Clone build plan " + toPlan + " was successful." + message);
        } catch (CliClient.ClientException clientException) {
            log.error(clientException.getMessage(), clientException);
            if (clientException.getMessage().contains("already exists")) {
                throw new BambooException(clientException.getMessage());
            }
        } catch (CliClient.RemoteRestException e) {
            log.error(e.getMessage(), e);
            throw new BambooException("Something went wrong while cloning build plan", e);
        }
        return toPlan;
    }

    /**
     * Enables the given build plan.
     *
     * @param projectKey
     * @param planKey
     * @return
     */
    public String enablePlan(String projectKey, String planKey) throws BambooException {

        try {
            log.info("Enable build plan " + projectKey + "-" + planKey);
            String message = getBambooClient().getPlanHelper().enablePlan(projectKey + "-" + planKey, true);
            log.info("Enable build plan " + projectKey + "-" + planKey + " was successful. " + message);
            return message;
        } catch (CliClient.ClientException | CliClient.RemoteRestException e) {
            log.error(e.getMessage(), e);
            throw new BambooException("Something went wrong while enabling the build plan", e);
        }
    }

    public String updatePlanRepository(String bambooProject, String bambooPlan, String bambooRepositoryName, String repoProjectName, String repoName) throws BambooException {
        return continuousIntegrationUpdateService.updatePlanRepository(bambooProject, bambooPlan, bambooRepositoryName, repoProjectName, repoName);
    }

    /**
     * Deletes the given plan.
     *
     * @param projectKey
     * @param planKey
     * @return
     */
    public String deletePlan(String projectKey, String planKey) {
        try {
            log.info("Delete build plan " + projectKey + "-" + planKey);
            String message = getBambooClient().getPlanHelper().deletePlan(projectKey + "-" + planKey);
            log.info("Delete build plan was successful. " + message);
            return message;
        } catch (CliClient.ClientException | CliClient.RemoteRestException e) {
            log.error(e.getMessage());
            throw new BambooException("Something went wrong while deleting the build plan", e);
        }
    }

    /**
     * Retrieves the latest build result for the given plan key and saves it as result.
     * It checks if the build result is the current one. If not, it waits for a configurable delay and then tries again.
     *
     * @param participation
     */
    @Override
    @Transactional
    public Result onBuildCompleted(Participation participation) {
        log.debug("Retrieving build result...");
        Boolean isOldBuildResult = true;
        Map buildResults = new HashMap<>();
        try {
            buildResults = retrieveLatestBuildResult(participation.getBuildPlanId());
            isOldBuildResult = TimeUnit.SECONDS.toMillis(ZonedDateTime.now().toEpochSecond() - ((ZonedDateTime) buildResults.get("buildCompletedDate")).toEpochSecond()) > (20 * 1000);     // older than 20s
        } catch (Exception ex) {
            log.warn("Exception when retrieving a Bamboo build result for build plan " + participation.getBuildPlanId() + ": " + ex.getMessage());
        }

        //TODO: put this request into a timer / queue instead of blocking the request! Because blocking the request actually means that other requests cannot be exectuted
        if (isOldBuildResult) {
            log.debug("It seems we got an old build result from Bamboo for build plan " + participation.getBuildPlanId() + ". Waiting 1s to retrieve build result...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("Sleep error", e);
            }
            log.debug("Retrieving build result (second try)...");
            buildResults = retrieveLatestBuildResult(participation.getBuildPlanId());
        }

        if (buildResults.containsKey("buildReason")) {
            String buildReason = (String)buildResults.get("buildReason");
            if (buildReason.contains("First build for this plan")) {
                return null;
            }
        }

        //TODO: only save this result if it is newer (e.g. + 5s) than the last saved result for this participation --> this avoids saving exact same results multiple times

        Result result = new Result();
        result.setSuccessful((boolean) buildResults.get("successful"));
        result.setResultString((String) buildResults.get("buildTestSummary"));
        result.setCompletionDate((ZonedDateTime) buildResults.get("buildCompletedDate"));
        result.setScore(calculateScoreForResult(result));
        result.setBuildArtifact(buildResults.containsKey("artifact"));
        result.setParticipation(participation);

        Map buildResultDetails = retrieveLatestBuildResultDetails(participation.getBuildPlanId());
        if (result.getFeedbacks() != null && result.getFeedbacks().size() > 0) {
            //cleanup
            for(Feedback feedback : new ArrayList<Feedback>(result.getFeedbacks())) {
                result.removeFeedback(feedback);
                feedbackRepository.delete(feedback);
            }
        }
        addFeedbackToResult(result, buildResultDetails);

        if (buildResults.containsKey("vcsRevisionKey")) {
            ProgrammingSubmission programmingSubmission = programmingSubmissionRepository.findByCommitHash((String) buildResults.get("vcsRevisionKey"));
            if (programmingSubmission == null) { // no matching programmingsubmission
                log.warn("Could not find ProgrammingSubmission for Commit-Hash {} (Participation {}, Build-Plan {})", buildResults.get("vcsRevisionKey"), participation.getId(), participation.getBuildPlanId());
                // TODO Think about this case and handle it properly (e.g. by creating this submission now)
                // this might be a wrong build (what could be the reason), or this might be due to test changes
                // what happens if only the test has changes? should we then create a new submission?
            } else {
                log.info("Found corresponding submission to build result with Commit-Hash {}", buildResults.get("vcsRevisionKey"));
                result.setSubmission(programmingSubmission);
                programmingSubmission.setResult(result);
                programmingSubmissionRepository.save(programmingSubmission); // result gets saved later, no need to save it now
            }

        } else { // No commit hash in build result
            log.warn("Could not find Commit-Hash (Participation {}, Build-Plan {})", participation.getId(), participation.getBuildPlanId());
        }

        resultRepository.save(result);
        //The following was intended to prevent caching problems, but does not work properly due to lazy instantiation exceptions
//        Hibernate.initialize(participation.getResults());
//        participation.addResult(result);
//        participationRepository.save(participation);
        return result;
    }

    /**
     * Converts build result details into feedback and stores it in the result object
     * @param
     * @param buildResultDetails returned build result details from the rest API of bamboo
     *
     * @return a list of feedbacks itemsstored in a result
     */
    public List<Feedback> addFeedbackToResult(Result result, Map<String, Object> buildResultDetails) {
        if(buildResultDetails == null) {
            return null;
        }

        try {
            List<Map<String, Object>> details = (List<Map<String, Object>>)buildResultDetails.get("details");
            if(!details.isEmpty()) {
                result.setHasFeedback(true);
            }
            //breaking down the Bamboo API answer to get all the relevant details
            for(Map<String, Object> detail : details) {
                String className = (String)detail.get("className");
                String methodName = (String)detail.get("methodName");

                HashMap<String, Object> errorsMap = (HashMap<String, Object>) detail.get("errors");
                List<HashMap<String, Object>> errors = (List<HashMap<String, Object>>)errorsMap.get("error");

                String errorMessageString = "";
                for(HashMap<String, Object> error : errors) {
                    //Splitting string at the first linebreak to only get the first line of the Exception
                    errorMessageString += ((String)error.get("message")).split("\\n", 2)[0] + "\n";
                }

                Feedback feedback = new Feedback();
                feedback.setText(methodName);
                feedback.setDetailText(errorMessageString);
                feedback.setType(FeedbackType.AUTOMATIC);
                feedback.setPositive(false);
                feedback = feedbackRepository.save(feedback);
                result.addFeedback(feedback);
            }
        } catch(Exception failedToParse) {
            log.error("Parsing from bamboo to feedback failed" + failedToParse);
        }

        return result.getFeedbacks();
    }

    /**
     * Calculates the score for a result. Therefore is uses the number of successful tests in the latest build.
     *
     * @param result
     * @return
     */
    private Long calculateScoreForResult(Result result) {

        if (result.isSuccessful()) {
            return (long) 100;
        }

        if (result.getResultString() != null && !result.getResultString().isEmpty()) {

            Pattern p = Pattern.compile("^([0-9]+) of ([0-9]+) failed");
            Matcher m = p.matcher(result.getResultString());

            if (m.find()) {
                float failedTests = Float.parseFloat(m.group(1));
                float totalTests = Float.parseFloat(m.group(2));
                float score = (totalTests - failedTests) / totalTests;
                return (long) (score * 100);
            }
        }
        return (long) 0;
    }

    /**
     * Performs a request to the Bamboo REST API to retrive the latest result for the given plan.
     *
     * @param planKey the key of the plan for which to retrieve the latest result
     * @return a map containing the following data:
     * - successful:       if the build was successful
     * - buildTestSummary:      a string generated by Bamboo summarizing the build result
     * - buildCompletedDate:    the completion date of the build
     */
    private Map<String, Object> retrieveLatestBuildResult(String planKey) {
        HttpHeaders headers = HeaderUtil.createAuthorization(BAMBOO_USER, BAMBOO_PASSWORD);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = null;
        try {
            response = restTemplate.exchange(
                BAMBOO_SERVER_URL + "/rest/api/latest/result/" + planKey.toUpperCase() + "/latest.json?expand=testResults,artifacts",
                HttpMethod.GET,
                entity,
                Map.class);
        } catch (Exception e) {
            log.error("HttpError while retrieving results", e);
        }
        if (response != null) {
            Map<String, Object> result = new HashMap<>();
            boolean successful = (boolean) response.getBody().get("successful");
            result.put("successful", successful);
            String buildTestSummary = (String) response.getBody().get("buildTestSummary");
            result.put("buildTestSummary", buildTestSummary);
            if (response.getBody().containsKey("buildReason")) {
                result.put("buildReason", response.getBody().get("buildReason"));
            }
            String dateString = (String) response.getBody().get("buildCompletedDate");
            ZonedDateTime buildCompletedDate = ZonedDateTime.parse(dateString);
            result.put("buildCompletedDate", buildCompletedDate);
            if (response.getBody().containsKey("vcsRevisionKey")) {
                result.put("vcsRevisionKey", response.getBody().get("vcsRevisionKey"));
            }

            if(response.getBody().containsKey("artifacts")) {
                Map<String, Object> artifacts = (Map<String, Object>)response.getBody().get("artifacts");
                if((int)artifacts.get("size") > 0 && artifacts.containsKey("artifact")) {
                    Map<String, Object> firstArtifact = (Map<String, Object>) ((ArrayList<Map>) artifacts.get("artifact")).get(0);
                    String artifact = (String) ((Map<String, Object>) firstArtifact.get("link")).get("href");
                    result.put("artifact", artifact);
                }
            }

            return result;
        }
        return null;
    }

    /**
     * Performs a request to the Bamboo REST API to retrieve details on the failed tests of the latest build.
     *
     * @param planKey the key of the plan for which to retrieve the details
     * @return
     */
    private Map<String, Object> retrieveLatestBuildResultDetails(String planKey) {
        HttpHeaders headers = HeaderUtil.createAuthorization(BAMBOO_USER, BAMBOO_PASSWORD);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = null;
        try {
            // e.g. https://bamboobruegge.in.tum.de/rest/api/latest/result/EIST16W1-TESTEXERCISEAPP-JOB1/latest.json?expand=testResults.failedTests.testResult.errors
            response = restTemplate.exchange(
                BAMBOO_SERVER_URL + "/rest/api/latest/result/" + planKey.toUpperCase() + "-JOB1/latest.json?expand=testResults.failedTests.testResult.errors",
                HttpMethod.GET,
                entity,
                Map.class);
        } catch (Exception e) {
            log.error("HttpError while retrieving build result details", e);
        }
        if (response != null) {
            Map<String, Object> result = new HashMap<>();
            List resultDetails = (List) ((Map) ((Map) response.getBody().get("testResults")).get("failedTests")).get("testResult");
            result.put("details", resultDetails);
            return result;
        }
        return null;
    }


    /**
     * Performs a request to the Bamboo REST API to retrieve the build log of the latest build.
     *
     * @param planKey
     * @return
     */
    //TODO: save this in the result class so that ArTEMiS does not need to retrieve it everytime
    public List<BuildLogEntry> retrieveLatestBuildLogs(String planKey) {
        HttpHeaders headers = HeaderUtil.createAuthorization(BAMBOO_USER, BAMBOO_PASSWORD);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = null;
        try {
            response = restTemplate.exchange(
                BAMBOO_SERVER_URL + "/rest/api/latest/result/" + planKey.toUpperCase() + "-JOB1/latest.json?expand=logEntries&max-results=250",
                HttpMethod.GET,
                entity,
                Map.class);
        } catch (Exception e) {
            log.error("HttpError while retrieving build result logs from Bamboo: " + e.getMessage());
        }

        ArrayList logs = new ArrayList<BuildLogEntry>();

        if (response != null) {
            for (HashMap<String, Object> logEntry : (List<HashMap>) ((Map) response.getBody().get("logEntries")).get("logEntry")) {
                Instant i = Instant.ofEpochMilli((long) logEntry.get("date"));
                ZonedDateTime logDate = ZonedDateTime.ofInstant(i, ZoneId.systemDefault());
                BuildLogEntry log = new BuildLogEntry(logDate, (String) logEntry.get("log"));
                logs.add(log);
            }
        }
        return logs;
    }


    /**
     * Gets the latest available artifact for the given plan key
     *
      * @param participation
     * @return
     */
    public ResponseEntity retrieveLatestArtifact(Participation participation) {
        String planKey = participation.getBuildPlanId();
        Map<String, Object> latestResult = retrieveLatestBuildResult(planKey);
        // If the build has an artifact, the response contains an artifact key.
        // It seems this key is only available if the "Share" checkbox in Bamboo was used.
        if(latestResult.containsKey("artifact")) {
            // The URL points to the directory. Bamboo returns an "Index of" page.
            // Recursively walk through the responses until we get the actual artifact.
            return retrieveArtifactPage((String)latestResult.get("artifact"));
        }
        else {
            throw new BambooException("No build artifact available for this plan");
        }
    }

    /**
     * Gets the content from a Bamboo artifact link
     * Follows links on HTML directory pages, if necessary
     *
     * @param url
     * @return
     */
    private ResponseEntity retrieveArtifactPage(String url) throws BambooException {
        HttpHeaders headers = HeaderUtil.createAuthorization(BAMBOO_USER, BAMBOO_PASSWORD);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<byte[]> response;

        try {
            response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
        } catch (Exception e) {
            log.error("HttpError while retrieving build artifact", e);
            throw new BambooException("HttpError while retrieving build artifact");
        }

        if(response.getHeaders().containsKey("Content-Type") && response.getHeaders().get("Content-Type").get(0).equals("text/html")) {
            // This is an "Index of" HTML page.
            String html = new String(response.getBody(), StandardCharsets.UTF_8);
            Pattern p = Pattern.compile("href=\"(.*?)\"", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(html);
            if (m.find()) {
                url = m.group(1);
                // Recursively walk through the responses until we get the actual artifact.
                return retrieveArtifactPage(BAMBOO_SERVER_URL + url);
            } else {
                throw new BambooException("No artifact link found on artifact page");
            }
        }
        else {
            // Actual artifact file
            return response;
        }
    }

    /**
     * Retrieves the current build status of the given plan.
     *
     * @param planKey the key of the plan for which to retrieve the status
     * @returna map containing the following data:
     * - isActive: true if the plan is queued or building
     * - isBuilding: true if the plan is building
     */
    public Map<String, Boolean> retrieveBuildStatus(String planKey) {
        HttpHeaders headers = HeaderUtil.createAuthorization(BAMBOO_USER, BAMBOO_PASSWORD);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = null;
        try {
            response = restTemplate.exchange(
                BAMBOO_SERVER_URL + "/rest/api/latest/plan/" + planKey.toUpperCase() + ".json",
                HttpMethod.GET,
                entity,
                Map.class);
        } catch (Exception e) {
            log.error("Bamboo HttpError '" + e.getMessage() + "' while retrieving build status for plan " + planKey, e);
        }
        if (response != null) {
            Map<String, Boolean> result = new HashMap<>();
            boolean isActive = (boolean) response.getBody().get("isActive");
            boolean isBuilding = (boolean) response.getBody().get("isBuilding");
            result.put("isActive", isActive);
            result.put("isBuilding", isBuilding);
            return result;
        }
        return null;
    }

    /**
     * Check if the given build plan is valid and accessible on Bamboo.
     *
     * @param buildPlanId unique identifier for build plan on CI system
     * @return
     */
    @Override
    public Boolean buildPlanIdIsValid(String buildPlanId) {
        HttpHeaders headers = HeaderUtil.createAuthorization(BAMBOO_USER, BAMBOO_PASSWORD);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = null;
        try {
            response = restTemplate.exchange(
                BAMBOO_SERVER_URL + "/rest/api/latest/plan/" + buildPlanId.toUpperCase(),
                HttpMethod.GET,
                entity,
                Map.class);
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    private String getProjectKeyFromBuildPlanId(String buildPlanId) {
        return buildPlanId.split("-")[0];
    }

    private String getPlanKeyFromBuildPlanId(String buildPlanId) {
        return buildPlanId.split("-")[1];
    }

    private String cleanPlanKey(String name) {
        return name.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
