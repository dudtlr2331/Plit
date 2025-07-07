package com.plit.common.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.net.URI;
import java.util.*;

@Slf4j
@Component
public class ChatHandler implements WebSocketHandler {

    private final Map<String, List<WebSocketSession>> roomSessions = new HashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        String query = uri != null ? uri.getQuery() : "";
        String roomId = getQueryValue(query, "roomId");
        String userId = getQueryValue(query, "userId");

        session.getAttributes().put("roomId", roomId);
        session.getAttributes().put("userId", userId);

        roomSessions.computeIfAbsent(roomId, k -> new ArrayList<>()).add(session);
        log.info("연결됨 - roomId: {}, userId: {}", roomId, userId);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String roomId = (String) session.getAttributes().get("roomId");
        String userId = (String) session.getAttributes().get("userId");
        String payload = (String) message.getPayload();

        log.info("메시지 - [{}] {}: {}", roomId, userId, payload);

        for (WebSocketSession sess : roomSessions.getOrDefault(roomId, new ArrayList<>())) {
            if (sess.isOpen()) {
                sess.sendMessage(new TextMessage(userId + ": " + payload));
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("에러 발생", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String roomId = (String) session.getAttributes().get("roomId");
        String userId = (String) session.getAttributes().get("userId");

        roomSessions.getOrDefault(roomId, new ArrayList<>()).remove(session);
        log.info("연결 종료 - roomId: {}, userId: {}", roomId, userId);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private String getQueryValue(String query, String key) {
        if (query == null) return "";
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals(key)) {
                return pair[1];
            }
        }
        return "";
    }
}