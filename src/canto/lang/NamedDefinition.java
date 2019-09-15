/* Canto Compiler and Runtime Engine
 * 
 * NamedDefinition.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;
import canto.runtime.CantoObjectWrapper;
import canto.runtime.Holder;

/**
* NamedDefinition is a definition which can be looked up by name, allowing it to be
* instantiated, and also making it elegible for subclassing.
*
* @author Michael St. Hippolyte
* @version $Revision: 1.150 $
*/

public class NamedDefinition extends AnonymousDefinition {


    public static NameNode createNameNode(String name) {
        if (name == null || name.length() == 0) {
            return new NameNode(Name.ANONYMOUS);
 
        // if the name is not multipart, does not have parameters, and is not
        // an array or table element, all we need is a plain vanilla NameNode
        } else if (name.indexOf('.') < 0 && name.indexOf('(') < 0
                    && name.indexOf('[') < 0 && name.indexOf('{') < 0) {

            return new NameNode(name);

        // otherwise, create a ComplexName.  ComplexName will create an instance
        // of CantoParser to properly parse the string and create whatever nodes
        // are necessary to represent it.
        } else {
            return new ComplexName(name);
        }
    }

    private NameNode name;
    private boolean array;
    private Type type;
    private Type supertype;
    private List<Construction> resolvedConstructions = null;
    private boolean identity = false;
    private NameNode alias = null;
    private NameNode paramAlias = null;
    private NameNode reference = null;
    private boolean ownerOfNext = false;
    private boolean ownerOfSub = false;
    private boolean ownerOfDefs = false;
    private List<KeepStatement> keeps = null;
    transient private List<KeepStatement> keepsAndSuperKeeps = null;

    public NamedDefinition() {
        super();
    }

    public NamedDefinition(Definition def, Context context) {
        super(def, context);
        if (def instanceof NamedDefinition) {
            NamedDefinition ndef = (NamedDefinition) def;
            supertype = ndef.supertype;
            name = ndef.name;
            array = ndef.array;
            type = ndef.type;
            resolvedConstructions = ndef.resolvedConstructions;
            identity = ndef.identity;
            alias = ndef.alias;
            paramAlias = ndef.paramAlias;
            ownerOfNext = ndef.ownerOfNext;
            ownerOfSub = ndef.ownerOfSub;
            ownerOfDefs = ndef.ownerOfDefs;
            keeps = ndef.keeps;
        }
    }

    public NamedDefinition(String name, CantoNode parent, Definition owner, Type superType, int access, int dur, Object value) {
        super();
        
        setAccess(access);
        setDurability(dur);
        setOwner(owner);
        ComplexDefinition complexOwner = ComplexDefinition.getComplexOwner(owner);
        if (complexOwner != null) {
            setDefinitionTable(complexOwner.getDefinitionTable());
        }
        jjtSetParent((AbstractNode) parent);
        AbstractNode valueNode = (value instanceof AbstractNode ? (AbstractNode) value : new PrimitiveValue(value));
        init(superType, new NameNode(name), valueNode);
    }
    

    public String getName() {
        return (name == null ? null : name.getName());
    }

    public boolean isAnonymous() {
        return (name == null || name.getName().equals(ANONYMOUS));
    }

    public NameNode getNameNode() {
        return name;
    } 

    public void init(Type supertype, NameNode name, AbstractNode contents) {
        setSuper(supertype);
        setName(name);
        setType(createType());
        
        // external definitions have complex names; the params are on the last part
        if (name instanceof ComplexName) {
            name = ((ComplexName) name).getLastPart();
        }
        if (name instanceof NameWithParams && ((NameWithParams) name).getNumParamLists() > 0) {
            setParamLists(((NameWithParams) name).getParamLists());
        } else {
            setParamLists(null);
        }

        // if the contents are a single named instantiation, make this an alias
        Instantiation instance = null;
        if (contents instanceof Instantiation) {
            instance = (Instantiation) contents;

        } else if (contents instanceof Block) {
            CantoNode node = contents;
            while (node instanceof Block && node.getNumChildren() == 1) {
                node = node.getChild(0);
            }
            if (node instanceof Instantiation) {
                instance = (Instantiation) node; 
            }
        }
        if (instance != null) {
            CantoNode reference = instance.getReference();
            if (reference instanceof NameNode) {
                setAlias((NameNode) reference);
            }
        }
        setContents(contents);
    }


    /** Called in the validation pass to check aliases and remove any that point to
     *  parameters, since the logic for aliases fails with parameters.  This can't be
     *  done during initialization because the instances haven't been resolved yet.
     *
     *  Also remove the alias if this definition is an owner of definitions, or if the
     *  alias is a child of one of this definition's ancestors, because such an alias
     *  can lead to circular definitions.
     */
    public void checkAlias() {
        if (alias != null) {
            boolean notAlias = false;
            String aliasName = alias.getName();
            
            if (isOwner()) {
                notAlias = true;
                
            } else if (checkIdentity(aliasName)) {
                notAlias = true;
                
            } else if (hasChildDefinition(aliasName)) {
                notAlias = true;
            }

            if (notAlias) {
                NameNode ref = alias;
                setAlias(null);
                setReference(ref);

            } else {
                Instantiation instance = getAliasInstance();
                if (instance.isParameterKind()) {
                    paramAlias = alias;
                    alias = null;
                }
            }
        
        }
    }

    /** Determines if this is an identity definition (e.g. foo(x) = x)
     *  and sets the identity flag accordingly. 
     * @param name
     * @return identity flag
     */
    private boolean checkIdentity(String name) {
        // very cheesy way of finding identity
        DefinitionTable defTable = getDefinitionTable();
        Definition def = defTable.getInternalDefinition(getFullName(), name);
        if (def != null && def.isFormalParam() && def.getName().equals(name)) {
            identity = true;
        } else {
            identity = false;
        }
        return identity;
    }
    
    public boolean isOwner() {
        return ownerOfDefs;
    }

    protected void setIsOwner(boolean flag) {
        ownerOfDefs = flag;
    }
   
    /** Returns true if this definition represents a collection. */
    public boolean isCollection() {
        return getType().isCollection();
    }

    /** Returns true if this definition represents an array. */
    public boolean isArray() {
        return getType().isArray();
    }

    /** Returns true if this definition represents a table. */
    public boolean isTable() {
        return getType().isTable();
    }

    /** Returns true if the passed definition either equals this definition or is included
     *  in this definition (for subclasses of NamedDefinition which are composites).  The
     *  base class simply checks for equals.
     */
    public boolean includes(Definition def) {
        return equals(def);
    }

    protected Type createType() {
        NameNode nameNode = getNameNode();
        ComplexType type = new ComplexType(this, nameNode.getName(), nameNode.getDims(), nameNode.getArguments());
        type.setOwner(getOwner());
        return type;
    }

    protected void setName(NameNode name) {
        this.name = name;
    }

    protected void setType(Type type) {
        this.type = type;
    }

    protected void setAlias(NameNode alias) {
        this.alias = alias;
        this.reference = alias;
    }

    /** Returns true if this definition is an alias. */
    public boolean isAlias() {
        return (alias != null || paramAlias != null);
    }

    /** Returns the aliased name, if this definition is an alias to a definition
     *  or a parameter, else null. */
    public NameNode getAlias() {
        return (alias != null ? alias : paramAlias);
    }

    protected void setParamAlias(NameNode alias) {
        this.paramAlias = alias;
        this.reference = alias;
    }

    /** Returns true if this definition is an alias to a parameter. */
    public boolean isParamAlias() {
        return (paramAlias != null);
    }

    /** Returns the aliased name, if this definition is an alias to a parameter, else null. */
    public NameNode getParamAlias() {
        return paramAlias;
    }

    /** Returns the aliased instantiation, if this definition is an alias, else null. */
    public Instantiation getAliasInstance() {
        Instantiation aliasInstance = null;
        if (alias != null || paramAlias != null) {
            CantoNode node = getContents();
            while (node instanceof Block && node.getNumChildren() == 1) {
                node = node.getChild(0);
            }
            if (node instanceof Instantiation) {
                aliasInstance = (Instantiation) node;
            }
        }
        return aliasInstance;
    }

    /** Returns true if this definition is an alias or cached identity (which is effectively
     *  an alias in the context where it's cached).
     **/
    public boolean isAliasInContext(Context context) {
        if (identity) {
            Holder holder = null;
            try {
                holder = context.getKeepdHolderForDef(this, null, null);
            } catch (Redirection r) {
                ;
            }
            if (holder != null && holder.resolvedInstance != null && !this.equals(holder.resolvedInstance.getDefinition())) {
                return true;
            } else {
                return false;
            }
        }
        return isAlias();
    }

    /** Returns the cached instance name, if this definition is a cached identity, or
     *  the aliased name, if this definition is an alias, else null.
     **/
    public NameNode getAliasInContext(Context context) {
        
        if (identity) {
            Holder holder = null;
            try {
                holder = context.getKeepdHolderForDef(this, null, null);
            } catch (Redirection r) {
                ;
            }
            if (holder != null && holder.resolvedInstance != null && !this.equals(holder.resolvedInstance.getDefinition())) {
                return holder.resolvedInstance.getReferenceName();
            }
        }
        return getAlias();
    }

    /** Returns the cached instance, if this definition is a cached identity, or the aliased 
     *  instantiation, if this definition is an alias, else null.
     **/
    public Instantiation getAliasInstanceInContext(Context context) {
        if (identity) {
            Holder holder = null;
            try {
                holder = context.getKeepdHolderForDef(this, null, null);
            } catch (Redirection r) {
                ;
            }
            if (holder != null) {
                ResolvedInstance ri = holder.resolvedInstance;
                if (ri != null && !this.equals(ri.getDefinition())) {
                    return ri;
                }
            }
        }
        return getAliasInstance();
    }
    
    
    protected void setReference(NameNode reference) {
        this.reference = reference;
    }

    /** Returns true if this definition is a reference.  A reference is weaker than an alias; in
     *  particular, a reference only points to a definition, not its children. */
    public boolean isReference() {
        return (reference != null);
    }

    /** Returns the referenced name, if this definition is a reference, else null. */
    public NameNode getReference() {
        return reference;
    }

    /** Returns the associated type object. */
    public Type getType() {
        return type;
    }

    /** Returns true if <code>def</code> equals or is a superdefinition of 
     *  this definition.
     */
    public boolean equalsOrExtends(Definition def) {
        return (equals(def) || (def != null && def instanceof NamedDefinition && ((NamedDefinition) def).isSubDefinition(this)));
    }

    /** Returns the supertype, or null if unspecified. */
    public Type getSuper() {
        return supertype;
    }

    public Iterator<Type> getCompatibleSupers(Context context) {
        return new SingleItemIterator<Type>(getSuper(context));
    }
   
    /** Returns the supertype for the given context. */
    public Type getSuper(Context context) {
        return getSuper(context, context.getParameters());
    }
   
    /** Returns the supertype for the given context.  If the supertype is a list of types,
     *  returns the type in the list that best matches the parameters in the context.
     */
    public Type getSuper(Context context, ParameterList params) {
        Type st = supertype;
        if (st instanceof TypeList) {
            st = ((TypeList) st).getTypeForParams(params, context);
        }
        return st;
    }

    /** Returns the supertype of this definition which corresponds to the owner of the
     *  childDef, or is a subtype of the owner of childDef.
     * 
     *  This method enables the identification of the specific chain of supertypes within a 
     *  multiple inheritance tree of supertypes which leads to the parent  
     */
    public Type getSuperForChild(Context context, Definition childDef) throws Redirection {
        NameNode name = childDef.getNameNode();
        Type st = getSuper(context);
        if (st == null) {
            return null;

        } else if (st instanceof TypeList) {
               Iterator<Type> it = ((TypeList) st).iterator();
               // first check if one of these types is the one
            while (it.hasNext()) {
                   Type nextType = it.next();
                NamedDefinition sd = (NamedDefinition) nextType.getDefinition();
                if (sd == null) {
                    if (context.getErrorThreshhold() <= Context.IGNORABLE_ERRORS) {
                        throw new Redirection(Redirection.STANDARD_ERROR, "Definition not found for " + nextType.getName());
                    } else {
                        continue;
                    }
                }
                try {
                    Definition def = ((DefinitionInstance) sd.getChild(name, name.getArguments(), name.getIndexes(), null, context, false, false, null, null)).def;
                    if (def != null && def.equals(childDef)) {
                        return nextType;
                    }
                } catch (Exception e) {
                    vlog("Exception getting super for child: " + e);
                } catch (Redirection r) {
                    ;
                }
            }

            // none of these supertypes corresponds to the parent definition, so
            // check recursively if any of the supertypes has an ancestor which
            // is the parent.
            it = ((TypeList) st).iterator();
            while (it.hasNext()) {
                Type nextType = (Type) it.next();
                NamedDefinition sd = (NamedDefinition) nextType.getDefinition();
                Type t = sd.getSuperForChild(context, childDef);
                if (t != null) {
                    return nextType;
                }
               }
               
            // not found
            return null;

        } else {
            // only one superdefinition
            NamedDefinition sd = (NamedDefinition) st.getDefinition();
            if (sd == null) {
                return null;
            }
            try {
                Definition def = ((DefinitionInstance) sd.getChild(name, name.getArguments(), name.getIndexes(), null, context, false, false, null, null)).def;
                // if the child is external, the def doesn't have to be equal; it could be an
                // interface definition
                if (def != null && (def.equals(childDef) || childDef.isExternal())) {
                    return st;
                }
            } catch (Redirection r) {
                ;
            }
            if (sd.getSuperForChild(context, childDef) != null) {
                return st;
            } else {
                return null;
            }
        }
    }

    /** Sets the supertype. */
    protected void setSuper(Type supertype) {
        this.supertype = supertype;
    }

    /** Returns true if this definition contains a <code>next</code> statement.
     */
    public boolean hasNext(Context context) {
        return ownerOfNext;
    }

    void setHasNext(boolean hasNext) {
        ownerOfNext = hasNext;
    }

    /** Returns a linked list of definitions with <code>next</code> statements.  If this
     *  definition is a single definition and has a next statement, the linked list will
     *  have a single element (this definition).
     */
    public LinkedList<Definition> getNextList(Context context) {
        if (hasNext(context)) {
            LinkedList<Definition> nextList = new LinkedList<Definition>();
            nextList.add(this);
            return nextList;
        } else {
            return null;
        }
    }

    /** Returns true if this definition contains a <code>sub</code> statement,
     *  or is empty and <code>hasSub</code> called on its superdefinition (if any)
     *  returns true.
     */
    public boolean hasSub(Context context) {
        if (getConstructions(context).size() > 0) {
            return ownerOfSub;
        } else {
            NamedDefinition superDef = getSuperDefinition(context);
            if (superDef != null) {
                return superDef.hasSub(context);
            } else {
                return false;
            }
        }
    }

    void setHasSub(boolean hasSub) {
        ownerOfSub = hasSub;
    }

    public boolean isSubDefinition(Definition def) {
        NamedDefinition subDef = null;
        try {
            subDef = (NamedDefinition) def;
        } catch (ClassCastException cce) {
            return false;
        }

        for (NamedDefinition superDef = subDef.getSuperDefinition(); superDef != null; superDef = superDef.getSuperDefinition()) {
            if (superDef.includes(this)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSuper(Type type) {
        for (Type st = getSuper(); st != null; st = st.getDefinition().getSuper()) {
            if (st.includes(type)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSubType(Type type) {
        for (Type st = type.getDefinition().getSuper(); st != null; st = st.getDefinition().getSuper()) {
            if (st.includes(type)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSuperType(String name) {
        Type st = getSuper();
        while (st != null) {
            if (st.includes(name)) {
                return true;
            }
            Definition def = st.getDefinition();
            st = (def != null ? def.getSuper() : null);
        }
        return false;
    }

    public NamedDefinition getSuperDefinition() {
        return getSuperDefinition(null);
    }

    public int getDimSize() {
        if (name instanceof NameWithDims) {
            List<Dim> dims = ((NameWithDims) name).getDims();
            if (dims != null && dims.size() > 0) {
                Dim dim = (Dim) dims.get(0);
                if (dim.getType() == Dim.TYPE.DEFINITE) {
                    return dim.getSize();
                }
            }
        }
        return -1;
    }

    public Definition getChildDefinition(NameNode node, Context context) {
        String name = node.getName();
        Definition specialDef = null;
        if (name == Name.OWNER) {
            specialDef = (NamedDefinition) getOwnerInContext(context);

        } else if (node.getName() == Name.SITE) {
            specialDef = getSite();

        // strip off the owner prefix, if present
        } else if (node.isComplex() && ((Name) node.getChild(0)).getName() == Name.OWNER) {
            int n = node.getNumChildren();
            node = new ComplexName(node, 1, n);
            specialDef = getOwnerInContext(context).getChildDefinition(node, context);
        }
        if (specialDef != null && specialDef instanceof NamedDefinition) {
            return new AliasedDefinition((NamedDefinition) specialDef, node);
        }

        // Check to see if this is a built-in field such as <code>type</code>
        // or <code>count</code>
        int n = node.getNumChildren();

        // dereference a complex name with a lone child
        while (n == 1) {
            CantoNode child = node.getChild(0);
            if (!(child instanceof NameNode)) {
                break;
            }
            node = (NameNode) child;
            n = node.getNumChildren();
        }

        // two possibilities: immediate child definition (single part) or
        // deeper descendant (multiple parts)
        if (n > 1 && node.isComplex()) { // deeper descendant
 
            NameNode prefix = (NameNode) node.getChild(0);
            NameNode suffix;
            if (n == 2) {
                suffix = (NameNode) node.getChild(1);
            } else {
                suffix = new ComplexName(node, 1, n);
            }
            Definition prefixDef = getChildDefinition(prefix, context);
            if (prefixDef == null) {
                return null;
            } else {
                return ((NamedDefinition) prefixDef).getChildDefinition(suffix, context);
            }

        // handle ".type"
        } else if (name == Name.TYPE) {
            return new TypeDefinition(this);
 
        // handle ".count"
        } else if (name == Name.COUNT) {
            try {
                return new CountDefinition(this, context, null, null);
            } catch (Redirection r) {
                return new CountDefinition(this);
            }
            
        // handle ".keys"
        } else if (name == Name.KEYS) {
            return new KeysDefinition(this, context);
        }
 
        Definition def = getExplicitChildDefinition(node);

        // if not found, try supertypes and supertype aliases
        if (def == null) {

//            AbstractNode contents = getContents();
//            if (context != null && !isAlias() && !isIdentity() && contents instanceof Construction) {
//                Construction construction = ((Construction) contents).getUltimateConstruction(context);
//            
//                if (construction instanceof Instantiation && !((Instantiation) construction).isParameterKind()) {
//                    Instantiation instance = (Instantiation) construction;
//                    NameNode contentsName = instance.getReferenceName();
//                    if (!node.equals(contentsName)) {
//                        Definition contentDef = instance.getDefinition(context, this);
//                        ArgumentList contentArgs = null;
//                        ParameterList contentParams = null;
//        
//                        if (contentDef == null || contentDef == this) {
//                            Type contentType = ((Instantiation) construction).getType(context, this);
//                            if (contentType != null) {
//                                contentDef = contentType.getDefinition();
//                                if (contentDef != null) {
//                                    contentArgs = ((Instantiation) construction).getArguments(); // contentType.getArguments(context);
//                                    contentParams = contentDef.getParamsForArgs(contentArgs, context, false);
//                                }
//                            }
//                        }
//        
//                        if (contentDef != null) {
//                            try {
//                                context.push(contentDef, contentParams, contentArgs, false);
//                                DefinitionInstance defInstance = (DefinitionInstance) context.getDescendant(contentDef, contentArgs, node, false, null);
//                                if (defInstance != null) {
//                                    def = defInstance.def;
//                                }
//                            } catch (Redirection r) {
//                                ;
//                            } finally {
//                                context.pop();
//                            }
//                        }
//                    }
//                } else {
//                    Type type = construction.getType(context, this);
//                    if (type != null && type != DefaultType.TYPE && !type.isPrimitive() && !type.equals(getType())) {
//                        Definition runtimeDef = type.getDefinition();
//                        if (runtimeDef != null && !runtimeDef.equals(this) && runtimeDef.getName() != Name.THIS && runtimeDef.canHaveChildDefinitions()) {
//                            ArgumentList typeArgs = type.getArguments(context);
//                            ParameterList typeParams = runtimeDef.getParamsForArgs(typeArgs, context, false);
//                            try {
//                                context.push(runtimeDef, typeParams, typeArgs, false);
//                                def = runtimeDef.getChildDefinition(node, context);
//                            } catch (Redirection r) {
//                                ;
//                            } finally {
//                                context.pop();
//                            }
//                        }
//                    }
//                }
//            }
            
            if (def == null) {
                for (NamedDefinition nd = getSuperDefinition(); nd != null && !nd.isPrimitive(); nd = nd.getSuperDefinition()) {
                    def = nd.getExplicitChildDefinition(node);
                    if (def != null) {
                       break;
                    }
                   
                    if (nd.isAlias()) {
                        NamedDefinition aliasDef = nd;
                        int numAliasPushes = 0;
                        try {
                            do {
                                Instantiation aliasInstance = aliasDef.getAliasInstance();
                                NameNode aliasName = aliasInstance.getReferenceName();
                                
                                // avoid infinite recursion
                                if (node.equals(aliasName)) {
                                    break;
                                }
                                aliasDef = (NamedDefinition) aliasInstance.getDefinition(context);  //, aliasDef);
                                if (aliasDef == null) {
                                    break;
                                }
                                if (aliasDef instanceof ExternalDefinition) {
                                    // should we be retrieving the external object from the cache here?  We're just
                                    // getting a definition, so maybe it doesn't matter.
                                    Object obj = aliasDef.getChild(node, node.getArguments(), node.getIndexes(), null, context, false, true, null, null);
                                    if (obj != UNDEFINED && obj != null) {
                                        def = ((DefinitionInstance) obj).def;
                                    }
                                
                                } else {
                                    def = aliasDef.getExplicitChildDefinition(node);
                                }
                                if (def != null) {
                                    break;
                                }
                                ArgumentList aliasArgs = aliasInstance.getArguments();
                                ParameterList aliasParams = aliasDef.getParamsForArgs(aliasArgs, context);
                                context.push(nd, aliasParams, aliasArgs, false);
                                numAliasPushes++;
       
                            } while (aliasDef.isAlias());
                        } catch (Redirection r) {
                            ;
                        } finally {
                            while (numAliasPushes-- > 0) {
                                context.pop();
                            }
                        }
                        if (def != null) {
                            break;
                        }
                    }
                }
            }
            if (def == null) {
                return null;
            }
        }

        return def;
    }

    public Definition getExplicitChildDefinition(NameNode node) {
        return getDefinitionTable().getDefinition(this, node);
    }

    /** Returns true if this is an identity definition.
     */
    public boolean isIdentity() {
        return identity;
    }

    public boolean hasChildDefinition(String name) {
        if (name == OWNER || name == TYPE || name == COUNT || name == SUB || name == SUPER || name == NEXT || name == NEXT) {
            return true;
        }

        DefinitionTable defTable = getDefinitionTable();
        Definition def = defTable.getInternalDefinition(getFullName(), name);

        // if not found, try supertypes
        if (def == null) {
            for (NamedDefinition nd = getSuperDefinition(); nd != null && !nd.isPrimitive(); nd = nd.getSuperDefinition()) {
                def = nd.getDefinitionTable().getInternalDefinition(nd.getFullName(), name);
                if (def != null) {
                   return true;
                }
            }
            return false;

        } else {
            return true;
        }
    }

    /** Get a child of this definition as a definition. This only works for named definitions. */
    public Definition getDefinitionChild(NameNode childName, Context context, ArgumentList args) throws Redirection {
        AliasedDefinition adef = new AliasedDefinition(this, getNameNode());
        Definition def = adef.getDefForContext(context, args);
        return def.getChildDefinition(childName, context);
    }
    

    public NamedDefinition getSuperDefinition(Context context) {
        NamedDefinition superdef = null;
        Type st = (context == null ? getSuper() : getSuper(context));
        if (st != null) {
            superdef = (NamedDefinition) st.getDefinition();
            if (superdef == null && st instanceof NameNode && st.getName().length() > 0) {
                NameNode node = (NameNode) st;
                Definition owner = getOwner();
                while (owner != null) {
                    superdef = (NamedDefinition) owner.getChildDefinition(node, context);
                    if (superdef != null) {
                        break;
                    }
                    owner = owner.getOwner();
                }
            }
            if (superdef != null && context != null) {
                try {
                    superdef = (NamedDefinition) superdef.getDefinitionForArgs(st.getArguments(context), context);
                } catch (Throwable t) {
                    ;
                }
            }
        }
        return superdef;
    }

    /** Returns the underdefinition, which is the definition, if any, that this definition
     *  overrides, i.e., the child of this definition's owner's superclass that has the same
     *  name as this definition.
     **/
    public NamedDefinition getUnderDefinition(Context context) {
        NamedDefinition underDef = null;
        NameNode name = getNameNode();
        try {
            Definition ownerDef = getOwner();
            if (ownerDef != null) {
                for (NamedDefinition superDef = ownerDef.getSuperDefinition(); superDef != null; superDef = superDef.getSuperDefinition()) {
                    Definition childDef = superDef.getChildDefinition(name, context);
                    if (childDef != null) {
                        underDef = (NamedDefinition) childDef;
                        break;
                    }
                }
            }
        } finally {
            ;
        }
        return underDef;
    }
    
   
    /** Returns the immediate subdefinition of this definition in the current context,
     *  or null if not found.  This method assumes that this definition or a subdefinition
     *  is currently being constructed, so the top of the context stack will contain this
     *  definition or a subdefintion.
     */
    public Definition getImmediateSubdefinition(Context context) {
        NamedDefinition subDef = (NamedDefinition) context.peek().def;
        for (NamedDefinition superDef = subDef.getSuperDefinition(); superDef != null; superDef = superDef.getSuperDefinition()) {
            if (superDef.includes(this)) {
                return subDef;
            }
        }
        return null;
    }

    public CollectionDefinition getCollectionDefinition(Context context, ArgumentList args) throws Redirection {
        if (isAlias()) {
            Instantiation aliasInstance = getAliasInstance();
            Definition aliasDef = aliasInstance.getDefinition(context, this, false);
            if (aliasDef != null && aliasDef.isCollection()) {
                return aliasDef.getCollectionDefinition(context, aliasInstance.getArguments());
            }            
        }
        
        Definition superDef = getSuperDefinition(context);
        if (superDef == null) {
            return null;
        } else {
            return superDef.getCollectionDefinition(context, args);
        }
    }


    /** Calls the generic version of createCollectionInstance. */
    public CollectionInstance createCollectionInstance(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        return CollectionBuilder.createCollectionInstanceForDef(this, context, args, indexes, null);
    }

    /** Calls createCollectionInstance for the passed object. */
    public CollectionInstance createCollectionInstance(Context context, ArgumentList args, List<Index> indexes, Object collectionData) throws Redirection {
        return CollectionBuilder.createCollectionInstanceForDef(this, context, args, indexes, collectionData);
    }

    /** Returns an instance of this collection in the specified context with the specified
     *  arguments.
     */
    public CollectionInstance getCollectionInstance(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        CollectionInstance collection = null;
        String name = getName();
        String fullName = getFullNameInContext(context);
        Definition defInKeep = null;
        Definition nominalDefInKeep = null;

        if (getDurability() != Definition.DYNAMIC && (args == null || !args.isDynamic())) {
            //cachevlog("  = = =]  collection: retrieving " + name + " from cache [= = = ");

            Object collectionObject = null;
            Holder holder = context.getDefHolder(name, fullName, args, indexes, false);
            if (holder != null && holder.resolvedInstance != null && holder.resolvedInstance instanceof CollectionInstance) {
                collectionObject = holder.resolvedInstance;
            } else {
                collectionObject = context.getData(this, name, args, null);
            }
            if (collectionObject instanceof CollectionInstance) {
                collection = (CollectionInstance) collectionObject;

            // externally-created collections might not be wrapped in a CollectionInstance yet
            } else if (collectionObject != null) {
                collection = createCollectionInstance(context, args, indexes, collectionObject);
                //context.putData(this, args, null, name, modifier, collection);
            }

            holder = context.getKeepdHolderForDef(this, args, indexes);
            if (holder != null) {
                defInKeep = holder.def;
                nominalDefInKeep = holder.nominalDef;
            }
            
            //cachevlog("  = = =]  " + name + " collection data: " + (collection == null ? "null" : collection.toString()));
        }

        if (collection == null || !(equals(defInKeep) || equals(nominalDefInKeep))) {
            collection = createCollectionInstance(context, args, indexes);
            //cachevlog("  = = =]  collection: storing data for " + name + " in cache [= = = ");
            ResolvedInstance ri = null;
            if (collection instanceof ResolvedInstance) {
                ri = (ResolvedInstance) collection;
            }
            
            context.putData(this, args, this, args, null, name, collection, ri);
            //cachevlog("  = = =]  " + name + " collection data: " + (collection == null ? "null" : collection.toString()));
            
        }
        return collection;
        
    }
    
    protected Definition getExplicitDefinition(NameNode node, ArgumentList args, Context context) {
        Definition def = getExplicitChildDefinition(node);
        if (def != null) {
            if (def.isFormalParam()) {

                // we should allow DefParameters if this is being called in scope,
                // but I haven't figured the best way to do that
                def = null;
               
            } else if (def instanceof NamedDefinition && args != null) {
                NamedDefinition ndef = (NamedDefinition) def;
                int numUnpushes = 0;
                try {
                    NamedDefinition argsOwner = (NamedDefinition) args.getOwner();
                    if (argsOwner != null) {
                        int limit = context.size() - 1;
                        while (numUnpushes < limit) {
                            NamedDefinition nextdef = (NamedDefinition) context.peek().def;
                            if (argsOwner.equals(nextdef) || argsOwner.isSubDefinition(nextdef)) {
                                break;
                            }
                            numUnpushes++;
                            context.unpush();
                        }
                        // if we didn't find the owner in the stack, put it back the way it came
                        if (numUnpushes == limit) {
                            while (numUnpushes-- > 0) {
                                context.repush();
                            }
                        }
                    }
                    def = ndef.getDefinitionForArgs(args, context);

                } finally {
                    while (numUnpushes-- > 0) {
                        context.repush();
                    }
                }
            }
        }
        return def;
    }

    protected Definition getExternalDefinition(NameNode node, Context context) {
        return getSite().getExternalDefinition(this, node, DefaultType.TYPE, context);
    }

    public Object getChild(NameNode node, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context context, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection {
        if (context == null) {
            throw new Redirection(Redirection.STANDARD_ERROR, "getChild requires a context; none provided.");
        } else if (context.peek() == null) {
            throw new Redirection(Redirection.STANDARD_ERROR, "getChild requires a non-empty context; passed context is empty.");
        }
        Definition instantiatedDef = context.peek().def;

        if (node.getName() == Name.OWNER) {
            Definition owner = getOwnerInContext(context);
            if (owner == null) {
                return null;
            }
            if (generate) {
                return owner.instantiate(args, indexes, context);
            } else {
                Definition aliasedDef = new AliasedDefinition((NamedDefinition) owner, node);
                return aliasedDef.getDefInstance(args, indexes);
            }

        } else if (node.getName().equals(Name.DEF)) {
            if (generate) {
                return instantiate(args, indexes, context);
            } else {
                Definition aliasedDef = new AliasedDefinition(this, node);
                return aliasedDef.getDefInstance(args, indexes);
            }

        } else if (node.getName().equals(Name.THIS)) {
            if (generate) {
                // this does not deal with arguments or indexes
                return new CantoObjectWrapper(this, args, indexes, context);
            } else {
                Definition aliasedDef = new AliasedDefinition(this, node);
                return aliasedDef.getDefInstance(args, indexes);
            }

        } else if (node.getName() == Name.SITE) {
            if (generate) {
                return getSite().instantiate(args, indexes, context);
            } else {
                Definition aliasedDef = new AliasedDefinition(getSite(), node);
                return aliasedDef.getDefInstance(args, indexes);
            }
            
        } else if (node.getName() == Name.TYPE) {
            Definition ultimateDef = getUltimateDefinition(context);
            
            Definition superDef = getSuperDefinition(context);
            while (ultimateDef.isAliasInContext(context)) {
                Instantiation aliasInstance = ultimateDef.getAliasInstanceInContext(context);
                if (ultimateDef.isParamAlias() && aliasInstance != null) {
                    aliasInstance = aliasInstance.getUltimateInstance(context);
                }
                if (aliasInstance == null) {
                    break;
                }
                Definition aliasDef = aliasInstance.getDefinition(context, this, false);  // def or nextDef?
                if (aliasDef == null) {
                    break;
                }
        
                // we are only interested in aliases in the same hierarchy
                if (superDef != null && !aliasDef.equalsOrExtends(superDef)) {
                    break;
                }
                superDef = aliasDef;  // ratchet towards subclasses
                ultimateDef = aliasDef;
            }
            // make sure we don't end up with the superclass
            if (equalsOrExtends(ultimateDef)) {
                ultimateDef = this;
            }
            if (generate) {
                return new PrimitiveValue(ultimateDef.getType());
            } else {
                Definition typeDef = new TypeDefinition(ultimateDef);
                return typeDef.getDefInstance(null, null);
            }

        // strip off the owner prefix, if present
        } else if (node.isComplex() && ((Name) node.getChild(0)).getName() == Name.OWNER) {
            int n = node.getNumChildren();
            node = new ComplexName(node, 1, n);
            // if there's another owner prefix, forward to the owner
            //if (node.getName() == Name.OWNER || (node.isComplex() && ((Name) node.getChild(0)).getName() == Name.OWNER)) {
            try {
                context.unpush();
                return getOwnerInContext(context).getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
            } finally {
                context.repush();
            }
            //}
        }

        // Check for complex names includinga built-in field such as <code>count</code>
        NameNode lastNode = node;
        int n = (node.isComplex() ? node.getNumChildren() : 0);
        if (n > 0) {
            lastNode = (NameNode) node.getChild(n - 1);
        }

        if (lastNode.getName().equals(Name.KEYS)) {
            if (n <= 1) {
                Definition keysDef = new KeysDefinition(this, context);
                if (generate) {
                    return keysDef.instantiate(args, indexes, context);
                } else {
                    return keysDef.getDefInstance(null, indexes);
                }
            } else {
                NameNode mapNode = new ComplexName(node, 0, n - 1);
                DefinitionInstance defInstance = (DefinitionInstance) getChild(mapNode, args, indexes, parentArgs, context, false, trySuper, parentObj, resolver);
                Definition mapDef = defInstance.def;
                if (mapDef == null) {
                    return (generate ? UNDEFINED : null);
                } else if (mapDef instanceof DynamicObject) {
                    mapDef = (Definition) ((DynamicObject) mapDef).initForContext(context, args, indexes);
                }

                CollectionDefinition collectionDef = mapDef.getCollectionDefinition(context, args);
                if (collectionDef != null) {
                    return collectionDef.getChild(lastNode, args, indexes, parentArgs, context, generate, trySuper, parentObj, null);
                }

                Definition keysDef = new KeysDefinition(mapDef, context);
                if (generate) {
                    return keysDef.instantiate(args, indexes, context);
                } else {
                    return keysDef.getDefInstance(null, indexes);
                }
            }
            
        } else if (lastNode.getName() == Name.COUNT) {
            if (n <= 1) {
                CollectionDefinition collectionDef = getCollectionDefinition(context, args);
                if (collectionDef != null) {
                    return collectionDef.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, null);
                }
                if (generate) {
                    return new PrimitiveValue(1);
                } else {
                    Definition countDef = new CountDefinition(this, context, args, indexes);
                    return countDef.getDefInstance(null, null);
                }
            } else {
                NameNode countNode = new ComplexName(node, 0, n - 1);
                DefinitionInstance defInstance = (DefinitionInstance) getChild(countNode, args, indexes, parentArgs, context, false, trySuper, parentObj, resolver);
                Definition countDef = defInstance.def;
                if (countDef == null) {
                    return (generate ? UNDEFINED : null);
                } else if (countDef instanceof DynamicObject) {
                    countDef = (Definition) ((DynamicObject) countDef).initForContext(context, args, indexes);
                }

                CollectionDefinition collectionDef = countDef.getCollectionDefinition(context, args);
                if (collectionDef != null) {
                    return collectionDef.getChild(lastNode, args, indexes, parentArgs, context, generate, trySuper, parentObj, null);
                }
                
                if (generate) {
                    if (countDef instanceof CollectionDefinition) {
                        return new PrimitiveValue(((CollectionDefinition) countDef).getSize(context, args, indexes));
                    } else {
                        return new PrimitiveValue(1);
                    } 
                } else {
                    countDef = new CountDefinition(countDef, context, args, indexes);
                    return countDef.getDefInstance(null, null);
                }
            }
        }

        // look to see if this is the child of an instantiated object
        if (parentObj != null && parentObj instanceof CantoObjectWrapper) {
            CantoObjectWrapper obj = (CantoObjectWrapper) parentObj;
            Context resolutionContext = obj.getResolutionContext();
            Definition parentDef = obj.getDefinition();
            if (resolutionContext != null && parentDef != null) {
                return resolutionContext.getDescendant(parentDef, parentArgs, node, generate, null);
            }
        }
        
        // next check to see if this is an external definition
        Definition def = getExternalDefinition(node, context);
        if (def != null && !generate) {
            return def.getDefInstance(args, indexes);
        }

        // if this is an alias, look up the definition in the aliased definition
        if (isAlias()) {
            String name = node.getName();
            String aliasName = alias != null ? alias.getName() : paramAlias.getName();
            //avoid recursion
            if (!name.equals(aliasName)) {
                Instantiation nearAliasInstance = getAliasInstance(); 
                Instantiation aliasInstance = nearAliasInstance.getUltimateInstance(context);
                ArgumentList aliasArgs = aliasInstance.getArguments();     // aliasInstance.getUltimateInstance(context).getArguments();
                // avoid recursion
                boolean nameEqualsArg = false;
                if (aliasArgs != null && aliasArgs.size() > 0) {
                    Iterator<Construction> it = aliasArgs.iterator();
                     while (it.hasNext()) {
                         Construction aliasArg = it.next();
                         if (aliasArg instanceof Instantiation) {
                             if (name.equals(((Instantiation) aliasArg).getDefinitionName())) {
                                 nameEqualsArg = true;
                                 vlog(name + " is also an alias arg; skipping lookup");
                                 break;
                            }
                        }
                    }
                }
                if (!nameEqualsArg) {
                    Definition aliasDef = aliasInstance.getDefinition(context);
                    if (aliasDef != null) {
                        int numPushes = 0;
                        
                        try {
                            NameNode nameNode = aliasInstance.getReferenceName();
                            for (int i = 0; i < nameNode.numParts() - 1; i++) {
                                NameNode partName = (NameNode) nameNode.getChild(i);
                                Instantiation partInstance = new Instantiation(partName, aliasInstance.getOwner());
                                partInstance.setKind(context.getParameterKind(partName.getName()));
                                Definition partDef = partInstance.getDefinition(context);
                                if (partDef == null) {
                                    break;
                                }
                                ArgumentList partArgs = partInstance.getArguments();
                                List<Index> partIndexes = partInstance.getIndexes();
                                if (partIndexes == null || partIndexes.size() == 0) {
                                    String nm = partName.getName();
                                    String fullNm = partDef.getFullNameInContext(context);
                                    Holder holder = context.getDefHolder(nm, fullNm, partArgs, partIndexes, false);
                                    if (holder != null) {
                                        Definition nominalDef = holder.nominalDef;
                                        if (nominalDef != null && !nominalDef.isCollection() && nominalDef.getDurability() != Definition.DYNAMIC) { 
                                            if (nominalDef.isIdentity()) {
                                                partDef = holder.def;
                                                if (partArgs == null) {
                                                    partArgs = holder.args;
                                                }
                                            } else {
                                                partDef = nominalDef;
                                                if (partArgs == null) {
                                                    partArgs = holder.nominalArgs;
                                                }
                                            }
                                        }
                                    }
                                }
                                ParameterList partParams = partDef.getParamsForArgs(partArgs, context, false);
                                context.push(partDef, partParams, partArgs, false);
                                numPushes++;
                            }
                            ParameterList aliasParams = aliasDef.getParamsForArgs(aliasArgs, context, false);
                            context.push(aliasDef, aliasParams, aliasArgs, generate);
                            numPushes++;
                            Object child = aliasDef.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
                            return child;

                        } finally {
                            while (numPushes-- > 0) {
                                context.pop();
                            }
                        }
                    }
                }
            }
        }
        
        // now see if it's a complex name
        if (n > 1 && node.isComplex()) {
            NameNode prefix = (NameNode) node.getChild(0);
            NameNode suffix;
            if (n == 2) {
                suffix = (NameNode) node.getChild(1);
            } else {
                suffix = new ComplexName(node, 1, n);
            }
            ArgumentList prefixArgs = prefix.getArguments();
            ParameterList prefixParams = null;
            List<Index> prefixIndexes = prefix.getIndexes();
            Definition prefixDef = null;
            if (prefixArgs == null) {
                String nm = prefix.getName();
                String fullNm = getFullNameInContext(context) + "." + nm;
                Holder holder = context.getDefHolder(nm, fullNm, null, prefixIndexes, false);
                if (holder != null && holder.def != null && holder.def.getDurability() != DYNAMIC && isDynamic() && !((CantoNode) holder.def).isDynamic() && !holder.def.equals(context.getDefiningDef())) {
                    prefixDef = holder.def;
                    prefixArgs = holder.args;
                }
            }
            if (prefixDef == null) {
                DefinitionInstance defInstance = (DefinitionInstance) getChild(prefix, prefixArgs, prefix.getIndexes(), parentArgs, context, false, trySuper, parentObj, resolver);
                if (defInstance != null) {
                    prefixDef = defInstance.def;
                }
            }
            if (prefixDef != null) {
                if (prefixDef instanceof ExternalDefinition) {
                    ExternalDefinition externalDef = (ExternalDefinition) prefixDef;
                    prefixDef = externalDef.getDefForContext(context, args);
                }
                
                prefixParams = prefixDef.getParamsForArgs(prefixArgs, context);
                
                // if the prefix definition is an alias, look up the aliased
                // definition and use that instead.
                if (prefixDef.isAliasInContext(context)) {
                    NameNode alias = prefixDef.isParamAlias() ? prefixDef.getParamAlias() : prefixDef.getAliasInContext(context);
                    ArgumentList aliasArgs = alias.getArguments();
                    List<Index> aliasIndexes = alias.getIndexes();
                    Definition aliasDef = null;
                    Context.Entry aliasEntry = context.getParameterEntry(alias, false);
                    if (aliasEntry == null) {
                        for (Definition owner = prefixDef.getOwner(); owner != null; owner = owner.getOwner()) {
                            Definition ownerInContext = owner.getSubdefInContext(context);
                            if (ownerInContext != null && !ownerInContext.equalsOrExtends(this)) {
                                DefinitionInstance defInstance = (DefinitionInstance) ownerInContext.getChild(alias, aliasArgs, aliasIndexes, parentArgs, context, false, trySuper, parentObj, resolver);
                                if (defInstance != null && defInstance.def != null) {
                                    aliasDef = defInstance.def;
                                    break;
                                }
                            }
                        }
                    }

                    if (aliasEntry != null) {
                        prefixDef = aliasEntry.def;
                        prefixParams = aliasEntry.params;
                        prefixArgs = aliasEntry.args;
                        prefixIndexes = aliasIndexes;

                    } else if (aliasDef != null) {
                        prefixDef = aliasDef;
                        prefixArgs = aliasArgs;
                        prefixParams = prefixDef.getParamsForArgs(prefixArgs, context);
                        prefixIndexes = null;
                    }
                } 
                if (prefixDef != null) {
                    
                    if (prefixDef.isIdentity()) {
                        Holder holder = context.peek().getDefHolder(prefixDef.getName(), prefixDef.getFullName(), null, false);
                        if (holder != null && holder.def != null) {
                            prefixDef = (NamedDefinition) holder.def;
                            prefixArgs = holder.args;
                        }
                    }                    
                    
                    try {
                        context.push(prefixDef, prefixParams, prefixArgs);
                        
                        Object child = prefixDef.getChild(suffix, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
    
                        if (!generate && child == null && prefixDef.isAliasInContext(context)) {
                            ComplexName childName = new ComplexName(prefixDef.getAliasInContext(context), suffix);
                            Definition externalDef = getExternalDefinition(childName, context);
                            if (externalDef != null) {
                                child = externalDef.getDefInstance(childName.getArguments(), childName.getIndexes());
                            }
                        }
                        if (child == null && isAlias()) {
                            return super.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
                        } else if (child instanceof Definition) {
                            return ((Definition) child).getDefInstance(args, indexes);
                        } else {
                            return child;
                        }
                    } finally {
                        context.pop();
                    }
                }
            }
        }

        // is this a parameter?
        ParameterList params = getParamsForArgs(parentArgs, context);
        if (params != null && params.size() > 0) {
            Iterator<DefParameter> it = params.iterator();
            while (it.hasNext()) {
                DefParameter param = it.next();
                if (param.getName().equals(node.getName())) {
                    def = param;
                    break;
                }
            }
        }

        // next see if this is a fully named definition or an immediate child
        if (def == null) {
            def = getExplicitDefinition(node, args, context);
        }
        
        // if not, then try supertypes or alias.
        if (def == null) {
            // try alias
// Not needed -- we check alias above            
//            if (isAlias()) {
//                // look up the definition in the aliased definition
//
//                //avoid recursion
//                String aliasName = getAlias().getFirstPart().getName();
//                if (!aliasName.equals(node.getName()) && !aliasName.equals(Name.THIS)  && !aliasName.equals(Name.OWNER)) {
//                    
//                    Instantiation aliasInstance = getAliasInstance();
//                    Definition aliasDef = aliasInstance.getDefinition(context, this);
//
//                    // if it's null, it's a problem, but not our problem.  We won't abort the
//                    // current operation just because we found an undefined construction.  That
//                    // will happen when/if the program tries to instantiate it.
//                    
//                    if (aliasDef != null) {
//                        ArgumentList aliasArgs = aliasInstance.getArguments();
//                        ParameterList aliasParams = aliasDef.getParamsForArgs(aliasArgs, context, false);
//                        context.push(instantiatedDef, aliasParams, aliasArgs, false);
//                        Object child = aliasDef.getChild(node, args, null, parentArgs, context, generate, trySuper, parentObj, resolver);
//                        context.pop();
//                        if ((generate && child != UNDEFINED) || (!generate && child != null)) {
//                            return child;
//                        }
//                    }
//                }
//            }

            // not an alias; see if it is a construction that defines the child
            AbstractNode contents = getContents();
            if (!isAlias() && !isIdentity() && !(contents instanceof ConstructionGenerator) && contents instanceof Construction && !((Construction) contents).isPrimitive()) {
                Construction construction = ((Construction) contents).getUltimateConstruction(context);
                if (construction instanceof Instantiation && !((Instantiation) construction).isParameterKind()) {
                    Instantiation instance = (Instantiation) construction;
                    NameNode name = instance.getReferenceName();
                    // avoid regression
                    if (name != null && !node.equals(name) && !hasChildDefinition(name.getName()) && (resolver == null || !resolver.getNameNode().equals(name))) {
                        Definition contentDef = instance.getDefinition(context, this, false);
                        if (contentDef == null || contentDef == this) {
                            Type contentType = instance.getType(context, generate);
                            if (contentType != null) {
                                contentDef = contentType.getDefinition();
                            }
                        }
                        if (contentDef != null && contentDef.getNameNode() != null
                                && !contentDef.isIdentity()
                                //&& !contentDef.getNameNode().equals(name)
                                && !contentDef.getNameNode().isSpecial()
                                && !contents.equals(construction)
                                && !context.contains(contentDef)
                                && !contentDef.getOwner().equalsOrExtends(this)) { // && !contentDef.equals(resolver)) {

                            ArgumentList contentArgs = instance.getArguments(); // contentType.getArguments(context);

                            // make sure the value we are resolving is not one of the arguments
                            boolean nameEqualsArg = false;
                            if (contentArgs != null && contentArgs.size() > 0) {
                                Iterator<Construction> it = contentArgs.iterator();
                                 while (it.hasNext()) {
                                     Construction contentArg = it.next();
                                     if (contentArg instanceof Instantiation) {
                                         if (name.getName().equals(((Instantiation) contentArg).getDefinitionName())) {
                                             nameEqualsArg = true;
                                             vlog(name + " is also a content arg; skipping lookup");
                                             break;
                                        }
                                    }
                                }
                            }
                            if (!nameEqualsArg) {
                                ParameterList contentParams = contentDef.getParamsForArgs(contentArgs, context, false);
                                context.push(contentDef, contentParams, contentArgs, false);
                                try {
                                    Object child = context.getDescendant(contentDef, contentArgs, new ComplexName(node), generate, parentObj);
                                    if ((generate && child != UNDEFINED) || (!generate && child != null)) {
                                        return child;
                                    }
                                } finally {
                                    context.pop();
                                }
                            }
                        }
                    }
//                } else if (!this.equals(resolver)) {
//                    Type type = construction.getType(context, generate);
//                    if (type != null && type != DefaultType.TYPE && !type.isPrimitive() && !type.equals(getType())) {
//                        Definition runtimeDef = type.getDefinition();
//                        if (runtimeDef != null && runtimeDef.getName() != Name.THIS && runtimeDef.canHaveChildDefinitions()) {
//                            ArgumentList typeArgs = type.getArguments(context);
//                            ParameterList typeParams = runtimeDef.getParamsForArgs(typeArgs, context, false);
//                            try {
//                                context.push(runtimeDef, typeParams, typeArgs, false);
//                                Object child = runtimeDef.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
//                                if ((generate && child != UNDEFINED) || (!generate && child != null)) {
//                                    return child;
//                                }
//                            } finally {
//                                context.pop();
//                            }
//                        }
//                    }
                }
            }
            
            // no luck yet; try supertypes if trySuper is true
            //
            // There's a problem with this code: it ignores the args when getting the superdefinition,
            // which means that if there are multiple super definitions it may not pick the right one.
            if (trySuper) {
                NamedDefinition nd = getSuperDefinition();
                if (nd != null && (isCollection() == nd.isCollection())) {
                    Type st = getSuper(context);
                    ArgumentList superArgs = st.getArguments(context);
                    // if one of the super arguments is the name we're looking for, avoid
                    // calling getParamsForArgs, which could lead to infinite recursion
                    List<ParameterList> paramLists = nd.getParamLists();
                    boolean foundSame = false;
                    if (paramLists != null && paramLists.size() > 1) {
                        int numArgs = superArgs.size();
                        for (int i = 0; i < numArgs; i++) {
          
                            Construction superArg;
                            try {
                                superArg = superArgs.get(i);
                            } catch (Exception e) {
                                System.err.println("Exception in ComplexDefinition.getChild looking for supArg");
                                continue;
                            }
                            if (superArg instanceof Instantiation) {
                                CantoNode ref = ((Instantiation) superArg).getReference();
                                if (node.equals(ref)) {
                                    vlog("...can't look up " + node.getName() + " in supertype " + st.getName());
                                    foundSame = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!foundSame) {
                        boolean unpushedSuper = false;
                        boolean pushedSuper = false;
                        if (!nd.equals(context.peek().superdef)) {
                            //Context.Entry entry = context.doublePeek();
                            //if (entry != null && nd.equals(entry.superdef)) {
                            //    context.unpush();
                            //    unpushedSuper = true;
                            //} else {
                                ParameterList superParams = nd.getParamsForArgs(superArgs, context, false);
                                context.push(instantiatedDef, nd, superParams, superArgs);
                                pushedSuper = true;                            
                            //}
                        }
                        try {
                            Object child = nd.getChild(node, args, indexes, superArgs, context, generate, trySuper, parentObj, resolver);
                            if ((!generate && child != null) || (generate && child != UNDEFINED)) {
                                return child;
                            }
                        } finally {
                            if (unpushedSuper) {
                                context.repush();
                            } else if (pushedSuper) {
                                context.pop();
                            }  
                        }
                    }
                }

                // finally look to see if this is a de-aliased definition
                if (context.size() > 1) {
                    try {
                        context.unpush();
                        Definition precedingDef = context.peek().def;
                        if (precedingDef.isAlias() && getName().equals(precedingDef.getAlias().getName())) {
                            nd = precedingDef.getSuperDefinition();
                            if (!nd.equals(this) && nd.hasChildDefinition(node.getName())) {
                                Type superSt = precedingDef.getSuper();
                                ArgumentList superSuperArgs = superSt.getArguments(context);
                                ParameterList superSuperParams = nd.getParamsForArgs(superSuperArgs, context);
                                context.unpop(nd, superSuperParams, superSuperArgs);
                                try {
                                    return nd.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
                                } finally {
                                    context.repop();
                                }
                            }
                        }
                    } finally {
                        context.repush();
                    }
                }
                
                // add code to see what the parent type would be if instantiated, and if
                // the result is an external type, instantiate the parent then try the child.
            }
            return (generate ? UNDEFINED : null);
        }

        // if the reference has one or more indexes, and the definition is an
        // array definition, get the appropriate element in the array.
        if (indexes != null && indexes.size() > 0) {
            int numPushes = 0;
            try {
                Definition elementRef = null;
                
                // first look to see if the collection already exists in the cache
                Holder holder = context.peek().getDefHolder(def.getName(), def.getFullNameInContext(context), null, false);
                if (holder != null && holder.def != null && holder.def instanceof NamedDefinition) {
                    NamedDefinition hdef = (NamedDefinition) holder.def;
                    if (CollectionDefinition.isCollectionObject(holder.data) && hdef != null && hdef.isCollection()) {
                        CollectionInstance collection = hdef.createCollectionInstance(context, parentArgs, indexes, holder.data);
                        if (collection != null) {
                            elementRef = new ElementReference(collection, indexes);
                        } else {
                            def = hdef;
                        }
                    }
                }
                
                if (elementRef == null) {
                    while (def.isReference() && !def.isCollection()) {
                        params = def.getParamsForArgs(args, context);
                        context.push(def, params, args, false);
                        numPushes++;
                        Instantiation refInstance = (Instantiation) def.getContents();
                        Definition childDef = null;
                        
                        // this is to get indexed aliases to work right.  A bit hacky.
                        if (def instanceof ElementReference) {
                            Definition elementDef = ((ElementReference) def).getElementDefinition(context);
                            childDef = (elementDef != null ? elementDef : (Definition) refInstance.getDefinition(context));
                        } else if (refInstance != null) {
                            childDef = (Definition) refInstance.getDefinition(context);
                        }
                        if (childDef != null) {
                            // if the ref is complex, track back through prefix supers and aliases
                            CantoNode ref = refInstance.getReference();
                            if (ref instanceof ComplexName) {
                                NameNode refName = (NameNode) ref;
                                ArgumentList refArgs = args;
                                n = ref.getNumChildren();
                                while (n > 1) {
                                    NameNode prefix = (NameNode) refName.getChild(0);
                                    refName = new ComplexName(refName, 1, n);
                                    n--;
                                    
                                    ArgumentList prefixArgs = prefix.getArguments();
                                    Instantiation prefixInstance = new Instantiation(prefix, def);
                                    Definition prefixDef = (Definition) prefixInstance.getDefinition(context);  // lookup(context, false);
                                    numPushes += context.pushSupersAndAliases(def, refArgs, prefixDef);
                                    def = prefixDef;
                                    refArgs = prefixArgs;
                                }
                                numPushes += context.pushSupersAndAliases(def, refArgs, childDef);
                                
                            }
                            def = childDef;
                        } else {
                            def = null;
                            break;
                        }
                        args = refInstance.getArguments();
                    }
                
                    CollectionDefinition collectionDef = null;
                    if (def != null) {
                        collectionDef = def.getCollectionDefinition(context, parentArgs);
        
                    } else {
                        while (numPushes-- > 0) {
                            context.pop();
                        }
        
                        // find array definition
                        for (NamedDefinition nd = getSuperDefinition(); nd != null; nd = nd.getSuperDefinition()) {
                            if (nd instanceof ComplexDefinition) {
                                def = ((ComplexDefinition) nd).getExplicitDefinition(node, args, context);
                                if (def != null && def instanceof CollectionDefinition) {
                                    collectionDef = (CollectionDefinition) def;
                                    break;
                                }
                            }
                        }
                        if (collectionDef == null) {
                            Definition owner = getOwner();
                            Iterator<Context.Entry> it = context.iterator();
                            while (it.hasNext()) {
                                Context.Entry entry = (Context.Entry) it.next();
                                Definition defcon = entry.def;
                                // avoid infinite recursion
                                if (!defcon.equals(this)) {
                                    def = defcon.getChildDefinition(node, args, null, parentArgs, context, this);
                                    if (def != null && def instanceof CollectionDefinition) {
                                        collectionDef = (CollectionDefinition) def;
                                        break;
                                    }
                                }
                            }
                            if (collectionDef == null) {   // not in the class hierarchy, try the container hierarchy
                                it = context.iterator();
                                while (it.hasNext()) {
                                    Context.Entry entry = (Context.Entry) it.next();
                                    Definition defcon = entry.def;
                                    // avoid infinite recursion
                                    if (!defcon.equals(this)) {
                                        for (owner = defcon.getOwner(); owner != null; owner = owner.getOwner()) {
                                            def = owner.getChildDefinition(node, args, null, parentArgs, context, this);
                                            if (def != null && def instanceof CollectionDefinition) {
                                                collectionDef = (CollectionDefinition) def;
                                                break;
                                            }
                                        }
                                        if (collectionDef != null) {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        if (collectionDef == null) {
                            log(node.getName() + " not found.");
                            return null;
                        }
                    }
         
                    while (numPushes-- > 0) {
                        context.pop();
                    }
                    elementRef = collectionDef.getElementReference(context, args, indexes);
                }
                
                if (elementRef != null) {
                    def = elementRef;
                    indexes = null;
                }
                
            } finally {
                while (numPushes-- > 0) {
                    context.pop();
                }
            }
        }
        if (def == null && isAlias()) {
            return super.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
 
        } else if (generate) {
            if (def == null) {
                return UNDEFINED;
            } else {
                return def.instantiate(args, indexes, context);
            }

        } else {
            return ((Definition) def).getDefInstance(args, indexes);
        }
    }

    public Object dummy_getChild(NameNode node, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context context, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection {

        if (this instanceof Definition) {
            return getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
        }

        // forward to special definition if appropriate
        NamedDefinition specialDef = null;
        boolean isContainer = false;
        if (node.numParts() == 1) {
            if (node.getName().equals(Name.OWNER)) {
                specialDef = (NamedDefinition) getOwnerInContext(context);
                
            } else if (node.getName().equals(Name.DEF)) {
                specialDef = this;
    
            } else if (node.getName().equals(Name.SITE)) {
                specialDef = getSite();
                
            } else if (node.getName().equals(Name.KEYS)) {
                Definition keysDef = new KeysDefinition(this, context);
                if (generate) {
                    return keysDef.instantiate(args, indexes, context);
                } else {
                    return keysDef.getDefInstance(null, indexes);
                }
                
            } else if (node.getName().equals(Name.COUNT)) {
                CollectionDefinition collectionDef = getCollectionDefinition(context, args);
                if (collectionDef != null) {
                    return collectionDef.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
                }
                if (generate) {
                    return new PrimitiveValue(1);
                } else {
                    Definition countDef = new CountDefinition(this, context, args, indexes);
                    return countDef.getDefInstance(null, null);
                }

            } else if (node.getName().equals(Name.CONTAINER)) {
                isContainer = true;
            }
            
        } else {
            String firstName = node.getFirstPart().getName();
            if (firstName.equals(Name.OWNER)) {
                int n = node.getNumChildren();
                node = new ComplexName(node, 1, n);
                DefinitionInstance defInstance = (DefinitionInstance) getOwnerInContext(context).getChild(node, node.getArguments(), node.getIndexes(), parentArgs, context, generate, trySuper, parentObj, resolver);
                specialDef = (NamedDefinition) (defInstance == null ? null : defInstance.def);
 
           } else if (firstName.equals(Name.CONTAINER)) {
               isContainer = true;
           }
        }
        
        if (isContainer) {
            Definition def = null;
            ComplexDefinition container = (ComplexDefinition) getOwner();
            if (container != null) {
                while (def == null) {
                    Iterator<Context.Entry> it = context.iterator();
                    while (it.hasNext()) {
                        Definition cdef = it.next().def;
                        if (cdef instanceof NamedDefinition && (container.equals(cdef) || container.isSubDefinition((NamedDefinition) cdef))) {
                            def = cdef;
                            break;
                        }
                    }
                }
                if (def != null && def instanceof ComplexDefinition) {
                    container = (ComplexDefinition) def;
                }
                if (!node.isComplex()) {
                    specialDef = container;
                } else {
                    int n = node.getNumChildren();
                    node = new ComplexName(node, 1, n);
                    Object obj = container.getChild(node, node.getArguments(), node.getIndexes(), parentArgs, context, generate, trySuper, parentObj, resolver);
                    if (generate) {
                        specialDef = (NamedDefinition) obj;
                    } else {
                        specialDef = (NamedDefinition) ((DefinitionInstance) obj).def;
                    }
                }
            }
        }
        
        if (specialDef != null) {
            // see comment above on why we do this crazy stuff
            Definition aliasedDef = new AliasedDefinition((NamedDefinition) specialDef, node);
            if (generate) {
                //return aliasedDef;
                return specialDef.instantiate(args, indexes, context);
            } else {
                return aliasedDef.getDefInstance(args, indexes);
            }
        }
            
        // if this is an alias, look up the definition in the aliased definition
        if (isAlias()) {
            String name = node.getName();
            String aliasName = alias != null ? alias.getName() : paramAlias.getName();
            //avoid recursion
            if (!name.equals(aliasName)) {
                Instantiation nearAliasInstance = getAliasInstance(); 
                Instantiation aliasInstance = nearAliasInstance.getUltimateInstance(context);
                ArgumentList aliasArgs = aliasInstance.getArguments();     // aliasInstance.getUltimateInstance(context).getArguments();
                // avoid recursion
                boolean nameEqualsArg = false;
                if (aliasArgs != null && aliasArgs.size() > 0) {
                    Iterator<Construction> it = aliasArgs.iterator();
                     while (it.hasNext()) {
                         Construction aliasArg = it.next();
                         if (aliasArg instanceof Instantiation) {
                             if (name.equals(((Instantiation) aliasArg).getDefinitionName())) {
                                 nameEqualsArg = true;
                                 vlog(name + " is also an alias arg; skipping lookup");
                                 break;
                            }
                        }
                    }
                }
                if (!nameEqualsArg) {
                    Definition aliasDef = aliasInstance.getDefinition(context);
                    if (aliasDef != null) {
                        int numPushes = 0;
                        
                        try {
                            NameNode nameNode = aliasInstance.getReferenceName();
                            for (int i = 0; i < nameNode.numParts() - 1; i++) {
                                NameNode partName = (NameNode) nameNode.getChild(i);
                                Instantiation partInstance = new Instantiation(partName, aliasInstance.getOwner());
                                partInstance.setKind(context.getParameterKind(partName.getName()));
                                Definition partDef = partInstance.getDefinition(context);
                                if (partDef == null) {
                                    break;
                                }
                                ArgumentList partArgs = partInstance.getArguments();
                                List<Index> partIndexes = partInstance.getIndexes();
                                if (partIndexes == null || partIndexes.size() == 0) {
                                    String nm = partName.getName();
                                    String fullNm = partDef.getFullNameInContext(context);
                                    Holder holder = context.getDefHolder(nm, fullNm, partArgs, partIndexes, false);
                                    if (holder != null) {
                                        Definition nominalDef = holder.nominalDef;
                                        if (nominalDef != null && !nominalDef.isCollection() && nominalDef.getDurability() != Definition.DYNAMIC) { 
                                            if (nominalDef.isIdentity()) {
                                                partDef = holder.def;
                                                if (partArgs == null) {
                                                    partArgs = holder.args;
                                                }
                                            } else {
                                                partDef = nominalDef;
                                                if (partArgs == null) {
                                                    partArgs = holder.nominalArgs;
                                                }
                                            }
                                        }
                                    }
                                }
                                ParameterList partParams = partDef.getParamsForArgs(partArgs, context, false);
                                context.push(partDef, partParams, partArgs, false);
                                numPushes++;
                            }
                            ParameterList aliasParams = aliasDef.getParamsForArgs(aliasArgs, context, false);
                            context.push(aliasDef, aliasParams, aliasArgs, generate);
                            numPushes++;
                            Object child = aliasDef.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
                            return child;

                        } finally {
                            while (numPushes-- > 0) {
                                context.pop();
                            }
                        }
                    }
                }
            }

        } else if (isIdentity()) {
            Holder holder = context.peek().getDefHolder(getName(), getFullNameInContext(context), null, false);
            if (holder != null && holder.def != null && holder.def != this) {
                Definition def = holder.def;
                Context resolutionContext = (holder.resolvedInstance != null ? holder.resolvedInstance.getResolutionContext() : context);
                ParameterList params = def.getParamsForArgs(holder.args, resolutionContext, false);
                resolutionContext.push(def, params, holder.args, false);
                try {
                    return def.getChild(node, args, indexes, parentArgs, resolutionContext, generate, trySuper, parentObj, resolver);
                } finally {
                    resolutionContext.pop();
                }
            }
            // otherwise fall through to try super    
                
        // if the content is a construction, see if it defines a child by this name,
        // either via its type or by a cached definition if it is an identity
        } else {
            AbstractNode contents = getContents();
            if (contents instanceof Construction) {
                Construction construction = ((Construction) contents).getUltimateConstruction(context);
            
                if (construction instanceof Instantiation) {
                    Definition contentDef = ((Instantiation) construction).getDefinition(context, this, false);
                    ArgumentList contentArgs = null;
                    ParameterList contentParams = null;
    
                    if (contentDef == null || contentDef == this) {
                        Type contentType = ((Instantiation) construction).getType(context, generate);
                        if (contentType != null) {
                            contentDef = contentType.getDefinition();
                            if (contentDef != null) {
                                contentArgs = ((Instantiation) construction).getArguments(); // contentType.getArguments(context);
                                contentParams = contentDef.getParamsForArgs(contentArgs, context, false);
                            }
                        }
                    }
    
                    if (contentDef != null) {
                        context.push(contentDef, contentParams, contentArgs, false);
                        try {
                            Object child = context.getDescendant(contentDef, contentArgs, new ComplexName(node), generate, parentObj);
                            
                          //  Object child = contentDef.getChild(node, null, context, generate, trySuper);
                            if ((generate && child != UNDEFINED) || (!generate && child != null)) {
                                return child;
                            }
                        } finally {
                            context.pop();
                        }
                    }
                } else  {
                    Type type = construction.getType(context, generate);
                    if (type != null) {
                        Definition runtimeDef = type.getDefinition();
                        if (runtimeDef != null && runtimeDef.canHaveChildDefinitions()) {
                            Object child = runtimeDef.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
                            if ((generate && child != UNDEFINED) || (!generate && child != null)) {
                                return child;
                            }
                        }
                    }
                }
            }
        }
        
        // not found, so try supertypes if the trySuper flag is true 
        if (trySuper) {
            NamedDefinition nd = getSuperDefinition();
            if (nd != null) {
                Type st = getSuper();
                ArgumentList superArgs = st.getArguments(context);
                ParameterList superParams = nd.getParamsForArgs(superArgs, context);
                NamedDefinition instantiatedDef = (NamedDefinition) context.peek().def;
                if (!instantiatedDef.equals(this) && !isSubDefinition(instantiatedDef)) {
                    instantiatedDef = this;
                }
                context.unpop(instantiatedDef, superParams, superArgs);
                Object child = null;
                try {
                    child = nd.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
                    if ((!generate && child != null) || (generate && child != UNDEFINED)) {
                        return child;
                    }
                } finally {
                    context.repop();
                }
            }

            // finally look to see if this is a de-aliased definition
            if (context.size() > 1) {
                try {
                    context.unpush();
                    Definition precedingDef = context.peek().def;
                    if (precedingDef.isAlias() && getName().equals(precedingDef.getAlias().getName())) {
                        nd = precedingDef.getSuperDefinition();
                        if (!nd.equals(this) && nd.hasChildDefinition(node.getName())) {
                            Type st = precedingDef.getSuper();
                            ArgumentList superArgs = st.getArguments(context);
                            ParameterList superParams = nd.getParamsForArgs(superArgs, context);
                            context.unpop(nd, superParams, superArgs);
                            try {
                                return nd.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
        
                            } finally {
                                context.repop();
                            }
                        }
                    }
                    
                } finally {
                    context.repush();
                }
            }
        }

        return (generate ? UNDEFINED : null);
    }

    public KeepStatement getKeep(String key) {
        if (keepsAndSuperKeeps != null) {
            Iterator<KeepStatement> it = keepsAndSuperKeeps.iterator();
            while (it.hasNext()) {
                KeepStatement keep = it.next();
                if (keep.contains(key)) {
                    return keep;
                }
            }
        }
        return null;
    }


    public void addKeep(KeepStatement keep) {
        if (keeps == null) {
            keeps = Context.newArrayList(1, KeepStatement.class);
        }
        keeps.add(keep);
    }

    public boolean hasKeeps() {
        return ((keepsAndSuperKeeps != null && keepsAndSuperKeeps.size() > 0) || (keeps != null && keeps.size() > 0));
    }
    
    public List<KeepStatement> getKeeps() {
        return (keepsAndSuperKeeps != null ? keepsAndSuperKeeps : keeps);
    }

    protected void setKeeps(List<KeepStatement> keeps) {
        this.keeps = keeps;
    }

    /** Returns the definitions named in keep directives. This requires that the keep statement
     *  list returned by getKeeps is in order from subclass up through superclasses.
     **/
    public Definition[] keep_defs(Context context) {
        List<KeepStatement> keeps = getKeeps();
        int size = (keeps == null ? 0 : keeps.size());
        HashMap<String, Definition> defMap = new HashMap<String, Definition>(size);
        if (keeps != null) {
            for (KeepStatement k : keeps) {
                try {
                    Definition[] kdefs = k.getDefs(context);
                    for (Definition d : kdefs) {
                        if (defMap.get(d.getName()) == null) {
                            defMap.put(d.getName(), d);
                        }
                    }
                } catch (Redirection r) {
                    log("Unable to return def for keep statement " + k + ": " + r);
                }
            }
        }
        Definition[] defs = new Definition[defMap.size()];
        
        return defMap.values().toArray(defs);
    }

    /** This method is called during the link pass so that cross site subclassing
     *  can be supported.nitialized.  See the Linker class in canto.lang.SiteLoader.
     */
    public void resolveKeeps() {
        // resolve keeps in superdefinitions
        if (keeps != null) {
            keepsAndSuperKeeps = Context.newArrayList(keeps);
        }
        NamedDefinition sdef = getSuperDefinition();
        if (sdef != null) {
            List<KeepStatement> superKeeps = sdef.getKeeps();
            if (superKeeps != null) {
                if (keepsAndSuperKeeps != null) {
                    keepsAndSuperKeeps.addAll(superKeeps);
                } else {
                    keepsAndSuperKeeps = Context.newArrayList(superKeeps);
                }
            }
        }
        
    }

    DefinitionTable getDefinitionTable() {
        NamedDefinition owner = (NamedDefinition) getOwner();
        if (owner == null) {
            log("NamedDefinition " + getFullName() + " has no owner!");
            return null;
        }
        return owner.getDefinitionTable();
    }
    
    // methods in the definition api
    
    /** Gets the cached value for this definition in the current context,
     *  if the definition is cacheable and a value is present in the cache,
     *  else constructs the definition with the passed arguments. 
     */
    public Object get(Context context, ArgumentList args) throws Redirection {
        Object data = null;
        if (getDurability() != DYNAMIC && (args == null || !args.isDynamic())) {
            data = context.getData(this, getName(), args, null);
            if (data != null) {
                return data;
            }
        }
        data = instantiate(context, args);
        if (data != null && getDurability() != DYNAMIC && (args == null || !args.isDynamic())) {
            context.putData(this, args, null, getName(), data);
        }
        return data;
    }
    
    /** Gets the cached value for this definition in the current context,
     *  if the definition is cacheable and a value is present in the cache,
     *  else constructs the definition with no arguments. 
     */
    public Object get(Context context) throws Redirection {
        return get(context, null);
    }
    
}

