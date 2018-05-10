/* Canto Compiler and Runtime Engine
 * 
 * InOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import canto.runtime.Context;

import java.util.Arrays;

/**
 * In operator.  Returns true if the first operand is a member of the
 * second operand.  If the second operand is not a collection, returns
 * false.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.1 $
 */
public class InOperator extends BinaryOperator {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Value operate(Value firstVal, Value secondVal) {
        
        Object memberObj = firstVal.getValue();
        Object collectionObj = secondVal.getValue();
        boolean isIn = false;

        if (collectionObj instanceof String) {
            isIn = (((String) collectionObj).indexOf(memberObj.toString()) > -1);
        	
        } else if (collectionObj instanceof Map) {
            isIn = ((Map) collectionObj).containsValue(memberObj);

        } else {
            List<?> list = null;
            Class clazz = collectionObj.getClass();
            if (clazz.isArray()) {
                list = Arrays.asList(collectionObj);
            } else if (collectionObj instanceof List) {
                list = ((List<?>) collectionObj);
            }
            
            if (list != null) {
                Iterator<Object> it = (Iterator<Object>) list.iterator();
                while (it.hasNext() && !isIn) {
                    Object element = it.next();
                    if (element.equals(memberObj)) {
                        isIn = true;
                    } else if (element instanceof Value && memberObj.equals(((Value) element).getValue())) {
                        isIn = true;    
                    }
                }
            }
        }
        return new PrimitiveValue(isIn);
    }

    /** Always returns boolean type */
    public Type getResultType(Type firstType, Type secondType, Context context) {
        return PrimitiveType.BOOLEAN;
    }

    public String toString() {
        return " in ";
    }
}
