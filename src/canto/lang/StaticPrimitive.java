/* Canto Compiler and Runtime Engine
 * 
 * StaticPrimitive.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
 * A StaticPrimitive is a chunk containing no children or
 * dynamic data, only static data.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.13 $
 */

public class StaticPrimitive extends AbstractNode implements Construction {

    private Object data = null;
    private Type type = null;

    public StaticPrimitive() {
        super();
    }

    public StaticPrimitive(Object data) {
        this.data = data;
    }

    /** Returns <code>true</code> */
    final public boolean isPrimitive() {
        return true;
    }

    /** Returns <code>true</code> */
    final public boolean isStatic() {
        return true;
    }

    /** Returns <code>false</code> */
    final public boolean isDynamic() {
        return false;
    }

    /** Returns <code>false</code> */
    final public boolean isDefinition() {
        return false;
    }

    public boolean getBoolean(Context context) throws Redirection {
        return PrimitiveValue.getBooleanFor(data);
    }

    public String getText(Context context) throws Redirection {
        return getText();
    }

    public Object getData(Context context) throws Redirection {
        return data;
    }

    public Object getData(Context context, Definition def) throws Redirection {
        return data;
    }

    public boolean isAbstract(Context context) {
        return false;
    }
    
    public String getText() {
        if (data == null) {
            return null;
        } else if (data instanceof String) {
            return (String) data;
        } else if (data instanceof StringReference) {
            StringReference stref = (StringReference) data;
            return stref.getString();
        } else if (data instanceof Value) {
            Value val = (Value) data;
            return val.getString();
        } else {
            return data.toString();
        }
    }

    /** Gets the length of this element */
    public int getLength() {
        if (data instanceof String) {
            String string = (String) data;
            return string.length();
        } else if (data instanceof StringReference) {
            StringReference stref = (StringReference) data;
            return stref.getLength();
        } else if (data instanceof Value) {
            Value val = (Value) data;
            return val.getString().length();
        } else {
            return data.toString().length();
        }
    }

    /** Returns the type of this construction in the specified context. */
    public Type getType(Context context, boolean generate) {
        if (type == null) {
            type = new PrimitiveType(data.getClass());
        }
        return type; 
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
        return getText() + '\n';
    }

	public String getString(Context context) throws Redirection {
		return getText();
	}

	public byte getByte(Context context) throws Redirection {
        return (byte) PrimitiveValue.getIntFor(data);
	}

	public char getChar(Context context) throws Redirection {
        return PrimitiveValue.getCharFor(data);
	}

	public int getInt(Context context) throws Redirection {
        return PrimitiveValue.getIntFor(data);
	}

	public long getLong(Context context) throws Redirection {
        return PrimitiveValue.getLongFor(data);
	}

	public double getDouble(Context context) throws Redirection {
        return PrimitiveValue.getDoubleFor(data);
	}

	public Value getValue(Context context) throws Redirection {
        return new PrimitiveValue(data);
	}
}

