public class Counter implements Reader {
    /**
     * You may add your fields here.
     */
    
    /**
     * Initializes a Counter.
     *
     * @param numServers
     *          The number of servers which it must read from.
     */
    public Counter(int numServers) {
        //TODO: Implement me!
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
        //TODO: Implement me!
    }
    
    /**
     * You may add other methods here.
     */
}

class CountingServer implements Server {
    /**
     * You may add your fields here.
     */
    
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
    public CountingServer(Counter counter, int processNum) {
        //TODO: Implement me!
    }
    
    /**
     * Increments the value of the Counter.
     * 
     * This method must add one to the value of all future calls to read()
     * (on the Counter from the constructor) linearized after this call.
     * Each CountingServer object will only be operated on by one thread.
     */
    public void inc() {
        //TODO: Implement me!
    }
}

