/* Canto Compiler and Runtime Engine
 * 
 * NullValue.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
 * Null value.  There are four kinds of null values in Canto: null primitive,
 * which is similar to null in Java, null static block, null dynamic block and
 * null abstract block.  The difference is primarily syntactical (how the value
 * is expressed and where it may appear), but if necessary nulls of various
 * types may be distinguished via the getType method.
 *
 * @author Michael St. Hippolyte
 */

public class NullValue extends Block implements Value {

    public final static int PRIMITIVE = 0;
    public final static int LITERAL_STRING = 1;
    public final static int STATIC_BLOCK = 2;
    public final static int DYNAMIC_BLOCK = 3;
    public final static int ABSTRACT_BLOCK = 4;
    public final static int EXTERNAL_BLOCK = 5;
    
    public final static NullValue NULL_VALUE = new NullValue();

    private int type;

    public NullValue() {
        type = PRIMITIVE;
    }

    public NullValue(int type) {
        this.type = type;
    }

    /** Returns <code>true</code> */
    public boolean isPrimitive() {
        return true;
    }

    /** Returns <code>true</code> if this is a null static block,
     *  <code>false</code> otherwise.
     */
    public boolean isStatic() {
        return (type <= STATIC_BLOCK);
    }

    /** Returns <code>true</code> if this is a null dynamic block,
     *  <code>false</code> otherwise.
     */
    public boolean isDynamic() {
        return (type >= DYNAMIC_BLOCK);
    }

    /** Returns <code>true</code> if this is an abstract block,
     *  <code>false</code> otherwise.
     */
    public boolean isAbstract(Context context) {
        return (type == ABSTRACT_BLOCK);
    }

    /** Returns <code>true</code> if this is an external block,
     *  <code>false</code> otherwise.
     */
    public boolean isExternal() {
        return (type == EXTERNAL_BLOCK);
    }

    /** Returns <code>false</code> */
    public boolean isDefinition() {
        return false;
    }

    protected void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public Class<?> getValueClass() {
        switch (type) {
            case LITERAL_STRING:
                return String.class;
            case STATIC_BLOCK:
                return StaticBlock.class;
            case DYNAMIC_BLOCK:
                return CantoBlock.class;
            default:
                return null;
        }
    }

    /** ValueSource interface method; returns this Value. **/
    public Value getValue(Context context) throws Redirection {
    	return this;
    }
    	         
    public Object getValue() {
        return null;
    }

    public String getString() {
        return "";
    }

    public boolean getBoolean() {
        return false;
    }

    public byte getByte() {
        return (byte) 0;
    }

    public char getChar() {
        return (char) 0;
    }

    public int getInt() {
        return 0;
    }

    public long getLong() {
        return 0L;
    }

    public float getFloat() {
        return 0.0f;
    }

    public double getDouble() {
        return 0.0;
    }

    /** Return the construction that this construction resolves to, if it
     *  is a wrapper or alias of some sort, or else return this construction.
     *  This class is not a wrapper or alias, so it returns this construction.
     */
    public Construction getUltimateConstruction(Context context) {
        return this;
    }

    public String toString(String prefix) {
        switch (type) {
            case LITERAL_STRING:
                return "\"\"";  // ""
            case STATIC_BLOCK:
                return "[/]";
            case DYNAMIC_BLOCK:
                return "[\\]";
            case ABSTRACT_BLOCK:
                return "[?]";
            case EXTERNAL_BLOCK:
                return "[&]";
            default:
                return "null";
        }
    }
}
