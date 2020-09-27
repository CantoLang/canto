/* Canto Compiler and Runtime Engine
 * 
 * ModOperator.java
 *
 * Copyright (c) 2020 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * Power operator.
 */
public class PowerOperator extends ArithmeticOperator {

    public boolean operate(boolean op1, boolean op2) {
        throw new UnsupportedOperationException("'**' operator not defined for booleans");
    }

    public byte operate(byte op1, byte op2) {
        return (byte) Math.pow(op1, op2);
    }

    public char operate(char op1, char op2) {
        throw new UnsupportedOperationException("'**' operator not defined for characters");
    }

    public int operate(int op1, int op2) {
        return (int) Math.pow(op1, op2);
    }

    public long operate(long op1, long op2) {
        return (long) Math.pow(op1, op2);
    }

    public double operate(double op1, double op2) {
        return Math.pow(op1, op2);
    }

    public String operate(String op1, String op2) {
        throw new UnsupportedOperationException("'**' operator not defined for strings");
    }

    public Object arrayOperate(Object op1, Object op2) {
        throw new UnsupportedOperationException("'**' operator not defined for arrays");
    }


    public String toString() {
        return "**";
    }
}
