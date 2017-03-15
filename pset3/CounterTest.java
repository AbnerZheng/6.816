import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

interface Snapshot {
    /**
     * Gets an atomic snapshot of the values in Snapshot.
     * 
     * This method is called by "scanner" threads. This method
     * returns the values in each slot of the Snapshot object.
     * 
     * @return
     *          The value of the Snapshot.
     */
    public int[] scan();
    
    /**
     * Updates the value of the Snapshot in the slot for a given thread.
     * 
     * This method is called by "updater" threads. This method sets the slot
     * in the snapshot at location index to have value val.
     * 
     * @param index
     *          The index in the array to update.
     * @param val
     *          The value to be written.
     */
    public void update(int index, int val);
}

interface Reader {
    public int read();
}

interface Server {
    public void inc();
}

////////////////////////////////////////////////////////////////////////////////

class ServerRunner implements Runnable {
    public final AtomicBoolean done;
    private final Server server;
    private final int processNum;
    private final int numIncs;
    public ServerRunner(Counter counter, int processNum, int numIncs) {
        done = new AtomicBoolean(false);
        this.server = new CountingServer(counter, processNum);
        this.processNum = processNum;
        this.numIncs = numIncs;
    }
    public void run() {
        for (int i = 0; i < numIncs; i++) {
            server.inc();
        }
        done.set(true);
    }
}

class ReaderRunner implements Runnable {
    public final AtomicBoolean done;
    private final Reader reader;
    public int scanCount;
    public int[] recentSnapshots;
    
    public ReaderRunner(Reader reader) {
        this.done = new AtomicBoolean(false);
        this.reader = reader;
        this.scanCount = 0;
        this.recentSnapshots = new int[10];
    }
    public void run() {
        while(!done.get()) {
            final int curVal = reader.read();
            scanCount++;
            recentSnapshots[scanCount % recentSnapshots.length] = curVal;
        }
    }
}

class StopWatch {
    private long startTime;
    public StopWatch() {
        startTime = System.nanoTime();
    }
    public void reset() {
        startTime = System.nanoTime();
    }
    public double peek() {
        return (System.nanoTime() - startTime) / 1000000.0; // milliseconds
    }
}

public class CounterTest {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("ERROR! Usage: java CounterTest [numTrials] [numIncs] [serverCount]");
            return;
        }
        
        final int numTrials = Integer.parseInt(args[0]);
        final int numIncs = Integer.parseInt(args[1]);
        final int serverCount = Integer.parseInt(args[2]);
        
        StopWatch sw = new StopWatch();
        
        final int readerCount = 1;
        final int numThreads = serverCount + readerCount;
        
        double incPerMS[] = new double[numTrials];
        double readsPerMS[] = new double[numTrials];
        
        ServerRunner[] server = new ServerRunner[serverCount];
        ReaderRunner reader;
        Thread[] workerThread = new Thread[numThreads];
        
        for (int t = 0; t < numTrials; t++) {
            Counter counter = new Counter(numThreads);
            
            reader = new ReaderRunner(counter);
            workerThread[serverCount] = new Thread(reader);
            
            for (int i = 0; i < serverCount; i++) {
                server[i] = new ServerRunner(counter, i, numIncs);
                workerThread[i] = new Thread(server[i]);
            }
            
            sw.reset();
            
            for (int i = 0; i < numThreads; i++ ) {
                workerThread[i].start();
            }
            
            // wait until each server is done
            boolean allDone = false;
            while (!allDone) {
                allDone = true;
                for (int i = 0; i < serverCount; i++ ) {
                    if (server[i].done.get() == false) {
                        allDone = false;
                    }
                }
            }
            
            // notify all of the readers to stop
            reader.done.set(true);
            
            // join every thread
            for (int i = 0; i < numThreads; i++ ) {
                try {
                    workerThread[i].join();
                } catch (InterruptedException ignore) {;}      
            }
            double ms = sw.peek();
            
            incPerMS[t] = numIncs * serverCount * 1.0 / ms;
            
            readsPerMS[t] = 0.0;
            readsPerMS[t] += reader.scanCount;
            readsPerMS[t] /= ms;
        }
        
        System.out.print(serverCount + " servers, " + readerCount + " readers :\n\tIncs/ms = [");
        for (int t = 0; t < numTrials; t++) {
            System.out.print(" " + String.format("%.2f",incPerMS[t]));
        }
        System.out.print(" ]\n\tReads/ms = [");
        for (int t = 0; t < numTrials; t++) {
            System.out.print(" " + String.format("%.2f",readsPerMS[t]));
        }
        System.out.println(" ]");
    }
	
}
