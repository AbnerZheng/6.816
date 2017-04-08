import java.util.Random;
import java.util.List;

public interface PacketWorker extends Runnable {
    long totalPackets = 0;
    public void run();
}

class SerialPacketWorker implements PacketWorker {

    PaddedPrimitiveNonVolatile<Boolean> done;
    final PacketSource pkt;
    final Fingerprint residue = new Fingerprint();
    long fingerprint = 0;
    long totalPackets = 0;
    final int numSources;
    final boolean uniformBool;

    public SerialPacketWorker(PaddedPrimitiveNonVolatile<Boolean> done,
                              PacketSource pkt,
                              boolean uniformBool,
                              int numSources) {
        this.done = done;
        this.pkt = pkt;
        this.uniformBool = uniformBool;
        this.numSources = numSources;
    }

    public void run() {
        Packet tmp;
        while (!done.value) {
            for (int i = 0; i < numSources; i++) {
                if (uniformBool)
                    tmp = pkt.getUniformPacket(i);
                else
                    tmp = pkt.getExponentialPacket(i);
                totalPackets++;
                fingerprint += residue.getFingerprint(tmp.iterations, tmp.seed);
            }
        }
    }
}

class Dispatcher implements PacketWorker {

    private final PaddedPrimitiveNonVolatile<Boolean> done;
    private final List<WaitFreeQueue<Packet>> queues;
    private final PacketSource pkt;
    long totalPackets = 0;

    // Parameters
    private final int numSources;
    private final boolean uniformBool;
    private final int queueDepth;

    public Dispatcher(PaddedPrimitiveNonVolatile<Boolean> done,
                      List<WaitFreeQueue<Packet>> queues,
                      PacketSource pkt,
                      int numSources,
                      boolean uniformBool,
                      int queueDepth) {
        this.done = done;
        this.queues = queues;
        this.pkt = pkt;
        this.numSources = numSources;
        this.uniformBool = uniformBool;
        this.queueDepth = queueDepth;
        assert numSources == queues.size();
    }

    public void run() {
        Packet packet;
        boolean ready = true;
        while (!done.value) {
            // Add a packet to each queue
            for (int i = 0; i < numSources; i++) {
                packet = uniformBool ? pkt.getUniformPacket(i) : pkt.getExponentialPacket(i);
                while (true) {
                    try {
                        queues.get(i).enq(packet);
                        totalPackets++;
                        break;
                    } catch (FullException e) {
                        continue; // Try again
                    }
                }
            }
        }
    }
}


class ParallelPacketWorker implements PacketWorker {

    public enum Strategy {
        LockFree,
        HomeQueue,
        RandomQueue,
        LastQueue
    }

    private final int threadID;
    private final PaddedPrimitiveNonVolatile<Boolean> done;
    private final List<WaitFreeQueue<Packet>> queues;
    private final List<Lock> locks;

    // For doing work
    final Fingerprint residue = new Fingerprint();
    long fingerprint = 0;
    long totalPackets = 0;

    // Parameters
    private final int numSources;
    private final Strategy strategy;

    public ParallelPacketWorker(int threadID,
                                PaddedPrimitiveNonVolatile<Boolean> done,
                                List<WaitFreeQueue<Packet>> queues,
                                List<Lock> locks,
                                int numSources,
                                short strategy) {
        this.threadID = threadID;
        this.done = done;
        this.queues = queues;
        this.locks = locks;
        this.numSources = numSources;
        switch (strategy) {
            case 0: this.strategy = Strategy.LockFree; break;
            case 1: this.strategy = Strategy.HomeQueue; break;
            case 2: this.strategy = Strategy.RandomQueue; break;
            default: this.strategy = Strategy.LastQueue;
        };

        assert 0 <= threadID && threadID < queues.size();
        if (this.strategy != Strategy.LockFree)
            assert locks.size() == queues.size();
    }

    public void run() {
        switch (strategy) {
            case LockFree:
                runLockFree();
                break;
            case HomeQueue:
                runHomeQueue();
                break;
            case RandomQueue:
                runRandomQueue();
                break;
            case LastQueue:
                runLastQueue();
                break;
            default:
                break;
        }
        cleanUp();
    }

    /**
     * Update the number of packets processed and get the fingerprint
     * @param pkt packet to be processed
     */
    private void processPacket(Packet pkt) {
        totalPackets++;
        fingerprint += residue.getFingerprint(pkt.iterations, pkt.seed);
    }

    /**
     * Grabs the lock and tries to dequeue a packet.
     * @param queue a wait-free queue
     * @param lock corresponding lock
     * @return the dequeued packet, or null if the queue is empty
     */
    private Packet deq(WaitFreeQueue<Packet> queue, Lock lock) {
        try {
            lock.lock();
            return queue.deq();
        } catch (EmptyException e) {
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param rand a random object
     * @return the id of a random queue that does not appear to be contended
     */
    private int pickUncontendedID(Random rand) {
        int id = rand.nextInt(numSources);
        Lock lock = locks.get(id);
        while (lock.isContended()) {
            id = rand.nextInt(numSources);
            lock = locks.get(id);
        }
        return id;
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

    private void runLockFree() {
        WaitFreeQueue<Packet> queue = queues.get(threadID);
        while (!done.value) {
            try {
                Packet pkt = queue.deq();
                processPacket(pkt);
            } catch (EmptyException e) {
                continue;
            }
        }
    }

    private void runHomeQueue() {
        WaitFreeQueue<Packet> queue = queues.get(threadID);
        Lock lock = locks.get(threadID);
        while (!done.value) {
            Packet pkt = deq(queue, lock);
            if (pkt != null)
                processPacket(pkt);
        }
    }

    private void runRandomQueue() {
        Random rand = new Random();
        WaitFreeQueue<Packet> queue;
        Lock lock;
        while (!done.value) {
            // Choose a random queue
            int id = rand.nextInt(numSources);
            queue = queues.get(id);
            lock = locks.get(id);

            // Try to dequeue and process the packet
            Packet pkt = deq(queue, lock);
            if (pkt != null)
                processPacket(pkt);
        }
    }

    private void runLastQueue() {
        int id;
        Random rand = new Random();
        WaitFreeQueue<Packet> queue;
        Lock lock;

        // Choose a random uncontended queue
        id = pickUncontendedID(rand);
        lock = locks.get(id);
        queue = queues.get(id);
        while (!done.value) {
            Packet pkt = deq(queue, lock);
            if (pkt != null) {
                processPacket(pkt);
            } else {
                // Pick another random uncontended queue
                id = pickUncontendedID(rand);
                lock = locks.get(id);
                queue = queues.get(id);
            }
        }
    }
}
