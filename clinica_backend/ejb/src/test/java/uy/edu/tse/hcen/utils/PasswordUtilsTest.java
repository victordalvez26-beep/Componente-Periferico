package uy.edu.tse.hcen.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilsTest {

    @Test
    void testHashPasswordGeneratesDifferentHashes() {
        String password = "mySecurePassword123";
        String hash1 = PasswordUtils.hashPassword(password);
        String hash2 = PasswordUtils.hashPassword(password);
        
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotEquals(hash1, hash2); // Different salts
        assertTrue(hash1.startsWith("$2a$")); // BCrypt format
        assertTrue(hash2.startsWith("$2a$"));
    }
    
    @Test
    void testVerifyPasswordWithCorrectPassword() {
        String password = "testPassword456";
        String hash = PasswordUtils.hashPassword(password);
        
        assertTrue(PasswordUtils.verifyPassword(password, hash));
    }
    
    @Test
    void testVerifyPasswordWithIncorrectPassword() {
        String password = "correctPassword";
        String wrongPassword = "wrongPassword";
        String hash = PasswordUtils.hashPassword(password);
        
        assertFalse(PasswordUtils.verifyPassword(wrongPassword, hash));
    }
}

