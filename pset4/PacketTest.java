import java.util.List;
import java.util.ArrayList;

class SerialPacket {
  public static void main(String[] args) {
    try {
      final int numMilliseconds = Integer.parseInt(args[0]);
      final int numSources = Integer.parseInt(args[1]);
      final long mean = Long.parseLong(args[2]);
      final boolean uniformFlag = Boolean.parseBoolean(args[3]);
      final short experimentNumber = Short.parseShort(args[4]);

      @SuppressWarnings({"unchecked"})
      StopWatch timer = new StopWatch();
      PacketSource pkt = new PacketSource(mean, numSources, experimentNumber);
      PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
      PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);

      SerialPacketWorker workerData = new SerialPacketWorker(done, pkt, uniformFlag, numSources);
      Thread workerThread = new Thread(workerData);

      workerThread.start();
      timer.startTimer();
      try {
        Thread.sleep(numMilliseconds);
      } catch (InterruptedException ignore) {
        ;
      }
      done.value = true;
      memFence.value = true;  // memFence is a 'volatile' forcing a memory fence
      try {                   // which means that done.value is visible to the workers
        workerThread.join();
      } catch (InterruptedException ignore) {
        ;
      }
      timer.stopTimer();
      final long totalCount = workerData.totalPackets;
      System.out.println("count:\t" + totalCount);
      System.out.println("time:\t" + timer.getElapsedTime());
      System.out.println("thrpt:\t" + totalCount / timer.getElapsedTime() + " pkts / ms");
    } catch (ArrayIndexOutOfBoundsException e) {
      System.out.println("ERROR: java SerialPacket [numMilliseconds] [numSources] [mean] [uniformFlag] [experimentNumber]");
    }
  }
}

class ParallelPacket {
  public static void main(String[] args) {
    try {
      final int numMilliseconds = Integer.parseInt(args[0]);
      final int numSources = Integer.parseInt(args[1]);
      final long mean = Long.parseLong(args[2]);
      final boolean uniformFlag = Boolean.parseBoolean(args[3]);
      final short experimentNumber = Short.parseShort(args[4]);
      final int queueDepth = Integer.parseInt(args[5]);
      final int lockType = Integer.parseInt(args[6]);
      final short strategy = Short.parseShort(args[7]);

      @SuppressWarnings({"unchecked"})

      // Allocate and initialize your Lamport queues
              List<WaitFreeQueue<Packet>> queues = new ArrayList<>();
      for (int i = 0; i < numSources; i++) {
        queues.add(new WaitFreeQueue<Packet>(queueDepth));
      }

      StopWatch timer = new StopWatch();
      PacketSource pkt = new PacketSource(mean, numSources, experimentNumber);

      // Allocate and initialize locks and any signals used to marshal threads (eg. done signals)
      PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
      PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);
      LockAllocator lockAllocator = new LockAllocator();
      List<Lock> locks = new ArrayList<>();
      for (int i = 0; i < numSources; i++) {
        locks.add(lockAllocator.getLock(lockType));
      }

      // Allocate and initialize Dispatcher and Worker threads
      PacketWorker dispatchData = new Dispatcher(done, queues, pkt, numSources, uniformFlag, queueDepth);
      Thread dispatchThread = new Thread(dispatchData);
      List<Thread> workerThreads = new ArrayList<>();
      for (int i = 0; i < numSources; i++) {
        PacketWorker workerData = new ParallelPacketWorker(i, done, queues, locks, numSources, strategy);
        Thread workerThread = new Thread(workerData);
        workerThreads.add(workerThread);
      }

      // call .start() on your Workers
      for (Thread worker : workerThreads)
        worker.start();

      timer.startTimer();

      // call .start() on your Dispatcher
      dispatchThread.start();

      try {
        Thread.sleep(numMilliseconds);
      } catch (InterruptedException ignore) {
        ;
      }

      // assert signals to stop Dispatcher - remember, Dispatcher needs to deliver an
      // equal number of packets from each source
      //
      // call .join() on Dispatcher
      //
      // assert signals to stop Workers - they are responsible for leaving the queues
      // empty - use whatever protocol you like, but one easy one is to have each
      // worker verify that it's corresponding queue is empty after it observes the
      // done signal set to true
      //
      // call .join() for each Worker
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
      final long totalCount = dispatchData.totalPackets;
      System.out.println("count:\t" + totalCount);
      System.out.println("time:\t" + timer.getElapsedTime());
      System.out.println("thrpt:\t" + totalCount / timer.getElapsedTime() + " pkts / ms");
    } catch (ArrayIndexOutOfBoundsException e) {
      System.out.println("ERROR: java ParallelPacket [numMilliseconds] [numSources] [mean] [uniformFlag] " +
                         "[experimentNumber] [queueDepth] [lockType] [strategy]");
      System.out.println("Lock Types: 0=TAS, 1=Backoff, 2=ReentrantWrapper, 4=CLH, 5=MCS");
      System.out.println("Strategy: 0=LockFree, 1=HomeQueue, 2=RandomQueue, 4=LastQueue");
    }
  }
}