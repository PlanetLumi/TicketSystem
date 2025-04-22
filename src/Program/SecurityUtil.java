package Program;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SecurityUtil {
    // Holds the single AES key loaded from the keystore
    private static SecretKey aesKey;

    /**
     * Initialize the AES key once at startup.
     */
    public static void init(SecretKey key) {
        aesKey = key;
    }

    /**
     * Encrypts data using AES-GCM with a fresh nonce.
     * Returns nonce||ciphertext||tag.
     */
    public static byte[] encryptGcm(byte[] plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] nonce = new byte[12];
        new SecureRandom().nextBytes(nonce);
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);
        byte[] ciphertext = cipher.doFinal(plaintext);
        byte[] out = new byte[nonce.length + ciphertext.length];
        System.arraycopy(nonce, 0, out, 0, nonce.length);
        System.arraycopy(ciphertext, 0, out, nonce.length, ciphertext.length);
        return out;
    }

    /**
     * Decrypts data produced by encryptGcm: expects nonce||ciphertext||tag.
     */
    public static byte[] decryptGcm(byte[] input) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] nonce = Arrays.copyOfRange(input, 0, 12);
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);
        byte[] ciphertext = Arrays.copyOfRange(input, 12, input.length);
        return cipher.doFinal(ciphertext);
    }

    // -------------------------------
    // Password hashing (use a proper KDF instead of this in production)
    // -------------------------------
    public static String hashWithSalt(String password, String salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt.getBytes("UTF-8"));
        byte[] hashBytes = digest.digest(password.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
    public static String generateSaltedHash(String password) throws Exception {
        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] saltBytes = new byte[16]; sr.nextBytes(saltBytes);
        String salt = Base64.getEncoder().encodeToString(saltBytes);
        String hash = hashWithSalt(password, salt);
        return salt + "$" + hash;
    }
    public static boolean verifyPassword(String password, String stored) throws Exception {
        String[] parts = stored.split("\\$");
        if (parts.length != 2) throw new IllegalArgumentException("Invalid stored password format");
        return hashWithSalt(password, parts[0]).equals(parts[1]);
    }

    // -------------------------------
    // Misc utilities
    // -------------------------------
    public static boolean hasRequiredPrivileges(User user, SecurityLevel required) {
        return user.getSecurityLevel().ordinal() >= required.ordinal();
    }
    public static boolean isPasswordComplex(String password) {
        String pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return password.matches(pattern);
    }
    public static String sanitizeInput(String input) {
        return input.replaceAll("[^A-Za-z0-9 .,!?@_-]", "");
    }
    public static void setFilePermissions(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) return;
        try {
            Set<PosixFilePermission> perms = EnumSet.noneOf(PosixFilePermission.class);
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(path, perms);
        } catch (UnsupportedOperationException ignored) {}
    }
    public static boolean logEvent(String details, String logID) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String ip = getLocalIpAddress();
        String mac = getLocalMacAddress();
        String user = Optional.ofNullable(SessionManager.getInstance().getCurrentUser())
                .map(User::getUsername).orElse("anonymous");
        String record = String.join(",", timestamp, details, ip, mac, user);
        System.out.println("[AUDIT] " + record);
        String primary = FindAuditID.getAuditLogFileName(logID) + "primary.txt";
        String backup  = FindAuditID.getAuditLogFileName(logID) + "backup.txt";
        try (FileWriter w1 = new FileWriter(primary, true);
             FileWriter w2 = new FileWriter(backup, true)) {
            w1.write(record + System.lineSeparator());
            w2.write(record + System.lineSeparator());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    public static String getLocalIpAddress() {
        try { return InetAddress.getLocalHost().getHostAddress(); }
        catch (UnknownHostException e) { return "UNKNOWN_IP"; }
    }
    public static String getLocalMacAddress() {
        try {
            NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            if (ni == null) return "UNKNOWN_MAC";
            byte[] mac = ni.getHardwareAddress();
            if (mac == null) return "UNKNOWN_MAC";
            StringBuilder sb = new StringBuilder();
            for (byte b : mac) sb.append(String.format("%02X", b));
            return sb.toString();
        } catch (Exception e) {
            return "UNKNOWN_MAC";
        }
    }
    public static String readFileAsString(String path) throws IOException, IOException {
        return new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
    }
}
