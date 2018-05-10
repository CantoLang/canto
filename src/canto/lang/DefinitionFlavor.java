/* Canto Compiler and Runtime Engine
 * 
 * DefinitionFlavor.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

/**
 * A DefinitionFlavor is a definition with a specific parameter list.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.27 $
 */

public class DefinitionFlavor extends ComplexDefinition {

    public Definition def;
    public ParameterList params;

    public DefinitionFlavor(Definition def, Context context, ParameterList params) throws Redirection {
        // the context shouldn't be needed, pass null
        super(def, null);
        this.def = def;
        this.params = params;
    }

    public ParameterList getParameters() {
        return params;
    }

    /** Overridden to always return the parameter list associated with this definition flavor,
     *  regardless of the arguments.
     */
    protected ParameterList getMatch(ArgumentList args, Context argContext) {
        return getParameters();
    }

    public boolean equals(Object obj) {
        if (obj instanceof DefinitionFlavor) {
            DefinitionFlavor defFlavor = (DefinitionFlavor) obj;
            return (def.equals(defFlavor.def) && params.equals(defFlavor.params));
        } else {
            return def.equals(obj);
        }
    }

    // ComplexDefinition methods are overridden to call the delegate.  They will throw
    // ClassCastExceptions if the delegate definition is not a ComplexDefinition.

    /** Calls the delegate.  Throws a ClassCastException if the delegate definition
     *  is not a ComplexDefinition.
     */
    protected Definition getExplicitDefinition(NameNode name, ArgumentList args, Context argContext) throws Redirection {
        return ((ComplexDefinition) def).getExplicitDefinition(name, args, argContext);
    }

    /** Calls the delegate.  Throws a ClassCastException if the delegate definition
     *  is not a ComplexDefinition.
     */
    public Definition getExplicitChildDefinition(NameNode name) {
        return ((NamedDefinition) def).getExplicitChildDefinition(name);
    }

    DefinitionTable getDefinitionTable() {
        return ((AnonymousDefinition) def).getDefinitionTable();
    }

    public void addDefinition(Definition newDef, boolean replace) throws DuplicateDefinitionException {
        ((AnonymousDefinition) def).addDefinition(newDef, replace);
    }

    // Definition interface methods are all handled by delegation

    /** Returns the name. */
    public String getName() {
        return def.getName();
    }

    /** Returns true if the definition this is a flavor of is abstract, i.e.,
     *  contains an abstract construction.
     */
    public boolean isAbstract(Context context) {
        return def.isAbstract(context);
    }

    /** Returns true if the definition this is a flavor of is primitive.
     */
    public boolean isPrimitive() {
        return ((AbstractNode) def).isPrimitive();
    }

    /** Returns the access modifier. */
    public int getAccess() {
        return def.getAccess();
    }

    /** Returns the durability modifier. */
    public int getDurability() {
        return def.getDurability();
    }

    /** Returns the associated type object. */
    public Type getType() {
        return def.getType();
    }

    /** Returns true if this definition contains a <code>next</code> statement.
     */
    public boolean hasNext(Context context) {
    	return def.hasNext(context);
    }

    /** Returns a linked list of lateral superdefinitions -- superdefinitions that contain
     *  a <code>next</code> statement, or null if there are no such superdefinitions.
     */
    public LinkedList<Definition> getNextList(Context context) {
    	return def.getNextList(context);
    }; 


    /** Returns the supertype, or null if unspecified. */
    public Type getSuper() {
        return def.getSuper();
    }

    /** Returns true if <code>name</code> is the name of an ancestor of this
     *  definition.
     */
    public boolean isSuperType(String name) {
        return def.isSuperType(name);
    }

    /** Returns the superdefinition, or null if unspecified.  The context does not affect
     *  how the superdefinition is resolved; rather, it allows the supertype arguments to
     *  be resolved, and a DefinitionFlavor to be returned if the superdefinition has
     *  multiple parameter lists.
     */
    public NamedDefinition getSuperDefinition(Context context) {
        return def.getSuperDefinition(context);
    }

    /** Returns the superdefinition, or null if unspecified. */
    public NamedDefinition getSuperDefinition() {
        return def.getSuperDefinition();
    }

    /** Returns the immediate subdefinition of this definition in the current context,
     *  or null if not found.  This method assumes that this definition or a subdefinition
     *  is currently being constructed, so the top of the context stack will contain this
     *  definition or a subdefintion.
     */
    public Definition getImmediateSubdefinition(Context context) {
        return def.getImmediateSubdefinition(context);
    }
    
    /** Returns the underdefinition, or null if unspecified.  The underdefinition is the
     *  one this definition overrides -- i.e., the child definition by the same name as
     *  this definition in the superdefinition of this definition's owner.  
     */
    public NamedDefinition getUnderDefinition(Context context) {
        return def.getUnderDefinition(context);
    }

    /** Returns true if this definition contains a <code>sub</code> statement,
     *  or is empty and <code>hasSub</code> called on its superdefinition (if any)
     *  returns true.
     */
    public boolean hasSub(Context context) {
        return def.hasSub(context);
    }

    public boolean isSubDefinition(NamedDefinition subDef) {
        return (def instanceof NamedDefinition && ((NamedDefinition) def).isSubDefinition(subDef));
    }

    /** Returns the full name, including the ownership chain, in dot notation */
    public String getFullName() {
        return def.getFullName();
    }

    /** Returns the full name, with the ownership chain adjusted to reflect the
     *  actual subclasses, in dot notation.
     */
    public String getFullNameInContext(Context context) {
        return def.getFullNameInContext(context);
    }
    
    /** Returns the context of this definition, or null if none. */
    public Definition getOwner() {
        return def.getOwner();
    }

    public Object getChild(NameNode name, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context argContext, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection {
        return def.getChild(name, args, indexes, parentArgs, argContext, generate, trySuper, parentObj, resolver);
    }

    /** Returns the child definition by the specified name.
     */
    public Definition getChildDefinition(NameNode name, Context context) {
        return def.getChildDefinition(name, context);
    }


    /** Instantiates a child definition of the specified name in the specified context and
     *   returns the result.
     */
    public Object getChildData(NameNode childName, Type type, Context context, ArgumentList args) throws Redirection {
        return def.getChildData(childName, type, context, args);
    }


    /** Returns a list of formal parameter lists associated with this definition, or
     *  null if none are defined.
     */
    public List<ParameterList> getParamLists() {
        return def.getParamLists();
    }

    /** Gets a list of constructions comprising this definition */
    public List<Construction> getConstructions(Context context) {
        return def.getConstructions(context);
    }

    /** Gets the contents of the definition as a single node, which may be a block,
     *  an array or a single value.
     */
    public AbstractNode getContents() {
        return def.getContents();
    }
}
