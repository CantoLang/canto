/* Canto Compiler and Runtime Engine
 * 
 * ContinueStatement.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * A directive to continue with a construction after another
 * construction completes.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.4 $
 */

public class ContinueStatement extends CantoStatement {

    public ContinueStatement() {
        super();
    }

    public boolean isDynamic() {
        return false;
    }
}
