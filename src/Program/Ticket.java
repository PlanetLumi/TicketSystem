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
    private SecurityLevel securityLevel;
    private TicketStatus status;
    private final RequestType type;

    public Ticket(RequestType type, String title, String creator) {
        this(type, title, creator,
                //set default levels
                type.getDefaultPriority(),
                type.getDefaultLvl());
    }
    public Ticket(String title,
                  String creator,
                  int priority,
                  SecurityLevel level) {
        this(RequestType.OTHER,      // default type
                title,
                creator,
                priority,
                level);
    }
    //Constructor for ticket data
    public Ticket(RequestType type, String title, String creator,
                  int priorityOverride, SecurityLevel levelOverride) {
        this.ticketID = ++globalIDCounter;
        this.type     = type;
        this.title    = title;
        this.creator  = creator;
        this.priority = priorityOverride;
        this.securityLevel = levelOverride;
        this.status   = TicketStatus.OPEN;
    }

    public RequestType getType() { return type; }


    public static synchronized void syncGlobalIDCounter(int highestID) {
        //Finds current GlobalID to stop outdating
        if (highestID > globalIDCounter) {
            globalIDCounter = highestID;
        }
    }
    // Getters etc
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

    //Change status
    public void setStatus(TicketStatus newStatus) {
        this.status = newStatus;
    }public static void setGlobalCounter(int newVal) {
        globalIDCounter = newVal;
    }

    public TicketStatus getStatus() {
        return this.status;
    }
    //PULL DETAILS AS STRING FOR AUDIT AND DISPLAY
    @Override
    public String toString() {
        return "Program.Ticket[ID=" + ticketID +
                ", title='" + title + '\'' +
                ", priority=" + priority + "]";
    }


}
