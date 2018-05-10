/* Canto Compiler and Runtime Engine
 * 
 * RegExp.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * Base class for regexp nodes.  A regexp node holds a pattern matching string.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.3 $
 */
abstract public class RegExp extends NameNode {

    public RegExp(String regexp) {
        super(regexp);
    }

    abstract public boolean matches(String str);
}
