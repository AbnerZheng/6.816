import java.util.concurrent.locks.ReentrantLock;

/**
 * Lock-Free Closed-Address Hash Table
 *
 * This design uses a regular ReentrantLock and does not require a readWriteLock, since the contains() method does not
 * ever make use of the readLock() functionality. This design has the additional requirement that if a contains() and
 * an add() or a remove() method proceed concurrently that the result of the contains() is linearizable with the other
 * call.
 *
 * The table resizes when the size of any bucket exceeds a max bucket size threshold.
 */
class LockFreeHashTable<T> implements HashTable<T> {

    private SerialList<T,Integer>[] table;
    private final ReentrantLock[] locks;
    private final int maxBucketSize;
    private int mask;

    // Invariants:
    // mask = 1 << logSize - 1
    // table.length = mask + 1

    /**
     * @param logSize the starting capacity of the hash table is 2**(logSize)
     * @param maxBucketSize the max average size of a bucket before resizing
     */
    @SuppressWarnings("unchecked")
    public LockFreeHashTable(int logSize, int maxBucketSize) {
        this.mask = (1 << logSize) - 1;
        this.maxBucketSize = maxBucketSize;
        this.table = new SerialList[mask + 1];
        this.locks = new ReentrantLock[mask + 1];
        for (int i = 0; i <= mask; i++) {
            this.locks[i] = new ReentrantLock();
        }
    }

    /**
     * Adds the key value pair to the hash table.
     * * Overwrites the existing value if key already exists.
     * @param key key to be added
     * @param val corresponding value
     */
    public void add(int key, T val) {
        try {
            acquire(key);
            addNoCheck(key, val);
        } finally {
            release(key);
        }
        resizeIfNecessary(key);
    }

    /**
     * Removes the key from the hash table.
     * @param key key to be removed
     * @return true iff the key was successfully removed
     */
    public boolean remove(int key) {
        try {
            acquire(key);
            if (table[key & mask] != null)
                return table[key & mask].remove(key);
            else
                return false;
        } finally {
            release(key);
        }
    }

    /**
     * Returns whether the key is in the hash table.
     * @param key key to check for
     * @return true iff the key is in the hash table
     */
    public boolean contains(int key) {
        return table[key & mask] != null && table[key & mask].contains(key);
    }

    /**
     * Acquires a lock
     * @param key the key the lock should correspond to
     */
    private void acquire(int key) {
        locks[key % locks.length].lock();
    }

    /**
     * Releases a lock
     * @param key the key the lock should correspond to
     */
    private void release(int key) {
        locks[key % locks.length].unlock();
    }

    /**
     * Adds the key value pair to the hash table without checking for max bucket size or acquiring locks.
     * @param key
     * @param x
     */
    private void addNoCheck(int key, T x) {
        int index = key & mask;
        if (table[index] == null)
            table[index] = new SerialList<T,Integer>(key,x);
        else
            table[index].addNoCheck(key,x);
    }

    /**
     * Resizes the hash table if the bucket corresponding to the given key exceeds the max bucket size.
     * @param key key to check the bucket for
     */
    private void resizeIfNecessary(int key) {
        while (table[key & mask] != null && table[key & mask].getSize() > maxBucketSize)
            resize();
    }

    /**
     * Doubles the size of the hash table and reassigns all key value pairs.
     */
    @SuppressWarnings("unchecked")
    private void resize() {
        // Acquire all write locks in sequential order
        for (int i = 0; i < locks.length; i++) {
            acquire(i);
        }

        // Resize the table
        mask = 2 * mask + 1;
        SerialList<T,Integer>[] newTable = new SerialList[2 * table.length];
        for (int i = 0; i < table.length; i++) {
            if (table[i] == null)
                continue;
            SerialList<T,Integer>.Iterator<T,Integer> iterator = table[i].getHead();
            while (iterator != null) {
                int newIndex = iterator.key & mask;
                if (newTable[newIndex] == null)
                    newTable[newIndex] = new SerialList<T,Integer>(iterator.key, iterator.getItem());
                else
                    newTable[newIndex].addNoCheck(iterator.key, iterator.getItem());
                iterator = iterator.getNext();
            }
        }
        table = newTable;

        // Release all write locks
        for (int i = 0; i < locks.length; i++) {
            release(i);
        }
    }
}