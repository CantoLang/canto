/* Canto Compiler and Runtime Engine
 * 
 * CantoNode.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

/**
 * The CantoNode interface is implemented by all canto objects.  CantoNodes are
 * either primitive or complex; a primitive node contains simple data or logic,
 * while a complex node contains other nodes.  CantoNodes may also belong to one
 * of three mutually exclusive special categories: static, meaning they contain
 * unchanging data; dynamic, meaning they generate data; and definitions, meaning
 * they are employed by other nodes to generate runtime data.  Some nodes belong
 * to none of the three categories, meaning they neither contain nor generate data,
 * nor are referenced directly by nodes which do.
 *
 * @author Michael St. Hippolyte
 */
public interface CantoNode {

    /** If true, this node cannot have children. */
    public boolean isPrimitive();

    /** If true, this chunk represents static information. */
    public boolean isStatic();

    /** If true, this chunk is dynamically generated at runtime. */
    public boolean isDynamic();

    /** If true, this node�is a definition. */
    public boolean isDefinition();

    /** Returns the nth child node of this node */
    public CantoNode getChild(int n);

    /** Returns an iterator over the child nodes of this node. */
    public Iterator<CantoNode> getChildren();

    /** Returns the number of child nodes belonging to this node. */
    public int getNumChildren();

    /** Returns this node's parent, or null if this is the root node. */
    public CantoNode getParent();
    
    /** Returns the next child after this node in the parent's child nodes. */
    public CantoNode getNextSibling();
    
    /** Returns the owner, which is the Definition in whose namespace this
     *  definition is in.
     */
    public Definition getOwner();

    /** Returns this node's name.  Not every node type has a meaningful name. */
    public String getName();
    
    /** Return the string representation of this node, suitable for display. */
    public String toString(String indent);
}
