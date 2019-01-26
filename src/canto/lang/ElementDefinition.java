/* Canto Compiler and Runtime Engine
 * 
 * ElementDefinition.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;

import canto.runtime.Context;
import canto.runtime.CantoObjectWrapper;

/**
 * ElementDefinition defines an element in an array or table.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.46 $
 */

public class ElementDefinition extends AnonymousDefinition {

    /** Flag indicating whether the contents of the definition is
     *  a wrapped element (as opposed to the element itself).
     */
    private boolean wrapped = false;

    public ElementDefinition() {
        super();
    }

    public ElementDefinition(Definition owner, Object element) {
        super();
        setOwner(owner);
        setElement(element);
    }

    protected void setElement(Object element) {
        if (element instanceof ElementDefinition) {
            ElementDefinition def = (ElementDefinition) element;
            setContents(def.getContents());
            wrapped = def.wrapped;
        } else if (element instanceof ResolvedInstance) {
            setContents((AbstractNode) element);
        } else if (element instanceof Value) {
            Value value = (Value) element;
            Definition owner = getOwner();
            if (owner != null && owner instanceof CollectionDefinition) {
                Type ownerType = ((CollectionDefinition) owner).getElementType();
                if (ownerType != null) {
                    Class<?> collectionClass = ownerType.getTypeClass(null);
                    Class<?> elementClass = value.getValueClass();
                    if (collectionClass != null && elementClass != null && !collectionClass.isAssignableFrom(elementClass)) {
                        element = new PrimitiveValue(element, collectionClass);
                    }
                }
            }
            setContents((AbstractNode) element);
            
        } else if (element instanceof Chunk || element instanceof Definition) {
            setContents((AbstractNode) element);
        } else {
            setContents(new PrimitiveValue(element));
            wrapped = true;
        }
    }

    public Context getResolutionContext() {
        AbstractNode contents = getContents();
        if (contents instanceof ResolvedInstance) {
            return ((ResolvedInstance) contents).getResolutionContext();
        } else {
            return ((AnonymousDefinition) getOwner()).initContext;
        }
    }

    public Object getElement() {
        return getElement(null);
    }
    
    public Object getElement(Context context) {
        AbstractNode contents = getContents();

        if (wrapped) {
            return ((Value) contents).getValue();

        } else if (contents instanceof Instantiation && !(contents instanceof ResolvedInstance)) {
            if (context == null) {
                context = getResolutionContext();
                if (context != null) {
                    return new ResolvedInstance((Instantiation) contents, context, true);
                }
            }
            if (context != null) {
                contents = new ResolvedInstance((Instantiation) contents, context, false);
            }
        }
        return contents;
    }

    public String getName() {
        Context context = ((AnonymousDefinition) getOwner()).initContext;
        Object element = getElement(context);
        if (element == null) {
            return "";
        } else if (element instanceof Name) {
            return ((Name) element).getName();
        } else if (element instanceof Instantiation) {
            return "";
            //return ((Instantiation) element).getName();
        } else if (element instanceof Value) {
            Class<?> vc = ((Value) element).getValueClass();
            if (vc == null) {
                return "";
            } else {
                return ((Value) element).getValueClass().getName();
            }
        } else {
            return element.getClass().getName();
        }
    }

    public Definition getChildDefinition(NameNode name, Context context) {
        Context elementContext = ((AnonymousDefinition) getOwner()).initContext;
        if (elementContext != null) {
            context = elementContext;
        }
        Object element = getElement(context);
        if (context == null) { // && element instanceof AbstractNode) {
            throw new NullPointerException("null context passed to getChildDefinition");
//            AbstractNode node = (AbstractNode) element;
//            try {
//                context = new Context(node.getOwner());
//            } catch (Redirection r) {
//                throw new IllegalStateException("Unable to create context for child definition " + name.getName() + ": " + r.getMessage());
//                //context = new Context();
//            }
        }
        if (element instanceof Definition) {
            return ((Definition) element).getChildDefinition(name, context);
        } else if (element instanceof Instantiation) {
            Instantiation instance = (Instantiation) element;
            Definition def = instance.getDefinition(context);
            if (def != null) {
                return def.getChildDefinition(name, context);
            }
        } else if (element instanceof ValueMap) {
            ValueMap map = (ValueMap) element;
            return new ElementDefinition(this, map.get(name)); 
        }
        return null;
    }


    public Object getChild(NameNode name, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context context, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection {
        Object element = getElement(context);
        if (element instanceof Definition) {
            return ((Definition) element).getChild(name, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
        } else if (element instanceof Instantiation) {
            Instantiation instance = (Instantiation) element;
            Definition def = instance.getDefinition(context);
            if (def != null) {
                Context resolutionContext = context;
                ArgumentList childArgs = args;
                if (instance instanceof ResolvedInstance) {
                    resolutionContext = ((ResolvedInstance) instance).getResolutionContext();
                    if (args != null) {
                        childArgs = ResolvedInstance.resolveArguments(args, context);
                    }
                }
                indexes = context.resolveIndexes(indexes);
                ArgumentList elementArgs = instance.getArguments();
                ParameterList elementParams = def.getParamsForArgs(elementArgs, resolutionContext);
                resolutionContext.push(def, elementParams, elementArgs, false);
                try {
                    Object child = def.getChild(name, childArgs, indexes, parentArgs, resolutionContext, generate, trySuper, parentObj, resolver);
                    if (child != null && !generate) {
                        Definition childDef = ((DefinitionInstance) child).def;
                        if (childDef != null && childDef.isAliasInContext(context)) {
                            Instantiation aliasInstance = childDef.getAliasInstanceInContext(context);
                            ArgumentList aliasArgs = aliasInstance.getArguments();
                            ParameterList aliasParams = childDef.getParamsForArgs(aliasArgs, resolutionContext);
                            resolutionContext.push(childDef, aliasParams, aliasArgs, false);
                            try {
                                Definition aliasDef = aliasInstance.getDefinition(resolutionContext, this);
                                if (aliasDef != null) {
                                    child = aliasDef.getDefInstance(aliasArgs, aliasInstance.getIndexes());
                                }
                            } finally {
                                resolutionContext.pop();
                            }
                        }
                    }
                    return child;
                } finally {
                    resolutionContext.pop();
                }
            }
        } else if (element instanceof Value) {
             if (name.getName().equals(Name.COUNT)) {
                if (generate) {
                    return new PrimitiveValue(1);
                } else {
                    Definition countDef = new SingleElementCount(this);
                    return countDef.getDefInstance(null, null);
                }
            } else {
                Object obj = ((Value) element).getValue();
                if (obj instanceof CantoObjectWrapper) {
                    CantoObjectWrapper wrapper = (CantoObjectWrapper) obj;
                    if (generate) {
                        return wrapper.getChildData(name, null, args);
                    } else {
                        Definition def = wrapper.getDefinition();
                        return def.getDefInstance(args, indexes);
                    }
                }
            }
        } else if (element instanceof ValueMap) {
            ValueMap map = (ValueMap) element;
            Object obj = map.get(name.getName());
            if (generate) {
                return obj;
            } else {
                Definition def = new ElementDefinition(this, obj);
                return def.getDefInstance(args, indexes);
            }
        }
        return null;
    }


    /** Instantiates a child definition in a specified context and returns the result. */
    public Object getChildData(NameNode childName, Type type, Context context, ArgumentList args) throws Redirection {
        Object data = null;
//        ArgumentList args = childName.getArguments();

        Object element = getElement(context);
        if (element instanceof Definition) {
            // should we maybe pass null as the instance here??
            data = ((Definition) element).getChildData(childName, type, context, args);
        } else if (element instanceof Instantiation) {
            Instantiation elementInstance = (Instantiation) element;
            Definition def = elementInstance.getDefinition(context);
            if (def != null) {
                ArgumentList elementArgs = elementInstance.getArguments();
                ParameterList elementParams = def.getParamsForArgs(elementArgs, context);
                context.push(def, elementParams, elementArgs, false);
                data = def.getChildData(childName, type, context, args);
                context.pop();
            }
        } else if (element instanceof Value) {
            String name = childName.getName();
            
            // TODO: need to handle collection values here...
            if (name == Name.COUNT) {
                data = new PrimitiveValue(1);
            } else if (name == Name.TYPE) {
                data = getType().getName();
            }

        } else if (element instanceof ValueMap) {
            ValueMap map = (ValueMap) element;
            data = map.get(childName.getName());
        }
        return data;
    }

    /** Returns the type of this element definition, which is the narrower of two types: the
     *  dynamically-determined type of the element itself, or the base type of the collection,
     *  whichever is narrower.
     */
    public Type getType() {
        
        Type contentType = DefaultType.TYPE;
        AbstractNode contents = getContents();
        
        if (contents instanceof PrimitiveValue) {
            contentType = ((PrimitiveValue) contents).getType();
        } else if (contents instanceof ResolvedInstance) {
            contentType = ((ResolvedInstance) contents).getType();
        }
        
        Type collectionType = DefaultType.TYPE;
        Definition def = getOwner();
        if (def != null) {
            if (def instanceof CollectionDefinition) {
                collectionType = ((CollectionDefinition) def).getElementType();  // TODO: calculate correct type
            } else {
                collectionType = def.getType();
            }
        }
        
        if (contentType == DefaultType.TYPE) {
            return collectionType;
        } else if (collectionType == DefaultType.TYPE) {
            return contentType;
        } else if (contentType.isPrimitive()) {
            return collectionType;
        } else if (collectionType.isPrimitive()) {
            return contentType;
        } else if (contentType.isTypeOf(collectionType.getName())) {
            return contentType;
        } else if (collectionType.isTypeOf(contentType.getName())) {
            return collectionType;
        } else {
            return PrimitiveType.VOID;
        }
    }

    private Definition getBaseDefinition(Context context) {
        Object element = getElement(context);
        if (element instanceof Definition) {
            return (Definition) element;
        } else if (element instanceof Instantiation) {
            Instantiation instance = (Instantiation) element;
            Definition def = instance.getDefinition(context);
if (def == null)
System.err.println("***** ElementDefinition getBaseDefinition null due to no def for instance " + instance.getName());

            if (def != null) {
                return def;
            }
        }
if (getOwner() == null)
System.err.println("***** ElementDefinition getBaseDefinition null due to null owner");
        return getOwner();
    }

    public Type getSuper() {
        return getOwner().getSuper();
    }

    public Type getSuper(Context context) {
        return getBaseDefinition(context).getSuper(context);
    }

    public NamedDefinition getSuperDefinition() {
        return getOwner().getSuperDefinition();
    }

    public NamedDefinition getSuperDefinition(Context context) {
        return getBaseDefinition(context).getSuperDefinition(context);
    }
    
    public Definition getUltimateDefinition(Context context) {
        return getBaseDefinition(context);
    }

    protected ParameterList getMatch(ArgumentList args, Context argContext) {
        return ((AnonymousDefinition) getBaseDefinition(argContext)).getMatch(args, argContext);
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(prefix);
        sb.append(getContents().toString());
        return sb.toString();
    }
    
}

class SingleElementCount extends CountDefinition {

    public SingleElementCount(Definition elementDef) {
        super(elementDef);
    }

    public AbstractNode getContents() {
        return new PrimitiveValue(1);
    }
}

