/* Canto Compiler and Runtime Engine
 * 
 * RelationalOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
 * Base class for relational operators.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.10 $
 */

abstract public class RelationalOperator extends BinaryOperator {

    protected boolean ignoreCase = false;
    
    public RelationalOperator() {}

    abstract public boolean operate(boolean bool1, boolean bool2);
    abstract public boolean operate(byte op1, byte op2);
    abstract public boolean operate(char op1, char op2);
    abstract public boolean operate(int op1, int op2);
    abstract public boolean operate(long op1, long op2);
    abstract public boolean operate(double op1, double op2);
    abstract public boolean operate(String op1, String op2);
    
    abstract public Value collectionOperate(Object op1, Object op2);

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
            
            return collectionOperate(obj1, obj2);
        } else {
            return operate(firstObj.getValue(context), new DeferredValue(secondObj, context));
        }
    }
    
    public Value operate(Value firstVal, Value secondVal) {
        return new PrimitiveValue(getBoolean(firstVal, secondVal));
    }

    /** Relational operators always return a boolean result **/
    public Type getResultType(Type firstType, Type secondType, Context context) {
        return PrimitiveType.BOOLEAN;
    }

    public boolean getBoolean(Value firstVal, Value secondVal) {
        Class<?> firstClass = firstVal.getValueClass();
        Class<?> secondClass = secondVal.getValueClass();

        int resultOrder = Math.max(getTypeOrder(firstClass), getTypeOrder(secondClass));
        boolean bool = false;
        switch (resultOrder) {
            case Value.BOOLEAN:
                bool = operate(firstVal.getBoolean(), secondVal.getBoolean());
                break;
            case Value.BYTE:
                bool = operate(firstVal.getByte(), secondVal.getByte());
                break;
            case Value.CHAR:
                bool = operate(firstVal.getChar(), secondVal.getChar());
                break;
            case Value.INT:
                bool = operate(firstVal.getInt(), secondVal.getInt());
                break;
            case Value.LONG:
                bool = operate(firstVal.getLong(), secondVal.getLong());
                break;
            case Value.DOUBLE:
                bool = operate(firstVal.getDouble(), secondVal.getDouble());
                break;
            default:
                bool = operate(firstVal.getString(), secondVal.getString());
                break;
        }
        return bool;
    }


    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }
}
