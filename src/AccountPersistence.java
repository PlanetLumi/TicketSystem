import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AccountPersistence {

    /**
     * Saves the list of user accounts to a CSV file.
     * Each line will contain: username, role, securityLevel.
     *
     * @param accounts The list of User accounts.
     * @param filePath The file path where the account details are saved.
     */
    public static void saveAccountsToFile(List<User> accounts, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header line (optional)
            writer.write("username,role,securityLevel\n");
            for (User account : accounts) {
                writer.write(account.getUsername() + ","
                        + account.getRole() + ","
                        + account.getSecurityLevel() + "\n");
            }
            System.out.println("Accounts saved successfully to " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the list of user accounts from a CSV file.
     * Assumes the file has a header and that each subsequent line
     * contains: username,role,securityLevel.
     *
     * @param filePath The file path to read from.
     * @return A List of User objects.
     */
    public static List<User> loadAccountsFromFile(String filePath) {
        List<User> accounts = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Read header and ignore it
            if ((line = reader.readLine()) == null) {
                System.out.println("File is empty!");
                return accounts;
            }
            // Process each subsequent line
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String username = parts[0];
                    // For simplicity, we are not restoring the passwordHash here.
                    // In a full solution, you'd store and read the hashed password as well.
                    UserRole role = UserRole.valueOf(parts[1]); // assumes exact enum names
                    SecurityLevel securityLevel = SecurityLevel.valueOf(parts[2]);
                    // Create a User object with a placeholder for passwordHash.
                    User account = new User(username, "restoredPasswordHash", role, securityLevel);
                    accounts.add(account);
                }
            }
            System.out.println("Accounts loaded successfully from " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return accounts;
    }

    // For demonstration, a simple main method
    public static void main(String[] args) {
        List<User> accounts = new ArrayList<>();
        // Create sample accounts
        accounts.add(new User("adminUser", "hashedAdmin", UserRole.ADMIN, SecurityLevel.TOPLEVEL));
        accounts.add(new User("techUser", "hashedTech", UserRole.TECHNICIAN, SecurityLevel.ADMIN));
        accounts.add(new User("endUser", "hashedEnd", UserRole.END_USER, SecurityLevel.BASE));

        // Save accounts to file
        String filePath = "accounts.csv";
        saveAccountsToFile(accounts, filePath);

        // Reload accounts from file and print them
        List<User> loadedAccounts = loadAccountsFromFile(filePath);
        System.out.println("Loaded Accounts:");
        for (User user : loadedAccounts) {
            System.out.println("Username: " + user.getUsername() +
                    " | Role: " + user.getRole() +
                    " | Security Level: " + user.getSecurityLevel());
        }
    }
}
