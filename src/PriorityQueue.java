public class PriorityQueue {
    private Ticket[] heap;  // array-based heap
    private int size;       // number of tickets currently in the heap

    public PriorityQueue(int capacity) {
        this.heap = new Ticket[capacity];
        this.size = 0;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int getSize() {
        return size;
    }

    public void addTicket(Ticket ticket) {
        if (size == heap.length) {
            throw new RuntimeException("Heap is full");
        }
        // Place the ticket at the end
        heap[size] = ticket;
        size++;

        // Bubble up
        heapifyUp(size - 1);
    }

    private void heapifyUp(int index) {
        // While we're not at the root, compare with parent
        while (index > 0) {
            int parentIndex = (index - 1) / 2; // parent formula

            // If child has a smaller priority => swap
            if (heap[index].getPriority() < heap[parentIndex].getPriority()) {
                swap(index, parentIndex);
                index = parentIndex;  // continue from parent's position
            } else {
                break; // already in the correct spot
            }
        }
    }
    public Ticket peek() {
        if (isEmpty()) {
            return null;
        }
        return heap[0];  // root
    }
    public Ticket pollTicket() {
        if (isEmpty()) {
            return null;
        }
        Ticket highestPriorityTicket = heap[0];

        // Move the last element to root
        heap[0] = heap[size - 1];
        heap[size - 1] = null; // optional, to avoid memory leak
        size--;

        // Bubble down from root
        heapifyDown(0);

        return highestPriorityTicket;
    }

    private void heapifyDown(int index) {
        while (true) {
            int leftChild = 2 * index + 1;
            int rightChild = 2 * index + 2;
            int smallest = index;

            // Check left child
            if (leftChild < size &&
                    heap[leftChild].getPriority() < heap[smallest].getPriority()) {
                smallest = leftChild;
            }
            // Check right child
            if (rightChild < size &&
                    heap[rightChild].getPriority() < heap[smallest].getPriority()) {
                smallest = rightChild;
            }

            // If we found a smaller child, swap with that child
            if (smallest != index) {
                swap(index, smallest);
                index = smallest;  // keep going down
            } else {
                break;
            }
        }
    }

    private void swap(int i, int j) {
        Ticket temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }
    public Ticket findTicketByID(int ticketID) {
        for (int i = 0; i < size; i++) {
            if (heap[i].getTicketID() == ticketID) {
                return heap[i];
            }
        }
        return null;
    }
    public void updateTicketPriority(int ticketID, int newPriority) {int index = -1;
        for (int i = 0; i < size; i++) {
            if (heap[i].getTicketID() == ticketID) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            System.out.println("Ticket not found.");
            return;
        }

        int oldPriority = heap[index].getPriority();
        heap[index].setPriority(newPriority);

        if (newPriority < oldPriority) {
            heapifyUp(index);
        } else if (newPriority > oldPriority) {
            heapifyDown(index);
        }
    }


    public void updateTicketPriority(int ticketID, int newPriority, User currentUser) {
        Ticket ticket = findTicketByID(ticketID);
        if (ticket == null) {
            System.out.println("Ticket not found.");
            return;
        }

        // Check if the current user's security level meets the ticket's required level.
        if (!SecurityUtil.hasRequiredPrivileges(currentUser, ticket.getSecurityLevel())) {
            System.out.println("Access denied: insufficient security privileges to update this ticket.");
            return;
        }

        // If the check passes, allow the update.
        int oldPriority = ticket.getPriority();
        ticket.setPriority(newPriority);

        // Re-heapify as needed:
        if (newPriority < oldPriority) {
            // bubble up logic
        } else if (newPriority > oldPriority) {
            // bubble down logic
        }
    }

    public void deleteTicket(int ticketID, User currentUser) {
        Ticket ticket = findTicketByID(ticketID);
        if (ticket == null) {
            System.out.println("Ticket not found.");
            return;
        }
        // Require at least TOPLEVEL privileges to delete a sensitive ticket.
        if (!SecurityUtil.hasRequiredPrivileges(currentUser, SecurityLevel.TOPLEVEL)) {
            System.out.println("Access denied: insufficient privileges to delete this ticket.");
        }

    }

}
