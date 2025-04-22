package JUnit;

import Program.SecurityLevel;
import Program.SessionManager;
import Program.User;
import Program.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    private SessionManager mgr;

    @BeforeEach
    void setUp() {
        mgr = SessionManager.getInstance();
        mgr.clearSession();
    }

    @Test
    void getInstanceAlwaysReturnsSame() {
        SessionManager m2 = SessionManager.getInstance();
        assertSame(mgr, m2);
    }

    @Test
    void setGetAndClearCurrentUser() {
        User u = new User("alice", "hash", UserRole.END_USER, SecurityLevel.BASE);
        mgr.setCurrentUser(u);
        assertEquals("alice", mgr.getCurrentUser().getUsername());
        mgr.clearSession();
        assertNull(mgr.getCurrentUser());
    }
}
