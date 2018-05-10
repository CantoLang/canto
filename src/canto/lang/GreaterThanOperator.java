/* Canto Compiler and Runtime Engine
 * 
 * GreaterThanOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * Greater than operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.7 $
 */
public class GreaterThanOperator extends RelationalOperator {

    public Value collectionOperate(Object op1, Object op2) {
        throw new UnsupportedOperationException("'>' operator not defined for collections");
    }

    public boolean operate(boolean op1, boolean op2) {
        return op1 && !op2;
    }
    public boolean operate(byte op1, byte op2) {
        return op1 > op2;
    }
    public boolean operate(char op1, char op2) {
        return op1 > op2;
    }
    public boolean operate(int op1, int op2) {
        return op1 > op2;
    }
    public boolean operate(long op1, long op2) {
        return op1 > op2;
    }
    public boolean operate(double op1, double op2) {
        return op1 > op2;
    }
    public boolean operate(String op1, String op2) {
        if (ignoreCase) {
            return op1.compareToIgnoreCase(op2) > 0;
        } else {
            return op1.compareTo(op2) > 0;
        }
    }

    public String toString() {
        if (ignoreCase) {
            return " ~> ";
        } else {
            return " > ";
        }
    }
}
