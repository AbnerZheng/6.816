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
    * All fields accessed by both threads must be Atomic classes from
    * java.util.concurrent.atomic
    */
    private AtomicBoolean A1;
    private AtomicBoolean A2;
    private AtomicBoolean S1;
    private AtomicBoolean S2;
    
    public BreadHouse() {
        A1 = new AtomicBoolean(false);
        A2 = new AtomicBoolean(false);
        S1 = new AtomicBoolean(false);
        S2 = new AtomicBoolean(false);
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
        // When I get home, leave a note A1 saying "Alex came home".
        A1.set(true);

        // If I do see Sam’s note S2, leave a note A2 saying ”Alex avocado!”
        // else remove any A2 notes.
        if (S2.get()) {
            A2.set(true);
        } else {
            A2.set(false);
        }

        // Wait at the table as long as S1 is present and either both A2 and
        // S2 are present or both are not.
        while (S1.get() && !(A2.get() ^ S2.get())) {};

        // If I see that there is no bread, go buy a new loaf of bread.
        if (box.isEmpty()) {
            box.addLoaf();
        }

        // Remove my note A1.
        A1.set(false);
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
        // When I get home, leave a note S1 saying ”Sam came home”.
        S1.set(true);

        // If I don’t see Alex’s note A2, leave a note S2 saying ”Sam salami!”
        // else remove any S2 notes.
        if (!A2.get()) {
            S2.set(true);
        } else {
            S2.set(false);
        }

        // Wait at the table as long as A1 is present and exactly one of A2
        // or S2 are present.
        while (A1.get() && (A2.get() ^ S2.get())) {};

        // If I see that there is no bread, go buy a new loaf of bread.
        if (box.isEmpty()) {
            box.addLoaf();
        }

        // Remove my note S1.
        S1.set(false);
	}
}
