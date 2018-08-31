package de.tum.in.www1.artemis.service;

import de.tum.in.www1.artemis.domain.BuildLogEntry;
import de.tum.in.www1.artemis.domain.Feedback;
import de.tum.in.www1.artemis.domain.Participation;
import de.tum.in.www1.artemis.domain.Result;
import de.tum.in.www1.artemis.domain.enumeration.FeedbackType;
import de.tum.in.www1.artemis.exception.BambooException;
import de.tum.in.www1.artemis.repository.FeedbackRepository;
import de.tum.in.www1.artemis.repository.ParticipationRepository;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;
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

    @Value("${artemis.bitbucket.url}")
    private URL BITBUCKET_SERVER_URL;

    @Value("${artemis.bitbucket.user}")
    private String BITBUCKET_USER;

    @Value("${artemis.bitbucket.password}")
    private String BITBUCKET_PASSWORD;

    @Value("${artemis.bamboo.url}")
    private URL BAMBOO_SERVER_URL;

    @Value("${artemis.bamboo.bitbucket-application-link-id}")
    private String BITBUCKET_APPLICATION_LINK_ID;

    @Value("${artemis.bamboo.user}")
    private String BAMBOO_USER;

    @Value("${artemis.bamboo.password}")
    private String BAMBOO_PASSWORD;

    @Value("${artemis.bitbucket.url}")
    private URL BITBUCKET_SERVER;

    @Value("${artemis.result-retrieval-delay}")
    private int RESULT_RETRIEVAL_DELAY = 10000;

    private static String REPO_REFERRAL_NAME = "Assignment";

    private final GitService gitService;
    private final ResultRepository resultRepository;
    private final FeedbackRepository feedbackRepository;
    private final ParticipationRepository participationRepository;

    public BambooService(GitService gitService, ResultRepository resultRepository, FeedbackRepository feedbackRepository, ParticipationRepository participationRepository) {
        this.gitService = gitService;
        this.resultRepository = resultRepository;
        this.feedbackRepository = feedbackRepository;
        this.participationRepository = participationRepository;
    }

    public BitbucketClient getBitbucketClient() {
        final BitbucketClient bitbucketClient = new BitbucketClient();
        //setup the Bamboo Client to use the correct username and password

        String[] args = new String[]{
            "-s", BITBUCKET_SERVER_URL.toString(),
            "--user", BITBUCKET_USER,
            "--password", BITBUCKET_PASSWORD,
        };

        bitbucketClient.doWork(args); //only invoke this to set server address, username and password so that the following action will work
        return bitbucketClient;
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
    public String copyBuildPlan(String baseBuildPlanId, String wantedPlanKey) {
        wantedPlanKey = cleanPlanKey(wantedPlanKey);
        try {
            return clonePlan(getProjectKeyFromBuildPlanId(baseBuildPlanId), getPlanKeyFromBuildPlanId(baseBuildPlanId), wantedPlanKey);
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
    public void configureBuildPlan(String buildPlanId, URL repositoryUrl, String planKey) {
        updatePlanRepository(
            getProjectKeyFromBuildPlanId(buildPlanId),
            getPlanKeyFromBuildPlanId(buildPlanId),
            REPO_REFERRAL_NAME,
            getProjectKeyFromUrl(repositoryUrl),
            getRepositorySlugFromUrl(repositoryUrl)
        );
        enablePlan(getProjectKeyFromBuildPlanId(buildPlanId), getPlanKeyFromBuildPlanId(buildPlanId));
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
     * @param name        The name to give the cloned plan.
     * @return            The name of the new build plan
     */
    public String clonePlan(String baseProject, String basePlan, String name) throws BambooException {

        String toPlan = baseProject + "-" + name;
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

    /**
     * Updates the configured repository for a given plan to the given Bitbucket Server repository.
     *
     * @param bambooProject        The key of the Bamboo plan's project, e.g. 'EIST16W1'.
     * @param bambooPlan           The plan key, which is usually the name, e.g. 'ga56hur'.
     * @param bambooRepositoryName The name of the configured repository in the Bamboo plan.
     * @param bitbucketProject     The key for the Bitbucket Server (formerly Stash) project to which we want to update the plan.
     * @param bitbucketRepository  The name/slug for the Bitbucket Server (formerly Stash) repository to which we want to update the plan.
     */
    public String updatePlanRepository(String bambooProject, String bambooPlan, String bambooRepositoryName, String bitbucketProject, String bitbucketRepository) throws BambooException {

        try {
            //get the repositoryId to find the correct value for field2 below
            final BitbucketClient bitbucketClient = getBitbucketClient();
            RemoteRepository remoteRepository = bitbucketClient.getRepositoryHelper().getRemoteRepository(bitbucketProject, bitbucketRepository, true);

            final BambooClient bambooClient = new BambooClient();
            String[] args = new String[]{
                "--field1", "repository.stash.projectKey", "--value1", bitbucketProject,
                "--field2", "repository.stash.repositoryId", "--value2", remoteRepository.getId().toString(),
                "--field3", "repository.stash.repositorySlug", "--value3", bitbucketRepository,
                "--field4", "repository.stash.repositoryUrl", "--value4", buildSshRepositoryUrl(bitbucketProject, bitbucketRepository), // e.g. "ssh://git@repobruegge.in.tum.de:7999/madm/helloworld.git"
                "--field5", "repository.stash.server", "--value5", BITBUCKET_APPLICATION_LINK_ID,
                "--field6", "repository.stash.branch", "--value6", "master",
                "-s", BAMBOO_SERVER_URL.toString(),
                "--user", BAMBOO_USER,
                "--password", BAMBOO_PASSWORD,
//            "--targetServer", "https://repobruegge.in.tum.de"     //in the future, we might be able to use this and save many other arguments above, then we could also get rid of BITBUCKET_APPLICATION_LINK_ID
            };
            //workaround to pass additional fields
            bambooClient.doWork(args);

            log.info("Update plan repository for build plan " + bambooProject + "-" + bambooPlan);
            String message = bambooClient.getRepositoryHelper().addOrUpdateRepository(bambooRepositoryName, null, null, bambooProject + "-" + bambooPlan, "BITBUCKET_SERVER", null, false, true, true);
            log.info("Update plan repository for build plan " + bambooProject + "-" + bambooPlan + " was successful." + message);
            return message;
        } catch (CliClient.ClientException | CliClient.RemoteRestException e) {
            log.error(e.getMessage(), e);
            throw new BambooException("Something went wrong while updating the plan repository", e);
        }
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
            return retrievArtifactPage((String)latestResult.get("artifact"));
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
    private ResponseEntity retrievArtifactPage(String url) throws BambooException {
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
                return retrievArtifactPage(BAMBOO_SERVER_URL + url);
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


    private String buildSshRepositoryUrl(String project, String slug) {
        final int sshPort = 7999;

        return "ssh://git@" + BITBUCKET_SERVER.getHost() + ":" + sshPort + "/" + project.toLowerCase() + "/" + slug.toLowerCase() + ".git";
    }

    private String getProjectKeyFromBuildPlanId(String buildPlanId) {
        return buildPlanId.split("-")[0];
    }

    private String getPlanKeyFromBuildPlanId(String buildPlanId) {
        return buildPlanId.split("-")[1];
    }

    private String getProjectKeyFromUrl(URL repositoryUrl) {
        // https://ga42xab@repobruegge.in.tum.de/scm/EIST2016RME/RMEXERCISE-ga42xab.git
        return repositoryUrl.getFile().split("/")[2].toUpperCase();
    }


    private String cleanPlanKey(String name) {
        return name.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    private String getRepositorySlugFromUrl(URL repositoryUrl) {
        // https://ga42xab@repobruegge.in.tum.de/scm/EIST2016RME/RMEXERCISE-ga42xab.git
        String repositorySlug = repositoryUrl.getFile().split("/")[3];
        if (repositorySlug.endsWith(".git")) {
            repositorySlug = repositorySlug.substring(0, repositorySlug.length() - 4);
        }
        return repositorySlug;
    }
}
