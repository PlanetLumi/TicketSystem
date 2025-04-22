package Program;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Program.PriorityQueue that uses a min-heap in memory, logs changes, and supports
 * encrypted snapshotting with role-based checks.
 */
public class PriorityQueue implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Lock lock = new ReentrantLock();
    private Ticket[] heap;
    private int size;

    private String logFilePath;
    private String snapshotFilePath;

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public PriorityQueue(int capacity, String logFilePath, String snapshotFilePath) {
        this.heap = new Ticket[capacity];
        this.size = 0;
        this.logFilePath = logFilePath;
        this.snapshotFilePath = snapshotFilePath;
    }

    public boolean isEmpty() { return size == 0; }
    public int getSize() { return size; }

    // ----------------------------
    //  NORMAL HEAP OPERATIONS
    // ----------------------------
// (inside Program.PriorityQueue class)

    // helper to save a snapshot whenever the queue changes
    private void autoSnapshot(User user) {
        try {
            // always use the logged‑in user for the check
            saveSnapshotBinary(user);
        } catch (Exception e) {
            // snapshot failures shouldn’t kill your app
            System.err.println("Auto‑snapshot failed: " + e.getMessage());
        }
    }

    public void addTicket(Ticket ticket, User user) {
        lock.lock();
        try {
            if (size == heap.length) throw new RuntimeException("Heap is full");
            heap[size++] = ticket;
            heapifyUp(size - 1);
            SecurityUtil.logEvent("Program.User " + user.getUsername() + " created ticket...", "TCREATION");
        } finally {
            lock.unlock();
        }
        autoSnapshot(user);
    }

    public boolean updateTicketPriority(int ticketID, int newPriority, User user) {
        boolean ok;
        lock.lock();
        try {
            int idx = findIndexByID(ticketID);
            if (idx == -1) return false;
            heap[idx].setPriority(newPriority);
            heapifyUp(idx);
            heapifyDown(idx);
            SecurityUtil.logEvent("Program.User " + user.getUsername() + " updated ticket...", "TUPDATE");
            ok = true;
        } finally {
            lock.unlock();
        }
        if (ok) autoSnapshot(user);
        return ok;
    }

    public boolean deleteTicket(int ticketID, User user) {
        boolean ok;
        lock.lock();
        try {
            int idx = findIndexByID(ticketID);
            if (idx == -1) return false;
            heap[idx] = heap[--size];
            heap[size] = null;
            heapifyDown(idx);
            SecurityUtil.logEvent("Program.User " + user.getUsername() + " deleted ticket...", "TDELETE");
            ok = true;
        } finally {
            lock.unlock();
        }
        if (ok) autoSnapshot(user);
        return ok;
    }

    public Ticket pollTicket(User user) {
        Ticket result = null;
        lock.lock();
        try {
            if (size == 0) return null;
            // find first accessible
            for (int i = 0; i < size; i++) {
                if (heap[i].getSecurityLevel().ordinal() <= user.getSecurityLevel().ordinal()) {
                    result = heap[i];
                    deleteTicket(result.getTicketID(), user);
                    SecurityUtil.logEvent("Program.User " + user.getUsername() + " polled ticket...", "TUPDATE");
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
        if (result != null) autoSnapshot(user);
        return result;
    }


    public List<Ticket> listAccessibleTickets(User user) {
        List<Ticket> accessible = new ArrayList<>();
        lock.lock();
        try {
            for (int i = 0; i < size; i++) {
                if (heap[i].getSecurityLevel().ordinal() <= user.getSecurityLevel().ordinal()) {
                    accessible.add(heap[i]);
                }
            }
        } finally {
            lock.unlock();
        }
        return accessible;
    }

    public List<Ticket> searchAccessibleTickets(String titleQuery, User user) {
        List<Ticket> matches = new ArrayList<>();
        lock.lock();
        try {
            for (int i = 0; i < size; i++) {
                Ticket t = heap[i];
                if (t.getSecurityLevel().ordinal() <= user.getSecurityLevel().ordinal() &&
                        t.getTitle().toLowerCase().contains(titleQuery.toLowerCase())) {
                    matches.add(t);
                }
            }
        } finally {
            lock.unlock();
        }
        return matches;
    }

    public Ticket peek() {
        lock.lock();
        try {
            return isEmpty() ? null : heap[0];
        } finally {
            lock.unlock();
        }
    }

    // --- Snapshot & Security Features ---
    /**
     * Saves the entire queue to an encrypted snapshot file (only ADMIN).
     */
    public void saveSnapshotBinary(User currentUser) throws Exception {
        if (!SecurityUtil.hasRequiredPrivileges(currentUser, SecurityLevel.ADMIN)) {
            SecurityUtil.logEvent("Unauthorized snapshot attempt by user " + currentUser.getUsername(), "TCLOSE");
            System.out.println("ERROR: You do not have privileges to snapshot!");
            return;
        }
        if (snapshotFilePath == null) return;

        lock.lock();
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(this);
            }
            byte[] plain = bos.toByteArray();
            byte[] cipherData = SecurityUtil.encryptGcm(plain);
            String encoded = Base64.getEncoder().encodeToString(cipherData);

            String temp = snapshotFilePath + ".temp";
            try (FileWriter fw = new FileWriter(temp)) {
                fw.write(encoded);
            }
            SecurityUtil.setFilePermissions(temp);
            File tf = new File(temp), ff = new File(snapshotFilePath);
            if (ff.exists()) ff.delete();
            tf.renameTo(ff);
            SecurityUtil.setFilePermissions(snapshotFilePath);
            System.out.println("Snapshot saved to " + snapshotFilePath);
        } finally {
            lock.unlock();
            SecurityUtil.logEvent("Program.User " + currentUser.getUsername() + " completed snapshot.", "TCLOSE");
        }
    }

    /**
     * Loads the queue from an encrypted snapshot file.
     */
    public static PriorityQueue loadFromSnapshotBinary(String snapshotPath) throws Exception {
        File snap = new File(snapshotPath);
        if (!snap.exists()) {
            System.out.println("No snapshot file found at " + snapshotPath);
            return null;
        }
        String encoded = SecurityUtil.readFileAsString(snapshotPath);
        byte[] cipherData = Base64.getDecoder().decode(encoded);
        byte[] raw = SecurityUtil.decryptGcm(cipherData);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(raw))) {
            PriorityQueue q = (PriorityQueue) ois.readObject();
            System.out.println("Successfully loaded queue from snapshot: " + snapshotPath);
            return q;
        }
    }

    /**
     * Truncates (deletes) the append-only log. Only ADMIN.
     */
    public void truncateLog(User currentUser) throws IOException {
        if (!SecurityUtil.hasRequiredPrivileges(currentUser, SecurityLevel.ADMIN)) {
            SecurityUtil.logEvent("Unauthorized log truncate by " + currentUser.getUsername(), "TCLOSE");
            System.out.println("ERROR: No privileges to truncate the log!");
            return;
        }
        if (logFilePath == null) return;
        lock.lock();
        try {
            File f = new File(logFilePath);
            if (f.exists()) f.delete();
            SecurityUtil.logEvent("Program.User " + currentUser.getUsername() + " truncated log.", "TCLOSE");
        } finally {
            lock.unlock();
        }
    }

    // -----------
    // HEAP HELPERS
    // -----------
    private void heapifyUp(int i) {
        while (i > 0) {
            int p = (i - 1) / 2;
            if (heap[i].getPriority() < heap[p].getPriority()) {
                swap(i, p); i = p;
            } else break;
        }
    }
    private void heapifyDown(int i) {
        while (true) {
            int l = 2*i+1, r = 2*i+2, s = i;
            if (l < size && heap[l].getPriority() < heap[s].getPriority()) s=l;
            if (r < size && heap[r].getPriority() < heap[s].getPriority()) s=r;
            if (s!=i) { swap(i,s); i=s; } else break;
        }
    }
    private void swap(int a, int b) { Ticket t=heap[a]; heap[a]=heap[b]; heap[b]=t; }
    private int findIndexByID(int id) { for(int i=0;i<size;i++) if(heap[i].getTicketID()==id) return i; return -1; }
    public int getMaxTicketID() { int m=0; lock.lock(); try{ for(int i=0;i<size;i++) m=Math.max(m, heap[i].getTicketID()); } finally{lock.unlock();} return m; }
    public List<Ticket> getAllTickets(){ List<Ticket> list=new ArrayList<>(); lock.lock(); try{ for(int i=0;i<size;i++) list.add(heap[i]); } finally{lock.unlock();} return list; }
}
