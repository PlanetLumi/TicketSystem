package Program;

public class FindPrivilegeLevel {

    //Regulates required level using switch look-up of given request
    public static SecurityLevel getRequiredLevel(String command) {
        switch (command.toUpperCase()) {
            case "ADD_TICKET":
            case "VIEW_MY_TICKETS":
                return SecurityLevel.BASE;

            case "UPDATE_TICKET":
            case "DELETE_TICKET":
            case "ASSIGN_TICKET":

                return SecurityLevel.TOPLEVEL;

            case "SAVE_SNAPSHOT":
            case "TRUNCATE_LOG":
            case "VIEW_AUDIT_LOG":
                return SecurityLevel.ADMIN;

            default:
                return SecurityLevel.ADMIN; //ASSUMES highest security as default
        }
    }

     //Returns true if the given user has sufficient privileges to execute a command.

    public static boolean hasPrivilege(User user, String command) {
        SecurityLevel required = getRequiredLevel(command);
        return user.getSecurityLevel().ordinal() >= required.ordinal();
    }

    //checks privilege and logs request data
    public static boolean checkAndLogPrivilege(User user, String command) {
        boolean allowed = hasPrivilege(user, command);
        String username = (user != null) ? user.getUsername() : "<null user>";
        if (!allowed) {
            try {
                SecurityUtil.logEvent("Program.User " + username + " was denied access to command: " + command, "ACCESSDENIED");
            } catch (Exception ignored) {}
        }
        return allowed;
    }
}
