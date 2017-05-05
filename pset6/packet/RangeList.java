package pset6;

class RangeNode {
    int begin;
    int end;
    public RangeNode(int addressBegin, int addressEnd) {
        this.begin = addressBegin;
        this.end = addressEnd;
    }

    @Override
    public String toString() {
        return "[" + begin + ", " + end + ")";
    }

    @Override
    public int hashCode() {
        return this.begin;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj.getClass() != this.getClass()) return false;
        RangeNode other = (RangeNode)obj;
        return other.begin == this.begin && other.end == this.end;
    }
}

class RangeList {

    // To be replaced with skip lists. Must be some list where all the elements are sorted.
    // A range [a, b) is represented by RangeNode(a, b)
    LockFreeSkipList<RangeNode> ranges;
    final int min;
    final int max;

    public RangeList(int min, int max) {
        this.min = min;
        this.max = max + 1;
        ranges = new LockFreeSkipList<RangeNode>();
        ranges.add(new RangeNode(this.min, this.max));
    }

    /**
     * Returns whether the value is in the range
     * @param val value to check
     * @return true iff the value is in the range
     */
    public boolean contains(int val) {
        // The window will return two ranges [a, b) and [c, d)
        // val is by definition in the range [a, c)
        // The range list contains val if val is in [a, b), and doesn't if val is in [b, c)
        LockFreeSkipList<RangeNode>.SkipListWindow<RangeNode> window = ranges.findWindow(val);
        RangeNode pred = window.getPred();
        return pred != null && val < pred.end;
    }

    /**
     * Modifies the ranges to include the interval [start, end)
     * @param begin range begin, inclusive
     * @param end range end, exclusive
     */
    public void add(int begin, int end) {
        LockFreeSkipList<RangeNode>.SkipListWindow<RangeNode> window;
        RangeNode pred, curr;
        int beginToAdd = begin;
        int endToAdd = end;

        // Include begin
        window = ranges.findWindow(begin);
        pred = window.getPred();
        curr = window.getCurr();
        if (pred != null && begin < pred.end) {
            // Range is already included
            if (end <= pred.end) return;
            beginToAdd = pred.begin;
            ranges.remove(pred);
        }

        // Include end
        window = ranges.findWindow(end - 1);
        pred = window.getPred();
        curr = window.getCurr();
        if (pred != null && end <= pred.end) {
            endToAdd = pred.end;
            ranges.remove(pred);
        }

        // Add the range
        ranges.add(new RangeNode(beginToAdd, endToAdd));

        // Remove overlapping ranges (pred should be the newly-added range)
        LockFreeSkipList<RangeNode>.SkipListNode<RangeNode> currNode = ranges.findWindow(begin).curr;
        while (currNode.value != null && currNode.value.begin <= endToAdd) {
            ranges.remove(currNode.value);
            currNode = currNode.next[0].getReference();
        }
    }

    /**
     * Modifies the ranges to exclude the interval [start, end)
     * @param start range start, inclusive
     * @param end range end, exclusive
     */
    public void remove(int begin, int end) {
        LockFreeSkipList<RangeNode>.SkipListWindow<RangeNode> window;
        RangeNode pred, curr;

        // Remove begin
        window = ranges.findWindow(begin);
        pred = window.getPred();
        curr = window.getCurr();
        if (pred != null && begin < pred.end) {
            ranges.remove(pred);
            if (begin > pred.begin)
                ranges.add(new RangeNode(pred.begin, begin));
            if (end < pred.end) {
                ranges.add(new RangeNode(end, pred.end));
                return;
            }
        }

        // Remove end
        window = ranges.findWindow(end - 1);
        pred = window.getPred();
        curr = window.getCurr();
        if (pred != null && end <= pred.end) {
            ranges.remove(pred);
            if (end < pred.end) {
                ranges.add(new RangeNode(end, pred.end));
            }
        }

        // Remove overlapping ranges
        LockFreeSkipList<RangeNode>.SkipListNode<RangeNode> currNode = ranges.findWindow(begin).curr;
        while (currNode.value != null && currNode.value.end <= end) {
            ranges.remove(currNode.value);
            currNode = currNode.next[0].getReference();
        }
    }

    @Override
    public String toString() {
        LockFreeSkipList<RangeNode>.SkipListNode<RangeNode> node = ranges.head.next[0].getReference();
        String str = "";
        while (node.value != null) {
            str += node.value.toString() + " ";
            node = node.next[0].getReference();
        }
        return str;
    }
}

class RangeListTest {
    public static void main(String[] args) {
        RangeList list = new RangeList(1, 100);
        System.out.println(list);  // [1, 100)

        list.remove(10, 30);
        System.out.println(list);  // [1, 10) [30, 100)

        list.add(5, 40);
        System.out.println(list);  // [1, 100)

        list.remove(1, 10);
        list.remove(20, 50);
        list.remove(90, 100);
        System.out.println(list);  // [10, 20) [50, 90) [100, 101)

        list.add(15, 92);
        System.out.println(list);  // [10, 92) [100, 101)
    }
}