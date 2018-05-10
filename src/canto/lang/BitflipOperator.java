/* Canto Compiler and Runtime Engine
 * 
 * BitflipOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Bitwise Not operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.6 $
 */
public class BitflipOperator extends UnaryOperator {

    @SuppressWarnings("unchecked")
    public Value operate(Value val) {
        Value result = null;
        Class<?> valueClass = val.getValueClass();
        if (valueClass.isArray() || List.class.isAssignableFrom(valueClass) || CantoArray.class.isAssignableFrom(valueClass)) {
            Object obj = val.getValue();
            if (obj instanceof CantoArray) {
                obj = ((CantoArray) obj).getArrayObject();
            }
            Iterator<Object> it;
            int size;
            if (obj instanceof List) {
                size = ((List<Object>) obj).size();
                it = ((List<Object>) obj).iterator();
            } else {
                size = ((Object[]) obj).length;
                it = Arrays.asList((Object[]) obj).iterator();
            }
            List<Object> resultList = new ArrayList<Object>(size);
            while (it.hasNext()) {
                Object element = it.next();
                Value elementVal = (element instanceof Value ? (Value) element : new PrimitiveValue(element));
                resultList.add(operate(elementVal));
            }
            result = new PrimitiveValue(resultList);
            
        } else {
            int order = getTypeOrder(val.getValueClass());
            switch (order) {
                case Value.BOOLEAN:
                    result = new PrimitiveValue(!val.getBoolean());
                    break;
                case Value.BYTE:
                    byte b = (byte) ~val.getByte();
                    result = new PrimitiveValue(b);
                    break;
                case Value.CHAR:
                    char c = (char) ~val.getChar();
                    result = new PrimitiveValue(c);
                    break;
                case Value.INT:
                    result = new PrimitiveValue(~val.getInt());
                    break;
                case Value.LONG:
                    result = new PrimitiveValue(~val.getLong());
                    break;
                default:
                    throw new UnsupportedOperationException("bitflip operator only works on booleans and integral values");
            }
        }
        return result;
    }

    public String toString() {
        return "~";
    }
}
