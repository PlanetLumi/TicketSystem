package Program;

public class SessionManager {
    // private static instance of SessionManager (Singleton)
    private static SessionManager instance;

    //hold the current user
    private User currentUser;


    private SessionManager() { }

    //Grabber to find instance throughout program
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // Get current user
    public User getCurrentUser() {
        return currentUser;
    }

    // Set current user.
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    // Clear the session
    public void clearSession() {
        currentUser = null;
    }
}
