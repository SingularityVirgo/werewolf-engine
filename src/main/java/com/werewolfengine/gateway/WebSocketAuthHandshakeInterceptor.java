package com.werewolfengine.gateway;

import com.werewolfengine.gateway.auth.TokenStore;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_TOKEN = "token";

    private final TokenStore tokenStore;

    public WebSocketAuthHandshakeInterceptor(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String token = extractToken(request);
        attributes.put(ATTR_TOKEN, token);
        if (token != null) {
            tokenStore.resolve(token).ifPresent(userId -> attributes.put(ATTR_USER_ID, userId));
        }
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // no-op
    }

    static String extractToken(ServerHttpRequest request) {
        String query = request.getURI().getQuery();
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String part : query.split("&")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            if ("token".equals(part.substring(0, eq))) {
                return part.substring(eq + 1);
            }
        }
        return null;
    }
}
