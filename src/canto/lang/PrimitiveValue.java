/* Canto Compiler and Runtime Engine
 * 
 * PrimitiveValue.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import canto.runtime.Context;
import canto.runtime.CantoObjectWrapper;

import java.lang.reflect.Array;


/**
 * Value implementation for simple values.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.38 $
 */

public class PrimitiveValue extends AbstractNode implements Construction, Value {

    private Object value;
    private Class<?> nativeClass;

    public PrimitiveValue() {
        this(null, Void.TYPE);
    }

    public PrimitiveValue(Object value) {
        this.value = value;
        if (value instanceof Value) {
            this.value = ((Value) value).getValue();
            nativeClass = ((Value) value).getValueClass();
        } else if (value instanceof Boolean) {
            nativeClass = Boolean.TYPE;
        } else if (value instanceof Byte) {
            nativeClass = Byte.TYPE;
        } else if (value instanceof Character) {
            nativeClass = Character.TYPE;
        } else if (value instanceof Double || value instanceof Float) {
            nativeClass = Double.TYPE;
        } else if (value instanceof Integer || value instanceof Character || value instanceof Short) {
            nativeClass = Integer.TYPE;
        } else if (value instanceof Long) {
            nativeClass = Long.TYPE;
        } else if (value == null) {
            nativeClass = Void.TYPE;
        } else {
            nativeClass = value.getClass(); // String.class;
        }
        
//        if (value instanceof Definition) {
//System.out.println("Primitivizing a Definition!!! in PrimitiveValue 60");
//        }
    }

    public PrimitiveValue(Object value, Class<?> valueClass) {
        Object obj = (value instanceof Value ? ((Value) value).getValue() : value);
        if (valueClass == null || valueClass == Void.TYPE) {
            nativeClass = Void.TYPE;
        } else if (valueClass.equals(Boolean.class) || valueClass.equals(Boolean.TYPE)) {
            if (!(obj instanceof Boolean)) {
                value = new Boolean(getBooleanFor(obj));
            }
            nativeClass = Boolean.TYPE;
        } else if (valueClass.equals(Byte.class) || valueClass.equals(Byte.TYPE)) {
            if (!(obj instanceof Byte)) {
                value = new Byte(getByteFor(obj));
            }
            nativeClass = Byte.TYPE;
        } else if (valueClass.equals(Character.class) || valueClass.equals(Character.TYPE)) {
            if (!(obj instanceof Character)) {
                value = new Character(getCharFor(obj));
            }
            nativeClass = Character.TYPE;
        } else if (valueClass.equals(Double.class) || valueClass.equals(Float.class)
                    || valueClass.equals(Double.TYPE) || valueClass.equals(Float.TYPE)) {
            if (!(obj instanceof Double) && !(obj instanceof Float)) {
                value = new Double(getDoubleFor(obj));
            }
            nativeClass = Double.TYPE;
        } else if (valueClass.equals(Integer.class) || valueClass.equals(Short.class)
                    || valueClass.equals(Integer.TYPE) || valueClass.equals(Short.TYPE)) {
            if (!(obj instanceof Integer) && !(obj instanceof Short)) {
                value = new Integer(getIntFor(obj));
            }
            nativeClass = Integer.TYPE;
        } else if (valueClass.equals(Long.class) || valueClass.equals(Long.TYPE)) {
            if (!(obj instanceof Long)) {
                value = new Long(getLongFor(obj));
            }
            nativeClass = Long.TYPE;
        
        } else if (valueClass.equals(String.class) || valueClass.equals(Object.class) || valueClass.equals(Definition.class)) {
            nativeClass = valueClass;

        } else if (valueClass.isInstance(obj)) {
            nativeClass = valueClass;

        } else {
            throw new ClassCastException("Value is not a " + valueClass.getName());
        }
        
        this.value = value;
    }

    public PrimitiveValue(boolean booleanValue) {
        value = new Boolean(booleanValue);
        nativeClass = Boolean.TYPE;
    }

    public PrimitiveValue(byte byteValue) {
        value = new Byte(byteValue);
        nativeClass = Byte.TYPE;
    }

    public PrimitiveValue(char charValue) {
        value = new Character(charValue);
        nativeClass = Character.TYPE;
    }

    public PrimitiveValue(int intValue) {
        value = new Integer(intValue);
        nativeClass = Integer.TYPE;
    }

    public PrimitiveValue(long longValue) {
        value = new Long(longValue);
        nativeClass = Long.TYPE;
    }

    public PrimitiveValue(double doubleValue) {
        value = new Double(doubleValue);
        nativeClass = Double.TYPE;
    }

    /** Returns <code>true</code> */
    final public boolean isPrimitive() {
        return true;
    }

    /** Returns <code>true</code> */
    public boolean isStatic() {
        return true;
    }

    /** Returns <code>false</code> */
    public boolean isDynamic() {
        return false;
    }

    /** Returns <code>false</code> */
    public boolean isDefinition() {
        return false;
    }

    /** Chunk interface. */
    public boolean getBoolean(Context context) throws Redirection {
        return getBoolean();
    }

    public String getText(Context context) throws Redirection {
        return getString();
    }

    public Object getData(Context context) throws Redirection {
        return getValue();
    }

    public Object getData(Context context, Definition def) throws Redirection {
        return getValue();
    }

    public boolean isAbstract(Context context) {
        return false;
    }
    
    protected void setValueAndClass(Object value, Class<?> valueClass) {
        this.value = value;
        nativeClass = valueClass;
    }

    /** ValueSource interface method; returns this Value. **/
    public Value getValue(Context context) throws Redirection {
        return this;
    }
                 
    public Object getValue() {
        return value;
    }

    public Class<?> getValueClass() {
        return nativeClass;
    }

    public String getString() {
        Object value = getValue();
        if (value instanceof Byte) {
            int ival = ((Byte) value).intValue() & 0xFF;
            return Integer.toHexString(ival);
            
        } else {
            return getStringFor(value);
        }
    }

    public boolean getBoolean() {
        return getBooleanFor(getValue());
    }

    public byte getByte() {
        Object value = getValue();
        if (value == null) {
            return (byte) 0;
        } else if (value instanceof Number) {
            return ((Number) value).byteValue();
        } else if (value instanceof Character) {
            return (byte) ((Character) value).charValue();
        } else {
            String str = getStringFor(value);
            try {
                return Byte.parseByte(str);
            } catch (NumberFormatException pe) {
                return (byte) 0;
            }
        }
    }

    public int getInt() {
        Object value = getValue();
        if (value == null) {
            return 0;
        
        // treat bytes as unsigned
        } else if (value instanceof Byte) {
            return ((Byte) value).intValue() & 0xFF;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof Character) {
            return ((Character) value).charValue();
        } else {
            String str = getStringFor(value);
            if (str == null || str.length() == 0) {
                return 0;
            }
            if (str.charAt(0) == '#') {
                return hexValueOf(str.substring(1));
            }
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException pe) {
                return 0;
            }
        }
    }

    private static int hexValueOf(String str) {
        int value = 0;
        for (int i = 0; i < str.length(); i++) {
            value *= 16;

            char c = str.charAt(i);
            if (Character.isDigit(c)) {
                value += c - '0';
            } else if (c >= 'A' && c <= 'F') {
                value += c - 'A' + 10;
            } else if (c >= 'a' && c <= 'f') {
                value += c - 'a' + 10;
            } else {
                return 0;
            }
        }
        return value;
    }


    private static long longHexValueOf(String str) {
        long value = 0;
        for (int i = 0; i < str.length(); i++) {
            value *= 16;

            char c = str.charAt(i);
            if (Character.isDigit(c)) {
                value += c - '0';
            } else if (c >= 'A' && c <= 'F') {
                value += c - 'A' + 10;
            } else if (c >= 'a' && c <= 'f') {
                value += c - 'a' + 10;
            } else {
                return 0;
            }
        }
        return value;
    }

    public char getChar() {
        Object value = getValue();
        if (value == null) {
            return (char) 0;
        } else if (value instanceof Number) {
            return (char) ((Number) value).intValue();
        } else if (value instanceof Character) {
            return ((Character) value).charValue();
        } else {
            String str = getStringFor(value);
            if (str.length() == 0) {
                return (char) 0;
            } else {
                return str.charAt(0);
            }
        }
    }

    public long getLong() {
        Object value = getValue();
        if (value == null) {
            return 0L;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof Character) {
            return ((Character) value).charValue();
        } else {
            String str = getStringFor(value);
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException pe) {
                return 0L;
            }
        }
    }


    public double getDouble() {
        Object value = getValue();
        if (value == null) {
            return 0.0;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof Character) {
            return ((Character) value).charValue();
        } else {
            String str = getStringFor(value);
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException pe) {
                return 0.0;
            }
        }
    }

    public Type getType() {
        // doesn't handle multidimensional objects yet
        List<Dim> dims = null;
        Dim dim = Dim.createForObject(value);
        if (dim != null) {
            dims = new ArrayList<Dim>(1);
            dims.add(dim);
        } else {
            dims = new EmptyList<Dim>();
        }
        return new PrimitiveType(nativeClass, dims);
    }

    /** Returns the type of this construction in the specified context. */
    public Type getType(Context context, boolean generate) {
        return getType(); 
    }

    /** Returns the name of the definition being constructed */
    public String getDefinitionName() {
        return null;
    }

    /** Return the construction that this construction resolves to, if it
     *  is a wrapper or alias of some sort, or else return this construction.
     *  This class is not a wrapper or alias, so it returns this construction.
     */
    public Construction getUltimateConstruction(Context context) {
        return this;
    }

    public String toString() {
        return getString();
    }
    
    public String toString(String indent) {
        String str = getString();
        if (!Number.class.isAssignableFrom(nativeClass) && (!nativeClass.isPrimitive() || nativeClass.equals(Character.TYPE))) {
            str = '"' + str + '"';
        }
        return indent + str;
    }

    // static value extraction methods

    /** Returns the string interpretation of a value. */
    public static String getStringFor(Object value) {
        if (value == null) {
            return "";
        } else if (value instanceof String) {
            return (String) value;
        } else if (value instanceof StringReference) {
            StringReference stref = (StringReference) value;
            return stref.getString();
        } else if (value instanceof Object[] || value instanceof Collection<?> || value instanceof CantoArray) {
            
            if (value instanceof CantoArray) {
                value = ((CantoArray) value).getArrayObject();
                if (value == null) {
                    return "";
                }
            }
            if (value instanceof Object[]) {
                Object[] objects = (Object[]) value;
                if (objects.length == 0) {
                    return "[]";
                } else {
                    String str = "[" + getRecursiveStringFor(objects[0]);
                    for (int i = 1; i < objects.length; i++) {
                        str = str + "," + getRecursiveStringFor(objects[i]);
                    }
                    str = str + "]";
                    return str;
                }
            } else if (value instanceof Collection<?>) {
                String str = "[";
                Iterator<?> it = ((Collection<?>) value).iterator();
                while (it.hasNext()) {
                    Object obj = it.next();
                    str = str + getRecursiveStringFor(obj);
                    if (it.hasNext()) {
                        str = str + ",";
                    }
                }
                str = str + "]";
                return str;
            }
        } else if (value instanceof Name) {
            String name = ((Name) value).getName();
            return (name == null ?  "" : name);
        } else if (value instanceof Value) {
            String str = ((Value) value).getString();
            return (str == null ? "" : str);
        } else if (value instanceof CantoObjectWrapper) {
            String str = "";
            try {
                str = ((CantoObjectWrapper) value).getText();
            } catch (Redirection r) {
                ;
            }
            return (str == null ? "" : str);
        } else if (value instanceof ResolvedInstance) {
            String str = ((ResolvedInstance) value).getString();
            return (str == null ? "" : str);
        }
        // didn't fall into any of the above categories
        String str = value.toString();
        return (str == null ? "" : str);
    }

    private static String getRecursiveStringFor(Object obj) {
        String str = getStringFor(obj);
        return str;
    }
    
    public static boolean getBooleanFor(Object value) {
        if (value == null) {
            return false;
        } else if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        } else if (value instanceof Number) {
            return (((Number) value).intValue() != 0);
        } else if (value instanceof Collection<?>) {
            return (((Collection<?>) value).size() > 0);
        } else if (value.getClass().isArray()) {
            return (Array.getLength(value) > 0);
        } else if (value instanceof Map<?, ?>) {
            return (((Map<?, ?>) value).size() > 0);
        } else if (value instanceof CantoObjectWrapper) {
            return (((CantoObjectWrapper) value).getConstruction() != null);
        } else {
            try {
                 return (getStringFor(value).length() > 0);
            } catch (Exception e) {
                 return false;
            }
        }
    }

    /** Returns the byte interpretation of a value **/
    public static byte getByteFor(Object value) throws NumberFormatException {
        if (value == null) {
            return 0;
        } else if (value instanceof Number) {
            return ((Number) value).byteValue();
        } else {
            String str = value.toString();
            if (str.charAt(0) == '#') {
                return (byte) hexValueOf(str.substring(1));
            } else {
                return Byte.parseByte(str);
            }
        }
    }
    
    
    /** Returns the integer interpretation of a value. */
    public static int getIntFor(Object value) throws NumberFormatException {
        if (value == null) {
            return 0;
            
        // treat bytes as unsigned
        } else if (value instanceof Byte) {
            return ((Number) value).intValue() & 0xFF;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else {
            String str = value.toString();
            if (str.charAt(0) == '#') {
                return hexValueOf(str.substring(1));
            } else {
                return Integer.parseInt(str);
            }
        }
    }

    /** Returns the long interpretation of a value. */
    public static long getLongFor(Object value) throws NumberFormatException {
        if (value == null) {
            return 0;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else {
            String str = value.toString();
            if (str.charAt(0) == '#') {
                return longHexValueOf(str.substring(1));
            } else {
                return Long.parseLong(str);
            }
        }
    }

    /** Returns the double precision floating point interpretation of a value. */
    public static double getDoubleFor(Object value) throws NumberFormatException {
        if (value == null) {
            return 0.0;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            String str = value.toString();
            return Double.parseDouble(str);
        }
    }


    /** Returns the char interpretation of a value. */
    public static char getCharFor(Object value) throws NumberFormatException {
        if (value == null) {
            return 0;
        } else if (value instanceof Character) {
            return ((Character) value).charValue();
        } else if (value instanceof Number) {
            return (char) ((Number) value).intValue();
        } else {
            String str = value.toString();
            if (str == null || str.length() == 0) {
                return 0;
            } else {
                return str.charAt(0);
            }
        }
    }

    public String getString(Context context) throws Redirection {
        return getStringFor(value);
    }

    public byte getByte(Context context) throws Redirection {
        return (byte) getIntFor(value);
    }

    public char getChar(Context context) throws Redirection {
        return getCharFor(value);
    }

    public int getInt(Context context) throws Redirection {
        return getIntFor(value);
    }

    public long getLong(Context context) throws Redirection {
        return getLongFor(value);
    }

    public double getDouble(Context context) throws Redirection {
        return getDoubleFor(value);
    }

}
