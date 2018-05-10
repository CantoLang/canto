/* Canto Compiler and Runtime Engine
 * 
 * LogicalOrOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * Logical Or operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */
public class LogicalOrOperator extends BooleanOperator {

    public boolean operate(boolean op1, Value val2) {
        if (op1) {
            return true;
        }
        return val2.getBoolean();
    }

    public String toString() {
        return " || ";
    }
}
