/* Canto Compiler and Runtime Engine
 * 
 * ExternalCollectionDefinition.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

/**
 * Facade class to make a Java object available as a Canto definition.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */
public class ExternalCollectionDefinition extends CollectionDefinition {
    
    public static boolean isConstructibleFrom(Class<?> c) {
        return c.isArray() || List.class.isAssignableFrom(c) || Map.class.isAssignableFrom(c);
    }
    
    private ExternalDefinition externalDef = null;
    private Class<?> c = null;
    private Object object = null;
    protected ArgumentList args = null;

    public ExternalCollectionDefinition() {
        super();
    }

    public ExternalCollectionDefinition(ExternalDefinition externalDef, Context context, ArgumentList args, Class<?> c) throws Redirection {
         
         // this logic could be enhanced to handle multidimensional tables, arrays of tables,
         // tables of arrays etc.
         
         this.c = c;
         boolean isTable = Map.class.isAssignableFrom(c);
         int ndims = 0;
         if (c.isArray()) {
             String className = c.getName();
             while (className.charAt(ndims) == '[') {
                 ndims++;
             }
         } else {
        	 ndims = 1;
         }
         
         List<Dim> dims = new ArrayList<Dim>(ndims);
         for (int i = 0; i < ndims; i++) {
             Dim d = new Dim();
             if (isTable) {
                 d.setTable(true);
             }
             dims.add(d);
         }
         setDims(dims);
         setContents(externalDef.getContents());
         setOwner(externalDef.getOwner());
         this.externalDef = new ExternalDefinition(externalDef, context, args);
         init(externalDef.getSuper(), externalDef.getNameNode());
     }

     protected void setTableBuilder() {
         setBuilder(new ExternalTableBuilder(this, externalDef));
     }
     
     protected void setArrayBuilder() {
         setBuilder(new ExternalArrayBuilder(this, externalDef));
     }
     
     
     /** External definitions are always dynamic. **/    
     public int getDurability() {
         return DYNAMIC;
     }

     /** Returns <code>true</code> */
    public boolean isExternal() {
        return true;
    }

    public Object getChild(NameNode node, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context context, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection {
        if (externalDef != null) {
            return externalDef.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, null);
        } else {
            return super.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
        }
    }

    protected void init(Type supertype, NameNode name) {
        setSuper(supertype);
        setName(name);
        if (name instanceof NameWithParams  && ((NameWithParams) name).getNumParamLists() > 0) {
            setParamLists(((NameWithParams) name).getParamLists());
        } else {
            setParamLists(null);
        }
        setType(createType());
    }

    public void setModifiers(int access, int dur) {
        setAccess(access);
        setDurability(dur);
    }

    protected void setExternalClass(Class<?> c) {
        this.c = c;
    }

    public Class<?> getExternalClass() {
        if (c == null) {
            return Void.class;
        } else {
            return c;
        }
    }

    public Object getObject() {
        return object;
    }

    protected void setObject(Object object) {
        this.object = object;
    }

    public ArgumentList getArguments() {
        return args;
    }

    protected void setArguments(ArgumentList args) {
        this.args = args;
    }


}



