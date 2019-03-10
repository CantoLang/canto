/* Canto Compiler and Runtime Engine
 * 
 * Dim.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 * Empty object signifying an array dimension.  A Type object signifying an
 * array will have a Dim child for each dimension it has.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.10 $
 */
public class Dim extends AbstractNode {

    // dimension types

    public static enum TYPE {
        INDEFINITE, DEFINITE, APPEND, STREAM
    }

    /** Given an object, creates an appropriate Dim.  If the object is null, null
     *  is returned.  If the object is an array or a non-growable CantoArray, the 
     *  type of the returned Dim will be DEFINITE, otherwise it will be INDEFINITE.
     *  If the object is not a Map, Collection, CantoArray or array, null 
     *  is returned.
     */
    public static Dim createForObject(Object obj) {
        Dim dim = null;
        if (obj != null) {
            if (obj instanceof Map<?,?>) {
                dim = new Dim(TYPE.INDEFINITE, ((Map<?,?>) obj).size());
                dim.setTable(true);
            } else if (obj instanceof Collection<?>) {
                dim = new Dim(TYPE.INDEFINITE, ((Collection<?>) obj).size());
            } else if (obj instanceof CantoArray) {
                TYPE type = ((CantoArray) obj).isGrowable() ? TYPE.INDEFINITE : TYPE.DEFINITE;
                dim = new Dim(type, ((CantoArray) obj).getSize());
            } else if (obj.getClass().isArray()) {
                TYPE type = TYPE.DEFINITE;
                Object[] array = (Object[]) obj;
                for (Object element: array) {
                    if (element instanceof SuperStatement || element instanceof SubStatement || element instanceof NextStatement) {
                        type = TYPE.INDEFINITE;
                        break;
                    }
                }
                dim = new Dim(type, Array.getLength(obj));
            }
        }
        return dim;
    }

    /** Given a class, creates an appropriate Dim.  If the class is null, null
     *  is returned.  If the class is an array the type of the returned Dim will 
     *  be DEFINITE, otherwise it will be INDEFINITE.  If the class is not a Map,
     *  Collection or array, an empty Dim is returned.
     *  
     *  There is no way to tell the size of an instance from the class, so size
     *  is set to 0 in all cases.
     */
    public static Dim createForClass(Class<?> clazz) {
        Dim dim = null;
        if (clazz != null) {
            if (Map.class.isAssignableFrom(clazz)) {
                dim = new Dim(TYPE.INDEFINITE, 0);
                dim.setTable(true);
            } else if (Collection.class.isAssignableFrom(clazz)) {
                dim = new Dim(TYPE.INDEFINITE, 0);
            } else if (clazz.isArray()) {
                dim = new Dim(TYPE.DEFINITE, 0);
            } else {
                dim = new Dim();
            }
        }
        return dim;
    }

    
    private TYPE type = TYPE.INDEFINITE;
    private boolean is_table = false;
    private int size = 0;

    public Dim() {
        super();
    }

    public Dim(TYPE type, int size) {
        super();
        setType(type);
        setSize(size);
    }

    protected void setType(TYPE type) {
        this.type = type;
    }

    public TYPE getType() {
        return type;
    }

    public void setTable(boolean table) {
        is_table = table;
    }

    public boolean isTable() {
        return is_table;
    }

    protected void setSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    /** Returns <code>true</code> */
    final public boolean isPrimitive() {
        return true;
    }

    /** Returns <code>false</code> */
    final public boolean isStatic() {
        return false;
    }

    /** Returns <code>false</code> */
    final public boolean isDynamic() {
        return false;
    }

    /** Returns <code>false</code> */
    final public boolean isDefinition() {
        return false;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Dim) {
            Dim dim = (Dim) obj;
            return type == dim.getType() && size == dim.getSize() && isTable() == dim.isTable();
        }
        return false;
    }
    
    public String toString() {
        String str = "[" + (type == TYPE.APPEND ? "+" : (type == TYPE.DEFINITE ? String.valueOf(size) : "")) + "]";
        return str;
    }
}
