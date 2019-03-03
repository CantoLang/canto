/* Canto Compiler and Runtime Engine
 * 
 * NameNode.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;

/**
 * NameNode is the base class of Nodes which represent names (including type
 * names).  It holds a single identifier token; subclasses represent more
 * complex names.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.21 $
 */
public class NameNode extends AbstractNode implements Name {

    public static NameNode ANY = new NameNode("*");
    
    private String name;
    
    /** if the name has parts (i.e. contains a dot), parse the name just once
     *  and cache the parts. 
     */
    private String[] parts = null;

    /** For optimization, the base class has a place for caching the name; subclasses can put a value here to
     *  avoid recalculating the name on each call.
     */ 
    private String cachedName = null;

    public NameNode() {
        super();
    }

    public NameNode(String name) {
        super();
        setName(name);
    }

    public String getName() {
        return cachedName;
    }

    public void setName(String name) {
        this.name = name;
        cachedName = stripDelims(name);
        parts = cachedName.split("\\.");
    }
    
    /** Returns <code>true</code> */
    public boolean isPrimitive() {
        return true;
    }

    /** Returns <code>false</code> */
    public boolean isStatic() {
        return false;
    }

    /** Returns <code>true</code> if the name has dynamic arguments in any
     *  part of the name, else <code>false</code>.
     */
    public boolean isDynamic() {
        return (hasArguments() && getArguments().isDynamic());
    }

    /** Returns <code>false</code> */
    public boolean isDefinition() {
        return false;
    }

    /** Returns <code>true</code> if this is a complex name, i.e., if its children
     *  are names.
     */
    public boolean isComplex() {
        return false;
    }

    /** Returns the number of parts in this name.  The base class always returns 1. */
    public int numParts() {
        return parts.length;
    }
    
    /** Returns the first part of the name.  The base class simply returns the name. */
    public NameNode getFirstPart() {
        return this;
    }
    
    /** Returns the last part of the name.  The base class simply returns the name. */
    public NameNode getLastPart() {
        return this;
    }

    /** Returns the nth part of the name.  The base class returns the name if n is zero,
     *  else throws an IndexOutOfBounds exception.
     */
    public NameNode getPart(int n) {
        if (n == 0 && (parts == null || parts.length <= 1)) {
            return this;
        } else if (n < parts.length) {
        	return new NameNode(parts[n]);
        } else {
            throw new IndexOutOfBoundsException("name does not have part " + n);
        }
    }

    /** Returns <code>true</code> if this is a special name.
     */
    public boolean isSpecial() {
        return isSpecialName(getName());
    }        
    
    public static boolean isSpecialName(String name) {
        if (name == DEFAULT || name == CONTAINER || name == COUNT || name == CORE
        		|| name == KEYS || name == HERE || name == OWNER
        		|| name == SITE || name == SOURCE || name == SUB || name == SUPER
        		|| name == THIS || name == TYPE) {
            return true;
        } else {
            return false;
        }
    }


    /** Returns <code>true</code> if this name has one or more constraints, else false.  The base
     *  class returns false. */
    public boolean hasConstraints() {
        return false;
    }
    
    /** Returns the list of constraints associated with this name.  The base class
     *  always returns null.
     */
    public List<CantoNode> getConstraints() {
        return null;
    }

    
    /** Returns <code>true</code> if this name has one or more arguments, else false. */
    public boolean hasArguments() {
        return false;
    }

    /** Returns the list of arguments associated with this name.  The base
     *  class always returns null.
     */
    public ArgumentList getArguments() {
        return null;
    }

    /** Returns true if this name has one or more indexes, else false. */
    public boolean hasIndexes() {
        return false;
    }

    /** Returns the list of indexes associated with this name.  The base class
     *  always returns null.
     */
    public List<Index> getIndexes() {
        return null;
    }

    /** Returns the list of dimensions associated with this name.  The base class
     *  always returns null.
     */
    public List<Dim> getDims() {
        return null;
    }

    public String toString(String prefix) {
        return name;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Name) {
            String thisName = getName();
            String otherName = ((Name) obj).getName();
            if (thisName == null) {
                return (otherName == null);
            } else {
                return thisName.equals(otherName);
            }
        }
        return false;
    }
    
    private static String stripDelims(String str) {
        if (str != null) {
            int len = str.length();
            if (len > 1) {
                char a = str.charAt(0);
                char b = str.charAt(len - 1);
                if (a == b && (a == '"' || a == '\'' || a == '`')) {
                    str = str.substring(1, len - 1);
                }
            }
        }
        return str;
    }
}
