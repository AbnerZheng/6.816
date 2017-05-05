package pset6;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicMarkableReference;

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

class AtomicNode<T> {
    public final int key;
    public final T val;
    public final AtomicMarkableReference<AtomicNode<T>> next;

    public AtomicNode(int key, T val) {
        this.key = key;
        this.val = val;
        this.next = new AtomicMarkableReference<AtomicNode<T>>(null, false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (!(obj instanceof AtomicNode)) {
            return false;
        }
        AtomicNode<T> node = (AtomicNode<T>) obj;
        return !next.isMarked() && (this.key == node.key);
    }
}

class NodeTest {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        List<Node<Integer>> nodes = new ArrayList<Node<Integer>>();
        nodes.add(new Node(100, 5));
        nodes.add(new Node(200, 5));
        nodes.add(new Node(300, 5));
        System.out.println(nodes.contains(new Node(200, 10)));
        System.out.println(!nodes.contains(new Node(400, 5)));
        System.out.println((new Node(100, 5)).equals(new Node(100, 30)));
        System.out.println((new Node(100, 5)).equals(100));
    }
}