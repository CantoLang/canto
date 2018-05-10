/* Canto Compiler and Runtime Engine
 * 
 * CantoArray.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.Iterator;
import java.util.List;

import canto.runtime.Context;

/**
 * Interface for canto arrays.
 *
 * @author Michael St. Hippolyte
 */

public interface CantoArray {

    /** Returns the object containing the array data.  This will generally be
     *  an Object array or a List object, depending on whether the list
     *  is fixed or growable, but it may be other collection types if the
     *  source is external, for example a ResultSet returned from a database
     *  query.
     */
    public Object getArrayObject();

    /** Returns the object containing the array data, fully instantiated.  This
     *  will be the same as the data returned by getArrayObject, except that any
     *  items in the array which are element definitions are instantiated for the
     *  specified context.
     */
    public Object instantiateArray(Context context) throws Redirection;

    /** Returns the nth element of the array.  The return value may be a Chunk,
     *  a Value, another array or null.  Throws an ArrayIndexOutOfBoundsException
     *  if n is not greater than or equal to zero and less than the size of the
     *  array.
     */
    public Object get(int n);

    public int getSize();

    public boolean isGrowable();

    /** For a growable array, adds an element to the end of the array.  Throws
     *  an UnsupportedOperationException on a fixed array.
     */
    public boolean add(Object element);

    public boolean addAll(List<Object> list);

    public Object set(int n, Object element);

    public Iterator<Object> iterator();
}
