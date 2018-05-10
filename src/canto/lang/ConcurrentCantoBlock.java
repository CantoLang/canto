/* Canto Compiler and Runtime Engine
 * 
 * ConcurrentCantoBlock.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * A ConcurrentCantoBlock is a CantoBlock in which each construction is executed
 * concurrently.  ConcurrentCantoBlocks are delimited by <code>[++</code> and <code>++]</code>
 * brackets.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */

public class ConcurrentCantoBlock extends CantoBlock {

    public ConcurrentCantoBlock() {
        super();
    }


    public String getTokenString(String prefix) {
        String str = prefix + "[++\n" + getChildrenTokenString(prefix + "    ") + prefix + "++]\n";
        return str;
    }

    public String toString(String prefix) {
        String str = super.toString(prefix);
        str = str.substring(2, str.length()).substring(2);
        str = "[++" + str + "++]";
        return str;
    }

}

