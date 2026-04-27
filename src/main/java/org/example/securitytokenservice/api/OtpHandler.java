package org.example.securitytokenservice.api;

import com.sun.net.httpserver.HttpExchange;
import org.example.securitytokenservice.dto.ApiDtos;
import org.example.securitytokenservice.error.ApiException;
import org.example.securitytokenservice.model.DeliveryChannelType;
import org.example.securitytokenservice.model.User;
import org.example.securitytokenservice.service.AuthService;
import org.example.securitytokenservice.service.OtpService;

public class OtpHandler extends BaseHandler {
    private final OtpService otpService;

    public OtpHandler(AuthService authService, OtpService otpService) {
        super(authService);
        this.otpService = otpService;
    }

    @Override
    protected int handleRequest(HttpExchange exchange) throws Exception {
        User user = authenticateUser(exchange);

        String path = exchange.getRequestURI().getPath();
        if (path.endsWith("/generate") && isPost(exchange)) {
            return generate(exchange, user);
        }
        if (path.endsWith("/validate") && isPost(exchange)) {
            return validate(exchange, user);
        }

        throw new ApiException(404, "Endpoint not found");
    }

    private int generate(HttpExchange exchange, User user) throws Exception {
        ApiDtos.GenerateOtpRequest request = readJson(exchange, ApiDtos.GenerateOtpRequest.class);
        DeliveryChannelType channelType = DeliveryChannelType.from(request.channel());
        OtpService.GeneratedOtp generatedOtp = otpService.generate(user, request.operationId(), request.destination(), channelType);
        sendJson(exchange, 201, new ApiDtos.MessageResponse("OTP generated: id=" + generatedOtp.otpId() + ", expiresAt=" + generatedOtp.expiresAt()));
        return 201;
    }

    private int validate(HttpExchange exchange, User user) throws Exception {
        ApiDtos.ValidateOtpRequest request = readJson(exchange, ApiDtos.ValidateOtpRequest.class);
        otpService.validate(user, request.operationId(), request.code());
        sendJson(exchange, 200, new ApiDtos.MessageResponse("OTP validated"));
        return 200;
    }
}
