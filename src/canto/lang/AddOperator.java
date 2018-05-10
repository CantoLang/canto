/* Canto Compiler and Runtime Engine
 * 
 * AddOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;
import java.util.ArrayList;

/**
 * Addition operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.8 $
 */
public class AddOperator extends ArithmeticOperator {

    public boolean operate(boolean op1, boolean op2) {
        return op1 || op2;   // interpret '+' as logical or
    }

    public byte operate(byte op1, byte op2) {
        return (byte)(op1 + op2);
    }

    public char operate(char op1, char op2) {
        return (char) (op1 + op2);
    }

    public int operate(int op1, int op2) {
        return op1 + op2;
    }

    public long operate(long op1, long op2) {
        return op1 + op2;
    }

    public double operate(double op1, double op2) {
        return op1 + op2;
    }

    public String operate(String op1, String op2) {
        return op1 + op2;
    }


    public Object arrayOperate(Object op1, Object op2) {
        if (op1 instanceof List<?>) {
            List<?> list1 = (List<?>) op1;
            ArrayList<Object> resultList = null;
            if (op2 instanceof List<?>) {
                List<?> list2 = (List<?>) op2;
                resultList = new ArrayList<Object>(list1.size() + list2.size());
                resultList.addAll(list1);
                resultList.addAll(list2);
            } else if (op2 instanceof Object[]) {
                Object[] array2 = (Object[]) op2;
                resultList = new ArrayList<Object>(list1.size() + array2.length);
                resultList.addAll(list1);
                for (int i = 0; i < array2.length; i++) {
                    resultList.add(array2[i]);
                }

            } else {
                resultList = new ArrayList<Object>(list1.size() + 1);
                resultList.addAll(list1);
                resultList.add(op2);
            }
            return resultList;

        } else if (op2 instanceof List<?>) {
            List<?> list2 = (List<?>) op2;
            ArrayList<Object> resultList = null;
            if (op1 instanceof Object[]) {
                Object[] array1 = (Object[]) op1;
                resultList = new ArrayList<Object>(array1.length + list2.size());
                for (int i = 0; i < array1.length; i++) {
                    resultList.add(array1[i]);
                }
                resultList.addAll(list2);

            } else {
                resultList = new ArrayList<Object>(1 + list2.size());
                resultList.add(op1);
                resultList.addAll(list2);
            }
            return resultList;

        } else if (op1 instanceof Object[]) {
            Object[] array1 = (Object[]) op1;
            Object[] resultArray = null;
            if (op2 instanceof Object[]) {
                Object[] array2 = (Object[]) op2;
                resultArray = new Object[array1.length + array2.length];
                System.arraycopy(array1, 0, resultArray, 0, array1.length);
                System.arraycopy(array2, 0, resultArray, array1.length, array2.length);

            } else {
                resultArray = new Object[array1.length + 1];
                System.arraycopy(array1, 0, resultArray, 0, array1.length);
                resultArray[array1.length] = op2;
            }
            return resultArray;

        } else if (op2 instanceof Object[]) {
            Object[] array2 = (Object[]) op2;
            Object[] resultArray = new Object[1 + array2.length];
            resultArray[0] = op1;
            System.arraycopy(array2, 0, resultArray, 1, array2.length);
            return resultArray;

        } else {
            throw new UnsupportedOperationException("Illegal classes for array addition: " + op1.getClass().getName() + " and " + op2.getClass().getName());
        }
    }

    public String toString() {
        return " + ";
    }
}
