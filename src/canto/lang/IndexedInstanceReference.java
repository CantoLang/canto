/* Canto Compiler and Runtime Engine
 * 
 * IndexedInstanceReference.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

/**
 * IndexedInstanceReference represents a non-collection instance instantiated with
 * one or more array or table indexes.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.10 $
 */

public class IndexedInstanceReference extends CollectionDefinition {
    private List<Index> indexes;
    private ResolvedInstance instance;
    private CollectionInstance collection;

    public IndexedInstanceReference(ResolvedInstance instance, List<Index> indexes) {
        super();
        this.indexes = indexes;
        this.instance = instance;
        this.collection = null;
        setOwner(instance.getOwner());
    }

    public String getName() {
        StringBuffer ib = new StringBuffer(instance.getName());
        Iterator<Index> it = indexes.iterator();
        while (it.hasNext()) {
            Index index = it.next();
            ib.append(index.toString());
        }
        return ib.toString();
    }

    public boolean isAnonymous() {
        return false;
    }

    public boolean isReference() {
        return true;
    }

    public NameNode getNameNode() {
        return new NameWithIndexes(instance.getName(), indexes);
    }

    
    /** Returns true if this reference points to an abstract definition.  If the reference
     *  has no definition then this returns false, since it has a concrete value (null)
     */
    public boolean isAbstract(Context context) {
        try {
            Definition def = getElementDefinition(context);
            return (def == null ? false : def.isAbstract(context));
        } catch (Redirection r) {
            return false;
        }
    }

    /** Returns null. */
    public Block getCatchBlock() {
        return null;
    }

    /** Returns null. */
    public String getCatchIdentifier() {
        return null;
    }

    public List<Construction> getConstructions(Context context) {
        Definition def = null;
        try {
            def = getElementDefinition(context);
        } catch (Redirection r) {
            ;
        }
        if (def == null) {
            return new EmptyList<Construction>();
        } else {
            return def.getConstructions(context);
        }
    }

    protected Object construct(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        Definition def = getElementDefinition(context);
        if (def == null) {
            return null;
        } else {
            return context.construct(def, args);
        }
    }
    
        public AbstractNode getContents() {
        return (AbstractNode) instance;
    }

    public Type getType() {
        return instance.getDefinition().getType();
    }

    public Type getSuper() {
        return instance.getDefinition().getSuper();
    }

    public Type getSuper(Context context) {
        return instance.getDefinition(context).getSuper(context);
    }

    public NamedDefinition getSuperDefinition() {
        return instance.getDefinition().getSuperDefinition();
    }

    public NamedDefinition getSuperDefinition(Context context) {
        return instance.getDefinition(context).getSuperDefinition(context); //collectionDef.getSuperDefinition(context);
    }

    public Object getChild(NameNode node, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context argContext, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection {
        if (Name.COUNT.equals(node.getName())) {
            if (generate) {
                CollectionInstance collection = getCollection();
                return new PrimitiveValue(collection.getSize());
            } else {
                Definition countDef = new CountDefinition(this, argContext, args, indexes);
                return countDef.getDefInstance(null, null);
            }
        } else if (Name.KEYS.equals(node.getName())) {
            Definition def = getElementDefinition(getResolutionContext());
            CollectionDefinition keysDef = new KeysDefinition(def, argContext, args, indexes);
            if (generate) {
                return keysDef.getCollectionInstance(argContext, args, indexes).getCollectionObject();
                
            } else {
                return keysDef.getDefInstance(null, indexes);
            }
        }
        return super.getChild(node, args, indexes, parentArgs, argContext, generate, trySuper, parentObj, resolver);
    }

    public CollectionInstance getCollection() throws Redirection {
        if (collection == null) {
            Index index = indexes.get(0);
            Object obj = instance.generateData();
            Context context = instance.getResolutionContext();
            collection = getCollectionInstanceFor(context, obj, index);
        }
        return collection;
    }
    
    
    public Definition getElementDefinition(Context context) throws Redirection {
        Definition def = null;
        Iterator<Index> it = indexes.iterator();
        CollectionInstance coll = getCollection();
        Index index = it.hasNext() ? it.next() : null;
        while (index != null) {
            def = coll.getElement(index, context);
            if (it.hasNext()) {
                index = it.next();
                if (def instanceof ElementDefinition) {
                    Object obj = ((ElementDefinition) def).getElement(context);
                    coll = getCollectionInstanceFor(context, obj, index);
                } else if (def instanceof CollectionDefinition) {
                    // this isn't ideal
                    ResolvedCollection rc = (ResolvedCollection) coll;
                    coll = ((CollectionDefinition) def).getCollectionInstance(rc.getResolutionContext(), rc.getArguments(), rc.getIndexes());
                } else {
                    throw new ClassCastException("Bad definition type for collection");
                }
                if (coll == null) {
                    log("Null collection in multidimensional ElementReference!");
                    break;
                }
            } else {
                break;
            }
        }
        return def;
    }

    private CollectionInstance getCollectionInstanceFor(Context context, Object obj, Index index) throws Redirection {
        CollectionInstance coll = null;
        if (obj instanceof Value) {
            obj = ((Value) obj).getValue();
        }
        if (obj instanceof CollectionInstance) {
            coll = (CollectionInstance) obj;
        } else { 
            boolean isTable = !index.isNumericIndex(context);
            CollectionDefinition collectionDef = new CollectionDefinition();
            collectionDef.setOwner(this);
            if (isTable) {           
                coll = new ResolvedTable(collectionDef, context, null, null, obj);    
            } else {
                coll = new ResolvedArray(collectionDef, context, null, null, obj);
            }
        }
        return coll;
    }
    
    /** If this definition is a reference or an alias to another definition, returns the
     *  definition ultimately referenced after the entire chain of references and aliases
     *  has been resolved.
     */
    public Definition getUltimateDefinition(Context context) {
        try {
            return getElementDefinition(context);
        } catch (Redirection r) {
            log("getUltimateDefinition call failed on " + getFullName() +"; couldn't get element definition");
            return null;
        }
    }

    private Context getResolutionContext() {
        return instance.getResolutionContext();
    }

    
    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(prefix);

        Type type = getSuper();
        if (type != null) {
            sb.append(type.getName());
            sb.append(' ');
        }

        AbstractNode contents = getContents();  
        sb.append(contents.toString(""));

//        String name = getName();
//        if (name != null && !name.equals(Name.ANONYMOUS)) {
//            sb.append(name);
//            sb.append(' ');
//        }

        return sb.toString();
    }

}
