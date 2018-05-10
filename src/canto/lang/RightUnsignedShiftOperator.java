/* Canto Compiler and Runtime Engine
 * 
 * RightUnsignedShiftOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * Shift unsigned bits right operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */
public class RightUnsignedShiftOperator extends ShiftOperator {

    public byte operate(byte op1, int op2) {
        return (byte) (op1 >>> op2);
    }

    public char operate(char op1, int op2) {
        return (char) (op1 >>> op2);
    }

    public int operate(int op1, int op2) {
        return op1 >>> op2;
    }

    public long operate(long op1, int op2) {
        return op1 >>> op2;
    }

    public String operate(String op1, int op2) {
        throw new UnsupportedOperationException("'>>>' operator not defined for strings");
    }

    public String toString() {
        return " >>> ";
    }
}
