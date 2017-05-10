package pset6;

import java.util.concurrent.atomic.*;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public interface Lock {
  public void lock();
  public void unlock();
  public boolean isContended();
}

class TASLock implements Lock {
  AtomicBoolean state = new AtomicBoolean(false);
  public void lock() {
    while( state.getAndSet(true) ) {;} // keep trying to get the lock
  }
  public void unlock() {
    state.set(false);
  }

  /**
   * Checks if the calling thread observes another thread concurrently
   * calling lock(), in the critical section, or calling unlock().
   *
   * @return
   *          true if another thread is present, else false
   */
  public boolean isContended() {
    return state.get();
  }
}

class Backoff { // helper class for the BackoffLock
  volatile int tmp = 100;
  final int minDelay, maxDelay;
  int limit;
  int seed;
  public Backoff(int minDelay, int maxDelay) {
    this.minDelay = minDelay;
    this.maxDelay = maxDelay;
    this.limit = minDelay;
  }
  public void backoff() throws InterruptedException {
    for( int i = 0; i < limit; i++ ) { // simple 'local' delay - replace if you like...
      tmp += i;
    }
    if( 2*limit <= maxDelay )
      limit = 2*limit;
  }
}

class BackoffLock implements Lock {
  private AtomicBoolean state = new AtomicBoolean(false);
  int MIN_DELAY = (int)Math.pow(10, 6); // You should tune these parameters...
  int MAX_DELAY = (int)Math.pow(10, 7);

  public BackoffLock() {};

  public BackoffLock(int minDelay, int maxDelay) {
    assert minDelay <= maxDelay;
    MIN_DELAY = minDelay;
    MAX_DELAY = maxDelay;
  }

  public void lock() {
    while(state.get()) {;} // try to get the lock once before allocating a new Backoff(...)
    if(!state.getAndSet(true)) {
      return;
    } else {
      Backoff backoff = new Backoff(MIN_DELAY,MAX_DELAY);
      try {
        backoff.backoff();
      } catch (InterruptedException ignore) {;}
      while(true) {
        while(state.get()) {;}
        if(!state.getAndSet(true)) {
          return;
        } else {
          try {
            backoff.backoff();
          } catch (InterruptedException ignore) {;}
        }
      }
    }
  }
  public void unlock() {
    state.set(false);
  }
  /**
   * Checks if the calling thread observes another thread concurrently
   * calling lock(), in the critical section, or calling unlock().
   *
   * @return
   *          true if another thread is present, else false
   */
  public boolean isContended() {
    return state.get();
  }
}

class ReentrantWrapperLock implements Lock {
  private final ReentrantLock lock;
  public ReentrantWrapperLock() {
    lock = new ReentrantLock(false); // we don't care about fairness
  }
  public void lock() {
    lock.lock();
  }
  public void unlock() {
    lock.unlock();
  }
  /**
   * Checks if the calling thread observes another thread concurrently
   * calling lock(), in the critical section, or calling unlock().
   *
   * @return
   *          true if another thread is present, else false
   */
  public boolean isContended() {
    return lock.isLocked();
  }
}

class CLHLock implements Lock {

  private final AtomicReference<QNode> tail;
  private final ThreadLocal<QNode> myPred;
  private final ThreadLocal<QNode> myNode;

  public CLHLock() {
    tail = new AtomicReference<QNode>(new QNode());
    myNode = new ThreadLocal<QNode>() {
      protected QNode initialValue() {
        return new QNode();
      }
    };
    myPred = new ThreadLocal<QNode>() {
      protected QNode initialValue() {
        return null;
      }
    };
  }

  public void lock() {
    QNode qnode = myNode.get();
    qnode.locked.set(true);
    QNode pred = tail.getAndSet(qnode);
    myPred.set(pred);
    while (pred.locked.get()) {}
  }

  public void unlock() {
    QNode qnode = myNode.get();
    qnode.locked.set(false);
    myNode.set(myPred.get());
  }

  /**
   * Checks if the calling thread observes another thread concurrently
   * calling lock(), in the critical section, or calling unlock().
   *
   * @return
   *          true if another thread is present, else false
   */
  public boolean isContended() {
    return myNode.get().locked.get();
  }

  class QNode {
    public AtomicBoolean locked = new AtomicBoolean(false);
  }
}

class MCSLock implements Lock {

  AtomicReference<QNode> tail;
  ThreadLocal<QNode> myNode;

  public MCSLock() {
    tail = new AtomicReference<QNode>(null);
    myNode = new ThreadLocal<QNode>() {
      protected QNode initialValue() {
        return new QNode();
      }
    };
  }

  public void lock() {
    QNode qnode = myNode.get();
    QNode pred = tail.getAndSet(qnode);
    if (pred != null) {
      qnode.locked = true;
      pred.next = qnode;
      // wait until predecessor gives up the lock
      while (qnode.locked) {}
    }
  }

  public void unlock() {
    QNode qnode = myNode.get();
    if (qnode.next == null) {
      if (tail.compareAndSet(qnode, null))
        return;
      // wait until successor fills in its next field
      while (qnode.next == null) {}
    }
    qnode.next.locked = false;
    qnode.next = null;
  }

  /**
   * Checks if the calling thread observes another thread concurrently
   * calling lock(), in the critical section, or calling unlock().
   *
   * @return
   *          true if another thread is present, else false
   */
  public boolean isContended() {
    return tail.get() != null;
  }

  class QNode {
    volatile boolean locked = false;
    volatile QNode next = null;
  }
}

class LockAllocator {
  public static Lock getLock(int lockType) {
    return getLock(lockType, 128);
  }
  
  public static Lock getLock(int lockType, int maxThreads) {
    Lock lock;
    if( lockType == 0 ) {
      lock = new TASLock();
    }
    else if( lockType == 1 ) {
      lock = new BackoffLock();
    }
    else if( lockType == 2 ) {
      lock = new ReentrantWrapperLock();
    }
    else if( lockType == 3 ) {
      lock = new CLHLock();           // You need to write these...
    }
    else if( lockType == 4 ) {
      lock = new MCSLock();           // You need to write these...
    }
    else {
      System.out.println("This is not a valid lockType:");
      System.out.println(lockType);
      lock = new ReentrantWrapperLock();
    }
    return lock;
  }

  public void printLockType(int lockType) {
    if( lockType == 0 ) {
      System.out.println("TASLock");
    }
    else if( lockType == 1 ) {
      System.out.println("BackoffLock");
    }
    else if( lockType == 2 ) {
      System.out.println("ReentrantLock");
    }
    else if( lockType == 3 ) {
      System.out.println("CLHLock");
    }
    else if( lockType == 4 ) {
      System.out.println("MCSLock");
    }
    else {
      System.out.println("This is not a valid lockType:");
      System.out.println(lockType);
    }
  }
  public void printLockTypes() {
    for( int i = 0; i < 5; i++ ) {
      System.out.println(i + ":");
      printLockType(i);
    }
  }
}
