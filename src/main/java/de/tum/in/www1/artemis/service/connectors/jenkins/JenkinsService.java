package de.tum.in.www1.artemis.service.connectors.jenkins;

import static de.tum.in.www1.artemis.config.Constants.ASSIGNMENT_REPO_NAME;
import static de.tum.in.www1.artemis.config.Constants.TEST_REPO_NAME;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.http.client.HttpResponseException;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offbytwo.jenkins.JenkinsServer;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.ProgrammingLanguage;
import de.tum.in.www1.artemis.domain.participation.ProgrammingExerciseParticipation;
import de.tum.in.www1.artemis.exception.ContinuousIntegrationException;
import de.tum.in.www1.artemis.exception.JenkinsException;
import de.tum.in.www1.artemis.repository.FeedbackRepository;
import de.tum.in.www1.artemis.repository.ProgrammingExerciseRepository;
import de.tum.in.www1.artemis.repository.ProgrammingSubmissionRepository;
import de.tum.in.www1.artemis.service.BuildLogEntryService;
import de.tum.in.www1.artemis.service.connectors.AbstractContinuousIntegrationService;
import de.tum.in.www1.artemis.service.connectors.CIPermission;
import de.tum.in.www1.artemis.service.connectors.ConnectorHealth;
import de.tum.in.www1.artemis.service.connectors.jenkins.dto.TestCaseDTO;
import de.tum.in.www1.artemis.service.connectors.jenkins.dto.TestResultsDTO;
import de.tum.in.www1.artemis.service.connectors.jenkins.jobs.JenkinsJobService;
import de.tum.in.www1.artemis.service.dto.AbstractBuildResultNotificationDTO;
import de.tum.in.www1.artemis.service.util.UrlUtils;

@Profile("jenkins")
@Service
public class JenkinsService extends AbstractContinuousIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(JenkinsService.class);

    private static final String PIPELINE_SCRIPT_DETECTION_COMMENT = "// ARTEMIS: JenkinsPipeline";

    @Value("${jenkins.use-crumb:#{true}}")
    private boolean useCrumb;

    private final ProgrammingExerciseRepository programmingExerciseRepository;

    private final JenkinsBuildPlanService jenkinsBuildPlanService;

    private final JenkinsServer jenkinsServer;

    private final JenkinsJobService jenkinsJobService;

    private final JenkinsInternalUrlService jenkinsInternalUrlService;

    public JenkinsService(@Qualifier("jenkinsRestTemplate") RestTemplate restTemplate, JenkinsServer jenkinsServer, ProgrammingSubmissionRepository programmingSubmissionRepository,
            ProgrammingExerciseRepository programmingExerciseRepository, FeedbackRepository feedbackRepository,
            @Qualifier("shortTimeoutJenkinsRestTemplate") RestTemplate shortTimeoutRestTemplate, BuildLogEntryService buildLogService,
            JenkinsBuildPlanService jenkinsBuildPlanService, JenkinsJobService jenkinsJobService, JenkinsInternalUrlService jenkinsInternalUrlService) {
        super(programmingSubmissionRepository, feedbackRepository, buildLogService, restTemplate, shortTimeoutRestTemplate);
        this.programmingExerciseRepository = programmingExerciseRepository;
        this.jenkinsServer = jenkinsServer;
        this.jenkinsBuildPlanService = jenkinsBuildPlanService;
        this.jenkinsJobService = jenkinsJobService;
        this.jenkinsInternalUrlService = jenkinsInternalUrlService;
    }

    @Override
    public void createBuildPlanForExercise(ProgrammingExercise exercise, String planKey, VcsRepositoryUrl repositoryURL, VcsRepositoryUrl testRepositoryURL,
            VcsRepositoryUrl solutionRepositoryURL) {
        repositoryURL = jenkinsInternalUrlService.toInternalVcsUrl(repositoryURL);
        testRepositoryURL = jenkinsInternalUrlService.toInternalVcsUrl(testRepositoryURL);
        jenkinsBuildPlanService.createBuildPlanForExercise(exercise, planKey, repositoryURL, testRepositoryURL);
    }

    @Override
    public void performEmptySetupCommit(ProgrammingExerciseParticipation participation) {
        // Not needed for Jenkins
    }

    @Override
    public String copyBuildPlan(String sourceProjectKey, String sourcePlanName, String targetProjectKey, String targetProjectName, String targetPlanName,
            boolean targetProjectExists) {
        return jenkinsBuildPlanService.copyBuildPlan(sourceProjectKey, sourcePlanName, targetProjectKey, targetProjectName, targetPlanName, targetProjectExists);
    }

    @Override
    public void givePlanPermissions(ProgrammingExercise programmingExercise, String planName) {
        jenkinsBuildPlanService.givePlanPermissions(programmingExercise, planName);
    }

    @Override
    public void configureBuildPlan(ProgrammingExerciseParticipation participation) {
        // Refetch the programming exercise with the template participation and assign it to programmingExerciseParticipation to make sure it is initialized (and not a proxy)
        final var programmingExercise = programmingExerciseRepository.findWithTemplateAndSolutionParticipationById(participation.getProgrammingExercise().getId()).get();
        participation.setProgrammingExercise(programmingExercise);
        final var projectKey = programmingExercise.getProjectKey();
        final var planKey = participation.getBuildPlanId();
        final var templateRepoUrl = programmingExercise.getTemplateRepositoryUrl();
        updatePlanRepository(projectKey, planKey, ASSIGNMENT_REPO_NAME, null /* not needed */, participation.getRepositoryUrl(), templateRepoUrl, Optional.empty());
        enablePlan(projectKey, planKey);
    }

    @Override
    public void updatePlanRepository(String buildProjectKey, String buildPlanKey, String ciRepoName, String repoProjectKey, String newRepoUrl, String existingRepoUrl,
            Optional<List<String>> optionalTriggeredByRepositories) {
        newRepoUrl = jenkinsInternalUrlService.toInternalVcsUrl(newRepoUrl);
        existingRepoUrl = jenkinsInternalUrlService.toInternalVcsUrl(existingRepoUrl);

        // remove potential username from repo URL. Jenkins uses the Artemis Admin user and will fail if other usernames are in the URL
        final var repoUrl = newRepoUrl.replaceAll("(https?://)(.*@)(.*)", "$1$3");
        final var jobXmlDocument = jenkinsJobService.getJobConfigForJobInFolder(buildProjectKey, buildPlanKey);

        try {
            replaceScriptParameters(jobXmlDocument, ciRepoName, repoUrl, existingRepoUrl);
        }
        catch (IllegalArgumentException e) {
            log.info("Falling back to old Jenkins setup replacement for build xml");
            replaceRemoteURLs(jobXmlDocument, repoUrl, ciRepoName);
        }

        final var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        final var entity = new HttpEntity<>(jenkinsJobService.writeXmlToString(jobXmlDocument), headers);

        URI uri = Endpoint.PLAN_CONFIG.buildEndpoint(serverUrl.toString(), buildProjectKey, buildPlanKey).build(true).toUri();

        final var errorMessage = "Error trying to configure build plan in Jenkins " + buildPlanKey;
        try {
            final var response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new JenkinsException(errorMessage + "; statusCode=" + response.getStatusCode() + "; headers=" + response.getHeaders() + "; body=" + response.getBody());
            }
        }
        catch (HttpClientErrorException e) {
            log.error(errorMessage, e);
            throw new JenkinsException(errorMessage, e);
        }
    }

    private void replaceScriptParameters(Document jobXmlDocument, String ciRepoName, String repoUrl, String baseRepoUrl) throws IllegalArgumentException {
        final var scriptNode = findScriptNode(jobXmlDocument);
        if (scriptNode == null || scriptNode.getFirstChild() == null) {
            log.debug("Pipeline Script not found");
            throw new IllegalArgumentException("Pipeline Script not found");
        }

        String pipeLineScript = scriptNode.getFirstChild().getTextContent().trim();
        // If the script does not start with "pipeline" or the special comment,
        // it is not actually a pipeline script, but a deprecated programming exercise with an old build xml configuration
        if (!pipeLineScript.startsWith("pipeline") && !pipeLineScript.startsWith(PIPELINE_SCRIPT_DETECTION_COMMENT)) {
            log.debug("Pipeline Script not found");
            throw new IllegalArgumentException("Pipeline Script not found");
        }
        // Replace repo URL
        // TODO: properly replace the baseRepoUrl with repoUrl by looking up the ciRepoName in the pipelineScript
        pipeLineScript = pipeLineScript.replace(baseRepoUrl, repoUrl);

        scriptNode.getFirstChild().setTextContent(pipeLineScript);
    }

    /**
     * Replace old XML files that are not based on pipelines.
     * Will be removed in the future
     *
     * @param jobXmlDocument the Document where the remote config should replaced
     */
    @Deprecated
    private void replaceRemoteURLs(Document jobXmlDocument, String repoUrl, String repoNameInCI) throws IllegalArgumentException {
        final var remoteUrlNode = findUserRemoteConfigFor(jobXmlDocument, repoNameInCI);
        if (remoteUrlNode == null || remoteUrlNode.getFirstChild() == null) {
            throw new IllegalArgumentException("Url to replace not found in job xml document");
        }
        remoteUrlNode.getFirstChild().setNodeValue(repoUrl);
    }

    private org.w3c.dom.Node findScriptNode(Document jobXmlDocument) {
        final var userRemoteConfigs = jobXmlDocument.getElementsByTagName("script");
        return userRemoteConfigs.item(0);
    }

    private org.w3c.dom.Node findUserRemoteConfigFor(Document jobXmlDocument, String repoNameInCI) {
        final var userRemoteConfigs = jobXmlDocument.getElementsByTagName("hudson.plugins.git.UserRemoteConfig");
        if (userRemoteConfigs.getLength() != 2) {
            throw new IllegalArgumentException("Configuration of build plans currently only supports a model with two repositories, ASSIGNMENT and TESTS");
        }
        var firstUserRemoteConfig = userRemoteConfigs.item(0).getChildNodes();
        var urlElement = findUrlElement(firstUserRemoteConfig, repoNameInCI);
        if (urlElement != null) {
            return urlElement;
        }
        var secondUserRemoteConfig = userRemoteConfigs.item(1).getChildNodes();
        urlElement = findUrlElement(secondUserRemoteConfig, repoNameInCI);
        return urlElement;
    }

    private org.w3c.dom.Node findUrlElement(NodeList nodeList, String repoNameInCI) {
        boolean found = false;
        org.w3c.dom.Node urlNode = null;
        for (int i = 0; i < nodeList.getLength(); i++) {
            var childElement = nodeList.item(i);
            if ("name".equalsIgnoreCase(childElement.getNodeName())) {
                var nameValue = childElement.hasChildNodes() ? childElement.getFirstChild().getNodeValue() : null;
                // this name was added recently, so we cannot assume that all job xml files include this name
                if (repoNameInCI.equalsIgnoreCase(nameValue)) {
                    found = true;
                }
            }
            else if ("url".equalsIgnoreCase(childElement.getNodeName())) {
                urlNode = childElement;
                if (!found) {
                    // fallback for old xmls
                    var urlValue = childElement.hasChildNodes() ? childElement.getFirstChild().getNodeValue() : null;
                    if (urlValue != null && repoNameInCI.equals(ASSIGNMENT_REPO_NAME) && ((urlValue.contains("-exercise.git") || (urlValue.contains("-solution.git"))))) {
                        found = true;
                    }
                    else if (urlValue != null && repoNameInCI.equals(TEST_REPO_NAME) && urlValue.contains("-tests.git")) {
                        found = true;
                    }
                }
            }
        }

        if (found && urlNode != null) {
            return urlNode;
        }
        else {
            return null;
        }
    }

    @Override
    public void triggerBuild(ProgrammingExerciseParticipation participation) {
        final var projectKey = participation.getProgrammingExercise().getProjectKey();
        final var planKey = participation.getBuildPlanId();
        jenkinsBuildPlanService.triggerBuild(projectKey, planKey);
    }

    @Override
    public void deleteProject(String projectKey) {
        try {
            jenkinsServer.deleteJob(projectKey, useCrumb);
        }
        catch (HttpResponseException e) {
            // We don't throw an exception if the project doesn't exist in Jenkins (404 status)
            if (e.getStatusCode() != org.apache.http.HttpStatus.SC_NOT_FOUND) {
                log.error(e.getMessage(), e);
                throw new JenkinsException("Error while trying to delete folder in Jenkins for " + projectKey, e);
            }
        }
        catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new JenkinsException("Error while trying to delete folder in Jenkins for " + projectKey, e);
        }
    }

    @Override
    public void deleteBuildPlan(String projectKey, String planKey) {
        jenkinsBuildPlanService.deleteBuildPlan(projectKey, planKey);
    }

    @Override
    public String getPlanKey(Object requestBody) throws Exception {
        final var result = TestResultsDTO.convert(requestBody);
        final var nameParams = result.getFullName().split(" ");
        /*
         * Jenkins gives the full name of a job as <FOLDER NAME> » <JOB NAME> <Build Number> E.g. the third build of an exercise (projectKey = TESTEXC) for its solution build
         * (TESTEXC-SOLUTION) would be: TESTEXC » TESTEXC-SOLUTION #3 ==> This would mean that at index 2, we have the actual job/plan key, i.e. TESTEXC-SOLUTION
         */
        if (nameParams.length != 4) {
            var requestBodyString = new ObjectMapper().writeValueAsString(requestBody);
            log.error("Can't extract planKey from requestBody! Not a test notification result!: {}", requestBodyString);
            throw new JenkinsException("Can't extract planKey from requestBody! Not a test notification result!: " + requestBodyString);
        }

        return nameParams[2];
    }

    @Override
    public Result onBuildCompleted(ProgrammingExerciseParticipation participation, Object requestBody) {
        final var buildResult = TestResultsDTO.convert(requestBody);
        var newResult = createResultFromBuildResult(buildResult, participation);

        // Fetch submission or create a fallback
        var latestSubmission = super.getSubmissionForBuildResult(participation.getId(), buildResult).orElseGet(() -> createAndSaveFallbackSubmission(participation, buildResult));
        latestSubmission.setBuildFailed("No tests found".equals(newResult.getResultString()));

        // Parse, filter, and save the build logs if they exist
        if (buildResult.getLogs() != null) {
            ProgrammingLanguage programmingLanguage = participation.getProgrammingExercise().getProgrammingLanguage();
            List<BuildLogEntry> buildLogEntries = JenkinsBuildLogParseUtils.parseBuildLogsFromJenkinsLogs(buildResult.getLogs());
            buildLogEntries = filterUnnecessaryLogs(buildLogEntries, programmingLanguage);
            buildLogEntries = buildLogService.saveBuildLogs(buildLogEntries, latestSubmission);

            // Set the received logs in order to avoid duplicate entries (this removes existing logs)
            latestSubmission.setBuildLogEntries(buildLogEntries);
        }

        // Note: we only set one side of the relationship because we don't know yet whether the result will actually be saved
        newResult.setSubmission(latestSubmission);
        newResult.setRatedIfNotExceeded(participation.getProgrammingExercise().getDueDate(), latestSubmission);

        return newResult;
    }

    @Override
    public Optional<String> getWebHookUrl(String projectKey, String buildPlanId) {
        var urlString = serverUrl + "/project/" + projectKey + "/" + buildPlanId;
        return Optional.of(jenkinsInternalUrlService.toInternalCiUrl(urlString));
    }

    @Override
    public BuildStatus getBuildStatus(ProgrammingExerciseParticipation participation) {
        if (participation.getBuildPlanId() == null) {
            // The build plan does not exist, the build status cannot be retrieved
            return null;
        }

        final var projectKey = participation.getProgrammingExercise().getProjectKey();
        final var planKey = participation.getBuildPlanId();
        return jenkinsBuildPlanService.getBuildStatusOfPlan(projectKey, planKey);
    }

    @Override
    public boolean checkIfBuildPlanExists(String projectKey, String buildPlanId) {
        return jenkinsBuildPlanService.buildPlanExists(projectKey, buildPlanId);
    }

    @Override
    protected void addFeedbackToResult(Result result, AbstractBuildResultNotificationDTO buildResult) {
        final ProgrammingExercise programmingExercise = (ProgrammingExercise) result.getParticipation().getExercise();
        final ProgrammingLanguage programmingLanguage = programmingExercise.getProgrammingLanguage();
        final var jobs = ((TestResultsDTO) buildResult).getResults();

        // Extract test case feedback
        for (final var job : jobs) {
            for (final var testCase : job.getTestCases()) {
                var feedbackMessages = extractMessageFromTestCase(testCase).map(List::of).orElse(List.of());
                var feedback = feedbackRepository.createFeedbackFromTestCase(testCase.getName(), feedbackMessages, testCase.isSuccessful(), programmingLanguage);
                result.addFeedback(feedback);
            }
        }

        // Extract static code analysis feedback if option was enabled
        var staticCodeAnalysisReports = ((TestResultsDTO) buildResult).getStaticCodeAnalysisReports();
        if (Boolean.TRUE.equals(programmingExercise.isStaticCodeAnalysisEnabled()) && staticCodeAnalysisReports != null && !staticCodeAnalysisReports.isEmpty()) {
            var scaFeedback = feedbackRepository.createFeedbackFromStaticCodeAnalysisReports(staticCodeAnalysisReports);
            result.addFeedbacks(scaFeedback);
        }

        // Relevant feedback is negative, or positive with a message
        result.setHasFeedback(result.getFeedbacks().stream().anyMatch(fb -> !fb.isPositive() || fb.getDetailText() != null));
    }

    /**
     * Extracts the most helpful message from the given test case.
     * @param testCase the test case information as received from Jenkins.
     * @return the most helpful message that can be added to an automatic {@link Feedback}.
     */
    private Optional<String> extractMessageFromTestCase(final TestCaseDTO testCase) {
        var hasErrors = !CollectionUtils.isEmpty(testCase.getErrors());
        var hasFailures = !CollectionUtils.isEmpty(testCase.getFailures());
        var hasSuccessInfos = !CollectionUtils.isEmpty(testCase.getSuccessInfos());
        boolean successful = testCase.isSuccessful();

        if (successful && hasSuccessInfos && testCase.getSuccessInfos().get(0).getMostInformativeMessage() != null) {
            return Optional.of(testCase.getSuccessInfos().get(0).getMostInformativeMessage());
        }
        else if (hasErrors && testCase.getErrors().get(0).getMostInformativeMessage() != null) {
            return Optional.of(testCase.getErrors().get(0).getMostInformativeMessage());
        }
        else if (hasFailures && testCase.getFailures().get(0).getMostInformativeMessage() != null) {
            return Optional.of(testCase.getFailures().get(0).getMostInformativeMessage());
        }
        else if (hasErrors && testCase.getErrors().get(0).getType() != null) {
            return Optional.of(String.format("Unsuccessful due to an error of type: %s", testCase.getErrors().get(0).getType()));
        }
        else if (hasFailures && testCase.getFailures().get(0).getType() != null) {
            return Optional.of(String.format("Unsuccessful due to an error of type: %s", testCase.getFailures().get(0).getType()));
        }
        else if (!successful) {
            // this is an edge case which typically does not happen
            return Optional.of("Unsuccessful due to an unknown error. Please contact your instructor!");
        }
        else {
            // successful and no message available => do not generate one
            return Optional.empty();
        }
    }

    @Override
    public List<BuildLogEntry> getLatestBuildLogs(ProgrammingSubmission programmingSubmission) {
        ProgrammingExerciseParticipation programmingExerciseParticipation = (ProgrammingExerciseParticipation) programmingSubmission.getParticipation();
        String projectKey = programmingExerciseParticipation.getProgrammingExercise().getProjectKey();
        String buildPlanId = programmingExerciseParticipation.getBuildPlanId();
        ProgrammingLanguage programmingLanguage = programmingExerciseParticipation.getProgrammingExercise().getProgrammingLanguage();

        try {
            final var build = jenkinsJobService.getJobInFolder(projectKey, buildPlanId).getLastBuild();
            List<BuildLogEntry> buildLogEntries;

            // Attempt to parse pipeline logs
            final String pipelineLogs = build.details().getConsoleOutputText();
            if (pipelineLogs != null && pipelineLogs.contains("[Pipeline] Start of Pipeline")) {
                buildLogEntries = JenkinsBuildLogParseUtils.parseBuildLogsFromJenkinsLogs(List.of(pipelineLogs.split("\n")));
            }
            else {
                // Fallback to legacy logs
                final var logHtml = Jsoup.parse(build.details().getConsoleOutputHtml()).body();
                buildLogEntries = JenkinsBuildLogParseUtils.parseLogsLegacy(logHtml);
            }

            // Filter and save build logs
            buildLogEntries = filterUnnecessaryLogs(buildLogEntries, programmingLanguage);
            buildLogEntries = buildLogService.saveBuildLogs(buildLogEntries, programmingSubmission);
            programmingSubmission.setBuildLogEntries(buildLogEntries);
            programmingSubmissionRepository.save(programmingSubmission);
            return buildLogEntries;
        }
        catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new JenkinsException(e.getMessage(), e);
        }
    }

    /**
     * Removes the build logs that are not relevant to the student.
     *
     * @param buildLogEntries unfiltered build logs
     * @param programmingLanguage the programming language of the build
     * @return filtered build logs
     */
    private List<BuildLogEntry> filterUnnecessaryLogs(List<BuildLogEntry> buildLogEntries, ProgrammingLanguage programmingLanguage) {
        // There are color codes in the logs that need to be filtered out.
        // This is needed for old programming exercises
        // For example:[[1;34mINFO[m] is changed to [INFO]
        Stream<BuildLogEntry> filteredBuildLogs = buildLogEntries.stream().peek(buildLog -> {
            String log = buildLog.getLog();
            log = log.replace("\u001B[1;34m", "");
            log = log.replace("\u001B[m", "");
            log = log.replace("\u001B[1;31m", "");
            buildLog.setLog(log);
        });

        // Jenkins outputs each executed shell command with '+ <shell command>'
        filteredBuildLogs = filteredBuildLogs.filter(buildLog -> !buildLog.getLog().startsWith("+"));

        // Filter out the remainder of unnecessary logs
        return removeUnnecessaryLogsForProgrammingLanguage(filteredBuildLogs.collect(Collectors.toList()), programmingLanguage);
    }

    @Override
    public ResponseEntity<byte[]> retrieveLatestArtifact(ProgrammingExerciseParticipation participation) {
        // TODO, not necessary for the core functionality
        return null;
    }

    @Override
    public String checkIfProjectExists(String projectKey, String projectName) {
        try {
            final var job = jenkinsServer.getJob(projectKey);
            if (job == null) {
                // means the project does not exist
                return null;
            }
            else if (job.getUrl() == null || job.getUrl().isEmpty()) {
                return null;
            }
            else {
                return "The project " + projectKey + " already exists in the CI Server. Please choose a different short name!";
            }
        }
        catch (Exception emAll) {
            log.warn(emAll.getMessage());
            // in case of an error message, we assume the project exist (like in Bamboo service)
            return "The project already exists on the Continuous Integration Server. Please choose a different title and short name!";
        }
    }

    @Override
    public void enablePlan(String projectKey, String planKey) {
        jenkinsBuildPlanService.enablePlan(projectKey, planKey);
    }

    @Override
    public void giveProjectPermissions(String projectKey, List<String> groups, List<CIPermission> permissions) {
        // TODO after decision on how to handle users on Jenkins has been made
    }

    @Override
    public void removeAllDefaultProjectPermissions(String projectKey) {
        // TODO after decision on how to handle users on Jenkins has been made
    }

    @Override
    public ConnectorHealth health() {
        try {
            // Note: we simply check if the login page is reachable
            shortTimeoutRestTemplate.getForObject(serverUrl + "/login", String.class);
            return new ConnectorHealth(true, Map.of("url", serverUrl));
        }
        catch (Exception emAll) {
            var health = new ConnectorHealth(false, Map.of("url", serverUrl));
            health.setException(new JenkinsException("Jenkins Server is down!"));
            return health;
        }
    }

    @Override
    public void createProjectForExercise(ProgrammingExercise programmingExercise) throws ContinuousIntegrationException {
        try {
            jenkinsServer.createFolder(programmingExercise.getProjectKey(), useCrumb);
        }
        catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new JenkinsException("Error creating folder for exercise " + programmingExercise, e);
        }
    }

    private enum Endpoint {

        NEW_PLAN("job", "<projectKey>", "createItem"), NEW_FOLDER("createItem"), DELETE_FOLDER("job", "<projectKey>", "doDelete"),
        DELETE_JOB("job", "<projectKey>", "job", "<planName>", "doDelete"), PLAN_CONFIG("job", "<projectKey>", "job", "<planKey>", "config.xml"),
        TRIGGER_BUILD("job", "<projectKey>", "job", "<planKey>", "build"), ENABLE("job", "<projectKey>", "job", "<planKey>", "enable"),
        TEST_RESULTS("job", "<projectKey>", "job", "<planKey>", "lastBuild", "testResults", "api", "json"),
        LAST_BUILD("job", "<projectKey>", "job", "<planKey>", "lastBuild", "api", "json");

        private final List<String> pathSegments;

        Endpoint(String... pathSegments) {
            this.pathSegments = Arrays.asList(pathSegments);
        }

        public UriComponentsBuilder buildEndpoint(String baseUrl, Object... args) {
            return UrlUtils.buildEndpoint(baseUrl, pathSegments, args);
        }
    }
}
