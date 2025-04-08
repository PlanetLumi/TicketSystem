public class Main {
    public static void main(String[] args) {
       PriorityQueue pq = new PriorityQueue(10);

        // Create some tickets
        Ticket t1 = new Ticket("Fix network", "Alice", 2);
        Ticket t2 = new Ticket("Reset password", "Bob", 1);
        Ticket t3 = new Ticket("Install software", "Charlie", 3);

        // Add them to priority queue
        pq.addTicket(t1);
        pq.addTicket(t2);
        pq.addTicket(t3);

        // Peek highest priority
        System.out.println("Top priority ticket: " + pq.peek());
        // Should be t2 (priority=1)

        // Remove highest priority
        Ticket removed = pq.pollTicket();
        System.out.println("Removed: " + removed);

        // Now the new top
        System.out.println("Next highest priority: " + pq.peek());

        // Search by ID
        Ticket found = pq.findTicketByID(2); // for example, searching ID=2
        if (found != null) {
            System.out.println("Found ticket: " + found);
        } else {
            System.out.println("Ticket not found.");
        }
    }
}
