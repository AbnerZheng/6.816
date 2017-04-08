//import java.util.lang.*;
// This application launches a single worker who implements a counter with no locks
class SerialCounter {
  public static void main(String[] args) {
    try {
      final int numMilliseconds = Integer.parseInt(args[0]);

      StopWatch timer = new StopWatch();
      PaddedPrimitive<CounterStruct> counter = new PaddedPrimitive<CounterStruct>(new CounterStruct());
      PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
      Thread workerThread = new Thread(new SoloCounterWorker(counter,done), new String("SoloWorker"));
      workerThread.start();
      timer.startTimer();
      try {
        Thread.sleep(numMilliseconds);
      } catch (InterruptedException ignore) {;}
      done.value = true;
      timer.stopTimer();
      final long totalCount = counter.value.counter;
      try {
        workerThread.join();
      } catch (InterruptedException ignore) {;}
      System.out.println("SerialCounter");
      System.out.println("count:\t" + totalCount);
      System.out.println("time:\t" + timer.getElapsedTime());
      System.out.println("thrpt:\t" + totalCount/timer.getElapsedTime() + " inc / ms");
    } catch (ArrayIndexOutOfBoundsException e) {
      System.out.println("ERROR: java SerialCounter [numMilliseconds]");
    }
  }  
}

// This application launches numThreads workers who try to lock the counter and increment it
class ParallelCounter {
  public static void main(String[] args) {
    try {
      final int numMilliseconds = Integer.parseInt(args[0]);
      final int numThreads = Integer.parseInt(args[1]);
      final int lockType = Integer.parseInt(args[2]);

      PaddedPrimitive<CounterStruct> counter = new PaddedPrimitive<CounterStruct>(new CounterStruct());
      PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
      StopWatch timer = new StopWatch();
      Lock lock;

      LockAllocator la = new LockAllocator();
      if (lockType == 1 && args.length >= 5) {
        // Backoff lock with custom min and max delay
        lock = new BackoffLock(Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        BackoffLock tmp = (BackoffLock)lock;
        System.out.println("BackoffLock minDelay=" + tmp.MIN_DELAY + " maxDelay=" + tmp.MAX_DELAY);
      } else {
        lock = la.getLock(lockType);
        la.printLockType(lockType);
      }

      lock.lock(); // I'll grab the lock and then later unlock as I release the workers

      Thread[] workerThread = new Thread[numThreads];
      CounterWorker[] workerData = new CounterWorker[numThreads];

      for (int i = 0; i < numThreads; i++) {
        workerData[i] = new CounterWorker(counter, done, lock);
        workerThread[i] = new Thread(workerData[i], new String("Worker" + i));
      }

      for (int i = 0; i < numThreads; i++)
        workerThread[i].start();

      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        ;
      }

      lock.unlock(); // release the hounds
      timer.startTimer();
      try {
        Thread.sleep(numMilliseconds); // wait for a while
      } catch (InterruptedException e) {
        ;
      }
      lock.lock(); // stop the madness
      timer.stopTimer(); // measure the throughput...
      done.value = true;
      final long totalCount = counter.value.counter;
      System.out.println("count:\t" + totalCount);
      System.out.println("time:\t" + timer.getElapsedTime());
      System.out.println("thrpt:\t" + totalCount / timer.getElapsedTime() + " inc / ms");

      lock.unlock(); // give the workers a chance to see done.value == true

      long[] count = new long[numThreads];
      for (int i = 0; i < numThreads; i++) {
        try {
          workerThread[i].join();
          count[i] = workerData[i].count; // collect their independent counts
          //System.out.println(count[i]);
        } catch (InterruptedException ignore) {
          ;
        }
      }
      //System.out.println(counter.value.counter);
      System.out.println("stdev:\t" + Statistics.getStdDev(count));
    } catch (ArrayIndexOutOfBoundsException e) {
      System.out.println("ERROR: java ParallelCounter [numMilliseconds] [numThreads] [lockType]");
      System.out.println("Lock Types: 0=TAS, 1=Backoff, 2=ReentrantWrapper, 4=CLH, 5=MCS");
    }
  }
}

/**
 * Try minDelay from 10^1 to 10^8 and maxDelay from 10^1 to 10^8
 * Total 36 trials - identify the parameters with the maximum throughput
 */
class LockScalingTest {
  public static void main(String[] args) {
    try {
      String[] args2 = new String[]{args[0], args[1], "1", "10", "10"};
      for (int i = 1; i <= 8; i++) {
        int minDelay = (int)Math.pow(10, i);
        for (int j = i; j <= 8; j++) {
          int maxDelay = (int)Math.pow(10, j);
          args2[3] = String.valueOf(minDelay);
          args2[4] = String.valueOf(maxDelay);
          ParallelCounter.main(args2);
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      System.out.println("ERROR: java LockScalingTest [numMilliseconds] [numThreads]");
    }
  }
}