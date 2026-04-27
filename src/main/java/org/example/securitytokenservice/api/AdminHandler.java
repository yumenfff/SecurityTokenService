package org.example.securitytokenservice.api;

import com.sun.net.httpserver.HttpExchange;
import org.example.securitytokenservice.dto.ApiDtos;
import org.example.securitytokenservice.error.ApiException;
import org.example.securitytokenservice.model.OtpConfig;
import org.example.securitytokenservice.model.User;
import org.example.securitytokenservice.service.AdminService;
import org.example.securitytokenservice.service.AuthService;

import java.util.List;

public class AdminHandler extends BaseHandler {
    private final AdminService adminService;

    public AdminHandler(AuthService authService, AdminService adminService) {
        super(authService);
        this.adminService = adminService;
    }

    @Override
    protected int handleRequest(HttpExchange exchange) throws Exception {
        User user = authenticateUser(exchange);
        requireAdmin(user);

        String path = exchange.getRequestURI().getPath();
        if (path.endsWith("/otp-config") && isPut(exchange)) {
            return updateConfig(exchange);
        }
        if (path.endsWith("/users") && isGet(exchange)) {
            return listUsers(exchange);
        }
        if (path.contains("/users/") && isDelete(exchange)) {
            return deleteUser(exchange);
        }

        throw new ApiException(404, "Endpoint not found");
    }

    private int updateConfig(HttpExchange exchange) throws Exception {
        ApiDtos.UpdateOtpConfigRequest request = readJson(exchange, ApiDtos.UpdateOtpConfigRequest.class);
        OtpConfig config = adminService.updateOtpConfig(request.ttlSeconds(), request.codeLength());
        sendJson(exchange, 200, new ApiDtos.OtpConfigResponse(config.ttlSeconds(), config.codeLength(), config.updatedAt()));
        return 200;
    }

    private int listUsers(HttpExchange exchange) throws Exception {
        List<User> users = adminService.listNonAdminUsers();
        List<ApiDtos.UserResponse> responses = users.stream()
                .map(user -> new ApiDtos.UserResponse(user.id(), user.login(), user.role().name(), user.createdAt()))
                .toList();
        sendJson(exchange, 200, responses);
        return 200;
    }

    private int deleteUser(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String idPart = path.substring(path.lastIndexOf('/') + 1);
        long userId = Long.parseLong(idPart);
        adminService.deleteUser(userId);
        sendJson(exchange, 200, new ApiDtos.MessageResponse("User deleted"));
        return 200;
    }
}
