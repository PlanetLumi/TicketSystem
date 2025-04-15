import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        // Create a PriorityQueue for tickets with sufficient capacity
        PriorityQueue ticketQueue = new PriorityQueue(10000);
        // Create instances of the systems
        LoginSystem loginSystem = new LoginSystem();
        Scanner scanner = new Scanner(System.in);

        boolean exit = false;
        while (!exit) {
            System.out.println("\n===== IT Ticketing System Menu =====");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Create Ticket");
            System.out.println("4. Claim Ticket (Technician/Admin only)");
            System.out.println("5. View Top Ticket");
            System.out.println("6. Logout");
            System.out.println("7. Exit");
            System.out.print("Select an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            switch (choice) {
                case 1:
                    // Registration
                    System.out.print("Enter new username: ");
                    String newUsername = scanner.nextLine();
                    System.out.print("Enter new password: ");
                    String newPassword = scanner.nextLine();
                    // For self-registration, allowed role is END_USER and security level must be BASE.
                    if (RegistrationSystem.registerUser(newUsername, newPassword, UserRole.END_USER, SecurityLevel.BASE)) {
                        System.out.println("Registration successful.");
                    } else {
                        System.out.println("Registration failed.");
                    }
                    break;

                case 2:
                    // Login
                    System.out.print("Enter username: ");
                    String loginUsername = scanner.nextLine();
                    System.out.print("Enter password: ");
                    String loginPassword = scanner.nextLine();
                    if (loginSystem.login(loginUsername, loginPassword)) {
                        System.out.println("Logged in successfully.");
                    } else {
                        System.out.println("Login failed.");
                    }
                    break;

                case 3:
                    // Create Ticket – available only if logged in
                    if (SessionManager.getInstance().getCurrentUser() == null) {
                        System.out.println("You must log in to create a ticket.");
                        break;
                    }
                    User creator = SessionManager.getInstance().getCurrentUser();
                    System.out.print("Enter ticket title/description: ");
                    String ticketTitle = scanner.nextLine();
                    System.out.print("Enter ticket priority (number, lower number is higher priority): ");
                    int ticketPriority = scanner.nextInt();
                    scanner.nextLine(); // consume newline

                    // Here you can decide ticket security level based on request type.
                    // As an example, if the title contains "security", we'll treat it as TOPLEVEL; otherwise BASE.
                    SecurityLevel ticketSecLevel = ticketTitle.toLowerCase().contains("security")
                            ? SecurityLevel.TOPLEVEL : SecurityLevel.BASE;
                    Ticket newTicket = new Ticket(ticketTitle, creator.getUsername(), ticketPriority, ticketSecLevel);
                    ticketQueue.addTicket(newTicket);
                    System.out.println("Ticket created: " + newTicket);
                    break;

                case 4:
                    // Claim Ticket – only available to TECHNICIAN or ADMIN accounts.
                    if (SessionManager.getInstance().getCurrentUser() == null) {
                        System.out.println("You must log in to claim a ticket.");
                        break;
                    }
                    User currentUser = SessionManager.getInstance().getCurrentUser();
                    if (currentUser.getRole() != UserRole.TECHNICIAN && currentUser.getRole() != UserRole.ADMIN) {
                        System.out.println("Only technicians or admin users can claim tickets.");
                        break;
                    }
                    Ticket ticketToClaim = ticketQueue.pollTicket();
                    if (ticketToClaim == null) {
                        System.out.println("No tickets are available to claim.");
                    } else {
                        // Assign the ticket's owner as the current user.
                        ticketToClaim.setOwner(currentUser.getUsername());
                        System.out.println("Ticket claimed successfully: " + ticketToClaim);
                    }
                    break;

                case 5:
                    // View top ticket in the priority queue without removing it.
                    Ticket topTicket = ticketQueue.peek();
                    if (topTicket == null) {
                        System.out.println("No tickets found in the queue.");
                    } else {
                        System.out.println("Top Ticket: " + topTicket);
                    }
                    break;

                case 6:
                    // Logout
                    try {
                        loginSystem.logout();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case 7:
                    // Exit the application
                    exit = true;
                    break;

                default:
                    System.out.println("Invalid choice. Please select again.");
                    break;
            }
        }
        scanner.close();
        System.out.println("Exiting IT Ticketing System. Goodbye!");
    }
}
