/* Canto Compiler and Runtime Engine
 * 
 * AbstractOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;

import canto.runtime.Context;

/**
 * Abstract base class of Operator implementations.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.11 $
 */
abstract public class AbstractOperator extends AbstractNode implements Operator {

    /**  Used by subclasses for reconciling different operand types. */
    protected final static int getTypeOrder(Class<?> type) {
        if (type == Void.TYPE || type == Void.class || type == null) {
            return Value.VOID;
        } else if (type == Boolean.TYPE || type == Boolean.class) {
            return Value.BOOLEAN;
        } else if (type == Byte.TYPE || type == Byte.class) {
            return Value.BYTE;
        } else if (type == Short.TYPE || type == Short.class || type == Integer.TYPE || type == Integer.class) {
            return Value.INT;
        } else if (type == Long.TYPE || type == Long.class) {
            return Value.LONG;
        } else if (type == Float.TYPE || type == Float.class || type == Double.TYPE || type == Double.class) {
            return Value.DOUBLE;
        } else if (type == Character.TYPE || type == Character.class) {
            return Value.CHAR;
        } else {
            return Value.STRING;
        }
    }

    public AbstractOperator() {}

    /** Operate on one or more operands and return the result.
     */
    abstract public Value operate(List<Value> operands);

    /** Returns <code>true</code> */
    public boolean isPrimitive() {
        return true;
    }

    /** Returns <code>false</code> */
    public boolean isStatic() {
        return false;
    }

    /** Returns <code>false</code> */
    public boolean isDynamic() {
        return false;
    }

    /** Returns <code>false</code> */
    public boolean isDefinition() {
        return false;
    }

    /** Returns the type of the value which would be returned by operating on the
     *  specified types in the specified context.  The base class implementation returns
     *  the highest type which is the type or supertype of all operands, or
     *  DefaultType.TYPE if no such common type exists.
     */
    public Type getResultType(List<Type> types, Context context) {
        Type type = types.get(0);
        if (type != DefaultType.TYPE) {
            int numTypes = types.size();
            for (int i = 1; i < numTypes; i++) {
                Type nextType = (Type) types.get(i);
                if (nextType == DefaultType.TYPE) {
                    type = DefaultType.TYPE;
                    break;
                } else if (type.isTypeOf(nextType, context)) {
                    type = nextType;
                } else if (!nextType.isTypeOf(type, context)) {
                    type = DefaultType.TYPE;
                    break;
                }
            }
        }
        return type;
    }
    
    public String toString(String prefix) {
        return prefix + toString();
    }
}
