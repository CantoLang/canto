/* Canto Compiler and Runtime Engine
 * 
 * AnonymousDefinition.java
 *
 * Copyright (c) 2018-2021 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import canto.parser.ParsedStringLiteral;
import canto.runtime.Context;
import canto.runtime.CantoLogger;
/**
 * AnonymousDefinition is a definition without a name or type.  It is also the
 * base class of more complex definition types.
 *
 * @author Michael St. Hippolyte
 */

public class AnonymousDefinition extends CantoStatement implements Definition {

    /** Access level.  The allowed values are defined in Definition.  The default value
     *  is SITE_ACCESS.
     */
    private int access = SITE_ACCESS;

    /** Durability. */
    private int dur = IN_CONTEXT;

    /** The parameters for this definition, if any */
    private List<ParameterList> paramLists = null;

    /** The constructions comprising this definition */
    private List<Construction> constructions = null;

    /** True if this definition defines an element in an array or table. */
    private boolean elementDefinition = false;

    /** The context, if this definition is a copy initialized for a particular
     *  context, else null.
     */
    protected Context initContext = null;

    transient private String fullName = null;

    public AnonymousDefinition() {
        super();
        staticData = new StaticData();
    }

    public AnonymousDefinition(Definition def, Context context) {
        super((AbstractNode) def);
        if (def instanceof AnonymousDefinition) {
            AnonymousDefinition adef = (AnonymousDefinition) def;
            access = adef.access;
            dur = adef.dur;
            paramLists = adef.paramLists;
            elementDefinition = adef.elementDefinition;
            fullName = adef.fullName;
            staticData = adef.staticData;
        }
        if (context != null) {
            initContext = (Context) context.clone();
        }
    }

    // methods defined by Canto definition
    
    public Definition def() {
        return this;
    }

    /** Returns true if this definition equals or is a subdefinition of the
     *  passed type.
     */
    public boolean is_a(String typeName) {
        return getType().isTypeOf(typeName);
    }

    /** Returns true if this definition defines an array. */
    public boolean is_array() {
        return isArray();
    }
    
    /** Returns true if this definition defines a table. */
    public boolean is_table() {
        return isTable();
    }
    
    /** Returns true if this definition defines a collection. */
    public boolean is_collection() {
        return isCollection();
    }
    
    /** Returns the fully qualified name of this definition. */
    public String full_name() {
        return getFullName();
    }
    
    /** Returns the simple name of this definition. */
    public String name() {
        return getName();
    }
    
    /** Retrieves from cache or instantiates this definition as an array.  The default 
     *  is to return an array containing a single object, which is the retrieved/instantiated
     *  value of this definition.
     */
    public List<Object> get_array(Context context) throws Redirection {
        return new SingleItemList<Object>(get(context));
    }
    
    /** Retrieves from cache or instantiates this definition as a table.  The default 
     *  is to return a table containing a single object, which is the retrieved/instantiated
     *  value of this definition.
     */
    public Map<String, Object> get_table(Context context) throws Redirection {
        Map<String, Object> map = new HashMap<String, Object>(1);
        map.put(getName(), get(context));
        return map;
    }

    /** Instantiates this definition as an array.  The default is to return an array
     *  containing a single object, which is the instantiation of this definition.
     */
    public List<Object> instantiate_array(Context context) throws Redirection {
        return new SingleItemList<Object>(instantiate(context));
    }
    
    public List<Object> instantiate_array(canto_context context) throws Redirection {
        return instantiate_array(context.getContext());
    }
    
    /** Instantiates this definition as a table.  The default is to return a table
     *  containing a single object, which is the instantiation of this definition.
     */
    public Map<String, Object> instantiate_table(Context context) throws Redirection {
        Map<String, Object> map = new HashMap<String, Object>(1);
        map.put(getName(), instantiate(context));
        return map;
    }

    public Map<String, Object> instantiate_table(canto_context context) throws Redirection {
        return instantiate_table(context.getContext());
    }

    /** Returns the child definitions belonging to this definition, in a
     *  table keyed on name.
     */
    public Map<String, Definition> defs() {
        Map<String, Definition> defMap = null;
        CantoNode node = getContents();
        if (node instanceof Definition) {
            defMap = new HashMap<String, Definition>(1);
            defMap.put(node.getName(), (Definition) node);
        } else if (node instanceof List<?>) {
            List<?> nodeList = (List<?>) node;
            if (nodeList.size() > 0) {
                defMap = new HashMap<String, Definition>(nodeList.size());
                Iterator<?> it = nodeList.iterator();
                while (it.hasNext()) {
                    CantoNode child = (CantoNode) it.next();
                    if (child instanceof Definition) {
                        defMap.put(child.getName(), (Definition) child);
                    }
                }
            }
        } else if (node instanceof Block) {
            List<Definition> defList = ((Block) node).getDefinitions();
            if (defList.size() > 0) {
                defMap = new HashMap<String, Definition>(defList.size());
                Iterator<Definition> it = defList.iterator();
                while (it.hasNext()) {
                    Definition child = it.next();
                    defMap.put(child.getName(), child);
                }
            }
        }
        return defMap;
    }

    /** Returns the definitions named in keep directives. **/
    public Definition[] keep_defs(Context context) {
        return null;        
    }

    /** Returns the nearest ancestor of the specified type. */
    public Definition ancestor_of_type(String typeName) {
        for (Definition def = getOwner(); def != null; def = def.getOwner()) { 
            if (def.getType().isTypeOf(typeName)) {
                return def;
            }
        }
        return null;
    }

    /** Returns all the immediate child definitions that have the specified type. **/
    public Definition[] children_of_type(String typeName) {
        List<Definition> defList = new ArrayList<Definition>();
        Map<String, Definition> visitedMap = new HashMap<String, Definition>();
        addDescendantsOfType(typeName, defList, true, false, visitedMap);
        int size = defList.size();
        Definition[] defArray = new Definition[size];
        defArray = defList.toArray(defArray);
        return defArray;
    }

    /** Returns all the descendant definitions that have the specified type. **/
    public Definition[] descendants_of_type(String typeName) {
        List<Definition> defList = new ArrayList<Definition>();
        Map<String, Definition> visitedMap = new HashMap<String, Definition>();
        addDescendantsOfType(typeName, defList, true, true, visitedMap);
        int size = defList.size();
        Definition[] defArray = new Definition[size];
        defArray = defList.toArray(defArray);
        return defArray;
    }

    protected void addDescendantsOfType(String typeName, List<Definition> defs, boolean recurseSupers, boolean recurseChildren, Map<String, Definition> visited) {
        if (visited.get(getFullName()) != null) {
            return;
        }
        visited.put(getFullName(), this);
        
        boolean isCollection = isCollectionType(typeName);
        
        CantoNode node = getContents();
        if (node instanceof Definition) {
            Definition def = (Definition) node;
            Type type = def.getType();
            try {
                if (type != null && type.isTypeOf(typeName) && type.isCollection() == isCollection) {
                    defs.add(def);
                }
                
                if (recurseChildren && def instanceof AnonymousDefinition) {
                    ((AnonymousDefinition) def).addDescendantsOfType(typeName, defs, recurseSupers, true, visited);
                }
            } catch (Exception e) {
                log("Exception in addDescendantsOfType processing definition " + def.getName() + ": " + e);
            }

        } else if (node instanceof List<?>) {
            List<?> nodeList = (List<?>) node;
            Iterator<?> it = nodeList.iterator();
            while (it.hasNext()) {
                CantoNode child = (CantoNode) it.next();
                if (child instanceof Definition) {
                    Definition def = (Definition) child;
                    Type type = def.getType();
                    try {
                        if (type != null && type.isTypeOf(typeName) && type.isCollection() == isCollection) {
                            defs.add(def);
                        }
                        
                        if (recurseChildren && def instanceof AnonymousDefinition) {
                            ((AnonymousDefinition) def).addDescendantsOfType(typeName, defs, recurseSupers, true, visited);
                        }
                    } catch (Exception e) {
                        log("Exception in addDescendantsOfType processing definition " + def.getName() + ": " + e);
                    }
                }
            }

        } else if (node instanceof Block) {
            List<Definition> defList = ((Block) node).getDefinitions();
            Iterator<Definition> it = defList.iterator();
            while (it.hasNext()) {
                Definition def = it.next();
                try {
                    Type type = def.getType();
                    if (type != null && type.isTypeOf(typeName) && type.isCollection() == isCollection) {
                        defs.add(def);
                    }
                        
                    if (recurseChildren && def instanceof AnonymousDefinition) {
                        ((AnonymousDefinition) def).addDescendantsOfType(typeName, defs, recurseSupers, true, visited);
                    }
                } catch (Exception e) {
                    log("Exception in addDescendantsOfType processing definition " + def.getName() + ": " + e);
                }
            }
        }
        
        // if the recurseSupers flag is true, and the supertype hasn't already been handled,
        // call it
        if (recurseSupers) {
            Definition superDef = this.getSuperDefinition();
            if (superDef != null && visited.get(superDef.getFullName()) == null) {
                ((AnonymousDefinition) superDef).addDescendantsOfType(typeName, defs, recurseSupers, recurseChildren, visited);
            }
        }
    }
    
    static private boolean isCollectionType(String typeName) {
        // collections aren't handled yet
        return false;
    }
    
    //-----------------------------------
    
    /** Returns true if this definition has been initialized for the specified context or
     *  a context compatible with it (with the same root context and either equal or
     *  one a descendant of the other).  This is intended for dynamic definitions, which
     *  are context-dependent.
     */
    public boolean isInitialized(Context context) {
        return (initContext != null && initContext.isCompatible(context));
    }

    public boolean isDynamic() {
        return false;
    }

    public boolean isDefinition() {
        return true;
    }

    public boolean isExternal() {
        return false;
    }

    /** Returns false. */
    public boolean isPrimitive() {
        return false;
    }

    /** Returns true if this definition is abstract, i.e., it has an abstract
     *  block as its contents.
     */
    public boolean isAbstract(Context context) {
        CantoNode contents = getContents();
        if (contents instanceof Block && ((Block) contents).isAbstract(context)) {
            return true;
        } else {
            return false;
        }
    }

    /** Returns true if <code>name</code> is the name of an ancestor of this
     *  definition.
     */
    public boolean isSuperType(String name) {
        return (name.equals(""));
    }

    protected void setAccess(int access) {
        this.access = access;
    }

    public int getAccess() {
        return access;
    }

    /** Convenience method; returns true if this is a local definition
     *  (i.e., access is LOCAL_ACCESS).
     */
    public boolean isLocal() {
        return (access == LOCAL_ACCESS);
    }

    protected void setDurability(int dur) {
        this.dur = dur;
    }

    public int getDurability() {
        return dur;
    }

    /** Convenience method; returns true if the definition is
     *  global or static (i.e., durability is GLOBAL or STATIC).
     */
    public boolean isGlobal() {
        return (dur == GLOBAL || dur == STATIC);
    }
    
    protected void setName(NameNode name) {
        //throw new UnsupportedOperationException("This definition doesn't support naming");
        System.out.println("This definition doesn't support naming");
    }

    public String getName() {
        return ANONYMOUS;
    }

    /** An anonymous name has zero parts. */
    public int numParts() {
        return 0;
    }

    public boolean isAnonymous() {
        return true;
    }

    public NameNode getNameNode() {
        return null;
    }

    /** Returns the full name, with the ownership chain adjusted to reflect the
     *  actual subclasses, in dot notation.
     */
    public String getFullNameInContext(Context context) {
        String name = getName();
        if (name != null && name != Name.ANONYMOUS) {
            Definition owner = getOwner();
            if (owner == null || owner instanceof Site) {
                return name;
            }
            Definition contextDef = owner.getSubdefInContext(context);
            if (this.equals(contextDef)) {
                return getFullName();
            }
            String ownerName = contextDef.getFullNameInContext(context);
            if (ownerName != null && ownerName.length() > 0) {
                name = ownerName + '.' + name;
            }
        }
        return name;
    }

    
    public String getFullName() {
        if (fullName == null) {
            String name = getName();
            if (name != null && name != Name.ANONYMOUS) {
                Definition owner = getOwner();
                if (owner == null) {
                    // don't set fullName in this case, in case the owner
                    // hasn't been set yet.
                    return name;
                }
                String ownerName = owner.getFullName();
                while (ownerName == null || ownerName.length() == 0) {
                    owner = owner.getOwner();
                    if (owner == null) {
                        break;
                    }
                    ownerName = owner.getFullName();
                }
                if (ownerName != null && ownerName.length() > 0) {
                    char c = name.charAt(0);
                    if (c == '[' || c == '{') {
                        name = ownerName + name;
                    } else {
                        name = ownerName + '.' + name;
                    }
                }
            }
            fullName = name;
        }
        return fullName;
    }

    public List<ParameterList> getParamLists() {
        return paramLists;
    }

    protected void setParamLists(List<ParameterList> paramLists) {
        this.paramLists = paramLists;
    }

    /** If this definition is a reference to another definition, returns the definition
     *  ultimately referenced after the entire chain of references has been resolved.
     * 
     */
    public Definition getUltimateDefinition(Context context) {
        return this;
    }

    /** Returns true if the passed object is the same definition as this.
     * 
     * @param obj
     * @return true if the passed object equals this definition.
     */
    public boolean equals(Object obj) {
        if (obj instanceof Definition) {
            return equals((Definition) obj, null);
        } else {
            return false;
        }
    }
    
    /** Returns true if the passed object is the same definition as this
     *  in the specified context.
     * 
     * @param obj
     * @param context
     * @return true if the passed object equals this definition in the 
     *         specified context.
     */
    public boolean equals(Definition def, Context context) {
        if (def == this) {
            return true;
        }
        if (isAnonymous()) {
            return false;
        } else {
            Definition thisDef = (context == null ? this : getUltimateDefinition(context));
            if (thisDef == null) {
                thisDef = this;
            }
            Definition otherDef = (context == null ? def : def.getUltimateDefinition(context));
            if (otherDef == null) {
                otherDef = def;
            }
            
            String name = thisDef.getFullName();
            String otherName = otherDef.getFullName();
            if (name != null) {
                // not sure why this works, but filtering out dot names prevents keys that
                // are a child of a loop parameter from unwanted caching
                return !name.startsWith(".") && name.equals(otherName);
            } else {
                return (otherName == null);
            }
        }
    }

    /** Returns true if <code>def</code> equals this definition (AnonymousDefinitions are not
     *  extensions of any other definition so this method returns the same result as the 
     *  equals method).
     */
    public boolean equalsOrExtends(Definition def) {
        return equals(def);
    }


    /** The default Type is DefaultType.TYPE.
     */
    public Type getType() {
        return DefaultType.TYPE;
    }

    /** A <code>sub</code> statement in an anonymous definition would
     *  be meaningless.
     */
    public boolean hasSub(Context context) {
        return false;
    }
    
    /** A <code>next</code> statement in an anonymous definition would
     *  be meaningless.
     */
    public boolean hasNext(Context context) {
        return false;
    }

    /** Anonymous definitions have no superdefinitions with <code>next</code>
     *  statements; returns null.
     */
    public LinkedList<Definition> getNextList(Context context) {
        return null;
    }
    
    /** Anonymous definitions have no supertype; returns null. */
    public Type getSuper() {
        return null;
    }

    public Type getSuper(Context context) {
        return getSuper();
    }

    /** Anonymous definitions have no supertype; returns null. */
    public Type getSuperForChild(Context context, Definition childDef) throws Redirection {
        return null;
    }

    /** Anonymous definitions have no supertype; returns null. */
    public NamedDefinition getSuperDefinition() {
        return null;
    }

    /** Anonymous definitions have no supertype; returns null. */
    public NamedDefinition getSuperDefinition(Context context) {
        return null;
    }

    /** Returns the immediate subdefinition of this definition in the current context,
     *  or null if not found.  If this definition is not a NamedDefinition then this
     *  method will return null.
     */
    public Definition getImmediateSubdefinition(Context context) {
        return null;
    }

    /** Anonymous definitions have no underdefinitions; returns null. */
    public NamedDefinition getUnderDefinition(Context context) {
        return null;
    }

    /** Returns the first entry in the context stack whose definition equals or extends 
     *  this definition.  
     */
    public Context.Entry getEntryInContext(Context context) {
        Context.Entry entry = context.peek();
        if (entry.def.equalsOrExtends(this)) {
            return entry;
        } else {
            while (entry != null) {
                if (entry.def.equalsOrExtends(this)) {
                    return entry;
                }
                entry = entry.getPrevious();
            }
        }
        return null;
    }

    
    public Definition getSubdefInContext(Context context) {
        Context.Entry entry = getEntryInContext(context);
        if (entry != null) {
            return entry.def;
        } else {
            return this;
        }
    }

    public Definition getOwnerInContext(Context context) {
        Definition owner = getOwner();
        if (owner == null) {
            return null;
        }
        return owner.getSubdefInContext(context);
    }
        
    /** Returns the site containing this definition. */
    public Site getSite() {
        Definition owner = getOwner();
        if (owner == null) {
            return null;
        } else if (owner == this) {
            throw new IllegalStateException("Circular definition: " + getName() + " owns itself");
        
        } else {
            return owner.getSite();
        }
    }
    
    /** static data cache, unused if this definition is not declared to be static. **/
    private static class StaticData {
        public Object data = null;
    }
    private StaticData staticData;

    protected boolean hasStaticData() {
        return (staticData.data != null);
    }

    protected void setStaticData(Object data) {
        if (dur == GLOBAL || dur == STATIC) {
            vlog("Setting " + (dur == GLOBAL ? "global" : "static") + " data for " + getFullName());
            staticData.data = data;
        }
    }

    protected Object getStaticData() {
        return staticData.data;
    }

    /** For subclasses that are cacheable, gets the cached value for this 
     *  definition in the current context, if the definition is cacheable 
     *  and a value is present in the cache, else constructs the definition 
     *  with the passed arguments.  The base class is not cacheable, so 
     *  always does the latter.
     */
    public Object get(Context context, ArgumentList args) throws Redirection {
        return instantiate(context, args);
    }
    

    /** For subclasses that are cacheable, gets the cached value for this 
     *  definition in the current context, if the definition is cacheable 
     *  and a value is present in the cache, else constructs the definition 
     *  with no arguments.  The base class is not cacheable, so always does 
     *  the latter.
     */
    public Object get(Context context) throws Redirection {
        return instantiate(context);
    }

    /** Construct this definition without arguments in the specified canto_context, 
     *  which is available to canto code via the runtime reflection api.
     */
    public Object instantiate(Context context) throws Redirection {
        try {
            return instantiate(null, null, context);
        } catch (NullPointerException e) {
            return null;
        }
    }
    public Object instantiate(canto_context context) throws Redirection {
        return instantiate(context.getContext());
    }
    
    /** Construct this definition with the specified arguments in the specified 
     *  canto_context, which is available to canto code via the runtime reflection api.
     */
    public Object instantiate(Context context, ArgumentList args) throws Redirection {
        return instantiate(args, null, context); 
    }

    public Object instantiate(canto_context context, ArgumentList args) throws Redirection {
        return instantiate(context.getContext(), args); 
    }

    /** Construct this definition with the specified arguments in the specified context. */
    public Object instantiate(ArgumentList args, List<Index> indexes, Context context) throws Redirection {
        Definition initializedDef = context.initDef(this, args, indexes);
        if (initializedDef == null && indexes != null) {
            initializedDef = context.initDef(this, args, null);
        } else if (initializedDef != this) {
            indexes = null;
        }
        if (initializedDef != this && initializedDef != null && initializedDef instanceof AnonymousDefinition) {
            return ((AnonymousDefinition) initializedDef)._instantiate(context, args, indexes);
        } else {
            return _instantiate(context, args, indexes);
        }
    }
        
    private Object _instantiate(Context context, ArgumentList args, List<Index> indexes) throws Redirection {        
        if ((dur == GLOBAL || dur == STATIC) && staticData.data != null && (args == null || !args.isDynamic())) {
            return staticData.data;
        }

        if (isAbstract(context)) {
            if (context != null && context.getErrorThreshhold() > Context.FUNCTIONAL_ERRORS) {
                return null;
            }
            throw new Redirection(Redirection.STANDARD_ERROR, getFullName() + " is abstract; cannot instantiate");
        }

        CantoLogger.logInstantiation(context, this);

        if (dur == GLOBAL || dur == STATIC) {
            vlog("Constructing " + (dur == GLOBAL ? "global" : "static") + " data for " + getFullName());
            staticData.data = construct(context, args, indexes);
            return staticData.data;
        } else {
            return construct(context, args, indexes);
        }
    }

    /* TODO: this is probably the best place to coerce the constructed data into the proper type for this definition. */
    protected Object construct(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        // dynamic objects are objects such as arrays with logic in their
        // initialization expressions
        Definition def = this;
        if (def instanceof DynamicObject) {
            def = (Definition) ((DynamicObject) def).initForContext(context, args, indexes);
        }
        
        Object obj = context.construct(def, args);

        if (def.isCollection() && indexes != null) {
            obj = context.dereference(obj, indexes);
        }

        return obj;
    }

    /** Find the child definition, if any, by the specified name; if <code>generate</code> is
     *  false, return the definition, else instantiate it and return the result.  If <code>generate</code>
     *  is true and a definition is not found, return UNDEFINED.
     */
    public Object getChild(NameNode name, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context argContext, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection {
        if (generate) {
            return UNDEFINED;
        } else {
            return null;
        }
    }

   
    /** Returns the keep statement in this definition for the specified key.
     */
    public KeepStatement getKeep(String key) {
        return null;
    }


    /** Unnamed definitions are opaque, and the definitions they contain
     *  cannot be retrieved, so the base class returns null.  Definitions which
     *  support the retrieval of contained definitions (ComplexDefinition, for
     *  example) must override this to return the specified definition, if it
     *  exists, else null.
     */
    public Definition getChildDefinition(NameNode name, Context context) {
        return null;
    }

    /** Unnamed definitions are opaque, and the definitions they contain
     *  cannot be retrieved, so the base class returns false.
     */
    public boolean hasChildDefinition(String name, boolean localAllowed) {
        return false;
    }

    public Definition getChildDefinition(NameNode name, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context argContext, Definition resolver) {
        try {
            Object obj = getChild(name, args, indexes, parentArgs, argContext, false, true, null, resolver);
            if (obj instanceof Definition) {
                return (Definition) obj;
            } else if (obj instanceof DefinitionInstance) {
                return ((DefinitionInstance) obj).def;
            // presume obj is UNDEFINED
            } else {
                return null;
            }
        } catch (Throwable t) {
            log("Unable to find definition for " + name.getName() + " in " + getFullName());
            t.printStackTrace();
            return null;
        }
    }

    public final DefinitionInstance getChildDefInstance(NameNode name, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context argContext) {
        try {
            Object obj = getChild(name, args, indexes, parentArgs, argContext, false, true, null, null);
            if (obj instanceof DefinitionInstance) {
                return (DefinitionInstance) obj;
            // presume obj is UNDEFINED
            } else {
                return null;
            }
        } catch (Throwable t) {
            log("Unable to find definition for " + name.getName() + " in " + getFullName());
            t.printStackTrace();
            return null;
        }
    }

    /** Returns true if this definition has child definitions.  The base class returns
     *  true unless the definition is an alias, identity or primitive type.
     */
    public boolean canHaveChildDefinitions() {
        return !isAlias() && !isParamAlias() && !isIdentity() && !isPrimitive();
    }

    /** Instantiates a child definition in a specified context and returns the result.  The
     *  type parameter is only used if the child definition is external, in which case it
     *  is the Canto supertype of the external object.
     **/
    public Object getChildData(NameNode childName, Type type, Context context, ArgumentList args) throws Redirection {
        Object data = null;
        ArgumentList childArgs = childName.getArguments();
        List<Index> childIndexes = childName.getIndexes();

        // see if the argument definition has a child definition by that name
        Definition childDef = getChildDefinition(childName, childArgs, childIndexes, args, context, null);

        // if not, then look for alternatives 
        if (childDef == null) {

            // if not, then look for an aliased external definition
            if (isAlias()) {
                childDef = ExternalDefinition.createForName(this, new ComplexName(getAlias(), childName), type, getAccess(), getDurability(), context);
            }

            // if that didn't work, look for a special definition child
            if (childDef == null) {
                if (type != null && type.getName().equals("definition")) {
                    childDef = getDefinitionChild(childName, context, args);

                } else {
                    String cName = childName.getName();
                    if (cName.equals("defs") || cName.equals("descendants_of_type") || cName.equals("full_name")) {
                        ExternalDefinition externalDef = new ExternalDefinition("canto.lang.AnonymousDefinition", getParent(), getOwner(), type, getAccess(), DYNAMIC, this, null);
                        childDef = externalDef.getExternalChildDefinition(childName, context);
                    }
                }
            }
        }
        
        if (childDef != null) {
            if (childDef instanceof DefParameter) {
                return context.getParameter(childName, false, Object.class);
            }

            if (args == null) {
                Context.Entry entry = context.peek();
                args = entry.args;
            }
            //context.unpush();
            //int numUnpushes = 1;
            int numPushes = 0;
            
            try {
                if (childDef instanceof ElementReference) {
                    childDef = ((ElementReference) childDef).getElementDefinition(context);
                    if (childDef == null) {
                        throw new Redirection(Redirection.STANDARD_ERROR, "No definition for element " + childName.toString());
                    }

                // if the child name has one or more indexes, and the definition is a
                // collection definition, get the appropriate element in the collection.
                } else if (childDef instanceof CollectionDefinition && childName.hasIndexes()) {
                    CollectionDefinition collectionDef = (CollectionDefinition) childDef;
                    childDef = collectionDef.getElementReference(context, childName.getArguments(), childName.getIndexes());
                }
                
                numPushes = context.pushSupersAndAliases(this, args, childDef);
                //context.repush();
                //numUnpushes = 0;

                int dur = childDef.getDurability();
                if ((dur == STATIC || dur == GLOBAL) && childDef instanceof AnonymousDefinition) {
                    AnonymousDefinition aDef = (AnonymousDefinition) childDef;
                    synchronized (aDef.staticData) {
                        if (aDef.staticData.data == null) {
                            aDef.staticData.data = context.constructDef(childDef, childArgs, childIndexes);
                        }
                    }
                    if (dur == GLOBAL && childArgs != null && childArgs.isDynamic()) {
                        data = context.constructDef(childDef, childArgs, childIndexes);
                    } else {
                        data = aDef.staticData.data;
                    }
                } else if (dur != DYNAMIC) {
                    data = context.getData(childDef, childDef.getName(), childArgs, childIndexes);
                    if (data == null) {
                        data = context.constructDef(childDef, childArgs, childIndexes);
                    }
                } else {
                    data = context.constructDef(childDef, childArgs, childIndexes);
                }

            } finally {
                if (numPushes > 0) {
                    //if (numUnpushes == 0) {
                    //    context.unpush();
                    //    numUnpushes = 1;
                    //}
                    while (numPushes > 0) {
                        context.pop();
                        numPushes--;
                    }
                }
                //if (numUnpushes > 0) {
                //    context.repush();
                //}
            }
            if (data == null) {
                data = NullValue.NULL_VALUE;
            }
        }
        return data;
    }

    /** Get a child of this definition as a definition. This only works for named definitions. 
     *  @throws Redirection 
     */
    public Definition getDefinitionChild(NameNode childName, Context context, ArgumentList args) throws Redirection {
        return null;
    }
    
    /** The base class returns 1. */
    public int getSize() {
        return 1;
    }

    /** Get the definition table representing this definition's namespace.  Anonymous definitions
     *  are in the namespace of their owners, so this method returns the owner's definition table.
     */
    DefinitionTable getDefinitionTable() {
        return ((AnonymousDefinition) getOwner()).getDefinitionTable();
    }

    /** Anonymous definitions don't have their own definition tables, so this method
     *  does nothing.
     */
    void setDefinitionTable(DefinitionTable table) {
        ;
    }

    /** Add a definition to this definition's namespace.  Anonymous definitions are in the
     *  namespace of their owners, so this method adds the definition to its owner's namespace.
     */
    public void addDefinition(Definition def, boolean replace) throws DuplicateDefinitionException {
        ((AnonymousDefinition) getOwner()).addDefinition(def, replace);
    }

    @SuppressWarnings("unchecked")
    public List<Construction> getConstructions(Context context) {
        CantoNode contents = getContents();
        if (contents == null) {
            if (constructions == null) {
                constructions = new EmptyList<Construction>();
            }
          
        } else if (constructions == null || contents.isDynamic()) {
            if (contents instanceof DynamicElementBlock) {
                DynamicElementBlock dynamicElement = (DynamicElementBlock) ((DynamicElementBlock) contents).initForContext(context, null, null);
                contents = dynamicElement.getContents();
            }
            
            if (contents instanceof List<?>) {
                constructions = (List<Construction>) contents;
            } else if (contents instanceof Block) {
                constructions = ((Block) contents).getConstructions(context);
                if (constructions == null) {
                    constructions = new EmptyList<Construction>();
                }
            } else if (contents instanceof Construction) {
                constructions = new SingleItemList<Construction>((Construction) contents);

            } else if (contents instanceof CollectionDefinition) {
                CollectionInstance collectionInstance = null;
                try {
                    collectionInstance = ((CollectionDefinition) contents).getCollectionInstance(context, null, null);
                } catch (Redirection r) {
                    log(" ******** unable to obtain collection instance for " + (contents == null ? "(anonymous)" : ((CollectionDefinition) contents).getName()) + " ******");
                }
                if (collectionInstance != null) {
                    constructions = new SingleItemList<Construction>((Construction) collectionInstance);
                } else {
                    constructions = new EmptyList<Construction>();
                }
            } else if (contents instanceof Definition) {
                constructions = ((Definition) contents).getConstructions(context);

            } else {
                if (contents != null) log(" ******** unexpected contents: " + contents.getClass().getName() + " ******");
                constructions = new EmptyList<Construction>();
            }
        }
        return constructions;
    }

    public Block getCatchBlock() {
        CantoNode contents = getContents();
        if (contents instanceof Block) {
            return ((Block) contents).getCatchBlock();
        } else {
            return null;
        }
    }

    public String getCatchIdentifier() {
        Block catchBlock = getCatchBlock();
        if (catchBlock != null) {
            return catchBlock.getCatchIdentifier();
        } else {
            return null;
        }
    }

    public ParameterList getParamsForArgs(ArgumentList args, Context context, boolean validate) {
        ParameterList params = null;
        List<ParameterList> paramLists = getParamLists();
        int numParamLists = (paramLists == null ? 0 : paramLists.size());
        if (numParamLists > 1 || validate) {
            params = getMatch(args, context);

        } else if (numParamLists == 1) {
            params = paramLists.get(0);
        }
        return params;
    }


    public Context.Entry getEntryForArgs(ArgumentList args, Context context) throws Redirection {
        ParameterList params = getParamsForArgs(args, context);
        // if args is nonnull and params is null or smaller than args, there is no match.
        if (args != null && args.size() > 0 && (params == null || params.size() < args.size())) {
            throw new Redirection(Redirection.STANDARD_ERROR, "Attempt to reference " + getFullName() + " with incorrect number of arguments");
        }

        return context.newEntry(getDefinitionFlavor(context, params), this, params, args);
    }


    public ParameterList getParamsForArgs(ArgumentList args, Context argContext) {
        return getMatch(args, argContext);
    }

    public Definition getDefinitionForArgs(ArgumentList args, Context argContext) {
        ParameterList params = getParamsForArgs(args, argContext);
        // if args is nonnull and params is null or smaller than args, there is no match.
        if (args != null && args.size() > 0 && (params == null || params.size() < args.size())) {
            return null;
        } else {
            return getDefinitionFlavor(argContext, params);
        }
    }

    protected Definition getDefinitionFlavor(Context context, ParameterList params) {
        List<ParameterList> paramLists = getParamLists();
        if (paramLists != null && paramLists.size() > 1) {
            return new DefinitionFlavor(this, context, params);
        } else {
            return this;
        }
    }

    /** Go through all the parameter lists belonging to this definition and
     *  select the best match (if any) for the specified argument list and context.
     *
     *  Returns the most closely matching parameter list, or null if none matches.
     */
    protected ParameterList getMatch(ArgumentList args, Context argContext) {
        ParameterList params = null;
        List<ParameterList> paramLists = getParamLists(); 
        if (paramLists != null) {
            int score = NO_MATCH;
            Iterator<ParameterList> it = paramLists.iterator();
            while (it.hasNext()) {
                ParameterList p = it.next();
                int s = p.getScore(args, argContext, this);
                if (s < score) {
                    params = p;
                    score = s;
                }
            }
        }
        return params;
    }

    /** Go through all the parameter lists belonging to this definition and
     *  all the passed argument lists and select the best match (if any).
     *
     *  Returns an array of ListNodes with two elements, a ParameterList
     *  and an ArgumentList, or null if none matches.
     */
    public ListNode<?>[] getMatch(ArgumentList[] argLists, Context argContext) {
        ListNode<?>[] paramsAndArgs = null; 
        ListNode<?> args = null;
        ListNode<?> params = null;
        List<ParameterList> paramLists = getParamLists(); 
        if (paramLists != null) {
            int score = NO_MATCH;
            Iterator<ParameterList> itParams = paramLists.iterator();
            while (itParams.hasNext()) {
                ParameterList p = itParams.next();
                for (int i = 0; i < argLists.length; i++) {
                    ArgumentList a = argLists[i];
                    int s = p.getScore(a, argContext, this);
                    if (s < score) {
                        params = p;
                        args = a;
                        score = s;
                    }
                }
                
            }
        }
        if (params != null || args != null) {
            paramsAndArgs = new ListNode<?>[2];
            paramsAndArgs[0] = params;
            paramsAndArgs[1] = args;
        }
        return paramsAndArgs;
    }

    /** Always returns false; an anonymous definition cannot be an identity. */
    public boolean isIdentity() {
        return false;
    }

    /** Always returns false; an anonymous definition cannot be a formal parameter definition. */
    public boolean isFormalParam() {
        return false;
    }
    
    /** Always returns false; an anonymous definition cannot be an alias. */
    public boolean isAlias() {
        return false;
    }

    /** Always returns null; an anonymous definition cannot be an alias. */
    public NameNode getAlias(){
        return null;
    }

    /** Always returns false; an anonymous definition cannot be an alias. */
    public boolean isParamAlias() {
        return false;
    }

    /** Always returns null; an anonymous definition cannot be an alias. */
    public NameNode getParamAlias(){
        return null;
    }

    /** Always returns null; an anonymous definition cannot be an alias. */
    public Instantiation getAliasInstance() {
        return null;
    }

    /** Always returns false; an anonymous definition cannot be an alias. */
    public boolean isAliasInContext(Context context) {
        return false;
    }

    /** Always returns null; an anonymous definition cannot be an alias. */
    public NameNode getAliasInContext(Context context) {
        return null;
    }
    
    /** Always returns null; an anonymous definition cannot be an alias. */
    public Instantiation getAliasInstanceInContext(Context context) {
        return null;
    }
    
    /** Always returns false; an anonymous definition cannot be a reference. */
    public boolean isReference() {
        return false;
    }

    /** Always returns null; an anonymous definition cannot be a reference. */
    public NameNode getReference() {
        return null;
    }

    /** Returns true if this is an object definition; the base class returns false by default. */
    public boolean isObject() {
        return false;
    }
    
    /** Returns true if this is a collection; the base class returns false by default. */
    public boolean isCollection() {
        return false;
    }

    /** Returns true if this is an array; the base class returns false by default. */
    public boolean isArray() {
        return false;
    }

    /** Returns true if this is a table; the base class returns false by default. */
    public boolean isTable() {
        return false;
    }

    public CollectionDefinition getCollectionDefinition(Context context, ArgumentList args) throws Redirection {
        return null;
    }

    public List<Dim> getDims() {
        return null;
    }
    
    protected String getTypeAndName() {
        StringBuffer sb = new StringBuffer();
        
        switch (access) {
            case LOCAL_ACCESS:
                sb.append("local ");
                break;
            case PUBLIC_ACCESS:
                sb.append("public ");
                break;
        }

        Type type = getSuper();
        if (type != null) {
            sb.append(type.getName());
            sb.append(' ');
        }

        NameNode nameNode = getNameNode();
        if (nameNode != null) {
            String name = nameNode.toString("");
            if (name != null && !name.equals(Name.ANONYMOUS)) {
                sb.append(name);
                List<Dim> dims = getDims();
                if (dims != null && dims.size() > 0) { 
                    Iterator<Dim> it = dims.iterator();
                    while (it.hasNext()) {
                        it.next().toString();
                    }
                }
                sb.append(' ');
            }
        }
        return sb.toString();        
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(prefix);
        boolean hasName = false;

        String typeAndName = getTypeAndName();
        if (typeAndName.length() > 0) {
            hasName = true;
            sb.append(typeAndName);
        }

        AbstractNode contents = getContents();  
        if (contents != null & !(contents instanceof Block)) {
            if (hasName) {
                sb.append("= ");
            }
            // in this case, don't append the prefix to the content
            sb.append(contents.toString(""));
            sb.append('\n');
        } else {
            sb.append(super.toString(prefix));
        }
        return sb.toString();
    }

    public String getStringConstant(String name, String valueIfNotFound) {
        Definition d = getChildDefinition(new NameNode(name), null);
        if ( d != null ) {
            AbstractNode contents = d.getContents();
            if (( contents != null ) && ( contents instanceof ParsedStringLiteral )) {
                return (String)((ParsedStringLiteral)contents).getValue();
            }
        }
        return valueIfNotFound;
    }
    
    /** Returns an object wrapping this definition with arguments and indexes. */ 
    public DefinitionInstance getDefInstance(ArgumentList args, List<Index> indexes) {
        return new DefinitionInstance(this, args, indexes);
    }

    public void deserialize(String ser) {
        System.out.println("******* deserialize: " + ser);
    }
    
}
