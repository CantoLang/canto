/* Canto Compiler and Runtime Engine
 * 
 * Instantiation.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.*;

/**
* An Instantiation is a construction based on a definition.
*
* @author Michael St. Hippolyte
*/

public class Instantiation extends AbstractConstruction implements ValueGenerator /*, ConstructionGenerator */ {

    //
    // Different kinds of instantiation
    //

    public final static int UNRESOLVED = -1;
    public final static int STATICALLY_RESOLVED = 0;
    public final static int LOCAL = 1;
    public final static int PARAMETER = 2;
    public final static int PARAMETER_CHILD = 3;
    public final static int FOR_PARAMETER = 4;
    public final static int FOR_PARAMETER_CHILD = 5;
    public final static int CONTAINER_PARAMETER = 6;
    public final static int CONTAINER_PARAMETER_CHILD = 7;
    public final static int EXTERNALLY_RESOLVED = 8;
    public final static int EXPLICITLY_RESOLVED = 9;
    public final static int CLASS_RESOLVED = 10;
    public final static int DYNAMICALLY_RESOLVED = Integer.MAX_VALUE;

    // the following two values inclusively bound the range of all
    // parameter kinds

    public final static int PARAMETER_RANGE_LOW = 2;
    public final static int PARAMETER_RANGE_HIGH = 7;

    //
    // Pre-determined definitions and properties
    //

    /** If a <code>local</code> definition for this instantiation can be determined at compile time,
     *  this field will point to it.
     */
    protected Definition localDef = null;

    /** If this instantiation refers to an explicit (fully qualified) definition, this field will point
     *  to it (unless there is a <code>local</code> definition for this instantiation, or a matching
     *  parameter, in which case no effort will be made to find an explicit definition).
     */
    protected Definition explicitDef = null;

    /** If a definition for this instantiation exists in the class hierarchy, this field will point
     *  to it (unless there is a matching parameter for this instantiation, or a<code>local</code>
     *  or explicit definition, in which case no effort will be made to find a class definition).
     */
    protected Definition classDef = null;

    /** If an external definition for this instantiation can be determined at compile time,
     *  this field will point to it.
     */
    protected Definition externalDef = null;

    /** True if this is the instantiation of a parameter.
     */
    public boolean isParam = false;

    /** True if this is the instantiation of a child of a parameter.
     */
    public boolean isParamChild = false;

    /** The scope and type of resolution for this instantiation.  Until the
     *  <code>resolve</code> method is called, the value is UNRESOLVED; after
     *  that, it is STATICALLY_RESOLVED, LOCAL, PARAMETER, PARAMETER_CHILD,
     *  FOR_PARAMETER, FOR_PARAMETER_CHILD, CONTAINER_PARAMETER,
     *  CONTAINER_PARAMETER_CHILD or DYNAMICALLY_RESOLVED.
     */
    protected int kind = UNRESOLVED;

    protected CantoNode reference;
    protected ArgumentList args = null;
    protected List<Index> indexes = null;

    public Instantiation() {
        super();
    }

    public Instantiation(Object reference) {
        // this instantiation is unowned
        super();
        setReference(reference);
    }

    public Instantiation(Object reference, Definition owner) {
        super();
        setOwner(owner);
        setReference(reference);
        resolve(null);
    }

    public Instantiation(Object reference, ArgumentList args, List<Index> indexes) {
        // this instantiation is unowned
        super();
        setReference(reference);
        setArguments(args);
        setIndexes(indexes);
    }
    
    public Instantiation(Object reference, ArgumentList args, List<Index> indexes, Definition owner) {
        super();
        setOwner(owner);
        setReference(reference);
        setArguments(args);
        setIndexes(indexes);
        resolve(null);
    }

    
    /** Copies an instantiation but substitues different indexes and arguments. **/
    public Instantiation(Instantiation instance, ArgumentList args, List<Index> indexes) {
        super(instance);
        this.localDef = instance.localDef;
        this.explicitDef = instance.explicitDef;
        this.classDef = instance.classDef;
        this.externalDef = instance.externalDef;
        this.isParam = instance.isParam;
        this.isParamChild = instance.isParamChild;
        this.kind = instance.kind;
        this.reference = instance.reference;
        setArguments(args);
        setIndexes(indexes);
    }

    public boolean isPrimitive() {
        return false;
    }
    
    public boolean isSuper() {
        return Name.SUPER.equals(getName());
    }

    protected void setReference(Object obj) {
        if (obj instanceof CantoNode) {
            reference = (CantoNode) obj;
        } else {
            reference = new PrimitiveValue(obj);
        }
        if (reference instanceof NameNode) {
            setArguments(((NameNode) reference).getArguments());
            setIndexes(((NameNode) reference).getIndexes());
        }
        if (reference instanceof ValueGenerator) {
            // value generators are inherently dynamic
            setDynStat(true, false);

        } else if (reference instanceof Value) {
            // values are inherently static
            setDynStat(false, true);
        }
        String name = reference.getName();
        if (name == null || name.equals(Name.ANONYMOUS)) {
        	setAnonymous(true);
        }
    }

    public CantoNode getReference() {
        return reference;
    }

    public NameNode getReferenceName() {
        if (reference instanceof NameNode) {
            return (NameNode) reference;
        } else if (reference instanceof Definition) {
            return ((Definition) reference).getNameNode();
        } else {    
            return null;
        }
    }

    public void setArguments(ArgumentList args) {
        this.args = args;
    }

    /** Returns the list of arguments this instantiation has, if any. */
    public ArgumentList getArguments() {
        return args;
    }

    public void addIndexes(List<Index> indexes) {
        if (indexes != null) {
            if (this.indexes != null) {
                this.indexes.addAll(indexes);
            } else {
                this.indexes = indexes;
            }
        }
    }

    public void setIndexes(List<Index> indexes) {
        this.indexes = indexes;
    }

    /** Returns the list of indexes this instantiation has, if any. */
    public List<Index> getIndexes() {
        return indexes;
    }

    /** Returns true if this instantiation constructs a next **/
    public boolean hasNext() {
        if (getName().equals(Name.NEXT)) {
            return true;
        }
        ArgumentList args = getArguments();
        if (args != null) {
            Iterator<Construction> it = args.iterator();
            while (it.hasNext()) {
                Construction arg = it.next(); 
                if (arg instanceof AbstractConstruction) {
                    if (((AbstractConstruction) arg).hasNext()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
   

    /** Returns true if this instantiation constructs a sub **/
    public boolean hasSub() {
        if (getName().equals(Name.SUB)) {
            return true;
        }
        ArgumentList args = getArguments();
        if (args != null) {
            Iterator<Construction> it = args.iterator();
            while (it.hasNext()) {
                Construction arg = it.next(); 
                if (arg instanceof AbstractConstruction) {
                    if (((AbstractConstruction) arg).hasSub()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
   
   
   
    /** Return a code indicating the kind of instantiation this is, which will be
     *  one of the following values:
     *  <pre>
     *      UNRESOLVED                 Value before <code>resolve</code> is called
     *      STATICALLY_RESOLVED        The definition being instantiated is known ahead of time
     *      LOCAL                      The definition being instantiated is local
     *      PARAMETER                  A parameter is being instantiated
     *      PARAMETER_CHILD            The child of a parameter is being instantiated
     *      FOR_PARAMETER              A parameter in a <code>for</code> loop
     *      FOR_PARAMETER_CHILD        The child of a parameter in a <code>for</code> loop
     *      CONTAINER_PARAMETER        A parameter in the container
     *      CONTAINER_PARAMETER_CHILD  The child of a parameter in the container
     *      DYNAMICALLY_RESOLVED       The definition must be resolved in context
     *  </pre>
     */
    public int getKind() {
        return kind;
    }
    
    public void setKind(int kind) {
        this.kind = kind;
        isParam = (kind == PARAMETER || kind == FOR_PARAMETER || kind == CONTAINER_PARAMETER);
        isParamChild = (kind == PARAMETER_CHILD || kind == FOR_PARAMETER_CHILD || kind == CONTAINER_PARAMETER_CHILD);
    }
    
    private static int childOfKind(int k) {
        switch (k) {
            case PARAMETER:
            case PARAMETER_CHILD:
                return PARAMETER_CHILD;
            case FOR_PARAMETER:
            case FOR_PARAMETER_CHILD:
                return FOR_PARAMETER_CHILD;
            case CONTAINER_PARAMETER:
            case CONTAINER_PARAMETER_CHILD:
                return CONTAINER_PARAMETER_CHILD;
        }
        return k;        
    }

    /** Collect all information that can determined apart from any context, including a
     *  non-overridable definition, if it exists, and whether or not this is a reference
     *  to a parameter or the child of a parameter.
     */
    @SuppressWarnings("unchecked")
    synchronized public void resolve(Object forParamDef) {
        kind = UNRESOLVED;
        if (reference instanceof Definition && !(reference instanceof DefParameter)) {
            kind = STATICALLY_RESOLVED;
            return;
        }
        Definition owner = getOwner();
        while (owner != null && !(owner instanceof NamedDefinition)) {
            owner = owner.getOwner();
        }
        if (owner == null) {
            return;
        }

        NamedDefinition ndef = (NamedDefinition) owner;
        NameNode name = getReferenceName();
        if (name == null) {
            return;
        }
        
        String checkName = name.getName();
        boolean hasDot = (checkName.indexOf('.') > -1);

        vlog("Resolving " + checkName + " in " + ndef.getFullName() + "...");
        
        // first check for statement parameters, if any
        if (forParamDef != null) {
            NameNode forParamName;
            if (forParamDef instanceof DefParameter) {
                forParamName = ((DefParameter) forParamDef).getNameNode();
                if (hasDot) {
                    if (checkName.startsWith(forParamName.getName() + '.')) {
                        isParamChild = true;
                        vlog("   ..." + checkName + " refers to the child of a for statement parameter");
                        kind = FOR_PARAMETER_CHILD;
                        return;
                    }
                } else {
                    if (checkName.equals(forParamName.getName())) {
                        isParam = true;
                        vlog("   ..." + checkName + " refers to a for statement parameter");
                        kind = FOR_PARAMETER;
                        return;
                    }
                }
            } else if (forParamDef instanceof List<?>) {
                Iterator<DefParameter> it = ((List<DefParameter>) forParamDef).iterator();
                while (it.hasNext()) {
                    forParamName = ((DefParameter) it.next()).getNameNode();
                    if (hasDot) {
                        if (checkName.startsWith(forParamName.getName() + '.')) {
                            isParamChild = true;
                            vlog("   ..." + checkName + " refers to the child of a for statement parameter");
                            kind = FOR_PARAMETER_CHILD;
                            return;
                        }
                    } else {
                        if (checkName.equals(forParamName.getName())) {
                            isParam = true;
                            vlog("   ..." + checkName + " refers to a for statement parameter");
                            kind = FOR_PARAMETER;
                            return;
                        }
                    }
                }
            }
        }

        // not a for statement parameter; check the container chain's parameters for a match
        for (NamedDefinition nd = ndef; nd != null; nd = (NamedDefinition) nd.getOwner()) {
            List<ParameterList> paramLists = nd.getParamLists();
            if (paramLists != null) {
                int numLists = paramLists.size();

                // if there is a dot, this can't be a parameter but might be the child of a parameter;
                // if there is no dot, this can be a parameter but cannot be the child of a parameter.
                if (hasDot) {
                    for (int i = 0; i < numLists; i++) {
                        ParameterList params = paramLists.get(i);
                        int n = params.size();
                        for (int j = 0; j < n; j++) {
                            DefParameter param = (DefParameter) params.get(j);
                            if (checkName.startsWith(param.getName() + '.')) {
                                isParamChild = true;
                                if (nd.equals(ndef)) {
                                    vlog("   ..." + checkName + " refers to the child of a parameter");
                                    kind = PARAMETER_CHILD;
                                    return;
                                } else {
                                    vlog("   ..." + checkName + " refers to the child of a parameter in the container");
                                    kind = CONTAINER_PARAMETER_CHILD;
                                    break;
                                }
                            }
                        }
                        if (kind != UNRESOLVED) {
                            break;
                        }
                    }
                } else {
                    for (int i = 0; i < numLists; i++) {
                        ParameterList params = paramLists.get(i);
                        int n = params.size();
                        for (int j = 0; j < n; j++) {
                            DefParameter param = (DefParameter) params.get(j);
                            if (checkName.equals(param.getName())) {
                                isParam = true;
                                if (nd.equals(ndef)) {
                                    vlog("   ..." + checkName + " refers to a parameter");
                                    kind = PARAMETER;
                                    return;
                                } else {
                                    vlog("   ..." + checkName + " refers to a parameter in the container");
                                    kind = CONTAINER_PARAMETER;
                                    break;
                                }
                            }
                        }
                        if (kind != UNRESOLVED) {
                            break;
                        }
                    }
                }
                if (kind != UNRESOLVED) {
                    break;
                }
            }
        }

        if (kind != UNRESOLVED) {
            return;

        // formal parameters are dynamically resolved
        } else if (ndef.isFormalParam()) {
            kind = DYNAMICALLY_RESOLVED;
            return;
        }
        
        Definition def = null;
        ComplexDefinition container = null;
        if (ndef instanceof ComplexDefinition) {
            container = (ComplexDefinition) ndef;
        } else {
            owner = ndef.getOwner();
            while (owner != null && !(owner instanceof ComplexDefinition)) {
                owner = owner.getOwner();
            }
            container = (ComplexDefinition) owner;
        }
        if (container != null) {
            def = container.getExplicitChildDefinition(name);
            if (def != null) {
                if (def.isFormalParam()) {
                    if (hasDot) {
                        isParamChild = true;
                        vlog("   ..." + checkName + " refers to the child of a parameter in the container");
                        kind = CONTAINER_PARAMETER_CHILD;
                    } else {
                        isParam = true;
                        vlog("   ..." + checkName + " refers to a parameter in the container");
                        kind = CONTAINER_PARAMETER;
                    }

                } else if (def.getAccess() == Definition.LOCAL_ACCESS) {
                    localDef = def;
                    vlog("   ..." + checkName + " refers to local definition " + def.getFullName());
                    kind = LOCAL;
                } else if (checkName.equals(def.getFullName())) {
                    explicitDef = def;
                    vlog("   ..." + checkName + " is an explicit definition reference");
                    kind = EXPLICITLY_RESOLVED;
                } else {
                    classDef = def;
                    vlog("   ..." + checkName + " refers to class definition " + def.getFullName());
                    kind = CLASS_RESOLVED;
                }
                if (def.isExternal()) {
                    kind = EXTERNALLY_RESOLVED;
                }
            } else {
                classDef = container.getClassDefinition(name);
                if (classDef != null) {
                    vlog("   ..." + checkName + " refers to class definition " + classDef.getFullName());
                    kind = CLASS_RESOLVED;
                } else {
                    while (def == null) {
                        NamedDefinition superdef = container.getSuperDefinition(null);
                        while (superdef != null) {
                            def = superdef.getExplicitChildDefinition(name);
                            if (def != null) {
                                break;
                            }
                            superdef = superdef.getSuperDefinition(null);
                        }
                        if (def != null) {
                            break;
                        }
                        container = (ComplexDefinition) container.getOwner();
                        if (container == null) {
                            break;
                        }
                        def = container.getExplicitChildDefinition(name);
                    }
                    if (def != null) {
                        kind = DYNAMICALLY_RESOLVED;
                        vlog("   ..." + checkName + " requires a context to resolve");
                    } 
                }
            }
        }
        if (kind == UNRESOLVED) {
            kind = DYNAMICALLY_RESOLVED;
            DefinitionTable defTable = ((NamedDefinition) owner).getDefinitionTable();
            def = defTable.getDefinition((NamedDefinition) owner, getReferenceName());
            if (def != null) {
                kind = EXPLICITLY_RESOLVED;
                vlog("   ..." + checkName + " is an explicit reference");
            }
        }
    }

    /** Returns this instantiation's type in this context. */
    public Type getType(Context context, boolean generate) {
        return getType(context, null, generate, false);
    }


    /** Returns this instantiation's type in this context, narrowly determined; i.e.,
     *  if this is a argument, rather than returning the type specified by the parameter,
     *  this method returns the actual runtime type of the argument, which may be a
     *  subtype of the parameter type.
     */
    public Type getNarrowType(Context context, Definition resolver) {
        return getType(context, resolver, false, true);
    }


    private Type getType(Context context, Definition resolver, boolean generate, boolean narrow) {
        if (reference instanceof Definition) {
            return ((Definition) reference).getType();

        } else if (reference instanceof PrimitiveValue) {
            return ((PrimitiveValue) reference).getType();

        } else if (reference instanceof Expression) {
            return ((Expression) reference).getType(context, generate);
            
        } else {

            // unless the narrow flag is true, check explicitly for a parameter type before
            // calling getDefinition.  The reason for this is that getDefinition does not
            // return the parameter definition, but the definition of the argument which the
            // parameter resolves to, which may be a subtype of the actual parameter type.
            if (!narrow && reference instanceof NameNode && context != null) {
                NameNode name = (NameNode) reference;
                Type paramType = context.getParameterType(name, isContainerParameter(context));
                if (paramType != null) {
                    return paramType;
                }
            }

            // it's not a parameter, get the definition and return its type
            Definition def = getDefinition(context, resolver, false);
            if (def != null) {
                if (def instanceof ElementReference) {
                    try {
                        Definition elementDef = ((ElementReference) def).getElementDefinition(context);
                        if (elementDef != null) {
                            def = elementDef;
                        }
                    } catch (Redirection r) {
                        ;
                    }
                }
                return def.getType();
            }
        }
        return null;
    }

    /** Returns the name of the definition being constructed */
    public String getDefinitionName() {
        // this covers references that are Definitions as well because
        // Definition extends Name.
        if (reference instanceof Name) {
            return ((Name) reference).getName();
        } else {
            return null;
        }
    }

    /** Returns the cacheability of this construction in the specified context.  The
     *  possible return values are CACHE_STORABLE, CACHE_RETRIEVABLE, NOT_CACHEABLE or
     *  FULLY_CACHEABLE, depending on whether the construction can be stored in the cache,
     *  retrieved from the cache, neither or both.  The base class always returns
     *  NOT_CACHEABLE; only named constructions can be cached.
     */
    protected CacheabilityInfo getCacheability(Context context, Definition def) {
        if (reference instanceof Definition) {
            // it would be possible to add logic here to identify cacheable
            // explicit-definition instantiations, but for now we don't
            // cache them
            return NOT_CACHEABLE_INFO;

        } else if (reference instanceof NameNode) {
            NameNode nameNode = (NameNode) reference;

            // We don't cache collection element references, just the collection objects.
            // This could be changed to look at the cacheability of the indexes.
            if (nameNode.hasIndexes()) {
                return NOT_CACHEABLE_INFO;

            // We also don't cache count references
            } else if (Name.COUNT.equals(nameNode.getLastPart().getName())) {
                return NOT_CACHEABLE_INFO;
            }

            String name = nameNode.getName();
            if (name != null && name.length() > 0) {
                if (def == null) { // && !(isParam || isParamChild)) {
                    def = getDefinition(context);
                }
                ArgumentList args = getArguments();
                int cacheability;
                if (def == null) {
                    return NOT_CACHEABLE_INFO;

                } else if (def.getDurability() == Definition.DYNAMIC) {
                    cacheability = CACHE_STORABLE;

                // fully caching all parameters would break container parameter references and for loops.
                // but somehow recursive functions work
                } else if (isParam || isParamChild) {
                    boolean inContainer = isContainerParameter(context);
                    if (/* inContainer || */ isForParameter()) {
                        cacheability = CACHE_STORABLE;
                    } else {
                        cacheability = FULLY_CACHEABLE;
                        boolean dynamic = false;
                        if (isParam) {
                            Object arg;
                            ArgumentList argArgs = null;
                            arg = context.getArgumentForParameter(nameNode, false, inContainer);
                            if (arg != null) {
                                if (arg instanceof ArgumentList) {
                                    dynamic = ((ArgumentList) arg).isDynamic();
                                } else if (arg != ArgumentList.MISSING_ARG && arg instanceof Instantiation) {
                                    argArgs = ((Instantiation) arg).getArguments();
                                    if (argArgs != null) {
                                        dynamic = argArgs.isDynamic();
                                    }
                                }
                            }

                        } else {
                            dynamic = nameNode.isDynamic();
                        }
                        if (dynamic) {
                            cacheability = CACHE_STORABLE;
                        }
                    }

                } else {
                    cacheability = FULLY_CACHEABLE;
                }

                // if this is a dynamic or concurrent instantiation, it shouldn't be fully cached
                if (cacheability == FULLY_CACHEABLE && args != null && (args.isDynamic() || args.isConcurrent())) {
                    cacheability = CACHE_STORABLE;
                }
                
                return new CacheabilityInfo(cacheability, def);
            }
        }
        return NOT_CACHEABLE_INFO;
    }

    public boolean isParameter() {
        return isParam;
    }
    
    public boolean isParameterKind() {
        return (kind >= PARAMETER_RANGE_LOW && kind <= PARAMETER_RANGE_HIGH);
    }

    public boolean isContainerParameter(Context context) {
        return (kind == CONTAINER_PARAMETER || kind == CONTAINER_PARAMETER_CHILD
                || (!context.peek().isInLoop() && (kind == FOR_PARAMETER || kind == FOR_PARAMETER_CHILD)));
        
    }
    
    public boolean isForParameter() {
        return (kind == FOR_PARAMETER || kind == FOR_PARAMETER_CHILD);
        
    }

    public boolean isParameterChild() {
        return (kind == PARAMETER_CHILD || kind == CONTAINER_PARAMETER_CHILD || kind == FOR_PARAMETER_CHILD);
    }

//   private boolean isForParam() {
//       return (kind == FOR_PARAMETER || kind == FOR_PARAMETER_CHILD);
//   }

//   private boolean isInCollection() {
//       return (indexes != null);
//   }

    public String getNameModifier() {
        return getNameModifier(getArguments(), getIndexes());
    }

    public static String getNameModifier(ArgumentList args, List<Index> indexes) {
        String modifier = null;
        //if (args != null && args.size() > 0) {
        //    modifier = args.toString();
        //}
        if (indexes != null && indexes.size() > 0) {
            if (modifier == null) {
                modifier = "";
            }
            Iterator<Index> it = indexes.iterator();
            while (it.hasNext()) {
                modifier = modifier + it.next().toString();
            }
        }
        return modifier;
    }


   /** Returns the definition associated with this instance in the given context
    *  on behalf of another definition (an alias, for example).
    */
   public Definition getDefinition(Context context) {
       return getDefinition(context, null, false);
   }


   /** Returns the definition associated with this instance in the given context. */
   public Definition getDefinition(Context context, Definition resolver, boolean localScope) {
       if (reference instanceof Definition) {
           return (Definition) reference;

       // resolution sequence:
       //
       // 1. local
       // 2. parameter
       // 3. child of a parameter
       // 4. lookup

       } else if (localDef != null) {
           return localDef;

       } else try {
if (((NameNode)reference).getName().equals("m")) {
  System.out.println("inst 755");
}
           if (isParam || isParamChild) {
               return context.getParameterDefinition((NameNode) reference, isContainerParameter(context));
       
           } else {
               return (Definition) lookup((NameNode) reference, context, false, resolver, localScope);
           }
       } catch (Redirection r) {
           return null;
       }
   }

   /** Returns the fully dereferenced definition associated with this instance in the given context. */
   public Definition getUltimateDefinition(Context context) {
       Definition def = getDefinition(context);
       if (def != null) {
           def = def.getUltimateDefinition(context);
       }
       return def;
   }

   /** If this is a parameter, and the associated argument in the given context is an instantiation,
    *  then return it, otherwise return this instantiation. */
   public Instantiation getUltimateInstance(Context context) throws Redirection {
	   if (isParam || isParamChild) {
           NameNode name = (NameNode) reference;
           Object arg = context.getArgumentForParameter(name, isParamChild, isContainerParameter(context));
           if (arg != null && arg != ArgumentList.MISSING_ARG) {
               if (arg instanceof ArgumentList) {
                   arg = ((ArgumentList) arg).get(0);
               }
               if (arg instanceof Instantiation) {
                   Instantiation instance = (Instantiation) arg;
                   if (!instance.isAnonymous()) {
                       int kind = instance.getKind();
                       NameNode newName = null;
                       if (isParamChild) {
                           kind = childOfKind(kind);
                           NameNode childName = new ComplexName(name, 1, name.numParts());
                           newName = new ComplexName(instance.getReferenceName(), childName);
                       } else if (name.hasIndexes()) {
                           NameNode argName = instance.getReferenceName();
                           int numParts = argName.numParts();
                           if (numParts > 1) {
                               NameNode newSuffix = new NameWithIndexes(argName.getLastPart().getName(), instance.getArguments(), name.getIndexes());
                               NameNode newPrefix = new ComplexName(argName, 0, numParts - 1);
                               newName = new ComplexName(newPrefix, newSuffix);
                           } else {
                               newName = new NameWithIndexes(argName.getName(), instance.getArguments(), name.getIndexes());
                           }
                       }
                       
                       if (newName != null) {
                           Definition instanceOwner = instance.getOwner();
                           if (instanceOwner == null) {
                               instanceOwner = getOwner();
                           }
                           instance = new Instantiation(newName, instanceOwner);
                           instance.setKind(kind);
                       }
                       if (instance != null && instance != this && context.size() > 1) {
                           int numUnpushes = 0;
                           int limit = context.size() - 1;
                           try {
                               while (!context.paramIsPresent(name) && numUnpushes < limit) {
                                   context.unpush();
                                   numUnpushes++;
                               }
                               if (numUnpushes >= limit) {
                                   return instance;
                               }
                               context.unpush();
                               numUnpushes++;
                               return instance.getUltimateInstance(context);
                           } finally {
                               while (numUnpushes-- > 0) {
                                   context.repush();
                               }
                           }
                       }
                   }
               }
           }
       }
       return this;
   }
   
   
    /** Finds the definition for this instantiation in the given context,
     *  and either returns the definition or instantiates it and returns
     *  the generated data, depending on the value of the passed flag.  If the flag is
     *  false, the return value will be an instance of Definition, or null if no definition
     *  is found.  If the flag is true, the type of the return value will depend on the
     *  definition; if no definition is found, AbstractNode.UNDEFINED, which is a static
     *  instance of Object, is returned.
     */
    private Object lookup(NameNode name, Context context, boolean generate, Definition resolver, boolean localScope) throws Redirection {
        if (context == null) {
            throw new Redirection(Redirection.STANDARD_ERROR, "Instantiation requires a context; none provided.");
        }
        
        Object data = UNDEFINED;
        Definition def = null;
        NameNode restOfName = null;
        List<Index> indexes = getIndexes();
        ArgumentList args = getArguments();
        List<Index> prefixIndexes = null;
        ArgumentList prefixArgs = null;
 
        NamedDefinition owner = (NamedDefinition) getOwner().getSubdefInContext(context);

        int numPushes = 0;
        
       // This was breaking a test, so I commented it out.
       //
        // if this is an index, get the owner's owner, since the owner
        // is the element itself.
       // if (getParent() instanceof Index) {
       //     owner = (NamedDefinition) owner.getOwner();
       // }


        boolean dereferencedIndexes = false;
        try {

            // check for a cached definition if this is unadorned
            String nm = name.getName();
            Holder holder = null;
//            if (name.isComplex()) {
//                holder = context.getDefHolder(nm, nm, args, indexes, false);
//            } else if (!generate) {
//                holder = context.getDefHolder(nm, nm, args, indexes, false);
//            }
//            if (holder != null 
//                    && holder.nominalDef != null
//                    && (holder.def.getName().equals(nm) || nm.endsWith("." + holder.def.getName()))
//                    && holder.nominalDef.getDurability() != Definition.DYNAMIC
//                    && !((CantoNode) holder.nominalDef).isDynamic() 
//                    && (args == null || !args.isDynamic())
//                    && !name.hasIndexes()
//                    && !holder.nominalDef.equals(context.getDefiningDef())) {
//                def = holder.nominalDef;
//                if (args == null) {
//                    args = holder.nominalArgs;
//                }
//            }

            if (def == null) {
            	int n = name.numParts();
                
                if (n == 1) {
                    prefixArgs = args;
                    prefixIndexes = indexes;
    
                // if this is a multipart name, only look up the first part, then
                // resolve the rest directly using the found definition.  But since
                // this process may lead to a false negative (if some sequence of
                // parts at the beginning of the name resolves but then peters out),
                // also look up the explicit or external definition, if one exists.
                // This will  be overridden by any definition discovered through the
                // standard process, but will take effect if no such definition exists.

                } else {
                    DefinitionTable definitions = getComplexOwner().getDefinitionTable();
                    def = definitions.getDefinition(null, name);
                    if (def != null) {
                        if (def instanceof PartialDefinition) {
                            def = ((PartialDefinition) def).completeForContext(context);
                            if (def == null) {
                                return (generate ? UNDEFINED : null);
                            }
                        }
                    } else {
                        for (int np = n - 1; np > 0; np--) {
                        //for (int np = 1; np < n; np++) {
                            ComplexName prefix = new ComplexName(name, 0, np);
                            restOfName = new ComplexName(name, np, n);
                            nm = prefix.getName();
                            prefixIndexes = prefix.getIndexes();
                            prefixArgs = prefix.getArguments();
                            //if (prefixIndexes == null) {
                                holder = context.getDefHolder(nm, null, prefixArgs, prefixIndexes, false);
                                if (holder != null && holder.nominalDef != null && !holder.nominalDef.equals(context.getDefiningDef())) {
                                    def = holder.nominalDef;
                                    if (holder.nominalDef.getDurability() != Definition.DYNAMIC && !((CantoNode) holder.nominalDef).isDynamic() && (args == null || !args.isDynamic())) {
                                        if (holder.data != null && holder.data != UNDEFINED) {
                                            data = holder.data;
                                            // the following breaks a bunch of pent_game_tests for some reason
                                            //if (data instanceof CantoObjectWrapper && (n - np) == 1 && generate) {
                                            //    return ((CantoObjectWrapper) data).getChildData(restOfName);
                                            //}
                                        }
                                        if (prefixArgs == null && (holder.nominalArgs != null || holder.args != null)) {
                                            prefixArgs = (holder.nominalArgs != null ? holder.nominalArgs : holder.args);
                                            if (prefixArgs.isDynamic()) {
                                                prefixArgs = new ArgumentList(prefixArgs);
                                                prefixArgs.setDynamic(false);
                                            }
                                        }
                                        if (holder.resolvedInstance != null) {
                                            numPushes += context.pushParts(holder.resolvedInstance);
                                        } else if (np > 1) {
                                            numPushes += context.pushParts(prefix, np - 1, getOwner());
                                        }
                                        ParameterList nominalParams = holder.nominalDef.getParamsForArgs(holder.nominalArgs, context);
                                        context.push(holder.nominalDef, nominalParams, holder.nominalArgs, true);
                                        //ParameterList params = holder.def.getParamsForArgs(holder.args, context);
                                        //context.push(holder.def, params, holder.args, true);
                                        numPushes++;
                                    }
                                    break;
                                }
                            //}
                        }                           
                                                 

//                        restOfName = name.getLastPart();                  
//                        name = new ComplexName(name, 0, n - 1);
                        if (def == null) {
                            restOfName = new ComplexName(name, 1, n);
                            name = name.getFirstPart();
                            prefixIndexes = name.getIndexes();
                            prefixArgs = name.getArguments();
                        }
                        
                        
//                        if (args == null && indexes == null) {
//                            nm = name.getName();
//                            holder = context.getDefHolder(nm, nm, args, indexes, false);
//                            if (holder != null && holder.def != null && holder.def.getDurability() != Definition.DYNAMIC && !((CantoNode) holder.def).isDynamic() && !holder.def.equals(context.getDefiningDef())) {
//                                def = holder.def;
//                                args = holder.args;
//                                if (holder.data != null && holder.data != UNDEFINED) {
//                                    data = holder.data;
//                                }
//                                vlog("...found cached definition for part of name (" + nm + ") rest of name: " + restOfName.getName());
//
//                                // this appears to be redundant
////                                ParameterList nominalParams = holder.nominalDef.getParamsForArgs(holder.nominalArgs, context);
////                                context.push(holder.nominalDef, nominalParams, holder.nominalArgs, true);
////                                numPushes++;
////                                if (holder.resolvedInstance != null) {
////                                    numPushes += context.pushParts(holder.resolvedInstance);
////                                }
//                            }
//                        }

                    }
                }
            }
            
            
            // resolution sequence
            //
            // The first three have already been checked by the caller:
            //
            // 1. local
            // 2. parameter
            // 3. child of a parameter
            //
            // Here we will check the remaining possibilities:
            //
            // 4. subclass or superclass
            // 5. container local
            // 6. container superclass
            // 7. subclass or superclass container
            // 8. joined site
            // 9. explicit
            // 10. external

            //vlog("looking up definition for " + name);

//            if (args == null && indexes == null) {
//                nm = name.getName();
//                holder = context.getDefHolder(nm, nm, args, false);
//                if (holder != null && holder.def != null && holder.def.getDurability() != Definition.DYNAMIC && !((CantoNode) holder.def).isDynamic() && !holder.def.equals(context.getDefiningDef())) {
//                    def = holder.def;
//                    args = holder.args;
//                    if (restOfName != null) {
//                        vlog("...found cached definition for part of name (" + nm + ") rest of name: " + restOfName.getName());
//                    } else {
//                        vlog("...found cached definition for name: " + nm);
//                    }
//                }
//            }
            
            if (def == null) {
                def = lookupDef(name, prefixIndexes, getParent(), owner, classDef, context, resolver, localScope);
                dereferencedIndexes = true;
                if (def == null) {
                    return (generate ? UNDEFINED : null);
                }
            }

            // if the name is multipart, look up the rest of the name
            // note: remove the exDef check if you want Canto definitions
            // to be able to override external definitions
            if (restOfName != null) {
                if (indexes != null && !dereferencedIndexes) {
                    def = context.dereference(def, prefixArgs, prefixIndexes);
                    indexes = null;
                }
                if (numPushes > 0) {
                    context.pop();
                    numPushes--;
                }
                
                Context resolutionContext = context;
                if (holder != null && holder.resolvedInstance != null && holder.data != null) {
                    resolutionContext = holder.resolvedInstance.getResolutionContext();
                }
                
                Object parentObj = (data == UNDEFINED ? null : data);
//                if (parentObj == null && def instanceof ExternalDefinition) {
//                    def = ((ExternalDefinition) def).getDefForContext(resolutionContext, args);
//                    parentObj = ((ExternalDefinition) def).getObject();
//                }
                if (!generate) {
                    DefinitionInstance defInstance = (DefinitionInstance) resolutionContext.getDescendant(def, prefixArgs, restOfName, false, parentObj);
                    return (defInstance == null ? null : defInstance.def);
                    
                } else {
                    int numResContextPushes = 0;
                    try {
                        //numResContextPushes += resolutionContext.pushParts(name, name.numParts(), getOwner());
                        //resolutionContext.validateSize();
                        return resolutionContext.getDescendant(def, prefixArgs, restOfName, true, parentObj);
                    } finally {
                        while (numResContextPushes-- > 0) {
                            resolutionContext.pop();
                        }
                    }
                }
            }

            
            if (def != null && !dereferencedIndexes) {
                def = dereference(context, def);
                indexes = null;
                dereferencedIndexes = true;
            }
            
            if (!generate) {
                return def;
            } else if (def == null) {
                return UNDEFINED;
            }

            // see if this is a reference to a parameter in this context
            if (def.isFormalParam()) {
                NameNode paramName = def.getNameNode();
                int numEntries = context.size();
                int i = 0;
                try {
                    while (i < numEntries) {
                        data = context.getParameterInstance(paramName, false, false);
                        if (data != null || context.paramIsPresent(paramName) || i == numEntries - 1) {
                            break;
                        }
                        context.unpush();
                        i++;
                    }
                } finally {
                    while (i > 0) {
                        context.repush();
                        i--;
                    }
                }

            } else {
                args = getArguments();
                List<Index> initIndexes = dereferencedIndexes ? null : getIndexes();
                Definition initializedDef = context.initDef(def, args, initIndexes);
                if (initializedDef == null && initIndexes != null) {
                    initializedDef = context.initDef(def, args, null);
                } else if (initializedDef != def) {
                    initIndexes = null;
                }
                if (initializedDef != null) {
                    data = instantiate(context, initializedDef, args, initIndexes);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new Redirection(Redirection.STANDARD_ERROR, "Exception " + (generate ? "instantiating " : "looking up ") + getDefinitionName() + ": " + e.toString());

        } finally {
            while (numPushes-- > 0) {
                context.pop();
            }
        }
        return data;
    }

    private static Definition lookupDef(NameNode name, List<Index> indexes, CantoNode parent, NamedDefinition owner, Definition classDef, Context context, Definition resolver, boolean localScope) throws Redirection {
        Definition def = null;
        ArgumentList args = name.getArguments();
        Definition defclass = context.peek().def;

        // first check for a special name
        def = lookupSpecialName(name, parent, owner, context);

        if (def == null) {
        
            // look for definition in owner subclass or superclass
    
            // this is a hack to take care of subclassed aliased parents
            // (see alias_test in the test suite)
            int limit = context.size() - 1;
            int numUnpushes = 0;
            try {
                while (numUnpushes < limit) {
                    numUnpushes++;
                    context.unpush();
                    Definition nextdef = context.peek().def;
                    if (nextdef.equalsOrExtends(defclass)) {
                        if (nextdef instanceof ComplexDefinition) {
                            defclass = nextdef;
                        }
                    } else {
                        break;
                    }
                }
            } finally {
                while (numUnpushes-- > 0) {
                    context.repush();
                }
            }
            if (owner.equals(defclass) || owner.isSubDefinition(defclass)) {
                if (!name.equals(defclass.getAlias()) && defclass.getChildDefinition(name, context) != null) {
//                    Definition def1 = defclass.getChildDefinition(name, context);
//                    Definition def2 = defclass.getChildDefinition(name, args, indexes, null, context);
//                    if (!def1.equals(def2)) {
//                        def1 = defclass.getChildDefinition(name, context);
//                        def2 = defclass.getChildDefinition(name, args, indexes, null, context);
//                    }
//                    def = def1;
                    def = defclass.getChildDefinition(name, args, indexes, null, context, resolver);
                    if (def != null && indexes != null) {
                        indexes = null;
                    }
                }
            } else {
                Iterator<Context.Entry> it = context.iterator();
                Context.Entry entry = it.next();  // we already checked the top of the stack, so skip it
                Definition lastDef = entry.def;
    
                while (it.hasNext()) {
                    entry = it.next();
                    if (entry.def.equals(lastDef)) {
                        continue;
                    } else {
                        lastDef = entry.def;
                    }
    
                    if (entry.def instanceof ComplexDefinition) {
                        ComplexDefinition cdef = (ComplexDefinition) entry.def;
                        if (owner.equals(cdef) || owner.isSubDefinition(cdef)) {
                            if (!cdef.equals(resolver) && cdef.getChildDefinition(name, context) != null) {
                                def = cdef.getChildDefinition(name, args, indexes, null, context, resolver);
                                if (def != null && indexes != null) {
                                    indexes = null;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        if (def == null && !localScope) {

            // remaining resolution sequence:
            //
            // 5. container local
            // 6. container superclass
            // 7. superclass container
            // 8. joined site
            // 9. explicit
            // 10. external
    
    
            // container local
            //ComplexDefinition container = ComplexDefinition.getComplexOwner(owner.getOwner());
            // don't need to call getOwnerInContext here because in the loop that follows
            // we find the subdefinition on the stack
            NamedDefinition container = (NamedDefinition) owner.getOwner();
    
            // if the container is at the site level, we handle it a little differently
            int numUnpushes = 0;
            int limit = context.size() - 1;
            try {
                while (numUnpushes < limit && container != null  && !(container instanceof Site)) {
                    Iterator<Context.Entry> it = context.iterator();
                    while (it.hasNext()) {
                        Context.Entry entry = (Context.Entry) it.next();
                        if (!(entry.def instanceof NamedDefinition)) {
                            continue;
                        }
                        
                        NamedDefinition ndef = (NamedDefinition) entry.def;
                        
                        if (ndef.equals(owner) || owner.isSubDefinition(ndef)) {
                            continue;    
    
                        } else if (ndef.equals(container) || container.isSubDefinition(ndef)) {
                            if (ndef.isAlias()) {
                                Instantiation aliasInstance = ndef.getAliasInstance();
                                ndef = (NamedDefinition) aliasInstance.getDefinition(context);
                            } else if (ndef.isIdentity()) {
                                Holder holder = entry.getDefHolder(ndef.getName(), ndef.getFullName(), null, false);
                                if (holder != null && holder.def != null) {
                                    ndef = (NamedDefinition) holder.def;
                                }
                            }
                            container = ndef;
                            break;
                        }
                    }
                    
                    def = container.getChildDefinition(name, context);
                    if (def != null) {
                        break;
                    }
                    context.unpush();
                    numUnpushes++;
                    //container = ComplexDefinition.getComplexOwner(container.getOwner());
                    container = (NamedDefinition) container.getOwnerInContext(context);
                }
            } finally {
                while (numUnpushes-- > 0) {
                    context.repush();
                }
            }
            
            if (def == null && container instanceof Site) {
                // try local site first, then container site, then root site, 
                // then adopted sites, then core

                Site site = owner.getSite();
                def = site.getChildDefinition(name, context);
                if (def == null) {
                    Site containerSite = (Site) container;
                    if (!site.equals(containerSite)) {
                        def = containerSite.getChildDefinition(name, context);
                    }
                }
                if (def == null) {
                    Site rootSite = (Site) context.getRootEntry().def;
                    if (!site.equals(rootSite) && !container.equals(rootSite)) {
                        def = rootSite.getChildDefinition(name, context);
                    }
                }

                if (def == null) {
                    def = site.getAdoptedDefinition(null, name);
                    if (def == null) {
                        Core core = site.getCore();
                        def = core.getChildDefinition(name, context);
                    }
                }
            }
    
            // 6. container superclass
            if (def == null) {
                container = ComplexDefinition.getComplexOwner(owner.getOwner());
                while (def == null && container != null && !(container instanceof Site)) {
                    Iterator<Context.Entry> it = context.iterator();
                    while (it.hasNext()) {
                        Context.Entry entry = it.next();
                        if (entry.def instanceof ComplexDefinition) {
                            ComplexDefinition cdef = (ComplexDefinition) entry.def;
                            if (container.equals(cdef) || container.isSubDefinition(cdef)) {
                                container = cdef;
                                break;
                            }
                        }
                    }
    
                    defclass = container.getSuperDefinition(null);  //context);
                    while (defclass != null) {
                        def = defclass.getChildDefinition(name, context);
                        if (def != null) {
                            break;
                        }
                        defclass = defclass.getSuperDefinition(null);  //context);
                    }
                    container = ComplexDefinition.getComplexOwner(container.getOwner());
                }
    
                // 7. superclass container
                if (def == null) {
                    NamedDefinition superdef = (NamedDefinition) context.peek().def;
                    while (def == null) {
                        superdef = superdef.getSuperDefinition(null);  //context);
                        if (superdef == null) {
                            break;
                        }
                        container = ComplexDefinition.getComplexOwner(superdef.getOwner());
                        while (def == null && container != null) {
                            Iterator<Context.Entry> it = context.iterator();
                            while (it.hasNext()) {
                                Context.Entry entry = it.next();
                                if (entry.def instanceof ComplexDefinition) {
                                    ComplexDefinition cdef = (ComplexDefinition) entry.def;
                                    if (container.equals(cdef) || container.isSubDefinition(cdef)) {
                                        container = cdef;
                                        break;
                                    }
                                }
                            }
    
                            def = container.getChildDefinition(name, context);
                            if (def != null) {
                                break;
                            }
                            container = ComplexDefinition.getComplexOwner(container.getOwner());
                        }
                    }
                }
            }
        }

        if (def == null) {

            // classDef is the definition, if any, resolved statically by
            // considering only the type hierarchy of the instance.
            // If classDef is external, let other code handle it,
            // because classDef might be a partial definition
            if (classDef != null && !classDef.isExternal()) {
                // def will have most likely already been resolved
                // to classDef by earlier code.  But it may have been
                // missed. This could happen if the original instance's 
                // owner is a NamedDefinition but not a ComplexDefinition
                def = classDef;

            } else {

                // for the remaining possibilities, restore the name variable
                // (if the name is multipart, name will have been set to just
                // the first part), get the applicable definition table and call
                // it.  The definition table will check all three possibilities
                // (joined site, explicit and external) in the proper order.
                ComplexDefinition complexOwner = ComplexDefinition.getComplexOwner(owner);
                DefinitionTable definitions = complexOwner.getDefinitionTable();
                def = definitions.getDefinition(null, name);
                
                if (def == null) {
                    
                    def = owner.getSite().getAdoptedDefinition(null, name);
                    
                    if (def == null) {

                        Core core = owner.getSite().getCore();
                        if (core != null) {
                            DefinitionTable coreDefinitions = core.getDefinitionTable();
                            def = coreDefinitions.getDefinition(null, name);
                        }
                    }
                }
            }
        }

        if (def != null) {
            if (indexes != null) {
                def = context.dereference(def, args, indexes);

            //} else if (!(def instanceof ElementReference)
            //        && !def.isCollection()
            //        && context.getKeepdDefinition(def, args) == null
            //        && !name.isSpecial()) {
            //
            //    context.putDefinition(def, name.getName(), args, indexes);
            }
        }
        return def;
    }
    
    Definition dereference(Context context, Definition def) throws Redirection {
        return context.dereference(def, getArguments(), getIndexes());
    }


    /** Returns the definition associated with this instance in the given context.  If
     *  <code>checkForParam</code> is true, the context parameters are checked to see if
     *  this is the instantiation of a parameter; if false, the parameters are not
     *  not checked.
     */
    Definition getInstanceDef(Context context, boolean checkForParam, Definition resolver) throws Redirection {

        Definition def = null;
        ArgumentList args = getArguments();
        List<Index> indexes = getIndexes();
        if (reference instanceof Definition) {
            def = (Definition) reference;

            List<ParameterList> paramLists = def.getParamLists();
            int numParamLists = (paramLists == null ? 0 : paramLists.size());
            if (numParamLists > 1) {
                ParameterList params = def.getParamsForArgs(args, context);
                if (params == null) {
                    def = null;
                } else {
                    def = new DefinitionFlavor(def, context, params);
                }

            } else if (numParamLists == 1) {
                int score = ((ParameterList) paramLists.get(0)).getScore(args, context, def);
                if (score >= Definition.NO_MATCH) {
                    def = null;
                }
            }

        //} else if (cachedDef != null && (context == null ? (contextMarker == null) : context.equals(contextMarker))) {
        //    //vlog("using cached definition for " + getDefinitionName());
        //    def = cachedDef;

        } else {

            NameNode name = (NameNode) reference;
            if (name.isComplex()) {
                vlog("!!! getInstanceDef but name is complex!!! (" + name.getName() + ")");
            }

            if (context != null) {

                // first check to see if it's a special name
                def = lookupSpecialName(context);
                if (def != null) {
                    return def;
                }

                // if the check parameter flag is true, check to see if
                // this is a parameter instantiation
                if (checkForParam) {
                    
// this was to try to catch parameters that are nested multiple levels deep in arguments
//                    boolean inContainer = (getKind() == CONTAINER_PARAMETER);
//                    int n = context.size();
//                    int i = 0;
//                    try {
//                        while (i < n) {
//                            def = (Definition) context.getParameter(name, false, inContainer, false);
//                            if (def != null || context.paramIsPresent(name) || i == n - 1) {
//                                break;
//                            }
//                            context.unpush();
//                            i++;
//                        }
//                    } finally {
//                        while (i > 0) {
//                            context.repush();
//                            i--;
//                        }
//                    }
                    
                    def = context.getParameterDefinition(name, isContainerParameter(context));

                }

                if (def == null) {

                    if (classDef != null) {
                        Definition defclass = context.peek().def;
                        if (!defclass.equals(resolver)) {
                            def = defclass.getChildDefinition(name, args, indexes, null, context, resolver);
                        }
                        if (def == null) {
                            def = classDef;
                        } else {
                            indexes = null;
                        }

                    } else {
                        Iterator<Context.Entry> it = context.iterator();
                        Definition lastDef = null;
                        boolean isSpecial = name.isSpecial();
                        
                        while (it.hasNext()) {
                            Context.Entry entry = it.next();
                            Definition defcon = entry.def;
                            if (defcon.equals(lastDef)) {
                                continue;
                            } else {
                                lastDef = defcon;
                            }

                            if (defcon.equals(resolver)) {
                                continue;
                            } else if (defcon.isAlias()) {
                                continue;
                            } else if (!isSpecial && defcon.getChildDefinition(name, context) == null) {
                                //vlog("getInstanceDef: context entry " + defcon.getFullName() + " doesn't have child " + name.getName());
                                continue;
                            }
 
                            def = defcon.getChildDefinition(name, args, indexes, null, context, resolver);
                            if (def != null) {
                                indexes = null;
                                break;
                            }
                        }
                    }
                    // if not a parameter or in the class hierarchy or context
                    // stack, try the container hierarchy
                    if (def == null) {
                        Iterator<Context.Entry> it = context.iterator();
                        Definition lastDef = null;
                        boolean isSpecial = name.getFirstPart().isSpecial();

                        while (it.hasNext()) {
                            Context.Entry entry = it.next();
                            Definition defcon = entry.def;
                            if (defcon.equals(lastDef)) {
                                continue;
                            } else {
                                lastDef = defcon;
                            }

                            for (Definition owner = defcon.getOwner(); owner != null; owner = owner.getOwner()) {
                                owner = context.getSubdefinitionInContext(owner);
                                if (owner.equals(resolver)) {
                                    continue;
                                } else if (!isSpecial && owner.getChildDefinition(name, context) == null) {
                                    //vlog("getInstanceDef: context entry owner " + owner.getFullName() + " doesn't have child " + name.getName());
                                    continue;
                                }
                                def = owner.getChildDefinition(name, args, indexes, null, context, resolver);
                                if (def != null) {
                                    indexes = null;
                                    break;
                                }
                            }
                            if (def != null) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (def != null && indexes != null) {
            def = context.dereference(def, args, indexes);
        }
        return def;
    }

    private Definition lookupSpecialName(Context context) throws Redirection {
        return lookupSpecialName((NameNode) reference, getParent(), getOwner().getSubdefInContext(context), context);
    }
    
    private static Definition lookupSpecialName(NameNode nameNode, CantoNode parent, Definition owner, Context context) throws Redirection {
        int numUnpushes = 0;
        try {
            Definition def = null;
            ComplexName restOfName = null;
            int n = nameNode.numParts();
   
            // if this is a multipart name, only look up the first part, then
            // resolve the rest directly using the found definition.  But since
            // this process may lead to a false negative (if some sequence of
            // parts at the beginning of the name resolves but then peters out),
            // also look up the explicit or external definition, if one exists.
            // This will  be overridden by any definition discovered through the
            // standard process, but will take effect if no such definition exists.
            if (n > 1) {
                restOfName = new ComplexName(nameNode, 1, n);
                nameNode = nameNode.getFirstPart();
            }
           
            String name = nameNode.getName();
   
            if (name == Name.SUB) {
                NamedDefinition subdef = (NamedDefinition) context.peek().def;
                NamedDefinition localOwner = (parent instanceof NamedDefinition ? (NamedDefinition) parent : (NamedDefinition) parent.getOwner());
                // if the definition on the context stack is the same as the local
                // owner, then there is no actual sub for this reference to
                // point to.
                if (!subdef.equals(localOwner)) {
                    def = new SubDefinition(subdef, (NamedDefinition) owner);
                } else {
                    def = new EmptyDefinition();
                }
   
            } else if (name == Name.THIS) {
                def = owner;

            } else if (name == Name.DEF) {
                def = owner;
   
            } else if (name == Name.OWNER) {
                def = owner.getOwnerInContext(context);
   
            } else if (name == Name.CONTAINER) {
                if (context.size() > 1) {
                    numUnpushes++;
                    context.unpush();
                    def = context.peek().def;
                }
               
            } else if (name == Name.SUPER) {
                def = owner.getSuperDefinition(context);

            } else if (name == Name.SITE || name == Name.CORE) {
                def = context.getRootEntry().def;
                while (def != null) {
                    if (def instanceof Site && name == Name.SITE) {
                        break;
                    } else if (def instanceof Core && name == Name.CORE) {
                        break;
                    }
                    def = def.getOwner();
                }

            } else if (name == Name.HERE) {
                Definition siteDef = context.getRootEntry().def;
                while (siteDef != null) {
                    if (siteDef instanceof Site) {
                        break;
                    }
                    siteDef = siteDef.getOwner();
                }
                if (siteDef == null) {
                    throw new Redirection(Redirection.STANDARD_ERROR, "unable to find site containing " + owner.getName());
                }
                Site site = (Site) siteDef;
                Definition superdef = site.getDefinition("canto_context");
                Type superType = (superdef != null ? superdef.getType() : null);
                CantoContext cantoContext = new CantoContext(site, context);
                def = new ExternalDefinition(Name.HERE, parent, owner, superType, Definition.LOCAL_ACCESS, Definition.DYNAMIC, cantoContext, null);
             }
            
            if (def != null) {
                // core needs an AliasedDefinition wrapper; otherwise, only if the name is different and the def is not already wrapped
                if (def instanceof NamedDefinition && !(def instanceof AliasedDefinition) /* && !(Name.THIS.equals(name)) */  && ("core".equals(name) || !def.getName().equals(name))) {
                    def = new AliasedDefinition((NamedDefinition) def, nameNode);
                }
                if (nameNode.getIndexes() != null) {
                    def = context.dereference(def, nameNode.getArguments(), nameNode.getIndexes());
                }
                if (restOfName != null) {
                    def = ((DefinitionInstance) context.getDescendant(def, nameNode.getArguments(), restOfName, false, null)).def;
                }
            }
            return def;
         
        } finally {
            while (numUnpushes-- > 0) {
                context.repush();
            }
        }
    }

    /** Returns true if this instantiation is abstract in the specified context. */
    public boolean isAbstract(Context context) {
        log("????????? calling isAbstract on instance: " + toString(""));
        try {
            getData(context);
        } catch (AbstractConstructionException ace) {
            return true;
        } catch (Redirection r) {
            ;
        }
        return false;
    }

    /** Returns true if this instantiation is defined in the specified context. */
    public boolean isDefined(Context context) {
        if (getDefinition(context) != null) {
            return true;
        } else {
            return false;
        }
    }

    public Object getData(Context context, Definition def) throws Redirection {
        Object data = null;
        
        //if (isAnonymous() && reference instanceof Definition) {
        //    data = context.construct((Definition) reference, getArguments());    
        //} else {
            data = super.getData(context, def);
        //}
        return data;
    }
        
    public Object generateData(Context context, Definition definition) throws Redirection {
        
        if (context == null) {
            throw new Redirection(Redirection.STANDARD_ERROR, "Instantiation requires a context; none provided.");
        }

        Object data = null;
        if (reference instanceof ValueGenerator) {
            // value generators are inherently dynamic
            setDynStat(true, false);
            data = ((ValueGenerator) reference).getData(context);
            vlog("  * Instantiating a value using ValueGenerator " + reference.getClass().getName()); // + ", yielding " + (data == null ? "null" : "value " + ((Value) data).getString()));

        } else if (reference instanceof Value) {
            // values are inherently static
            setDynStat(false, true);
            data = ((Value) reference).getValue();
            vlog("  * Instantiating value " + ((Value) reference).getString() + " directly");

        } else if (reference instanceof Definition) {
            data = instantiate(context, (Definition) reference);
        
        } else if (reference instanceof NameNode) {
            NameNode nameNode = getReferenceName();

            if (localDef != null) {
                data = instantiate(context, localDef);

            } else if (isParam) {
                data = context.getParameterInstance(nameNode, false, isContainerParameter(context), getOwner());
                
            } else if (isParamChild) {
                data = context.getParameterInstance(nameNode, true, isContainerParameter(context), getOwner());
 
            // for now we don't handle passed definitions with multipart names.  To do so, we would
            // need to push intermediate definitions on the context stack before instantiating.  So for
            // now we defer to lookup(), which handles all the necessary context adjustments.     
            } else if (definition != null && !nameNode.isComplex()) {
                data = instantiate(context, definition);

            } else {
                data = lookup(nameNode, context, true, null, false);
            }

        } else if (reference instanceof Definition) {
            data = instantiate(context, (Definition) reference);

        } else {
            throw new Redirection(Redirection.STANDARD_ERROR, "Unknown reference class: " + reference.getClass().getName());
        }
        
        if (data == UNDEFINED) {
            String name = getName();
            log("*** " + name + " not defined, cannot instantiate ***");
            if (context.getErrorThreshhold() <= Context.IGNORABLE_ERRORS) {
                throw new Redirection(Redirection.STANDARD_ERROR, name + " not defined; cannot instantiate");
            }
            data = null;
        } else if (data == null) {
            data = NullValue.NULL_VALUE;
        }
        
        return data;
    }



    public List<Construction> generateConstructions(Context context) throws Redirection {
        List<Construction> constructions = null;
        if (reference instanceof NameNode) {

            int numParamsPushed = 0;
            int numPushes = 0;

            try {
                Instantiation instance = this;
                Instantiation lastInstance = null;
                Definition def = null;
                while (instance != lastInstance && def == null) {
                    lastInstance = instance;
                    NameNode nameNode = (NameNode) instance.reference;
                    String name = nameNode.getName();

                    if (instance.localDef != null) {
                        def = localDef;

                    } else if (instance.isParam || instance.isParamChild) {
                        def = context.getParameterDefinition(nameNode, instance.isContainerParameter(context));

                    } else {

                        Object obj = instance.getDefinition(context);  // lookup(context, false);
                        if (obj instanceof Definition) {
 
                            def = (Definition) obj;
                            // see if this is a reference to a parameter in this context
                            if (def.isFormalParam()) {
                                NameNode paramName = def.getNameNode();
                                Definition paramDef = null;
                                int n = context.size();
                                int i = 0;
                                try {
                                    while (i < n) {
                                        paramDef = context.getParameterDefinition(paramName, false);
                                        if (paramDef != null) break;
                                        context.unpush();
                                        i++;
                                    }
                                } finally {
                                    while (i > 0) {
                                        context.repush();
                                        i--;
                                    }
                                }
                                if (paramDef != null) {
                                    def = paramDef;
                                }

                           } else if (def.isAliasInContext(context)) {
                               ArgumentList args = instance.getArguments();
                               ParameterList params = def.getParamsForArgs(args, context);
                               if (params == null && args != null) {
                                   vlog("arguments in " + name + " do not match parameters in " + def.getFullName());
                               }
                               context.push(def, params, args, true);
                               numPushes++;
                               instance = def.getAliasInstanceInContext(context);
                               def = null;
                           }
                       }
                   }
               }

               Instantiation newInstance = new ResolvedInstance(instance, context, false);
               constructions = new SingleItemList<Construction>(newInstance);

           } finally {
                while (numPushes > 0) {
                    context.pop();
                    numPushes--;
                }
                while (numParamsPushed > 0) {
                    context.popParam();
                    numParamsPushed--;
                }
            }

        } else if (reference instanceof Definition) {
            constructions = ((Definition) reference).getConstructions(context);

        } else {
            constructions = new SingleItemList<Construction>(this);
        }
        return constructions;
    }

    public Object instantiate(Context context, Definition def) throws Redirection {
        return instantiate(context, def, getArguments(), getIndexes());
    }

    public Object instantiate(Context context, Definition def, ArgumentList args, List<Index> indexes) throws Redirection {
        Object obj = null;
        if (isConcurrent()) {
            ConcurrentInstantiator ci = new ConcurrentInstantiator(context, def, args, indexes);
            synchronized (ci.startLock) {
                ci.start();
                try {
                    ci.startLock.wait();
                } catch (InterruptedException ie) {
                    ;
                }
            }
            obj = ci;
            
        } else {
            obj = def.instantiate(args, indexes, context);
        }
        return obj;
    }
    
    static Redirection NOT_STARTED_YET = new Redirection(Redirection.STANDARD_ERROR, "Not started yet.");

    class ConcurrentInstantiator extends Thread {
        
        Context concurrentContext;
        Object startLock = new Object();
        Definition def;
        ArgumentList args;
        List<Index> indexes;
        Object status;

        ConcurrentInstantiator(Context context, Definition def, ArgumentList args, List<Index> indexes) {
            concurrentContext = (Context) context.clone();
            this.def = def;
            this.args = args;
            this.indexes = indexes;
            status = NOT_STARTED_YET;
        }
        
        public void run() {
            log("Launching concurrent instantiation of " + def.getName());

            status = null;
            synchronized (startLock) {
                startLock.notify();
            }
            
            try {
                status = def.instantiate(args, indexes, concurrentContext);
                
            } catch (Throwable t) {
                log("    ...concurrent instantiation failed to complete: " + t);
                status = t;
            }
        }
        
        public Object getStatus() {
            return status;
        }
        
        public String toString() {
            if (status instanceof Throwable) {
                return status.toString();
            } else {
                return "";
            }
        }
    }
    
    
    private ComplexDefinition getComplexOwner() {
        return ComplexDefinition.getComplexOwner(getOwner());
    }
    
    public String getName() {
        if (reference != null) {
            return reference.getName();
        } else {
            return "";
        }
    }

    public String toString(String prefix) {
        return toString(prefix, reference);
    }
   
//       StringBuffer sb = new StringBuffer(prefix);
//       if (reference instanceof Definition) {
//           Definition definition = (Definition) reference;
//           String name = definition.getName();
//           if (name == null || name.equals(Name.ANONYMOUS)) {
//               String defstr = ((AbstractNode) definition).toString(prefix);
//               if (defstr.endsWith("\n")) {
//                   defstr = defstr.substring(0, defstr.length() - 1);
//               }
//               sb.append(defstr);
//           } else {
//               sb.append(name);
//           }
//           String modifier = getNameModifier();
//           if (modifier != null) {
//            sb.append(modifier);
//           }
//       } else {
//           sb.append(reference.toString());
//       }
//       if (sb.length() > 0 && trailingDelimiter && !(getParent() instanceof ArgumentList) && !(getParent() instanceof Index) && !(getParent() instanceof Expression)) {
//           char endchar = sb.charAt(sb.length() - 1);
//           if (endchar != '\n' && endchar != '\r') {
//               sb.append(";\n");
//           }
//       }
//       return sb.toString();
//   }

}

class SubDefinition extends AliasedDefinition {
    static final NameNode SUBALIAS = new NameNode(Name.SUB);

    NamedDefinition superDef;

    public SubDefinition(NamedDefinition def, NamedDefinition superDef) {
        super(def, SUBALIAS);
        this.superDef = superDef;
    }

    /** Returns the superdefinition, or null if unspecified. */
    public NamedDefinition getSuperDefinition(Context context) {
        return superDef;
    }

    /** Returns the superdefinition, or null if unspecified. */
    public NamedDefinition getSuperDefinition() {
        return superDef;
    }

    /** Construct this definition with the specified arguments in the specified context. */
    public Object instantiate(ArgumentList args, List<Index> indexes, Context context) throws Redirection {
        return context.constructSub(def, def);
    }
}

class EmptyDefinition extends AnonymousDefinition {
    public EmptyDefinition() {
        super();
    }
    public boolean isSuperType(String name) {
        return (name.equals("definition"));
    }
}




