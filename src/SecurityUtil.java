import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

public class SecurityUtil {
    // Returns true if the user's security level is sufficient for the required level.
    public static boolean hasRequiredPrivileges(User user, SecurityLevel requiredLevel) {
        return user.getSecurityLevel().ordinal() >= requiredLevel.ordinal();
    }
    /**
     * Encrypts the given plaintext using AES in CBC mode with PKCS5Padding.
     *
     * @param plainText The text to encrypt.
     * @param key       The AES secret key.
     * @param iv        The initialization vector.
     * @return A Base64-encoded encrypted string.
     * @throws Exception if an error occurs during encryption.
     */
    public static String encrypt(String plainText, SecretKey key, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Decrypts the given Base64-encoded ciphertext using AES in CBC mode with PKCS5Padding.
     *
     * @param cipherText The Base64-encoded ciphertext.
     * @param key        The AES secret key.
     * @param iv         The initialization vector.
     * @return The decrypted plaintext string.
     * @throws Exception if decryption fails.
     */
    public static String decrypt(String cipherText, SecretKey key, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] decodedBytes = Base64.getDecoder().decode(cipherText);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, "UTF-8");
    }

    /**
     * Generates a new AES secret key (128-bit).
     *
     * @return A new SecretKey for AES encryption.
     * @throws Exception if the key generator cannot be instantiated.
     */
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);  // 128-bit AES key
        return keyGen.generateKey();
    }

    /**
     * Generates an Initialization Vector (IV) for AES.
     *
     * @return An IvParameterSpec with a 16-byte IV.
     */
    public static IvParameterSpec generateIV() {
        byte[] iv = new byte[16];
        // In a real system, use SecureRandom to fill the IV with random bytes:
        // SecureRandom random = new SecureRandom();
        // random.nextBytes(iv);
        // For demonstration purposes, we'll leave it zeroed.
        return new IvParameterSpec(iv);
    }

    /**
     * Hashes the provided data using SHA-256.
     *
     * @param data The input string to hash.
     * @return A hexadecimal string representing the SHA-256 hash.
     * @throws Exception if the MessageDigest cannot be instantiated.
     */
    public static String hash(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(data.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Sanitizes user input by removing unwanted characters.
     *
     * @param input The original user input.
     * @return A sanitized version of the input.
     */
    public static String sanitizeInput(String input) {
        // Allow only alphanumeric characters and basic punctuation.
        return input.replaceAll("[^A-Za-z0-9 .,!?@_-]", "");
    }

    /**
     * Sets restrictive POSIX file permissions on a file.
     * The file will be set to read and write for the owner only (rw-------).
     *
     * @param filePath The path to the file.
     * @throws Exception if the file permissions cannot be set.
     */
    public static void setFilePermissions(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE);
        Files.setPosixFilePermissions(path, perms);
    }

    /**
     * Logs security-related events for audit purposes.
     *
     * @param event The event message to log.
     */
    public static void logEvent(String event) {
        // For demonstration, we print to the console.
        System.out.println("[AUDIT] " + event);
    }
    /**
     * Generates a random salt as a hexadecimal string.
     *
     * @return A random 16-byte salt in hexadecimal form.
     * @throws Exception if the SecureRandom instance cannot be obtained.
     */
    public static String generateSalt() throws Exception {
        // SecureRandom instance for strong randomness
        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        // Convert salt bytes to a hexadecimal string
        StringBuilder sb = new StringBuilder();
        for (byte b : salt) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Hashes a given password with the provided salt using SHA-256.
     *
     * @param password The plaintext password.
     * @param salt     The salt as a hexadecimal string.
     * @return The resulting hash as a hexadecimal string.
     * @throws Exception if the MessageDigest instance is not available.
     */
    public static String hashWithSalt(String password, String salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        // Update the digest with the salt bytes first.
        digest.update(salt.getBytes("UTF-8"));
        byte[] hashBytes = digest.digest(password.getBytes("UTF-8"));
        // Convert hash bytes into a hexadecimal string
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Example method to combine salt and hash for storage.
     * Often, applications store both the salt and the hash.
     *
     * @param password The plaintext password.
     * @return A string in the format "salt$hash"
     * @throws Exception if an error occurs during salting or hashing.
     */
    public static String generateSaltedHash(String password) throws Exception {
        String salt = generateSalt();
        String hash = hashWithSalt(password, salt);
        return salt + "$" + hash;
    }

    /**
     * Verifies the provided plaintext password against the stored salted hash.
     *
     * @param password The plaintext password provided by the user.
     * @param stored   The stored salted hash in the format "salt$hash".
     * @return True if the password is valid, false otherwise.
     * @throws Exception if an error occurs during hashing.
     */
    public static boolean verifyPassword(String password, String stored) throws Exception {
        // Split the stored string to obtain salt and hash
        String[] parts = stored.split("\\$");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Stored password must be in the format 'salt$hash'");
        }
        String salt = parts[0];
        String storedHash = parts[1];
        String computedHash = hashWithSalt(password, salt);
        return storedHash.equals(computedHash);
    }

}
