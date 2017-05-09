package pset6;

import java.util.Random;
import java.util.concurrent.atomic.AtomicMarkableReference;

interface SkipList<T> {
    public boolean add(T x);
    public boolean remove(T x);
    public boolean contains(T x);
}

@SuppressWarnings("unchecked")
class LockFreeSkipList<T> implements SkipList<T> {
    private static final int MAX_LEVEL = 8;
    private static final Random rand = new Random();
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

    private int randomLevel() {
        return rand.nextInt(MAX_LEVEL);
    }

    public boolean add(T x) {
        int topLevel = randomLevel();
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

    // key is included in the range [pred, succ)
    public SkipListWindow<T> findWindow(int key) {
        int bottomLevel = 0;
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
                if (level == bottomLevel) {
                    return new SkipListWindow<T>(pred, curr);
                }
            }
        }
    }

    public boolean find(T x, SkipListNode<T>[] preds, SkipListNode<T>[] succs) {
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