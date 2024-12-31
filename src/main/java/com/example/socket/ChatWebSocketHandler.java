package com.example.socket;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;

public class ChatWebSocketHandler extends TextWebSocketHandler {

    // Quản lý người dùng: sessionId -> username
    private final ConcurrentHashMap<String, String> userSessions = new ConcurrentHashMap<>();
    // Quản lý session: username -> WebSocketSession
    private final ConcurrentHashMap<String, WebSocketSession> sessionsByUsername = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("------------------------------------------------");
        System.out.println("User connected: " + session.getId());
        System.out.println("------------------------------------------------");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("------------------------------------------------");
        System.out.println("Received: " + payload);
        System.out.println("------------------------------------------------");

        // Xử lý tin nhắn theo định dạng: "type:username:message"
        String[] parts = payload.split(":", 3);
        if (parts.length < 2) return;

        String type = parts[0]; // "join" hoặc "message"
        String target = parts[1]; // Tên người dùng (hoặc "all")
        String content = parts.length == 3 ? parts[2] : "";

        if (type.equals("join")) {
            // Gán username cho session
            userSessions.put(session.getId(), target);
            sessionsByUsername.put(target, session);
            System.out.println("------------------------------------------------");
            System.out.println(target + " joined.");
            System.out.println("------------------------------------------------");
        } else if (type.equals("message")) {
            if (target.equals("all")) {
                // Gửi tin nhắn broadcast cho tất cả người dùng
                for (WebSocketSession s : sessionsByUsername.values()) {
                    if (s.isOpen()) {
                        s.sendMessage(new TextMessage("Broadcast: " + content));
                    }
                }
            } else {
                // Gửi tin nhắn cho người dùng cụ thể
                WebSocketSession recipientSession = sessionsByUsername.get(target);
                if (recipientSession != null && recipientSession.isOpen()) {
                    recipientSession.sendMessage(new TextMessage("From " + userSessions.get(session.getId()) + ": " + content));
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        String username = userSessions.remove(session.getId());
        if (username != null) {
            sessionsByUsername.remove(username);
            System.out.println(username + " disconnected.");
        }
    }


}
