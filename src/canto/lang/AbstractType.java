/* Canto Compiler and Runtime Engine
 * 
 * AbstractType.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

/**
 * Base class for types.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.43 $
 */

abstract public class AbstractType extends NameNode implements Type {

    private Definition definition = null;

    public AbstractType() {
        super();
    }

    protected void setDefinition(Definition def) {
        definition = def;
    }

    public Definition getDefinition() {
        return definition;
    }

    public Type getSuper() {
        if (definition != null) {
            return definition.getSuper();
        } else {
            return null;
        }
    }
    
    
    /** Returns true if this is an external type.
     */
    public boolean isExternal() {
        return (definition != null && definition.isExternal());
    }

    /** Returns true if this is a primitive type.
     */
    public boolean isPrimitive() {
        String name = getName();

        return (name == null
                || name.equals(ANONYMOUS) || name.equals(DEFAULT)
                || name.equals("boolean") || name.equals("byte") || name.equals("char")
                || name.equals("double")  || name.equals("float")
                || name.equals("int")     || name.equals("long") || name.equals("short")
                || name.equals("string"));
    }

    public boolean isInstance(Value value) {
        String name = getName();
        String valName = value.getValueClass().getName();
        return name.equals(valName);
    }

    public boolean isInstance(Object obj, Context context) {
        String name = getName();
        
        // if we are checking for a definition, and this is a Definition,
        // return true
        //if ("definition".equals(name) && obj instanceof Definition) {
        //    return true;
        //}
        
        // if the object is an instantiation, first check for static type match
        if (obj instanceof Instantiation) {
            Instantiation instance = (Instantiation) obj;
            Definition def = instance.getDefinition(context);
            List<Index> indexes = instance.getIndexes();
            if (def == null) {
                return false;
            } else if (name.equals(def.getName())) {
                return true;
            } else if (name.equals(def.getFullName())) {
                return true;
            } else if (name.equals(def.getUltimateDefinition(context).getName())) {
                return true;
            } else if (def instanceof AliasedDefinition && name.equals(((AliasedDefinition) def).getAliasedDefinition(context).getName())) {
                return true;
            } else if (def.isSuperType(name)) {
                return true;
            } else if (def.getType().isTypeOf(name)) {
                return true;
                
            } else if (def instanceof ElementReference) {
                try {
                    Definition elementDef = ((ElementReference) def).getElementDefinition(context);
                    Object contents = elementDef.getContents();
                    if (contents instanceof Instantiation) {
                        Definition innerDef = ((Instantiation) contents).getDefinition(context);
                        if (innerDef.isSuperType(name)) {
                            return true;
                        }
                    }
                } catch (Redirection r) {
                    ;
                }
                
            } else if (def.isCollection() && indexes != null && indexes.size() > 0) {
                try {
                    CollectionDefinition collectionDef = def.getCollectionDefinition(context, null);
                    ElementReference elementRef = collectionDef.getElementReference(context, null, indexes);
                    Definition elementDef = elementRef.getElementDefinition(context);
                    Object contents = elementDef.getContents();
                    if (contents instanceof Instantiation) {
                        Definition innerDef = ((Instantiation) contents).getDefinition(context);
                        if (innerDef.isSuperType(name)) {
                            return true;
                        }
                    }
                } catch (Redirection r) {
                    ;
                }
            }                
            // if the type is not undefined, return false

            if (def.getSuperDefinition() != null) {
                return false;
            }
        }

        // if obj is an instantiation of undefined type and the static type comparison
        // above failed, the following code will check for a dynamic type match
        if (obj instanceof ValueGenerator) {
            ValueGenerator valgen = (ValueGenerator) obj;
            try {
                Value val = valgen.getValue(context);
                Class<?> c = val.getValueClass();
                return (c != null && name.equals(c.getName()));
            } catch (Redirection r) {
                return false;
            }

        } else if (obj instanceof Value) {
            Value val = (Value) obj;
            Class<?> c = val.getValueClass();
            return (c != null && name.equals(c.getName()));

        } else {
            return name.equals(obj.getClass().getName());
        }
    }

    /** Returns true if the passed type is the same as or is a supertype of
     *  this type in the specified context.
     */
    public boolean isTypeOf(Type type, Context context) {
        return (levelsBelow(type, context) >= 0);
    }

    /** Returns true if the passed string is the name of this type or a supertype
     *  of this type.
     */
    public boolean isTypeOf(String typeName) {
        for (Type t = this; t != null && t.getDefinition() != null; t = t.getDefinition().getSuper()) {
            if (t.includes(typeName)) {
                return true;
            }
        }
        return false;
    }

    
    
    /** Returns true if the passed type is the same as or is a component of
     *  this type.  The base class delegates this to the equals method.
     */
    public boolean includes(Type type) {
        return equals(type);
    }

    /** Returns true if a type of the passed name is the same as or is a component of
     *  this type.  The base class delegates this to the equals method.
     */
    public boolean includes(String typeName) {
        return getName().equals(typeName);
    }

    /** Returns the types of all children this type defines **/
    public Type[] getChildTypes() {
        return getChildTypes(false);
    }

    
    /** Returns the types of all children this type defines that
     *  represent persistable properties.  By default, this would
     *  be all cacheable (i.e. not dynamic and not static) chlid
     *  types.
     **/
    public Type[] getPersistableChildTypes() {
        return getChildTypes(true);
    }


    private Type[] getChildTypes(boolean onlyPersistable) {
        Type[] childTypes = null;
        if (definition != null && definition instanceof AbstractNode && definition.canHaveChildDefinitions()) {
            CantoBlock block = null;
            // the child definitions will be children of a CantoBlock,
            // which may be either the second or third child of the definition
            Object child = ((AbstractNode) definition).getChild(1);
            if (child instanceof CantoBlock) {
                block = (CantoBlock) child;
            } else {
                child = ((AbstractNode) definition).getChild(2);
                if (child instanceof CantoBlock) {
                    block = (CantoBlock) child;
                }
            }
            if (block != null) { 
                AbstractNode[] nodes = block.children;
                List<Type> types = new ArrayList<Type>(nodes.length); 
                for (int i = 0; i < nodes.length; i++) {                 
                    if (nodes[i] instanceof Definition) {
                        Definition childDef = (Definition) nodes[i];
                        if (!onlyPersistable || childDef.getDurability() == Definition.IN_CONTEXT) {
                            types.add(childDef.getType());
                        }
                    }
                }
                childTypes = new Type[types.size()];
                childTypes = (Type[]) types.toArray(childTypes);
            }
        }
        return childTypes;
    }

    
    /** Most types don't need to be resolved, so do nothing by default. **/
    public void resolve() {}

    public int levelsBelow(Type type, Context context) {
        if (this.equals(type)) {
            return 0;
        }
       
        if (getDims().size() > 0 || type.getDims().size() > 0) {
            Type thisType = getBaseType();
            Type otherType = type.getBaseType();
            // avoid infinite recursion
            if (!equals(thisType) || !type.equals(otherType)) {
                int levels = thisType.levelsBelow(otherType, context);
                if (levels >= 0) {
                    return levels + 1;
                } else {
                    return -1;
                }
            }
        }

        Definition def = getDefinition();
        if (def == null) {
            ArgumentList args = getArguments(context);
            for (Definition owner = getOwner(); owner != null; owner = owner.getOwner()) {
                // skip parameters and their immediate owners (the definitions they are
                // parameters for)
                if (owner.isFormalParam()) {
                    owner = owner.getOwner().getOwner();
                }

                def = owner.getChildDefinition(this, args, null, null, context, null);
                if (def != null) {
                    break;
                }
            }
        }
        if (def == null) {
            return -1;
        }

        int levels = -1;
        //Class argClass = def.getType().getTypeClass(context);
        //Class paramClass = type.getTypeClass(context);
        //if (paramClass.isAssignableFrom(argClass)) {
        //    levels = (argClass.equals(String.class) ? Definition.PARAM_DEFAULT : 0) + 10;
        //}

        Type st = def.getSuper();
        if (st == null) {

            // we don't automatically know what an ExternalDefinition is an implementation
            // of, so we can't rule out a hierarchical relationship; return an arbitrary
            // number higher than a close match
            if (def instanceof ExternalDefinition) {
                return 1024;
            }
            
            return levels;
        }
        int levelsUnder = st.levelsBelow(type, context);
        if (levelsUnder >= 0) {
            levels = levelsUnder + 1;
        }
        return levels;
    }


    /** Returns true if this type represents a collection. */
    public boolean isCollection() {
        if (getDims().size() > 0 || isArray() || isTable()) {
            return true;
        } else {
            return false;
        }
    }

    
    /** Returns true if this type or a supertype of this type represents a collection. */
    public boolean inheritsCollection() {
        for (Type t = this; t != null; t = t.getSuper()) {
            if (t.isCollection()) {
                return true;
            }
        }
        return false;
    }

    
    /** Returns the collection (array or table) type this type 
     * represents or is a subtype of, if any, else null.
     */
    public Type getCollectionType() {
        Class<?> c = getTypeClass(null);
        if (c != null && (c.isArray() || List.class.isAssignableFrom(c) || Map.class.isAssignableFrom(c))) {
            return this;
        }
        Definition def = getDefinition();
        if (def != null) {
            if (def instanceof CollectionDefinition) {
                return this;
            } else {
                Type st = def.getSuper();
                if (st != null) {
                    return st.getCollectionType();
                }
            }
        }
        return null;
    }

    /** Returns true if this type represents an array. */
    public boolean isArray() {
        return getArrayType() != null;
    }
        
    /** Returns the array type this type represents or is a subtype of, if any, else null. */
    public Type getArrayType() {
        Class<?> c = getTypeClass(null);
        if (c != null && (c.isArray() || List.class.isAssignableFrom(c))) {
            return this;
        }
        Definition def = getDefinition();
        if (def != null) {
            if (def instanceof CollectionDefinition && ((CollectionDefinition) def).isArray()) {
                return this;
            } else {
                Type st = def.getSuper();
                if (st != null) {
                    return st.getArrayType();
                }
            }
        }
        return null;
    }

    /** Returns true if this type represents a table . */
    public boolean isTable() {
        return getTableType() != null;
    }
  
    /** Returns the table type this type represents or is a subtype of, if any, else null. */
    public Type getTableType() {
        Class<?> c = getTypeClass(null);
        if (c != null && Map.class.isAssignableFrom(c)) {
            return this;
        }
        Definition def = getDefinition();
        if (def != null) {
            if (def instanceof CollectionDefinition && ((CollectionDefinition) def).isTable()) {
                return this;
            } else {
                Type st = def.getSuper();
                if (st != null) {
                    return st.getTableType();
                }
            }
        }
        return null;
    }

    /** Returns the base type, not including dimensions, represented by this type. */
    public Type getBaseType() {
        return this;
    }

    
    /** Returns true if this type can be the supertype of a type with the specified parameters. **/
    public boolean canBeSuperForParams(ParameterList params, Context context) {
        ArgumentList typeArgs = getArguments(context);
        int numArgs = (typeArgs == null ? 0 : typeArgs.size());
        int numParams = (params == null ? 0 : params.size());
        if (numArgs == 0) {
            return true;    // don't need any args; any subtype will match

        } else if (numParams < numArgs) {
            return false;    // not enough params to match with args
            
        } else {
            Iterator<Construction> itArgs = typeArgs.iterator();
            while (itArgs.hasNext()) {
                if (!params.matchesArg(itArgs.next())) {
                    return false;
                }
            }
            return true;    // every arg was matched
        }
    }
    
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;

        } else if (obj instanceof Type) {
            Type type = (Type) obj;
            int theseParts = numParts();
            int thoseParts = type.numParts();
            
            if ((theseParts == thoseParts && super.equals(type))
                 || (theseParts != thoseParts && (definition != null && definition.equals(type.getDefinition())))) {

                List<Dim> theseDims = getDims();
                List<Dim> thoseDims = type.getDims();
                if (theseDims == null || theseDims.size() == 0) {
                    return (thoseDims == null || thoseDims.size() == 0);
                } else {
                    return theseDims.equals(thoseDims);
                }
            }
        }
        return false;
    }
    
    public String toString() {
        return getName();
    }

    abstract public List<Dim> getDims();

    abstract public ArgumentList getArguments(Context context);

    abstract public Value convert(Value val);

    abstract public Class<?> getTypeClass(Context context);

}
