package JUnit;

import Program.SecurityLevel;
import Program.User;
import Program.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void gettersReturnConstructorValues() {
        User u = new User("charlie", "hash123", UserRole.TECHNICIAN, SecurityLevel.TOPLEVEL);
        assertEquals("charlie",          u.getUsername());
        assertEquals(UserRole.TECHNICIAN, u.getRole());
        assertEquals(SecurityLevel.TOPLEVEL, u.getSecurityLevel());
    }
}
