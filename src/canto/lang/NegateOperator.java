/* Canto Compiler and Runtime Engine
 * 
 * NegateOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * Logical Not operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */
public class NegateOperator extends UnaryOperator {

    public Value operate(Value val) {
        Value result = null;
        int order = getTypeOrder(val.getValueClass());
        switch (order) {
            case Value.BOOLEAN:
                result = new PrimitiveValue(!val.getBoolean());
                break;
            case Value.BYTE:
                byte b = (byte) ~val.getByte();
                result = new PrimitiveValue(b);
                break;
            case Value.CHAR:
                char c = (char) ~val.getChar();
                result = new PrimitiveValue(c);
                break;
            case Value.INT:
                result = new PrimitiveValue(-val.getInt());
                break;
            case Value.LONG:
                result = new PrimitiveValue(-val.getLong());
                break;
            case Value.DOUBLE:
                result = new PrimitiveValue(-val.getDouble());
                break;
            default:
                throw new UnsupportedOperationException("negate operator only works on numbers");
        }
        return result;
    }

    public String toString() {
        return "-";
    }
}
