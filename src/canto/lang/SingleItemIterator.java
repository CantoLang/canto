/* Canto Compiler and Runtime Engine
 * 
 * SingleItemIterator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;


/**
 * A SingleItemIterator is an iterator which only returns one item.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.2 $
 */

public class SingleItemIterator<E> implements Iterator<E> {
    boolean done = false;
    E item;
        
    public SingleItemIterator(E item) {
        this.item = item;
    }
   
    public boolean hasNext() {
        return !done;
    }
    
    public E next() {
        done = true;
        return item;
    }
   
    public void remove() {
        throw new UnsupportedOperationException("SingleItemIterator does not support the remove method.");
    }
}
