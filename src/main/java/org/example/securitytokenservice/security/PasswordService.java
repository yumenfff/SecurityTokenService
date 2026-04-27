package org.example.securitytokenservice.security;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordService {
    private PasswordService() {
    }

    public static String hash(String value) {
        return BCrypt.hashpw(value, BCrypt.gensalt(10));
    }

    public static boolean matches(String value, String hash) {
        return BCrypt.checkpw(value, hash);
    }
}
