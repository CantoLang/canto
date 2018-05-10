/* Canto Compiler and Runtime Engine
 * 
 * EqualsOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;
import java.util.Map;


/**
 * Equals operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.7 $
 */
public class EqualsOperator extends RelationalOperator {
    
    /** The equals operator returns true for collections if the two collections are
     *  the same kind (array or table), the same size and have the same elements.
     **/
    public Value collectionOperate(Object op1, Object op2) {
        int size;
        if (op1 instanceof List<?>) {
            List<?> list1 = (List<?>) op1;
            size = list1.size();
            if (op2 instanceof List<?>) {
                List<?> list2 = (List<?>) op2;
                if (list2.size() != size) {
                    return new PrimitiveValue(false);
                }
                for (int i = 0; i < size; i++) {
                    Object obj1 = list1.get(i);
                    Value val1 = (obj1 instanceof Value ? (Value) obj1 : new PrimitiveValue(obj1));
                    Object obj2 = list2.get(i);
                    Value val2 = (obj2 instanceof Value ? (Value) obj2 : new PrimitiveValue(obj2));
                    Value result = operate(val1, val2);
                    if (!result.getBoolean()) {
                        return result;
                    }
                }

            } else if (op2 instanceof Object[]) {
                Object[] array2 = (Object[]) op2;
                if (array2.length != size) {
                    return new PrimitiveValue(false);
                }
                for (int i = 0; i < size; i++) {
                    Object obj1 = list1.get(i);
                    Value val1 = (obj1 instanceof Value ? (Value) obj1 : new PrimitiveValue(obj1));
                    Object obj2 = array2[i];
                    Value val2 = (obj2 instanceof Value ? (Value) obj2 : new PrimitiveValue(obj2));
                    Value result = operate(val1, val2);
                    if (!result.getBoolean()) {
                        return result;
                    }
                }

            } else {
                // one is an array and one isn't
                return new PrimitiveValue(false);
            }
            return new PrimitiveValue(true);

        } else if (op1 instanceof Object[]) {
            Object[] array1 = (Object[]) op1;
            size = array1.length;
            if (op2 instanceof List<?>) {
                List<?> list2 = (List<?>) op2;
                if (list2.size() != size) {
                    return new PrimitiveValue(false);
                }
                for (int i = 0; i < size; i++) {
                    Object obj1 = array1[i];
                    Value val1 = (obj1 instanceof Value ? (Value) obj1 : new PrimitiveValue(obj1));
                    Object obj2 = list2.get(i);
                    Value val2 = (obj2 instanceof Value ? (Value) obj2 : new PrimitiveValue(obj2));
                    Value result = operate(val1, val2);
                    if (!result.getBoolean()) {
                        return result;
                    }
                }

            } else if (op2 instanceof Object[]) {
                Object[] array2 = (Object[]) op2;
                if (array2.length != size) {
                    return new PrimitiveValue(false);
                }
                for (int i = 0; i < size; i++) {
                    Object obj1 = array1[i];
                    Value val1 = (obj1 instanceof Value ? (Value) obj1 : new PrimitiveValue(obj1));
                    Object obj2 = array2[i];
                    Value val2 = (obj2 instanceof Value ? (Value) obj2 : new PrimitiveValue(obj2));
                    Value result = operate(val1, val2);
                    if (!result.getBoolean()) {
                        return result;
                    }
                }

            } else {
                // one is an array and one isn't
                return new PrimitiveValue(false);
            }
            return new PrimitiveValue(true);

        } else if (op1 instanceof Map && op2 instanceof Map) {
            Map<?,?> map1 = (Map<?,?>) op1;
            Map<?,?> map2 = (Map<?,?>) op2;
            size = map1.size();
            if (map2.size() != size) {
                return new PrimitiveValue(false);
            }

            for (Object key: map1.keySet()) {
                Object obj1 = map1.get(key);
                Value val1 = (obj1 instanceof Value ? (Value) obj1 : new PrimitiveValue(obj1));
                Object obj2 = map2.get(key);
                Value val2 = (obj2 instanceof Value ? (Value) obj2 : new PrimitiveValue(obj2));
                Value result = operate(val1, val2);
                if (!result.getBoolean()) {
                    return result;
                }
            }
            return new PrimitiveValue(true);

        } else {
            return new PrimitiveValue(false);
        }
    }

    
    public boolean operate(boolean op1, boolean op2) {
        return op1 == op2;
    }
    public boolean operate(byte op1, byte op2) {
        return op1 == op2;
    }
    public boolean operate(char op1, char op2) {
        return op1 == op2;
    }
    public boolean operate(int op1, int op2) {
        return op1 == op2;
    }
    public boolean operate(long op1, long op2) {
        return op1 == op2;
    }
    public boolean operate(double op1, double op2) {
        return op1 == op2;
    }
    public boolean operate(String op1, String op2) {
        if (op1 == null || op2 == null) {
            return op1 == op2;
        } else if (ignoreCase) {
            return op1.compareToIgnoreCase(op2) == 0;
        } else {
            return op1.compareTo(op2) == 0;
        }
    }

    public String toString() {
        if (ignoreCase) {
            return " ~== ";
        } else {
            return " == ";
        }
    }
}
