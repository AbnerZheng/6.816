import java.util.concurrent.locks.ReentrantReadWriteLock;
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

    private volatile AtomicNode<T>[] table;
    private final ReentrantReadWriteLock[] locks;
    private final int maxProbes;
    private final int c1 = (int)(Math.random() * Integer.MAX_VALUE);
    private final int c2 = (int)(Math.random() * Integer.MAX_VALUE);

    /**
     * @param logSize the starting capacity of the hash table is 2**(logSize)
     * @param maxProbes the maximum number of probes for a slot before resizing
     */
    @SuppressWarnings("unchecked")
    public AwesomeHashTable(int logSize, int maxProbes) {
        int size = 1 << logSize;
        this.table = (AtomicNode<T>[]) new AtomicNode[size];
        this.locks = new ReentrantReadWriteLock[size];
        this.maxProbes = maxProbes;
        for (int i = 0; i < size; i++) {
            this.locks[i] = new ReentrantReadWriteLock();
        }
    }

    /**
     * Adds the key value pair to the hash table.
     * @param key key to be added
     * @param val corresponding value
     */
    public void add(int key, T val) {
        // Get the current table capacity
        AtomicNode<T>[] tempTable = table;
        int capacity = tempTable.length;
        int currIndex;

        // Probe maxProbes entries
        boolean added = false;
        for (int k = 0; k < maxProbes; k++) {
            currIndex = hash(key, k) % capacity;
            try {
                acquire(currIndex);
                // Table was resized, try again
                if (table != tempTable)
                    break;

                AtomicNode<T> currNode = table[currIndex];

                if (added) {
                    // Delete other references to the key
                    if (currNode == null) {
                        return;
                    } else if (currNode.key == key) {
                        currNode.delete();
                        return;
                    }
                } else {
                    // Try to add the key
                    if (currNode == null) {
                        table[currIndex] = new AtomicNode<T>(key, val);
                        return;
                    } else if (currNode.isDeleted()) {
                        table[currIndex] = new AtomicNode<T>(key, val);
                        added = true;
                    } else if (currNode.key == key) {
                        table[currIndex] = new AtomicNode<T>(key, val);
                        return;
                    }
                }
            } finally {
                release(currIndex);
            }
        }

        // Resize the table if out of probes and try again
        if (table == tempTable) resize();
        add(key, val);
    }

    /**
     * Removes the key from the hash table.
     * @param key key to be removed
     * @return true iff the key was successfully removed
     */
    public boolean remove(int key) {
        // Get the current table capacity
        AtomicNode<T>[] tempTable = table;
        int capacity = tempTable.length;
        int currIndex;

        // Probe up to k entries
        for (int i = 0; i < maxProbes; i++) {
            currIndex = hash(key, i) % capacity;
            try {
                acquire(currIndex);

                // Table was resized, try again
                if (table != tempTable)
                    break;

                // Hopefully delete the key
                if (table[currIndex] == null) {
                    return false;
                } else if (table[currIndex].isDeleted()) {
                    continue;
                } else if (table[currIndex].key == key) {
                    table[currIndex].delete();
                    return true;
                }
            } finally {
                release(currIndex);
            }
        }

        // Try again if the table was resized
        if (table != tempTable) {
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
        AtomicNode<T>[] tempTable = table;
        int capacity = tempTable.length;
        int currIndex;

        // Probe up to k entries
        for (int i = 0; i < maxProbes; i++) {
            currIndex = hash(key, i) % capacity;
            try {
                acquireRead(currIndex);

                // Table was resized, try again
                if (table != tempTable)
                    break;

                // Maybe find the key
                if (table[currIndex] == null) {
                    return false;
                } else if (table[currIndex].isDeleted()) {
                    continue;
                } else if (table[currIndex].key == key) {
                    return true;
                }
            } finally {
                releaseRead(currIndex);
            }
        }

        // Try again if the table was resized
        if (table != tempTable) {
            return contains(key);
        }
        return false;
    }

    private void acquire(int lock) {
        locks[lock % locks.length].writeLock().lock();
    }

    private void acquireRead(int lock) {
        locks[lock % locks.length].readLock().lock();
    }

    private void release(int lock) {
        locks[lock % locks.length].writeLock().unlock();
    }

    private void releaseRead(int lock) {
        locks[lock % locks.length].readLock().unlock();
    }

    private boolean addNoCheck(AtomicNode<T>[] table, int key, T val) {
        int capacity = table.length;
        for (int i = 0; i < capacity; i++) {
            int currIndex = hash(key, i) % capacity;

            // When adding without check, can only add to null spaces
            if (table[currIndex] == null) {
                table[currIndex] = new AtomicNode<T>(key, val);
                return i >= maxProbes;
            }
        }
        return true;
    }

    private int hash(int key, int i) {
        return Math.abs(key + c1 * i + c2 * i * i);
    }

    /**
     * Doubles the size of the hash table and reassigns all key value pairs.
     */
    @SuppressWarnings("unchecked")
    private void resize() {
        AtomicNode<T>[] tempTable = table;
        boolean needsResize = false;

        try {
            // Acquire all write locks in sequential order
            for (int i = 0; i < locks.length; i++) {
                acquire(i);
            }

            // Check if someone beat us to it
            if (table != tempTable)
                return;

            // Resize the table
            int newCapacity = 2 * table.length;
            AtomicNode<T>[] newTable = (AtomicNode<T>[]) new AtomicNode[newCapacity];
            for (int i = 0; i < table.length; i++) {
                if (table[i] == null) continue;
                if (table[i].isDeleted()) continue;
                needsResize = addNoCheck(newTable, table[i].key, table[i].val) || needsResize;
            }
            table = newTable;
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