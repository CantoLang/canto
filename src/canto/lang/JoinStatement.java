/* Canto Compiler and Runtime Engine
 * 
 * JoinStatement.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * A join statement.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.3 $
 */

public class JoinStatement extends ComplexName {

    public JoinStatement() {
        super();
    }

    public String toString(String prefix) {
        return prefix + "join " + getName();
    }
}
