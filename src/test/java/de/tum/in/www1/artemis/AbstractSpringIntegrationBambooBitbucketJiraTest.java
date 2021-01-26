package de.tum.in.www1.artemis;

import static de.tum.in.www1.artemis.config.Constants.*;
import static de.tum.in.www1.artemis.domain.enumeration.BuildPlanType.SOLUTION;
import static de.tum.in.www1.artemis.domain.enumeration.BuildPlanType.TEMPLATE;
import static de.tum.in.www1.artemis.util.TestConstants.COMMIT_HASH_OBJECT_ID;
import static io.github.jhipster.config.JHipsterConstants.SPRING_PROFILE_TEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import com.atlassian.bamboo.specs.util.BambooServer;
import com.fasterxml.jackson.core.JsonProcessingException;

import de.tum.in.www1.artemis.connector.bamboo.BambooRequestMockProvider;
import de.tum.in.www1.artemis.connector.bitbucket.BitbucketRequestMockProvider;
import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.Team;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.enumeration.BuildPlanType;
import de.tum.in.www1.artemis.domain.enumeration.RepositoryType;
import de.tum.in.www1.artemis.domain.participation.ProgrammingExerciseStudentParticipation;
import de.tum.in.www1.artemis.service.connectors.BitbucketBambooUpdateService;
import de.tum.in.www1.artemis.service.connectors.bamboo.BambooService;
import de.tum.in.www1.artemis.service.connectors.bamboo.dto.BambooBuildPlanDTO;
import de.tum.in.www1.artemis.service.connectors.bamboo.dto.BambooBuildResultDTO;
import de.tum.in.www1.artemis.service.connectors.bamboo.dto.BambooRepositoryDTO;
import de.tum.in.www1.artemis.service.connectors.bamboo.dto.BambooTriggerDTO;
import de.tum.in.www1.artemis.service.connectors.bitbucket.BitbucketService;
import de.tum.in.www1.artemis.service.connectors.bitbucket.dto.BitbucketRepositoryDTO;
import de.tum.in.www1.artemis.service.ldap.LdapUserService;
import de.tum.in.www1.artemis.util.AbstractArtemisIntegrationTest;

@SpringBootTest(properties = { "artemis.athene.token-validity-in-seconds=10800",
        "artemis.athene.base64-secret=YWVuaXF1YWRpNWNlaXJpNmFlbTZkb283dXphaVF1b29oM3J1MWNoYWlyNHRoZWUzb2huZ2FpM211bGVlM0VpcAo=" })
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
// NOTE: we use a common set of active profiles to reduce the number of application launches during testing. This significantly saves time and memory!
@ActiveProfiles({ SPRING_PROFILE_TEST, "artemis", "bamboo", "bitbucket", "jira", "ldap", "scheduling", "athene" })
public abstract class AbstractSpringIntegrationBambooBitbucketJiraTest extends AbstractArtemisIntegrationTest {

    @SpyBean
    protected LdapUserService ldapUserService;

    // please only use this to verify method calls using Mockito. Do not mock methods, instead mock the communication with Bamboo using the corresponding RestTemplate.
    @SpyBean
    protected BitbucketBambooUpdateService continuousIntegrationUpdateService;

    // please only use this to verify method calls using Mockito. Do not mock methods, instead mock the communication with Bamboo using the corresponding RestTemplate.
    @SpyBean
    protected BambooService continuousIntegrationService;

    // please only use this to verify method calls using Mockito. Do not mock methods, instead mock the communication with Bitbucket using the corresponding RestTemplate.
    @SpyBean
    protected BitbucketService versionControlService;

    @SpyBean
    protected BambooServer bambooServer;

    @Autowired
    protected BambooRequestMockProvider bambooRequestMockProvider;

    @Autowired
    protected BitbucketRequestMockProvider bitbucketRequestMockProvider;

    @AfterEach
    public void resetSpyBeans() {
        Mockito.reset(ldapUserService, continuousIntegrationUpdateService, continuousIntegrationService, versionControlService, bambooServer);
        super.resetSpyBeans();
    }

    @Override
    public void mockCopyRepositoryForParticipation(ProgrammingExercise exercise, String username) throws URISyntaxException, IOException {
        bitbucketRequestMockProvider.mockCopyRepositoryForParticipation(exercise, username);
    }

    @Override
    public void mockConnectorRequestsForStartParticipation(ProgrammingExercise exercise, String username, Set<User> users, boolean ltiUserExists, HttpStatus status)
            throws IOException, URISyntaxException {
        // Step 1a)
        bitbucketRequestMockProvider.mockCopyRepositoryForParticipation(exercise, username);
        // Step 1b)
        bitbucketRequestMockProvider.mockConfigureRepository(exercise, username, users, ltiUserExists);
        // Step 2a)
        bambooRequestMockProvider.mockCopyBuildPlanForParticipation(exercise, username);
        // Step 2b)
        // Note: no need to mock empty commit (Step 2c) because this is done on a git repository
        mockUpdatePlanRepositoryForParticipation(exercise, username);
        // Step 1c)
        bitbucketRequestMockProvider.mockAddWebHooks(exercise);
    }

    @Override
    public void mockConnectorRequestsForResumeParticipation(ProgrammingExercise exercise, String username, Set<User> users, boolean ltiUserExists)
            throws IOException, URISyntaxException {
        // Step 2a)
        bambooRequestMockProvider.mockCopyBuildPlanForParticipation(exercise, username);
        // Step 2b)
        mockUpdatePlanRepositoryForParticipation(exercise, username);
    }

    @Override
    public void mockUpdatePlanRepositoryForParticipation(ProgrammingExercise exercise, String username) throws IOException, URISyntaxException {
        final var projectKey = exercise.getProjectKey();
        final var bitbucketRepoName = projectKey.toLowerCase() + "-" + username;
        mockUpdatePlanRepository(exercise, username, ASSIGNMENT_REPO_NAME, bitbucketRepoName, List.of());
        bambooRequestMockProvider.mockEnablePlan(exercise.getProjectKey(), username);
    }

    @Override
    public void mockUpdatePlanRepository(ProgrammingExercise exercise, String planName, String repoNameInCI, String repoNameInVcs, List<String> triggeredBy)
            throws IOException, URISyntaxException {
        final var projectKey = exercise.getProjectKey();
        final var buildPlanKey = (projectKey + "-" + planName).toUpperCase();

        final var bambooRepositoryAssignment = new BambooRepositoryDTO(296200357L, ASSIGNMENT_REPO_NAME);
        final var bambooRepositoryTests = new BambooRepositoryDTO(296200356L, TEST_REPO_NAME);
        final var bitbucketRepository = new BitbucketRepositoryDTO("id", repoNameInVcs, projectKey, "ssh:cloneUrl");

        bambooRequestMockProvider.mockGetBuildPlanRepositoryList(buildPlanKey);

        bitbucketRequestMockProvider.mockGetBitbucketRepository(exercise, repoNameInVcs, bitbucketRepository);

        var applicationLinksToBeReturned = bambooRequestMockProvider.createApplicationLink();
        var applicationLink = applicationLinksToBeReturned.getApplicationLinks().get(0);
        bambooRequestMockProvider.mockGetApplicationLinks(applicationLinksToBeReturned);

        if (ASSIGNMENT_REPO_NAME.equals(repoNameInCI)) {
            bambooRequestMockProvider.mockUpdateRepository(buildPlanKey, bambooRepositoryAssignment, bitbucketRepository, applicationLink);
        }
        else {
            bambooRequestMockProvider.mockUpdateRepository(buildPlanKey, bambooRepositoryTests, bitbucketRepository, applicationLink);
        }

        if (!triggeredBy.isEmpty()) {
            // in case there are triggers
            List<BambooTriggerDTO> triggerList = bambooRequestMockProvider.mockGetTriggerList(buildPlanKey);

            for (var trigger : triggerList) {
                bambooRequestMockProvider.mockDeleteTrigger(buildPlanKey, trigger.getId());
            }

            for (var ignored : triggeredBy) {
                // we only support one specific case for the repository above here
                bambooRequestMockProvider.mockAddTrigger(buildPlanKey, bambooRepositoryAssignment.getId().toString());
            }
        }
    }

    @Override
    public void mockConnectorRequestsForSetup(ProgrammingExercise exercise) throws Exception {
        final var exerciseRepoName = exercise.generateRepositoryName(RepositoryType.TEMPLATE);
        final var solutionRepoName = exercise.generateRepositoryName(RepositoryType.SOLUTION);
        final var testRepoName = exercise.generateRepositoryName(RepositoryType.TESTS);
        bambooRequestMockProvider.mockCheckIfProjectExists(exercise, false);
        bitbucketRequestMockProvider.mockCheckIfProjectExists(exercise, false);
        bitbucketRequestMockProvider.mockCreateProjectForExercise(exercise);
        bitbucketRequestMockProvider.mockCreateRepository(exercise, exerciseRepoName);
        bitbucketRequestMockProvider.mockCreateRepository(exercise, testRepoName);
        bitbucketRequestMockProvider.mockCreateRepository(exercise, solutionRepoName);
        bitbucketRequestMockProvider.mockAddWebHooks(exercise);
        mockBambooBuildPlanCreation(exercise);

        doNothing().when(gitService).pushSourceToTargetRepo(any(), any());
    }

    private void mockBambooBuildPlanCreation(ProgrammingExercise exercise) throws IOException, URISyntaxException {
        // TODO: check the actual plan and plan permissions that get passed here
        doReturn(null).when(bambooServer).publish(any());
        bambooRequestMockProvider.mockRemoveAllDefaultProjectPermissions(exercise);
        bambooRequestMockProvider.mockGiveProjectPermissions(exercise);
    }

    @Override
    public void mockConnectorRequestsForImport(ProgrammingExercise sourceExercise, ProgrammingExercise exerciseToBeImported, boolean recreateBuildPlans)
            throws IOException, URISyntaxException, GitAPIException {
        final var projectKey = exerciseToBeImported.getProjectKey();
        final var templateRepoName = exerciseToBeImported.generateRepositoryName(RepositoryType.TEMPLATE);
        final var solutionRepoName = exerciseToBeImported.generateRepositoryName(RepositoryType.SOLUTION);
        final var testsRepoName = exerciseToBeImported.generateRepositoryName(RepositoryType.TESTS);
        var nextParticipationId = sourceExercise.getTemplateParticipation().getId() + 1;
        final var artemisSolutionHookPath = artemisServerUrl + PROGRAMMING_SUBMISSION_RESOURCE_API_PATH + nextParticipationId++;
        final var artemisTemplateHookPath = artemisServerUrl + PROGRAMMING_SUBMISSION_RESOURCE_API_PATH + nextParticipationId;
        final var artemisTestsHookPath = artemisServerUrl + TEST_CASE_CHANGED_API_PATH + (sourceExercise.getId() + 1);

        bitbucketRequestMockProvider.mockCheckIfProjectExists(exerciseToBeImported, false);
        bitbucketRequestMockProvider.mockCreateProjectForExercise(exerciseToBeImported);
        bitbucketRequestMockProvider.mockCreateRepository(exerciseToBeImported, templateRepoName);
        bitbucketRequestMockProvider.mockCreateRepository(exerciseToBeImported, solutionRepoName);
        bitbucketRequestMockProvider.mockCreateRepository(exerciseToBeImported, testsRepoName);
        bitbucketRequestMockProvider.mockGetExistingWebhooks(projectKey, templateRepoName);
        bitbucketRequestMockProvider.mockAddWebhook(projectKey, templateRepoName, artemisTemplateHookPath);
        bitbucketRequestMockProvider.mockGetExistingWebhooks(projectKey, solutionRepoName);
        bitbucketRequestMockProvider.mockAddWebhook(projectKey, solutionRepoName, artemisSolutionHookPath);
        bitbucketRequestMockProvider.mockGetExistingWebhooks(projectKey, testsRepoName);
        bitbucketRequestMockProvider.mockAddWebhook(projectKey, testsRepoName, artemisTestsHookPath);

        doNothing().when(gitService).pushSourceToTargetRepo(any(), any());

        bambooRequestMockProvider.mockCheckIfProjectExists(exerciseToBeImported, false);
        if (!recreateBuildPlans) {
            // Mocks for copying the build plans
            bambooRequestMockProvider.mockCopyBuildPlan(sourceExercise.getProjectKey(), TEMPLATE.getName(), projectKey, TEMPLATE.getName(), false);
            bambooRequestMockProvider.mockCopyBuildPlan(sourceExercise.getProjectKey(), SOLUTION.getName(), projectKey, SOLUTION.getName(), true);
            // TODO: Mock continuousIntegrationService.givePlanPermissions for Template and Solution plan
            doReturn(null).when(bambooServer).publish(any());
            bambooRequestMockProvider.mockGiveProjectPermissions(exerciseToBeImported);
            bambooRequestMockProvider.mockEnablePlan(projectKey, TEMPLATE.getName());
            bambooRequestMockProvider.mockEnablePlan(projectKey, SOLUTION.getName());
            mockUpdatePlanRepository(exerciseToBeImported, TEMPLATE.getName(), ASSIGNMENT_REPO_NAME, templateRepoName, List.of(ASSIGNMENT_REPO_NAME));
            mockUpdatePlanRepository(exerciseToBeImported, TEMPLATE.getName(), TEST_REPO_NAME, testsRepoName, List.of());
            mockUpdatePlanRepository(exerciseToBeImported, SOLUTION.getName(), ASSIGNMENT_REPO_NAME, solutionRepoName, List.of());
            mockUpdatePlanRepository(exerciseToBeImported, SOLUTION.getName(), TEST_REPO_NAME, testsRepoName, List.of());
            bambooRequestMockProvider.mockTriggerBuild(exerciseToBeImported.getProjectKey() + "-" + TEMPLATE.getName());
            bambooRequestMockProvider.mockTriggerBuild(exerciseToBeImported.getProjectKey() + "-" + SOLUTION.getName());
        }
        else {
            // Mocks for recreating the build plans
            mockBambooBuildPlanCreation(exerciseToBeImported);
        }
    }

    @Override
    public void mockRemoveRepositoryAccess(ProgrammingExercise exercise, Team team, User firstStudent) throws URISyntaxException {
        final var repositorySlug = (exercise.getProjectKey() + "-" + team.getParticipantIdentifier()).toLowerCase();
        bitbucketRequestMockProvider.mockRemoveMemberFromRepository(repositorySlug, exercise.getProjectKey(), firstStudent);
    }

    @Override
    public void mockRepositoryWritePermissions(Team team, User newStudent, ProgrammingExercise exercise, HttpStatus status) throws URISyntaxException {
        final var repositorySlug = (exercise.getProjectKey() + "-" + team.getParticipantIdentifier()).toLowerCase();
        bitbucketRequestMockProvider.mockGiveWritePermission(exercise, repositorySlug, newStudent.getLogin(), status);
    }

    @Override
    public void mockRetrieveArtifacts(ProgrammingExerciseStudentParticipation participation) throws MalformedURLException, URISyntaxException, JsonProcessingException {
        // prepare the build result
        bambooRequestMockProvider.mockQueryLatestBuildResultFromBambooServer(participation.getBuildPlanId());
        // prepare the artifact to be null
        bambooRequestMockProvider.mockRetrieveEmptyArtifactPage();
    }

    @Override
    public void mockGetBuildLogs(ProgrammingExerciseStudentParticipation participation, List<BambooBuildResultDTO.BambooBuildLogEntryDTO> logs)
            throws URISyntaxException, JsonProcessingException {
        bambooRequestMockProvider.mockGetBuildLogs(participation.getBuildPlanId(), logs);
    }

    @Override
    public void mockFetchCommitInfo(String projectKey, String repositorySlug, String hash) throws URISyntaxException, JsonProcessingException {
        bitbucketRequestMockProvider.mockFetchCommitInfo(projectKey, repositorySlug, hash);
    }

    @Override
    public void mockCopyBuildPlan(ProgrammingExerciseStudentParticipation participation) throws Exception {
        final var projectKey = participation.getProgrammingExercise().getProjectKey();
        final var planName = BuildPlanType.TEMPLATE.getName();
        final var username = participation.getParticipantIdentifier();
        bambooRequestMockProvider.mockCopyBuildPlan(projectKey, planName, projectKey, username.toUpperCase(), true);
    }

    @Override
    public void mockConfigureBuildPlan(ProgrammingExerciseStudentParticipation participation) throws Exception {
        final var buildPlanId = participation.getBuildPlanId();
        final var repositoryUrl = participation.getVcsRepositoryUrl();
        final var projectKey = buildPlanId.split("-")[0];
        final var planKey = participation.getBuildPlanId();
        final var repoProjectName = urlService.getProjectKeyFromRepositoryUrl(repositoryUrl);
        bambooRequestMockProvider.mockUpdatePlanRepository(projectKey, planKey, ASSIGNMENT_REPO_NAME, repoProjectName, participation.getRepositoryUrl(), null /* not needed */,
                Optional.empty());

        // Isn't mockEnablePlan() written incorrectly since projectKey isn't even used by the bamboo service?
        var splitted = buildPlanId.split("-");
        bambooRequestMockProvider.mockEnablePlan(splitted[0], splitted[1]);
    }

    @Override
    public void mockTriggerFailedBuild(ProgrammingExerciseStudentParticipation participation) throws Exception {
        doReturn(COMMIT_HASH_OBJECT_ID).when(gitService).getLastCommitHash(any());
        String buildPlanId = participation.getBuildPlanId();
        bambooRequestMockProvider.mockGetBuildPlan(buildPlanId, buildPlanId != null ? new BambooBuildPlanDTO() : null);
        mockCopyBuildPlan(participation);
        mockConfigureBuildPlan(participation);
        bambooRequestMockProvider.mockTriggerBuild(participation);
    }

    @Override
    public void mockNotifyPush(ProgrammingExerciseStudentParticipation participation) throws Exception {
        final String slug = "test201904bprogrammingexercise6-exercise-testuser";
        final String hash = "9b3a9bd71a0d80e5bbc42204c319ed3d1d4f0d6d";
        mockFetchCommitInfo(participation.getProgrammingExercise().getProjectKey(), slug, hash);
        bambooRequestMockProvider.mockTriggerBuild(participation);
    }

    @Override
    public void mockTriggerParticipationBuild(ProgrammingExerciseStudentParticipation participation) throws Exception {
        doReturn(COMMIT_HASH_OBJECT_ID).when(gitService).getLastCommitHash(any());
        mockCopyBuildPlan(participation);
        mockConfigureBuildPlan(participation);
        bambooRequestMockProvider.mockTriggerBuild(participation);
    }

    @Override
    public void mockTriggerInstructorBuildAll(ProgrammingExerciseStudentParticipation participation) throws Exception {
        doReturn(COMMIT_HASH_OBJECT_ID).when(gitService).getLastCommitHash(any());
        mockCopyBuildPlan(participation);
        mockConfigureBuildPlan(participation);
        bambooRequestMockProvider.mockTriggerBuild(participation);
    }

    @Override
    public void resetMockProvider() {
        bitbucketRequestMockProvider.reset();
        bambooRequestMockProvider.reset();
    }
}
