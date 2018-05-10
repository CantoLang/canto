/* Canto Compiler and Runtime Engine
 * 
 * ConstructionContainer.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;

import canto.runtime.Context;

/**
 * ConstructionContainer is the interface for objects which own dynamic and/or
 * static constructions.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */

public interface ConstructionContainer {

    /** Returns the list of constructions owned by this container. */
    public List<Construction> getConstructions(Context context);
}
