/* Canto Compiler and Runtime Engine
 * 
 * Filter.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;


/**
 * A Filter distinguishes among paths.
 *
 * @author Michael St. Hippolyte
 */

public interface Filter {

    /** Returns true if the path is distinguished by this filter. */
    public boolean filter(String path);
}
