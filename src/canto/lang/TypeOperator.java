/* Canto Compiler and Runtime Engine
 * 
 * TypeOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * Class cast operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.4 $
 */
public class TypeOperator extends UnaryOperator {

    public Value operate(Value val) {
        Type type = (Type) getChild(0);
        return type.convert(val);
    }

    /** Ignore the passed type and return the type this
     *  operator casts to.
     */
    public Type getResultType(Type valType) {
        Type type = (Type) getChild(0);
        return type;
    }

    public String toString() {
        Type type = (Type) getChild(0);
        return "(" + type.getName() + ")\n";
    }
}
