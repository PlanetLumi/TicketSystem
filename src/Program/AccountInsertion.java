package Program;

public class AccountInsertion {

    public static void InsertAccounts(){
        try{
            if (RegistrationSystem.registerUser(SecurityUtil.sanitizeInput("endUser"), SecurityUtil.generateSaltedHash("endPass34!"), UserRole.END_USER, SecurityLevel.BASE)) {
                System.out.println("Account created successfully");
            } else{
                System.out.println("EndUser account creation failed");
            }
            if (RegistrationSystem.registerUser(SecurityUtil.sanitizeInput("adminUser"), SecurityUtil.generateSaltedHash("adminPass46!"), UserRole.ADMIN, SecurityLevel.ADMIN)) {
                System.out.println("Account created successfully");
            } else{
                System.out.println("Admin account creation failed");
            }
            if (RegistrationSystem.registerUser(SecurityUtil.sanitizeInput("techUser"), SecurityUtil.generateSaltedHash("techPass23!"), UserRole.TECHNICIAN, SecurityLevel.TOPLEVEL)) {
            } else{
                System.out.println("Technician account creation failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(){
        InsertAccounts();
    }
}
