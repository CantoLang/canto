/* Canto Compiler and Runtime Engine
 * 
 * CollectionDefinition.java
 *
 * Copyright (c) 2018-2021 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.CantoObjectWrapper;
import canto.runtime.Context;
import canto.runtime.Holder;

/**
 * CollectionDefinition is the common base class for array and table definitions.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.90 $
 */

public class CollectionDefinition extends ComplexDefinition /* implements DynamicObject */ {

    static public boolean isCollectionObject(Object obj) {
        return obj != null && (obj.getClass().isArray() || obj instanceof List<?> || obj instanceof Map<?, ?>);
    }
    
    private List<Dim> dims;
    private Dim.TYPE majorDimType = null;
    private boolean majorIsTable = false;
    private element_decorator decorator = null;
    private CollectionBuilder builder = null;
    private boolean dynamic = false;
    private boolean dynamic_initialized = false;

    
    public CollectionDefinition() {
        super();
    }

    public CollectionDefinition(CollectionDefinition def, Context context) {
        super(def, context);
        setDims(def.dims);
    }

    /** Returns true. */
    public boolean isCollection() {
        return true;
    }

    /** Returns true if this CollectionDefinition is initialized directly as a
     *  a collection, i.e., it's not an alias or a standard definition merely
     *  typed as a collection.
     **/
    public boolean isHonestCollection() {
        return (!isAlias() && !isParamAlias() && !(getContents() instanceof CantoBlock)); 
    }

    public CollectionDefinition getCollectionDefinition(Context context, ArgumentList args) {
        return this;
    }
    
    public element_decorator getDecorator() {
        return decorator;
    }
   
    /** Canto interface method */
    public int count() {
        return getSize();
    }

    public CantoArray getArray(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        if (isArray()) {
            ResolvedArray array = (ResolvedArray) getCollectionInstance(context, args, indexes);
            return array.getArray();
        } else {
            throw new Redirection(Redirection.STANDARD_ERROR, "getArray called on a table.");
        }
    }

    public Map<String, Object> getTable(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        if (isTable()) {
            ResolvedTable table = (ResolvedTable) getCollectionInstance(context, args, indexes);
            return table.getTable();
        } else {
            throw new Redirection(Redirection.STANDARD_ERROR, "getTable called on an array.");
        }
    }

    /** Retrieves from cache or instantiates this definition as an array.
     */
    @SuppressWarnings("unchecked")
    public List<Object> get_array(Context context) throws Redirection {
        Object data = context.getData(this, getName(), null, null);
        if (data != null && data instanceof List<?>) {
            return (List<Object>) data;
        }
        return instantiate_array(context);
    }
    
    /** Retrieves from cache or instantiates this definition as a table.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> get_table(Context context) throws Redirection {
        Object data = context.getData(this, getName(), null, null);
        if (data != null && data instanceof Map<?,?>) {
            return (Map<String, Object>) data;
        }
        return instantiate_table(context);
    }

    /** Instantiates this definition as an array.  
     */
    @SuppressWarnings("unchecked")
    public List<Object> instantiate_array(Context context) throws Redirection {
        Object arrayObj = getArray(context, null, null).getArrayObject();
        if (arrayObj instanceof List) {
            return (List<Object>) arrayObj;
        } else {
            return Arrays.asList((Object[]) arrayObj);
        }
    }
    
    /** Instantiates this definition as a table.
     */
    public Map<String, Object> instantiate_table(Context context) throws Redirection {
        return getTable(context, null, null);
    }

    
    /** Overrides the definition of resolveKeeps in NamedDefinition to do nothing, because
     *  arrays aren't in the scope of their superdefinitions.
     */
    public void resolveKeeps() {}

    public void resolveDims() {}
    
    public Object getChild(NameNode node, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context argContext, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection {
        if (Name.COUNT.equals(node.getName())) {
            if (generate) {
                if (parentObj != null) {
                    int size = 0;
                    if (parentObj instanceof ResolvedCollection) {
                        size = ((ResolvedCollection) parentObj).getSize();
                    } else if (parentObj.getClass().isArray()) {
                        size = java.lang.reflect.Array.getLength(parentObj);
                    } else if (parentObj instanceof List<?>) {
                        List<?> list = (List<?>) parentObj;
                        size = list.size();
                    } else if (parentObj instanceof Map<?,?>) {
                        Map<?,?> map = (Map<?,?>) parentObj;
                        size = map.size();
                    }
                    return new PrimitiveValue(size);    
                } else {
                    return new PrimitiveValue(getSize(argContext, parentArgs, indexes));
                }
            } else {
                Definition countDef = new CountDefinition(this, argContext, parentArgs, null);
                return countDef.getDefInstance(null, null);
            }
        } else if (Name.KEYS.equals(node.getName())) {
            CollectionDefinition keysDef = new KeysDefinition(this, argContext, parentArgs, null);
            if (generate) {
                //return keysDef.getCollectionInstance(argContext, args, indexes).getCollectionObject();
                return keysDef.construct(argContext, args, indexes);
                
            } else {
                return keysDef.getDefInstance(null, indexes);
            }
            
            
        }
        return super.getChild(node, args, indexes, parentArgs, argContext, generate, trySuper, parentObj, resolver);
    }

    public int getSize() {
        // this was commented out t correct a double G in array_test, but
        // that doesn't seem to be a problem any more.
        if (majorDimType == Dim.TYPE.DEFINITE) {
            return ((Dim) dims.get(0)).getSize();
        } else {
            return -1;
        }
    }


    public int getSize(Context context, ArgumentList args, List<Index> indexes) {
        if (majorDimType == Dim.TYPE.DEFINITE) {
            return ((Dim) dims.get(0)).getSize();
        } else try {
            CollectionInstance instance = getCollectionInstance(context, args, indexes);
            return instance.getSize();
        } catch (Redirection r) {
            log("Error in getSize call on " + getFullName() + ": " + r.getMessage());
            return 0;
        }
    }

    protected void setDims(List<Dim> dims) {
        this.dims = dims;
        int numDims = dims.size();
        Dim majorDim = (Dim) dims.get(numDims - 1);
        majorDimType = majorDim.getType();
        //if (builder == null) {
        //    setTable(majorDim.isTable());
        //}
    }
        
    protected void setTable(boolean isTable) {
        majorIsTable = isTable;
        if (majorIsTable) {
            setTableBuilder();
        } else {
            setArrayBuilder();
        }
    }

    public List<Dim> getDims() {
        return dims;
    }

    public CollectionBuilder getBuilder() {
        return builder;
    }

    protected void setBuilder(CollectionBuilder builder) {
        this.builder = builder;
    }
    
    protected void setTableBuilder() {
        setBuilder(new TableBuilder(this));
    }
    
    protected void setArrayBuilder() {
        setBuilder(new ArrayBuilder(this));
    }
    
    protected Type createType() {
        ArgumentList args = null;
        NameNode nameNode = this.getNameNode();
        if (nameNode instanceof NameWithArgs) {
            args = ((NameWithArgs) nameNode).getArguments();
        }

        ComplexType type = new ComplexType(this, this.getName(), dims, args);
        type.setOwner(this);
        return type;
    }

//    public CollectionDefinition initCollection(Context context, ArgumentList args) throws Redirection {
//        return (CollectionDefinition) initForContext(context, args);
//    }
//
//    abstract public Object initForContext(Context context, ArgumentList args) throws Redirection;

    /** Construct this definition with the specified arguments in the specified context. */
    protected Object construct(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        CollectionInstance instance = getCollectionInstance(context, args, null);
//        return instance;
        
        // get the arguments and parameters, if any, to push on the
        // context stack with the definition
        ParameterList params = getParamsForArgs(args, context);
        context.push(this, params, args, false);

        try {
            Object obj = instantiateCollectionObject(context, instance.getCollectionObject());
            if (indexes != null) {
                obj = context.dereference(obj, indexes);    
            }
            return obj;
            
        } finally {
            context.pop();
        }
    }

    /** Returns true if this definition defines a array, false it defines an table. */
    public boolean isArray() {
        return !majorIsTable;
    }

    /** Returns true if this definition defines a table, false it defines an array. */
    public boolean isTable() {
        return majorIsTable;
    }


    /** Creates a resolved instance of this collection in the specified context with the specified
     *  arguments.
     */
    public CollectionInstance createCollectionInstance(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        if (builder == null) {
            Type type = getType();
            Class<?> c = type.getTypeClass(context);
            if (c != null && (Map.class.isAssignableFrom(c))) {
                setTable(true);
            } else {
                setTable(false);
            }
        }
        return builder.createCollectionInstance(context, args, indexes);
    }

    /** Wraps the passed data in a collection instance in the specified context with the specified
     *  arguments.
     */
    public CollectionInstance createCollectionInstance(Context context, ArgumentList args, List<Index> indexes, Object collectionData) throws Redirection {
        if (builder == null) {
            Type type = getType();
            Class<?> c = type.getTypeClass(context);
            if (c != null && (Map.class.isAssignableFrom(c))) {
                setTable(true);
            } else {
                setTable(false);
            }
        }
        return builder.createCollectionInstance(context, args, indexes, collectionData);
    }

    public boolean isDynamic() {
        if (!dynamic_initialized) {
            Object contents = getContents();

            // array defined with an ArrayInitExpression
            if (contents instanceof ArgumentList) {
                ArgumentList elements = (ArgumentList) contents;
                if (elements != null) {
                    Iterator<Construction> it = elements.iterator();
                    while (it.hasNext()) {
                        Construction element;
                        try {
                            element = it.next();
                        } catch (Exception e) {
                            System.err.println("!!! Found non-construction in ArgumentList, ArrayDefinition 123");
                            e.printStackTrace();
                            continue;
                        }
                        if (element instanceof ConstructionGenerator) {
                            dynamic = true;
                            break;
                        }
                    }
                }
            }
        }
        return dynamic;
    }

    public boolean isGrowable() {
        return (majorDimType != Dim.TYPE.DEFINITE);
    }

    public Object getObjectForElement(Object element) {
        if (element instanceof ElementDefinition) {
            return ((ElementDefinition) element).getElement();
        } else if (element instanceof Holder) {
            return ((Holder) element).data;
        } else if (element instanceof ExternalDefinition) {
            return ((ExternalDefinition) element).getObject();
        } else {
            return element;
        }
    }

    public ElementReference getElementReference(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        CollectionInstance instance = getCollectionInstance(context, args, null);
        return new ElementReference(instance, indexes);
    }

    /**
     * @throws Redirection 
     */
    protected Object instantiateCollectionObject(Context context, Object collection) throws Redirection {
        //Type st = getSuper();
        // this seems to be unnecessary
        boolean resolveOnly = true; //!(st == null || st.isPrimitive() || st.isExternal());

        if (collection == null) {
            return null;

//        } else if (collection instanceof CantoArray) {
//            return ((CantoArray) collection).instantiateArray(context);

        } else if (collection instanceof Object[]) {
            Object[] elementArray = (Object[]) collection;
            Object[] array = new Object[elementArray.length];
            for (int i = 0; i < elementArray.length; i++) {
                Object obj = getObjectForElement(elementArray[i]);
                if (resolveOnly) {
                    obj = resolveElement(context, obj);
                    array[i] = obj;
                } else {
                    array[i] = getUnresolvedObjectValue(context, obj);
                }
            }
            return array;

        } else if (collection instanceof List<?>) {
            List<Object> list = Context.newArrayList(((List<?>)collection).size(), Object.class);
            Iterator<?> it = ((List<?>) collection).iterator();
            while (it.hasNext()) {
                Object obj = getObjectForElement(it.next());
                if (resolveOnly) {
                    obj = resolveElement(context, obj);
                    list.add(obj);
                } else {
                    list.add(getUnresolvedObjectValue(context, obj));
                }
            }
            return list;

        } else if (collection instanceof InstantiatedMap) {
            return collection;

        } else if (collection instanceof Map<?,?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = new InstantiatedMap(((Map<String, Object>) collection), this, context);
            return map;

        } else {
            return collection;
        }
    }
    
    private static Object getUnresolvedObjectValue(Context context, Object element) {
        //if (!(element instanceof ResolvedInstance) && !(element instanceof CantoObjectWrapper)) {
            return getObjectValue(context, element);
        //}
        //return element;
    }
    
    private Object resolveElement(Context context, Object element) {
        if (element instanceof ResolvedInstance) {
            return element;
        } else if (element instanceof Instantiation) {
            return new ResolvedInstance((Instantiation) element, context, false);
        } else {
            return element;
        }
    }

    public List<Construction> getConstructions(Context context) {
        CollectionInstance collectionInstance = null;
        try {
            collectionInstance = getCollectionInstance(context, null, null);
        } catch (Redirection r) {
            log(" ******** unable to obtain collection instance for " + getName() + " ******");
        }
        if (collectionInstance != null) {
            return new SingleItemList<Construction>((Construction) collectionInstance);
        } else {
            return new EmptyList<Construction>();
        }
    }
    
    
    protected Construction getConstructionForElement(Object element) {
        if (element instanceof Construction) {
            return (Construction) element;
        } else {
            return new PrimitiveValue(getObjectForElement(element));
        }
    }
    
    protected Definition getDefinitionForElement(Object element) {
        if (element == null) {
            return null;
        } else if (element instanceof Definition) {
            if (getElementType().isTypeOf("definition")) {
                Definition def = (Definition) element;
                return new ElementDefinition(this, new AliasedDefinition(def, def.getNameNode()));
            } else {
                return (Definition) element;
            }
        } else if (element instanceof AbstractNode) {
            return new ElementDefinition(this, element);
        } else if (element instanceof Holder) {
            Holder holder = (Holder) element;
            if (holder.data != null && holder.data != UNINSTANTIATED) {
                return getDefinitionForElement(holder.data);
            } else {
                return null;  // ((Holder) element).def;
            }
        } else if (isPrimitiveValue(element)) {
            return new ElementDefinition(this, element);
        } else if (element instanceof CantoObjectWrapper) {
            return new ElementDefinition(this, ((CantoObjectWrapper) element).getConstruction());
        } else {
            return new ExternalDefinition((NameNode) null, this, this, getElementType(), getAccess(), getDurability(), element, null);
        }
    }
    
    private static boolean isPrimitiveValue(Object value) {
        return (value instanceof Boolean || value instanceof Byte
                || value instanceof Character || value instanceof Double
                || value instanceof Float || value instanceof Integer
                || value instanceof Character || value instanceof Short
                || value instanceof Long || value instanceof String
                || value instanceof Object[] || value instanceof List<?>
                || value instanceof Map<?,?>);
    }

   public Type getElementType() {
        Type superType = getSuper();
        if (superType == null) {
            Definition owner = getOwner();
            if (owner instanceof CollectionDefinition) {
                superType = ((CollectionDefinition) owner).getSuper();
            }
        }
        if (superType == null) {
            return DefaultType.TYPE;
        } else {
            return superType.getBaseType();
        }
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(prefix);

        String typeAndName = getTypeAndName();
        if (typeAndName.length() > 0) {
            sb.append(typeAndName);
            sb.append("= ");
        }
        
        Object contents = getContents();
        if (isArray()) {
             if (contents instanceof ListNode<?>) {
                 ListNode<?> listNode = (ListNode<?>) contents;
                 sb.append(listNode.toString("[ ", " ]"));
             } else {
                 sb.append("[ ");
                 sb.append(contents.toString());
                 sb.append(" ]");
             }
        } else {
            if (contents instanceof ListNode<?>) {
                ListNode<?> listNode = (ListNode<?>) contents;
                sb.append(listNode.toString("{ ", " }"));
            } else {
                sb.append("{ ");
                sb.append(contents.toString());
                sb.append(" }");
            }
        }
        return sb.toString();
    }
}
