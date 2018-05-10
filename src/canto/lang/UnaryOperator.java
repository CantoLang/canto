/* Canto Compiler and Runtime Engine
 * 
 * UnaryOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;

/**
 * Base class for unary operators.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */

abstract public class UnaryOperator extends AbstractOperator {

    public UnaryOperator() {}

    public Value operate(List<Value> operands) {
        return operate((Value) operands.get(0));
    }

    abstract public Value operate(Value val);

    public Type getResultType(Type type) {
        return type;
    }
}
