package Program;

public class SessionManager {
    // A private static instance of the Program.SessionManager (Singleton)
    private static SessionManager instance;

    // Instance variable to hold the current user
    private User currentUser;

    // Private constructor to prevent instantiation from other classes.
    private SessionManager() { }

    // Public method to get the single instance of Program.SessionManager.
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // Getter for the current user.
    public User getCurrentUser() {
        return currentUser;
    }

    // Setter for the current user.
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    // Clear the session (e.g., on logout)
    public void clearSession() {
        currentUser = null;
    }
}
