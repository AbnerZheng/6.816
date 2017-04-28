import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicReference;

/**
 * App-Specific Hash Table
 */
class AppSpecificHashTable<T> implements HashTable<T> {

    private AtomicReference<SerialList<T,Integer>[]> tableReference;
    private final ReentrantReadWriteLock[] locks;
    private final int maxBucketSize;

    private enum LockType { READ, WRITE }

    /**
     * @param logSize the starting capacity of the hash table is 2**(logSize)
     * @param maxBucketSize the max average size of a bucket before resizing
     */
    @SuppressWarnings("unchecked")
    public AppSpecificHashTable(int logSize, int maxBucketSize) {
        int capacity = 1 << logSize;
        this.maxBucketSize = 64;
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
