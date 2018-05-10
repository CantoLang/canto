/* Canto Compiler and Runtime Engine
 * 
 * MultiplyOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * Addition operator.
 *
 * @author Michael St. Hippolyte
 */

public class MultiplyOperator extends ArithmeticOperator {

    public boolean operate(boolean op1, boolean op2) {
        return op1 && op2;   // interpret '*' as logical and
    }

    public byte operate(byte op1, byte op2) {
        return (byte)(op1 * op2);
    }

    public char operate(char op1, char op2) {
        return (char)(op1 * op2);
    }

    public int operate(int op1, int op2) {
        return op1 * op2;
    }

    public long operate(long op1, long op2) {
        return op1 * op2;
    }

    public double operate(double op1, double op2) {
        return op1 * op2;
    }

    /** The * operator on Strings in Canto concatenates like the + operator, except
     *  that it adds a space if the first string ends with a non-whitespace character
     *  and the second string starts with a non-whitespace character.
     */
    public String operate(String op1, String op2) {
        int len1 = op1.length();

        if (op1.length() > 0 && op2.length() > 0) {
            if (!Character.isWhitespace(op1.charAt(len1 - 1)) && !Character.isWhitespace(op2.charAt(0))) {
                return op1 + ' ' + op2;
            }
        }
        return op1 + op2;
    }

    public Object arrayOperate(Object op1, Object op2) {
        throw new UnsupportedOperationException("'*' operator not defined for arrays");
    }

    public String toString() {
        return " * ";
    }
}
