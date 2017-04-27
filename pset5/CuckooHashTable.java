import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

class CuckooHashTable<T> implements HashTable<T> {

    private volatile List<Node<T>>[][] table;
    private ReentrantLock[][] locks;

    private final int PROBE_SIZE = 8;
    private final int THRESHOLD = 4;
    private final int RANDOM = (int)(Math.random() * Integer.MAX_VALUE);
    private final int MAX_RELOCS;

    @SuppressWarnings("unchecked")
    public CuckooHashTable(int logSize, int maxRelocs) {
        int capacity = 1 << logSize;
        MAX_RELOCS = maxRelocs;
        table = (List<Node<T>>[][]) new ArrayList[2][capacity];
        locks = new ReentrantLock[2][capacity];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < capacity; j++) {
                table[i][j] = new ArrayList<Node<T>>(PROBE_SIZE);
                locks[i][j] = new ReentrantLock();
            }
        }
    }

    public void add(int key, T x) {
        int h0, h1;
        int tableToRelocate = -1, setToRelocate = -1;
        boolean mustResize = false;
        try {
            acquire(key);

            int mask = table[0].length - 1;
            h0 = hash0(key) & mask;
            h1 = hash1(key) & mask;
            List<Node<T>> set0 = table[0][h0];
            List<Node<T>> set1 = table[1][h1];

            remove(key);
            if (set0.size() < THRESHOLD) {
                set0.add(new Node<T>(key, x)); return;
            } else if (set1.size() < THRESHOLD) {
                set1.add(new Node<T>(key, x)); return;
            } else if (set0.size() < PROBE_SIZE) {
                set0.add(new Node<T>(key, x));
                tableToRelocate = 0;
                setToRelocate = h0;
            } else if (set1.size() < PROBE_SIZE) {
                set1.add(new Node<T>(key, x));
                tableToRelocate = 1;
                setToRelocate = h1;
            } else {
                mustResize = true;
            }
        } finally {
            release(key);
        }
        if (mustResize) {
            resize(); add(key, x);
        } else if (!relocate(tableToRelocate, setToRelocate)) {
            resize();
        }
    }

    public boolean remove(int key) {
        try {
            acquire(key);
            int mask = table[0].length - 1;
            List<Node<T>> set0 = table[0][hash0(key) & mask];
            List<Node<T>> set1 = table[1][hash1(key) & mask];
            return set0.remove((Integer)key) || set1.remove((Integer)key);
        } finally {
            release(key);
        }
    }

    public boolean contains(int key) {
        try {
            acquire(key);
            int mask = table[0].length - 1;
            List<Node<T>> set0 = table[0][hash0(key) & mask];
            List<Node<T>> set1 = table[1][hash1(key) & mask];
            return set0.contains(key) || set1.contains(key);
        } finally {
            release(key);
        }
    }

    private int hash0(int key) {
        return key;
    }

    private int hash1(int key) {
        return key ^ RANDOM;
    }

    /**
     * Acquire the locks for a given key.
     * @param key
     */
    private void acquire(int key) {
        locks[0][hash0(key) % locks[0].length].lock();
        locks[1][hash1(key) % locks[1].length].lock();
    }

    /**
     * Release the locks for a given key.
     * @param key
     */
    private void release(int key) {
        locks[0][hash0(key) % locks[0].length].unlock();
        locks[1][hash1(key) % locks[1].length].unlock();
    }

    /**
     * @param i table to relocate
     * @param hi set to relocate
     * @return true iff relocation was successful
     */
    private boolean relocate(int i, int hi) {
        int hj = 0;
        int j = 1 - i;
        for (int round = 0; round < MAX_RELOCS; round++) {
            // Find the oldest element in the given set
            int mask = table[0].length - 1;
            List<Node<T>> iSet = table[i][hi];
            if (iSet.size() == 0) return true;
            Node<T> y = iSet.get(0);
            switch (i) {
            case 0: hj = hash1(y.key) & mask; break;
            case 1: hj = hash0(y.key) & mask; break;
            }

            // Try to relocate it to the other set
            try {
                acquire(y.key);
                List<Node<T>> jSet = table[j][hj];

                // Check to see if the table has already been resized
                if (table[0].length - 1 != mask) return true;

                if (iSet.remove(y)) {
                    if (jSet.size() < THRESHOLD) {
                        jSet.add(y);
                        return true;
                    } else if (jSet.size() < PROBE_SIZE) {
                        // Must perform another relocation
                        jSet.add(y);
                        i = 1 - i;
                        hi = hj;
                        j = 1 - j;
                    } else {
                        // Could not relocate, need to resize
                        iSet.add(y);
                        return false;
                    }
                } else if (iSet.size() >= THRESHOLD) {
                    continue;
                } else {
                    return true;
                }
            } finally {
                release(y.key);
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        try {
            int oldCapacity = table[0].length;

            // Obtain all locks
            for (ReentrantLock aLock : locks[0]) {
                aLock.lock();
            }

            // Someone beat us to resizing
            if (table[0].length != oldCapacity) {
                return;
            }

            // Initialize the new table
            int newCapacity = 2 * oldCapacity;
            List<Node<T>>[][] oldTable = table;
            table = (List<Node<T>>[][]) new List[2][newCapacity];
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < newCapacity; j++) {
                    table[i][j] = new ArrayList<Node<T>>(PROBE_SIZE);
                }
            }

            // Add all the old elements
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < oldCapacity; j++) {
                    for (Node<T> node : oldTable[i][j]) {
                        add(node.key, node.val);
                    }
                }
            }
        } finally {
            // Release all locks
            for (ReentrantLock aLock : locks[0]) {
                aLock.unlock();
            }
        }
    }
}

//class CuckooHashTableTest {
//    public static void main(String[] args) {
//        CuckooHashTable<Integer> table = new CuckooHashTable<Integer>(2, 8);
////        for( int i = 0; i < 256; i++ ) {
////            table.add(i,i*i);
////        }
//        table.add(234234, 234234234);
//        System.out.println(table.contains(234234));
//    }
//}