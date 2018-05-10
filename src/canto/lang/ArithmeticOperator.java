/* Canto Compiler and Runtime Engine
 * 
 * ArithmeticOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

/**
 * Base class for arithmetic operators.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.12 $
 */
abstract public class ArithmeticOperator extends BinaryOperator {

    public ArithmeticOperator() {}

    abstract public boolean operate(boolean op1, boolean op2);
    abstract public byte operate(byte op1, byte op2);
    abstract public int operate(int op1, int op2);
    abstract public long operate(long op1, long op2);
    abstract public double operate(double op1, double op2);
    /** chars are converted to ints for the arithmetic and the result is
     *  converted back to a char */
    abstract public char operate(char op1, char op2);
    abstract public String operate(String op1, String op2);

    /** Some arithmetic operators can operate on arrays */
    abstract public Object arrayOperate(Object op1, Object op2);

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
                if (obj1 == null) {
                    obj1 = new Object[0];
                }
                obj1 = ArrayBuilder.instantiateElements(obj1, context);

            } else if (firstObj instanceof Value) {
                obj1 = ((Value) firstObj).getValue();
                
            } else {
                obj1 = firstObj.getValue(context);
                if (obj1 instanceof Value) {
                    obj1 = ((Value) obj1).getValue();
                }
            }

            Object obj2 = null;
            if (type2 != null && type2.isArray()) {
                Value val = secondObj.getValue(context);
                obj2 = val.getValue();
                if (obj2 instanceof CantoArray) {
                    obj2 = ((CantoArray) obj2).getArrayObject();
                }
                if (obj2 == null) {
                    obj2 = new Object[0];
                }
                obj2 = ArrayBuilder.instantiateElements(obj2, context);

            } else if (secondObj instanceof Value) {
                obj2 = ((Value) secondObj).getValue();
                
            } else {
                obj2 = secondObj.getValue(context);
                if (obj2 instanceof Value) {
                    obj2 = ((Value) obj2).getValue();
                }
            }
            
            Object resultArray = arrayOperate(obj1, obj2);
            return new PrimitiveValue(resultArray);
        } else {
            return operate(firstObj.getValue(context), new DeferredValue(secondObj, context));
        }
    }

    
    
    public Value operate(Value firstVal, Value secondVal) {
        Value result = null;

        Object firstObj = firstVal.getValue();
        if (firstObj instanceof CantoArray) {
            firstObj = ((CantoArray) firstObj).getArrayObject();
        }

        Object secondObj = secondVal.getValue();
        if (secondObj instanceof CantoArray) {
            secondObj = ((CantoArray) secondObj).getArrayObject();
        }

        if (firstObj instanceof List<?> || firstObj instanceof Object[] || secondObj instanceof List<?> || secondObj instanceof Object[]) {
            Object resultArray = arrayOperate(firstObj, secondObj);
            result = new PrimitiveValue(resultArray);

        } else {
            Class<?> firstClass = firstVal.getValueClass();
            Class<?> secondClass = secondVal.getValueClass();
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
                case Value.DOUBLE:
                    double d = operate(firstVal.getDouble(), secondVal.getDouble());
                    result = new PrimitiveValue(d);
                    break;
                default:
                    String str = operate(firstVal.getString(), secondVal.getString());
                    result = new PrimitiveValue(str, String.class);
                    break;
            }
        }
        return result;
    }
    
}
