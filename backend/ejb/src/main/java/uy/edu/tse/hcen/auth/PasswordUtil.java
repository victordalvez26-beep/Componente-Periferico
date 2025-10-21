package uy.edu.tse.hcen.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Simple PBKDF2 password utilities.
 * Note: This is intentionally small and meant for demo/local use. Prefer a well-tested
 * library such as BCrypt or Argon2 in production.
 */
public final class PasswordUtil {
    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256;

    private PasswordUtil() {}

    public static String generatePBKDF2Hash(String password) {
        try {
            byte[] salt = new byte[16];
            SecureRandom sr = SecureRandom.getInstanceStrong();
            sr.nextBytes(salt);
            byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            // store as iterations:salt:hash (base64)
            return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean verifyPBKDF2(String password, String stored) {
        try {
            String[] parts = stored.split(":" );
            int it = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] hash = Base64.getDecoder().decode(parts[2]);
            byte[] attempted = pbkdf2(password.toCharArray(), salt, it, hash.length * 8);
            if (attempted.length != hash.length) return false;
            int diff = 0;
            for (int i = 0; i < hash.length; i++) diff |= hash[i] ^ attempted[i];
            return diff == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }
}
