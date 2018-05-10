/* Canto Compiler and Runtime Engine
 * 
 * CodeGenerator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
 * A CodeGenerator is an object which generates Canto source code dynamically (i.e.
 * for a particular context).
 *
 * @author Michael St. Hippolyte
 */

public interface CodeGenerator {

    /** Generates Canto source code given a context. */
    public String generateCode(Context context) throws Redirection;
}
