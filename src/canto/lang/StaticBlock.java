/* Canto Compiler and Runtime Engine
 * 
 * StaticBlock.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
 * A StaticBlock is a container whose children are static by default.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.4 $
 */
public class StaticBlock extends Block {

    public StaticBlock() {
        super();
    }

    public StaticBlock(AbstractNode[] children) {
        super(children);
    }

    public boolean isStatic() {
        return true;
    }

    public boolean isDynamic() {
        return false;
    }

    public boolean isAbstract(Context context) {
        return false;
    }

    public String toString(String prefix) {
        String str = "[|\n" + super.toString(prefix) + prefix + "|]\n";
        return str;
    }

    public String toString(String firstPrefix, String prefix) {
        return firstPrefix + toString(prefix);
    }
}
