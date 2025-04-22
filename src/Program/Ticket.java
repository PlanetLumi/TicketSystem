package Program;

import java.io.Serializable;

public class Ticket implements Serializable {
    private static int globalIDCounter = 0;  // Auto-increment ID generator
    private static final long serialVersionUID = 1L;

    private int ticketID;
    private String title;       // description
    private String creator;
    private String owner;       // assigned technician, if any
    private int priority;     // smaller number = higher priority (1 = highest)
    private SecurityLevel securityLevel; // field for security level
    private TicketStatus status;


    public Ticket(String title, String creator, int priority, SecurityLevel securityLevel) {
        this.ticketID = ++globalIDCounter;  // unique ID
        this.title = title;
        this.creator = creator;
        this.priority = priority;
        this.securityLevel = securityLevel;
        this.status = TicketStatus.OPEN; // default on creation

    }
    public static synchronized void syncGlobalIDCounter(int highestID) {
        // If globalIDCounter is already higher, leave it.
        // If highestID is bigger, set it to highestID so new tickets won't reuse an old ID.
        if (highestID > globalIDCounter) {
            globalIDCounter = highestID;
        }
    }
    // Getters and setters
    public int getTicketID() {
        return ticketID;
    }

    public String getTitle() {
        return title;
    }

    public String getCreator() {
        return creator;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(SecurityLevel securityLevel) {
        this.securityLevel = securityLevel;
    }

    // Optional: a setter to change ticket status
    public void setStatus(TicketStatus newStatus) {
        this.status = newStatus;
    }public static void setGlobalCounter(int newVal) {
        globalIDCounter = newVal;
    }

    public TicketStatus getStatus() {
        return this.status;
    }
    @Override
    public String toString() {
        return "Program.Ticket[ID=" + ticketID +
                ", title='" + title + '\'' +
                ", priority=" + priority + "]";
    }


}
