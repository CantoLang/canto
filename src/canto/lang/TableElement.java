/* Canto Compiler and Runtime Engine
 * 
 * TableElement.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
 * A TableElement is a key-object pair representing an entry in a table.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.14 $
 */
public class TableElement extends ElementDefinition implements Construction {

    public final static Value DEFAULT_KEY = new NullValue(NullValue.LITERAL_STRING);
    public final static Value UNINITIALIZED_KEY = new NullValue(NullValue.DYNAMIC_BLOCK);

    private Value key = UNINITIALIZED_KEY;
    private ValueGenerator dynamicKey = null;

    public TableElement() {
        super();
    }
    
    public TableElement(Definition owner, Value key, Object element) {
        super(owner, element);
        setKey(key);
    }

    /** Returns <code>true</code> if the key is dynamic, else <code>false</code>. */
    public boolean isDynamic() {
        return (dynamicKey != null);
    }

    protected void setKey(Value key) {
        if (key != null) {
            this.key = key;
        } else {
            this.key = DEFAULT_KEY;
        }
    }

    protected void setDynamicKey(ValueGenerator key) {
        this.dynamicKey = key;
    }

    public Value getDynamicKey(Context context) throws Redirection {
        return dynamicKey.getValue(context);
    }

    public Value getKey() {
        return key;
    }

    public Object getObject() {
        return getContents();
    }

    /** Returns <code>false</code> */
    public boolean isPrimitive() {
        return false;
    }

    /** Returns <code>false</code> */
    public boolean isStatic() {
        return false;
    }

    /** Returns <code>false</code> */
    public boolean isDefinition() {
        return false;
    }

    public String getDefinitionName() {
        return super.getName();
    }

    public Type getType(Context context, boolean generate) {
        return super.getType();
    }

    public boolean getBoolean(Context context) throws Redirection {
        Object data = null;
        try {
            data = getData(context);
        } catch (Redirection r) {
            return false;
        }
        return AbstractConstruction.booleanValueOf(data);
    }

    public Object getData(Context context) throws Redirection {
        return getData(context, null);
    }

    public Object getData(Context context, Definition def) throws Redirection {
        Object element = getElement(context);
        while (element instanceof Construction) {
            element = ((Construction) element).getData(context);
            
        }
        if (element instanceof Value) {
            element = ((Value) element).getValue();
        }
        return element;
    }

    public String getText(Context context) throws Redirection {
        return getData(context).toString();
    }

    /** Return the construction that this choice resolves to.
     */
    public Construction getUltimateConstruction(Context context) {
        Object element = getElement(context);
        if (element instanceof Construction) {
            return (Construction) element;
        }
        return this;
    }
    
    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(prefix);
        
        Object element = getElement();

        sb.append(getKey().toString());
        sb.append(": ");

        if (element instanceof AbstractNode) {
            sb.append(((AbstractNode) element).toString(""));
        } else {
            sb.append(element.toString());
        }
        sb.append('\n');
        return sb.toString();
    }

	public String getString(Context context) throws Redirection {
		return getText(context);
	}

	public byte getByte(Context context) throws Redirection {
        Object data = null;
        try {
            data = getData(context);
        } catch (Redirection r) {
            return 0;
        }
        return (byte) PrimitiveValue.getIntFor(data);
	}

	public char getChar(Context context) throws Redirection {
        Object data = null;
        try {
            data = getData(context);
        } catch (Redirection r) {
            return 0;
        }
        return PrimitiveValue.getCharFor(data);
	}

	public int getInt(Context context) throws Redirection {
        Object data = null;
        try {
            data = getData(context);
        } catch (Redirection r) {
            return 0;
        }
        return PrimitiveValue.getIntFor(data);
	}

	public long getLong(Context context) throws Redirection {
        Object data = null;
        try {
            data = getData(context);
        } catch (Redirection r) {
            return 0;
        }
        return PrimitiveValue.getLongFor(data);
	}

	public double getDouble(Context context) throws Redirection {
        Object data = null;
        try {
            data = getData(context);
        } catch (Redirection r) {
            return 0;
        }
        return PrimitiveValue.getDoubleFor(data);
	}

	public Value getValue(Context context) throws Redirection {
        Object element = getElement(context);
        while (element instanceof Construction && !(element instanceof Value)) {
            element = ((Construction) element).getData(context);
        }
        if (element instanceof Value) {
            return (Value) element;
        } else {
            return new PrimitiveValue(element);
        }
	}
}
