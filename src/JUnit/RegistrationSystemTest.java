package JUnit;

import Program.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationSystemTest {
    @TempDir Path tmp;
    private Path accountsFile, accountsCopy;

    @BeforeEach
    void setup() {
        accountsFile = tmp.resolve("accounts.csv");
        accountsCopy = tmp.resolve("accounts2.csv");
        // Create an empty accounts file with a header
        try (PrintWriter pw = new PrintWriter(accountsFile.toFile())) {
            pw.println("username,role,securityLevel,passwordHash");
        } catch (IOException e) { fail(e); }

        // Ensure the persistence layer uses temp files
        AccountPersistence.setPaths(accountsFile.toString(), accountsCopy.toString());
        // Clear any session user
        SessionManager.getInstance().clearSession();
    }

    @Test
    void rejectsWeakPassword() {
        boolean ok = RegistrationSystem.registerUser("u","weak", UserRole.END_USER, SecurityLevel.BASE);
        assertFalse(ok);
    }

    @Test
    void allowsSelfRegistrationAtBaseOnly() {
        assertTrue(RegistrationSystem.registerUser("joe","Aa1@aaaa",UserRole.END_USER,SecurityLevel.BASE));
        // Cannot self-register at TOPLEVEL
        assertFalse(RegistrationSystem.registerUser("jane","Aa1@aaaa",UserRole.END_USER,SecurityLevel.TOPLEVEL));
    }

    @Test
    void rejectsDuplicateUsernameIgnoringCase() {
        assertTrue(RegistrationSystem.registerUser("bob","Aa1@aaaa",UserRole.END_USER,SecurityLevel.BASE));
        assertFalse(RegistrationSystem.registerUser("BOB","Aa1@bbbb",UserRole.END_USER,SecurityLevel.BASE));
    }

    @Test
    void enforcesSessionUserCannotCreateHigherLevel() {
        // Log in a BASE user
        User sess = new User("admin","h",UserRole.END_USER,SecurityLevel.BASE);
        SessionManager.getInstance().setCurrentUser(sess);
        // Cannot create a TOPLEVEL account
        assertFalse(RegistrationSystem.registerUser("x","Aa1@aaaa",UserRole.TECHNICIAN,SecurityLevel.TOPLEVEL));
        //Create another BASE account
        assertTrue(RegistrationSystem.registerUser("y","Aa1@aaaa",UserRole.END_USER,SecurityLevel.BASE));
    }
}
