package uy.edu.tse.hcen.utils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtils {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // Private constructor to prevent instantiation
    private PasswordUtils() {
        // utility class
    }

    /**
     * Hashea la contraseña para almacenamiento seguro.
     */
    public static String hashPassword(String password) {
        return encoder.encode(password);
    }

    /**
     * Verifica una contraseña plana con el hash almacenado.
     */
    public static boolean verifyPassword(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}