/* Canto Compiler and Runtime Engine
 * 
 * ShiftOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * Base class for shift operators.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.6 $
 */
abstract public class ShiftOperator extends BinaryOperator {

    public ShiftOperator() {}

    abstract public byte operate(byte op1, int op2);
    abstract public char operate(char op1, int op2);
    abstract public int operate(int op1, int op2);
    abstract public long operate(long op1, int op2);
    abstract public String operate(String op1, int op2);

    public Value operate(Value firstVal, Value secondVal) {
        Class<?> type = firstVal.getValueClass();
        Value result = null;

        int amount = getShiftAmount(secondVal);

        if (type == Byte.TYPE) {
            // Java bytes are signed, Canto bytes are unsigned, so 
            // convert into an int and mask off high bytes in order to 
            // avoid extended sign bit problems
            byte b = (byte) operate(firstVal.getInt() & 0xFF, amount);
            result = new PrimitiveValue(b);
        } else if (type == Character.TYPE) {
            char c = operate(firstVal.getChar(), amount);
            result = new PrimitiveValue(c);
        } else if (type == Short.TYPE || type == Integer.TYPE) {
            int n = operate(firstVal.getInt(), amount);
            result = new PrimitiveValue(n);
        } else if (type == Long.TYPE) {
            long ln = operate(firstVal.getLong(), amount);
            result = new PrimitiveValue(ln);
        } else if (type == String.class) {
            String str = operate(firstVal.getString(), amount);
            result = new PrimitiveValue(str, String.class);
        } else {
            throw new UnsupportedOperationException("Invalid operand type for shift operation");
        }
        return result;
    }

    private int getShiftAmount(Value value) {
        Class<?> type = value.getValueClass();
        if (type == Byte.TYPE || type == Short.TYPE || type == Integer.TYPE || type == Long.TYPE || type == Character.TYPE ||
              type == Byte.class || type == Short.class || type == Integer.class || type == Long.class || type == Character.class) {
            return value.getInt();
        } else {
            throw new UnsupportedOperationException("Second operand of shift operator must be an integral type");
        }
    }
}
