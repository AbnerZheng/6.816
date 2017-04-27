import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.ArrayList;
import java.lang.Math.*;
import java.util.concurrent.atomic.*;

/**
 * Linearly Probed Open-Address
 *
 * This is a standard linear probing style hash table, where each entry in the table has a counter which defines the
 * maximum number of steps necessary to find a previously added item - that is, if an add() method had to walk k steps
 * to find a vacant spot, the counter would be set to the max of the previous value and k. This allows both the
 * remove() and contains() methods to limit their searches. Beware of the potential for deadlock when grabbing locks
 * for multiple hash indices (as in add() and remove()) in the event that the home index is not vacant.
 *
 * The table resizes when k for any spots exceeds a certain max probe threshold.
 */

class LinearProbeHashTable<T> implements HashTable<T> {

    private AtomicReference<Node<T>[]> tableReference;
    private final ReentrantLock[] locks;
    private int maxProbes;

    /**
     * @param logSize the starting capacity of the hash table is 2**(logSize)
     * @param maxProbes the maximum number of probes for a slot before resizing
     */
    @SuppressWarnings("unchecked")
    public LinearProbeHashTable(int logSize, int maxProbes) {
        int size = 1 << logSize;
        this.tableReference = new AtomicReference((Node<T>[]) new Node[size]);
        this.locks = new ReentrantLock[size];
        this.maxProbes = maxProbes;
        for (int i = 0; i < size; i++) {
            this.locks[i] = new ReentrantLock();
        }
    }

    /**
     * Adds the key value pair to the hash table.
     * @param key key to be added
     * @param val corresponding value
     */
    public void add(int key, T val) {
        // Get the current table capacity
        Node<T>[] table = tableReference.get();
        int capacity = table.length;
        int startIndex = key & (capacity - 1);
        int currIndex = startIndex;

        // Probe up to maxProbes entries
        for (int k = 0; k < maxProbes; k++) {
            try {
                acquire(startIndex, currIndex);
                // Table was resized, try again
                if (!tableReference.compareAndSet(table, table))
                    break;

                if (table[currIndex] == null) {
                    // There is no node here
                    table[currIndex] = new Node<T>(key, val);
                    table[startIndex].k = Math.max(table[startIndex].k, k);
                    return;
                } else if (table[currIndex].deleted) {
                    // The node here was deleted
                    table[currIndex] = new Node<T>(key, val, table[currIndex].k);
                    table[startIndex].k = Math.max(table[startIndex].k, k);
                    return;
                } else if (table[currIndex].key == key) {
                    // The node here has the same key
                    table[currIndex].val = val;
                    return;
                }
            } finally {
                release(startIndex, currIndex);
            }
            currIndex = (currIndex + 1) % capacity;
        }

        // Resize the table and try again out of probes
        if (tableReference.compareAndSet(table, table)) resize();
        add(key, val);
    }

    /**
     * Removes the key from the hash table.
     * @param key key to be removed
     * @return true iff the key was successfully removed
     */
    public boolean remove(int key) {
        // Get the current table capacity
        Node<T>[] table = tableReference.get();
        int capacity = table.length;
        int startIndex = key & (capacity - 1);

        // Return false if the first entry does not exist
        Node<T> firstEntry = table[startIndex];
        if (firstEntry == null) {
            return false;
        }

        // Probe up to k entries
        for (int i = 0; i <= firstEntry.k; i++) {
            int currIndex = (startIndex + i) % capacity;
            try {
                acquire(currIndex);

                // The size of the table changed, try again
                if (!tableReference.compareAndSet(table, table))
                    break;

                // Hopefully delete the key
                if (table[currIndex] == null) {
                    return false;
                } else if (table[currIndex].deleted) {
                    continue;
                } else if (table[currIndex].key == key) {
                    table[currIndex].deleted = true;
                    return true;
                }
            } finally {
                release(currIndex);
            }
        }

        if (!tableReference.compareAndSet(table, table)) {
            return remove(key);
        }
        return false;
    }

    /**
     * Returns whether the key is in the hash table.
     * @param key key to check for
     * @return true iff the key is in the hash table
     */
    public boolean contains(int key) {
        // Get the current table capacity
        Node<T>[] table = tableReference.get();
        int capacity = table.length;
        int startIndex = key & (capacity - 1);

        // Return false if the first entry does not exist
        Node<T> firstEntry = table[startIndex];
        if (firstEntry == null) {
            return false;
        }

        // Probe up to k entries
        for (int i = 0; i <= firstEntry.k; i++) {
            int currIndex = (startIndex + i) % capacity;
            try {
                acquire(currIndex);

                // The size of the table changed, try again
                if (!tableReference.compareAndSet(table, table))
                    break;

                // Maybe find the key
                if (table[currIndex] == null) {
                    return false;
                } else if (table[currIndex].deleted) {
                    continue;
                } else if (table[currIndex].key == key) {
                    return true;
                }
            } finally {
                release(currIndex);
            }
        }

        if (!tableReference.compareAndSet(table, table)) {
            return contains(key);
        }
        return false;
    }

    private void acquire(int lock) {
        locks[lock % locks.length].lock();
    }

    private void release(int lock) {
        locks[lock % locks.length].unlock();
    }

    private void acquire(int lockOne, int lockTwo) {
        int numLocks = locks.length;
        locks[Math.min(lockOne % numLocks, lockTwo % numLocks)].lock();
        locks[Math.max(lockOne % numLocks, lockTwo % numLocks)].lock();
    }

    private void release(int lockOne, int lockTwo) {
        int numLocks = locks.length;
        locks[Math.min(lockOne % numLocks, lockTwo % numLocks)].unlock();
        locks[Math.max(lockOne % numLocks, lockTwo % numLocks)].unlock();
    }

    private boolean addNoCheck(Node<T>[] table, int key, T val) {
        int capacity = table.length;
        int startIndex = key & (capacity - 1);
        for (int i = 0; i < capacity; i++) {
            int currIndex = (startIndex + i) % capacity;

            // When adding without check, can only add to null spaces
            if (table[currIndex] == null) {
                table[currIndex] = new Node<T>(key, val);
                table[startIndex].k = Math.max(table[startIndex].k, i);
                return table[startIndex].k >= maxProbes;
            }
        }
        return true;
    }

    /**
     * Doubles the size of the hash table and reassigns all key value pairs.
     */
    @SuppressWarnings("unchecked")
    private void resize() {
        Node<T>[] table = tableReference.get();
        boolean needsResize = false;

        try {
            // Acquire all write locks in sequential order
            for (int i = 0; i < locks.length; i++) {
                acquire(i);
            }

            // Check if someone beat us to it
            if (!tableReference.compareAndSet(table, table))
                return;

            // Resize the table
            Node<T>[] newTable = (Node<T>[]) new Node[2 * table.length];
            for (int i = 0; i < table.length; i++) {
                if (table[i] == null) continue;
                if (table[i].deleted) continue;
                needsResize = addNoCheck(newTable, table[i].key, table[i].val) || needsResize;
            }
            tableReference.compareAndSet(table, newTable);
        } finally {
            // Release all write locks
            for (int i = 0; i < locks.length; i++) {
                release(i);
            }
        }

        // See if the table needs another resizing
        if (needsResize) resize();
    }
}