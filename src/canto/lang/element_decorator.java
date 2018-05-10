/* Canto Compiler and Runtime Engine
 * 
 * element_decorator.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * When collections are instantiated, an site may want control over how
 * the individual elements are presented -- for instance, whether they 
 * are quoted or not.  This interface corresponds to the Canto definition
 * that specifies such a decorator.
 */
public interface element_decorator {

    /** Decorate a collection element. **/
    public String decorate_element(Object data);
}


