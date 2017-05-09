package pset6;

interface FirewallWorker extends Runnable {
    public void run();
}

class SerialWorker implements FirewallWorker {

    final PaddedPrimitiveNonVolatile<Boolean> done;
    final PacketGenerator source;
    final Fingerprint fingerprint;
    final PSource png;
    final PDestination r;
    Histogram histogram;
    long totalPackets = 0;

    final int numAddressesLog;

    public SerialWorker(PaddedPrimitiveNonVolatile<Boolean> done, PacketGenerator source, int numAddressesLog) {
        this.done = done;
        this.source = source;
        this.fingerprint = new Fingerprint();
        this.png = new PSource(numAddressesLog  );
        this.r = new PDestination(numAddressesLog);
        this.histogram = new Histogram();
        this.numAddressesLog = numAddressesLog;
    }

    /**
     * Process A^(3/2), where A is the number of addresses in the system or 2^numAddressesLog, configuration
     * packets to ensure the permissions tables are in steady state.
     */
    public void initConfig() {
        System.out.printf("Initializing permissions table");
        Packet pkt;
        final int numAddresses = 1 << numAddressesLog;
        final int initSize = (int)Math.pow(numAddresses, 1.5);
        final int initSizeFrac = initSize / 20;
        for (int i = 0; i < initSize; i++) {
            if (i % initSizeFrac == initSizeFrac - 1)
                System.out.printf(".");
            pkt = source.getConfigPacket();
            handleConfigPacket(pkt.config);
        }
        System.out.println("DONE");
    }

    /**
     * Print a graphical representation of the fingerprint histogram data, where the x-axis is divided into
     * buckets based on the [0, 2^16) possible fingerprints and the y-axis is the frequency of each bucket.
     */
    public void printHistogram() {
        System.out.println(histogram);
    }

    /**
     * Process packets until given the signal to stop.
     */
    public void run() {
        Packet pkt;
        histogram = new Histogram();
        while (!done.value) {
            pkt = source.getPacket();
            switch(pkt.type) {
                case ConfigPacket:
                    handleConfigPacket(pkt.config);
                    break;
                case DataPacket:
                    handleDataPacket(pkt.header, pkt.body);
                    break;
            }
            totalPackets++;
        }
    }

    /**
     * Processes a data packet by doing the following:
     * Applies the fingerprint and adds the value to the histogram. Could do something special with packet trains?
     *
     * Ignores the data packet if it does not have the appropriate permissions!
     * @param header packet header
     * @param body packet body
     */
    private void handleDataPacket(Header header, Body body) {
        final int source = header.source;
        final int dest = header.dest;

        // The packet does not have the appropriate permissions
        if (!png.isValid(source) || !r.isValid(source, dest)) return;

        // Process the packet
        int fprnt = fingerprint.getFingerprint(body.iterations, body.seed);
        histogram.add(fprnt);
    }

    /**
     * Modifies the permissions of a particular address in both source and destination contexts.
     * Serializable with other configuration packets.
     * @param config packet configuration
     */
    private void handleConfigPacket(Config config) {
        final int address = config.address;
        png.set(address, config.personaNonGrata);
        r.set(address, config.addressBegin, config.addressEnd, config.acceptingRange);
    }
}