package pset6;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;

public interface BucketList<T,K> {
  public boolean contains(K key);
  public boolean remove(K key);
  public T get(K key);
  public void add(K key, T item);
  public int getSize();
  public abstract class Iterator {
    public abstract boolean hasNext();
    public abstract Iterator getNext();
  }
}


class Window<T> {
  public AtomicNode<T> pred, curr;
  public Window(AtomicNode<T> pred, AtomicNode<T> curr) {
    this.pred = pred;
    this.curr = curr;
  }
  public T getPred() {
    return pred.val;
  }
  public T getCurr() {
    return curr.val;
  }
  @Override
  public String toString() {
    return "PRED " + pred.val + ", CURR " + curr.val;
  }
}

class LockFreeList<T> {

  private static final int HI_MASK = 0x00800000;
  private static final int MASK = 0x00FFFFFF;
  AtomicNode<T> head;

  public LockFreeList() {
    this.head = new AtomicNode<T>(Integer.MIN_VALUE, null);  // head sentinel
    this.head.next.set(new AtomicNode<T>(Integer.MAX_VALUE, null), false);
  }

  public LockFreeList(AtomicNode<T> head) {
    this.head = head;
  }

  // key is included in the range [pred, succ)
  public Window<T> findInclusive(int key) {
    AtomicNode<T> pred = null, curr = null, succ = null;
    boolean[] marked = {false};
    boolean snip;
    key = makeOrdinaryKey(key);
    retry: while (true) {
      pred = head;
      curr = pred.next.getReference();
      while (true) {
        succ = curr.next.get(marked);
        while (marked[0]) {
          snip = pred.next.compareAndSet(curr, succ, false, false);
          if (!snip) continue retry;
          curr = succ;
          succ = curr.next.get(marked);
        }
        if (curr.key > key)
          return new Window<T>(pred, curr);
        pred = curr;
        curr = succ;
      }
    }
  }

  // key is included in the range (pred, succ]
  public Window<T> find(int key) {
    return find(head, makeOrdinaryKey(key));
  }

  public Window<T> find(AtomicNode<T> head, int key) {
    AtomicNode<T> pred = null, curr = null, succ = null;
    boolean[] marked = {false};
    boolean snip;
    retry: while (true) {
      pred = head;
      curr = pred.next.getReference();
      while (true) {
        succ = curr.next.get(marked);
        while (marked[0]) {
          snip = pred.next.compareAndSet(curr, succ, false, false);
          if (!snip) continue retry;
          curr = succ;
          succ = curr.next.get(marked);
        }
        if (curr.key >= key)
          return new Window<T>(pred, curr);
        pred = curr;
        curr = succ;
      }
    }
  }

  public boolean add(int index, T item) {
    int key = makeOrdinaryKey(index);
    while (true) {
      Window<T> window = find(head, key);
      AtomicNode<T> pred = window.pred, curr = window.curr;
      if (curr.key == key) {
        return false;
      } else {
        AtomicNode<T> node = new AtomicNode<T>(key, item);
        node.next.set(curr, false);
        if (pred.next.compareAndSet(curr, node, false, false)) {
          return true;
        }
      }
    }
  }

  public boolean remove(int index) {
    boolean snip;
    int key = makeOrdinaryKey(index);
    while (true) {
      Window<T> window = find(head, key);
      AtomicNode<T> pred = window.pred, curr = window.curr;
      if (curr.key != key) {
        return false;
      } else {
        AtomicNode<T> succ = curr.next.getReference();
        snip = curr.next.attemptMark(succ, true);
        if (!snip)
          continue;
        pred.next.compareAndSet(curr, succ, false, false);
        return true;
      }
    }
  }

  public boolean contains(int index) {
    int key = makeOrdinaryKey(index);
    Window<T> window = find(head, key);
    AtomicNode<T> pred = window.pred;
    AtomicNode<T> curr = window.curr;
    return curr.key == key;
  }

  public T get(int index) {
    int key = makeOrdinaryKey(index);
    Window<T> window = find(head, key);
    AtomicNode<T> pred = window.pred;
    AtomicNode<T> curr = window.curr;
    if (curr.key == key)
      return curr.val;
    else
      return null;
  }

  /**
   * Inserts a sentinel node for the given index, if it does not already exist,
   * and returns the corresponding lock-free list.
   * @param index the sentinel index
   * @return the lock-free list starting at the sentinel node of the given index
   */
  public LockFreeList<T> getSentinel(int index) {
    int key = makeSentinelKey(index);
    boolean splice;
    while (true) {
      Window<T> window = find(head, key);
      AtomicNode<T> pred = window.pred;
      AtomicNode<T> curr = window.curr;
      if (curr.key == key) {
        return new LockFreeList<T>(curr);
      } else {
        AtomicNode<T> node = new AtomicNode<T>(key, null);
        node.next.set(pred.next.getReference(), false);
        splice = pred.next.compareAndSet(curr, node, false, false);
        if (splice)
          return new LockFreeList<T>(node);
      }
    }
  }

  public static int makeOrdinaryKey(int key) {
    int code = key & MASK; // take 3 lowest bytes
    return Integer.reverse(code | HI_MASK);
  }

  public static int makeSentinelKey(int sentinelKey) {
    return Integer.reverse(sentinelKey & MASK);
  }
}

class SerialList<T,K> implements BucketList<T,K> {
  int size = 0;
  SerialList<T,K>.Iterator<T,K> head;

  public SerialList() {
    this.head = null;
    this.size = 0;
  }
  public SerialList(K key, T item) {
    this.head = new SerialList<T,K>.Iterator<T,K>(key,item,null);
    this.size = 1;
  }
  public Iterator<T,K> getHead() {
    return head;
  }
  public Iterator<T,K> getItem(K key) {
    SerialList<T,K>.Iterator<T,K> iterator = head;
    while( iterator != null ) {
      if( iterator.key.equals(key) )
        return iterator;
      else
        iterator = iterator.next;
    }
    return null;
  }
  public boolean contains(K key) {
    SerialList<T,K>.Iterator<T,K> iterator = getItem(key);
    if( iterator == null )
      return false;
    else
      return true;
  }
  public T get(K key) {
    return getItem(key).getItem();
  }
  @SuppressWarnings("unchecked")
  public boolean remove(K key) {
    if( contains(key) == false )
      return false;
    SerialList<T,K>.Iterator<T,K> iterator = head;
    if( iterator == null )
      return false;
    if( head.key.equals(key) ) {
      head = head.getNext();
      size--;
      return true;
    }
    while( iterator.hasNext() ) {
      if( iterator.getNext().key.equals(key) ) {
        iterator.setNext(iterator.getNext().getNext());
        size--;
        return true;
      }
      else
        iterator = iterator.getNext();
    }
    return false;
  }
  public void add(K key, T item) {
    SerialList<T,K>.Iterator<T,K> tmpItem = getItem(key);
    if( tmpItem != null ) {
      tmpItem.item = item; // we're overwriting, so the size stays the same
    }
    else {
      @SuppressWarnings("unchecked")      
      SerialList<T,K>.Iterator<T,K> firstItem = new SerialList<T,K>.Iterator<T,K>(key, item, head);
      head = firstItem;
      size++;
    }
  }
  public void addNoCheck(K key, T item) {
    SerialList<T,K>.Iterator<T,K> firstItem = new SerialList<T,K>.Iterator<T,K>(key, item, head);
    head = firstItem;
    size++;
  }
  public int getSize() {
    return size;
  }
  @SuppressWarnings("unchecked")
  public void printList() {
    SerialList<T,K>.Iterator<T,K> iterator = head;
    System.out.println("Size: " + size);
    while( iterator != null ) {
      System.out.println(iterator.getItem());
      iterator = iterator.getNext();
    }
  }
  public class Iterator<T,K> {
    @SuppressWarnings("unchecked")
    public final K key;
    private T item;
    private Iterator<T,K> next;
    public Iterator(K key, T item, Iterator<T,K> next) {
      this.key = key;
      this.item = item;
      this.next = next;
    }
    @SuppressWarnings("unchecked")
    public Iterator() {
      this.key = (K) new Object();
      this.item = (T) new Object();
      this.next = null;
    }
    public boolean hasNext() {
      return next != null;
    }
    @SuppressWarnings("unchecked")
    public Iterator getNext() {
      return next;
    }
    public void setNext(Iterator<T,K> next) {this.next = next; }
    public T getItem() { return item; }
    public void setItem(T item) { this.item = item; }
  }
}

class BucketListTest {
  public static void main(String[] args) {  
    SerialList<Long,Long> list = new SerialList<Long,Long>();
    for( long i = 0; i < 15; i++ ) {
      list.add(i,i*i);
      list.printList();
    }
    for( long i = 14; i > 0; i -= 2 ) {
      list.remove(i);
      list.printList();
    }
  }
}
