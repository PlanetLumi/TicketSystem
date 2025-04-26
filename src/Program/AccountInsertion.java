package Program;

public class AccountInsertion {
    public static void main(String args[]){
        Program.User bootstrap = new Program.User(
                "inject",
                "unusedHash",
                Program.UserRole.ADMIN,
                Program.SecurityLevel.ADMIN
        );
        Program.SessionManager.getInstance().setCurrentUser(bootstrap);
        InsertAccounts();
    }
    public static void InsertAccounts(){
        try{
            if (RegistrationSystem.registerUser("endUser","endPass3!", UserRole.END_USER, SecurityLevel.BASE)) {
                System.out.println("Account created successfully");
            } else{
                System.out.println("EndUser account creation failed");
            }
            if (RegistrationSystem.registerUser("adminUser", "adminPass6!", UserRole.ADMIN, SecurityLevel.ADMIN)) {
                System.out.println("Account created successfully");
            } else{
                System.out.println("Admin account creation failed");
            }
            if (RegistrationSystem.registerUser("techUser", "techPass2!", UserRole.TECHNICIAN, SecurityLevel.TOPLEVEL)) {
            } else{
                System.out.println("Technician account creation failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
