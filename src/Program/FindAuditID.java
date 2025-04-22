package Program;

public class FindAuditID {

    /**
     * Returns the filename for a given Audit ID.
     *
     * @param auditID the identifier for the type of audit event.
     * @return the corresponding filename as a String.
     */
    public static String getAuditLogFileName(String auditID) {
        switch (auditID) {
            case "ATTEMPT":
                // Audit ID 1: Login attempts (both successful and failed can be split further if needed)
                return "login_attempts.log";
            case "LOGINSUCCESS":
                // Audit ID 2: Successful login attempts
                return "successful_logins.log";

                case "REGISTERED":
                return "account_registered.log";

            case "LOGOUT":
                return "account_logout.log";
            case "TCREATION":
                // Audit ID 3: Program.Ticket creations
                return "ticket_creations.log";
            case "TUPDATE":
                // Audit ID 4: Program.Ticket updates
                return "ticket_updates.log";
            case "TDELETE":
                // Audit ID 5: Program.Ticket deletions
                return "ticket_deletions.log";
            case "TCLOSE":
                    return "ticket_close.log";

            default:
                // If no matching audit ID is found, return a default log filename.
                return "general_audit.log";
        }
    }
}