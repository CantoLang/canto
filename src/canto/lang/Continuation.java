/* Canto Compiler and Runtime Engine
 * 
 * Continuation.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 *  A Continuation is thrown by a Canto <code>continue</code> statement.  This transfers
 *  the current construction of the page to a different definition.
 */
public class Continuation extends Throwable {

    private static final long serialVersionUID = 1L;
    
    String location;

    public Continuation(String location) {
        super();
        this.location = location;
    }

    public String getLocation() {
        return location;
    }
}
