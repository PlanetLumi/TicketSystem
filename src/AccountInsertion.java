import java.util.ArrayList;
import java.util.List;

public class AccountInsertion {

    public static void main(String[] args) {
        // List to store the created accounts.
        List<User> accounts = new ArrayList<>();

        try {
            // Use salted hashing for passwords.
            String adminSaltedHash = SecurityUtil.generateSaltedHash("adminPass");
            String techSaltedHash = SecurityUtil.generateSaltedHash("techPass");
            String endUserSaltedHash = SecurityUtil.generateSaltedHash("endPass");

            // Sanitize usernames to ensure only valid characters are used.
            String adminUsername = SecurityUtil.sanitizeInput("adminUser");
            String techUsername = SecurityUtil.sanitizeInput("techUser");
            String endUsername = SecurityUtil.sanitizeInput("endUser");

            // Create accounts with roles and security levels.
            // (Ensure your User class accepts these parameters.)
            User adminAccount = new User(adminUsername, adminSaltedHash, UserRole.ADMIN, SecurityLevel.TOPLEVEL);
            User technicianAccount = new User(techUsername, techSaltedHash, UserRole.TECHNICIAN, SecurityLevel.ADMIN);
            User endUserAccount = new User(endUsername, endUserSaltedHash, UserRole.END_USER, SecurityLevel.BASE);

            accounts.add(adminAccount);
            accounts.add(technicianAccount);
            accounts.add(endUserAccount);

            // Log the account creation event using audit logging.
            SecurityUtil.logEvent("Created admin account for " + adminAccount.getUsername());
            SecurityUtil.logEvent("Created technician account for " + technicianAccount.getUsername());
            SecurityUtil.logEvent("Created end-user account for " + endUserAccount.getUsername());

            // Print inserted accounts and their security details.
            System.out.println("Inserted Accounts:");
            for (User account : accounts) {
                System.out.println("Username: " + account.getUsername() +
                        " | Role: " + account.getRole() +
                        " | Security Level: " + account.getSecurityLevel());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
