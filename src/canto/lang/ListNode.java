/* Canto Compiler and Runtime Engine
 * 
 * ListNode.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

/**
 * An ListNode is a node which contains a list of nodes.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.11 $
 */
public class ListNode<E> extends AbstractNode implements List<E> {

    private List<E> list;
    private int numInserted = 0;

    public ListNode() {
        super();
    }

    public ListNode(int capacity) {
        super();
        list = new ArrayList<E>(capacity);
    }

    public ListNode(List<E> list) {
        super();
        this.list = list;
    }

    protected void setList(List<E> list) {
        this.list = list;
    }

    /** Returns <code>false</code> */
    public boolean isPrimitive() {
        return false;
    }

    /** Returns <code>false</code> */
    public boolean isStatic() {
        return false;
    }

    /** Returns <code>false</code> */
    public boolean isDynamic() {
        return false;
    }

    /** Returns <code>false</code> */
    public boolean isDefinition() {
        return false;
    }

    /** Clone this node, including the list it contains.  The cloned node's list will
     *  be an ArrayList containing the same elements as the list in this node, regardless
     *  of the actual type of the list in this node (which need not be an ArrayList, or
     *  cloneable).
     */
    @SuppressWarnings("unchecked")
    public Object clone() {
        Object copy = super.clone();
        if (list != null) {
            ((ListNode<E>) copy).list = Context.newArrayList(list);
        }
        return copy;
    }

    /** Insert items into this list.  The wrapped list is replaced with a new
     *  ArrayList combining the contents of the passed list and the current list.
     */
    public void insert(List<E> newItems) {
        if (list == null || list.size() == 0) {
            list = (List<E>) Context.newArrayList(newItems);
        } else {
            List<E> newList = (List<E>) Context.newArrayList(newItems);
            newList.addAll(list);
            list = newList;
        }
        numInserted += newItems.size();
    }

    /** Undo the combined effect of all insert calls to this list.
     */
    public synchronized void uninsert() {
        if (numInserted > 0) {
            removeFirst(numInserted);
            numInserted = 0;
        }
    }

    /** Remove the specified number of items from the beginning of this list.
     */
    private void removeFirst(int n) {
        if (list == null || list.size() == 0) {
            throw new IndexOutOfBoundsException("Cannot remove elements from list; list is empty.");
        }
        int len = list.size();
        if (n > len) {
            throw new IndexOutOfBoundsException("Cannot remove " + n + "elements from list; list size is only " + len);
        }
        List<E> newList = (List<E>) Context.newArrayList(len - n, list);
        for (int i = n; i < len; i++) {
            newList.add(list.get(i));
        }
        list = newList;
    }

    public String toString() {
        return toString("(", ")");
    }
    
    
    /** Convert this list into a string with the specified delimeters at each
     *  end.  This is used to handle use of ArgumentList in colleciont
     *  definitions.
     */
    public String toString(String leftDelim, String rightDelim) {
        StringBuffer sb = new StringBuffer();
        sb.append(leftDelim);
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            sb.append(obj.toString());
            if (it.hasNext()) {
                sb.append(',');
            }
        }
        sb.append(rightDelim);
        return sb.toString();
    }
    
    
    // List implementation

    public int size() { return list.size(); }
    public boolean isEmpty() { return list.isEmpty(); }
    public boolean contains(Object o) { return list.contains(o); }
    public Iterator<E> iterator() { return list.iterator(); }
    public Object[] toArray() { return list.toArray(); }
    public <T> T[] toArray(T a[]) { return list.toArray(a); }
    public boolean add(E o) { return list.add(o); }
    public boolean remove(Object o) { return list.remove(o); }
    public boolean containsAll(Collection<?> c) { return list.containsAll(c); }
    public boolean addAll(Collection<? extends E> c) { return list.addAll(c); }
    public boolean addAll(int index, Collection<? extends E> c) { return list.addAll(index, c); }
    public boolean removeAll(Collection<?> c) { return list.removeAll(c); }
    public boolean retainAll(Collection<?> c) { return list.retainAll(c); }
    public void clear() { list.clear(); }
    public boolean equals(Object o) { return list.equals(o); }
    public int hashCode() { return list.hashCode(); }
    public E get(int index) { return list.get(index); }
    public E set(int index, E element) { return list.set(index, element); }
    public void add(int index, E element) { list.add(index, element); }
    public E remove(int index) { return list.remove(index); }
    public int indexOf(Object o) { return list.indexOf(o); }
    public int lastIndexOf(Object o) { return list.lastIndexOf(o); }
    public ListIterator<E> listIterator() { return list.listIterator(); }
    public ListIterator<E> listIterator(int index) { return list.listIterator(index); }
    public List<E> subList(int fromIndex, int toIndex) { return list.subList(fromIndex, toIndex); }
}
