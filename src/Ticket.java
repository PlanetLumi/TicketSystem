public class Ticket {
    private static int globalIDCounter = 0;  // Auto-increment ID generator

    private int ticketID;
    private String title;       // description
    private String creator;
    private String owner;       // assigned technician, if any
    private int priority;       // smaller number = higher priority (1 = highest)

    public Ticket(String title, String creator, int priority) {
        this.ticketID = ++globalIDCounter;  // unique ID
        this.title = title;
        this.creator = creator;
        this.priority = priority;
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

    @Override
    public String toString() {
        return "Ticket[ID=" + ticketID +
                ", title='" + title + '\'' +
                ", priority=" + priority + "]";
    }
}
