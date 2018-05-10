/* Canto Compiler and Runtime Engine
 * 
 * CantoStatement.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * CantoStatement is the base class of all canto statements.
 *
 * @author Michael St. Hippolyte
 */

abstract public class CantoStatement extends AbstractNode {

    private AbstractNode contents;

    public CantoStatement() {
        super();
    }

    public CantoStatement(AbstractNode node) {
        super(node);
        if (node instanceof CantoStatement) {
            contents = ((CantoStatement) node).contents;
        }
    }

    protected void setContents(AbstractNode contents) {
        this.contents = contents;
    }

    public AbstractNode getContents() {
        return contents;
    }

    /** Some Canto statements generate data, some do not.  Statement types must implement
     *  this method and return the appropriate value.
     */
    abstract public boolean isDynamic();

    /** Canto statements may or may not be primitives.  Primitives cannot have children, i.e.
     *  getContents won't return a block, even an empty one.
     */
    public boolean isPrimitive() {
        return (!(contents instanceof Block) && !(contents instanceof Expression));
    }


    /** Subclasses which represent constructions should override this method to
     *  return true.  A construction is a Canto statement which is executed
     *  during the instantiation of the containing object.
     */
    public boolean isConstruction() {
        return false;
    }

    /** Subclasses which represent definitions should override this method to
     *  return true.
     */
    public boolean isDefinition() {
        return false;
    }

    /** Canto statements are not static data. */
    public boolean isStatic() {
        return false;
    }

    public String getName() {
        return toString().trim();
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(prefix);
        if (contents != null) {
            if (contents.isPrimitive()) {
                sb.append(" = ");
            } else {
                sb.append(' ');
            }
            sb.append(contents.toString(prefix));
            sb.append('\n');
        }
        return sb.toString();
    }
}

