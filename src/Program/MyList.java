package Program;

//Interface to mimic ArrayList ticket class
public interface MyList<E> extends java.lang.Iterable<E> {
    void add(E e);
    //Return ticket at index
    E    get(int index);
    int  size();
    //Return true if target in list
    boolean contains(E target);

}