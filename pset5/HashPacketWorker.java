import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.Random;

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
  private final List<ReentrantLock> locks;
  private final HashTable<Packet> table;
  private final int numWorkers;

  // Statistics
  private final Fingerprint fingerprint = new Fingerprint();
  long totalPackets = 0;
  long residue = 0;

  public ParallelHashPacketWorker(int threadID,
                                  PaddedPrimitiveNonVolatile<Boolean> done,
                                  List<WaitFreeQueue<HashPacket<Packet>>> queues,
                                  List<ReentrantLock> locks,
                                  HashTable<Packet> table,
                                  int numWorkers) {
    this.threadID = threadID;
    this.done = done;
    this.queues = queues;
    this.locks = locks;
    this.table = table;
    this.numWorkers = numWorkers;
    assert 0 <= threadID && threadID < queues.size();
  }

  public void run() {
    runLastQueue();
    cleanUp();
  }

  private void runRandomQueue() {
    Random rand = new Random();
    WaitFreeQueue<HashPacket<Packet>> queue;
    ReentrantLock lock;
    while (!done.value) {
      // Choose a random queue
      int id = rand.nextInt(numSources);
      queue = queues.get(id);
      lock = locks.get(id);

      // Try to dequeue and process the packet
      HashPacket<Packet> pkt = deq(queue, lock);
      if (pkt != null)
        processPacket(pkt);
    }
  }

  private void runLastQueue() {
    int id;
    Random rand = new Random();
    WaitFreeQueue<HashPacket<Packet>> queue;
    ReentrantLock lock;

    // Choose a random uncontended queue
    id = pickUncontendedID(rand);
    queue = queues.get(id);
    lock = locks.get(id);
    while (!done.value) {
      HashPacket<Packet> pkt = deq(queue, lock);
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

  /**
   * Grabs the lock and tries to dequeue a packet.
   * @param queue a wait-free queue
   * @param lock corresponding lock
   * @return the dequeued packet, or null if the queue is empty
   */
  private HashPacket<Packet> deq(WaitFreeQueue<HashPacket<Packet>> queue, ReentrantLock lock) {
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
    int id = rand.nextInt(numWorkers);
    ReentrantLock lock = locks.get(id);
    while (lock.isLocked()) {
      id = rand.nextInt(numWorkers);
      lock = locks.get(id);
    }
    return id;
  }

  /**
   * Performs the operation specified by the hash packet onto the hash table
   * @param pkt hash packet
   * @param table table to do operation on
   */
  private void processPacket(HashPacket<Packet> pkt) {
    if (table == null) return;
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

  /**
   * Leave the queue corresponding to its thread id empty before finishing
   */
  private void cleanUp() {
    WaitFreeQueue<HashPacket<Packet>> queue = queues.get(threadID);
    while (true) {
      try {
        HashPacket<Packet> pkt = queue.deq();
        if (table != null)
          processPacket(pkt);
      } catch (EmptyException e) {
        return;
      }
    }
  }
}
