/* Canto Compiler and Runtime Engine
 * 
 * PrimitiveType.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.ArrayList;
import java.util.List;

import canto.runtime.Context;

/**
 * A built-in type name.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.24 $
 */

public class PrimitiveType extends AbstractType {

    /** Shared primitive types */
    public final static Type BOOLEAN = new PrimitiveType(Boolean.TYPE);
    public final static Type BYTE = new PrimitiveType(Byte.TYPE);
    public final static Type CHAR = new PrimitiveType(Character.TYPE);
    public final static Type DOUBLE = new PrimitiveType(Double.TYPE);
    public final static Type FLOAT = new PrimitiveType(Float.TYPE);
    public final static Type INT = new PrimitiveType(Integer.TYPE);
    public final static Type LONG = new PrimitiveType(Long.TYPE);
    public final static Type SHORT = new PrimitiveType(Short.TYPE);
    public final static Type STRING = new PrimitiveType(String.class);
    public final static Type VOID = new PrimitiveType(Void.TYPE);

    public Class<?> type;
    public List<Dim> dims;

    public PrimitiveType() {
        this(Void.TYPE);
        dims = new EmptyList<Dim>();
    }

    public PrimitiveType(Class<?> type) {
        super();
        setType(type);
        // doesn't handle multidimensional types yet
        Dim dim = Dim.createForClass(type);
        if (dim != null) {
            dims = new ArrayList<Dim>(1);
            dims.add(dim);
        } else {
            dims = new EmptyList<Dim>();
        }
    }

    public PrimitiveType(Class<?> type, List<Dim> dims) {
        super();
        setType(type);
        setDims(dims);
    }
    
    /** Returns <code>true</code> */
    public boolean isPrimitive() {
        return true;
    }

    protected void setType(Class<?> type) {
        this.type = type;
        if (type.equals(String.class)) {
            setName("string");

        } else {
            // this happens to work because Canto primitive types (other than
            // string) have the same name as the corresponding Java primitive types
            setName(type.getName());
        }
    }
    
    protected void setDims(List<Dim> dims) {
        this.dims = dims;
    }

    public List<Dim> getDims() {
        return dims;
    }

    public ArgumentList getArguments(Context context) {
        return null;
    }

    public Value convert(Value val) {
        if (type.equals(Boolean.TYPE)) {
            return new PrimitiveValue(val.getBoolean());
        } else if (type.equals(Byte.TYPE)) {
            return new PrimitiveValue(val.getByte());
        } else if (type.equals(Character.TYPE)) {
            return new PrimitiveValue(val.getChar());
        } else if (type.equals(Integer.TYPE)) {
            return new PrimitiveValue(val.getInt());
        } else if (type.equals(Short.TYPE)) {
            return new PrimitiveValue((short) val.getInt());
        } else if (type.equals(Long.TYPE)) {
            return new PrimitiveValue(val.getLong());
        } else if (type.equals(Float.TYPE)) {
            return new PrimitiveValue(val.getDouble());
        } else if (type.equals(Double.TYPE)) {
            return new PrimitiveValue(val.getDouble());
        } else if (type.equals(Void.TYPE)) {
            return new PrimitiveValue();
        } else if (type.equals(String.class)) {
            return new PrimitiveValue(val.getString());
        } else {
            throw new IllegalArgumentException("Conversion failed; " + type.getName() + " is not a primitive type");
        }
    }

    public Class<?> getTypeClass(Context context) {
        return type;
    }

    public int levelsBelow(Type type, Context context) {
        if (type == null) {
            return -1;
        } else if (this.equals(type)) {
            return 0;
        }

        Class<?> typeClass = null;
        if (type instanceof PrimitiveType) {
            typeClass = ((PrimitiveType) type).getTypeClass(context);

        } else {
            Definition def = type.getDefinition();
            if (def instanceof ExternalDefinition) {
                typeClass = ((ExternalDefinition) def).getExternalClass(context);
            }
        }

        if (typeClass == null) {
            return -1;
        }

        Class<?> thisClass = getTypeClass(context);
        int levels = 0;
        for (Class<?> c = thisClass; c != null; c = c.getSuperclass()) {
            if (c.equals(typeClass)) {
                return levels;
            // see if a widening conversion can handle it 
            } else if (isWidenableFrom(typeClass, c)) {
        	    return levels + 1;

            // or a narrowing conversion
            } else if (isNarrowableFrom(typeClass, c)) {
                return levels + 2;
            
            // or a lossy conversion
            } else if (isApproximableTo(typeClass, c)) {
                return levels + 3;

            } 
            levels++;
        }
        if (typeClass.isAssignableFrom(thisClass)) {
            return levels;
        } else if (thisClass.equals(Boolean.TYPE) || thisClass.equals(String.class)) {
            return levels * Definition.PARAM_DEFAULT;
        }

        return -1;
    }

    /** Returns true if an instance of classFrom can be assigned to an instance of classTo through
     *  a widening conversion, e.g. from int to long, or float to double.
     */
    private static boolean isWidenableFrom(Class<?> classTo, Class<?> classFrom) {
        if (classTo.equals(Integer.TYPE) &&
             (classFrom.equals(Short.TYPE)
           || classFrom.equals(Character.TYPE))) {
                  
            return true;

        } else if (classTo.equals(Long.TYPE) &&
                (classFrom.equals(Integer.TYPE)
              || classFrom.equals(Short.TYPE)
              || classFrom.equals(Character.TYPE))) {
              
            return true;
    			
        } else if (classTo.equals(Double.TYPE) &&
              (classFrom.equals(Float.TYPE)
            || classFrom.equals(Integer.TYPE)
            || classFrom.equals(Short.TYPE)
            || classFrom.equals(Character.TYPE))) {
                     
            return true;

        } else if (classTo.equals(Float.TYPE) &&
                (classFrom.equals(Integer.TYPE)
              || classFrom.equals(Short.TYPE)
              || classFrom.equals(Character.TYPE))) {
                       
              return true;
        }
    	return false;
    }
    
    
    /** Returns true if an instance of classFrom can be assigned to an instance of classTo through
     *  a narrowing conversion, e.g. from long to int, or double to float.
     */
    private static boolean isNarrowableFrom(Class<?> classTo, Class<?> classFrom) {
        if (classTo.equals(Integer.TYPE) && classFrom.equals(Long.TYPE)) {
            return true;

        } else if ((classTo.equals(Short.TYPE) || classTo.equals(Character.TYPE)) &&
                   (classFrom.equals(Integer.TYPE) || classFrom.equals(Long.TYPE))) {
            return true;
                
        } else if (classTo.equals(Float.TYPE) && classFrom.equals(Double.TYPE)) {
            return true;

        }
        return false;
    }

    /** Returns true if an instance of classFrom can be assigned to an instance of classTo through
     *  a conversion involving a loss of precision, e.g. from float to int.
     */
    private static boolean isApproximableTo(Class<?> classFrom, Class<?> classTo) {
        if ((classTo.equals(Integer.TYPE) || classTo.equals(Short.TYPE) || classTo.equals(Character.TYPE) || classTo.equals(Long.TYPE))
         && (classFrom.equals(Double.TYPE) || classFrom.equals(Float.TYPE))) {
            return true;
        }
        return false;
    }

    public Definition getDefinition() {
        return new PrimitiveDefinition();
    }

    class PrimitiveDefinition extends NamedDefinition {

        public PrimitiveDefinition() {
            super();
            // set the owner to the top-level owner in the node tree, which
            // would presumably be the core.
            Definition owner = PrimitiveType.this.getOwner();
            if (owner != null) {
                while (owner.getOwner() != null) {
                    owner = owner.getOwner();
                }
                setOwner(owner);
            }
        }

        /** Returns <code>false</code>.
         */
        public boolean isAbstract(Context context) {
            return false;
        }

        /** Returns <code>true</code>.
         */
        public boolean isPrimitive() {
            return true;
        }

        /** Returns <code>PUBLIC_ACCESS</code>. */
        public int getAccess() {
            return PUBLIC_ACCESS;
        }

        /** Returns <code>STATIC</code>. */
        public int getDurability() {
            return STATIC;
        }

        /** Returns the associated PrimitiveType object. */
        public Type getType() {
            return PrimitiveType.this;
        }

        /** Returns null. */
        public Type getSuper() {
            return null;
        }


        /** Returns false.
         */
        public boolean isSuperType(String name) {
            return false;
        }

        /** Returns the full name, which for primitives is the same as
         *  the short name.
         */
        public String getFullName() {
            return getName();
        }

        public String getName() {
            return PrimitiveType.this.getName();
        }

        /** Returns <code>false</code>.
         */
        public boolean hasSub() {
            return false;
        }
    }
}
