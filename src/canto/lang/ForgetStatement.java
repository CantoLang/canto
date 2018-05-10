/* Canto Compiler and Runtime Engine
 * 
 * ForgetStatement.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * A discard directive.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */

public class ForgetStatement extends CantoStatement {

    private String name;

    public ForgetStatement() {
        super();
    }

    public ForgetStatement(String name) {
        super();
        this.name = name;
    }

    public boolean isDynamic() {
        return false;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
