/* Canto Compiler and Runtime Engine
 * 
 * BinaryOperator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import canto.runtime.Context;

/**
 * Base class for binary operators.
 *
 * @author Michael St. Hippolyte
 */

abstract public class BinaryOperator extends AbstractOperator {

    public BinaryOperator() {}

    public Value operate(List<Value> operands) {
        Value val = operands.get(0);
        int numOperands = operands.size();
        for (int i = 1; i < numOperands; i++) {
            val = operate(val, (Value) operands.get(i));
        }
        return val;
    }

    /** Some operations can and should operate on objects rather than values.
     *  For example, an addition operation between an array and an object should
     *  adds the object to the array, which does not require instantiating it.
     *  
     *  Any operators that implement object handling should override this function
     *  to handle such special cases.  The function should call the standard version
     *  of <code>operate</code> to handle standard cases.
     */
    public Value operate(ValueSource firstObj, ValueSource secondObj, Context context) throws Redirection {
        return operate(firstObj.getValue(context), new DeferredValue(secondObj, context));	
    }

    abstract public Value operate(Value firstVal, Value secondVal);
    
    /** Get the type of the result of this operation.  The default logic, which may if necessary be
     *  overridden by a particular binary operator, is as follows:
     *  
     *    -- if either type is null or the default type, the result is the default type
     *  
     *    -- if one of the types is an array, the result is an array; the base type of the result is
     *       determined by recursively calling this method with the base type of these types
     *       
     *    -- if one of the types is a table, the result is a table; the base type of the result is
     *       determined by recursively calling this method with the base type of these types
     *       
     *    -- if one of the types is an array and the other is a table, the result is the default type
     *       
     *    -- if the types have a non-primitive common ancestor type, the result is that ancestor type
     *       
     *    -- if the primitive ancestors of the types are both numeric, the result is the primitive type
     *       with the greater precision
     *       
     *    -- if the primitive ancestors of the types are external types, the result is an external type
     *       referencing the common ancestor class of their external classes
     *       
     *    -- otherwise, the result is the default type
     *       
     *  
     * @param firstType
     * @param secondType
     * @param context
     * @return
     */
    
    public Type getResultType(Type firstType, Type secondType, Context context) {
//        if (firstType == null || firstType == DefaultType.TYPE || secondType == null || secondType == DefaultType.TYPE) {
//            return DefaultType.TYPE;
//        }
        
        if (firstType == null || secondType == null) {
            return DefaultType.TYPE;
        }

        Type firstArrayType = firstType.getArrayType();
        Type firstTableType = firstType.getTableType();
        Type secondArrayType = secondType.getArrayType();
        Type secondTableType = secondType.getTableType();
        
        boolean firstIsArray = (firstArrayType != null);
        boolean firstIsTable = (firstTableType != null);
        boolean secondIsArray = (secondArrayType != null);
        boolean secondIsTable = (secondTableType != null);

        if ((firstIsArray && secondIsTable) || (firstIsTable && secondIsArray)) {
            return DefaultType.TYPE;

        } else if (firstIsArray || secondIsArray || firstIsTable || secondIsTable) {
            // one or both of the types is an array or table.  Pick which of
            // the two types should determine the output type.  The rules:
            //
            //    -- if only one of the types is a collection, use that type.
            //    -- if both types are a collection, then figure out out which is the supertype.
            //    -- if they are incompatible types (neither is a supertype of or equal to the other), then 
            //       return the default type to indicate that we can't operate on them as collections, 
            //       because they are incompatible.

            Type determiningType = null;
            if ((firstIsArray || firstIsTable) && !(secondIsArray || secondIsTable)) {
                determiningType = firstType;
            } else if (!(firstIsArray || firstIsTable) && (secondIsArray || secondIsTable)) {
                determiningType = secondType;
            } else if (secondType.isTypeOf(firstType, context)) {
                determiningType = firstType;
            } else if (firstType.isTypeOf(secondType, context)) {
                determiningType = secondType;
            } else {
                return DefaultType.TYPE;
            }
            
            // If the determing type is a native collection type (as opposed to a subtype of a collection), then
            // create a new native collection type.  Otherwise just return the determining type.
            
            if (determiningType.getDims().size() > 0) {
                Type resultBaseType = getResultType(firstType.getBaseType(), secondType.getBaseType(), context);
                ArgumentList resultArgs;
                List<Dim> resultDims;
                if (determiningType == firstType) {
                    resultArgs = firstType.getArguments(context);
                } else {
                    resultArgs = secondType.getArguments(context);
                } 
                List<Dim> firstTypeDims = firstType.getDims();
                List<Dim> secondTypeDims = secondType.getDims();
                
                if (firstTypeDims.size() >= secondTypeDims.size()) {
                    resultDims = firstTypeDims;
                } else {
                    resultDims = secondTypeDims;
                }
                ComplexType resultType = new ComplexType(resultBaseType, resultDims, resultArgs);
                resultType.setOwner(getOwner());
                return resultType;

            } else {
                return determiningType;
            }
        }
        
        // if we got here, neither type is a collection
        if (firstType.isTypeOf(secondType, context)) {
            return secondType;
        } else if (secondType.isTypeOf(firstType, context)) {
            return firstType;
        } else {
            Class<?> firstClass = firstType.getTypeClass(context);
            Class<?> secondClass = secondType.getTypeClass(context);
            
            if (firstClass.isAssignableFrom(secondClass)) {
                return firstType;
            } else if (secondClass.isAssignableFrom(firstClass)) {
                return secondType;

            } else if (isNumericClass(firstClass) && isNumericClass(secondClass)) {

                if (getNumericClassRank(firstClass) >= getNumericClassRank(secondClass)) {
                    return firstType;
                } else {
                    return secondType;
                }
                
            } else if (isNumericClass(firstClass) || isNumericClass(secondClass)) {
                if (Character.class.isAssignableFrom(firstClass) || Character.TYPE.equals(firstClass)) {
                    return firstType;
                } else if (Character.class.isAssignableFrom(secondClass) || Character.TYPE.equals(secondClass)) {
                    return secondType;
                } 
            }
            return DefaultType.TYPE;
        }
    }

    protected Type getValueSourceType(Object obj, Context context, boolean generate) {
        if (obj instanceof Construction) {
            return ((Construction) obj).getType(context, generate);
        } else if (obj instanceof PrimitiveValue) {
            return ((PrimitiveValue) obj).getType();
        } else {
            return DefaultType.TYPE;
        }
    }

    private static boolean isNumericClass(Class<?> c) {
        if (Number.class.isAssignableFrom(c)) {
            return true;
        } else if (c.isPrimitive() && !Boolean.TYPE.equals(c) && !Character.TYPE.equals(c)) {
            return true;
        } else {
            return false;
        }
    }
    
    private static int getNumericClassRank(Class<?> c) {
        
        if (BigDecimal.class.equals(c)) {
            return 8;
        } else if (Double.class.equals(c) || Double.TYPE.equals(c)) {
            return 7;
        } else if (Float.class.equals(c) || Float.TYPE.equals(c)) {
            return 6;
        } else if (BigInteger.class.equals(c)) {
            return 5;
        } else if (Long.class.equals(c) || Long.TYPE.equals(c)) {
            return 4;
        } else if (Integer.class.equals(c) || Integer.TYPE.equals(c)) {
            return 3;
        } else if (Short.class.equals(c) || Short.TYPE.equals(c)) {
            return 2;
        } else if (Byte.class.equals(c) || Byte.TYPE.equals(c)) {
            return 1;
        } else if (isNumericClass(c)) {  // generic Number or unknown Number subclass
            return 0;
        } else {
            return -1;
        }
    }

    /** a class to allow deferring the instantiation of an operand, in case
     *  it's not necessary, such as the second operand of a logical and expression,
     *  when the first operand evaluates to false.
     *  
     *  The deferred value will be valid only if instantiated while its definition
     *  is valid in the context passed to the constructor, i.e. the context is not
     *  cloned.
     */

    static class DeferredValue implements Value {
        private Value val = null;
        private ValueGenerator valGen = null;
        private Context context = null;
        private Object marker = null;
        private boolean generated = false;

        public DeferredValue(ValueSource valSource, Context context) {
            if (valSource instanceof Value) {
                val = (Value) valSource;
            } else {
                valGen = (ValueGenerator) valSource;
                this.context = context;
                marker = context.getMarker();
            }
        }

        /** ValueSource interface method; returns this Value. **/
        public Value getValue(Context context) throws Redirection {
        	return this;
        }
        	         
        private Value generateValue() {
            if (valGen == null || generated) {
                return val;
            }
            
            if (context == null) {
                throw new NullPointerException("null context for operator");
            } else if (!context.equals(marker)) {
                throw new IllegalStateException("context in illegal state for operator"); 
            }
            try {
                // this is to avoid trying to generate later,
                // which can spark the IllegalStateException above.
                generated = true;
                return valGen.getValue(context);
            } catch (Redirection r) {
                return null;
            }
        }
        
        public String getString() {
            if (val == null) {
                val = generateValue();
                if (val == null) {
                    return null;
                }
            }
            return val.getString();
        }
        
        
        public boolean getBoolean() {
            if (val == null) {
                val = generateValue();
                if (val == null) {
                    return false;
                }
            }
            return val.getBoolean();
        }


        public byte getByte() {
            if (val == null) {
                val = generateValue();
                if (val == null) {
                    return 0;
                }
            }
            return val.getByte();
        }


        public char getChar() {
            if (val == null) {
                val = generateValue();
                if (val == null) {
                    return 0;
                }
            }
            return val.getChar();
        }

        public int getInt() {
            if (val == null) {
                val = generateValue();
                if (val == null) {
                    return 0;
                }
            }
            return val.getInt();
        }


        public long getLong() {
            if (val == null) {
                val = generateValue();
                if (val == null) {
                    return 0;
                }
            }
            return val.getLong();
        }


        public double getDouble() {
            if (val == null) {
                val = generateValue();
                if (val == null) {
                    return 0;
                }
            }
            return val.getDouble();
        }


        public Object getValue() {
            if (val == null) {
                val = generateValue();
                if (val == null) {
                    return null;
                }
            }
            return val.getValue();
        }

        public Class<?> getValueClass() {
            if (val == null) {
                val = generateValue();
                if (val == null) {
                    return null;
                }
            }
            return val.getValueClass();
        }
        
    }

}
