import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.ArrayList;
import java.lang.Math.*;
import java.util.concurrent.atomic.*;

/**
 * Awesome Hash Table
 *
 * The awesome hash table is an extension of the linear probe hash table.
 */

class AwesomeHashTable<T> implements HashTable<T> {

    private AtomicReference<AtomicNode<T>[]> tableReference;
    private final ReentrantLock[] locks;
    private final int maxProbes;

    /**
     * @param logSize the starting capacity of the hash table is 2**(logSize)
     * @param maxProbes the maximum number of probes for a slot before resizing
     */
    @SuppressWarnings("unchecked")
    public AwesomeHashTable(int logSize, int maxProbes) {
        int size = 1 << logSize;
        this.tableReference = new AtomicReference((AtomicNode<T>[]) new AtomicNode[size]);
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
        AtomicNode<T>[] table = tableReference.get();
        int capacity = table.length;
        int currIndex = key & (capacity - 1);

        // Probe up to maxProbes entries
        for (int k = 0; k < maxProbes; k++) {
            try {
                acquire(currIndex);
                // Table was resized, try again
                if (!tableReference.compareAndSet(table, table))
                    break;

                AtomicNode<T> currNode = table[currIndex];
                if (currNode == null || currNode.deleted) {
                    // There is no node here or the node was deleted
                    table[currIndex] = new AtomicNode<T>(key, val);
                    return;
                } else if (currNode.key == key) {
                    // The node here has the same key
                    currNode.val = val;
                    return;
                }
            } finally {
                release(currIndex);
            }
            currIndex = (currIndex + 1) % capacity;
        }

        // Resize the table if out of probes and try again
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
        AtomicNode<T>[] table = tableReference.get();
        int capacity = table.length;
        int currIndex = key & (capacity - 1);

        // Probe up to k entries
        for (int i = 0; i < maxProbes; i++) {
            try {
                acquire(currIndex);

                // Table was resized, try again
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
            currIndex = (currIndex + 1) % capacity;
        }

        // Try again if the table was resized
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
        AtomicNode<T>[] table = tableReference.get();
        int capacity = table.length;
        int currIndex = key & (capacity - 1);

        // Probe up to k entries
        for (int i = 0; i < maxProbes; i++) {
            try {
                acquire(currIndex);

                // Table was resized, try again
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
            currIndex = (currIndex + 1) % capacity;
        }

        // Try again if the table was resized
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

    private boolean addNoCheck(AtomicNode<T>[] table, int key, T val) {
        int capacity = table.length;
        int startIndex = key & (capacity - 1);
        for (int i = 0; i < capacity; i++) {
            int currIndex = (startIndex + i) % capacity;

            // When adding without check, can only add to null spaces
            if (table[currIndex] == null) {
                table[currIndex] = new AtomicNode<T>(key, val);
                return i >= maxProbes;
            }
        }
        return true;
    }

    /**
     * Doubles the size of the hash table and reassigns all key value pairs.
     */
    @SuppressWarnings("unchecked")
    private void resize() {
        AtomicNode<T>[] table = tableReference.get();
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
            int newCapacity = 2 * table.length;
            AtomicNode<T>[] newTable = (AtomicNode<T>[]) new AtomicNode[newCapacity];
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