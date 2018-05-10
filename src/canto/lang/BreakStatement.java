/* Canto Compiler and Runtime Engine
 * 
 * BreakStatement.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * A <code>break</code> statement
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.3 $
 */

public class BreakStatement extends CantoStatement {

    public BreakStatement() {
        super();
    }

    public boolean isDynamic() {
        return false;
    }
}
