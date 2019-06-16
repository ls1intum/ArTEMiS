package de.tum.in.www1.artemis.service.connectors;

import static de.tum.in.www1.artemis.config.Constants.*;

import java.net.URL;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.atlassian.bamboo.specs.api.builders.AtlassianModule;
import com.atlassian.bamboo.specs.api.builders.BambooKey;
import com.atlassian.bamboo.specs.api.builders.applink.ApplicationLink;
import com.atlassian.bamboo.specs.api.builders.notification.AnyNotificationRecipient;
import com.atlassian.bamboo.specs.api.builders.notification.Notification;
import com.atlassian.bamboo.specs.api.builders.permission.PermissionType;
import com.atlassian.bamboo.specs.api.builders.permission.Permissions;
import com.atlassian.bamboo.specs.api.builders.permission.PlanPermissions;
import com.atlassian.bamboo.specs.api.builders.plan.Job;
import com.atlassian.bamboo.specs.api.builders.plan.Plan;
import com.atlassian.bamboo.specs.api.builders.plan.PlanIdentifier;
import com.atlassian.bamboo.specs.api.builders.plan.Stage;
import com.atlassian.bamboo.specs.api.builders.plan.branches.BranchCleanup;
import com.atlassian.bamboo.specs.api.builders.plan.branches.PlanBranchManagement;
import com.atlassian.bamboo.specs.api.builders.plan.configuration.ConcurrentBuilds;
import com.atlassian.bamboo.specs.api.builders.plan.dependencies.Dependencies;
import com.atlassian.bamboo.specs.api.builders.project.Project;
import com.atlassian.bamboo.specs.api.builders.repository.VcsChangeDetection;
import com.atlassian.bamboo.specs.api.builders.repository.VcsRepositoryIdentifier;
import com.atlassian.bamboo.specs.api.builders.requirement.Requirement;
import com.atlassian.bamboo.specs.builders.notification.PlanCompletedNotification;
import com.atlassian.bamboo.specs.builders.repository.bitbucket.server.BitbucketServerRepository;
import com.atlassian.bamboo.specs.builders.repository.viewer.BitbucketServerRepositoryViewer;
import com.atlassian.bamboo.specs.builders.task.*;
import com.atlassian.bamboo.specs.builders.trigger.BitbucketServerTrigger;
import com.atlassian.bamboo.specs.model.task.TestParserTaskProperties;
import com.atlassian.bamboo.specs.util.BambooServer;
import com.atlassian.bamboo.specs.util.SimpleUserPasswordCredentials;
import com.atlassian.bamboo.specs.util.UserPasswordCredentials;

import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.enumeration.ProgrammingLanguage;
import de.tum.in.www1.artemis.domain.enumeration.RepositoryType;

@Service
@Profile("bamboo")
public class BambooBuildPlanService {

    @Value("${artemis.bamboo.user}")
    private String BAMBOO_USER;

    @Value("${artemis.bamboo.password}")
    private String BAMBOO_PASSWORD;

    @Value("${artemis.bamboo.url}")
    private URL BAMBOO_SERVER_URL;

    @Value("${artemis.jira.admin-group-name}")
    private String ADMIN_GROUP_NAME;

    @Value("${server.url}")
    private URL SERVER_URL;

    @Value("${artemis.bamboo.bitbucket-application-link-id}")
    private String BITBUCKET_APPLICATION_LINK_ID;

    public Plan createTemplateBuildPlan(ProgrammingExercise programmingExercise, String repositoryName, String testRepositoryName) {
        Plan plan = createBuildPlanForExercise(programmingExercise, RepositoryType.TEMPLATE.getName(), repositoryName, testRepositoryName);
        publishPlan(plan);
        setAndPublishPermissions(plan, programmingExercise);
        return plan;
    }

    public Plan createSolutionBuildPlan(ProgrammingExercise programmingExercise, String repositoryName, String testRepositoryName, Plan templateRepositoryPlan) {
        Plan plan = createBuildPlanForExercise(programmingExercise, RepositoryType.SOLUTION.getName(), repositoryName, testRepositoryName)
                .dependencies(new Dependencies().childPlans(templateRepositoryPlan.getIdentifier()));
        publishPlan(plan);
        setAndPublishPermissions(plan, programmingExercise);
        return plan;
    }

    public Plan createBuildPlanForExercise(ProgrammingExercise programmingExercise, String planKey, String repositoryName, String testRepositoryName) {
        final String planDescription = planKey + " Build Plan for Exercise " + programmingExercise.getTitle();
        final String projectKey = programmingExercise.getProjectKey();
        final String projectName = programmingExercise.getProjectName();

        Plan plan = createDefaultBuildPlan(planKey, planDescription, projectKey, projectName, repositoryName, testRepositoryName)
                .stages(createBuildStage(programmingExercise.getProgrammingLanguage(), programmingExercise.getSequentialTestRuns()));

        return plan;
    }

    public Plan createTestBuildPlanForExercise(ProgrammingExercise programmingExercise, String testVcsRepositorySlug, Plan solutionRepositoryPlan, Plan templateRepositoryPlan) {
        // Bamboo build plan
        final String planName = "TESTS"; // Must be unique within the project
        final String planDescription = planName + " Build Plan for Exercise " + programmingExercise.getTitle();

        // Bamboo build project
        final String projectKey = programmingExercise.getProjectKey();
        final String projectName = programmingExercise.getProjectName();

        Plan plan = new Plan(createBuildProject(projectName, projectKey), planName, planName).description(planDescription)
                .pluginConfigurations(new ConcurrentBuilds().useSystemWideDefault(true))
                .planRepositories(createBuildPlanRepository(TEST_REPO_NAME, projectKey, testVcsRepositorySlug))
                // The test build plan will trigger the template and solution repository on every change.
                // This is the only purpose of the test repository build plan.
                .dependencies(new Dependencies().childPlans(templateRepositoryPlan.getIdentifier(), solutionRepositoryPlan.getIdentifier())).triggers(new BitbucketServerTrigger())
                .planBranchManagement(createPlanBranchManagement()).notifications(createNotification());
        publishPlan(plan);

        setAndPublishPermissions(plan, programmingExercise);
        return plan;
    }

    private void setAndPublishPermissions(Plan plan, ProgrammingExercise programmingExercise) {
        Course course = programmingExercise.getCourse();
        final String teachingAssistantGroupName = course.getTeachingAssistantGroupName();
        final String instructorGroupName = course.getInstructorGroupName();
        final PlanPermissions planPermission = setPlanPermission(programmingExercise.getProjectKey(), plan.getKey().toString(), teachingAssistantGroupName, instructorGroupName,
                ADMIN_GROUP_NAME);
        publishPlanPermissions(planPermission);
    }

    private void publishPlan(Plan plan) {
        UserPasswordCredentials userPasswordCredentials = new SimpleUserPasswordCredentials(BAMBOO_USER, BAMBOO_PASSWORD);
        BambooServer bambooServer = new BambooServer(BAMBOO_SERVER_URL.toString(), userPasswordCredentials);
        bambooServer.publish(plan);
    }

    private void publishPlanPermissions(PlanPermissions planPermissions) {
        UserPasswordCredentials userPasswordCredentials = new SimpleUserPasswordCredentials(BAMBOO_USER, BAMBOO_PASSWORD);
        BambooServer bambooServer = new BambooServer(BAMBOO_SERVER_URL.toString(), userPasswordCredentials);
        bambooServer.publish(planPermissions);
    }

    Project createBuildProject(String name, String key) {
        return new Project().key(key).name(name);
    }

    private Stage createBuildStage(ProgrammingLanguage programmingLanguage, Boolean sequentialBuildRuns) {
        if (programmingLanguage == ProgrammingLanguage.JAVA && !sequentialBuildRuns) {
            return new Stage("Default Stage").jobs(new Job("Default Job", new BambooKey("JOB1")).tasks(createCheckoutTask(ASSIGNMENT_REPO_PATH, ""),
                    new MavenTask().goal("clean test").jdk("JDK 1.8").executableLabel("Maven 3").description("Tests").hasTests(true)));
        }
        else if (programmingLanguage == ProgrammingLanguage.JAVA && sequentialBuildRuns) {
            return new Stage("Default Stage").jobs(new Job("Default Job", new BambooKey("JOB1")).tasks(createCheckoutTask(ASSIGNMENT_REPO_PATH, ""),
                    new MavenTask().goal("\'-Dtest=*StructuralTest\' clean test").jdk("JDK 1.8").executableLabel("Maven 3").description("Structural tests").hasTests(true),
                    new MavenTask().goal("\'-Dtest=*BehaviorTest\' clean test").jdk("JDK 1.8").executableLabel("Maven 3").description("Behavior tests").hasTests(true)));
        }
        else if (programmingLanguage == ProgrammingLanguage.PYTHON && !sequentialBuildRuns) {
            return new Stage("Default Stage")
                    .jobs(new Job("Default Job", new BambooKey("JOB1"))
                            .tasks(createCheckoutTask("", "tests"),
                                    new ScriptTask().description("Builds and tests the code").inlineBody("pytest --junitxml=test-reports/results.xml\nexit 0"),
                                    new TestParserTask(TestParserTaskProperties.TestType.JUNIT).resultDirectories("test-reports/results.xml"))
                            .requirements(new Requirement("Python3")));
        }
        else if (programmingLanguage == ProgrammingLanguage.PYTHON && sequentialBuildRuns) {
            return new Stage("Default Stage").jobs(new Job("Default Job", new BambooKey("JOB1")).tasks(createCheckoutTask("", "tests"),
                    new ScriptTask().description("Builds and tests the code").inlineBody("pytest test/structural --junitxml=test-reports/structural-results.xml\nexit 0"),
                    new ScriptTask().description("Builds and tests the code").inlineBody("pytest test/behavior --junitxml=test-reports/behavior-results.xml\nexit 0"),
                    new TestParserTask(TestParserTaskProperties.TestType.JUNIT).resultDirectories("test-reports/results.xml")).requirements(new Requirement("Python3")));
        }

        return null;
    }

    private Plan createDefaultBuildPlan(String planKey, String planDescription, String projectKey, String projectName, String repositoryName, String vcsTestRepositorySlug) {
        return new Plan(createBuildProject(projectName, projectKey), planKey, planKey).description(planDescription)
                .pluginConfigurations(new ConcurrentBuilds().useSystemWideDefault(true))
                .planRepositories(createBuildPlanRepository(ASSIGNMENT_REPO_NAME, projectKey, repositoryName),
                        createBuildPlanRepository(TEST_REPO_NAME, projectKey, vcsTestRepositorySlug))
                .triggers(new BitbucketServerTrigger()).planBranchManagement(createPlanBranchManagement()).notifications(createNotification());
    }

    private VcsCheckoutTask createCheckoutTask(String assignmentPath, String testPath) {
        return new VcsCheckoutTask().description("Checkout Default Repository").checkoutItems(
                new CheckoutItem().repository(new VcsRepositoryIdentifier().name(ASSIGNMENT_REPO_NAME)).path(assignmentPath), // NOTE: this path needs to be specified in the Maven
                                                                                                                              // pom.xml in the Tests Repo
                new CheckoutItem().repository(new VcsRepositoryIdentifier().name(TEST_REPO_NAME)).path(testPath));
    }

    private PlanBranchManagement createPlanBranchManagement() {
        return new PlanBranchManagement().delete(new BranchCleanup()).notificationForCommitters();
    }

    private Notification createNotification() {
        return new Notification().type(new PlanCompletedNotification()).recipients(
                new AnyNotificationRecipient(new AtlassianModule("de.tum.in.www1.bamboo-server:recipient.server")).recipientString(SERVER_URL + NEW_RESULT_RESOURCE_API_PATH));
    }

    private BitbucketServerRepository createBuildPlanRepository(String name, String vcsProjectKey, String repositorySlug) {
        return new BitbucketServerRepository().name(name).repositoryViewer(new BitbucketServerRepositoryViewer()).server(new ApplicationLink().id(BITBUCKET_APPLICATION_LINK_ID))
                .projectKey(vcsProjectKey).repositorySlug(repositorySlug.toLowerCase())   // make sure to use lower case to avoid problems in change detection between Bamboo and
                                                                                          // Bitbucket
                .shallowClonesEnabled(true).remoteAgentCacheEnabled(false).changeDetection(new VcsChangeDetection());
    }

    private PlanPermissions setPlanPermission(String bambooProjectKey, String bambooPlanKey, String teachingAssistantGroupName, String instructorGroupName, String adminGroupName) {
        final PlanPermissions planPermission = new PlanPermissions(new PlanIdentifier(bambooProjectKey, bambooPlanKey)).permissions(
                new Permissions().userPermissions(BAMBOO_USER, PermissionType.EDIT, PermissionType.BUILD, PermissionType.CLONE, PermissionType.VIEW, PermissionType.ADMIN)
                        .groupPermissions(adminGroupName, PermissionType.CLONE, PermissionType.BUILD, PermissionType.EDIT, PermissionType.VIEW, PermissionType.ADMIN)
                        .groupPermissions(instructorGroupName, PermissionType.CLONE, PermissionType.BUILD, PermissionType.EDIT, PermissionType.VIEW, PermissionType.ADMIN)
                        .groupPermissions(teachingAssistantGroupName, PermissionType.BUILD, PermissionType.EDIT, PermissionType.VIEW));
        return planPermission;
    }

}
