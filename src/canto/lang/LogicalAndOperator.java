/* Canto Compiler and Runtime Engine
 * 
 * LogicalAndOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * Logical And operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.6 $
 */

public class LogicalAndOperator extends BooleanOperator {

    public boolean operate(boolean op1, Value val2) {
        if (!op1) {
            return false;
        }
        return val2.getBoolean();
    }

    public String toString() {
        return " && ";
    }
}
