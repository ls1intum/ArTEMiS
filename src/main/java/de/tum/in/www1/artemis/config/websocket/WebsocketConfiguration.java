package de.tum.in.www1.artemis.config.websocket;

import java.security.Principal;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurationSupport;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;
import org.xnio.Options;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.www1.artemis.security.AuthoritiesConstants;

@Configuration
public class WebsocketConfiguration extends WebSocketMessageBrokerConfigurationSupport {

    private final Logger log = LoggerFactory.getLogger(WebsocketConfiguration.class);

    public static final String IP_ADDRESS = "IP_ADDRESS";

    private final ObjectMapper objectMapper;

    private TaskScheduler messageBrokerTaskScheduler;

    private WebSocketMessageBrokerStats webSocketMessageBrokerStats;

    private ThreadPoolTaskExecutor inboundChannelExecutor;

    private ThreadPoolTaskExecutor outboundChannelExecutor;

    public WebsocketConfiguration(MappingJackson2HttpMessageConverter springMvcJacksonConverter) {
        this.objectMapper = springMvcJacksonConverter.getObjectMapper();
    }

    @PostConstruct
    public void init() {
        // using Autowired leads to a weird bug, because the order of the method execution is changed. This somehow prevents messages send to single clients
        // later one, e.g. in the code editor. Therefore we call this method here directly to get a reference and adapt the logging period!
        webSocketMessageBrokerStats = webSocketMessageBrokerStats();
        webSocketMessageBrokerStats.setLoggingPeriod(5 * 1000);
    }

    @Override
    protected void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // limit these values to prevent problems with slow clients
        registration.setSendTimeLimit(5 * 1000).setSendBufferSizeLimit(128 * 1024);
    }

    @Bean
    public UndertowServletWebServerFactory undertowServletWebServerFactory() {
        final var factory = new UndertowServletWebServerFactory();
        factory.addBuilderCustomizers(builder -> builder.setSocketOption(Options.WRITE_TIMEOUT, 60 * 1000));
        return factory;
    }

    @Autowired
    public void setMessageBrokerTaskScheduler(TaskScheduler taskScheduler) {
        this.messageBrokerTaskScheduler = taskScheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic").setHeartbeatValue(new long[] { 25000, 25000 }).setTaskScheduler(messageBrokerTaskScheduler);
        // increase the limit of concurrent connections (default is 1024 which is much too low)
        config.setCacheLimit(10000);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        DefaultHandshakeHandler handshakeHandler = defaultHandshakeHandler();
        // NOTE: by setting a WebSocketTransportHandler we disable http poll, http stream and other exotic workarounds and only support real websocket connections.
        // nowadays all modern browsers support websockets and workarounds are not necessary any more and might only lead to problems
        WebSocketTransportHandler webSocketTransportHandler = new WebSocketTransportHandler(handshakeHandler);
        registry.addEndpoint("/websocket/tracker").setAllowedOrigins("*").withSockJS().setTransportHandlers(webSocketTransportHandler)
                .setInterceptors(httpSessionHandshakeInterceptor());
    }

    // TODO: allow to customize these settings via application.yml file

    @Override
    public ThreadPoolTaskExecutor clientOutboundChannelExecutor() {
        outboundChannelExecutor = super.clientOutboundChannelExecutor();
        // outboundChannelExecutor.setQueueCapacity(100 * 1000);
        // a higher pool size can help when problems with slow clients occur
        outboundChannelExecutor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 4);
        // outboundChannelExecutor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 8);
        outboundChannelExecutor.setKeepAliveSeconds(10);
        outboundChannelExecutor.setAllowCoreThreadTimeOut(true);
        return outboundChannelExecutor;
    }

    @Override
    public ThreadPoolTaskExecutor clientInboundChannelExecutor() {
        inboundChannelExecutor = super.clientInboundChannelExecutor();
        // inboundChannelExecutor.setQueueCapacity(100 * 1000);
        // outboundChannelExecutor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 8);
        inboundChannelExecutor.setKeepAliveSeconds(10);
        inboundChannelExecutor.setAllowCoreThreadTimeOut(true);
        return inboundChannelExecutor;
    }

    @NotNull
    @Override
    protected MappingJackson2MessageConverter createJacksonConverter() {
        // NOTE: We need to adapt the default messageConverter for WebSocket messages
        // with a messageConverter that uses the same ObjectMapper that our REST endpoints use.
        // This gives us consistency in how specific datatypes are serialized (e.g. timestamps)
        MappingJackson2MessageConverter converter = super.createJacksonConverter();
        converter.setObjectMapper(objectMapper);
        return converter;
    }

    @Bean
    public HandshakeInterceptor httpSessionHandshakeInterceptor() {
        return new HandshakeInterceptor() {

            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                if (request instanceof ServletServerHttpRequest) {
                    ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                    attributes.put(IP_ADDRESS, servletRequest.getRemoteAddress());
                }
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
                if (exception != null) {
                    log.warn("Exception occurred in WS.afterHandshake: " + exception.getMessage());
                }
            }
        };
    }

    private DefaultHandshakeHandler defaultHandshakeHandler() {
        return new DefaultHandshakeHandler() {

            @Override
            protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                Principal principal = request.getPrincipal();
                if (principal == null) {
                    Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.ANONYMOUS));
                    principal = new AnonymousAuthenticationToken("WebsocketConfiguration", "anonymous", authorities);
                }
                log.debug("determineUser: " + principal);
                return principal;
            }
        };
    }
}
