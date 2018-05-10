/* Canto Compiler and Runtime Engine
 * 
 * DynamicObject.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;

import canto.runtime.Context;

/**
 * Interface for wrapped objects (e.g. arrays) whose value must be recomputed
 * when the context changes.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.10 $
 */

public interface DynamicObject {

    /** Returns a copy of the object initialized for the specified context, given the
     *  specified arguments (if the object is something that takes arguments, such as a
     *  definition instance).   If the object is already initialized or needs no
     *  initialization, this method returns the original object.
     */
    public Object initForContext(Context context, ArgumentList args, List<Index> indexes) throws Redirection;

    /** Returns true if this object is already initialized for the specified context,
     *  i.e., if <code>initForContext(context, args) == this</code> is true.
     */
    public boolean isInitialized(Context context);
}
