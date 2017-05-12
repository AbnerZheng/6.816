package pset6;

import java.util.List;

class Dispatcher implements FirewallWorker {

    private final PaddedPrimitiveNonVolatile<Boolean> done;
    private final List<WaitFreeQueue<Packet>> queues;
    private final PacketGenerator source;
    private final int numWorkers;
    private final boolean original;
    long totalPackets = 0;

    public Dispatcher(PaddedPrimitiveNonVolatile<Boolean> done,
                      List<WaitFreeQueue<Packet>> queues,
                      PacketGenerator source,
                      int numWorkers,
                      boolean original) {
        this.done = done;
        this.queues = queues;
        this.source = source;
        this.numWorkers = numWorkers;
        this.original = original;
    }

    public void run() {
        if (original) oneAtATime();
        else fillToTheMax();
    }

    private void oneAtATime() {
        Packet pkt;
        while (!done.value) {
            for (int i = 0; i < numWorkers; i++) {
                pkt = source.getPacket();
                while (true) {
                    try {
                        queues.get(i).enq(pkt);
                        totalPackets++;
                        break;
                    } catch (FullException e) {
                        continue; // Try again until it's not full
                    }
                }
            }
        }
    }

    private void fillToTheMax() {
        Packet pkt;
        while (!done.value) {
            // Add a packet to each queue
            for (int i = 0; i < numWorkers; i++) {
                pkt = source.getPacket();
                try {
                    queues.get(i).enq(pkt);
                    totalPackets++;
                    break;
                } catch (FullException e) {
                    continue; // Try again until it's not full
                }
            }
        }
    }
}