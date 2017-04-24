import java.util.concurrent.locks.Lock;
import java.util.List;

public interface HashPacketWorker<T> extends Runnable {
  public void run();
}

class HashPacketDispatcher implements HashPacketWorker {

  private final PaddedPrimitiveNonVolatile<Boolean> done;
  private final List<WaitFreeQueue<HashPacket<Packet>>> queues;
  private final HashPacketGenerator source;
  public long totalPackets = 0;

  // Parameters
  private final int numWorkers;

  public HashPacketDispatcher(PaddedPrimitiveNonVolatile<Boolean> done,
                              List<WaitFreeQueue<HashPacket<Packet>>> queues,
                              HashPacketGenerator source,
                              int numWorkers) {
    this.done = done;
    this.queues = queues;
    this.source = source;
    this.numWorkers = numWorkers;
    assert numWorkers == queues.size();
  }

  public void run() {
    HashPacket<Packet> pkt;
    while (!done.value) {
      // Add a packet to each queue
      for (int i = 0; i < numWorkers; i++) {
        pkt = source.getRandomPacket();
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
}

class SerialHashPacketWorker implements HashPacketWorker {
  PaddedPrimitiveNonVolatile<Boolean> done;
  final HashPacketGenerator source;
  final SerialHashTable<Packet> table;
  long totalPackets = 0;
  long residue = 0;
  Fingerprint fingerprint;
  public SerialHashPacketWorker(
    PaddedPrimitiveNonVolatile<Boolean> done, 
    HashPacketGenerator source,
    SerialHashTable<Packet> table) {
    this.done = done;
    this.source = source;
    this.table = table;
    fingerprint = new Fingerprint();
  }
  
  public void run() {
    HashPacket<Packet> pkt;
    while( !done.value ) {
      totalPackets++;
      pkt = source.getRandomPacket();
      residue += fingerprint.getFingerprint(pkt.getItem().iterations,pkt.getItem().seed);
      switch(pkt.getType()) {
        case Add: 
          table.add(pkt.mangleKey(),pkt.getItem());
          break;
        case Remove:
          table.remove(pkt.mangleKey());
          break;
        case Contains:
          table.contains(pkt.mangleKey());
          break;
      }
    }
  }  
}

class ParallelHashPacketWorker implements HashPacketWorker {

  private final int threadID;
  private final PaddedPrimitiveNonVolatile<Boolean> done;
  private final List<WaitFreeQueue<HashPacket<Packet>>> queues;
  private final List<HashTable<Packet>> tables;
  private final int numWorkers;

  // Statistics
  private final Fingerprint fingerprint = new Fingerprint();
  long totalPackets = 0;
  long residue = 0;

  public ParallelHashPacketWorker(int threadID,
                                  PaddedPrimitiveNonVolatile<Boolean> done,
                                  List<WaitFreeQueue<HashPacket<Packet>>> queues,
                                  List<HashTable<Packet>> tables,
                                  int numWorkers) {
    this.threadID = threadID;
    this.done = done;
    this.queues = queues;
    this.tables = tables;
    this.numWorkers = numWorkers;
  }

  public void run() {

  }
}
