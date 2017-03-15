import java.util.concurrent.atomic.*; // for AtomicXXX classes
import java.util.concurrent.locks.*;  // for Lock and ReentrantLock

class AtomicSnapshot implements Snapshot {
    /**
     * The current values of the snapshot object.
     * 
     * This is where you can store the values that update will change and that
     * scan will read and return. While all operations on this array are atomic
     * and changes will be seen across all threads, it does not have a built-in
     * atomic snapshot method.
     */
    protected final AtomicIntegerArray array;
    
    /**
     * AtomicSnapshot constructor.
     * 
     * Constructs a new AtomicSnapshot object with size numSlots. The initial
     * value of each slot is set to 0.
     * 
     * @param numSlots
     *          The size of the internal snapshot array.
     */
    public AtomicSnapshot(int numSlots) {
        //TODO: Implement me!
    }
    
    /**
     * Gets an atomic snapshot of the values in AtomicSnapshot.
     * 
     * This method returns a view of the values in each slot of the
     * AtomicSnapshot object such that for each index i, the last call of
     * update(i, view[i]) is the last update(i, val) that is linearized before
     * this method call. If no update(i, *) calls are linearized before this
     * method call, then view[i] == 0.
     * 
     * @return
     *          The value of the Snapshot.
     */
    public int[] scan() {
        //TODO: Implement me!
    }
    
    /**
     * Updates the value of the AtomicSnapshot in the slot for a given thread.
     * 
     * This method sets the slot in the AtomicSnapshot at location index to have
     * value val such that all calls to scan linearized after this method call
     * will read view[index] == val until after the next update(index, val2) for
     * some val != val2 is linearized.
     * 
     * @param index
     *          The index in the array to update.
     * @param val
     *          The value to be written.
     */
    public void update(int index, int val) {
        //TODO: Implement me!
    }
}
