package de.tum.in.www1.artemis;

import static de.tum.in.www1.artemis.config.Constants.ASSIGNMENT_REPO_NAME;
import static de.tum.in.www1.artemis.config.Constants.TEST_REPO_NAME;
import static de.tum.in.www1.artemis.domain.enumeration.BuildPlanType.SOLUTION;
import static de.tum.in.www1.artemis.domain.enumeration.BuildPlanType.TEMPLATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.offbytwo.jenkins.JenkinsServer;

import de.tum.in.www1.artemis.connector.gitlab.GitlabRequestMockProvider;
import de.tum.in.www1.artemis.connector.jenkins.JenkinsRequestMockProvider;
import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.Team;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.enumeration.RepositoryType;
import de.tum.in.www1.artemis.domain.participation.ProgrammingExerciseStudentParticipation;
import de.tum.in.www1.artemis.service.connectors.bamboo.dto.BambooBuildResultDTO;
import de.tum.in.www1.artemis.service.connectors.gitlab.GitLabService;
import de.tum.in.www1.artemis.service.connectors.jenkins.JenkinsService;
import de.tum.in.www1.artemis.util.AbstractArtemisIntegrationTest;

@SpringBootTest(properties = { "artemis.athene.token-validity-in-seconds=10800",
        "artemis.athene.base64-secret=YWVuaXF1YWRpNWNlaXJpNmFlbTZkb283dXphaVF1b29oM3J1MWNoYWlyNHRoZWUzb2huZ2FpM211bGVlM0VpcAo=" })
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
// NOTE: we use a common set of active profiles to reduce the number of application launches during testing. This significantly saves time and memory!

@ActiveProfiles({ "artemis", "gitlab", "jenkins", "athene" })
@TestPropertySource(properties = { "info.guided-tour.course-group-tutors=", "info.guided-tour.course-group-students=artemis-artemistutorial-students",
        "info.guided-tour.course-group-instructors=artemis-artemistutorial-instructors", "artemis.user-management.use-external=false" })

public abstract class AbstractSpringIntegrationJenkinsGitlabTest extends AbstractArtemisIntegrationTest {

    // please only use this to verify method calls using Mockito. Do not mock methods, instead mock the communication with Jenkins using the corresponding RestTemplate.
    @SpyBean
    protected JenkinsService continuousIntegrationService;

    // please only use this to verify method calls using Mockito. Do not mock methods, instead mock the communication with Gitlab using the corresponding RestTemplate and
    // GitlabApi.
    @SpyBean
    protected GitLabService versionControlService;

    @SpyBean
    protected JenkinsServer jenkinsServer;

    @Autowired
    protected JenkinsRequestMockProvider jenkinsRequestMockProvider;

    @Autowired
    protected GitlabRequestMockProvider gitlabRequestMockProvider;

    @AfterEach
    public void resetSpyBeans() {
        Mockito.reset(continuousIntegrationService, versionControlService, jenkinsServer);
        super.resetSpyBeans();
    }

    @Override
    public void mockConnectorRequestsForSetup(ProgrammingExercise exercise) throws Exception {
        final var projectKey = exercise.getProjectKey();
        final var exerciseRepoName = exercise.generateRepositoryName(RepositoryType.TEMPLATE);
        final var solutionRepoName = exercise.generateRepositoryName(RepositoryType.SOLUTION);
        final var testRepoName = exercise.generateRepositoryName(RepositoryType.TESTS);
        gitlabRequestMockProvider.mockCheckIfProjectExists(exercise, false);
        gitlabRequestMockProvider.mockCreateProjectForExercise(exercise);
        gitlabRequestMockProvider.mockCreateRepository(exercise, exerciseRepoName);
        gitlabRequestMockProvider.mockCreateRepository(exercise, testRepoName);
        gitlabRequestMockProvider.mockCreateRepository(exercise, solutionRepoName);
        gitlabRequestMockProvider.mockAddAuthenticatedWebHook();
        jenkinsRequestMockProvider.mockCreateProjectForExercise(exercise);
        jenkinsRequestMockProvider.mockCreateBuildPlan(projectKey);
        jenkinsRequestMockProvider.mockTriggerBuild();
    }

    @Override
    public void mockConnectorRequestsForImport(ProgrammingExercise sourceExercise, ProgrammingExercise exerciseToBeImported, boolean recreateBuildPlans) throws Exception {

        final var sourceProjectKey = sourceExercise.getProjectKey();
        final var targetProjectKey = exerciseToBeImported.getProjectKey();

        final var sourceTemplateRepoName = sourceExercise.generateRepositoryName(RepositoryType.TEMPLATE);
        final var sourceSolutionRepoName = sourceExercise.generateRepositoryName(RepositoryType.SOLUTION);
        final var sourceTestsRepoName = sourceExercise.generateRepositoryName(RepositoryType.TESTS);

        final var targetTemplateRepoName = exerciseToBeImported.generateRepositoryName(RepositoryType.TEMPLATE);
        final var targetSolutionRepoName = exerciseToBeImported.generateRepositoryName(RepositoryType.SOLUTION);
        final var targetTestsRepoName = exerciseToBeImported.generateRepositoryName(RepositoryType.TESTS);

        gitlabRequestMockProvider.mockCheckIfProjectExists(exerciseToBeImported, false);
        jenkinsRequestMockProvider.mockCheckIfProjectExists(exerciseToBeImported, false);

        // Mock ProgramingExerciseImportService::importRepositories
        gitlabRequestMockProvider.mockCreateProjectForExercise(exerciseToBeImported);
        gitlabRequestMockProvider.mockForkRepository(sourceProjectKey, targetProjectKey, sourceTemplateRepoName, targetTemplateRepoName, HttpStatus.CREATED);
        gitlabRequestMockProvider.mockForkRepository(sourceProjectKey, targetProjectKey, sourceSolutionRepoName, targetSolutionRepoName, HttpStatus.CREATED);
        gitlabRequestMockProvider.mockForkRepository(sourceProjectKey, targetProjectKey, sourceTestsRepoName, targetTestsRepoName, HttpStatus.CREATED);
        gitlabRequestMockProvider.mockAddAuthenticatedWebHook();
        gitlabRequestMockProvider.mockAddAuthenticatedWebHook();
        gitlabRequestMockProvider.mockAddAuthenticatedWebHook();

        doNothing().when(gitService).pushSourceToTargetRepo(any(), any());

        if (!recreateBuildPlans) {
            String templateBuildPlanId = targetProjectKey + "-" + TEMPLATE.getName();
            String solutionBuildPlanId = targetProjectKey + "-" + SOLUTION.getName();

            // Mocks ProgramingExerciseImportService::cloneAndEnableAllBuildPlans
            jenkinsRequestMockProvider.mockCreateProjectForExercise(exerciseToBeImported);
            jenkinsRequestMockProvider.mockCopyBuildPlan(sourceExercise.getProjectKey(), targetProjectKey);
            jenkinsRequestMockProvider.mockCopyBuildPlan(sourceExercise.getProjectKey(), targetProjectKey);
            jenkinsRequestMockProvider.mockEnablePlan(targetProjectKey, templateBuildPlanId);
            jenkinsRequestMockProvider.mockEnablePlan(targetProjectKey, solutionBuildPlanId);

            // Mocks ProgramingExerciseImportService::updatePlanRepositoriesInBuildPlans
            mockUpdatePlanRepository(exerciseToBeImported, templateBuildPlanId, ASSIGNMENT_REPO_NAME, targetTemplateRepoName, List.of(ASSIGNMENT_REPO_NAME));
            mockUpdatePlanRepository(exerciseToBeImported, templateBuildPlanId, TEST_REPO_NAME, targetTestsRepoName, List.of());
            mockUpdatePlanRepository(exerciseToBeImported, solutionBuildPlanId, ASSIGNMENT_REPO_NAME, targetSolutionRepoName, List.of());
            mockUpdatePlanRepository(exerciseToBeImported, solutionBuildPlanId, TEST_REPO_NAME, targetTestsRepoName, List.of());
        }
        else {
            // Mocks for recreating the build plans
            jenkinsRequestMockProvider.mockCreateProjectForExercise(exerciseToBeImported);
            jenkinsRequestMockProvider.mockCreateBuildPlan(targetProjectKey);
            jenkinsRequestMockProvider.mockTriggerBuild();
        }
    }

    @Override
    public void mockForkRepositoryForParticipation(ProgrammingExercise exercise, String username, HttpStatus status) throws URISyntaxException, IOException {
        gitlabRequestMockProvider.mockForkRepositoryForParticipation(exercise, username, status);
    }

    @Override
    public void mockConnectorRequestsForStartParticipation(ProgrammingExercise exercise, String username, Set<User> users, boolean ltiUserExists, HttpStatus status)
            throws Exception {
        // Step 1a)
        gitlabRequestMockProvider.mockForkRepositoryForParticipation(exercise, username, status);
        // Step 1b)
        gitlabRequestMockProvider.mockConfigureRepository(exercise, username, users, ltiUserExists);
        // Step 2a)
        jenkinsRequestMockProvider.mockCopyBuildPlanForParticipation(exercise, username);
        // Step 2b)
        jenkinsRequestMockProvider.mockConfigureBuildPlan(exercise, username);
        // Note: Step 2c) is not needed in the Jenkins setup
        // Step 1c)
        gitlabRequestMockProvider.mockAddAuthenticatedWebHook();
    }

    @Override
    public void mockConnectorRequestsForResumeParticipation(ProgrammingExercise exercise, String username, Set<User> users, boolean ltiUserExists) throws Exception {
        // Step 2a)
        jenkinsRequestMockProvider.mockCopyBuildPlanForParticipation(exercise, username);
        // Step 2b)
        jenkinsRequestMockProvider.mockConfigureBuildPlan(exercise, username);
        // Note: Step 2c) is not needed in the Jenkins setup
    }

    @Override
    public void mockUpdatePlanRepositoryForParticipation(ProgrammingExercise exercise, String username) throws IOException, URISyntaxException {
        final var projectKey = exercise.getProjectKey();
        final var repoName = projectKey.toLowerCase() + "-" + username;
        mockUpdatePlanRepository(exercise, username, ASSIGNMENT_REPO_NAME, repoName, List.of());
    }

    @Override
    public void mockUpdatePlanRepository(ProgrammingExercise exercise, String planName, String repoNameInCI, String repoNameInVcs, List<String> triggeredBy)
            throws IOException, URISyntaxException {
        jenkinsRequestMockProvider.mockUpdatePlanRepository(exercise.getProjectKey(), planName);
    }

    @Override
    public void mockRemoveRepositoryAccess(ProgrammingExercise exercise, Team team, User firstStudent) throws Exception {
        final var repositorySlug = (exercise.getProjectKey() + "-" + team.getParticipantIdentifier()).toLowerCase();
        gitlabRequestMockProvider.mockRemoveMemberFromRepository(repositorySlug, firstStudent);
    }

    @Override
    public void mockRepositoryWritePermissions(Team team, User newStudent, ProgrammingExercise exercise, HttpStatus status) throws Exception {
        final var repositorySlug = (exercise.getProjectKey() + "-" + team.getParticipantIdentifier()).toLowerCase();
        gitlabRequestMockProvider.mockAddMemberToRepository(repositorySlug, newStudent);
    }

    @Override
    public void mockRetrieveArtifacts(ProgrammingExerciseStudentParticipation participation) throws MalformedURLException, URISyntaxException, JsonProcessingException {
        // Not necessary for the core functionality
    }

    @Override
    public void mockGetBuildLogs(ProgrammingExerciseStudentParticipation participation, List<BambooBuildResultDTO.BambooBuildLogEntryDTO> logs)
            throws URISyntaxException, JsonProcessingException {
        // TODO: implement
    }

    @Override
    public void resetMockProvider() {
        gitlabRequestMockProvider.reset();
        jenkinsRequestMockProvider.reset();
    }
}
