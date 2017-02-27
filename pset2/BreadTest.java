import java.util.concurrent.atomic.AtomicBoolean;

class SmallBreadBox implements BreadBox {
    private AtomicBoolean hasLoaf;
    SmallBreadBox() { hasLoaf = new AtomicBoolean(false); }
    public void addLoaf() {
        if (! hasLoaf.compareAndSet(false, true)) {
            System.out.println("Failure: Attempting to put two loaves of bread in one bread box!");
            System.exit(1);
        }
    }
    public boolean isEmpty() {
        return ! hasLoaf.get();
    }
    public void clear() {
        hasLoaf.set(false);
    }
}

class Worker implements Runnable {
    AtomicBoolean done;
    final BreadHouse house;
    final BreadBox box;
    final boolean eatsOut;
    public Worker(BreadHouse house, BreadBox box, boolean eatsOut) {
        done = new AtomicBoolean(false);
        this.house = house;
        this.box = box;
        this.eatsOut = eatsOut;
    }
    public void run() {
        long threadId = Thread.currentThread().getId() % BreadTest.workerCount;
        
        if (threadId == 0) {
            if (eatsOut) {
                done.set(true);
                // System.out.println("Alex ate out instead.");
            }
            else {
                house.alex(box);
                done.set(true);
                // System.out.println("Alex is done!");
            }
        }
        else {
            if (eatsOut) {
                done.set(true);
                // System.out.println("Sam ate out instead.");
            }
            else {
                house.sam(box);
                done.set(true);
                // System.out.println("Sam is done!");
            }
        }
    }
}

public class BreadTest {
    static final int workerCount = 2;
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java BreadTest [numDays]");
            return;
        }
        final int days = Integer.parseInt(args[0]);
        
        Worker[] worker = new Worker[workerCount];
        Thread[] workerThread = new Thread[workerCount];
        
        int numSuccess = 0;
        
        BreadHouse house = new BreadHouse();
        SmallBreadBox box = new SmallBreadBox();
        
        for (int eatOutId = 0; eatOutId < days; eatOutId++) {
            //System.out.println("Day #" + (1 + eatOutId));
            
            box.clear();
            
            for (int i = 0; i < workerCount; i++ ) {
                worker[i] = new Worker(house, box, eatOutId == i);
                workerThread[i] = new Thread(worker[i]);
            }
            
            for (int i = 0; i < workerCount; i++ ) {
                workerThread[i].start();
            }
            
            boolean allDone = false;
            while (!allDone) {
                allDone = true;
                for (int i = 0; i < workerCount; i++ ) {
                    if (worker[i].done.get() == false) {
                        allDone = false;
                    }
                }
            }
            
            for (int i = 0; i < workerCount; i++ ) {
                try {
                    workerThread[i].join();
                } catch (InterruptedException ignore) {;}      
            }
            
            if (box.isEmpty()) {
                System.out.println("Day #" + (1 + eatOutId));
                System.out.println("Failure! Didn't get bread!");
                return;
            }
            else {
                // System.out.println("Success! Got bread!");
                numSuccess++;
            }
        }
        
        System.out.println("Successes: " + numSuccess + " / " + days);
    }
}
