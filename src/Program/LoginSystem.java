package Program;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class LoginSystem {

   //Set the current session to user details and return boolean
    public static boolean login(String username, String password) throws IOException {
        //Log attempt
        if (SecurityUtil.logEvent("LOGIN ATTEMPT: " + username, "ATTEMPT")) {
            //checks against saved details from given login parameters
            User user = validateUser(username, password);
            if (user != null) {
                //Do not allow login if not logged into audit, even if standard, security standard
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


    //Validate users by given details
    private static User validateUser(String username, String password) {
        //Find main account filebase
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
                        // Security util verifys against hash which cannot be done directly
                        if (SecurityUtil.verifyPassword(password, storedPasswordHash)) {
                            // Create a user object from the file data.
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
    //Log user out
    public static void logout() throws IOException {
        // Retrieve the current user from the sessionManager.
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            String username = currentUser.getUsername();
            // Log the logout event with the username.
            SecurityUtil.logEvent("Account logout: " + username, "LOGOUT");
            // Clear the current user session.
            SessionManager.getInstance().clearSession();
            System.out.println("Program.User " + username + " has been logged out.");
        } else {
            System.out.println("No user is currently logged in.");
        }
    }

}
