package Program;

import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Scanner;

/**
 * Clean, single‑loop version of the interactive CLI runner.
 *
 * <p>Key points:
 * <ul>
 *     <li>Only <b>one</b> global <code>exitApp</code> flag.</li>
 *     <li>Scanner is created once and closed once.</li>
 *     <li>Auth menu and main menu factored into helper methods.</li>
 *     <li>Snapshot hook guarantees encrypted backup on JVM shutdown.</li>
 * </ul>
 */
public class Main {

    // -------------------------------------------------------------------------
    // Configuration (never hard‑code secrets – these come from env‑vars)
    // -------------------------------------------------------------------------

    private static final Path KEYSTORE_PATH = Path.of(System.getenv("KEYSTORE_PATH"));
    private static final Path KEYSTORE_FILE = Path.of(System.getenv("KEYSTORE_FILE"));

    private static final String SNAPSHOT_PATH = "tickets.snapshot";
    private static final String LOG_PATH = "ticketsLog.csv";

    private static boolean exitApp = false;   // single flag controlling program lifetime

    // -------------------------------------------------------------------------
    // Main entry
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        // -- 1) Initialise crypto ------------------------------------------------
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        String pwd = Files.readString(KEYSTORE_PATH).trim();
        char[] ksPwd = pwd.toCharArray();
        try (FileInputStream fis = new FileInputStream(KEYSTORE_FILE.toFile())) {
            ks.load(fis, ksPwd);
        }
        SecretKey aesKey = ((KeyStore.SecretKeyEntry) ks.getEntry(
                "ticketing-aes", new KeyStore.PasswordProtection(ksPwd)
        )).getSecretKey();
        SecurityUtil.init(aesKey);

        // -- 2) Load queue from event‑log / snapshot -----------------------------
        TicketFileHandler fileHandler = new TicketFileHandler(LOG_PATH, SNAPSHOT_PATH);
        final PriorityQueue ticketQueue = fileHandler.loadQueueFromLog();
        Ticket.syncGlobalIDCounter(ticketQueue.getMaxTicketID());

        // -- 3) Ensure we always persist a final encrypted snapshot --------------
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                User u = SessionManager.getInstance().getCurrentUser();
                if (u != null) ticketQueue.saveSnapshotBinary(u);
            } catch (Exception e) {
                System.err.println("Shutdown snapshot failed: " + e.getMessage());
            }
        }));

        // -- 4) Interactive loop -------------------------------------------------
        try (Scanner scanner = new Scanner(System.in)) {
            while (!exitApp) {
                if (SessionManager.getInstance().getCurrentUser() == null) {
                    authMenu(scanner);
                } else {
                    mainMenu(scanner, ticketQueue);
                }
            }
        }

        System.out.println("Goodbye.");
    }

    // -------------------------------------------------------------------------
    // Authentication menu (shown when there is no signed‑in user)
    // -------------------------------------------------------------------------

    private static void authMenu(Scanner sc) {
        System.out.println("\n=== Welcome to IT Ticketing System ===");
        System.out.println("1. Self‑Register");
        System.out.println("2. Login");
        System.out.println("3. Exit");
        System.out.print("Select an option: ");

        switch (sc.nextLine().trim()) {
            case "1" -> {
                System.out.print("Enter new username: ");
                String u = sc.nextLine();
                System.out.print("Enter new password: ");
                String p = sc.nextLine();
                boolean ok = RegistrationSystem.registerUser(u, p, UserRole.END_USER, SecurityLevel.BASE);
                System.out.println(ok ? "Registration successful. Please login." : "Registration failed.");
            }
            case "2" -> {
                System.out.print("Username: ");
                String u = sc.nextLine();
                System.out.print("Password: ");
                String p = sc.nextLine();
                try {
                    if (LoginSystem.login(u, p)) {
                        System.out.println("Login successful. Welcome, " + u + "!\n");
                    } else {
                        System.out.println("Login failed. Try again.");
                    }
                } catch (IOException e) {
                    System.out.println("Login error: " + e.getMessage());
                }
            }
            case "3" -> exitApp = true;
            default -> System.out.println("Invalid option.");
        }
    }

    // -------------------------------------------------------------------------
    // Main menu (after a user is logged‑in)
    // -------------------------------------------------------------------------

    private static void mainMenu(Scanner sc, PriorityQueue q) throws IOException {
        User cur = SessionManager.getInstance().getCurrentUser();
        System.out.println("\n=== Main Menu (User: " + cur.getUsername() + ") ===");
        System.out.println("0. Register User");
        System.out.println("1. Create Ticket");
        System.out.println("2. Claim Ticket");
        System.out.println("3. View Top Ticket");
        System.out.println("4. Update Ticket Priority");
        System.out.println("5. Delete Ticket");
        System.out.println("6. Complete Ticket");
        System.out.println("7. List Tickets");
        System.out.println("8. Search Tickets");
        System.out.println("9. Save Queue Snapshot");
        System.out.println("10. Logout");
        System.out.println("11. Exit");
        System.out.print("Select an option: ");

        switch (sc.nextLine().trim()) {
            case "0" -> registerUser(sc, cur);
            case "1" -> createTicket(sc, q, cur);
            case "2" -> claimTicket(q, cur);
            case "3" -> viewTopTicket(q);
            case "4" -> updatePriority(sc, q, cur);
            case "5" -> deleteTicket(sc, q, cur);
            case "6" -> completeTicket(sc, q, cur);
            case "7" -> listTickets(q, cur);
            case "8" -> searchTickets(sc, q, cur);
            case "9" -> saveSnapshot(q);
            case "10" -> LoginSystem.logout();
            case "11" -> exitApp = true;
            default -> System.out.println("Invalid option.");
        }
    }

    // -------------------------------------------------------------------------
    // Helper operations --------------------------------------------------------
    // -------------------------------------------------------------------------

    private static void registerUser(Scanner sc, User current) {
        System.out.print("Enter username for new account: ");
        String u = sc.nextLine();
        System.out.print("Enter password: ");
        String p = sc.nextLine();
        System.out.print("Enter role (ADMIN, TECHNICIAN, END_USER): ");
        String roleStr = sc.nextLine().trim().toUpperCase();
        System.out.print("Enter security level (BASE, ADMIN, TOPLEVEL): ");
        String levelStr = sc.nextLine().trim().toUpperCase();
        try {
            UserRole role = UserRole.valueOf(roleStr);
            SecurityLevel lvl = SecurityLevel.valueOf(levelStr);
            boolean ok = RegistrationSystem.registerUser(u, p, role, lvl);
            System.out.println(ok ? "User registered." : "Registration failed.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Invalid role or security level.");
        }
    }

    private static void createTicket(Scanner sc, PriorityQueue q, User cur) {
        // --- 1) choose type -------------------------------------------------------
        System.out.println("Select request type:");
        for (int i = 0; i < RequestType.values().length; i++) {
            System.out.printf("  %d. %s%n", i + 1, RequestType.values()[i]);
        }
        System.out.print("Choice: ");
        int choice = Integer.parseInt(sc.nextLine().trim()) - 1;
        if (choice < 0 || choice >= RequestType.values().length) {
            System.out.println("Invalid selection.");
            return;
        }
        RequestType type = RequestType.values()[choice];

        // --- 2) title -------------------------------------------------------------
        System.out.print("Short title / description: ");
        String title = sc.nextLine();

        // --- 3) optional manual priority -----------------------------------------
        int priority = type.getDefaultPriority();
        System.out.printf("Default priority for %s is %d. Override? (y/N): ",
                type.name(), priority);
        String ans = sc.nextLine().trim();
        if (ans.equalsIgnoreCase("y")) {
            System.out.print("Enter new numeric priority (1 = highest): ");
            priority = Integer.parseInt(sc.nextLine().trim());
        }

        // --- 4) create & enqueue --------------------------------------------------
        Ticket t = new Ticket(type, title, cur.getUsername(), priority,
                type.getDefaultLvl());
        q.addTicket(t, cur);
        System.out.println("Created: " + t);
    }

    // Main.claimTicket()
    private static void claimTicket(PriorityQueue q, User cur) {
        if (!SecurityUtil.hasRequiredPrivileges(cur, SecurityLevel.TOPLEVEL)) {
            System.out.println("Access denied."); return;
        }
        Ticket t = q.peek();                    // don’t remove
        if (t == null) { System.out.println("No ticket to claim."); return; }

        if (t.getOwner() != null) {
            System.out.println("Ticket already claimed by " + t.getOwner());
            return;
        }
        t.setOwner(cur.getUsername());
        t.setStatus(TicketStatus.CLAIMED);
        System.out.println("Claimed: " + t);
    }

    private static void viewTopTicket(PriorityQueue q) {
        Ticket t = q.peek();
        System.out.println(t != null ? "Top: " + t : "No tickets.");
    }

    private static void updatePriority(Scanner sc, PriorityQueue q, User cur) {
        if (!SecurityUtil.hasRequiredPrivileges(cur, SecurityLevel.ADMIN)) {
            System.out.println("Access denied.");
            return;
        }
        try {
            System.out.print("Ticket ID to update: ");
            int id = Integer.parseInt(sc.nextLine().trim());
            System.out.print("New priority: ");
            int np = Integer.parseInt(sc.nextLine().trim());
            boolean ok = q.updateTicketPriority(id, np, cur);
            System.out.println(ok ? "Priority updated." : "Ticket not found.");
        } catch (NumberFormatException ex) {
            System.out.println("Invalid number.");
        }
    }

    private static void deleteTicket(Scanner sc, PriorityQueue q, User cur) {
        if (!SecurityUtil.hasRequiredPrivileges(cur, SecurityLevel.ADMIN)) {
            System.out.println("Access denied.");
            return;
        }
        try {
            System.out.print("Ticket ID to delete: ");
            int id = Integer.parseInt(sc.nextLine().trim());
            boolean ok = q.deleteTicket(id, cur);
            System.out.println(ok ? "Ticket " + id + " deleted." : "Ticket not found.");
        } catch (NumberFormatException ex) {
            System.out.println("Invalid number.");
        }
    }

    private static void completeTicket(Scanner sc, PriorityQueue q, User cur) {
        if (!SecurityUtil.hasRequiredPrivileges(cur, SecurityLevel.TOPLEVEL)) {
            System.out.println("Access denied.");
            return;
        }
        try {
            System.out.print("Ticket ID to complete: ");
            int id = Integer.parseInt(sc.nextLine().trim());
            boolean ok = q.deleteTicket(id, cur);
            System.out.println(ok ? "Ticket " + id + " completed." : "Ticket not found.");
        } catch (NumberFormatException ex) {
            System.out.println("Invalid number.");
        }
    }

    private static void listTickets(PriorityQueue q, User cur) {
        q.listAccessibleTickets(cur).forEach(System.out::println);
    }

    private static void searchTickets(Scanner sc, PriorityQueue q, User cur) {
        System.out.print("Keyword: ");
        String kw = sc.nextLine();
        q.searchAccessibleTickets(kw, cur).forEach(System.out::println);
    }

    private static void saveSnapshot(PriorityQueue q) {
        try {
            User u = SessionManager.getInstance().getCurrentUser();
            if (u != null) {
                q.saveSnapshotBinary(u);
                System.out.println("Snapshot saved.");
            }
        } catch (Exception e) {
            System.out.println("Snapshot error: " + e.getMessage());
        }
    }
}