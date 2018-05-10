/* Canto Compiler and Runtime Engine
 * 
 * NameWithArgs.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

/**
 * A NameWithArgs is an identifier and an associated list of arguments.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.8 $
 */
public class NameWithArgs extends NameNode {

    public NameWithArgs() {
        super();
    }

    public NameWithArgs(String name, ArgumentList args) {
        super(name);
        children = new AbstractNode[1];
        children[0] = args;
    }

    /** Always returns true. */
    public boolean hasArguments() {
        return (getArguments() != null);
    }

    /** Returns the list of arguments associated with this name. */
    public ArgumentList getArguments() {
        CantoNode node = getChild(0);
        if (node instanceof ArgumentList) {
            return (ArgumentList) node;
        } else {
            return null;
        }
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(super.getName());
        ArgumentList args = getArguments();
        if (args != null) {
            sb.append('(');
            if (args.isDynamic()) {
                sb.append(": ");
            }
            Iterator<Construction> it = args.iterator();
            while (it.hasNext()) {
                AbstractNode node = (AbstractNode) it.next();
                sb.append(node.toString());
                if (it.hasNext()) {
                    sb.append(',');
                }
            }
            if (args.isDynamic()) {
                sb.append(" :");
            }
            sb.append(')');
        }
        return sb.toString();
    }
}
