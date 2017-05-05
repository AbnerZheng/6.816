package pset6;

public interface HashTable<T> {
    public void add(int key, T x);
    public boolean remove(int key);
    public boolean contains(int key);
    public T get(int key);
}
