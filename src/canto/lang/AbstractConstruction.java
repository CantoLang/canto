/* Canto Compiler and Runtime Engine
 * 
 * AbstractConstruction.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.lang.reflect.*;
import java.util.*;

import canto.runtime.Context;
import canto.runtime.CantoDebugger;
import canto.runtime.CantoObjectWrapper;
import canto.runtime.Holder;


/**
 * Abstract implementation of a construction.  A construction is a statement which 
 * generates data.
 *
 * A construction may be dynamic, static, or contextual.  Dynamic constructions are
 * evaluated every time they are executed.  Static constructions are only evaluated
 * once.  A contextual construction is re-evaluated only if the context has changed.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.130 $
 */

abstract public class AbstractConstruction extends AbstractNode implements Construction {

    // possible values returned by getCacheability

    /** The construction cannot be stored or retrieved from the cache. */
    public static final int NOT_CACHEABLE = 0;

    /** The construction can be retrieved from the cache but not stored. */
    public static final int CACHE_RETRIEVABLE = 1;

    /** The construction can be stored in the cache but not retrieved. */
    public static final int CACHE_STORABLE = 2;

    /** The construction can be stored or retrieved from the cache. */
    public static final int FULLY_CACHEABLE = 3;


    /** Static utility method to extract the value out of objects which may be
     *  either values or value generators.
     */
    public static final Value valueOf(ValueSource val, Context context) throws Redirection {
        if (val instanceof Value) {
            return (Value) val;
        } else {
            return ((ValueGenerator) val).getValue(context);
        }
    }

    /** Static utility method to extract a boolean value out of objects which may be
     *  either values or value generators.  Unlike the valueOf method, this method
     *  does not throw a Redirection; redirected constructions are interpreted as false.
     *
     *  This is useful for operations which are not themselves construction operations
     *  but utilize construction operations in their implementation.
     */
    public static final boolean booleanValueOf(ValueSource val, Context context) {
        Value value;

        if (val instanceof Value) {
            value = (Value) val;
        } else {
            try {
                value = ((ValueGenerator) val).getValue(context);
            } catch (Redirection r) {
                value = new PrimitiveValue(false);
            }
        }
        return value.getBoolean();
    }

    
    public static Construction getConstructionForObject(Object obj) {
        if (obj == null) {
            return NullValue.NULL_VALUE;
        } else if (obj instanceof Construction) {
            return (Construction) obj;
        } else if (obj instanceof Holder) {
            return getConstructionForObject(((Holder) obj).data);
        } else if (obj instanceof Definition) {
            Definition def = (Definition) obj;
            if (def instanceof NamedDefinition && !(def instanceof AliasedDefinition)) {
                def = new AliasedDefinition((NamedDefinition) def, def.getNameNode());
            }
            return new PrimitiveValue(def);
        } else {    
            return new PrimitiveValue(obj);
        }
    }

    public static Construction getConstructionForElement(Object element, Context context) throws Redirection {
        if (element == null) {
            return NullValue.NULL_VALUE;
        } else if (element instanceof Construction) {
            return (Construction) element;
        } else if (element instanceof Holder) {
            return getConstructionForElement(((Holder) element).data, context);
        } else if (element instanceof Definition) {
            // where would we get args?
            if (element instanceof CollectionDefinition) {
                CollectionDefinition collectionDef = (CollectionDefinition) element;
                if (collectionDef.isTable()) {
                    return new ResolvedTable(collectionDef, context, null, null);
                } else {
                    return new ResolvedArray(collectionDef, context, null, null);
                }
            } else {
                return new ResolvedInstance((Definition) element, context, null, null);
            }
        } else {    
            return new PrimitiveValue(element);
        }
    }
    
    /** Resolve an element of a collection according to whether it is an array, a table, or a simple instance. */
    static ResolvedInstance resolveInstance(Instantiation instance, Context context, boolean shareContext) throws Redirection {
        ResolvedInstance resolvedInstance = null;
        if (instance instanceof ResolvedInstance) {
            resolvedInstance = (ResolvedInstance) instance;
        } else {
            CantoNode reference = instance.getReference();
            if (reference instanceof CollectionDefinition) {
                CollectionDefinition collectionDef = (CollectionDefinition) reference;
                if (collectionDef.isTable()) {
                    resolvedInstance = new ResolvedTable(collectionDef, context, instance.getArguments(), instance.getIndexes());
                } else {
                    resolvedInstance = new ResolvedArray(collectionDef, context, instance.getArguments(), instance.getIndexes());
                }
            } else if (instance.isContainerParameter(context)) {
                resolvedInstance = (ResolvedInstance) context.getParameter(instance.getReferenceName(), true, ResolvedInstance.class);
            } else {
                resolvedInstance = new ResolvedInstance(instance, context, shareContext);
            }
        }
        return resolvedInstance;
    }
    
    
    protected boolean is_dynamic = false;
    protected boolean is_static = false;
    protected boolean trailingDelimiter = false;
    protected boolean concurrent = false;
    protected boolean anonymous = false;

    // used to avoid repetitive definition lookup
    protected static class CacheabilityInfo {
        
        public int cacheability;
        public Definition def;
        public CacheabilityInfo(int cacheability, Definition def) {
            this.cacheability = cacheability;
            this.def = def;
        }
    }        
    public static CacheabilityInfo NOT_CACHEABLE_INFO = new CacheabilityInfo(NOT_CACHEABLE, null);
    public static CacheabilityInfo CACHE_STORABLE_INFO = new CacheabilityInfo(CACHE_STORABLE, null);
    public static CacheabilityInfo CACHE_RETRIEVABLE_INFO = new CacheabilityInfo(CACHE_RETRIEVABLE, null);
    public static CacheabilityInfo FULLY_CACHEABLE_INFO = new CacheabilityInfo(FULLY_CACHEABLE, null);

    public AbstractConstruction() {
        super();
    }

    public AbstractConstruction(AbstractConstruction construction) {
        super(construction);
        // we make a copy of the children array because it gets altered when
        // when an expression is resolved
        if (children != null) {
            AbstractNode[] childrenCopy = new AbstractNode[children.length];
            System.arraycopy(children, 0, childrenCopy, 0, children.length);
            children = childrenCopy;
        }
        
        setDynStat(construction.isDynamic(), construction.isStatic());
        setTrailingDelimiter(construction.trailingDelimiter);
    }

    private Object generateData(Context context, Definition def, CantoDebugger debugger) throws Redirection {
        if (debugger == null) {
            return generateData(context, def);
        } else {
            Object data = generateData(context, def);
            debugger.constructed(this, context, data);
            return data;
        }
    }

    abstract public Object generateData(Context context, Definition def) throws Redirection;

    public boolean isDynamic() {
        return is_dynamic;
    }

    public boolean isStatic() {
        return is_static;
    }

    protected void setDynStat(boolean dyn, boolean stat) {
        is_dynamic = dyn;
        is_static = stat;
    }

    public boolean isConcurrent() {
        return concurrent;
    }
    
    protected void setConcurrent(boolean flag) {
        concurrent = flag;
    }
    
    public boolean isAnonymous() {
        return anonymous;
    }
    
    protected void setAnonymous(boolean flag) {
        anonymous = flag;
    }
    
    public boolean isConstruction() {
        return true;
    }

    public boolean isDefinition() {
        return false;
    }

    /** Subclasses may override this */
    public boolean isPrimitive() {
        return true;
    }
    
    public boolean isParameter() {
        return false;
    }

    public boolean isParameterChild() {
        return false;
    }

    public boolean isParameterKind() {
        return false;
    }

    protected void setTrailingDelimiter(boolean flag) {
        trailingDelimiter = flag;
    }

    /**
     * Returns the list of arguments associated with this construction, if any.  The
     * base class always returns null.
     */
    public ArgumentList getArguments() {
        return null;
    }

    /**
     * Returns the list of indexes associated with this construction, if any.  The
     * base class always returns null.
     */
    public List<Index> getIndexes() {
        return null;
    }

    /** Returns the name of the definition being constructed. */
    public String getDefinitionName() {
        return null;
    }

    /** Returns the definition for this construction in the specified context. */
    public Definition getDefinition(Context context) {
        return null;
    }

    /** Returns true if this construction constructs a next **/
    public boolean hasNext() {
        return false;
    }
    
    /** Returns true if this construction constructs a sub **/
    public boolean hasSub() {
        return false;
    }

    /** Return the construction that this construction resolves to, if it
     *  is a wrapper or alias of some sort, or else return this construction.
     *  The base class returns this construction.
     */
    public Construction getUltimateConstruction(Context context) {
        return this;
    }

    /** Returns the list of constructions owned by this container. */
    public List<Construction> getConstructions(Context context) {
        return new EmptyList<Construction>();
    }

    /** Returns true if this chunk is abstract, i.e., if it cannot be 
     *  instantiated because to do so would require instantiating an abstract
     *  definition.
     */
    public boolean isAbstract(Context context) {
        List<Construction> constructions = getConstructions(context);
        if (constructions != null) {
            Iterator<Construction> it = constructions.iterator();
            while (it.hasNext()) {
                Construction node = it.next();
                if (node == null) {
                    continue;
                } else if (node.isAbstract(context)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /** Returns the cacheability of this construction in the specified context.  The
     *  possible return values are CACHE_STORABLE, CACHE_RETRIEVABLE, NOT_CACHEABLE or
     *  FULLY_CACHEABLE, depending on whether the construction can be stored in the cache,
     *  retrieved from the cache, neither or both.  The base class always returns
     *  NOT_CACHEABLE; only named constructions can be cached.
     */
    protected CacheabilityInfo getCacheability(Context context, Definition def) {
        return NOT_CACHEABLE_INFO;
    }

    public NameNode getReferenceName() {
        return null;
    }

    public String getNameModifier() {
        return null;
    }

    /** Return the definition of the object referenced by any indexes.  If there are no
     *  indexes, or if indexes are not applicable, return the passed definition.
     *  @throws Redirection 
     */
    Definition dereference(Context context, Definition def) throws Redirection {
        return def;    
    }
    
    public Object getData(Context context) throws Redirection {
        return getData(context, null);
    }
    
    /**
     * @throws Redirection  
     */
    public Instantiation getUltimateInstance(Context context) throws Redirection {
        return null;
    }
    
    transient private Object staticData = null;
    public Object getData(Context context, Definition def) throws Redirection {
        CantoDebugger debugger = (context != null ? context.getDebugger() : null);
        if (debugger != null) {
            debugger.getting(this, context);
        }
        if (isDynamic()) {
            return generateData(context, def, debugger);

        } else if (isStatic()) {
            if (staticData == null) {
                staticData = generateData(context, def, debugger);
            }
            return staticData;

        } else {
            CacheabilityInfo cacheInfo = getCacheability(context, def);
            int cacheability = cacheInfo.cacheability;
            if (cacheability == NOT_CACHEABLE) {
                return generateData(context, cacheInfo.def, debugger);

            } else {
                Definition defInKeep = null;
                //Definition nominalDefInKeep = null;
                String name = getDefinitionName();
                ArgumentList args = getArguments();
                String defName = name;
                ArgumentList defArgs = args;
                if (isParameterChild()) {
                    Instantiation ultimateInstance = getUltimateInstance(context);
                    if (ultimateInstance != null) { 
                        name = ultimateInstance.getName();
                        args = ultimateInstance.getArguments();
                    }
                }
               
                NameNode nameNode = getReferenceName();
                Holder holder = null;
                if (context != null && name != null) {
                    holder = context.getDefHolder(name, null, getArguments(), getIndexes(), false);
                    if (holder != null && holder.def != null) {
                        defInKeep = holder.def;
                        //nominalDefInKeep = holder.nominalDef;
                    }
                }

                // this logic needs improvement to properly handle multipart names.  Right now
                // multipart and single name references to the same object are not recognized
                // as being the same, so a cache lookup might erroneously fail, leading to
                // two possbily different cached values for the same object (under two
                // different names).
                Object data = null;
                if (def == null && cacheInfo.def != null) {
                    //if (nominalDefInKeep != null && !cacheInfo.def.equalsOrExtends(nominalDefInKeep)) {
                    //    def = nominalDefInKeep;
                    //} else {
                        def = cacheInfo.def;
                    //}
                }
                if ((cacheability & CACHE_RETRIEVABLE) == CACHE_RETRIEVABLE) {
                    if (holder == null || holder.def == null) {
                        holder = context.getDefHolder(name, null, getArguments(), getIndexes(), false);
                        if (holder != null) {
                            defInKeep = holder.def;
                            //nominalDefInKeep = holder.nominalDef;
                        }
                    }
                    //Definition ultimateDef = (cacheInfo.def != null ? cacheInfo.def.getUltimateDefinition(context) : null);
                    if (holder != null && holder.data != null) {

                        // if this is the child of a cached object, get the data from the
                        // object instead of directly from the cache
                        Holder parentHolder = null;
                        if (nameNode.numParts() > 1) {
                            NameNode parentNameNode = new ComplexName(nameNode, 0, nameNode.numParts() - 1);
                            parentHolder = context.getDefHolder(parentNameNode.getName(), null, parentNameNode.getArguments(), parentNameNode.getIndexes(), false);
                            if (parentHolder != null && parentHolder.data instanceof CantoObjectWrapper) {
                                CantoObjectWrapper parentObj = (CantoObjectWrapper) parentHolder.data;
                                data = parentObj.getChildData(nameNode.getLastPart()); 
                            }
                        }

                        if (data == null) {
                            data = holder.data;
                        }
                    }
                    
                    if (def != null && def.isIdentity() && defInKeep != null) {
                        def = defInKeep;
                    }
                    
                    if (defInKeep == null) {
                        //cachevlog(" - - - no definition in cache for " + name + " - - - ");

                    //} else if (cacheInfo.def != null && !cacheInfo.def.equals(defInKeep) && !cacheInfo.def.equals(nominalDefInKeep) && !(ultimateDef == null || ultimateDef.equals(defInKeep))) {
                    //    //cachevlog(" - - - def is " + cacheInfo.def.getFullName() + " but defInKeep is " + defInKeep.getFullName());
                        
                    } else if (data == null) {
                        if (def == null) {
                            def = getDefinition(context);
                        }
                        if (def != null) {
                            data = context.getData(def, name, getArguments(), getIndexes());
                            if (data == null) {
                                //cachevlog(" - - - no data in cache for " + name + "; must instantiate - - - ");
                            } else if (debugger != null) {
                                debugger.retrievedFromKeep(def.getFullName(), context, data);
                            }
                        }
                    }
                }
                List<Index> indexes = getIndexes();
                if (data == null) {
                    if (Name.THIS.equals(name)) {
                        Definition owner = getOwner();
                        Context.Entry entry = owner.getEntryInContext(context);
                        if (entry == null || (args != null && args.size() > 0) || (indexes != null && indexes.size() > 0)) {
                            return new CantoObjectWrapper(owner, args, indexes, context);
                        } else {
                            return new CantoObjectWrapper(entry.def, entry.args, null, context);
                        }
                    } else if (def != null && Name.THIS.equals(def.getName()) && def instanceof AliasedDefinition) {
                        if (nameNode != null) {
                            int n = nameNode.numParts();
                            if (n > 1 && Name.THIS.equals(nameNode.getLastPart().getName())) {
                                nameNode = nameNode.getPart(n - 2);
                            }
                            return new CantoObjectWrapper(((AliasedDefinition) def).getAliasedDefinition(context), nameNode.getArguments(), nameNode.getIndexes(), context);
                        }
                    }
                    
                    data = generateData(context, def, debugger);
                    if ((cacheability & CACHE_STORABLE) == CACHE_STORABLE) {
                        if (data instanceof Definition) {
                            def = (Definition) data;
                        } else if (def == null) {
                            def = getDefinition(context);
                        }
                        
                        // if this is an identity, then the definition of the passed instantiation
                        // should already be cached; use it instead so children etc. resolve to it
                        Definition nominalDef = def;
                        ArgumentList nominalArgs = args;
                        ResolvedInstance ri = null;
                        

                        if (data instanceof ResolvedInstance) {
                            ri = (ResolvedInstance) data;
                            data = ri.generateData();
                        
                        // if an object wrapper is itself wrapped in a Value,
                        // unwrap it
                        } else if (data instanceof Value) {
                            Object val = ((Value) data).getValue();
                            if (val instanceof CantoObjectWrapper) {
                                data = val;
                            }
                        }
                        
                        if (data instanceof CantoObjectWrapper) {
                            Construction construction = ((CantoObjectWrapper) data).getConstruction();
                            if (construction instanceof ResolvedInstance) {
                                ri = (ResolvedInstance) construction;
                            }
                            Definition objDef = ((CantoObjectWrapper) data).getDefinition();
                            if (objDef != null) {
                                def = objDef;
                                args = (construction instanceof Instantiation ? ((Instantiation) construction).getArguments() : null);
                            }
                        }
                        if (def != null) {
                            if (def.isIdentity() && args != null && args.size() == 1) {
                                CantoNode node = (CantoNode) args.get(0);
                                if (node instanceof Instantiation) {
                                    String argName = node.getName();
                                    ArgumentList argArgs = ((Instantiation) node).getArguments();
                                    List<Index> argIndexes = ((Instantiation) node).getIndexes();
                                    holder = context.getDefHolder(argName, null, argArgs, argIndexes, false);
                                    if (holder != null && holder.def != null) {
                                        def = holder.def.getUltimateDefinition(context);
                                        args = holder.args;
                                        indexes = null;
                                        ri = holder.resolvedInstance;
                                    }
                                    if (ri == null) {
                                        ri = new ResolvedInstance((Instantiation) node, context, false);
                                    }
                                }
                            } else {
                                def = def.getUltimateDefinition(context);
                            }
                        }
                        //cachevlog(" - - - storing " + name + " in cache - - - ");
                        if (ri == null && this instanceof ResolvedInstance) {
                            ri = (ResolvedInstance) this;
                        }
                        //if (isParameter() && nominalDef != null && !name.equals(nominalDef.getName())) {
                        //    name = nominalDef.getName();
                        //}
                        context.putData(nominalDef, nominalArgs, def, defArgs, indexes, defName, data, ri);
                    }
                }
                return data;
            }
        }
    }

    public String getText(Context context) throws Redirection {
        Object data = getData(context);
        if (data instanceof Construction) {
            return ((Construction) data).getText(context);
        } else if (data instanceof Map) {
            Definition def = getDefinition(context);
            CollectionDefinition collectionDef = (def instanceof CollectionDefinition ? (CollectionDefinition) def : null); 
            return TableBuilder.getTextForMap(collectionDef, (Map<?,?>) data, context);
        } else {
            return getStringForData(data);
        }
    }
        
    static public String getStringForData(Object data) {
        if (data instanceof String) {
            return (String) data;
        } else if (data instanceof StringReference) {
            StringReference stref = (StringReference) data;
            return stref.getString();
        } else if (data instanceof Value) {
            return ((Value) data).getString();
        } else if (data instanceof Name) {
            return ((Name) data).getName();
        } else if (data == null) {
            return null;
        } else {
            return PrimitiveValue.getStringFor(data);
        }
    }


    public Type getType(Context context, boolean generate) {
        return DefaultType.TYPE;
    }

    // because several subclasses implement ValueGenerator, the
    // methods are implemented here (even though this class does
    // not implement ValueGenerator)

    public Value getValue(Context context) throws Redirection {
        Object data = null;
        data = getData(context);
        if (data instanceof ResolvedInstance) {
            data = ((ResolvedInstance) data).generateData();
        }
        if (data instanceof CollectionInstance) {
            data = ((CollectionInstance) data).getCollectionObject();
        }
        if (data == null) {
            return new PrimitiveValue();
        } else if (data instanceof Value) {
            return (Value) data;
        } else {
            return new PrimitiveValue(data, data.getClass());
        }
    }

    public String getString(Context context) throws Redirection {
        return getText(context);
    }

    public boolean getBoolean(Context context) {
        Object data = null;
        try {
            data = getData(context);
        } catch (Redirection r) {
            return false;
        }
        return booleanValueOf(data);
    }

    public static final boolean booleanValueOf(Object data) {
        if (data == null) {
            return false;
        } else if (data instanceof Number) {
            return (((Number) data).intValue() != 0);
        } else if (data instanceof Character) {
            return (((Character) data).charValue() != 0);
        } else if (data instanceof Boolean) {
            return ((Boolean) data).booleanValue();
        } else if (data instanceof String) {
            return (((String) data).trim().length() > 0);
        } else if (data instanceof Value) {
            return ((Value) data).getBoolean();
        } else if (data instanceof Collection<?>) {
            return (((Collection<?>) data).size() > 0);
        } else {
            Class<? extends Object> c = data.getClass();
            if (c.isArray()) {
                return (Array.getLength(data) > 0);
            }
            Method m = null;
            boolean b = true;
            try {
                m = c.getMethod("length", (Class<?>[]) null);
                b = booleanValueOf(m.invoke(data, (Object[]) null));
            } catch (Exception e) { try {
                m = c.getMethod("getLength", (Class<?>[]) null);
                b = booleanValueOf(m.invoke(data, (Object[]) null));
            } catch (Exception ee) { try {
                m = c.getMethod("size", (Class<?>[]) null);
                b = booleanValueOf(m.invoke(data, (Object[]) null));
            } catch (Exception eee) { try {
                m = c.getMethod("getSize", (Class<?>[]) null);
                b = booleanValueOf(m.invoke(data, (Object[]) null));
            } catch (Exception eeee) {
                ;
            } } } }
            return b;
        }
    }

    public byte getByte(Context context) {
        try {
            return getValue(context).getByte();
        } catch (Redirection r) {
            return (byte) 0;
        }
    }

    public char getChar(Context context) {
        try {
            return getValue(context).getChar();
        } catch (Redirection r) {
            return (char) 0;
        }
    }

    public int getInt(Context context) {
        try {
            return getValue(context).getInt();
        } catch (Redirection r) {
            return 0;
        }
    }

    public long getLong(Context context) {
        try {
            return getValue(context).getLong();
        } catch (Redirection r) {
            return 0L;
        }
    }

    public double getDouble(Context context) {
        try {
            return getValue(context).getDouble();
        } catch (Redirection r) {
            return 0.0d;
        }
    }

    public String toString(String prefix, Object reference) {
        StringBuffer sb = new StringBuffer(prefix);
        if (reference instanceof Definition) {
            Definition definition = (Definition) reference;
            String name = definition.getName();
            if (name == null || name.equals(Name.ANONYMOUS)) {
                String defstr = ((AbstractNode) definition).toString(prefix);
                if (defstr.endsWith("\n")) {
                    defstr = defstr.substring(0, defstr.length() - 1);
                }
                sb.append(defstr);
            } else {
                sb.append(name);
            }
            String modifier = getNameModifier();
            if (modifier != null) {
                sb.append(modifier);
            }
        } else {
            sb.append(reference.toString());
        }
        if (sb.length() > 0 && trailingDelimiter && !(getParent() instanceof ArgumentList) && !(getParent() instanceof Index) && !(getParent() instanceof Expression)) {
            char endchar = sb.charAt(sb.length() - 1);
            if (endchar != '\n' && endchar != '\r') {
                sb.append(";\n");
            }
        }
        return sb.toString();
    }
    
}
