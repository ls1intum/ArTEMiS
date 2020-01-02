package de.tum.in.www1.artemis.connector.bitbucket;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.service.connectors.bitbucket.dto.BitbucketProjectSearchDTO;

@Component
@Profile("bitbucket")
public class BitbucketRequestMockProvider {

    @Value("${artemis.version-control.url}")
    private URL BITBUCKET_SERVER_URL;

    private final RestTemplate restTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    private MockRestServiceServer mockServer;

    public BitbucketRequestMockProvider(@Qualifier("bitbucketRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void enableMockingOfRequests() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    public void mockCheckIfProjectExists(ProgrammingExercise exercise) throws IOException, URISyntaxException {
        final var projectKey = exercise.getProjectKey();
        final var projectName = exercise.getProjectName();
        final var bitbucketSearchDTO = new BitbucketProjectSearchDTO();
        bitbucketSearchDTO.setSize(0);
        bitbucketSearchDTO.setSearchResults(new ArrayList<>());

        mockServer.expect(ExpectedCount.manyTimes(), requestTo(BITBUCKET_SERVER_URL + "/rest/api/1.0/projects/" + projectKey)).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        final var projectSearchPath = UriComponentsBuilder.fromUri(BITBUCKET_SERVER_URL.toURI()).path("/rest/api/1.0/projects").queryParam("name", projectName);
        mockServer.expect(ExpectedCount.manyTimes(), requestTo(projectSearchPath.build().toUri())).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK).body(mapper.writeValueAsString(bitbucketSearchDTO)).contentType(MediaType.APPLICATION_JSON));
    }
}
