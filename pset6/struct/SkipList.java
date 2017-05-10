package pset6;

import java.util.Random;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.*;

interface SkipList<T> {
    static final Random rand = new Random();
    static final int MAX_LEVEL = 16;
    static int randomLevel() {
        return rand.nextInt(MAX_LEVEL);
    }

//    class SkipListNode<T> {};
    boolean add(T x);
    boolean remove(T x);
    boolean contains(T x);
//    SkipListNode<T> findPred(int key);
//    SkipListNode<T> findCurr(int key);
}

@SuppressWarnings("unchecked")
class LazySkipList<T> implements SkipList<T> {
    public final SkipListNode<T> head = new SkipListNode<T>(Integer.MIN_VALUE);
    public final SkipListNode<T> tail = new SkipListNode<T>(Integer.MAX_VALUE);

    public class SkipListNode<T> {
        final ReentrantLock lock = new ReentrantLock();
        final T value;
        final int key;
        final SkipListNode<T>[] next;
        volatile boolean marked = false;
        volatile boolean fullyLinked = false;
        private int topLevel;

        /**
         * Sentinel node constructor
         * @param key
         */
        public SkipListNode(int key) {
            this.value = null;
            this.key = key;
            this.next = new SkipListNode[MAX_LEVEL + 1];
            this.topLevel = MAX_LEVEL;
        }

        public SkipListNode(T x, int height) {
            this.value = x;
            this.key = x.hashCode();
            this.next = new SkipListNode[height + 1];
            this.topLevel = height;
        }

        public void lock() {
            lock.lock();
        }

        public void unlock() {
            lock.unlock();
        }
    }

    public LazySkipList() {
        for (int i = 0; i < head.next.length; i++) {
            head.next[i] = tail;
        }
    }

    private int find(T x, SkipListNode<T>[] preds, SkipListNode<T>[] succs) {
        int key = x.hashCode();
        int lFound = -1;
        SkipListNode<T> pred = head;
        for (int level = MAX_LEVEL; level >= 0; level--) {
            SkipListNode<T> curr = pred.next[level];
            while (key > curr.key) {
                pred = curr;
                curr = pred.next[level];
            }
            if (lFound == -1 && key == curr.key) {
                lFound = level;
            }
            preds[level] = pred;
            succs[level] = curr;
        }
        return lFound;
    }

    // key is included in the range [pred, curr)
    public SkipListNode<T> findPred(int key) {
        int lFound = -1;
        SkipListNode<T> pred = head, curr = null;
        for (int level = MAX_LEVEL; level >= 0; level--) {
            curr = pred.next[level];
            while (key >= curr.key) {
                pred = curr;
                curr = pred.next[level];
            }
        }
        return pred;
    }

    // key is included in the range [pred, curr)
    public SkipListNode<T> findCurr(int key) {
        int lFound = -1;
        SkipListNode<T> pred = head, curr = null;
        for (int level = MAX_LEVEL; level >= 0; level--) {
            curr = pred.next[level];
            while (key >= curr.key) {
                pred = curr;
                curr = pred.next[level];
            }
        }
        return curr;
    }

    public boolean add(T x) {
        int topLevel = SkipList.randomLevel();
        SkipListNode<T> preds[] = (SkipListNode<T>[]) new SkipListNode[MAX_LEVEL + 1];
        SkipListNode<T> succs[] = (SkipListNode<T>[]) new SkipListNode[MAX_LEVEL + 1];
        while (true) {
            int lFound = find(x, preds, succs);
            if (lFound != -1) {
                SkipListNode<T> nodeFound = succs[lFound];
                if (!nodeFound.marked) {
                    while (!nodeFound.fullyLinked) {}
                    return false;
                }
                continue;
            }
            int highestLocked = -1;
            try {
                SkipListNode<T> pred, succ;
                boolean valid = true;
                for (int level = 0; valid && level <= topLevel; level++) {
                    pred = preds[level];
                    succ = succs[level];
                    pred.lock.lock();
                    highestLocked = level;
                    valid = !pred.marked && !succ.marked && pred.next[level] == succ;
                }
                if (!valid) continue;
                SkipListNode<T> newNode = new SkipListNode(x, topLevel);
                for (int level = 0; level <= topLevel; level++)
                    newNode.next[level] = succs[level];
                for (int level = 0; level <= topLevel; level++)
                    preds[level].next[level] = newNode;
                newNode.fullyLinked = true;  // successful add linearization point
                return true;
            } finally {
                for (int level = 0; level <= highestLocked; level++)
                    preds[level].unlock();
            }
        }
    }

    public boolean remove(T x) {
        SkipListNode<T> victim = null;
        boolean isMarked = false;
        int topLevel = -1;
        SkipListNode<T> preds[] = (SkipListNode<T>[]) new SkipListNode[MAX_LEVEL + 1];
        SkipListNode<T> succs[] = (SkipListNode<T>[]) new SkipListNode[MAX_LEVEL + 1];
        while (true) {
            int lFound = find(x, preds, succs);
            if (lFound != -1) {
                victim = succs[lFound];
            }
            if (isMarked || (lFound != -1 && (victim.fullyLinked && victim.topLevel == lFound && !victim.marked))) {
                if (!isMarked) {
                    topLevel = victim.topLevel;
                    victim.lock.lock();
                    if (victim.marked) {
                        victim.lock.unlock();
                        return false;
                    }
                    victim.marked = true;
                    isMarked = true;
                }
                int highestLocked = -1;
                try {
                    SkipListNode<T> pred, succ;
                    boolean valid = true;
                    for (int level = 0; valid && level <= topLevel; level++) {
                        pred = preds[level];
                        pred.lock.lock();
                        highestLocked = level;
                        valid = !pred.marked && pred.next[level] == victim;
                    }
                    if (!valid) continue;
                    for (int level = topLevel; level >= 0; level--)
                        preds[level].next[level] = victim.next[level];
                    victim.lock.unlock();
                    return true;
                } finally {
                    for (int level = 0; level <= highestLocked; level++)
                        preds[level].unlock();
                }
            } else return false;
        }
    }

    public boolean contains(T x) {
        SkipListNode<T> preds[] = (SkipListNode<T>[]) new SkipListNode[MAX_LEVEL + 1];
        SkipListNode<T> succs[] = (SkipListNode<T>[]) new SkipListNode[MAX_LEVEL + 1];
        int lFound = find(x, preds, succs);
        return (lFound != -1 && succs[lFound].fullyLinked && !succs[lFound].marked);
    }
}

@SuppressWarnings("unchecked")
class LockFreeSkipList<T> implements SkipList<T> {
    public final SkipListNode<T> head = new SkipListNode<T>(Integer.MIN_VALUE);
    public final SkipListNode<T> tail = new SkipListNode<T>(Integer.MAX_VALUE);

    public class SkipListNode<T> {
        public final int key;
        public final T value;
        public final AtomicMarkableReference<SkipListNode<T>>[] next;
        private int topLevel;

        // Constructor for sentinel nodes
        public SkipListNode(int key) {
            this.key = key;
            this.value = null;
            this.next = (AtomicMarkableReference<SkipListNode<T>>[]) new AtomicMarkableReference[MAX_LEVEL + 1];
            for (int i = 0; i < next.length; i++) {
                next[i] = new AtomicMarkableReference<SkipListNode<T>>(null, false);
            }
            this.topLevel = MAX_LEVEL;
        }

        // Constructor for ordinary nodes
        public SkipListNode(T x, int height) {
            this.key = x.hashCode();
            this.value = x;
            this.next = (AtomicMarkableReference<SkipListNode<T>>[]) new AtomicMarkableReference[height + 1];
            for (int i = 0; i < next.length; i++) {
                next[i] = new AtomicMarkableReference<SkipListNode<T>>(null, false);
            }
            this.topLevel = height;
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    public class SkipListWindow<T> {
        public SkipListNode<T> pred, curr;
        public SkipListWindow(SkipListNode<T> pred, SkipListNode<T> curr) {
            this.pred = pred;
            this.curr = curr;
        }
        public T getPred() {
            return pred.value;
        }
        public T getCurr() {
            return curr.value;
        }
        @Override
        public String toString() {
            return "PRED " + pred.value + ", CURR " + curr.value;
        }
    }

    public LockFreeSkipList() {
        for (int i = 0; i < head.next.length; i++) {
            head.next[i] = new AtomicMarkableReference<SkipListNode<T>>(tail, false);
        }
    }

    public boolean add(T x) {
        int topLevel = SkipList.randomLevel();
        int bottomLevel = 0;
        SkipListNode<T>[] preds = (SkipListNode<T>[]) new SkipListNode[MAX_LEVEL + 1];
        SkipListNode<T>[] succs = (SkipListNode<T>[]) new SkipListNode[MAX_LEVEL + 1];
        while (true) {
            boolean found = find(x, preds, succs);
            if (found)
                return false;

            SkipListNode<T> newNode = new SkipListNode(x, topLevel);
            for (int level = bottomLevel; level <= topLevel; level++) {
                SkipListNode<T> succ = succs[level];
                newNode.next[level].set(succ, false);
            }
            SkipListNode<T> pred = preds[bottomLevel];
            SkipListNode<T> succ = succs[bottomLevel];
            newNode.next[bottomLevel].set(succ, false);
            if (!pred.next[bottomLevel].compareAndSet(succ, newNode, false, false))
                continue;

            for (int level = bottomLevel + 1; level <= topLevel; level++) {
                while (true) {
                    pred = preds[level];
                    succ = succs[level];
                    if (pred.next[level].compareAndSet(succ, newNode, false, false))
                        break;
                    find(x, preds, succs);
                }
            }
            return true;
        }
    }

    public boolean remove(T x) {
        int bottomLevel = 0;
        SkipListNode<T>[] preds = (SkipListNode<T>[]) new SkipListNode[MAX_LEVEL + 1];
        SkipListNode<T>[] succs = (SkipListNode<T>[]) new SkipListNode[MAX_LEVEL + 1];
        SkipListNode<T> succ;
        while (true) {
            boolean found = find(x, preds, succs);
            if (!found)
                return false;

            SkipListNode<T> nodeToRemove = succs[bottomLevel];
            for (int level = nodeToRemove.topLevel; level >= bottomLevel + 1; level--) {
                boolean[] marked = {false};
                succ = nodeToRemove.next[level].get(marked);
                while (!marked[0]) {
                    nodeToRemove.next[level].attemptMark(succ, true);
                    succ = nodeToRemove.next[level].get(marked);
                }
            }
            boolean[] marked = {false};
            succ = nodeToRemove.next[bottomLevel].get(marked);
            while (true) {
                boolean iMarkedIt = nodeToRemove.next[bottomLevel].compareAndSet(succ, succ, false, true);
                succ = succs[bottomLevel].next[bottomLevel].get(marked);
                if (iMarkedIt) {
                    find(x, preds, succs);
                    return true;
                } else if (marked[0]) {
                    return false;
                }
            }
        }
    }

    // key is included in the range [pred, curr)
    public SkipListNode<T> findPred(int key) {
        int bottomLevel = 0;
        boolean[] marked = {false};
        SkipListNode<T> pred = null, curr = null, succ = null;
        retry: while (true) {
            pred = head;
            for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
                curr = pred.next[level].getReference();
                while (true) {
                    succ = curr.next[level].get(marked);
                    while (marked[0]) {
                        curr = pred.next[level].getReference();
                        succ = curr.next[level].get(marked);
                    }
                    if (curr.key <= key) {
                        pred = curr;
                        curr = succ;
                    } else {
                        break;
                    }
                }
            }
            return pred;
        }
    }

    // key is included in the range [pred, curr)
    public SkipListNode<T> findCurr(int key) {
        int bottomLevel = 0;
        boolean[] marked = {false};
        SkipListNode<T> pred = null, curr = null, succ = null;
        retry: while (true) {
            pred = head;
            for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
                curr = pred.next[level].getReference();
                while (true) {
                    succ = curr.next[level].get(marked);
                    while (marked[0]) {
                        curr = pred.next[level].getReference();
                        succ = curr.next[level].get(marked);
                    }
                    if (curr.key <= key) {
                        pred = curr;
                        curr = succ;
                    } else {
                        break;
                    }
                }
            }
            return curr;
        }
    }

    private boolean find(T x, SkipListNode<T>[] preds, SkipListNode<T>[] succs) {
        int bottomLevel = 0;
        int key = x.hashCode();
        boolean[] marked = {false};
        boolean snip;
        SkipListNode<T> pred = null, curr = null, succ = null;
        retry: while (true) {
            pred = head;
            for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
                curr = pred.next[level].getReference();
                while (true) {
                    succ = curr.next[level].get(marked);
                    while (marked[0]) {
                        snip = pred.next[level].compareAndSet(curr, succ, false, false);
                        if (!snip) continue retry;
                        curr = pred.next[level].getReference();
                        succ = curr.next[level].get(marked);
                    }
                    if (curr.key < key) {
                        pred = curr;
                        curr = succ;
                    } else {
                        break;
                    }
                }
                preds[level] = pred;
                succs[level] = curr;
            }
            return curr.key == key;
        }
    }

    public boolean contains(T x) {
        int bottomLevel = 0;
        int v = x.hashCode();
        boolean[] marked = {false};
        SkipListNode<T> pred = head, curr = null, succ = null;
        for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
            curr = pred.next[level].getReference();
            while (true) {
                succ = curr.next[level].get(marked);
                while (marked[0]) {
                    curr = pred.next[level].getReference();
                    succ = curr.next[level].get(marked);
                }
                if (curr.key < v) {
                    pred = curr;
                    curr = succ;
                } else {
                    break;
                }
            }
        }
        return curr.key == v;
    }
}