/* Canto Compiler and Runtime Engine
 * 
 * DuplicateDefinitionException.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 *  Exception thrown when an attempt is made to add a definition with the same
 *  full name and signature as a definition previously added.
 */
public class DuplicateDefinitionException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public DuplicateDefinitionException() {
        super();
    }
    public DuplicateDefinitionException(String str) {
        super(str);
    }
}
