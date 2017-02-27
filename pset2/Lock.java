import java.util.concurrent.atomic.AtomicIntegerArray;


/*
 * The Lock interface defines methods that need to
 * be present in all Lock objects. All Locks that 
 * inherit from this interface will need to implement
 * the existing methods.
 */
public interface Lock {

	/*
     * This method is called before a process enters the critical section. Once this
     * method returns, it signals that the calling process can safely enter the
     * critical section.
     *
     * @param processNum
     *            A unique integer that represents the ID of the calling process
     */
	public void lock(int processNum);

	/*
     * This method is called after a process leaves the critical section. Once this
     * method returns, it signals that the calling process has left the critical section
     * and other processes can contend to enter.
     *
     * @param processNum
     *            A unique integer that represents the ID of the calling process
     */
	public void unlock(int processNum);
}


/*
 * The One-Bit Lock is a locking algorithm that utilizes n bits to achieve 
 * mutual exclusion. It implements the Lock interface defined above.
 */
class OneBitLock implements Lock {

	/*
     * Flags is an AtomicIntegerArray that is used to track which processes are
     * contending to enter the critical section. We will use the value 1 to indicate
     * a process is interested in entering the critical section, and the value 0
     * to indicate it is not. All operations on this array are atomic and changes
     * will be seen across all threads.
     */
	private AtomicIntegerArray flags;

	/*
     * Constructor for the One-Bit Lock
     */
	public OneBitLock(int n){
		//TODO: Implement me!
	}

	/*
     * The Lock method for the One-Bit Lock. This method is called right before a process
     * enters the critical section. For the purpose of this assignment, only one process
     * can hold the lock at any give moment in time. Only the holder of the lock can safely
     * enter the critical section.
     */
	public void lock(int processNum){
		//TODO: Implement me!

	}

	/*
     * The Unlock method for the One-Bit Lock. This method is called right after a process
     * leaves the critical section. After this method is called, the lock is released and
     * free to be grabbed by other processes contending for it.
     */
	public void unlock(int processNum) {
		//TODO: Implement me!
	}

}
