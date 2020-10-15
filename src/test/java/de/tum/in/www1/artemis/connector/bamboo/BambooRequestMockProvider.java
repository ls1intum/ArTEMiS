package de.tum.in.www1.artemis.connector.bamboo;

import static de.tum.in.www1.artemis.util.FileUtils.loadFileFromResources;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.enumeration.BuildPlanType;
import de.tum.in.www1.artemis.domain.participation.ProgrammingExerciseParticipation;
import de.tum.in.www1.artemis.service.connectors.bamboo.dto.*;
import de.tum.in.www1.artemis.service.connectors.bitbucket.dto.BitbucketRepositoryDTO;
import de.tum.in.www1.artemis.util.TestConstants;

@Component
@Profile("bamboo")
public class BambooRequestMockProvider {

    @Value("${artemis.continuous-integration.url}")
    private URL bambooServerUrl;

    @Value("${artemis.version-control.url}")
    private URL bitbucketServerUrl;

    @Autowired
    private ObjectMapper mapper;

    private final RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    public BambooRequestMockProvider(@Qualifier("bambooRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void enableMockingOfRequests() {
        enableMockingOfRequests(false);
    }

    public void enableMockingOfRequests(boolean ignoreExpectOrder) {
        MockRestServiceServer.MockRestServiceServerBuilder builder = MockRestServiceServer.bindTo(restTemplate);
        builder.ignoreExpectOrder(ignoreExpectOrder);
        mockServer = builder.build();
    }

    public void reset() {
        if (mockServer != null) {
            mockServer.reset();
        }
    }

    /**
     * This method mocks that the programming exercise with the same project key (based on the course + programming exercise short name) already exists
     *
     * @param exercise the programming exercise that already exists
     */
    public void mockProjectKeyExists(ProgrammingExercise exercise) {
        mockServer.expect(requestTo(bambooServerUrl + "/rest/api/latest/project/" + exercise.getProjectKey())).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));
    }

    /**
     * This method mocks that the programming exercise with the same project name already exists (depending on the boolean input exists), based on the programming exercise title
     *
     * @param exercise the programming exercise that might already exist
     * @param exists   whether the programming exercise with the same title exists
     * @throws IOException an IO exception when reading test files
     * @throws URISyntaxException exceptions related to URI handling in test REST calls
     */
    public void mockCheckIfProjectExists(ProgrammingExercise exercise, final boolean exists) throws IOException, URISyntaxException {
        final var projectKey = exercise.getProjectKey();
        final var projectName = exercise.getProjectName();
        final var bambooSearchDTO = new BambooProjectsSearchDTO();
        final var searchResult = new BambooProjectsSearchDTO.SearchResultDTO();
        final var foundProject = new BambooProjectSearchDTO();
        foundProject.setProjectName(exercise.getProjectName() + (exists ? "" : "abc"));
        searchResult.setSearchEntity(foundProject);
        bambooSearchDTO.setSize(1);
        bambooSearchDTO.setSearchResults(List.of(searchResult));

        mockServer.expect(requestTo(bambooServerUrl + "/rest/api/latest/project/" + projectKey)).andExpect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        final var projectSearchPath = UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/rest/api/latest/search/projects").queryParam("searchTerm", projectName);
        mockServer.expect(requestTo(projectSearchPath.build().toUri())).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK).body(mapper.writeValueAsString(bambooSearchDTO)).contentType(MediaType.APPLICATION_JSON));
    }

    public void mockRemoveAllDefaultProjectPermissions(ProgrammingExercise exercise) {
        final var projectKey = exercise.getProjectKey();
        List.of("ANONYMOUS", "LOGGED_IN").stream().map(role -> {
            try {
                return UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/rest/api/latest/permissions/project/").pathSegment(projectKey).path("/roles/").pathSegment(role)
                        .build().toUri();
            }
            catch (URISyntaxException e) {
                throw new AssertionError("Should be able to build URIs for Bamboo roles in mock setup");
            }
        }).forEach(rolePath -> mockServer.expect(requestTo(rolePath)).andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT)));
    }

    public void mockGiveProjectPermissions(ProgrammingExercise exercise) throws URISyntaxException, IOException {
        final var projectKey = exercise.getProjectKey();

        final var instructorURI = buildGivePermissionsURIFor(projectKey, exercise.getCourseViaExerciseGroupOrCourseMember().getInstructorGroupName());
        mockServer.expect(requestTo(instructorURI)).andExpect(method(HttpMethod.PUT))
                .andExpect(content().json(mapper.writeValueAsString(List.of("CREATE", "READ", "ADMINISTRATION")))).andRespond(withStatus(HttpStatus.NO_CONTENT));

        if (exercise.getCourseViaExerciseGroupOrCourseMember().getTeachingAssistantGroupName() != null) {
            final var tutorURI = buildGivePermissionsURIFor(projectKey, exercise.getCourseViaExerciseGroupOrCourseMember().getTeachingAssistantGroupName());
            mockServer.expect(requestTo(tutorURI)).andExpect(method(HttpMethod.PUT)).andExpect(content().json(mapper.writeValueAsString(List.of("READ"))))
                    .andRespond(withStatus(HttpStatus.NO_CONTENT));
        }
    }

    private URI buildGivePermissionsURIFor(String projectKey, String groupName) throws URISyntaxException {
        return UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/rest/api/latest/permissions/project/").pathSegment(projectKey).path("/groups/").pathSegment(groupName)
                .build().toUri();
    }

    public void mockCopyBuildPlanForParticipation(ProgrammingExercise exercise, String username) throws URISyntaxException, JsonProcessingException {
        final var projectKey = exercise.getProjectKey();
        final var targetPlanName = username.toUpperCase();
        mockCopyBuildPlan(projectKey, BuildPlanType.TEMPLATE.getName(), projectKey, targetPlanName, true);
    }

    public void mockBuildPlanExists(final String buildPlanId, final boolean exists) throws URISyntaxException, JsonProcessingException {
        if (exists) {
            mockGetBuildPlan(buildPlanId, new BambooBuildPlanDTO(buildPlanId));
        }
        else {
            mockGetBuildPlan(buildPlanId, null);
        }
    }

    public void mockGetBuildPlan(String buildPlanId, BambooBuildPlanDTO buildPlanToBeReturned) throws URISyntaxException, JsonProcessingException {
        final var uri = UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/rest/api/latest/plan/").pathSegment(buildPlanId).build().toUri();
        if (buildPlanToBeReturned != null) {
            mockServer.expect(requestTo(uri)).andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(mapper.writeValueAsString(buildPlanToBeReturned)));
        }
        else {
            mockServer.expect(requestTo(uri)).andExpect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        }
    }

    public void mockAddTrigger(String buildPlanKey, String repository) throws URISyntaxException, IOException {
        mockGetBuildPlanRepositoryList(buildPlanKey);

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        // we only support one very specific case here
        parameters.add("repositoryTrigger", repository);
        parameters.add("planKey", buildPlanKey);
        parameters.add("triggerId", "-1");
        parameters.add("createTriggerKey", "com.atlassian.bamboo.plugins.stash.atlassian-bamboo-plugin-stash:stashTrigger");
        parameters.add("userDescription", null);
        parameters.add("confirm", "true");
        parameters.add("bamboo.successReturnMode", "json");
        parameters.add("decorator", "nothing");
        URI uri = UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/chain/admin/config/createChainTrigger.action").queryParams(parameters).build().toUri();
        mockServer.expect(requestTo(uri)).andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
    }

    public void mockDeleteTrigger(String buildPlanKey, Long id) throws URISyntaxException {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("triggerId", Long.toString(id));
        parameters.add("confirm", "true");
        parameters.add("decorator", "nothing");
        parameters.add("bamboo.successReturnMode", "json");
        parameters.add("planKey", buildPlanKey);
        URI uri = UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/chain/admin/config/deleteChainTrigger.action").queryParams(parameters).build().toUri();
        mockServer.expect(requestTo(uri)).andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
    }

    @NotNull
    public List<BambooTriggerDTO> mockGetTriggerList(String buildPlanKey) throws IOException, URISyntaxException {
        var triggerList = List.of(new BambooTriggerDTO(1L, "foo", "artemis"));

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("buildKey", buildPlanKey);
        final var triggerListHtmlResponse = loadFileFromResources("test-data/bamboo-response/build-plan-trigger-list-response.html");
        URI uri = UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/chain/admin/config/editChainTriggers.action").queryParams(parameters).build().toUri();
        mockServer.expect(requestTo(uri)).andExpect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.OK).contentType(MediaType.TEXT_HTML).body(triggerListHtmlResponse));
        return triggerList;
    }

    public void mockUpdateRepository(String buildPlanKey, BambooRepositoryDTO bambooRepository, BitbucketRepositoryDTO bitbucketRepository,
            ApplicationLinksDTO.ApplicationLinkDTO applicationLink) throws URISyntaxException {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("planKey", buildPlanKey);
        parameters.add("selectedRepository", "com.atlassian.bamboo.plugins.stash.atlassian-bamboo-plugin-stash:stash-rep");
        parameters.add("repositoryName", bambooRepository.getName());
        parameters.add("repositoryId", Long.toString(bambooRepository.getId()));
        parameters.add("confirm", "true");
        parameters.add("save", "Save repository");
        parameters.add("bamboo.successReturnMode", "json");
        parameters.add("repository.stash.branch", "master");
        parameters.add("repository.stash.repositoryId", bitbucketRepository.getId());
        parameters.add("repository.stash.repositorySlug", bitbucketRepository.getSlug());
        parameters.add("repository.stash.projectKey", bitbucketRepository.getProject().getKey());
        parameters.add("repository.stash.repositoryUrl", bitbucketRepository.getCloneSshUrl());
        parameters.add("repository.stash.server", applicationLink.getId());

        URI uri = UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/chain/admin/config/updateRepository.action").queryParams(parameters).build().toUri();
        mockServer.expect(requestTo(uri)).andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
    }

    public ApplicationLinksDTO createApplicationLink() {
        final var applicationLinks = new ApplicationLinksDTO();
        final var applicationLink = new ApplicationLinksDTO.ApplicationLinkDTO();
        applicationLink.setRpcUrl(bitbucketServerUrl.toString());
        applicationLink.setId("123b1230-e123-3123-9123-9123e2123123");
        applicationLinks.setApplicationLinks(List.of(applicationLink));
        return applicationLinks;
    }

    public void mockGetApplicationLinks(ApplicationLinksDTO applicationLinksToBeReturned) throws URISyntaxException, JsonProcessingException {
        URI uri = UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/rest/applinks/latest/applicationlink").queryParam("expand", "").build().toUri();
        mockServer.expect(requestTo(uri)).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(mapper.writeValueAsString(applicationLinksToBeReturned)));
    }

    public void mockGetBuildPlanRepositoryList(String buildPlanKey) throws IOException, URISyntaxException {
        final var repositoryListHtmlResponse = loadFileFromResources("test-data/bamboo-response/build-plan-repository-list-response.html");
        var uri = UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/chain/admin/config/editChainRepository.action").queryParam("buildKey", buildPlanKey).build().toUri();
        mockServer.expect(requestTo(uri)).andExpect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.OK).contentType(MediaType.TEXT_HTML).body(repositoryListHtmlResponse));
    }

    public void mockTriggerBuild(ProgrammingExerciseParticipation participation) throws URISyntaxException {
        final var buildPlan = participation.getBuildPlanId();
        mockTriggerBuild(buildPlan);
    }

    public void mockTriggerBuild(String buildPlan) throws URISyntaxException {
        final var triggerBuildPath = UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/rest/api/latest/queue/").pathSegment(buildPlan).build().toUri();
        mockServer.expect(requestTo(triggerBuildPath)).andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
    }

    public void mockQueryLatestBuildResultFromBambooServer(String planKey) throws URISyntaxException, JsonProcessingException, MalformedURLException {
        final var response = createBuildResult(planKey);
        final var uri = UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/rest/api/latest/result").pathSegment(planKey.toUpperCase() + "-JOB1")
                .pathSegment("latest.json").queryParam("expand", "testResults.failedTests.testResult.errors,artifacts,changes,vcsRevisions").build().toUri();
        mockServer.expect(requestTo(uri)).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(mapper.writeValueAsString(response)));
    }

    /**
     * This method mocks that the artifact page the latest build result is empty
     */
    public void mockRetrieveEmptyArtifactPage() throws URISyntaxException, MalformedURLException {
        var indexOfResponse = "href=\"/download/1\"";
        var noArtifactsResponse = "";
        final var uri = new URL(bambooServerUrl + "/download/").toURI();
        final var uri2 = new URL(bambooServerUrl + "/download/1").toURI();

        mockServer.expect(requestTo(uri)).andExpect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.OK).contentType(MediaType.TEXT_HTML).body(indexOfResponse));
        mockServer.expect(requestTo(uri2)).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(noArtifactsResponse));
    }

    /**
     * This method mocks that the build log of a given plan has failed
     *
     * @param planKey the build plan id
     */
    public void mockGetBuildLogs(String planKey, List<BambooBuildResultDTO.BambooBuildLogEntryDTO> buildLogs) throws URISyntaxException, JsonProcessingException {
        var response = new BambooBuildResultDTO(new BambooBuildResultDTO.BambooBuildLogEntriesDTO(buildLogs));
        final var uri = UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/rest/api/latest/result").pathSegment(planKey.toUpperCase() + "-JOB1")
                .pathSegment("latest.json").queryParam("expand", "logEntries&max-results=2000").build().toUri();

        mockServer.expect(requestTo(uri)).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(mapper.writeValueAsString(response)));
    }

    private QueriedBambooBuildResultDTO createBuildResult(final String planKey) throws MalformedURLException {
        final var buildResult = new QueriedBambooBuildResultDTO();
        final var testResults = new QueriedBambooBuildResultDTO.BambooTestResultsDTO();
        final var failedTests = new QueriedBambooBuildResultDTO.BambooFailedTestsDTO();
        final var changes = new BambooChangesDTO();
        final var artifacts = new BambooArtifactsDTO();
        final var buildArtifact1 = new BambooArtifactsDTO.BambooArtifactDTO();
        final var buildArtifact2 = new BambooArtifactsDTO.BambooArtifactDTO();
        final var buildLink = new BambooArtifactsDTO.BambooArtifactLinkDTO();

        failedTests.setExpand("testResult");
        failedTests.setSize(3);
        failedTests.setTestResults(List.of(createFailedTest("test1"), createFailedTest("test2"), createFailedTest("test3")));

        testResults.setAll(3);
        testResults.setExistingFailed(0);
        testResults.setFailed(3);
        testResults.setFixed(0);
        testResults.setNewFailed(0);
        testResults.setQuarantined(0);
        testResults.setSkipped(0);
        testResults.setSuccessful(0);
        testResults.setFailedTests(failedTests);

        buildResult.setBuildCompletedDate(ZonedDateTime.now().minusMinutes(1));
        buildResult.setBuildReason("Initial clean build");
        buildResult.setBuildState(QueriedBambooBuildResultDTO.BuildState.FAILED);
        buildResult.setBuildTestSummary("0 of 3 passed");
        buildResult.setVcsRevisionKey(TestConstants.COMMIT_HASH_STRING);
        buildResult.setTestResults(testResults);

        changes.setSize(0);
        changes.setExpand("change");
        changes.setChanges(new LinkedList<>());
        buildResult.setChanges(changes);

        buildLink.setLinkToArtifact(new URL(bambooServerUrl + "/download/"));
        buildLink.setRel("self");
        buildArtifact1.setLink(buildLink);
        buildArtifact1.setName("Build log");
        buildArtifact1.setProducerJobKey(planKey + "-JOB-1");
        buildArtifact1.setShared(false);
        buildArtifact2.setLink(buildLink);
        buildArtifact2.setName("Mock Build log");
        buildArtifact2.setProducerJobKey(planKey + "-JOB-1");
        buildArtifact2.setShared(false);
        artifacts.setArtifacts(List.of(buildArtifact1, buildArtifact2));
        buildResult.setArtifacts(artifacts);

        return buildResult;
    }

    private BambooTestResultDTO createFailedTest(final String testName) {
        final var failed = new BambooTestResultDTO();
        final var resultError = new BambooTestResultDTO.BambooTestResultErrorsDTO();
        final var error = new BambooTestResultDTO.BambooTestErrorDTO();

        error.setMessage("java.lang.AssertionError: Some assertion failed");

        resultError.setMaxResult(1);
        resultError.setSize(1);
        resultError.setErrorMessages(List.of(error));

        failed.setClassName("some.package.ClassName");
        failed.setMethodName(testName);
        failed.setDuration(2);
        failed.setDurationInSeconds(120);
        failed.setStatus("failed");
        failed.setErrors(resultError);

        return failed;
    }

    /**
     * configures mock REST request for copying a build plan
     * @param sourceProjectKey the source Bamboo project key
     * @param sourcePlanName the source Bamboo build plan name
     * @param targetProjectKey the target Bamboo project key
     * @param targetPlanName the target Bamboo build plan name
     * @param targetProjectExists whether the target project already exists
     * @throws URISyntaxException can happen due to wrong URL handling
     * @throws JsonProcessingException can happen due to wrong response handling
     */
    public void mockCopyBuildPlan(String sourceProjectKey, String sourcePlanName, String targetProjectKey, String targetPlanName, boolean targetProjectExists)
            throws URISyntaxException, JsonProcessingException {

        mockGetBuildPlan(targetProjectKey + "-" + targetPlanName, null);

        final var sourcePlanKey = sourceProjectKey + "-" + sourcePlanName;
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        if (targetProjectExists) {
            parameters.add("existingProjectKey", targetProjectKey);
        }
        else {
            parameters.add("existingProjectKey", "newProject");
            parameters.add("projectKey", targetProjectKey);
            parameters.add("projectName", targetProjectKey);
        }

        parameters.add("planKeyToClone", sourcePlanKey);
        parameters.add("chainName", targetPlanName);
        parameters.add("chainKey", targetPlanName);
        parameters.add("chainDescription", "Build plan for exercise " + sourceProjectKey);
        parameters.add("clonePlan", "true");
        parameters.add("tmp.createAsEnabled", "false");
        parameters.add("chainEnabled", "false");
        parameters.add("save", "Create");
        parameters.add("bamboo.successReturnMode", "json");
        var uri = UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/build/admin/create/performClonePlan.action").queryParams(parameters).build().toUri();
        mockServer.expect(requestTo(uri)).andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
    }

    public void mockEnablePlan(String projectKey, String planName) throws URISyntaxException {
        final var planKey = projectKey + "-" + planName;
        var uri = UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/rest/api/latest/plan/" + planKey.toUpperCase() + "/enable").build().toUri();
        mockServer.expect(requestTo(uri)).andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
    }

    public void mockDeleteBambooBuildProject(String projectKey) throws URISyntaxException, JsonProcessingException {

        var projectResponse = new BambooProjectDTO(projectKey, projectKey, projectKey);
        projectResponse.setPlans(new BambooProjectDTO.BambooBuildPlansDTO(List.of()));

        // 1) get all plans
        var uri = UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/rest/api/latest/project/" + projectKey).queryParam("expand", "plans").queryParam("max-results", 5000)
                .build().toUri();
        mockServer.expect(requestTo(uri)).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(mapper.writeValueAsString(projectResponse)));

        // 2) in a normal scenario this list is empty, so we only expect another call to delete the project
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("selectedProjects", projectKey);
        parameters.add("confirm", "true");
        parameters.add("bamboo.successReturnMode", "json");

        uri = UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/admin/deleteBuilds.action").queryParams(parameters).build().toUri();
        mockServer.expect(requestTo(uri)).andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
    }

    public void mockDeleteBambooBuildPlan(String planKey) throws URISyntaxException, JsonProcessingException {

        mockGetBuildPlan(planKey, null);

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("selectedBuilds", planKey);
        parameters.add("confirm", "true");
        parameters.add("bamboo.successReturnMode", "json");

        var uri = UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/admin/deleteBuilds.action").queryParams(parameters).build().toUri();
        mockServer.expect(requestTo(uri)).andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.OK));
    }

    public void mockHealth(String state, HttpStatus httpStatus) throws URISyntaxException, JsonProcessingException {
        var response = Map.of("state", state);
        var uri = UriComponentsBuilder.fromUri(bambooServerUrl.toURI()).path("/rest/api/latest/server").build().toUri();
        mockServer.expect(requestTo(uri)).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(httpStatus).contentType(MediaType.APPLICATION_JSON).body(mapper.writeValueAsString(response)));
    }
}
