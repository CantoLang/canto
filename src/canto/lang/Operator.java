/* Canto Compiler and Runtime Engine
 * 
 * Operator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;

/**
 * Operator interface.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */

public interface Operator {

    /** Operates on one or more operands and return the result.
     */
    public Value operate(List<Value> operands);

}
