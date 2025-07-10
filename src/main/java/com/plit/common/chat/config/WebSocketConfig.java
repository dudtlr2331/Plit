package com.plit.common.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 구독(prefix)
        config.enableSimpleBroker("/sub");
        // 발행(prefix)
        config.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 연결 endpoint + SockJS
        registry.addEndpoint("/ws-stomp").setAllowedOriginPatterns("*").withSockJS();
        registry.addEndpoint("/ws/chat").setAllowedOriginPatterns("*").withSockJS();
    }
}

