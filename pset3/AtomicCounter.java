public class AtomicCounter implements Reader {

    protected final Snapshot snapshot;

    /**
     * Initializes a Counter.
     *
     * @param numServers
     *          The number of servers which it must read from.
     */
    public AtomicCounter(int numServers) {
        snapshot = new AtomicSnapshot(numServers);
    }

    /**
     * Returns the value of the Counter.
     *
     * This method returns the sum of all increments linearized before
     * this method's linearization point.
     * Your implementation only needs to support one thread that calls read().
     *
     * @return
     *          The value of the Counter.
     */
    public int read() {
        // The linearization point
        int[] scan = snapshot.scan();

        // Sum the values in the snapshot
        int sum = 0;
        for (int i = 0; i < scan.length; i++) {
            sum += scan[i];
        }
        return sum;
    }

    public void update(int processNum, int val) {
        snapshot.update(processNum, val);
    }
}

class CountingServer implements Server {

    protected final AtomicCounter counter;
    protected final int processNum;
    protected int value;

    /**
     * Initializes a CountingServer.
     *
     * @param counter
     *          The Counter which receives data from this server.
     *
     * @param processNum
     *          A unique integer that represents the ID of the incrementing
     *          process.
     */
    public CountingServer(AtomicCounter counter, int processNum) {
        this.counter = counter;
        this.processNum = processNum;
        this.value = 0;
    }

    /**
     * Increments the value of the Counter.
     *
     * This method must add one to the value of all future calls to read()
     * (on the Counter from the constructor) linearized after this call.
     * Each CountingServer object will only be operated on by one thread.
     */
    public void inc() {
        // Update the internal value
        value++;

        // Update the counter value (linearization point)
        counter.update(processNum, value);
    }
}

