/* Canto Compiler and Runtime Engine
 * 
 * CantoBlock.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;


/**
 * A CantoBlock is a container whose children are Canto statements.
 *
 * @author Michael St. Hippolyte
 */

public class CantoBlock extends Block {

    public CantoBlock() {
        super();
    }

    public CantoBlock(AbstractNode[] children) {
        super(children);
    }

    public boolean isDynamic() {
        return true;
    }

    public boolean isStatic() {
        return false;
    }

    public boolean isAbstract(Context context) {
        return false;
    }

    //    public String toString() {
//        String str = "[=\n";
//        Iterator it = getChildren();
//        while (it.hasNext()) {
//            CantoNode node = (CantoNode) it.next();
//            str = str + "\n    " + node.toString();
//        }
//        str = str + "\n=]\n";
//        return str;
//    }

    public String getTokenString(String prefix) {
        String str = prefix + "{=\n" + getChildrenTokenString(prefix + "    ") + prefix + "=}\n";
        return str;
    }

    public String toString(String prefix) {
        String str = "{=\n" + super.toString(prefix) + prefix + "=}";
        return str;
    }

    public String toString(String firstPrefix, String prefix) {
        return firstPrefix + toString(prefix);
    }
}

