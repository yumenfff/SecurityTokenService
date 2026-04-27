package org.example.securitytokenservice.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.securitytokenservice.dto.ApiDtos;
import org.example.securitytokenservice.error.ApiException;
import org.example.securitytokenservice.model.Role;
import org.example.securitytokenservice.model.User;
import org.example.securitytokenservice.security.TokenClaims;
import org.example.securitytokenservice.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class BaseHandler implements HttpHandler {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    protected final AuthService authService;

    protected BaseHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        int status = 500;
        try {
            status = handleRequest(exchange);
        } catch (ApiException e) {
            status = e.statusCode();
            sendJson(exchange, status, new ApiDtos.ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unhandled error", e);
            sendJson(exchange, status, new ApiDtos.ErrorResponse("Internal server error"));
        } finally {
            log.info("{} {} -> {}", exchange.getRequestMethod(), exchange.getRequestURI().getPath(), status);
        }
    }

    protected abstract int handleRequest(HttpExchange exchange) throws Exception;

    protected <T> T readJson(HttpExchange exchange, Class<T> type) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            byte[] data = inputStream.readAllBytes();
            if (data.length == 0) {
                throw new ApiException(400, "Request body is empty");
            }
            return mapper.readValue(data, type);
        }
    }

    protected void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] data = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(data);
        }
    }

    protected User authenticateUser(HttpExchange exchange) {
        TokenClaims claims = authenticateClaims(exchange);
        return authService.requireExistingUser(claims);
    }

    private TokenClaims authenticateClaims(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ApiException(401, "Missing bearer token");
        }
        String token = header.substring("Bearer ".length()).trim();
        TokenClaims claims = authService.getTokenService().verify(token);
        if (claims == null) {
            throw new ApiException(401, "Invalid or expired token");
        }
        return claims;
    }

    protected void requireAdmin(User user) {
        if (user.role() != Role.ADMIN) {
            throw new ApiException(403, "Access denied");
        }
    }

    protected static boolean isPost(HttpExchange exchange) {
        return "POST".equalsIgnoreCase(exchange.getRequestMethod());
    }

    protected static boolean isPut(HttpExchange exchange) {
        return "PUT".equalsIgnoreCase(exchange.getRequestMethod());
    }

    protected static boolean isGet(HttpExchange exchange) {
        return "GET".equalsIgnoreCase(exchange.getRequestMethod());
    }

    protected static boolean isDelete(HttpExchange exchange) {
        return "DELETE".equalsIgnoreCase(exchange.getRequestMethod());
    }
}
