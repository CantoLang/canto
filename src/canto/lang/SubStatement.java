/* Canto Compiler and Runtime Engine
 * 
 * SubStatement.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
 * A sub statement.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.6 $
 */

public class SubStatement extends AbstractConstruction {

    public SubStatement() {
        super();
    }

    public boolean isDynamic() {
        return true;
    }

    /** Returns true. **/
    public boolean hasSub() {
        return true;
    }

    public String toString(String prefix) {
        return prefix + "sub;\n";
    }

    public Object generateData(Context context, Definition def) {
        return "";
    }
}
