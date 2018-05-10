/* Canto Compiler and Runtime Engine
 * 
 * CantoTable.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * Interface for Canto tables.
 *
 * @author Michael St. Hippolyte
 */

public interface CantoTable {

    /** Returns an element in the table.  The return value may be a Chunk,
     *  a Value, another array or null.  Throws a NoSuchElementException if the
     *  table does not contain an entry for the specified key, nor a default
     *  entry.
     */
    public Object get(Object key);

    public int getSize();

    public boolean isGrowable();

    public void put(Object key, Object element);
}
