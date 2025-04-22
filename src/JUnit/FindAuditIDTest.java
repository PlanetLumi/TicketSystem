package JUnit;

import Program.FindAuditID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FindAuditIDTest {

    @Test
    void mapsKnownIdsCorrectly() {
        assertEquals("login_attempts.log",     FindAuditID.getAuditLogFileName("ATTEMPT"));
        assertEquals("successful_logins.log",  FindAuditID.getAuditLogFileName("LOGINSUCCESS"));
        assertEquals("account_registered.log", FindAuditID.getAuditLogFileName("REGISTERED"));
        assertEquals("account_logout.log",     FindAuditID.getAuditLogFileName("LOGOUT"));
        assertEquals("ticket_creations.log",   FindAuditID.getAuditLogFileName("TCREATION"));
        assertEquals("ticket_updates.log",     FindAuditID.getAuditLogFileName("TUPDATE"));
        assertEquals("ticket_deletions.log",   FindAuditID.getAuditLogFileName("TDELETE"));
        assertEquals("ticket_close.log",       FindAuditID.getAuditLogFileName("TCLOSE"));
        // default
        assertEquals("general_audit.log",      FindAuditID.getAuditLogFileName("FOO"));
    }
}
