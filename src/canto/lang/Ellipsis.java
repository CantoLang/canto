/* Canto Compiler and Runtime Engine
 * 
 * Ellipsis.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * An ellipsis in an array definition.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.3 $
 */

public class Ellipsis extends SuperStatement {

    public Ellipsis() {
        super();
    }

    public String toString(String prefix) {
        return "...";
    }
}
