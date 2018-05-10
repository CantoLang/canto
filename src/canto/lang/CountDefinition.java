/* Canto Compiler and Runtime Engine
 * 
 * CountDefinition.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import canto.runtime.Context;

import java.lang.reflect.Array;

/**
 * CountDefinition represents the built-in <code>count</code> field which belongs
 * to every table and array.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.24 $
 */

public class CountDefinition extends NamedDefinition implements DynamicObject {

    private static Type countType = PrimitiveType.INT;
    public Definition def;
    //private int count;
    private ResolvedInstance ri = null;
    private PrimitiveValue value = null;
    private ArgumentList args;
    private List<Index> indexes;

    public CountDefinition(Definition def) {
        super();
        this.def = def;
        super.init(countType, new NameNode(Name.COUNT), null);
        
        args = null;
        indexes = null;
        value = null;
        ri = null;
        setDurability(DYNAMIC);
    }

    public CountDefinition(Definition def, Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        super(PrimitiveType.INT.getDefinition(), context);
        setName(new NameNode(Name.COUNT));
        if (def instanceof ExternalDefinition) {
            def = ((ExternalDefinition) def).getDefForContext(context, args);
        }
        if (def instanceof DynamicObject) {
            def = (Definition) ((DynamicObject) def).initForContext(context, args, indexes);
        }
        if (def instanceof CountDefinition) {
            def = ((CountDefinition) def).def;
        }
        this.def = def;
        this.args = args;
        this.indexes = indexes;
        this.ri = null;
        this.ri = new ResolvedInstance(def, context, args, indexes);
        
        setDurability(DYNAMIC);
    }

    private int getCount() {
        int count = 0;
        if (ri == null) {
            if (def instanceof CollectionDefinition) {
                count = ((CollectionDefinition) def).getSize();
            } else if (def instanceof ExternalDefinition) {
                count = ((ExternalDefinition) def).getObjectSize();
            } else {
                count = 1;
            }
        } else {
            Context context = ri.getResolutionContext();
            boolean unpushed = false;

            try {
                CollectionDefinition collectionDef = def.getCollectionDefinition(context, args);
                if (collectionDef != null) {
                    count = collectionDef.getSize(context, args, indexes);

                } else {
                    Instantiation instance;
                    if (def.equals(context.peek().def) && context.size() > 1) {
                        ArgumentList instanceArgs = context.getArguments();
                        context.unpush();
                        unpushed = true;
                        instance = new Instantiation(def, instanceArgs, null);
                    } else {
                        instance = new Instantiation(def, this);
                    }
                    Object data = instance.getData(context);
                    
                    count = getCountForObject(data);
                        
                }
            } catch (Redirection r) {
                ;                
            } finally {
                if (unpushed) {
                    context.repush();
                }
            }
        }
        
        return count;
    }
    
    
    static public int getCountForObject(Object data) {
        int count = 0;
        if (data instanceof Value) {
            data = ((Value)data).getValue();
        }
        if (data == null) {
            count = 0;
        } else if (data instanceof Map<?,?>) {
            count = ((Map<?,?>) data).size();
        } else if (data instanceof Collection<?>) {
            count = ((Collection<?>) data).size();
        } else if (data instanceof CantoArray) {
            count = ((CantoArray) data).getSize();
        } else if (data.getClass().isArray()) {
            count =  Array.getLength(data);
        } else {
            count = 1;
        }
                
        return count;
    }
    

    // override to avoid call to getContents
    public Block getCatchBlock() {
        return null;
    }
        
    public AbstractNode getContents() {
        if (value == null) {
            value = new PrimitiveValue(getCount()); 
        }
        return value; 
    }

    /** Returns <code>false</code>.
     */
    public boolean isAbstract(Context context) {
        return false;
    }

    /** Returns <code>PUBLIC_ACCESS</code>. */
    public int getAccess() {
        return PUBLIC_ACCESS;
    }

    /** Returns the type corresponding to this definition. */
    public Type getType() {
        ComplexType type = new ComplexType(this, "count");
        type.setOwner(this);
        return type;
    }

    /** Returns primitive integer type. */
    public Type getSuper() {
        return countType;
    }


    /** Returns true only if the name is "int"
     */
    public boolean isSuperType(String name) {
        return "int".equals(name);
    }

    /** Returns the encapsulated definition's full name, with ".count" appended.  */
    public String getFullName() {
        return def.getFullName() + ".count";
    }

    public String getFullNameInContext(Context context) {
        return def.getFullNameInContext(context) + ".count";
    }

    public String getName() {
        return "count";
    }

    /** Returns the encapsulated definition. */
    public Definition getOwner() {
        return def;
    }

    /** Returns a copy of this count definition initialized for the specified context.  The
     *  passed arguments are ignored.
     */
    public Object initForContext(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        if (initContext == null || !initContext.equals(context)) {
            return new CountDefinition(def, context, this.args, this.indexes);
        } else {
            return this;
        }
    }
}

