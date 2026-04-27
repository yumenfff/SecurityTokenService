package org.example.securitytokenservice.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.securitytokenservice.model.Role;
import org.example.securitytokenservice.model.User;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;

public class TokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private final byte[] secret;
    private final long tokenTtlMinutes;
    private final Clock clock;

    public TokenService(String secret, long tokenTtlMinutes) {
        this(secret, tokenTtlMinutes, Clock.systemUTC());
    }

    public TokenService(String secret, long tokenTtlMinutes, Clock clock) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.tokenTtlMinutes = tokenTtlMinutes;
        this.clock = clock;
    }

    public IssuedToken issueToken(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(tokenTtlMinutes, ChronoUnit.MINUTES);

        String headerJson = toJson(Map.of(
                "alg", "HS256",
                "typ", "JWT"
        ));
        String payloadJson = toJson(Map.of(
                "sub", user.id(),
                "login", user.login(),
                "role", user.role().name(),
                "iat", issuedAt.getEpochSecond(),
                "exp", expiresAt.getEpochSecond()
        ));

        String header = encode(headerJson);
        String payload = encode(payloadJson);
        String signingInput = header + "." + payload;
        String token = signingInput + "." + sign(signingInput);
        return new IssuedToken(token, expiresAt);
    }

    public TokenClaims verify(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String headerJson = decode(parts[0]);
            String payloadJson = decode(parts[1]);
            String signingInput = parts[0] + "." + parts[1];

            byte[] expectedSignature = signBytes(signingInput);
            byte[] actualSignature = decodeBytes(parts[2]);
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                return null;
            }

            Map<String, Object> header = MAPPER.readValue(headerJson, MAP_TYPE);
            if (!"JWT".equals(String.valueOf(header.get("typ"))) || !"HS256".equals(String.valueOf(header.get("alg")))) {
                return null;
            }

            Map<String, Object> payload = MAPPER.readValue(payloadJson, MAP_TYPE);
            long userId = toLong(payload.get("sub"));
            String login = toString(payload.get("login"));
            Role role = Role.valueOf(toString(payload.get("role")));
            Instant issuedAt = Instant.ofEpochSecond(toLong(payload.get("iat")));
            Instant expiresAt = Instant.ofEpochSecond(toLong(payload.get("exp")));
            Instant now = clock.instant();
            if (expiresAt.isBefore(now) || issuedAt.isAfter(now.plusSeconds(5))) {
                return null;
            }

            return new TokenClaims(userId, login, role, issuedAt, expiresAt);
        } catch (Exception ex) {
            return null;
        }
    }

    private String sign(String payload) {
        return encode(signBytes(payload));
    }

    private byte[] signBytes(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign token", e);
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static String decode(String value) {
        return new String(decodeBytes(value), StandardCharsets.UTF_8);
    }

    private static byte[] decodeBytes(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private static String toJson(Map<String, Object> payload) {
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize token JSON", e);
        }
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("Invalid numeric claim value");
    }

    private static String toString(Object value) {
        if (value instanceof String s && !s.isBlank()) {
            return s;
        }
        throw new IllegalArgumentException("Invalid string claim value");
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
