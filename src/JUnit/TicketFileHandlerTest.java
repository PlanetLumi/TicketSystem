
package JUnit;

import Program.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TicketFileHandlerTest {
    @TempDir Path tempDir;

    @Test
    void logAndLoadReplayAddUpdateDelete() throws IOException {
        // Ensure SessionManager has a user
        SessionManager.getInstance().setCurrentUser(
                new User("tester", "hash", UserRole.ADMIN, SecurityLevel.ADMIN)
        );

        Path logPath  = tempDir.resolve("tickets.log");
        Path snapPath = tempDir.resolve("tickets.snapshot");
        TicketFileHandler h = new TicketFileHandler(logPath.toString(), snapPath.toString());

        // Create two tickets and log operations
        Ticket t1 = new Ticket("One", "alice", 1, SecurityLevel.BASE);
        Ticket t2 = new Ticket("Two", "bob",   2, SecurityLevel.BASE);
        h.logAdd(t1);
        h.logAdd(t2);

        t1.setPriority(9);
        h.logUpdate(t1);

        h.logDelete(t2.getTicketID());

        // Replay log
        PriorityQueue q = h.loadQueueFromLog();
        MyList<Ticket> tickets = q.getAllTickets();

        //update priority
        assertEquals(1, tickets.size());
        Ticket replayed = tickets.get(1);
        assertEquals(t1.getTicketID(), replayed.getTicketID());
        assertEquals(9, replayed.getPriority());
    }
    @Test
    void defaultPriorityMatchesEnum() {
        Ticket t = new Ticket(RequestType.NETWORK, "Wi-Fi down", "alice");
        assertEquals(2, t.getPriority());
    }
    @Test
    void malformedLinesAreSkipped() throws IOException {
        SessionManager.getInstance().setCurrentUser(
                new User("tester", "hash", UserRole.ADMIN, SecurityLevel.ADMIN)
        );
        Path badLog = tempDir.resolve("bad.log");
        java.nio.file.Files.writeString(badLog, "MALFORMED LINE\n");
        TicketFileHandler h = new TicketFileHandler(badLog.toString(), null);
        //should return an empty queue
        PriorityQueue q = h.loadQueueFromLog();
        assertNotNull(q);
        assertTrue(q.isEmpty());
    }
}
