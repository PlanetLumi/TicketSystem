package Program;
//Replacement for List


import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;


public final class MyArrayList<E> implements MyList<E> {
    private static final int DEFAULT_CAP = 8;
    private E[] data;         // contiguous storage
    private int size;
    private int modCount;     // for fail-fast iterator


    @SuppressWarnings("unchecked")
    //Array holding elements
    public MyArrayList(int cap) {
        data = (E[]) new Object[Math.max(cap, DEFAULT_CAP)];
    }
    //assigns default
    public MyArrayList() { this(DEFAULT_CAP); }

    @Override public void add(E e) {
        //if too small for data grow reasonable
        if (size == data.length) grow();
        //Add to array
        data[size++] = e;
        //Track that array has been updated
        modCount++;
    }
    @Override public E get(int i) {
        //Go to index in array
        if (i < 0 || i >= size) throw new IndexOutOfBoundsException(i);
        return data[i];
    }
    @Override public int size() { return size; }
    @Override
    public boolean contains(E target) {

        if (target == null) {
            for (int i = 0; i < size; i++) {
                if (data[i] == null) return true;
            }
        } else {
            //Linear loop through each element in array until target found
            for (int i = 0; i < size; i++) {
                if (target.equals(data[i])) return true;
            }
        }
        return false;
    }
    //Iterator aware of concurrent changes
    @Override public java.util.Iterator<E> iterator() {
        return new java.util.Iterator<>() {
            //Find changes num
            private final int expectedMod = modCount;
            //Find current position
            private int cursor;
            //Check to see if at end of list
            @Override public boolean hasNext() { return cursor < size; }
            @Override public E next() {
                if (modCount != expectedMod)
                    throw new ConcurrentModificationException();
                if (cursor >= size) throw new NoSuchElementException();
                return data[cursor++];
            }
        };
    }

    @SuppressWarnings("unchecked")
    //Replace current array with larger array - memory friendly
    private void grow() {
        int newCap = data.length + (data.length >>> 1) + 1; //Grow by 1.5 x
        E[] bigger = (E[]) new Object[newCap]; //Set new size
        System.arraycopy(data, 0, bigger, 0, size); //Mirror into array
        data = bigger; //Replace
    }


}
