package pset6;

import java.util.List;
import java.util.ArrayList;
import java.lang.Math.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;

public interface HashTable<T> {
    public void add(int key, T x);
    public boolean remove(int key);
    public boolean contains(int key);
    public T get(int key);
}

/**
 * This hash table cannot be resized! It assumes that collisions do not happen!
 * @param <T>
 */
class ArrayHashTable<T> implements HashTable<T> {

    class Node<T> {
        public final T val;
        public boolean deleted;

        public Node(T val) {
            this.val = val;
            this.deleted = false;
        }
    }

    private enum LockType { READ, WRITE }

    private final ArrayHashTable<T>.Node<T>[] table;
//    private final ReentrantReadWriteLock[] locks;
//    private final int NUM_LOCKS = 1 << 4;
//    private final int LOCK_MASK = NUM_LOCKS - 1;

    /**
     * @param logSize the number of potential keys of the hash table is 2**(logSize)
     */
    @SuppressWarnings("unchecked")
    public ArrayHashTable(int logSize) {
        table = (ArrayHashTable<T>.Node<T>[]) new Node[1 << logSize];
//        locks = new ReentrantReadWriteLock[NUM_LOCKS];
//        for (int i = 0; i < NUM_LOCKS; i++) {
//            locks[i] = new ReentrantReadWriteLock();
//        }
    }

    /**
     * Adds the key value pair to the hash table.
     * @param key key to be added
     * @param val corresponding value
     */
    public void add(int key, T val) {
//        try {
//            acquire(key, LockType.WRITE);
            table[key] = new ArrayHashTable<T>.Node<T>(val);
//        } finally {
//            release(key, LockType.WRITE);
//        }
    }

    /**
     * Removes the key from the hash table.
     * @param key key to be removed
     * @return true iff the key was successfully removed
     */
    public boolean remove(int key) {
//        try {
//            acquire(key, LockType.WRITE);
            ArrayHashTable<T>.Node<T> node = table[key];
            if (node != null && !node.deleted) {
                node.deleted = true;
                return true;
            }
            return false;
//        } finally {
//            release(key, LockType.WRITE);
//        }
    }

    /**
     * Returns whether the key is in the hash table.
     * @param key key to check for
     * @return true iff the key is in the hash table
     */
    public boolean contains(int key) {
//        try {
//            acquire(key, LockType.READ);
            ArrayHashTable<T>.Node<T> node = table[key];
            return node != null && !node.deleted;
//        } finally {
//            release(key, LockType.READ);
//        }
    }

    /**
     * @param key key to check for
     * @return the value associated with the key
     */
    public T get(int key) {
//        try {
//            acquire(key, LockType.READ);
            ArrayHashTable<T>.Node<T> node = table[key];
            if (node != null && !node.deleted) {
                return node.val;
            }
            return null;
//        } finally {
//            release(key, LockType.READ);
//        }
    }

//    private void acquire(int lock, LockType type) {
//        if (type == LockType.READ)
//            locks[lock & LOCK_MASK].readLock().lock();
//        else if (type == LockType.WRITE)
//            locks[lock & LOCK_MASK].writeLock().lock();
//    }
//
//    private void release(int lock, LockType type) {
//        if (type == LockType.READ)
//            locks[lock & LOCK_MASK].readLock().unlock();
//        else if (type == LockType.WRITE)
//            locks[lock & LOCK_MASK].writeLock().unlock();
//    }
}

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
        boolean added = false;
        for (int k = 0; k < maxProbes; k++) {
            try {
                acquire(startIndex, currIndex);

                // Table was resized, try again
                if (!tableReference.compareAndSet(table, table))
                    break;

                Node<T> currNode = table[currIndex];

                if (added) {
                    // Delete other references to the key
                    if (currNode == null) {
                        return;
                    } else if (currNode.key == key) {
                        currNode.deleted = true;
                        return;
                    }
                } else {
                    // Try to add the key
                    if (currNode == null) {
                        // There is no node here
                        table[currIndex] = new Node<T>(key, val);
                        table[startIndex].k = Math.max(table[startIndex].k, k);
                        return;
                    } else if (currNode.deleted) {
                        // The node here was deleted
                        table[currIndex] = new Node<T>(key, val, table[currIndex].k);
                        table[startIndex].k = Math.max(table[startIndex].k, k);
                        added = true;
                    } else if (currNode.key == key) {
                        // The node here has the same key
                        currNode.val = val;
                        return;
                    }
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

    /**
     * @param key key to check for
     * @return the value associated with the key
     */
    public T get(int key) {
        // Get the current table capacity
        Node<T>[] table = tableReference.get();
        int capacity = table.length;
        int startIndex = key & (capacity - 1);

        // Return false if the first entry does not exist
        Node<T> firstEntry = table[startIndex];
        if (firstEntry == null) {
            return null;
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
                    return null;
                } else if (table[currIndex].deleted) {
                    continue;
                } else if (table[currIndex].key == key) {
                    return table[currIndex].val;
                }
            } finally {
                release(currIndex);
            }
        }

        if (!tableReference.compareAndSet(table, table)) {
            return get(key);
        }
        return null;
    }

    private void acquire(int lock) {
        locks[lock & 0xF].lock();
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

    private AtomicReference<SerialList<T,Integer>[]> tableReference;
    private final ReentrantLock[] locks;
    private final int maxBucketSize = 4;

    /**
     * @param logSize the starting capacity of the hash table is 2**(logSize)
     * @param maxBucketSize the max average size of a bucket before resizing
     */
    @SuppressWarnings("unchecked")
    public LockFreeHashTable(int logSize, int maxBucketSize) {
        int capacity = 1 << logSize;
        this.tableReference = new AtomicReference(new SerialList[capacity]);
        this.locks = new ReentrantLock[capacity];
        for (int i = 0; i < capacity; i++) {
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
            SerialList<T, Integer>[] table = tableReference.get();
            int index = key & (table.length - 1);
            if (table[index] == null)
                table[index] = new SerialList<T, Integer>(key, val);
            else
                table[index].add(key, val);
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
            SerialList<T, Integer>[] table = tableReference.get();
            int index = key & (table.length - 1);
            if (table[index] != null)
                return table[index].remove(key);
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
        SerialList<T, Integer>[] table = tableReference.get();
        int index = key & (table.length - 1);
        SerialList<T, Integer> list = table[index];
        return list != null && list.contains(key);
    }

    /**
     * @param key key to check for
     * @return the value associated with the key
     */
    public T get(int key) {
        SerialList<T, Integer>[] table = tableReference.get();
        int index = key & (table.length - 1);
        SerialList<T, Integer> list = table[index];
        if (list != null) {
            SerialList<T, Integer>.Iterator<T, Integer> iterator = list.getItem(key);
            if (iterator != null) {
                return iterator.getItem();
            }
        }
        return null;
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
    private void addNoCheck(SerialList<T, Integer>[] table, int key, T x) {
        int index = key & (table.length - 1);
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
        SerialList<T, Integer>[] table = tableReference.get();
        int index = key & (table.length - 1);
        while (table[index] != null && table[index].getSize() > maxBucketSize) {
            resize();
            table = tableReference.get();
            index = key & (table.length - 1);
        }
    }

    /**
     * Doubles the size of the hash table and reassigns all key value pairs.
     */
    @SuppressWarnings("unchecked")
    private void resize() {
        SerialList<T, Integer>[] table = tableReference.get();
        try {
            // Acquire all write locks in sequential order
            for (int i = 0; i < locks.length; i++) {
                acquire(i);
            }

            // Check if someone beat us to it
            if (!tableReference.compareAndSet(table, table))
                return;

            // Resize the table
            SerialList<T, Integer>[] newTable = new SerialList[2 * table.length];
            for (int i = 0; i < table.length; i++) {
                if (table[i] == null)
                    continue;
                SerialList<T, Integer>.Iterator<T, Integer> iterator = table[i].getHead();
                while (iterator != null) {
                    int newIndex = iterator.key & (2 * table.length - 1);
                    if (newTable[newIndex] == null)
                        newTable[newIndex] = new SerialList<T, Integer>(iterator.key, iterator.getItem());
                    else
                        addNoCheck(newTable, iterator.key, iterator.getItem());
                    iterator = iterator.getNext();
                }
            }
            tableReference.compareAndSet(table, newTable);
        } finally {
            // Release all write locks
            for (int i = 0; i < locks.length; i++) {
                release(i);
            }
        }
    }
}

/**
 * Lock-based Closed-Address Hash Table
 *
 * This is a standard hash table where add() and remove() methods use a writeLock() to make modifications to the list
 * located at a bucket and contains() uses the corresponding readLock (see Section 8.3 in the text). A resize() method
 * merely grabs all writeLocks in sequential order (to avoid deadlock with a rival resize() attempt) to halt activity
 * during the course of the resize() operation.
 *
 * The table resizes when the size of any bucket exceeds a max bucket size threshold.
 */
class LockingHashTable<T> implements HashTable<T> {

    private AtomicReference<SerialList<T,Integer>[]> tableReference;
    private final ReentrantReadWriteLock[] locks;
    private final int maxBucketSize;

    private enum LockType { READ, WRITE }

    /**
     * @param logSize the starting capacity of the hash table is 2**(logSize)
     * @param maxBucketSize the max average size of a bucket before resizing
     */
    @SuppressWarnings("unchecked")
    public LockingHashTable(int logSize, int maxBucketSize) {
        int capacity = 1 << logSize;
        this.maxBucketSize = maxBucketSize;
        this.tableReference = new AtomicReference(new SerialList[capacity]);
        this.locks = new ReentrantReadWriteLock[capacity];
        for (int i = 0; i < capacity; i++) {
            this.locks[i] = new ReentrantReadWriteLock();
        }
    }

    /**
     * Adds the key value pair to the hash table.
     * Overwrites the existing value if key already exists.
     * @param key key to be added
     * @param val corresponding value
     */
    public void add(int key, T val) {
        try {
            acquire(key, LockType.WRITE);
            SerialList<T, Integer>[] table = tableReference.get();
            int index = key & (table.length - 1);
            if (table[index] == null)
                table[index] = new SerialList<T, Integer>(key, val);
            else
                table[index].add(key, val);
        } finally {
            release(key, LockType.WRITE);
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
            acquire(key, LockType.WRITE);
            SerialList<T, Integer>[] table = tableReference.get();
            int index = key & (table.length - 1);
            if (table[index] != null)
                return table[index].remove(key);
            else
                return false;
        } finally {
            release(key, LockType.WRITE);
        }
    }

    /**
     * Returns whether the key is in the hash table.
     * @param key key to check for
     * @return true iff the key is in the hash table
     */
    public boolean contains(int key) {
        try {
            acquire(key, LockType.READ);
            SerialList<T, Integer>[] table = tableReference.get();
            int index = key & (table.length - 1);
            return table[index] != null && table[index].contains(key);
        } finally {
            release(key, LockType.READ);
        }
    }

    /**
     * @param key key to check for
     * @return the value associated with the key
     */
    public T get(int key) {
        try {
            acquire(key, LockType.READ);
            SerialList<T, Integer>[] table = tableReference.get();
            int index = key & (table.length - 1);

            SerialList<T, Integer> list = table[index];
            if (list != null) {
                SerialList<T, Integer>.Iterator<T, Integer> iterator = list.getItem(key);
                if (iterator != null) {
                    return iterator.getItem();
                }
            }
            return null;
        } finally {
            release(key, LockType.READ);
        }
    }


    /**
     * Acquires a lock
     * @param key the key the lock should correspond to
     * @param type the type of lock to acquire (read or write)
     */
    private void acquire(int key, LockType type) {
        if (type == LockType.READ)
            locks[key % locks.length].readLock().lock();
        else if (type == LockType.WRITE)
            locks[key % locks.length].writeLock().lock();
    }

    /**
     * Releases a lock
     * @param key the key the lock should correspond to
     * @param type the type of lock to release (read or write)
     */
    private void release(int key, LockType type) {
        if (type == LockType.READ)
            locks[key % locks.length].readLock().unlock();
        else if (type == LockType.WRITE)
            locks[key % locks.length].writeLock().unlock();
    }

    /**
     * Adds the key value pair to the hash table without checking for max bucket size or acquiring locks.
     * @param key
     * @param x
     */
    private void addNoCheck(SerialList<T, Integer>[] table, int key, T x) {
        int index = key & (table.length - 1);
        if (table[index] == null)
            table[index] = new SerialList<T, Integer>(key, x);
        else
            table[index].addNoCheck(key, x);
    }

    /**
     * Resizes the hash table if the bucket corresponding to the given key exceeds the max bucket size.
     * @param key key to check the bucket for
     */
    private void resizeIfNecessary(int key) {
        SerialList<T, Integer>[] table = tableReference.get();
        int index = key & (table.length - 1);
        while (table[index] != null && table[index].getSize() > maxBucketSize) {
            resize();
            table = tableReference.get();
            index = key & (table.length - 1);
        }
    }

    /**
     * Doubles the size of the hash table and reassigns all key value pairs.
     */
    @SuppressWarnings("unchecked")
    private void resize() {
        SerialList<T, Integer>[] table = tableReference.get();
        try {
            // Acquire all write locks in sequential order
            for (int i = 0; i < locks.length; i++) {
                acquire(i, LockType.WRITE);
            }

            // Check if someone beat us to it
            if (!tableReference.compareAndSet(table, table))
                return;

            // Resize the table
            SerialList<T, Integer>[] newTable = new SerialList[2 * table.length];
            for (int i = 0; i < table.length; i++) {
                if (table[i] == null)
                    continue;
                SerialList<T, Integer>.Iterator<T, Integer> iterator = table[i].getHead();
                while (iterator != null) {
                    int newIndex = iterator.key & (2 * table.length - 1);
                    if (newTable[newIndex] == null)
                        newTable[newIndex] = new SerialList<T, Integer>(iterator.key, iterator.getItem());
                    else
                        addNoCheck(newTable, iterator.key, iterator.getItem());
                    iterator = iterator.getNext();
                }
            }
            tableReference.compareAndSet(table, newTable);
        } finally {
            // Release all write locks
            for (int i = 0; i < locks.length; i++) {
                release(i, LockType.WRITE);
            }
        }
    }
}
