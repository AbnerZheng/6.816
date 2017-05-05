package pset6;

class PSource {

    final HashTable<Boolean> permissions;
    final int logSize = 4;
    final int maxBucketSize = 4;
    final int maxProbes = 4;

    public PSource() {
//        permissions = new LockFreeHashTable<Boolean>(logSize, maxBucketSize);
        permissions = new LinearProbeHashTable<Boolean>(logSize, maxProbes);
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
     * @param permission new permission value
     */
    public void set(int address, boolean permission) {
        permissions.add(address, permission);
    }
}