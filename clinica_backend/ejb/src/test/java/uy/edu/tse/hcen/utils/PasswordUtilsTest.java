package uy.edu.tse.hcen.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilsTest {

    @Test
    void testHashPassword() {
        String password = "testPassword123";
        String hash = PasswordUtils.hashPassword(password);
        
        assertNotNull(hash);
        assertNotEquals(password, hash);
        assertTrue(hash.length() > 0);
    }

    @Test
    void testHashPasswordEmpty() {
        String password = "";
        String hash = PasswordUtils.hashPassword(password);
        
        assertNotNull(hash);
        assertNotEquals(password, hash);
    }

    @Test
    void testHashPasswordNull() {
        // BCrypt lanza IllegalArgumentException, no NullPointerException
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordUtils.hashPassword(null);
        });
    }

    @Test
    void testHashPasswordDifferentHashes() {
        String password = "samePassword";
        String hash1 = PasswordUtils.hashPassword(password);
        String hash2 = PasswordUtils.hashPassword(password);
        
        // BCrypt genera hashes diferentes cada vez (salt incluido)
        assertNotEquals(hash1, hash2);
    }

    @Test
    void testVerifyPasswordCorrect() {
        String password = "testPassword123";
        String hash = PasswordUtils.hashPassword(password);
        
        assertTrue(PasswordUtils.verifyPassword(password, hash));
    }

    @Test
    void testVerifyPasswordIncorrect() {
        String password = "testPassword123";
        String hash = PasswordUtils.hashPassword(password);
        String wrongPassword = "wrongPassword";
        
        assertFalse(PasswordUtils.verifyPassword(wrongPassword, hash));
    }

    @Test
    void testVerifyPasswordEmpty() {
        String password = "";
        String hash = PasswordUtils.hashPassword(password);
        
        assertTrue(PasswordUtils.verifyPassword(password, hash));
    }

    @Test
    void testVerifyPasswordWithNullPassword() {
        String hash = PasswordUtils.hashPassword("test");
        
        // BCrypt lanza IllegalArgumentException, no NullPointerException
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordUtils.verifyPassword(null, hash);
        });
    }

    @Test
    void testVerifyPasswordWithNullHash() {
        // Si el hash es null, verifyPassword retorna false, no lanza excepción
        assertFalse(PasswordUtils.verifyPassword("password", null));
    }

    @Test
    void testVerifyPasswordWithInvalidHash() {
        String invalidHash = "invalidHash";
        
        assertFalse(PasswordUtils.verifyPassword("password", invalidHash));
    }

    @Test
    void testHashPasswordSpecialCharacters() {
        String password = "p@ssw0rd!#$%^&*()";
        String hash = PasswordUtils.hashPassword(password);
        
        assertNotNull(hash);
        assertTrue(PasswordUtils.verifyPassword(password, hash));
    }

    @Test
    void testHashPasswordUnicode() {
        String password = "contraseña123";
        String hash = PasswordUtils.hashPassword(password);
        
        assertNotNull(hash);
        assertTrue(PasswordUtils.verifyPassword(password, hash));
    }

    @Test
    void testHashPasswordLong() {
        String password = "a".repeat(200);
        String hash = PasswordUtils.hashPassword(password);
        
        assertNotNull(hash);
        assertTrue(PasswordUtils.verifyPassword(password, hash));
    }

    @Test
    void testHashPasswordVeryLong() {
        String password = "a".repeat(1000);
        String hash = PasswordUtils.hashPassword(password);
        
        assertNotNull(hash);
        assertTrue(PasswordUtils.verifyPassword(password, hash));
    }

    @Test
    void testHashPasswordWithSpaces() {
        String password = "password with spaces";
        String hash = PasswordUtils.hashPassword(password);
        
        assertNotNull(hash);
        assertTrue(PasswordUtils.verifyPassword(password, hash));
    }

    @Test
    void testHashPasswordCaseSensitive() {
        String password1 = "Password123";
        String password2 = "password123";
        
        String hash1 = PasswordUtils.hashPassword(password1);
        String hash2 = PasswordUtils.hashPassword(password2);
        
        assertNotEquals(hash1, hash2);
        assertTrue(PasswordUtils.verifyPassword(password1, hash1));
        assertTrue(PasswordUtils.verifyPassword(password2, hash2));
        assertFalse(PasswordUtils.verifyPassword(password1, hash2));
        assertFalse(PasswordUtils.verifyPassword(password2, hash1));
    }

    @Test
    void testMultiplePasswords() {
        String[] passwords = {
            "password1",
            "password2",
            "password3",
            "test123",
            "admin"
        };
        
        for (String password : passwords) {
            String hash = PasswordUtils.hashPassword(password);
            assertNotNull(hash);
            assertTrue(PasswordUtils.verifyPassword(password, hash));
            assertFalse(PasswordUtils.verifyPassword("wrong", hash));
        }
    }
}

