/* Canto Compiler and Runtime Engine
 * 
 * TypeList.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

/**
 * A TypeList is a list of types.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.35 $
 */
public class TypeList extends ListNode<Type> implements Type {

    @SuppressWarnings("unchecked")
    static public Type addTypes(Type type1, Type type2) {
    	Definition owner = null;
        List<Type> list = new ArrayList<Type>();
        if (type1 != null) {
            if (type1 instanceof Collection<?>) {
                list.addAll((Collection<Type>) type1);
            } else {
                list.add(type1);
            }
            owner = ((AbstractNode) type1).getOwner();
        }
        if (type2 != null) {
            if (type2 instanceof Collection<?>) {
                Iterator<?> it = ((Collection<?>) type2).iterator();
                while (it.hasNext()) {
                    Object t = it.next();
                    if (!list.contains(t)) {
                        list.add((Type) t);
                    }
                }
            } else {
                if (!list.contains(type2)) {
                    list.add(type2);
                }
            }
            if (owner == null) {
            	owner = ((AbstractNode) type2).getOwner();
            }
        }
        
        int n = list.size();
        if (n == 0) {
            return null;
        } else if (n == 1) {
            return (Type) list.get(0);
        } else {
        	return new TypeList(list, owner);
        }
    }
    
    private Definition multiDef = null;

    public TypeList() {
        super();
    }

    public TypeList(List<Type> list, Definition owner) {
        super(list);
        setOwner(owner);
        resolve();
    }

    /** Returns true if the passed value is an instance of any of the member types in the
     *  specified context.
     */
    public boolean isInstance(Object obj, Context context) {
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            if (t.isInstance(obj, context)) {
                return true;
            }
        }
        return false;
    }

    /** Returns true if any type in the list is external. */
    public boolean isExternal() {
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            if (t.isExternal()) {
                return true;
            }
        }
        return false;
    }

    /** Returns true only if every type in the list is primitive. */
    public boolean isPrimitive() {
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            if (!t.isPrimitive()) {
                return false;
            }
        }
        return true;
    }

    /** Returns true only if every type in the list is special. */
    public boolean isSpecial() {
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            if (!t.isSpecial()) {
                return false;
            }
        }
        return true;
    }

    /** Returns true if the passed type is the same as or is a supertype of any of the
     *  member types in the specified context.
     */
    public boolean isTypeOf(Type type, Context context) {
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            if (t.isTypeOf(type, context)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTypeOf(String typeName) {
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            if (t.isTypeOf(typeName)) {
                return true;
            }
        }
        return false;
    }


    /** Returns true if the passed type is the same as or is a component of
     *  this type.
     */
    public boolean includes(Type type) {
        if (equals(type)) {
            return true;
        } else {
            Iterator<Type> it = iterator();
            while (it.hasNext()) {
                Type t = it.next();
                if (t.includes(type)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Returns true if a type of the passed name is the same as or is a component of
     *  this type. 
     */
    public boolean includes(String typeName) {
    	if (getName().equals(typeName)) {
    		return true;
        } else {
            Iterator<Type> it = iterator();
            while (it.hasNext()) {
                Type t = it.next();
                if (t.includes(typeName)) {
                    return true;
                }
            }
            return false;
    	}
    }

    public Type[] getChildTypes() {
        Type[] childTypes = null;
        List<Type> types = new ArrayList<Type>();
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            types.addAll(Arrays.asList(t.getChildTypes()));
        }
        childTypes = (Type[]) types.toArray(childTypes);
        return childTypes;
    }

    public Type[] getPersistableChildTypes() {
        Type[] childTypes = null;
        List<Type> types = new ArrayList<Type>();
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            types.addAll(Arrays.asList(t.getPersistableChildTypes()));
        }
        childTypes = (Type[]) types.toArray(childTypes);
        return childTypes;
    }

    
    /** Converts a value of an arbitrary type to a value of this type. */
    public Value convert(Value val) {
        throw new UnsupportedOperationException("TypeList doesn't support convert()");
    }

    /** Returns the dimensions associated with this type, or an empty list
     *  if this is not a collection type.
     */
    public List<Dim> getDims() {
        throw new UnsupportedOperationException("TypeList doesn't support getDims()");
    }

    /** Returns true only if the list is not empty and every type in the list represents
     *  a collection.
     */
    public boolean isCollection() {
        boolean iscoll = false;
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            if (t.isCollection()) {
                iscoll = true;
            } else {
                return false;
            }
        }
        return iscoll;
    }


    /** Returns true if any of the types in the list inherits a collection. */
    public boolean inheritsCollection() {
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            if (t.inheritsCollection()) {
                return true;
            }
        }
        return false;
    }

    /** Returns the collection (array or table) type this type 
     * represents or is a subtype of, if any, else null.
     */
    public Type getCollectionType() {
        Type collectionType = null;
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            if (collectionType == null) {
                collectionType = t.getCollectionType();
            } else {
                collectionType = addTypes(collectionType, t.getCollectionType());
            }
        }
        return collectionType;
    }
    
    /** Returns true if this type represents an array. */
    public boolean isArray() {
        boolean isarray = false;
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            if (t.isArray()) {
                isarray = true;
            } else {
                return false;
            }
        }
        return isarray;
    }

    /** Returns the array type this type represents or is a subtype of, if any, else null. */
    public Type getArrayType() {
        Type arrayType = null;
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            if (arrayType == null) {
                arrayType = t.getArrayType();
            } else {
                arrayType = addTypes(arrayType, t.getArrayType());
            }
        }
        return arrayType;
    }
    	
    /** Returns true if this type represents a table. */
    public boolean isTable() {
        boolean istable = false;
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            if (t.isTable()) {
                istable = true;
            } else {
                return false;
            }
        }
        return istable;
    }

    /** Returns the table type this type represents or is a subtype of, if any, else null. */
    public Type getTableType() {
        Type tableType = null;
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            if (tableType == null) {
                tableType = t.getTableType();
            } else {
                tableType = addTypes(tableType, t.getTableType());
            }
        }
        return tableType;
    }
    	
    /** Returns the base type, not including dimensions, represented by this type. */
    public Type getBaseType() {
        Type baseType = null;
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            if (baseType == null) {
                baseType = t.getBaseType();
            } else {
                baseType = addTypes(baseType, t.getBaseType());
            }
        }
        return (baseType != null ? baseType : this);
    }
    

    /** Returns true if this type can be the supertype of a type with the specified parameters. **/
    public boolean canBeSuperForParams(ParameterList params, Context context) {
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            if (t.canBeSuperForParams(params, context)) {
                return true;
            }
        }
        return false;
    }

    /** Returns the specific type in this list which best matches the passed arguments,
     *  or null if no types match.
     */
    public Type getTypeForParams(ParameterList params, Context context) {
        Type closestType = null;
        int numClosestArgs = -1;
        int numParams = (params == null ? 0 : params.size());
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            if (t.getDefinition().hasNext(context)) {
            	continue;
            }
            ArgumentList typeArgs = t.getArguments(context);
            int numArgs = (typeArgs == null ? 0 : typeArgs.size());
            if (numParams == 0) {
                if (numArgs == 0) {
                    return t;
                } else if (closestType == null) {
                    closestType = t;
                    numClosestArgs = numArgs;
                }
            } else if (numArgs > numClosestArgs) {
                boolean matches = true;
                if (numArgs > 0) {
                    Iterator<Construction> itArgs = typeArgs.iterator();
                    while (itArgs.hasNext()) {
                        if (!params.matchesArg(itArgs.next())) {
                            matches = false;
                            break;
                        }
                    }
                }
                if (matches) {
                    closestType = t;
                    numClosestArgs = numArgs;
                }
            }
        }
        return closestType;
    }

    /** Returns the arguments associated with this type, or an empty list
     *  if this type has no associated arguments.
     */
    public ArgumentList getArguments(Context context) {
        throw new UnsupportedOperationException("TypeList doesn't support getArguments()");
    }

    public Definition getDefinition() {
        if (!resolved) {
            resolve();
        }
        return multiDef;
    }

    public Type getSuper() {
        if (!resolved) {
            resolve();
        }
        if (multiDef != null) {
            return multiDef.getSuper();
        } else {
            return null;
        }
    }
    
    /** Returns the most specific common class of values of this type. */
    public Class<?> getTypeClass(Context context) {
        Class<?> typeClass = null;
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            Class<?> tc = t.getTypeClass(context);
            if (typeClass == null) {
                typeClass = tc;
            } else if (tc != null) {
                if (tc.isAssignableFrom(typeClass)) {
                    typeClass = tc;
                } else if (!typeClass.isAssignableFrom(tc)) {
                    typeClass = null;
                    break;
                }
            }
        }
        return typeClass;
    }

    public int numParts() {
        return -1;
    }

    public int levelsBelow(Type type, Context context) {
        if (this.equals(type)) {
            return 0;
        }
        int levels = Integer.MAX_VALUE;
        Iterator<Type> it = iterator();
        while (it.hasNext()) {
            Type t = it.next();
            int n = t.levelsBelow(type, context);
            if (n >= 0 && n < levels) {
                levels = n;
            }
        }
        if (levels == Integer.MAX_VALUE) {
            levels = -1;
        }
        return levels;
    }

    /** flag used by resolve method */
    private boolean resolved = false;

    /** Recursively resolves member types. **/
    public void resolve() {
        if (!resolved) {
            List<Definition> definitions = new ArrayList<Definition>(size());
            Iterator<Type> it = iterator();
            while (it.hasNext()) {
                Type t = it.next();
                t.resolve();
                Definition def = t.getDefinition();
                // if any definition is unresolvable, abort
                if (def == null) {
                    return;
                }
                definitions.add(def);
            }
            multiDef = new MultiDefinition(this, definitions);
            resolved = true;
        }
    }
}

class MultiDefinition extends NamedDefinition {
    TypeList types;
    List<Definition> definitions;

    public MultiDefinition(TypeList types, List<Definition> definitions) {
        super();
        setOwner(types.getOwner());
        this.types = types;
        this.definitions = definitions;
        int numLists = 0;
        Iterator<Definition> it = definitions.iterator();
        while (it.hasNext()) {
            List<ParameterList> lists = it.next().getParamLists();
            if (lists != null) {
                numLists += lists.size();
            }
        }
        if (numLists > 0) {
            List<ParameterList> paramLists = Context.newArrayList(numLists, ParameterList.class);
            it = definitions.iterator();
            while (it.hasNext()) {
                List<ParameterList> lists = ((Definition) it.next()).getParamLists();
                if (lists != null) {
                    paramLists.addAll(lists);
                }
            }
            setParamLists(paramLists);
        }

    }

    /** Returns true if the passed definition either equals this definition or is included
     *  in this definition.
     */
    public boolean includes(Definition def) {
        if (equals(def)) {
            return true;
        } else {
            Iterator<Definition> it = definitions.iterator();
            while (it.hasNext()) {
                Definition d = it.next();
                if (d instanceof NamedDefinition) {
                    if (((NamedDefinition) d).includes(def)) {
                        return true;
                    }
                } else {
                    if (d.equals(def)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }



    /** Returns <code>true</code> unless one of the definitions in the list
     *  is abstract.
     */
    public boolean isAbstract(Context context) {
        Iterator<Definition> it = definitions.iterator();
        while (it.hasNext()) {
            Definition def = it.next();
            if (def.isAbstract(context)) {
                return true;
            }
        }
        return false;
    }

    /** Returns <code>true</code> only if all of the definitions in the list
     *  are primitive.
     */
    public boolean isPrimitive() {
        Iterator<Definition> it = definitions.iterator();
        while (it.hasNext()) {
            AbstractNode node = (AbstractNode) it.next();
            if (!node.isPrimitive()) {
                return false;
            }
        }
        return true;
    }

    /** Returns the minimum of the access values for all definitions. */
    public int getAccess() {
        int access = PUBLIC_ACCESS;
        Iterator<Definition> it = definitions.iterator();
        while (it.hasNext()) {
            Definition def = it.next();
            access = Math.min(access, def.getAccess());
        }
        return access;
    }

    /** Returns the minimum of the durability values for all definitions. */
    public int getDurability() {
        int dur = STATIC;
        Iterator<Definition> it = definitions.iterator();
        while (it.hasNext()) {
            Definition def = it.next();
            dur = Math.min(dur, def.getDurability());
        }
        return dur;
    }

    public Definition getExplicitChildDefinition(NameNode node) {
        Iterator<Definition> it = definitions.iterator();
        // this may not be exactly right -- in some cases the favored definition
        // is not first in the list, such as when a superclass farther in the list
        // has been selected because of the parameter signature.  But we don't have
        // enough information here to know when that's the case.
        while (it.hasNext()) {
            Definition def = it.next();
            if (def instanceof NamedDefinition) {
            	Definition childDef = ((NamedDefinition) def).getExplicitChildDefinition(node);
            	if (childDef != null) {
                    return childDef;
            	}
            }
        }
        return null;
    }

    /** Returns the keeps in all the definitions. */
    public List<KeepStatement> getKeeps() {
        List<KeepStatement> allKeeps = super.getKeeps();
        if (allKeeps == null) {
            allKeeps = new ArrayList<KeepStatement>(0);
            Iterator<Definition> it = definitions.iterator();
            while (it.hasNext()) {
                Definition def = it.next();
                if (def instanceof NamedDefinition) {
                    List<KeepStatement> keeps = ((NamedDefinition) def).getKeeps();
                    if (keeps != null) {
                        allKeeps.addAll(keeps);
                    }
                }
            }
            setKeeps(allKeeps);
        }
        return allKeeps;
    }

    /** Returns the associated PrimitiveType object. */
    public Type getType() {
        return types;
    }

    /** Returns a TypeList created from the supertypes of all the definitions in the list
     *  that have non-null supertypes.
     */
    public Type getSuper() {
        List<Type> sts = Context.newArrayList(definitions.size(), Type.class);
        Iterator<Definition> it = definitions.iterator();
        while (it.hasNext()) {
            Definition def = it.next();
            Type st = def.getSuper();
            if (st != null) {
                sts.add(st);
            }
        }
        int len = sts.size();
        if (len > 1) {
            return new TypeList(sts, getOwner());
        } else if (len == 1) {
            return (Type) sts.get(0);
        } else {
            return null;
        }
    }


    public NamedDefinition getSuperDefinition(Context context) {
        List<Definition> sdefs = Context.newArrayList(definitions.size(), Definition.class);
        List<Type> sts = Context.newArrayList(definitions.size(), Type.class);
        Iterator<Definition> it = definitions.iterator();
        while (it.hasNext()) {
            Definition def = it.next();
            NamedDefinition sdef = def.getSuperDefinition(context);
            Type st = def.getSuper();
            if (sdef != null && st != null) {
                sdefs.add(sdef);
                sts.add(st);
            }
        }
        if (sdefs.size() > 0) {
            NamedDefinition newDef = new MultiDefinition(new TypeList(sts, getOwner()), sdefs);
            // since this is a synthetic definition, it doesn't really have a single owner,
            // so provide it with an arbitrary owner in order to give it access to a
            // definition table.
            newDef.setOwner(getOwner());
            return newDef;
        } else {
            return null;
        }
    }


    /** Returns a linked list of member definitions that have <code>next</code> statements.
     *  If there are none, returns null.
     */
    public LinkedList<Definition> getNextList(Context context) {
        LinkedList<Definition> nextList = null;
        Iterator<Definition> it = definitions.iterator();
        while (it.hasNext()) {
            Definition def = it.next();
            if (def.hasNext(context)) {
                if (nextList == null) {
                    nextList = new LinkedList<Definition>();
                }
                nextList.add(def);
            }
        }
    
        return nextList;
    }

    protected Definition getDefinitionFlavor(Context context, ParameterList params) throws Redirection {
        Definition selectedDef;
        if (params == null) {
            if (definitions == null || definitions.size() < 1) {
                return null;
            }
            selectedDef = (Definition) definitions.get(0);
        } else {
            selectedDef = params.getOwner();
        }

        // ugly cast, but I'm hesitant to add getDefinitionFlavor to the Definition interface
        return ((AnonymousDefinition) selectedDef).getDefinitionFlavor(context, params);
    }

    /** Returns the full names of all the definitions in the list in array format, i.e.
     *  comma-separated and bracketed with square brackets.
     */
    public String getFullName() {
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        Iterator<Definition> it = definitions.iterator();
        while (it.hasNext()) {
            Definition def = it.next();
            sb.append(def.getFullName());
            if (it.hasNext()) {
                sb.append(',');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /** Returns the names in array format, i.e. comma-separated and bracketed with
     *  square brackets.
     */
    public String getName() {
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        Iterator<Definition> it = definitions.iterator();
        while (it.hasNext()) {
            Definition def = it.next();
            sb.append(def.getName());
            if (it.hasNext()) {
                sb.append(',');
            }
        }
        sb.append(']');
        return sb.toString();
    }
    
    /** Returns true if any of the definitions return true.
     */
    public boolean hasNext(Context context) {
        Iterator<Definition> it = definitions.iterator();
        while (it.hasNext()) {
            Definition def = it.next();
            if (def.hasNext(context)) {
                return true;
            }
        }
        return false;
    }

    /** Returns true if any of the definitions return true.
     */
    public boolean hasSub(Context context) {
        Iterator<Definition> it = definitions.iterator();
        while (it.hasNext()) {
            Definition def = it.next();
            if (def.hasSub(context)) {
                return true;
            }
        }
        return false;
    }

    /** Returns true if any of the definitions return true.
     */
    public boolean isSubDefinition(NamedDefinition subDef) {
        Iterator<Definition> it = definitions.iterator();
        while (it.hasNext()) {
            Definition def = it.next();
            if (def instanceof NamedDefinition && ((NamedDefinition) def).isSubDefinition(subDef)) {
                return true;
            }
        }
        return false;
    }

    /** Returns true if any of the definitions return true.
     */
    public boolean isSuper(Type type) {
        Iterator<Definition> it = definitions.iterator();
        while (it.hasNext()) {
            Definition def = it.next();
            if (def instanceof NamedDefinition && ((NamedDefinition) def).isSuper(type)) {
                return true;
            }
        }
        return false;
    }

    /** Returns true if any of the definitions return true.
     */
    public boolean isSuperType(String name) {
        Iterator<Definition> it = definitions.iterator();
        while (it.hasNext()) {
            Definition def = it.next();
            if (def.isSuperType(name)) {
                return true;
            }
        }
        return false;
    }

    public NamedDefinition getSuperDefinition() {
        return getSuperDefinition(null);
    }

    public int getDimSize() {
        throw new UnsupportedOperationException("can't call getDimSize() on a MultiDefinition");
    }

    /** Returns the child definition of the specified name.  If only one definition in
     *  the list has a child by the specified name, returns that definition.  If more
     *  that one definition has such a child, returns a MultiDefinition containing the
     *  list of definitions.  If no definition has such a child, returns null.
     */
    public Definition getChildDefinition(NameNode node, Context context) {
        int size = types.size();
        List<Type> childTypes = Context.newArrayList(size, Type.class);
        List<Definition> childDefs = Context.newArrayList(size, Definition.class);
        Iterator<Definition> it = definitions.iterator();
        while (it.hasNext()) {
            Definition def = it.next();
            Definition child = def.getChildDefinition(node, context);
            if (child != null) {
                childTypes.add(child.getType());
                childDefs.add(child);
            }
        }
        int len = childTypes.size();
        if (len > 1) {
            TypeList typeList = new TypeList(childTypes, this);
            return new MultiDefinition(typeList, childDefs);
        } else if (len == 1) {
            return (Definition) childDefs.get(0);
        } else {
            return null;
        }
    }


    public Object getChild(NameNode node, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context argContext, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection {
        Redirection r = null;
        Object failed = (generate ? UNDEFINED : null);
        Iterator<Definition> it = definitions.iterator();
        while (it.hasNext()) {
            Definition def = it.next();
            try {
                Object obj = def.getChild(node, args, indexes, parentArgs, argContext, generate, trySuper, parentObj, resolver);
                if (obj != null && obj != UNDEFINED) {
                    return obj;

                // indicate that the definition was found but generated null
                } else if (generate && obj == null) {
                    failed = null;
                }

            } catch (Throwable t) {
                if (r == null && t instanceof Redirection) {
                    r = (Redirection) t;
                }
            }
        }
        return failed;
    }
}
