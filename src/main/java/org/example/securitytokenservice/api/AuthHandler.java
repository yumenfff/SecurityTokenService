package org.example.securitytokenservice.api;

import com.sun.net.httpserver.HttpExchange;
import org.example.securitytokenservice.dto.ApiDtos;
import org.example.securitytokenservice.model.User;
import org.example.securitytokenservice.service.AuthService;

public class AuthHandler extends BaseHandler {
    public AuthHandler(AuthService authService) {
        super(authService);
    }

    @Override
    protected int handleRequest(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        if (path.endsWith("/register") && isPost(exchange)) {
            return register(exchange);
        }
        if (path.endsWith("/login") && isPost(exchange)) {
            return login(exchange);
        }
        throw new org.example.securitytokenservice.error.ApiException(404, "Endpoint not found");
    }

    private int register(HttpExchange exchange) throws Exception {
        ApiDtos.RegisterRequest request = readJson(exchange, ApiDtos.RegisterRequest.class);
        User user = authService.register(request.login(), request.password(), request.role());
        sendJson(exchange, 201, new ApiDtos.UserResponse(user.id(), user.login(), user.role().name(), user.createdAt()));
        return 201;
    }

    private int login(HttpExchange exchange) throws Exception {
        ApiDtos.LoginRequest request = readJson(exchange, ApiDtos.LoginRequest.class);
        var token = this.authService.login(request.login(), request.password());
        var user = this.authService.getTokenService().verify(token.token());
        if (user == null) {
            throw new IllegalStateException("Issued token failed verification");
        }
        sendJson(exchange, 200, new ApiDtos.TokenResponse(token.token(), "Bearer", user.role().name(), token.expiresAt()));
        return 200;
    }
}
