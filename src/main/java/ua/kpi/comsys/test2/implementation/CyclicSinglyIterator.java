package ua.kpi.comsys.test2.implementation;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class CyclicSinglyIterator implements Iterator<Byte> {
    private final Node headSnapshot;
    private final int sizeSnapshot;
    private final ModCounter mod;
    private final int expectedMod;

    private Node current;
    private Node prev;
    private int seen = 0;
    private boolean canRemove = false;

    public interface ModCounter {
        int modCount();
        void removeCurrent(Node prev, Node current);
    }

    public CyclicSinglyIterator(Node head, int size, ModCounter modCounter) {
        this.headSnapshot = head;
        this.sizeSnapshot = size;
        this.mod = modCounter;
        this.expectedMod = modCounter.modCount();
        this.current = head;
        this.prev = null;
    }

    @Override
    public boolean hasNext() {
        return seen < sizeSnapshot;
    }

    @Override
    public Byte next() {
        if (expectedMod != mod.modCount()) throw new ConcurrentModificationException();
        if (!hasNext()) throw new NoSuchElementException();
        byte v = current.value;
        prev = current;
        current = current.next;
        seen++;
        canRemove = true;
        return v;
    }

    @Override
    public void remove() {
        if (expectedMod != mod.modCount()) throw new ConcurrentModificationException();
        if (!canRemove) throw new IllegalStateException("next() not called or already removed");
        mod.removeCurrent(null, null);
        throw new UnsupportedOperationException("Iterator.remove() is not supported in this implementation");
    }
}
