package uy.edu.tse.hcen.utils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtils {

    // Lazy initialization holder pattern for thread-safe lazy loading
    private static class EncoderHolder {
        private static final BCryptPasswordEncoder INSTANCE = new BCryptPasswordEncoder();
    }

    // Private constructor to prevent instantiation
    private PasswordUtils() {
        // utility class
    }

    private static BCryptPasswordEncoder getEncoder() {
        return EncoderHolder.INSTANCE;
    }

    /**
     * Hashea la contraseña para almacenamiento seguro.
     */
    public static String hashPassword(String password) {
        return getEncoder().encode(password);
    }

    /**
     * Verifica una contraseña plana con el hash almacenado.
     */
    public static boolean verifyPassword(String rawPassword, String encodedPassword) {
        return getEncoder().matches(rawPassword, encodedPassword);
    }
}