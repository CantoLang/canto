/* Canto Compiler and Runtime Engine
 * 
 * BitwiseOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.ArrayList;
import java.util.List;

import canto.runtime.Context;

/**
 * Base class for bitwise operators.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.7 $
 */

abstract public class BitwiseOperator extends BinaryOperator {

    public BitwiseOperator() {}

    abstract public boolean operate(boolean op1, boolean op2);
    abstract public byte operate(byte op1, byte op2);
    abstract public char operate(char op1, char op2);
    abstract public int operate(int op1, int op2);
    abstract public long operate(long op1, long op2);

    /** bitwise operators operate on arrays by operating on the corresponding elements in the arrays.  If
     *  the two arrays do not have the same number of elements a runtime exception is thrown.
     * @throws Redirection 
     **/
    public Object arrayOperate(Object op1, Object op2, Context context) throws Redirection {
        int size;
        if (op1 instanceof List<?>) {
            List<?> list1 = (List<?>) op1;
            size = list1.size();
            ArrayList<Object> resultList = null;
            if (op2 instanceof List<?>) {
                List<?> list2 = (List<?>) op2;
                if (list2.size() != size) {
                    throw new UnsupportedOperationException("arrays must be the same size for bitwise operators");
                }
                resultList = new ArrayList<Object>(size);
                for (int i = 0; i < size; i++) {
                    Object obj1 = list1.get(i);
                    Value val1 = (obj1 instanceof Value ? (Value) obj1 : new PrimitiveValue(obj1));
                    Object obj2 = list2.get(i);
                    Value val2 = (obj2 instanceof Value ? (Value) obj2 : new PrimitiveValue(obj2));
                    
                    resultList.add(operate(val1, val2));
                }

            } else if (op2 instanceof Object[]) {
                Object[] array2 = (Object[]) op2;
                if (array2.length != size) {
                    throw new UnsupportedOperationException("arrays must be the same size for bitwise operators");
                }
                resultList = new ArrayList<Object>(size);

                for (int i = 0; i < size; i++) {
                    Object obj1 = list1.get(i);
                    Value val1 = (obj1 instanceof Value ? (Value) obj1 : new PrimitiveValue(obj1));
                    Object obj2 = array2[i];
                    Value val2 = (obj2 instanceof Value ? (Value) obj2 : new PrimitiveValue(obj2));
                    
                    resultList.add(operate(val1, val2));
                }

            } else {
                throw new UnsupportedOperationException("if either bitwise operand is an array the other must also be an array");
            }
            return resultList;

        } else if (op1 instanceof Object[]) {
            Object[] array1 = (Object[]) op1;
            size = array1.length;
            if (op2 instanceof List<?>) {
                List<?> list2 = (List<?>) op2;
                if (list2.size() != size) {
                    throw new UnsupportedOperationException("arrays must be the same size for bitwise operators");
                }
                ArrayList<Object> resultList = null;
                resultList = new ArrayList<Object>(size);
                for (int i = 0; i < size; i++) {
                    Object obj1 = array1[i];
                    Value val1 = (obj1 instanceof Value ? (Value) obj1 : new PrimitiveValue(obj1));
                    Object obj2 = list2.get(i);
                    Value val2 = (obj2 instanceof Value ? (Value) obj2 : new PrimitiveValue(obj2));
                    
                    resultList.add(operate(val1, val2));
                }
                return resultList;

            } else if (op2 instanceof Object[]) {
                Object[] array2 = (Object[]) op2;
                if (array2.length != size) {
                    throw new UnsupportedOperationException("arrays must be the same size for bitwise operators");
                }
                Object[] resultArray = new Object[size];
                for (int i = 0; i < size; i++) {
                    Object obj1 = array1[i];
                    Value val1;
                    if (obj1 instanceof Value) {
                    	val1 = (Value) obj1;
                    } else if (obj1 instanceof ValueGenerator) {
                  	    val1 = ((ValueGenerator) obj1).getValue(context);
                    } else {
                        val1 = new PrimitiveValue(obj1);
                    }
                    Object obj2 = array2[i];
                    Value val2;
                    if (obj2 instanceof Value) {
                    	val2 = (Value) obj2;
                    } else if (obj2 instanceof ValueGenerator) {
                  	    val2 = ((ValueGenerator) obj2).getValue(context);
                    } else {
                        val2 = new PrimitiveValue(obj2);
                    }
                    
                    resultArray[i] = operate(val1, val2);
                }
                return resultArray;

            } else {
                throw new UnsupportedOperationException("if either bitwise operand is an array the other must also be an array");
            }

        } else {
            throw new UnsupportedOperationException("Illegal classes for bitwise array operation: " + op1.getClass().getName() + " and " + op2.getClass().getName());
        }
    }

    public Value operate(ValueSource firstObj, ValueSource secondObj, Context context) throws Redirection {
        Type type1 = getValueSourceType(firstObj, context, true);
        Type type2 = getValueSourceType(secondObj, context, true);
        
        if ((type1 != null && type1.isArray()) || (type2 != null && type2.isArray())) {
            Object obj1 = null;
            if (type1 != null && type1.isArray()) {
                Value val = firstObj.getValue(context);
                obj1 = val.getValue();
                if (obj1 instanceof CantoArray) {
                    obj1 = ((CantoArray) obj1).getArrayObject();
                }
                obj1 = ArrayBuilder.instantiateElements(obj1, context);

            } else if (firstObj instanceof Value) {
                obj1 = ((Value) firstObj).getValue();
                
            } else {
                obj1 = firstObj.getValue(context);
            }

            Object obj2 = null;
            if (type2 != null && type2.isArray()) {
                Value val = secondObj.getValue(context);
                obj2 = val.getValue();
                if (obj2 instanceof CantoArray) {
                    obj2 = ((CantoArray) obj2).getArrayObject();
                }
                obj2 = ArrayBuilder.instantiateElements(obj2, context);

            } else if (secondObj instanceof Value) {
                obj2 = ((Value) secondObj).getValue();
                
            } else {
                obj2 = secondObj.getValue(context);
            }
            
            Object resultArray = arrayOperate(obj1, obj2, context);
            return new PrimitiveValue(resultArray);
        } else {
            return operate(firstObj.getValue(context), new DeferredValue(secondObj, context));
        }
    }

    
    public Value operate(Value firstVal, Value secondVal) {
        Class<?> firstClass = firstVal.getValueClass();
        Class<?> secondClass = secondVal.getValueClass();
        Value result = null;

        int resultOrder = Math.max(getTypeOrder(firstClass), getTypeOrder(secondClass));
        switch (resultOrder) {
            case Value.BOOLEAN:
                boolean bool = operate(firstVal.getBoolean(), secondVal.getBoolean());
                result = new PrimitiveValue(bool);
                break;
            case Value.BYTE:
                byte b = operate(firstVal.getByte(), secondVal.getByte());
                result = new PrimitiveValue(b);
                break;
            case Value.CHAR:
                char c = operate(firstVal.getChar(), secondVal.getChar());
                result = new PrimitiveValue(c);
                break;
            case Value.INT:
                int n = operate(firstVal.getInt(), secondVal.getInt());
                result = new PrimitiveValue(n);
                break;
            case Value.LONG:
                long ln = operate(firstVal.getLong(), secondVal.getLong());
                result = new PrimitiveValue(ln);
                break;
            default:
                String optype = getClass().getName();
                int ix = optype.lastIndexOf('.');
                if (ix > 0) {
                    optype = optype.substring(ix + 1);
                }
                throw new UnsupportedOperationException(optype + " only defined for boolean and integral values");
        }
        return result;
    }
}
