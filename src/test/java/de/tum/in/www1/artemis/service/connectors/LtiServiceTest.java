package de.tum.in.www1.artemis.service.connectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import de.tum.in.www1.artemis.authentication.AuthenticationIntegrationTestHelper;
import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.repository.LtiOutcomeUrlRepository;
import de.tum.in.www1.artemis.repository.LtiUserIdRepository;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.security.ArtemisAuthenticationProvider;
import de.tum.in.www1.artemis.service.user.UserService;
import de.tum.in.www1.artemis.web.rest.dto.LtiLaunchRequestDTO;

public class LtiServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LtiOutcomeUrlRepository ltiOutcomeUrlRepository;

    @Mock
    private ResultRepository resultRepository;

    @Mock
    private ArtemisAuthenticationProvider artemisAuthenticationProvider;

    @Mock
    private LtiUserIdRepository ltiUserIdRepository;

    @Mock
    private HttpServletResponse response;

    private Exercise exercise;

    private LtiService ltiService;

    private LtiLaunchRequestDTO launchRequest;

    private User user;

    private LtiUserId ltiUserId;

    private final String courseStudentGroupName = "courseStudentGroupName";

    private LtiOutcomeUrl ltiOutcomeUrl;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
        ltiService = new LtiService(userService, userRepository, ltiOutcomeUrlRepository, resultRepository, artemisAuthenticationProvider, ltiUserIdRepository, response);
        Course course = new Course();
        course.setStudentGroupName(courseStudentGroupName);
        exercise = new TextExercise();
        exercise.setCourse(course);
        launchRequest = AuthenticationIntegrationTestHelper.setupDefaultLtiLaunchRequest();
        user = new User();
        user.setLogin("login");
        user.setPassword("password");
        user.setGroups(new HashSet<>());
        ltiUserId = new LtiUserId();
        ltiUserId.setUser(user);
        ltiOutcomeUrl = new LtiOutcomeUrl();

        ReflectionTestUtils.setField(ltiService, "USER_GROUP_NAME_EDX", Optional.of(""));
        ReflectionTestUtils.setField(ltiService, "USER_GROUP_NAME_U4I", Optional.of(""));
        ReflectionTestUtils.setField(ltiService, "USER_PREFIX_EDX", Optional.of(""));
        ReflectionTestUtils.setField(ltiService, "USER_PREFIX_U4I", Optional.of(""));
    }

    @Test
    public void handleLaunchRequest_LTILaunchFromEdx() {
        launchRequest.setUser_id("student");

        InternalAuthenticationServiceException exception = assertThrows(InternalAuthenticationServiceException.class, () -> {
            ltiService.handleLaunchRequest(launchRequest, exercise);
        });

        String expectedMessage = "Invalid username sent by launch request. Please do not launch the exercise from edX studio. Use 'Preview' instead.";
        assertThat(exception.getMessage().equals(expectedMessage));
    }

    @Test
    public void handleLaunchRequest_InvalidContextLabel() {
        launchRequest.setContext_label("randomLabel");

        InternalAuthenticationServiceException exception = assertThrows(InternalAuthenticationServiceException.class, () -> {
            ltiService.handleLaunchRequest(launchRequest, exercise);
        });

        String expectedMessage = "Unknown context_label sent in LTI Launch Request: " + launchRequest.toString();
        assertThat(exception.getMessage().equals(expectedMessage));
    }

    @Test
    public void handleLaunchRequest_existingMappingForLtiUserId() {
        when(ltiUserIdRepository.findByLtiUserId(launchRequest.getUser_id())).thenReturn(Optional.of(ltiUserId));
        when(userRepository.getUserWithGroupsAndAuthorities()).thenReturn(user);

        onSuccessfulAuthenticationSetup(user, ltiUserId);

        ltiService.handleLaunchRequest(launchRequest, exercise);

        onSuccessfulAuthenticationAssertions(user, ltiUserId);
    }

    @Test
    public void handleLaunchRequest_lookupWithLtiEmailAddress() {
        String username = "username";
        String email = launchRequest.getLis_person_contact_email_primary();
        launchRequest.setCustom_lookup_user_by_email(true);
        when(ltiUserIdRepository.findByLtiUserId(launchRequest.getUser_id())).thenReturn(Optional.empty());
        when(artemisAuthenticationProvider.getUsernameForEmail(email)).thenReturn(Optional.of(username));
        when(artemisAuthenticationProvider.getOrCreateUser(new UsernamePasswordAuthenticationToken(username, ""), "", launchRequest.getLis_person_sourcedid(), email, true))
                .thenReturn(user);

        onSuccessfulAuthenticationSetup(user, ltiUserId);

        ltiService.handleLaunchRequest(launchRequest, exercise);

        onSuccessfulAuthenticationAssertions(user, ltiUserId);
    }

    @Test
    public void handleLaunchRequest_newUserIsNotRequired() {
        String username = launchRequest.getLis_person_sourcedid();
        Set<String> groups = new HashSet<>();
        groups.add("");
        user.setActivated(false);
        when(ltiUserIdRepository.findByLtiUserId(launchRequest.getUser_id())).thenReturn(Optional.empty());
        when(userRepository.findOneByLogin(username)).thenReturn(Optional.empty());
        when(userService.createUser(username, groups, "", launchRequest.getLis_person_sourcedid(), launchRequest.getLis_person_contact_email_primary(), null, null, "en"))
                .thenReturn(user);

        onSuccessfulAuthenticationSetup(user, ltiUserId);

        ltiService.handleLaunchRequest(launchRequest, exercise);

        onSuccessfulAuthenticationAssertions(user, ltiUserId);
        verify(userService).activateUser(user);

        SecurityContextHolder.clearContext();
        launchRequest.setContext_label("randomLabel");

        InternalAuthenticationServiceException exception = assertThrows(InternalAuthenticationServiceException.class, () -> {
            ltiService.handleLaunchRequest(launchRequest, exercise);
        });

        String expectedMessage = "User group not activated or unknown context_label sent in LTI Launch Request: " + launchRequest.toString();
        assertThat(exception.getMessage().equals(expectedMessage));
    }

    @Test
    public void handleLaunchRequest_noAuthenticationWasSuccessful() {
        launchRequest.setCustom_require_existing_user(true);
        when(ltiUserIdRepository.findByLtiUserId(launchRequest.getUser_id())).thenReturn(Optional.empty());
        when(response.containsHeader("Set-Cookie")).thenReturn(true);
        List<String> headers = new ArrayList<>();
        headers.add("JSESSIONID=(123);");
        when(response.getHeaders("Set-Cookie")).thenReturn(headers);
        when(response.getHeader("Set-Cookie")).thenReturn(headers.get(0));
        String sessionId = "(123)";

        ltiService.handleLaunchRequest(launchRequest, exercise);

        assertThat(ltiService.launchRequestForSession.containsKey(sessionId));
        assertThat(ltiService.launchRequestForSession.containsValue(Pair.of(launchRequest, exercise)));
        assertThat(ltiService.launchRequestForSession.get(sessionId).equals(Pair.of(launchRequest, exercise)));
    }

    @Test
    public void onSuccessfulLtiAuthentication() {
        ltiUserId.setLtiUserId("oldStudentId");
        onSuccessfulAuthenticationSetup(user, ltiUserId);

        ltiService.onSuccessfulLtiAuthentication(launchRequest, exercise);

        onSuccessfulAuthenticationAssertions(user, ltiUserId);
    }

    private void onSuccessfulAuthenticationSetup(User user, LtiUserId ltiUserId) {
        when(userRepository.getUserWithGroupsAndAuthorities()).thenReturn(user);
        when(ltiUserIdRepository.findByUser(user)).thenReturn(Optional.of(ltiUserId));
        ltiOutcomeUrlRepositorySetup(user);
    }

    private void ltiOutcomeUrlRepositorySetup(User user) {
        when(ltiOutcomeUrlRepository.findByUserAndExercise(user, exercise)).thenReturn(Optional.of(ltiOutcomeUrl));
    }

    private void onSuccessfulAuthenticationAssertions(User user, LtiUserId ltiUserId) {
        assertThat(user.getGroups().contains(courseStudentGroupName));
        assertThat("ff30145d6884eeb2c1cef50298939383".equals(ltiUserId.getLtiUserId()));
        assertThat("some.outcome.service.url.com".equals(ltiOutcomeUrl.getUrl()));
        assertThat("someResultSourceId".equals(ltiOutcomeUrl.getSourcedId()));
        verify(userService, times(1)).saveUser(user);
        verify(artemisAuthenticationProvider, times(1)).addUserToGroup(user, courseStudentGroupName);
        verify(ltiOutcomeUrlRepository, times(1)).save(ltiOutcomeUrl);
    }

    @Test
    public void verifyRequest_oauthSecretNotSpecified() {
        ReflectionTestUtils.setField(ltiService, "OAUTH_SECRET", Optional.empty());
        HttpServletRequest request = mock(HttpServletRequest.class);

        String message = ltiService.verifyRequest(request);

        assertThat("verifyRequest for LTI is not supported on this Artemis instance, artemis.lti.oauth-secret was not specified in the yml configuration".equals(message));
    }

    @Test
    public void verifyRequest_unsuccessfulVerification() {
        ReflectionTestUtils.setField(ltiService, "OAUTH_SECRET", Optional.of("secret"));
        String url = "http://some.url.com";
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(anyString())).thenReturn(null);
        when(request.getRequestURL()).thenReturn(new StringBuffer(url));
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(request.getParameterNames()).thenReturn(Collections.emptyEnumeration());

        String message = ltiService.verifyRequest(request);

        assertThat("LTI signature verification failed with message: Failed to validate: parameter_absent; error: bad_request, launch result: null".equals(message));
    }

    @Test
    public void onNewResult() {
        ReflectionTestUtils.setField(ltiService, "OAUTH_KEY", Optional.of("oauthKey"));
        ReflectionTestUtils.setField(ltiService, "OAUTH_SECRET", Optional.of("oauthSecret"));

        StudentParticipation participation = new StudentParticipation();
        User user = new User();
        participation.setParticipant(user);
        participation.setExercise(exercise);
        participation.setId(27L);
        Result result = new Result();
        result.setScore(3L);
        LtiOutcomeUrl ltiOutcomeUrl = new LtiOutcomeUrl();
        ltiOutcomeUrl.setUrl("https://some.url.com/");
        ltiOutcomeUrl.setSourcedId("sourceId");
        ltiOutcomeUrl.setExercise(exercise);
        ltiOutcomeUrl.setUser(user);

        ltiOutcomeUrlRepositorySetup(user);
        when(resultRepository.findFirstByParticipationIdOrderByCompletionDateDesc(27L)).thenReturn(Optional.of(result));

        ltiService.onNewResult(participation);

        verify(resultRepository, times(1)).findFirstByParticipationIdOrderByCompletionDateDesc(27L);
    }

    @Test
    public void handleLaunchRequestForSession() {
        String sessionId = "sessionId";
        ltiService.launchRequestForSession.put(sessionId, Pair.of(launchRequest, exercise));
        onSuccessfulAuthenticationSetup(user, ltiUserId);

        ltiService.handleLaunchRequestForSession(sessionId);

        onSuccessfulAuthenticationAssertions(user, ltiUserId);
        assertThat(ltiService.launchRequestForSession.isEmpty());
    }

}
