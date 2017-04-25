class Node<T> {
    public final int key;
    public T val;
    public int k;
    public boolean deleted;

    public Node(int key, T val) {
        this.key = key;
        this.val = val;
        this.k = 0;
        this.deleted = false;
    }

    public Node(int key, T val, int k) {
        this.key = key;
        this.val = val;
        this.k = k;
        this.deleted = false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (obj instanceof Integer) {
            return !deleted && ((int)obj == this.key);
        }
        if (!(obj instanceof Node)) {
            return false;
        }
        Node<T> node = (Node<T>) obj;
        return !deleted && (this.key == node.key);
    }
}
