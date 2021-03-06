package pset6;

/**
 * A histogram of fingerprints, which take values in [0, 2^16).
 */
class Histogram {

    final int maxValue;
    final int numBuckets;
    final int bucketSize;
    final int[] buckets;

    public Histogram() {
        this.maxValue = 1 << 16;
        this.numBuckets = 1 << 7;
        this.bucketSize = maxValue / numBuckets;
        buckets = new int[numBuckets];
    }

    public Histogram(int numBuckets) {
        this.maxValue = 1 << 16;
        this.numBuckets = numBuckets;
        this.bucketSize = maxValue / numBuckets;
        buckets = new int[numBuckets];
    }

    /**
     * Adds a value to the histogram
     * @param val value to add to the histogram
     */
    public void add(int val) {
        int bucketNum = (int) (val / bucketSize);
        buckets[bucketNum]++;
    }

    public long getTotalPackets() {
        long sum = 0;
        for (int i = 0; i < numBuckets; i++) {
            sum += buckets[i];
        }
        return sum;
    }

    @Override
    public String toString() {
	System.out.println(numBuckets + " buckets, each of size " + bucketSize + " up to " + maxValue);
//        String[] strs = new String[numBuckets];
//        for (int i = 0; i < numBuckets; i++) {
//            strs[i] = i * bucketSize + "-" + (i + 1) * bucketSize + "\t " + buckets[i];
//        }
//        return "Number of Buckets: " + numBuckets + "\nBucket Size: " + bucketSize + "\n" + String.join("\n", strs);
//        String[] strs = new String[numBuckets];
        String strs = "";
	for (int i = 0; i < numBuckets; i++) {
            strs += Integer.toString(buckets[i]) + ", ";
        }
        return strs + "\nTotal processed: " + getTotalPackets();
    }
}
