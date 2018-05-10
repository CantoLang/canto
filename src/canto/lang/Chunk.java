/* Canto Compiler and Runtime Engine
 * 
 * Chunk.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
 * A Chunk is a Canto object with a visible manifestation.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.9 $
 */

public interface Chunk extends ValueGenerator {

    /** Gets the contents of the chunk as a boolean value, given a particular 
     *  context.
     */
    public boolean getBoolean(Context context) throws Redirection;

    /** Gets the contents of the chunk as text, given a particular context.  This
     *  is a short cut for calling toString on the object returned by getData,
     *  for convenience and to eliminate unnecessary conversions.
     */
    public String getText(Context context) throws Redirection;

    /** Gets the data for this chunk.  This data may be either generated or
     *  retrieved from a cache, depending on the context and the durability
     *  property (static, dynamic, contextual) of this chunk.  The data is
     *  returned as an instance of the native class of the chunk (i.e., the
     *  class corresponding to the primitive type of the data).
     */
    public Object getData(Context context) throws Redirection;

    /** Returns true if this chunk is abstract, i.e., if it cannot be 
     *  instantiated because to do so would require instantiating an abstract
     *  definition.
     */
    public boolean isAbstract(Context context);
}
