import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.ArrayList;
import java.lang.Math.*;
import java.util.concurrent.atomic.*;

/**
 * Awesome Hash Table
 *
 * The awesome hash table is a lock-free hash table.
 */

class AwesomeHashTable<T> implements HashTable<T> {

    LockFreeList<T>[] bucket;
    AtomicInteger bucketSize;
    AtomicInteger setSize;

    // When the average bucket load crosses this threshold, we double the table capacity
    private final float THRESHOLD = 4;
    private final int PROBABILITY = 30;

    @SuppressWarnings("unchecked")
    public AwesomeHashTable(int logSize, int maxProbes) {
        bucket = (LockFreeList<T>[]) new LockFreeList[1 << (logSize + 20)];
        bucket[0] = new LockFreeList<T>();
        bucketSize = new AtomicInteger(1 << logSize);
        setSize = new AtomicInteger(0);
    }

    public void add(int key, T val) {
        int myBucket = key % bucketSize.get();
        LockFreeList<T> b = getLockFreeList(myBucket);
        if (!b.add(key, val)) return;  // The key is already there
        int setSizeNow = setSize.getAndIncrement();
//        if ((int)(Math.random() * PROBABILITY) == 0) {
//            int setSizeNow = setSize.getAndAdd(PROBABILITY);
            int bucketSizeNow = bucketSize.get();
            if (setSizeNow / bucketSizeNow > THRESHOLD)
                bucketSize.compareAndSet(bucketSizeNow, 2 * bucketSizeNow);
//        }
    }

    public boolean remove(int key) {
        int myBucket = key % bucketSize.get();
        LockFreeList<T> b = getLockFreeList(myBucket);
        setSize.getAndDecrement();
        return b.remove(key);
    }

    public boolean contains(int key) {
        int myBucket = key % bucketSize.get();
        LockFreeList<T> b = getLockFreeList(myBucket);
        return b.contains(key);
    }

    private LockFreeList<T> getLockFreeList(int myBucket) {
        if (bucket[myBucket] == null)
            initializeBucket(myBucket);
        return bucket[myBucket];
    }

    private void initializeBucket(int myBucket) {
        int parent = getParent(myBucket);
        if (bucket[parent] == null)
            initializeBucket(parent);
        bucket[myBucket] = bucket[parent].getSentinel(myBucket);
    }

    private int getParent(int myBucket) {
        int parent = bucketSize.get();
        do {
            parent = parent >> 1;
        } while (parent > myBucket);
        parent = myBucket - parent;
        return parent;
    }
}

class AwesomeHashTableTest {
    public static void main(String[] args) {
        AwesomeHashTable<Integer> table = new AwesomeHashTable<Integer>(2, 4);
        int trues, falses;
        for (int i = 0; i < 512; i++) {
            table.add(i, i);
        }

        for (int i = 0; i < 512; i+=2) {
            table.remove(i);
        }
        trues = 0; falses = 0;
        for (int i = 0; i < 512; i+=3) {
            if (table.contains(i))
                trues++;
            else
                falses++;
        }
        System.out.println(trues);
        System.out.println(falses);
    }
}
