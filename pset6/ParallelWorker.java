package pset6;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;
import java.util.List;
import java.util.Random;

class ParallelWorker implements FirewallWorker {

    // Worker attributes
    private final int threadID;
    private final int numWorkers;
    private final int numAddressesLog;
    private final PacketGenerator source;

    // Obtain packet tasks
    private final PaddedPrimitiveNonVolatile<Boolean> done;
    private final List<WaitFreeQueue<Packet>> queues;
    private final List<ReentrantLock> locks;

    // Statistics
    private final Fingerprint fingerprint;
    private final PSource png;
    private final PDestination r;
    private Histogram histogram;
    private Histogram cached;
    long totalPackets = 0;

    // Train cache
    private int tag = -1;

    // Global lock??
    private static ReentrantReadWriteLock globalLock = new ReentrantReadWriteLock();

    public ParallelWorker(int threadID,
                          int numWorkers,
                          int numAddressesLog,
                          PacketGenerator source,
                          PaddedPrimitiveNonVolatile<Boolean> done,
                          List<WaitFreeQueue<Packet>> queues,
                          List<ReentrantLock> locks,
                          PSource png,
                          PDestination r,
                          Histogram histogram) {
        this.threadID = threadID;
        this.numWorkers = numWorkers;
        this.numAddressesLog = numAddressesLog;
        this.source = source;
        this.done = done;
        this.queues = queues;
        this.locks = locks;
        this.fingerprint = new Fingerprint();
        this.png = png;
        this.r = r;
        this.histogram = new Histogram();
        this.cached = histogram;
    }

    /**
     * Process A^(3/2), where A is the number of addresses in the system or 2^numAddressesLog, configuration
     * packets to ensure the permissions tables are in steady state.
     */
    public void initConfig() {
        System.out.printf("Initializing permissions table");
        final int numAddresses = 1 << numAddressesLog;
        final int initSize = (int)Math.pow(numAddresses, 1.5);
        final int initSizeFrac = initSize / 20;
        for (int i = 0; i < initSize; i++) {
            if (i % initSizeFrac == initSizeFrac - 1)
                System.out.printf(".");
            handleConfigPacket(source.getConfigPacket().config);
        }
        System.out.println("DONE");
    }

    public void run() {
        histogram = cached;
        runRandomQueue();
        cleanUp();
    }

    private void runRandomQueue() {
        Random rand = new Random();
        WaitFreeQueue<Packet> queue;
        ReentrantLock lock;
        Packet pkt;
        while (!done.value) {
            // Choose a random queue
            int id = rand.nextInt(numWorkers);
            queue = queues.get(id);
            lock = locks.get(id);

            // Try to dequeue and process the packet
            try {
                lock.lock();
                pkt = queue.deq();
            } catch (EmptyException e) {
                pkt = null;
            } finally {
                lock.unlock();
            }
            if (pkt != null)
                processPacket(pkt);
        }
    }

    public Histogram getHistogram() {
        return histogram;
    }

    public void printHistogram() {
        System.out.println(histogram);
    }

    /**
     * Executes the task specified by the packet
     * @param pkt packet
     */
    private void processPacket(Packet pkt) {
        switch (pkt.type) {
        case ConfigPacket:
            handleConfigPacket(pkt.config);
            break;
        case DataPacket:
            handleDataPacket(pkt.header, pkt.body);
            break;
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
    private int match = 0;
    private int noMatch = 0;
    private int fails = 0;
    private void handleDataPacket(Header header, Body body) {
        final int source = header.source;
        final int dest = header.dest;

//        System.out.println("MATCH " + match + ", NOMATCH " + noMatch + ", FAIL " + fails);
        // The packet does not have the appropriate permissions
        if (header.tag != tag) {
            try {
                globalLock.readLock().lock();
                if (!png.isValid(source) || !r.isValid(source, dest)) {
                    fails++;
                    return;
                }
            } finally {
                globalLock.readLock().unlock();
            }
        }

        // Process the packet
        int fprnt = fingerprint.getFingerprint(body.iterations, body.seed);
        histogram.add(fprnt);
        tag = header.tag;
    }

    /**
     * Modifies the permissions of a particular address in both source and destination contexts.
     * Serializable with other configuration packets.
     * @param config packet configuration
     */
    private void handleConfigPacket(Config config) {
        final int address = config.address;
        png.set(address, config.personaNonGrata);
        try {
            globalLock.writeLock().lock();
            r.set(address, config.addressBegin, config.addressEnd, config.acceptingRange);
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    /**
     * Leave the queue corresponding to its thread id empty before finishing
     */
    private void cleanUp() {
        WaitFreeQueue<Packet> queue = queues.get(threadID);
        while (true) {
            try {
                Packet pkt = queue.deq();
                processPacket(pkt);
            } catch (EmptyException e) {
                return;
            }
        }
    }
}
