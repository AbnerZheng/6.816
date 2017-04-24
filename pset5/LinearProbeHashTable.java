import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.ArrayList;
import java.lang.Math.*;

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

    private List<Node<T>> table;
    private final ReentrantLock[] locks;
    private int logSize;
    private int mask;
    private int maxProbes;

    // Invariants:
    // mask = 1 << logSize - 1
    // table.size() = mask + 1

    /**
     * @param logSize the starting capacity of the hash table is 2**(logSize)
     * @param maxProbes the maximum number of probes for a slot before resizing
     */
    @SuppressWarnings("unchecked")
    public LinearProbeHashTable(int logSize, int maxProbes) {
        this.mask = 1 << logSize - 1;
        this.table = new ArrayList<Node<T>>(mask + 1);
        this.locks = new ReentrantLock[mask + 1];
        this.maxProbes = maxProbes;
        for (int i = 0; i <= mask; i++) {
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
        int capacity = table.size();
        int startIndex = key & mask;

        // Probe up to maxProbes entries
        for (int i = 0; i < maxProbes; i++) {
            int currIndex = (startIndex + i) % table.size();
            if (table.get(currIndex) == null) {
                // The entry is empty
                try {
                    // Acquire locks
                    acquire(startIndex, currIndex);

                    // Release the locks and try again if the table changed
                    if (table.size() != capacity) break;

                    // If the entry is no longer empty, continue
                    if (table.get(currIndex) != null) continue;

                    // Otherwise add the value and update k
                    table.set(currIndex, new Node<T>(key, val));
                    table.get(startIndex).k = Math.max(table.get(startIndex).k, i);
                    return;
                } finally {
                    // Release locks
                    release(startIndex, currIndex);
                }
            } else if (table.get(currIndex).key == key) {
                // We're updating the existing entry
                try {
                    // Acquire locks
                    acquire(currIndex);

                    // Release the locks and try again if the table changed
                    if (table.size() != capacity) break;

                    // If the entry is no longer the same key, continue
                    if (table.get(currIndex).key != key) continue;

                    // Otherwise update the key value pair
                    table.get(currIndex).val = val;
                    return;
                } finally {
                    // Release locks
                    release(currIndex);
                }
            }
        }

        // Resize the table and try again out of probes
        if (table.size() == capacity) resize();
        add(key, val);
    }

    /**
     * Removes the key from the hash table.
     * @param key key to be removed
     * @return true iff the key was successfully removed
     */
    public boolean remove(int key) {
        // Get the current table capacity
        int capacity = table.size();
        int startIndex = key & mask;

        // Return false if the first entry does not exist
        Node<T> firstEntry = table.get(startIndex);
        if (firstEntry == null) {
            return false;
        }

        // Probe up to k entries
        for (int i = 0; i <= firstEntry.k; i++) {
            int currIndex = (startIndex + i) % table.size();
            if (table.get(currIndex).key == key) {
                try {
                    acquire(currIndex);

                    // Check that the value is still there when you remove it
                    if (table.get(currIndex) != null && table.get(currIndex).key == key) {
                        table.set(currIndex, null);
                        return true;
                    }
                    break;
                } finally {
                    release(currIndex);
                }
            }
        }

        // The table size has changed or the entry does not exist
        if (table.size() != capacity) return remove(key);
        return false;
    }

    /**
     * Returns whether the key is in the hash table.
     * @param key key to check for
     * @return true iff the key is in the hash table
     */
    public boolean contains(int key) {
        int index = key & mask;
        Node<T> firstEntry = table.get(index);

        // Return false if the first entry does not exist
        if (firstEntry == null) {
            return false;
        }

        // Probe up to k entries
        int k = firstEntry.k;
        for (int i = index; i <= index + firstEntry.k; i++) {
            if (table.get(i % table.size()).key == key)
                return true;
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

    private boolean addNoCheck(List<Node<T>> table, int key, T val) {
        // Probe up to maxProbes entries
        int startIndex = key & mask;
        for (int i = 0; i < table.size(); i++) {
            int currIndex = (startIndex + i) % table.size();
            Node<T> entry = table.get(currIndex);
            if (entry == null) {
                table.set(currIndex, new Node<T>(key, val));
                table.get(startIndex).k = Math.max(table.get(startIndex).k, i);
                return table.get(startIndex).k >= maxProbes;
            }
        }
        return true;
    }

    /**
     * Doubles the size of the hash table and reassigns all key value pairs.
     */
    @SuppressWarnings("unchecked")
    private void resize() {
        int oldCapacity = table.size();

        // Acquire all write locks in sequential order
        for (int i = 0; i < locks.length; i++) {
            locks[i].lock();
        }

        // Check if someone beat us to it
        if (oldCapacity != table.size()) return;

        // Resize the table
        this.mask = 2 * mask + 1;
        int size = 2 * table.size();
        List<Node<T>> newTable = new ArrayList<Node<T>>(size);
        boolean needsResize = false;
        for (int i = 0; i < table.size(); i++) {
            needsResize = addNoCheck(newTable, table.get(i).key, table.get(i).val) || needsResize;
        }
        table = newTable;

        // Release all write locks
        for (int i = 0; i < locks.length; i++) {
            locks[i].unlock();
        }

        // See if the table needs another resizing
        if (needsResize) resize();
    }
}