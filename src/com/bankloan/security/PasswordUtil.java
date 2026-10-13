package com.bankloan.security;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PBKDF2 with HMAC-SHA256 slow password hashing utility.
 * Recommended by NIST for secure password storage.
 */
public class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_BYTES = 16;  // 128 bits

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a cryptographically strong random salt encoded as Base64.
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes a password using PBKDF2WithHmacSHA256 and the provided salt.
     */
    public static String hashPassword(String password, String saltBase64) {
        if (password == null || saltBase64 == null) {
            throw new IllegalArgumentException("Password and salt must not be null");
        }
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error calculating PBKDF2 password hash", e);
        }
    }

    /**
     * Verifies a plain text candidate password against the stored salt and hash.
     * Uses constant-time equality check to prevent timing attacks.
     */
    public static boolean verifyPassword(String candidatePassword, String storedHashBase64, String saltBase64) {
        if (candidatePassword == null || storedHashBase64 == null || saltBase64 == null) {
            return false;
        }
        String computedHash = hashPassword(candidatePassword, saltBase64);
        return slowEquals(
            Base64.getDecoder().decode(computedHash),
            Base64.getDecoder().decode(storedHashBase64)
        );
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
