/* Canto Compiler and Runtime Engine
 * 
 * CantoContext.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.util.List;
import java.util.Map;

import canto.lang.ArgumentList;
import canto.lang.ComplexName;
import canto.lang.Construction;
import canto.lang.Definition;
import canto.lang.Instantiation;
import canto.lang.NameNode;
import canto.lang.Redirection;
import canto.lang.Site;
import canto.lang.canto_context;

/**
 * This object wraps a Context and implements a Canto canto_context object, defined in core and representing
 * a Canto construction context, which can be used to construct objects.
 */
public class CantoContext implements canto_context {

    /** Returns the internal context object associated with this context. **/
    private Context context;
    private Site site;
    private boolean initialized;
    private boolean inUse = false;

    public CantoContext(CantoDomain cantoSite) {
        site = (Site) cantoSite.getMainOwner();
        context = cantoSite.getNewContext();
        initialized = true;
    }
 
    public CantoContext(Site site, Context context) {
        this.site = site;
        this.context = context;
        initialized = false;
    }
    
    public CantoContext(CantoContext cantoContext) {
        site = cantoContext.site;
        context = new Context(cantoContext.context, false);
        context.setTop(context.getRootEntry());
        initialized = true;
    }
    
    private void init() {
        initialized = true;
        context = new Context(context, false);
    }
    
    public boolean isInUse() {
        return inUse;
    }
    
    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }
    
    /** Returns the name of the site at the top of this context. **/
    public String site_name() {
        return site.getName();
    }
    
    /** Returns the cached value, if any, for a particular name in this context. **/
    public Object get(String name) throws Redirection {
        if (!initialized) {
            init();
        }
        return context.getData(null, name, null, null);
    }

    /** Sets the cached value, if any, for a particular name in this context. **/
    public void put(String name, Object data) throws Redirection {
        if (!initialized) {
            init();
        }
        // strip off .keep if it's there
        int ix = name.indexOf(".keep");
        if (ix > 0) {
            name = name.substring(0,  ix);
        }
        
        // find the definition if any corresponding to this name
        Instantiation instance = new Instantiation(new NameNode(name), context.peek().def);
        Definition def = instance.getDefinition(context);
        
        context.putData(def, null, null, name, data);
    }

    /** Constructs a Canto object of a particular name.  **/
    public Object construct(String name) throws Redirection {
        return construct(name, null);
    }

    /** Constructs a Canto object of a particular name, passing in particular arguments.  **/
    public Object construct(String name, List<Construction> args) throws Redirection {
        if (!initialized) {
            init();
        }
        NameNode nameNode = (name.indexOf('.') > 0 ? new ComplexName(name) : new NameNode(name));
        Instantiation instance;
        if (args != null) {
            ArgumentList argList = (args instanceof ArgumentList ? (ArgumentList) args : new ArgumentList(args));
            instance = new Instantiation(nameNode, argList, null, context.peek().def);
        } else {
            instance = new Instantiation(nameNode, context.peek().def);
        }
        return instance.getData(context);
    }
    
    public CantoContext container_context() {
        if (context.size() <= 1) {
            return null;
        }
        context.unpush();            
        try {
            CantoContext containerContext = new CantoContext(site, context);
            // force the copy before unpopping
            containerContext.init();
            return containerContext;
        } finally {
            context.repush();
        }        
    }

    public Object get(Definition def) throws Redirection {
        return def.get(getContext());
    }

    public Object get(Definition def, ArgumentList args) throws Redirection {
        return def.get(getContext(), args);
    }

    public List<Object> get_array(Definition def) throws Redirection {
        return def.get_array(getContext());
    }
    
    public Map<String, Object> get_table(Definition def) throws Redirection {
        return def.get_table(getContext());
    }

    
    
    /** Returns the internal context object associated with this context. **/
    public Context getContext() {
        // don't return the uninitialized context, to prevent it being altered
        if (!initialized) {
            init();
        }
        return context;
    }
    
    public String toString() {
        return context.toHTML();
    }
}


