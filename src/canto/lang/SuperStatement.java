/* Canto Compiler and Runtime Engine
 * 
 * SuperStatement.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
 * A super statement.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.4 $
 */

public class SuperStatement extends AbstractConstruction {

    public SuperStatement() {
        super();
    }

    public boolean isDynamic() {
        return true;
    }

    public String toString(String prefix) {
        return prefix + "super;\n";
    }

    public Object generateData(Context context, Definition def) {
        return "";
    }
}
