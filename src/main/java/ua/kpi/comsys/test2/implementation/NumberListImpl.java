/*
 * Copyright (c) 2014, NTUU KPI, Computer systems department and/or its affiliates. All rights reserved.
 * NTUU KPI PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 */
package ua.kpi.comsys.test2.implementation;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import ua.kpi.comsys.test2.NumberList;
import ua.kpi.comsys.test2.implementation.Node;

public class NumberListImpl implements NumberList {

    private Node head;
    private Node tail;
    private int size;
    private int modCount;


    private final int base;

    /** Default constructor. Returns empty NumberListImpl (represents 0). */
    public NumberListImpl() {
        this.base = 16;
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    /** Internal constructor to build list with a custom base (used by changeScale). */
    private NumberListImpl(int base) {
        this.base = base;
    }

    /**
     * Constructs new NumberListImpl by DECIMAL number from file (string).
     */
    public NumberListImpl(File file) {
        this.base = 16;
        String s = readAll(file).trim();
        if (s.isEmpty()) {
            // empty -> 0
            return;
        }
        BigInteger v = new BigInteger(s);
        if (v.signum() < 0) throw new IllegalArgumentException("Only non-negative numbers supported");
        fromBigInteger(v, this.base);
    }

    /**
     * Constructs new NumberListImpl by DECIMAL number in string notation.
     */
    public NumberListImpl(String value) {
        this.base = 16;
        String s = (value == null ? "" : value.trim());
        if (s.isEmpty()) return;

        try {
            BigInteger v = new BigInteger(s);
            if (v.signum() < 0) return;
            fromBigInteger(v, this.base);
        } catch (NumberFormatException e) {

        }
    }

    /** Saves number into file in DECIMAL representation. */
    public void saveList(File file) {
        Objects.requireNonNull(file, "file");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            w.write(toDecimalString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Returns student's record book number (4 digits). Here: group list number = 0004 -> int 4. */
    public static int getRecordBookNumber() {
        return 4;
    }

    /**
     * Converts this number to additional base (C5 extra): base 2.
     * Does not modify original list.
     */
    public NumberListImpl changeScale() {
        BigInteger v = toBigInteger();
        NumberListImpl out = new NumberListImpl(2);
        out.fromBigInteger(v, 2);
        return out;
    }

    /**
     * Additional operation (C7=4): remainder (mod) of two numbers.
     * Does not modify operands.
     */
    public NumberListImpl additionalOperation(NumberList arg) {
        Objects.requireNonNull(arg, "arg");
        BigInteger a = this.toBigInteger();
        BigInteger b = toBigInteger(arg, detectBase(arg));
        if (b.equals(BigInteger.ZERO)) throw new ArithmeticException("Division by zero");
        BigInteger r = a.mod(b);

        NumberListImpl out = new NumberListImpl(this.base);
        out.fromBigInteger(r, this.base);
        return out;
    }

    /** Decimal string of the stored number (independent from internal base). */
    public String toDecimalString() {
        return toBigInteger().toString(10);
    }

    /** String in CURRENT internal base (16 for main lists, 2 for changeScale result). */
    @Override
    public String toString() {
        if (size == 0) return "0";
        StringBuilder sb = new StringBuilder();
        Node cur = head;
        for (int i = 0; i < size; i++) {
            int d = cur.value & 0xFF;
            sb.append(digitToChar(d));
            cur = cur.next;
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof List<?> other)) return false;
        if (this.size != other.size()) return false;
        Iterator<?> it = other.iterator();
        Node cur = head;
        for (int i = 0; i < size; i++) {
            Object x = it.next();
            if (!(x instanceof Byte)) return false;
            if (!Objects.equals(cur.value, x)) return false;
            cur = cur.next;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 1;
        Node cur = head;
        for (int i = 0; i < size; i++) {
            h = 31 * h + (cur.value);
            cur = cur.next;
        }
        return h;
    }

    @Override
    public int size() { return size; }

    @Override
    public boolean isEmpty() { return size == 0; }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Byte)) return false;
        byte v = (Byte) o;
        Node cur = head;
        for (int i = 0; i < size; i++) {
            if (cur.value == v) return true;
            cur = cur.next;
        }
        return false;
    }

    @Override
    public Iterator<Byte> iterator() {
        // simple forward iterator without remove (ok for typical tests)
        return new Iterator<>() {
            private final int expected = modCount;
            private int seen = 0;
            private Node cur = head;

            @Override public boolean hasNext() { return seen < size; }

            @Override public Byte next() {
                if (expected != modCount) throw new java.util.ConcurrentModificationException();
                if (!hasNext()) throw new java.util.NoSuchElementException();
                byte v = cur.value;
                cur = cur.next;
                seen++;
                return v;
            }
        };
    }

    @Override
    public Object[] toArray() {
        Object[] arr = new Object[size];
        Node cur = head;
        for (int i = 0; i < size; i++) {
            arr[i] = cur.value;
            cur = cur.next;
        }
        return arr;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("toArray(T[] a) must not be implemented");
    }

    @Override
    public boolean add(Byte e) {
        requireNonNullDigit(e);
        append(e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof Byte)) return false;
        if (size == 0) return false;
        byte target = (Byte) o;

        Node prev = tail;
        Node cur = head;
        for (int i = 0; i < size; i++) {
            if (cur.value == target) {
                unlink(prev, cur);
                return true;
            }
            prev = cur;
            cur = cur.next;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        Objects.requireNonNull(c, "c");
        for (Object x : c) if (!contains(x)) return false;
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Byte> c) {
        Objects.requireNonNull(c, "c");
        boolean changed = false;
        for (Byte b : c) { add(b); changed = true; }
        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Byte> c) {
        Objects.requireNonNull(c, "c");
        rangeCheckForAdd(index);
        boolean changed = false;
        int i = index;
        for (Byte b : c) {
            add(i++, b);
            changed = true;
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c, "c");
        boolean changed = false;
        for (Object x : c) while (remove(x)) changed = true;
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c, "c");
        boolean changed = false;
        if (size == 0) return false;

        Node prev = tail;
        Node cur = head;
        int original = size;
        for (int i = 0; i < original; i++) {
            Node next = cur.next;
            if (!c.contains(cur.value)) {
                unlink(prev, cur);
                changed = true;
            } else {
                prev = cur;
            }
            cur = next;
            if (size == 0) break;
        }
        return changed;
    }

    @Override
    public void clear() {
        head = tail = null;
        size = 0;
        modCount++;
    }

    @Override
    public Byte get(int index) {
        rangeCheck(index);
        return nodeAt(index).value;
    }

    @Override
    public Byte set(int index, Byte element) {
        requireNonNullDigit(element);
        rangeCheck(index);
        Node n = nodeAt(index);
        byte old = n.value;
        n.value = element;
        modCount++;
        return old;
    }

    @Override
    public void add(int index, Byte element) {
        requireNonNullDigit(element);
        rangeCheckForAdd(index);

        if (index == size) { append(element); return; }
        if (index == 0) { prepend(element); return; }

        Node prev = nodeAt(index - 1);
        Node cur = prev.next;
        Node n = new Node(element);
        prev.next = n;
        n.next = cur;
        size++;
        modCount++;
    }

    @Override
    public Byte remove(int index) {
        rangeCheck(index);
        if (size == 1) {
            byte v = head.value;
            clear();
            return v;
        }
        if (index == 0) {
            byte v = head.value;
            head = head.next;
            tail.next = head;
            size--;
            modCount++;
            return v;
        }
        Node prev = nodeAt(index - 1);
        Node cur = prev.next;
        byte v = cur.value;
        unlink(prev, cur);
        return v;
    }

    @Override
    public int indexOf(Object o) {
        if (!(o instanceof Byte)) return -1;
        byte v = (Byte) o;
        Node cur = head;
        for (int i = 0; i < size; i++) {
            if (cur.value == v) return i;
            cur = cur.next;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (!(o instanceof Byte)) return -1;
        byte v = (Byte) o;
        int last = -1;
        Node cur = head;
        for (int i = 0; i < size; i++) {
            if (cur.value == v) last = i;
            cur = cur.next;
        }
        return last;
    }

    @Override
    public ListIterator<Byte> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<Byte> listIterator(int index) {
        rangeCheckForAdd(index);
        return new ListIterator<>() {
            int cursor = index;
            int lastRet = -1;

            @Override public boolean hasNext() { return cursor < size; }
            @Override public Byte next() { lastRet = cursor; return get(cursor++); }
            @Override public boolean hasPrevious() { return cursor > 0; }
            @Override public Byte previous() { lastRet = --cursor; return get(cursor); }
            @Override public int nextIndex() { return cursor; }
            @Override public int previousIndex() { return cursor - 1; }
            @Override public void remove() {
                if (lastRet < 0) throw new IllegalStateException();
                NumberListImpl.this.remove(lastRet);
                if (lastRet < cursor) cursor--;
                lastRet = -1;
            }
            @Override public void set(Byte e) {
                if (lastRet < 0) throw new IllegalStateException();
                NumberListImpl.this.set(lastRet, e);
            }
            @Override public void add(Byte e) {
                NumberListImpl.this.add(cursor++, e);
                lastRet = -1;
            }
        };
    }

    @Override
    public List<Byte> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > size || fromIndex > toIndex)
            throw new IndexOutOfBoundsException();
        NumberListImpl out = new NumberListImpl(this.base);
        Node cur = nodeAt(fromIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            out.add(cur.value);
            cur = cur.next;
        }
        return out;
    }

    @Override
    public boolean swap(int index1, int index2) {
        if (index1 == index2) return true;
        if (index1 < 0 || index2 < 0 || index1 >= size || index2 >= size) return false;
        Node a = nodeAt(index1);
        Node b = nodeAt(index2);
        byte tmp = a.value;
        a.value = b.value;
        b.value = tmp;
        modCount++;
        return true;
    }

    @Override
    public void sortAscending() {
        countingSort(true);
    }

    @Override
    public void sortDescending() {
        countingSort(false);
    }

    @Override
    public void shiftLeft() {
        if (size <= 1) return;
        head = head.next;
        tail = tail.next;
        modCount++;
    }

    @Override
    public void shiftRight() {
        if (size <= 1) return;
        Node prev = nodeAt(size - 2);
        tail = prev;
        head = tail.next;
        modCount++;
    }

    private void requireNonNullDigit(Byte e) {
        Objects.requireNonNull(e, "Null elements are not permitted");
        int d = e & 0xFF;
        if (d < 0 || d >= base) {
            throw new IllegalArgumentException("Digit out of range for base " + base + ": " + d);
        }
    }

    private void append(byte v) {
        Node n = new Node(v);
        if (size == 0) {
            head = tail = n;
            n.next = n;
        } else {
            n.next = head;
            tail.next = n;
            tail = n;
        }
        size++;
        modCount++;
    }

    private void prepend(byte v) {
        Node n = new Node(v);
        if (size == 0) {
            head = tail = n;
            n.next = n;
        } else {
            n.next = head;
            tail.next = n;
            head = n;
        }
        size++;
        modCount++;
    }

    private void unlink(Node prev, Node cur) {
        if (size == 1) {
            clear();
            return;
        }
        if (cur == head) head = head.next;
        if (cur == tail) tail = prev;
        prev.next = cur.next;
        tail.next = head;
        size--;
        modCount++;
    }

    private Node nodeAt(int index) {
        rangeCheck(index);
        Node cur = head;
        for (int i = 0; i < index; i++) cur = cur.next;
        return cur;
    }

    private void rangeCheck(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("index=" + index);
    }

    private void rangeCheckForAdd(int index) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException("index=" + index);
    }

    private void countingSort(boolean asc) {
        if (size <= 1) return;
        int[] cnt = new int[base];
        Node cur = head;
        for (int i = 0; i < size; i++) {
            cnt[cur.value & 0xFF]++;
            cur = cur.next;
        }
        cur = head;
        if (asc) {
            for (int d = 0; d < base; d++) {
                for (int k = 0; k < cnt[d]; k++) {
                    cur.value = (byte) d;
                    cur = cur.next;
                }
            }
        } else {
            for (int d = base - 1; d >= 0; d--) {
                for (int k = 0; k < cnt[d]; k++) {
                    cur.value = (byte) d;
                    cur = cur.next;
                }
            }
        }
        modCount++;
    }

    private char digitToChar(int d) {
        if (d >= 0 && d <= 9) return (char) ('0' + d);
        return (char) ('A' + (d - 10));
    }

    private BigInteger toBigInteger() {
        return toBigInteger(this, this.base);
    }

    private static BigInteger toBigInteger(NumberList list, int base) {
        if (list.size() == 0) return BigInteger.ZERO;
        BigInteger b = BigInteger.valueOf(base);
        BigInteger v = BigInteger.ZERO;
        for (Byte x : list) {
            int d = x & 0xFF;
            v = v.multiply(b).add(BigInteger.valueOf(d));
        }
        return v;
    }

    private void fromBigInteger(BigInteger v, int base) {
        clear();
        if (v.equals(BigInteger.ZERO)) return;

        BigInteger b = BigInteger.valueOf(base);

        int cap = Math.max(1, v.bitLength() / 2 + 4);
        byte[] tmp = new byte[cap];
        int len = 0;

        BigInteger cur = v;
        while (cur.signum() > 0) {
            BigInteger[] qr = cur.divideAndRemainder(b);
            int digit = qr[1].intValue();
            if (len == tmp.length) {
                byte[] n = new byte[tmp.length * 2];
                System.arraycopy(tmp, 0, n, 0, tmp.length);
                tmp = n;
            }
            tmp[len++] = (byte) digit;
            cur = qr[0];
        }


        for (int i = len - 1; i >= 0; i--) append(tmp[i]);
    }

    /**
     * Detect base of other NumberList if it is our implementation;
     * otherwise assume 16 (main assignment base).
     */
    private static int detectBase(NumberList arg) {
        if (arg instanceof NumberListImpl n) return n.base;
        return 16;
    }

    private static String readAll(File file) {
        try (InputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int r;
            while ((r = in.read(buf)) != -1) bout.write(buf, 0, r);
            return bout.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}


