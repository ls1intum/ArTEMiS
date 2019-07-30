package de.tum.in.www1.artemis.service.connectors;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.AssessmentType;
import de.tum.in.www1.artemis.domain.enumeration.FeedbackType;
import de.tum.in.www1.artemis.domain.enumeration.SubmissionType;
import de.tum.in.www1.artemis.exception.BambooException;
import de.tum.in.www1.artemis.exception.BitbucketException;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.swift.bamboo.cli.BambooClient;
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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.tum.in.www1.artemis.config.Constants.ASSIGNMENT_REPO_NAME;

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

    private final GitService gitService;
    private final ResultRepository resultRepository;
    private final FeedbackRepository feedbackRepository;
    private final ParticipationRepository participationRepository;
    private final ProgrammingSubmissionRepository programmingSubmissionRepository;
    private final Optional<VersionControlService> versionControlService;
    private final Optional<ContinuousIntegrationUpdateService> continuousIntegrationUpdateService;
    private final BambooBuildPlanService bambooBuildPlanService;

    public BambooService(GitService gitService, ResultRepository resultRepository, FeedbackRepository feedbackRepository, ParticipationRepository participationRepository,
                         ProgrammingSubmissionRepository programmingSubmissionRepository, Optional<VersionControlService> versionControlService,
                         Optional<ContinuousIntegrationUpdateService> continuousIntegrationUpdateService, BambooBuildPlanService bambooBuildPlanService) {
        this.gitService = gitService;
        this.resultRepository = resultRepository;
        this.feedbackRepository = feedbackRepository;
        this.participationRepository = participationRepository;
        this.programmingSubmissionRepository = programmingSubmissionRepository;
        this.versionControlService = versionControlService;
        this.continuousIntegrationUpdateService = continuousIntegrationUpdateService;
        this.bambooBuildPlanService = bambooBuildPlanService;
    }

    @Override
    public void createBuildPlanForExercise(ProgrammingExercise programmingExercise, String planKey, String repositoryName, String testRepositoryName) {
        bambooBuildPlanService.createBuildPlanForExercise(programmingExercise, planKey, repositoryName, testRepositoryName);
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
    public String copyBuildPlan(String templateBuildPlanId, String wantedPlanKey) {
        wantedPlanKey = getCleanPlanKey(wantedPlanKey);
        String projectKey = getProjectKeyFromBuildPlanId(templateBuildPlanId);
        try {
            return clonePlan(projectKey, getPlanKeyFromBuildPlanId(templateBuildPlanId), projectKey, wantedPlanKey); // Save the new plan in the same project
        } catch (BambooException bambooException) {
            if (bambooException.getMessage().contains("already exists")) {
                log.info("Build Plan already exists. Going to recover build plan information...");
                return getProjectKeyFromBuildPlanId(templateBuildPlanId) + "-" + wantedPlanKey;
            } else throw bambooException;
        }
    }

    //TODO: this method has moved to BitbucketService, but missed the toUpperCase() there, so we reactivated it here
    private String getProjectKeyFromUrl(URL repositoryUrl) {
        // https://ga42xab@repobruegge.in.tum.de/scm/EIST2016RME/RMEXERCISE-ga42xab.git
        return repositoryUrl.getFile().split("/")[2].toUpperCase();
    }

    @Override
    public void configureBuildPlan(ProgrammingExerciseParticipation participation) {
        String buildPlanId = participation.getBuildPlanId();
        URL repositoryUrl = participation.getRepositoryUrlAsUrl();
        updatePlanRepository(
            getProjectKeyFromBuildPlanId(buildPlanId),
            getPlanKeyFromBuildPlanId(buildPlanId),
            ASSIGNMENT_REPO_NAME,
            getProjectKeyFromUrl(repositoryUrl),
            versionControlService.get().getRepositoryName(repositoryUrl)
        );
        enablePlan(getProjectKeyFromBuildPlanId(buildPlanId), getPlanKeyFromBuildPlanId(buildPlanId));
        // We need to trigger an initial update in order for Gitlab to work correctly
        continuousIntegrationUpdateService.get().triggerUpdate(buildPlanId, true);

        // Empty commit - Bamboo bug workaround

        if (BAMBOO_EMPTY_COMMIT_WORKAROUND_NECESSARY) {
            try {
                Repository repo = gitService.getOrCheckoutRepository(repositoryUrl, true);
                gitService.commitAndPush(repo, "Setup");
                ProgrammingExercise exercise = participation.getProgrammingExercise();
                if (exercise == null) {
                    log.warn("Cannot access exercise in 'configureBuildPlan' to determine if deleting the repo after cloning make sense. Will decide to delete the repo");
                    gitService.deleteLocalRepository(repo);
                } else {
                    //only delete the git repository, if the online editor is NOT allowed
                    //this saves some performance on the server, when the student opens the online editor, because the repo does not need to be cloned again
                    //Note: the null check is necessary, because otherwise we might get a null pointer exception
                    if (exercise.isAllowOnlineEditor() == null || exercise.isAllowOnlineEditor() == Boolean.FALSE) {
                        gitService.deleteLocalRepository(repo);
                    }
                }
            } catch (GitAPIException ex) {
                log.error("Git error while doing empty commit", ex);
                return;
            } catch (IOException ex) {
                log.error("IOError while doing empty commit", ex);
                return;
            } catch (InterruptedException ex) {
                log.error("InterruptedException while doing empty commit", ex);
                return;
            } catch (NullPointerException ex) {
                log.error("NullPointerException while doing empty commit", ex);
                return;
            }
        }
    }

    @Override
    public void triggerBuild(ProgrammingExerciseParticipation participation) {
        HttpHeaders headers = HeaderUtil.createAuthorization(BAMBOO_USER, BAMBOO_PASSWORD);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        try {
            restTemplate.exchange(
                BAMBOO_SERVER_URL + "/rest/api/latest/queue/" + participation.getBuildPlanId(),
                HttpMethod.POST,
                entity,
                Map.class);
        } catch (Exception e) {
            log.error("HttpError while triggering build", e);
        }
    }

    @Override
    public void deleteBuildPlan(String buildPlanId) {
        deletePlan(getProjectKeyFromBuildPlanId(buildPlanId), getPlanKeyFromBuildPlanId(buildPlanId));
    }

    @Override
    public void deleteProject(String projectKey) {
        try {
            log.info("Delete project " + projectKey);
            String message = getBambooClient().getProjectHelper().deleteProject(projectKey);
            log.info("Delete project was successful. " + message);
        } catch (CliClient.ClientException | CliClient.RemoteRestException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public BuildStatus getBuildStatus(ProgrammingExerciseParticipation participation) {
        Map<String, Boolean> status = retrieveBuildStatus(participation.getBuildPlanId());
        if (status == null) {
            return BuildStatus.INACTIVE;
        }
        if (status.get("isActive") && !status.get("isBuilding")) {
            return BuildStatus.QUEUED;
        } else if (status.get("isActive") && status.get("isBuilding")) {
            return BuildStatus.BUILDING;
        } else {
            return BuildStatus.INACTIVE;
        }
    }

    @Override
    public List<Feedback> getLatestBuildResultDetails(Result result) {
        ProgrammingExerciseParticipation programmingExerciseParticipation = (ProgrammingExerciseParticipation) result.getParticipation();
        if (result.getParticipation() == null || programmingExerciseParticipation.getBuildPlanId() == null) {
            // most probably the build was cleaned and we do not have access to it any more.
            return null;
        }
        Map<String, Object> buildResultDetails = retrieveLatestBuildResult(programmingExerciseParticipation.getBuildPlanId());
        List<Feedback> feedbackItems = addFeedbackToResult(result, buildResultDetails);
        return feedbackItems;
    }

    @Override
    public List<BuildLogEntry> getLatestBuildLogs(String buildPlanId) {
        return retrieveLatestBuildLogs(buildPlanId);
    }

    @Override
    public URL getBuildPlanWebUrl(ProgrammingExerciseParticipation participation) {
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
     * @param templateProject The Bamboo project in which the plan is contained.
     * @param templatePlan    The plan's name.
     * @param toProject       The Bamboo project in which the new plan should be contained.
     * @param name            The name to give the cloned plan.
     * @return The name of the new build plan
     */
    public String clonePlan(String templateProject, String templatePlan, String toProject, String name) throws BambooException {

        String toPlan = toProject + "-" + name;
        try {
            log.info("Clone build plan " + templateProject + "-" + templatePlan + " to " + toPlan);
            String message = getBambooClient().getPlanHelper().clonePlan(templateProject + "-" + templatePlan, toPlan, toPlan, "", "", true);
            log.info("Clone build plan " + toPlan + " was successful." + message);
        } catch (CliClient.ClientException clientException) {
            if (clientException.getMessage().contains("already exists")) {
                throw new BambooException(clientException.getMessage());
            } else {
                log.error(clientException.getMessage(), clientException);
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
        return continuousIntegrationUpdateService.get().updatePlanRepository(bambooProject, bambooPlan, bambooRepositoryName, repoProjectName, repoName);
    }

    /**
     * Deletes the given plan.
     *
     * @param projectKey
     * @param planKey
     * @return
     */
    private void deletePlan(String projectKey, String planKey) {
        try {
            log.info("Delete build plan " + projectKey + "-" + planKey);
            String message = getBambooClient().getPlanHelper().deletePlan(projectKey + "-" + planKey);
            log.info("Delete build plan was successful. " + message);
        } catch (CliClient.ClientException | CliClient.RemoteRestException e) {
            log.error(e.getMessage());
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
    @Deprecated
    public Result onBuildCompletedOld(ProgrammingExerciseParticipation participation) {
        log.debug("Retrieving build result...");
        Boolean isOldBuildResult = true;
        Map buildResults = new HashMap<>();
        try {
            buildResults = retrieveLatestBuildResult(participation.getBuildPlanId());
            isOldBuildResult = TimeUnit.SECONDS.toMillis(ZonedDateTime.now().toEpochSecond() - ((ZonedDateTime) buildResults.get("buildCompletedDate")).toEpochSecond()) > (60 * 1000);     // older than 60s
        } catch (Exception ex) {
            log.warn("Exception when retrieving a Bamboo build result for build plan " + participation.getBuildPlanId() + ": " + ex.getMessage());
        }

        //TODO: put this request into a timer / queue instead of blocking the request! Because blocking the request actually means that other requests cannot be exectuted
        if (isOldBuildResult) {
            log.warn("It seems we got an old build result from Bamboo for build plan " + participation.getBuildPlanId() + ". Waiting 1s to retrieve build result...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("Sleep error", e);
            }
            log.debug("Retrieving build result (second try)...");
            buildResults = retrieveLatestBuildResult(participation.getBuildPlanId());
        }

        if (buildResults != null && buildResults.containsKey("buildReason")) {
            String buildReason = (String) buildResults.get("buildReason");
            if (buildReason.contains("First build for this plan")) {
                //Filter the first build plan that was automatically executed when the build plan was created
                return null;
            }
        }

        //TODO: only save this result if it is newer (e.g. + 5s) than the last saved result for this participation --> this avoids saving exact same results multiple times

        Result result = new Result();
        result.setRatedIfNotExceeded(participation.getProgrammingExercise().getDueDate(), ZonedDateTime.now());
        result.setAssessmentType(AssessmentType.AUTOMATIC);
        result.setSuccessful((boolean) buildResults.get("successful"));
        result.setResultString((String) buildResults.get("buildTestSummary"));
        result.setCompletionDate((ZonedDateTime) buildResults.get("buildCompletedDate"));
        result.setScore(calculateScoreForResult(result));
        result.setBuildArtifact(buildResults.containsKey("artifact"));
        result.setParticipation((Participation) participation);

        addFeedbackToResult(result, buildResults);
        // save result, otherwise the next database access programmingSubmissionRepository.findByCommitHash will throw an exception
        resultRepository.save(result);

        if (buildResults.containsKey("vcsRevisionKey") || buildResults.containsKey("changesetId")) {
            //we prefer 'changesetId', because it should be correct for multiple commits leading to a build or when test cases have changed.
            // In case it does not exist (e.g. due to a manual build), we fall back to vcsRevisionKey which should be correct in most cases
            String commitHash = (String) buildResults.get("changesetId");
            if (commitHash == null || "".equals(commitHash)) {
                commitHash = (String) buildResults.get("vcsRevisionKey");
            }

            //Due to test case changes there might be multiple submissions with the same commit hash, but with different participations, therefore we have to take the participation_id into account
            ProgrammingSubmission programmingSubmission = programmingSubmissionRepository.findFirstByParticipationIdAndCommitHash(participation.getId(), commitHash);
            if (programmingSubmission == null) { // no matching programming submission
                log.warn("Could not find ProgrammingSubmission for Commit-Hash {} (Participation {}, Build-Plan {}). Will create it subsequently...", commitHash, participation.getId(), participation.getBuildPlanId());
                // this might be a wrong build (what could be the reason), or this might be due to test changes
                // what happens if only the test has changes? should we then create a new submission?
                programmingSubmission = new ProgrammingSubmission();
                programmingSubmission.setParticipation((Participation) participation);
                programmingSubmission.setSubmitted(true);
                programmingSubmission.setType(SubmissionType.OTHER);
                programmingSubmission.setCommitHash(commitHash);
                programmingSubmission.setSubmissionDate(result.getCompletionDate());
            } else {
                log.info("Found corresponding submission to build result with Commit-Hash {}", commitHash);
            }
            // connect submission and result
            result.setSubmission(programmingSubmission);
            programmingSubmission.setResult(result);
            programmingSubmissionRepository.save(programmingSubmission); // result gets saved later, no need to save it now

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

    @Override
    public String getPlanKey(Object requestBody) throws BambooException {
        try {
            Map<String, Object> requestBodyMap = (Map<String, Object>) requestBody;
            Map<String, Object> planMap = (Map<String, Object>) requestBodyMap.get("plan");
            String planKey = (String) planMap.get("key");

            return planKey;

        } catch (Exception e) {
            log.error("Error when getting plan key");
            throw new BitbucketException("Could not get plan key", e);
        }
    }

    @Override
    public Result onBuildCompletedNew(ProgrammingExerciseParticipation participation, Object requestBody) throws Exception {
        log.debug("Retrieving build result (NEW) ...");
        try {
            Map<String, Object> requestBodyMap = (Map<String, Object>) requestBody;
            Map<String, Object> buildMap = (Map<String, Object>) requestBodyMap.get("build");
            String buildReason = (String) buildMap.get("reason");
            if (buildReason != null && buildReason.contains("First build for this plan")) {
                //Filter the first build plan that was automatically executed when the build plan was created
                return null;
            }

            Result result = new Result();
            result.setRatedIfNotExceeded(participation.getProgrammingExercise().getDueDate(), ZonedDateTime.now());
            result.setAssessmentType(AssessmentType.AUTOMATIC);
            result.setSuccessful((Boolean) buildMap.get("successful"));

            Map<String, Object> testSummary = (Map<String, Object>) buildMap.get("testSummary");
            result.setResultString((String) testSummary.get("description"));

            result.setCompletionDate(ZonedDateTime.parse((String) buildMap.get("buildCompletedDate")));
            result.setScore(calculateScoreForResult(result));
            result.setBuildArtifact((Boolean) buildMap.get("artifact"));
            result.setParticipation((Participation) participation);

            addFeedbackToResultNew(result, (List<Object>) buildMap.get("jobs"));

            // save result, otherwise the next database access programmingSubmissionRepository.findByCommitHash will throw an exception
            resultRepository.save(result);

            List<Object> vcsList = (List<Object>) buildMap.get("vcs");

            String commitHash = null;
            for (Object changeSet : vcsList) {
                Map<String, Object> changeSetMap = (Map<String, Object>) changeSet;
                if (changeSetMap.get("repositoryName").equals(ASSIGNMENT_REPO_NAME)) { // We are only interested in the last commit hash of the assignment repo, not the test repo
                    commitHash = (String) changeSetMap.get("id");
                }
            }

            if (commitHash == null) {
                log.warn("Could not find Commit-Hash (Participation {}, Build-Plan {})", participation.getId(), participation.getBuildPlanId());

            } else {
                ProgrammingSubmission programmingSubmission = programmingSubmissionRepository.findFirstByParticipationIdAndCommitHash(participation.getId(), commitHash);
                if (programmingSubmission == null) { // no matching programming submission
                    log.warn("Could not find ProgrammingSubmission for Commit-Hash {} (Participation {}, Build-Plan {}). Will create it subsequently...", commitHash, participation.getId(), participation.getBuildPlanId());
                    // this might be a wrong build (what could be the reason), or this might be due to test changes
                    // what happens if only the test has changes? should we then create a new submission?
                    programmingSubmission = new ProgrammingSubmission();
                    programmingSubmission.setParticipation((Participation) participation);
                    programmingSubmission.setSubmitted(true);
                    programmingSubmission.setType(SubmissionType.OTHER);
                    programmingSubmission.setCommitHash(commitHash);
                    programmingSubmission.setSubmissionDate(result.getCompletionDate());
                } else {
                    //TODO: handle the case that the programming submission alredy has a result
                    if (programmingSubmission.getResult() != null) {
                        log.warn("A result for the programming submission " + programmingSubmission.getId() + " does already exist");
                    }
                    log.info("Found corresponding submission to build result with Commit-Hash {}", commitHash);
                }

                result.setSubmission(programmingSubmission);
                programmingSubmission.setResult(result);
                participation.addSubmissions(programmingSubmission);
            }

            participation.addResult(result);
            return result;

        } catch (Exception e) {
            log.error("Error when getting build result");
            throw new BitbucketException("Could not get build result", e);
        }
    }

    /**
     * Converts build result details into feedback and stores it in the result object
     *
     * @param
     * @param buildResultDetails returned build result details from the rest API of bamboo
     * @return a list of feedbacks itemsstored in a result
     */
    public List<Feedback> addFeedbackToResult(Result result, Map<String, Object> buildResultDetails) {
        if (buildResultDetails == null) {
            return null;
        }

        try {
            List<Map<String, Object>> details = (List<Map<String, Object>>) buildResultDetails.get("details");
            if (!details.isEmpty()) {
                result.setHasFeedback(true);
            }
            //breaking down the Bamboo API answer to get all the relevant details
            for (Map<String, Object> detail : details) {
                String className = (String) detail.get("className");
                String methodName = (String) detail.get("methodName");

                Map<String, Object> errorsMap = (Map<String, Object>) detail.get("errors");
                List<Map<String, Object>> errors = (List<Map<String, Object>>) errorsMap.get("error");

                String errorMessageString = "";
                for (Map<String, Object> error : errors) {
                    //Splitting string at the first linebreak to only get the first line of the Exception
                    errorMessageString += ((String) error.get("message")).split("\\n", 2)[0] + "\n";
                }

                createAutomaticFeedback(result, methodName, false, errorMessageString);
            }
        } catch (Exception failedToParse) {
            log.error("Parsing from bamboo to feedback failed" + failedToParse);
        }

        return result.getFeedbacks();
    }

    private void createAutomaticFeedback(Result result, String methodName, boolean positive, String errorMessageString) {
        Feedback feedback = new Feedback();
        feedback.setText(methodName);
        feedback.setDetailText(errorMessageString);
        feedback.setType(FeedbackType.AUTOMATIC);
        feedback.setPositive(positive);
        feedback = feedbackRepository.save(feedback);
        result.addFeedback(feedback);
    }

    /**
     * Converts build result details into feedback and stores it in the result object
     *
     * @param result     the result for which the feedback should be added
     * @param jobs the jobs list of the requestBody
     * @return a list of feedbacks itemsstored in a result
     */
    public List<Feedback> addFeedbackToResultNew(Result result, List<Object> jobs) {
        if (jobs == null) {
            return null;
        }

        try {
            List<Map<String, Object>> castedJobs = (List<Map<String, Object>>) (Object) jobs; // TODO: check if this works correctly

            for (Map<String, Object> job : castedJobs) {
                List<Map<String, Object>> failedTests = (List<Map<String, Object>>) job.get("failedTests");
                for (Map<String, Object> failedTest : failedTests) {
                    String className = (String) failedTest.get("className");
                    String methodName = (String) failedTest.get("name"); // in the attribute "methodName", bamboo seems to apply some unwanted logic

                    List<String> errors = (List<String>) failedTest.get("errors");
                    String errorMessageString = "";
                    for (String error : errors) {
                        //Splitting string at the first linebreak to only get the first line of the Exception
                        errorMessageString += error.split("\\n", 2)[0] + "\n";
                    }

                    log.debug("errorMSGString is {}", errorMessageString);

                    createAutomaticFeedback(result, methodName, false, errorMessageString);
                }

                List<Map<String, Object>> succuessfulTests = (List<Map<String, Object>>) job.get("successfulTests");
                for (Map<String, Object> succuessfulTest : succuessfulTests) {
                    String className = (String) succuessfulTest.get("className");
                    String methodName = (String) succuessfulTest.get("name"); // in the attribute "methodName", bamboo seems to apply some unwanted logic

                    createAutomaticFeedback(result, methodName, true, null);
                }

                if (!failedTests.isEmpty()) {
                    result.setHasFeedback(true);
                }
            }

        } catch (Exception e) {
            log.error("Could not get feedback from jobs " + e);
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

            Pattern pattern = Pattern.compile("^([0-9]+) of ([0-9]+) failed");
            Matcher matcher = pattern.matcher(result.getResultString());

            if (matcher.find()) {
                float failedTests = Float.parseFloat(matcher.group(1));
                float totalTests = Float.parseFloat(matcher.group(2));
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
     * - successful:            if the build was successful
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
                BAMBOO_SERVER_URL + "/rest/api/latest/result/" + planKey.toUpperCase() + "-JOB1/latest.json?expand=testResults.failedTests.testResult.errors,artifacts,changes",
                HttpMethod.GET,
                entity,
                Map.class);
        } catch (Exception e) {
            log.error("HttpError while retrieving latest build results from Bamboo for planKey " + planKey + ":" + e.getMessage());
        }
        if (response != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("successful", response.getBody().get("buildState").equals("Successful"));
            result.put("buildTestSummary", response.getBody().get("buildTestSummary"));
            if (response.getBody().containsKey("buildReason")) {
                result.put("buildReason", response.getBody().get("buildReason"));
            }
            String dateString = (String) response.getBody().get("buildCompletedDate");
            ZonedDateTime buildCompletedDate = ZonedDateTime.parse(dateString);
            result.put("buildCompletedDate", buildCompletedDate);

            //search for test case results
            List resultDetails = (List) ((Map) ((Map) response.getBody().get("testResults")).get("failedTests")).get("testResult");
            //might be empty
            result.put("details", resultDetails);

            //search for version control information
            if (response.getBody().containsKey("vcsRevisionKey")) {
                result.put("vcsRevisionKey", response.getBody().get("vcsRevisionKey"));
            }
            if (response.getBody().containsKey("changes")) {
                Map<String, Object> changesEntry = (Map<String, Object>) response.getBody().get("changes");
                int size = (int) changesEntry.get("size");
                if (changesEntry.containsKey("change")) {
                    List<Object> changesList = (List<Object>) changesEntry.get("change");
                    if (changesList.size() > 0) {
                        //Take the latest change, i.e. size - 1
                        Map<String, Object> change = (Map<String, Object>) changesList.get(size - 1);
                        if (change.containsKey("changesetId")) {
                            result.put("changesetId", change.get("changesetId"));
                        }
                    }
                }
            }

            //search for artifacts: take the first one that is not a build log
            if (response.getBody().containsKey("artifacts")) {
                Map<String, Object> artifactsEntity = (Map<String, Object>) response.getBody().get("artifacts");
                if ((int) artifactsEntity.get("size") > 0 && artifactsEntity.containsKey("artifact")) {
                    List<Map<String, Object>> artifacts = (List<Map<String, Object>>) artifactsEntity.get("artifact");
                    for (Map<String, Object> artifact : artifacts) {
                        if (((String) artifact.get("name")).equalsIgnoreCase("Build log")) {
                            continue;
                        } else {
                            String link = (String) ((Map<String, Object>) artifact.get("link")).get("href");
                            result.put("artifact", link);
                        }
                    }
                }
            }

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
    //TODO: save this on the Artemis server, e.g. in the result class so that Artemis does not need to retrieve it every time
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

        List logs = new ArrayList<BuildLogEntry>();

        if (response != null) {
            for (Map<String, Object> logEntry : (List<Map>) ((Map) response.getBody().get("logEntries")).get("logEntry")) {
                String logString = (String) logEntry.get("log");
                boolean compilationErrorFound = true;

                if (logString.contains("COMPILATION ERROR")) {
                    compilationErrorFound = true;
                }

                if (compilationErrorFound && logString.contains("BUILD FAILURE")) {
                    // hide duplicated information that is displayed in the section COMPILATION ERROR and in the section BUILD FAILURE and stop here
                    break;
                }

                //filter unnecessary logs
                if ((logString.startsWith("[INFO]") && !logString.contains("error")) ||
                    logString.startsWith("[WARNING]") ||
                    logString.startsWith("[ERROR] [Help 1]") ||
                    logString.startsWith("[ERROR] For more information about the errors and possible solutions") ||
                    logString.startsWith("[ERROR] Re-run Maven using") ||
                    logString.startsWith("[ERROR] To see the full stack trace of the errors") ||
                    logString.startsWith("[ERROR] -> [Help 1]")
                ) {
                    continue;
                }

                //Replace some unnecessary information and hide complex details to make it easier to read the important information
                logString = logString.replaceAll("/opt/bamboo-agent-home/xml-data/build-dir/", "");

                Instant instant = Instant.ofEpochMilli((long) logEntry.get("date"));
                ZonedDateTime logDate = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
                BuildLogEntry log = new BuildLogEntry(logDate, logString);
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
    public ResponseEntity retrieveLatestArtifact(ProgrammingExerciseParticipation participation) {
        String planKey = participation.getBuildPlanId();
        Map<String, Object> latestResult = retrieveLatestBuildResult(planKey);
        // If the build has an artifact, the response contains an artifact key.
        // It seems this key is only available if the "Share" checkbox in Bamboo was used.
        if (latestResult.containsKey("artifact")) {
            // The URL points to the directory. Bamboo returns an "Index of" page.
            // Recursively walk through the responses until we get the actual artifact.
            return retrieveArtifactPage((String) latestResult.get("artifact"));
        } else {
            throw new BambooException("No build artifact available for this plan");
        }
    }

    @Override
    public String checkIfProjectExists(String projectKey, String projectName) {
        HttpHeaders headers = HeaderUtil.createAuthorization(BAMBOO_USER, BAMBOO_PASSWORD);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = null;
        try {
            response = restTemplate.exchange(
                BAMBOO_SERVER_URL + "/rest/api/latest/project/" + projectKey,
                HttpMethod.GET,
                entity,
                Map.class);
            log.warn("Bamboo project " + projectKey + " already exists");
            return "The project " + projectKey + " already exists in the CI Server. Please choose a different short name!";
        } catch (HttpClientErrorException e) {
            log.debug("Bamboo project " + projectKey + " does not exit");
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                //only if this is the case, we additionally check that the project name is unique
                response = restTemplate.exchange(
                    BAMBOO_SERVER_URL + "/rest/api/latest/search/projects?searchTerm=" + projectName,
                    HttpMethod.GET,
                    entity,
                    Map.class);
                if ((Integer) response.getBody().get("size") != 0) {
                    List<Object> ciProjects = (List<Object>) response.getBody().get("searchResults");
                    for (Object ciProject : ciProjects) {
                        String ciProjectName = (String) ((Map) ((Map) ciProject).get("searchEntity")).get("projectName");
                        if (ciProjectName.equalsIgnoreCase(projectName)) {
                            log.warn("Bamboo project with name" + projectName + " already exists");
                            return "The project " + projectName + " already exists in the CI Server. Please choose a different title!";
                        }
                    }
                }
                return null;
            }
        }
        return "The project already exists in the CI Server. Please choose a different title and short name!";
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

        //Note: Content-Type might contain additional elements such as the UTF-8 encoding, therefore we now use contains instead of equals
        if (response.getHeaders().containsKey("Content-Type") && response.getHeaders().get("Content-Type").get(0).contains("text/html")) {
            // This is an "Index of" HTML page.
            String html = new String(response.getBody(), StandardCharsets.UTF_8);
            Pattern pattern = Pattern.compile("href=\"(.*?)\"", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                url = matcher.group(1);
                // Recursively walk through the responses until we get the actual artifact.
                return retrieveArtifactPage(BAMBOO_SERVER_URL + url);
            } else {
                throw new BambooException("No artifact link found on artifact page");
            }
        } else {
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

    private String getCleanPlanKey(String name) {
        return name.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
