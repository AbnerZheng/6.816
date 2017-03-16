import java.util.concurrent.atomic.*;

public class CLHLock implements Lock {
    /**
     * Add your fields here
     */

    public CLHLock() {
        //TODO: Implement me!
    }

    public void lock() {
        //TODO: Implement me!
    }

    public void unlock() {
        //TODO: Implement me!
    }

    /**
     * Checks if the calling thread observes another thread concurrently
     * calling lock(), in the critical section, or calling unlock().
     * 
     * @return
     *          true if another thread is present, else false
     */
    public boolean isContended() {
        //TODO: Implement me!
    }

    class QNode {
        /**
        * Add your fields here
        */
    }
}
