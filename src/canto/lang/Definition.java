/* Canto Compiler and Runtime Engine
 * 
 * Definition.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

/**
 * A Definition is an object which describes a class of objects.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.70 $
 */

public interface Definition extends Name {

    // The modifier values are such that for groups of definitions, the lowest value
    // governs.  For example, if a group of five definitions includes one dynamic one,
    // the group as a whole is considered dynamic.  Definition groups arise when a
    // definition has multiple supertypes; the superdefinition of such a definition is
    // a definition group.


    // access modifiers

    /** Corresponds to the <code>local</code> keyword. */
    public final static int LOCAL_ACCESS = 0;

    /** Corresponds to no access modifer keyword. */
    public final static int SITE_ACCESS = 1;

    /** Corresponds to the <code>public</code> keyword. */
    public final static int PUBLIC_ACCESS = 2;


    // durability modifiers

    /** Instances of this definition should be reconstructed every time they are referenced.
     *  Corresponds to the <code>dynamic</code> keyword.
     */
    public final static int DYNAMIC = 0;

    /** Instances of this definition should be retrieved from the cache when possible, else
     *  reconstructed.  Corresponds to no durability modifer keyword.
     */
    public final static int IN_CONTEXT = 1;

    /** Instances of this definition should only be constructed if they have not been
     *  constructed before or construction is forced via a dynamic instantiation.
     *   Corresponds to the <code>global</code> keyword.
     */
    public final static int GLOBAL = 2;

    /** Instances of this definition should only be constructed once.  Corresponds to 
     *  the <code>static</code> keyword.
     */
    public final static int STATIC = 3;
    

    // signature-matching scores

    /** The signatures match perfectly */
    public final static int PERFECT_MATCH = 0;

    /** The signatures don't match */
    public final static int NO_MATCH = Integer.MAX_VALUE / 2;

    /** The signatures don't match */
    public final static int QUESTIONABLE_MATCH = NO_MATCH - 16384; 
            
    /** The parameter type is the default type */
    public final static int PARAM_DEFAULT = 256;

    /** An argument is a null literal, which can match anything */
    public final static int ARG_NULL = 128;

    /** An argument is missing */
    public final static int ARG_MISSING = 16384;


    /** Returns true if this definition is abstract, i.e.,
     *  contains an abstract construction.
     */
    public boolean isAbstract(Context context);

    /** Returns the access modifier. */
    public int getAccess();

    /** Returns the durability modifier. */
    public int getDurability();

    /** Convenience method; returns true if the definition is
     *  global or static (i.e., durability is GLOBAL or STATIC).
     */
    public boolean isGlobal();
   
    /** Returns the associated type object. */
    public Type getType();

    /** If this definition has multiple parameter lists, or is a multiple definition (i.e. the
     *  superdefinition of an object with multiple supertypes), returns the specific definition
     *  flavor that matches the passed arguments; otherwise, simply returns the original
     *  definition.
     */
    public Definition getDefinitionForArgs(ArgumentList args, Context argContext);

    /** Returns true if this definition contains a <code>sub</code> statement,
     *  or is empty and <code>hasSub</code> called on its superdefinition (if any)
     *  returns true.
     */
    public boolean hasSub(Context context);
    
    /** Returns true if this definition contains a <code>next</code> statement.
     */
    public boolean hasNext(Context context);
    
    /** Returns a linked list of definitions with <code>next</code> statements.  If this
     *  definition is a single definition and has a next statement, the linked list will
     *  have a single element (this definition).  If the definition is a multidefinition,
     *  and one or more of the member definitions have a next statment, the linked list
     *  will contain those definitions in the order they appear.  Otherwise this
     *  method returns null.
     */
    public LinkedList<Definition> getNextList(Context context); 

  	/** Returns the supertype, or null if unspecified. */
    public Type getSuper();

    /** Returns the supertype, or null if unspecified, given the specified context. */
    public Type getSuper(Context context);

    /** Returns the supertype of this definition which corresponds to the owner of the
     *  childDef, or is a subtype of the owner of childDef.
     * 
     *  This method enables the identification of the specific chain of supertypes within a 
     *  multiple inheritance tree of supertypes which leads to the parent  
     */
    public Type getSuperForChild(Context context, Definition childDef) throws Redirection;

    /** Returns the superdefinition, or null if unspecified.  The context does not affect
     *  how the superdefinition is resolved; rather, it allows the supertype arguments to
     *  be resolved, and a DefinitionFlavor to be returned if the superdefinition has
     *  multiple parameter lists.
     */
    public NamedDefinition getSuperDefinition(Context context);

    /** Returns the superdefinition, or null if unspecified. */
    public NamedDefinition getSuperDefinition();

    /** Returns the immediate subdefinition of this definition in the current context,
     *  or null if not found.  This method assumes that this definition or a subdefinition
     *  is currently being constructed, so the top of the context stack will contain this
     *  definition or a subdefintion.
     */
    public Definition getImmediateSubdefinition(Context context);
    
    /** Returns the underdefinition, or null if unspecified.  The underdefinition is the
     *  one this definition overrides -- i.e., the child definition by the same name as
     *  this definition in the superdefinition of this definition's owner.  
     */
    public NamedDefinition getUnderDefinition(Context context);

    /** Returns the first definition in the context stack that equals or extends this
     *  definition. When a child of this definition is instantiated, its true owner is
     *  presumed to be the one returned by this method.  
     */
    public Definition getSubdefInContext(Context context);
    
    /** Returns the first entry in the context stack whose definition equals or extends 
     *  this definition.  
     */
    public Context.Entry getEntryInContext(Context context);

    /** Returns true if <code>name</code> is the name of an ancestor of this
     *  definition.
     */
    public boolean isSuperType(String name);

    /** Returns true if <code>def</code> equals or is a superdefinition of 
     *  this definition.
     */
    public boolean equalsOrExtends(Definition def);

    /** Returns true if <code>def</code> equals this definition in the
     * specified context.
     */
    public boolean equals(Definition def, Context context);

    /** Returns the full name, including the ownership chain, in dot notation */
    public String getFullName();

    /** Returns the full name, with the ownership chain adjusted to reflect the
     *  actual subclasses, in dot notation.
     */
    public String getFullNameInContext(Context context);

    /** Returns the containing definition, or null if none. */
    public Definition getOwner();

    /** Returns the site containing this definition, or equal to this
     *  definition.  
     */
    public Site getSite();
    
    /** Returns a NameNode representing the name of this definition, or null if
     *  this definition is anonymous.
     */
    public NameNode getNameNode();

    /** Returns true if this definition is unnamed. */
    public boolean isAnonymous();

    /** Construct this definition with the specified arguments and indexes in the specified
     *  context.
     */
    public Object instantiate(ArgumentList args, List<Index> indexes, Context context) throws Redirection;

    /** If this definition is a reference or an alias to another definition, returns the
     *  definition ultimately referenced after the entire chain of references and aliases
     *  has been resolved.
     */
    public Definition getUltimateDefinition(Context context);

    /** Find the child definition, if any, by the specified name; if <code>generate</code> is
     *  false, return the definition, else instantiate it and return the result.  If <code>generate</code>
     *  is true and a definition is not found, return UNDEFINED.
     * 
     *  The <code>trySuper</code> flag determines if this definition's superdefinition, if any, is
     *  checked, if the definition has no child by the specified name. 
     * 
     *  If <code>parentObj</code> is not null, and the child is a method of an external object, then
     *  the method will be called on <code>parentObj</code> when instantiated.
     * @param resolver TODO
     */
    public Object getChild(NameNode name, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context argContext, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection;

    /** Returns the child definition of the specified name, argument listand indexes if it 
     *  belongs to this definition or one of its superclasses.  If the argument list contains
     *  instantiations which must be resolved, a context should be supplied, otherwise
     *  argContext may be null.
     */
    public Definition getChildDefinition(NameNode name, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context argContext, Definition resolver);

    /** Returns the definition by the specified name if it belongs to this definition.  This
     *  version of getChildDefinition does not attempt to resolve the definition for a
     *  particular set of arguments, so no argument list is required.  The context is provided
     *  purely to resolve the parent's definition if it is aliased.
     */
    public Definition getChildDefinition(NameNode name, Context context);

    /** Returns true if this definition has a child definition by the specified name.
     */
    public boolean hasChildDefinition(String name);

    /** Returns the keep statement in this definition for the specified key.
     */
    public KeepStatement getKeep(String key);

    /** Instantiates a child definition with the specified name and type
     *  in the specified context
     */
    public Object getChildData(NameNode childName, Type type, Context context, ArgumentList args) throws Redirection;

    /**  Returns the parameter list associated with this definition which most
     *   closely matches the specified arguments, or null if this definition has
     *   no matching parameter lists.  If <code>validate</code> is false,
     *   definitions with only one parameter list can ignore the passed arguments
     *   and simply return its parameter list unchecked.
     */
    public ParameterList getParamsForArgs(ArgumentList args, Context argContext, boolean validate);

    /**  Returns the parameter list associated with this definition which most
     *   closely matches the specified arguments, or null if this definition has
     *   no matching parameter lists.
     */
    public ParameterList getParamsForArgs(ArgumentList args, Context argContext);

    /** Returns a list of formal parameter lists associated with this definition, or
     *  null if none are defined.
     */
    public List<ParameterList> getParamLists();

    /** Gets a list of constructions comprising this definition. */
    public List<Construction> getConstructions(Context context);


    /** Gets the contents of the definition as a single node, which may be a block,
     *  an array or a single value.
     */
    public AbstractNode getContents();

    /** Returns true if this definition can have child definitions. */
    public boolean canHaveChildDefinitions();

    /** Returns true if this definition is an identity definition. */
    public boolean isIdentity();

    /** Returns true if this definition is formal parameter definition. */
    public boolean isFormalParam();
    
    /** Returns true if this definition is an alias. */
    public boolean isAlias();

    /** Returns the aliased name, if this definition is an alias, else null. */
    public NameNode getAlias();

    /** Returns true if this definition is an alias of a parameter. */
    public boolean isParamAlias();

    /** Returns the aliased name, if this definition is an alias of a parameter, else null. */
    public NameNode getParamAlias();

    /** Returns the aliased instantiation, if this definition is an alias, else null. */
    public Instantiation getAliasInstance();

    /** Returns true if this definition is an alias or cached identity (which is effectively
     *  an alias in the context where it's cached).
     **/
    public boolean isAliasInContext(Context context);

    /** Returns the cached instance name, if this definition is a cached identity, or
     *  the aliased name, if this definition is an alias, else null.
     **/
    public NameNode getAliasInContext(Context context);

    /** Returns the cached instance, if this definition is a cached identity, or the aliased 
     *  instantiation, if this definition is an alias, else null.
     **/
    public Instantiation getAliasInstanceInContext(Context context);
    
    /** Returns true if this definition is a reference. */
    public boolean isReference();

    /** Returns the referenced name, if this definition is a reference, else null. */
    public NameNode getReference();

    /** Returns true if this definition is external. */
    public boolean isExternal();

    /** Returns true if this is a collection. */
    public boolean isCollection();
    
    /** Returns true if this is an object definition. */
    public boolean isObject();
    
    /** Returns this definition in the form of a collection definition.  May have unpredictable results if this
     *  definition does not actually reference a collection (i.e. if isCollection() returns false).
     */
    public CollectionDefinition getCollectionDefinition(Context context, ArgumentList args) throws Redirection;

    /** If this definition represents a collection, returns the size of the collection;
     *  else returns 1.
     */
    public int getSize();

    /** Returns a String constant from a Definition */
    public String getStringConstant( String typeName, String valueIfNotFound );

    /** Returns an object wrapping this definition with arguments and indexes. */ 
    public DefinitionInstance getDefInstance(ArgumentList args, List<Index> indexes);


    public Definition getOwnerInContext(Context context);

    // methods in definition Canto type
    
    /** Returns the child definitions belonging to this definition, in
     *  a table keyed on name.
     */
    public Map<String, Definition> defs();
    
    /** Returns the definitions named in keep directives. **/
    public Definition[] keep_defs(Context context);

    /** Returns all the immediate child definitions that have the specified type. */
    public Definition[] children_of_type(String typeName);
   
    /** Returns all the descendant definitions that have the specified type. */
    public Definition[] descendants_of_type(String typeName);
   
    /** Returns the nearest ancestor of the specified type. */
    public Definition ancestor_of_type(String typeName);

    /** Returns true if this definition defines an array. */
    public boolean is_array();
    
    /** Returns true if this definition defines a table. */
    public boolean is_table();
    
    /** Returns true if this definition equals or is a subdefinition of the
     *  passed type.
     */
    public boolean is_a(String typeName);
    
    /** Returns the simple name of the definition. */
    public String name();

    /** Returns the fully qualified name of the definition. */
    public String full_name();
    
    /** Gets the cached value for this definition in the current context,
     *  if the definition is cacheable and a value is present in the cache,
     *  else constructs the definition with the passed arguments. 
     */
    public Object get(Context context, ArgumentList args) throws Redirection;
    
    /** Gets the cached value for this definition in the current context,
     *  if the definition is cacheable and a value is present in the cache,
     *  else constructs the definition with no arguments. 
     */
    public Object get(Context context) throws Redirection;
    
    /** Retrieves from cache or instantiates this definition as an array.
     */
    public List<Object> get_array(Context context) throws Redirection;
    
    /** Retrieves from cache or instantiates this definition as a table.
     */
    public Map<String, Object> get_table(Context context) throws Redirection;

    /** Constructs this definition with the specified arguments in the current 
     *  context, passed in automatically.
     */
    public Object instantiate(Context context, ArgumentList args) throws Redirection;
    public Object instantiate(canto_context context, ArgumentList args) throws Redirection;
    
    /** Constructs this definition without arguments in the current context, 
     *  passed in automatically.
     */
    public Object instantiate(Context context) throws Redirection;
    public Object instantiate(canto_context context) throws Redirection;
    
    /** Instantiates this definition as an array.
     */
    public List<Object> instantiate_array(Context context) throws Redirection;
    public List<Object> instantiate_array(canto_context context) throws Redirection;

    /** Instantiates this definition as a table.
     */
    public Map<String, Object> instantiate_table(Context context) throws Redirection;
    public Map<String, Object> instantiate_table(canto_context context) throws Redirection;

    /** Go through all the parameter lists belonging to this definition and
     *  all the passed argument lists and select the best match (if any).
     *
     *  Returns an array of ListNodes with two elements, a ParameterList
     *  and an ArgumentList, or null if none matches.  Because the arguments
     *  are different types of ListNodes
     */
     public ListNode<?>[] getMatch(ArgumentList[] argLists, Context argContext);

}
