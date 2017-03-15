/**
 * An immutable pair of integers: a value and a sequence number (aka timestamp).
 */
public class Register {
    /**
     * The value of the register.
     */
    public final int val;
    /**
     * The sequence number (aka timestamp) of the register.
     */
    public final int seq;
    
    /**
     * Constructs a Register with zeros.
     */
    public Register() {
        val = 0;
        seq = 0;
    }
    /**
     * Constructs a Register with the given value and sequence number.
     */
    public Register(int v, int s) {
        val = v;
        seq = s;
    }
}
