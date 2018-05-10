/* Canto Compiler and Runtime Engine
 * 
 * AnyAny.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * Equivalant to one or more asterisks ("*.*.*").
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.3 $
 */
public class AnyAny extends RegExp {
    public AnyAny() {
        super("**");
    }

    public boolean matches(String str) {
        return (str != null);
    }

    public String toString() {
        return "**";
    }
}
