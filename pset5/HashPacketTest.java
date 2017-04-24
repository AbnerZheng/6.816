import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

class SerialHashPacket {
  public static void main(String[] args) {

    final int numMilliseconds = Integer.parseInt(args[0]);    
    final float fractionAdd = Float.parseFloat(args[1]);
    final float fractionRemove = Float.parseFloat(args[2]);
    final float hitRate = Float.parseFloat(args[3]);
    final int maxBucketSize = Integer.parseInt(args[4]);
    final long mean = Long.parseLong(args[5]);
    final int initSize = Integer.parseInt(args[6]);

    @SuppressWarnings({"unchecked"})
    StopWatch timer = new StopWatch();
    HashPacketGenerator source = new HashPacketGenerator(fractionAdd,fractionRemove,hitRate,mean);
    PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
    PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);
    SerialHashTable<Packet> table = new SerialHashTable<Packet>(1, maxBucketSize);
    
    for( int i = 0; i < initSize; i++ ) {
      HashPacket<Packet> pkt = source.getAddPacket();
      table.add(pkt.mangleKey(), pkt.getItem());
    }
    SerialHashPacketWorker workerData = new SerialHashPacketWorker(done, source, table);
    Thread workerThread = new Thread(workerData);
    
    workerThread.start();
    timer.startTimer();
    try {
      Thread.sleep(numMilliseconds);
    } catch (InterruptedException ignore) {;}
    done.value = true;
    memFence.value = true;
    try {
      workerThread.join();
    } catch (InterruptedException ignore) {;}      
    timer.stopTimer();
    final long totalCount = workerData.totalPackets;
    System.out.println("count: " + totalCount);
    System.out.println("time: " + timer.getElapsedTime());
    System.out.println(totalCount/timer.getElapsedTime() + " pkts / ms");
  }
}

class ParallelHashPacket {
  public static void main(String[] args) {

    final int numMilliseconds = Integer.parseInt(args[0]);    
    final float fractionAdd = Float.parseFloat(args[1]);
    final float fractionRemove = Float.parseFloat(args[2]);
    final float hitRate = Float.parseFloat(args[3]);
    final int maxBucketSize = Integer.parseInt(args[4]);
    final long mean = Long.parseLong(args[5]);
    final int initSize = Integer.parseInt(args[6]);
    final int numWorkers = Integer.parseInt(args[7]); 
    final int tableType = Integer.parseInt(args[8]);
    final int queueDepth = 8;
    // -1=None, 0=Locking, 1=LockFree, 2=LinearProbe, 3=Cuckoo, 4=Awesome, 5=AppSpecific

    StopWatch timer = new StopWatch();

    // Allocate and initialize Lamport queues and hash tables (if tableType != -1)
    List<WaitFreeQueue<HashPacket<Packet>>> queues = new ArrayList<>();
    HashTable<Packet> table;
    for (int i = 0; i < numWorkers; i++) {
      queues.add(new WaitFreeQueue<HashPacket<Packet>>(queueDepth));
    }
    switch (tableType) {
      case 0: table = new LockingHashTable<Packet>(initSize, maxBucketSize); break;
      case 1: table = new LockFreeHashTable<Packet>(initSize, maxBucketSize); break;
      case 2: table = new LinearProbeHashTable<Packet>(initSize, maxBucketSize); break;
      case 3: table = new CuckooHashTable<Packet>(initSize, maxBucketSize); break;
      case 4: table = new AwesomeHashTable<Packet>(initSize, maxBucketSize); break;
      case 5: table = new AppSpecificHashTable<Packet>(initSize, maxBucketSize); break;
      default: table = null;
    }

    HashPacketGenerator source = new HashPacketGenerator(fractionAdd,fractionRemove,hitRate,mean);

    // initialize your hash table w/ initSize number of add() calls using
    for (int i = 0; i < initSize; i++) {
      HashPacket<Packet> addPacket = source.getAddPacket();
      table.add(addPacket.key, addPacket.body);
    }

    // Allocate and initialize locks and any signals used to marshal threads (eg. done signals)
    PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
    PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);
    List<ReentrantLock> locks = new ArrayList<>();
    for (int i = 0; i < numWorkers; i++) {
      // Best strategy from pset 4 was last queue and reentrant lock
      locks.add(new ReentrantLock(false));
    }

    // Allocate and initialize Dispatcher and Worker threads
    HashPacketDispatcher dispatchData = new HashPacketDispatcher(done, queues, source, numWorkers);
    Thread dispatchThread = new Thread(dispatchData);
    List<Thread> workerThreads = new ArrayList<>();
    for (int i = 0; i < numWorkers; i++) {
      HashPacketWorker workerData = new ParallelHashPacketWorker(i, done, queues, locks, table, numWorkers);
      Thread workerThread = new Thread(workerData);
      workerThreads.add(workerThread);
    }

    // Call .start() on your Workers
    for (Thread worker : workerThreads)
      worker.start();

    timer.startTimer();

    // Call .start() on your Dispatcher
    dispatchThread.start();

    try {
      Thread.sleep(numMilliseconds);
    } catch (InterruptedException ignore) {;}

    // Assert signals to stop Dispatcher
    //
    // Call .join() on Dispatcher
    //
    // Assert signals to stop Workers - they are responsible for leaving
    // the queues empty
    //
    // Call .join() for each Worker
    done.value = true;
    memFence.value = true;  // memFence is a 'volatile' forcing a memory fence
    try {                   // which means that done.value is visible to the workers
      dispatchThread.join();
      for (Thread workerThread : workerThreads)
        workerThread.join();
    } catch (InterruptedException ignore) {
      ;
    }

    timer.stopTimer();

    // Report the total number of packets processed and total time
    final long totalCount = dispatchData.totalPackets;
    System.out.println("count:\t" + totalCount);
    System.out.println("time:\t" + timer.getElapsedTime());
    System.out.println("thrpt:\t" + totalCount / timer.getElapsedTime() + " pkts / ms");
  }
}
