/* Canto Compiler and Runtime Engine
 * 
 * Construction.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
 * A construction is a Canto statement which generates data.  Exactly
 * when the data is generated varies, depending on whether a construction
 * is dynamic, static, or contextual.  Dynamic constructions are
 * evaluated every time they are executed.  Static constructions are only 
 * evaluated once.  A contextual construction is re-evaluated only if the
 * context has changed.
 *
 * Regardless of the variety, data is generated lazily, i.e. at runtime
 * rather than compile time.  
 *
 * @author Michael St. Hippolyte
 */

public interface Construction extends Chunk {

    /** Returns the type of this construction in the specified context. */
    public Type getType(Context context, boolean generate);

    /** Returns the name of the definition being constructed */
    public String getDefinitionName();

    /** Gets the data for this construction, using the passed definition if
     *  provided.  This data may be either generated or retrieved from a cache, 
     *  depending on the context and the durability property (static, dynamic,
     *  contextual) of this construction.
     *  
     *  The definition parameter may be null, in which case the construction 
     *  object determines the definition based on the object's name and type
     *  as well as the context.
     */
    public Object getData(Context context, Definition def) throws Redirection;
    
    public Construction getUltimateConstruction(Context context);

    public boolean isPrimitive();

}
