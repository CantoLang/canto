/* Canto Compiler and Runtime Engine
 * 
 * UnknownType.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;

import canto.runtime.Context;

/**
 * The unknown type.  There is a single global instance of this class, named
 * <code>UnknownType.TYPE</code>.  The constructor is private to enforce the
 * singleton pattern.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.7 $
 */

public class UnknownType extends AbstractType {

    public final static Type TYPE = new UnknownType();

    private UnknownType() {
        super();
    }

    /** Returns <code>true</code> */
    public boolean isPrimitive() {
        return true;
    }

    /** Returns an empty string. */
    public String getName() {
        return "";
    }

    public List<Dim> getDims() {
        return new EmptyList<Dim>();
    }

    public ArgumentList getArguments(Context context) {
        return null;
    }

    public Class<?> getTypeClass(Context context) {
        return Object.class;
    }


    /** Returns the passed value unchanged. */
    public Value convert(Value val) {
        return val;
    }
}
