/* Canto Compiler and Runtime Engine
 * 
 * DefaultType.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;

import canto.runtime.Context;

/**
 * The default object type.  There is a single global instance of this class, named
 * <code>DefaultType.TYPE</code>.  The constructor is private to enforce the singleton pattern.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.10 $
 */

public class DefaultType extends AbstractType {

    public final static Type TYPE = new DefaultType();

    private DefaultType() {
        super();
        setName("");
    }

    /** Returns <code>true</code> */
    public boolean isPrimitive() {
        return true;
    }

    private static List<Dim> empty_dims = new EmptyList<Dim>();
    public List<Dim> getDims() {
        return empty_dims;
    }

    public ArgumentList getArguments(Context context) {
        return null;
    }

    /** Returns the passed value unchanged. */
    public Value convert(Value val) {
        return val;
    }

    /** Returns <code>Object.class</code>.
     */
    public Class<?> getTypeClass(Context context) {
        return Object.class;
    }
}
