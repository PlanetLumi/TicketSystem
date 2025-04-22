package JUnit;// src/test/java/com/yourorg/ticketing/JUnit.FindPrivilegeLevelTest.java
import Program.FindPrivilegeLevel;
import Program.SecurityLevel;
import Program.User;
import Program.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FindPrivilegeLevelTest {

    @Test
    void getRequiredLevelForKnownCommands() {
        assertEquals(SecurityLevel.BASE, FindPrivilegeLevel.getRequiredLevel("ADD_TICKET"));
        assertEquals(SecurityLevel.TOPLEVEL, FindPrivilegeLevel.getRequiredLevel("DELETE_TICKET"));
        assertEquals(SecurityLevel.ADMIN, FindPrivilegeLevel.getRequiredLevel("SAVE_SNAPSHOT"));
    }

    @Test
    void defaultLevelIsBase() {
        assertEquals(SecurityLevel.BASE, FindPrivilegeLevel.getRequiredLevel("UNKNOWN_COMMAND"));
    }

    @Test
    void hasPrivilegeReturnsTrueWhenEnough() {
        User admin = new User("a", "h", UserRole.ADMIN, SecurityLevel.ADMIN);
        User tech = new User("t", "h", UserRole.TECHNICIAN, SecurityLevel.TOPLEVEL);
        assertTrue(FindPrivilegeLevel.hasPrivilege(admin, "TRUNCATE_LOG"));
        assertTrue(FindPrivilegeLevel.hasPrivilege(tech, "ASSIGN_TICKET"));
        assertFalse(FindPrivilegeLevel.hasPrivilege(tech, "SAVE_SNAPSHOT"));
    }

    @Test
    void checkAndLogPrivilegeDoesNotThrow() {
        // Should simply return false for insufficient privilege
        User u = new User("bob", "h", UserRole.END_USER, SecurityLevel.BASE);
        boolean ok = FindPrivilegeLevel.checkAndLogPrivilege(u, "UPDATE_TICKET");
        assertFalse(ok);
    }
}
