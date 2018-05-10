/* Canto Compiler and Runtime Engine
 * 
 * Console.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import canto.lang.*;

/**
 * A Console monitors the construction process interactively.
 *
 * @author Michael St. Hippolyte
 */

public interface Console {

    // this is called in Instantiation
    public void reportLookup(NameNode name, Definition def);

    // these are called in Context
    public Object reportStartConstruction(Construction construction);
    public Object reportEndConstruction(Construction construction);

    // this is called in Instantiation
    public Object reportInstantiation(Instantiation instantiation);
}
