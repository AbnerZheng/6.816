import java.util.concurrent.atomic.*;

class Worker implements Runnable {
    AtomicBoolean done;
    Counter cnt;
    int workerCount;
    OneBitLock lock;
    public Worker(Counter cnt, int workerCount, OneBitLock lock) {
        done = new AtomicBoolean(false);
        this.cnt = cnt;
        this.workerCount = workerCount;
        this.lock = lock;
    }
    static final int ITERS = 500;
    public void run() {
        long threadId = Thread.currentThread().getId();
        int processId = (int) threadId % workerCount;
        for (int i = 0; i < ITERS; i++) {
            lock.lock(processId);
            cnt.inc();
            lock.unlock(processId);
        }
        done.set(true);
        
        //System.out.println("Worker is done!");
    }
}

class Counter {
    AtomicInteger value;
    Counter() { value = new AtomicInteger(0); }
    public void inc() {
        final int N = 10000;
        for (int i = 0; i < N; i++) {
            value.set(value.get() + 1);
        }
        value.set(value.get() - N + 1);
    }
    public int getVal() {
        return value.get();
    }
    void reset() {
        value.set(0);
    }
}

class StopWatch {
    private long startTime;
    public StopWatch() {
        startTime = System.currentTimeMillis();
    }
    public void reset() {
        startTime = System.currentTimeMillis();
    }
    public long peek() {
        return System.currentTimeMillis() - startTime;
    }
}

public class LocksTest {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java LocksTest [numThreads] [numTrials]");
            return;
        }
        final int numThreads = Integer.parseInt(args[0]);
        final int numTrials = Integer.parseInt(args[1]);
        
        long[] times = new long[numTrials];
        StopWatch sw = new StopWatch();
        
        Counter cnt = new Counter();
        
        OneBitLock lock = new OneBitLock(numThreads);
        
        for (int trial = 0; trial < numTrials; trial++) {
            Worker[] worker = new Worker[numThreads];
            Thread[] workerThread = new Thread[numThreads];
            for (int i = 0; i < numThreads; i++ ) {
                worker[i] = new Worker(cnt, numThreads, lock);
                workerThread[i] = new Thread(worker[i]);
            }
            
            cnt.reset();
            sw.reset();
            
            for (int i = 0; i < numThreads; i++ ) {
                workerThread[i].start();
            }
            
            boolean allDone = false;
            while (!allDone) {
                allDone = true;
                for (int i = 0; i < numThreads; i++ ) {
                    if (worker[i].done.get() == false) {
                        allDone = false;
                    }
                }
            }
            
            for (int i = 0; i < numThreads; i++ ) {
                try {
                    workerThread[i].join();
                } catch (InterruptedException ignore) {;}      
            }
            
            times[trial] = sw.peek();
            
            if (Worker.ITERS * numThreads != cnt.getVal()) {
                System.out.println(
                    "Counter error! Expected " +
                    (Worker.ITERS * numThreads) +
                    " got " + cnt.getVal()
                );
            }
        }
        
        
        System.out.print("Threads = " + numThreads + ", Times (ms) = {");
        for (int trial = 0; trial < numTrials; trial++) {
            System.out.print(" " + times[trial]);
        }
        System.out.println(" }");
    }
    
}
