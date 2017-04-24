class Node<T> {
    public final int key;
    public T val;
    public int k;

    public Node(int key, T val) {
        this.key = key;
        this.val = val;
        this.k = 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (obj instanceof Integer) {
            return (int)obj == this.key;
        }
        if (!(obj instanceof Node)) {
            return false;
        }
        Node<T> node = (Node<T>) obj;
        return this.key == node.key;
    }
}
