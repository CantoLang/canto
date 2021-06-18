/* Canto Compiler and Runtime Engine
 * 
 * canto_context.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;
import java.util.Map;

import canto.runtime.Context;


/**
 * This interface corresponds to the canto_context object, defined in core and representing
 * a canto construction context, which can be used to construct objects.
 */
public interface canto_context {

    /** Returns the name of the site at the top of this context. **/
    public String site_name();

    /** Returns the cached value, if any, for a particular name in this context. **/
    public Object get(String name) throws Redirection;

    /** Sets the cached value, if any, for a particular name in this context. **/
    public void put(String name, Object data) throws Redirection;
    
    public Map<String, Object> cache();

    /** Constructs a Canto object of a particular name.  **/
    public Object construct(String name) throws Redirection;

    /** Constructs a Canto object of a particular name, passing in particular arguments.  **/
    public Object construct(String name, List<Construction> args) throws Redirection;

    /** Returns the context associated with the container of the current context. **/
    public canto_context container_context() throws Redirection;
    
    /** Returns the internal context object corresponding to this context. **/
    public Context getContext();
}


