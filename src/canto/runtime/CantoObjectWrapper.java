/* Canto Compiler and Runtime Engine
 * 
 * CantoObjectWrapper.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.util.List;

import canto.lang.*;

import java.util.ArrayList;

/**
 * @author Michael St. Hippolyte
 */

public class CantoObjectWrapper {

    public static ArrayList<Context> contextList = new ArrayList<Context>();
    
    Construction construction;
    Context context;
    Type type;
    Definition def;

    /** Constructs a new CantoWrapperObject, given an arbitrary Canto construction
     *  and a CantoSite.
     */
    public CantoObjectWrapper(Construction construction, CantoDomain site) {
        this.construction = construction;
        if (construction instanceof ResolvedInstance) {
            ResolvedInstance ri = (ResolvedInstance) construction;
            context = ri.getResolutionContext();
            def = ri.getDefinition();
            type = ri.getType();
                    
        } else {
            context = site.getNewContext();
            type = construction.getType(context, false);
            def = type.getDefinition();
        }
    }

    /** Constructs a new CantoWrapperObject, given a definition, data
     *  and context.
     */
    public CantoObjectWrapper(Definition def, ArgumentList args, List<Index> indexes, Context context) throws Redirection {
        def = def.getSubdefInContext(context);
        ResolvedInstance ri = new ResolvedInstance(def, context, args, indexes);
        construction = ri;
        
        Context resolutionContext = ri.getResolutionContext();

        this.context = (resolutionContext == context ? context.clone(false) : resolutionContext);
        //this.context.validateSize();
        contextList.add(this.context);
        
        this.def = def;
        type = def.getType();
    }

    public Definition getDefinition() {
    	return def;
    }
    
    public Type getType() {
    	return type;
    }
    
    public Context getResolutionContext() {
        return context;
    }
    
    public Construction getConstruction() {
        return construction;
    }
    
    public ArgumentList getArguments() {
        if (construction instanceof Instantiation) {
            return ((Instantiation) construction).getArguments();
        } else {
            return null;
        }
    }
    
    public Object getData() throws Redirection {
        //context.validateSize();
        return construction.getData(context);
    }

    public String getText() throws Redirection {
        Object data = construction.getData(context);
        if (data instanceof CantoObjectWrapper) {
            return null;
        } else {
            return AbstractConstruction.getStringForData(data);
        }
    }

    public Object getChildData(NameNode name, Type type, ArgumentList args) {
        //context.validateSize();
        try {
            return def.getChildData(name, type, context, args);
        } catch (Redirection r) {
            return null;
        }
    }

    public Object getChildData(NameNode name) {
        //context.validateSize();
        Definition parentDef = def;
        ArgumentList args = getArguments();
        int n = name.numParts();
        try {
            if (n == 1) {
                return parentDef.getChildData(name, null, context, args);
            } else {
                return parentDef.getChild(name, name.getArguments(), name.getIndexes(), args, context, true, true, this, null);
            }

        } catch (Redirection r) {
            return null;
        }
    }

    public boolean getChildBoolean(String name) {
        try {
            return PrimitiveValue.getBooleanFor(def.getChildData(new NameNode(name), null, context, getArguments()));
        } catch (Redirection r) {
            return false;
        }
    }

    public String getChildText(String name) {
        try {
            return PrimitiveValue.getStringFor(def.getChildData(new NameNode(name), null, context, getArguments()));
        } catch (Redirection r) {
            return null;
        }
    }

    public int getChildInt(String name) {
        try {
            return PrimitiveValue.getIntFor(def.getChildData(new NameNode(name), null, context, getArguments()));
        } catch (Redirection r) {
            throw new NumberFormatException("unable to get int for " + name);
        }
    }

    public Object[] getChildArray(String name) {
        Object obj = this.getChildData(new NameNode(name));
        if (obj instanceof List) {
        	return ((List) obj).toArray();
        } else {
            return (Object[]) obj;
        }
    }
        
    public boolean isChildDefined(String name) {
        return def.hasChildDefinition(name);
    }
    
    public String toString() {
        return "(" + construction.toString() + ")";
    }

    public Object getChild(NameNode node, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, boolean generate, boolean trySuper, Object parentObject, Definition resolver) throws Redirection {
        return def.getChild(node, args, indexes, parentArgs, context, generate, trySuper, null, resolver);
    }
}

