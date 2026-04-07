package com.learnfast.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    private final WebSocketConfig config = new WebSocketConfig();

    @Mock MessageBrokerRegistry brokerRegistry;
    @Mock StompEndpointRegistry endpointRegistry;
    @Mock StompWebSocketEndpointRegistration endpointRegistration;

    // ── configureMessageBroker ────────────────────────────────────────────────

    @Test
    void configureMessageBroker_enablesSimpleBrokerOnTopicPrefix() {
        config.configureMessageBroker(brokerRegistry);

        verify(brokerRegistry).enableSimpleBroker("/topic");
    }

    @Test
    void configureMessageBroker_setsAppDestinationPrefix() {
        config.configureMessageBroker(brokerRegistry);

        verify(brokerRegistry).setApplicationDestinationPrefixes("/app");
    }

    // ── registerStompEndpoints ────────────────────────────────────────────────

    @Test
    void registerStompEndpoints_registersWsEndpointWithSockJS() {
        when(endpointRegistry.addEndpoint("/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.setAllowedOriginPatterns("*")).thenReturn(endpointRegistration);

        config.registerStompEndpoints(endpointRegistry);

        verify(endpointRegistry).addEndpoint("/ws");
        verify(endpointRegistration).setAllowedOriginPatterns("*");
        verify(endpointRegistration).withSockJS();
    }

    @Test
    void registerStompEndpoints_allowsAllOriginPatterns() {
        when(endpointRegistry.addEndpoint(anyString())).thenReturn(endpointRegistration);
        when(endpointRegistration.setAllowedOriginPatterns(anyString())).thenReturn(endpointRegistration);

        config.registerStompEndpoints(endpointRegistry);

        ArgumentCaptor<String> originCaptor = ArgumentCaptor.forClass(String.class);
        verify(endpointRegistration).setAllowedOriginPatterns(originCaptor.capture());
        assertThat(originCaptor.getValue()).isEqualTo("*");
    }
}
