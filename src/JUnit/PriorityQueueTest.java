package JUnit;

import Program.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PriorityQueueTest {
    private PriorityQueue queue;
    private User baseUser, techUser;

    @BeforeEach
    void setUp() {
        queue = new PriorityQueue(10, /*logFilePath=*/null, /*snapshotFilePath=*/null);
        baseUser = new User("alice", "h", UserRole.END_USER, SecurityLevel.BASE);
        techUser = new User("bob",   "h", UserRole.TECHNICIAN, SecurityLevel.TOPLEVEL);
    }

    @Test
    void addPeekPollMaintainsMinHeapOrder() {
        Ticket low  = new Ticket("low",  "alice", 5, SecurityLevel.BASE);
        Ticket high = new Ticket("high", "alice", 1, SecurityLevel.BASE);
        queue.addTicket(low,  baseUser);
        queue.addTicket(high, baseUser);

        assertEquals(2, queue.getSize());
        assertEquals(high, queue.peek());
        assertEquals(high, queue.pollTicket(baseUser));
        assertEquals(1, queue.getSize());
        assertEquals(low, queue.peek());
    }

    @Test
    void updatePriorityReordersHeap() {
        Ticket t1 = new Ticket("T1", "alice", 1, SecurityLevel.BASE);
        Ticket t2 = new Ticket("T2", "alice", 2, SecurityLevel.BASE);
        queue.addTicket(t1, baseUser);
        queue.addTicket(t2, baseUser);

        // Now make T1 less urgent than T2
        assertTrue(queue.updateTicketPriority(t1.getTicketID(), 10, baseUser));
        assertEquals(t2, queue.peek());
    }

    @Test
    void deleteTicketRemovesElementAndReheapifies() {
        Ticket a = new Ticket("A", "alice", 1, SecurityLevel.BASE);
        Ticket b = new Ticket("B", "alice", 2, SecurityLevel.BASE);
        Ticket c = new Ticket("C", "alice", 3, SecurityLevel.BASE);
        queue.addTicket(a, baseUser);
        queue.addTicket(b, baseUser);
        queue.addTicket(c, baseUser);

        assertTrue(queue.deleteTicket(b.getTicketID(), baseUser));
        List<Ticket> all = queue.getAllTickets();
        assertFalse(all.contains(b));
        assertEquals(2, queue.getSize());
    }

    @Test
    void listAndSearchRespectSecurityLevels() {
        Ticket pub    = new Ticket("Public", "alice", 1, SecurityLevel.BASE);
        Ticket secret = new Ticket("Secret", "alice", 1, SecurityLevel.TOPLEVEL);
        queue.addTicket(pub,    baseUser);
        queue.addTicket(secret, baseUser);

        // BASE only sees "Public"
        assertEquals(1, queue.listAccessibleTickets(baseUser).size());
        // TECH sees both
        assertEquals(2, queue.listAccessibleTickets(techUser).size());

        // search by title; BASE cannot see "Secret"
        assertEquals(1, queue.searchAccessibleTickets("sec", techUser).size());
        assertEquals(0, queue.searchAccessibleTickets("sec", baseUser).size());
    }

    @Test
    void getSizeAndIsEmptyReflectState() {
        assertTrue(queue.isEmpty());
        Ticket t = new Ticket("X", "alice", 1, SecurityLevel.BASE);
        queue.addTicket(t, baseUser);
        assertFalse(queue.isEmpty());
        assertEquals(1, queue.getSize());
    }
}
