/* Canto Compiler and Runtime Engine
 * 
 * AbstractConstructionException.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 *  Exception thrown when an attempt is made to instantiate an abstract
 *  definition.
 */
public class AbstractConstructionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AbstractConstructionException() {
        super();
    }
    public AbstractConstructionException(String str) {
        super(str);
    }
}
