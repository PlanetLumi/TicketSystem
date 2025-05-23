package Program;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//Priority queue using Min-Heap system
public class PriorityQueue implements Serializable {
    private static final long serialVersionUID = 1L;

    //Multithreading ticket lock
    private final Lock lock = new ReentrantLock();
    private Ticket[] heap;
    private int size;
    private transient TicketFileHandler fileHandler;

    private String logFilePath;
    private String snapshotFilePath;

    //Used for time stamping
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    //For JUnit testing
    public PriorityQueue(int capacity) {
        this(capacity, null, null);
    }

    //Main instantiation
    public PriorityQueue(int capacity,
                         String logFilePath,
                         String snapshotFilePath) {

        this.heap = new Ticket[capacity];
        this.size = 0;
        this.logFilePath = logFilePath;
        this.snapshotFilePath = snapshotFilePath;

        this.fileHandler = (logFilePath != null && snapshotFilePath != null)
                ? new TicketFileHandler(logFilePath, snapshotFilePath)
                : null;
    }

    public boolean isEmpty() { return size == 0; }
    public int getSize() { return size; }
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (logFilePath != null && snapshotFilePath != null) {  // recreate helper
            this.fileHandler = new TicketFileHandler(logFilePath, snapshotFilePath);
        }
    }

    //save a snapshot whenever the queue changes
    private void autoSnapshot(User user) {
        if (user == null) return;
        //Cannot be done if not admin
        if (SecurityUtil.hasRequiredPrivileges(user, SecurityLevel.ADMIN)) {
            try {
                saveSnapshotBinary(user);
            } catch (Exception e) {
                System.err.println("Auto-snapshot failed: " + e.getMessage());
            }
        }
    }

    public void addTicket(Ticket ticket, User user) {
        lock.lock();
        try {
            if (size == heap.length) throw new RuntimeException("Heap is full");
            heap[size++] = ticket;
            heapifyUp(size - 1);
            if (fileHandler != null) fileHandler.logAdd(ticket);
            String actor = (user != null ? user.getUsername() : "SYSTEM");
            //Log user
            SecurityUtil.logEvent("User " + actor + " created ticket...", "TCREATION");
        } finally {
            //Lock thread
            lock.unlock();
        }
        //Save new ticket set
        autoSnapshot(user);
    }

    public boolean updateTicketPriority(int ticketID, int newPriority, User user) {
        boolean ok;
        lock.lock();
        try {
            int idx = findIndexByID(ticketID);
            if (idx == -1) return false;
            //Take found Id and inputted priority and update
            heap[idx].setPriority(newPriority);
            heapifyUp(idx);
            heapifyDown(idx);
            String actor = (user != null ? user.getUsername() : "SYSTEM");
            //Log
            SecurityUtil.logEvent("User " + actor + " updated ticket...", "TUPDATE");
            if (fileHandler != null) fileHandler.logUpdate(heap[idx]);
            ok = true;
        } finally {
            //Lock thread
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
            if (fileHandler != null) fileHandler.logDelete(ticketID);
            String actor = (user != null ? user.getUsername() : "SYSTEM");
            SecurityUtil.logEvent("User " + actor + " deleted ticket...", "TDELETE");
            ok = true;
        } finally {
            lock.unlock();
        }
        if (ok) autoSnapshot(user);
        return ok;
    }

    //Pop the head element
    public Ticket pollTicket(User user) {
        Ticket result = null;
        //Lock to get around corruption
        lock.lock();
        try {
            if (size == 0) return null;
            //Linear scan of heap
            for (int i = 0; i < size; i++) {
                //Tackles security but lowers polling speed
                if (user == null ||
                        heap[i].getSecurityLevel().ordinal() <= user.getSecurityLevel().ordinal()) {
                    //Return ticket allowed to see
                    result = heap[i];
                    //Remove ticket from queue
                    deleteTicket(result.getTicketID(), user);
                    String actor = (user != null ? user.getUsername() : "SYSTEM");
                    SecurityUtil.logEvent("User " + actor + " polled ticket...", "TUPDATE");
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
        if (result != null) autoSnapshot(user);
        return result;
    }

    //List tickets user can access
    public MyList<Ticket> listAccessibleTickets(User user) {
        MyArrayList<Ticket> acc = new MyArrayList<>();
        lock.lock();
        try {
            //Linear look up
            for (int i = 0; i < size; i++) {
                if (user == null ||
                        heap[i].getSecurityLevel().ordinal() <= user.getSecurityLevel().ordinal()) {
                    acc.add(heap[i]);
                }
            }
        } finally {
            lock.unlock();
        }
        return acc;
    }


    public MyList<Ticket> searchAccessibleTickets(String titleQuery, User user) {
        MyArrayList<Ticket> matches = new MyArrayList<>();
        lock.lock();
        try {
            for (int i = 0; i < size; i++) {
                Ticket t = heap[i];
                if ((user == null ||
                        t.getSecurityLevel().ordinal() <= user.getSecurityLevel().ordinal()) &&
                        t.getTitle().toLowerCase().contains(titleQuery.toLowerCase())) {
                    matches.add(t);
                }
            }
        } finally {
            lock.unlock();
        }
        return matches;
    }

    //Find top of queue
    public Ticket peek() {
        lock.lock();
        try {
            return isEmpty() ? null : heap[0];
        } finally {
            lock.unlock();
        }
    }

    //Save queue as binary to condense space
    public void saveSnapshotBinary(User currentUser) throws Exception {
        if (!SecurityUtil.hasRequiredPrivileges(currentUser, SecurityLevel.ADMIN)) {
            SecurityUtil.logEvent("Unauthorized snapshot attempt by user " + currentUser.getUsername(), "TCLOSE");
            System.out.println("ERROR: You do not have privileges to snapshot!");
            return;
        }
        if (snapshotFilePath == null) return;

        lock.lock();
        try {
            //Turn queue into bytes
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(this);
            }
            byte[] plain = bos.toByteArray();
            byte[] cipherData = SecurityUtil.encryptGcm(plain);
            //Encrypt
            String encoded = Base64.getEncoder().encodeToString(cipherData);

            //Save in temp to not overwrite main
            String temp = snapshotFilePath + ".temp";
            try (FileWriter fw = new FileWriter(temp)) {
                fw.write(encoded);
            }
            SecurityUtil.setFilePermissions(temp);
            //Save to main
            File tf = new File(temp), ff = new File(snapshotFilePath);
            if (ff.exists()) ff.delete();
            tf.renameTo(ff);
            SecurityUtil.setFilePermissions(snapshotFilePath);
            System.out.println("Snapshot saved to " + snapshotFilePath);
        } finally {
            lock.unlock();
            SecurityUtil.logEvent("User " + currentUser.getUsername() + " completed snapshot.", "TCLOSE");
        }
    }
    //Load binary file
    public static PriorityQueue loadFromSnapshotBinary(String snapshotPath) throws Exception {
        File snap = new File(snapshotPath);
        if (!snap.exists()) {
            System.out.println("No snapshot file found at " + snapshotPath);
            return null;
        }
        //Decode encrypted snapshot
        String encoded = SecurityUtil.readFileAsString(snapshotPath);
        byte[] cipherData = Base64.getDecoder().decode(encoded);
        byte[] raw = SecurityUtil.decryptGcm(cipherData);
        //implement as queue
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(raw))) {
            PriorityQueue q = (PriorityQueue) ois.readObject();
            System.out.println("Successfully loaded queue from snapshot: " + snapshotPath);
            return q;
        }
    }

    //HELPERS UTILISING BINARY HEAP FUNCTIONALITY

    private void heapifyUp(int i) {
        while (i > 0) {
            int p = (i - 1) / 2; //PARENT INDEX
            if (heap[i].getPriority() < heap[p].getPriority()) {
                swap(i, p); i = p; //BUBBLE SORT TO TOP OF LIST
            } else break;
        }
    }
    private void heapifyDown(int i) {
        while (true) {
            int l = 2*i+1, r = 2*i+2, s = i; //Binary search
            if (l < size && heap[l].getPriority() < heap[s].getPriority()) s = l;
            if (r < size && heap[r].getPriority() < heap[s].getPriority()) s = r;
            if (s != i) //Finds smallest child
            { swap(i, s); //bubble sort down
                i = s; } else break;
        }
    }

    private void swap(int a, int b) { Ticket t = heap[a]; heap[a] = heap[b]; heap[b] = t; } //Swap two features, used for bubble sorting
    private int findIndexByID(int id) { for (int i = 0; i < size; i++) if (heap[i].getTicketID() == id) return i; return -1; } //Linear search
    public int getMaxTicketID() { int m = 0; lock.lock(); try { for (int i = 0; i < size; i++) m = Math.max(m, heap[i].getTicketID()); } finally { lock.unlock(); } return m; } //Finds lagest ID for searching parameter
    public MyList<Ticket> getAllTickets() {
        MyArrayList<Ticket> list = new MyArrayList<>(); //Own list
        lock.lock();
        try { for (int i = 0; i < size; i++)
            list.add(heap[i]); //Add all into list to use
        } finally {
            lock.unlock();
        } return list;
    }
}
