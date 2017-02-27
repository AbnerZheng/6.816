import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

/*
 * The BreadBox interface defines methods that are present in all BreadBox
 * objects. The default state of a BreadBox is empty.
 */
interface BreadBox {
   /*
    * Returns whether or not there is a loaf of bread in the breadbox. The
    * default state is empty.
    *
    * @return
    *            True if there is a loaf, false if it is still empty.
    */
    public boolean isEmpty();
    
   /*
    * This method adds a loaf of bread to an empty breadbox.
    * 
    * If this method is called on a non-empty breadbox, an exception is thrown.
    */
    public void addLoaf();
}

/*
 * The BreadHouse class represents the house where Alex and Sam live.
 * 
 * When Alex arrives at home, method alex() is called. When Sam arrives at home,
 * method alex() is called.
 */
public class BreadHouse {
   /*
    * TODO: Add fields to keep track of kitchen table notes here!
    * All fields accessed by both threads must be Atomic classes from
    * java.util.concurrent.atomic
    */
    private AtomicBoolean exampleField;
    
    public BreadHouse() {
        // TODO: Implement me!
        exampleField = new AtomicBoolean(false);
    }
    
   /*
    * Executes Alex's strategy.
    *
    * This method is called at most once, when Alex arrives at home from work.
    * It may run concurrently with sam(), which will get the same,
    * initially-empty BreadBox, or alex() may run before sam(), after sam(),
    * or without sam() ever running.
    * When this method returns, box should not be empty.
    *
    * @param box
    *            A reference to the breadbox which must be refilled.
    */
    public void alex(BreadBox box) {
        // TODO: Implement me!
	}
	
   /*
    * Executes Sam's strategy.
    *
    * This method is called at most once, when Sam arrives at home from work.
    * It may run concurrently with alex(), which will get the same,
    * initially-empty BreadBox, or sam() may run before alex(), after alex(),
    * or without alex() ever running.
    * When this method returns, box should not be empty.
    * 
    * @param box
    *            A reference to the breadbox which must be refilled.
    */
    public void sam(BreadBox box) {
        // TODO: Implement me!
	}
}
