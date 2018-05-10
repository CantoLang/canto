/* Canto Compiler and Runtime Engine
 * 
 * BitwiseOrOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * Bitwise And operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.6 $
 */
public class BitwiseOrOperator extends BitwiseOperator {

    public boolean operate(boolean op1, boolean op2) {
        return op1 | op2;
    }

    public byte operate(byte op1, byte op2) {
        return (byte)(op1 | op2);
    }

    public char operate(char op1, char op2) {
        return (char)(op1 | op2);
    }

    public int operate(int op1, int op2) {
        return op1 | op2;
    }

    public long operate(long op1, long op2) {
        return op1 | op2;
    }

    public String toString() {
        return " | ";
    }
}
