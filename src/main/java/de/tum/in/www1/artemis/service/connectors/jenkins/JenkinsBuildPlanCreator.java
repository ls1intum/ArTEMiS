package de.tum.in.www1.artemis.service.connectors.jenkins;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;

import de.tum.in.www1.artemis.ResourceLoaderService;
import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.domain.VcsRepositoryUrl;
import de.tum.in.www1.artemis.domain.enumeration.ProgrammingLanguage;
import de.tum.in.www1.artemis.domain.enumeration.StaticCodeAnalysisTool;
import de.tum.in.www1.artemis.service.util.XmlFileUtils;

@Profile("jenkins")
@Component
public class JenkinsBuildPlanCreator implements JenkinsXmlConfigBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(JenkinsBuildPlanCreator.class);

    private static final String STATIC_CODE_ANALYSIS_REPORT_DIR = "staticCodeAnalysisReports";

    private static final String REPLACE_PIPELINE_SCRIPT = "#pipelineScript";

    private static final String REPLACE_TEST_REPO = "#testRepository";

    private static final String REPLACE_ASSIGNMENT_REPO = "#assignmentRepository";

    private static final String REPLACE_GIT_CREDENTIALS = "#gitCredentials";

    private static final String REPLACE_ASSIGNMENT_CHECKOUT_PATH = "#assignmentCheckoutPath";

    private static final String REPLACE_TESTS_CHECKOUT_PATH = "#testsCheckoutPath";

    private static final String REPLACE_PUSH_TOKEN = "#secretPushToken";

    private static final String REPLACE_ARTEMIS_NOTIFICATION_URL = "#notificationsUrl";

    private static final String REPLACE_NOTIFICATIONS_TOKEN = "#jenkinsNotificationToken";

    private static final String REPLACE_STATIC_CODE_ANALYSIS_SCRIPT = "#staticCodeAnalysisScript";

    private static final String REPLACE_DOCKER_IMAGE_NAME = "#dockerImage";

    private static final String REPLACE_JENKINS_TIMEOUT = "#jenkinsTimeout";

    private String artemisNotificationUrl;

    @Value("${artemis.continuous-integration.secret-push-token}")
    private String pushToken;

    @Value("${artemis.continuous-integration.vcs-credentials}")
    private String gitCredentialsKey;

    @Value("${artemis.continuous-integration.build-timeout:#{30}}")
    private String buildTimeout;

    @Value("${server.url}")
    private String ARTEMIS_SERVER_URL;

    @Value("${artemis.continuous-integration.artemis-authentication-token-key}")
    private String ARTEMIS_AUTHENTICATION_TOKEN_KEY;

    private final ResourceLoaderService resourceLoaderService;

    private final JenkinsService jenkinsService;

    public JenkinsBuildPlanCreator(ResourceLoaderService resourceLoaderService, @Lazy JenkinsService jenkinsService) {
        this.resourceLoaderService = resourceLoaderService;
        this.jenkinsService = jenkinsService;
    }

    @PostConstruct
    public void init() {
        this.artemisNotificationUrl = ARTEMIS_SERVER_URL + "/api" + Constants.NEW_RESULT_RESOURCE_PATH;
    }

    public String getPipelineScript(ProgrammingLanguage programmingLanguage, VcsRepositoryUrl testRepositoryURL, VcsRepositoryUrl assignmentRepositoryURL,
            boolean isStaticCodeAnalysisEnabled) {
        var pipelinePath = getResourcePath(programmingLanguage, isStaticCodeAnalysisEnabled);
        var replacements = getReplacements(programmingLanguage, testRepositoryURL, assignmentRepositoryURL, isStaticCodeAnalysisEnabled);
        return replacePipelineScriptParameters(pipelinePath, replacements);
    }

    private Map<String, String> getReplacements(ProgrammingLanguage programmingLanguage, VcsRepositoryUrl testRepositoryURL, VcsRepositoryUrl assignmentRepositoryURL,
            boolean isStaticCodeAnalysisEnabled) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put(REPLACE_TEST_REPO, testRepositoryURL.getURL().toString());
        replacements.put(REPLACE_ASSIGNMENT_REPO, assignmentRepositoryURL.getURL().toString());
        replacements.put(REPLACE_GIT_CREDENTIALS, gitCredentialsKey);
        replacements.put(REPLACE_ASSIGNMENT_CHECKOUT_PATH, Constants.ASSIGNMENT_CHECKOUT_PATH);
        replacements.put(REPLACE_TESTS_CHECKOUT_PATH, Constants.TESTS_CHECKOUT_PATH);
        replacements.put(REPLACE_ARTEMIS_NOTIFICATION_URL, artemisNotificationUrl);
        replacements.put(REPLACE_NOTIFICATIONS_TOKEN, ARTEMIS_AUTHENTICATION_TOKEN_KEY);
        replacements.put(REPLACE_DOCKER_IMAGE_NAME, jenkinsService.getDockerImageName(programmingLanguage));
        replacements.put(REPLACE_JENKINS_TIMEOUT, buildTimeout);
        // at the moment, only Java and Swift are supported
        if (isStaticCodeAnalysisEnabled) {
            String staticCodeAnalysisScript = createStaticCodeAnalysisScript(programmingLanguage);
            replacements.put(REPLACE_STATIC_CODE_ANALYSIS_SCRIPT, staticCodeAnalysisScript);
        }
        return replacements;
    }

    private String[] getResourcePath(ProgrammingLanguage programmingLanguage, boolean isStaticCodeAnalysisEnabled) {
        final var buildPlan = isStaticCodeAnalysisEnabled ? "Jenkinsfile-staticCodeAnalysis" : "Jenkinsfile";
        return new String[] { "templates", "jenkins", programmingLanguage.name().toLowerCase(), buildPlan };
    }

    @Override
    public Document buildBasicConfig(ProgrammingLanguage programmingLanguage, VcsRepositoryUrl testRepositoryURL, VcsRepositoryUrl assignmentRepositoryURL,
            boolean isStaticCodeAnalysisEnabled) {
        final var resourcePath = Paths.get("templates", "jenkins", "config.xml");

        String pipeLineScript = getPipelineScript(programmingLanguage, testRepositoryURL, assignmentRepositoryURL, isStaticCodeAnalysisEnabled);
        pipeLineScript = pipeLineScript.replace("'", "&apos;");
        pipeLineScript = pipeLineScript.replace("<", "&lt;");
        pipeLineScript = pipeLineScript.replace(">", "&gt;");
        pipeLineScript = pipeLineScript.replace("\\", "\\\\");

        Map<String, String> replacements = Map.of(REPLACE_PIPELINE_SCRIPT, pipeLineScript, REPLACE_PUSH_TOKEN, pushToken);

        final var xmlResource = resourceLoaderService.getResource(resourcePath.toString());
        return XmlFileUtils.readXmlFile(xmlResource, replacements);
    }

    private String replacePipelineScriptParameters(String[] pipelineScriptPath, Map<String, String> variablesToReplace) {
        final var resource = resourceLoaderService.getResource(pipelineScriptPath);
        try {
            var pipelineScript = StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());
            if (variablesToReplace != null) {
                for (final var replacement : variablesToReplace.entrySet()) {
                    pipelineScript = pipelineScript.replace(replacement.getKey(), replacement.getValue());
                }
            }

            return pipelineScript;
        }
        catch (IOException e) {
            final var errorMessage = "Error loading template Jenkins build XML: " + e.getMessage();
            LOG.error(errorMessage, e);
            throw new IllegalStateException(errorMessage, e);
        }
    }

    // at the moment, only Java and Swift are supported
    private String createStaticCodeAnalysisScript(ProgrammingLanguage programmingLanguage) {
        StringBuilder script = new StringBuilder();
        String lineEnding = "&#xd;";
        // Make directory for generated static code analysis reports
        script.append("mkdir ").append(STATIC_CODE_ANALYSIS_REPORT_DIR).append(lineEnding);
        if (programmingLanguage == ProgrammingLanguage.JAVA) {
            script.append("mvn ");
            // Execute all static code analysis tools for Java
            script.append(StaticCodeAnalysisTool.createBuildPlanCommandForProgrammingLanguage(programmingLanguage)).append(lineEnding);
            // Copy all static code analysis reports to new directory
            for (var tool : StaticCodeAnalysisTool.getToolsForProgrammingLanguage(programmingLanguage)) {
                script.append("cp target/").append(tool.getFilePattern()).append(" ").append(STATIC_CODE_ANALYSIS_REPORT_DIR).append(lineEnding);
            }
        }
        else if (programmingLanguage == ProgrammingLanguage.SWIFT) {
            StaticCodeAnalysisTool tool = StaticCodeAnalysisTool.getToolsForProgrammingLanguage(programmingLanguage).get(0);
            script.append("echo \"---------- execute static code analysis ----------\"").append(lineEnding);
            // Copy swiftlint configuration into student's repository
            script.append("cp .swiftlint.yml assignment || true").append(lineEnding);
            // Execute swiftlint within the student's repository and save the report into the sca directory
            // sh command: swiftlint lint assignment > <scaDir>/<result>.xml
            script.append("swiftlint lint assignment > ").append(STATIC_CODE_ANALYSIS_REPORT_DIR).append("/").append(tool.getFilePattern()).append(lineEnding);
        }
        return script.toString();
    }
}
