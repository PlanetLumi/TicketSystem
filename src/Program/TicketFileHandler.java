package Program;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;



/**
 * A standalone file handler that uses an append-only log mechanism to save
 * ticket operations (ADD, UPDATE, DELETE) to disk, then reconstruct the
 * final state by replaying the log on startup.
 */
public class TicketFileHandler {
    // Where the append-only log is stored
    private String logFilePath;
    private String snapshotFilePath;

    // Simple date format for printing timestamps
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // Constructor
    public TicketFileHandler(String logFilePath, String snapshotFilePath) {
        this.logFilePath = logFilePath;
        this.snapshotFilePath = snapshotFilePath;

        // If needed, create the file if it doesn't exist
        File file = new File(logFilePath);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Appends a line to the log file with operation = "ADD" and the ticket's latest data.
     */
    public synchronized void logAdd(Ticket ticket) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String entry = String.format("%s,ADD,%d,%s,%s,%d,%s,%s",
                timestamp,
                ticket.getTicketID(),
                sanitizeCSV(ticket.getTitle()),
                sanitizeCSV(ticket.getCreator()),
                ticket.getPriority(),
                (ticket.getOwner() == null ? "" : sanitizeCSV(ticket.getOwner())),
                ticket.getSecurityLevel());
        appendLine(entry);
    }

    /**
     * Appends a line to the log file with operation = "UPDATE" and the ticket's updated data.
     */
    public synchronized void logUpdate(Ticket ticket) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String entry = String.format("%s,UPDATE,%d,%s,%s,%d,%s,%s",
                timestamp,
                ticket.getTicketID(),
                sanitizeCSV(ticket.getTitle()),
                sanitizeCSV(ticket.getCreator()),
                ticket.getPriority(),
                (ticket.getOwner() == null ? "" : sanitizeCSV(ticket.getOwner())),
                ticket.getSecurityLevel());
        appendLine(entry);
    }

    /**
     * Appends a line to the log file with operation = "DELETE" for a ticket ID.
     */
    public synchronized void logDelete(int ticketID) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        // For deletion, we don't need ticket's entire data, just the ID
        String entry = String.format("%s,DELETE,%d",
                timestamp, ticketID);
        appendLine(entry);
    }

    /**
     * Helper method to append a single line to the log file.
     */
    private void appendLine(String line) {
        // Write in append mode
        try (FileWriter writer = new FileWriter(logFilePath, true)) {
            writer.write(line + System.lineSeparator());
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Rebuilds the Program.PriorityQueue from the appended log.
     * - Reads the log line by line.
     * - For ADD/UPDATE, store/update a Program.Ticket in a Map (ticketId -> Program.Ticket).
     * - For DELETE, remove from the Map.
     * - Then, the final Map state is inserted into a new Program.PriorityQueue, ignoring older duplicates.
     */
    public PriorityQueue loadQueueFromLog() throws IOException {
        PriorityQueue queue = new PriorityQueue(10000, logFilePath, snapshotFilePath);
        // Keep track of the latest ticket state by ticketID
        Map<Integer, Ticket> latestTickets = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // e.g. "2025-04-16T09:00:00,ADD,200,Email outage,bob,2,,BASE"
                // or   "2025-04-16T09:05:00,UPDATE,200,Email outage,bob,1,bob,BASE"
                // or   "2025-04-16T09:10:00,DELETE,200"
                String[] parts = line.split(",", -1); // -1 to preserve empty fields
                if (parts.length < 3) {
                    // skip malformed lines
                    continue;
                }

                String operation = parts[1].trim();    // ADD/UPDATE/DELETE
                int ticketId = Integer.parseInt(parts[2].trim());

                switch (operation) {
                    case "ADD":
                    case "UPDATE":
                        // For ADD/UPDATE, we need all data
                        // e.g. parts = [timestamp, operation, id, title, creator, priority, owner, securityLevel]
                        if (parts.length >= 8) {
                            // parse fields
                            String title = unSanitizeCSV(parts[3].trim());
                            String creator = unSanitizeCSV(parts[4].trim());
                            int priority = Integer.parseInt(parts[5].trim());
                            String owner = unSanitizeCSV(parts[6].trim());
                            SecurityLevel secLevel = SecurityLevel.valueOf(parts[7].trim());

                            Ticket t = new Ticket(title, creator, priority, secLevel);
                            // The Program.Ticket constructor auto-increments ticketID, but we want the logged ID
                            // We can "force" the ticketID if needed, or store it in a new field
                            // For simplicity, let's pretend we re-assign it here:
                            forceSetTicketID(t, ticketId);

                            if (owner != null && !owner.isEmpty()) {
                                t.setOwner(owner);
                            }
                            latestTickets.put(ticketId, t);
                        }
                        break;
                    case "DELETE":
                        // remove from map
                        latestTickets.remove(ticketId);
                        break;
                    default:
                        // ignore unknown ops
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Insert the final version of each ticket into the queue
        for (Ticket t : latestTickets.values()) {
            queue.addTicket(t, SessionManager.getInstance().getCurrentUser());
        }
        return queue;
    }

    /**
     * Helper method: forcibly override the ticket ID.
     * Typically we rely on the static globalIDCounter, but we want to honor the log's ID.
     */
    private void forceSetTicketID(Ticket t, int forcedID) {
        // Reflection approach or re-assign the static in a 'hacky' way
        // For demonstration only:
        try {
            java.lang.reflect.Field idField = Ticket.class.getDeclaredField("ticketID");
            idField.setAccessible(true);
            idField.setInt(t, forcedID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Ensures CSV fields do not contain commas or newlines (trivial sanitization).
     */
    private String sanitizeCSV(String input) {
        if (input == null) return "";
        return input.replace(",", ";").replaceAll("[\\r\\n]+", " ");
    }

    /**
     * Basic un-sanitization if needed (optional).
     */
    private String unSanitizeCSV(String input) {
        // In this example, we assume the user never typed semicolons.
        return input.replace(";", ",");
    }
}
