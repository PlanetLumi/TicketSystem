package Program;

import java.util.List;

public class RegistrationSystem {

    private static final String ACCOUNTS_FILE = "accounts.csv";
    private static final String ACCOUNTS_FILE_COPY = "accounts2.csv";
    /**
     * Registers a new user securely.
     *
     * @param username The new user's username.
     * @param password The plain-text password.
     * @param role     The desired role for the account (e.g., ADMIN, TECHNICIAN, END_USER).
     * @param level    The desired security level for the account (e.g., BASE, ADMIN, TOPLEVEL).
     * @return True if registration succeeds; false otherwise.
     */
    public static boolean registerUser(String username, String password, UserRole role, SecurityLevel level) {
        try {
            if(!SecurityUtil.isPasswordComplex(password)) {
                System.out.println("Error: Password is not complex enough.");
                return false;
            }
            //  Sanitize the username to remove any unwanted characters.
            String sanitizedUsername = SecurityUtil.sanitizeInput(username);

            // Load persisted accounts from file.
            List<User> accounts = AccountPersistence.loadAccountsFromFile(ACCOUNTS_FILE);

            // Check if the username already exists (ignoring case).
            for (User existingUser : accounts) {
                if (existingUser.getUsername().equalsIgnoreCase(sanitizedUsername)) {
                    System.out.println("Error: Username '" + sanitizedUsername + "' already exists.");
                    return false;
                }
            }

            // SECURITY CHECK:
            // Get the current logged-in user from the Program.SessionManager.
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                // Self-registration: allow creation only of BASE accounts.
                if (level != SecurityLevel.BASE) {
                    System.out.println("Self-registration error: You can only create an account with BASE security level.");
                    return false;
                }
            } else {
                // If a user is logged in, they must not create an account with a higher security level than theirs.
                if (level.ordinal() > currentUser.getSecurityLevel().ordinal()) {
                    System.out.println("Error: You cannot create an account with a higher security level than your own.");
                    return false;
                }
            }

            // Generate a salted hash of the password.
            String saltedHash = SecurityUtil.generateSaltedHash(password);

            // Create the new user account.
            User newUser = new User(sanitizedUsername, saltedHash, role, level);
            if(SecurityUtil.logEvent("Registered new user: " + sanitizedUsername, "REGISTERED" )) {

                // Add the new user to the accounts list.
                accounts.add(newUser);

                // Persist the updated accounts list to the file.
                AccountPersistence.saveAccountsToFile(accounts, ACCOUNTS_FILE);
                AccountPersistence.saveAccountsToFile(accounts, ACCOUNTS_FILE_COPY);
            } else {
                System.out.println("Error: COULD NOT LOG CREATION, PROCCESS FAILED.");
                return false;
            }
            // Log the registration event.

            System.out.println("Registration successful for user: " + sanitizedUsername);
            return true;
        } catch (Exception e) {
            System.out.println("Registration failed due to an error:");
            e.printStackTrace();
            return false;
        }
    }

}
