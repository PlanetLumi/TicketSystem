package Program;

import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Scanner;

public class Main {
    // Supply these via env‑vars or your orchestrator (never hard‑code!)
    private static final Path KEYSTORE_PATH = Path.of(System.getenv("KEYSTORE_PATH"));
    private static final Path KEYSTORE_FILE      = Path.of(System.getenv("KEYSTORE_FILE"));

    private static final String SNAPSHOT_PATH = "tickets.snapshot";
    private static final String LOG_PATH      = "ticketsLog.csv";

    public static void main(String[] args) throws Exception {
        // 1) Load AES key from JCEKS keystore
        System.out.println("→ KEYSTORE_PATH = " + System.getenv("KEYSTORE_PATH"));
        System.out.println("→ KEYSTORE_FILE = " + System.getenv("KEYSTORE_FILE"));

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        String pwd = Files.readString(KEYSTORE_PATH).trim();
        char[] ksPassword = pwd.toCharArray();
        try (FileInputStream fis = new FileInputStream(KEYSTORE_FILE.toFile())) {
            ks.load(fis, ksPassword);
        }
        SecretKey aesKey = ((KeyStore.SecretKeyEntry) ks.getEntry(
                "ticketing-aes",
                new KeyStore.PasswordProtection(ksPassword)
        )).getSecretKey();

        // 2) Tell your Program.SecurityUtil about it
        SecurityUtil.init(aesKey);

        // 3) Bootstrap your handler/queue as before
        TicketFileHandler fileHandler = new TicketFileHandler(LOG_PATH, SNAPSHOT_PATH);
        final PriorityQueue ticketQueue = fileHandler.loadQueueFromLog();
        Ticket.syncGlobalIDCounter(ticketQueue.getMaxTicketID());

        // … rest of your menu loop unchanged …
        Scanner scanner = new Scanner(System.in);
        boolean exitApp = false;

        // Authentication loop
        while (!exitApp && SessionManager.getInstance().getCurrentUser() == null) {
            System.out.println("\n=== Welcome to IT Ticketing System ===");
            System.out.println("1. Self-Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Select an option: ");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    // Only BASE self-registration allowed when not logged in
                    System.out.print("Enter new username: ");
                    String srUser = scanner.nextLine();
                    System.out.print("Enter new password: ");
                    String srPass = scanner.nextLine();
                    if (RegistrationSystem.registerUser(srUser, srPass, UserRole.END_USER, SecurityLevel.BASE)) {
                        System.out.println("Registration successful. Please login.");
                    } else {
                        System.out.println("Registration failed.");
                    }
                    break;
                case "2":
                    System.out.print("Username: ");
                    String loginUser = scanner.nextLine();
                    System.out.print("Password: ");
                    String loginPass = scanner.nextLine();
                    try {
                        if (LoginSystem.login(loginUser, loginPass)) {
                            System.out.println("Login successful. Welcome, " + loginUser + "!\n");
                        } else {
                            System.out.println("Login failed. Try again.");
                        }
                    } catch (IOException e) {
                        System.out.println("Login error: " + e.getMessage());
                    }
                    break;
                case "3":
                    exitApp = true;
                    break;
                default:
                    System.out.println("Invalid option, please choose 1-3.");
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                User u = SessionManager.getInstance().getCurrentUser();
                if (u != null) {
                    try {
                        ticketQueue.saveSnapshotBinary(u);
                    } catch (Exception e) {
                        System.err.println("Shutdown snapshot failed: " + e.getMessage());
                    }
                }
            }));
        }

        // Program.Main application loop for logged-in users
        while (!exitApp && SessionManager.getInstance().getCurrentUser() != null) {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            System.out.println("\n=== Program.Main Menu (Program.User: " + currentUser.getUsername() + ") ===");
            System.out.println("0. Register Program.User (at or below your level)");
            System.out.println("1. Create Program.Ticket");
            System.out.println("2. Claim Program.Ticket (Technician/Admin)");
            System.out.println("3. View Top Program.Ticket");
            System.out.println("4. Update Program.Ticket Priority (Admin)");
            System.out.println("5. Delete Program.Ticket (Admin)");
            System.out.println("6. Complete Program.Ticket (Technician/Admin)");
            System.out.println("7. List All Tickets");
            System.out.println("8. Search Tickets by Title");
            System.out.println("9. Save Program.Ticket Queue");
            System.out.println("10. Logout");
            System.out.println("11. Exit");
            System.out.print("Select an option: ");
            String option = scanner.nextLine().trim();
            switch (option) {
                case "0":
                    // Register new user with role/level <= current user's level
                    System.out.print("Enter username for new account: ");
                    String newUser = scanner.nextLine();
                    System.out.print("Enter password for new account: ");
                    String newPass = scanner.nextLine();
                    System.out.print("Enter role (ADMIN, TECHNICIAN, END_USER): ");
                    String roleStr = scanner.nextLine().trim().toUpperCase();
                    System.out.print("Enter security level (BASE, ADMIN, TOPLEVEL): ");
                    String levelStr = scanner.nextLine().trim().toUpperCase();
                    try {
                        UserRole role = UserRole.valueOf(roleStr);
                        SecurityLevel level = SecurityLevel.valueOf(levelStr);
                        if (RegistrationSystem.registerUser(newUser, newPass, role, level)) {
                            System.out.println("Program.User registered successfully.");
                        } else {
                            System.out.println("Registration failed. Check privileges and input.");
                        }
                    } catch (IllegalArgumentException e) {
                        System.out.println("Invalid role or security level.");
                    }
                    break;
                case "1":
                    // Create Program.Ticket
                    System.out.print("Program.Ticket title: ");
                    String title = scanner.nextLine();
                    System.out.print("Priority (number): ");
                    int prio = Integer.parseInt(scanner.nextLine().trim());
                    SecurityLevel sec = title.toLowerCase().contains("security")
                            ? SecurityLevel.TOPLEVEL : SecurityLevel.BASE;
                    Ticket t = new Ticket(title, currentUser.getUsername(), prio, sec);
                    ticketQueue.addTicket(t, currentUser);
                    System.out.println("Created: " + t);
                    break;
                case "2":
                    // Claim Program.Ticket
                    if (!SecurityUtil.hasRequiredPrivileges(currentUser, SecurityLevel.TOPLEVEL)) {
                        System.out.println("Access denied."); break;
                    }
                    Ticket claimed = ticketQueue.pollTicket(currentUser);
                    if (claimed == null) System.out.println("No ticket to claim.");
                    else {
                        claimed.setOwner(currentUser.getUsername());
                        System.out.println("Claimed: " + claimed);
                    }
                    break;
                case "3":
                    // View Top Program.Ticket
                    Ticket top = ticketQueue.peek();
                    System.out.println(top != null ? "Top: " + top : "No tickets.");
                    break;
                case "4":
                    // Update priority
                    if (!SecurityUtil.hasRequiredPrivileges(currentUser, SecurityLevel.ADMIN)) {
                        System.out.println("Access denied."); break;
                    }
                    System.out.print("Program.Ticket ID to update: ");
                    int uid = Integer.parseInt(scanner.nextLine().trim());
                    System.out.print("New priority: ");
                    int np = Integer.parseInt(scanner.nextLine().trim());
                    ticketQueue.updateTicketPriority(uid, np, currentUser);
                    System.out.println("Priority update requested.");
                    break;
                case "5":
                    // Delete Program.Ticket
                    if (!SecurityUtil.hasRequiredPrivileges(currentUser, SecurityLevel.ADMIN)) {
                        System.out.println("Access denied."); break;
                    }
                    System.out.print("Program.Ticket ID to delete: ");
                    int did = Integer.parseInt(scanner.nextLine().trim());
                    ticketQueue.deleteTicket(did, currentUser);
                    System.out.println("Delete requested.");
                    break;
                case "6":
                    // Complete Program.Ticket
                    if (!SecurityUtil.hasRequiredPrivileges(currentUser, SecurityLevel.TOPLEVEL)) {
                        System.out.println("Access denied."); break;
                    }
                    System.out.print("Program.Ticket ID to complete: ");
                    int cid = Integer.parseInt(scanner.nextLine().trim());
                    ticketQueue.deleteTicket(cid, currentUser);
                    System.out.println("Completed ticket " + cid);
                    break;
                case "7":
                    // List all accessible tickets
                    ticketQueue.listAccessibleTickets(currentUser)
                            .forEach(System.out::println);
                    break;
                case "8":
                    // Search by title
                    System.out.print("Keyword: ");
                    String kw = scanner.nextLine();
                    ticketQueue.searchAccessibleTickets(kw, currentUser)
                            .forEach(System.out::println);
                    break;
                case "9":
                    // Save encrypted snapshot to disk
                    ticketQueue.saveSnapshotBinary(SessionManager.getInstance().getCurrentUser());
                    System.out.println("Snapshot saved to " + SNAPSHOT_PATH);
                    break;
                case "10":
                    // Logout
                    LoginSystem.logout();
                    break;
                case "11":
                    exitApp = true;
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }

        System.out.println("Goodbye.");
        scanner.close();

    }
}
