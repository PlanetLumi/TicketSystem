
package Program;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AccountPersistence {
    private static String mainPathOverride;
    private static String copyPathOverride;

    public static void setPaths(String main, String copy) {
        mainPathOverride = main;
        copyPathOverride = copy;
    }

    public static void saveAccountsToFile(List<User> accounts, String filePath) {
        String path = filePath;
        if ("accounts.csv".equals(filePath) && mainPathOverride != null) {
            path = mainPathOverride;
        } else if ("accounts2.csv".equals(filePath) && copyPathOverride != null) {
            path = copyPathOverride;
        }
        try (FileWriter writer = new FileWriter(path)) {
            writer.write("username,role,securityLevel,passwordHash\n");
            for (User u : accounts) {
                writer.write(
                        u.getUsername() + "," +
                                u.getRole() + "," +
                                u.getSecurityLevel() + "," +
                                u.getPasswordHash() +   //real hash
                                "\n"
                );
            }
            for (User u : accounts) {
                writer.write(u.getUsername() + "," + u.getRole() + "," + u.getSecurityLevel() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<User> loadAccountsFromFile(String filePath) {
        String path = filePath;
        //main account directory
        if ("accounts.csv".equals(filePath) && mainPathOverride != null) {
            path = mainPathOverride;
        }
        List<User> accounts = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line = reader.readLine(); // skip header
            if (line == null) return accounts;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                    if (parts.length >= 4) {
                        accounts.add(new User(
                                parts[0],                           // username
                                parts[3],                           // restore the real salted hash
                                UserRole.valueOf(parts[1]),
                                SecurityLevel.valueOf(parts[2])
                    ));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return accounts;
    }
}
