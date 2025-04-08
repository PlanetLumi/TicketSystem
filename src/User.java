public class User {
    private String username;
    private String passwordHash;   // store a hashed & salted password
    private UserRole role;         //enum ADMIN, TECHNICIAN, END_USER
    private SecurityLevel securityLevel; //enum BASE, ADMIN, TOPLEVEL

    public User(String username, String passwordHash, UserRole role, SecurityLevel securityLevel) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.securityLevel = securityLevel;
    }

    // Getters and setters:
    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }
}