package JUnit;

import Program.SecurityUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilTest {

    @BeforeAll
    static void initKey() {
        // Use a fixed 16‑byte zero key for AES‑GCM tests
        byte[] keyBytes = new byte[16];
        SecretKey aesKey = new SecretKeySpec(keyBytes, "AES");
        SecurityUtil.init(aesKey);
    }

    @Test
    void encryptThenDecryptReturnsOriginal() throws Exception {
        byte[] plaintext = "Hello, world!".getBytes();
        byte[] cipher = SecurityUtil.encryptGcm(plaintext);
        assertNotNull(cipher, "Cipher text should not be null");
        byte[] recovered = SecurityUtil.decryptGcm(cipher);
        assertArrayEquals(plaintext, recovered,
                "Decryption must return plain text");
    }

    @Test
    void generateSaltedHashAndVerify() throws Exception {
        String password = "S3cr3t!";
        String saltedHash = SecurityUtil.generateSaltedHash(password);
        assertTrue(saltedHash.contains("$"), "Salted hash should contain a '$' delimiter");
        assertTrue(SecurityUtil.verifyPassword(password, saltedHash));
        assertFalse(SecurityUtil.verifyPassword("wrong", saltedHash));
    }

    @Test
    void isPasswordComplexDetectsValidAndInvalid() {
        assertTrue(SecurityUtil.isPasswordComplex("Aa1@abcd"));
        assertFalse(SecurityUtil.isPasswordComplex("short1!"), "Too short");
        assertFalse(SecurityUtil.isPasswordComplex("NOCAPS1!"), "No lowercase");
        assertFalse(SecurityUtil.isPasswordComplex("nocaps1!"), "No uppercase");
        assertFalse(SecurityUtil.isPasswordComplex("NoNumber!"), "No digit");
    }

    @Test
    void sanitizeInputStripsIllegalCharacters() {
        String dirty = "<script>alert('x')</script>";
        String clean = SecurityUtil.sanitizeInput(dirty);
        assertFalse(clean.contains("<"));
        assertFalse(clean.contains(">"));
        // Allowed: letters, numbers, space, . , ! ? @ _ -
        String allowed = "Hello_1@test.com!";
        assertEquals(allowed, SecurityUtil.sanitizeInput(allowed));
    }
}
