package de.tum.in.www1.artemis.service.connectors.jenkins;

import java.net.URL;

import org.w3c.dom.Document;

public interface JenkinsXmlConfigBuilder {

    /**
     * Creates a basic build config for Jenkins based on the given repository URLs. I.e. a build that tests the assignment
     * code and exports the build results to Artemis afterwards. If static code analysis is activated, the plan will additionally
     * execute supported static code analysis tools.
     *
     * @param testRepositoryURL The URL of the repository containing all exercise tests
     * @param assignmentRepositoryURL The URL of the assignment repository, i.e. template or participation repo
     * @param isStaticCodeAnalysisEnabled Flag which determines whether a build plan with or without static code analysis is created
     * @return The parsed XML document containing the Jenkins build config
     */
    Document buildBasicConfig(URL testRepositoryURL, URL assignmentRepositoryURL, boolean isStaticCodeAnalysisEnabled);
}
