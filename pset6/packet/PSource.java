package pset6;

class PSource {

    final HashTable<Boolean> permissions;
    final int logSize = 4;
    final int maxBucketSize = 4;
    final int maxProbes = 4;
    final int numAddresses;

    public PSource(int numAddressesLog) {
        this.numAddresses = 1 << numAddressesLog;
        this.permissions = new ArrayHashTable<Boolean>(numAddressesLog);
    }

    /**
     * Returns whether the given address can send packets across this firewall at all.
     * @param address address to check
     * @return true iff the address is allowed to send packets
     */
    public boolean isValid(int address) {
        Boolean val = permissions.get(address);
        if (val == null) {
            return true;
        } else {
            return val;
        }
    }

    /**
     * Changes the permission of the given address
     * @param address address to set
     * @param png persona non grata
     */
    public void set(int address, boolean png) {
        permissions.add(address, !png);
    }

    /**
     * Displays the actual pngFraction.
     * @return
     */
    @Override
    public String toString() {
        int numValid = 0;
        for (int i = 0; i < numAddresses; i++) {
            if (isValid(i)) {
                numValid++;
            }
        }
        return "pngFraction = " + (1.0 - (double)numValid / numAddresses);
    }
}