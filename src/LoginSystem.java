import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class LoginSystem {

    /**
     * Attempts to log in a user by reading the persisted accounts file ("accounts.csv")
     * and validating the provided credentials. Only one user can log in at a time.
     *
     * @param username The username entered by the user.
     * @param password The plain-text password entered.
     * @return True if login succeeds; false otherwise.
     */
    public boolean login(String username, String password) throws IOException {
        if (SecurityUtil.logEvent("LOGIN ATTEMPT: " + username, "ATTEMPT")) {
            User user = validateUser(username, password);
            if (user != null) {
                if (SecurityUtil.logEvent("Account Login " + user.getUsername(), "LOGIN")) {
                    // Set the current user in the session (only one active session allowed)
                    SessionManager.getInstance().setCurrentUser(user);
                    System.out.println("Login successful. Welcome, " + user.getUsername());
                    return true;
                } else {
                    System.out.println("System Log failed, account login failed.");
                    return false;
                }
            } else {
                System.out.println("Login failed.");
                return false;
            }
        } else {
            System.out.println("Login Attempt file log failed.");
            return false;
        }
    }

    /**
     * Validates the entered username and password by scanning the accounts file.
     * This method reads the file "accounts.csv" line by line (without loading all accounts into memory)
     * and checks if the provided credentials match an entry.
     *
     * @param username The entered username.
     * @param password The entered plain-text password.
     * @return The corresponding User object if the credentials are valid; otherwise null.
     */
    private User validateUser(String username, String password) {
        try (BufferedReader reader = new BufferedReader(new FileReader("accounts.csv"))) {
            String line;
            // Read header line (assumed to be present) and ignore it.
            line = reader.readLine();
            if (line == null) {
                System.out.println("Account file is empty.");
                return null;
            }
            while ((line = reader.readLine()) != null) {
                // Expected CSV format: username,role,securityLevel,passwordHash
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String fileUsername = parts[0];
                    String roleString = parts[1];
                    String securityLevelString = parts[2];
                    String storedPasswordHash = parts[3];
                    if (fileUsername.equals(username)) {
                        // Use the security utility to verify the plain-text password against the stored salted hash.
                        if (SecurityUtil.verifyPassword(password, storedPasswordHash)) {
                            // Create a User object from the file data.
                            User user = new User(
                                    fileUsername,
                                    storedPasswordHash,
                                    UserRole.valueOf(roleString),
                                    SecurityLevel.valueOf(securityLevelString)
                            );
                            return user;
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading accounts file:");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Error during password verification:");
            e.printStackTrace();
        }
        return null;
    }
    public void logout() throws IOException {
        // Retrieve the current user from the SessionManager.
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            String username = currentUser.getUsername();
            // Log the logout event with the username.
            SecurityUtil.logEvent("Account logout: " + username, "LOGOUT");
            // Clear the current user session.
            SessionManager.getInstance().clearSession();
            System.out.println("User " + username + " has been logged out.");
        } else {
            System.out.println("No user is currently logged in.");
        }
    }

}
