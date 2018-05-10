/* Canto Compiler and Runtime Engine
 * 
 * UninitializedObjectException.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 *  Exception thrown when an attempt is made to perform an operation on an
 *  object which has not been properly initialized.
 */
public class UninitializedObjectException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public UninitializedObjectException() {
        super();
    }
    public UninitializedObjectException(String str) {
        super(str);
    }
}
