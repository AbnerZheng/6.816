import java.util.concurrent.atomic.*;

public class CLHLock implements Lock {

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
