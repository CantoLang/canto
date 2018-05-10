/* Canto Compiler and Runtime Engine
 * 
 * ElementReference.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

/**
 * ElementReference represents an instance of an array or table element.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.48 $
 */

public class ElementReference extends AnonymousDefinition {
    private CollectionInstance collection;
    private Definition collectionDef;
    private List<Index> indexes;
    private Instantiation instance;

    public ElementReference(CollectionInstance collection, List<Index> indexes) {
        super();
        this.collection = collection;
        this.collectionDef = collection.getDefinition();
        this.indexes = indexes;
        this.instance = new Instantiation((Instantiation) collection, null, indexes);
        setOwner(collectionDef);
    }

    public String getName() {
        StringBuffer ib = new StringBuffer();
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
        return new NameWithIndexes(collection.getName(), indexes);
    }

    public List<ParameterList> getParamLists() {
        return (collectionDef != null ? collectionDef.getParamLists() : null);
    }

    public CollectionInstance getCollectionInstance() {
        return collection;
    }

    public Definition getCollectionDefinition() {
        return collectionDef;
    }        
        
    
    public CollectionDefinition getCollectionDefinition(Context context, ArgumentList args) throws Redirection{
        return collectionDef.getCollectionDefinition(context, args);
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
        ResolvedInstance ri = getResolvedElement(context);
        if (ri != null) {
            return ri.generateData();
        }
        
        Definition def = getElementDefinition(context);
        if (def == null) {
            return null;
        } else if (args != null) {
            try {
                ParameterList params = getParamsForArgs(args, context);
                context.push(this, params, args, true);
                return context.construct(def, args);
            } finally {
                context.pop();                
            }
            
        } else {
            return context.construct(def, args);
        }
    }

    public AbstractNode getContents() {
        return (AbstractNode) instance;
    }

    public boolean equalsOrExtends(Definition def) {
        Type baseType = getType().getBaseType();
        return baseType.isTypeOf(def.getType().getName());
    }

    public Type getType() {
        if (collectionDef instanceof CollectionDefinition) {
            return ((CollectionDefinition) collectionDef).getElementType();
        } else {
            return collectionDef.getType();
        }
    }

    public Definition getBaseDefinition() {
    	return collectionDef;
    }
    
    public Definition getBaseDefinition(Context context) {
        try {
            Definition baseDef = getElementDefinition(context);
            if (baseDef != null) {
                return baseDef;
            } else {
                return collectionDef;
            }
        } catch (Redirection r) {
            return collectionDef;
        }
    }

    public Type getSuper() {
        return getBaseDefinition().getSuper(); //collectionDef.getSuper();
    }

    public Type getSuper(Context context) {
        return getBaseDefinition(context).getSuper(context); //collectionDef.getSuper();
    }

    public NamedDefinition getSuperDefinition() {
        return getBaseDefinition().getSuperDefinition(); //collectionDef.getSuperDefinition();
    }

    public NamedDefinition getSuperDefinition(Context context) {
        return getBaseDefinition(context).getSuperDefinition(context); //collectionDef.getSuperDefinition(context);
    }

    public Object getChild(NameNode node, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context argContext, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection {
        Definition def = getElementDefinition(argContext);
        if (def == null) {
            return (generate ? UNDEFINED : null);
        } else {
            return def.getChild(node, args, indexes, parentArgs, argContext, generate, trySuper, parentObj, resolver);
        }
    }

    public Definition getElementDefinition(Context context) throws Redirection {
        Definition def = null;
        Iterator<Index> it = indexes.iterator();
        CollectionInstance coll = collection;
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
                } else if (def == null){
                    throw new IndexOutOfBoundsException("Null element in multidimensional ElementReference");
                } else {
                    throw new ClassCastException("Bad definition type for collection");
                }
                if (coll == null) {
                    log("Null collection in multidimensional ElementReference");
                    break;
                }
            } else {
                break;
            }
        }
        return def;
    }

    public ResolvedInstance getResolvedElement(Context context) throws Redirection {
        Definition def = null;
        Iterator<Index> it = indexes.iterator();
        CollectionInstance coll = collection;
        Index index = it.hasNext() ? it.next() : null;
        while (index != null) {
            if (it.hasNext()) {
                def = coll.getElement(index, context);
                index = it.next();
                if (def instanceof ElementDefinition) {
                    Object obj = ((ElementDefinition) def).getElement(context);
                    coll = getCollectionInstanceFor(context, obj, index);
                } else if (def instanceof CollectionDefinition) {
                    // this isn't ideal
                    ResolvedCollection rc = (ResolvedCollection) coll;
                    coll = ((CollectionDefinition) def).getCollectionInstance(rc.getResolutionContext(), rc.getArguments(), rc.getIndexes());
                } else if (def == null){
                    throw new IndexOutOfBoundsException("Null element in multidimensional ElementReference");
                } else {
                    throw new ClassCastException("Bad definition type for collection");
                }
                if (coll == null) {
                    log("Null collection in multidimensional ElementReference");
                    break;
                }
            } else {
                return coll.getResolvedElement(index, context);
            }
        }
        return null;
    }

    private CollectionInstance getCollectionInstanceFor(Context context, Object obj, Index index) throws Redirection {
        CollectionInstance coll = null;
        if (obj instanceof CollectionInstance) {
            coll = (CollectionInstance) obj;
        } else {
            boolean isTable = !index.isNumericIndex(context);
            CollectionDefinition def = new CollectionDefinition();
            def.setOwner(this);
            if (isTable) {
                coll = new ResolvedTable(def, context, null, null, obj);    
            } else {
                coll = new ResolvedArray(def, context, null, null, obj);
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
            Definition elementDef = getElementDefinition(context);
            return elementDef != null ? elementDef.getUltimateDefinition(context) : null;
        } catch (Redirection r) {
            log("getUltimateDefinition call failed on " + getFullName() +"; couldn't get element definition");
            return null;
        }
    }

    /** Returns true if this definition is an alias. */
    public boolean isAlias() {
        return true;
    }

    /** Returns the aliased name, if this definition is an alias to a definition
     *  or a parameter, else null. */
    public NameNode getAlias() {
        return instance.getReferenceName();
    }
    
    /** Returns the aliased instantiation, if this definition is an alias, else null. */
    public Instantiation getAliasInstance() {
        return instance;
    }
    
    public ParameterList getParamsForArgs(ArgumentList args, Context argContext) {
        try {
        	Definition def = getElementDefinition(argContext);
            if (def != null) {
                return ((AnonymousDefinition) def).getMatch(args, argContext);
            }
        } catch (Redirection r) {
        	;
        }
    	return null;
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
