/* Canto Compiler and Runtime Engine
 * 
 * TypeDefinition.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
 * TypeDefinition is a definition corresponding to the Canto <code>type</code> keyword.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.10 $
 */

public class TypeDefinition extends NamedDefinition {

    public Definition def;
    public String contextName;

    public TypeDefinition(Definition def) {
        super();
        this.def = def;
        setContents(new PrimitiveValue(def.getName()));
    }

    /** Returns <code>false</code>.
     */
    public boolean isAbstract(Context context) {
        return false;
    }

    /** Returns <code>PUBLIC_ACCESS</code>. */
    public int getAccess() {
        return PUBLIC_ACCESS;
    }

    /** Returns <code>DYNAMIC</code>. */
    public int getDurability() {
        return DYNAMIC;
    }

    /** Returns the default type. */
    public Type getType() {
        return DefaultType.TYPE;
    }

    /** Returns null. */
    public Type getSuper() {
        return null;
    }

    /** Returns false. */
    public boolean isSuperType(String name) {
        return false;
    }

    /** Returns false. */
    public boolean isAnonymous() {
        return false;
    }
    
    /** Returns the encapsulated definition's full name.
     */
    public String getFullName() {
        return def.getFullName();
    }

    /** Returns the encapsulated definition's full name in context.
     */
    public String getFullNameInContext(Context context) {
        return def.getFullNameInContext(context);
    }

    public String getName() {
        if (def instanceof DefParameter) {
            String name = ((DefParameter) def).getReferenceName();
            if ("sub".equals(name)) {
                return contextName;
            } else {
                return name;
            }
        } else {
            return def.getName();
        }
    }

    public NameNode getNameNode() {
    	return def.getNameNode();
    }
    
    /** Returns the encapsulated definition. */
    public Definition getOwner() {
        return def;
    }
}
