package pset6;

//class RangeNode {
//    int address;
//    boolean start;  // true if address begin, false if address end
//    private RangeNode(int address, boolean start) {
//        this.address = address;
//        this.start = start;
//    }
//
//    public static RangeNode start(int address) {
//        return new RangeNode(address, true);
//    }
//    public static RangeNode end(int address) {
//        return new RangeNode(address, false);
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (obj == null) return false;
//        if (obj.getClass() != this.getClass()) return false;
//        RangeNode other = (RangeNode)obj;
//        return other.address == this.address;
//    }
//
//    public int compareTo(RangeNode node) {
//        if (this.address < node.address) return -1;
//        if (this.address > node.address) return 1;
//        return 0;
//    }
//
//    public int compareTo(int val) {
//        if (this.address < val) return -1;
//        if (this.address > val) return 1;
//        return 0;
//    }
//}

class RangeNode {
    int begin;
    int end;
    public RangeNode(int addressBegin, int addressEnd) {
        this.begin = addressBegin;
        this.end = addressEnd;
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
    LockFreeList<RangeNode> ranges;
    final int min;
    final int max;

    public RangeList(int min, int max) {
        this.min = min;
        this.max = max + 1;
        ranges = new LockFreeList<RangeNode>();
//        ranges.add(min, RangeNode.start(min));
//        ranges.add(max, RangeNode.end(max));
        ranges.add(min, new RangeNode(min, max));
    }

    /**
     * Returns whether the value is in the range
     * @param val value to check
     * @return true iff the value is in the range
     */
    public boolean contains(int val) {
//        Window<RangeNode> window = ranges.find(val);
//        return (window.getPred().start && window.getSucc().address > val);

        // The window will return two ranges [a, b) and [c, d)
        // val is by definition in the range [a, c)
        // The range list contains val if val is in [a, b), and doesn't if val is in [b, c)
        Window<RangeNode> window = ranges.findInclusive(val);
        RangeNode pred = window.getPred();
        return pred != null && val < pred.end;
    }

    /**
     * Modifies the ranges to include the interval [start, end)
     * @param begin range begin, inclusive
     * @param end range end, exclusive
     */
    public void add(int begin, int end) {
        Window<RangeNode> window;
        RangeNode pred, curr;
        int beginToAdd = begin;
        int endToAdd = end;

        // Include begin
        window = ranges.findInclusive(begin);
        pred = window.getPred();
        curr = window.getCurr();
        if (pred != null && begin < pred.end) {
            // Range is already included
            if (end <= pred.end) return;
            beginToAdd = pred.begin;
            ranges.remove(pred.begin);
        }

        // Include end
        window = ranges.findInclusive(end - 1);
        pred = window.getPred();
        curr = window.getCurr();
        if (pred != null && end <= pred.end) {
            endToAdd = pred.end;
            ranges.remove(pred.begin);
        }

        // Add the range
        ranges.add(beginToAdd, new RangeNode(beginToAdd, endToAdd));

        // Remove overlapping ranges (pred should be the newly-added range)
        AtomicNode<RangeNode> currNode = ranges.findInclusive(begin).curr;
        while (currNode.val != null && currNode.val.begin <= endToAdd) {
            ranges.remove(currNode.val.begin);
            currNode = currNode.next.getReference();
        }
    }

    /**
     * Modifies the ranges to exclude the interval [start, end)
     * @param start range start, inclusive
     * @param end range end, exclusive
     */
    public void remove(int begin, int end) {
        Window<RangeNode> window;
        RangeNode pred, curr;

        // Remove begin
        window = ranges.findInclusive(begin);
        pred = window.getPred();
        curr = window.getCurr();
        if (pred != null && begin < pred.end) {
            ranges.remove(pred.begin);
            if (begin > pred.begin)
                ranges.add(pred.begin, new RangeNode(pred.begin, begin));
            if (end < pred.end) {
                ranges.add(end, new RangeNode(end, pred.end));
                return;
            }
        }

        // Remove end
        window = ranges.findInclusive(end - 1);
        pred = window.getPred();
        curr = window.getCurr();
        if (pred != null && end <= pred.end) {
            ranges.remove(pred.begin);
            if (end < pred.end) {
                ranges.add(end, new RangeNode(end, pred.end));
            }
        }

        // Remove overlapping ranges
        AtomicNode<RangeNode> currNode = ranges.findInclusive(begin).curr;
        while (currNode.val != null && currNode.val.end <= end) {
            ranges.remove(currNode.val.begin);
            currNode = currNode.next.getReference();
        }
    }
}