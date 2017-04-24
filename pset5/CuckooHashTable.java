import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

class CuckooHashTable<T> implements HashTable<T> {

    private volatile int capacity;
    private volatile List<Node<T>>[][] table;

    private ReentrantLock[][] locks;

    // Invariants
    // capacity = table[0].length

    private final int PROBE_SIZE = 4;
    private final int THRESHOLD = 2;
    private final int RANDOM = (int)(Math.random() * Integer.MAX_VALUE);
    private final int MAX_RELOCS;

    public CuckooHashTable(int size, int maxRelocs) {
        capacity = size;
        table = (List<Node<T>>[][]) new ArrayList[2][capacity];
        locks = new ReentrantLock[2][capacity];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < capacity; j++) {
                table[i][j] = new ArrayList<Node<T>>(PROBE_SIZE);
                locks[i][j] = new ReentrantLock();
            }
        }
        MAX_RELOCS = maxRelocs;
    }

    public void add(int key, T x) {
        T y = null;
        int h0 = hash0(key), h1 = hash1(key);
        int i = -1, h = -1;
        boolean mustResize = false;
        try {
            acquire(key);
            if (contains(key)) return;
            List<Node<T>> set0 = table[0][h0];
            List<Node<T>> set1 = table[1][h1];
            if (set0.size() < THRESHOLD) {
                set0.add(new Node<T>(key, x)); return;
            } else if (set1.size() < THRESHOLD) {
                set1.add(new Node<T>(key, x)); return;
            } else if (set0.size() < PROBE_SIZE) {
                set0.add(new Node<T>(key, x)); i = 0; h = h0;
            } else if (set1.size() < PROBE_SIZE) {
                set1.add(new Node<T>(key, x)); i = 1; h = h1;
            } else {
                mustResize = true;
            }
        } finally {
            release(key);
        }
        if (mustResize) {
            resize(); add(key, x);
        } else if (!relocate(i, h)) {
            resize();
        }
    }

    public boolean remove(int key) {
        try {
            acquire(key);
            List<Node<T>> set0 = table[0][hash0(key) % capacity];
            if (set0.contains(key)) {
                set0.remove(key);
                return true;
            }
            List<Node<T>> set1 = table[1][hash1(key) % capacity];
            if (set1.contains(key)) {
                set1.remove(key);
                return true;
            }
            return false;
        } finally {
            release(key);
        }
    }

    public boolean contains(int key) {
        return table[0][hash0(key)].contains(key) || table[1][hash1(key)].contains(key);
    }

    private int hash0(int key) {
        return key & (capacity - 1);
    }

    private int hash1(int key) {
        return (key ^ RANDOM) & (capacity - 1);
    }

    private void acquire(int key) {
        locks[0][hash0(key) % locks[0].length].lock();
        locks[1][hash1(key) % locks[1].length].lock();
    }

    private void release(int key) {
        locks[0][hash0(key) % locks[0].length].unlock();
        locks[1][hash1(key) % locks[1].length].unlock();
    }

    private boolean relocate(int i, int hi) {
        int hj = -1;
        int j = 1 - i;
        for (int round = 0; round < MAX_RELOCS; round++) {
            List<Node<T>> iSet = table[i][hi];  // set to shrink
            Node<T> y = iSet.get(0);  // switch out the oldest element
            switch (i) {
                case 0: hj = hash1(y.key) % capacity; break;
                case 1: hj = hash0(y.key) % capacity; break;
            }
            acquire(y.key);
            List<Node<T>> jSet = table[j][hj];  // set to grow
            try {
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
                        return relocate(i, hi);
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

    private void resize() {
        int oldCapacity = capacity;
        for (ReentrantLock aLock : locks[0]) {
            aLock.lock();
        }
        try {
            if (capacity != oldCapacity) {
                return;
            }
            List<Node<T>>[][] oldTable = table;
            capacity = 2 * capacity;
            table = (List<Node<T>>[][]) new List[2][capacity];
            for (List<Node<T>>[] row : table) {
                for (int i = 0; i < row.length; i++) {
                    row[i] = new ArrayList<Node<T>>(PROBE_SIZE);
                }
            }
            for (List<Node<T>>[] row : oldTable) {
                for (List<Node<T>> set : row) {
                    for (Node<T> z : set) {
                        add(z.key, z.val);
                    }
                }
            }
        } finally {
            for (ReentrantLock aLock : locks[0]) {
                aLock.unlock();
            }
        }
    }
}