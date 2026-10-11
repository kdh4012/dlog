package com.dlog.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * ADMIN_PASSWORD_HASH 생성용 CLI.
 * 사용법: ./gradlew hashPassword -Ppw=비밀번호
 */
public final class PasswordHashGenerator {

    private PasswordHashGenerator() {
    }

    public static void main(String[] args) {
        if (args.length != 1 || args[0].isBlank()) {
            System.err.println("Usage: ./gradlew hashPassword -Ppw=<password>");
            System.exit(1);
        }
        System.out.println(new BCryptPasswordEncoder().encode(args[0]));
    }
}
