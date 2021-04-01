package de.tum.in.www1.artemis;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

import de.tum.in.www1.artemis.web.rest.ClientForwardResource;
import de.tum.in.www1.artemis.web.rest.vm.LoggerVM;

/**
 * Test class for the ClientForwardController REST controller.
 *
 * @see ClientForwardResource
 */
public class ClientForwardTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testManagementEndpoint() throws Exception {
        request.getList("/management/logs", HttpStatus.OK, LoggerVM.class);
    }

    @Test
    public void testClientEndpoint() throws Exception {
        ResultActions perform = request.getMvc().perform(get("/non-existant-mapping"));
        perform.andExpect(status().isOk()).andExpect(forwardedUrl("/"));
    }

    @Test
    public void testNestedClientEndpoint() throws Exception {
        request.getMvc().perform(get("/admin/user-management")).andExpect(status().isOk()).andExpect(forwardedUrl("/"));
    }
}
