package pset6;

class PDestination {

    final HashTable<RangeList> table;
    final int minAddress;
    final int maxAddress;
    final int logSize = 4;
    final int maxBucketSize = 4;
    final int maxProbes = 4;

    public PDestination(int numAddressesLog) {
        minAddress = 0;
        maxAddress = 1 << numAddressesLog;
        table = new LinearProbeHashTable<RangeList>(logSize, maxProbes);
    }

    /**
     * Returns whether the source has permissions to send packets to the destination.
     * @param source starting address
     * @param destination end address
     * @return true iff the source is allowed to send packets to the destination
     */
    public boolean isValid(int source, int destination) {
        RangeList list = table.get(destination);

        // If the list does not exist, allow the packet to go through
        if (list == null) {
            return true;
        } else {
            return list.contains(source);
        }
    }

    /**
     * Modifies address to either accept or reject the range of addresses from [addressBegin, addressEnd),
     * depending on the boolean value of acceptingRange.
     * @param sourceAddress the address whose permissions are to be modified
     * @param addressBegin range start, inclusive
     * @param addressEnd range end, exclusive
     * @param acceptingRange true iff the address should accept this range
     */
    public void set(int sourceAddress, int addressBegin, int addressEnd, boolean acceptingRange) {
        RangeList list = table.get(addressEnd);
        if (list == null) {
            list = new RangeList(minAddress, maxAddress);
            table.add(addressEnd, list);
        }
        if (acceptingRange) {
            list.add(addressBegin, addressEnd);
        } else {
            list.remove(addressBegin, addressEnd);
        }
    }
}