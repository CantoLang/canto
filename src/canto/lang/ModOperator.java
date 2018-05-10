/* Canto Compiler and Runtime Engine
 * 
 * ModOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * Modulo operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.7 $
 */
public class ModOperator extends ArithmeticOperator {

    public boolean operate(boolean op1, boolean op2) {
        return op1 ^ op2;   // interpret '%' as logical xor
    }

    public byte operate(byte op1, byte op2) {
        return (byte)(op1 % op2);
    }

    public char operate(char op1, char op2) {
        return (char) (op1 % op2);
    }

    public int operate(int op1, int op2) {
        return op1 % op2;
    }

    public long operate(long op1, long op2) {
        return op1 % op2;
    }

    public double operate(double op1, double op2) {
        return op1 % op2;
    }

    public String operate(String op1, String op2) {
        throw new UnsupportedOperationException("'%' operator not defined for strings");
    }

    public Object arrayOperate(Object op1, Object op2) {
        throw new UnsupportedOperationException("'%' operator not defined for arrays");
    }


    public String toString() {
        return " % ";
    }
}
